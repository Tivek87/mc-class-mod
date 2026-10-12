package nl.tivek.multiversepowers.creature.wizard;

import java.util.EnumSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import nl.tivek.multiversepowers.creature.wizard.staff.WizardStaff;

// The Wizard with no foe about (none in reach of his senses, WizardEntities' follow range): his staff called home if
// it is out, down out of the air to his feet, then a pastime now and then (WizardPastimes), strolling and looking
// round between them. A foe in sight cuts it short (WizardMoves comes first).
final class WizardIdle extends Goal {
    // Ticks between pastimes, at least and more at random; the first comes soon.
    private static final int REST = 160;
    private static final int REST_MORE = 260;
    private final Wizard wizard;
    private final WizardMoves moves;
    private final WizardPastimes pastimes;
    private int kind;
    private int t;
    private int last;
    private int nextAt = 80;

    WizardIdle(Wizard wizard, WizardMoves moves) {
        this.wizard = wizard;
        this.moves = moves;
        this.pastimes = new WizardPastimes(wizard);
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    private boolean calm() {
        return this.wizard.getTarget() == null && this.wizard.isAlive() && !this.moves.busy();
    }

    @Override
    public boolean canUse() {
        return this.calm() && this.wizard.move() == 0 && (this.wizard.flies() || this.staffAway()
                || this.wizard.tickCount >= this.nextAt && this.wizard.onGround());
    }

    @Override
    public boolean canContinueToUse() {
        return this.calm() && this.kind != 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    // His staff out where he left it in a fight: thrown, stuck fast or fighting on its own.
    private boolean staffAway() {
        WizardStaff staff = WizardStaff.of(this.wizard);
        if (staff == null) {
            if (this.wizard.staffOut()) {
                this.wizard.setStaffOut(false);
            }
            return false;
        }
        int mode = staff.mode();
        return mode == WizardStaff.STUCK || mode == WizardStaff.FREE || mode == WizardStaff.THROWN
                || mode >= WizardStaff.PLANTED && this.kind == 0;
    }

    @Override
    public void start() {
        this.t = 0;
        this.wizard.getNavigation().stop();
        WizardStaff staff = WizardStaff.of(this.wizard);
        if (staff != null && staff.mode() >= WizardStaff.PLANTED) {
            staff.putAway();
        } else if (this.staffAway()) {
            staff.call();
            this.kind = WizardMove.STAFF_CALL;
        } else if (this.wizard.flies()) {
            this.kind = WizardMove.ALIGHT;
        } else {
            ServerLevel level = (ServerLevel) this.wizard.level();
            this.kind = this.pastimes.pick(level, this.wizard.getRandom(), this.last);
            this.last = this.kind;
            this.pastimes.begin(level, this.kind);
        }
        if (this.kind != 0) {
            this.wizard.start(this.kind);
        }
    }

    @Override
    public void tick() {
        this.t++;
        ServerLevel level = (ServerLevel) this.wizard.level();
        if (this.kind == WizardMove.ALIGHT) {
            this.moves.flight().alight(level, this.t);
        } else if (WizardMove.pastime(this.kind)) {
            this.pastimes.tick(level, this.kind, this.t);
        }
        if (this.t >= WizardMove.length(this.kind)) {
            this.done();
        }
    }

    private void done() {
        int now = this.wizard.tickCount;
        if (WizardMove.pastime(this.kind)) {
            this.pastimes.end(this.kind);
            this.nextAt = now + REST + this.wizard.getRandom().nextInt(REST_MORE);
        } else {
            this.nextAt = Math.min(this.nextAt, now + 40);
        }
        this.kind = 0;
        this.wizard.start(0);
    }

    @Override
    public void stop() {
        if (this.kind == 0) {
            return;
        }
        if (WizardMove.pastime(this.kind)) {
            this.pastimes.end(this.kind);
        }
        if (this.wizard.move() == this.kind) {
            this.wizard.start(0);
        }
        this.kind = 0;
    }
}
