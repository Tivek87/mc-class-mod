package nl.tivek.multiversepowers.creature.animal.flamingo.client;

import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.animal.client.Channels;
import nl.tivek.multiversepowers.creature.animal.client.Gait;
import nl.tivek.multiversepowers.creature.animal.client.Strides;
import nl.tivek.multiversepowers.creature.animal.flamingo.Flamingo;
import nl.tivek.multiversepowers.creature.client.PoseSprings;
import nl.tivek.multiversepowers.creature.client.Trunk;

// How the flamingo moves, as the Caribbean flamingo was designed (flamingo_demo.html), every turn in the design's frame
// (y up, z forward; FlamingoModel turns them into the game's): standing it wades, both legs planted a little apart,
// breathing, its S-neck glancing about; it walks with the design's high-stepping stride, its neck bobbing, each foot
// kept where it was set down; and its acts: the one-leg stand (the other leg folded back in a figure four, the foot
// hanging), filter feeding (the neck plunged down, the bill upside down, sweeping from side to side and chattering),
// the wing salute (neck tall, head flagging, wings spread wide to flash their black, honking), preening the wing it has
// turned to, a honk with its head flagging, and a stretch of a leg and the wing on its side together. Beyond the
// design: a run with its wings beating to take off, flight with its neck straight out ahead and its legs trailing, and
// paddling afloat. The chick stands and bobs as the design's chick, fluttering its stubby wings. Every channel follows
// on springs, as the design eases each joint to its pose.
final class FlamingoPoses {
    // How far the body is raised from where it rests, and moved sideways; its turn; the tail's.
    static final int BODY_Y = 0;
    static final int BODY_X = 1;
    static final int BODY = 2;
    static final int TAIL = 5;
    // Each neck piece's turn (five, three each).
    static final int NECK = 8;
    static final int HEAD = 23;
    static final int JAW = 26;
    // Each wing's turn and its outer piece's turn about z.
    static final int LEFT_WING = 27;
    static final int RIGHT_WING = 31;
    // Each leg: the hip's turn, the joint halfway down about x and z, the foot about x.
    static final int LEFT_LEG = 35;
    static final int RIGHT_LEG = 41;
    static final int BLINK = 47;
    static final int LOOK = 48;
    private static final int COUNT = 49;
    // From the hip to the foot, legs straight, adult and chick.
    private static final float LEG = 14.6F;
    private static final float CHICK_LEG = 8.6F;
    private static final double[][] SPRINGS = Channels.springs(COUNT, 0.6, 0.6, 1.0);
    private final Channels pose = new Channels(COUNT);
    private final Channels act = new Channels(COUNT);
    private final Strides strides = new Strides();
    private final PoseSprings springs = new PoseSprings(SPRINGS[0], SPRINGS[1]);

