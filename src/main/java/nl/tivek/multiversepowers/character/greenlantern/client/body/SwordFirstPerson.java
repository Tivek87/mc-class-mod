package nl.tivek.multiversepowers.character.greenlantern.client.body;

import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ViewportEvent;
import nl.tivek.multiversepowers.character.greenlantern.ability.SwordMove;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.character.greenlantern.client.render.SwordPainter;
import nl.tivek.multiversepowers.config.client.ClientSettings;
import nl.tivek.multiversepowers.engine.math.Ease;
import org.joml.Quaternionf;
import org.joml.Vector3f;

abstract class SwordFirstPerson extends SwordSeen {
    static final Vec3 OWN_SHOULDER_RIGHT = new Vec3(0.8, -1.3, 0.1);
    static final Vec3 OWN_SHOULDER_LEFT = new Vec3(-0.86, -1.32, 0.12);
    private static final double OWN_FOLLOW = 0.3;
    private static final float OWN_ARM = 0.66F;
    private static final double OWN_ARM_PAST = 0.1;
    private static final float OWN_ARM_SHORTEST = 0.66F;
    private static final float OWN_ARM_LONGEST = 1.3F;
    private static final double ARM_BACK = 11.0 / 16.0;
    // the game always draws first-person hands at this FOV, whatever the player's own setting is
    private static final double HAND_FOV = 70.0;
    static final float KICK_TICKS = 5.0F;
    static final float KICK_ROLL = 1.6F;
    static final float KICK_DIP = 0.8F;
    private static final double OWN_BEAM = 0.4;

    record Drawn(Vec3 grip, Vec3 blade, Vec3 edge, Vec3 shield, Vec3 face, Vec3 top, float sword,
            float shieldGrown) {
    }

    @Nullable
    static Drawn drawn;
    @Nullable
    static Drawn shards;
    static float shardsSince;
    static float kickAt = -100.0F;
    static float kickRoll;
    static float kickHard;
    static float shown;
    static float shownAt = Float.NaN;
    @Nullable
    static float[] looked;
    @Nullable
    static float[] lookGone;
    static float lookGoneAt;

    static void feel(LocalPlayer player, Own mine, float now) {
        int t = (int) Math.floor(now - mine.start);
        if (t == mine.felt) {
            return;
        }
        boolean blow = switch (mine.move.kind()) {
            case ATTACK, BASH, SLAM -> contains(mine.move.hits(), t);
            case FLURRY -> t >= SwordMove.FIRST_STAB && (t - SwordMove.FIRST_STAB) % SwordMove.STAB_EVERY == 0
                    && (t - SwordMove.FIRST_STAB) / SwordMove.STAB_EVERY < SwordMove.STABS;
            case EQUIP, CHARGE -> false;
        };
        if (!blow) {
            return;
        }
        mine.felt = t;
        kick(player, mine.start + t, switch (mine.move.kind()) {
            case SLAM -> 2.0F;
            case BASH -> 1.3F;
            case FLURRY -> 0.45F;
            default -> mine.move == SwordMove.OVERHEAD || mine.move == SwordMove.LUNGE ? 1.4F : 1.0F;
        });
    }

    static void kick(LocalPlayer player, float at, float hard) {
        Blend blend = BLENDS.get(player.getId());
        double across = 1.0;
        if (blend != null && blend.move != null) {
            SwordPoses.Pose then = curve(blend, at - blend.start, at);
            SwordPoses.Pose was = curve(blend, at - 1.0F - blend.start, at - 1.0F);
            across = then.hand().add(then.blade()).x - was.hand().add(was.blade()).x;
        }
        kickAt = at;
        kickRoll = across < 0.0 ? -1.0F : 1.0F;
        kickHard = hard;
    }

    private static boolean contains(int[] ticks, int t) {
        for (int tick : ticks) {
            if (tick == t) {
                return true;
            }
        }
        return false;
    }

    static void turn(ViewportEvent.ComputeCameraAngles event, float[] look) {
        event.setPitch(event.getPitch() - lookUp(event.getPitch(), look[0]) * Mth.RAD_TO_DEG);
        event.setYaw(event.getYaw() + look[1] * Mth.RAD_TO_DEG);
    }

    static float lookUp(float pitch, float up) {
        return (pitch - Mth.clamp(pitch - up * Mth.RAD_TO_DEG, -90.0F, 90.0F)) * Mth.DEG_TO_RAD;
    }

    @Nullable
    static float[] lookGone(float now) {
        float[] gone = lookGone;
        if (gone == null) {
            return null;
        }
        float back = 1.0F - (float) Ease.smooth((now - lookGoneAt) / LOOK_BACK);
        if (back <= 0.0F) {
            lookGone = null;
            return null;
        }
        return new float[] { gone[0] * back, gone[1] * back };
    }

    static boolean handsFree(LocalPlayer player) {
        return !player.isInvisible() && player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty();
    }

