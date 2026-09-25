package nl.tivek.multiversepowers.update.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;

/** One release of the mod on GitHub: its version, when it came out, its notes and the jar to download. */
record Release(String version, Instant published, String notes, @Nullable Asset jar) {
    /** Only jars from the mod's own release page are ever downloaded. */
    static final String DOWNLOAD_PREFIX = "https://github.com/" + UpdateChecker.REPO + "/releases/download/";
    private static final String JAR_PREFIX = "multiverse-powers-";

    /** The jar of a release, with the SHA-256 checksum GitHub worked out for it. */
    record Asset(String name, URI url, long size, String sha256) {
    }

    /** Every full release in a GitHub release list (drafts and pre-releases left out). */
    static List<Release> parseList(String json) {
        List<Release> releases = new ArrayList<>();
        JsonArray array = JsonParser.parseString(json).getAsJsonArray();
        for (JsonElement element : array) {
            JsonObject release = element.getAsJsonObject();
            if (flag(release, "draft") || flag(release, "prerelease") || !release.has("tag_name")) {
                continue;
            }
            String version = stripV(release.get("tag_name").getAsString());
            Instant published = text(release, "published_at") == null ? Instant.EPOCH
                    : Instant.parse(text(release, "published_at"));
            String notes = text(release, "body") == null ? "" : text(release, "body");
            releases.add(new Release(version, published, notes, jar(release)));
        }
        return releases;
    }

    @Nullable
    private static Asset jar(JsonObject release) {
        if (!release.has("assets")) {
            return null;
        }
        for (JsonElement element : release.getAsJsonArray("assets")) {
            JsonObject asset = element.getAsJsonObject();
            String name = text(asset, "name");
            String url = text(asset, "browser_download_url");
            String digest = text(asset, "digest");
            if (name != null && name.startsWith(JAR_PREFIX) && name.endsWith(".jar") && !name.contains("/")
                    && url != null && url.startsWith(DOWNLOAD_PREFIX)
                    && digest != null && digest.startsWith("sha256:")) {
                long size = asset.has("size") ? asset.get("size").getAsLong() : -1;
                return new Asset(name, URI.create(url), size, digest.substring("sha256:".length()).toLowerCase(Locale.ROOT));
            }
        }
        return null;
    }

    private static boolean flag(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() && object.get(key).getAsBoolean();
    }

    @Nullable
    private static String text(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : null;
    }

    static String stripV(String tag) {
        return tag.startsWith("v") || tag.startsWith("V") ? tag.substring(1) : tag;
    }

    /**
     * Which version is newer: below 0 when {@code a} is older, above 0 when newer. The numbers count ("0.0.10" is
     * past "0.0.9"); with the same numbers a version without a suffix is past one with ("1.0.0" after "1.0.0-alpha").
     */
    static int compare(String a, String b) {
        String[] partsA = split(stripV(a));
        String[] partsB = split(stripV(b));
        String[] numbersA = partsA[0].split("\\.");
        String[] numbersB = partsB[0].split("\\.");
        for (int i = 0; i < Math.max(numbersA.length, numbersB.length); i++) {
            int difference = Long.compare(number(numbersA, i), number(numbersB, i));
            if (difference != 0) {
                return difference;
            }
        }
        if (partsA[1].isEmpty() || partsB[1].isEmpty()) {
            return Boolean.compare(partsA[1].isEmpty(), partsB[1].isEmpty());
        }
        return partsA[1].compareTo(partsB[1]);
    }

    private static String[] split(String version) {
        int dash = version.indexOf('-');
        return dash < 0 ? new String[] {version, ""} : new String[] {version.substring(0, dash), version.substring(dash + 1)};
    }

    private static long number(String[] numbers, int index) {
        if (index >= numbers.length) {
            return 0;
        }
        try {
            return Long.parseLong(numbers[index].trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
