package nl.tivek.multiversepowers.creature.animal.deer.client;

import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.animal.client.Channels;
import nl.tivek.multiversepowers.creature.animal.client.Gait;
import nl.tivek.multiversepowers.creature.animal.client.Strides;
import nl.tivek.multiversepowers.creature.animal.deer.Deer;
import nl.tivek.multiversepowers.creature.client.PoseSprings;
import nl.tivek.multiversepowers.creature.client.Trunk;

// How the deer moves: a light walk, a foot at a time a quarter of a stride apart, each hoof set down and kept where it
// was set while the body goes over it, the foreleg folding up at the knee and the hind leg at the hock as it lifts, the
// head nodding with each forefoot and the body leaning into its turns; galloping away when it runs, hind feet then
// forefeet, its body rocking, ears laid back and white tail raised; ears and tail flicking, eyes blinking and its jaw
// chewing the cud while it stands; and its acts: grazing with its muzzle in the grass, standing alert with its ears
// forward and stamping a forefoot, grooming its flank and lying down with its legs folded under it. The whole pose
// follows on springs, ears and tail slower and swinging past.
final class DeerPoses {
    static final int PITCH = 0;
    static final int ROLL = 1;
    static final int BOB = 2;
    static final int SINK = 3;
    static final int NECK = 4;
    static final int NECK_YAW = 5;
    static final int HEAD = 6;
    static final int HEAD_YAW = 7;
    static final int HEAD_ROLL = 8;
    static final int JAW = 9;
    // How far out each ear lies (right, then left), and how far both tip forward.
    static final int EAR_R = 10;
    static final int EAR_L = 11;
    static final int EARS_TIP = 12;
    static final int TAIL = 13;
    static final int TAIL_SIDE = 14;
    static final int LOOK = 15;
    static final int BLINK = 16;
    // Per leg (right front, left front, right hind, left hind): swing, outward, the joint below it (knee or stifle),
    // the hock (hind legs only), and the hoof.
    static final int LEGS = 17;
    static final int PER_LEG = 5;
    private static final int COUNT = LEGS + 4 * PER_LEG;
    private static final int RF = LEGS;
    private static final int LF = LEGS + PER_LEG;
    private static final int RH = LEGS + 2 * PER_LEG;
    private static final int LH = LEGS + 3 * PER_LEG;
    private static final int[] FEET = { RF, LF, RH, LH };
    // Where in a stride each foot is set down: walking left hind, left fore, right hind, right fore a quarter apart;
    // galloping the hind feet close together, then the forefeet half a stride on.
    private static final float[] WALK_AT = { 0.75F, 0.25F, 0.5F, 0.0F };
    private static final float[] RUN_AT = { 0.58F, 0.5F, 0.08F, 0.0F };
    private static final float WALK_SWEEP = 12.0F;
    private static final float RUN_SWEEP = 30.0F;
    private static final float LEG = 12.0F;
    private static final double[][] SPRINGS = Channels.springs(COUNT, 0.45, 0.18, 0.4, EAR_R, EAR_L, EARS_TIP, TAIL,
            TAIL_SIDE);
    private final Channels pose = new Channels(COUNT);
    private final Channels act = new Channels(COUNT);
    private final Strides strides = new Strides();
    private final PoseSprings springs = new PoseSprings(SPRINGS[0], SPRINGS[1]);

