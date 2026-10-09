package nl.tivek.multiversepowers.character.thor.hammer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import nl.tivek.multiversepowers.character.thor.ThorMoves;
import nl.tivek.multiversepowers.character.thor.ThorStatePayload;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;

// Thor pulled to his hammer where it rests: first the wait, his hand stretched to it while a line of lightning grows
// between them, then the dash, which his own game makes at a set pace. The server keeps him from being taken for
// someone flying unlawfully, spares him the fall, shows the streak to everyone, and when he gets there has him catch
// it: with ground close under it he lands by it, hammer in his right hand; else in his left, flying on. A hard hit or a
// knockdown breaks it off; the hammer rests on.
public final class HammerPull {
    // No fall hurts him this long after it ends, caught or not.
    private static final int SPARED = 40;
    private static final int SLOWEST_LEFT = 20;
    private static final int NEAR_WAIT = 6;
    private static final Map<UUID, HammerPull> ALL = new HashMap<>();

    private final ServerPlayer owner;
    private final double pace;
    private int age;
    private int longest;
    private int near;
    private Vec3 trail;

    private HammerPull(ServerPlayer owner, double pace) {
        this.owner = owner;
        this.pace = pace;
        this.trail = chest(owner);
        this.longest = HammerRules.WAIT + SLOWEST_LEFT;
    }

    // Starts the pull to his hammer, resting or on its way to rest; `pace` is the dash's speed in blocks a tick.
    static boolean start(ServerPlayer player, double pace) {
        return start(player, pace, false);
    }

