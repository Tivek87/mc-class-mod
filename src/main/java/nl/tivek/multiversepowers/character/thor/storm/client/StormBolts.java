package nl.tivek.multiversepowers.character.thor.storm.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.thor.storm.StormFxPayload;
import nl.tivek.multiversepowers.engine.client.fx.CameraShake;
import nl.tivek.multiversepowers.engine.client.fx.ScreenFlash;
import nl.tivek.multiversepowers.engine.client.ragdoll.Ragdolls;
import nl.tivek.multiversepowers.engine.client.render.Bolts;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.render.Material;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;

// The bolts out of Thor's storm and round his lightning bomb, and the bomb's burst, as the server tells them: drawn
// each frame until they are over, with a flash, a crack where they strike, thunder later the further off it is (sound
// is slow) and a jolt of the view for whoever stands near.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class StormBolts {
    static final Material LIGHTNING = new Material(0x7FD4FF, 0xD6F4FF, 0x3FA2FF, 0xF6FBFF);
    static final Bolts.Look LOOK = new Bolts.Look(LIGHTNING, 0x2E5BFF, 0x1E272E);
    static final int CORE = 0xDDF3FF;
    static final Vec3 EAST = new Vec3(1.0, 0.0, 0.0);
    static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);
    static final Vec3 SOUTH = new Vec3(0.0, 0.0, 1.0);
    private static final int BLAST = 40;
    private static final int MOST = 48;
    // Sound covers this many blocks a tick.
    private static final double SOUND_SPEED = 17.0;
    private static final RandomSource RANDOM = RandomSource.create();

    private static final class Fx {
        final StormFxPayload said;
        final long born;
        int thunder;

        Fx(StormFxPayload said, long born, int thunder) {
            this.said = said;
            this.born = born;
            this.thunder = thunder;
        }

        int kind() {
            return this.said.kind();
        }
    }

    private static final List<Fx> LIVE = new ArrayList<>();

    private StormBolts() {
    }

    public static void add(StormFxPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        if (LIVE.size() >= MOST) {
            LIVE.remove(0);
        }
        Vec3 eye = minecraft.gameRenderer.getMainCamera().getPosition();
        Vec3 at = payload.kind() == StormFxPayload.BLAST ? payload.from() : payload.to();
        double far = eye.distanceTo(at);
        LIVE.add(new Fx(payload, level.getGameTime(), payload.kind() == StormFxPayload.SPARK ? -1
                : (int) (far / SOUND_SPEED)));
        struck(level, payload, at, far);
    }

    // The moment it strikes: the flash, the crack, the jolt, its sparks and smoke, and limp bodies thrown off.
    private static void struck(ClientLevel level, StormFxPayload payload, Vec3 at, double far) {
        switch (payload.kind()) {
            case StormFxPayload.BOLT -> {
                if (far < 96.0) {
                    level.setSkyFlashTime(2);
                }
                near(far, 40.0, 0.35F, 2.5F, 10);
                level.playLocalSound(at.x, at.y, at.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER, 2.0F,
                        0.9F + RANDOM.nextFloat() * 0.2F, false);
                puffs(level, ParticleTypes.ELECTRIC_SPARK, at, 40, 0.5, 0.6);
                puffs(level, ParticleTypes.LARGE_SMOKE, at, 10, 0.6, 0.05);
                Ragdolls.blast(at, 1.5);
            }
            case StormFxPayload.SPARK -> {
                near(far, 14.0, 0.1F, 0.6F, 6);
                level.playLocalSound(at.x, at.y, at.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.5F,
                        1.5F + RANDOM.nextFloat() * 0.3F, false);
                puffs(level, ParticleTypes.ELECTRIC_SPARK, at, 12, 0.3, 0.35);
            }
            case StormFxPayload.BLAST -> {
                double radius = payload.size();
                level.setSkyFlashTime(3);
                near(far, radius * 5.0, 0.65F, 4.0F, 18);
                puffs(level, ParticleTypes.ELECTRIC_SPARK, at, 90, radius * 0.3, 1.2);
                puffs(level, ParticleTypes.CLOUD, payload.to(), 30, radius * 0.4, 0.25);
                level.addParticle(ParticleTypes.FLASH, at.x, at.y, at.z, 0.0, 0.0, 0.0);
                Ragdolls.blast(at, radius * 0.5);
                Ragdolls.blast(payload.to(), radius * 0.5);
            }
            default -> {
            }
        }
    }

    // Whether a burst has gone off within `within` of `at`.
    static boolean burstNear(Vec3 at, double within) {
        for (Fx fx : LIVE) {
            if (fx.kind() == StormFxPayload.BLAST && fx.said.from().distanceToSqr(at) < within * within) {
                return true;
            }
        }
        return false;
    }

    // A flash and a jolt for whoever stands within `reach`, the stronger the nearer.
    private static void near(double far, double reach, float flash, float shake, int ticks) {
        double close = 1.0 - far / reach;
        if (close > 0.0) {
            ScreenFlash.add(CORE, flash * (float) close, ticks / 2 + 2);
            CameraShake.add(shake * (float) close, ticks);
        }
    }

    private static void puffs(ClientLevel level, ParticleOptions particle, Vec3 at, int count, double spread,
            double speed) {
        for (int k = 0; k < count; k++) {
            level.addParticle(particle, at.x + (RANDOM.nextDouble() - 0.5) * spread * 2.0,
                    at.y + RANDOM.nextDouble() * spread, at.z + (RANDOM.nextDouble() - 0.5) * spread * 2.0,
                    (RANDOM.nextDouble() - 0.5) * speed, RANDOM.nextDouble() * speed * 0.6,
                    (RANDOM.nextDouble() - 0.5) * speed);
        }
    }

    private static int life(Fx fx) {
        return switch (fx.kind()) {
            case StormFxPayload.BOLT -> Bolts.BOLT;
            case StormFxPayload.SPARK -> Bolts.ARC;
            default -> BLAST;
        };
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.isPaused() || LIVE.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        Iterator<Fx> all = LIVE.iterator();
        while (all.hasNext()) {
            Fx fx = all.next();
            long age = now - fx.born;
            if (fx.thunder >= 0 && age >= fx.thunder) {
                Vec3 at = fx.kind() == StormFxPayload.BLAST ? fx.said.from() : fx.said.to();
                level.playLocalSound(at.x, at.y, at.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER,
                        fx.kind() == StormFxPayload.BLAST ? 8.0F : 6.0F, 0.75F + RANDOM.nextFloat() * 0.2F, false);
                fx.thunder = -1;
            }
            if (fx.thunder < 0 && age > life(fx)) {
                all.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || LIVE.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        double now = level.getGameTime() + partialTick;
        Camera camera = event.getCamera();
        ConstructPainter painter = new ConstructPainter(event.getPoseStack(), camera.getPosition(),
                (float) (now % 24000.0), event.getFrustum(), LIGHTNING);
        for (Fx fx : LIVE) {
            double age = now - fx.born;
            if (age < 0.0 || age > life(fx)) {
                continue;
            }
            StormFxPayload said = fx.said;
            switch (fx.kind()) {
                case StormFxPayload.BOLT -> Bolts.bolt(painter, LOOK, said.from(), said.to(), age, said.seed());
                case StormFxPayload.SPARK -> Bolts.arc(painter, LOOK, said.from(), said.to(), age, said.seed());
                default -> blast(painter, said.from(), said.to(), said.size(), age, said.seed());
            }
        }
        painter.finish(minecraft.renderBuffers().bufferSource());
    }

    // The bomb bursting at `c` over `ground`: a white flash swelling out, a shock front running past the edge, bolts
    // thrown out of it down onto the ground all round and up into the sky, and on the ground a ring of light running
    // out over glowing cracks that cool.
    private static void blast(ConstructPainter painter, Vec3 c, Vec3 ground, double radius, double age, int seed) {
        painter.material(LIGHTNING);
        double fade = Math.max(0.0, 1.0 - age / 12.0);
        if (fade > 0.0) {
            double size = 1.0 + (radius * 0.7 - 1.0) * Ease.smooth(Math.min(1.0, age / 4.0));
            painter.haze(c, EAST.scale(size), UP.scale(size), SOUTH.scale(size), CORE, 0.9 * fade);
            painter.flare(c, 1.0 + 6.0 * fade, fade);
        }
        double front = Math.max(0.0, 1.0 - age / 14.0);
        if (front > 0.0) {
            painter.shell(c, 1.0 + radius * 1.1 * Ease.smooth(Math.min(1.0, age / 10.0)), LIGHTNING.glow(),
                    0.8 * front);
        }
        if (age < 10.0) {
            double on = 1.0 - age / 10.0;
            int flick = seed + (int) (age * 1.5);
            for (int k = 0; k < 14; k++) {
                double angle = Math.PI * 2.0 * k / 14.0 + Noise.of(seed, k, 1) * 0.4;
                double reach = radius * (0.55 + 0.45 * Noise.of(seed, k, 2));
                Vec3 end = new Vec3(c.x + Math.cos(angle) * reach, ground.y + 0.05, c.z + Math.sin(angle) * reach);
                Bolts.jag(painter, LOOK, c, end, 10, 1.2, 0.18, on, flick + k * 7);
            }
            for (int k = 0; k < 5; k++) {
                Vec3 end = c.add(Noise.direction(seed, k + 20).multiply(6.0, 0.0, 6.0))
                        .add(0.0, 8.0 + 6.0 * Noise.of(seed, k, 3), 0.0);
                Bolts.jag(painter, LOOK, c, end, 9, 1.0, 0.12, 0.8 * on, flick + k * 11);
            }
        }
        Vec3 floor = ground.add(0.0, 0.06, 0.0);
        if (age < 14.0) {
            double u = age / 14.0;
            painter.circle(floor, EAST, SOUTH, 0.5 + radius * 1.2 * Ease.smooth(u), 0.18, 1.2,
                    Colors.alpha(0.95 * (1.0 - u)), Colors.alpha(0.6 * (1.0 - u)));
        }
        double cool = Math.max(0.0, 1.0 - age / BLAST);
        for (int k = 0; k < 16; k++) {
            double angle = Math.PI * 2.0 * k / 16.0 + Noise.of(seed, k, 4) * 0.3;
            double reach = radius * (0.4 + 0.5 * Noise.of(seed, k, 5));
            Vec3 end = floor.add(Math.cos(angle) * reach, 0.0, Math.sin(angle) * reach);
            Bolts.jag(painter, LOOK, floor, end, 6, 0.5, 0.06, cool * cool, seed + k * 5);
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        LIVE.clear();
    }
}
