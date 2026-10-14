package nl.tivek.multiversepowers.creature.animal.duck.client;

import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.animal.client.Channels;
import nl.tivek.multiversepowers.creature.animal.client.Gait;
import nl.tivek.multiversepowers.creature.animal.client.Strides;
import nl.tivek.multiversepowers.creature.animal.duck.Duck;
import nl.tivek.multiversepowers.creature.client.PoseSprings;
import nl.tivek.multiversepowers.creature.client.Trunk;

// How the duck moves: a waddle on land, each foot kept where it was set down, the body rolling from foot to foot, the
// head bobbing and the tail wagging against the roll, its eyes blinking; afloat it
// lies still on the water with its feet paddling under it; in flight its neck is stretched out ahead, its feet trail
// and its wings beat fast; and its acts: tipped up to dabble with its tail in the air and its feet kicking, preening
// a wing, rearing up to beat its wings where it stands, quacking with its head thrown forward and its bill open at
// each quack, and asleep with its head turned back onto its shoulders, eyes shut. The pose follows on springs.
final class DuckPoses {
    static final int PITCH = 0;
    static final int ROLL = 1;
    static final int BOB = 2;
    static final int SINK = 3;
    static final int NECK = 4;
    static final int HEAD = 5;
    static final int HEAD_YAW = 6;
    static final int HEAD_ROLL = 7;
    static final int TAIL = 8;
    static final int TAIL_WAG = 9;
    // How far its wings are out (0 folded, 1 in flight), their beat and the outer wing's own bend.
    static final int SPREAD = 10;
    static final int BEAT = 11;
    static final int BEAT_OUTER = 12;
    // A folded wing lifted off the body (right, then left).
    static final int WING_R = 13;
    static final int WING_L = 14;
    static final int LEG_R = 15;
    static final int LEG_L = 16;
    // A foot's own slope: 0 flat on the ground.
    static final int FOOT_R = 17;
    static final int FOOT_L = 18;
    static final int LOOK = 19;
    static final int BLINK = 20;
    // How far the lower bill is open.
    static final int BILL = 21;
    private static final int COUNT = 22;
    // How far a foot travels across the ground in a stride, in pixels.
    private static final float SWEEP = 2.4F;
    private static final double[][] SPRINGS = Channels.springs(COUNT, 0.9, 0.3, 0.45, TAIL_WAG, TAIL);
    private final Channels pose = new Channels(COUNT);
    private final Channels act = new Channels(COUNT);
    private final Strides strides = new Strides();
    private final PoseSprings springs = new PoseSprings(SPRINGS[0], SPRINGS[1]);
    private float swim;

    Channels of(Duck duck, float limbSwing, float amount, float age, float partialTick) {
        Channels p = this.pose.clear();
        float[] c = p.v;
        float spread = Mth.lerp(partialTick, duck.oSpread, duck.spread);
        float flap = Mth.lerp(partialTick, duck.oFlap, duck.flap);
        this.swim = Mth.lerp(partialTick, duck.oSwim, duck.swim) * (1.0F - spread);
        float walk = Mth.clamp(amount, 0.0F, 1.0F) * (1.0F - spread) * (1.0F - this.swim);
        int id = duck.getId();
        c[LOOK] = 1.0F;
        c[SPREAD] = spread;
        // In flight: nose a little up, neck out ahead, feet trailing.
        c[PITCH] = -0.12F * spread;
        c[NECK] = 1.25F * spread;
        c[BEAT] = 0.75F * Mth.sin(flap);
        c[BEAT_OUTER] = 0.45F * Mth.sin(flap - 0.8F);
        c[BOB] = -0.5F * spread * Mth.cos(flap);
        c[LEG_R] = c[LEG_L] = 1.35F * spread;
        // A foot told against the ground: a quarter turn past its leg, it trails in line with it.
        c[FOOT_R] = c[FOOT_L] = 2.85F * spread;
        // The waddle: each foot set down and kept where it was while the body goes over it, its toes curling as it
        // lifts; the body rolls onto the foot that carries it and the head bobs once a step.
        float duty = 0.62F;
        float phase = this.strides.phase(id, age, duck.level().getGameTime(), Strides.rate(walk, duty, SWEEP));
        float on = Gait.smooth(0.0F, 0.25F, walk);
        float[] load = new float[2];
        for (int s = 0; s < 2; s++) {
            float at = phase - Mth.PI * s;
            float swing = on * 0.6F * Gait.swing(at, duty);
            c[LEG_R + s] += swing;
            c[FOOT_R + s] += on * 1.1F * Gait.toe(at, duty) + 0.5F * on * Gait.lift(at, duty);
            load[s] = Gait.load(at, duty);
        }
        c[ROLL] = 0.2F * on * (load[1] - load[0]);
        c[BOB] += 0.3F * on * Math.abs(Mth.sin(phase));
        c[TAIL_WAG] = -1.5F * c[ROLL];
        c[NECK] += 0.12F * on * Mth.cos(2.0F * phase);
        c[HEAD] = -0.1F * on * Mth.cos(2.0F * phase);
        c[BLINK] = Channels.blink(age, id);
        // Afloat its feet trail and paddle, one after the other, faster as it swims on.
        float kick = age * 0.3F + limbSwing * 1.5F;
        float right = this.swim * (0.6F + 0.55F * Mth.sin(kick));
        float left = this.swim * (0.6F - 0.55F * Mth.sin(kick));
        c[LEG_R] += right;
        c[FOOT_R] += 2.0F * right;
        c[LEG_L] += left;
        c[FOOT_L] += 2.0F * left;
        c[BOB] += 0.25F * this.swim * Mth.sin(age * 0.09F);
        c[ROLL] += 0.04F * this.swim * Mth.sin(age * 0.06F);
        c[TAIL_WAG] += 0.5F * Channels.twitch(age, id, 140, 14.0F) * Mth.sin(age * 1.6F) * (1.0F - spread);
        c[HEAD_ROLL] = 0.3F * Channels.twitch(age, id + 2, 170, 30.0F) * (1.0F - spread);
        for (int kind = Duck.DABBLE; kind <= Duck.TUCK; kind++) {
            float w = duck.blend(kind, partialTick) * (1.0F - spread);
            if (w > 0.0F) {
                p.mix(this.act(kind, (float) duck.actAge(kind, partialTick), id), w);
            }
        }
        // The wingbeat, the bill and the eyes are quick enough as they are.
        float beat = c[BEAT];
        float outer = c[BEAT_OUTER];
        float bill = c[BILL];
        float blink = c[BLINK];
        this.springs.follow(id, c, age, duck.level().getGameTime());
        c[BEAT] = beat;
        c[BEAT_OUTER] = outer;
        c[BILL] = bill;
        c[BLINK] = blink;
        return p;
    }

