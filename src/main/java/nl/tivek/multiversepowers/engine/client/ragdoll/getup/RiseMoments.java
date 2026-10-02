package nl.tivek.multiversepowers.engine.client.ragdoll.getup;

// The moments a person gets up by (PersonRise), for a body that steps through with its right foot (and posts its left
// hand, face up); mirrored for the other side.
final class RiseMoments {
    // The ends that rest at a moment: a hand flat on the ground or on its own knee, a knee on the ground, a foot flat
    // or on its toes.
    static final int HAND_R = 1;
    static final int HAND_L = 2;
    static final int KNEE_R = 4;
    static final int KNEE_L = 8;
    static final int FOOT_R = 16;
    static final int FOOT_L = 32;
    static final int ON_KNEE_R = 64;
    static final int ON_KNEE_L = 128;
    static final int TOES_R = 256;
    static final int TOES_L = 512;

    // What moves on a curve of its own, a little ahead of or behind the moments: the trunk, the head, each arm (with
    // its shoulder blade) and each leg.
    static final int TRUNK = 0;
    static final int HEAD = 1;
    static final int ARM_R = 2;
    static final int GROUPS = 6;

    // A moment: when (u); how far ahead (+) or behind each group reaches it; the pelvis's turn (yaw, pitch ahead: 1.57
    // face down, -1.57 face up, roll); where its hips are from its way to where it stands (sideways, ahead; how high
    // comes from the ground); the waist's and low back's fold (ahead +); the head's nod (down +) and turn; the shoulder
    // blades' shrug (up +) and roll (ahead +) beyond what the arms make them do, right then left; each limb's turn as a
    // model part turns (x, y, z) and its middle and end joints' folds (an elbow ahead, a knee back, a wrist ahead, an
    // ankle pointing), right arm, left arm, right leg, left leg; and the ends resting.
    record Moment(float u, float[] lead, float yaw, float pitch, float roll, float side, float ahead, float waist,
            float low, float nod, float shake, float[] blades, float[][] limbs, int rests) {
        Moment at(float when) {
            return new Moment(when, this.lead, this.yaw, this.pitch, this.roll, this.side, this.ahead, this.waist,
                    this.low, this.nod, this.shake, this.blades, this.limbs, this.rests);
        }
    }

    private static float[] lead(float trunk, float head, float armR, float armL, float legR, float legL) {
        return new float[] { trunk, head, armR, armL, legR, legL };
    }

