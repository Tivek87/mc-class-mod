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

/**
 * The first construct of the wheel: a sword of hard light in the ring hand and a shield on the other arm. They take
 * shape when he picks them (the sword grows out of his fist and is tossed up spinning and caught, looked over and banged
 * on the shield, see {@link SwordMove#EQUIP}) and break into solid pieces when he puts them away. While he holds them the mouse is theirs:
 * <ul>
 * <li><b>Left click:</b> one of twelve cuts and thrusts (see {@link SwordMove}), picked at random each time and
 * flowing on from the last.</li>
 * <li><b>Left held 2 seconds:</b> the shield before his chest (it takes most of what comes from the front) and twelve
 * quick stabs all over the front.</li>
 * <li><b>Right held:</b> he blocks: the shield up before him takes most of what comes from the front, for as long as he
 * holds it. A cut, a thrust or the flurry lowers it for as long as the move lasts; it comes back up by itself after.</li>
 * <li><b>Right click:</b> bent forward behind the locked shield he charges straight ahead, and rams everyone in his way
 * aside with one of six rams and a light hit; running into a wall, clicking again or running out of time ends it, and he
 * slams the shield into the ground for a small shockwave.</li>
 * </ul>
 * The numbers are settings of the Construct Wheel. His client plays every move the moment he clicks; the server strikes
 * on the move's own ticks and shows the moves to everyone else.
 */
@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class SwordShield extends SwordShieldBlows {
    /** How long the sword and shield take to break up once he puts them away, in ticks. */
    public static final int BREAK_TICKS = 8;
    // A move may start this many ticks before the last one is ready: the click came over the network a little later.
    private static final int SLACK = 2;
    // What counts as in front of him for the guard: straight ahead is 1, straight behind -1.
    private static final double FRONT = 0.2;
    // What part of a hit from the front still gets through the shield locked before him while he charges.
    private static final float CHARGE_KEPT = 0.25F;
    private static final double VIEW_RANGE = 96.0;

    private static final Map<UUID, SwordShield> HELD = new HashMap<>();
    // The sound of taking the sword and shield out, for everyone round him, at most this often.
    private static final Throttle PICK_SOUND = new Throttle(5);

    private final int id = PowerRing.newId();
    private boolean flurry;
    private int chargeStart;
    private boolean blocking;
    private int breaking = -1;

    private SwordShield(ServerPlayer owner) {
        super(owner);
    }

    /**
     * The construct wheel: he picked something. The sword and shield take shape when he picks them, and break up when he
     * picks anything else (empty hands too).
     */
    public static void hold(ServerPlayer player, Construct construct) {
        SwordShield now = HELD.get(player.getUUID());
        boolean want = construct == Construct.SWORD_SHIELD && Characters.of(player) == GameCharacter.GREEN_LANTERN
                && player.isAlive() && !Arrival.busy(player);
        if (want && (now == null || now.breaking >= 0)) {
            SwordShield sword = new SwordShield(player);
            // One that was still breaking up is gone at once: however fast they are taken out and put away, he never
            // has more than the one he holds and the one breaking up.
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

    /** True while this player holds the sword and shield, whole: the mouse is theirs. */
    public static boolean equipped(ServerPlayer player) {
        SwordShield sword = HELD.get(player.getUUID());
        return sword != null && sword.breaking < 0;
    }

    /**
     * The button of the hand that attacks, while he holds the sword: a tap cuts or thrusts (the move his client picked),
     * holding it does the flurry, and letting go of a held button ends the flurry.
     *
     * @return true when something happened
     */
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

    /**
     * The button of the hand that defends, while he holds the shield: holding it blocks, a click charges, and letting go
     * ends the block; a charge ends by itself (a wall, a second click, or its time running out: his client tells).
     *
     * @return true when something happened
     */
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

    /** The server stops: nobody holds a sword any more. */
    public static void clear() {
        HELD.clear();
        PICK_SOUND.clear();
    }

    /** True when he may start a new move: the last one has come far enough, and he is not charging. */
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

    /** He raises the shield before him and holds it there: it takes most of what comes from the front. */
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

    /** True while the shield is up to block: held up, and not lowered for a cut, a thrust or the flurry. */
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
        // Let go early: the flurry is over there and then.
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
            // Its clock runs on while it breaks up: everyone else's game keeps time by it (the pieces fly apart, and his
            // arms lower, evenly).
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
            // Holding the shield up costs the ring a little all the while; an empty ring lets it drop.
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
                    // The step of the lunge: he is thrown forward a little way.
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
                // A ram or the run itself only ever happen while he charges.
            }
        }
        this.send(level);
        return true;
    }

    /**
     * The sounds of their taking shape, for everyone round him but himself: his own game plays them the moment they
     * happen (see {@link #equipSounds}).
     */
    private void equipping(int t) {
        equipSounds(t - 1, t, this::soundForOthers);
    }

    /** Somewhere a sound is played: at a volume and a pitch. */
    @FunctionalInterface
    public interface Sounding {
        void play(SoundEvent sound, float volume, float pitch);
    }

    /**
     * The sounds of taking the sword and shield out that fall after {@code from} and up to {@code to} ticks in: the
     * strap of the shield closing round the forearm, the flick that tosses the sword, its whir each time it turns half
     * over in the air, the catch, the gleam running up the blade, and two bangs on the face of the shield, the second
     * lighter.
     */
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

    /** True when {@code moment} falls after {@code from} and no later than {@code to}. */
    private static boolean crossed(float from, float to, float moment) {
        return from < moment && moment <= to;
    }

    /** He put them away, or can no longer hold them: they break into solid pieces. */
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

    /**
     * What everyone around needs to play it: which move it is (and whether he blocks or charges), from which of its
     * ticks, the way a charge goes, and how far it has broken up ({@code size}: -1 while whole).
     */
    private void send(ServerLevel level) {
        Vec3 at = this.owner.position();
        int move = this.move.ordinal() | (this.blocking ? SwordMove.BLOCKING : 0)
                | (this.charging ? SwordMove.CHARGING : 0);
        PacketDistributor.sendToPlayersNear(level, null, at.x, at.y, at.z, VIEW_RANGE,
                new ConstructPayload(this.id, this.owner.getId(), at, this.charging ? this.way
                        : this.owner.getLookAngle(), this.breaking, 1.0F, this.moveStart, true, ConstructPayload.SWORD,
                        move, this.age, null));
    }

    /** A sound for everyone round him but himself: his own game played it already, the moment he picked. */
    private void soundForOthers(SoundEvent sound, float volume, float pitch) {
        this.owner.level().playSound(this.owner, this.owner.getX(), this.owner.getY() + 1.0, this.owner.getZ(), sound,
                SoundSource.PLAYERS, volume, pitch);
    }

    /**
     * A hit on someone holding the shield up before him: blocking it stops most of what comes from the front, the
     * flurry's guard a good part of it, the shield locked before him in a charge nearly all of it. Damage that goes
     * through armour anyway goes through. A blocked hit sparks off the face of the shield.
     */
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
