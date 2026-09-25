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
import nl.tivek.multiversepowers.character.greenlantern.ability.FlameMove;
import nl.tivek.multiversepowers.character.greenlantern.ability.Flamethrower;
import nl.tivek.multiversepowers.character.greenlantern.client.render.FlamePainter;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.config.client.ClientSettings;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.math.Ease;
import org.joml.Matrix4f;
import org.joml.Vector3f;

abstract class FlameFirstPerson extends FlameSeen {
    static final float KICK_TICKS = 5.0F;
    private static final float KICK_ROLL = 1.6F;
    private static final float KICK_DIP = 0.9F;
    private static final float SWEEP_LOOK = 0.12F;
    private static final float TREMBLE = 0.14F;
    private static final double AHEAD = 0.6;

    static final ScreenSpot NOZZLE = new ScreenSpot();
    static final ScreenSpot FURTHER = new ScreenSpot();

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
        FlameCurves.Pose pose = pose(player, state, partialTick);
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
        Vector3f rightShoulder = SwordFirstPerson.shoulder(SwordFirstPerson.OWN_SHOULDER_RIGHT,
                pose.grip().subtract(FlameKeys.GUARD.grip()), 0.0F).lerp(RechargeAnimation.SHOULDER_RIGHT, rest);
        boolean handsTaken = state.broken() >= 0.0F && SwordArms.holding();
        if (!handsTaken) {
            SwordFirstPerson.arm(stack, buffers, event.getPackedLight(), player, renderer, 1.0F, pose.grip(),
                    rightShoulder, rest);
        }
        if (rest < 1.0F && !handsTaken) {
            Vec3 left = pose.leftHand(FlameKeys.VIEW_SWEEP, FlameCurves.OWN_GUN);
            Vec3 guard = FlameKeys.GUARD.leftHand(FlameKeys.VIEW_SWEEP, FlameCurves.OWN_GUN);
            SwordFirstPerson.arm(stack, buffers, event.getPackedLight(), player, renderer, -1.0F, left,
                    SwordFirstPerson.shoulder(SwordFirstPerson.OWN_SHOULDER_LEFT, left.subtract(guard), 0.0F), 0.0F);
        }
        LanternPainter painter = LanternPainter.hand(stack, player.tickCount + partialTick);
        float now = now(partialTick);
        ConstructPainter.Frame old = shards;
        if (old != null) {
            double apart = Math.max(1.0E-3, (now - shardsSince) / Flamethrower.BREAK_TICKS);
            FlamePainter.gun(painter, old, FlamePainter.GROWN_BY + 10.0, FlamePainter.Glow.READY, apart, null);
        }
        double apart = apart(state);
        if (apart < 1.0) {
            ConstructPainter.Frame gun = pose.gun(FlameKeys.VIEW_SWEEP, FlameCurves.OWN_GUN);
            FlamePainter.gun(painter, gun, formed(state), FlamePoses.glow(state.move(), state.t(), heat(player),
                    state.firing()), apart, pose.grip().add(0.0, 0.03, -0.02));
            if (apart <= 0.0) {
                drawn = gun;
                Vec3 nozzle = FlamePainter.NOZZLE;
                NOZZLE.onHand(stack, gun.at(nozzle.x, nozzle.y, nozzle.z));
                FURTHER.onHand(stack, gun.at(nozzle.x, nozzle.y, nozzle.z + AHEAD));
            }
        }
        painter.finish(minecraft.renderBuffers().bufferSource());
        stack.popPose();
    }

    public static void drawOwn(LanternPainter painter, LocalPlayer player, Camera camera, Matrix4f projection,
            Matrix4f modelView, float partialTick) {
        State state = state(player, partialTick);
        if (state == null || apart(state) > 0.0) {
            return;
        }
        Vec3 nozzle = NOZZLE.world(camera, projection, modelView);
        Vec3 further = FURTHER.world(camera, projection, modelView);
        if (nozzle != null && further != null && further.distanceToSqr(nozzle) > 1.0E-6) {
            feed(player, state, nozzle, further.subtract(nozzle).normalize(), partialTick);
        }
        around(painter, player, state, partialTick, true);
    }

    @Nullable
    static float[] ownLook(LocalPlayer player, float partialTick) {
        State state = state(player, partialTick);
        float feel = Math.min(1.0F, ClientSettings.cameraShake());
        if (state == null || feel <= 0.0F) {
            return null;
        }
        float now = now(partialTick);
        float[] look = new float[2];
        float[] watching = watching(BLENDS.get(player.getId()), state, now);
        if (watching != null) {
            float t = watching[0];
            FlameCurves.Pose equip = state.move() == FlameMove.EQUIP ? pose(player, state, partialTick)
                    : FlamePoses.at(FlameMove.EQUIP, t, time(partialTick), FlameKeys.REST, null);
            float[] glance = FlamePoses.look(t, equip);
            look[0] += glance[0] * watching[1];
            look[1] += glance[1] * watching[1];
        }
        if (state.move().kind() == FlameMove.Kind.SWEEP) {
            look[1] += pose(player, state, partialTick).sweep() * SWEEP_LOOK;
        }
        float amount = feel * (float) Ease.smooth(shown);
        return new float[] { look[0] * amount, look[1] * amount };
    }

    static void kick(float at, float hard, float roll) {
        kickAt = at;
        kickRoll = roll;
        kickHard = hard;
    }

    static void feel(Own mine, float now) {
        int t = (int) Math.floor(now - mine.start);
        if (t == mine.felt) {
            return;
        }
        mine.felt = t;
        float at = mine.start + t;
        switch (mine.move.kind()) {
            case SWEEP -> {
                if (t == FlameMove.SPRAY_FROM) {
                    kick(at, 0.5F, mine.move == FlameMove.SWEEP ? -1.0F : 1.0F);
                }
            }
            case INFERNO -> {
                if (t == FlameMove.BRACE) {
                    kick(at, 0.8F, 1.0F);
                }
            }
            case WALL -> {
                if (t == FlameMove.LAY_TO + 1) {
                    kick(at, 1.2F, 1.0F);
                }
            }
            case BURST -> {
                if (t == FlameMove.BLAST) {
                    kick(at, 1.4F, -1.0F);
                }
            }
            default -> {
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
        if (state != null && (state.firing() && state.t() >= FlameMove.BRACE || state.swirling())) {
            float time = player.tickCount + partialTick;
            float strength = TREMBLE * feel * (state.firing() ? 1.0F : 0.5F);
            roll += strength * Mth.sin(time * 3.7F);
            dip += strength * 0.6F * Mth.sin(time * 5.3F + 1.1F);
        }
        return new float[] { roll, dip };
    }
}
