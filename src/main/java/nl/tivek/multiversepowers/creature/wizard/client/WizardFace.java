package nl.tivek.multiversepowers.creature.wizard.client;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.MovingMonster;
import nl.tivek.multiversepowers.creature.wizard.WizardMove;
import nl.tivek.multiversepowers.creature.wizard.WizardPastimes;
import nl.tivek.multiversepowers.faction.mob.client.MobPreview;

// The Wizard's face this frame (WizardModel moves its parts): where his eyes look, how far his lids are shut, how high
// his brows are and how they knit, how wide his mouth is open, and how brightly his eyes flare. He looks at you, more
// often the angrier he is; else his eyes dart about, now and then glancing at whoever is near; he blinks. Gathering a
// great spell his brows knit and his eyes blaze, and he shouts as he lets it go; struck, he winces; stunned, his eyes
// roll under drooping lids; dying, his eyes go wide. At rest his face does what his habit or pastime does: yawning,
// gazing up at the stars, reading line by line, following the wisp, eyes shut in meditation.
final class WizardFace {
    private static final Map<MovingMonster, float[]> SHOWN = new WeakHashMap<>();
    // Where each value lives in a face's state: the shown look, brows, mouth, flare, and the frame's age.
    private static final int LOOK_X = 0;
    private static final int LOOK_Y = 1;
    private static final int RAISE = 2;
    private static final int FROWN = 3;
    private static final int OPEN = 4;
    private static final int FLARE = 5;
    private static final int AGE = 6;
    // How far the irises move (pixels) and how far the head is turned for them to reach their edge (degrees).
    private static final float REACH_X = 0.5F;
    private static final float REACH_Y = 0.22F;
    private static final float TURN_X = 35.0F;
    private static final float TURN_Y = 30.0F;
    float lookX;
    float lookY;
    float blink;
    float raise;
    float frown;
    float open;
    float flare;

