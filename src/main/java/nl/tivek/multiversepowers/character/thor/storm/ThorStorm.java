package nl.tivek.multiversepowers.character.thor.storm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.Characters;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.thor.ThorMoves;
import nl.tivek.multiversepowers.character.thor.ThorStatePayload;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.target.Targeting;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;
import nl.tivek.multiversepowers.faction.Factions;

// Thor's storm: a thunderstorm gathers over him and follows him while it lasts. Its key pressed again calls a bolt down
// out of it where he aims; by itself it strikes a foe under it now and then. Each bolt strikes one creature and leaps
// on (ChainBolt). Crouching and pressing it ends it. Every game draws the cloud over him itself from the flag he
// carries (`client/StormSky`) and is told only of the bolts.
public final class ThorStorm {
    public static final String ABILITY = "storm";
    // How long the cloud takes to gather before it strikes, and to clear away once it ends.
    public static final int GATHER = 50;
    public static final int CLEAR = 60;
    // Its underside: this far over him, and at least this far over the ground under him.
    private static final double OVER = 20.0;
    private static final double OVER_GROUND = 16.0;
    // It follows him: this share of the way each tick, at most this many blocks.
    private static final double FOLLOW = 0.06;
    private static final double FOLLOW_MOST = 1.2;
    private static final double AIM = 96.0;
    private static final double DEEPEST = 128.0;
    private static final int CALL_GAP = 8;
    // It strikes a foe by itself about once in this many ticks.
    private static final int STRIKE_EVERY = 36;
    // With no storm up, his bolts come down from this high over what they strike.
    private static final double HIGH = 32.0;
    private static final Map<UUID, ThorStorm> ALL = new HashMap<>();

    private final UUID owner;
    private final int ticks;
    private final double radius;
    private final float strikes;
    private Vec3 center;
    private int age;
    private int ended = -1;
    private long nextCall;
    private int nextStrike = GATHER;

    private ThorStorm(UUID owner, int ticks, double radius, float strikes, Vec3 center) {
        this.owner = owner;
        this.ticks = ticks;
        this.radius = radius;
        this.strikes = strikes;
        this.center = center;
    }