    Channels of(Deer deer, float limbSwing, float amount, float age, float partialTick) {
        Channels p = this.pose.clear();
        float[] c = p.v;
        c[LOOK] = 1.0F;
        float a = Mth.clamp(amount, 0.0F, 1.0F);
        float run = Trunk.in(a, 0.55, 0.85);
        float duty = Gait.duty(run, 0.62F, 0.35F);
        float phase = this.strides.phase(deer.getId(), age, deer.level().getGameTime(),
                Strides.rate(a, duty, Mth.lerp(run, WALK_SWEEP, RUN_SWEEP)));
        this.walk(c, phase, a, run, duty);
        int id = deer.getId();
        float turn = Mth.clamp(Mth.wrapDegrees(deer.yBodyRot - deer.yBodyRotO), -15.0F, 15.0F);
        c[ROLL] -= 0.01F * turn * a;
        c[NECK_YAW] += 0.025F * turn;
        c[BLINK] = Channels.blink(age, id);
        c[BOB] += 0.06F * Mth.sin(age * 0.1F);
        c[EAR_R] += 0.4F * Channels.twitch(age, id, 83, 4.0F);
        c[EAR_L] += 0.4F * Channels.twitch(age, id + 2, 109, 4.0F);
        c[EARS_TIP] -= 0.3F * Channels.twitch(age, id + 4, 151, 7.0F);
        c[TAIL_SIDE] += 0.35F * Channels.twitch(age, id + 6, 71, 6.0F) * Mth.sin(age * 1.5F);
        c[JAW] += 0.1F * Channels.twitch(age, id + 8, 420, 120.0F) * Math.max(0.0F, Mth.sin(age * 0.7F));
        for (int kind = Deer.GRAZE; kind <= Deer.BED; kind++) {
            float w = deer.blend(kind, partialTick);
            if (w > 0.0F) {
                p.mix(this.act(kind, (float) deer.actAge(kind, partialTick), id), w);
            }
        }
        float blink = c[BLINK];
        this.springs.follow(id, c, age, deer.level().getGameTime());
        c[BLINK] = blink;
        return p;
    }

    private void walk(float[] c, float phase, float a, float run, float duty) {
        float walk = a * (1.0F - run);
        float on = Gait.smooth(0.0F, 0.3F, a);
        float[] load = new float[4];
        float[] drop = new float[4];
        for (int k = 0; k < 4; k++) {
            int leg = FEET[k];
            float p = phase - Mth.TWO_PI * Mth.lerp(run, WALK_AT[k], RUN_AT[k]);
            float fold = on * Gait.fold(p, duty);
            float toe = on * Gait.toe(p, duty);
            if (k < 2) {
                float swing = on * Mth.lerp(run, 0.36F, 0.85F) * Gait.swing(p, duty);
                float knee = Mth.lerp(run, 1.4F, 1.9F) * fold;
                c[leg] = swing;
                c[leg + 2] = knee;
                // The hoof kept flat under the leg's lean, flicked back as it lifts.
                c[leg + 4] = 0.7F * toe - swing - knee;
                drop[k] = LEG * (1.0F - Mth.cos(swing));
            } else {
                float swing = on * Mth.lerp(run, 0.32F, 0.8F) * Gait.swing(p, duty);
                float stifle = Mth.lerp(run, 0.5F, 0.8F) * fold;
                float hock = -Mth.lerp(run, 0.95F, 1.4F) * fold;
                c[leg] = swing;
                c[leg + 2] = stifle;
                c[leg + 3] = hock;
                c[leg + 4] = 0.6F * toe - swing - stifle - hock;
                drop[k] = LEG * (1.0F - Mth.cos(swing));
            }
            load[k] = Gait.load(p, duty);
        }
        // The withers and the croup each as high as the legs under them stand, the body pitching between them.
        float fore = (load[0] * drop[0] + load[1] * drop[1]) / Math.max(0.3F, load[0] + load[1]);
        float hind = (load[2] * drop[2] + load[3] * drop[3]) / Math.max(0.3F, load[2] + load[3]);
        float flight = 1.0F - Math.min(1.0F, load[0] + load[1] + load[2] + load[3]);
        c[BOB] = 0.5F * (fore + hind) * (1.0F - run) - 1.6F * run * flight;
        c[PITCH] = (fore - hind) / 13.0F * (1.0F - run) + 0.14F * run * Mth.sin(phase - Mth.TWO_PI * 0.3F);
        c[ROLL] = 0.05F * walk * (load[0] - load[1]);
        // The head nodding down as each forefoot takes the weight, held level against the gallop's rocking.
        c[NECK] = 0.1F * walk * (load[0] + load[1] - 1.0F) + 0.35F * run;
        c[HEAD] = -0.15F * run - 0.5F * c[PITCH] * run;
        c[TAIL] = -2.0F * run;
        c[EARS_TIP] = -0.5F * run;
        c[EAR_R] = c[EAR_L] = -0.2F * run;
    }