    Channels of(Flamingo flamingo, float amount, float age, float partialTick, boolean chick) {
        Channels p = this.pose.clear();
        float[] c = p.v;
        int id = flamingo.getId();
        float t = age / 20.0F;
        if (chick) {
            chickIdle(c, t);
        } else {
            wade(c, t);
        }
        c[LOOK] = 1.0F;
        float spread = Mth.lerp(partialTick, flamingo.oSpread, flamingo.spread);
        float flap = Mth.lerp(partialTick, flamingo.oFlap, flamingo.flap);
        float fly = Gait.smooth(0.75F, 1.0F, spread);
        float swim = Mth.lerp(partialTick, flamingo.oSwim, flamingo.swim) * (1.0F - fly);
        float a = Mth.clamp(amount, 0.0F, 1.0F) * (1.0F - fly) * (1.0F - swim);
        float on = Gait.smooth(0.0F, 0.25F, a);
        if (on > 0.0F) {
            float run = Trunk.in(a, 0.6, 0.95);
            float reach = 1.0F + 0.35F * run;
            float sweep = 2.0F * (chick ? CHICK_LEG : LEG) * Mth.sin(0.42F * reach)
                    * flamingo.size();
            float phase = this.strides.phase(id, age, flamingo.level().getGameTime(), Strides.rate(a, 0.5F, sweep));
            // The design's stride run backwards in time, so a planted foot goes back under the body as it walks on.
            p.mix(this.with(c2 -> stride(c2, -phase, reach)), on);
        }
        float takeoff = Gait.smooth(0.0F, 0.75F, spread) * (1.0F - fly);
        if (takeoff > 0.0F) {
            p.mix(this.with(c2 -> takeoff(c2, flap)), takeoff);
        }
        if (fly > 0.0F) {
            p.mix(this.with(c2 -> flight(c2, flap)), fly);
        }
        if (swim > 0.0F) {
            p.mix(this.with(c2 -> swim(c2, t)), swim);
        }
        boolean flipped = id % 2 != 0;
        for (int kind = Flamingo.ONE_LEG; kind <= Flamingo.STRETCH; kind++) {
            float w = flamingo.blend(kind, partialTick) * (1.0F - spread);
            if (w > 0.0F) {
                float at = (float) flamingo.actAge(kind, partialTick) / 20.0F;
                int k = kind;
                Channels pose = this.with(c2 -> act(c2, k, at));
                if (flipped && kind != Flamingo.FEED && kind != Flamingo.DISPLAY && kind != Flamingo.HONK) {
                    mirror(pose.v);
                }
                p.mix(pose, w);
            }
        }
        c[BLINK] = Math.max(c[BLINK], Channels.blink(age, id));
        // The wingbeat, the jaw and the eyelids are quick enough as they are.
        float[] quick = { c[LEFT_WING + 2], c[LEFT_WING + 3], c[RIGHT_WING + 2], c[RIGHT_WING + 3], c[JAW],
                c[BLINK] };
        this.springs.follow(id, c, age, flamingo.level().getGameTime());
        c[LEFT_WING + 2] = quick[0];
        c[LEFT_WING + 3] = quick[1];
        c[RIGHT_WING + 2] = quick[2];
        c[RIGHT_WING + 3] = quick[3];
        c[JAW] = quick[4];
        c[BLINK] = quick[5];
        return p;
    }

    private interface Posing {
        void pose(float[] c);
    }

    // A whole pose of its own, from rest.
    private Channels with(Posing posing) {
        Channels p = this.act.clear();
        rest(p.v);
        posing.pose(p.v);
        return p;
    }

    private static void rest(float[] c) {
        set(c, TAIL, -0.38F, 0.0F, 0.0F);
        neck(c, 0, 0.82F, 0.0F, 0.0F);
        neck(c, 1, -0.58F, 0.0F, 0.0F);
        neck(c, 2, -0.38F, 0.0F, 0.0F);
        neck(c, 3, -0.12F, 0.0F, 0.0F);
        neck(c, 4, 0.52F, 0.0F, 0.0F);
        set(c, HEAD, -0.18F, 0.0F, 0.0F);
        c[LOOK] = 1.0F;
    }

    // Standing in the shallows on both legs, a little apart, breathing and glancing about.
    private static void wade(float[] c, float t) {
        rest(c);
        float breath = Mth.sin(t * 1.7F);
        float glance = Mth.sin(t * 1.1F);
        c[BODY_Y] = -0.15F + breath * 0.12F;
        set(c, BODY, 0.04F, glance * 0.04F, 0.0F);
        neck(c, 0, 0.92F + breath * 0.03F, 0.0F, 0.0F);
        neck(c, 1, -0.55F, 0.0F, 0.0F);
        neck(c, 2, -0.42F, glance * 0.08F, 0.0F);
        neck(c, 3, -0.18F, glance * 0.12F, 0.0F);
        neck(c, 4, 0.54F, glance * 0.14F, 0.0F);
        set(c, HEAD, -0.20F, glance * 0.16F, 0.0F);
        leg(c, LEFT_LEG, 0.14F, 0.0F, -0.18F, 0.04F);
        leg(c, RIGHT_LEG, -0.14F, 0.0F, -0.04F, 0.18F);
    }

