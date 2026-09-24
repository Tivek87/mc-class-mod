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
 * his lantern over it, a construct of its own like any other: a stepped foot, a barrel of light in a cage of bars with
 * a band round its heart, a cap with a ring to carry it by and a handle on either side. It grows out of the ring's
 * light, white-hot at first, and turns slowly. Light runs into it from all round, specks of it streaming in, rings of
 * light closing in on it and its heart burning brighter and brighter, until it bursts like a small sun: a flash with a
 * cross of light through it, rays shooting out, a shell of light racing out as far as the flash reaches, a ring of it
 * running out over the ground, and the lantern breaks into solid pieces. Then specks of its light drift down and fade.
 * Whoever looks at it is dazzled: their screen goes white-green a moment, fading, the more the straighter they looked
 * at it. The flash is light, the lantern a construct.
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
    private static final double LANTERN_BREAKS = 9.0;
    // How long the flash itself lasts, and the specks of light drifting down after it, in ticks.
    private static final double FLASH_TICKS = 12.0;
    private static final int MOTES = 26;
    /**
     * The flare's own lantern, standing on its foot at y = 0, 2.3 blocks high at scale 1 with its heart at
     * {@link #LANTERN_HEART}: a stepped foot, a barrel of light in a cage of six bars with a band round its heart, a
     * cap, a ring on top to carry it by and a handle on either side.
     */
    private static final ConstructPainter.Shape LANTERN = ConstructPainter.Shape.of(lantern());
    // Your own flare in first person hangs this far out along the way to your raised fist, a little over it, and this
    // much bigger, so it shows over the fist at the top right of your screen.
    private static final double OWN_OUT = 2.2;
    private static final double OWN_OVER = 0.1;
    private static final double OWN_SCALE = 0.75;

    private FlareLight() {
    }

    private static Mesh[] lantern() {
        Mesh foot = Mesh.lathe(24, 1.0, 0.0, 0.0, 0.64, 0.0, 0.64, 0.12, 0.52, 0.2, 0.42, 0.3, 0.48, 0.37, 0.0, 0.37);
        Mesh glass = Mesh.lathe(24, 1.55, 0.0, 0.37, 0.5, 0.41, 0.58, 0.72, 0.6, 1.05, 0.58, 1.38, 0.5, 1.69, 0.0,
                1.73);
        Mesh band = Mesh.torus(28, 6, 0.62, 0.055, 1.7).moved(0.0, LANTERN_HEART, 0.0);
        Mesh cap = Mesh.lathe(24, 1.05, 0.0, 1.71, 0.56, 1.71, 0.56, 1.78, 0.42, 1.88, 0.22, 1.96, 0.0, 1.98);
        Mesh ring = Mesh.torus(18, 6, 0.24, 0.055, 1.2).alongZ().moved(0.0, 2.25, 0.0);
        Mesh neck = Mesh.cylinder(8, 0.06, 1.96, 2.02, 1.1);
        Mesh[] parts = new Mesh[12];
        parts[0] = foot;
        parts[1] = glass;
        parts[2] = band;
        parts[3] = cap;
        parts[4] = ring;
        parts[5] = neck;
        for (int k = 0; k < 4; k++) {
            double angle = Math.PI * 2.0 * k / 6.0 + Math.PI / 6.0;
            parts[6 + k] = Mesh.box(-0.045, 0.38, -0.045, 0.045, 1.72, 0.045, 1.2).moved(0.61 * Math.cos(angle), 0.0,
                    0.61 * Math.sin(angle));
        }
        // The two bars at the sides carry the handles instead.
        for (int side = 0; side < 2; side++) {
            double x = side == 0 ? 1.0 : -1.0;
            parts[10 + side] = Mesh.merged(Mesh.box(-0.045, 0.38, -0.045, 0.045, 1.72, 0.045, 1.2).moved(0.61 * x, 0.0,
                    0.0), Mesh.tube(false, 6, 0.05, 1.1, new Vec3(0.6 * x, 0.62, 0.0), new Vec3(0.86 * x, 0.78, 0.0),
                            new Vec3(0.94 * x, 1.05, 0.0), new Vec3(0.86 * x, 1.32, 0.0),
                            new Vec3(0.6 * x, 1.48, 0.0)));
        }
        return parts;
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
        Vec3 flat = new Vec3(facing.x, 0.0, facing.z);
        flat = flat.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
        // It turns slowly as the light gathers, faster and faster.
        Vec3 forward = ConstructPainter.spin(flat, ConstructPainter.UP, 0.04 * clock * clock / gather);
        double size = LANTERN_SCALE * scale * ConstructPainter.backOut(clock / LANTERN_GROWS);
        Vec3 foot = at.add(0.0, LANTERN_OVER * scale, 0.0);
        Vec3 heart = foot.add(0.0, LANTERN_HEART * LANTERN_SCALE * scale, 0.0);
        double since = clock - gather;
        double apart = Mth.clamp(since / LANTERN_BREAKS, 0.0, 1.0);
        if (size > 0.01 && apart < 1.0) {
            ConstructPainter.Frame frame = ConstructPainter.Frame.of(foot, forward, ConstructPainter.UP, size);
            if (since < 0.0) {
                // Fresh out of the ring it is white-hot and cools to green as it takes shape, then burns hotter and
                // hotter as the light fills it.
                double fill = clock / gather;
                double fresh = 0.6 * (1.0 - ConstructPainter.smooth(clock / LANTERN_GROWS));
                painter.glare(Math.max(fresh, 0.5 * fill * fill));
                painter.shape(LANTERN, frame, 1.0, 1.0 + 0.5 * fill);
                painter.glare(0.0);
                // The ring feeds it.
                painter.beam(at, foot, 1.0, 0.6 * scale);
            } else {
                painter.glare(1.0 - 0.6 * apart);
                painter.shattered(LANTERN, frame, apart, 1.5);
                painter.glare(0.0);
            }
        }
        if (clock < gather) {
            gathering(painter, heart, clock / gather, scale);
            return;
        }
        burst(painter, heart, since, scale);
    }

    /** The light gathering in the lantern: specks streaming in, rings closing in on it, its heart swelling. */
    private static void gathering(ConstructPainter painter, Vec3 heart, double t, double scale) {
        painter.flare(heart, (0.15 + 0.7 * t * t) * scale, 0.6 + 0.4 * t);
        double clock = t * LightFlare.GATHER_TICKS;
        for (int k = 0; k < 18; k++) {
            double cycle = Mth.frac(clock / (5.0 - 2.0 * t) + ConstructPainter.noise(k, 81, 0));
            Vec3 way = ConstructPainter.direction(k, 81 + (int) (clock / 4.0));
            double far = 2.6 * (1.0 - cycle) * (1.0 - cycle) * scale;
            Vec3 head = heart.add(way.scale(far));
            Vec3 tail = heart.add(way.scale(far + 0.35 * (1.0 - cycle) * scale));
            painter.edge(tail, head, 0.05 * scale, 0.3 + 0.7 * cycle);
        }
        // Rings of light closing in on it, tipped every way, one after the other.
        for (int k = 0; k < 3; k++) {
            double close = Mth.frac(clock / 7.0 + k / 3.0);
            Vec3 tip = ConstructPainter.direction(k, 85);
            Vec3 a = tip.cross(ConstructPainter.UP).lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0)
                    : tip.cross(ConstructPainter.UP).normalize();
            Vec3 b = tip.cross(a).normalize();
            double radius = (2.2 * (1.0 - close) + 0.2) * scale;
            painter.circle(heart, a, b, radius, 0.03 * scale, 0.18 * scale,
                    ConstructPainter.alpha(0.8 * close * (0.4 + 0.6 * t)), ConstructPainter.alpha(0.35 * close));
        }
    }

    /**
     * The burst: a flash with a cross of light through it, rays shooting out, a shell of light racing out as far as the
     * flash reaches, a ring of it running out over the ground, and then specks of light drifting down and fading.
     */
    private static void burst(ConstructPainter painter, Vec3 at, double since, double scale) {
        if (since > LightFlare.BURST_TICKS) {
            return;
        }
        double fade = Math.max(0.0, 1.0 - since / FLASH_TICKS);
        Vec3 east = new Vec3(1.0, 0.0, 0.0);
        Vec3 south = new Vec3(0.0, 0.0, 1.0);
        Vec3 up = ConstructPainter.UP;
        if (fade > 0.0) {
            painter.flare(at, 0.6 + 4.2 * fade * fade, fade);
            // A cross of light through the flash, wide and flat, the way a sun glares in a lens.
            Vec3 toCamera = painter.camera().subtract(at);
            if (toCamera.lengthSqr() > 1.0E-6) {
                Vec3 across = toCamera.cross(up);
                across = across.lengthSqr() < 1.0E-6 ? east : across.normalize();
                double wide = (3.0 + 6.0 * (1.0 - fade)) * fade * scale;
                painter.edge(at.subtract(across.scale(wide)), at.add(across.scale(wide)), 0.14 * fade * scale, fade);
                painter.edge(at.subtract(0.0, wide * 0.5, 0.0), at.add(0.0, wide * 0.5, 0.0), 0.1 * fade * scale,
                        0.8 * fade);
            }
            // Rays shooting out.
            for (int k = 0; k < 20; k++) {
                Vec3 way = ConstructPainter.direction(k, 83);
                double reach = (1.5 + 6.0 * ConstructPainter.noise(k, 83, 5))
                        * (0.4 + 0.6 * Math.min(1.0, since / 3.0));
                painter.edge(at.add(way.scale(0.3)), at.add(way.scale(reach)), 0.12 * fade, fade);
            }
            // A shell of light racing out as far as the flash reaches.
            double out = 1.0 - Math.pow(1.0 - Math.min(1.0, since / 6.0), 3.0);
            double radius = Math.max(0.3, 8.0 * out);
            painter.circle(at, east, south, radius, 0.08, 0.6, ConstructPainter.alpha(fade),
                    ConstructPainter.alpha(0.5 * fade));
            painter.circle(at, east, up, radius, 0.06, 0.4, ConstructPainter.alpha(0.7 * fade),
                    ConstructPainter.alpha(0.35 * fade));
            painter.circle(at, south, up, radius, 0.06, 0.4, ConstructPainter.alpha(0.7 * fade),
                    ConstructPainter.alpha(0.35 * fade));
        }
        // A ring of light running out over the ground under him.
        double ground = Math.max(0.0, 1.0 - since / 18.0);
        if (ground > 0.0 && scale >= 1.0) {
            double run = 1.0 - Math.pow(1.0 - Math.min(1.0, since / 14.0), 2.0);
            Vec3 feet = at.subtract(0.0, 2.6, 0.0);
            painter.circle(feet, east, south, 0.5 + 12.0 * run, 0.12, 0.8, ConstructPainter.alpha(0.8 * ground),
                    ConstructPainter.alpha(0.4 * ground));
        }
        // Specks of its light drifting down and fading.
        double left = 1.0 - since / LightFlare.BURST_TICKS;
        for (int k = 0; k < MOTES; k++) {
            Vec3 way = ConstructPainter.direction(k, 87);
            double out = (1.0 - Math.exp(-since * 0.25)) * (2.0 + 3.0 * ConstructPainter.noise(k, 87, 3)) * scale;
            Vec3 mote = at.add(way.scale(out)).subtract(0.0, 0.002 * since * since * scale, 0.0);
            double twinkle = 0.6 + 0.4 * Math.sin(since * 0.9 + k);
            painter.flare(mote, 0.08 * scale, left * twinkle);
        }
    }

    /** How far this player's ring fist is thrown up high, 0 to 1: for a flare, or to call an air strike's plane. */
    static float up(Entity player, float partialTick) {
        return Math.max(raised(player, partialTick), PlanePainter.raised(player, partialTick));
    }

    /**
     * How far this player's ring fist is up for a flare, 0 to 1: straight up as it starts, and down again a moment
     * after the burst.
     */
    static float raised(Entity player, float partialTick) {
        Going flare = ClientConstructs.flare(player.getId(), partialTick);
        if (flare == null) {
            return 0.0F;
        }
        double down = LightFlare.GATHER_TICKS + LightFlare.ARM_DOWN;
        return (float) (ConstructPainter.smooth(flare.clock() / 2.5) * (1.0 - ConstructPainter.smooth((flare.clock()
                - down + 5.0) / 5.0)));
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
