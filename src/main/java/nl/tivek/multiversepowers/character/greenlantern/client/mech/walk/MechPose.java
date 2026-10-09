package nl.tivek.multiversepowers.character.greenlantern.client.mech.walk;

import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.MechPainter;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.touch.MechHandRig;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechAttacks;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechMoves;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechScript;

// How a walking mech stands at one moment: its spot on the ground, its swaying body, where its feet are, how its arms
// swing and what its pilot's hands do. Feet are in the world, index 0 the right one.
public final class MechPose {
    MechScript.Stage stage;
    // The hips ride the legs; the torso above them turns on the waist by `turn` to face where its pilot looks.
    MechScript.Stage hips;
    MechScript.Stage torso;
    double turn;
    // The torso's lean ahead and to its side over the waist, as it swings and the legs turn under it.
    double lean;
    double bank;
    public final Vec3[] ankle = new Vec3[2];
    public final Vec3[] toes = new Vec3[2];
    public final double[] tip = new double[2];
    double swing;
    double walking;
    double running;
    // How far its exhaust is charged up by running on (0 to 1).
    double boost;
    public double headYaw;
    public double headPitch;
    double leverLeft;
    double leverRight;
    public int button = -1;
    public double press;
    MechAttacks.Blow blow = MechAttacks.Blow.NONE;
    // What its pilot's crosshair rests on while the missile arm aims (null: straight ahead).
    @Nullable
    Vec3 aim;
    // Climbing, where each hand holds on (the middle of its palm, in the world), which way its fingers run and its palm
    // faces, how firmly it holds (0: not at all) and how far its fingers curl.
    public final Vec3[] ledge = new Vec3[2];
    final Vec3[] along = new Vec3[2];
    final Vec3[] facing = new Vec3[2];
    final double[] grip = new double[2];
    final double[] curl = new double[2];
    // As its weight swings them (index 0 the right): each hand's turn at its wrist, folded towards its palm (+) and
    // tilted across it, and each shoulder's shrug (up +), in radians.
    public final double[] fold = new double[2];
    public final double[] tilt = new double[2];
    public final double[] shrug = new double[2];

    public MechPose(MechScript.Stage stage) {
        this.stage = stage;
        this.hips = stage;
        this.torso = stage;
        for (int side = 0; side < 2; side++) {
            this.ankle[side] = stage.point(MechPainter.side(MechScript.ANKLE, side == 0));
            this.toes[side] = stage.ahead();
        }
    }

    // A mech as its build poses it, the hips and torso where the build has them (MechBuild).
    public MechPose(MechScript.Stage stage, MechScript.Stage hips, MechScript.Stage torso) {
        this(stage);
        this.hips = hips;
        this.torso = torso;
    }

    MechPose copy() {
        MechPose pose = new MechPose(this.stage);
        pose.hips = this.hips;
        pose.torso = this.torso;
        pose.turn = this.turn;
        pose.lean = this.lean;
        pose.bank = this.bank;
        pose.running = this.running;
        pose.boost = this.boost;
        for (int side = 0; side < 2; side++) {
            pose.ankle[side] = this.ankle[side];
            pose.toes[side] = this.toes[side];
            pose.tip[side] = this.tip[side];
            pose.ledge[side] = this.ledge[side];
            pose.along[side] = this.along[side];
            pose.facing[side] = this.facing[side];
            pose.grip[side] = this.grip[side];
            pose.curl[side] = this.curl[side];
            pose.fold[side] = this.fold[side];
            pose.tilt[side] = this.tilt[side];
            pose.shrug[side] = this.shrug[side];
        }
        pose.swing = this.swing;
        pose.walking = this.walking;
        pose.headYaw = this.headYaw;
        pose.headPitch = this.headPitch;
        pose.leverLeft = this.leverLeft;
        pose.leverRight = this.leverRight;
        pose.button = this.button;
        pose.press = this.press;
        pose.blow = this.blow;
        pose.aim = this.aim;
        return pose;
    }

