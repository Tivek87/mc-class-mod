package nl.tivek.welcomescreen.client.character.lantern;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import nl.tivek.welcomescreen.WelcomeScreenMod;
import nl.tivek.welcomescreen.character.lantern.LightFlare;
import org.joml.Vector3f;

/**
 * The Lantern Flare as everyone sees it (see {@link LightFlare}): he throws his ring fist up high and the ring shapes
 * his lantern over it, a construct like any other: it grows out of the ring's light, white-hot at first. Light runs
 * into it from all round, specks of it streaming in and its heart burning brighter and brighter, until it bursts like a
 * small sun, rays shooting out and a shell of light racing out as far as the flash reaches, and the lantern breaks into
 * solid pieces. Whoever looks at it is dazzled: their screen goes white-green a moment, fading, the more the straighter
 * they looked at it. The flash is light, the lantern a construct.
 */
@EventBusSubscriber(modid = WelcomeScreenMod.MODID, value = Dist.CLIENT)
public final class FlareLight {
    // How long the dazzle lasts on your screen, in ticks, and how much of it you get of your own flare (you hold it up
    // over your head, not in your eyes).
    private static final float DAZZLE_TICKS = 22.0F;
    private static final float OWN_DAZZLE = 0.35F;
    private static final int DAZZLE_COLOUR = 0xE8FFF0;
    // Your ring fist in first person, thrown up high: top right of your screen, the arm rising to it from below.
    static final Vector3f UP_HIGH = new Vector3f(0.42F, 0.24F, -0.78F);
    private static final Vector3f ARM_FROM = new Vector3f(0.75F, -1.1F, -0.15F);
    // Seen from outside: the arm up high, tipped a little forward and out to the side, so the fist stands clear of
    // the head.
    private static final float RAISED = -2.75F;
    private static final float RAISED_OUT = -0.4F;
    // The lantern: how big it is, how far over the fist it stands, where its heart is in its own height (at scale 1),
    // and how long it takes to take shape and to break up, in ticks.
    private static final double LANTERN_SCALE = 0.34;
    private static final double LANTERN_OVER = 0.12;
    private static final double LANTERN_HEART = 1.05;
    private static final double LANTERN_GROWS = 6.0;
    private static final double LANTERN_BREAKS = 7.0;
    // Your own flare in first person hangs this far out along the way to your raised fist, a little over it, and this
    // much bigger, so it shows over the fist at the top right of your screen.
    private static final double OWN_OUT = 2.2;
    private static final double OWN_OVER = 0.1;
    private static final double OWN_SCALE = 0.75;

    private FlareLight() {
    }

    /** A flare going: whose it is, how long ago its light began to gather, where it is, how far the flash reaches. */
    record Going(int owner, double clock, Vec3 center, double radius) {
    }

    /** Where the ring is while its owner holds it up for a flare: high over his head, a little to his right. */
    static Vec3 ring(Entity owner, float partialTick) {
        Vec3 look = owner.getViewVector(partialTick);
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
        Vec3 right = new Vec3(-flat.z, 0.0, flat.x);
        return owner.getEyePosition(partialTick).add(0.0, 0.75, 0.0).add(right.scale(0.3)).add(flat.scale(0.15));
    }

    /**
     * Your own flare in first person: over your fist, which is thrown up at the top right of your screen (where it
     * really is, over your head, you would not see it).
     */
    static Vec3 ownRing(Camera camera) {
        Vec3 forward = new Vec3(camera.getLookVector());
        Vec3 up = new Vec3(camera.getUpVector());
        Vec3 left = new Vec3(camera.getLeftVector());
        Vec3 way = forward.scale(-UP_HIGH.z()).subtract(left.scale(UP_HIGH.x())).add(up.scale(UP_HIGH.y()))
                .normalize();
        return camera.getPosition().add(way.scale(OWN_OUT)).add(up.scale(OWN_OVER));
    }

