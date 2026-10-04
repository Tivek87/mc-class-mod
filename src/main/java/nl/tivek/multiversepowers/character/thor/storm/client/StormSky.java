package nl.tivek.multiversepowers.character.thor.storm.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.thor.ThorStatePayload;
import nl.tivek.multiversepowers.character.thor.client.ClientThor;
import nl.tivek.multiversepowers.character.thor.storm.ThorStorm;
import nl.tivek.multiversepowers.engine.client.render.Bolts;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.world.LocalWeather;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;

// The storm over every Thor in sight who called one up: a dark cloud that gathers out of a bolt from his raised hand,
// turns slowly over him (the middle fastest) and follows him, lit from inside by lightning crawling along its underside,
// with grey curtains of rain hanging from it; for whoever stands under it the game's own rain, dark sky and rumbling
// (LocalWeather). Each game moves it after him the way the server does (ThorStorm.follow).
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class StormSky {
    private static final int PUFFS = 108;
    private static final int SHREDS = 14;
    private static final int CURTAINS = 10;
    private static final int DARK = 0x262D36;
    private static final int STORM = 0x353E4A;
    private static final int GREY = 0x515C69;
    private static final int RAIN = 0x5E6B7A;
    private static final RandomSource RANDOM = RandomSource.create();

    private static final class Storm {
        final int seed;
        final double radius;
        final List<Flicker> flickers = new ArrayList<>();
        Vec3 center;
        Vec3 before;
        int age;
        int ended = -1;

        Storm(int seed, double radius, Vec3 center) {
            this.seed = seed;
            this.radius = radius;
            this.center = center;
            this.before = center;
        }

        // How far it has gathered, or cleared away again, at this moment: 0 to 1.
        double strength(float partialTick) {
            double age = this.age + partialTick;
            double gathered = Ease.smooth(age / ThorStorm.GATHER);
            return this.ended < 0 ? gathered : gathered * (1.0 - Ease.smooth((age - this.ended) / ThorStorm.CLEAR));
        }

        Vec3 center(float partialTick) {
            return this.before.lerp(this.center, partialTick);
        }
    }

    // Lightning crawling along the cloud's underside for a few ticks.
    private record Flicker(Vec3 from, Vec3 to, int born, int life, int seed) {
    }

    private static final Int2ObjectOpenHashMap<Storm> STORMS = new Int2ObjectOpenHashMap<>();

    static {
        LocalWeather.add(StormSky::rain);
    }

    private StormSky() {
    }

    // Under a storm it rains: fully under its middle, less towards its edge, none above it.
    private static float rain(ClientLevel level, Vec3 at) {
        double most = 0.0;
        for (Storm storm : STORMS.values()) {
            if (at.y > storm.center.y + 4.0) {
                continue;
            }
            double far = Math.sqrt(Mth.square(at.x - storm.center.x) + Mth.square(at.z - storm.center.z));
            double under = 1.0 - Ease.smooth((far - storm.radius * 0.7) / (storm.radius * 0.6));
            most = Math.max(most, under * storm.strength(0.0F));
        }
        return (float) most;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.isPaused()) {
            return;
        }
        ClientThor.each((id, view) -> {
            Storm storm = STORMS.get(id.intValue());
            Entity thor = level.getEntity(id);
            if (view.has(ThorStatePayload.STORMING) && thor != null && (storm == null || storm.ended >= 0)) {
                STORMS.put(id.intValue(), new Storm(RANDOM.nextInt(), radius(),
                        ThorStorm.cloudAt(level, thor.position())));
            }
        });
        var all = STORMS.int2ObjectEntrySet().iterator();
        while (all.hasNext()) {
            Int2ObjectMap.Entry<Storm> entry = all.next();
            Storm storm = entry.getValue();
            Entity thor = level.getEntity(entry.getIntKey());
            storm.before = storm.center;
            if (thor != null) {
                storm.center = ThorStorm.follow(storm.center, ThorStorm.cloudAt(level, thor.position()));
            }
            storm.age++;
            if (storm.ended < 0 && (thor == null || !ClientThor.has(thor, ThorStatePayload.STORMING))) {
                storm.ended = storm.age;
            }
            if (storm.ended >= 0 && storm.age - storm.ended > ThorStorm.CLEAR) {
                all.remove();
                continue;
            }
            flicker(level, storm);
        }
    }

    private static double radius() {
        CharacterAbility storm = GameCharacter.THOR.byName(ThorStorm.ABILITY);
        return storm == null ? 24.0 : storm.value("radius");
    }

    // Now and then lightning crawls along its underside, at times with a far rumble.
    private static void flicker(ClientLevel level, Storm storm) {
        storm.flickers.removeIf(flicker -> storm.age - flicker.born() > flicker.life());
        double strength = storm.strength(0.0F);
        if (strength < 0.5 || storm.flickers.size() >= 4 || RANDOM.nextFloat() > 0.12F * strength) {
            return;
        }
        double angle = RANDOM.nextDouble() * Math.PI * 2.0;
        double reach = Math.sqrt(RANDOM.nextDouble()) * storm.radius * 0.8;
        Vec3 from = storm.center.add(Math.cos(angle) * reach, -0.5, Math.sin(angle) * reach);
        double way = RANDOM.nextDouble() * Math.PI * 2.0;
        double length = 5.0 + RANDOM.nextDouble() * 9.0;
        Vec3 to = from.add(Math.cos(way) * length, (RANDOM.nextDouble() - 0.5) * 1.5, Math.sin(way) * length);
        storm.flickers.add(new Flicker(from, to, storm.age, 3 + RANDOM.nextInt(4), RANDOM.nextInt()));
        if (RANDOM.nextFloat() < 0.35F) {
            level.playLocalSound(from.x, from.y, from.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 3.0F,
                    0.5F + RANDOM.nextFloat() * 0.2F, false);
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        // After the game's own clouds and rain, or its white clouds would cover the storm.
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER || STORMS.isEmpty()) {
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
                (float) (now % 24000.0), event.getFrustum(), StormBolts.LIGHTNING);
        // Lit by the sun: grey by day, near black at night.
        double sun = Mth.clamp(Mth.cos(level.getTimeOfDay(partialTick) * Mth.TWO_PI) * 2.0F + 0.5F, 0.0F, 1.0F);
        double shade = 0.3 + 0.7 * sun;
        for (Int2ObjectMap.Entry<Storm> entry : STORMS.int2ObjectEntrySet()) {
            Storm storm = entry.getValue();
            double strength = storm.strength(partialTick);
            if (strength <= 0.0) {
                continue;
            }
            Vec3 c = storm.center(partialTick);
            Entity thor = level.getEntity(entry.getIntKey());
            calling(painter, storm, thor, c, partialTick);
            curtains(painter, level, storm, c, strength, shade);
            cloud(painter, storm, c, strength, shade);
            lightning(painter, storm, c, strength, partialTick);
        }
        painter.finish(minecraft.renderBuffers().bufferSource());
        // The game draws the world border next, the way it set up for its rain: put that back.
        RenderTarget weather = minecraft.levelRenderer.getWeatherTarget();
        if (weather != null) {
            weather.bindWrite(false);
        } else {
            RenderSystem.depthMask(false);
        }
    }

    // Rolling puffs: a dark underside over a wide flat disc, thickest in the middle, and over it towers billowing up,
    // highest over the middle and greyer the higher they reach; all turn round the middle (fastest there) and slowly
    // swell and shrink, and ragged shreds hang under it, drifting faster. While it gathers it spreads out from the
    // middle; clearing, it thins away.
    private static void cloud(ConstructPainter painter, Storm storm, Vec3 c, double strength, double shade) {
        double time = painter.time();
        double spread = 0.25 + 0.75 * strength;
        for (int k = 0; k < PUFFS; k++) {
            double out = Math.sqrt(Noise.of(storm.seed, k, 1));
            double middle = 1.0 - out;
            double angle = Noise.of(storm.seed, k, 2) * Math.PI * 2.0 + time * 0.004 * (1.0 + 2.5 * middle);
            boolean tower = k % 3 == 0;
            double reach = out * storm.radius * (tower ? 0.8 : 1.1) * spread;
            double lift = tower ? 1.5 + 7.0 * middle * Noise.of(storm.seed, k, 3) * spread
                    : (Noise.of(storm.seed, k, 3) - 0.5) * 3.0 * (0.6 + middle) - 1.5 * middle * middle;
            Vec3 at = c.add(Math.cos(angle) * reach, lift, Math.sin(angle) * reach);
            double swell = 1.0 + 0.12 * Math.sin(time * 0.03 + k * 1.7);
            double size = (4.0 + 5.0 * Noise.of(storm.seed, k, 4) + 2.0 * middle) * (0.4 + 0.6 * strength) * swell;
            float high = (float) Mth.clamp((lift + 2.0) / 10.0, 0.0, 1.0);
            int rgb = Colors.mix(k % 2 == 0 ? DARK : STORM, GREY, high);
            painter.lightDisc(at, size, Colors.shade(rgb, shade), (tower ? 0.7 : 0.8) * strength, 0.35,
                    storm.seed + k + (int) (time * 0.1));
        }
        for (int k = 0; k < SHREDS; k++) {
            double angle = Noise.of(storm.seed, k, 31) * Math.PI * 2.0 + time * 0.012;
            double reach = Math.sqrt(Noise.of(storm.seed, k, 32)) * storm.radius * 0.9 * spread;
            Vec3 at = c.add(Math.cos(angle) * reach, -2.5 - 2.0 * Noise.of(storm.seed, k, 33), Math.sin(angle) * reach);
            painter.lightDisc(at, (2.5 + 2.0 * Noise.of(storm.seed, k, 34)) * strength, Colors.shade(DARK, shade),
                    0.5 * strength, 0.6, storm.seed + k * 3 + 500 + (int) (time * 0.2));
        }
    }

    // Grey curtains of rain hanging from its underside to the ground, drifting round with it and coming and going.
    private static void curtains(ConstructPainter painter, ClientLevel level, Storm storm, Vec3 c, double strength,
            double shade) {
        double time = painter.time();
        int rgb = Colors.shade(RAIN, shade);
        for (int k = 0; k < CURTAINS; k++) {
            double life = 160.0 + 120.0 * Noise.of(storm.seed, k, 5);
            double phase = (time + Noise.of(storm.seed, k, 6) * life) / life;
            int round = (int) Math.floor(phase);
            double shown = Math.sin(Math.PI * (phase - round)) * strength;
            if (shown <= 0.02) {
                continue;
            }
            double angle = Noise.of(storm.seed + round, k, 7) * Math.PI * 2.0 + time * 0.004;
            double reach = Math.sqrt(Noise.of(storm.seed + round, k, 8)) * storm.radius * 0.8;
            double x = c.x + Math.cos(angle) * reach;
            double z = c.z + Math.sin(angle) * reach;
            double ground = Math.min(c.y - 2.0, level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x),
                    Mth.floor(z)));
            Vec3 top = new Vec3(x, c.y - 1.0, z);
            Vec3 bottom = new Vec3(x + 1.5, ground, z + 0.8);
            double width = 4.0 + 5.0 * Noise.of(storm.seed + round, k, 9);
            painter.lightTaper(top, bottom, width, width * 1.3, rgb, 0.3 * shown, 0.08 * shown);
        }
    }

    // Lightning crawling along its underside, lighting the cloud round it from inside.
    private static void lightning(ConstructPainter painter, Storm storm, Vec3 c, double strength, float partialTick) {
        for (Flicker flicker : storm.flickers) {
            double age = storm.age + partialTick - flicker.born();
            double on = strength * (1.0 - age / (flicker.life() + 1.0)) * (0.7 + 0.3 * Noise.of(flicker.seed(),
                    (int) (age * 2.0), 1));
            if (on <= 0.0) {
                continue;
            }
            Vec3 drift = c.subtract(storm.center);
            Vec3 from = flicker.from().add(drift);
            Vec3 to = flicker.to().add(drift);
            painter.glowDisc(from.lerp(to, 0.5).add(0.0, 1.0, 0.0), 6.0 + from.distanceTo(to) * 0.5,
                    StormBolts.LIGHTNING.glow(), 0.5 * on, 0.4, flicker.seed());
            Bolts.jag(painter, StormBolts.LOOK, from, to, 8, 1.1, 0.1, on, flicker.seed() + (int) (age * 1.5));
        }
    }

    // As it gathers, a bolt runs up out of his raised hand into the middle of the cloud.
    private static void calling(ConstructPainter painter, Storm storm, Entity thor, Vec3 c, float partialTick) {
        double age = storm.age + partialTick;
        if (thor == null || storm.ended >= 0 || age > 32.0) {
            return;
        }
        double on = Ease.smooth((age - 4.0) / 3.0) * (1.0 - Ease.smooth((age - 24.0) / 8.0));
        if (on <= 0.0) {
            return;
        }
        Vec3 hand = thor.getPosition(partialTick).add(0.0, thor.getBbHeight() + 0.6, 0.0);
        painter.material(StormBolts.LIGHTNING);
        Bolts.jag(painter, StormBolts.LOOK, hand, c, 18, 1.4, 0.16, on, storm.seed + (int) (age * 1.5));
        painter.flare(hand, 0.8 * on, on);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        STORMS.clear();
    }
}
