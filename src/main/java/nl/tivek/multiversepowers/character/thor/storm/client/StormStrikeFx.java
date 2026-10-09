package nl.tivek.multiversepowers.character.thor.storm.client;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.thor.ThorBlow;
import nl.tivek.multiversepowers.character.thor.client.ClientThor;
import nl.tivek.multiversepowers.character.thor.storm.StormFxPayload;
import nl.tivek.multiversepowers.engine.client.ragdoll.Ragdolls;
import nl.tivek.multiversepowers.engine.client.render.Bolts;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;

// A Storm Throw as everyone sees it, as in God of War: while he holds the hammer high a blue star of light gathers in
// it; hurled, it streaks down as a bolt that grows behind it with the star at its head; where it strikes a small fan
// of light and a spray of lightning burst up out of the ground, and lightning runs out over the ground in a wide V,
// its arms more to the sides than ahead, and crackles there a while as it fades. Never a ring or a round glow.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
final class StormStrikeFx {
    static final int LIFE = 56;
    // Its bolt blazes this long once it reached the ground.
    private static final int AFTER = 10;
    private static final int RAYS = 9;
    private static final int FOUNTAIN = 7;
    // Lightning along each arm of the V, and CRAWL - 2 shorter runs beside them.
    private static final int CRAWL = 6;
    // Where the raised hammer's head is over his feet, and out to his left, times his size.
    private static final double RAISED_UP = 2.5;
    private static final double RAISED_LEFT = 0.35;
    private static final RandomSource RANDOM = RandomSource.create();

    private StormStrikeFx() {
    }

    static int hurlLife(StormFxPayload said) {
        return (int) Math.ceil(flight(said)) + AFTER;
    }

    // How long the hammer takes to fly the bolt's length.
    private static double flight(StormFxPayload said) {
        return Math.max(1.0, said.from().distanceTo(said.to()) / Math.max(0.5, said.size()));
    }

    // The bolt behind the hurled hammer: it grows from his hand down to where the hammer is, brightest at its head,
    // with the star there and forks flickering off it; at the ground it blazes once and fades.
    static void hurl(ConstructPainter painter, Vec3 eye, StormFxPayload said, double age) {
        Vec3 from = said.from();
        Vec3 to = said.to();
        double length = from.distanceTo(to);
        if (length < 0.5) {
            return;
        }
        painter.material(StormBolts.LIGHTNING);
        double reach = Math.min(1.0, age / flight(said));
        double after = age - flight(said);
        double on = after < 0.0 ? 1.0 : Math.max(0.0, 1.0 - after / AFTER);
        double wide = after < 0.0 ? 1.0 : 1.0 + 0.8 * on;
        int glow = StormBolts.LIGHTNING.mass();
        int core = StormBolts.LIGHTNING.hot();
        int flick = said.seed() + (int) (age * 2.0);
        List<Vec3> channel = Bolts.jagged(from, to, 24, Math.min(2.0, length * 0.06), said.seed());
        int n = channel.size();
        double tipAt = reach * (n - 1);
        int last = Math.min(n - 2, (int) Math.floor(tipAt));
        Vec3 tip = channel.get(last).lerp(channel.get(last + 1), Math.min(1.0, tipAt - last));
        for (int i = 1; i < n && i - 1 <= tipAt; i++) {
            Vec3 a = channel.get(i - 1);
            Vec3 b = i <= tipAt ? channel.get(i) : tip;
            double behind = Math.max(0.0, reach - (double) i / (n - 1));
            double bright = on * (after < 0.0 ? 1.0 - 0.45 * Math.min(1.0, behind * 3.0) : 1.0);
            painter.glowTaper(a, b, 1.1 * wide, 1.1 * wide, glow, 0.5 * bright, 0.5 * bright);
            painter.lightTaper(a, b, 0.18 * wide, 0.18 * wide, core, 0.95 * bright, 0.95 * bright);
            painter.glowTaper(a, b, 3.5 * wide, 3.5 * wide, StormBolts.LOOK.halo(), 0.12 * bright, 0.12 * bright);
        }
        for (int k = 0; k < 5; k++) {
            Vec3 start = channel.get(Math.min(n - 1, (int) (Noise.of(said.seed(), k, 31) * reach * (n - 1))));
            Vec3 out = Noise.direction(flick, k + 3).scale(1.0 + 2.0 * Noise.of(said.seed(), k, 32));
            Bolts.jag(painter, StormBolts.LOOK, start, start.add(out), 5, 0.4, 0.05, 0.7 * on, flick + k * 13);
        }
        if (after < 0.0) {
            star(painter, eye, tip, 1.0, 1.0);
        } else {
            painter.flare(to.add(0.0, 0.3, 0.0), 1.4 * on, on);
        }
    }