    // The design's chick: bobbing, its neck swaying, its stubby wings fluttering.
    private static void chickIdle(float[] c, float t) {
        rest(c);
        float sway = Mth.sin(t * 2.2F);
        c[BODY_Y] = Mth.sin(t * 3.2F) * 0.12F;
        neck(c, 0, 0.55F, 0.0F, 0.0F);
        neck(c, 1, -0.35F, 0.0F, 0.0F);
        neck(c, 2, -0.22F, sway * 0.15F, 0.0F);
        neck(c, 3, -0.08F, sway * 0.15F, 0.0F);
        neck(c, 4, 0.35F, 0.0F, 0.0F);
        set(c, HEAD, -0.15F + Mth.sin(t * 3.8F) * 0.08F, sway * 0.2F, 0.0F);
        float flutter = Math.max(0.0F, Mth.sin(t * 6.5F)) * 0.35F;
        c[LEFT_WING + 2] = flutter;
        c[RIGHT_WING + 2] = -flutter;
    }

    // The high-stepping stride: each foot lifted high, its joint folding and its toes curling, the neck bobbing.
    private static void stride(float[] c, float w, float reach) {
        float sin = Mth.sin(w);
        float cos = Mth.cos(w);
        float bob = Mth.sin(w * 2.0F);
        c[BODY_Y] = -0.3F + Math.abs(sin) * 0.45F;
        set(c, BODY, 0.06F + bob * 0.02F, sin * 0.05F, cos * 0.03F);
        neck(c, 0, 0.88F + bob * 0.08F, 0.0F, 0.0F);
        neck(c, 1, -0.52F - bob * 0.06F, 0.0F, 0.0F);
        neck(c, 2, -0.40F - bob * 0.05F, 0.0F, 0.0F);
        neck(c, 3, -0.15F + bob * 0.06F, 0.0F, 0.0F);
        neck(c, 4, 0.52F + bob * 0.06F, 0.0F, 0.0F);
        set(c, HEAD, -0.24F - bob * 0.05F, 0.0F, 0.0F);
        float left = Math.max(0.0F, cos);
        float right = Math.max(0.0F, -cos);
        leg(c, LEFT_LEG, reach * (sin * 0.42F - left * 0.48F), 0.0F, left * 1.15F, left * 0.85F - sin * 0.25F);
        leg(c, RIGHT_LEG, reach * (-sin * 0.42F - right * 0.48F), 0.0F, right * 1.15F, right * 0.85F + sin * 0.25F);
    }

    // The run to take off: long strides, leaning in, the neck reaching forward, the wings half open and beating.
    private static void takeoff(float[] c, float flap) {
        stride(c, -flap * 0.6F, 1.3F);
        c[BODY + 0] += 0.15F;
        c[NECK] += 0.3F;
        c[NECK + 3] += 0.2F;
        float beat = 0.8F + 0.45F * Mth.sin(flap);
        c[LEFT_WING + 2] = beat;
        c[LEFT_WING + 3] = 0.3F + 0.25F * Mth.sin(flap - 0.9F);
        c[RIGHT_WING + 2] = -beat;
        c[RIGHT_WING + 3] = -c[LEFT_WING + 3];
    }

    // In flight: level, the neck straight out ahead, the legs trailing straight behind, the wings beating deep.
    private static void flight(float[] c, float flap) {
        c[BODY_Y] = -0.5F * Mth.cos(flap);
        set(c, BODY, -0.05F, 0.0F, 0.0F);
        set(c, TAIL, -0.1F, 0.0F, 0.0F);
        neck(c, 0, 1.4F, 0.0F, 0.0F);
        neck(c, 1, 0.1F, 0.0F, 0.0F);
        neck(c, 2, 0.05F, 0.0F, 0.0F);
        neck(c, 3, 0.0F, 0.0F, 0.0F);
        neck(c, 4, 0.0F, 0.0F, 0.0F);
        set(c, HEAD, -1.5F, 0.0F, 0.0F);
        float beat = 1.25F + 0.6F * Mth.sin(flap);
        float outer = 0.25F + 0.4F * Mth.sin(flap - 0.9F);
        set(c, LEFT_WING, 0.0F, 0.0F, beat);
        c[LEFT_WING + 3] = outer;
        set(c, RIGHT_WING, 0.0F, 0.0F, -beat);
        c[RIGHT_WING + 3] = -outer;
        leg(c, LEFT_LEG, 1.45F, 0.0F, 0.05F, 1.55F);
        leg(c, RIGHT_LEG, 1.45F, 0.0F, 0.05F, 1.55F);
        c[LOOK] = 0.0F;
    }

