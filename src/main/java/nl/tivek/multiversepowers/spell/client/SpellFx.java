package nl.tivek.multiversepowers.spell.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.render.Material;
import nl.tivek.multiversepowers.spell.SpellFxPayload;

// Every spell effect the server told of, drawn each frame until it is over.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class SpellFx {
    private static final int MOST = 64;
    private static final int TRAIL = 14;
    private static final int LOST_AFTER = 3;
    private static final Material PLAIN = new Material(0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF);

    static final class Fx {
        final SpellFxPayload said;
        final double born;
        final ArrayDeque<Vec3> trail = new ArrayDeque<>();
        Vec3 at;
        Vec3 was;
        int lost;
        boolean over;

        Fx(SpellFxPayload said, double born) {
            this.said = said;
            this.born = born;
            this.at = said.from();
            this.was = said.from();
        }

        int kind() {
            return this.said.kind();
        }

        int seed() {
            return this.said.seed();
        }
    }

    private static final List<Fx> LIVE = new ArrayList<>();

    private SpellFx() {
    }

    public static void add(SpellFxPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        if (LIVE.size() >= MOST) {
            LIVE.remove(0);
        }
        LIVE.add(new Fx(payload, level.getGameTime()));
    }

    private static int life(Fx fx) {
        return switch (fx.kind()) {
            case SpellFxPayload.FIREBALL -> 200;
            case SpellFxPayload.FIRE_BURST -> FireFx.BURST;
            case SpellFxPayload.STORM, SpellFxPayload.VIAL -> fx.said.ticks();
            case SpellFxPayload.BOLT -> StormFx.BOLT;
            case SpellFxPayload.ARC -> StormFx.ARC;
            case SpellFxPayload.POISON -> fx.said.ticks();
            case SpellFxPayload.GUST -> WindFx.LIFE;
            case SpellFxPayload.VOID_IN -> VoidFx.IN;
            case SpellFxPayload.VOID_OUT -> VoidFx.OUT;
            case SpellFxPayload.AMBUSH -> VoidFx.STRIKE;
            case SpellFxPayload.CLAP -> ClapFx.LIFE;
            default -> 0;
        };
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.isPaused() || LIVE.isEmpty()) {
            return;
        }
        double now = level.getGameTime();
        Iterator<Fx> all = LIVE.iterator();
        while (all.hasNext()) {
            Fx fx = all.next();
            if (fx.kind() == SpellFxPayload.FIREBALL) {
                follow(level, fx);
            }
            Particles.tick(level, fx, now - fx.born);
            if (fx.over || now - fx.born > life(fx)) {
                all.remove();
            }
        }
    }

    // The fireball rides on the game's own projectile: its place is taken from it every tick, with a trail behind.
    private static void follow(ClientLevel level, Fx fx) {
        Entity ball = level.getEntity(fx.said.entity());
        if (ball == null || !ball.isAlive()) {
            if (++fx.lost > LOST_AFTER) {
                fx.over = true;
            }
            return;
        }
        fx.lost = 0;
        fx.was = fx.at;
        fx.at = ball.getBoundingBox().getCenter();
        fx.trail.addFirst(fx.at);
        while (fx.trail.size() > TRAIL) {
            fx.trail.removeLast();
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
                (float) (now % 24000.0), event.getFrustum(), PLAIN);
        for (Fx fx : LIVE) {
            double age = now - fx.born;
            if (age < 0.0) {
                continue;
            }
            switch (fx.kind()) {
                case SpellFxPayload.FIREBALL -> FireFx.fly(painter, fx, partialTick, age);
                case SpellFxPayload.FIRE_BURST -> FireFx.burst(painter, fx.said.from(), age, fx.seed());
                case SpellFxPayload.STORM -> StormFx.charge(painter, fx.said.from(), age, fx.said.ticks(), fx.seed());
                case SpellFxPayload.BOLT -> StormFx.bolt(painter, fx.said.from(), fx.said.to(), age, fx.seed());
                case SpellFxPayload.ARC -> StormFx.arc(painter, fx.said.from(), fx.said.to(), age, fx.seed());
                case SpellFxPayload.VIAL -> PoisonFx.vial(painter, fx.said.from(), fx.said.to(), age,
                        fx.said.ticks());
                case SpellFxPayload.POISON -> PoisonFx.cloud(painter, fx.said.from(), fx.said.to().x, age,
                        fx.said.ticks(), fx.seed());
                case SpellFxPayload.GUST -> WindFx.gust(painter, fx.said.from(), fx.said.to(), age, fx.seed());
                case SpellFxPayload.VOID_IN -> VoidFx.enter(painter, fx.said.from(), age, fx.seed());
                case SpellFxPayload.VOID_OUT -> VoidFx.leave(painter, fx.said.from(), age, fx.seed());
                case SpellFxPayload.AMBUSH -> VoidFx.strike(painter, fx.said.from(), fx.said.to(), age, fx.seed());
                case SpellFxPayload.CLAP -> ClapFx.draw(painter, fx.said.from(), fx.said.to(), age, fx.seed());
                default -> {
                }
            }
        }
        painter.finish(minecraft.renderBuffers().bufferSource());
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        LIVE.clear();
    }
}
