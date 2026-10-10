package nl.tivek.multiversepowers.character.thor;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.entity.Captives;
import nl.tivek.multiversepowers.engine.entity.HeldMobs;
import nl.tivek.multiversepowers.engine.entity.HeldPlayers;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.math.Vectors;
import nl.tivek.multiversepowers.engine.target.Targeting;
import nl.tivek.multiversepowers.spell.SpellTargets;

// Thor takes hold of a creature by the throat, lifts it, and for a moment (CHOICE) his player picks how it ends: his
// left fist punches it in the body, he throws it away, or he slams it head first into the ground. Picked nothing, it
// is one of a pool: those three, or punches first and then the throw or the slam; grabbed at the end of a grab dash
// (running) he may also leap high with it over his head, then slam down with it, hurl it down, or let it fall and
// slam onto it. A player is held the same way; one that cannot be held (another power holds it) takes it all where it
// stands. Thrown away, it flies as a missile and hurts what it crashes into.
public final class ThorGrab {
    private static final double REACH = 3.5;
    private static final double AHEAD = 0.35;
    private static final double DASH_REACH = 10.0;
    private static final double DASH_CATCH = 2.0;
    private static final int DASH_WATCH = 12;
    // Ticks his player has to pick the ending, before one is picked for him.
    public static final int CHOICE = 30;
    private static final int LONGEST = CHOICE + 120;
    private static final int PUNCH_GAP = 6;
    private static final int DROP_WAIT = 20;
    private static final double SLAM_RADIUS = 3.0;
    // Where his right fist holds a creature by the throat, in his sizes: ahead of his right shoulder and as high, and
    // how far up a creature's height its throat is.
    private static final double GRIP_AHEAD = 0.55;
    private static final double GRIP_RIGHT = 0.15;
    private static final double GRIP_UP = 1.45;
    private static final double THROAT = 0.8;
    // A thrown creature hurts what it crashes into for this share of the grab's damage, while it flies this long.
    private static final float MISSILE = 0.8F;
    private static final int MISSILE_TICKS = 40;
    private static final double MISSILE_SLOW = 0.3;
    // The escape game a player he holds plays (Captives): one second, one try.
    public static final int ESCAPE = 2;
    private static final double BREAK_FREE = 0.6;
    private static final Map<UUID, ThorGrab> ALL = new HashMap<>();
    private static final Set<UUID> HELD_PLAYERS = new HashSet<>();

    static {
        HeldMobs.addHolder(entity -> entity instanceof ServerPlayer player && HELD_PLAYERS.contains(player.getUUID()));
    }

    public enum Act {
        THROW,
        HEAD_SLAM,
        PUNCHES,
        PUNCH_THROW,
        PUNCH_SLAM,
        HOIST_SLAM,
        HOIST_THROW,
        HOIST_DROP
    }

    // What his player may pick, in the order the prompt shows them: left click punches, right click throws, the
    // scroll wheel slams.
    public static final Act[] PICKS = { Act.PUNCHES, Act.THROW, Act.HEAD_SLAM };

    private static final ThorBlow[] PUNCHES = { ThorBlow.BODY_HOOK, ThorBlow.JAB, ThorBlow.LEAD_HOOK };

    private final UUID owner;
    private final LivingEntity target;
    private final float damage;
    private final boolean dashed;
    // Null while his player still picks.
    @Nullable
    private Act act;
    private int actAt;
    private boolean held;
    private boolean overhead;
    private boolean done;
    private int age;
    private int apex = -1;
    private int dropAt = -1;
    private double lastY;

    private ThorGrab(ServerPlayer owner, LivingEntity target, float damage, boolean dashed) {
        this.owner = owner.getUUID();
        this.target = target;
        this.damage = damage;
        this.dashed = dashed;
        this.lastY = owner.getY();
    }

    public static boolean carrying(ServerPlayer player) {
        ThorGrab grab = ALL.get(player.getUUID());
        return grab != null && grab.held;
    }

