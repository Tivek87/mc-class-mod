package nl.tivek.multiversepowers.galactus.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.client.fx.CameraShake;
import nl.tivek.multiversepowers.engine.client.fx.ScreenFlash;
import nl.tivek.multiversepowers.galactus.Galactus;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

// Galactus in this player's sky (`Galactus`), drawn right after the sky, so the world stands in front of him and he in
// front of the moon and stars. He is worked out where the game draws its sky: the eye in the middle, the moon's disc
// 12.5 across at 100 away, its way across the sky as the game turns it. At the poke his index fingertip, as wide as
// the moon, lies by the moon's lower edge; his arm runs back from it to his shoulder and his body stands far behind,
// so he fills half the sky. He is placed from his pose at the touch, so whatever his arm does on the way the fingertip
// lands there. Before and after, all of him stands the same size but that many times further off (a speck far out).
// It is drawn shrunk about the eye to fit in the game's view, which looks the same; lit by the moon and a cosmic rim
// light, so he reads at night, with his aura, the lightning crackling at his fingers and the moon ringing.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class GalactusSky {
    private static final double MOON_DISTANCE = 100.0;
    private static final double MOON_WIDTH = 12.5;
    // How far off the farthest of him is drawn, and how tall he is in model pixels.
    private static final double DRAWN = 80.0;
    private static final double TALL = 72.0;
    // How many times further off than at the poke he stands right by the moon.
    private static final double BESIDE_FAR = 30.0;
    private static final int RIPPLE = 0xE8F6FF;
    private static final double RIPPLE_TICKS = 70.0;
    private static final GalactusRig RIG = new GalactusRig();
    private static final GalactusMesh MESH = new GalactusMesh();
    // From his pose at the touch: how far the fingertip lies from the right shoulder, and where that shoulder is.
    private static float reachLength = -1.0F;
    @Nullable
    private static Vector3f touchShoulder;
    private static long start = -1L;
    private static float speed = 1.0F;
    private static double lastT = Double.NaN;

    private GalactusSky() {
    }

    public static void told(long from, float pace) {
        start = from;
        speed = pace;
        lastT = Double.NaN;
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        start = -1L;
    }

    @SubscribeEvent
    public static void onStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY || start < 0L) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || level.effects().skyType() != DimensionSpecialEffects.SkyType.NORMAL) {
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        double t = (level.getGameTime() - start + partialTick) * speed;
        if (t > Galactus.GONE) {
            start = -1L;
            return;
        }
        sounds(minecraft, t);
        draw(event, level, t, partialTick);
    }

    public static void onRegisterShaders(RegisterShadersEvent event) {
        GalactusMesh.register(event);
    }

    private static void draw(RenderLevelStageEvent event, ClientLevel level, double t, float partialTick) {
        // The moon where the game draws it, and the sky's right and up seen towards it.
        float turn = level.getTimeOfDay(partialTick) * 360.0F;
        Vector3f toMoon = Axis.YP.rotationDegrees(-90.0F).mul(Axis.XP.rotationDegrees(turn))
                .transform(new Vector3f(0.0F, -1.0F, 0.0F));
        Vec3 m = new Vec3(toMoon.x(), toMoon.y(), toMoon.z()).normalize();
        Vec3 right = m.cross(new Vec3(0.0, 1.0, 0.0));
        right = right.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : right.normalize();
        Vec3 up = right.cross(m);
        Vec3 moon = m.scale(MOON_DISTANCE);
        // The fingertip by the moon's lower right edge; the arm reaching out at it from far behind, the shoulder some 40
        // degrees up, so his body towers from the horizon over half the sky.
        Vec3 edge = right.scale(0.72).subtract(up.scale(0.69)).normalize();
        Vec3 tip = moon.add(edge.scale(MOON_WIDTH)).scale(0.985);
        Vec3 reach = m.scale(-0.92).subtract(right.scale(0.365)).add(up.scale(0.11)).normalize();
        double pixel = MOON_WIDTH / GalactusRig.fingerWidth();
        touch();
        Vec3 shoulder = tip.subtract(reach.scale(reachLength * pixel));
        // He stands upright facing the eye.
        Vec3 ahead = new Vec3(-shoulder.x, 0.0, -shoulder.z);
        ahead = ahead.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : ahead.normalize();
        Vec3 left = new Vec3(0.0, 1.0, 0.0).cross(ahead);
        Matrix3f stand = new Matrix3f((float) left.x, (float) left.y, (float) left.z, 0.0F, 1.0F, 0.0F,
                (float) ahead.x, (float) ahead.y, (float) ahead.z);
        Matrix3f into = new Matrix3f(stand).transpose();
        Vector3f shoulderOff = stand.transform(new Vector3f(touchShoulder)).mul((float) pixel);
        Vec3 feet = shoulder.subtract(shoulderOff.x(), shoulderOff.y(), shoulderOff.z());
        // Further off he stands where his chest is seen from the eye, so he never sinks below the horizon.
        double far = Galactus.far(t);
        Vec3 chestOff = place(stand, pixel, new Vector3f(0.0F, 44.0F, 4.0F));
        Vec3 chest = feet.add(chestOff).scale(far);
        feet = chest.subtract(chestOff);
        // Far out he stands just by the moon, a speck beside it, and comes in to where he reaches from.
        double by = Mth.clamp(Math.log(far) / Math.log(BESIDE_FAR), 0.0, 1.0);
        by = by * by * (3.0 - 2.0 * by);
        if (by > 0.0) {
            Vec3 beside = m.add(right.scale(0.06)).subtract(up.scale(0.08)).normalize();
            Vec3 moved = chest.normalize().lerp(beside, by).normalize().scale(chest.length());
            feet = feet.add(moved.subtract(chest));
            chest = moved;
        }
        // His pose: the arm laid along the reach, the head turned to the moon.
        Vector3f reachHere = into.transform(new Vector3f((float) reach.x, (float) reach.y, (float) reach.z));
        Vec3 toMoonHere = moon.subtract(feet).scale(1.0 / pixel);
        Vector3f lookAt = into.transform(new Vector3f((float) toMoonHere.x, (float) toMoonHere.y,
                (float) toMoonHere.z));
        GalactusMotion.Pose pose = GalactusMotion.at(t);
        GalactusMotion.apply(RIG, pose, reachHere, lookAt);
        // Shrunk about the eye until the farthest of him lies within the game's view.
        float shrink = (float) (DRAWN / (feet.length() + TALL * pixel));
        Matrix4f sky = new Matrix4f().scale(shrink);
        Matrix4fStack view = RenderSystem.getModelViewStack();
        view.pushMatrix();
        view.set(event.getModelViewMatrix());
        RenderSystem.applyModelViewMatrix();
        Matrix4f body = new Matrix4f(event.getModelViewMatrix()).mul(sky)
                .translate((float) feet.x, (float) feet.y, (float) feet.z).mul(new Matrix4f(stand))
                .scale((float) pixel);
        float alpha = (float) (Mth.clamp(t / 40.0, 0.0, 1.0) * Mth.clamp((Galactus.GONE - t) / 120.0, 0.0, 1.0));
        float day = Mth.clamp((level.getSkyDarken(partialTick) - 0.2F) / 0.8F, 0.0F, 1.0F);
        Vec3 hand = feet.add(place(stand, pixel, RIG.at(GalactusRig.HAND + GalactusRig.RIGHT,
                new Vector3f(0.0F, -2.5F, 0.0F))));
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        // A cosmic haze round him, behind all of him: violet, a teal veil off to one side, and the glow of his hand.
        GalactusFx.additive();
        RenderSystem.depthMask(false);
        double aura = 0.75 + 0.25 * Math.sin(t * 0.05);
        GalactusFx.disc(sky, chest, 46.0 * pixel, GalactusFx.VIOLET, (float) (0.3 * aura) * alpha);
        GalactusFx.disc(sky, chest.add(place(stand, pixel, new Vector3f(-14.0F, 14.0F, -8.0F))), 34.0 * pixel,
                GalactusFx.TEAL, 0.12F * alpha);
        GalactusFx.disc(sky, hand, 9.0 * pixel, GalactusFx.VIOLET, 0.35F * pose.charge * alpha);
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
        MESH.draw(RIG, body, event.getProjectionMatrix(), lights(event, moon, chest, day), (float) t, pose.energy,
                pose.charge, alpha);
        GalactusFx.additive();
        RenderSystem.depthMask(false);
        // Far off he is a speck of light by the moon.
        float speck = (float) Mth.clamp((far - 20.0) / 60.0, 0.0, 1.0) * alpha;
        if (speck > 0.0F) {
            double across = chest.length() * 0.007;
            GalactusFx.disc(sky, chest, across * 2.5, GalactusFx.VIOLET, 0.5F * speck);
            GalactusFx.star(sky, chest, across, GalactusFx.SPARK, speck);
        }
        // His eyes burn through the dark.
        for (int side = -1; side <= 1; side += 2) {
            Vec3 eye = feet.add(place(stand, pixel, RIG.at(GalactusRig.HEAD, new Vector3f(side * 1.3F, 5.1F,
                    4.4F))));
            eye = eye.subtract(eye.normalize().scale(2.0 * pixel));
            GalactusFx.disc(sky, eye, 1.6 * pixel * GalactusRig.HEAD_SIZE, GalactusFx.VIOLET,
                    (0.55F + 0.15F * (float) Math.sin(t * 0.13)) * alpha * (1.0F - speck));
        }
        lightning(level, sky, feet, stand, pixel, moon, tip, t, pose.charge * alpha * (float) (1.0 - by));
        // The fingertip flares where it touches the moon.
        double poke = Galactus.poke(t);
        if (poke > 0.0) {
            GalactusFx.disc(sky, tip, MOON_WIDTH * 0.7, GalactusFx.SPARK, (float) (0.55 * poke * poke) * alpha);
        }
        double since = t - Galactus.TOUCH;
        if (since >= 0.0 && since < RIPPLE_TICKS) {
            ripple(sky, moon, tip, since);
        }
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        view.popMatrix();
        RenderSystem.applyModelViewMatrix();
        // The world is drawn after him with depths of its own.
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
    }

    // His pose at the touch, worked out once: the reach's length and the shoulder it runs from.
    private static void touch() {
        if (touchShoulder != null) {
            return;
        }
        GalactusMotion.Pose pose = GalactusMotion.at(Galactus.TOUCH);
        reachLength = RIG.reachInChest(pose.root, pose.turn, pose.swell).length();
        touchShoulder = RIG.joint(GalactusRig.UPPER + GalactusRig.RIGHT);
    }

    private static Vec3 place(Matrix3f stand, double pixel, Vector3f local) {
        Vector3f at = stand.transform(new Vector3f(local)).mul((float) pixel);
        return new Vec3(at.x, at.y, at.z);
    }

    // The moon (or by day the sun) lights him from before him and above; violet and teal light his edges from the
    // cosmos behind him, so he reads against the night.
    private static GalactusMesh.Lights lights(RenderLevelStageEvent event, Vec3 moon, Vec3 chest, float day) {
        Matrix4f camera = event.getModelViewMatrix();
        Vec3 fromMoon = moon.subtract(chest).normalize().add(0.0, 0.45, 0.0).normalize();
        Vector3f key = camera.transformDirection(new Vector3f((float) fromMoon.x, (float) fromMoon.y,
                (float) fromMoon.z)).normalize();
        Vector3f keyColor = new Vector3f(0.8F, 0.85F, 1.1F).lerp(new Vector3f(1.1F, 1.05F, 0.95F), day);
        Vector3f ambient = new Vector3f(0.11F, 0.09F, 0.18F).lerp(new Vector3f(0.32F, 0.31F, 0.38F), day);
        return new GalactusMesh.Lights(key, keyColor, ambient, new Vector3f(-0.55F, 0.7F, -0.45F).normalize(),
                new Vector3f(0.8F, 0.38F, 1.0F), new Vector3f(0.75F, -0.25F, -0.6F).normalize(),
                new Vector3f(0.25F, 0.8F, 0.95F));
    }

    // Lightning crackling at his right hand's fingertips as he reaches, leaping between them, and onto the moon as he
    // strikes it; it flickers anew every few frames.
    private static void lightning(ClientLevel level, Matrix4f sky, Vec3 feet, Matrix3f stand, double pixel, Vec3 moon,
            Vec3 tip, double t, float strength) {
        if (strength <= 0.05F) {
            return;
        }
        long flick = (level.getGameTime() * 3L + (long) (Minecraft.getInstance().getTimer()
                .getGameTimeDeltaPartialTick(false) * 3.0F));
        Random random = new Random(flick * 7919L);
        Vec3[] ends = new Vec3[5];
        for (int f = 0; f < 5; f++) {
            Vector3f local = RIG.at(GalactusRig.digit(GalactusRig.RIGHT, f, 2),
                    GalactusRig.endLocal(GalactusRig.RIGHT, f));
            ends[f] = feet.add(place(stand, pixel, local));
        }
        Vec3 palm = feet.add(place(stand, pixel, RIG.at(GalactusRig.HAND + GalactusRig.RIGHT,
                new Vector3f(0.0F, -2.5F, 0.0F))));
        double width = 0.07 * pixel;
        int arcs = 2 + Math.round(4.0F * Math.min(1.0F, strength));
        for (int i = 0; i < arcs; i++) {
            Vec3 from = ends[random.nextInt(5)];
            Vec3 out = from.subtract(palm).normalize().add(new Vec3(random.nextGaussian(), random.nextGaussian(),
                    random.nextGaussian()).scale(0.6)).normalize();
            Vec3 to = from.add(out.scale((2.0 + random.nextDouble() * 4.0) * pixel));
            GalactusFx.bolt(sky, from, to, width, Math.min(1.0F, strength) * (0.6F + 0.4F * random.nextFloat()),
                    random);
        }
        int a = random.nextInt(5);
        int b = (a + 1 + random.nextInt(4)) % 5;
        GalactusFx.bolt(sky, ends[a], ends[b], width * 0.8, Math.min(1.0F, strength) * 0.8F, random);
        double since = t - Galactus.TOUCH;
        if (since > -4.0 && since < 40.0) {
            float struck = (float) Mth.clamp(1.0 - since / 40.0, 0.0, 1.0);
            Vec3[] face = GalactusFx.facing(moon);
            for (int i = 0; i < 4; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0;
                double r = MOON_WIDTH * (0.15 + 0.4 * random.nextDouble());
                Vec3 on = moon.add(face[0].scale(Math.cos(angle) * r)).add(face[1].scale(Math.sin(angle) * r));
                GalactusFx.bolt(sky, tip, on, width * 1.4, struck, random);
            }
        }
    }

    // Struck, the moon rings: a flash where the fingertip meets it and rings of light running out round it.
    private static void ripple(Matrix4f base, Vec3 moon, Vec3 tip, double since) {
        double u = since / RIPPLE_TICKS;
        float fade = (float) (1.0 - u);
        Vec3 touch = moon.add(tip.subtract(moon).scale(0.5));
        if (since < 12.0) {
            float flash = (float) (1.0 - since / 12.0);
            GalactusFx.disc(base, touch, MOON_WIDTH * (0.6 + 1.6 * (1.0 - flash)), RIPPLE, 0.9F * flash);
        }
        GalactusFx.disc(base, moon, MOON_WIDTH * (0.7 + 0.5 * u), RIPPLE, 0.35F * fade * fade);
        for (int k = 0; k < 3; k++) {
            double ring = u * 1.6 - k * 0.18;
            if (ring > 0.0 && ring < 1.0) {
                GalactusFx.ring(base, moon, MOON_WIDTH * (0.55 + 3.2 * ring), MOON_WIDTH * 0.06 * (1.0 + 2.0 * ring),
                        RIPPLE, (float) (0.8 * (1.0 - ring)) * fade);
            }
        }
    }

    // The moments heard by all who see him: his coming, the arm rising, the poke, his going.
    private static void sounds(Minecraft minecraft, double t) {
        double was = lastT;
        lastT = t;
        if (Double.isNaN(was)) {
            if (t < 5.0) {
                play(minecraft, SoundEvents.BEACON_ACTIVATE, 0.5F, 1.0F);
            }
            return;
        }
        if (was < Galactus.RAISE && t >= Galactus.RAISE) {
            play(minecraft, SoundEvents.WARDEN_SONIC_CHARGE, 0.5F, 1.0F);
        }
        if (was < Galactus.TOUCH && t >= Galactus.TOUCH) {
            play(minecraft, SoundEvents.WARDEN_SONIC_BOOM, 0.5F, 1.0F);
            play(minecraft, SoundEvents.BELL_RESONATE, 0.5F, 1.0F);
            play(minecraft, SoundEvents.GENERIC_EXPLODE.value(), 0.5F, 0.8F);
            ScreenFlash.add(RIPPLE, 0.35F, 16);
            CameraShake.add(3.0F, 30);
        }
        if (was < Galactus.LOWERED && t >= Galactus.LOWERED) {
            play(minecraft, SoundEvents.BEACON_DEACTIVATE, 0.5F, 1.0F);
        }
    }

    private static void play(Minecraft minecraft, SoundEvent sound, float pitch, float volume) {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }
}
