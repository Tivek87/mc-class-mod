package nl.tivek.multiversepowers.creature.wizard;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.MonsterBlows;
import nl.tivek.multiversepowers.creature.wizard.rift.WizardPortal;
import nl.tivek.multiversepowers.creature.wizard.staff.WizardStaff;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// What the Wizard does at rest with no foe about (WizardIdle), on the server: reading a book that floats open before
// him (READ), sitting on his staff in the air (PERCH), juggling orbs of light with his staff leaned on a wall or
// planted beside him (JUGGLE), meditating cross-legged in the air before his planted staff (MEDITATE), walking in and
// out of two rifts and tossing light through them (PORTALS), brewing in a cauldron he conjures (BREW), playing with a
// wisp of light that flits round him (WISP), drawing a circle of runes round himself with his staff's foot (SCRIBE) and
// practising his spells on the empty air (PRACTICE). What he holds and how he moves is drawn by each game from the
// move's age (client/PastimeProps, client/WizardRest); his staff, set down, is a staff/WizardStaff of its own.
public final class WizardPastimes {
    // How high he sits on his staff, and how high he floats meditating.
    private static final double PERCH_HIGH = 2.6;
    private static final double MEDITATE_HIGH = 0.9;
    // The other rift of his play stands this far to his side; he steps through at these ticks.
    private static final double PORTAL_APART = 6.0;
    private static final int[] PORTAL_IN = { 50, 172 };
    private static final int PORTAL_OUT = 6;
    // A practice spell every so many ticks.
    private static final int PRACTICE_EVERY = 36;
    private static final int[] BREW_COLOURS = { 0x7A3CFF, 0x3CFFB0, 0xFF5A8C, 0x5AC8FF };
    private final Wizard wizard;
    @Nullable
    private WizardStaff staff;
    @Nullable
    private WizardPortal near;
    @Nullable
    private WizardPortal far;
    private Vec3 home = Vec3.ZERO;
    private float facing;

    WizardPastimes(Wizard wizard) {
        this.wizard = wizard;
    }

    // Where the wisp he plays with is `t` ticks into his play: flitting round him in loops, now near, now far.
    public static Vec3 wisp(Entity wizard, double t) {
        double a = t * 0.075 + wizard.getId();
        double r = 1.4 + 0.6 * Math.sin(t * 0.031 + 1.0);
        return wizard.position().add(Math.cos(a) * r, 1.7 + 0.5 * Math.sin(t * 0.093) + 0.25 * Math.sin(t * 0.21),
                Math.sin(a * 1.3) * r);
    }

    // A pastime he has room for, never the one he just did.
    int pick(ServerLevel level, RandomSource random, int last) {
        for (int tries = 0; tries < 12; tries++) {
            int kind = WizardMove.PASTIMES[random.nextInt(WizardMove.PASTIMES.length)];
            if (kind != last && this.room(level, kind)) {
                return kind;
            }
        }
        return WizardMove.READ;
    }

    private boolean room(ServerLevel level, int kind) {
        return switch (kind) {
            case WizardMove.PERCH, WizardMove.MEDITATE -> level.noCollision(this.wizard,
                    this.wizard.getBoundingBox().expandTowards(0.0, PERCH_HIGH + 0.5, 0.0));
            case WizardMove.PORTALS -> this.side(level) != null;
            default -> true;
        };
    }

    void begin(ServerLevel level, int kind) {
        this.home = this.wizard.position();
        this.facing = this.wizard.yBodyRot;
        this.wizard.getNavigation().stop();
        if (kind == WizardMove.READ) {
            level.playSound(null, this.home.x, this.home.y + 1.0, this.home.z, SoundEvents.BOOK_PAGE_TURN,
                    SoundSource.HOSTILE, 1.0F, 0.8F);
        }
    }

    void tick(ServerLevel level, int kind, int t) {
        int release = WizardMove.release(kind);
        int length = WizardMove.length(kind);
        this.hold();
        switch (kind) {
            case WizardMove.READ -> this.read(level, t, release);
            case WizardMove.PERCH -> this.perch(level, t, release, length);
            case WizardMove.JUGGLE -> this.juggle(level, t, release, length);
            case WizardMove.MEDITATE -> this.meditate(level, t, release, length);
            case WizardMove.PORTALS -> this.portals(level, t, release, length);
            case WizardMove.BREW -> this.brew(level, t, release, length);
            case WizardMove.WISP -> this.wizard.getLookControl().setLookAt(wisp(this.wizard, t));
            case WizardMove.SCRIBE -> this.scribe(level, t, release, length);
            case WizardMove.PRACTICE -> this.practice(level, t, release);
            default -> {
            }
        }
    }

