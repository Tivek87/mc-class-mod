package nl.tivek.multiversepowers.character.thor.hammer;

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
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.thor.ThorCharge;
import nl.tivek.multiversepowers.character.thor.ThorMoves;
import nl.tivek.multiversepowers.character.thor.ThorStatePayload;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// What every part of the hammer shares: whether it is on Thor (in his right hand or on his belt) or out (flying out,
// resting in the world, flying back), where it is and which way it goes, the entity that shows it, and its one effect
// while it is out. Back on him it is caught in a hand or put on his belt.
abstract class MjolnirCore {
    enum State {
        HOME,
        OUT,
        RESTING,
        BACK
    }

    // How it was thrown: to come back, to stay, to be followed, or from the sky at a crowd.
    enum Throw {
        RETURN,
        STAY,
        FOLLOW,
        STORM
    }

    // Where it lands on him: the CATCH move's arg.
    enum Hand {
        RIGHT,
        LEFT,
        BELT
    }

    static final Map<UUID, Mjolnir> ALL = new HashMap<>();
    // It is caught this close to his hand (times his size).
    static final double CATCH = 1.8;
    // Flying out longer than this, it comes home by itself.
    static final int LONGEST = 300;

    final UUID owner;
    // On him: in his right hand (true) or on his belt; in flight his left hand holds it either way.
    boolean armed;
    // Drawn back for Throw and Follow.
    boolean cocked;
    State state = State.HOME;
    Throw kind = Throw.RETURN;
    @Nullable
    ThrownHammer shown;
    Vec3 at = Vec3.ZERO;
    Vec3 way = Vec3.ZERO;
    int age;
    float damage;
    // Who it hit on this trip, each only once.
    final Set<UUID> hit = new HashSet<>();
    private boolean running;

    MjolnirCore(UUID owner) {
        this.owner = owner;
    }

    abstract void out(ServerLevel level, ServerPlayer owner);

    abstract void rest(ServerLevel level, ServerPlayer owner);

    abstract void back(ServerLevel level, ServerPlayer owner);

    // Once it is on him again (`caught`: in a hand, else come home from far off).
    abstract void homed(ServerPlayer owner, Hand hand, boolean caught);

    // Every tick it is out, after it moved: the lightning it shoots of its own.
    abstract void arcs(ServerLevel level, ServerPlayer owner);

    // Its one effect, while it is out.
    final void run(ServerLevel level) {
        if (this.running) {
            return;
        }
        this.running = true;
        Effects.start(level, (lvl, tick) -> {
            this.running = this.tick(lvl);
            return this.running;
        });
    }

    private boolean tick(ServerLevel level) {
        if (ALL.get(this.owner) != this || this.state == State.HOME) {
            return false;
        }
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(this.owner);
        if (owner == null || !owner.isAlive() || owner.level() != level) {
            this.home(owner, Hand.BELT, false);
            return false;
        }
        if (this.shown != null && this.shown.isRemoved()) {
            if (this.state != State.BACK) {
                this.home(owner, Hand.BELT, false);
                return false;
            }
            // Flying home over land no one has loaded, it was unloaded with it: it flies on and is shown again there.
            this.shown = null;
        }
        if (this.state == State.BACK && this.shown == null && level.isLoaded(BlockPos.containing(this.at))) {
            this.shown = this.show(level, owner, this.at, ThrownHammer.BACK);
        }
        // Flying home it always gets there, however far: only a throw out runs out.
        if (this.state == State.OUT && ++this.age > LONGEST) {
            this.home(owner, Hand.BELT, false);
            return false;
        }
        if (this.shown != null) {
            this.shown.setCharged(ThorCharge.hammer(owner) > 1.0F);
        }
        switch (this.state) {
            case OUT -> this.out(level, owner);
            case RESTING -> this.rest(level, owner);
            case BACK -> this.back(level, owner);
            case HOME -> {
            }
        }
        if (this.state != State.HOME) {
            this.arcs(level, owner);
        }
        return this.state != State.HOME;
    }

    final ThrownHammer show(ServerLevel level, ServerPlayer owner, Vec3 at, byte rest) {
        ThrownHammer shown = new ThrownHammer(ThrownHammer.TYPE.get(), level);
        shown.setSize(owner.getScale());
        shown.setCharged(ThorCharge.hammer(owner) > 1.0F);
        shown.setOwner(owner.getId());
        shown.setRest(rest);
        Vec3 lead = this.lead();
        shown.moveTo(at.x, at.y, at.z, yaw(lead), headFirst(lead));
        level.addFreshEntity(shown);
        return shown;
    }

    // Moves it on, its head leading the way it goes out and its grip the way it comes back, into his hand.
    final void move(Vec3 to) {
        this.at = to;
        if (this.shown != null) {
            Vec3 lead = this.lead();
            this.shown.moveTo(to.x, to.y, to.z, yaw(lead), headFirst(lead));
        }
    }

    private Vec3 lead() {
        return this.state == State.BACK ? this.way.reverse() : this.way;
    }

    static float yaw(Vec3 way) {
        return (float) Math.toDegrees(Math.atan2(-way.x, way.z));
    }

    // It never spins: one end leads the way it flies (0 stands that end up, 90 lays it level).
    static float headFirst(Vec3 way) {
        return (float) (90.0 - Math.toDegrees(Math.atan2(way.y, Math.sqrt(way.x * way.x + way.z * way.z))));
    }

    final void trail(ServerLevel level, ServerPlayer owner) {
        ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, this.at, 2, 0.15, 0.05);
        if (ThorCharge.hammer(owner) > 1.0F) {
            ParticleFx.at(level, ParticleFx.dust(ThorMoves.GLOW, 1.2F), this.at);
        }
    }

    // Back on him: slammed into a hand or onto his belt with a smack and a crack of thunder, or (come home from far
    // off, or with him gone) simply there again.
    final void home(@Nullable ServerPlayer owner, Hand hand, boolean caught) {
        if (this.shown != null) {
            this.shown.discard();
            this.shown = null;
        }
        this.state = State.HOME;
        this.cocked = false;
        if (hand == Hand.RIGHT) {
            this.armed = true;
        } else if (hand == Hand.BELT) {
            this.armed = false;
        }
        if (owner == null) {
            return;
        }
        this.homed(owner, hand, caught);
        ServerLevel level = owner.serverLevel();
        double size = owner.getScale();
        Vec3 at = Mjolnir.where(owner);
        if (caught) {
            level.playSound(null, at.x, at.y, at.z, SoundEvents.MACE_SMASH_AIR, SoundSource.PLAYERS, 0.9F, 1.3F);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 1.0F, 0.8F);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.35F,
                    1.9F);
            ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, at, 14, 0.2 * size, 0.15);
        } else {
            level.playSound(null, owner.getX(), owner.getY() + size, owner.getZ(), SoundEvents.TRIDENT_RETURN,
                    SoundSource.PLAYERS, 0.7F, 0.9F);
            ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, owner.position().add(0.0, size, 0.0), 10,
                    0.3 * size, 0.1);
        }
        ThorMoves.tell(owner, caught ? ThorStatePayload.CATCH : ThorStatePayload.NONE, hand.ordinal());
    }
}