    void of(MovingMonster caster, float age, float amount, float headYaw, float headPitch) {
        int move = caster.move();
        float t = (float) caster.moveAge(age - caster.tickCount);
        int id = caster.getId();
        boolean angry = caster.isAggressive();
        boolean preview = MobPreview.drawing(caster);
        // Where he wants to look, before the moment's own look.
        float wantX;
        float wantY;
        float flick = (float) Math.floor((age + id * 13) / (25.0F + 35.0F * noise(id, (int) (age / 60.0F))));
        float glance = noise(id * 7, (int) (age / 90.0F));
        Vec3 eye = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        boolean near = !preview && eye.distanceToSqr(caster.position()) < (angry ? 32.0 * 32.0 : 12.0 * 12.0);
        if (near && (angry ? glance < 0.85F : glance < 0.35F)) {
            float[] look = this.toward(caster, eye, age, headYaw, headPitch);
            wantX = look[0];
            wantY = look[1];
        } else {
            wantX = (noise(id + (int) flick, 3) - 0.5F) * 2.0F * REACH_X * 0.9F;
            wantY = (noise(id + (int) flick, 5) - 0.5F) * REACH_Y;
        }
        float raise = angry ? -0.15F : 0.1F;
        float frown = angry ? 0.22F : 0.0F;
        float open = 0.0F;
        float flare = 0.0F;
        float shut = 0.0F;
        if (angry && near) {
            // A cold glint now and then as he looks at you.
            flare = 0.6F * WizardRest.bump(((age + id * 31) % 140.0F) / 22.0F);
        }
        if (move == 0 && amount < 0.1F && !angry) {
            int habit = WizardRest.habit(age + id * 37);
            float w = habit < 0 ? 0.0F : WizardRest.bump(WizardRest.through(age + id * 37));
            switch (habit) {
                case WizardRest.YAWN -> {
                    float wide = WizardRest.bump(Mth.clamp(WizardRest.through(age + id * 37) * 1.6F - 0.1F, 0.0F,
                            1.0F));
                    open = wide;
                    shut = 0.8F * wide;
                    raise += 0.3F * wide;
                }
                case WizardRest.STARGAZE -> {
                    wantY = -REACH_Y;
                    raise += 0.3F * w;
                }
                case WizardRest.PEER -> {
                    wantX = -0.4F;
                    wantY = -0.1F;
                    flare = Math.max(flare, 0.6F * w);
                    frown += 0.1F * w;
                }
                case WizardRest.CHARM -> open = 0.3F * Math.abs(Mth.sin(age * 0.9F)) * w;
                case WizardRest.FLAME -> {
                    wantX = 0.3F;
                    wantY = 0.1F;
                    flare = Math.max(flare, 0.4F * w);
                }
                case WizardRest.LOOK -> wantX = REACH_X * Mth.sin(WizardRest.through(age + id * 37) * Mth.TWO_PI);
                case WizardRest.STRETCH -> {
                    shut = 0.6F * w;
                    open = 0.4F * w;
                }
                case WizardRest.TWIRL -> wantX = -0.3F;
                default -> {
                }
            }
        } else if (WizardMove.pastime(move)) {
            switch (move) {
                case WizardMove.READ -> {
                    float line = (t % 30.0F) / 30.0F;
                    wantX = REACH_X * (0.7F - 1.4F * line);
                    wantY = 0.18F;
                    raise += 0.15F;
                }
                case WizardMove.JUGGLE -> {
                    wantX = 0.35F * Mth.sin(t * 0.29F);
                    wantY = -0.16F;
                }
                case WizardMove.MEDITATE -> {
                    float length = WizardMove.length(move);
                    shut = Mth.clamp((t - 30.0F) / 10.0F, 0.0F, 1.0F) * Mth.clamp((length - 30.0F - t) / 10.0F,
                            0.0F, 1.0F);
                    raise += 0.05F;
                }
                case WizardMove.BREW -> {
                    wantY = 0.16F;
                    float sniff = WizardRest.bump(Mth.clamp((t - WizardMove.length(move) + 56.0F) / 24.0F, 0.0F,
                            1.0F));
                    shut = 0.7F * sniff;
                    raise += 0.3F * sniff;
                }
                case WizardMove.WISP -> {
                    float[] look = this.toward(caster, WizardPastimes.wisp(caster, t), age, headYaw, headPitch);
                    wantX = look[0];
                    wantY = look[1];
                    raise += 0.25F;
                }
                case WizardMove.SCRIBE -> wantY = 0.2F;
                case WizardMove.PRACTICE -> {
                    float cast = ((t - 16.0F) % 36.0F + 36.0F) % 36.0F;
                    open = 0.35F * WizardRest.bump(Mth.clamp((cast - 6.0F) / 12.0F, 0.0F, 1.0F));
                    frown += 0.1F;
                }
                default -> {
                }
            }
        } else if (move != 0) {
            float release = WizardMove.release(move);
            boolean great = WizardMove.big(move) || WizardMove.isSpecial(move) || WizardMove.carries(move) != 0;
            if (move == WizardMove.DEATH) {
                raise = 0.5F;
                frown = -0.3F;
                open = 0.8F;
                shut = 0.0F;
                flare = t > WizardMove.BREAK - 8 ? 1.0F : 0.3F;
            } else if (move == WizardMove.STUNNED) {
                shut = 0.5F;
                raise = 0.3F;
                frown = -0.25F;
                open = 0.5F;
                wantX = REACH_X * Mth.sin(age * 0.3F);
                wantY = -0.1F;
            } else if (move == WizardMove.ARRIVE) {
                shut = t < WizardMove.LAND - 10 ? 0.9F : 0.0F;
                flare = Mth.clamp(1.0F - Math.abs(t - WizardMove.LAND) / 12.0F, 0.0F, 1.0F);
            } else if (move == WizardMove.WATCH) {
                shut = 0.3F;
                frown = 0.18F;
                raise = -0.05F;
            } else if (great) {
                float gather = Mth.clamp(t / Math.max(1.0F, release), 0.0F, 1.0F);
                frown = 0.22F + 0.2F * gather;
                flare = Math.max(flare, t <= release ? gather : Mth.clamp(1.0F - (t - release) / 10.0F, 0.0F, 1.0F));
                open = WizardRest.bump(Mth.clamp((t - release + 4.0F) / 14.0F, 0.0F, 1.0F));
            } else if (WizardMove.light(move) || WizardMove.close(move)) {
                open = 0.35F * WizardRest.bump(Mth.clamp((t - release + 2.0F) / 8.0F, 0.0F, 1.0F));
                frown += 0.1F;
            }
        }
        if (caster.hurtTime > 0) {
            float f = caster.hurtTime / 10.0F;
            shut = Math.max(shut, f);
            frown = Math.max(frown, 0.4F * f);
            open = Math.max(open, 0.6F * f);
        }
        float[] shown = SHOWN.computeIfAbsent(caster, c -> new float[] { 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, age });
        float dt = Mth.clamp(age - shown[AGE], 0.0F, 10.0F);
        shown[AGE] = age;
        shown[LOOK_X] = follow(shown[LOOK_X], Mth.clamp(wantX, -REACH_X, REACH_X), 0.7F, dt);
        shown[LOOK_Y] = follow(shown[LOOK_Y], Mth.clamp(wantY, -REACH_Y, REACH_Y), 0.7F, dt);
        shown[RAISE] = follow(shown[RAISE], raise, 0.3F, dt);
        shown[FROWN] = follow(shown[FROWN], frown, 0.3F, dt);
        shown[OPEN] = follow(shown[OPEN], open, 0.45F, dt);
        shown[FLARE] = follow(shown[FLARE], flare, 0.35F, dt);
        this.lookX = shown[LOOK_X];
        this.lookY = shown[LOOK_Y];
        this.raise = shown[RAISE];
        this.frown = shown[FROWN];
        this.open = shown[OPEN];
        this.flare = shown[FLARE];
        // A blink every few seconds, quick: shut over two ticks and open over three.
        float period = 70.0F + 80.0F * noise(id, (int) ((age + id * 11) / 150.0F));
        float into = (age + id * 11) % period;
        float natural = into < 2.0F ? into / 2.0F : into < 5.0F ? 1.0F - (into - 2.0F) / 3.0F : 0.0F;
        this.blink = Mth.clamp(Math.max(shut, natural), 0.0F, 1.0F);
    }