    // Where it struck, at `from`: the V opens towards `to`, `size` radians either side of the way there.
    static void strike(ConstructPainter painter, Vec3 eye, StormFxPayload said, double age) {
        Vec3 c = said.from();
        Vec3 flat = new Vec3(said.to().x - c.x, 0.0, said.to().z - c.z);
        double length = flat.length();
        if (length < 0.5) {
            return;
        }
        painter.material(StormBolts.LIGHTNING);
        Vec3 ahead = flat.scale(1.0 / length);
        Vec3 side = ahead.cross(StormBolts.UP);
        double half = said.size();
        int seed = said.seed();
        int flick = seed + (int) (age * 1.5);
        // Struck right by your eyes, its glare thins out, or it would white the whole view out.
        double inCore = 1.0 - Ease.smooth((eye.distanceTo(c) - 1.5) / 3.0);
        if (age < 4.0) {
            double f = 1.0 - age / 4.0;
            painter.flare(c.add(0.0, 0.4, 0.0), 2.2 * f, f * (1.0 - 0.7 * inCore));
        }
        if (age < 10.0) {
            fan(painter, c, ahead, side, half, length, age, seed, 1.0 - 0.6 * inCore);
        }
        if (age < 11.0) {
            fountain(painter, c, ahead, side, half, age, seed, flick);
        }
        Vec3 floor = c.add(0.0, 0.06, 0.0);
        double run = Ease.smooth(Math.min(1.0, age / 6.0));
        double fade = age < 16.0 ? 1.0 : Math.max(0.0, 1.0 - (age - 16.0) / (LIFE - 16.0));
        for (int k = 0; k < CRAWL && fade > 0.0; k++) {
            double arm = k % 2 == 0 ? -1.0 : 1.0;
            double s = k < 2 ? arm : arm * (0.88 + 0.1 * Noise.of(seed, k, 21));
            double reach = length * (k < 2 ? 1.0 : 0.5 + 0.3 * Noise.of(seed, k, 22)) * run;
            double bright = fade * (0.65 + 0.35 * Noise.of(seed, flick, k));
            crawl(painter, floor, along(ahead, side, s * half), reach, bright, seed + k * 17, flick);
        }
    }

    // The fan of light: a few shafts bursting up and out of the ground over the V, thin where it struck and wider
    // and faint where they end, shot out in a moment and fading.
    private static void fan(ConstructPainter painter, Vec3 c, Vec3 ahead, Vec3 side, double half, double length,
            double age, int seed, double glare) {
        double grow = Ease.smooth(Math.min(1.0, age / 2.0));
        double on = (1.0 - Ease.smooth((age - 2.0) / 8.0)) * glare;
        for (int k = 0; k < RAYS; k++) {
            double s = k / (RAYS - 1.0) * 2.0 - 1.0 + (Noise.of(seed, k, 1) - 0.5) * 0.15;
            double lift = 0.25 + 0.9 * Noise.of(seed, k, 2);
            double reach = length * (0.3 + 0.25 * Noise.of(seed, k, 3)) * grow;
            Vec3 end = c.add(along(ahead, side, s * half).scale(Math.cos(lift) * reach))
                    .add(0.0, Math.sin(lift) * reach, 0.0);
            double bright = on * (0.6 + 0.4 * Noise.of(seed, k, 4));
            painter.lightTaper(c, end, 0.08, 0.5 * grow, StormBolts.CORE, 0.9 * bright, 0.0);
            painter.glowTaper(c, end, 0.3, 1.2 * grow, StormBolts.LIGHTNING.glow(), 0.4 * bright, 0.0);
        }
    }

