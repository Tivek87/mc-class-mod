package nl.tivek.multiversepowers.character.thor.hammer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import nl.tivek.multiversepowers.character.thor.ThorBlow;
import nl.tivek.multiversepowers.character.thor.ThorMoves;
import nl.tivek.multiversepowers.character.thor.ThorStatePayload;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.target.Targeting;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;

// The Storm Throw, as Thor throws it in God of War: on the ground he flings the hammer high up, dashes up after it on
// a streak of lightning (HammerPull), catches it at the top, holds it high over his head while the storm gathers in it
// and hurls it down as a bolt just short of his foe, then drops; in flight he only holds it up and hurls it. Where it
// strikes its lightning bursts up in a V that opens past the foe (StormStrike), and it flies back to him. Until it is
// back his flight never fails him for want of it.
public final class StormThrow {
    // He tosses it this high over his eyes and this share of the way towards his foe, at most this far; with less room
    // than TOSS_LOWEST over his feet (a ceiling) there is no Storm Throw from the ground.
    private static final double TOSS_UP = 13.0;
    private static final double TOSS_TOWARDS = 0.3;
    private static final double TOSS_FARTHEST = 7.0;
    private static final double TOSS_LOWEST = 6.0;
    private static final double UNDER_ROOF = 1.5;
    // The foe he aims at or the ground he looks at is this far off at most; hurled, it flies this far at most.
    private static final double REACH = 48.0;
    private static final double HURL_REACH = 72.0;
    // It strikes this far short of the foe, on his side, so the foe stands in the V.
    private static final double SHORT = 1.6;
    private static final double GROUND_LOOK = 8.0;
    // Dropping after the hurl, no fall hurts him this long.
    private static final int SPARED = 100;
    private static final int LONGEST = 240;
    private static final Map<UUID, StormThrow> ALL = new HashMap<>();

    private enum Phase {
        TOSS,
        PULL,
        HURL,
        OUT
    }

    private final ServerPlayer owner;
    private final float damage;
    private final boolean fromGround;
    @Nullable
    private final LivingEntity foe;
    private final Vec3 spot;
    private Vec3 apex = Vec3.ZERO;
    private Phase phase = Phase.TOSS;
    private int age;
    private int phaseAge;

    private StormThrow(ServerPlayer owner, float damage, boolean fromGround, @Nullable LivingEntity foe, Vec3 spot) {
        this.owner = owner;
        this.damage = damage;
        this.fromGround = fromGround;
        this.foe = foe;
        this.spot = spot;
    }

    // From the ground the whole throw, in flight only the hurl; never without the hammer on him, a foe or ground where
    // he aims, or (on the ground) room over him.
    public static boolean start(ServerPlayer player, float damage) {
        if (find(player) != null || !Mjolnir.home(player) || HammerPull.pulling(player) || player.isPassenger()) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        LivingEntity foe = Targeting.aimLiving(player, level, REACH);
        Vec3 spot;
        if (foe != null) {
            spot = foe.position();
        } else {
            Vec3 eye = player.getEyePosition();
            BlockHitResult block = LoadedWorld.clip(level, new ClipContext(eye,
                    eye.add(player.getLookAngle().scale(REACH)), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY,
                    player));
            if (block.getType() == HitResult.Type.MISS) {
                return false;
            }
            spot = block.getLocation();
        }
        boolean flying = ThorMoves.flying(player);
        StormThrow storm = new StormThrow(player, damage, !flying, foe, spot);
        if (flying) {
            if (!storm.hurl(level)) {
                return false;
            }
        } else {
            Vec3 apex = apex(level, player, spot);
            if (apex == null) {
                return false;
            }
            storm.apex = apex;
            ThorMoves.tell(player, ThorStatePayload.STRIKE, ThorBlow.STORM_TOSS.ordinal());
        }
        ALL.put(player.getUUID(), storm);
        Effects.start(level, (lvl, tick) -> storm.tick(lvl));
        return true;
    }

    // Under way, the hammer not yet hurled: nothing else meanwhile.
    public static boolean busy(ServerPlayer player) {
        StormThrow storm = find(player);
        return storm != null && storm.phase != Phase.OUT;
    }

    // Under way until the hammer is back with him.
    public static boolean active(ServerPlayer player) {
        return find(player) != null;
    }

    // Broken off (a knockdown): a hammer still in his hand stays there.
    public static void stop(ServerPlayer player) {
        if (ALL.remove(player.getUUID()) != null) {
            Mjolnir.keep(player);
        }
    }

    public static void leave(ServerPlayer player) {
        ALL.remove(player.getUUID());
    }

    public static void clear() {
        ALL.clear();
    }

    @Nullable
    private static StormThrow find(ServerPlayer player) {
        StormThrow storm = ALL.get(player.getUUID());
        return storm != null && storm.owner == player ? storm : null;
    }