    // Afloat: the legs hang under it and paddle slowly, one after the other.
    private static void swim(float[] c, float t) {
        wade(c, t);
        float kick = Mth.sin(t * 3.0F);
        leg(c, LEFT_LEG, 0.5F + 0.35F * kick, 0.0F, 0.6F + 0.4F * Math.max(0.0F, kick), 0.6F);
        leg(c, RIGHT_LEG, 0.5F - 0.35F * kick, 0.0F, 0.6F + 0.4F * Math.max(0.0F, -kick), 0.6F);
        set(c, TAIL, -0.2F, 0.0F, 0.0F);
    }

    private static void act(float[] c, int kind, float t) {
        switch (kind) {
            case Flamingo.ONE_LEG -> oneLeg(c, t);
            case Flamingo.FEED -> feed(c, t);
            case Flamingo.PREEN -> preen(c, t);
            case Flamingo.DISPLAY -> salute(c, t);
            case Flamingo.HONK -> honk(c, t);
            case Flamingo.STRETCH -> stretch(c, t);
            default -> {
            }
        }
    }

    // Standing tall on one straight leg, the other bent sharply in a figure four, its foot hanging relaxed.
    private static void oneLeg(float[] c, float t) {
        float breath = Mth.sin(t * 1.9F);
        float look = Mth.sin(t * 0.85F);
        c[BODY_Y] = breath * 0.12F;
        set(c, BODY, 0.02F + breath * 0.015F, 0.0F, -0.02F);
        neck(c, 0, 0.86F + breath * 0.03F, 0.0F, 0.0F);
        neck(c, 1, -0.60F - breath * 0.02F, 0.0F, 0.0F);
        neck(c, 2, -0.38F, look * 0.06F, 0.0F);
        neck(c, 3, -0.14F, look * 0.08F, 0.0F);
        neck(c, 4, 0.56F, look * 0.10F, 0.0F);
        set(c, HEAD, -0.22F + breath * 0.02F, look * 0.12F, 0.0F);
        leg(c, RIGHT_LEG, 0.02F, -0.05F, -0.02F, 0.0F);
        c[RIGHT_LEG + 4] = 0.05F;
        leg(c, LEFT_LEG, -1.05F + breath * 0.03F, 0.04F, 1.68F + Mth.sin(t * 1.5F) * 0.04F, 1.22F);
    }

    // The neck swept down into the water, the bill upside down, sweeping from side to side, the jaw chattering.
    private static void feed(float[] c, float t) {
        float sweep = Mth.sin(t * 3.6F);
        float chatter = Math.abs(Mth.sin(t * 18.0F));
        c[BODY_Y] = -0.8F;
        set(c, BODY, 0.46F, sweep * 0.08F, 0.0F);
        set(c, TAIL, -0.15F, 0.0F, 0.0F);
        neck(c, 0, 1.28F, sweep * 0.14F, 0.0F);
        neck(c, 1, 0.42F, sweep * 0.14F, 0.0F);
        neck(c, 2, 0.35F, sweep * 0.12F, 0.0F);
        neck(c, 3, 0.28F, sweep * 0.10F, 0.0F);
        neck(c, 4, 0.22F, sweep * 0.08F, 0.0F);
        set(c, HEAD, 0.32F, sweep * 0.18F, 0.0F);
        c[JAW] = chatter * 0.26F;
        leg(c, LEFT_LEG, -0.42F, 0.05F, -0.04F, 0.46F);
        leg(c, RIGHT_LEG, -0.52F, -0.05F, 0.06F, 0.46F);
        c[LOOK] = 0.0F;
    }