    // The fountain: lightning sprays up and out of where it struck, over the V, each spray forking once.
    private static void fountain(ConstructPainter painter, Vec3 c, Vec3 ahead, Vec3 side, double half, double age,
            int seed, int flick) {
        double on = Ease.smooth(Math.min(1.0, age / 1.5)) * (1.0 - Ease.smooth((age - 4.0) / 7.0));
        for (int k = 0; k < FOUNTAIN; k++) {
            double lift = 0.55 + 0.8 * Noise.of(seed, k, 12);
            double tall = 1.5 + 2.0 * Noise.of(seed, k, 13);
            Vec3 start = c.add(ahead.scale(0.6 * Noise.of(seed, k, 14)));
            Vec3 end = start.add(along(ahead, side, (Noise.of(seed, k, 11) * 2.0 - 1.0) * half)
                    .scale(Math.cos(lift) * tall)).add(0.0, Math.sin(lift) * tall, 0.0);
            double bright = on * (0.7 + 0.3 * Noise.of(seed, flick, k));
            Bolts.jag(painter, StormBolts.LOOK, start, end, 10, tall * 0.16, 0.09, bright, flick + k * 7);
            Vec3 middle = start.lerp(end, 0.5);
            Vec3 twig = middle.add(Noise.direction(seed, k + 50).scale(tall * 0.35)).add(0.0, tall * 0.15, 0.0);
            Bolts.jag(painter, StormBolts.LOOK, middle, twig, 5, tall * 0.08, 0.05, 0.7 * bright, flick + k * 7 + 3);
        }
    }

    // Lightning running out low over the ground along `way`, `reach` blocks, forking three times on its way.
    private static void crawl(ConstructPainter painter, Vec3 floor, Vec3 way, double reach, double bright, int seed,
            int flick) {
        if (reach < 0.5 || bright <= 0.01) {
            return;
        }
        Vec3 start = floor.add(way.scale(0.5)).add(0.0, 0.2, 0.0);
        Vec3 end = floor.add(way.scale(reach)).add(0.0, 0.1, 0.0);
        List<Vec3> path = StormBolts.low(Bolts.jagged(start, end, 18, reach * 0.1, seed + flick), floor.y);
        Bolts.path(painter, StormBolts.LOOK, path, 0.1, bright);
        for (int f = 0; f < 3; f++) {
            Vec3 from = path.get(path.size() * (1 + f) / 4);
            double turn = (f % 2 == 0 ? 1.0 : -1.0) * (0.5 + 0.5 * Noise.of(seed, f, 8));
            Vec3 twist = new Vec3(way.x * Math.cos(turn) - way.z * Math.sin(turn), 0.0,
                    way.x * Math.sin(turn) + way.z * Math.cos(turn));
            Vec3 tip = from.add(twist.scale(reach * (0.2 + 0.15 * Noise.of(seed, f, 9))));
            Bolts.path(painter, StormBolts.LOOK, StormBolts.low(Bolts.jagged(from, tip, 8, reach * 0.05,
                    seed + f + flick), floor.y), 0.06, 0.8 * bright);
        }
    }

    // `ahead` turned `angle` radians towards `side`, level.
    private static Vec3 along(Vec3 ahead, Vec3 side, double angle) {
        return ahead.scale(Math.cos(angle)).add(side.scale(Math.sin(angle)));
    }

