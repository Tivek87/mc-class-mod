package nl.tivek.multiversepowers.character.greenlantern.client.mech;

import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
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
    final Vec3[] ankle = new Vec3[2];
    final Vec3[] toes = new Vec3[2];
    final double[] tip = new double[2];
    double swing;
    double walking;
    double running;
    double headYaw;
    double headPitch;
    double leverLeft;
    double leverRight;
    int button = -1;
    double press;
    MechAttacks.Blow blow = MechAttacks.Blow.NONE;

    MechPose(MechScript.Stage stage) {
        this.stage = stage;
        this.hips = stage;
        this.torso = stage;
        for (int side = 0; side < 2; side++) {
            this.ankle[side] = stage.point(MechPainter.side(MechScript.ANKLE, side == 0));
            this.toes[side] = stage.ahead();
        }
    }

    MechPose copy() {
        MechPose pose = new MechPose(this.stage);
        pose.hips = this.hips;
        pose.torso = this.torso;
        pose.turn = this.turn;
        pose.lean = this.lean;
        pose.bank = this.bank;
        pose.running = this.running;
        for (int side = 0; side < 2; side++) {
            pose.ankle[side] = this.ankle[side];
            pose.toes[side] = this.toes[side];
            pose.tip[side] = this.tip[side];
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
        pose.torso = MechScript.upper(pose.hips, pose.turn, pose.lean, pose.bank);
        for (int side = 0; side < 2; side++) {
            pose.ankle[side] = from.ankle[side].lerp(to.ankle[side], u);
            pose.toes[side] = from.toes[side].lerp(to.toes[side], u).normalize();
            pose.tip[side] = Mth.lerp(u, from.tip[side], to.tip[side]);
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

    // Where the pilot sits, by the soles of their feet.
    public Vec3 seat() {
        return this.torso.point(MechScript.COCKPIT);
    }

    // The grip of one lever (side 1 right, -1 left), thrown forward by its push.
    public Vec3 grip(int side) {
        double push = (side > 0 ? this.leverRight : this.leverLeft) * MechScript.LEVER_THROW;
        Vec3 foot = new Vec3(side * MechScript.LEVER.x, MechScript.LEVER.y, MechScript.LEVER.z);
        return this.torso.point(foot.add(0.0, Math.cos(push) * MechScript.LEVER_LENGTH,
                Math.sin(push) * MechScript.LEVER_LENGTH));
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

    // One arm as the walk swings it, taken over by a blow while one is struck (at `t` of the build, past its end).
    public MechMoves.Arm arm(boolean right, double t, @Nullable MechAttacks.Held held) {
        // Running, the forearms come up and pump instead of hanging.
        double hang = this.walking * (1.0 - 0.8 * this.running);
        MechMoves.Arm arm = MechMoves.walking(right, t, this.swing, hang);
        if (!this.blow.striking()) {
            return arm;
        }
        MechScript.Stage frame = MechAttacks.frame(this.stage.base(), this.torso, MechAttacks.body(this.blow).twist());
        return MechAttacks.arm(this.blow, right, frame, this.torso, held, arm);
    }
}