    // The wing salute: the neck stretched tall, the head flagging from side to side, the wings spread wide and
    // flashing their black, the bill opening at each beat with a honk.
    private static void salute(float[] c, float t) {
        float flap = Mth.sin(t * 5.2F);
        float flag = Mth.sin(t * 4.5F);
        c[BODY_Y] = 0.3F + Math.abs(flap) * 0.25F;
        set(c, BODY, -0.22F, flag * 0.08F, 0.0F);
        set(c, TAIL, -0.52F, 0.0F, 0.0F);
        neck(c, 0, 0.52F, 0.0F, 0.0F);
        neck(c, 1, -0.35F, 0.0F, 0.0F);
        neck(c, 2, -0.12F, flag * 0.15F, 0.0F);
        neck(c, 3, 0.02F, flag * 0.22F, 0.0F);
        neck(c, 4, 0.25F, flag * 0.28F, 0.0F);
        set(c, HEAD, -0.15F, flag * 0.32F, 0.0F);
        c[JAW] = 0.18F + Math.max(0.0F, flap) * 0.15F;
        float spread = 1.28F + flap * 0.22F;
        set(c, LEFT_WING, 0.22F, 0.25F, spread);
        c[LEFT_WING + 3] = 0.35F + flap * 0.15F;
        set(c, RIGHT_WING, 0.22F, -0.25F, -spread);
        c[RIGHT_WING + 3] = -0.35F - flap * 0.15F;
        leg(c, LEFT_LEG, 0.20F, 0.04F, 0.02F, -0.22F);
        leg(c, RIGHT_LEG, 0.20F, -0.04F, 0.02F, -0.22F);
        c[LOOK] = 0.0F;
    }

    // The neck curved back over the left wing, nibbling and grooming it as it lifts and flutters.
    private static void preen(float[] c, float t) {
        float nibble = Mth.sin(t * 12.0F);
        float sway = Mth.sin(t * 2.4F);
        c[BODY_Y] = -0.1F;
        set(c, BODY, 0.05F, 0.08F, 0.0F);
        neck(c, 0, 0.65F, 0.28F, 0.0F);
        neck(c, 1, -0.45F, 0.42F, 0.0F);
        neck(c, 2, -0.52F, 0.55F, -0.15F);
        neck(c, 3, -0.48F + nibble * 0.06F, 0.45F, -0.18F);
        neck(c, 4, 0.42F + nibble * 0.08F, 0.35F, 0.0F);
        set(c, HEAD, 0.35F + nibble * 0.10F, 0.25F, 0.0F);
        c[JAW] = Math.abs(nibble) * 0.22F;
        set(c, LEFT_WING, 0.08F, 0.12F, 0.38F + Math.abs(nibble) * 0.08F);
        c[LEFT_WING + 3] = 0.18F + sway * 0.06F;
        leg(c, LEFT_LEG, 0.04F, 0.0F, -0.08F, 0.04F);
        leg(c, RIGHT_LEG, -0.08F, 0.0F, 0.04F, 0.04F);
        c[LOOK] = 0.0F;
    }

    // Three honks with the neck tall and the head flagging, as in the salute but with the wings folded.
    private static void honk(float[] c, float t) {
        float up = Trunk.in(t, 0.0, 0.3) * (1.0F - Trunk.in(t, Flamingo.HONK_END / 20.0 - 0.3,
                Flamingo.HONK_END / 20.0));
        float flap = Mth.sin(t * 5.2F);
        float flag = Mth.sin(t * 4.5F);
        wade(c, t);
        c[BODY_Y] += 0.2F * up;
        c[BODY] = Mth.lerp(up, c[BODY], -0.12F);
        neck(c, 0, Mth.lerp(up, c[NECK], 0.55F), 0.0F, 0.0F);
        neck(c, 1, Mth.lerp(up, c[NECK + 3], -0.35F), 0.0F, 0.0F);
        neck(c, 2, Mth.lerp(up, c[NECK + 6], -0.15F), flag * 0.12F * up, 0.0F);
        neck(c, 3, Mth.lerp(up, c[NECK + 9], 0.0F), flag * 0.18F * up, 0.0F);
        neck(c, 4, Mth.lerp(up, c[NECK + 12], 0.25F), flag * 0.24F * up, 0.0F);
        set(c, HEAD, -0.15F - 0.12F * Math.max(0.0F, flap) * up, flag * 0.3F * up, 0.0F);
        c[JAW] = up * (0.05F + Math.max(0.0F, flap) * 0.3F);
        c[LOOK] = 1.0F - up;
    }

