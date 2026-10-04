package nl.tivek.multiversepowers.update.client;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.bugreport.client.BugReportScreen;
import nl.tivek.multiversepowers.engine.client.gui.NavScreen;
import nl.tivek.multiversepowers.engine.client.gui.PixelIcons;
import nl.tivek.multiversepowers.update.client.tour.Tour;

// The update manager: one window with its pages down the left (updates: what is new and every version; feedback: a bug
// report, an idea or the ones you submitted) and the tour; each page, or part of one, is one of these. The version on
// the title bar copies what a bug report wants to know about your game.
public abstract class ManagerScreen extends NavScreen {
    public static final String UPDATES = "updates";
    public static final String NEWS = "news";
    public static final String VERSIONS = "versions";
    public static final String FEEDBACK = "feedback";
    public static final String BUG = "bug";
    public static final String IDEA = "idea";
    public static final String REPORTS = "reports";
    public static final String TOUR = "tour";
    private static final long COPIED_MS = 1500L;

    private static long copiedAt = -COPIED_MS;
    // The feedback part opened last, which the feedback page opens on again.
    private static String feedback = BUG;

    protected ManagerScreen(Component title, @Nullable Screen root, String page) {
        super(title, root, page);
    }

    // Opens the manager on what is new; closing it returns to `root`.
    public static void open(@Nullable Screen root) {
        open(root, NEWS);
    }

    // Opens the page or part `id` (the updates page on what is new), keeping where the manager returns to.
    public static void open(@Nullable Screen root, String id) {
        String part = id.equals(FEEDBACK) ? feedback : id;
        if (part.equals(BUG) || part.equals(IDEA) || part.equals(REPORTS)) {
            feedback = part;
        }
        Minecraft.getInstance().setScreen(switch (part) {
            case BUG -> BugReportScreen.bug(root);
            case IDEA -> BugReportScreen.idea(root);
            case REPORTS -> BugReportScreen.reports(root);
            case VERSIONS -> new UpdateManagerScreen(root, VERSIONS);
            default -> new UpdateManagerScreen(root, NEWS);
        });
    }

    // The updates page's parts: what is new and every version, `picked` open.
    protected List<Tab> updatesTabs(String picked) {
        return List.of(this.tab(NEWS, null, picked), this.tab(VERSIONS, null, picked));
    }

    // The feedback page's parts: a bug report, an idea and the ones you submitted, `picked` open.
    protected List<Tab> feedbackTabs(String picked) {
        int sent = BugReportScreen.sentCount();
        return List.of(this.tab(BUG, null, picked), this.tab(IDEA, null, picked),
                this.tab(REPORTS, sent == 0 ? null : Component.literal(String.valueOf(sent)), picked));
    }

    private Tab tab(String id, @Nullable Component count, String picked) {
        return new Tab(id, text("tab." + id), count, id.equals(picked), () -> open(this.root, id));
    }

    // Where the window open now (the manager, the settings) returns to, or the screen itself when it is no such window.
    @Nullable
    public static Screen rootOf(@Nullable Screen screen) {
        return screen instanceof NavScreen window ? window.root() : screen;
    }

    public static Component text(String key, Object... args) {
        return Component.translatable("screen." + MultiversePowers.MODID + ".update." + key, args);
    }

    @Override
    protected List<Item> items() {
        return List.of(
                this.item(UPDATES, PixelIcons.Icon.DOWNLOAD),
                this.item(FEEDBACK, PixelIcons.Icon.BUG),
                new Item(TOUR, text("nav." + TOUR), PixelIcons.Icon.SPARK, Tour::replay, true));
    }

    private Item item(String id, PixelIcons.Icon icon) {
        return new Item(id, text("nav." + id), icon, () -> open(this.root, id), false);
    }

    @Override
    protected Component brand() {
        return text("brand");
    }

    @Override
    protected PixelIcons.Icon brandIcon() {
        return PixelIcons.Icon.DOWNLOAD;
    }

    @Override
    protected Component brandTag() {
        return Util.getMillis() - copiedAt < COPIED_MS ? text("copied")
                : Component.literal(UpdateManagerScreen.name(UpdateChecker.installed()));
    }

    @Override
    protected void clickedBrandTag() {
        String neoForge = ModList.get().getModContainerById("neoforge")
                .map(container -> container.getModInfo().getVersion().toString()).orElse("?");
        Minecraft.getInstance().keyboardHandler.setClipboard("Multiverse Powers v" + UpdateChecker.installed()
                + " · Minecraft " + SharedConstants.getCurrentVersion().getName() + " · NeoForge " + neoForge);
        copiedAt = Util.getMillis();
    }

    @Override
    @Nullable
    protected Badge badge(String id) {
        return switch (id) {
            case UPDATES -> {
                if (UpdateInstaller.state() == UpdateInstaller.State.DOWNLOADING) {
                    yield new Badge(Component.literal(Math.round(UpdateInstaller.progress() * 100.0F) + "%"), ACCENT);
                }
                yield UpdateChecker.latest() != null ? new Badge(null, WARN) : null;
            }
            case TOUR -> Tour.waiting() ? new Badge(null, ACCENT) : null;
            default -> null;
        };
    }
}
