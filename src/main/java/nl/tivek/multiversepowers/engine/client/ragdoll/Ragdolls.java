package nl.tivek.multiversepowers.engine.client.ragdoll;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.config.client.ClientSettings;
import nl.tivek.multiversepowers.engine.client.model.ModelParts;
import nl.tivek.multiversepowers.engine.client.render.EntityPass;
import org.joml.Matrix4f;
import org.slf4j.Logger;

// Every limp creature in the player's own game: what makes one go limp (it dies, a power holds it, a blow throws it),
// its body stepping every tick, its parts put in place each time it is drawn, and a dead one's body left lying where
// it fell before it sinks away. Only what this player sees: the server and the other players never know.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class Ragdolls {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final TagKey<EntityType<?>> NEVER = tag("ragdoll_none");
    private static final TagKey<EntityType<?>> STIFF = tag("ragdoll_stiff");
    private static final int SUBSTEPS = 20;
    // How fast a blow must send a creature to throw it limp, in blocks per tick: along the ground, and upward.
    private static final double THROWN = 0.9;
    private static final double TOSSED = 0.6;
    // A creature bigger than this (width squared times height) is too heavy for a blow to throw it limp.
    private static final double HEAVY = 3.5;
    private static final double FASTEST = 40.0;
    // A body that dies standing still tips over sideways as it goes limp (a stiff one falls as one piece), turning
    // this fast (radians per second); one knocked away harder than CALM falls the way it was knocked.
    private static final double TOPPLE = 2.0;
    private static final double TOPPLE_STIFF = 2.4;
    private static final double GIVE_WAY = 1.5;
    private static final double CALM = 2.0;
    // Ticks on the ground, still, before a thrown creature gets up (or, if it never left the ground, before it
    // shakes the blow off); and the longest it may fly limp.
    private static final int LANDED = 3;
    private static final int SHRUG = 10;
    private static final int LONGEST_FLIGHT = 200;
    private static final double GET_UP = 0.1;
    private static final double GO_LIMP = 0.25;
    private static final int SINK_TICKS = 40;
    private static final double SINK_SPEED = 0.035;
    // A limp creature is let go a little further off than where it may go limp, so it does not flicker at the edge.
    private static final double LET_GO = 1.2;

    private static final Int2ObjectOpenHashMap<Ragdoll> LIVE = new Int2ObjectOpenHashMap<>();
    private static final List<Ragdoll> CORPSES = new ArrayList<>();
    private static final IntOpenHashSet HELD = new IntOpenHashSet();
    private static final IntOpenHashSet THROWN_NOW = new IntOpenHashSet();
    private static final IntOpenHashSet FAILED = new IntOpenHashSet();
    private static final Set<Class<?>> UNFIT = new HashSet<>();
    private static final List<Predicate<Entity>> CLAIMS = new ArrayList<>();
    private static final Restore RESTORE = new Restore();
    private static final LevelBlocks BLOCKS = new LevelBlocks();
    private static final RandomSource RANDOM = RandomSource.create();
    private static PoseStack stack = new PoseStack();
    @Nullable
    private static ClientLevel lastLevel;
    private static int ticks;
    @Nullable
    private static Ragdoll drawing;
    @Nullable
    private static Ragdoll corpse;
    private static int deathTime = -1;

    private Ragdolls() {
    }

    private static TagKey<EntityType<?>> tag(String name) {
        return TagKey.create(Registries.ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, name));
    }

    // Something else that poses a creature itself (a power that squashes, strings up or stretches it): while it
    // does, the creature never goes limp.
    public static void claim(Predicate<Entity> claim) {
        CLAIMS.add(claim);
    }

    // The server says a power has picked the creature up, or let it go.
    public static void held(int entity, boolean held) {
        if (held) {
            HELD.add(entity);
        } else {
            HELD.remove(entity);
        }
    }

    // Called right after a creature's model took its own pose for this frame.
    public static void pose(EntityModel<?> model, LivingEntity entity, float partialTick, PoseStack pose) {
        if (!EntityPass.inWorld() || drawing != null) {
            return;
        }
        Ragdoll doll = corpse != null && corpse.entity == entity ? corpse : LIVE.get(entity.getId());
        if (doll != null && doll != corpse && (doll.entity != entity || doll.model != model)) {
            LIVE.remove(entity.getId());
            doll = null;
        }
        Matrix4f drawn = pose.last().pose();
        if (doll == null) {
            doll = start(model, entity, partialTick, drawn);
            if (doll == null) {
                return;
            }
        }
        if (!rigid(drawn) || claimed(entity)) {
            return;
        }
        double sink = doll.sunk < 0 ? 0.0 : (doll.sunk + partialTick) * SINK_SPEED;
        doll.pose(drawn, camera(), partialTick, sink, RESTORE);
        drawing = doll;
        if (doll.state == Ragdoll.State.DEAD) {
            // Its body lies limp instead of turning over, so it is not tinted red for dying either (a blow still is).
            deathTime = entity.deathTime;
            entity.deathTime = 0;
        }
    }

    // Called as any model of the creature being drawn starts drawing: a layer's own copy of the model follows.
    public static void layer(EntityModel<?> model) {
        Ragdoll doll = drawing;
        if (doll != null && model != doll.model) {
            doll.copyTo(model, RESTORE);
        }
    }

    // Whether the creature's body lies limp away from where it stands, so its round shadow there must go.
    public static boolean lying(Entity entity) {
        if (corpse != null && corpse.entity == entity) {
            return true;
        }
        Ragdoll doll = LIVE.get(entity.getId());
        return doll != null && doll.entity == entity && doll.state == Ragdoll.State.DEAD;
    }

    // Whether this dead creature's body stays lying where it fell, so it does not puff away yet.
    public static boolean keepsBody(LivingEntity entity) {
        Ragdoll doll = LIVE.get(entity.getId());
        return doll != null && doll.entity == entity && doll.state == Ragdoll.State.DEAD && ClientSettings.ragdolls()
                && ClientSettings.get(ClientSettings.CORPSE_SECONDS) > 0.0;
    }

    @Nullable
    private static Ragdoll start(EntityModel<?> model, LivingEntity entity, float partialTick, Matrix4f drawn) {
        if (!ClientSettings.ragdolls() || UNFIT.contains(model.getClass()) || FAILED.contains(entity.getId())
                || Ashes.burning(entity.getId())) {
            return null;
        }
        Ragdoll.State state = wanted(entity);
        if (state == null || entity.getType().is(NEVER) || entity.isInvisible() || entity.isSleeping()
                || claimed(entity) || !rigid(drawn)
                || state != Ragdoll.State.DEAD && (entity.isPassenger() || entity.isVehicle())) {
            return null;
        }
        Vec3 camera = camera();
        double reach = ClientSettings.get(ClientSettings.RAGDOLL_REACH);
        double near = entity.distanceToSqr(camera);
        if (near > reach * reach || !room(near, camera)) {
            return null;
        }
        if (!ModelParts.known(model)) {
            UNFIT.add(model.getClass());
            return null;
        }
        List<ModelParts.Part> parts = ModelParts.of(model);
        if (parts == null) {
            FAILED.add(entity.getId());
            return null;
        }
        RagdollProfiles.Profile profile = RagdollProfiles.of(entity.getType());
        if (profile.never()) {
            FAILED.add(entity.getId());
            return null;
        }
        boolean stiff = entity.getType().is(STIFF) || profile.stiff();
        Vec3 velocity = velocity(entity);
        Ragdoll doll = Ragdoll.build(entity, model, parts, new Matrix4f(drawn), camera, partialTick, state, stiff,
                profile, velocity);
        if (state == Ragdoll.State.DEAD) {
            doll.dead = ticks;
            // Its flames would stand on where it stood, not on the body.
            entity.clearFire();
            entity.setSharedFlagOnFire(false);
            // It falls to one side, turning about the line it faces along.
            double yaw = Math.toRadians(entity.yBodyRot);
            double side = RANDOM.nextBoolean() ? 1.0 : -1.0;
            double ax = -Math.sin(yaw) * side;
            double az = Math.cos(yaw) * side;
            if (velocity.horizontalDistance() < CALM) {
                double spin = stiff ? TOPPLE_STIFF : TOPPLE;
                doll.tip(ax * spin, 0.0, az * spin, entity.getPosition(partialTick));
            }
            if (!stiff) {
                doll.giveWay(RANDOM, ax, az, GIVE_WAY);
            }
        }
        LIVE.put(entity.getId(), doll);
        return doll;
    }

    @Nullable
    private static Ragdoll.State wanted(LivingEntity entity) {
        if (entity.isDeadOrDying()) {
            return Ragdoll.State.DEAD;
        }
        if (HELD.contains(entity.getId())) {
            return Ragdoll.State.HELD;
        }
        return THROWN_NOW.contains(entity.getId()) ? Ragdoll.State.FLYING : null;
    }

    private static boolean claimed(Entity entity) {
        for (Predicate<Entity> claim : CLAIMS) {
            if (claim.test(entity)) {
                return true;
            }
        }
        return false;
    }

    // Only a creature drawn at its true size and shape can go limp: not one squashed or stretched by some effect.
    private static boolean rigid(Matrix4f m) {
        float a = m.m00() * m.m00() + m.m01() * m.m01() + m.m02() * m.m02();
        float b = m.m10() * m.m10() + m.m11() * m.m11() + m.m12() * m.m12();
        float c = m.m20() * m.m20() + m.m21() * m.m21() + m.m22() * m.m22();
        if (a < 1.0E-6F || Math.abs(a - b) > 0.1F * a || Math.abs(a - c) > 0.1F * a) {
            return false;
        }
        float ab = m.m00() * m.m10() + m.m01() * m.m11() + m.m02() * m.m12();
        float ac = m.m00() * m.m20() + m.m01() * m.m21() + m.m02() * m.m22();
        float bc = m.m10() * m.m20() + m.m11() * m.m21() + m.m12() * m.m22();
        return Math.abs(ab) < 0.05F * a && Math.abs(ac) < 0.05F * a && Math.abs(bc) < 0.05F * a
                && m.determinant3x3() > 0.0F;
    }

    private static Vec3 velocity(LivingEntity entity) {
        Vec3 moved = new Vec3(entity.getX() - entity.xOld, entity.getY() - entity.yOld, entity.getZ() - entity.zOld);
        Vec3 push = entity.getDeltaMovement();
        Vec3 velocity = (push.lengthSqr() > moved.lengthSqr() ? push : moved).scale(20.0);
        double speed = velocity.length();
        return speed > FASTEST ? velocity.scale(FASTEST / speed) : velocity;
    }

    // A creature a blow or a blast sends flying: just hurt and given a hard push (the server tells its speed at once,
    // while where it is only follows a few ticks later), not one that flies by itself or is too heavy.
    private static boolean thrown(Mob mob) {
        if (mob.hurtTime <= 0) {
            return false;
        }
        Vec3 push = mob.getDeltaMovement();
        if (push.horizontalDistanceSqr() <= THROWN * THROWN && push.y <= TOSSED) {
            return false;
        }
        return !mob.isNoGravity() && !mob.isPassenger() && !mob.isVehicle() && !mob.isInWater() && !mob.isInLava()
                && !(mob instanceof FlyingMob) && !(mob instanceof FlyingAnimal) && !(mob instanceof Bat)
                && mob.getBbWidth() * mob.getBbWidth() * mob.getBbHeight() <= HEAVY;
    }

    // Whether something solid is right under the creature. A client only learns a creature's own onGround() when it
    // moves, so one that stood still since it came into view would never seem to stand.
    private static boolean grounded(Entity entity) {
        return !entity.level().noCollision(entity, entity.getBoundingBox().move(0.0, -0.06, 0.0));
    }

    private static Vec3 camera() {
        return Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
    }

    // Makes room for one more at squared distance `near` by letting the furthest go, if that one is further.
    private static boolean room(double near, Vec3 camera) {
        if (LIVE.size() + CORPSES.size() < ClientSettings.get(ClientSettings.RAGDOLL_MOST)) {
            return true;
        }
        Ragdoll far = furthest(camera);
        if (far == null || far.coreAt(1.0).distanceToSqr(camera) <= near) {
            return false;
        }
        forget(far);
        return true;
    }

    @Nullable
    private static Ragdoll furthest(Vec3 camera) {
        Ragdoll far = null;
        double most = -1.0;
        for (Ragdoll doll : LIVE.values()) {
            double d = doll.coreAt(1.0).distanceToSqr(camera);
            if (d > most) {
                most = d;
                far = doll;
            }
        }
        for (Ragdoll doll : CORPSES) {
            double d = doll.coreAt(1.0).distanceToSqr(camera);
            if (d > most) {
                most = d;
                far = doll;
            }
        }
        return far;
    }

    private static void forget(Ragdoll doll) {
        if (!CORPSES.remove(doll)) {
            LIVE.remove(doll.entity.getId());
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level != lastLevel) {
            clear();
            HELD.clear();
            lastLevel = level;
        }
        if (level == null || minecraft.isPaused()) {
            return;
        }
        ticks++;
        THROWN_NOW.clear();
        if (!ClientSettings.ragdolls()) {
            if (!LIVE.isEmpty() || !CORPSES.isEmpty()) {
                clear();
            }
            return;
        }
        Vec3 camera = camera();
        double reach = ClientSettings.get(ClientSettings.RAGDOLL_REACH);
        double near = reach * reach;
        double far = near * LET_GO * LET_GO;
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof Mob mob && mob.isAlive() && !LIVE.containsKey(mob.getId())
                    && mob.distanceToSqr(camera) <= near && thrown(mob)) {
                THROWN_NOW.add(mob.getId());
            }
        }
        BLOCKS.in(level);
        ObjectIterator<Ragdoll> live = LIVE.values().iterator();
        while (live.hasNext()) {
            Ragdoll doll = live.next();
            LivingEntity entity = doll.entity;
            if (entity.isRemoved() || claimed(entity) || entity.distanceToSqr(camera) > far) {
                live.remove();
                continue;
            }
            if (entity.isDeadOrDying() && doll.state != Ragdoll.State.DEAD) {
                doll.die(ticks);
            }
            if (doll.state != Ragdoll.State.DEAD && !carried(doll, entity)) {
                live.remove();
                continue;
            }
            doll.step(SUBSTEPS, BLOCKS);
        }
        double keep = ClientSettings.get(ClientSettings.CORPSE_SECONDS) * 20.0;
        Iterator<Ragdoll> bodies = CORPSES.iterator();
        while (bodies.hasNext()) {
            Ragdoll doll = bodies.next();
            if (doll.sunk >= 0) {
                if (++doll.sunk >= SINK_TICKS) {
                    poof(level, doll);
                    bodies.remove();
                }
                continue;
            }
            if (doll.coreAt(1.0).distanceToSqr(camera) > far) {
                bodies.remove();
                continue;
            }
            if (ticks - doll.dead >= keep) {
                doll.sunk = 0;
                continue;
            }
            doll.step(SUBSTEPS, BLOCKS);
        }
        int most = ClientSettings.get(ClientSettings.RAGDOLL_MOST);
        while (LIVE.size() + CORPSES.size() > most) {
            Ragdoll furthest = furthest(camera);
            if (furthest == null) {
                break;
            }
            forget(furthest);
        }
    }

    // A living limp creature hangs from what holds it, or flies until it lands and gets up; false once it stands.
    private static boolean carried(Ragdoll doll, LivingEntity entity) {
        boolean held = HELD.contains(entity.getId());
        doll.state = held ? Ragdoll.State.HELD : Ragdoll.State.FLYING;
        doll.follow(entity, !held);
        if (held) {
            doll.ending = false;
            doll.still = 0;
        } else {
            boolean grounded = grounded(entity);
            doll.flew |= !grounded;
            boolean resting = entity.isInWater() || entity.isInLava()
                    || Math.abs(entity.getX() - entity.xOld) + Math.abs(entity.getZ() - entity.zOld) < 0.1
                            && grounded;
            if (resting) {
                doll.still++;
            } else {
                doll.still = 0;
                if (entity instanceof Mob mob && thrown(mob)) {
                    doll.ending = false;
                }
            }
            if (doll.still >= (doll.flew ? LANDED : SHRUG) || doll.age > LONGEST_FLIGHT) {
                doll.ending = true;
            }
        }
        if (doll.ending) {
            doll.limp -= GET_UP;
            return doll.limp > 0.0;
        }
        doll.limp = Math.min(1.0, doll.limp + GO_LIMP);
        return true;
    }

    private static void poof(ClientLevel level, Ragdoll doll) {
        Vec3 at = doll.coreAt(1.0);
        double wide = doll.entity.getBbWidth();
        for (int i = 0; i < 20; i++) {
            level.addParticle(ParticleTypes.POOF, at.x + (RANDOM.nextDouble() - 0.5) * wide * 2.0,
                    at.y + RANDOM.nextDouble() * 0.5, at.z + (RANDOM.nextDouble() - 0.5) * wide * 2.0,
                    RANDOM.nextGaussian() * 0.02, RANDOM.nextGaussian() * 0.02, RANDOM.nextGaussian() * 0.02);
        }
    }

    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }
        Entity entity = event.getEntity();
        int id = entity.getId();
        HELD.remove(id);
        FAILED.remove(id);
        Ragdoll doll = LIVE.get(id);
        if (doll == null || doll.entity != entity) {
            return;
        }
        LIVE.remove(id);
        if (doll.state == Ragdoll.State.DEAD && entity.isRemoved() && ClientSettings.ragdolls()
                && ClientSettings.get(ClientSettings.CORPSE_SECONDS) > 0.0) {
            doll.entity.deathTime = 0;
            doll.entity.hurtTime = 0;
            doll.entity.setSharedFlagOnFire(false);
            CORPSES.add(doll);
        }
    }

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        if (drawing != null && drawing.entity == event.getEntity()) {
            settle();
        }
    }

    // A body lying where it fell has no name over it.
    @SubscribeEvent
    public static void onNameTag(RenderNameTagEvent event) {
        if (corpse != null && event.getEntity() == corpse.entity) {
            event.setCanRender(TriState.FALSE);
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) {
            settle();
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || CORPSES.isEmpty()) {
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
            for (Ragdoll doll : CORPSES) {
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
                corpse = doll;
                try {
                    dispatcher.render(body, body.getX() - camera.x, body.getY() - camera.y, body.getZ() - camera.z,
                            body.getYRot(), partialTick, stack, buffers, dispatcher.getPackedLightCoords(body,
                                    partialTick));
                } catch (RuntimeException e) {
                    LOGGER.warn("A limp body could not be drawn and is let go", e);
                    broken = doll;
                    stack = new PoseStack();
                } finally {
                    corpse = null;
                    settle();
                }
            }
        } finally {
            EntityPass.inWorld(false);
        }
        if (broken != null) {
            CORPSES.remove(broken);
        }
        buffers.endLastBatch();
    }

    // Puts back what the creature just drawn had changed in its shared model.
    private static void settle() {
        if (drawing == null) {
            return;
        }
        RESTORE.undo();
        if (deathTime >= 0) {
            drawing.entity.deathTime = deathTime;
        }
        drawing = null;
        deathTime = -1;
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
        HELD.clear();
    }

    private static void clear() {
        settle();
        LIVE.clear();
        CORPSES.clear();
        THROWN_NOW.clear();
        FAILED.clear();
    }
}
