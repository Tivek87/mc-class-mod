package nl.tivek.multiversepowers.character.thor.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import nl.tivek.multiversepowers.character.thor.ThorStatePayload;
import nl.tivek.multiversepowers.engine.client.pose.Stance;
import nl.tivek.multiversepowers.engine.client.render.EntityPass;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.spell.client.ClientClaps;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// Thor's body as he moves, built on the game's own walk: the hips sink and the trunk leans and bends at the waist
// (Stance), the feet are set where the move wants them and the knees bend to reach, the hands likewise with the
// elbows. Every part is blended in by how far into its move he is, and what ThorBody carries over (a landing, flight,
// a float) keeps it moving with momentum. Model space, in pixels: y down, -z ahead, +x his left.
public final class ThorPoses {
    private static final Vector3f KNEE = new Vector3f(0.0F, 0.0F, -1.0F);
    private static final Vector3f RIGHT_ELBOW = new Vector3f(-0.5F, 0.2F, 1.0F);
    private static final Vector3f LEFT_ELBOW = new Vector3f(0.5F, 0.2F, 1.0F);
    // Where the axe hangs from his belt, on his left hip, in the chest's frame.
    static final Vector3f BELT = new Vector3f(5.6F, 11.0F, 0.6F);
    private static final float JUMP_WAIT = 2.0F;

    private static final Quaternionf LEAN = new Quaternionf();
    private static final Quaternionf WAIST = new Quaternionf();
    private static final Quaternionf CHEST = new Quaternionf();
    private static final Quaternionf AIM = new Quaternionf();
    private static final Vector3f NECK = new Vector3f();
    private static final Vector3f HIPS = new Vector3f();

    private ThorPoses() {
    }

    // What the pose of this frame is made of, summed over every move that is on.
    static final class Mix {
        float drop;
        float pitch;
        float waist;
        float roll;
        float twist;
        final Vector3f[] feet = { new Vector3f(), new Vector3f() };
        final float[] footWeight = new float[2];
        boolean airborne;
        final Vector3f[] hands = { new Vector3f(), new Vector3f() };
        final float[] handWeight = new float[2];
        final boolean[] rootHands = new boolean[2];
        // A hand aimed ahead of him, not of his chest: it leans with the chest but does not turn with it.
        final boolean[] aimedHands = new boolean[2];
        // The way an elbow bends out, where a move says (its own weight, over the arm's usual way).
        final Vector3f[] poles = { new Vector3f(), new Vector3f() };
        final float[] poleWeight = new float[2];
        float weight;

        void reset() {
            this.drop = 0.0F;
            this.pitch = 0.0F;
            this.waist = 0.0F;
            this.roll = 0.0F;
            this.twist = 0.0F;
            this.footWeight[0] = this.footWeight[1] = 0.0F;
            this.handWeight[0] = this.handWeight[1] = 0.0F;
            this.poleWeight[0] = this.poleWeight[1] = 0.0F;
            this.rootHands[0] = this.rootHands[1] = false;
            this.aimedHands[0] = this.aimedHands[1] = false;
            this.airborne = false;
            this.weight = 0.0F;
        }

        // A hand target (in the chest's frame unless `root`), blended over what is there by `w`.
        void hand(int side, float x, float y, float z, float w, boolean root) {
            if (w <= 0.0F) {
                return;
            }
            float had = this.handWeight[side];
            float total = had + w;
            this.hands[side].lerp(new Vector3f(x, y, z), w / total);
            this.handWeight[side] = Math.min(1.0F, total);
            this.rootHands[side] |= root;
        }

        void foot(int side, float x, float y, float z, float w) {
            if (w <= 0.0F) {
                return;
            }
            float had = this.footWeight[side];
            float total = had + w;
            this.feet[side].lerp(new Vector3f(x, y, z), w / total);
            this.footWeight[side] = Math.min(1.0F, total);
        }

        void pole(int side, Vector3f way, float w) {
            if (w <= 0.0F) {
                return;
            }
            float total = this.poleWeight[side] + w;
            this.poles[side].lerp(way, w / total);
            this.poleWeight[side] = Math.min(1.0F, total);
        }
    }