    @Nullable
    static float[] ownLook(LocalPlayer player, float partialTick) {
        State state = state(player, partialTick);
        float feel = Math.min(1.0F, ClientSettings.cameraShake());
        if (state == null || feel <= 0.0F) {
            return null;
        }
        SwordPoses.Pose posed = pose(player, state, partialTick);
        Blend blend = BLENDS.get(player.getId());
        float now = now(partialTick);
        float[] watching = watching(blend, state, now);
        if (watching == null) {
            return null;
        }
        float t = watching[0];
        SwordPoses.Pose pose = state.move() == SwordMove.EQUIP ? posed : taking(t, now);
        float[] look = SwordPoses.look(t, pose);
        float amount = watching[1] * feel * (float) Ease.smooth(shown);
        return new float[] { look[0] * amount, look[1] * amount };
    }

    static void drawOwn(LanternPainter painter, LocalPlayer player, State state, SwordPoses.Pose pose,
            double apart, float now) {
        Vec3 grip = pose.hand();
        Vec3 blade = pose.blade();
        Vec3 edge = pose.edge();
        float flying = flying(state);
        if (flying >= 0.0F) {
            SwordPoses.Flight flight = SwordPoses.flight(flying);
            if (apart <= 0.0) {
                tossTrail(painter, flying, SwordPoses.OWN_SWORD, SwordPoses::flight);
            }
            grip = flight.grip();
            blade = flight.blade();
            edge = flight.edge();
        }
        float swordGrown = SwordPoses.swordGrown(state.move(), formed(state));
        float shieldGrown = SwordPoses.shieldGrown(state.move(), formed(state));
        SwordPainter.sword(painter, grip, blade, edge, SwordPoses.OWN_SWORD, swordGrown, apart);
        SwordPainter.shield(painter, pose.shield(), pose.face(), pose.top(), SwordPoses.OWN_SHIELD, shieldGrown, apart);
        if (apart > 0.0) {
            return;
        }
        drawn = new Drawn(grip, blade, edge, pose.shield(), pose.face(), pose.top(), swordGrown, shieldGrown);
        gleam(painter, state, grip, blade, SwordPoses.OWN_SWORD);
        if (shieldGrown < 1.0F) {
            painter.beam(pose.hand(), pose.shield().add(pose.face().scale(0.06 * SwordPoses.OWN_SHIELD)),
                    feeding(shieldGrown), OWN_BEAM);
        }
        clang(painter, state, pose.shield(), pose.face(), pose.top(), SwordPoses.OWN_SHIELD);
        Blend blend = BLENDS.get(player.getId());
        trail(painter, state, (ago, from) -> {
            SwordPoses.Pose at = earlier(blend, state, now, ago);
            SwordPoses.Pose seen = at.turned(at.orbit());
            return seen.hand().add(seen.blade().scale(from * SwordPoses.OWN_SWORD));
        });
    }

    static void arm(PoseStack stack, MultiBufferSource buffers, int light, LocalPlayer player,
            PlayerRenderer renderer, float side, Vec3 hand, Vector3f from, float rest) {
        Vec3 back = new Vec3(from.x - hand.x, from.y - hand.y, from.z - hand.z);
        if (back.lengthSqr() < 1.0E-6) {
            return;
        }
        back = back.normalize();
        float length = Mth.lerp(rest, (float) Mth.clamp((outOfSight(hand, back) + OWN_ARM_PAST) / ARM_BACK,
                OWN_ARM_SHORTEST, OWN_ARM_LONGEST), 1.0F);
        float thick = Mth.lerp(rest, OWN_ARM, 1.0F);
        Quaternionf along = new Quaternionf().rotationTo(new Vector3f(0.0F, 0.0F, 1.0F), vector(back));
        stack.pushPose();
        stack.translate(hand.x, hand.y, hand.z);
        stack.mulPose(along);
        stack.scale(thick, thick, length);
        stack.mulPose(new Quaternionf(along).conjugate());
        stack.translate(-hand.x, -hand.y, -hand.z);
        ownArms = true;
        try {
            RechargeAnimation.arm(stack, buffers, light, player, renderer, side, vector(hand), from);
        } finally {
            ownArms = false;
        }
        stack.popPose();
    }

    private static double outOfSight(Vec3 at, Vec3 way) {
        Minecraft minecraft = Minecraft.getInstance();
        double up = Math.tan(HAND_FOV * 0.5 * Mth.DEG_TO_RAD);
        double across = up * minecraft.getWindow().getWidth() / Math.max(1, minecraft.getWindow().getHeight());
        double out = past(-at.y + at.z * up, -way.y + way.z * up);
        out = Math.min(out, past(at.y + at.z * up, way.y + way.z * up));
        out = Math.min(out, past(at.x + at.z * across, way.x + way.z * across));
        out = Math.min(out, past(-at.x + at.z * across, -way.x + way.z * across));
        out = Math.min(out, past(at.z, way.z));
        return out == Double.MAX_VALUE ? 0.0 : out;
    }

    private static double past(double a, double b) {
        if (a >= 0.0) {
            return 0.0;
        }
        return b > 0.0 ? -a / b : Double.MAX_VALUE;
    }

    static Vector3f shoulder(Vec3 rest, Vec3 moved, float orbit) {
        return vector(SwordPoses.spin(rest.add(moved.scale(OWN_FOLLOW)), orbit));
    }

    private static Vector3f vector(Vec3 at) {
        return new Vector3f((float) at.x, (float) at.y, (float) at.z);
    }
}