    // The left leg stretched right back and the left wing spread down along it, the neck reaching forward.
    private static void stretch(float[] c, float t) {
        float on = Trunk.in(t, 0.0, 0.5) * (1.0F - Trunk.in(t, 1.6, 2.1));
        wade(c, t);
        c[BODY_X] = -0.9F * on;
        c[BODY] += 0.15F * on;
        c[BODY + 2] = -0.05F * on;
        neck(c, 0, Mth.lerp(on, c[NECK], 1.15F), 0.0F, 0.0F);
        neck(c, 1, Mth.lerp(on, c[NECK + 3], -0.3F), 0.0F, 0.0F);
        neck(c, 4, Mth.lerp(on, c[NECK + 12], 0.3F), 0.0F, 0.0F);
        leg(c, LEFT_LEG, 1.0F * on + 0.14F * (1.0F - on), 0.12F * on, 0.15F * on, 1.1F * on);
        leg(c, RIGHT_LEG, -0.05F * on, -0.05F * on, 0.0F, 0.0F);
        set(c, LEFT_WING, 0.35F * on, -0.15F * on, 0.75F * on);
        c[LEFT_WING + 3] = 0.45F * on;
    }

    // The same pose on the other side: left and right swapped, and every turn about y and z the other way.
    private static void mirror(float[] c) {
        c[BODY_X] = -c[BODY_X];
        for (int at : new int[] { BODY, TAIL, NECK, NECK + 3, NECK + 6, NECK + 9, NECK + 12, HEAD }) {
            c[at + 1] = -c[at + 1];
            c[at + 2] = -c[at + 2];
        }
        for (int k = 0; k < 4; k++) {
            float l = c[LEFT_WING + k];
            c[LEFT_WING + k] = c[RIGHT_WING + k];
            c[RIGHT_WING + k] = l;
        }
        c[LEFT_WING + 1] = -c[LEFT_WING + 1];
        c[LEFT_WING + 2] = -c[LEFT_WING + 2];
        c[LEFT_WING + 3] = -c[LEFT_WING + 3];
        c[RIGHT_WING + 1] = -c[RIGHT_WING + 1];
        c[RIGHT_WING + 2] = -c[RIGHT_WING + 2];
        c[RIGHT_WING + 3] = -c[RIGHT_WING + 3];
        for (int k = 0; k < 6; k++) {
            float l = c[LEFT_LEG + k];
            c[LEFT_LEG + k] = c[RIGHT_LEG + k];
            c[RIGHT_LEG + k] = l;
        }
        for (int leg : new int[] { LEFT_LEG, RIGHT_LEG }) {
            c[leg + 1] = -c[leg + 1];
            c[leg + 2] = -c[leg + 2];
            c[leg + 4] = -c[leg + 4];
        }
    }

    private static void set(float[] c, int at, float x, float y, float z) {
        c[at] = x;
        c[at + 1] = y;
        c[at + 2] = z;
    }

    private static void neck(float[] c, int piece, float x, float y, float z) {
        set(c, NECK + 3 * piece, x, y, z);
    }

    // A leg: the hip's turn about x and z, the joint's fold about x, the foot's.
    private static void leg(float[] c, int at, float hip, float out, float ankle, float foot) {
        c[at] = hip;
        c[at + 1] = 0.0F;
        c[at + 2] = out;
        c[at + 3] = ankle;
        c[at + 4] = 0.0F;
        c[at + 5] = foot;
    }
}
