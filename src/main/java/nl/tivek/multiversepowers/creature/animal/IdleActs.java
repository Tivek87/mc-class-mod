package nl.tivek.multiversepowers.creature.animal;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.world.entity.ai.goal.Goal;

// What a wild animal does now and then while nothing else wants it: stands still and plays one of its acts for a
// while, picked at random among those that fit where it is. Any hurt, a target or a goal before it ends the act.
public final class IdleActs extends Goal {
    private final WildAnimal animal;
    private final List<Act> acts = new ArrayList<>();
    private final int every;
    private Act playing;
    private boolean wet;

    private record Act(int kind, int least, int most, int weight, Predicate<WildAnimal> fits) {
    }

    // On average one act every `every` ticks of standing about.
    public IdleActs(WildAnimal animal, int every) {
        this.animal = animal;
        this.every = every;
        // Not the look: an act's own pose says how far its head follows what it looks at.
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
    }

    // For an animal at home under water as well: its acts go on there too.
    public IdleActs inWater() {
        this.wet = true;
        return this;
    }

    public IdleActs add(int kind, int least, int most, int weight) {
        return this.add(kind, least, most, weight, a -> true);
    }

    public IdleActs add(int kind, int least, int most, int weight, Predicate<WildAnimal> fits) {
        this.acts.add(new Act(kind, least, most, weight, fits));
        return this;
    }

    @Override
    public boolean canUse() {
        WildAnimal a = this.animal;
        if (a.act() != 0 || !a.onGround() || !this.wet && a.isInWater() || a.getTarget() != null || a.hurtTime > 0
                || a.isLeashed() || a.getRandom().nextInt(this.every) != 0) {
            return false;
        }
        int total = 0;
        for (Act act : this.acts) {
            total += act.fits.test(a) ? act.weight : 0;
        }
        if (total == 0) {
            return false;
        }
        int pick = a.getRandom().nextInt(total);
        for (Act act : this.acts) {
            if (act.fits.test(a) && (pick -= act.weight) < 0) {
                this.playing = act;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        WildAnimal a = this.animal;
        return a.act() == this.playing.kind && a.hurtTime == 0 && a.getTarget() == null
                && (this.wet || !a.isInWater());
    }

    @Override
    public void start() {
        Act act = this.playing;
        this.animal.getNavigation().stop();
        this.animal.start(act.kind, act.least + this.animal.getRandom().nextInt(act.most - act.least + 1));
    }

    @Override
    public void stop() {
        this.animal.end(this.playing.kind);
    }
}
