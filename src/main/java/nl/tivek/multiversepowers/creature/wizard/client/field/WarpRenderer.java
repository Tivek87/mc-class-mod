package nl.tivek.multiversepowers.creature.wizard.client.field;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.client.fx.Additive;
import nl.tivek.multiversepowers.creature.wizard.field.TimeWarp;

// The time warp: a great clock face of golden light on the ground, its rim marked with the hours and minutes, gear
// rings turning against each other inside it, an hour hand, a minute hand and a racing second hand, a shimmering wall
// of light at its edge. Standing inside it, the edges of your sight go golden (under the game's own screen).
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class WarpRenderer extends EntityRenderer<TimeWarp> {
    private static final int WALL_SIDES = 64;

    public WarpRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(TimeWarp warp, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(TimeWarp warp, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int light) {
        float strength = warp.strength(partialTick);
        if (strength <= 0.001F) {
            return;
        }
        float age = warp.tickCount + partialTick;
        float radius = (float) TimeWarp.RADIUS * (0.4F + 0.6F * Math.min(1.0F, age / TimeWarp.GROWS));
        VertexConsumer buffer = buffers.getBuffer(Additive.TYPE);
        PoseStack.Pose at = pose.last();
        float y = 0.04F;
        float[] gold = Additive.rgb(Wizard.TIME, 0.9F * strength);
        float[] white = Additive.rgb(0xFFF4D6, 0.8F * strength);
        float[] faint = Additive.rgb(Wizard.TIME, 0.15F * strength);
        float[] none = Additive.rgb(0, 0.0F);
        Additive.ring(at, buffer, radius * 0.97F, radius, y, 72, gold, gold);
        Additive.ring(at, buffer, radius * 0.9F, radius * 0.92F, y, 72, gold, gold);
        Additive.ring(at, buffer, 0.0F, radius, y - 0.002F, 48, faint, faint);
        Additive.ticks(at, buffer, radius * 0.96F, radius * 0.1F, 12, 0.0F, 0.22F, y + 0.001F, white);
        Additive.ticks(at, buffer, radius * 0.96F, radius * 0.035F, 60, 0.0F, 0.06F, y + 0.001F, gold);
        // Gear rings turning against each other.
        Additive.ring(at, buffer, radius * 0.62F, radius * 0.65F, y + 0.002F, 60, gold, gold);
        Additive.ticks(at, buffer, radius * 0.66F, radius * 0.05F, 30, age * 0.03F, 0.18F, y + 0.002F, gold);
        Additive.ring(at, buffer, radius * 0.36F, radius * 0.39F, y + 0.003F, 48, gold, gold);
        Additive.ticks(at, buffer, radius * 0.36F, radius * 0.05F, 20, -age * 0.05F, 0.16F, y + 0.003F, gold);
        Additive.star(at, buffer, radius * 0.55F, 12, 5, age * 0.008F, 0.07F, y + 0.004F, faint);
        // The hands.
        this.hand(at, buffer, age * 0.012F, radius * 0.5F, 0.32F, y + 0.006F, white);
        this.hand(at, buffer, age * 0.06F, radius * 0.8F, 0.2F, y + 0.007F, white);
        this.hand(at, buffer, age * 0.35F, radius * 0.88F, 0.08F, y + 0.008F, gold);
        Additive.ring(at, buffer, 0.0F, 0.45F, y + 0.009F, 20, white, white);
        // The shimmering wall at its edge.
        for (int i = 0; i < WALL_SIDES; i++) {
            float a0 = i * Mth.TWO_PI / WALL_SIDES;
            float a1 = (i + 1) * Mth.TWO_PI / WALL_SIDES;
            float h = 2.2F + 0.6F * Mth.sin(a0 * 5.0F + age * 0.2F);
            float[] wall = Additive.rgb(Wizard.TIME, 0.35F * strength * (0.7F + 0.3F * Mth.sin(i + age * 0.3F)));
            Additive.quad(at, buffer, new float[] { Mth.cos(a0) * radius, 0.0F, Mth.sin(a0) * radius },
                    new float[] { Mth.cos(a1) * radius, 0.0F, Mth.sin(a1) * radius },
                    new float[] { Mth.cos(a1) * radius, h, Mth.sin(a1) * radius },
                    new float[] { Mth.cos(a0) * radius, h, Mth.sin(a0) * radius }, wall, wall, none, none);
        }
    }

    // A clock hand from the middle out to `length` at `turn` (radians, clockwise from north).
    private void hand(PoseStack.Pose at, VertexConsumer buffer, float turn, float length, float width, float y,
            float[] colour) {
        float x = Mth.sin(turn) * length;
        float z = -Mth.cos(turn) * length;
        Additive.line(at, buffer, -x * 0.12F, -z * 0.12F, x, z, width, y, colour);
    }

    // How deep in a warp the player stands, 0 to 1.
    private static float inside(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return 0.0F;
        }
        float most = 0.0F;
        for (TimeWarp warp : minecraft.level.getEntitiesOfClass(TimeWarp.class,
                new AABB(player.blockPosition()).inflate(TimeWarp.RADIUS + 1.0))) {
            if (warp.holds(player.position())) {
                most = Math.max(most, warp.strength(partialTick));
            }
        }
        return most;
    }

    @SubscribeEvent
    static void onGui(RenderGuiEvent.Pre event) {
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float deep = inside(partialTick);
        if (deep <= 0.0F) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        float tick = 0.85F + 0.15F * Mth.cos(Minecraft.getInstance().level.getGameTime() * Mth.PI / 10.0F);
        int alpha = (int) (110 * deep * tick);
        int edge = alpha << 24 | 0xC8962A;
        int clear = 0x00C8962A;
        int band = Math.max(20, h / 4);
        graphics.fillGradient(0, 0, w, band, edge, clear);
        graphics.fillGradient(0, h - band, w, h, clear, edge);
        for (int x = 0; x < band; x++) {
            int a = (int) (alpha * (1.0F - x / (float) band));
            graphics.fill(x, 0, x + 1, h, a << 24 | 0xC8962A);
            graphics.fill(w - x - 1, 0, w - x, h, a << 24 | 0xC8962A);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(TimeWarp warp) {
        return Additive.ORB;
    }
}
