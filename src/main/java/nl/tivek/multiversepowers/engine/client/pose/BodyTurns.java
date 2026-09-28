package nl.tivek.multiversepowers.engine.client.pose;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.player.AbstractClientPlayer;

// Turns a player's whole body as it is drawn (a flyer lying flat, a dash leaning into its run), before the game's own
// turn of the body: every power's turn in one list, applied in the order they were added.
public final class BodyTurns {
    @FunctionalInterface
    public interface Turn {
        void turn(AbstractClientPlayer player, PoseStack pose, float scale);
    }

    private static final List<Turn> TURNS = new ArrayList<>();

    private BodyTurns() {
    }

    public static void add(Turn turn) {
        TURNS.add(turn);
    }

    public static void apply(AbstractClientPlayer player, PoseStack pose, float scale) {
        for (int i = 0; i < TURNS.size(); i++) {
            TURNS.get(i).turn(player, pose, scale);
        }
    }
}
