package nl.tivek.multiversepowers.character.greenlantern.client.body;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import nl.tivek.multiversepowers.character.greenlantern.ability.EnergyWhip;
import nl.tivek.multiversepowers.character.greenlantern.ability.WhipMove;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.character.greenlantern.client.render.WhipPainter;
import nl.tivek.multiversepowers.config.client.ClientSettings;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.math.Ease;
import org.joml.Matrix4f;
import org.joml.Vector3f;

// In first person the handle is drawn with the hands, but the lash in the world: it reaches far out, round and
// behind creatures, so it must meet the world's own depth. The two are joined where the handle shows on screen.
abstract class WhipFirstPerson extends WhipSeen {
    static final float KICK_TICKS = 5.0F;
    private static final float KICK_ROLL = 1.4F;
    private static final float KICK_DIP = 0.8F;
    private static final float TREMBLE = 0.1F;
    private static final double FURTHER = 0.3;
    private static final double STUB = 0.16;

    // In first person every move of the hands is kept smaller and further off, so an arm never fills the view; the
    // lash, drawn in the world, still moves in full.
    private static final Vec3 OWN_GUARD = new Vec3(0.44, -0.54, -0.98);
    private static final Vec3 OWN_LEFT = new Vec3(-0.36, -0.95, -0.8);
    private static final double OWN_MOVES = 0.55;

    static final ScreenSpot ROOT = new ScreenSpot();
    static final ScreenSpot ALONG = new ScreenSpot();

    @Nullable
    static ConstructPainter.Frame drawn;
    @Nullable
    static ConstructPainter.Frame shards;
    static float shardsSince;
    static float kickAt = -100.0F;
    static float kickRoll;
    static float kickHard;
    static float shown;
    static float shownAt = Float.NaN;

    static void drawHands(RenderHandEvent event, LocalPlayer player) {
        Minecraft minecraft = Minecraft.getInstance();
        float partialTick = event.getPartialTick();
        State state = state(player, partialTick);
        if (state == null) {
            return;
        }
        WhipCurves.Pose made = pose(player, state, partialTick);
        WhipCurves.Pose pose = own(made).orbited();
        Vec3 moved = own(made).grip().subtract(OWN_GUARD);
        float orbit = made.orbit();
        float rest = Mth.clamp(pose.rest(), 0.0F, 1.0F);
        PoseStack stack = event.getPoseStack();
        stack.pushPose();
        float[] look = ownLook(player, partialTick);
        if (look != null) {
            stack.mulPose(Axis.XP.rotation(-SwordFirstPerson.lookUp(player.getViewXRot(partialTick), look[0])));
            stack.mulPose(Axis.YP.rotation(look[1]));
        }
        MultiBufferSource buffers = event.getMultiBufferSource();
        PlayerRenderer renderer = (PlayerRenderer) minecraft.getEntityRenderDispatcher().getRenderer(player);
        Vector3f rightShoulder = SwordFirstPerson.shoulder(SwordFirstPerson.OWN_SHOULDER_RIGHT, moved, orbit)
                .lerp(RechargeAnimation.SHOULDER_RIGHT, rest);
        boolean handsTaken = state.broken() >= 0.0F && (SwordArms.holding() || FlameArms.holding());
        if (!handsTaken) {
            SwordFirstPerson.arm(stack, buffers, event.getPackedLight(), player, renderer, 1.0F, pose.grip(),
                    rightShoulder, rest);
            if (rest < 1.0F && pose.leftOn() > 0.01F) {
                Vec3 left = WhipKeys.REST.left().lerp(pose.left(), Ease.smooth(pose.leftOn()));
                Vec3 shifted = own(made).left().subtract(OWN_LEFT).scale(pose.leftOn());
                SwordFirstPerson.arm(stack, buffers, event.getPackedLight(), player, renderer, -1.0F, left,
                        SwordFirstPerson.shoulder(SwordFirstPerson.OWN_SHOULDER_LEFT, shifted, orbit), 0.0F);
            }
        }
        LanternPainter painter = LanternPainter.hand(stack, player.tickCount + partialTick);
        float now = now(partialTick);
        ConstructPainter.Frame old = shards;
        if (old != null) {
            WhipPainter.handle(painter, old, 1.0, Math.max(1.0E-3, (now - shardsSince) / EnergyWhip.BREAK_TICKS));
        }
        double apart = apart(state);
        if (apart < 1.0) {
            ConstructPainter.Frame handle = WhipPainter.held(pose.grip(), pose.handle(), pose.top(),
                    WhipCurves.OWN_WHIP);
            WhipPainter.handle(painter, handle, grown(state), apart);
            Vec3 tip = handle.at(0.0, 0.0, WhipPainter.TIP);
            ROOT.onHand(stack, tip);
            ALONG.onHand(stack, handle.at(0.0, 0.0, WhipPainter.TIP + FURTHER));
            if (apart <= 0.0) {
                drawn = handle;
                if (lashOut(state)) {
                    WhipPainter.stub(painter, tip, tip.add(pose.handle().scale(STUB * WhipCurves.OWN_WHIP)),
                            pose.top(), WhipCurves.OWN_WHIP);
                }
            }
        }
        painter.finish(minecraft.renderBuffers().bufferSource());
        stack.popPose();
    }

