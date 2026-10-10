package nl.tivek.multiversepowers.engine.client.escape;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.engine.entity.EscapePayload;

// The escape game of a held player (Captives): a stripe runs to and fro along a bar, and a click while it is over the
// yellow zone scores one, over the red one (smaller) one of its own; enough of either sets them free. Each click is a
// try: the zones move after every one, and with all tries spent the score starts over (or the game is lost). Each
// game is content's: its rules and the look of its bar (a Theme).
public final class EscapeGames {
    // `tries` clicks a round; `yellow` and `red` hits to win (red 0: no red zone); zone widths as parts of the bar;
    // the stripe crosses the bar `speed` times a second; `ticks` to play at most (0: no limit); `again`: a round
    // spent starts over rather than losing.
    public record Rules(int tries, int yellow, int red, float yellowWidth, float redWidth, float speed, int ticks,
            boolean again) {
    }

    @FunctionalInterface
    public interface Theme {
        void draw(GuiGraphics graphics, float x, float y, float width, float height, View view);
    }

    // What a theme draws: where the stripe and zones are (parts of the bar), the score, the tries left this round,
    // how fresh the last click is (1 just now) and whether it scored, the time left (1 to 0, or -1 with no limit),
    // and whether the game is won or lost.
    public record View(Rules rules, float stripe, float yellowAt, float redAt, int yellowHits, int redHits, int tries,
            float flash, boolean scored, float timeLeft, boolean won, boolean lost) {
    }

    private record Game(Rules rules, Theme theme) {
    }

    private static final Map<Integer, Game> GAMES = new HashMap<>();
    private static final long FLASH_MS = 350L;
    // How long a won or lost game stays up.
    private static final long END_MS = 1200L;
    private static final RandomSource RANDOM = RandomSource.create();

    @Nullable
    private static Game game;
    private static boolean held;
    private static long startMs;
    private static float yellowAt;
    private static float redAt;
    private static int yellowHits;
    private static int redHits;
    private static int tries;
    private static long clickMs = Long.MIN_VALUE / 2L;
    private static boolean scored;
    private static boolean won;
    private static long lostMs = -1L;
    private static long endMs = -1L;

    private EscapeGames() {
    }

    public static void register(int id, Rules rules, Theme theme) {
        GAMES.put(id, new Game(rules, theme));
    }

    // The server holds this player for game `id`, or lets them go (0).
    public static void told(int id) {
        Game next = GAMES.get(id);
        held = id != 0;
        // Let go once won (or lost), the game still shows how it ended a moment.
        if (next == null && endMs >= 0L) {
            return;
        }
        game = next;
        if (next == null) {
            return;
        }
        startMs = Util.getMillis();
        yellowHits = 0;
        redHits = 0;
        tries = 0;
        won = false;
        lostMs = -1L;
        endMs = -1L;
        clickMs = Long.MIN_VALUE / 2L;
        place(next.rules());
    }

    // Held now: no walking, no powers; the left button plays.
    public static boolean active() {
        return held;
    }

    // A game is up: being played, or showing how it ended.
    static boolean shown() {
        return game != null;
    }

    // Still playing: not yet won, lost or out of time.
    static boolean playing() {
        return game != null && !won && lostMs < 0L;
    }

    static void click() {
        Game now = game;
        if (now == null || !playing()) {
            return;
        }
        Rules rules = now.rules();
        float stripe = stripe(rules);
        boolean red = rules.red() > 0 && stripe >= redAt && stripe <= redAt + rules.redWidth();
        boolean yellow = !red && stripe >= yellowAt && stripe <= yellowAt + rules.yellowWidth();
        if (red) {
            redHits++;
        } else if (yellow) {
            yellowHits++;
        }
        scored = red || yellow;
        clickMs = Util.getMillis();
        tries++;
        if (yellowHits >= rules.yellow() || rules.red() > 0 && redHits >= rules.red()) {
            won = true;
            endMs = Util.getMillis();
            PacketDistributor.sendToServer(EscapePayload.INSTANCE);
            return;
        }
        if (tries >= rules.tries()) {
            if (!rules.again()) {
                lostMs = Util.getMillis();
                endMs = lostMs;
                return;
            }
            tries = 0;
            yellowHits = 0;
            redHits = 0;
        }
        place(rules);
    }

    // Every frame: out of time loses; a won or lost game shows a moment, then goes.
    static void tick() {
        Game now = game;
        if (now == null) {
            return;
        }
        long ms = Util.getMillis();
        if (playing() && now.rules().ticks() > 0 && ms - startMs >= now.rules().ticks() * 50L) {
            lostMs = ms;
            endMs = ms;
        }
        if (endMs >= 0L && ms - endMs > END_MS) {
            game = null;
        }
    }

    static void draw(GuiGraphics graphics, float x, float y, float width, float height) {
        Game now = game;
        if (now == null) {
            return;
        }
        Rules rules = now.rules();
        long ms = Util.getMillis();
        float flash = 1.0F - Mth.clamp((ms - clickMs) / (float) FLASH_MS, 0.0F, 1.0F);
        float left = rules.ticks() <= 0 ? -1.0F
                : 1.0F - Mth.clamp((ms - startMs) / (rules.ticks() * 50.0F), 0.0F, 1.0F);
        now.theme().draw(graphics, x, y, width, height, new View(rules, stripe(rules), yellowAt, redAt, yellowHits,
                redHits, tries, flash, scored, left, won, lostMs >= 0L));
    }

    // To and fro along the bar, `speed` crossings a second.
    private static float stripe(Rules rules) {
        double crossings = (Util.getMillis() - startMs) / 1000.0 * rules.speed();
        double phase = crossings % 2.0;
        return (float) (phase < 1.0 ? phase : 2.0 - phase);
    }

    // New zones, each somewhere on the bar and never on each other.
    private static void place(Rules rules) {
        yellowAt = 0.04F + RANDOM.nextFloat() * (0.92F - rules.yellowWidth());
        if (rules.red() <= 0) {
            return;
        }
        for (int attempt = 0; attempt < 20; attempt++) {
            redAt = 0.04F + RANDOM.nextFloat() * (0.92F - rules.redWidth());
            if (redAt + rules.redWidth() < yellowAt - 0.03F || redAt > yellowAt + rules.yellowWidth() + 0.03F) {
                return;
            }
        }
        redAt = yellowAt > 0.5F ? 0.05F : 0.95F - rules.redWidth();
    }
}
