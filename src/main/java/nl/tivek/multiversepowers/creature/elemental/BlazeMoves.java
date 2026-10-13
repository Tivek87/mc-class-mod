package nl.tivek.multiversepowers.creature.elemental;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.MonsterBlows;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;

// How the Elemental Blaze fights: it floats round its foe at a distance, a little above it, keeping it in sight, and
// casts one element after another, never the same twice running, picking what suits (a gust when its foe comes close,
// lightning when foes stand together, the frost ray at one in the open, a stone where there is ground to tear it from).
// Each cast shifts first (BlazeTimeline). From its second phase every cast is an Elemental Surge: a second, different
// element follows at once. At half health it stops whatever it does and awakens.
final class BlazeMoves extends Goal {
    private static final double CAST_RANGE = 16.0;
    private static final double CLOSE = 4.5;
    // Past this its gust no longer reaches.
    private static final double GUST = 6.0;
    private final ElementalBlaze blaze;
    private int kind;
    private int t;
    private int ready = 30;
    private int repath;
    private boolean surged;
    @Nullable
    private Element last;
    // What its last cast of its own choosing was: a surge's second cast aside, it never opens with the same twice.
    @Nullable
    private Element lead;

    BlazeMoves(ElementalBlaze blaze) {
        this.blaze = blaze;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.blaze.getTarget();
        return target != null && target.isAlive() && this.blaze.canAttack(target) || this.blaze.awakenDue();
    }

    @Override
    public boolean canContinueToUse() {
        return this.kind != 0 || this.canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.blaze.setAggressive(true);
    }

    @Override
    public void stop() {
        if (this.kind != 0) {
            this.blaze.start(0);
        }
        this.kind = 0;
        this.blaze.setAggressive(false);
        this.blaze.getNavigation().stop();
    }

