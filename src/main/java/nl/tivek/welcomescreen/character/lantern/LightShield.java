package nl.tivek.welcomescreen.character.lantern;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.welcomescreen.WelcomeScreenMod;
import nl.tivek.welcomescreen.character.CharacterAbility;
import nl.tivek.welcomescreen.character.Characters;
import nl.tivek.welcomescreen.network.ConstructPayload;
import nl.tivek.welcomescreen.spell.SpellCasting;
import nl.tivek.welcomescreen.spell.SpellEffect;
import nl.tivek.welcomescreen.spell.SpellFx;

/**
 * The shield: what the ring does when Green Lantern taps the button of the hand that defends. A round pane of
 * hard light stands in front of him, in the way he looks, and takes most of every hit that comes from the
 * front; the next tap puts it away again. It drinks a little ring power every second and falls apart when the
 * ring runs dry. While he flies it becomes a pointed cone out in front of him, that still blocks what comes at
 * him from ahead and rams whatever he flies into (see {@link Flight}).
 *
 * <p>Holding the button raises the dome instead (see {@link LightDome}); the shield waits under it until the
 * dome comes down again.
 *
 * <p>Hits that go straight through armour anyway (poison, falling, drowning) go through the shield as well:
 * hard light stops what comes flying at you, not what is already inside you. So do arrows that pierce.
 */
@EventBusSubscriber(modid = WelcomeScreenMod.MODID)
public final class LightShield implements SpellEffect {
    // How wide the pane is, in blocks, and how far in front of his eyes it hangs. Clients work out that same
    // spot for their own shield, so it follows the way they look without a tick of lag.
    private static final float SIZE = 1.7F;
    public static final double AHEAD = 0.85;
    private static final double VIEW_RANGE = 128.0;
    // A hit is caught when it comes from in front of him: straight ahead is 1, straight behind is -1.
    private static final double FRONT = 0.1;
    // How long it takes to fold out and to fall apart, in ticks.
    private static final int OPEN_TICKS = 3;

    private static final Map<UUID, LightShield> UP = new HashMap<>();

    private final int id;
    private final ServerPlayer owner;
    private final float kept;
    private final float perTick;
    private int open;
    private int closing = -1;
    private int flash;

    private LightShield(ServerPlayer owner, CharacterAbility ability) {
        this.id = PowerRing.newId();
        this.owner = owner;
        this.kept = (float) ability.value("damageKept");
        this.perTick = (float) (ability.value("powerPerSecond") / 20.0);
    }

    /**
     * The button of the hand that defends: a tap puts the shield up or away again, holding it raises the dome,
     * and letting go of a held button lets the dome down.
     *
     * @return true when something changed
     */
    static boolean use(ServerPlayer owner, ServerLevel level, CharacterAbility ability, boolean on, int data) {
        if (!on) {
            return LightDome.lower(owner);
        }
        if ((data & Characters.HOLD) != 0) {
            return LightDome.raise(owner, level, ability);
        }
        if ((data & Characters.TAP) == 0) {
            return false;
        }
        if (UP.containsKey(owner.getUUID())) {
            stop(owner);
            level.playSound(null, owner.getX(), owner.getY() + 1.2, owner.getZ(), SoundEvents.AMETHYST_CLUSTER_BREAK,
                    SoundSource.PLAYERS, 0.5F, 1.4F);
            return true;
        }
        if (Lantern.busy(owner) || Flight.descending(owner)) {
            return false;
        }
        if (PowerRing.power(owner) <= 0.0F) {
            PowerRing.tell(owner, "no_power");
            return false;
        }
        LightShield shield = new LightShield(owner, ability);
        UP.put(owner.getUUID(), shield);
        SpellCasting.start(level, shield);
        shield.send(level);
        level.playSound(null, owner.getX(), owner.getY() + 1.2, owner.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 0.5F, 1.1F);
        PowerRing.sync(owner);
        return true;
    }

    /** True while this player has the shield up (under the dome as well). */
    static boolean up(ServerPlayer player) {
        return UP.containsKey(player.getUUID());
    }

    /** Something hit the shield or was rammed by it: its light flares. */
    static void flash(ServerPlayer player) {
        LightShield shield = UP.get(player.getUUID());
        if (shield != null) {
            shield.flash = OPEN_TICKS;
        }
    }

    /** The shield is put away: tapped away, taken over by the lantern, or the ring gave out. */
    static void stop(ServerPlayer player) {
        LightShield shield = UP.remove(player.getUUID());
        if (shield != null) {
            shield.closing = 0;
            PowerRing.sync(player);
        }
    }

    /** The server stops: every shield is gone. */
    static void clear() {
        UP.clear();
    }