    /**
     * The flare itself: the lantern taking shape over the ring, the light gathering in it, and the burst.
     *
     * @param at     where the ring is
     * @param facing the way its maker faces
     * @param own    true for your own flare in first person (see {@link #ownRing})
     */
    static void draw(ConstructPainter painter, Vec3 at, Vec3 facing, double clock, boolean own) {
        double gather = LightFlare.GATHER_TICKS;
        double scale = own ? OWN_SCALE : 1.0;
        Vec3 forward = new Vec3(facing.x, 0.0, facing.z);
        forward = forward.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : forward.normalize();
        Vec3 right = forward.cross(ConstructPainter.UP).normalize();
        double size = LANTERN_SCALE * scale * SlamPainter.backOut(clock / LANTERN_GROWS);
        Vec3 foot = at.add(0.0, LANTERN_OVER * scale, 0.0);
        Vec3 heart = foot.add(0.0, LANTERN_HEART * LANTERN_SCALE * scale, 0.0);
        double since = clock - gather;
        double apart = Mth.clamp(since / LANTERN_BREAKS, 0.0, 1.0);
        if (size > 0.01 && apart < 1.0) {
            ConstructPainter.Frame frame = new ConstructPainter.Frame(foot, right, ConstructPainter.UP, forward, size);
            // Fresh out of the ring it is white-hot and cools to green as it takes shape; it flares up as it bursts.
            painter.glare(since >= 0.0 ? 1.0 - 0.5 * apart
                    : 0.6 * (1.0 - ConstructPainter.smooth(clock / LANTERN_GROWS)));
            SlamDrops.lantern(painter, frame, apart, since >= 0.0 ? 1.5 : 1.0 + 0.5 * clock / gather);
            painter.glare(0.0);
            if (since < 0.0) {
                // The ring feeds it.
                painter.beam(at, foot, 1.0, 0.6 * scale);
            }
        }
        if (clock < gather) {
            double t = clock / gather;
            painter.flare(heart, (0.15 + 0.6 * t * t) * scale, 0.6 + 0.4 * t);
            // Specks of light running in from all round.
            for (int k = 0; k < 14; k++) {
                double cycle = Mth.frac(clock / 5.0 + ConstructPainter.noise(k, 81, 0));
                Vec3 way = ConstructPainter.direction(k, 81 + (int) (clock / 5.0));
                double far = 2.4 * (1.0 - cycle) * (1.0 - cycle) * scale;
                Vec3 head = heart.add(way.scale(far));
                Vec3 tail = heart.add(way.scale(far + 0.35 * (1.0 - cycle) * scale));
                painter.edge(tail, head, 0.05 * scale, 0.3 + 0.7 * cycle);
            }
            return;
        }
        if (since > LightFlare.BURST_TICKS) {
            return;
        }
        at = heart;
        double fade = 1.0 - since / LightFlare.BURST_TICKS;
        painter.flare(at, 0.6 + 3.5 * fade * fade, fade);
        // Rays shooting out, and a shell of light racing out as far as the flash reaches.
        for (int k = 0; k < 16; k++) {
            Vec3 way = ConstructPainter.direction(k, 83);
            double reach = (1.5 + 5.0 * ConstructPainter.noise(k, 83, 5)) * (0.4 + 0.6 * Math.min(1.0, since / 3.0));
            painter.edge(at.add(way.scale(0.3)), at.add(way.scale(reach)), 0.12 * fade, fade);
        }
        double out = 1.0 - Math.pow(1.0 - Math.min(1.0, since / 6.0), 3.0);
        double radius = Math.max(0.3, 7.0 * out);
        Vec3 east = new Vec3(1.0, 0.0, 0.0);
        Vec3 south = new Vec3(0.0, 0.0, 1.0);
        Vec3 up = ConstructPainter.UP;
        painter.circle(at, east, south, radius, 0.08, 0.6, ConstructPainter.alpha(fade),
                ConstructPainter.alpha(0.5 * fade));
        painter.circle(at, east, up, radius, 0.06, 0.4, ConstructPainter.alpha(0.7 * fade),
                ConstructPainter.alpha(0.35 * fade));
        painter.circle(at, south, up, radius, 0.06, 0.4, ConstructPainter.alpha(0.7 * fade),
                ConstructPainter.alpha(0.35 * fade));
    }