    // Still on the spot, but where a pastime moves him.
    private void hold() {
        if (!this.wizard.isNoGravity()) {
            this.wizard.setDeltaMovement(this.wizard.getDeltaMovement().multiply(0.0, 1.0, 0.0));
        }
    }

    // The pastime is over or cut short: his staff back in his hand, the rifts shut, his feet on the ground.
    void end(int kind) {
        if (this.staff != null) {
            this.staff.putAway();
            this.staff = null;
        }
        for (WizardPortal rift : new WizardPortal[] { this.near, this.far }) {
            if (rift != null) {
                rift.close();
            }
        }
        this.near = null;
        this.far = null;
        this.wizard.setInvisible(false);
        this.wizard.setNoGravity(this.wizard.flies());
    }

    private Vec3 ahead(double right, double up, double ahead) {
        float yaw = this.facing * Mth.DEG_TO_RAD;
        double sin = Mth.sin(yaw);
        double cos = Mth.cos(yaw);
        return this.home.add(-cos * right - sin * ahead, up, -sin * right + cos * ahead);
    }

    private void read(ServerLevel level, int t, int release) {
        Vec3 book = this.wizard.at(-0.05, 1.2, 0.6);
        this.wizard.getLookControl().setLookAt(book.x, book.y, book.z);
        if (t > release && t % 5 == 0) {
            ParticleFx.send(level, ParticleTypes.ENCHANT, book.x, book.y + 0.3, book.z, 3, 0.5, 0.4, 0.5, 0.4);
        }
        if (t > release && t % 70 == 35) {
            level.playSound(null, book.x, book.y, book.z, SoundEvents.BOOK_PAGE_TURN, SoundSource.HOSTILE, 1.0F,
                    0.9F + 0.2F * this.wizard.getRandom().nextFloat());
        }
    }

    // His staff laid sideways under him, he rises on it, drifts slowly round over where he was, and comes down again.
    private void perch(ServerLevel level, int t, int release, int length) {
        if (t < release) {
            return;
        }
        float yaw = this.wizard.yBodyRot * Mth.DEG_TO_RAD;
        Vec3 across = new Vec3(Mth.cos(yaw), 0.0, Mth.sin(yaw));
        if (t == release) {
            this.wizard.setNoGravity(true);
            this.staff = WizardStaff.lay(level, this.wizard, WizardStaff.SEAT, this.seat(), across);
            level.playSound(null, this.home.x, this.home.y + 1.0, this.home.z, SoundEvents.AMETHYST_BLOCK_RESONATE,
                    SoundSource.HOSTILE, 1.2F, 1.3F);
        }
        boolean down = t > length - 45;
        double a = (t - release) * 0.012;
        Vec3 want = down ? this.home : this.home.add(Math.cos(a) * 1.4 - 1.4, PERCH_HIGH + 0.2 * Math.sin(t * 0.05),
                Math.sin(a) * 1.4);
        Vec3 step = want.subtract(this.wizard.position()).scale(0.08);
        this.wizard.setDeltaMovement(step);
        if (this.staff != null) {
            this.staff.setMiddle(this.seat());
            this.staff.point(across);
        }
        if (t % 9 == 0) {
            Vec3 hand = this.wizard.at(0.0, 1.25, 0.45);
            ParticleFx.send(level, Wizard.dust(Wizard.light(this.wizard.phase()), 0.8F), hand.x, hand.y, hand.z, 2,
                    0.15, 0.1, 0.15, 0.0);
            ParticleFx.send(level, ParticleTypes.END_ROD, hand.x, hand.y + 0.2, hand.z, 1, 0.1, 0.1, 0.1, 0.02);
        }
    }

    // Under his thighs as he sits, a little behind his middle.
    private Vec3 seat() {
        return this.wizard.at(0.0, 0.62, -0.05);
    }

    // His staff leaned on a wall at his side (planted there if none is near), then orbs of light tossed hand to hand.
    private void juggle(ServerLevel level, int t, int release, int length) {
        if (t == release) {
            this.setDown(level, true);
        }
        if (t > release && t < length - 12 && (t - release) % 11 == 0) {
            Vec3 hand = this.wizard.at((t / 11) % 2 == 0 ? 0.35 : -0.35, 1.35, 0.5);
            level.playSound(null, hand.x, hand.y, hand.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE,
                    0.7F, 1.2F + 0.3F * this.wizard.getRandom().nextFloat());
        }
        if (t == length - 12) {
            this.takeUp();
        }
    }

