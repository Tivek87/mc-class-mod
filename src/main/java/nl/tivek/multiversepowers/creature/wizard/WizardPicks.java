package nl.tivek.multiversepowers.creature.wizard;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.wizard.field.GlacialSpikes;
import nl.tivek.multiversepowers.creature.wizard.field.TimeWarp;
import nl.tivek.multiversepowers.creature.wizard.spell.ArcaneFamiliar;
import nl.tivek.multiversepowers.creature.wizard.spell.RuneSentry;

// Which move the Wizard picks next (WizardMoves): what is ready, fits how far his foe is and weighs most. Light spells
// often; up close his staff (while it is in his hand) or a close spell, else away by a skip or one of his ways of
// going; far off a way of going to them; now and then he stops a moment and only watches them. Every spell weighs more
// the longer since he last cast it, so every one he knows comes round.
abstract class WizardPicks extends Goal {
    // The least a spell he picks waits after he cast it (WizardMove's own waits are longer), and how many ticks
    // uncast make a spell weigh once more when he picks the next.
    static final int REST = 100;
    private static final int LONGING = 300;
    // How much sooner his spells are ready again in each phase.
    private static final double[] QUICKER = { 1.0, 1.0, 0.88, 0.77, 0.67, 0.58 };
    static final double NEAR = 6.5;
    static final double FAR = 15.0;
    private static final double BLINK_FROM = 22.0;
    private static final int MOST_FAMILIARS = 2;
    private static final int MOST_SENTRIES = 2;
    // Ticks out of sight of his target before he goes to where he sees it.
    private static final int BLIND = 40;

    final Wizard wizard;
    final int[] ready = new int[WizardMove.KINDS + 1];
    // When he last cast each spell (ticks of his).
    final int[] castAt = new int[WizardMove.KINDS + 1];
    // Ticks he has not seen his target: walled in, he goes elsewhere once it lasts.
    int blind;
    int nextBig = 40;
    int nextUltimate;
    int lastBig;
    // The great spell his phase opens with, cast first once it fits.
    int opening;
    Vec3 skip = Vec3.ZERO;

    WizardPicks(Wizard wizard) {
        this.wizard = wizard;
    }

    final boolean ready(int kind) {
        return this.wizard.tickCount >= this.ready[kind] && WizardMove.phase(kind) <= this.wizard.phase();
    }

    // A spell just cast waits its own time, shorter each phase, but never less than REST (half that in a time warp).
    final int again(int kind) {
        double haste = this.wizard.hasted() ? 0.5 : 1.0;
        double quicker = QUICKER[Mth.clamp(this.wizard.phase(), 1, WizardMove.PHASES)] * haste;
        int again = WizardMove.again(kind);
        // His drain heals him: it waits its full 20 seconds in every phase.
        if (kind == WizardMove.DRAIN) {
            return again;
        }
        return again >= REST ? (int) Math.max(REST * haste, again * quicker) : (int) (again * quicker);
    }

    final int pick(LivingEntity target, double gap, double far, boolean sees, int now) {
        RandomSource random = this.wizard.getRandom();
        if (this.wizard.flies()) {
            int air = this.pickInAir(target, gap, sees, random);
            if (air >= 0) {
                return air;
            }
        } else if (gap < 2.4) {
            int close = this.close(true, random);
            if (close != 0) {
                return close;
            }
        }
        if (gap < 4.5 && random.nextInt(2) == 0) {
            int spell = this.close(false, random);
            if (spell != 0) {
                return spell;
            }
        }
        if (!this.wizard.flies()) {
            if (gap < 3.5 && this.ready(WizardMove.SKIP) && random.nextInt(2) == 0) {
                return this.skipFrom(target, true);
            }
            if (gap < 3.5 && random.nextInt(3) == 0) {
                int away = this.away(random, far, true);
                if (away != 0) {
                    return away;
                }
            }
        }
        if (far > BLINK_FROM) {
            int away = this.away(random, far, false);
            if (away != 0) {
                return away;
            }
        }
        if (!sees) {
            return this.blind > BLIND ? this.away(random, far, false) : 0;
        }
        if (now >= this.nextBig) {
            int big = this.big(far, random);
            if (big != 0) {
                return big;
            }
        }
        if (far > NEAR && far < 18.0 && this.ready(WizardMove.TETHER) && random.nextInt(5) == 0) {
            return WizardMove.TETHER;
        }
        if (far > NEAR && this.ready(WizardMove.WATCH) && random.nextInt(12) == 0) {
            return WizardMove.WATCH;
        }
        if (!this.wizard.flies() && this.ready(WizardMove.SKIP) && (far > FAR + 2.0 || random.nextInt(6) == 0)) {
            return this.skipFrom(target, far < NEAR);
        }
        boolean bolt = this.ready(WizardMove.BOLT) && far < 26.0;
        boolean spark = this.ready(WizardMove.SPARK) && far < 24.0;
        if (bolt && (!spark || random.nextInt(2) == 0)) {
            return WizardMove.BOLT;
        }
        return spark ? WizardMove.SPARK : 0;
    }

