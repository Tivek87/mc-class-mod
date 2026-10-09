package nl.tivek.multiversepowers.character.thor.client.blow;

import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.client.ClientCharacter;
import nl.tivek.multiversepowers.character.thor.ThorBlow;
import nl.tivek.multiversepowers.character.thor.ThorStatePayload;
import nl.tivek.multiversepowers.character.thor.client.ClientThor;
import nl.tivek.multiversepowers.character.thor.client.motion.ThorMotion;
import nl.tivek.multiversepowers.character.thor.client.motion.ThorPull;
import nl.tivek.multiversepowers.character.thor.client.pose.ThorHammerLayer;
import nl.tivek.multiversepowers.character.thor.hammer.ThrownHammer;
import nl.tivek.multiversepowers.engine.client.render.entity.FirstPersonArm;
import nl.tivek.multiversepowers.engine.client.render.entity.FirstPersonLeg;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Spring;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// Your own Thor's blows seen from his eyes: both fists up in his guard and thrown where each blow goes, and the leg of
// a kick coming up into view; with the hammer in hand, his right fist holds it, low and out to the right, so it never
// hides what he hits. With it out of his hands: drawn back over his shoulder to throw, an arm stretched to it as he
// calls it or is pulled to it, and the catch knocking his hand back. View space: x right, y up, -z ahead.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class ThorFists {
    private static final Vector3f REST_FROM = new Vector3f(0.75F, -1.1F, -0.15F);
    private static final Vector3f HIP = new Vector3f(0.14F, -1.05F, 0.05F);
    // With the hammer in hand his fist swings this much of a bare fist's way and keeps at least this far right, low
    // and out.
    private static final float ARMED_SWING = 0.45F;
    private static final Vector3f CLEAR = new Vector3f(0.34F, -0.12F, -0.72F);
    // The left hand out ahead, aiming, while the right draws the hammer back.
    private static final Vector3f AIM = new Vector3f(-0.3F, -0.24F, -0.75F);
    private static final Vector3f AIM_FROM = new Vector3f(-0.6F, -0.8F, 0.15F);
    // An arm stretched out to the hammer, along his look to it: where it starts, and how far the hand is from there.
    private static final Vector3f SHOULDER = new Vector3f(0.2F, -0.25F, 0.0F);
    private static final float ARM = 0.9F;
    private static final float CATCH_TICKS = 8.0F;
    private static final Spring DRAW = new Spring();
    private static final Spring REACH = new Spring();
    private static float time = Float.NaN;

    private ThorFists() {
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || player.isInvisible() || ClientCharacter.active() != GameCharacter.THOR
                || !player.getMainHandItem().isEmpty() || ThorMotion.flying()) {
            return;
        }
        float partialTick = event.getPartialTick();
        ClientThor.View view = ClientThor.view(player);
        if (event.getHand() == InteractionHand.MAIN_HAND) {
            weigh(player, view, partialTick);
        }
        ThorBlowPoses.Pose pose = view == null ? null : ThorBlowPoses.of(view, partialTick);
        boolean armed = view != null && view.has(ThorStatePayload.ARMED)
                && (!view.has(ThorStatePayload.THROWN) || ThorHammerLayer.windingUp(view, partialTick));
        boolean holding = holds(view);
        float w = pose == null ? 0.0F : pose.weight;
        float drawn = (float) DRAW.value;
        Vector3f way = toHammer(player, partialTick);
        float reach = way == null ? 0.0F : (float) REACH.value;
        float shove = caught(view, partialTick, ThorStatePayload.RIGHT_HAND);
        if (w < 1.0E-3F && !armed && !holding && drawn < 1.0E-3F && reach < 1.0E-3F && shove < 1.0E-3F) {
            return;
        }
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        // The hand he catches it in: the left when he will fly on with it.
        int reachSide = ThorMotion.awaiting() || pulledToFly(player, view) ? 1 : 0;
        PlayerRenderer renderer = (PlayerRenderer) minecraft.getEntityRenderDispatcher().getRenderer(player);
        for (int side = 0; side < 2; side++) {
            if (side == 1 && w < 1.0E-3F && drawn < 1.0E-3F && (reachSide == 0 || reach < 1.0E-3F)) {
                continue;
            }
            float sign = side == 0 ? 1.0F : -1.0F;
            Vector3f rest = side == 0 ? FirstPersonArm.HAND_RIGHT : FirstPersonArm.HAND_LEFT;
            Vector3f hand = new Vector3f(rest);
            Vector3f from = new Vector3f(REST_FROM.x * sign, REST_FROM.y, REST_FROM.z);
            if (side == 0 && holding) {
                hand.set(fist(view, pose, partialTick));
                from.set(ThorBlowPoses.heldFrom());
                if (pose != null && ThorBlow.grabbing(view.blow, view.blowAge(partialTick))) {
                    from.lerp(pose.from[0], w);
                }
            } else if (pose != null) {
                // The hammer's swings and its throws are drawn smaller than a fist's, so its head and the arm stay low
                // and to the right, a throw alike before and after it lets go.
                ThorBlow thrown = ThorBlow.byIndex(view.blow);
                boolean small = side == 0 && (armed || thrown == ThorBlow.HAMMER_THROW
                        || thrown == ThorBlow.STORM_TOSS);
                float swing = small ? w * ARMED_SWING : w;
                hand.lerp(pose.seen[side], swing);
                from.lerp(pose.from[side], swing);
                if (small) {
                    clear(hand, from, w);
                }
            }
            if (drawn > 1.0E-3F) {
                hand.lerp(side == 0 ? ThorBlowKeys.Spot.THROW_BACK.seen : AIM, drawn);
                from.lerp(side == 0 ? ThorBlowKeys.Spot.THROW_BACK.from : AIM_FROM, drawn);
            }
            if (side == reachSide && reach > 1.0E-3F) {
                Vector3f shoulder = new Vector3f(SHOULDER.x * sign, SHOULDER.y, SHOULDER.z);
                hand.lerp(new Vector3f(way).mul(ARM).add(shoulder), reach);
                from.lerp(new Vector3f(way).mul(-0.6F).add(shoulder), reach);
            }
            if (side == 0) {
                hand.add(0.03F * shove, -0.08F * shove, 0.05F * shove);
            }
            FirstPersonArm.arm(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), player,
                    renderer, sign, hand, from);
            if (side == 0 && armed) {
                hammer(event, player, hand, from);
            }
        }
        if (pose != null && pose.footSide >= 0 && pose.kick * pose.weight > 0.05F) {
            float sign = pose.footSide == 0 ? 1.0F : -1.0F;
            Vector3f hip = new Vector3f(HIP.x * sign, HIP.y, HIP.z);
            Vector3f down = new Vector3f(hip).add(0.0F, -0.75F, 0.0F);
            Vector3f foot = down.lerp(pose.footSeen, pose.kick * pose.weight);
            FirstPersonLeg.leg(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), player,
                    renderer, sign, hip, foot);
        }
    }

    // Your right fist while you hold a creature up by the throat, as it is drawn (view space): where the creature hangs
    // from in your own view; null while you hold none.
    @Nullable
    public static Vector3f holding(LocalPlayer player, float partialTick) {
        ClientThor.View view = ClientThor.view(player);
        return holds(view) ? fist(view, ThorBlowPoses.of(view, partialTick), partialTick) : null;
    }

    private static boolean holds(@Nullable ClientThor.View view) {
        return view != null && view.has(ThorStatePayload.CARRYING) && view.carried >= 0
                && view.move() != ThorStatePayload.HOIST && view.move() != ThorStatePayload.DIVE
                && !view.has(ThorStatePayload.FLYING);
    }

    // Out where he holds it, or where a grab's own ending takes it, eased in from there.
    private static Vector3f fist(ClientThor.View view, @Nullable ThorBlowPoses.Pose pose, float partialTick) {
        Vector3f fist = ThorBlowPoses.heldSeen();
        if (pose != null && ThorBlow.grabbing(view.blow, view.blowAge(partialTick))) {
            fist.lerp(pose.seen[0], pose.weight);
        }
        return fist;
    }

    // Mjolnir in his right fist, held as others see it held.
    private static void hammer(RenderHandEvent event, LocalPlayer player, Vector3f hand, Vector3f from) {
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        FirstPersonArm.toArm(pose, 1.0F, hand, from);
        ThorHammerLayer.inFist(pose, true, ThorHammerLayer.SEEN_TILT, ThorHammerLayer.SEEN_LEAN);
        pose.scale(ThorHammerLayer.SEEN_SIZE, ThorHammerLayer.SEEN_SIZE, ThorHammerLayer.SEEN_SIZE);
        ThorHammerLayer.draw(ThorHammerLayer.GRIP, ThorHammerLayer.glow(player, event.getPartialTick()), pose,
                event.getMultiBufferSource(), event.getPackedLight());
        pose.popPose();
    }

    // With the hammer in hand his blows and guard keep right of and below the crosshair and out at arm's length, so
    // neither his arm nor its head fills the view: the arm moved along, its way kept.
    private static void clear(Vector3f hand, Vector3f from, float w) {
        Vector3f shift = new Vector3f(Math.max(hand.x, CLEAR.x), Math.min(hand.y, CLEAR.y), Math.min(hand.z, CLEAR.z))
                .sub(hand).mul(w);
        hand.add(shift);
        from.add(shift);
    }

    // How far into the draw and the reach his arms are, eased so they swing into them and back out.
    private static void weigh(LocalPlayer player, @Nullable ClientThor.View view, float partialTick) {
        float now = player.tickCount + partialTick;
        float dt = Float.isNaN(time) ? 0.0F : Mth.clamp(now - time, 0.0F, 3.0F);
        time = now;
        boolean drawing = ThorMotion.drawn(partialTick) >= 0.0F;
        // Let go, the arm comes through fast: the throw's own keys take it from there.
        DRAW.step(drawing ? 1.0 : 0.0, dt, drawing ? 0.12 : 0.3, 1.0);
        boolean reaching = ThorMotion.awaiting() || view != null
                && (view.has(ThorStatePayload.CALLING) || view.has(ThorStatePayload.PULLING));
        REACH.step(reaching ? 1.0 : 0.0, dt, 0.15, 1.0);
    }

    // The way from your eyes to your hammer out of your hands (view space), kept ahead enough to reach out for, or
    // null while it is on you.
    @Nullable
    public static Vector3f toHammer(LocalPlayer player, float partialTick) {
        ThrownHammer hammer = ClientThor.hammer(player);
        if (hammer == null) {
            return null;
        }
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 to = hammer.getPosition(partialTick).subtract(camera.getPosition());
        if (to.lengthSqr() < 1.0E-4) {
            return null;
        }
        Vector3f way = new Vector3f((float) to.x, (float) to.y, (float) to.z).normalize()
                .rotate(new Quaternionf(camera.rotation()).conjugate());
        way.z = Math.min(way.z, -0.35F);
        return way.normalize();
    }

    // The catch knocking the hand back (0 to 1), in the right hand on the ground, the left in flight.
    public static float caught(@Nullable ClientThor.View view, float partialTick, int hand) {
        if (view == null || view.move() != ThorStatePayload.CATCH || view.arg() != hand) {
            return 0.0F;
        }
        float age = view.age(partialTick);
        return age >= CATCH_TICKS ? 0.0F
                : (float) (Ease.smooth(age / 1.5) * (1.0 - Ease.smooth((age - 1.5) / (CATCH_TICKS - 1.5))));
    }

    // Pulled to a hammer up in the air: he will catch it in his left hand and fly on.
    private static boolean pulledToFly(LocalPlayer player, @Nullable ClientThor.View view) {
        ThrownHammer hammer = ClientThor.hammer(player);
        return view != null && hammer != null && view.has(ThorStatePayload.PULLING) && !ThorPull.landsBy(hammer);
    }
}
