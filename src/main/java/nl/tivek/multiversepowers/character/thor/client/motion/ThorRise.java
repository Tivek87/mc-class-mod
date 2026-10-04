package nl.tivek.multiversepowers.character.thor.client.motion;

import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.thor.storm.LightningBomb;
import nl.tivek.multiversepowers.engine.client.ragdoll.Downed;
import nl.tivek.multiversepowers.engine.math.Ease;

// Your own Thor's lightning bomb: he rises slowly, hangs still while it charges and, once it has burst, sinks back
// down, on the server's times (LightningBomb). Never told the server took it, he lets go of it.
public final class ThorRise {
    private static final int UNTOLD = 12;
    private static final double SINK = -0.3;
    private static int age = -1;
    private static boolean taken;
    private static double fromY;
    private static double sink;

    private ThorRise() {
    }

    public static boolean active() {
        return age >= 0;
    }

    static void start(LocalPlayer player) {
        age = 0;
        taken = false;
        fromY = player.getY();
        sink = 0.0;
    }

    // The server's word: it took the bomb, or a knockdown put it out.
    public static void told(boolean putOut) {
        if (putOut) {
            age = -1;
        } else {
            taken = true;
        }
    }

    static void stop() {
        age = -1;
    }

    static void tick(LocalPlayer player, Input input) {
        ThorGroundMotion.still(input);
        input.shiftKeyDown = false;
        int t = age++;
        if (Downed.now() || !taken && t > UNTOLD) {
            age = -1;
            return;
        }
        Vec3 v = player.getDeltaMovement();
        if (t < LightningBomb.RISE) {
            double y = fromY + LightningBomb.HEIGHT * Ease.smooth((t + 1.0) / LightningBomb.RISE);
            player.setDeltaMovement(v.x * 0.5, y - player.getY(), v.z * 0.5);
        } else if (t < LightningBomb.BURST) {
            player.setDeltaMovement(v.x * 0.5, 0.0, v.z * 0.5);
        } else {
            sink = Mth.lerp(0.15, sink, SINK);
            player.setDeltaMovement(v.x * 0.9, sink, v.z * 0.9);
            if (t > LightningBomb.BURST + 3 && player.onGround() || t > LightningBomb.BURST + LightningBomb.SINK) {
                age = -1;
            }
        }
        player.resetFallDistance();
    }
}