    // Face down: it draws its hands in under its shoulders and lifts its head, pushes its chest up, draws its knees in
    // under its hips, steps its right foot up beside its hand, kneels up with its hand on that knee, rises over that
    // foot and brings the other up beside it.
    static final Moment[] FRONT = {
            // Hands drawn in flat beside its chest, head lifting, feet lying pointed.
            new Moment(0.14F, lead(-0.02F, 0.03F, 0.025F, -0.02F, -0.02F, -0.02F), 0.0F, 1.5F, 0.0F, 0.0F, 0.0F,
                    -0.12F, -0.05F, -0.45F, 0.0F, new float[] { 0.05F, 0.0F, 0.05F, 0.0F },
                    new float[][] { { 0.45F, 0.0F, 0.55F, 2.3F, 0.4F }, { 0.45F, 0.0F, -0.55F, 2.3F, 0.4F },
                            { 0.0F, 0.0F, 0.05F, 0.15F, 0.3F }, { 0.0F, 0.0F, -0.05F, 0.15F, 0.3F } },
                    HAND_R | HAND_L | KNEE_R | KNEE_L),
            // Pushing its chest up on straightening arms, hips still low.
            new Moment(0.28F, lead(0.0F, 0.02F, 0.0F, 0.0F, -0.02F, -0.02F), 0.0F, 1.2F, 0.0F, 0.0F, 1.0F, -0.25F,
                    -0.1F, -0.3F, 0.0F, new float[] { 0.08F, 0.15F, 0.08F, 0.15F },
                    new float[][] { { -0.9F, 0.0F, 0.15F, 0.25F, 0.9F }, { -0.9F, 0.0F, -0.15F, 0.25F, 0.9F },
                            { -0.15F, 0.0F, 0.06F, 0.25F, 0.3F }, { -0.15F, 0.0F, -0.06F, 0.25F, 0.3F } },
                    HAND_R | HAND_L | KNEE_R | KNEE_L),
            // Hips drawn back over its knees: on its hands and knees.
            new Moment(0.41F, lead(0.0F, -0.02F, 0.0F, 0.0F, 0.02F, 0.02F), 0.0F, 1.42F, 0.0F, 0.0F, -4.0F, 0.1F,
                    0.15F, -0.1F, 0.0F, new float[] { 0.05F, 0.12F, 0.05F, 0.12F },
                    new float[][] { { -1.4F, 0.0F, 0.08F, 0.05F, 1.0F }, { -1.4F, 0.0F, -0.08F, 0.05F, 1.0F },
                            { -1.45F, 0.0F, 0.05F, 1.55F, 0.2F }, { -1.45F, 0.0F, -0.05F, 1.55F, 0.2F } },
                    HAND_R | HAND_L | KNEE_R | KNEE_L),
            // Its right foot stepped up beside its right hand, its weight on its hands and left knee.
            new Moment(0.53F, lead(0.0F, -0.02F, -0.02F, 0.0F, 0.035F, 0.0F), 0.1F, 1.0F, -0.12F, 0.0F, 0.0F, 0.2F,
                    0.1F, 0.0F, 0.0F, new float[] { 0.05F, 0.1F, 0.05F, 0.1F },
                    new float[][] { { -1.0F, 0.0F, 0.1F, 0.2F, 0.8F }, { -1.0F, 0.0F, -0.1F, 0.2F, 0.8F },
                            { -1.9F, 0.0F, 0.1F, 2.1F, 0.0F }, { -1.0F, 0.0F, -0.05F, 1.6F, 0.15F } },
                    HAND_R | HAND_L | KNEE_L | FOOT_R),
            // Kneeling up on its left knee, its right hand on its right knee.
            new Moment(0.65F, lead(0.0F, -0.025F, 0.02F, -0.03F, 0.0F, 0.0F), 0.0F, 0.25F, 0.0F, 0.0F, 1.0F, 0.1F,
                    0.05F, 0.12F, 0.0F, new float[] { 0.0F, 0.0F, 0.0F, 0.0F },
                    new float[][] { { -0.9F, 0.0F, 0.2F, 0.8F, 0.3F }, { 0.2F, 0.0F, -0.1F, 0.3F, 0.0F },
                            { -1.55F, 0.0F, 0.1F, 1.6F, 0.0F }, { -0.05F, 0.0F, -0.05F, 1.55F, 0.1F } },
                    KNEE_L | FOOT_R | ON_KNEE_R),
            // Rising over its right foot, pushing on that knee, its left foot on its toes behind.
            new Moment(0.78F, lead(0.0F, -0.02F, 0.0F, -0.02F, 0.0F, 0.0F), 0.0F, 0.45F, 0.0F, 0.0F, 3.0F, 0.15F,
                    0.1F, 0.1F, 0.0F, new float[] { 0.0F, 0.0F, 0.0F, 0.0F },
                    new float[][] { { -0.6F, 0.0F, 0.25F, 0.5F, 0.2F }, { -0.3F, 0.0F, -0.1F, 0.4F, 0.0F },
                            { -0.95F, 0.0F, 0.05F, 1.0F, 0.0F }, { 0.35F, 0.0F, -0.05F, 0.6F, 0.5F } },
                    FOOT_R | TOES_L | ON_KNEE_R),
            // Standing, its left foot brought up beside its right, its arms swinging down.
            new Moment(0.9F, lead(0.0F, -0.02F, -0.02F, -0.03F, 0.0F, 0.0F), 0.0F, 0.03F, 0.0F, 0.0F, 0.0F, 0.0F,
                    0.0F, 0.0F, 0.0F, new float[] { 0.0F, 0.0F, 0.0F, 0.0F },
                    new float[][] { { 0.06F, 0.0F, 0.06F, 0.12F, 0.0F }, { -0.08F, 0.0F, -0.06F, 0.15F, 0.0F },
                            { 0.0F, 0.0F, 0.0F, 0.05F, 0.0F }, { 0.0F, 0.0F, 0.0F, 0.05F, 0.0F } },
                    FOOT_R | FOOT_L) };

