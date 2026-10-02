package nl.tivek.multiversepowers.engine.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

// A model of no item or block, loaded from assets/<namespace>/models/<path>.json once registered, and drawn from the
// block atlas as the item renderer draws one: the corner of its block on the pose's origin.
public final class StandaloneModel {
    // The game's own entity cutout and eyes, but of our own: its cutout on the block atlas is drawn only once every
    // creature is done, so it would cover a glow drawn over it.
    private static final RenderType BODY = RenderType.create("welcomescreen_standalone",
            DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_CUTOUT_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(InventoryMenu.BLOCK_ATLAS, false, false))
                    .setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
                    .setLightmapState(RenderStateShard.LIGHTMAP)
                    .setOverlayState(RenderStateShard.OVERLAY)
                    .createCompositeState(true));
    private static final RenderType GLOW = RenderType.create("welcomescreen_standalone_glow",
            DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_EYES_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(InventoryMenu.BLOCK_ATLAS, false, false))
                    .setTransparencyState(RenderStateShard.ADDITIVE_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .createCompositeState(false));

    private final ModelResourceLocation location;

    public StandaloneModel(ResourceLocation path) {
        this.location = ModelResourceLocation.standalone(path);
    }

    public void register(ModelEvent.RegisterAdditional event) {
        event.register(this.location);
    }

    public void draw(PoseStack pose, MultiBufferSource buffers, int light) {
        Minecraft minecraft = Minecraft.getInstance();
        BakedModel model = minecraft.getModelManager().getModel(this.location);
        minecraft.getItemRenderer().renderModelLists(model, ItemStack.EMPTY, light, OverlayTexture.NO_OVERLAY, pose,
                buffers.getBuffer(BODY));
    }

    // Light added over what is drawn, as bright by night as by day: its picture's black adds nothing. Drawn after the
    // model it lies on, in the same pose.
    public void glow(PoseStack pose, MultiBufferSource buffers, int color, float strength) {
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(this.location);
        VertexConsumer buffer = buffers.getBuffer(GLOW);
        // A vertex colour past 1 wraps round to dark.
        float k = Mth.clamp(strength, 0.0F, 1.0F) / 255.0F;
        float[] rgb = { (color >> 16 & 0xFF) * k, (color >> 8 & 0xFF) * k, (color & 0xFF) * k };
        RandomSource random = RandomSource.create();
        for (Direction side : Direction.values()) {
            random.setSeed(42L);
            put(buffer, pose, model.getQuads(null, side, random, ModelData.EMPTY, null), rgb);
        }
        random.setSeed(42L);
        put(buffer, pose, model.getQuads(null, null, random, ModelData.EMPTY, null), rgb);
    }

    private static void put(VertexConsumer buffer, PoseStack pose, List<BakedQuad> quads, float[] rgb) {
        for (BakedQuad quad : quads) {
            buffer.putBulkData(pose.last(), quad, rgb[0], rgb[1], rgb[2], 1.0F, LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY);
        }
    }
}