    @Override
    public boolean tick(ServerLevel level, int age) {
        if (this.closing >= 0) {
            this.closing++;
            if (this.closing >= OPEN_TICKS) {
                PacketDistributor.sendToPlayersInDimension(level, ConstructPayload.remove(this.id));
                return false;
            }
            this.send(level);
            return true;
        }
        if (UP.get(this.owner.getUUID()) != this) {
            this.closing = 0;
            return true;
        }
        if (!PowerRing.fuels(this.owner, level)) {
            this.drop(level, false);
            return true;
        }
        // Under the dome the shield folds away and costs nothing; it comes back out once the dome is gone.
        boolean domed = LightDome.up(this.owner);
        if (!domed) {
            float power = PowerRing.power(this.owner);
            if (power <= 0.0F) {
                this.drop(level, true);
                return true;
            }
            PowerRing.setPower(this.owner, power - this.perTick);
        }
        this.open = domed ? Math.max(0, this.open - 1) : Math.min(OPEN_TICKS, this.open + 1);
        this.flash = Math.max(0, this.flash - 1);
        this.send(level);
        return true;
    }

    /** It comes down: either the ring ran dry or its owner is gone. */
    private void drop(ServerLevel level, boolean empty) {
        UP.remove(this.owner.getUUID(), this);
        this.closing = 0;
        if (empty) {
            PowerRing.tell(this.owner, "no_power");
        }
        this.send(level);
        PowerRing.sync(this.owner);
    }

    /** A hit lands on it: the light flares where it was struck. */
    private void struck(ServerLevel level, float blocked) {
        this.flash = OPEN_TICKS;
        Vec3 at = this.point();
        SpellFx.sphereOut(level, SpellFx.dust(PowerRing.GREEN, 1.4F), at, 12 + (int) Math.min(20.0F, blocked * 2),
                0.25);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8F,
                0.9F);
    }

    /** Where it stands: in front of his eyes in the way he looks, or out in front of him in the way he flies. */
    private Vec3 point() {
        if (Flight.flying(this.owner)) {
            return this.owner.getBoundingBox().getCenter().add(Flight.heading(this.owner).scale(1.1));
        }
        return this.owner.getEyePosition().add(this.owner.getLookAngle().scale(AHEAD));
    }

    /** The way it faces: where he looks, or where he flies. */
    private Vec3 front() {
        return Flight.flying(this.owner) ? Flight.heading(this.owner) : this.owner.getLookAngle();
    }

    private void send(ServerLevel level) {
        Vec3 at = this.point();
        float shown = this.closing >= 0 ? (1.0F - (float) this.closing / OPEN_TICKS) * this.open / OPEN_TICKS
                : (float) this.open / OPEN_TICKS;
        PacketDistributor.sendToPlayersNear(level, null, at.x, at.y, at.z, VIEW_RANGE,
                new ConstructPayload(this.id, this.owner.getId(), at, this.front(), SIZE, shown,
                        this.flash > 0 ? 1.0F : 0.0F, true,
                        Flight.flying(this.owner) ? ConstructPayload.RAM : ConstructPayload.SHIELD));
    }

    /**
     * Whether a hit gets through hard light no matter how it comes: what goes through armour (poison, falling,
     * drowning, the void), and an arrow that pierces.
     */
    static boolean goesThrough(DamageSource source) {
        return source.is(DamageTypeTags.BYPASSES_ARMOR) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                || source.getDirectEntity() instanceof AbstractArrow arrow && arrow.getPierceLevel() > 0;
    }

    /** Every hit on a player who holds the shield up, as long as it comes at him from the front. */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        LightShield shield = UP.get(player.getUUID());
        // Under the dome the dome takes the hits (see LightDome).
        if (shield == null || event.getAmount() <= 0.0F || LightDome.up(player) || goesThrough(event.getSource())) {
            return;
        }
        Vec3 from = event.getSource().getSourcePosition();
        if (from != null) {
            Vec3 front = shield.front();
            Vec3 toSource = from.subtract(player.getBoundingBox().getCenter());
            // On foot only the way you face counts, flat along the ground; in the air the cone points in 3D.
            if (!Flight.flying(player)) {
                front = new Vec3(front.x, 0.0, front.z);
                toSource = new Vec3(toSource.x, 0.0, toSource.z);
            }
            if (front.lengthSqr() < 1.0E-4 || toSource.lengthSqr() < 1.0E-4
                    || front.normalize().dot(toSource.normalize()) < FRONT) {
                return;
            }
        }
        float before = event.getAmount();
        event.setAmount(before * shield.kept);
        shield.struck(player.serverLevel(), before - event.getAmount());
    }
}