    // His staff set down at his right: leaned on a wall within reach if `lean` and there is one, else planted.
    private void setDown(ServerLevel level, boolean lean) {
        float yaw = this.wizard.yBodyRot * Mth.DEG_TO_RAD;
        Vec3 right = new Vec3(-Mth.cos(yaw), 0.0, -Mth.sin(yaw));
        Vec3 foot = Wizard.groundUnder(level, this.wizard.position().add(right.scale(0.9)).add(0.0, 0.5, 0.0));
        if (lean) {
            for (int step = 1; step <= 2; step++) {
                BlockPos wall = BlockPos.containing(this.wizard.position().add(right.scale(0.9 + step * 0.5))
                        .add(0.0, 1.5, 0.0));
                if (level.isLoaded(wall) && level.getBlockState(wall).isFaceSturdy(level, wall,
                        Direction.getNearest(-right.x, 0.0, -right.z))) {
                    Vec3 against = Vec3.atCenterOf(wall).subtract(right.scale(0.5)).subtract(0.0, 0.5, 0.0);
                    Vec3 lower = Wizard.groundUnder(level, against.subtract(right.scale(0.45)).add(0.0, 1.0, 0.0));
                    Vec3 way = against.add(0.0, 1.0, 0.0).subtract(lower).normalize();
                    this.staff = WizardStaff.lay(level, this.wizard, WizardStaff.LEANED,
                            lower.add(way.scale(WizardStaff.FOOT - 0.05)), way);
                    this.thud(level, lower);
                    return;
                }
            }
        }
        this.staff = WizardStaff.lay(level, this.wizard, WizardStaff.PLANTED,
                foot.add(0.0, WizardStaff.FOOT - 0.25, 0.0), new Vec3(0.0, 1.0, 0.0));
        this.thud(level, foot);
    }

    private void thud(ServerLevel level, Vec3 at) {
        level.playSound(null, at.x, at.y, at.z, SoundEvents.WOOD_PLACE, SoundSource.HOSTILE, 1.0F, 0.8F);
        MonsterBlows.dust(level, at, 6, 0.2);
    }

