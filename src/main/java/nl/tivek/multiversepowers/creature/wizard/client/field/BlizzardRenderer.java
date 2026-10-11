package nl.tivek.multiversepowers.creature.wizard.client.field;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.creature.wizard.field.Blizzard;
import nl.tivek.multiversepowers.creature.wizard.client.fx.Additive;

// The Wizard's blizzard: sheets of driven snow whirling round in bands at three heights, torn and thickest at its
// wall, and frost glittering on the ground inside. Seen from inside it the world is lost in white: the fog closes in
// and pales the further in the eye is.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class BlizzardRenderer extends EntityRenderer<Blizzard> {
    private static final int SIDES = 40;
    private static final int SNOW = 0xF4F9FF;
    private static final int FROST = 0xE2F4FF;
    private static final float[] BANDS = { 0.4F, 1.6F, 2.9F };
    private static final float FOG_NEAR = 0.0F;
    private static final float FOG_FAR = 7.0F;

    public BlizzardRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(Blizzard storm, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(Blizzard storm, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int light) {
        float strength = storm.strength(partialTick);
        if (strength <= 0.001F) {
            return;
        }
        float age = storm.tickCount + partialTick;
        float radius = (float) storm.radius(partialTick);
        VertexConsumer buffer = buffers.getBuffer(Additive.MIST);
        PoseStack.Pose at = pose.last();
        float[] none = Additive.mist(SNOW, 0.0F);
        for (int band = 0; band < BANDS.length; band++) {
            float spin = age * (0.12F + 0.03F * band) + band * 2.1F;
            float r = radius * (0.92F + 0.06F * band);
            float y = BANDS[band];
            float tall = 1.1F + 0.3F * band;
            for (int i = 0; i < SIDES; i++) {
                float a0 = i * Mth.TWO_PI / SIDES + spin;
                float a1 = (i + 1) * Mth.TWO_PI / SIDES + spin;
                // Torn: thick in places, gone in others, the tears turning with it.
                float tear0 = sheet(i, band, age);
                float tear1 = sheet(i + 1, band, age);
                float[] c0 = Additive.mist(SNOW, 0.42F * strength * tear0);
                float[] c1 = Additive.mist(SNOW, 0.42F * strength * tear1);
                float x0 = Mth.cos(a0) * r;
                float z0 = Mth.sin(a0) * r;
                float x1 = Mth.cos(a1) * r;
                float z1 = Mth.sin(a1) * r;
                float lean = 0.25F;
                Additive.quad(at, buffer, new float[] { x0, y, z0 }, new float[] { x1, y, z1 },
                        new float[] { x1 * (1.0F - lean * 0.2F), y + tall * 0.5F, z1 * (1.0F - lean * 0.2F) },
                        new float[] { x0 * (1.0F - lean * 0.2F), y + tall * 0.5F, z0 * (1.0F - lean * 0.2F) },
                        none, none, c1, c0);
                Additive.quad(at, buffer,
                        new float[] { x0 * (1.0F - lean * 0.2F), y + tall * 0.5F, z0 * (1.0F - lean * 0.2F) },
                        new float[] { x1 * (1.0F - lean * 0.2F), y + tall * 0.5F, z1 * (1.0F - lean * 0.2F) },
                        new float[] { x1, y + tall, z1 }, new float[] { x0, y + tall, z0 }, c0, c1, none, none);
            }
        }
        // Frost on the ground: a pale ring at its wall fading in towards the middle.
        for (int i = 0; i < SIDES; i++) {
            float a0 = i * Mth.TWO_PI / SIDES;
            float a1 = (i + 1) * Mth.TWO_PI / SIDES;
            float[] edge = Additive.mist(FROST, 0.55F * strength);
            float[] in = Additive.mist(FROST, 0.15F * strength);
            Additive.quad(at, buffer, new float[] { Mth.cos(a0) * radius * 0.4F, 0.03F, Mth.sin(a0) * radius * 0.4F },
                    new float[] { Mth.cos(a1) * radius * 0.4F, 0.03F, Mth.sin(a1) * radius * 0.4F },
                    new float[] { Mth.cos(a1) * radius, 0.03F, Mth.sin(a1) * radius },
                    new float[] { Mth.cos(a0) * radius, 0.03F, Mth.sin(a0) * radius }, in, in, edge, edge);
        }
        super.render(storm, yaw, partialTick, pose, buffers, light);
    }

    private static float sheet(int i, int band, float age) {
        float s = Mth.sin(i * 0.9F + band * 1.3F + age * 0.05F) * 0.5F + Mth.sin(i * 2.3F - age * 0.08F) * 0.5F;
        return Mth.clamp(0.35F + 0.65F * s, 0.0F, 1.0F);
    }

    // How far into a blizzard the eye is: 0 outside, 1 deep in its middle while it whirls its hardest.
    private static float inside(Camera camera, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return 0.0F;
        }
        Vec3 eye = camera.getPosition();
        float most = 0.0F;
        for (Blizzard storm : minecraft.level.getEntitiesOfClass(Blizzard.class, new AABB(eye, eye).inflate(
                Blizzard.RADIUS + 1.0, Blizzard.TALL + 2.0, Blizzard.RADIUS + 1.0))) {
            Vec3 at = storm.getPosition(partialTick);
            double radius = storm.radius(partialTick);
            double far = Math.sqrt((eye.x - at.x) * (eye.x - at.x) + (eye.z - at.z) * (eye.z - at.z));
            if (eye.y < at.y - 1.0 || eye.y > at.y + Blizzard.TALL + 1.0) {
                continue;
            }
            float deep = (float) Mth.clamp((radius + 0.5 - far) / (radius * 0.6), 0.0, 1.0);
            most = Math.max(most, deep * storm.strength(partialTick));
        }
        return most;
    }

    @SubscribeEvent
    static void onFog(ViewportEvent.RenderFog event) {
        float deep = inside(event.getCamera(), (float) event.getPartialTick());
        if (deep <= 0.0F) {
            return;
        }
        event.setNearPlaneDistance(Mth.lerp(deep, event.getNearPlaneDistance(), FOG_NEAR));
        event.setFarPlaneDistance(Mth.lerp(deep, event.getFarPlaneDistance(), FOG_FAR));
        event.setCanceled(true);
    }

    @SubscribeEvent
    static void onFogColour(ViewportEvent.ComputeFogColor event) {
        float deep = inside(event.getCamera(), (float) event.getPartialTick());
        if (deep <= 0.0F) {
            return;
        }
        event.setRed(Mth.lerp(deep, event.getRed(), 0.82F));
        event.setGreen(Mth.lerp(deep, event.getGreen(), 0.9F));
        event.setBlue(Mth.lerp(deep, event.getBlue(), 0.97F));
    }

    @Override
    public ResourceLocation getTextureLocation(Blizzard storm) {
        return Additive.ORB;
    }
}