    private static final Mix MIX = new Mix();

    public static boolean pose(PlayerModel<?> model, LivingEntity entity) {
        if (!EntityPass.inWorld()) {
            return false;
        }
        ClientThor.View view = ClientThor.view(entity);
        ThorBody body = ThorBody.of(entity);
        if (view == null || body == null) {
            return false;
        }
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        float age = view.age(partialTick);
        Mix mix = MIX;
        mix.reset();
        dash(mix, view, body, age);
        jump(mix, view, body, age, entity);
        floating(mix, model, body, entity);
        flight(mix, view, body, age, entity);
        slam(mix, view, age);
        blows(mix, view, entity, partialTick);
        float absorb = (float) Math.max(0.0, body.absorb.value);
        mix.drop += absorb;
        mix.pitch += absorb * 0.05F;
        mix.weight = Math.max(mix.weight, Math.min(1.0F, absorb * 2.0F));
        if (mix.weight < 1.0E-3F && body.axe.value < 1.0E-3) {
            return false;
        }
        apply(model, mix, body);
        return true;
    }

    // Sets the hips, trunk, feet and hands the mix asks for, over the game's own pose.
    private static void apply(PlayerModel<?> model, Mix mix, ThorBody body) {
        Vector3f[] ownFeet = { Stance.foot(model, true, new Vector3f()), Stance.foot(model, false, new Vector3f()) };
        HIPS.set(0.0F, Stance.HIP_Y + mix.drop, 0.0F);
        LEAN.rotationZYX(mix.roll * 0.6F, mix.twist * 0.4F, mix.pitch);
        WAIST.rotationZYX(mix.roll * 0.4F, mix.twist * 0.6F, mix.waist);
        Stance.trunk(model, HIPS, LEAN, WAIST);
        for (int side = 0; side < 2; side++) {
            boolean right = side == 0;
            Vector3f foot = new Vector3f(ownFeet[side]);
            if (mix.airborne) {
                // In the air the feet hang from the hips where the game's own pose has them.
                foot.y += mix.drop;
            }
            foot.lerp(mix.feet[side], mix.footWeight[side]);
            Stance.leg(model, right, foot, KNEE);
        }
        Stance.neck(NECK);
        Stance.chest(CHEST);
        for (int side = 0; side < 2; side++) {
            boolean right = side == 0;
            Vector3f hand = Stance.hand(model, right, new Vector3f());
            float axe = right ? 0.0F : (float) body.axe.value;
            float w = mix.handWeight[side];
            if (w <= 0.0F && axe <= 0.0F) {
                continue;
            }
            Vector3f target = new Vector3f(mix.hands[side]);
            if (mix.aimedHands[side]) {
                AIM.rotationX(mix.pitch + mix.waist).transform(target).add(NECK);
            } else if (!mix.rootHands[side]) {
                CHEST.transform(target).add(NECK);
            }
            hand.lerp(target, w);
            Vector3f elbow = new Vector3f(right ? RIGHT_ELBOW : LEFT_ELBOW).lerp(mix.poles[side],
                    mix.poleWeight[side]);
            Stance.arm(model, right, hand, elbow);
        }
    }

    // His blows and the guard he keeps between them (ThorBlowPoses): hands, trunk and a kicking foot, over his walk.
    // A thunderclap has his arms meanwhile.
    private static void blows(Mix mix, ClientThor.View view, LivingEntity entity, float partialTick) {
        if (ClientClaps.active(entity) || view.has(ThorStatePayload.FLYING)) {
            return;
        }
        ThorBlowPoses.Pose pose = ThorBlowPoses.of(view, partialTick);
        if (pose == null || pose.weight < 1.0E-3F) {
            return;
        }
        float w = pose.weight;
        mix.weight = Math.max(mix.weight, w);
        mix.drop += pose.drop * w;
        mix.pitch += pose.pitch * w;
        mix.twist += pose.twist * w;
        mix.roll += pose.roll * w;
        for (int side = 0; side < 2; side++) {
            Vector3f hand = pose.hand[side];
            // Aimed ahead only where the blow has most of the hand, not while its guard fades under another move.
            mix.aimedHands[side] = w >= mix.handWeight[side];
            mix.hand(side, hand.x, hand.y, hand.z, w, false);
            mix.pole(side, pose.pole[side], w);
        }
        if (pose.footSide >= 0 && pose.kick > 1.0E-3F) {
            float kick = pose.kick * w;
            mix.foot(pose.footSide, pose.foot.x, pose.foot.y, pose.foot.z, kick);
            int planted = 1 - pose.footSide;
            mix.foot(planted, planted == 0 ? -Stance.HIP_X : Stance.HIP_X, Stance.GROUND, 0.5F, kick);
        }
    }

