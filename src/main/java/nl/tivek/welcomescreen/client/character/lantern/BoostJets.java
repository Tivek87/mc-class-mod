package nl.tivek.welcomescreen.client.character.lantern;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import nl.tivek.welcomescreen.WelcomeScreenMod;

/**
 * At top speed (see {@link ClientFlight#boosting}) two chains of hard light shoot out of Green Lantern's waist, back
 * and out to his left and to his right, and at the end of each a small fighter jet of hard light grows out of the
 * ring's light, flying along with him, the turbo booster at its back glowing and roaring out a flame. The jets lean
 * a little behind him into his turns and bob on their chains. Once he drops below top speed they break into solid
 * pieces, and so do the chains. Everyone around sees them; all of it is constructs, save the flames.
 */
@EventBusSubscriber(modid = WelcomeScreenMod.MODID, value = Dist.CLIENT)
public final class BoostJets {
    // How long the chains take to run out of his waist, when the jets start to grow at their ends and how long that
    // takes, and how long everything takes to break up once he drops below top speed, in ticks.
    private static final float CHAIN_TICKS = 7.0F;
    private static final float JET_FROM = 5.0F;
    private static final float JET_TICKS = 9.0F;
    private static final float BREAK_TICKS = 16.0F;
    // Where the jets hang from his waist, in blocks: back, out to either side, and up.
    private static final double BACK = 2.6;
    private static final double OUT = 1.8;
    private static final double LIFT = 0.3;
    // How big the jets are (next to the small jet, two blocks long), how long a link of the chains is and how far the
    // chains sag, in blocks.
    private static final double JET_SCALE = 1.2;
    private static final double LINK = 0.36;
    private static final double SAG = 0.3;
    // Where the chains hook onto a jet: its nose, along its own z at scale 1.
    private static final double NOSE = 0.95;
    // How quickly the way the jets fly follows his: a little behind, so they swing out in his turns.
    private static final double FOLLOW = 0.45;
    // His waist: how high the pivot of his body is (a part of his height), and how far his waist is from it along the
    // body, which lies along the way he flies at top speed.
    private static final double PIVOT = 0.55;
    private static final double WAIST = 0.29;
    // How long the flames of the boosters are, at full thrust.
    private static final double FLAME = 1.7;
    /** A small jet, two blocks long at scale 1: x to its right, y up, z ahead. */
    private static final ConstructPainter.Shape SMALL_JET = ConstructPainter.Shape.of(smallJet());
    // Where the booster at the back of the small jet is, along z at scale 1, and how wide its nozzle is.
    private static final double SMALL_NOZZLE_Z = -1.02;
    private static final double SMALL_NOZZLE = 0.16;

    private static final Map<Integer, State> STATES = new HashMap<>();
    private static int clientTicks;

    private BoostJets() {
    }

    /**
     * The small jet: a pointed body with its canopy, swept wings, two tail fins leaning outwards, and the ring of its
     * turbo booster at its back.
     */
    private static Mesh[] smallJet() {
        Mesh body = Mesh.loft(14, 1.0, new double[] { 1.02, 0.0, 0.0, -0.01 }, new double[] { 0.86, 0.045, 0.04, 0.0 },
                new double[] { 0.55, 0.095, 0.085, 0.01 }, new double[] { 0.1, 0.115, 0.095, 0.0 },
                new double[] { -0.6, 0.11, 0.085, 0.0 }, new double[] { -0.95, 0.085, 0.07, 0.0 },
                new double[] { -1.0, 0.075, 0.065, 0.0 });
        Mesh canopy = Mesh.loft(10, 1.55, new double[] { 0.72, 0.0, 0.0, 0.06 },
                new double[] { 0.58, 0.045, 0.045, 0.08 }, new double[] { 0.38, 0.05, 0.05, 0.085 },
                new double[] { 0.24, 0.0, 0.0, 0.07 });
        Mesh wing = Mesh.wing(0.62, -0.62, 0.22, -0.55, -0.4, -0.02, 0.05, 0.012, 1.0).moved(0.08, -0.02, 0.0);
        Mesh fin = Mesh.wing(0.34, -0.98, -0.55, -1.0, -0.84, 0.0, 0.035, 0.01, 1.0).turned(0.0, 0.0, 1.0, 72.0)
                .moved(0.05, 0.05, 0.0);
        Mesh booster = Mesh.torus(14, 5, SMALL_NOZZLE, 0.03, 1.6).alongZ().moved(0.0, 0.0, SMALL_NOZZLE_Z);
        return new Mesh[] { body, canopy, wing, wing.mirrored(), fin, fin.mirrored(), booster };
    }

