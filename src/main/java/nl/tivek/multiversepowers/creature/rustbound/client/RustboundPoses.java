package nl.tivek.multiversepowers.creature.rustbound.client;

import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.client.PoseSprings;
import nl.tivek.multiversepowers.creature.client.Trunk;
import nl.tivek.multiversepowers.creature.rustbound.Rustbound;
import nl.tivek.multiversepowers.creature.rustbound.RustboundMoves;

// How the Rustbound holds itself this frame (RustboundModel): stooped, its heavy right fist hanging low and dragging
// its shoulder down, a lopsided lurch that drops its weight on that side; the core pulsing, now and then a flicker that
// shudders through it. On top of that its moves on the server's timeline: the fist raised high and smashed down, the
// pull with its feet planted and its arms out, trembling, then jerked back, and a sidearm fling with its thin left arm.
// Arms and legs turn forward with a negative angle; a positive lean bends the trunk forward.
final class RustboundPoses {
    private static final PoseSprings SPRINGS = new PoseSprings(0.28, 0.8);

    float lean;
    float sink;
    float twist;
    float roll;
    float nod;
    float shake;
    float tilt;
    float jaw;
    float legOut;
    float glow;
    final float[] arm = new float[2];
    final float[] armOut = new float[2];
    final float[] elbow = new float[2];
    final float[] leg = new float[2];
    final float[] knee = new float[2];

    void of(Rustbound rustbound, float limbSwing, float amount, float age) {
        this.lean = RustboundModel.LEAN;
        this.sink = 0.5F;
        this.twist = 0.0F;
        this.roll = -0.05F;
        this.nod = -0.05F;
        this.shake = 0.0F;
        this.tilt = 0.08F;
        this.jaw = 0.0F;
        this.legOut = 0.0F;
        this.glow = rustbound.wet() ? 0.08F : 0.35F + 0.12F * Mth.sin(age * 0.15F);
        this.arm[0] = -0.08F;
        this.armOut[0] = 0.12F;
        this.elbow[0] = 0.15F;
        this.arm[1] = -0.2F;
        this.armOut[1] = 0.02F;
        this.elbow[1] = 0.45F;
        for (int s = 0; s < 2; s++) {
            this.leg[s] = -0.1F;
            this.knee[s] = 0.22F;
        }
        int move = rustbound.move();
        double t = rustbound.moveAge(age - rustbound.tickCount);
        this.walk(limbSwing, amount);
        switch (move) {
            case RustboundMoves.SLAM -> this.slam(t);
            case RustboundMoves.PULL -> this.pull(t, age, rustbound.wet());
            case RustboundMoves.SHRAPNEL -> this.fling(t);
            default -> {
            }
        }
        float[] c = { this.lean, this.sink, this.twist, this.roll, this.nod, this.shake, this.tilt, this.jaw,
                this.legOut, this.glow, this.arm[0], this.arm[1], this.armOut[0], this.armOut[1], this.elbow[0],
                this.elbow[1], this.leg[0], this.leg[1], this.knee[0], this.knee[1] };
        SPRINGS.follow(rustbound.getId(), c, age, rustbound.level().getGameTime());
        this.lean = c[0];
        this.sink = c[1];
        this.twist = c[2];
        this.roll = c[3];
        this.nod = c[4];
        this.shake = c[5];
        this.tilt = c[6];
        this.jaw = Math.max(0.0F, c[7]);
        this.legOut = c[8];
        this.glow = Mth.clamp(c[9], 0.0F, 1.0F);
        for (int s = 0; s < 2; s++) {
            this.arm[s] = c[10 + s];
            this.armOut[s] = c[12 + s];
            this.elbow[s] = Math.max(0.0F, c[14 + s]);
            this.leg[s] = c[16 + s];
            this.knee[s] = Math.max(0.0F, c[18 + s]);
        }
        if (move == 0) {
            this.flicker(age, rustbound.getId(), rustbound.wet());
        }
    }

    // A heavy, lopsided lurch: the weight drops hard on the right with the fist, which swings little; the thin left
    // arm swings loose.
    private void walk(float limbSwing, float amount) {
        float phase = limbSwing * 0.55F;
        float w = Math.min(1.0F, amount * 1.5F);
        float c = Mth.cos(phase);
        float s = Mth.sin(phase);
        this.leg[0] += 0.6F * w * c;
        this.leg[1] -= 0.6F * w * c;
        this.knee[0] += 0.5F * w * Math.max(0.0F, -s);
        this.knee[1] += 0.5F * w * Math.max(0.0F, s);
        this.arm[0] -= 0.12F * w * c;
        this.arm[1] += 0.4F * w * c;
        this.elbow[1] += 0.2F * w * Math.max(0.0F, -c);
        this.roll -= 0.1F * w * Math.max(0.0F, s);
        this.sink += 1.1F * w * Math.abs(s);
        this.twist += 0.09F * w * c;
        this.tilt -= 0.06F * w * s;
    }