    // The dash, turned the way he dashes: `lead` is the leg on the side he goes (the right going ahead or back).
    private static void dash(Mix mix, ClientThor.View view, ThorBody body, float age) {
        if (view.move() != ThorStatePayload.DASH || age >= ThorKeys.end(ThorKeys.DASH)) {
            return;
        }
        float[] k = ThorKeys.at(ThorKeys.DASH, age);
        float w = k[ThorKeys.WEIGHT];
        double angle = (view.arg() & 0xFF) / 256.0 * Math.PI * 2.0;
        double wx = -Math.sin(angle);
        double wz = Math.cos(angle);
        double yaw = Math.toRadians(body.yaw);
        // Into the model's frame: +x his left, -z ahead.
        float x = (float) (wx * Math.cos(yaw) + wz * Math.sin(yaw));
        float z = (float) -(-wx * Math.sin(yaw) + wz * Math.cos(yaw));
        float ahead = -z;
        float side = x;
        float forward = Math.max(0.0F, ahead);
        float back = Math.max(0.0F, -ahead);
        mix.weight = Math.max(mix.weight, w);
        mix.drop += k[ThorKeys.DROP] * w;
        mix.pitch += (k[ThorKeys.PITCH] * (forward - 0.45F * back) + 0.08F * Math.abs(side)) * w;
        mix.waist += k[ThorKeys.WAIST] * (forward - 0.3F * back) * w;
        mix.roll += k[ThorKeys.ROLL] * side * w;
        int lead = Math.abs(side) > Math.abs(ahead) ? side > 0.0F ? 1 : 0 : 0;
        for (int s = 0; s < 2; s++) {
            boolean leads = s == lead;
            float hipX = s == 0 ? -Stance.HIP_X : Stance.HIP_X;
            float reach = leads ? k[ThorKeys.LEAD] : -k[ThorKeys.TRAIL];
            float lift = leads ? k[ThorKeys.LEAD_LIFT] : k[ThorKeys.TRAIL_LIFT];
            mix.foot(s, hipX + x * reach, Stance.GROUND - lift, z * reach, w);
        }
        float arms = k[ThorKeys.ARMS] * w;
        float across = Math.abs(side);
        float sum = Math.max(1.0E-3F, forward + back + across);
        for (int s = 0; s < 2; s++) {
            float sign = s == 0 ? -1.0F : 1.0F;
            boolean outside = side * sign > 0.0F;
            // Driving back behind the hips going ahead, reaching forward going back, out and across going aside.
            float hx = (sign * 6.0F * forward + sign * 5.0F * back
                    + (outside ? sign * 11.0F : -sign * 0.5F) * across) / sum;
            float hy = (11.0F * forward + 7.0F * back + (outside ? 8.0F : 7.0F) * across) / sum;
            float hz = ((s == 0 ? 5.0F : 3.5F) * forward - 6.0F * back + (outside ? 1.0F : -5.0F) * across) / sum;
            mix.hand(s, hx, hy, hz, arms, false);
        }
    }