    // One of his ways of going elsewhere that is ready, by weight: the blink with its ghost, the flicker of short hops
    // (from his second phase), a step through his rifts, and up close the veil, slipping behind them while a solid image
    // of him stands on (from his third). 0 for none.
    private int away(RandomSource random, double far, boolean close) {
        int[] kinds = { WizardMove.BLINK, WizardMove.FLICKER, WizardMove.RIFT_STEP, WizardMove.VEIL };
        double[] weight = { 2.0, 1.5, 1.5, close ? 2.0 : 0.0 };
        double total = 0.0;
        for (int i = 0; i < kinds.length; i++) {
            if (!this.ready(kinds[i])) {
                weight[i] = 0.0;
            }
            total += weight[i];
        }
        double roll = random.nextDouble() * total;
        for (int i = 0; i < kinds.length; i++) {
            roll -= weight[i];
            if (weight[i] > 0.0 && roll < 0.0) {
                return kinds[i];
            }
        }
        return 0;
    }

    // In the air: his staff only on a foe at his height, else away into the air from one who comes close, and now and
    // then a blink elsewhere for no reason they can see. -1 to pick as on the ground.
    private int pickInAir(LivingEntity target, double gap, boolean sees, RandomSource random) {
        double below = this.wizard.getY() + this.wizard.hover() - target.getY();
        if (gap < 2.6 && Math.abs(below) < 1.6 && random.nextInt(3) != 0) {
            int close = this.close(true, random);
            if (close != 0) {
                return close;
            }
        }
        if (gap < 4.0 && random.nextInt(2) == 0) {
            if (this.ready(WizardMove.FLICKER) && random.nextInt(3) == 0) {
                return WizardMove.FLICKER;
            }
            if (this.ready(WizardMove.AIRSTEP)) {
                return WizardMove.AIRSTEP;
            }
        }
        if (sees && this.ready(WizardMove.AIRSTEP) && random.nextInt(14) == 0) {
            return WizardMove.AIRSTEP;
        }
        return -1;
    }

    // Up close (`blows`: in reach of his staff) a blow or a short spell, else (a few steps off) a short spell, picked
    // by weight among those ready; on the ground right up close a blink away now and then. 0 for none. His staff's
    // blows wait while it is out of his hand.
    private int close(boolean blows, RandomSource random) {
        int[] kinds = blows ? new int[] { WizardMove.STRIKE, WizardMove.COMBO, WizardMove.WHIRL, WizardMove.REPULSE,
                WizardMove.FROST_CONE, WizardMove.FLAME_LASH, WizardMove.BLINK }
                : new int[] { WizardMove.REPULSE, WizardMove.FROST_CONE, WizardMove.FLAME_LASH };
        double[] weight = new double[kinds.length];
        double total = 0.0;
        boolean staff = !this.wizard.staffOut();
        for (int i = 0; i < kinds.length; i++) {
            int kind = kinds[i];
            if (!this.ready(kind) || kind == WizardMove.BLINK && this.wizard.flies()
                    || !staff && (kind == WizardMove.STRIKE || kind == WizardMove.COMBO || kind == WizardMove.WHIRL)) {
                continue;
            }
            weight[i] = switch (kind) {
                case WizardMove.STRIKE, WizardMove.COMBO -> 3.0;
                case WizardMove.WHIRL, WizardMove.REPULSE -> 2.0;
                case WizardMove.BLINK -> 1.0;
                default -> 1.5;
            };
            weight[i] *= 1.0 + Mth.clamp((this.wizard.tickCount - this.castAt[kind]) / (double) LONGING, 0.0, 2.0);
            total += weight[i];
        }
        double roll = random.nextDouble() * total;
        for (int i = 0; i < kinds.length; i++) {
            roll -= weight[i];
            if (weight[i] > 0.0 && roll < 0.0) {
                return kinds[i];
            }
        }
        return 0;
    }

    // A quick glide: away and aside when they are close, towards and aside when they are far, else round them.
    final int skipFrom(LivingEntity target, boolean away) {
        Vec3 to = target.position().subtract(this.wizard.position()).multiply(1.0, 0.0, 1.0);
        double far = to.length();
        Vec3 ahead = far < 1.0E-3 ? new Vec3(0.0, 0.0, 1.0) : to.scale(1.0 / far);
        Vec3 aside = new Vec3(-ahead.z, 0.0, ahead.x).scale(this.wizard.getRandom().nextBoolean() ? 1.0 : -1.0);
        double toward = away ? -0.75 : far > FAR ? 0.8 : 0.0;
        this.skip = ahead.scale(toward).add(aside.scale(away ? 0.65 : 1.0)).normalize();
        return WizardMove.SKIP;
    }