    // Where the storm's underside wants to be for someone at `at`: over him, and well over the ground under him.
    public static Vec3 cloudAt(Level level, Vec3 at) {
        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(at.x), Mth.floor(at.z));
        return new Vec3(at.x, Math.max(at.y + OVER, ground + OVER_GROUND), at.z);
    }

    // One tick of the storm following him, the same here and in every game that draws it.
    public static Vec3 follow(Vec3 center, Vec3 want) {
        Vec3 step = want.subtract(center).scale(FOLLOW);
        double far = step.length();
        return far > FOLLOW_MOST ? center.add(step.scale(FOLLOW_MOST / far)) : center.add(step);
    }

    public static boolean up(ServerPlayer player) {
        ThorStorm storm = ALL.get(player.getUUID());
        return storm != null && storm.ended < 0;
    }

    public static int flags(ServerPlayer player) {
        return up(player) ? ThorStatePayload.STORMING : 0;
    }

    public static boolean summon(ServerPlayer player, CharacterAbility ability) {
        if (up(player)) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        ThorStorm storm = new ThorStorm(player.getUUID(), (int) Math.round(ability.value("seconds") * 20.0),
                ability.value("radius"), (float) ability.value("strikeDamage"), cloudAt(level, player.position()));
        ALL.put(player.getUUID(), storm);
        ThorMoves.tell(player, ThorStatePayload.STORM, 0);
        Vec3 sky = storm.center;
        level.playSound(null, sky.x, sky.y, sky.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 3.0F, 0.5F);
        level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.TRIDENT_THUNDER.value(),
                SoundSource.PLAYERS, 1.0F, 0.6F);
        Effects.start(level, (lvl, age) -> storm.tick(lvl));
        return true;
    }

    // His crouched press: the storm ends and clears away.
    public static void calm(ServerPlayer player) {
        ThorStorm storm = ALL.get(player.getUUID());
        if (storm != null && storm.ended < 0) {
            storm.end(player);
        }
    }

    // A bolt where he aims, under the storm once it has gathered: onto the creature he aims at, in the air too, else
    // down onto the ground there. True when one struck.
    public static boolean call(ServerPlayer player, float damage) {
        ThorStorm storm = ALL.get(player.getUUID());
        ServerLevel level = player.serverLevel();
        if (storm == null || storm.ended >= 0 || storm.age < GATHER || level.getGameTime() < storm.nextCall) {
            return false;
        }
        storm.nextCall = level.getGameTime() + CALL_GAP;
        ThorMoves.tell(player, ThorStatePayload.CALL, 0);
        LivingEntity aimed = Targeting.aimLiving(player, level, AIM);
        if (aimed != null && storm.under(aimed.position())) {
            ChainBolt.onto(level, player, storm.top(level, aimed.position()), aimed, damage, 1.0F);
            return true;
        }
        Vec3 aim = Targeting.aimPoint(player, level, AIM);
        double dx = aim.x - storm.center.x;
        double dz = aim.z - storm.center.z;
        double far = Math.sqrt(dx * dx + dz * dz);
        if (far > storm.radius) {
            aim = new Vec3(storm.center.x + dx * storm.radius / far, aim.y, storm.center.z + dz * storm.radius / far);
        }
        Vec3 ground = storm.ground(level, player, aim);
        ChainBolt.down(level, player, storm.top(level, ground), ground, damage, 1.0F);
        return true;
    }

    // Where a bolt of his comes down from onto `at`: his storm while it is up over there, else the sky high over it.
    public static Vec3 sky(ServerPlayer player, Vec3 at) {
        ThorStorm storm = ALL.get(player.getUUID());
        ServerLevel level = player.serverLevel();
        if (storm != null && storm.ended < 0 && storm.under(at)) {
            return storm.top(level, at);
        }
        RandomSource random = level.getRandom();
        return at.add((random.nextDouble() - 0.5) * 6.0, HIGH, (random.nextDouble() - 0.5) * 6.0);
    }

    private boolean under(Vec3 at) {
        return at.y < this.center.y
                && Mth.square(at.x - this.center.x) + Mth.square(at.z - this.center.z) <= this.radius * this.radius;
    }

    // Where in the cloud a bolt onto `at` starts: over it, a little to one side.
    private Vec3 top(ServerLevel level, Vec3 at) {
        RandomSource random = level.getRandom();
        return new Vec3(at.x + (random.nextDouble() - 0.5) * 5.0, this.center.y + 1.0,
                at.z + (random.nextDouble() - 0.5) * 5.0);
    }

    private boolean tick(ServerLevel level) {
        if (ALL.get(this.owner) != this) {
            return false;
        }
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(this.owner);
        if (player == null || player.level() != level || !player.isAlive()) {
            ALL.remove(this.owner, this);
            if (player != null && this.ended < 0) {
                ThorMoves.tell(player, ThorStatePayload.NONE, 0);
            }
            return false;
        }
        this.age++;
        this.center = follow(this.center, cloudAt(level, player.position()));
        if (this.ended >= 0) {
            if (this.age - this.ended > CLEAR) {
                ALL.remove(this.owner, this);
                return false;
            }
            return true;
        }
        if (this.age >= GATHER + this.ticks) {
            this.end(player);
        } else if (this.age >= this.nextStrike) {
            RandomSource random = level.getRandom();
            this.nextStrike = this.age + STRIKE_EVERY * 2 / 3 + random.nextInt(STRIKE_EVERY * 2 / 3 + 1);
            this.strikeAlone(level, player, random);
        }
        return true;
    }

    private void end(ServerPlayer player) {
        this.ended = this.age;
        ThorMoves.tell(player, ThorStatePayload.NONE, 0);
        CharacterAbility ability = GameCharacter.THOR.byName(ABILITY);
        if (ability != null) {
            Characters.startCooldown(player, ability);
        }
    }

    // A foe in the open under it, flying or not, else a bolt somewhere under it away from him that hurts nothing.
    private void strikeAlone(ServerLevel level, ServerPlayer player, RandomSource random) {
        Vec3 c = this.center;
        List<LivingEntity> foes = level.getEntitiesOfClass(LivingEntity.class, new AABB(c.x - this.radius,
                c.y - DEEPEST, c.z - this.radius, c.x + this.radius, c.y, c.z + this.radius),
                entity -> Factions.hostile(player, entity) && Targeting.mayStrike(player, entity)
                        && this.under(entity.position())
                        && level.canSeeSky(BlockPos.containing(entity.getX(), entity.getEyeY(), entity.getZ())));
        if (!foes.isEmpty()) {
            LivingEntity foe = foes.get(random.nextInt(foes.size()));
            ChainBolt.onto(level, player, this.top(level, foe.position()), foe, this.strikes, 0.8F);
            return;
        }
        double angle = random.nextDouble() * Math.PI * 2.0;
        double reach = this.radius * (0.35 + 0.6 * random.nextDouble());
        Vec3 ground = this.ground(level, player, new Vec3(c.x + Math.cos(angle) * reach, c.y,
                c.z + Math.sin(angle) * reach));
        ChainBolt.down(level, player, this.top(level, ground), ground, 0.0F, 0.8F);
    }

    // Where a bolt from the cloud straight down onto `at` strikes: the first thing in its way.
    private Vec3 ground(ServerLevel level, ServerPlayer player, Vec3 at) {
        Vec3 top = new Vec3(at.x, this.center.y, at.z);
        Vec3 bottom = new Vec3(at.x, Math.max(level.getMinBuildHeight(), this.center.y - DEEPEST), at.z);
        BlockHitResult hit = LoadedWorld.clip(level, new ClipContext(top, bottom, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.ANY, player));
        return hit.getType() == HitResult.Type.MISS ? at : hit.getLocation();
    }

    public static void leave(ServerPlayer player) {
        ThorStorm storm = ALL.remove(player.getUUID());
        if (storm != null && storm.ended < 0) {
            ThorStatePayload.send(player, 0, ThorStatePayload.NONE, 0);
        }
    }

    public static void clear() {
        ALL.clear();
    }
}
