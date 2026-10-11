package nl.tivek.multiversepowers.galactus.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.client.fx.CameraShake;
import nl.tivek.multiversepowers.engine.client.fx.ScreenFlash;
import nl.tivek.multiversepowers.galactus.Galactus;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

// Galactus in this player's sky (`Galactus`), drawn right after the sky, so the world stands in front of him and he in
// front of the moon and stars. He is worked out where the game draws its sky: the eye in the middle, the moon's disc
// 12.5 across at 100 away, its way across the sky as the game turns it. At the poke his index fingertip, two pixels
// of his, lies by the moon's lower edge, as wide as the moon; his arm runs back from it to his shoulder and his body
// stands far behind, so he fills half the sky. Before and after, all of him stands the same size but that many times
// further off (a speck far out). It is drawn shrunk about the eye to fit in the game's view, which looks the same.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class GalactusSky {
    private static final double MOON_DISTANCE = 100.0;
    private static final double MOON_WIDTH = 12.5;
    // How far short of the moon the fingertip starts its poke, and how far off the farthest of him is drawn.
    private static final double JAB = 30.0;
    private static final double DRAWN = 80.0;
    // How many times further off than at the poke he stands right by the moon.
    private static final double BESIDE_FAR = 30.0;
    private static final int AURA = 0x6A2C9A;
    private static final int SPARK = 0xCFEFFF;
    private static final int RIPPLE = 0xE8F6FF;
    private static final double RIPPLE_TICKS = 70.0;
    private static final GalactusMesh MESH = new GalactusMesh();
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
        // The fingertip by the moon's lower right edge; the arm reaching up and in to it from the lower right.
        Vec3 edge = right.scale(0.72).subtract(up.scale(0.69)).normalize();
        Vec3 tip = moon.add(edge.scale(MOON_WIDTH)).scale(0.985);
        Vec3 reach = m.scale(-0.72).subtract(right.scale(0.5)).add(up.scale(0.48)).normalize();
        double pixel = MOON_WIDTH / GalactusShape.FINGER;
        Vector3f tipFromShoulder = new Vector3f(GalactusShape.TIP[0], GalactusShape.ELBOW + GalactusShape.TIP[1],
                GalactusShape.TIP[2]);
        Vec3 shoulder = tip.subtract(reach.scale(tipFromShoulder.length() * pixel));
        // He stands upright facing the eye.
        Vec3 ahead = new Vec3(-shoulder.x, 0.0, -shoulder.z);
        ahead = ahead.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : ahead.normalize();
        Vec3 left = new Vec3(0.0, 1.0, 0.0).cross(ahead);
        Matrix3f stand = new Matrix3f((float) left.x, (float) left.y, (float) left.z, 0.0F, 1.0F, 0.0F,
                (float) ahead.x, (float) ahead.y, (float) ahead.z);
        Vector3f shoulderOff = stand.transform(new Vector3f(GalactusShape.SHOULDER[0], GalactusShape.SHOULDER[1],
                GalactusShape.SHOULDER[2])).mul((float) pixel);
        double poke = Galactus.poke(t);
        Vec3 feet = shoulder.subtract(shoulderOff.x(), shoulderOff.y(), shoulderOff.z())
                .subtract(reach.scale((1.0 - poke) * JAB));
        double far = Galactus.far(t);
        feet = feet.scale(far);
        // Far out he stands just by the moon, a speck beside it, and comes in to where he reaches from.
        Vec3 chest = feet.add(new Vec3(ahead.x * 6.0, 43.0, ahead.z * 6.0).scale(pixel));
        double by = Mth.clamp(Math.log(far) / Math.log(BESIDE_FAR), 0.0, 1.0);
        by = by * by * (3.0 - 2.0 * by);
        if (by > 0.0) {
            Vec3 beside = m.add(right.scale(0.06)).subtract(up.scale(0.08)).normalize();
            Vec3 moved = chest.normalize().lerp(beside, by).normalize().scale(chest.length());
            feet = feet.add(moved.subtract(chest));
            chest = moved;
        }
        // The arm: from hanging a little out to pointing along `reach`, the elbow bending on the way up.
        double arm = Galactus.arm(t);
        Vector3f reachHere = new Matrix3f(stand).transpose().transform(new Vector3f((float) reach.x,
                (float) reach.y, (float) reach.z));
        Quaternionf aim = new Quaternionf().rotationTo(new Vector3f(tipFromShoulder).normalize(), reachHere);
        Quaternionf hanging = new Quaternionf().rotationZ(-0.12F);
        Quaternionf shoulderTurn = new Quaternionf(hanging).slerp(aim, (float) arm);
        float elbow = (float) (1.1 * Math.sin(Math.PI * arm));
        // Shrunk about the eye until the farthest of him lies within the game's view.
        double size = 72.0 * pixel;
        float shrink = (float) (DRAWN / (feet.length() + size));
        // The buffers take the view whole; what is drawn at once takes the view from the game's own matrix.
        Matrix4f sky = new Matrix4f().scale(shrink);
        Matrix4f base = new Matrix4f(event.getModelViewMatrix()).mul(sky);
        Matrix4fStack view = RenderSystem.getModelViewStack();
        view.pushMatrix();
        view.set(event.getModelViewMatrix());
        RenderSystem.applyModelViewMatrix();
        Matrix4f body = new Matrix4f(base).translate((float) feet.x, (float) feet.y, (float) feet.z)
                .mul(new Matrix4f(stand)).scale((float) pixel);
        Matrix4f upper = new Matrix4f(body).translate(GalactusShape.SHOULDER[0], GalactusShape.SHOULDER[1],
                GalactusShape.SHOULDER[2]).rotate(shoulderTurn);
        Matrix4f lower = new Matrix4f(upper).translate(0.0F, GalactusShape.ELBOW, 0.0F).rotateX(-elbow);
        float alpha = (float) (Mth.clamp(t / 40.0, 0.0, 1.0) * Mth.clamp((Galactus.GONE - t) / 120.0, 0.0, 1.0));
        float light = 0.38F + 0.62F * level.getSkyDarken(partialTick);
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        // A cosmic haze round him, behind all of him.
        additive();
        RenderSystem.depthMask(false);
        disc(sky, chest, 50.0 * pixel, AURA, 0.32F * alpha);
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
        // The arm reaching up to the moon is lit by it, so the poke reads against the night.
        float armLight = light + (1.0F - light) * 0.75F * (float) arm;
        MESH.draw(body, upper, lower, event.getProjectionMatrix(), light, armLight, alpha);
        additive();
        RenderSystem.depthMask(false);
        // Far off he is a speck of light by the moon.
        float speck = (float) Mth.clamp((far - 20.0) / 60.0, 0.0, 1.0) * alpha;
        if (speck > 0.0F) {
            double across = chest.length() * 0.007;
            disc(sky, chest, across * 2.5, AURA, 0.5F * speck);
            star(sky, chest, across, SPARK, speck);
        }
        // The fingertip flares where it touches the moon.
        if (poke > 0.0) {
            disc(sky, tip, MOON_WIDTH * 0.7, SPARK, (float) (0.55 * poke * poke) * alpha);
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

    // Struck, the moon rings: a flash where the fingertip meets it and rings of light running out round it.
    private static void ripple(Matrix4f base, Vec3 moon, Vec3 tip, double since) {
        double u = since / RIPPLE_TICKS;
        float fade = (float) (1.0 - u);
        Vec3 touch = moon.add(tip.subtract(moon).scale(0.5));
        if (since < 12.0) {
            float flash = (float) (1.0 - since / 12.0);
            disc(base, touch, MOON_WIDTH * (0.6 + 1.6 * (1.0 - flash)), RIPPLE, 0.9F * flash);
        }
        disc(base, moon, MOON_WIDTH * (0.7 + 0.5 * u), RIPPLE, 0.35F * fade * fade);
        for (int k = 0; k < 3; k++) {
            double ring = u * 1.6 - k * 0.18;
            if (ring > 0.0 && ring < 1.0) {
                ring(base, moon, MOON_WIDTH * (0.55 + 3.2 * ring), MOON_WIDTH * 0.06 * (1.0 + 2.0 * ring),
                        RIPPLE, (float) (0.8 * (1.0 - ring)) * fade);
            }
        }
    }

    // A soft disc facing the eye at `at`, `rgb` at its middle fading to nothing at its edge.
    private static void disc(Matrix4f base, Vec3 at, double radius, int rgb, float alpha) {
        if (alpha <= 0.0F) {
            return;
        }
        Vec3[] face = facing(at);
        BufferBuilder fan = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES,
                DefaultVertexFormat.POSITION_COLOR);
        int sides = 40;
        for (int i = 0; i < sides; i++) {
            Vec3 a = around(at, face, radius, i, sides);
            Vec3 b = around(at, face, radius, i + 1, sides);
            fan.addVertex(base, (float) at.x, (float) at.y, (float) at.z).setColor(color(rgb, alpha));
            fan.addVertex(base, (float) a.x, (float) a.y, (float) a.z).setColor(color(rgb, 0.0F));
            fan.addVertex(base, (float) b.x, (float) b.y, (float) b.z).setColor(color(rgb, 0.0F));
        }
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferUploader.drawWithShader(fan.buildOrThrow());
    }

    // A thin ring facing the eye, brightest along its line.
    private static void ring(Matrix4f base, Vec3 at, double radius, double width, int rgb, float alpha) {
        if (alpha <= 0.0F) {
            return;
        }
        Vec3[] face = facing(at);
        BufferBuilder band = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_COLOR);
        int sides = 64;
        for (int i = 0; i < sides; i++) {
            for (int half = -1; half <= 1; half += 2) {
                Vec3 a = around(at, face, radius, i, sides);
                Vec3 b = around(at, face, radius, i + 1, sides);
                Vec3 c = around(at, face, radius + half * width, i + 1, sides);
                Vec3 d = around(at, face, radius + half * width, i, sides);
                band.addVertex(base, (float) a.x, (float) a.y, (float) a.z).setColor(color(rgb, alpha));
                band.addVertex(base, (float) b.x, (float) b.y, (float) b.z).setColor(color(rgb, alpha));
                band.addVertex(base, (float) c.x, (float) c.y, (float) c.z).setColor(color(rgb, 0.0F));
                band.addVertex(base, (float) d.x, (float) d.y, (float) d.z).setColor(color(rgb, 0.0F));
            }
        }
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferUploader.drawWithShader(band.buildOrThrow());
    }

    // A four-pointed star of light facing the eye.
    private static void star(Matrix4f base, Vec3 at, double size, int rgb, float alpha) {
        Vec3[] face = facing(at);
        BufferBuilder rays = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES,
                DefaultVertexFormat.POSITION_COLOR);
        for (int k = 0; k < 4; k++) {
            double angle = k * Math.PI / 2.0;
            Vec3 out = face[0].scale(Math.cos(angle)).add(face[1].scale(Math.sin(angle)));
            Vec3 side = face[0].scale(-Math.sin(angle)).add(face[1].scale(Math.cos(angle)));
            Vec3 point = at.add(out.scale(size * 3.0));
            Vec3 a = at.add(side.scale(size * 0.3));
            Vec3 b = at.subtract(side.scale(size * 0.3));
            rays.addVertex(base, (float) a.x, (float) a.y, (float) a.z).setColor(color(rgb, alpha));
            rays.addVertex(base, (float) b.x, (float) b.y, (float) b.z).setColor(color(rgb, alpha));
            rays.addVertex(base, (float) point.x, (float) point.y, (float) point.z).setColor(color(rgb, 0.0F));
        }
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferUploader.drawWithShader(rays.buildOrThrow());
        disc(base, at, size * 1.2, 0xFFFFFF, alpha);
    }

    // Two axes square to the way from the eye to `at`.
    private static Vec3[] facing(Vec3 at) {
        Vec3 view = at.normalize();
        Vec3 a = view.cross(new Vec3(0.0, 1.0, 0.0));
        a = a.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : a.normalize();
        return new Vec3[] { a, a.cross(view) };
    }

    private static Vec3 around(Vec3 at, Vec3[] face, double radius, int i, int sides) {
        double angle = Math.PI * 2.0 * i / sides;
        return at.add(face[0].scale(Math.cos(angle) * radius)).add(face[1].scale(Math.sin(angle) * radius));
    }

    private static int color(int rgb, float alpha) {
        return Mth.clamp(Math.round(alpha * 255.0F), 0, 255) << 24 | rgb & 0xFFFFFF;
    }

    private static void additive() {
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
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