    // A star of light: a round glare with a long level streak through it, as a lens sees a blinding point.
    private static void star(ConstructPainter painter, Vec3 eye, Vec3 at, double size, double strength) {
        painter.flare(at, 0.9 * size, strength);
        painter.glowDisc(at, 2.8 * size, StormBolts.LIGHTNING.glow(), 0.35 * strength, 0.15, 7);
        Vec3 level = at.subtract(eye).cross(StormBolts.UP);
        if (level.lengthSqr() < 1.0E-6) {
            return;
        }
        Vec3 streak = level.normalize().scale(5.0 * size);
        for (int way = -1; way <= 1; way += 2) {
            Vec3 end = at.add(streak.scale(way));
            painter.lightTaper(at, end, 0.35 * size, 0.0, StormBolts.CORE, 0.9 * strength, 0.0);
            painter.glowTaper(at, at.add(streak.scale(1.4 * way)), size, 0.0, StormBolts.LIGHTNING.glow(),
                    0.4 * strength, 0.0);
        }
    }

    // The moment it strikes: a flash and a jolt for whoever stands near, the crack, sparks and smoke thrown up, limp
    // bodies flung.
    static void struck(ClientLevel level, StormFxPayload said, double far) {
        Vec3 c = said.from();
        boolean close = far < StormBolts.CLOSE;
        if (far < 96.0) {
            level.setSkyFlashTime(2);
        }
        StormBolts.near(far, 32.0, close ? 0.1F : 0.3F, 1.8F, 10);
        level.playLocalSound(c.x, c.y, c.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER, 2.5F,
                0.75F + RANDOM.nextFloat() * 0.15F, false);
        level.playLocalSound(c.x, c.y, c.z, SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 1.5F, 1.1F,
                false);
        StormBolts.puffs(level, ParticleTypes.ELECTRIC_SPARK, c.add(0.0, 0.3, 0.0), close ? 6 : 20, 0.7, 0.5);
        if (!close) {
            StormBolts.puffs(level, ParticleTypes.LARGE_SMOKE, c, 4, 0.5, 0.06);
        }
        Ragdolls.blast(c, 2.0);
        Ragdolls.blast(c.lerp(said.to(), 0.5), 1.2);
    }

    // While a Thor holds the hammer high to hurl it, a blue star of light gathers in it, ever brighter; not in your
    // own first-person view, right over your eyes, where it would white the view out.
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || level == null) {
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Camera camera = event.getCamera();
        ConstructPainter painter = null;
        for (Player player : level.players()) {
            double raised = raised(ClientThor.view(player), partialTick);
            if (raised <= 0.0 || player.isInvisible() || player == camera.getEntity() && !camera.isDetached()) {
                continue;
            }
            if (painter == null) {
                painter = new ConstructPainter(event.getPoseStack(), camera.getPosition(),
                        (float) ((level.getGameTime() + partialTick) % 24000.0), event.getFrustum(),
                        StormBolts.LIGHTNING);
            }
            double size = player.getScale();
            Vec3 ahead = Vec3.directionFromRotation(0.0F, player.yBodyRot);
            Vec3 at = player.getPosition(partialTick).add(0.0, RAISED_UP * size, 0.0)
                    .add(ahead.cross(StormBolts.UP).normalize().scale(-RAISED_LEFT * size));
            star(painter, camera.getPosition(), at, 0.6 + 0.9 * raised, raised);
        }
        if (painter != null) {
            painter.finish(minecraft.renderBuffers().bufferSource());
        }
    }

    // How far the storm has gathered in a Thor's hammer held high to hurl (0 to 1), or 0 while he holds none up.
    private static double raised(@Nullable ClientThor.View view, float partialTick) {
        if (view == null || view.blow != ThorBlow.STORM_THROW.ordinal()) {
            return 0.0;
        }
        float age = view.blowAge(partialTick);
        int hit = ThorBlow.STORM_THROW.hit();
        return age >= hit ? 0.0 : Ease.smooth(age / (hit * 0.8));
    }
}