    static MechPose between(MechPose from, MechPose to, double u) {
        MechPose pose = to.copy();
        pose.stage = between(from.stage, to.stage, u);
        pose.hips = between(from.hips, to.hips, u);
        pose.turn = from.turn + Math.IEEEremainder(to.turn - from.turn, Math.PI * 2.0) * u;
        pose.lean = Mth.lerp(u, from.lean, to.lean);
        pose.bank = Mth.lerp(u, from.bank, to.bank);
        pose.running = Mth.lerp(u, from.running, to.running);
        pose.boost = Mth.lerp(u, from.boost, to.boost);
        pose.torso = MechScript.upper(pose.hips, pose.turn, pose.lean, pose.bank);
        for (int side = 0; side < 2; side++) {
            pose.ankle[side] = from.ankle[side].lerp(to.ankle[side], u);
            pose.toes[side] = from.toes[side].lerp(to.toes[side], u).normalize();
            pose.tip[side] = Mth.lerp(u, from.tip[side], to.tip[side]);
            pose.fold[side] = Mth.lerp(u, from.fold[side], to.fold[side]);
            pose.tilt[side] = Mth.lerp(u, from.tilt[side], to.tilt[side]);
            pose.shrug[side] = Mth.lerp(u, from.shrug[side], to.shrug[side]);
            // A hand on its way between holds moves on smoothly between the ticks.
            if (from.ledge[side] != null && to.ledge[side] != null) {
                pose.ledge[side] = from.ledge[side].lerp(to.ledge[side], u);
                pose.along[side] = from.along[side].lerp(to.along[side], u).normalize();
                pose.facing[side] = from.facing[side].lerp(to.facing[side], u).normalize();
                pose.grip[side] = Mth.lerp(u, from.grip[side], to.grip[side]);
                pose.curl[side] = Mth.lerp(u, from.curl[side], to.curl[side]);
            }
        }
        pose.swing = Mth.lerp(u, from.swing, to.swing);
        pose.walking = Mth.lerp(u, from.walking, to.walking);
        pose.headYaw = Mth.lerp(u, from.headYaw, to.headYaw);
        pose.headPitch = Mth.lerp(u, from.headPitch, to.headPitch);
        pose.leverLeft = Mth.lerp(u, from.leverLeft, to.leverLeft);
        pose.leverRight = Mth.lerp(u, from.leverRight, to.leverRight);
        pose.press = from.button == to.button ? Mth.lerp(u, from.press, to.press) : to.press;
        pose.blow = from.blow.kind() == to.blow.kind() && to.blow.age() >= from.blow.age() ? new MechAttacks.Blow(
                to.blow.kind(), Mth.lerp(u, from.blow.age(), to.blow.age()), to.blow.from(), to.blow.turn())
                : to.blow;
        pose.aim = from.aim != null && to.aim != null ? from.aim.lerp(to.aim, u) : to.aim;
        return pose;
    }

    private static MechScript.Stage between(MechScript.Stage a, MechScript.Stage b, double u) {
        Vec3 ahead = a.ahead().lerp(b.ahead(), u).normalize();
        Vec3 up = a.up().lerp(b.up(), u);
        up = up.subtract(ahead.scale(up.dot(ahead))).normalize();
        return new MechScript.Stage(a.base().lerp(b.base(), u), ahead, ahead.cross(up), up, b.targetY(), b.pilotY(),
                b.pilotZ());
    }

    public MechScript.Stage stage() {
        return this.stage;
    }

    public MechScript.Stage hips() {
        return this.hips;
    }

    public MechScript.Stage torso() {
        return this.torso;
    }

    public double walking() {
        return this.walking;
    }

    public double running() {
        return this.running;
    }

    public double boost() {
        return this.boost;
    }

    // Where the pilot sits, by the soles of their feet.
    public Vec3 seat() {
        return this.torso.point(MechScript.COCKPIT);
    }

    // The grip of one lever (side 1 right, -1 left), thrown forward by its push.
    public Vec3 grip(int side) {
        return grip(this.torso, side, side > 0 ? this.leverRight : this.leverLeft);
    }

    // The grip of a lever in `torso` thrown `push` of the way forward (-1 back).
    public static Vec3 grip(MechScript.Stage torso, int side, double push) {
        double angle = push * MechScript.LEVER_THROW;
        Vec3 foot = new Vec3(side * MechScript.LEVER.x, MechScript.LEVER.y, MechScript.LEVER.z);
        return torso.point(foot.add(0.0, Math.cos(angle) * MechScript.LEVER_LENGTH,
                Math.sin(angle) * MechScript.LEVER_LENGTH));
    }

    public void levers(double right, double left) {
        this.leverRight = right;
        this.leverLeft = left;
    }

    public double push(int side) {
        return side > 0 ? this.leverRight : this.leverLeft;
    }

    // The button a hand is reaching for or pressing (side 1 right, -1 left), and how far: 0 on its lever, 1 pressed.
    public int button(int side) {
        if (this.button < 0 || this.press <= 0.0) {
            return -1;
        }
        return (MechScript.BUTTONS[this.button].x > 0.0) == (side > 0) ? this.button : -1;
    }

    public double press() {
        return this.press;
    }

    public MechAttacks.Blow blow() {
        return this.blow;
    }

    // Whether its hands reach for or hold what it climbs.
    public boolean climbing() {
        return this.ledge[0] != null && (this.grip[0] > 0.0 || this.grip[1] > 0.0);
    }

    // One arm as the walk swings it, taken over by a blow while one is struck or by the ledge it climbs (at `t` of the
    // build, past its end).
    public MechMoves.Arm arm(boolean right, double t, @Nullable MechAttacks.Held held) {
        return this.arm(right, t, held, null);
    }

    // As above, a climbing hand resting on what it holds when the blocks round it are known.
    public MechMoves.Arm arm(boolean right, double t, @Nullable MechAttacks.Held held, @Nullable MechHandRig.Ground ledge) {
        MechMoves.Arm arm = MechMoves.walking(right, t, this.swing, this.walking, this.running);
        int s = right ? 0 : 1;
        Vec3 spot = this.ledge[s];
        if (this.grip[s] > 0.0 && spot != null) {
            return MechClimb.laid(arm, this.torso, right, spot, this.along[s], this.facing[s], this.grip[s],
                    this.curl[s], ledge);
        }
        if (!this.blow.striking()) {
            return arm;
        }
        MechScript.Stage frame = MechAttacks.frame(this.stage.base(), this.torso, MechAttacks.body(this.blow).twist());
        return MechAttacks.arm(this.blow, right, frame, this.torso, held, arm, this.aim);
    }
}
