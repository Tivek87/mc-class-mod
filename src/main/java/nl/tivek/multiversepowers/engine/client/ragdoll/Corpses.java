package nl.tivek.multiversepowers.engine.client.ragdoll;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import nl.tivek.multiversepowers.config.client.ClientSettings;
import nl.tivek.multiversepowers.engine.client.render.EntityPass;
import nl.tivek.multiversepowers.engine.physics.Blocks;
import org.slf4j.Logger;

// The bodies left lying where they fell once their creatures are gone: each lies there as long as the player set, and
// never less than LIE_LEAST, counted from when it came to lie, then sinks into the ground and puffs away.
final class Corpses {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SINK_TICKS = 40;
    private static final double SINK_SPEED = 0.035;
    // Ticks a body lies at least before it sinks away, however short the time the player set; one that never comes
    // to lie goes this long after that.
    static final int LIE_LEAST = 100;
    private static final int NEVER_LAY = 600;

    static final List<Ragdoll> ALL = new ArrayList<>();
    private static PoseStack stack = new PoseStack();
    // The body being drawn now.
    @Nullable
    static Ragdoll drawing;

    private Corpses() {
    }

    static void add(Ragdoll doll) {
        doll.entity.deathTime = 0;
        doll.entity.hurtTime = 0;
        doll.entity.setSharedFlagOnFire(false);
        ALL.add(doll);
    }

    static void blast(ClientLevel level, Vec3 center, double power, RandomSource random) {
        for (Ragdoll doll : ALL) {
            if (doll.sunk < 0) {
                doll.blast(center, power, RagdollCauses.seen(level, center, doll.coreAt(1.0)), random);
            }
        }
    }

    // How far a body has sunk into the ground this frame (blocks).
    static double sink(Ragdoll doll, float partialTick) {
        return doll.sunk < 0 ? 0.0 : (doll.sunk + partialTick) * SINK_SPEED;
    }

    static void tick(ClientLevel level, Vec3 camera, double far, int now, int substeps, Blocks blocks,
            RandomSource random) {
        double keep = Math.max(LIE_LEAST, ClientSettings.get(ClientSettings.CORPSE_SECONDS) * 20.0);
        Iterator<Ragdoll> bodies = ALL.iterator();
        while (bodies.hasNext()) {
            Ragdoll doll = bodies.next();
            if (doll.sunk >= 0) {
                if (++doll.sunk >= SINK_TICKS) {
                    poof(level, doll, random);
                    bodies.remove();
                }
                continue;
            }
            if (doll.coreAt(1.0).distanceToSqr(camera) > far) {
                bodies.remove();
                continue;
            }
            if (doll.rested >= keep || now - doll.dead >= keep + NEVER_LAY) {
                doll.sunk = 0;
                continue;
            }
            RagdollCrowd.among(doll);
            doll.step(substeps, blocks);
            RagdollFalls.settle(doll, now);
        }
    }

    // Makes room for another: the body that has lain longest sinks away, if one has lain long enough.
    static boolean makeRoom() {
        Ragdoll lain = null;
        for (Ragdoll doll : ALL) {
            if (doll.sunk < 0 && doll.rested >= LIE_LEAST && (lain == null || doll.rested > lain.rested)) {
                lain = doll;
            }
        }
        if (lain == null) {
            return false;
        }
        lain.sunk = 0;
        return true;
    }

    // How many lie there, not counting those already sinking away.
    static int lying() {
        int n = 0;
        for (Ragdoll doll : ALL) {
            n += doll.sunk < 0 ? 1 : 0;
        }
        return n;
    }

    private static void poof(ClientLevel level, Ragdoll doll, RandomSource random) {
        Vec3 at = doll.coreAt(1.0);
        double wide = doll.entity.getBbWidth();
        for (int i = 0; i < 20; i++) {
            level.addParticle(ParticleTypes.POOF, at.x + (random.nextDouble() - 0.5) * wide * 2.0,
                    at.y + random.nextDouble() * 0.5, at.z + (random.nextDouble() - 0.5) * wide * 2.0,
                    random.nextGaussian() * 0.02, random.nextGaussian() * 0.02, random.nextGaussian() * 0.02);
        }
    }

    // Draws every body in view after the world's creatures; `settle` puts back what drawing one changed in its model.
    static void draw(RenderLevelStageEvent event, Runnable settle) {
        if (ALL.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 camera = event.getCamera().getPosition();
        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        Ragdoll broken = null;
        EntityPass.inWorld(true);
        try {
            for (Ragdoll doll : ALL) {
                Vec3 at = doll.coreAt(partialTick);
                if (!event.getFrustum().isVisible(new AABB(at.x - 2.0, at.y - 2.0, at.z - 2.0, at.x + 2.0, at.y + 2.0,
                        at.z + 2.0))) {
                    continue;
                }
                LivingEntity body = doll.entity;
                // The removed creature is drawn where its body lies, its eyes on the body so it takes the light there;
                // sinking moves only the drawn body, or it would take the dark of the ground it sinks into.
                body.setPos(at.x, at.y - body.getEyeHeight(), at.z);
                body.xOld = body.xo = body.getX();
                body.yOld = body.yo = body.getY();
                body.zOld = body.zo = body.getZ();
                drawing = doll;
                try {
                    dispatcher.render(body, body.getX() - camera.x, body.getY() - camera.y, body.getZ() - camera.z,
                            body.getYRot(), partialTick, stack, buffers, dispatcher.getPackedLightCoords(body,
                                    partialTick));
                } catch (RuntimeException e) {
                    LOGGER.warn("A limp body could not be drawn and is let go", e);
                    broken = doll;
                    stack = new PoseStack();
                } finally {
                    drawing = null;
                    settle.run();
                }
            }
        } finally {
            EntityPass.inWorld(false);
        }
        if (broken != null) {
            ALL.remove(broken);
        }
        buffers.endLastBatch();
    }

    static void clear() {
        ALL.clear();
        drawing = null;
    }
}
