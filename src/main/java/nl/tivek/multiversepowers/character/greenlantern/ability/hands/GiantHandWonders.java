package nl.tivek.multiversepowers.character.greenlantern.ability.hands;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandPose;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandVictimPayload;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.entity.HeldMobs;
import nl.tivek.multiversepowers.engine.entity.Knockdowns;
import nl.tivek.multiversepowers.engine.entity.TimeFreeze;
import nl.tivek.multiversepowers.engine.entity.TornBlock;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.world.BlockRules;
import static nl.tivek.multiversepowers.character.greenlantern.ability.hands.GiantHands.SCALE;
import static nl.tivek.multiversepowers.engine.math.Vectors.flat;

// What the baseball bat, the hourglass, the gravity flip and the black hole do: a home run, time stopped round the
// hand, everything under it falling up and slammed back down, everything round it drawn into a black hole that bursts.
abstract class GiantHandWonders extends GiantHandShots {
    private static final double BAT_DAMAGE = 1.6;
    private static final double BAT_OUT = 3.4;
    private static final double BAT_UP = 1.4;
    // How near the bat's barrel a creature's box must come to be hit, and from how far along the handle it hits.
    private static final double BAT_REACH = 0.75;
    private static final double BAT_BARREL = 1.5;
    private static final int TRAIL_TICKS = 30;
    private static final double TIME_RADIUS = 9.0;
    private static final double TIME_DAMAGE = 1.0;
    private static final int TIME_LOOKS = 5;
    private static final double GRAVITY_RADIUS = 5.5;
    private static final double GRAVITY_HIGH = 8.0;
    private static final double GRAVITY_DAMAGE = 1.4;
    private static final double SLAM_SPEED = 2.6;
    private static final int SLAM_MOST = 40;
    private static final int GRAVITY_LOOKS = 6;
    private static final double PULL_RADIUS = HandPose.HOLE_PULL;
    private static final double CORE = 3.5;
    private static final double CORE_DAMAGE = 0.6;
    private static final int CORE_EVERY = 12;
    private static final double BLAST_RADIUS = HandPose.HOLE_BLAST_RADIUS;
    private static final double BLAST_DAMAGE = 2.5;
    private static final double BLAST_OUT = 3.4;
    private static final double BLAST_UP = 1.1;
    private static final int DUST = 8;
    // The hardest block a black hole tears out (deepslate), and the fewest ticks a torn block still needs to get in
    // before the fist crushes the hole.
    private static final float HOLE_HARDEST = 3.0F;
    private static final int HOLE_LAST = 10;

    private record Floating(LivingEntity living, double high) {
    }

    private final List<LivingEntity> stopped = new ArrayList<>();
    private final List<Floating> floating = new ArrayList<>();
    private final List<LivingEntity> falling = new ArrayList<>();
    private final List<TornBlock> torn = new ArrayList<>();
    private int tornCount;
    private Vec3 field;
    private int slammedAt = -1;

    GiantHandWonders(GiantHands storm, int variant, Vec3 base, LivingEntity target) {
        super(storm, variant, base, target);
    }

    GiantHandWonders(GiantHands storm, int variant, Vec3 base, LivingEntity target, Vec3 facing) {
        super(storm, variant, base, target, facing);
    }

    @Override
    void feat(ServerLevel level) {
        switch (this.move) {
            case HandPose.BAT -> this.bat(level);
            case HandPose.HOURGLASS -> this.hourglass(level);
            case HandPose.GRAVITY -> this.gravity(level);
            case HandPose.BLACKHOLE -> this.blackHole(level);
            default -> super.feat(level);
        }
    }

    @Override
    void letGo() {
        super.letGo();
        this.goOn(null);
        for (Floating up : this.floating) {
            HandVictimPayload.send(up.living(), this.id(), HandVictimPayload.LANDED);
        }
        for (LivingEntity down : this.falling) {
            HandVictimPayload.send(down, this.id(), HandVictimPayload.LANDED);
        }
        this.floating.clear();
        this.falling.clear();
    }

    private double part(double share) {
        return this.storm.ability.getDamage() * share;
    }