    @Override
    public void tick() {
        LivingEntity target = this.blaze.getTarget();
        if (this.blaze.awakenDue() && this.kind != BlazeTimeline.AWAKEN) {
            this.begin(BlazeTimeline.AWAKEN);
        }
        if (target != null) {
            this.blaze.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
        if (this.kind != 0) {
            this.t++;
            this.hover();
            if (target != null) {
                MonsterBlows.face(this.blaze, target.position().subtract(this.blaze.position()));
            }
            this.go(target);
            return;
        }
        if (target == null) {
            return;
        }
        this.move(target);
        double far = this.blaze.distanceTo(target);
        if (this.blaze.tickCount >= this.ready && far <= CAST_RANGE && this.blaze.getSensing().hasLineOfSight(target)) {
            this.surged = false;
            this.lead = this.pick(target, far, this.lead);
            this.begin(BlazeTimeline.kind(this.lead, false));
        }
    }

    private void begin(int kind) {
        this.kind = kind;
        this.t = 0;
        this.blaze.getNavigation().stop();
        Element element = BlazeTimeline.element(kind);
        if (kind == BlazeTimeline.AWAKEN) {
            this.blaze.awaken();
        } else {
            this.blaze.start(kind);
        }
        if (kind == BlazeTimeline.AWAKEN) {
            this.blaze.playSound(SoundEvents.BLAZE_AMBIENT, 2.0F, 0.5F);
            this.blaze.playSound(SoundEvents.BEACON_POWER_SELECT, 1.5F, 0.6F);
        } else if (element != null) {
            this.last = element;
            this.blaze.playSound(shiftSound(element), 1.2F, kind > Element.ALL.length ? 1.3F : 0.9F);
            this.blaze.playSound(SoundEvents.BLAZE_AMBIENT, 0.8F, 1.2F);
        }
    }

    // The element's own sound as its rod swings round: a rumble, a crackle, a chime, a breath of wind, a fizz.
    private static SoundEvent shiftSound(Element element) {
        return switch (element) {
            case EARTH -> SoundEvents.GRAVEL_BREAK;
            case FIRE -> SoundEvents.FIRE_AMBIENT;
            case ICE -> SoundEvents.AMETHYST_BLOCK_RESONATE;
            case WIND -> SoundEvents.BREEZE_INHALE;
            case STORM -> SoundEvents.BEEHIVE_WORK;
        };
    }

    private void go(@Nullable LivingEntity target) {
        if (!(this.blaze.level() instanceof ServerLevel level)) {
            return;
        }
        if (this.kind == BlazeTimeline.AWAKEN) {
            if (this.t == BlazeTimeline.BURST) {
                BlazeCasts.nova(level, this.blaze);
            }
            if (this.t >= BlazeTimeline.AWAKEN_END) {
                this.end(null, false);
            }
            return;
        }
        Element element = BlazeTimeline.element(this.kind);
        if (element == null) {
            this.end(null, false);
            return;
        }
        int c = this.t - BlazeTimeline.shift(this.kind);
        if (c == 0 && element == Element.ICE) {
            BlazeCasts.frost(level, this.blaze, BlazeTimeline.BEAM);
        }
        if (c == 0 && element == Element.EARTH) {
            BlazeCasts.heave(level, this.blaze, BlazeTimeline.LIFT);
        }
        if (c == BlazeTimeline.cast(element)) {
            this.cast(level, element, target);
        }
        if (c >= BlazeTimeline.end(element)) {
            this.end(target, this.kind <= Element.ALL.length);
        }
    }

    private void cast(ServerLevel level, Element element, @Nullable LivingEntity target) {
        switch (element) {
            case FIRE -> BlazeCasts.scorch(level, this.blaze);
            case WIND -> BlazeCasts.gale(level, this.blaze);
            case STORM -> {
                Vec3 from = BlazeCasts.tip(this.blaze, 1.0F);
                boolean seen = target != null && this.blaze.hasLineOfSight(target)
                        && this.blaze.distanceTo(target) <= CAST_RANGE + 2.0;
                Vec3 miss = target != null ? target.position() : from.add(MonsterBlows.ahead(this.blaze).scale(6.0));
                BlazeCasts.chain(level, this.blaze, seen ? target : null, miss);
            }
            default -> {
            }
        }
    }

    // A move done: in the second phase the first cast of a surge is followed at once by a quick, different one.
    private void end(@Nullable LivingEntity target, boolean first) {
        if (first && this.blaze.phase() >= 2 && !this.surged && target != null && target.isAlive()) {
            this.surged = true;
            Element next = this.pick(target, this.blaze.distanceTo(target), this.last);
            this.begin(BlazeTimeline.kind(next, true));
            return;
        }
        this.kind = 0;
        this.blaze.start(0);
        int rest = this.blaze.phase() >= 2 ? 26 + this.blaze.getRandom().nextInt(16)
                : 36 + this.blaze.getRandom().nextInt(24);
        this.ready = this.blaze.tickCount + rest;
    }

    // Which element suits now, never the one it just cast (nor `not`).
    private Element pick(LivingEntity target, double far, @Nullable Element not) {
        double[] weight = new double[Element.ALL.length];
        weight[Element.FIRE.ordinal()] = 3.0;
        weight[Element.ICE.ordinal()] = far > 5.0 ? 2.5 : 1.0;
        weight[Element.WIND.ordinal()] = far < CLOSE ? 6.0 : far < GUST ? 1.0 : 0.0;
        weight[Element.EARTH.ordinal()] = this.ground() ? 2.5 : 0.0;
        int crowd = MonsterBlows.foes(this.blaze, target.getBoundingBox().inflate(6.0)).size();
        weight[Element.STORM.ordinal()] = 1.5 + 1.5 * Math.min(3, crowd - 1);
        if (this.last != null) {
            weight[this.last.ordinal()] = 0.0;
        }
        if (not != null) {
            weight[not.ordinal()] = 0.0;
        }
        double total = 0.0;
        for (double each : weight) {
            total += each;
        }
        double roll = this.blaze.getRandom().nextDouble() * total;
        for (Element element : Element.ALL) {
            roll -= weight[element.ordinal()];
            if (roll < 0.0 && weight[element.ordinal()] > 0.0) {
                return element;
            }
        }
        for (Element element : new Element[] {Element.FIRE, Element.ICE, Element.STORM}) {
            if (element != not && element != this.last) {
                return element;
            }
        }
        return Element.FIRE;
    }

    // Whether there is ground below and before it to tear a stone from.
    private boolean ground() {
        Vec3 over = this.blaze.position().add(MonsterBlows.ahead(this.blaze).scale(1.5));
        return LoadedWorld.clip(this.blaze.level(), new ClipContext(over, over.add(0.0, -9.0, 0.0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.blaze)).getType() == HitResult.Type.BLOCK;
    }

    // While it casts it hangs where it is, bobbing a little.
    private void hover() {
        this.blaze.getNavigation().stop();
        Vec3 v = this.blaze.getDeltaMovement().scale(0.6);
        this.blaze.setDeltaMovement(v.x, v.y + Mth.sin(this.blaze.tickCount * 0.2F) * 0.006, v.z);
    }

    // Between casts it drifts round its foe: farther off as it comes close, a few blocks above it, where it can still
    // see it.
    private void move(LivingEntity target) {
        if (--this.repath > 0 && !this.blaze.getNavigation().isDone()) {
            return;
        }
        this.repath = 20 + this.blaze.getRandom().nextInt(15);
        double haste = this.blaze.phase() >= 2 ? 1.3 : 1.0;
        Vec3 spot = this.spot(target);
        if (spot != null) {
            this.blaze.getNavigation().moveTo(spot.x, spot.y, spot.z, haste);
        } else if (this.blaze.distanceTo(target) > 10.0 || !this.blaze.getSensing().hasLineOfSight(target)) {
            this.blaze.getNavigation().moveTo(target, haste);
        }
    }

    @Nullable
    private Vec3 spot(LivingEntity target) {
        Vec3 from = target.position();
        Vec3 away = this.blaze.position().subtract(from).multiply(1.0, 0.0, 1.0);
        double heading = away.lengthSqr() < 1.0E-4 ? this.blaze.getRandom().nextDouble() * Mth.TWO_PI
                : Math.atan2(away.z, away.x);
        double near = this.blaze.phase() >= 2 ? 6.0 : 8.0;
        for (int tries = 0; tries < 8; tries++) {
            double angle = heading + (this.blaze.getRandom().nextDouble() - 0.5) * 1.8;
            double far = near + this.blaze.getRandom().nextDouble() * 4.0;
            double up = Math.max(1.5, 2.0 + this.blaze.getRandom().nextDouble() * 2.5 - tries * 0.3);
            Vec3 spot = from.add(Math.cos(angle) * far, up, Math.sin(angle) * far);
            if (!this.blaze.level().isLoaded(BlockPos.containing(spot))) {
                continue;
            }
            AABB box = this.blaze.getDimensions(this.blaze.getPose()).makeBoundingBox(spot);
            if (!this.blaze.level().noCollision(this.blaze, box.inflate(0.2))) {
                continue;
            }
            Vec3 eye = spot.add(0.0, this.blaze.getEyeHeight(), 0.0);
            if (LoadedWorld.clip(this.blaze.level(), new ClipContext(eye, target.getEyePosition(),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.blaze)).getType() == HitResult.Type.MISS) {
                return spot;
            }
        }
        return null;
    }
}