    private static void jump(Mix mix, ClientThor.View view, ThorBody body, float age, LivingEntity entity) {
        if (view.move() != ThorStatePayload.JUMP) {
            return;
        }
        float t = age - JUMP_WAIT;
        if (t >= ThorKeys.end(ThorKeys.JUMP) && (entity.onGround() || view.has(ThorStatePayload.FLOATING))) {
            return;
        }
        float[] k = ThorKeys.at(ThorKeys.JUMP, t);
        float fade = view.has(ThorStatePayload.FLOATING) ? 1.0F - (float) body.floating.value : 1.0F;
        float w = k[ThorKeys.J_WEIGHT] * fade;
        mix.weight = Math.max(mix.weight, w);
        mix.airborne = t >= 0.0F;
        mix.drop += k[ThorKeys.J_DROP] * w;
        mix.pitch += k[ThorKeys.J_PITCH] * w;
        float tuck = k[ThorKeys.J_TUCK] * w;
        for (int s = 0; s < 2; s++) {
            float sign = s == 0 ? -1.0F : 1.0F;
            float hipX = sign * Stance.HIP_X;
            if (mix.airborne) {
                // Knees drawn up in the rise, one a little higher than the other.
                float up = tuck * (s == 0 ? 5.5F : 4.0F);
                mix.foot(s, hipX, Stance.HIP_Y + mix.drop + 12.0F - up, 1.5F * tuck, w);
            }
            float upArm = k[ThorKeys.J_ARMS_UP];
            float backArm = k[ThorKeys.J_ARMS_BACK];
            float sum = Math.max(1.0E-3F, upArm + backArm);
            mix.hand(s, sign * (7.0F * upArm + 6.0F * backArm) / sum, (-5.0F * upArm + 12.0F * backArm) / sum,
                    (-4.0F * upArm + 5.0F * backArm) / sum, w * Math.min(1.0F, sum), false);
        }
    }

    // Hanging high at the top of a super jump: arms out balancing on the wind, legs loose, and the trunk turning with
    // where he aims.
    private static void floating(Mix mix, PlayerModel<?> model, ThorBody body, LivingEntity entity) {
        float w = (float) body.floating.value * (1.0F - (float) body.fly.value);
        if (w < 1.0E-3F) {
            return;
        }
        float time = body.time;
        mix.weight = Math.max(mix.weight, w);
        mix.airborne = true;
        mix.pitch += (Mth.clamp(model.head.xRot, -0.8F, 0.9F) * 0.35F + 0.05F) * w;
        mix.twist += Mth.clamp(model.head.yRot, -1.0F, 1.0F) * 0.5F * w;
        for (int s = 0; s < 2; s++) {
            float sign = s == 0 ? -1.0F : 1.0F;
            float sway = (float) (Noise.smooth(entity.getId() * 7 + s, time * 0.04) - 0.5) * 2.0F;
            mix.hand(s, sign * (9.5F + sway), 7.5F + sway * 1.5F, 1.0F - sway, w, false);
            float knee = s == 0 ? 3.5F : 1.5F;
            mix.foot(s, sign * Stance.HIP_X, Stance.HIP_Y + mix.drop + 12.0F - knee + sway * 0.6F,
                    1.2F + sway * 0.5F, w);
        }
    }

