package nl.tivek.multiversepowers.character.greenlantern.ability;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.Characters;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.greenlantern.Arrival;
import nl.tivek.multiversepowers.character.greenlantern.Construct;
import nl.tivek.multiversepowers.character.greenlantern.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.engine.ability.Throttle;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class SwordShield extends SwordShieldBlows {
    public static final int BREAK_TICKS = 8;
    // A move may start this many ticks before the last one is ready: the click came over the network a little later.
    private static final int SLACK = 2;
    private static final double FRONT = 0.2;
    private static final float CHARGE_KEPT = 0.25F;
    private static final double VIEW_RANGE = 96.0;

    private static final Map<UUID, SwordShield> HELD = new HashMap<>();
    private static final Throttle PICK_SOUND = new Throttle(5);

    private final int id = PowerRing.newId();
    private boolean flurry;
    private int chargeStart;
    private boolean blocking;
    private int breaking = -1;

    private SwordShield(ServerPlayer owner) {
        super(owner);
    }

    public static void hold(ServerPlayer player, Construct construct) {
        SwordShield now = HELD.get(player.getUUID());
        boolean want = construct == Construct.SWORD_SHIELD && Characters.of(player) == GameCharacter.GREEN_LANTERN
                && player.isAlive() && !Arrival.busy(player);
        if (want && (now == null || now.breaking >= 0)) {
            SwordShield sword = new SwordShield(player);
            // One that was still breaking up is gone at once: he never has more than one held and one breaking up.
            if (now != null) {
                now.breaking = Math.max(now.breaking, BREAK_TICKS - 1);
            }
            HELD.put(player.getUUID(), sword);
            Effects.start(player.serverLevel(), sword);
            if (PICK_SOUND.allow(player)) {
                sword.soundForOthers(SoundEvents.BEACON_POWER_SELECT, 0.8F, 1.6F);
                sword.soundForOthers(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 0.9F);
            }
            sword.send(player.serverLevel());
        } else if (!want && now != null) {
            now.breakUp();
        }
    }

    public static boolean equipped(ServerPlayer player) {
        SwordShield sword = HELD.get(player.getUUID());
        return sword != null && sword.breaking < 0;
    }

    public static boolean attack(ServerPlayer player, ServerLevel level, boolean on, int data) {
        SwordShield sword = HELD.get(player.getUUID());
        if (sword == null || sword.breaking >= 0) {
            return false;
        }
        if (!on) {
            return sword.stopFlurry();
        }
        if ((data & Characters.HOLD) != 0) {
            return sword.startFlurry();
        }
        if ((data & Characters.TAP) == 0) {
            return false;
        }
        SwordMove move = SwordMove.byIndex(data >> Characters.MOVE_SHIFT);
        return sword.begin(move != null && move.kind() == SwordMove.Kind.ATTACK ? move
                : SwordMove.randomAttack(player.getRandom(), null));
    }

    public static boolean defend(ServerPlayer player, ServerLevel level, boolean on, int data) {
        SwordShield sword = HELD.get(player.getUUID());
        if (sword == null || sword.breaking >= 0) {
            return false;
        }
        if (!on) {
            return sword.blocking ? sword.stopBlock() : sword.stopCharge();
        }
        if ((data & Characters.HOLD) != 0) {
            return sword.startBlock();
        }
        return (data & Characters.TAP) != 0 && sword.startCharge();
    }

    public static void clear() {
        HELD.clear();
        PICK_SOUND.clear();
    }

    private boolean free() {
        return !this.charging && this.age - this.moveStart >= this.move.ready() - SLACK;
    }

    private boolean begin(SwordMove next) {
        if (!this.free()) {
            return false;
        }
        this.move = next;
        this.moveStart = this.age;
        this.flurry = false;
        this.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.6F, 1.3F + 0.3F * this.owner.getRandom().nextFloat());
        return true;
    }

    private boolean startBlock() {
        if (this.charging || this.blocking) {
            return false;
        }
        this.blocking = true;
        this.sound(SoundEvents.ARMOR_EQUIP_IRON.value(), 0.8F, 1.2F);
        this.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.5F, 1.6F);
        return true;
    }

    private boolean stopBlock() {
        if (!this.blocking) {
            return false;
        }
        this.blocking = false;
        return true;
    }

    private boolean shieldUp() {
        return this.blocking && !this.move.swings(this.age - this.moveStart);
    }

    private boolean startFlurry() {
        if (!this.free()) {
            return false;
        }
        float cost = (float) wheel().value("flurryPowerCost");
        float power = PowerRing.power(this.owner);
        if (power + 1.0E-4F < cost) {
            PowerRing.tell(this.owner, "no_power");
            return false;
        }
        PowerRing.setPower(this.owner, power - cost);
        this.move = SwordMove.FLURRY;
        this.moveStart = this.age;
        this.flurry = true;
        this.sound(SoundEvents.SHIELD_BLOCK, 0.8F, 1.2F);
        this.sound(SoundEvents.BEACON_POWER_SELECT, 0.5F, 2.0F);
        return true;
    }

    private boolean stopFlurry() {
        if (!this.flurry) {
            return false;
        }
        this.flurry = false;
        if (this.move == SwordMove.FLURRY && this.age - this.moveStart < SwordMove.FLURRY.ticks()) {
            this.moveStart = this.age - SwordMove.FLURRY.ticks();
        }
        return true;
    }

    private boolean startCharge() {
        if (!this.free() || this.blocking || Flight.flying(this.owner)) {
            return false;
        }
        float cost = (float) wheel().value("chargePowerCost");
        float power = PowerRing.power(this.owner);
        if (power + 1.0E-4F < cost) {
            PowerRing.tell(this.owner, "no_power");
            return false;
        }
        PowerRing.setPower(this.owner, power - cost);
        this.move = SwordMove.CHARGE;
        this.moveStart = this.age;
        this.chargeStart = this.age;
        this.charging = true;
        this.flurry = false;
        this.lastRam = null;
        this.way = flat(this.owner.getLookAngle());
        this.shoved.clear();
        this.sound(SoundEvents.ARMOR_EQUIP_NETHERITE.value(), 1.0F, 0.7F);
        this.sound(SoundEvents.RAVAGER_ROAR, 0.35F, 1.6F);
        return true;
    }

    @Override
    public boolean tick(ServerLevel level, int tick) {
        if (this.breaking >= 0) {
            // Its clock runs on while it breaks up: everyone else's game keeps time by it (see send).
            this.age++;
            this.breaking++;
            if (this.breaking >= BREAK_TICKS) {
                ConstructPayload.sendRemove(level, this.id, this.owner.position());
                HELD.remove(this.owner.getUUID(), this);
                return false;
            }
            this.send(level);
            return true;
        }
        if (HELD.get(this.owner.getUUID()) != this || !PowerRing.fuels(this.owner, level)) {
            this.breakUp();
            this.send(level);
            return true;
        }
        this.age++;
        int t = this.age - this.moveStart;
        if (this.shieldUp()) {
            float cost = (float) wheel().value("blockPowerPerSecond") / 20.0F;
            float power = PowerRing.power(this.owner);
            if (power + 1.0E-4F < cost) {
                this.stopBlock();
                PowerRing.tell(this.owner, "no_power");
            } else {
                PowerRing.setPower(this.owner, power - cost);
            }
        }
        if (this.charging) {
            this.charge(level, this.age - this.chargeStart);
            this.send(level);
            return true;
        }
        switch (this.move.kind()) {
            case ATTACK, SLAM -> {
                int[] hits = this.move.hits();
                for (int k = 0; k < hits.length; k++) {
                    if (t == hits[k]) {
                        this.strike(level);
                    }
                }
                if (this.move == SwordMove.LUNGE && t == 5) {
                    Vec3 ahead = flat(this.owner.getLookAngle());
                    this.owner.setDeltaMovement(this.owner.getDeltaMovement().add(ahead.scale(0.75)).add(0.0, 0.06,
                            0.0));
                    this.owner.hurtMarked = true;
                }
            }
            case FLURRY -> {
                int stab = (t - SwordMove.FIRST_STAB) / SwordMove.STAB_EVERY;
                if (this.flurry && t >= SwordMove.FIRST_STAB && (t - SwordMove.FIRST_STAB) % SwordMove.STAB_EVERY == 0
                        && stab < SwordMove.STABS) {
                    this.stab(level, stab);
                }
                if (t >= SwordMove.FLURRY.ticks()) {
                    this.flurry = false;
                }
            }
            case EQUIP -> this.equipping(t);
            case BASH, CHARGE -> {
            }
        }
        this.send(level);
        return true;
    }

    private void equipping(int t) {
        equipSounds(t - 1, t, this::soundForOthers);
    }

    @FunctionalInterface
    public interface Sounding {
        void play(SoundEvent sound, float volume, float pitch);
    }

    public static void equipSounds(float from, float to, Sounding sounding) {
        if (crossed(from, to, SwordMove.SHIELD_LOCK)) {
            sounding.play(SoundEvents.ARMOR_EQUIP_IRON.value(), 0.55F, 1.5F);
            sounding.play(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.5F, 1.7F);
        }
        if (crossed(from, to, SwordMove.TOSS)) {
            sounding.play(SoundEvents.PLAYER_ATTACK_SWEEP, 0.6F, 1.1F);
        }
        for (int k = 1; k < 2 * SwordMove.TOSS_TURNS; k++) {
            if (crossed(from, to, SwordMove.whir(k))) {
                sounding.play(SoundEvents.PLAYER_ATTACK_SWEEP, 0.35F, 1.7F + 0.05F * k);
            }
        }
        if (crossed(from, to, SwordMove.CATCH)) {
            sounding.play(SoundEvents.ARMOR_EQUIP_IRON.value(), 1.0F, 1.2F);
            sounding.play(SoundEvents.AMETHYST_BLOCK_PLACE, 0.8F, 1.4F);
        }
        if (crossed(from, to, SwordMove.GLEAM)) {
            sounding.play(SoundEvents.AMETHYST_BLOCK_CHIME, 0.9F, 1.5F);
        }
        for (int knock : new int[] { SwordMove.KNOCK, SwordMove.KNOCK_AGAIN }) {
            if (crossed(from, to, knock)) {
                boolean first = knock == SwordMove.KNOCK;
                sounding.play(SoundEvents.SHIELD_BLOCK, first ? 1.0F : 0.8F, first ? 1.1F : 1.35F);
                sounding.play(SoundEvents.ANVIL_LAND, first ? 0.3F : 0.2F, first ? 1.8F : 2.0F);
                sounding.play(SoundEvents.AMETHYST_BLOCK_HIT, 0.8F, first ? 1.0F : 1.25F);
            }
        }
    }

    private static boolean crossed(float from, float to, float moment) {
        return from < moment && moment <= to;
    }

    private void breakUp() {
        if (this.breaking >= 0) {
            return;
        }
        this.breaking = 0;
        this.flurry = false;
        this.charging = false;
        this.blocking = false;
        this.sound(SoundEvents.AMETHYST_CLUSTER_BREAK, 0.9F, 1.3F);
    }

    private void send(ServerLevel level) {
        Vec3 at = this.owner.position();
        int move = this.move.ordinal() | (this.blocking ? SwordMove.BLOCKING : 0)
                | (this.charging ? SwordMove.CHARGING : 0);
        PacketDistributor.sendToPlayersNear(level, null, at.x, at.y, at.z, VIEW_RANGE,
                new ConstructPayload(this.id, this.owner.getId(), at, this.charging ? this.way
                        : this.owner.getLookAngle(), this.breaking, 1.0F, this.moveStart, true, ConstructPayload.SWORD,
                        move, this.age, null));
    }

    private void soundForOthers(SoundEvent sound, float volume, float pitch) {
        this.owner.level().playSound(this.owner, this.owner.getX(), this.owner.getY() + 1.0, this.owner.getZ(), sound,
                SoundSource.PLAYERS, volume, pitch);
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        SwordShield sword = HELD.get(player.getUUID());
        if (sword == null || sword.breaking >= 0 || event.getAmount() <= 0.0F
                || LightShield.goesThrough(event.getSource())) {
            return;
        }
        boolean guarding = sword.flurry && sword.move == SwordMove.FLURRY;
        boolean blocking = sword.shieldUp();
        if (!guarding && !sword.charging && !blocking) {
            return;
        }
        Vec3 front = flat(player.getLookAngle());
        Vec3 from = event.getSource().getSourcePosition();
        if (from != null) {
            Vec3 toSource = new Vec3(from.x - player.getX(), 0.0, from.z - player.getZ());
            if (toSource.lengthSqr() > 1.0E-4 && front.dot(toSource.normalize()) < FRONT) {
                return;
            }
        }
        float kept = sword.charging ? CHARGE_KEPT : blocking ? (float) wheel().value("blockDamageKept")
                : (float) wheel().value("guardDamageKept");
        event.setAmount(event.getAmount() * kept);
        sword.sound(SoundEvents.SHIELD_BLOCK, 1.0F, 0.9F + 0.2F * player.getRandom().nextFloat());
        sword.sound(SoundEvents.AMETHYST_BLOCK_HIT, 0.8F, 1.4F);
        Vec3 face = player.getEyePosition().add(front.scale(0.7)).subtract(0.0, 0.35, 0.0);
        player.serverLevel().sendParticles(ParticleTypes.CRIT, face.x, face.y, face.z, 10, 0.2, 0.25, 0.2, 0.3);
        ParticleFx.cloud(player.serverLevel(), ParticleFx.dust(PowerRing.BRIGHT, 1.0F), face, 6, 0.25, 0.06);
    }
}