    // The iris offset that looks at `at` from his eyes, as far as the irises reach past where his head turned.
    private float[] toward(MovingMonster caster, Vec3 at, float age, float headYaw, float headPitch) {
        Vec3 from = caster.getEyePosition(age - caster.tickCount);
        Vec3 way = at.subtract(from);
        double flat = Math.sqrt(way.x * way.x + way.z * way.z);
        float yaw = (float) (Mth.atan2(-way.x, way.z) * Mth.RAD_TO_DEG);
        float pitch = (float) (-Mth.atan2(way.y, flat) * Mth.RAD_TO_DEG);
        float turn = Mth.wrapDegrees(yaw - (caster.yBodyRot + headYaw));
        float tilt = pitch - headPitch;
        return new float[] { -Mth.clamp(turn / TURN_X, -1.0F, 1.0F) * REACH_X,
                Mth.clamp(tilt / TURN_Y, -1.0F, 1.0F) * REACH_Y };
    }

    private static float follow(float shown, float want, float rate, float dt) {
        return shown + (want - shown) * (1.0F - (float) Math.pow(1.0F - rate, dt));
    }

    // A steady random fraction for (a, b).
    private static float noise(int a, int b) {
        int h = a * 73856093 ^ b * 19349663;
        h ^= h >>> 13;
        h *= 0x5bd1e995;
        h ^= h >>> 15;
        return (h & 0xFFFF) / 65535.0F;
    }
}