    // The bat grows, winds back and swings flat: whatever the barrel passed through on its way is sent flying the way
    // it swung, trailing light. Then the bat is flipped away and breaks apart in the air.
    private void bat(ServerLevel level) {
        HandPose.Place place = this.place();
        Vec3 grip = place.at(HandPose.BAT_HOLD);
        if (this.t == HandPose.ARRIVES + 4) {
            this.storm.sound(level, grip, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.6F, 0.8F);
            this.storm.sound(level, grip, SoundEvents.BEACON_POWER_SELECT, 1.0F, 1.5F);
        }
        if (this.t == HandPose.BAT_SWINGS) {
            this.storm.sound(level, grip, SoundEvents.PLAYER_ATTACK_SWEEP, 2.0F, 0.5F);
            this.storm.sound(level, grip, SoundEvents.ENDER_DRAGON_FLAP, 1.4F, 1.4F);
        }
        if (this.t == HandPose.BAT_HITS) {
            this.swing(level);
        }
        if (this.t == HandPose.BAT_FLIPS) {
            this.storm.sound(level, grip, SoundEvents.PLAYER_ATTACK_SWEEP, 1.4F, 1.6F);
        }
        if (this.t == HandPose.BAT_BREAKS) {
            Vec3 high = grip.add(0.0, 5.0 * SCALE, 0.0);
            ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.3F), high, 24, 0.35);
            this.storm.sound(level, high, SoundEvents.GLASS_BREAK, 1.6F, 0.8F);
            this.storm.sound(level, high, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.6F, 0.9F);
        }
    }

    private void swing(ServerLevel level) {
        List<Vec3[]> sweeps = new ArrayList<>();
        for (double at = this.t - 3.0; at <= this.t + 0.5; at += 0.25) {
            HandPose.Place then = this.pose(at).place(this.base, this.aim.subtract(this.base), SCALE);
            sweeps.add(new Vec3[] { then.at(HandPose.batPoint(BAT_BARREL)),
                    then.at(HandPose.batPoint(HandPose.BAT_TIP)) });
        }
        HandPose.Place now = this.place();
        HandPose.Place before = this.pose(this.t - 1.0).place(this.base, this.aim.subtract(this.base), SCALE);
        Vec3 tip = now.at(HandPose.batPoint(HandPose.BAT_TIP));
        Vec3 way = flat(tip.subtract(before.at(HandPose.batPoint(HandPose.BAT_TIP))));
        boolean struck = false;
        for (LivingEntity living : this.near(level, HandPose.BAT_TIP + 4.0)) {
            AABB box = living.getBoundingBox().inflate(BAT_REACH * SCALE);
            if (!passes(sweeps, box)) {
                continue;
            }
            struck = true;
            this.hit(level, living, this.part(BAT_DAMAGE), way, BAT_OUT, BAT_UP);
            this.trail(level, living);
            ParticleFx.send(level, ParticleTypes.CRIT, living.getX(), living.getY() + living.getBbHeight() * 0.5,
                    living.getZ(), 20, 0.4, 0.4, 0.4, 0.6);
        }
        Vec3 barrel = now.at(HandPose.batPoint(4.5));
        ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.3F), barrel, 14, 0.3);
        this.storm.sound(level, barrel, SoundEvents.PLAYER_ATTACK_SWEEP, 2.0F, 0.7F);
        if (struck) {
            this.storm.sound(level, barrel, SoundEvents.PLAYER_ATTACK_CRIT, 2.4F, 0.6F);
            this.storm.sound(level, barrel, SoundEvents.ANVIL_LAND, 1.0F, 1.8F);
            this.storm.sound(level, barrel, SoundEvents.WOOD_BREAK, 2.4F, 0.5F);
            this.storm.sound(level, barrel, SoundEvents.FIREWORK_ROCKET_TWINKLE, 1.4F, 1.0F);
        }
    }

    // Whether a point of the barrel passed through the box on its way: each sampled bat is checked along its length.
    private static boolean passes(List<Vec3[]> sweeps, AABB box) {
        for (Vec3[] line : sweeps) {
            for (int k = 0; k <= 8; k++) {
                if (box.contains(line[0].lerp(line[1], k / 8.0))) {
                    return true;
                }
            }
        }
        return false;
    }

    // A home run leaves a streak of light behind it while it flies.
    private void trail(ServerLevel level, LivingEntity living) {
        Effects.start(level, (lvl, age) -> {
            if (age >= TRAIL_TICKS || !living.isAlive() || living.level() != lvl || age > 4 && living.onGround()) {
                return false;
            }
            Vec3 middle = living.getBoundingBox().getCenter();
            ParticleFx.send(lvl, ParticleTypes.END_ROD, middle.x, middle.y, middle.z, 2, 0.15, 0.15, 0.15, 0.01);
            ParticleFx.cloud(lvl, ParticleFx.dust(PowerRing.BRIGHT, 1.2F), middle, 3, 0.2, 0.0);
            return true;
        });
    }

    // The hourglass turns over and time stops round the hand: everything near stands still where it is, and whatever
    // comes near meanwhile stops too. As the hourglass breaks, time goes on and every stopped creature takes the blow.
    private void hourglass(ServerLevel level) {
        HandPose.Place place = this.place();
        Vec3 glass = place.at(HandPose.GLASS_FOOT);
        if (this.t == HandPose.ARRIVES + 4) {
            this.storm.sound(level, glass, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.6F, 0.6F);
        }
        if (this.t == HandPose.TIME_STOPS - 4) {
            this.storm.sound(level, glass, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 1.6F);
        }
        if (this.t == HandPose.TIME_STOPS) {
            ParticleFx.shockwave(level, ParticleFx.dust(PowerRing.PALE, 1.6F), this.base.add(0.0, 0.3, 0.0), 48,
                    TIME_RADIUS / 12.0);
            this.storm.sound(level, glass, SoundEvents.BELL_BLOCK, 2.4F, 0.5F);
            this.storm.sound(level, glass, SoundEvents.BEACON_DEACTIVATE, 2.0F, 0.6F);
            this.storm.sound(level, glass, SoundEvents.AMETHYST_BLOCK_CHIME, 2.0F, 0.5F);
        }
        if (this.t >= HandPose.TIME_STOPS && this.t < HandPose.TIME_GOES) {
            if ((this.t - HandPose.TIME_STOPS) % TIME_LOOKS == 0) {
                this.stopAll(level);
            }
            if ((this.t - HandPose.TIME_STOPS) % 10 == 5) {
                boolean tock = (this.t - HandPose.TIME_STOPS) / 10 % 2 == 1;
                this.storm.sound(level, glass, SoundEvents.TRIPWIRE_CLICK_ON, 1.4F, tock ? 0.55F : 0.7F);
            }
        }
        if (this.t == HandPose.GLASS_BREAKS) {
            ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.3F), glass, 24, 0.3);
            this.storm.sound(level, glass, SoundEvents.GLASS_BREAK, 2.0F, 0.6F);
            this.storm.sound(level, glass, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.6F, 0.7F);
        }
        if (this.t == HandPose.TIME_GOES) {
            this.goOn(level);
            ParticleFx.send(level, ParticleTypes.FLASH, glass.x, glass.y, glass.z, 1, 0.0, 0.0, 0.0, 0.0);
            ParticleFx.shockwave(level, ParticleFx.dust(PowerRing.BRIGHT, 1.6F), this.base.add(0.0, 0.3, 0.0), 48,
                    TIME_RADIUS / 10.0);
            this.storm.sound(level, glass, SoundEvents.BELL_BLOCK, 2.4F, 1.2F);
            this.storm.sound(level, glass, SoundEvents.BEACON_ACTIVATE, 2.0F, 1.4F);
        }
    }

    private void stopAll(ServerLevel level) {
        int most = HandPose.TIME_GOES - this.t + 10;
        for (LivingEntity living : this.near(level, TIME_RADIUS + 2.0)) {
            double dx = living.getX() - this.base.x;
            double dz = living.getZ() - this.base.z;
            if (dx * dx + dz * dz > TIME_RADIUS * TIME_RADIUS * SCALE * SCALE || this.stopped.contains(living)
                    || !TimeFreeze.freeze(level, living, most)) {
                continue;
            }
            this.stopped.add(living);
            HandVictimPayload.send(living, this.id(), HandVictimPayload.STOPPED);
            ParticleFx.cloud(level, ParticleTypes.END_ROD, living.getBoundingBox().getCenter(), 6, 0.4, 0.0);
        }
    }

    // Time goes on: every stopped creature moves again, and with a level, takes the blow the stopped time owed it.
    private void goOn(ServerLevel level) {
        for (LivingEntity living : this.stopped) {
            TimeFreeze.thaw(living);
            HandVictimPayload.send(living, this.id(), HandVictimPayload.GOES_ON);
            if (level != null && living.isAlive()) {
                Vec3 away = flat(living.position().subtract(this.base));
                this.hit(level, living, this.part(TIME_DAMAGE), away, 0.5, 0.35);
            }
        }
        this.stopped.clear();
    }

    // The hand turns over and gravity with it: everything under it falls up and hangs high above the ground until
    // the hand turns back and slams it all down again; what hits the ground is hurt and knocked down.
    private void gravity(ServerLevel level) {
        if (this.t == HandPose.GRAVITY_FLIPS - 6) {
            this.field = new Vec3(this.aim.x, this.base.y, this.aim.z);
            this.storm.sound(level, this.field, SoundEvents.BEACON_POWER_SELECT, 2.0F, 0.5F);
        }
        if (this.field == null) {
            return;
        }
        if (this.t >= HandPose.GRAVITY_FLIPS && this.t < HandPose.GRAVITY_SLAMS - 10
                && (this.t - HandPose.GRAVITY_FLIPS) % GRAVITY_LOOKS == 0) {
            this.liftAll(level);
        }
        if (this.t < HandPose.GRAVITY_SLAMS) {
            for (Floating up : this.floating) {
                this.hang(up);
            }
        }
        if (this.t == HandPose.GRAVITY_SLAMS) {
            for (Floating up : this.floating) {
                if (up.living().isAlive()) {
                    up.living().setDeltaMovement(0.0, -SLAM_SPEED, 0.0);
                    up.living().hurtMarked = true;
                    this.falling.add(up.living());
                }
            }
            this.floating.clear();
            this.slammedAt = this.t;
            this.storm.sound(level, this.field, SoundEvents.ENDER_DRAGON_FLAP, 2.4F, 0.5F);
            this.storm.sound(level, this.field, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 2.4F, 0.5F);
        }
        if (this.slammedAt >= 0) {
            this.fall(level);
        }
    }

    private void liftAll(ServerLevel level) {
        boolean first = this.t == HandPose.GRAVITY_FLIPS;
        for (LivingEntity living : this.near(level, GRAVITY_RADIUS + 6.0)) {
            double dx = living.getX() - this.field.x;
            double dz = living.getZ() - this.field.z;
            double dy = living.getY() - this.field.y;
            if (dx * dx + dz * dz > GRAVITY_RADIUS * GRAVITY_RADIUS * SCALE * SCALE || dy < -3.0 || dy > 4.0
                    || HeldMobs.isHeldByAnyone(living) || this.lifted(living)) {
                continue;
            }
            this.floating.add(new Floating(living, this.field.y + (GRAVITY_HIGH + living.getRandom().nextDouble())
                    * SCALE));
            living.setDeltaMovement(living.getDeltaMovement().x * 0.3, 1.0, living.getDeltaMovement().z * 0.3);
            living.hurtMarked = true;
            HandVictimPayload.send(living, this.id(), HandVictimPayload.FLIPPED);
            ParticleFx.cloud(level, ParticleFx.dust(PowerRing.PALE, 1.2F), living.position(), 8, 0.4, 0.05);
        }
        if (first) {
            ParticleFx.shockwave(level, ParticleFx.dust(PowerRing.PALE, 1.5F), this.field.add(0.0, 0.2, 0.0), 40,
                    GRAVITY_RADIUS / 12.0);
            this.dustAt(level, this.field, 30, GRAVITY_RADIUS * 0.5);
            this.storm.sound(level, this.field, SoundEvents.ELYTRA_FLYING, 1.6F, 1.4F);
            this.storm.sound(level, this.field, SoundEvents.BEACON_ACTIVATE, 2.0F, 0.6F);
        }
    }

    private boolean lifted(LivingEntity living) {
        for (Floating up : this.floating) {
            if (up.living() == living) {
                return true;
            }
        }
        return false;
    }

    // Held floating high over the field: up towards its height, drifting back over the middle, bobbing a little.
    private void hang(Floating up) {
        LivingEntity living = up.living();
        if (!living.isAlive()) {
            return;
        }
        double rise = Math.max(-0.3, Math.min(0.9, (up.high() - living.getY()) * 0.2))
                + 0.03 * Math.sin((this.t + living.getId()) * 0.3);
        Vec3 back = new Vec3(this.field.x - living.getX(), 0.0, this.field.z - living.getZ()).scale(0.02);
        Vec3 drift = living.getDeltaMovement().multiply(0.6, 0.0, 0.6).add(back);
        living.setDeltaMovement(drift.x, rise, drift.z);
        living.resetFallDistance();
        living.hurtMarked = true;
    }

    private void fall(ServerLevel level) {
        boolean timedOut = this.t - this.slammedAt > SLAM_MOST;
        this.falling.removeIf(living -> {
            if (!living.isAlive()) {
                return true;
            }
            boolean ground = this.t > this.slammedAt + 1 && !level.noCollision(living,
                    living.getBoundingBox().move(0.0, -0.25, 0.0));
            if (!ground && !timedOut) {
                living.setDeltaMovement(0.0, -SLAM_SPEED, 0.0);
                living.resetFallDistance();
                living.hurtMarked = true;
                return false;
            }
            HandVictimPayload.send(living, this.id(), HandVictimPayload.LANDED);
            if (ground) {
                Vec3 away = flat(living.position().subtract(this.field));
                living.resetFallDistance();
                this.hit(level, living, this.part(GRAVITY_DAMAGE), away, 0.4, 0.3);
                Knockdowns.knock(living);
                this.dustAt(level, living.position(), 24, 0.8);
                ParticleFx.shockwave(level, ParticleFx.dust(PowerRing.PALE, 1.3F), living.position().add(0.0, 0.2,
                        0.0), 20, 0.4);
                this.storm.sound(level, living.position(), SoundEvents.ANVIL_LAND, 1.4F, 0.6F);
                this.storm.sound(level, living.position(), SoundEvents.GENERIC_EXPLODE.value(), 1.2F, 1.3F);
            }
            return true;
        });
    }

    // A black hole grows over the palm and draws everything round it in, round and round it ever faster, burning what
    // comes close; the fist crushes it and it bursts out, throwing everything away.
    private void blackHole(ServerLevel level) {
        HandPose.Place place = this.place();
        Vec3 hole = place.at(HandPose.HOLE_AT);
        if (this.t == HandPose.HOLE_FORMS) {
            this.storm.sound(level, hole, SoundEvents.WARDEN_SONIC_CHARGE, 2.0F, 0.5F);
            this.storm.sound(level, hole, SoundEvents.BEACON_ACTIVATE, 2.0F, 0.5F);
            this.storm.sound(level, hole, SoundEvents.END_PORTAL_SPAWN, 0.8F, 0.6F);
        }
        double pull = HandPose.holePull(this.t);
        for (TornBlock block : this.torn) {
            block.aim(hole);
        }
        if (pull > 0.01 && this.t < HandPose.HOLE_CRUSHES) {
            this.draw(level, hole, pull);
            this.tearGround(level, hole, pull);
            if (this.t % 3 == 0) {
                ParticleFx.send(level, ParticleTypes.PORTAL, hole.x, hole.y, hole.z, 12, 3.0, 3.0, 3.0, 1.0);
            }
            if (this.t % 2 == 0) {
                this.groundDust(level, hole, pull);
            }
            if (this.t % 4 == 0) {
                ParticleFx.implosion(level, ParticleTypes.REVERSE_PORTAL, hole, 6.0 * SCALE, 10, 0.6 * pull);
            }
            if (this.t % 20 == 0) {
                this.storm.sound(level, hole, SoundEvents.PORTAL_AMBIENT, 2.0F, 0.5F);
            }
        }
        if (this.t == HandPose.HOLE_CRUSHES) {
            this.storm.sound(level, hole, SoundEvents.ANVIL_LAND, 1.4F, 0.4F);
            this.storm.sound(level, hole, SoundEvents.WARDEN_HEARTBEAT, 2.4F, 0.6F);
        }
        if (this.t == HandPose.HOLE_BLASTS) {
            this.burst(level, hole);
        }
    }

    // Blocks of the ground round the hand torn out of the world, one or more a tick, the harder it pulls the more,
    // each set to fall in before the fist closes.
    private void tearGround(ServerLevel level, Vec3 hole, double pull) {
        int most = this.storm.ability.intValue("holeBlocks");
        int left = HandPose.HOLE_CRUSHES - this.t - 2;
        if (pull < 0.2 || left < HOLE_LAST) {
            return;
        }
        int tries = 1 + (int) (pull * 3.0);
        for (int k = 0; k < tries && this.tornCount < most; k++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0;
            double out = (2.5 + level.random.nextDouble() * (PULL_RADIUS - 4.0)) * SCALE;
            int x = Mth.floor(this.base.x + Math.cos(angle) * out);
            int z = Mth.floor(this.base.z + Math.sin(angle) * out);
            if (!level.isLoaded(new BlockPos(x, (int) this.base.y, z))) {
                continue;
            }
            int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, top - 1, z);
            BlockState state = level.getBlockState(pos);
            if (Math.abs(top - this.base.y) > 10.0 || state.isAir() || state.hasBlockEntity()
                    || !state.getFluidState().isEmpty()) {
                continue;
            }
            float hardness = state.getDestroySpeed(level, pos);
            if (hardness < 0.0F || hardness > HOLE_HARDEST || !BlockRules.mayBreak(level, this.storm.owner, pos,
                    state)) {
                continue;
            }
            // A plant on it goes first, so it never drops as an item.
            BlockPos above = pos.above();
            BlockState cover = level.getBlockState(above);
            if (!cover.isAir() && cover.getFluidState().isEmpty() && !cover.hasBlockEntity()
                    && BlockRules.mayBreak(level, this.storm.owner, above, cover)) {
                level.setBlock(above, Blocks.AIR.defaultBlockState(), 3);
            }
            double far = hole.distanceTo(Vec3.atCenterOf(pos));
            this.torn.add(TornBlock.tear(level, pos, state, hole, Math.min(left, 16 + (int) (far * 2.4))));
            this.tornCount++;
        }
        this.torn.removeIf(block -> !block.isAlive());
    }

    // Dust of the ground round the hand torn up and flying in.
    private void groundDust(ServerLevel level, Vec3 hole, double pull) {
        for (int k = 0; k < DUST; k++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0;
            double out = (4.0 + level.random.nextDouble() * (PULL_RADIUS - 4.0)) * SCALE;
            BlockPos column = BlockPos.containing(this.base.x + Math.cos(angle) * out, this.base.y,
                    this.base.z + Math.sin(angle) * out);
            if (!level.isLoaded(column)) {
                continue;
            }
            int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
            BlockState ground = level.getBlockState(new BlockPos(column.getX(), top - 1, column.getZ()));
            if (ground.isAir() || Math.abs(top - this.base.y) > 12.0) {
                continue;
            }
            Vec3 from = new Vec3(column.getX() + 0.5, top + 0.1, column.getZ() + 0.5);
            ParticleFx.fly(level, new BlockParticleOption(ParticleTypes.BLOCK, ground), from,
                    hole.subtract(from).normalize(), 0.5 + 0.6 * pull);
        }
    }

    private void draw(ServerLevel level, Vec3 hole, double pull) {
        boolean burns = this.t % CORE_EVERY == 0;
        for (LivingEntity living : this.near(level, PULL_RADIUS + 2.0)) {
            if (HeldMobs.isHeldByAnyone(living)) {
                continue;
            }
            Vec3 to = hole.subtract(living.getBoundingBox().getCenter());
            double far = to.length();
            if (far > PULL_RADIUS * SCALE || far < 1.0E-3) {
                continue;
            }
            Vec3 in = to.scale(1.0 / far);
            Vec3 round = in.cross(new Vec3(0.0, 1.0, 0.0));
            round = round.lengthSqr() < 1.0E-6 ? Vec3.ZERO : round.normalize();
            double near = 1.0 - far / (PULL_RADIUS * SCALE);
            Vec3 want = far < 1.6 ? round.scale(0.35).add(in.scale((far - 1.0) * 0.3))
                    : in.scale(0.4 + 0.85 * near).add(round.scale(0.4 * near));
            living.setDeltaMovement(living.getDeltaMovement().scale(1.0 - 0.5 * pull).add(want.scale(0.5 * pull)));
            living.resetFallDistance();
            living.hurtMarked = true;
            if (burns && far < CORE * SCALE) {
                Vec3 keep = living.getDeltaMovement();
                this.hit(level, living, this.part(CORE_DAMAGE), in, 0.0, 0.0);
                living.setDeltaMovement(keep);
            }
        }
    }

    private void burst(ServerLevel level, Vec3 hole) {
        for (LivingEntity living : this.near(level, BLAST_RADIUS + 2.0)) {
            Vec3 from = living.getBoundingBox().getCenter().subtract(hole);
            double far = from.length();
            if (far > BLAST_RADIUS * SCALE) {
                continue;
            }
            double share = 1.0 - 0.6 * far / (BLAST_RADIUS * SCALE);
            this.hit(level, living, this.part(BLAST_DAMAGE * share), flat(from), BLAST_OUT * share, BLAST_UP);
        }
        for (TornBlock block : this.torn) {
            block.discard();
        }
        this.torn.clear();
        // The burst reaches as far as its light runs out (client/render/hand/light/HandHoleLight).
        double reach = BLAST_RADIUS * SCALE;
        ParticleFx.send(level, ParticleTypes.EXPLOSION_EMITTER, hole.x, hole.y, hole.z, 2, 0.6, 0.6, 0.6, 0.0);
        ParticleFx.sphereOut(level, ParticleTypes.ELECTRIC_SPARK, hole, 60, 1.4);
        ParticleFx.sphereOut(level, ParticleTypes.END_ROD, hole, 50, 0.9);
        ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 2.4F), hole, 90, 1.3);
        ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.PALE, 1.6F), hole, 60, 0.7);
        ParticleFx.shockwave(level, ParticleFx.dust(PowerRing.PALE, 2.2F), this.base.add(0.0, 0.2, 0.0), 120,
                reach / 12.0);
        ParticleFx.shockwave(level, ParticleTypes.CLOUD, this.base.add(0.0, 0.3, 0.0), 60, reach / 16.0);
        this.groundBurst(level, reach);
        this.storm.sound(level, hole, SoundEvents.GENERIC_EXPLODE.value(), 3.0F, 0.5F);
        this.storm.sound(level, hole, SoundEvents.WARDEN_SONIC_BOOM, 2.0F, 0.7F);
        this.storm.sound(level, hole, SoundEvents.LIGHTNING_BOLT_THUNDER, 1.4F, 1.5F);
        this.storm.sound(level, hole, SoundEvents.DRAGON_FIREBALL_EXPLODE, 2.0F, 0.6F);
    }

    // The ground thrown up in a ring as the burst runs over it.
    private void groundBurst(ServerLevel level, double reach) {
        for (int k = 0; k < 40; k++) {
            double angle = Math.PI * 2.0 * k / 40.0 + level.random.nextDouble() * 0.15;
            double out = reach * (0.3 + 0.7 * level.random.nextDouble());
            int x = Mth.floor(this.base.x + Math.cos(angle) * out);
            int z = Mth.floor(this.base.z + Math.sin(angle) * out);
            if (!level.isLoaded(new BlockPos(x, (int) this.base.y, z))) {
                continue;
            }
            int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockState ground = level.getBlockState(new BlockPos(x, top - 1, z));
            if (ground.isAir() || Math.abs(top - this.base.y) > 10.0) {
                continue;
            }
            ParticleFx.send(level, new BlockParticleOption(ParticleTypes.BLOCK, ground), x + 0.5, top + 0.1,
                    z + 0.5, 8, 0.4, 0.2, 0.4, 0.3);
        }
    }
}
