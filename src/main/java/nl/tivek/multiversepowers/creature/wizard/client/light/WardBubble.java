package nl.tivek.multiversepowers.creature.wizard.client.light;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.client.fx.Additive;

// His ward round him while it holds: a bubble of his phase's light, faint where it is seen face on and bright at its
// rim, sewn with a lattice of brighter cells that shimmer; cracks of white light spread over it as it weakens, more for
// every blow it has taken; a blow flashes it, and it trembles. When it breaks its cells fly apart outward and fade.
public final class WardBubble {
    private static final float WIDE = 1.25F;
    private static final float TALL = 1.55F;
    private static final float MIDDLE = 1.05F;
    private static final int RINGS = 10;
    private static final int SIDES = 18;
    private static final int MOST_CRACKS = 12;
    private static final int SHATTER = 16;

    private WardBubble() {
    }

    // `eye` is the camera from his feet, as the pose stands.
    public static void draw(Wizard wizard, float age, PoseStack pose, MultiBufferSource buffers, Vec3 eye) {
        float ward = wizard.ward();
        float broke = age - wizard.wardBroke();
        boolean breaking = ward <= 0.0F && broke >= 0.0F && broke < SHATTER;
        if (ward <= 0.0F && !breaking) {
            return;
        }
        int colour = Wizard.light(wizard.phase());
        float struck = Mth.clamp(1.0F - (age - wizard.wardStruck()) / 7.0F, 0.0F, 1.0F);
        float share = breaking ? 0.0F : Mth.clamp(ward / Wizard.wardOf(wizard.phase()), 0.0F, 1.0F);
        VertexConsumer glow = buffers.getBuffer(Additive.TYPE);
        pose.pushPose();
        pose.translate(0.0F, MIDDLE, 0.0F);
        float tremble = 1.0F + 0.04F * struck * Mth.sin(age * 4.0F);
        Vec3 from = eye.subtract(0.0, MIDDLE, 0.0);
        if (breaking) {
            float f = broke / SHATTER;
            float fade = 1.0F - f;
            Additive.shell(pose.last(), glow, WIDE, TALL, RINGS, SIDES, from, (ring, side, rim) -> Additive.rgb(
                    (ring + side) % 2 == 0 ? colour : 0xFFFFFF, (0.25F + 0.5F * rim) * fade), 0.3F + 0.3F * f,
                    f * 1.2F);
            pose.popPose();
            return;
        }
        float pulse = 0.5F + 0.5F * Mth.sin(age * 0.12F);
        Additive.shell(pose.last(), glow, WIDE * tremble, TALL * tremble, RINGS, SIDES, from, (ring, side, rim) -> {
            boolean lattice = (ring + side) % 3 == 0;
            float shimmer = 0.5F + 0.5F * Mth.sin(age * 0.2F + ring * 0.9F + side * 1.3F);
            float strength = 0.02F + (lattice ? 0.05F * shimmer : 0.0F) + 0.45F * rim * rim * rim
                    + 0.35F * struck;
            return Additive.rgb(colour, strength * (0.7F + 0.3F * pulse));
        }, 0.04F, 0.0F);
        cracks(pose, glow, wizard, share, from, struck);
        pose.popPose();
    }

    // Cracks of light on the bubble, one more as each share of it is lost, each wandering over it from where it began.
    private static void cracks(PoseStack pose, VertexConsumer glow, Wizard wizard, float share, Vec3 eye,
            float struck) {
        int count = Math.round((1.0F - share) * MOST_CRACKS);
        RandomSource random = RandomSource.create(wizard.getId() * 31L + wizard.phase());
        for (int c = 0; c < count; c++) {
            double lat = random.nextDouble() * 2.4 - 1.2;
            double lon = random.nextDouble() * Math.PI * 2.0;
            Vec3 last = on(lat, lon);
            for (int k = 0; k < 6; k++) {
                lat = Mth.clamp(lat + random.nextGaussian() * 0.22, -1.4, 1.4);
                lon += random.nextGaussian() * 0.3;
                Vec3 next = on(lat, lon);
                Additive.ribbon(pose.last(), glow, last, next, eye, 0.035F, Additive.rgb(0xFFFFFF,
                        0.55F + 0.4F * struck), Additive.rgb(0xFFFFFF, 0.35F + 0.4F * struck));
                last = next;
            }
        }
    }

    private static Vec3 on(double lat, double lon) {
        double out = 1.01;
        return new Vec3(Math.cos(lat) * Math.cos(lon) * WIDE * out, Math.sin(lat) * TALL * out,
                Math.cos(lat) * Math.sin(lon) * WIDE * out);
    }
}