    // Holding right: the nearest creature before him within reach.
    static boolean grab(ServerPlayer player, float damage) {
        if (ALL.containsKey(player.getUUID())) {
            return false;
        }
        LivingEntity target = nearest(player);
        if (target == null) {
            return false;
        }
        begin(player, target, damage, false);
        return true;
    }

    // Holding right while running: a straight dash (his game moves him, `yaw` and `tenths` say where) at what he aims
    // at; reaching it on the way he grabs it, else it was only a dash.
    static boolean dash(ServerPlayer player, int yaw, int tenths, float damage) {
        if (ALL.containsKey(player.getUUID()) || !ThorMoves.dash(player, yaw, tenths)) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        LivingEntity target = Targeting.aimLiving(player, level, DASH_REACH);
        if (target == null) {
            return true;
        }
        UUID id = player.getUUID();
        Effects.start(level, (lvl, age) -> {
            ServerPlayer thor = lvl.getServer().getPlayerList().getPlayer(id);
            if (thor == null || thor.level() != lvl || !target.isAlive() || age > DASH_WATCH
                    || ALL.containsKey(id)) {
                return false;
            }
            if (thor.getBoundingBox().getCenter().distanceTo(target.getBoundingBox().getCenter()) < DASH_CATCH
                    + target.getBbWidth() * 0.5) {
                begin(thor, target, damage, true);
                return false;
            }
            return true;
        });
        return true;
    }

