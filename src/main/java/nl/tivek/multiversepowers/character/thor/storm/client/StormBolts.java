package nl.tivek.multiversepowers.character.thor.storm.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.thor.storm.StormFxPayload;
import nl.tivek.multiversepowers.engine.client.fx.CameraShake;
import nl.tivek.multiversepowers.engine.client.fx.ParticleAmount;
import nl.tivek.multiversepowers.engine.client.fx.ScreenFlash;
import nl.tivek.multiversepowers.engine.client.ragdoll.Ragdolls;
import nl.tivek.multiversepowers.engine.client.render.Bolts;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.render.Material;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;

// The bolts out of Thor's storm and round his lightning bomb, the bomb's burst and a Storm Throw (StormStrikeFx), as
// the server tells them: drawn each frame until they are over, with a flash, a crack where they strike, thunder later
// the further off it is (sound is slow) and a jolt of the view for whoever stands near.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class StormBolts {
    static final Material LIGHTNING = new Material(0x7FD4FF, 0xD6F4FF, 0x3FA2FF, 0xF6FBFF);
    static final Bolts.Look LOOK = new Bolts.Look(LIGHTNING, 0x2E5BFF, 0x1E272E);
    static final int CORE = 0xDDF3FF;
    static final Vec3 EAST = new Vec3(1.0, 0.0, 0.0);
    static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);
    static final Vec3 SOUTH = new Vec3(0.0, 0.0, 1.0);
    private static final int BLAST = 46;
    private static final int MOST = 48;
    // A bolt ends this far over the ground at most and still scorches it: the middle of a creature it struck.
    private static final double FLOOR = 2.5;
    private static final int SKY_BOLTS = 3;
    // Struck or burst this near your eyes, it throws up nothing in your face.
    static final double CLOSE = 4.0;
    private static final int DOME_LIFE = 14;
    private static final int TENDRILS = 22;
    // Sound covers this many blocks a tick.
    private static final double SOUND_SPEED = 17.0;
    private static final RandomSource RANDOM = RandomSource.create();

    private static final class Fx {
        final StormFxPayload said;
        final long born;
        // The ground a bolt struck, or null when it struck something up in the air.
        @Nullable
        final Vec3 floor;
        int thunder;

        Fx(StormFxPayload said, long born, @Nullable Vec3 floor, int thunder) {
            this.said = said;
            this.born = born;
            this.floor = floor;
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
        Vec3 at = where(payload);
        double far = eye.distanceTo(at);
        Vec3 floor = payload.kind() == StormFxPayload.BOLT ? floor(level, at) : null;
        boolean thunders = payload.kind() == StormFxPayload.BOLT || payload.kind() == StormFxPayload.BLAST
                || payload.kind() == StormFxPayload.STRIKE;
        LIVE.add(new Fx(payload, level.getGameTime(), floor, thunders ? (int) (far / SOUND_SPEED) : -1));
        struck(level, payload, at, far, floor);
    }

    // Where it goes off, as far as its thunder goes: a burst or a Storm Throw's strike where it starts, else where it
    // ends.
    private static Vec3 where(StormFxPayload payload) {
        return payload.kind() == StormFxPayload.BLAST || payload.kind() == StormFxPayload.STRIKE ? payload.from()
                : payload.to();
    }

    // The ground under where a bolt strikes, if it is near: a creature it struck stands on it.
    @Nullable
    private static Vec3 floor(ClientLevel level, Vec3 at) {
        HitResult hit = level.clip(new ClipContext(at.add(0.0, 0.2, 0.0), at.add(0.0, -FLOOR, 0.0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, CollisionContext.empty()));
        return hit.getType() == HitResult.Type.MISS ? null : hit.getLocation();
    }

    // The moment it strikes: the flash, the crack, the jolt, its sparks and smoke, and limp bodies thrown off. Struck
    // right by your eyes, it throws up no smoke or flash in your face and its glare is fainter.
    private static void struck(ClientLevel level, StormFxPayload payload, Vec3 at, double far, @Nullable Vec3 floor) {
        boolean close = far < CLOSE;
        switch (payload.kind()) {
            case StormFxPayload.BOLT -> {
                if (far < 96.0) {
                    level.setSkyFlashTime(2);
                }
                near(far, 40.0, close ? 0.1F : 0.35F, 2.5F, 10);
                level.playLocalSound(at.x, at.y, at.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER, 2.0F,
                        0.9F + RANDOM.nextFloat() * 0.2F, false);
                puffs(level, ParticleTypes.ELECTRIC_SPARK, at, close ? 8 : 40, 0.5, 0.6);
                if (floor != null && !close) {
                    puffs(level, ParticleTypes.LARGE_SMOKE, floor, 5, 0.6, 0.05);
                }
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
                level.setSkyFlashTime(5);
                // The bomber is its middle: a short flash, or the glare would hide what it does.
                near(far, radius * 6.0, close ? 0.35F : 0.8F, 5.0F, close ? 10 : 18);
                puffs(level, ParticleTypes.ELECTRIC_SPARK, at, close ? 30 : 140, radius * 0.3, 1.4);
                puffs(level, ParticleTypes.CLOUD, payload.to(), 30, radius * 0.4, 0.25);
                dust(level, payload.to(), radius);
                if (!close) {
                    level.addParticle(ParticleTypes.FLASH, at.x, at.y, at.z, 0.0, 0.0, 0.0);
                }
                Ragdolls.blast(at, radius * 0.5);
                Ragdolls.blast(payload.to(), radius * 0.5);
            }
            case StormFxPayload.DOME -> {
                near(far, 24.0, 0.3F, 1.5F, 8);
                puffs(level, ParticleTypes.ELECTRIC_SPARK, at, 30, payload.size() * 0.5, 0.8);
            }
            case StormFxPayload.STRIKE -> StormStrikeFx.struck(level, payload, far);
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
    static void near(double far, double reach, float flash, float shake, int ticks) {
        double close = 1.0 - far / reach;
        if (close > 0.0) {
            ScreenFlash.add(CORE, flash * (float) close, ticks / 2 + 2);
            CameraShake.add(shake * (float) close, ticks);
        }
    }

    // Dust thrown out low over the ground all round, as the burst's wind runs out.
    private static void dust(ClientLevel level, Vec3 ground, double radius) {
        int count = ParticleAmount.count(70, RANDOM);
        for (int k = 0; k < count; k++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2.0;
            double from = RANDOM.nextDouble() * radius * 0.3;
            double speed = 0.5 + RANDOM.nextDouble() * 0.6;
            level.addParticle(ParticleTypes.CLOUD, ground.x + Math.cos(angle) * from, ground.y + 0.2
                    + RANDOM.nextDouble() * 0.5, ground.z + Math.sin(angle) * from, Math.cos(angle) * speed,
                    0.02 + RANDOM.nextDouble() * 0.05, Math.sin(angle) * speed);
        }
    }

    static void puffs(ClientLevel level, ParticleOptions particle, Vec3 at, int count, double spread, double speed) {
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
            case StormFxPayload.DOME -> DOME_LIFE;
            case StormFxPayload.HURL -> StormStrikeFx.hurlLife(fx.said);
            case StormFxPayload.STRIKE -> StormStrikeFx.LIFE;
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
                Vec3 at = where(fx.said);
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
                case StormFxPayload.BOLT -> {
                    // One striking right by your eyes (your bomb charging) stops short of them, or it fills the view.
                    double clear = 2.5 * (1.0 - Ease.smooth((camera.getPosition().distanceTo(said.to()) - 0.5) / 1.5));
                    Bolts.bolt(painter, LOOK, said.from(), off(said.to(), said.from(), clear), age, said.seed(),
                            said.size() > 0.0F ? said.size() : 1.0, fx.floor);
                }
                case StormFxPayload.SPARK -> Bolts.arc(painter, LOOK, said.from(), said.to(), age, said.seed());
                case StormFxPayload.DOME -> dome(painter, said.from(), said.size(), age, said.seed());
                case StormFxPayload.HURL -> StormStrikeFx.hurl(painter, camera.getPosition(), said, age);
                case StormFxPayload.STRIKE -> StormStrikeFx.strike(painter, camera.getPosition(), said, age);
                default -> blast(painter, camera.getPosition(), said.from(), said.to(), said.size(), age,
                        said.seed());
            }
        }
        painter.finish(minecraft.renderBuffers().bufferSource());
    }

    // The bomb bursting at `c` over `ground`: bolts out of the sky crash down onto him, a white-hot flash swells out,
    // bolts run from him down to the ground and on over it far past the edge, forking as they go, and a blue glow of
    // charged air lingers and drifts apart; on the ground a ring of light runs out over glowing cracks that cool. Seen
    // from inside it (the bomber's own eyes), its glare and glow round your eyes thin out, or they would white the
    // whole view out.
    private static void blast(ConstructPainter painter, Vec3 eye, Vec3 c, Vec3 ground, double radius, double age,
            int seed) {
        painter.material(LIGHTNING);
        double inCore = 1.0 - Ease.smooth((eye.distanceTo(c) - 1.5) / Math.max(1.0, radius * 0.7));
        // Bolts that start or end right at your eyes would fill the view: from inside they keep a little off.
        double clear = 2.0 * (1.0 - Ease.smooth((eye.distanceTo(c) - 1.0) / 2.0));
        for (int k = 0; k < SKY_BOLTS; k++) {
            // Struck as it bursts: from their first stroke on, one after another.
            double struck = age - k * 1.5;
            if (struck >= 0.0 && struck < 14.0) {
                Vec3 top = c.add((Noise.of(seed, k, 21) - 0.5) * radius * 1.2, 26.0 + 8.0 * Noise.of(seed, k, 22),
                        (Noise.of(seed, k, 23) - 0.5) * radius * 1.2);
                Bolts.bolt(painter, LOOK, top, off(c, top, clear * 1.5), struck + 1.5, seed + k * 101,
                        1.6 - 0.2 * k, null);
            }
        }
        double fade = Math.max(0.0, 1.0 - age / 12.0);
        if (fade > 0.0) {
            double size = 1.0 + (radius * 0.7 - 1.0) * Ease.smooth(Math.min(1.0, age / 4.0));
            painter.haze(c, EAST.scale(size), UP.scale(size), SOUTH.scale(size), CORE,
                    0.95 * fade * (1.0 - 0.8 * inCore));
            painter.flare(c, 1.5 + 9.0 * fade, fade * (1.0 - inCore));
        }
        double front = Math.max(0.0, 1.0 - age / 14.0);
        if (front > 0.0) {
            painter.shell(c, 1.0 + radius * 1.2 * Ease.smooth(Math.min(1.0, age / 10.0)), LIGHTNING.glow(),
                    0.8 * front * (1.0 - 0.5 * inCore));
        }
        Vec3 floor = ground.add(0.0, 0.06, 0.0);
        if (age < 18.0) {
            double on = age < 8.0 ? 1.0 : 1.0 - (age - 8.0) / 10.0;
            int flick = seed + (int) (age * 1.5);
            for (int k = 0; k < 10; k++) {
                double angle = Math.PI * 2.0 * k / 10.0 + Noise.of(seed, k, 1) * 0.5;
                double reach = radius * (0.25 + 0.4 * Noise.of(seed, k, 2));
                Vec3 end = new Vec3(c.x + Math.cos(angle) * reach, ground.y + 0.1, c.z + Math.sin(angle) * reach);
                Bolts.jag(painter, LOOK, off(c, end, clear), end, 12, 1.2, 0.2, on, flick + k * 7);
            }
            double run = Ease.smooth(Math.min(1.0, age / 3.0));
            for (int k = 0; k < TENDRILS; k++) {
                tendril(painter, floor, radius, run, on, seed + k * 13, k, flick);
            }
        }
        if (age < 10.0) {
            double on = 1.0 - age / 10.0;
            int flick = seed + (int) (age * 1.5);
            for (int k = 0; k < 5; k++) {
                Vec3 end = c.add(Noise.direction(seed, k + 20).multiply(6.0, 0.0, 6.0))
                        .add(0.0, 8.0 + 6.0 * Noise.of(seed, k, 3), 0.0);
                Bolts.jag(painter, LOOK, off(c, end, clear), end, 9, 1.0, 0.12, 0.8 * on, flick + k * 11);
            }
        }
        double mist = Ease.smooth(Math.min(1.0, age / 5.0)) * (1.0 - Ease.smooth((age - 10.0) / 32.0));
        if (mist > 0.0) {
            double wide = radius * (0.9 + 0.4 * Ease.smooth(Math.min(1.0, age / 30.0)));
            Vec3 middle = ground.lerp(c, 0.35);
            Vec3 off = eye.subtract(middle);
            double deep = new Vec3(off.x / wide, off.y / (wide * 0.45), off.z / wide).length();
            mist *= 1.0 - 0.65 * (1.0 - Ease.smooth((deep - 0.7) / 0.3));
            painter.haze(middle, EAST.scale(wide), UP.scale(wide * 0.45), SOUTH.scale(wide), LIGHTNING.glow(),
                    0.3 * mist);
            painter.haze(middle, EAST.scale(wide * 0.6), UP.scale(wide * 0.35), SOUTH.scale(wide * 0.6),
                    LIGHTNING.mass(), 0.22 * mist);
        }
        if (age < 16.0) {
            double u = age / 16.0;
            painter.circle(floor, EAST, SOUTH, 0.5 + radius * 1.4 * Ease.smooth(u), 0.25, 1.6,
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

    // `from`, moved `by` blocks towards `to` (at most halfway).
    private static Vec3 off(Vec3 from, Vec3 to, double by) {
        Vec3 way = to.subtract(from);
        double length = way.length();
        return by <= 0.0 || length < 1.0E-6 ? from : from.add(way.scale(Math.min(by, length * 0.5) / length));
    }

    // His sky shockwave: a shell of lightning bursting out round him to `radius`, arcs crackling out to it.
    private static void dome(ConstructPainter painter, Vec3 c, double radius, double age, int seed) {
        painter.material(LIGHTNING);
        double size = 0.6 + (radius - 0.6) * Ease.smooth(Math.min(1.0, age / 5.0));
        double on = 1.0 - Ease.smooth((age - 4.0) / 10.0);
        painter.shell(c, size, LIGHTNING.glow(), 0.9 * on);
        painter.haze(c, EAST.scale(size), UP.scale(size), SOUTH.scale(size), LIGHTNING.glow(), 0.25 * on);
        if (age < 3.0) {
            painter.flare(c, 2.0 + 3.0 * (1.0 - age / 3.0), 1.0 - age / 3.0);
        }
        int flick = seed + (int) (age * 1.5);
        for (int k = 0; k < 10; k++) {
            Bolts.jag(painter, LOOK, c, c.add(Noise.direction(seed, k + 40).scale(size)), 8, size * 0.15, 0.08, on,
                    flick + k * 5);
        }
    }

    // A tendril of the burst's lightning running out low over the ground, `run` of the way to its end, forking twice
    // on its way. Kept low: lightning running over the ground hugs it.
    private static void tendril(ConstructPainter painter, Vec3 floor, double radius, double run, double on, int seed,
            int k, int flick) {
        double angle = Math.PI * 2.0 * k / TENDRILS + Noise.of(seed, k, 3) * 0.4;
        double reach = radius * (0.9 + 0.6 * Noise.of(seed, k, 4)) * run;
        if (reach < 0.5 || on <= 0.01) {
            return;
        }
        Vec3 way = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
        Vec3 start = floor.add(way.scale(0.6)).add(0.0, 0.25, 0.0);
        Vec3 end = floor.add(way.scale(reach)).add(0.0, 0.1, 0.0);
        double bright = on * (0.75 + 0.25 * Noise.of(seed, flick, 6));
        List<Vec3> path = low(Bolts.jagged(start, end, 16, reach * 0.12, seed + flick), floor.y);
        Bolts.path(painter, LOOK, path, 0.09, bright);
        for (int f = 0; f < 2; f++) {
            Vec3 from = path.get(path.size() * (1 + f) / 3);
            double side = (f == 0 ? 1.0 : -1.0) * (0.5 + 0.4 * Noise.of(seed, f, 8));
            Vec3 tip = from.add(Math.cos(angle + side) * reach * 0.35, 0.0, Math.sin(angle + side) * reach * 0.35);
            Bolts.path(painter, LOOK, low(Bolts.jagged(from, tip, 8, reach * 0.06, seed + f + flick), floor.y), 0.06,
                    0.8 * bright);
        }
    }

    // A path pressed down onto the ground at `y`, its ups and downs kept small.
    static List<Vec3> low(List<Vec3> path, double y) {
        List<Vec3> pressed = new ArrayList<>(path.size());
        for (Vec3 point : path) {
            pressed.add(new Vec3(point.x, y + 0.12 + Math.min(0.5, Math.abs(point.y - y - 0.15) * 0.3), point.z));
        }
        return pressed;
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        LIVE.clear();
    }
}
