package nl.tivek.multiversepowers.character.thor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;

// Thor's moves on the server. His own game moves him (a dash, a super jump, flight, a blink); the server keeps him
// from being taken for someone hanging in the air unlawfully, spares him the fall, and shows every move to all who
// see him. One of these lives while he jumps, floats or flies.
@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class ThorMoves {
    static final int GLOW = 0x9FE8FF;
    static final int DEEP = 0x3FA2FF;
    // A super jump's float starts at its peak and lasts this long; its landing is cushioned this long after.
    static final int FLOAT_TICKS = 50;
    private static final int CUSHION = 10;
    private static final int LONGEST_JUMP = 400;
    // A hit this hard (half hearts) knocks him out of the sky.
    private static final float STUN = 5.0F;
    // Standing this long in flight lands him, if his game never said so.
    private static final int GROUNDED = 12;
    // A thunderclap wound up this long without going off (its button's letting go lost) stops showing.
    private static final int CHARGE_LONGEST = 40;
    static final double BLINK = 15.0;

    private static final Map<UUID, ThorMoves> ALL = new HashMap<>();

    private final ServerPlayer owner;
    private boolean flying;
    private int flightAge;
    private int grounded;
    private boolean lightning;
    private int lightningLeft;
    private float strikes;
    private float landing;
    private Vec3 trail = Vec3.ZERO;
    // When each creature near was last struck at lightning speed (game time).
    private final Map<UUID, Long> struck = new HashMap<>();
    private boolean jumping;
    private int jumpAge;
    private int floatAge = -1;
    private double lastY;
    private int cushion;
    private boolean charging;
    private int chargeAge;
    @Nullable
    GrabDive dive;

    private ThorMoves(ServerPlayer owner) {
        this.owner = owner;
        this.lastY = owner.getY();
    }

    private static ThorMoves of(ServerPlayer player) {
        ThorMoves moves = ALL.get(player.getUUID());
        if (moves == null || moves.owner != player) {
            moves = new ThorMoves(player);
            ALL.put(player.getUUID(), moves);
            ThorMoves started = moves;
            Effects.start(player.serverLevel(), (level, age) -> started.tick(level));
        }
        return moves;
    }

    @Nullable
    static ThorMoves find(ServerPlayer player) {
        ThorMoves moves = ALL.get(player.getUUID());
        return moves != null && moves.owner == player ? moves : null;
    }

    static boolean flying(ServerPlayer player) {
        ThorMoves moves = find(player);
        return moves != null && moves.flying;
    }

    // Whether a fall must not hurt him now: jumping, flying, or just landed from either.
    static boolean cushioned(ServerPlayer player) {
        ThorMoves moves = find(player);
        return moves != null && (moves.jumping || moves.flying || moves.cushion > 0);
    }

    int flags() {
        int flags = 0;
        flags |= this.flying ? ThorStatePayload.FLYING : 0;
        flags |= this.floatAge >= 0 && this.floatAge < FLOAT_TICKS ? ThorStatePayload.FLOATING : 0;
        flags |= this.lightning ? ThorStatePayload.LIGHTNING : 0;
        flags |= this.dive != null && this.dive.carrying() ? ThorStatePayload.CARRYING : 0;
        flags |= this.charging ? ThorStatePayload.CHARGING : 0;
        return flags | gear(this.owner);
    }

    // What lasts past his moves: the hammer, a grab, a charge.
    private static int gear(ServerPlayer player) {
        return Mjolnir.flags(player) | ThorCharge.flags(player)
                | (ThorGrab.carrying(player) ? ThorStatePayload.CARRYING : 0);
    }

    // His game says he winds up a thunderclap, or lets go of it before it went off; the clap itself ends it too.
    static void charging(ServerPlayer player, boolean on) {
        ThorMoves moves = on ? of(player) : find(player);
        if (moves == null || moves.charging == on) {
            return;
        }
        moves.charging = on;
        moves.chargeAge = 0;
        moves.sync(ThorStatePayload.NONE, 0);
    }

    static int flags(ServerPlayer player) {
        ThorMoves moves = find(player);
        return moves == null ? gear(player) : moves.flags();
    }

    // Tells everyone who sees him what he is now, with a move that starts (or NONE).
    static void tell(ServerPlayer player, int move, int arg) {
        ThorStatePayload.send(player, flags(player), move, arg);
    }

    // A move of his that may throw him high (a leap after the hammer, a hoist): no fall hurts him for this long.
    static void spare(ServerPlayer player, int ticks) {
        ThorMoves moves = of(player);
        moves.cushion = Math.max(moves.cushion, ticks);
    }

    void sync(int move, int arg) {
        ThorStatePayload.send(this.owner, this.flags(), move, arg);
    }

    // A quick dash along the ground, the way his game says he was moving: `yaw` in 256ths of a turn, `tenths` of a
    // block far.
    static boolean dash(ServerPlayer player, int yaw, int tenths) {
        if (flying(player) || player.isPassenger()) {
            return false;
        }
        ThorMoves moves = of(player);
        moves.sync(ThorStatePayload.DASH, (yaw & 0xFF) | (tenths & 0xFF) << 8);
        ServerLevel level = player.serverLevel();
        moves.sound(level, SoundEvents.TRIDENT_RIPTIDE_1.value(), 0.7F, 1.5F);
        moves.sound(level, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.35F, 1.9F);
        UUID id = player.getUUID();
        Effects.start(level, (lvl, age) -> {
            ServerPlayer thor = lvl.getServer().getPlayerList().getPlayer(id);
            if (thor == null || thor.level() != lvl || age > 9) {
                return false;
            }
            Vec3 feet = thor.position();
            ParticleFx.cloud(lvl, ParticleTypes.ELECTRIC_SPARK, feet.add(0.0, 0.9 * thor.getScale(), 0.0), 4, 0.25,
                    0.08);
            if (age < 6) {
                ParticleFx.cloud(lvl, ParticleTypes.CLOUD, feet.add(0.0, 0.1, 0.0), 2, 0.15, 0.02);
            }
            return true;
        });
        return true;
    }

    static boolean superJump(ServerPlayer player) {
        if (flying(player) || player.isPassenger()) {
            return false;
        }
        ThorMoves moves = of(player);
        moves.jumping = true;
        moves.jumpAge = 0;
        moves.floatAge = -1;
        moves.lastY = player.getY();
        moves.sync(ThorStatePayload.JUMP, 0);
        ServerLevel level = player.serverLevel();
        Vec3 feet = player.position();
        ParticleFx.shockwave(level, ParticleFx.dust(GLOW, 1.3F), feet.add(0.0, 0.1, 0.0), 28, 0.45);
        ParticleFx.cloud(level, ParticleTypes.CLOUD, feet.add(0.0, 0.2, 0.0), 14, 0.4, 0.06);
        ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, feet.add(0.0, 0.6 * player.getScale(), 0.0), 20, 0.4,
                0.2);
        moves.sound(level, SoundEvents.WIND_CHARGE_BURST.value(), 0.9F, 0.7F);
        moves.sound(level, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.5F, 1.4F);
        return true;
    }

    static boolean takeOff(ServerPlayer player) {
        if (flying(player) || player.isPassenger() || player.isSleeping() || player.isFallFlying()) {
            return false;
        }
        ThorMoves moves = of(player);
        moves.flying = true;
        moves.flightAge = 0;
        moves.grounded = 0;
        moves.jumping = false;
        moves.floatAge = -1;
        moves.sync(ThorStatePayload.TAKE_OFF, 0);
        ServerLevel level = player.serverLevel();
        moves.sound(level, SoundEvents.TRIDENT_RIPTIDE_2.value(), 0.8F, 0.8F);
        moves.sound(level, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.4F, 1.6F);
        ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, player.position().add(0.0, 0.3 * player.getScale(), 0.0),
                24, 0.5, 0.25);
        return true;
    }

    // His game says he touched down, or something knocks him out of the sky. Landing at lightning speed, the bolt he
    // has become strikes where he lands.
    static void land(ServerPlayer player, boolean touched) {
        ThorMoves moves = find(player);
        if (moves == null || !moves.flying) {
            return;
        }
        boolean bolt = moves.lightning && touched;
        moves.flying = false;
        moves.lightning = false;
        moves.cushion = CUSHION;
        if (moves.dive != null) {
            moves.dive.end();
        }
        moves.sync(touched ? ThorStatePayload.TOUCH_DOWN : ThorStatePayload.NONE, 0);
        if (touched) {
            ServerLevel level = player.serverLevel();
            ParticleFx.cloud(level, ParticleTypes.CLOUD, player.position().add(0.0, 0.1, 0.0), 10, 0.4, 0.04);
            moves.sound(level, SoundEvents.MACE_SMASH_GROUND, 0.5F, 1.3F);
            if (bolt) {
                SkyMoves.landingStrike(player, moves.landing);
            }
        }
    }

    // Lightning speed for `ticks`: he ends it by landing, or it wears off. `strikes` hurts each foe he passes, `landing`
    // what stands where he lands.
    static boolean lightning(ServerPlayer player, int ticks, float strikes, float landing) {
        ThorMoves moves = find(player);
        if (moves == null || !moves.flying || moves.lightning) {
            return false;
        }
        moves.lightning = true;
        moves.lightningLeft = ticks;
        moves.strikes = strikes;
        moves.landing = landing;
        moves.trail = player.position().add(0.0, 0.9 * player.getScale(), 0.0);
        moves.sync(ThorStatePayload.NONE, 0);
        ServerLevel level = player.serverLevel();
        moves.sound(level, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.6F, 1.7F);
        moves.sound(level, SoundEvents.TRIDENT_RIPTIDE_3.value(), 0.9F, 1.2F);
        return true;
    }

    // A blink of lightning fifteen blocks the way his game says he was going (`yaw` and `pitch` in 256ths), short of
    // any wall: his game moves him, this shows it.
    static boolean blink(ServerPlayer player, int yaw, int pitch) {
        ThorMoves moves = find(player);
        if (moves == null || !moves.flying) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        Vec3 from = player.position().add(0.0, 0.9 * player.getScale(), 0.0);
        Vec3 to = blinkEnd(level, player, from, blinkWay(yaw, pitch));
        moves.sync(ThorStatePayload.BLINK, 0);
        ParticleFx.zigzag(level, ParticleFx.dust(GLOW, 1.1F), from, to, 7, 0.5, 0.35);
        ParticleFx.zigzag(level, ParticleFx.dust(DEEP, 0.8F), from, to, 5, 0.7, 0.5);
        ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, from, 16, 0.3, 0.2);
        ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, to, 16, 0.3, 0.2);
        moves.sound(level, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.9F, 1.5F);
        level.playSound(null, to.x, to.y, to.z, SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 0.6F,
                1.8F);
        return true;
    }

    // A way packed into two bytes: yaw in 256ths of a turn, pitch in 256ths of half a turn from straight up.
    public static Vec3 blinkWay(int yaw, int pitch) {
        float yawDegrees = yaw * 360.0F / 256.0F;
        float pitchDegrees = pitch * 180.0F / 255.0F - 90.0F;
        return Vec3.directionFromRotation(pitchDegrees, yawDegrees);
    }

    // Where a blink from `from` along `look` ends: half a block short of the first block in the way.
    static Vec3 blinkEnd(ServerLevel level, ServerPlayer player, Vec3 from, Vec3 look) {
        Vec3 end = from.add(look.scale(BLINK));
        HitResult hit = LoadedWorld.clip(level, new ClipContext(from, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.MISS) {
            return end;
        }
        double far = Math.max(0.0, hit.getLocation().distanceTo(from) - 0.6 * player.getScale());
        return from.add(look.scale(far));
    }

    private boolean tick(ServerLevel level) {
        ServerPlayer player = this.owner;
        if (ALL.get(player.getUUID()) != this) {
            return false;
        }
        if (player.isRemoved() || !player.isAlive() || player.level() != level) {
            this.stop();
            return false;
        }
        if (this.flying) {
            this.flightAge++;
            player.resetFallDistance();
            player.connection.aboveGroundTickCount = 0;
            this.grounded = player.onGround() && this.flightAge > 20 ? this.grounded + 1 : 0;
            if (this.grounded >= GROUNDED) {
                land(player, true);
            } else if (this.lightning) {
                this.bolting(level, player);
            }
        }
        if (this.jumping) {
            this.jumpAge++;
            player.resetFallDistance();
            player.connection.aboveGroundTickCount = 0;
            double y = player.getY();
            if (this.floatAge < 0 && this.jumpAge > 3 && y <= this.lastY) {
                this.floatAge = 0;
                this.sync(ThorStatePayload.NONE, 0);
            } else if (this.floatAge >= 0 && ++this.floatAge == FLOAT_TICKS) {
                this.sync(ThorStatePayload.NONE, 0);
            }
            this.lastY = y;
            if (this.jumpAge > 5 && player.onGround() || this.jumpAge > LONGEST_JUMP) {
                this.jumping = false;
                this.floatAge = -1;
                this.cushion = CUSHION;
                this.sync(ThorStatePayload.TOUCH_DOWN, 0);
            }
        }
        if (this.cushion > 0) {
            this.cushion--;
            player.resetFallDistance();
        }
        if (this.charging && ++this.chargeAge > CHARGE_LONGEST) {
            this.charging = false;
            this.sync(ThorStatePayload.NONE, 0);
        }
        if (!this.flying && !this.jumping && this.cushion <= 0 && this.dive == null && !this.charging) {
            ALL.remove(player.getUUID(), this);
            return false;
        }
        return true;
    }

    // At lightning speed others see a bolt tearing through the air where he goes, and it strikes whatever he passes
    // near; it wears off after its time.
    private void bolting(ServerLevel level, ServerPlayer player) {
        Vec3 at = player.position().add(0.0, 0.9 * player.getScale(), 0.0);
        if (this.trail.distanceToSqr(at) > 0.04) {
            ParticleFx.zigzag(level, ParticleFx.dust(GLOW, 1.3F), this.trail, at, 4, 0.35, 0.3);
            ParticleFx.zigzag(level, ParticleFx.dust(DEEP, 0.9F), this.trail, at, 3, 0.5, 0.4);
        }
        this.trail = at;
        if (this.flightAge % 2 == 0) {
            ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, at, 5, 0.35, 0.15);
        }
        SkyMoves.strikeNear(player, this.struck, this.strikes);
        if (--this.lightningLeft <= 0) {
            this.lightning = false;
            this.sync(ThorStatePayload.NONE, 0);
        }
    }

    private void stop() {
        if (this.dive != null) {
            this.dive.end();
        }
        ALL.remove(this.owner.getUUID(), this);
    }

    static void leave(ServerPlayer player) {
        ThorMoves moves = find(player);
        if (moves != null) {
            boolean shown = moves.flying || moves.jumping || moves.charging;
            moves.stop();
            moves.flying = false;
            moves.jumping = false;
            moves.lightning = false;
            moves.charging = false;
            if (shown) {
                ThorStatePayload.send(player, 0, ThorStatePayload.NONE, 0);
            }
        }
    }

    static void clear() {
        for (ThorMoves moves : ALL.values()) {
            if (moves.dive != null) {
                moves.dive.end();
            }
        }
        ALL.clear();
    }

    void sound(ServerLevel level, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, this.owner.getX(), this.owner.getY() + this.owner.getScale(), this.owner.getZ(), sound,
                SoundSource.PLAYERS, volume, pitch);
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && cushioned(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getNewDamage() >= STUN && flying(player)) {
            land(player, false);
        }
    }

    ServerPlayer owner() {
        return this.owner;
    }
}
