package nl.tivek.multiversepowers.engine.client.ragdoll;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
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
import nl.tivek.multiversepowers.engine.client.model.BentParts;
import nl.tivek.multiversepowers.engine.client.model.ModelParts;
import nl.tivek.multiversepowers.engine.client.render.entity.EntityPass;
import nl.tivek.multiversepowers.engine.client.world.LevelBlocks;
import nl.tivek.multiversepowers.engine.entity.impact.ImpactPayload;
import org.joml.Matrix4f;

// Every limp creature in the player's own game: what makes one go limp (it dies, a power holds it, a blow throws it),
// its body stepping every tick, its parts put in place each time it is drawn, and a dead one's body left lying where
// it fell before it sinks away. Only what this player sees: the server and the other players never know.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class Ragdolls {
    private static final TagKey<EntityType<?>> NEVER = tag("ragdoll_none");
    private static final TagKey<EntityType<?>> STIFF = tag("ragdoll_stiff");
    private static final int SUBSTEPS = 20;
    static final int FAR_SUBSTEPS = 10;
    // A limp creature is let go a little further off than where it may go limp, so it does not flicker at the edge.
    private static final double LET_GO = 1.2;

    private static final Int2ObjectOpenHashMap<Ragdoll> LIVE = new Int2ObjectOpenHashMap<>();
    private static final IntOpenHashSet HELD = new IntOpenHashSet();
    private static final IntOpenHashSet THROWN_NOW = new IntOpenHashSet();
    private static final IntOpenHashSet FAILED = new IntOpenHashSet();
    private static final Set<Class<?>> UNFIT = new HashSet<>();
    private static final List<Predicate<Entity>> CLAIMS = new ArrayList<>();
    private static final Restore RESTORE = new Restore();
    private static final LevelBlocks BLOCKS = new LevelBlocks();
    private static final RagdollSteps STEPS = new RagdollSteps();
    private static final RandomSource RANDOM = RandomSource.create();
    @Nullable
    private static ClientLevel lastLevel;
    private static int ticks;
    @Nullable
    private static Ragdoll drawing;
    private static int deathTime = -1;

    private Ragdolls() {
    }

    // A blast of the given power at center, as the game counts an explosion's power (TNT is 4, it reaches twice that
    // in blocks): limp creatures and bodies near are thrown away from it, and creatures it pushes hard enough go limp
    // and fly.
    public static void blast(Vec3 center, double power) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || power <= 0.0 || !ClientSettings.ragdolls()) {
            return;
        }
        for (Ragdoll doll : LIVE.values()) {
            if (doll.state != Ragdoll.State.HELD) {
                doll.blast(center, power, RagdollCauses.seen(level, center, doll.coreAt(1.0)), RANDOM);
            }
        }
        Corpses.blast(level, center, power, RANDOM);
        RagdollCauses.blast(level, center, power, ticks, id -> !LIVE.containsKey(id));
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
        Ragdoll corpse = Corpses.drawing;
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
        if (!RagdollCauses.rigid(drawn) || claimed(entity)) {
            return;
        }
        doll.pose(drawn, camera(), partialTick, Corpses.sink(doll, partialTick), RESTORE);
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
        if (Corpses.drawing != null && Corpses.drawing.entity == entity) {
            return true;
        }
        Ragdoll doll = LIVE.get(entity.getId());
        return doll != null && doll.entity == entity && (doll.state == Ragdoll.State.DEAD
                || doll.state == Ragdoll.State.FLYING && doll.phase != Ragdoll.Phase.AIR);
    }

    // Whether the creature's limp body is in view, though its own box may not be: an arm or a head can lie outside it.
    public static boolean inView(Entity entity, Frustum frustum) {
        Ragdoll doll = LIVE.get(entity.getId());
        if (doll == null || doll.entity != entity) {
            return false;
        }
        Vec3 at = doll.coreAt(1.0);
        return frustum.isVisible(new AABB(at.x - 2.0, at.y - 2.0, at.z - 2.0, at.x + 2.0, at.y + 2.0, at.z + 2.0));
    }

    // Whether this dead creature's body stays lying where it fell, so it does not puff away yet.
    public static boolean keepsBody(LivingEntity entity) {
        Ragdoll doll = LIVE.get(entity.getId());
        return doll != null && doll.entity == entity && doll.state == Ragdoll.State.DEAD && ClientSettings.ragdolls();
    }

    @Nullable
    private static Ragdoll start(EntityModel<?> model, LivingEntity entity, float partialTick, Matrix4f drawn) {
        if (!ClientSettings.ragdolls() || UNFIT.contains(model.getClass()) || FAILED.contains(entity.getId())
                || Ashes.burning(entity.getId())) {
            return null;
        }
        Ragdoll.State state = wanted(entity);
        if (state == null || entity.getType().is(NEVER) || entity.isInvisible() || entity.isSleeping()
                || claimed(entity) || !RagdollCauses.rigid(drawn)
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
        boolean blasted = state != Ragdoll.State.HELD && RagdollCauses.nearBlast(entity);
        Vec3 velocity = blasted ? Vec3.ZERO : RagdollCauses.velocity(entity);
        Ragdoll doll = RagdollBuild.build(entity, model, parts, new Matrix4f(drawn), camera,
                entity.getPosition(partialTick), state, stiff, profile, velocity);
        if (blasted && entity.level() instanceof ClientLevel level) {
            RagdollCauses.throwByBlasts(doll, level, RANDOM);
        }
        RagdollCauses.Hit hit = state == Ragdoll.State.FLYING ? RagdollCauses.hit(entity.getId()) : null;
        if (hit != null) {
            RagdollFalls.hit(doll, hit.at(), hit.way(), hit.strength(), hit.reaction());
        }
        // Limp, it no longer faces any way of its own.
        Facings.forget(entity.getId());
        if (state == Ragdoll.State.DEAD) {
            doll.dead = ticks;
            // Its flames would stand on where it stood, not on the body.
            entity.clearFire();
            entity.setSharedFlagOnFire(false);
            RagdollFalls.die(doll, entity.yBodyRot, velocity, stiff, ticks, RagdollCauses.blow(entity.getId()));
        }
        LIVE.put(entity.getId(), doll);
        return doll;
    }

    // The server tells how the blow that killed a creature struck it: from where (null: from nowhere in particular),
    // how hard it pushed (blocks a tick) and where on the body it landed (null: not known). A body gone limp just now
    // is pushed by it at once; one not yet gone limp is when it does.
    public static void struck(int entity, @Nullable Vec3 from, Vec3 push, @Nullable Vec3 at) {
        if (!ClientSettings.ragdolls()) {
            return;
        }
        RagdollCauses.Blow blow = new RagdollCauses.Blow(from, push, at, ticks);
        Ragdoll doll = LIVE.get(entity);
        if (doll != null && doll.state == Ragdoll.State.DEAD && ticks - doll.dead <= RagdollCauses.BLOW_TICKS) {
            RagdollFalls.strike(doll, blow);
            return;
        }
        RagdollCauses.told(entity, blow);
    }

    // The server tells how a creature took a blow it lived through (Staggers): a body limp already takes it where it
    // landed, and one about to go limp (tripped, knocked off its feet) as it does.
    public static void impact(int entity, Vec3 at, Vec3 way, float strength, ImpactPayload.Reaction reaction) {
        if (!ClientSettings.ragdolls() || reaction == ImpactPayload.Reaction.FLINCH
                || reaction == ImpactPayload.Reaction.STAGGER) {
            return;
        }
        Ragdoll doll = LIVE.get(entity);
        if (doll != null && doll.state != Ragdoll.State.DEAD) {
            RagdollFalls.hit(doll, at, way, strength, reaction);
            return;
        }
        RagdollCauses.told(entity, new RagdollCauses.Hit(at, way, strength, reaction, ticks));
    }

    // The server tells that a falling creature caught hold of an edge for `ticks` (0: it lets go).
    public static void ledge(int entity, Vec3 edge, float nx, float nz, int ticks, boolean branch) {
        Ragdoll doll = LIVE.get(entity);
        if (doll == null || doll.state == Ragdoll.State.DEAD) {
            return;
        }
        if (ticks > 0) {
            doll.grab(edge, nx, nz, ticks, branch);
        } else {
            doll.letGo(doll.entity);
        }
    }

    @Nullable
    private static Ragdoll.State wanted(LivingEntity entity) {
        if (entity.isDeadOrDying()) {
            return Ragdoll.State.DEAD;
        }
        if (HELD.contains(entity.getId())) {
            return Ragdoll.State.HELD;
        }
        return THROWN_NOW.contains(entity.getId()) || RagdollCauses.blown(entity.getId(), ticks)
                || Knocked.down(entity.getId()) ? Ragdoll.State.FLYING : null;
    }

    // The creature's limp body now, if it has one.
    @Nullable
    static Ragdoll live(LivingEntity entity) {
        Ragdoll doll = LIVE.get(entity.getId());
        return doll != null && doll.entity == entity ? doll : null;
    }

    // Whether a power has the creature: holding it, or posing or drawing it in a way of its own.
    public static boolean taken(Entity entity) {
        return HELD.contains(entity.getId()) || claimed(entity);
    }

    static boolean claimed(Entity entity) {
        for (Predicate<Entity> claim : CLAIMS) {
            if (claim.test(entity)) {
                return true;
            }
        }
        return false;
    }

    private static Vec3 camera() {
        return Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
    }

    // Makes room for one more at squared distance `near`: the body that has lain longest sinks away, if one has lain
    // still long enough, else the furthest creature still alive is let go, if that one is further. A dead body is never
    // let go before it has lain still long enough.
    private static boolean room(double near, Vec3 camera) {
        if (counted() < ClientSettings.get(ClientSettings.RAGDOLL_MOST) || Corpses.makeRoom(SUBSTEPS, BLOCKS)) {
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
            if (doll.state != Ragdoll.State.DEAD && d > most) {
                most = d;
                far = doll;
            }
        }
        return far;
    }

    // Whether a body this far (squared) from the camera moves in full detail, with thuds; further ones step with
    // fewer substeps.
    static boolean detailed(double distanceSqr) {
        double detail = ClientSettings.get(ClientSettings.RAGDOLL_DETAIL);
        return distanceSqr <= detail * detail;
    }

    // Every limp creature and body but those already sinking away.
    private static int counted() {
        return LIVE.size() + Corpses.lying();
    }

    private static void forget(Ragdoll doll) {
        LIVE.remove(doll.entity.getId());
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
        Knocked.tick();
        Facings.tick();
        THROWN_NOW.clear();
        RagdollCauses.forget(ticks);
        if (!ClientSettings.ragdolls()) {
            if (!LIVE.isEmpty() || !Corpses.ALL.isEmpty()) {
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
                    && mob.distanceToSqr(camera) <= near && RagdollCauses.thrown(mob)) {
                THROWN_NOW.add(mob.getId());
            }
        }
        BLOCKS.in(level);
        RagdollCrowd.gather(LIVE.values(), Corpses.ALL);
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
                RagdollCauses.Blow blow = RagdollCauses.blow(entity.getId());
                if (blow != null) {
                    RagdollFalls.strike(doll, blow);
                }
            }
            if (doll.state != Ragdoll.State.DEAD && !carried(doll, entity)) {
                live.remove();
                continue;
            }
            RagdollCrowd.among(doll);
            doll.tone(ticks);
            boolean detailed = detailed(entity.distanceToSqr(camera));
            STEPS.add(doll, detailed ? SUBSTEPS : FAR_SUBSTEPS, detailed);
        }
        Corpses.tick(level, camera, far, SUBSTEPS, BLOCKS, RANDOM, STEPS);
        STEPS.run(level);
        boolean thuds = ClientSettings.ragdollThuds();
        for (int i = 0; i < STEPS.size(); i++) {
            Ragdoll doll = STEPS.doll(i);
            if (STEPS.detailed(i) && thuds) {
                doll.thud(ticks, RANDOM);
            }
            if (doll.state == Ragdoll.State.DEAD) {
                RagdollFalls.settle(doll, ticks);
            }
        }
        STEPS.clear();
        int most = ClientSettings.get(ClientSettings.RAGDOLL_MOST);
        while (counted() > most) {
            if (Corpses.makeRoom(SUBSTEPS, BLOCKS)) {
                continue;
            }
            Ragdoll furthest = furthest(camera);
            if (furthest == null) {
                break;
            }
            forget(furthest);
        }
    }

    // A living limp creature hangs from what holds it; one thrown flies along with its creature until it comes down,
    // then lies where it fell and gets up (RagdollMotor.carry). False once it stands again.
    private static boolean carried(Ragdoll doll, LivingEntity entity) {
        boolean held = HELD.contains(entity.getId());
        // Thrown again, not by the blow that just slammed it into a wall; or knocked down again as it gets up.
        boolean again = !held && doll.age > 2 && !RagdollFalls.slamming(doll) && entity instanceof Mob mob
                && (RagdollCauses.thrown(mob) || RagdollCauses.blown(mob.getId(), ticks));
        return doll.carry(entity, held, again, !held && Knocked.again(entity.getId()));
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
        Knocked.forget(id);
        Facings.forget(id);
        Ragdoll doll = LIVE.get(id);
        if (doll == null || doll.entity != entity) {
            return;
        }
        LIVE.remove(id);
        if (doll.state == Ragdoll.State.DEAD && entity.isRemoved() && ClientSettings.ragdolls()) {
            Corpses.add(doll);
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
        if (Corpses.drawing != null && event.getEntity() == Corpses.drawing.entity) {
            event.setCanRender(TriState.FALSE);
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) {
            settle();
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            Corpses.draw(event, Ragdolls::settle);
        }
    }

    // Puts back what the creature just drawn had changed in its shared model.
    private static void settle() {
        if (drawing == null) {
            return;
        }
        RESTORE.undo();
        BentParts.clear();
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
        Corpses.clear();
        THROWN_NOW.clear();
        RagdollCauses.clear();
        FAILED.clear();
        Knocked.clear();
        Facings.clear();
    }
}