    /** One flyer's jets: whether they are there, since when (client ticks), and the way they fly. */
    private static final class State {
        boolean on;
        int since;
        Vec3 way;
        Vec3 wayO;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.isPaused()) {
            return;
        }
        clientTicks++;
        for (AbstractClientPlayer player : level.players()) {
            boolean want = ClientFlight.boosting(player) && !player.isInvisible();
            State state = STATES.get(player.getId());
            if (state == null) {
                if (!want) {
                    continue;
                }
                state = new State();
                STATES.put(player.getId(), state);
            }
            if (state.on != want) {
                state.on = want;
                state.since = clientTicks;
                Vec3 at = player.position();
                if (want) {
                    level.playLocalSound(at.x, at.y, at.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.7F,
                            1.7F, false);
                    level.playLocalSound(at.x, at.y, at.z, SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.0F, 0.8F,
                            false);
                } else {
                    level.playLocalSound(at.x, at.y, at.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS,
                            0.9F, 1.1F, false);
                }
            }
            Vec3 moving = ClientFlight.velocity(player, 1.0F);
            Vec3 way = moving.lengthSqr() > 1.0E-4 ? moving.normalize()
                    : state.way != null ? state.way : player.getLookAngle();
            state.wayO = state.way == null ? way : state.way;
            state.way = state.way == null ? way : state.way.lerp(way, FOLLOW).normalize();
        }
        STATES.entrySet().removeIf(entry -> level.getEntity(entry.getKey()) == null
                || !entry.getValue().on && clientTicks - entry.getValue().since > BREAK_TICKS);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        STATES.clear();
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || STATES.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Camera camera = event.getCamera();
        float time = (float) (level.getGameTime() % 24000L) + partialTick;
        ConstructPainter painter = new ConstructPainter(event.getPoseStack(), camera.getPosition(), time,
                event.getFrustum());
        for (Map.Entry<Integer, State> entry : STATES.entrySet()) {
            Entity player = level.getEntity(entry.getKey());
            State state = entry.getValue();
            if (player == null || state.way == null) {
                continue;
            }
            Vec3 way = state.wayO.lerp(state.way, partialTick);
            way = way.lengthSqr() < 1.0E-6 ? state.way : way.normalize();
            draw(painter, player, state, way, clientTicks - state.since + partialTick, time, partialTick);
        }
        painter.finish(minecraft.renderBuffers().bufferSource());
    }

    /** One flyer's two chains and the jets on them. */
    private static void draw(ConstructPainter painter, Entity player, State state, Vec3 way, float age, float time,
            float partialTick) {
        Vec3 waist = player.getPosition(partialTick).add(0.0, player.getBbHeight() * PIVOT, 0.0)
                .subtract(way.scale(WAIST));
        Vec3 side = way.cross(ConstructPainter.UP);
        if (side.lengthSqr() < 1.0E-6) {
            side = Vec3.directionFromRotation(0.0F, player.getViewYRot(partialTick) + 90.0F);
        }
        side = side.normalize();
        double chain = state.on ? ConstructPainter.smooth(age / CHAIN_TICKS) : 1.0;
        double grown = state.on ? ConstructPainter.backOut((age - JET_FROM) / JET_TICKS) : 1.0;
        double apart = state.on ? 0.0 : Mth.clamp(age / BREAK_TICKS, 0.0, 1.0);
        for (int k = -1; k <= 1; k += 2) {
            // Bobbing gently on its chain, each to its own beat.
            double bob = 0.12 * Math.sin(time * 0.3 + k * 1.7);
            Vec3 jet = waist.subtract(way.scale(BACK)).add(side.scale(OUT * k)).add(0.0, LIFT + bob, 0.0);
            ConstructPainter.Frame frame = ConstructPainter.Frame.of(jet, way, ConstructPainter.UP,
                    JET_SCALE * Math.max(0.0, grown));
            Vec3 nose = jet.add(way.scale(NOSE * JET_SCALE * Math.max(0.2, grown)));
            painter.chain(waist, nose, SAG, LINK, 1.0, 1.1, chain, apart);
            if (grown <= 0.01) {
                continue;
            }
            if (apart > 0.0) {
                painter.shattered(SMALL_JET, frame, apart, 1.2);
                continue;
            }
            // Fresh out of the ring's light it is white-hot, cooling to green.
            painter.glare(0.6 * (1.0 - ConstructPainter.smooth((age - JET_FROM) / JET_TICKS)));
            painter.shape(SMALL_JET, frame, 1.0, 1.05);
            painter.glare(0.0);
            // The turbo booster at its back glows and roars out a flame.
            double thrust = ConstructPainter.smooth((age - JET_FROM - JET_TICKS * 0.5) / 6.0);
            painter.exhaust(frame.at(0.0, 0.0, SMALL_NOZZLE_Z), way.scale(-1.0), FLAME * JET_SCALE,
                    SMALL_NOZZLE * JET_SCALE * 1.1, thrust);
        }
    }
}