    private static void flight(Mix mix, ClientThor.View view, ThorBody body, float age, LivingEntity entity) {
        float fly = (float) body.fly.value;
        float axe = (float) body.axe.value;
        boolean takeOff = view.move() == ThorStatePayload.TAKE_OFF && age < 12.0F;
        boolean touchDown = (view.move() == ThorStatePayload.TOUCH_DOWN || view.move() == ThorStatePayload.SLAM)
                && age < ThorBody.SHEATHE + 6.0F;
        if (fly < 1.0E-3F && !takeOff && !touchDown && axe < 1.0E-3F) {
            return;
        }
        float time = body.time;
        float flat = body.flat();
        boolean fast = ThorMotion.lightning() && entity == Minecraft.getInstance().player
                || view.has(ThorStatePayload.LIGHTNING);
        mix.weight = Math.max(mix.weight, Math.max(fly, axe));
        if (fly > 1.0E-3F) {
            mix.airborne = true;
            float sway = (float) (Noise.smooth(entity.getId() * 5, time * 0.05) - 0.5);
            // Legs trail together behind him in fast flight, hang loose and a little bent in a hover.
            for (int s = 0; s < 2; s++) {
                float sign = s == 0 ? -1.0F : 1.0F;
                float bend = Mth.lerp(flat, s == 0 ? 3.0F : 1.5F, fast ? 0.2F : 0.8F) + sway * (s == 0 ? 1.0F : -1.0F);
                mix.foot(s, sign * Mth.lerp(flat, Stance.HIP_X, 0.9F), Stance.HIP_Y + 12.0F - bend,
                        Mth.lerp(flat, 1.0F + sway, 2.0F), fly);
            }
            mix.pitch += (0.1F - 0.15F * flat) * fly;
            // The free right hand: loose by his side in a hover, along his flank at speed, ready to grab.
            mix.hand(0, Mth.lerp(flat, -7.0F, -5.5F), Mth.lerp(flat, 9.0F, 11.0F) + sway, Mth.lerp(flat, -2.5F, 3.0F),
                    fly, false);
        }
        // The left hand: to the axe on the belt, then up ahead of him with it, then back to the belt.
        float reach = 0.0F;
        if (takeOff) {
            reach = (float) (Ease.smooth(age / ThorBody.DRAW) * (1.0 - Ease.smooth((age - ThorBody.DRAW) / 3.0)));
        } else if (touchDown) {
            reach = (float) (Ease.smooth(age / (ThorBody.SHEATHE - 1.0F))
                    * (1.0 - Ease.smooth((age - ThorBody.SHEATHE) / 5.0)));
        }
        if (reach > 1.0E-3F) {
            mix.weight = Math.max(mix.weight, reach);
            mix.hand(1, BELT.x, BELT.y, BELT.z - 0.8F, reach, false);
        }
        float held = axe * (1.0F - reach);
        if (held > 1.0E-3F) {
            float bob = (float) Math.sin(time * 0.11) * 0.6F;
            // Held up ahead in a hover, as if the axe held him up; straight out ahead of him at speed.
            mix.hand(1, Mth.lerp(flat, 4.5F, 2.5F), Mth.lerp(flat, -9.5F, -11.5F) + bob, Mth.lerp(flat, -5.5F, -1.0F),
                    held, false);
        }
        if (view.move() == ThorStatePayload.DIVE && fly > 1.0E-3F) {
            boolean carrying = view.has(ThorStatePayload.CARRYING);
            float dive = (float) Ease.smooth(age / 3.0) * fly;
            if (carrying) {
                // Driving down with it: his right hand down ahead of his feet, holding it under him.
                mix.hand(0, -4.5F, 15.0F, -5.0F, dive, false);
            } else {
                // Reaching ahead to grab.
                mix.hand(0, -3.5F, -10.5F, -2.0F, dive, false);
            }
        }
    }

    // Down on one knee with a fist on the ground, the dive's end.
    private static void slam(Mix mix, ClientThor.View view, float age) {
        if (view.move() != ThorStatePayload.SLAM || age >= ThorKeys.end(ThorKeys.SLAM)) {
            return;
        }
        float[] k = ThorKeys.at(ThorKeys.SLAM, age);
        float w = k[ThorKeys.S_WEIGHT];
        mix.weight = Math.max(mix.weight, w);
        mix.airborne = false;
        mix.drop += k[ThorKeys.S_DROP] * w;
        mix.pitch += k[ThorKeys.S_PITCH] * w;
        mix.waist += 0.15F * w;
        mix.foot(0, -Stance.HIP_X, Stance.GROUND, 5.5F, w);
        mix.foot(1, Stance.HIP_X + 0.5F, Stance.GROUND, -4.5F, w);
        mix.hand(0, -5.0F, Stance.GROUND - 0.5F, -4.0F, k[ThorKeys.S_FIST] * w, true);
    }

    public static boolean axeInHand(Entity entity) {
        ThorBody body = ThorBody.of(entity);
        return body != null && body.inHand;
    }

    // Registered with BodyTurns: his whole body leans and banks in flight.
    public static void turn(AbstractClientPlayer player, PoseStack pose, float scale) {
        ThorBody.turn(player, pose, scale);
    }

    static void clear() {
        ThorBody.forget();
    }
}