    private static WhipCurves.Pose own(WhipCurves.Pose pose) {
        if (pose.rest() >= 1.0F) {
            return pose;
        }
        Vec3 grip = OWN_GUARD.add(pose.grip().subtract(WhipKeys.GUARD.grip()).scale(OWN_MOVES));
        Vec3 left = OWN_LEFT.add(pose.left().subtract(WhipKeys.GUARD.left()).scale(OWN_MOVES));
        Vec3 held = new Vec3(Mth.clamp(grip.x, 0.05, 0.7), Mth.clamp(grip.y, -0.78, -0.16), Math.min(grip.z, -0.8));
        float ours = 1.0F - Mth.clamp(pose.rest(), 0.0F, 1.0F);
        return pose.gripping(pose.grip().lerp(held, ours), pose.left().lerp(left, ours));
    }

    private static boolean lashOut(State state) {
        return state.move() != WhipMove.EQUIP || state.t() > WhipMove.FORMED + 1.0F;
    }

    public static void drawOwn(LanternPainter painter, LocalPlayer player, Camera camera, Matrix4f projection,
            Matrix4f modelView, float partialTick) {
        State state = state(player, partialTick);
        if (state == null || apart(state) >= 1.0) {
            return;
        }
        Vec3 root = ROOT.world(camera, projection, modelView);
        Vec3 along = ALONG.world(camera, projection, modelView);
        if (root == null || along == null || along.distanceToSqr(root) < 1.0E-8) {
            return;
        }
        lash(painter, player, state, root, along.subtract(root), WhipCurves.WHIP_SCALE, partialTick, true,
                apart(state));
    }

    @Nullable
    static float[] ownLook(LocalPlayer player, float partialTick) {
        State state = state(player, partialTick);
        float feel = Math.min(1.0F, ClientSettings.cameraShake());
        if (state == null || feel <= 0.0F) {
            return null;
        }
        float[] watching = watching(BLENDS.get(player.getId()), state, now(partialTick));
        if (watching == null) {
            return null;
        }
        float t = watching[0];
        WhipCurves.Pose equip = state.move() == WhipMove.EQUIP ? pose(player, state, partialTick)
                : WhipPoses.at(WhipMove.EQUIP, t, time(partialTick), WhipKeys.REST, null, 0.0);
        float[] glance = WhipPoses.look(t, equip);
        float amount = watching[1] * feel * (float) Ease.smooth(shown);
        return new float[] { glance[0] * amount, glance[1] * amount };
    }

    static void kick(float at, float hard, float roll) {
        kickAt = at;
        kickRoll = roll;
        kickHard = hard;
    }

    // A crack, the lasso biting and the creature smacking down give the view a small jolt.
    static void feel(Own mine, float now) {
        int t = (int) Math.floor(now - mine.start);
        if (t == mine.felt) {
            return;
        }
        mine.felt = t;
        for (WhipMove.Crack crack : mine.move.cracks()) {
            if (t == (int) Math.ceil(crack.tick())) {
                kick(mine.start + crack.tick(), 0.3F + 0.6F * crack.strength(), t % 2 == 0 ? 1.0F : -1.0F);
            }
        }
        if (mine.move == WhipMove.LASSO) {
            if (t == WhipMove.LASSO_HAUL + 1) {
                kick(mine.start + t, 0.7F, -1.0F);
            } else if (t == WhipMove.LASSO_LAND) {
                kick(mine.start + t, 1.1F, 1.0F);
            }
        }
    }

    static float[] shake(LocalPlayer player, float partialTick) {
        float now = now(partialTick);
        float feel = ClientSettings.cameraShake() * (float) Ease.smooth(shown);
        float roll = 0.0F;
        float dip = 0.0F;
        float since = now - kickAt;
        if (since >= 0.0F && since < KICK_TICKS) {
            float fade = 1.0F - since / KICK_TICKS;
            float kick = kickHard * fade * fade * feel;
            roll += KICK_ROLL * kickRoll * kick;
            dip += KICK_DIP * kick;
        }
        State state = state(player, partialTick);
        if (state != null && (state.whirling() || state.spinning())) {
            float time = player.tickCount + partialTick;
            float strength = TREMBLE * feel * (state.whirling() ? 1.0F : 0.6F);
            roll += strength * Mth.sin(time * 3.3F);
            dip += strength * 0.5F * Mth.sin(time * 4.9F + 1.1F);
        }
        return new float[] { roll, dip };
    }
}