    private boolean tick(ServerLevel level) {
        ServerPlayer player = this.owner;
        if (ALL.get(player.getUUID()) != this) {
            return false;
        }
        this.phaseAge++;
        boolean on = !player.isRemoved() && player.isAlive() && player.level() == level && ++this.age <= LONGEST
                && switch (this.phase) {
                    case TOSS -> this.phaseAge < ThorBlow.STORM_TOSS.hit() - MjolnirFlight.RELEASE || this.toss(player);
                    case PULL -> HammerPull.pulling(player)
                            || Mjolnir.home(player) && ThorMoves.flying(player) && this.hurl(level);
                    case HURL -> this.winding(level, player);
                    case OUT -> !Mjolnir.home(player);
                };
        if (!on) {
            ALL.remove(player.getUUID(), this);
        }
        return on;
    }

    // The toss lets go of it: it flies up to hang at the top, and he goes up after it.
    private boolean toss(ServerPlayer player) {
        if (!Mjolnir.toss(player, this.apex) || !HammerPull.start(player, HammerRules.STORM_PACE, true)) {
            return false;
        }
        this.next(Phase.PULL);
        return true;
    }

    // Caught at the top (or flying already): raised high, then hurled down at his foe.
    private boolean hurl(ServerLevel level) {
        if (!Mjolnir.hurl(this.owner, this.damage, this.aim(level), HURL_REACH)) {
            return false;
        }
        this.next(Phase.HURL);
        Vec3 at = this.owner.position().add(0.0, 2.4 * this.owner.getScale(), 0.0);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0F, 0.6F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.4F, 1.6F);
        return true;
    }

    // Held high while the storm gathers in it, its aim kept on the foe, then let go: from the ground he drops once it
    // is out of his hand.
    private boolean winding(ServerLevel level, ServerPlayer player) {
        if (Mjolnir.home(player)) {
            return false;
        }
        if (this.phaseAge < ThorBlow.STORM_THROW.hit()) {
            Mjolnir.retarget(player, this.aim(level));
            this.gather(level, player);
            return true;
        }
        if (this.fromGround) {
            ThorMoves.land(player, false);
            ThorMoves.spare(player, SPARED);
        }
        this.next(Phase.OUT);
        return true;
    }

    private void next(Phase phase) {
        this.phase = phase;
        this.phaseAge = 0;
    }

    // The storm gathering in the hammer held high: sparks round it and a crackle rising in pitch.
    private void gather(ServerLevel level, ServerPlayer player) {
        double size = player.getScale();
        Vec3 at = player.position().add(0.0, 2.4 * size, 0.0);
        float grown = (float) this.phaseAge / ThorBlow.STORM_THROW.hit();
        ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, at, 2 + (int) (6 * grown), 0.3 * size, 0.12);
        if (this.phaseAge % 3 == 0) {
            level.playSound(null, at.x, at.y, at.z, SoundEvents.COPPER_BULB_TURN_ON, SoundSource.PLAYERS,
                    0.5F + 0.4F * grown, 1.0F + 0.9F * grown);
        }
    }

    // Where it is hurled: the ground just short of his foe, where the foe is now, else the ground he aimed at.
    private Vec3 aim(ServerLevel level) {
        Vec3 at = this.spot;
        if (this.foe != null && this.foe.isAlive() && this.foe.level() == level) {
            Vec3 feet = this.foe.position();
            Vec3 flat = new Vec3(feet.x - this.owner.getX(), 0.0, feet.z - this.owner.getZ());
            at = flat.lengthSqr() > SHORT * SHORT ? feet.subtract(flat.normalize().scale(SHORT)) : feet;
        }
        BlockHitResult ground = LoadedWorld.clip(level, new ClipContext(at.add(0.0, 1.0, 0.0),
                at.add(0.0, -GROUND_LOOK, 0.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY,
                CollisionContext.empty()));
        return ground.getType() == HitResult.Type.MISS ? at : ground.getLocation();
    }

    // Where the tossed hammer hangs for him: high over him and partway towards his foe, under any roof; null without
    // room enough, or with ground so close under it that he would land by it rather than fly on with it.
    @Nullable
    private static Vec3 apex(ServerLevel level, ServerPlayer player, Vec3 toward) {
        Vec3 eye = player.getEyePosition();
        Vec3 flat = new Vec3(toward.x - eye.x, 0.0, toward.z - eye.z);
        double far = flat.length();
        Vec3 out = far < 1.0E-4 ? Vec3.ZERO : flat.scale(Math.min(TOSS_FARTHEST, far * TOSS_TOWARDS) / far);
        Vec3 top = eye.add(out).add(0.0, TOSS_UP, 0.0);
        BlockHitResult roof = LoadedWorld.clip(level, new ClipContext(eye, top, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        if (roof.getType() != HitResult.Type.MISS) {
            double room = roof.getLocation().distanceTo(eye) - UNDER_ROOF;
            top = eye.add(top.subtract(eye).normalize().scale(Math.max(0.0, room)));
        }
        return top.y - player.getY() < TOSS_LOWEST || HammerPull.landsBy(level, top) ? null : top;
    }
}