    private void takeUp() {
        if (this.staff != null) {
            this.staff.putAway();
            this.staff = null;
            this.wizard.playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 0.9F, 0.8F);
        }
    }

    // His staff planted before him, he rises cross-legged into the air; motes flow from its crystal into him.
    private void meditate(ServerLevel level, int t, int release, int length) {
        if (t == release) {
            Vec3 foot = Wizard.groundUnder(level, this.ahead(0.0, 0.5, 1.15));
            this.staff = WizardStaff.lay(level, this.wizard, WizardStaff.PLANTED,
                    foot.add(0.0, WizardStaff.FOOT - 0.25, 0.0), new Vec3(0.0, 1.0, 0.0));
            this.thud(level, foot);
            this.wizard.setNoGravity(true);
        }
        if (t < release) {
            return;
        }
        boolean down = t > length - 30;
        double want = this.home.y + (down ? 0.0 : MEDITATE_HIGH + 0.08 * Math.sin(t * 0.06));
        this.wizard.setDeltaMovement(0.0, (want - this.wizard.getY()) * 0.08, 0.0);
        if (this.staff != null && t % 4 == 0 && !down) {
            Vec3 from = this.staff.tip();
            Vec3 to = this.wizard.position().add(0.0, 1.2, 0.0);
            ParticleFx.line(level, Wizard.dust(Wizard.light(this.wizard.phase()), 0.6F), from, to, 0.5);
            ParticleFx.send(level, ParticleTypes.ENCHANT, to.x, to.y, to.z, 2, 0.4, 0.4, 0.4, 0.5);
        }
        if (t == length - 8) {
            this.takeUp();
        }
    }

    // Where the other rift of his play stands: to his right or left, on the ground with room for him.
    @Nullable
    private Vec3 side(ServerLevel level) {
        float yaw = this.wizard.yBodyRot * Mth.DEG_TO_RAD;
        Vec3 right = new Vec3(-Mth.cos(yaw), 0.0, -Mth.sin(yaw));
        for (double s : new double[] { 1.0, -1.0 }) {
            Vec3 spot = Wizard.groundUnder(level, this.wizard.position().add(right.scale(s * PORTAL_APART))
                    .add(0.0, 2.0, 0.0));
            if (Math.abs(spot.y - this.wizard.getY()) < 2.0 && level.noCollision(this.wizard,
                    this.wizard.getBoundingBox().move(spot.subtract(this.wizard.position())))) {
                return spot;
            }
        }
        return null;
    }

    // Two rifts: he walks into the one before him and out of the other, tosses light into it that comes out of the
    // first, then goes back the way he came.
    private void portals(ServerLevel level, int t, int release, int length) {
        if (t == release) {
            Vec3 other = this.side(level);
            if (other == null) {
                return;
            }
            int style = this.wizard.getRandom().nextInt(WizardPortal.STYLES);
            int life = length - release - 10;
            this.near = WizardPortal.open(level, this.ahead(0.0, 1.1, 1.3), this.facing + 180.0F, 1.1F, life, style,
                    true);
            this.far = WizardPortal.open(level, other.add(0.0, 1.1, 0.0), this.facing, 1.1F, life, style, true);
        }
        if (this.near == null || this.far == null) {
            return;
        }
        for (int k = 0; k < PORTAL_IN.length; k++) {
            WizardPortal into = k == 0 ? this.near : this.far;
            WizardPortal out = k == 0 ? this.far : this.near;
            if (t == PORTAL_IN[k] - 10) {
                MonsterBlows.face(this.wizard, into.position().subtract(this.wizard.position()));
            } else if (t == PORTAL_IN[k]) {
                this.wizard.setInvisible(true);
                level.playSound(null, into.getX(), into.getY(), into.getZ(), SoundEvents.ENDERMAN_TELEPORT,
                        SoundSource.HOSTILE, 0.8F, 1.3F);
                ParticleFx.implosion(level, Wizard.dust(WizardPortal.rim(into.style()), 1.2F), into.position(), 1.2,
                        16, 0.25);
            } else if (t == PORTAL_IN[k] + PORTAL_OUT) {
                Vec3 at = out.position().add(out.facing().scale(0.6)).subtract(0.0, 1.1, 0.0);
                this.wizard.teleportTo(at.x, at.y, at.z);
                this.wizard.setInvisible(false);
                MonsterBlows.face(this.wizard, out.facing());
                ParticleFx.sphereOut(level, Wizard.dust(WizardPortal.rim(out.style()), 1.2F), out.position(), 18,
                        0.2);
                level.playSound(null, at.x, at.y, at.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.8F,
                        1.5F);
            }
        }
        // Light tossed into the far rift comes out of the near one and arcs away.
        if (t == 112) {
            Vec3 hand = this.wizard.at(-0.4, 1.4, 0.5);
            ParticleFx.line(level, Wizard.dust(Wizard.STARLIGHT, 1.0F), hand, this.far.position(), 0.3);
            level.playSound(null, hand.x, hand.y, hand.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE,
                    1.0F, 1.5F);
        } else if (t == 118) {
            Vec3 at = this.near.position();
            ParticleFx.send(level, ParticleTypes.END_ROD, at.x, at.y, at.z, 14, 0.1, 0.1, 0.1, 0.12);
            ParticleFx.send(level, ParticleTypes.FIREWORK, at.x, at.y, at.z, 10, 0.1, 0.1, 0.1, 0.1);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.HOSTILE, 1.0F,
                    1.2F);
        }
    }

    // A cauldron he conjures before him bubbles as he stirs it with his staff and drops in what he carries, until a
    // puff of coloured smoke.
    private void brew(ServerLevel level, int t, int release, int length) {
        Vec3 pot = this.wizard.at(0.0, 0.85, 1.3).subtract(0.0, this.wizard.hover(), 0.0);
        this.wizard.getLookControl().setLookAt(pot.x, pot.y, pot.z);
        if (t < release || t > length - 20) {
            return;
        }
        int colour = BREW_COLOURS[(this.wizard.getId() + t / 120) % BREW_COLOURS.length];
        if (t % 4 == 0) {
            ParticleFx.send(level, ParticleTypes.BUBBLE_POP, pot.x, pot.y, pot.z, 2, 0.2, 0.0, 0.2, 0.01);
        }
        if (t % 10 == 0) {
            ParticleFx.send(level, ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFF000000 | colour),
                    pot.x, pot.y + 0.1, pot.z, 3, 0.15, 0.1, 0.15, 0.0);
            level.playSound(null, pot.x, pot.y, pot.z, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.HOSTILE,
                    0.6F, 0.8F);
        }
        if (t == 80 || t == 150 || t == 220) {
            ItemStack drop = new ItemStack(t == 80 ? Items.GLOWSTONE_DUST : t == 150 ? Items.SPIDER_EYE
                    : Items.AMETHYST_SHARD);
            ParticleOptions bits = new ItemParticleOption(ParticleTypes.ITEM, drop);
            ParticleFx.send(level, bits, pot.x, pot.y + 0.4, pot.z, 8, 0.1, 0.1, 0.1, 0.05);
            ParticleFx.send(level, ParticleTypes.SPLASH, pot.x, pot.y, pot.z, 12, 0.2, 0.05, 0.2, 0.1);
            level.playSound(null, pot.x, pot.y, pot.z, SoundEvents.GENERIC_SPLASH, SoundSource.HOSTILE, 0.6F, 1.3F);
        }
        if (t == length - 50) {
            ParticleFx.send(level, ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFF000000 | colour),
                    pot.x, pot.y + 0.6, pot.z, 40, 0.4, 0.6, 0.4, 0.0);
            ParticleFx.send(level, ParticleTypes.LARGE_SMOKE, pot.x, pot.y + 0.4, pot.z, 10, 0.3, 0.3, 0.3, 0.02);
            level.playSound(null, pot.x, pot.y, pot.z, SoundEvents.BREWING_STAND_BREW, SoundSource.HOSTILE, 1.2F,
                    0.9F);
        }
    }

    // Turning slowly on the spot with his staff's foot on the ground, he draws a circle of runes round himself; it
    // flares as it closes.
    private void scribe(ServerLevel level, int t, int release, int length) {
        int stop = length - 40;
        if (t < release || t > stop) {
            if (t == stop + 1) {
                Vec3 at = this.home.add(0.0, 0.1, 0.0);
                ParticleFx.ring(level, Wizard.dust(Wizard.light(this.wizard.phase()), 1.4F), at, 1.7, 40, 0.0);
                ParticleFx.send(level, ParticleTypes.END_ROD, at.x, at.y + 0.3, at.z, 16, 1.0, 0.2, 1.0, 0.05);
                level.playSound(null, at.x, at.y, at.z, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.HOSTILE, 1.4F,
                        0.8F);
            }
            return;
        }
        float turn = 360.0F * (t - release) / (stop - release);
        float yaw = this.facing + turn;
        this.wizard.setYRot(yaw);
        this.wizard.yBodyRot = yaw;
        this.wizard.yHeadRot = yaw;
        if (t % 3 == 0) {
            float a = yaw * Mth.DEG_TO_RAD;
            Vec3 at = this.home.add(-Mth.sin(a) * 1.6, 0.08, Mth.cos(a) * 1.6);
            ParticleFx.send(level, Wizard.dust(Wizard.light(this.wizard.phase()), 0.9F), at.x, at.y, at.z, 2, 0.05,
                    0.0, 0.05, 0.0);
            ParticleFx.send(level, ParticleTypes.ENCHANT, at.x, at.y + 0.2, at.z, 1, 0.1, 0.1, 0.1, 0.1);
        }
    }

    // A small spell flung at the empty air before him, a different school each time.
    private void practice(ServerLevel level, int t, int release) {
        Vec3 aim = this.ahead(0.0, 2.2, 6.0);
        this.wizard.getLookControl().setLookAt(aim.x, aim.y, aim.z);
        if (t < release || (t - release) % PRACTICE_EVERY != 12) {
            return;
        }
        int school = (t / PRACTICE_EVERY + this.wizard.getId()) % 5;
        Vec3 from = this.wizard.staffTip();
        Vec3 at = aim.add(this.wizard.getRandom().nextGaussian() * 0.8, this.wizard.getRandom().nextGaussian() * 0.4,
                this.wizard.getRandom().nextGaussian() * 0.8);
        int colour = switch (school) {
            case 0 -> Wizard.FIRE;
            case 1 -> Wizard.ICE;
            case 2 -> Wizard.STORM;
            case 3 -> Wizard.STARLIGHT;
            default -> Wizard.VIOLET;
        };
        ParticleFx.line(level, Wizard.dust(colour, 0.8F), from, at, 0.4);
        ParticleFx.sphereOut(level, Wizard.dust(colour, 1.3F), at, 16, 0.12);
        ParticleOptions own = switch (school) {
            case 0 -> ParticleTypes.FLAME;
            case 1 -> ParticleTypes.SNOWFLAKE;
            case 2 -> ParticleTypes.ELECTRIC_SPARK;
            case 3 -> ParticleTypes.FIREWORK;
            default -> ParticleTypes.WITCH;
        };
        ParticleFx.sphereOut(level, own, at, 12, 0.1);
        level.playSound(null, at.x, at.y, at.z, switch (school) {
            case 0 -> SoundEvents.FIRECHARGE_USE;
            case 1 -> SoundEvents.POWDER_SNOW_BREAK;
            case 2 -> SoundEvents.LIGHTNING_BOLT_IMPACT;
            case 3 -> SoundEvents.FIREWORK_ROCKET_TWINKLE;
            default -> SoundEvents.ILLUSIONER_CAST_SPELL;
        }, SoundSource.HOSTILE, 0.8F, 1.3F);
    }
}
