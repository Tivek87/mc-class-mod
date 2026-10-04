package nl.tivek.multiversepowers.character.thor.storm.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.thor.ThorStatePayload;
import nl.tivek.multiversepowers.character.thor.client.ClientThor;
import nl.tivek.multiversepowers.character.thor.client.motion.ThorRise;
import nl.tivek.multiversepowers.character.thor.storm.LightningBomb;
import nl.tivek.multiversepowers.engine.client.render.Bolts;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;

// Thor's lightning bomb as everyone sees it until it bursts: static crawling over him and arcs leaping from him down
// to the ground as he rises, then, as it charges, a shell of light closing in on him with lightning drawn into him, a
// glare swelling at his chest and a ring on the ground showing how far the burst will reach. The burst is StormBolts'.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class BombFx {
    private static final double GROUND_LOOK = 14.0;
    private static final int LATE = 8;
    private static final double BURST_NEAR = 6.0;
    private static final RandomSource RANDOM = RandomSource.create();

    private record Bomb(Entity thor, float age) {
    }

    private BombFx() {
    }

    // Every Thor in sight whose bomb has not burst yet, and how far along it is; your own as long as your game lifts you.
    // Its glare holds until the server's burst shows, which comes a little after your own game's count.
    private static List<Bomb> bombs(ClientLevel level, float partialTick) {
        List<Bomb> bombs = new ArrayList<>();
        Entity own = Minecraft.getInstance().player;
        ClientThor.each((id, view) -> {
            float age = view.age(partialTick);
            if (view.move() == ThorStatePayload.BOMB && view.arg() != ThorStatePayload.PUT_OUT
                    && age < LightningBomb.BURST + LATE) {
                Entity thor = level.getEntity(id);
                if (thor != null && (thor != own || ThorRise.active())
                        && !StormBolts.burstNear(thor.position(), BURST_NEAR)) {
                    bombs.add(new Bomb(thor, age));
                }
            }
        });
        return bombs;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.isPaused()) {
            return;
        }
        for (Bomb bomb : bombs(level, 0.0F)) {
            Entity thor = bomb.thor();
            for (int k = 0; k < 2; k++) {
                level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                        thor.getX() + (RANDOM.nextDouble() - 0.5) * thor.getBbWidth() * 1.4,
                        thor.getY() + RANDOM.nextDouble() * thor.getBbHeight(),
                        thor.getZ() + (RANDOM.nextDouble() - 0.5) * thor.getBbWidth() * 1.4, 0.0, 0.05, 0.0);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        List<Bomb> bombs = bombs(level, partialTick);
        if (bombs.isEmpty()) {
            return;
        }
        Camera camera = event.getCamera();
        ConstructPainter painter = new ConstructPainter(event.getPoseStack(), camera.getPosition(),
                (float) ((level.getGameTime() + partialTick) % 24000.0), event.getFrustum(), StormBolts.LIGHTNING);
        for (Bomb bomb : bombs) {
            // Seen from inside his own eyes, what lies on his body would only blind him.
            boolean inside = bomb.thor() == camera.getEntity() && !camera.isDetached();
            draw(painter, level, bomb.thor(), bomb.age(), partialTick, inside);
        }
        painter.finish(minecraft.renderBuffers().bufferSource());
    }

    private static void draw(ConstructPainter painter, ClientLevel level, Entity thor, float age, float partialTick,
            boolean inside) {
        Vec3 feet = thor.getPosition(partialTick);
        Vec3 chest = feet.add(0.0, thor.getBbHeight() * 0.62, 0.0);
        double size = thor.getBbHeight() / 1.8;
        double time = painter.time();
        double rise = Ease.smooth(age / 10.0);
        double charge = Ease.smooth((age - LightningBomb.RISE) / LightningBomb.CHARGE);
        int flick = (int) (time * (2.0 + 3.0 * charge));
        int seed = thor.getId() * 7919;
        Vec3 ground = ground(level, thor, feet);
        painter.material(StormBolts.LIGHTNING);
        if (!inside) {
            int arcs = 3 + (int) (5.0 * charge);
            for (int k = 0; k < arcs; k++) {
                Vec3 a = chest.add(Noise.direction(seed + flick, k).multiply(0.45 * size, 0.9 * size, 0.45 * size));
                Vec3 b = a.add(Noise.direction(seed + flick, k + 40).scale(0.5 * size));
                Bolts.jag(painter, StormBolts.LOOK, a, b, 3, 0.15, 0.03, 0.8 * rise, seed + flick + k);
            }
            painter.glowDisc(chest, (1.0 + 1.6 * charge) * size, StormBolts.LIGHTNING.glow(),
                    (0.2 + 0.4 * charge) * rise, 0.25, seed + flick);
        }
        if (ground != null && age < LightningBomb.RISE + 4) {
            double on = rise * (1.0 - Ease.smooth((age - LightningBomb.RISE) / 4.0));
            for (int k = 0; k < 3; k++) {
                double angle = Noise.of(seed + flick, k, 1) * Math.PI * 2.0;
                double reach = 1.0 + 2.5 * Noise.of(seed + flick, k, 2);
                Vec3 end = new Vec3(feet.x + Math.cos(angle) * reach, ground.y + 0.05, feet.z + Math.sin(angle) * reach);
                Bolts.jag(painter, StormBolts.LOOK, feet.add(0.0, 0.2, 0.0), end, 7, 0.5, 0.05, 0.7 * on,
                        seed + flick * 3 + k);
            }
        }
        if (charge <= 0.0) {
            return;
        }
        double radius = radius();
        double shell = radius * (1.0 - 0.85 * charge);
        painter.shell(chest, shell, StormBolts.LIGHTNING.glow(), 0.6 * charge);
        for (int k = 0; k < 12; k++) {
            Vec3 from = chest.add(Noise.direction(seed, k + 60).scale(shell));
            Bolts.jag(painter, StormBolts.LOOK, from, chest, 6, 0.45, 0.04, 0.6 * charge, seed + flick + k * 13);
        }
        if (!inside) {
            double pulse = 0.85 + 0.15 * Math.sin(time * 1.3);
            painter.flare(chest, (0.6 + 2.2 * charge) * pulse * size, 0.4 + 0.6 * charge);
        }
        if (ground != null) {
            painter.circle(ground.add(0.0, 0.06, 0.0), StormBolts.EAST, StormBolts.SOUTH, radius, 0.08, 0.6,
                    Colors.alpha(0.8 * charge), Colors.alpha(0.4 * charge));
        }
    }

    // The ground under him, if it is near.
    private static Vec3 ground(ClientLevel level, Entity thor, Vec3 feet) {
        HitResult hit = level.clip(new ClipContext(feet.add(0.0, 0.1, 0.0), feet.add(0.0, -GROUND_LOOK, 0.0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, thor));
        return hit.getType() == HitResult.Type.MISS ? null : hit.getLocation();
    }

    private static double radius() {
        CharacterAbility bomb = GameCharacter.THOR.byName("lightning_bomb");
        return bomb == null ? 8.0 : bomb.value("radius");
    }
}