    // Now and then its core flickers and a shudder runs through it, its head jerking aside.
    private void flicker(float age, int id, boolean wet) {
        int slot = Mth.floor(age / 11.0F);
        int hash = slot * 83_492_791 ^ id * 29_765_423;
        if ((hash & 3) != 0) {
            return;
        }
        float into = age - slot * 11.0F;
        float jolt = into < 1.0F ? into : Math.max(0.0F, 1.0F - (into - 1.0F) / 6.0F);
        float way = (hash >> 2 & 1) == 0 ? 1.0F : -1.0F;
        this.glow += (wet ? 0.1F : 0.45F) * jolt * (0.5F + 0.5F * Mth.sin(into * 3.1F));
        this.shake += way * 0.25F * jolt;
        this.tilt += way * 0.2F * jolt;
        this.armOut[1] += 0.2F * jolt;
        this.roll += way * 0.04F * jolt;
    }

    // The fist goes up high over its head as it rears back, then it smashes it down, knees giving under the weight.
    private void slam(double t) {
        float wind = Trunk.in(t, 0.0, RustboundMoves.SLAM_HIT - 2) * (1.0F - Trunk.in(t, RustboundMoves.SLAM_HIT - 2,
                RustboundMoves.SLAM_HIT));
        float hit = Trunk.in(t, RustboundMoves.SLAM_HIT - 2, RustboundMoves.SLAM_HIT) * (1.0F - Trunk.in(t,
                RustboundMoves.SLAM_HIT + 4, RustboundMoves.SLAM_END));
        this.lean += -0.35F * wind + 0.55F * hit;
        this.nod += -0.3F * wind + 0.3F * hit;
        this.twist += 0.3F * wind - 0.15F * hit;
        this.jaw += 0.35F * wind;
        this.arm[0] += -2.7F * wind - 0.9F * hit;
        this.armOut[0] += 0.15F * wind - 0.05F * hit;
        this.elbow[0] += 0.9F * wind - 0.1F * hit;
        this.arm[1] += -0.6F * wind + 0.3F * hit;
        this.armOut[1] += 0.5F * wind;
        this.sink += 2.5F * hit;
        this.knee[0] += 0.6F * hit;
        this.knee[1] += 0.8F * hit;
        this.leg[0] -= 0.4F * hit;
        this.leg[1] += 0.3F * hit;
        this.glow += 0.2F * hit;
    }

    // Feet planted wide and knees bent, it brings both arms up in front of it, palms out; its core flares and it leans
    // back against the pull, trembling, head back, moaning; then it jerks its arms back to its chest.
    private void pull(double t, float age, boolean wet) {
        float wind = Trunk.in(t, 0.0, RustboundMoves.PULL_FROM);
        float hold = Trunk.in(t, RustboundMoves.PULL_FROM - 2, RustboundMoves.PULL_FROM) * (1.0F - Trunk.in(t,
                RustboundMoves.YANK - 1, RustboundMoves.YANK));
        float yank = Trunk.in(t, RustboundMoves.YANK - 1, RustboundMoves.YANK + 1) * (1.0F - Trunk.in(t,
                RustboundMoves.YANK + 3, RustboundMoves.PULL_END));
        float set = wind * (1.0F - yank * 0.6F) * (1.0F - Trunk.in(t, RustboundMoves.PULL_END - 6,
                RustboundMoves.PULL_END));
        float shiver = hold * Mth.sin(age * 5.3F);
        this.legOut += 0.32F * set;
        this.sink += 1.6F * set;
        this.lean += -0.35F * set - 0.3F * yank;
        this.nod += -0.35F * hold;
        this.jaw += 0.45F * hold;
        this.shake += 0.05F * shiver;
        this.roll += 0.02F * shiver;
        for (int s = 0; s < 2; s++) {
            this.arm[s] += -1.45F * set + 1.1F * yank;
            this.armOut[s] += 0.55F * set - 0.4F * yank + 0.04F * hold * Mth.sin(age * 6.1F + s * 1.7F);
            this.elbow[s] += 0.1F * set + 1.5F * yank;
            this.knee[s] += 0.55F * set;
        }
        this.glow += wet ? 0.0F : 0.55F * wind + 0.25F * hold * (0.5F + 0.5F * Mth.sin(age * 2.7F)) + 0.4F * yank;
    }

    // Twisting away, its thin left arm reaches back into the scrap circling it, then whips round and out in a wide
    // sidearm throw.
    private void fling(double t) {
        float wind = Trunk.in(t, 0.0, RustboundMoves.THROW - 1) * (1.0F - Trunk.in(t, RustboundMoves.THROW - 1,
                RustboundMoves.THROW));
        float out = Trunk.in(t, RustboundMoves.THROW - 1, RustboundMoves.THROW + 1) * (1.0F - Trunk.in(t,
                RustboundMoves.THROW + 3, RustboundMoves.THROW_END));
        this.twist += -0.6F * wind + 0.55F * out;
        this.lean += -0.1F * wind + 0.3F * out;
        this.nod += 0.1F * out;
        this.arm[1] += 0.7F * wind - 1.6F * out;
        this.armOut[1] += 1.1F * wind - 0.15F * out;
        this.elbow[1] += 1.0F * wind - 0.35F * out;
        this.arm[0] += -0.3F * wind + 0.15F * out;
        this.knee[1] += 0.3F * wind;
        this.leg[1] -= 0.25F * out;
    }
}
