package nl.tivek.multiversepowers.update.client;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import org.slf4j.Logger;

@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class UpdateChecker {
    public static final String REPO = "Tivek87/mc-class-mod";
    static final String USER_AGENT = "multiverse-powers-updater";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final URI LATEST = URI.create("https://github.com/" + REPO + "/releases/latest");
    private static final URI RELEASES = URI.create("https://api.github.com/repos/" + REPO + "/releases?per_page=30");
    private static final String TAG_PATH = "/releases/tag/";
    private static final long FIRST_CHECK_MS = 5_000L;
    private static final long INTERVAL_MS = 5 * 60_000L;
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Multiverse Powers update check");
        thread.setDaemon(true);
        return thread;
    });

    @Nullable
    private static String installed;
    private static long nextCheck;
    private static volatile boolean checking;
    @Nullable
    private static volatile String seenTag;
    private static List<Release> newer = List.of();
    @Nullable
    private static String announced;
    private static volatile long lastCheck;
    private static volatile boolean lastFailed;
    @Nullable
    private static volatile List<Release> releases;
    private static volatile boolean loadingReleases;
    private static volatile boolean releasesFailed;

    private UpdateChecker() {
    }

    static boolean checking() {
        return checking;
    }

    static long lastCheck() {
        return lastCheck;
    }

    static boolean lastFailed() {
        return lastFailed;
    }

    @Nullable
    static List<Release> releasesIfLoaded() {
        return releases;
    }

    static boolean releasesFailed() {
        return releasesFailed;
    }

    @Nullable
    static Release installedRelease() {
        List<Release> all = releases;
        return all == null ? null
                : all.stream().filter(release -> Release.compare(release.version(), installed()) == 0).findFirst().orElse(null);
    }

    static void loadReleases() {
        if (releases != null || loadingReleases) {
            return;
        }
        loadingReleases = true;
        releasesFailed = false;
        WORKER.execute(() -> {
            try {
                releases = newestFirst(releases());
            } catch (IOException | RuntimeException e) {
                releasesFailed = true;
                LOGGER.debug("Could not load the release notes", e);
            } catch (InterruptedException e) {
                releasesFailed = true;
                Thread.currentThread().interrupt();
            } finally {
                loadingReleases = false;
            }
        });
    }

    private static List<Release> newestFirst(List<Release> list) {
        return list.stream().sorted(Comparator.comparing(Release::version, Release::compare).reversed()).toList();
    }

    static void checkNow() {
        if (!checking) {
            nextCheck = 1L;
        }
    }

    public static String installed() {
        if (installed == null) {
            installed = ModList.get().getModContainerById(MultiversePowers.MODID)
                    .map(container -> container.getModInfo().getVersion().toString())
                    .orElse("0");
        }
        return installed;
    }

    static List<Release> newer() {
        return newer;
    }

    @Nullable
    static Release latest() {
        return newer.isEmpty() ? null : newer.get(0);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        long now = System.currentTimeMillis();
        if (nextCheck == 0L) {
            installed();
            UpdateInstaller.cleanUp();
            nextCheck = now + FIRST_CHECK_MS;
        }
        if (!checking && now >= nextCheck) {
            checking = true;
            nextCheck = now + INTERVAL_MS;
            WORKER.execute(UpdateChecker::check);
        }
    }

    private static void check() {
        lastFailed = false;
        try {
            String tag = latestTag();
            if (tag == null || tag.equals(seenTag)) {
                return;
            }
            String running = installed();
            if (Release.compare(tag, running) <= 0) {
                seenTag = tag;
                return;
            }
            List<Release> all = newestFirst(releases());
            releases = all;
            List<Release> fresh = all.stream()
                    .filter(release -> Release.compare(release.version(), running) > 0)
                    .toList();
            // A release whose jar is still uploading is picked up again next time.
            if (fresh.isEmpty() || fresh.get(0).jar() == null) {
                return;
            }
            seenTag = tag;
            Minecraft.getInstance().execute(() -> found(fresh));
        } catch (IOException | RuntimeException e) {
            lastFailed = true;
            LOGGER.debug("Update check failed", e);
        } catch (InterruptedException e) {
            lastFailed = true;
            Thread.currentThread().interrupt();
        } finally {
            lastCheck = System.currentTimeMillis();
            checking = false;
        }
    }

    private static void found(List<Release> fresh) {
        newer = fresh;
        Release latest = fresh.get(0);
        if (!latest.version().equals(announced)) {
            announced = latest.version();
            LOGGER.info("Multiverse Powers {} is out (running {})", latest.version(), installed());
            UpdatePopup.announce(latest);
        }
    }

    @Nullable
    // Cheap redirect check instead of the API, to avoid GitHub's rate limit.
    private static String latestTag() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(LATEST)
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .header("User-Agent", USER_AGENT)
                .timeout(Duration.ofSeconds(15))
                .build();
        HttpResponse<Void> response = HTTP.send(request, HttpResponse.BodyHandlers.discarding());
        String location = response.headers().firstValue("location").orElse("");
        int at = location.indexOf(TAG_PATH);
        if (response.statusCode() / 100 != 3 || at < 0) {
            return null;
        }
        String tag = URLDecoder.decode(location.substring(at + TAG_PATH.length()), StandardCharsets.UTF_8);
        return tag.isBlank() || tag.contains("/") ? null : tag;
    }

    private static List<Release> releases() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(RELEASES)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .timeout(Duration.ofSeconds(20))
                .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("GitHub answered " + response.statusCode());
        }
        return Release.parseList(response.body());
    }
}