    private Channels act(int kind, float t, int id) {
        Channels p = this.act.copy(this.pose);
        float[] c = p.v;
        switch (kind) {
            case Deer.GRAZE -> {
                // Now and then the head comes up to look round, still chewing.
                float up = Channels.twitch(t, id, 90, 32.0F);
                c[PITCH] = 0.1F * (1.0F - up);
                c[NECK] = 1.75F - 1.35F * up;
                c[NECK_YAW] = 0.15F * Mth.sin(t * 0.05F);
                c[HEAD] = 1.0F - 0.85F * up;
                c[JAW] = 0.18F * Math.max(0.0F, Mth.sin(t * 0.9F));
                stand(c, RF, -0.12F, 0.06F, 0.0F, 0.0F, 0.0F);
                stand(c, LF, -0.12F, 0.06F, 0.0F, 0.0F, 0.0F);
                c[LOOK] = 0.8F * up;
            }
            case Deer.ALERT -> {
                float lift = Trunk.in(t, 6.0, Deer.STAMP - 2.0) * (1.0F - Trunk.in(t, Deer.STAMP - 2.0, Deer.STAMP));
                c[NECK] = -0.4F;
                c[HEAD] = -0.1F;
                c[EAR_R] = c[EAR_L] = -0.55F;
                c[EARS_TIP] = 0.35F;
                c[TAIL] = -0.9F;
                c[BOB] -= 0.2F;
                stand(c, RF, -0.6F * lift, 0.0F, 1.2F * lift, 0.0F, 0.3F * lift);
                c[LOOK] = 1.0F;
            }
            case Deer.GROOM -> {
                float side = id % 2 == 0 ? 1.0F : -1.0F;
                c[NECK] = 0.9F;
                c[NECK_YAW] = side * 1.25F;
                c[HEAD_YAW] = side * 0.8F;
                c[HEAD] = 0.7F + 0.12F * Mth.sin(t * 1.6F);
                c[HEAD_ROLL] = side * 0.3F;
                c[EARS_TIP] = -0.3F;
                c[LOOK] = 0.0F;
            }
            case Deer.BED -> {
                c[SINK] = 8.0F;
                c[PITCH] = c[ROLL] = 0.0F;
                c[BOB] = 0.12F * Mth.sin(t * 0.07F);
                // Forelegs folded at the knee under the chest; hind legs folded beside the body.
                stand(c, RF, -1.45F, 0.05F, 2.8F, 0.0F, 0.4F);
                stand(c, LF, -1.45F, 0.05F, 2.8F, 0.0F, 0.4F);
                stand(c, RH, -1.25F, 0.45F, 2.2F, -2.45F, 0.3F);
                stand(c, LH, -1.25F, 0.45F, 2.2F, -2.45F, 0.3F);
                c[NECK] = -0.15F;
                c[HEAD] = 0.05F;
                c[JAW] = 0.12F * Math.max(0.0F, Mth.sin(t * 0.6F));
                c[EAR_R] += 0.15F;
                c[EAR_L] += 0.15F;
                c[TAIL] = 0.0F;
                c[LOOK] = 0.7F;
                c[BLINK] = Math.max(c[BLINK], 0.35F);
            }
            default -> {
            }
        }
        return p;
    }

    private static void stand(float[] c, int leg, float swing, float out, float knee, float hock, float hoof) {
        c[leg] = swing;
        c[leg + 1] = out;
        c[leg + 2] = knee;
        c[leg + 3] = hock;
        c[leg + 4] = hoof;
    }
}