    // `tossed`: up after the hammer a Storm Throw tossed, at its own pace.
    static boolean start(ServerPlayer player, double pace, boolean tossed) {
        if (pulling(player) || player.isPassenger() || ThorMoves.flying(player)) {
            return false;
        }
        HammerPull pull = new HammerPull(player, Math.max(0.1, pace));
        ALL.put(player.getUUID(), pull);
        ThorMoves.spare(player, HammerRules.WAIT + SPARED);
        ThorMoves.tell(player, ThorStatePayload.PULL, tossed ? ThorStatePayload.TOSSED : 0);
        ServerLevel level = player.serverLevel();
        Vec3 at = chest(player);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.6F, 1.8F);
        Effects.start(level, (lvl, tick) -> pull.tick(lvl));
        return true;
    }

    public static boolean pulling(ServerPlayer player) {
        HammerPull pull = ALL.get(player.getUUID());
        return pull != null && pull.owner == player;
    }

    public static int flags(ServerPlayer player) {
        return pulling(player) ? ThorStatePayload.PULLING : 0;
    }

    // His game says he got there: taken on his word while the hammer rests and he is about there.
    public static void arrive(ServerPlayer player) {
        HammerPull pull = ALL.get(player.getUUID());
        Vec3 hammer = Mjolnir.out(player);
        if (pull == null || hammer == null || !Mjolnir.resting(player)
                || chest(player).distanceTo(hammer) > HammerRules.THERE * player.getScale() + HammerRules.SLACK) {
            return;
        }
        pull.reach(hammer);
    }

    // Broken off (a hard hit, a knockdown): he drops, unhurt by the fall, and the hammer rests on.
    public static void stop(ServerPlayer player) {
        HammerPull pull = ALL.get(player.getUUID());
        if (pull != null) {
            pull.end();
        }
    }

    public static void leave(ServerPlayer player) {
        ALL.remove(player.getUUID());
    }

    public static void clear() {
        ALL.clear();
    }

    private boolean tick(ServerLevel level) {
        ServerPlayer player = this.owner;
        if (ALL.get(player.getUUID()) != this) {
            return false;
        }
        Vec3 hammer = Mjolnir.out(player);
        if (player.isRemoved() || !player.isAlive() || player.level() != level || hammer == null
                || !Mjolnir.resting(player) && !Mjolnir.goingOut(player)) {
            this.end();
            return false;
        }
        this.age++;
        player.resetFallDistance();
        player.connection.aboveGroundTickCount = 0;
        Vec3 chest = chest(player);
        if (this.age <= HammerRules.WAIT) {
            this.charge(level, player, hammer);
        } else {
            if (this.age == HammerRules.WAIT + 1) {
                this.longest = this.age + (int) Math.ceil(chest.distanceTo(hammer) / this.pace) + SLOWEST_LEFT;
                ThorMoves.spare(player, this.longest - this.age + SPARED);
                level.playSound(null, chest.x, chest.y, chest.z, SoundEvents.TRIDENT_RIPTIDE_3.value(),
                        SoundSource.PLAYERS, 0.9F, 1.3F);
                level.playSound(null, chest.x, chest.y, chest.z, SoundEvents.LIGHTNING_BOLT_THUNDER,
                        SoundSource.PLAYERS, 0.35F, 1.8F);
            }
            if (this.trail.distanceToSqr(chest) > 0.04) {
                ParticleFx.zigzag(level, ParticleFx.dust(ThorMoves.GLOW, 1.3F), this.trail, chest, 4, 0.35, 0.3);
                ParticleFx.zigzag(level, ParticleFx.dust(ThorMoves.DEEP, 0.9F), this.trail, chest, 3, 0.5, 0.4);
            }
            // While it still flies out the dash waits on it.
            if (Mjolnir.goingOut(player)) {
                this.longest++;
            }
        }
        this.trail = chest;
        // His own game says when he is there; only when it stays silent this close does the server catch for him.
        this.near = Mjolnir.resting(player) && chest.distanceTo(hammer) < HammerRules.THERE * player.getScale()
                ? this.near + 1 : 0;
        if (this.near > NEAR_WAIT) {
            this.reach(hammer);
            return false;
        }
        if (this.age > this.longest) {
            this.end();
            return false;
        }
        return true;
    }

    // The wait: a line of lightning from his reaching hand to the hammer, brighter each tick, with a rising crackle.
    private void charge(ServerLevel level, ServerPlayer player, Vec3 hammer) {
        float grown = (float) this.age / HammerRules.WAIT;
        Vec3 hand = MjolnirFlight.hand(player, !landsBy(level, hammer));
        ParticleFx.zigzag(level, ParticleFx.dust(ThorMoves.GLOW, 0.5F + 0.8F * grown), hand, hammer,
                4 + (int) (4 * grown), 0.3, 0.6 - 0.3 * grown);
        ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, hand, 2 + (int) (4 * grown), 0.1, 0.08);
        if (this.age % 3 == 1) {
            level.playSound(null, hand.x, hand.y, hand.z, SoundEvents.COPPER_BULB_TURN_ON, SoundSource.PLAYERS,
                    0.4F + 0.4F * grown, 1.2F + 0.8F * grown);
        }
    }

    // There: by the arrival rule he lands by it or flies on with it.
    private void reach(Vec3 hammer) {
        ServerPlayer player = this.owner;
        ALL.remove(player.getUUID(), this);
        boolean land = landsBy(player.serverLevel(), hammer);
        ThorMoves.spare(player, SPARED);
        if (!land) {
            ThorMoves.flyOn(player);
        }
        Mjolnir.reached(player, land);
        if (land) {
            ServerLevel level = player.serverLevel();
            ParticleFx.cloud(level, ParticleTypes.CLOUD, player.position().add(0.0, 0.1, 0.0), 10, 0.4, 0.04);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.MACE_SMASH_GROUND,
                    SoundSource.PLAYERS, 0.6F, 1.2F);
        }
    }

    private void end() {
        ALL.remove(this.owner.getUUID(), this);
        ThorMoves.spare(this.owner, SPARED);
        ThorMoves.tell(this.owner, ThorStatePayload.NONE, 0);
    }

    // The arrival rule: ground this close straight under the hammer, he lands by it.
    static boolean landsBy(ServerLevel level, Vec3 hammer) {
        return LoadedWorld.clip(level, new ClipContext(hammer.add(0.0, 0.05, 0.0),
                hammer.add(0.0, -HammerRules.GROUND_BELOW, 0.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY,
                CollisionContext.empty())).getType() != HitResult.Type.MISS;
    }

    private static Vec3 chest(ServerPlayer player) {
        return player.position().add(0.0, 0.9 * player.getScale(), 0.0);
    }
}