    // A big spell that is ready and fits, picked by weight; none twice running. A phase's own spell comes first.
    private int big(double far, RandomSource random) {
        int now = this.wizard.tickCount;
        if (this.opening != 0 && this.ready(this.opening) && this.fits(this.opening, far)) {
            int opening = this.opening;
            this.opening = 0;
            return opening;
        }
        double[] weight = new double[WizardMove.KINDS + 1];
        double total = 0.0;
        for (int kind = 1; kind <= WizardMove.KINDS; kind++) {
            if (!WizardMove.big(kind) || kind == this.lastBig || !this.ready(kind) || !this.fits(kind, far)
                    || WizardMove.ultimate(kind) && now < this.nextUltimate) {
                continue;
            }
            weight[kind] = this.weight(kind);
            total += weight[kind];
        }
        if (total <= 0.0) {
            return 0;
        }
        double roll = random.nextDouble() * total;
        for (int kind = 1; kind <= WizardMove.KINDS; kind++) {
            roll -= weight[kind];
            if (weight[kind] > 0.0 && roll < 0.0) {
                return kind;
            }
        }
        return 0;
    }

    // Each spell weighs more the longer since he last cast it, so every one he knows comes round.
    private double weight(int kind) {
        double waited = (this.wizard.tickCount - this.castAt[kind]) / (double) LONGING;
        double longing = 1.0 + Mth.clamp(waited, 0.0, 4.0);
        return this.base(kind) * longing;
    }

    private double base(int kind) {
        double base = switch (kind) {
            case WizardMove.FIREBALL, WizardMove.NOVA -> 3.0;
            case WizardMove.LIGHTNING, WizardMove.MIRRORS, WizardMove.ORBIT, WizardMove.LANCES, WizardMove.COMET,
                    WizardMove.INFERNO, WizardMove.PHANTOM, WizardMove.TENDRILS, WizardMove.SPIRAL,
                    WizardMove.SUPERNOVA -> 2.5;
            case WizardMove.GLACIAL, WizardMove.PILLARS, WizardMove.STAFF_THROW -> 2.5;
            case WizardMove.FAMILIARS, WizardMove.WARP, WizardMove.SENTRY, WizardMove.STAFF_FREE -> 1.5;
            default -> 2.0;
        };
        return WizardMove.ultimate(kind) ? base * 1.5 : base;
    }

    // Whether a big spell fits now: how far his foe is, whether he is in the air, whether his staff is in his hand.
    final boolean fits(int kind, double far) {
        boolean staff = !this.wizard.staffOut();
        return switch (kind) {
            case WizardMove.FIREBALL -> far > 4.0 && far < 24.0;
            case WizardMove.BLIZZARD -> far > 3.0 && far < 16.0;
            case WizardMove.CHAINS, WizardMove.RUNES, WizardMove.MIRRORS -> far < 16.0;
            case WizardMove.LIGHTNING, WizardMove.LIFT -> far < 14.0;
            case WizardMove.STARFALL -> far < 28.0;
            case WizardMove.FAMILIARS -> ArcaneFamiliar.owned(this.wizard) < MOST_FAMILIARS;
            case WizardMove.BEAM -> far > 4.0 && far < 24.0;
            case WizardMove.METEOR -> far < 30.0;
            case WizardMove.WARP -> far < 2.0 * TimeWarp.RADIUS - 3.0;
            case WizardMove.ORBIT, WizardMove.INFERNO, WizardMove.SPIRAL -> far < 19.0;
            case WizardMove.LANCES -> far > 4.0 && far < 28.0;
            case WizardMove.NOVA -> far < 8.5;
            case WizardMove.TORNADO -> far > 5.0 && far < 20.0;
            case WizardMove.COMET -> far > 4.0 && far < 22.0 && this.wizard.flies();
            case WizardMove.PHANTOM -> far < 20.0 && this.wizard.flies();
            case WizardMove.TENDRILS, WizardMove.DRAIN -> far < 16.0;
            case WizardMove.VOID_ORB -> far > 5.0 && far < 24.0;
            case WizardMove.SUPERNOVA -> far < 36.0 && this.wizard.flies();
            case WizardMove.GLACIAL -> far > 4.0 && far < GlacialSpikes.RANGE;
            case WizardMove.PILLARS -> far < 24.0;
            case WizardMove.SENTRY -> RuneSentry.owned(this.wizard) < MOST_SENTRIES;
            case WizardMove.STAFF_THROW -> staff && far > 5.0 && far < 22.0;
            case WizardMove.STAFF_FREE -> staff && far < 24.0;
            case WizardMove.RIFT_VOLLEY -> far < 22.0;
            default -> false;
        };
    }

    // Whether a move needs him up in the air: he takes off first.
    static boolean airborne(int kind) {
        return kind == WizardMove.COMET || kind == WizardMove.PHANTOM || kind == WizardMove.SUPERNOVA;
    }
}