    private Channels act(int kind, float t, int id) {
        Channels p = this.act.copy(this.pose);
        float[] c = p.v;
        float side = id % 2 == 0 ? 1.0F : -1.0F;
        c[HEAD_ROLL] = 0.0F;
        switch (kind) {
            case Duck.DABBLE -> {
                // Tipped right over: head and breast under, tail straight up, feet kicking to hold it there.
                float kick = Mth.sin(t * 0.9F);
                c[PITCH] = 1.25F + 0.05F * Mth.sin(t * 0.25F);
                c[SINK] = 1.5F;
                c[BOB] = 0.3F * Mth.sin(t * 0.25F);
                c[NECK] = 0.3F;
                c[HEAD] = 1.3F;
                c[TAIL] = 0.2F;
                c[TAIL_WAG] = 0.4F * Mth.sin(t * 0.8F);
                c[LEG_R] = 1.0F + 0.5F * kick;
                c[FOOT_R] = c[LEG_R] + 1.2F;
                c[LEG_L] = 1.0F - 0.5F * kick;
                c[FOOT_L] = c[LEG_L] + 1.2F;
                c[LOOK] = 0.0F;
            }
            case Duck.PREEN -> {
                float nibble = Mth.sin(t * 1.3F);
                c[HEAD_YAW] = side * 2.3F;
                c[NECK] = -0.15F;
                c[HEAD] = 0.5F + 0.1F * nibble;
                c[HEAD_ROLL] = side * 0.2F;
                // The wing its head has turned to lifts off its side.
                c[side > 0.0F ? WING_R : WING_L] = 0.4F + 0.06F * nibble;
                c[LOOK] = 0.0F;
            }
            case Duck.STRETCH -> {
                // Reared up on the water or its feet, wings out and beaten hard a few times.
                float on = Trunk.in(t, 0.0, 5.0);
                c[PITCH] = -0.6F * on;
                c[SINK] = -0.8F * on;
                c[SPREAD] = on;
                c[BEAT] = 0.7F * Mth.sin(t * 1.5F);
                c[BEAT_OUTER] = 0.45F * Mth.sin(t * 1.5F - 0.8F);
                c[NECK] = -0.1F;
                c[HEAD] = -0.15F;
                c[TAIL_WAG] = 0.3F * Mth.sin(t * 1.5F);
                c[LOOK] = 0.4F;
            }
            case Duck.QUACK -> {
                // Four quacks, each a little lower than the last: the head thrown forward and the bill up.
                float quack = note(t, 2.0) + 0.9F * note(t, 8.0) + 0.8F * note(t, 14.0) + 0.7F * note(t, 20.0);
                c[NECK] = 0.3F * quack;
                c[HEAD] = -0.35F * quack;
                c[BILL] = 0.55F * quack;
                c[BOB] = 0.3F * quack;
                c[TAIL_WAG] = 0.25F * Mth.sin(t * 1.4F);
                c[LOOK] = 0.7F;
            }
            case Duck.TUCK -> {
                // Asleep: settled on its belly, the head turned right back and laid between its shoulders.
                c[SINK] = 2.0F * (1.0F - this.swim);
                c[BOB] = 0.12F * Mth.sin(t * 0.07F);
                c[HEAD_YAW] = side * 2.8F;
                c[NECK] = -0.35F;
                c[HEAD] = 0.35F;
                c[LEG_R] = c[LEG_L] = -1.4F * (1.0F - this.swim);
                c[FOOT_R] = c[FOOT_L] = 0.0F;
                c[TAIL_WAG] = 0.0F;
                c[BLINK] = 1.0F;
                c[LOOK] = 0.0F;
            }
            default -> {
            }
        }
        return p;
    }

    // One quack starting at `from`: quick out, slower back.
    private static float note(float t, double from) {
        return Trunk.in(t, from, from + 1.5) * (1.0F - Trunk.in(t, from + 2.0, from + 5.0));
    }
}