    /** How far this player's ring fist is thrown up high, 0 to 1: for a flare, or to call a storm. */
    static float up(Entity player, float partialTick) {
        return Math.max(raised(player, partialTick), StormLight.raised(player, partialTick));
    }

    /** How far this player's ring fist is up for a flare, 0 to 1: straight up as it starts, down again after. */
    static float raised(Entity player, float partialTick) {
        Going flare = ClientConstructs.flare(player.getId(), partialTick);
        if (flare == null) {
            return 0.0F;
        }
        double end = LightFlare.GATHER_TICKS + LightFlare.BURST_TICKS;
        return (float) (ConstructPainter.smooth(flare.clock() / 2.5) * (1.0 - ConstructPainter.smooth((flare.clock()
                - end + 5.0) / 5.0)));
    }

    /** Seen from outside: the ring arm thrown straight up, the fist high over his head. */
    static void pose(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        if (arm != HumanoidArm.RIGHT) {
            return;
        }
        float up = up(entity, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
        if (up <= 0.0F) {
            return;
        }
        model.rightArm.xRot = Mth.lerp(up, model.rightArm.xRot, RAISED);
        model.rightArm.yRot = Mth.lerp(up, model.rightArm.yRot, 0.0F);
        model.rightArm.zRot = Mth.lerp(up, model.rightArm.zRot, RAISED_OUT);
    }

    /**
     * Your own ring fist in first person, thrown up high for a flare or a storm: before anything else would draw your
     * hand.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || player.isInvisible() || event.getHand() != InteractionHand.MAIN_HAND
                || !player.getMainHandItem().isEmpty()) {
            return;
        }
        float up = up(player, event.getPartialTick());
        if (up <= 0.0F) {
            return;
        }
        event.setCanceled(true);
        PoseStack pose = event.getPoseStack();
        MultiBufferSource buffers = event.getMultiBufferSource();
        PlayerRenderer renderer = (PlayerRenderer) minecraft.getEntityRenderDispatcher().getRenderer(player);
        RechargeAnimation.arm(pose, buffers, event.getPackedLight(), player, renderer, 1.0F,
                new Vector3f(RechargeAnimation.HAND_RIGHT).lerp(UP_HIGH, up), ARM_FROM);
    }

    /** Dazzled: a flare that burst where you could see it turns your screen white-green a moment. */
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float dazzle = 0.0F;
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 look = player.getViewVector(partialTick);
        for (Going flare : ClientConstructs.flares(partialTick)) {
            double since = flare.clock() - LightFlare.GATHER_TICKS;
            if (since < 0.0 || since > DAZZLE_TICKS) {
                continue;
            }
            Vec3 to = flare.center().subtract(eye);
            double distance = to.length();
            if (distance > flare.radius() * 1.6) {
                continue;
            }
            double facing = distance < 1.2 ? 1.0
                    : Mth.clamp((look.dot(to.scale(1.0 / distance)) - 0.1) / 0.7, 0.0, 1.0);
            boolean own = flare.owner() == player.getId();
            if (!own && distance > 1.2 && minecraft.level.clip(new ClipContext(eye, flare.center(),
                    ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player)).getType() != HitResult.Type.MISS) {
                continue;
            }
            float fade = (float) (1.0 - since / DAZZLE_TICKS);
            float near = (float) Mth.clamp(1.2 - distance / (flare.radius() * 1.6), 0.3, 1.0);
            dazzle = Math.max(dazzle, fade * fade * near * (own ? OWN_DAZZLE : (float) facing));
        }
        if (dazzle <= 0.01F) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(),
                (int) (230 * Mth.clamp(dazzle, 0.0F, 1.0F)) << 24 | DAZZLE_COLOUR);
    }
}