    // Face up: it tucks its chin and draws its knees up, sits up on its left hand, turns onto that hand and its left
    // knee with its right foot planted, and goes on as face down from kneeling.
    static final Moment[] BACK = {
            // Chin tucked, knees drawn up, hands flat beside its hips.
            new Moment(0.12F, lead(-0.02F, 0.03F, 0.0F, 0.0F, 0.015F, 0.0F), 0.0F, -1.5F, 0.0F, 0.0F, 0.0F, 0.15F,
                    0.05F, 0.45F, 0.0F, new float[] { 0.0F, 0.0F, 0.0F, 0.0F },
                    new float[][] { { 0.15F, 0.0F, 0.35F, 0.2F, 0.0F }, { 0.15F, 0.0F, -0.35F, 0.2F, 0.0F },
                            { -1.0F, 0.0F, 0.08F, 1.9F, -0.3F }, { -1.0F, 0.0F, -0.08F, 1.9F, -0.3F } },
                    HAND_R | HAND_L | FOOT_R | FOOT_L),
            // Sat up, curled over its knees, leaning on its left hand behind it, its right arm reaching ahead.
            new Moment(0.27F, lead(0.0F, 0.025F, 0.02F, 0.0F, 0.0F, 0.0F), 0.0F, -0.75F, 0.0F, 0.0F, -1.0F, 0.45F,
                    0.25F, 0.25F, 0.0F, new float[] { 0.1F, 0.1F, 0.0F, -0.1F },
                    new float[][] { { -0.9F, 0.0F, 0.1F, 0.4F, 0.0F }, { 0.7F, 0.0F, -0.35F, 0.15F, 0.0F },
                            { -1.1F, 0.0F, 0.08F, 2.0F, -0.2F }, { -1.1F, 0.0F, -0.08F, 2.0F, -0.2F } },
                    HAND_L | FOOT_R | FOOT_L),
            // Turned onto its left hand and knee, its right foot planted ahead.
            new Moment(0.42F, lead(0.0F, -0.02F, 0.0F, 0.0F, 0.0F, 0.02F), -0.4F, 0.55F, 0.22F, 1.5F, -1.0F, 0.3F,
                    0.15F, 0.15F, 0.0F, new float[] { 0.0F, 0.1F, 0.1F, 0.1F },
                    new float[][] { { -0.7F, 0.0F, 0.15F, 0.6F, 0.0F }, { -0.5F, 0.0F, -0.4F, 0.2F, 0.8F },
                            { -1.4F, 0.0F, 0.08F, 1.7F, 0.0F }, { -0.6F, 0.0F, -0.15F, 2.0F, 0.15F } },
                    HAND_L | KNEE_L | FOOT_R),
            FRONT[4].at(0.57F), FRONT[5].at(0.74F), FRONT[6].at(0.89F) };

    // Sitting up (slumped against a wall): it curls up from there, as face up once it has sat up.
    static final Moment[] SEATED = { BACK[1].at(0.17F), BACK[2].at(0.36F), FRONT[4].at(0.53F), FRONT[5].at(0.72F),
            FRONT[6].at(0.89F) };

    private RiseMoments() {
    }

    static boolean kneels(int l, int rests) {
        return l == 2 && (rests & KNEE_R) != 0 || l == 3 && (rests & KNEE_L) != 0;
    }

    static boolean onKnee(int l, int rests) {
        return l == 0 && (rests & ON_KNEE_R) != 0 || l == 1 && (rests & ON_KNEE_L) != 0;
    }

    static boolean toes(int l, int rests) {
        return l == 2 && (rests & TOES_R) != 0 || l == 3 && (rests & TOES_L) != 0;
    }

    // Whether limb l rests at all, and how: 0 not, 1 its far end flat on the ground, 2 its knee, 3 its hand on its
    // knee, 4 its foot on its toes.
    static int how(int l, int rests) {
        if (onKnee(l, rests)) {
            return 3;
        }
        if (kneels(l, rests)) {
            return 2;
        }
        if (toes(l, rests)) {
            return 4;
        }
        int bits = l == 0 ? HAND_R : l == 1 ? HAND_L : l == 2 ? FOOT_R : FOOT_L;
        return (rests & bits) != 0 ? 1 : 0;
    }

    // The same moment for a body stepping through with its left foot: left and right changed over.
    static Moment mirrored(Moment m) {
        float[] blades = m.blades();
        float[][] limbs = m.limbs();
        float[][] swapped = new float[4][];
        for (int l = 0; l < 4; l++) {
            float[] from = limbs[l ^ 1];
            swapped[l] = new float[] { from[0], -from[1], -from[2], from[3], from[4] };
        }
        int rests = m.rests();
        int flipped = 0;
        for (int bit = 0; bit < 10; bit += 2) {
            flipped |= (rests >> bit & 1) << bit + 1 | (rests >> bit + 1 & 1) << bit;
        }
        float[] lead = m.lead();
        return new Moment(m.u(), new float[] { lead[0], lead[1], lead[3], lead[2], lead[5], lead[4] }, -m.yaw(),
                m.pitch(), -m.roll(), -m.side(), m.ahead(), m.waist(), m.low(), m.nod(), -m.shake(),
                new float[] { blades[2], blades[3], blades[0], blades[1] }, swapped, flipped);
    }

}