    @Nullable
    private static LivingEntity nearest(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 ahead = new Vec3(player.getLookAngle().x, 0.0, player.getLookAngle().z).normalize();
        double reach = REACH * player.getScale();
        LivingEntity best = null;
        double nearest = Double.MAX_VALUE;
        for (LivingEntity target : level(player).getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(reach), entity -> Targeting.isTargetable(player, entity))) {
            Vec3 to = target.getBoundingBox().getCenter().subtract(eye);
            Vec3 flat = new Vec3(to.x, 0.0, to.z);
            double far = to.length();
            if (far > reach + target.getBbWidth() * 0.5
                    || flat.lengthSqr() > 1.0E-4 && flat.normalize().dot(ahead) < AHEAD) {
                continue;
            }
            if (far < nearest) {
                nearest = far;
                best = target;
            }
        }
        return best;
    }

    private static ServerLevel level(ServerPlayer player) {
        return player.serverLevel();
    }

    private static void begin(ServerPlayer player, LivingEntity target, float damage, boolean dashed) {
        ServerLevel level = player.serverLevel();
        ThorGrab grab = new ThorGrab(player, target, damage, dashed);
        grab.held = hold(target);
        ALL.put(player.getUUID(), grab);
        Vec3 at = target.getBoundingBox().getCenter();
        ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, at, 12, 0.3, 0.2);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0F, 0.6F);
        ThorMoves.tell(player, ThorStatePayload.GRAB, grabbed(target.getId(), dashed));
        Effects.start(level, (lvl, age) -> grab.tick(lvl));
    }

    private static boolean hold(LivingEntity target) {
        if (target instanceof Mob mob) {
            return HeldMobs.hold(mob);
        }
        if (target instanceof ServerPlayer player && !HeldMobs.isHeldByAnyone(player)) {
            player.stopRiding();
            HELD_PLAYERS.add(player.getUUID());
            Captives.hold(player, ESCAPE, ThorGrab::brokeFree);
            return true;
        }
        return false;
    }

    // Where a held creature goes: a player's own game moves them, so the server puts them there.
    private static void put(LivingEntity target, Vec3 at) {
        if (target instanceof ServerPlayer player) {
            HeldPlayers.holdAt(player, at.x, at.y, at.z);
        } else {
            target.setPos(at.x, at.y, at.z);
        }
    }

    // A grab's word to every game: what he holds, and whether a dash brought him to it.
    public static int grabbed(int id, boolean dashed) {
        return (id + 1) * 2 + (dashed ? 1 : 0);
    }

    // His player's pick of how it ends (one of PICKS), while he may still pick.
    static boolean pick(ServerPlayer player, int pick) {
        ThorGrab grab = ALL.get(player.getUUID());
        if (grab == null || grab.done || grab.act != null || pick < 0 || pick >= PICKS.length) {
            return false;
        }
        grab.start(player, PICKS[pick]);
        return true;
    }

    // Whether his hands are full with a creature he grabbed: no blow of his own until the grab ends.
    public static boolean busy(ServerPlayer player) {
        ThorGrab grab = ALL.get(player.getUUID());
        return grab != null && !grab.done;
    }

    // One of the pool, as before there was a pick: the leaps only after a dash, and only with a creature he holds.
    private Act any(ServerLevel level) {
        Act[] pool = Act.values();
        int kinds = this.dashed && this.held ? pool.length : Act.PUNCH_SLAM.ordinal() + 1;
        return pool[level.random.nextInt(kinds)];
    }

    private void start(ServerPlayer thor, Act act) {
        this.act = act;
        this.actAt = this.age;
        ThorMoves.tell(thor, ThorStatePayload.GRAB_ACT, act.ordinal());
    }

    // Where his right fist holds a creature by the throat (client and server alike).
    public static Vec3 grip(LivingEntity thor, float partialTick) {
        Vec3 look = Vec3.directionFromRotation(0.0F, thor.getViewYRot(partialTick));
        Vec3 right = look.cross(Vectors.UP).normalize();
        double size = thor.getScale();
        return thor.getPosition(partialTick).add(look.scale(GRIP_AHEAD * size)).add(right.scale(GRIP_RIGHT * size))
                .add(0.0, GRIP_UP * size, 0.0);
    }

    // How far up from its feet a creature is held: its throat.
    public static double throat(Entity held) {
        return held.getBbHeight() * THROAT;
    }

    private boolean tick(ServerLevel level) {
        ServerPlayer thor = level.getServer().getPlayerList().getPlayer(this.owner);
        if (this.done || ALL.get(this.owner) != this) {
            return false;
        }
        if (thor == null || thor.level() != level || !thor.isAlive() || !this.target.isAlive()
                || this.target.isRemoved() || this.target.level() != level || ++this.age > LONGEST) {
            this.end(thor);
            return false;
        }
        thor.resetFallDistance();
        if (this.held) {
            Vec3 at = this.overhead ? overhead(thor) : grip(thor, 1.0F).subtract(0.0, throat(this.target), 0.0);
            // Held up by the throat, never with its feet in the ground he stands on.
            at = new Vec3(at.x, Math.max(at.y, thor.getY()), at.z);
            put(this.target, at);
            this.target.setDeltaMovement(Vec3.ZERO);
            this.target.resetFallDistance();
        }
        if (this.act == null) {
            if (this.age >= CHOICE) {
                this.start(thor, this.any(level));
            }
            return true;
        }
        int t = this.age - this.actAt;
        switch (this.act) {
            case THROW -> this.punchesThen(level, thor, t, 0, true);
            case HEAD_SLAM -> this.punchesThen(level, thor, t, 0, false);
            case PUNCHES -> this.punchesThen(level, thor, t, 3, null);
            case PUNCH_THROW -> this.punchesThen(level, thor, t, 2, true);
            case PUNCH_SLAM -> this.punchesThen(level, thor, t, 2, false);
            case HOIST_SLAM, HOIST_THROW, HOIST_DROP -> this.hoist(level, thor, t);
        }
        return !this.done;
    }

    // `punches` blows to the body, then a throw (true), a smash into the ground (false) or letting go (null).
    private void punchesThen(ServerLevel level, ServerPlayer thor, int t, int punches, @Nullable Boolean throwIt) {
        int first = 2;
        for (int k = 0; k < punches; k++) {
            ThorBlow blow = PUNCHES[k % PUNCHES.length];
            if (t == first + k * PUNCH_GAP) {
                ThorMoves.tell(thor, ThorStatePayload.STRIKE, blow.ordinal());
            }
            if (t == first + k * PUNCH_GAP + blow.hit()) {
                this.hurt(level, thor, 0.5F);
                this.sparks(level, 6);
                level.playSound(null, this.target.getX(), this.target.getY() + 1.0, this.target.getZ(),
                        SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 0.9F);
            }
        }
        int finish = first + punches * PUNCH_GAP;
        if (throwIt == null) {
            if (t == finish + 2) {
                this.let();
                SpellTargets.push(this.target, flat(thor), 0.6, 0.2);
                this.end(thor);
            }
            return;
        }
        ThorBlow blow = throwIt ? ThorBlow.GRAB_HURL : ThorBlow.GRAB_SLAM;
        if (t == finish) {
            ThorMoves.tell(thor, ThorStatePayload.STRIKE, blow.ordinal());
        }
        if (t == finish + blow.hit()) {
            if (throwIt) {
                this.throwAway(level, thor);
            } else {
                this.headSlam(level, thor);
            }
            this.end(thor);
        }
    }

    private void throwAway(ServerLevel level, ServerPlayer thor) {
        this.let();
        this.hurt(level, thor, 1.0F);
        Vec3 look = thor.getLookAngle();
        this.target.setDeltaMovement(look.x * 2.2, Math.max(0.5, look.y * 2.2 + 0.4), look.z * 2.2);
        this.target.hasImpulse = true;
        this.target.hurtMarked = true;
        this.sparks(level, 14);
        level.playSound(null, thor.getX(), thor.getY() + thor.getScale(), thor.getZ(),
                SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.2F, 0.7F);
        missile(level, thor, this.target, this.damage * MISSILE);
    }

    // Thrown, it flies as a missile: each creature it crashes into on the way is hurt once and knocked on along its
    // flight, until it lands or slows. Followed by where it moves, as a thrown player's own game moves them.
    private static void missile(ServerLevel level, ServerPlayer thor, LivingEntity thrown, float damage) {
        UUID id = thor.getUUID();
        IntOpenHashSet struck = new IntOpenHashSet();
        Vec3[] last = { thrown.position() };
        Effects.start(level, (lvl, age) -> {
            ServerPlayer by = lvl.getServer().getPlayerList().getPlayer(id);
            if (by == null || !thrown.isAlive() || thrown.isRemoved() || thrown.level() != lvl
                    || age > MISSILE_TICKS) {
                return false;
            }
            Vec3 moved = age == 0 ? thrown.getDeltaMovement() : thrown.position().subtract(last[0]);
            last[0] = thrown.position();
            if (age > 2 && (moved.length() < MISSILE_SLOW || standing(lvl, thrown))) {
                return false;
            }
            AABB box = thrown.getBoundingBox().expandTowards(moved).inflate(0.25);
            for (LivingEntity hit : lvl.getEntitiesOfClass(LivingEntity.class, box, entity -> entity != thrown
                    && entity != by && !struck.contains(entity.getId()) && SpellTargets.hits(by, entity))) {
                struck.add(hit.getId());
                hit.invulnerableTime = 0;
                hit.hurt(lvl.damageSources().playerAttack(by), damage);
                SpellTargets.push(hit, moved.normalize(), 0.9, 0.35);
                Vec3 at = hit.getBoundingBox().getCenter();
                ParticleFx.cloud(lvl, ParticleTypes.ELECTRIC_SPARK, at, 10, 0.3, 0.2);
                lvl.playSound(null, at.x, at.y, at.z, SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0F,
                        0.6F);
            }
            return true;
        });
    }

    // Its head driven into the ground before him.
    private void headSlam(ServerLevel level, ServerPlayer thor) {
        this.let();
        Vec3 spot = thor.position().add(flat(thor).scale(1.3 * thor.getScale()));
        double floor = Targeting.floorBelow(level, BlockPos.containing(spot.x, thor.getY() + thor.getScale(),
                spot.z));
        put(this.target, new Vec3(spot.x, floor, spot.z));
        this.hurt(level, thor, 1.5F);
        // Set after the hit, whose own knockback would hop it back up off the ground.
        this.target.setDeltaMovement(0.0, -0.5, 0.0);
        this.target.hurtMarked = true;
        Vec3 ground = new Vec3(spot.x, floor + 0.1, spot.z);
        ParticleFx.shockwave(level, ParticleFx.dust(ThorMoves.GLOW, 1.1F), ground, 20, 0.3);
        ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, ground.add(0.0, 0.3, 0.0), 16, 0.5, 0.15);
        level.playSound(null, ground.x, ground.y, ground.z, SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.PLAYERS,
                1.0F, 0.9F);
    }

    // Up with it over his head, and at the top of the leap one of three endings.
    private void hoist(ServerLevel level, ServerPlayer thor, int t) {
        if (t == 4) {
            this.overhead = true;
            ThorMoves.spare(thor, 140);
            ThorMoves.tell(thor, ThorStatePayload.HOIST, 0);
            return;
        }
        if (t < 10) {
            return;
        }
        double y = thor.getY();
        if (this.apex < 0 && y <= this.lastY) {
            this.apex = t;
            switch (this.act) {
                case HOIST_SLAM -> {
                    this.overhead = false;
                    ThorMoves.tell(thor, ThorStatePayload.DROP, 0);
                }
                case HOIST_THROW -> {
                    this.let();
                    this.hurt(level, thor, 0.5F);
                    this.target.setDeltaMovement(0.0, -3.0, 0.0);
                    this.target.hurtMarked = true;
                    this.smashOnLanding(level, thor);
                    this.end(thor);
                    return;
                }
                default -> {
                    this.let();
                    this.target.setDeltaMovement(0.0, -0.8, 0.0);
                    this.target.hurtMarked = true;
                }
            }
        }
        this.lastY = y;
        // Let go at the top, it falls first; once it lies on the ground he comes down onto it. Its own onGround() is
        // stale from before he picked it up.
        if (this.act == Act.HOIST_DROP && this.apex >= 0 && this.dropAt < 0
                && (standing(level, this.target) || t >= this.apex + DROP_WAIT)) {
            this.dropAt = t;
            ThorMoves.tell(thor, ThorStatePayload.DROP, this.target.getId() + 1);
        }
        boolean falling = this.act == Act.HOIST_SLAM ? this.apex >= 0 : this.dropAt >= 0 && t > this.dropAt;
        if (falling && standing(level, thor)) {
            this.let();
            this.landing(level, thor);
            this.end(thor);
        }
    }

    private static boolean standing(ServerLevel level, Entity entity) {
        return !level.noCollision(entity, entity.getBoundingBox().move(0.0, -0.2, 0.0));
    }

    // He comes down on the ground with it (or onto it): everything round is hurt, it most.
    private void landing(ServerLevel level, ServerPlayer thor) {
        Vec3 center = thor.position();
        this.hurt(level, thor, 2.0F);
        for (LivingEntity near : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center)
                .inflate(SLAM_RADIUS), entity -> entity != this.target && SpellTargets.hits(thor, entity))) {
            near.invulnerableTime = 0;
            near.hurt(level.damageSources().playerAttack(thor), this.damage * 0.8F);
            Vec3 away = near.position().subtract(center);
            Vec3 way = away.horizontalDistanceSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0)
                    : new Vec3(away.x, 0.0, away.z).normalize();
            SpellTargets.push(near, way, 1.0, 0.45);
        }
        ParticleFx.shockwave(level, ParticleFx.dust(ThorMoves.GLOW, 1.5F), center.add(0.0, 0.15, 0.0), 36, 0.6);
        ParticleFx.shockwave(level, ParticleTypes.CLOUD, center.add(0.0, 0.2, 0.0), 20, 0.3);
        ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, center.add(0.0, 0.5, 0.0), 30, 1.0, 0.3);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.PLAYERS,
                1.4F, 0.8F);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS,
                1.0F, 1.1F);
    }

    // Hurled down, it is hurt again as it hits the ground.
    private void smashOnLanding(ServerLevel level, ServerPlayer thor) {
        LivingEntity hurled = this.target;
        float hard = this.damage * 2.0F;
        UUID id = thor.getUUID();
        Effects.start(level, (lvl, age) -> {
            if (!hurled.isAlive() || age > 80) {
                return false;
            }
            if (age < 2 || !hurled.onGround()) {
                return true;
            }
            ServerPlayer by = lvl.getServer().getPlayerList().getPlayer(id);
            hurled.invulnerableTime = 0;
            hurled.hurt(by == null ? lvl.damageSources().generic() : lvl.damageSources().playerAttack(by), hard);
            Vec3 at = hurled.position().add(0.0, 0.1, 0.0);
            ParticleFx.shockwave(lvl, ParticleTypes.CLOUD, at, 16, 0.3);
            lvl.playSound(null, at.x, at.y, at.z, SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS, 1.0F, 0.8F);
            return false;
        });
    }

    // Held up over his head, as both client and server put it.
    public static Vec3 overhead(LivingEntity thor) {
        return thor.position().add(0.0, thor.getBbHeight() + 0.3 * thor.getScale(), 0.0);
    }

    private static Vec3 flat(ServerPlayer thor) {
        return Vec3.directionFromRotation(0.0F, thor.getYRot());
    }

    private void hurt(ServerLevel level, ServerPlayer thor, float share) {
        this.target.invulnerableTime = 0;
        this.target.hurt(level.damageSources().playerAttack(thor), this.damage * share);
        if (this.held) {
            this.target.setDeltaMovement(Vec3.ZERO);
        }
    }

    private void sparks(ServerLevel level, int count) {
        ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, this.target.getBoundingBox().getCenter(), count, 0.3,
                0.2);
    }

    private void let() {
        if (this.held && this.target instanceof Mob mob) {
            HeldMobs.release(mob);
        } else if (this.held) {
            HELD_PLAYERS.remove(this.target.getUUID());
            if (this.target instanceof ServerPlayer captive) {
                Captives.release(captive);
            }
        }
        this.held = false;
        this.overhead = false;
    }

    private void end(@Nullable ServerPlayer thor) {
        if (this.done) {
            return;
        }
        this.done = true;
        this.let();
        ALL.remove(this.owner, this);
        if (thor != null) {
            ThorMoves.tell(thor, ThorStatePayload.NONE, 0);
        }
    }

    // A held player won the escape game: they tear out of his grip and are thrown back from him.
    private static void brokeFree(ServerPlayer captive) {
        for (ThorGrab grab : ALL.values().toArray(new ThorGrab[0])) {
            if (grab.target != captive || grab.done) {
                continue;
            }
            ServerLevel level = captive.serverLevel();
            ServerPlayer thor = level.getServer().getPlayerList().getPlayer(grab.owner);
            grab.end(thor);
            Vec3 away = thor == null ? Vec3.ZERO : captive.position().subtract(thor.position()).multiply(1.0, 0.0, 1.0);
            away = away.lengthSqr() < 1.0E-4 ? Vec3.ZERO : away.normalize().scale(BREAK_FREE);
            captive.setDeltaMovement(away.x, 0.35, away.z);
            captive.hurtMarked = true;
            Vec3 at = captive.getBoundingBox().getCenter();
            ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, at, 16, 0.4, 0.25);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0F,
                    1.3F);
        }
    }

    static void leave(ServerPlayer player) {
        ThorGrab grab = ALL.get(player.getUUID());
        if (grab != null) {
            grab.end(player);
        }
    }

    static void clear() {
        for (ThorGrab grab : ALL.values().toArray(new ThorGrab[0])) {
            grab.let();
            grab.done = true;
        }
        ALL.clear();
        HELD_PLAYERS.clear();
    }
}
