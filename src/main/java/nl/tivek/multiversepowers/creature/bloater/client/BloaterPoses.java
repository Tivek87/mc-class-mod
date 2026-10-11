package nl.tivek.multiversepowers.creature.bloater.client;

import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.bloater.Bloater;
import nl.tivek.multiversepowers.creature.bloater.BloaterMoves;
import nl.tivek.multiversepowers.creature.client.PoseSprings;
import nl.tivek.multiversepowers.creature.client.Trunk;

// How the Bloater stands this frame, as turns on top of its model's rest (BloaterModel): a slow waddle rolling from
// foot to foot, heavy breathing that heaves its belly, a slack jaw, and each of its moves on the server's timeline.
// Arms and legs turn forward with a negative angle; a positive lean bends the trunk forward.
final class BloaterPoses {
    private static final PoseSprings SPRINGS = new PoseSprings(0.28, 0.8);

    float lean;
    float twist;
    float roll;
    float sink;
    float nod;
    float shake;
    float jaw;
    float swell;
    float legSpread;
    final float[] arm = new float[2];
    final float[] spread = new float[2];
    final float[] leg = new float[2];

    void of(Bloater bloater, float limbSwing, float amount, float age) {
        this.lean = 0.0F;
        this.twist = 0.0F;
        this.roll = 0.0F;
        this.sink = 0.0F;
        this.nod = 0.0F;
        this.shake = 0.0F;
        this.jaw = 0.0F;
        this.swell = 0.0F;
        this.legSpread = 0.0F;
        for (int s = 0; s < 2; s++) {
            this.arm[s] = 0.0F;
            this.spread[s] = 0.0F;
            this.leg[s] = 0.0F;
        }
        int move = bloater.move();
        double t = bloater.moveAge(age - bloater.tickCount);
        this.walk(limbSwing, amount, move == BloaterMoves.RUSH);
        this.idle(age, bloater.getId());
        switch (move) {
            case BloaterMoves.SPIT -> this.spit(t);
            case BloaterMoves.SHRIEK -> this.shriek(t, age);
            case BloaterMoves.RUSH -> this.rush(t, age);
            default -> {
            }
        }
        float[] channels = { this.lean, this.twist, this.roll, this.sink, this.nod, this.jaw, this.swell,
                this.legSpread, this.arm[0], this.arm[1], this.spread[0], this.spread[1] };
        SPRINGS.follow(bloater.getId(), channels, age, bloater.level().getGameTime());
        this.lean = channels[0];
        this.twist = channels[1];
        this.roll = channels[2];
        this.sink = channels[3];
        this.nod = channels[4];
        this.jaw = Math.max(0.0F, channels[5]);
        this.swell = Math.max(0.0F, channels[6]);
        this.legSpread = Math.max(0.0F, channels[7]);
        this.arm[0] = channels[8];
        this.arm[1] = channels[9];
        this.spread[0] = channels[10];
        this.spread[1] = channels[11];
    }

    // Short steps, its weight rolling over the leg it stands on and its belly bouncing; running, longer and quicker.
    private void walk(float limbSwing, float amount, boolean running) {
        float phase = limbSwing * (running ? 0.75F : 0.6F);
        float w = Math.min(1.0F, amount * 1.4F);
        float c = Mth.cos(phase);
        float s = Mth.sin(phase);
        float stride = running ? 0.85F : 0.55F;
        this.leg[0] = stride * w * c;
        this.leg[1] = -this.leg[0];
        this.arm[0] = -0.25F * w * c;
        this.arm[1] = -this.arm[0];
        this.roll = 0.13F * w * s;
        this.twist = 0.08F * w * s;
        this.sink = 0.6F * w * Math.abs(c);
        this.nod += 0.05F * w * Math.abs(s);
    }

    // At rest it breathes deep and slow, its belly heaving, its jaw hanging and working.
    private void idle(float age, int id) {
        float breath = Mth.sin(age * 0.07F + id);
        this.swell += 0.05F * (1.0F + breath);
        this.lean -= 0.02F * breath;
        this.jaw = 0.15F + 0.07F * Mth.sin(age * 0.05F + id * 1.3F);
        for (int s = 0; s < 2; s++) {
            this.spread[s] += 0.04F * (1.0F + breath);
            this.arm[s] += 0.05F * Mth.sin(age * 0.05F + id + s * 2.0F);
        }
    }

    // It rears back, its belly heaving as it draws the slime up, then throws its head forward and spits it out wide.
    private void spit(double t) {
        float draw = Trunk.in(t, 0.0, BloaterMoves.SPAT - 3) * (1.0F - Trunk.in(t, BloaterMoves.SPAT - 3,
                BloaterMoves.SPAT));
        float out = Trunk.in(t, BloaterMoves.SPAT - 3, BloaterMoves.SPAT) * (1.0F - Trunk.in(t,
                BloaterMoves.SPAT + 2, BloaterMoves.SPIT_END));
        this.lean += -0.3F * draw + 0.35F * out;
        this.nod += -0.45F * draw + 0.4F * out;
        this.swell += 0.25F * draw;
        this.jaw = Math.max(this.jaw, 0.25F * draw + 1.0F * out);
        for (int s = 0; s < 2; s++) {
            this.spread[s] += 0.25F * draw;
            this.arm[s] += 0.35F * draw - 0.3F * out;
        }
    }

    // Head thrown back and arms flung wide, it shrieks, shaking.
    private void shriek(double t, float age) {
        float on = Trunk.in(t, 0.0, 4.0) * (1.0F - Trunk.in(t, BloaterMoves.SHRIEK_END - 3, BloaterMoves.SHRIEK_END));
        this.lean -= 0.3F * on;
        this.nod -= 0.8F * on;
        this.jaw = Math.max(this.jaw, 1.1F * on);
        this.shake += 0.06F * on * Mth.sin(age * 3.1F);
        this.swell += 0.15F * on;
        for (int s = 0; s < 2; s++) {
            this.spread[s] += 0.8F * on;
            this.arm[s] = Mth.lerp(on, this.arm[s], -0.6F) + 0.08F * on * Mth.sin(age * 2.4F + s * Mth.PI);
        }
    }

    // Leaning in, it runs with its arms flailing behind it and its mouth open, swelling more and more until it bursts,
    // trembling ever harder.
    private void rush(double t, float age) {
        float on = Trunk.in(t, 0.0, 4.0);
        float full = (float) Mth.clamp(t / BloaterMoves.BLOW, 0.0, 1.0);
        this.lean += 0.4F * on;
        this.nod -= 0.25F * on;
        this.jaw = Math.max(this.jaw, 0.65F * on);
        this.swell += full * full + 0.1F * on;
        this.shake += 0.04F * full * Mth.sin(age * 4.3F);
        this.roll += 0.03F * full * Mth.sin(age * 5.1F);
        for (int s = 0; s < 2; s++) {
            this.arm[s] = Mth.lerp(on, this.arm[s], 0.7F + 0.45F * Mth.sin(age * 1.1F + s * Mth.PI));
            this.spread[s] += 0.2F * on;
        }
    }
}
