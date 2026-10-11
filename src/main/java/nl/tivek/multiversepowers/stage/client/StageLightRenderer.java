package nl.tivek.multiversepowers.stage.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.EnumMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.client.render.StandaloneModel;
import nl.tivek.multiversepowers.stage.LampKind;
import nl.tivek.multiversepowers.stage.StageBlocks;
import nl.tivek.multiversepowers.stage.StageLightBlock;
import nl.tivek.multiversepowers.stage.StageLightBlockEntity;

// A stage light's moving parts: the yoke turning about its mount, the head turning and tilting in it (a mirror ball
// spinning), and its lens lit in the colour it shines. The beams are StageBeams'.
public final class StageLightRenderer implements BlockEntityRenderer<StageLightBlockEntity> {
    private record Parts(@Nullable StandaloneModel yoke, @Nullable StandaloneModel head,
            @Nullable StandaloneModel lens) {
    }

    private static final Map<LampKind, Parts> PARTS = new EnumMap<>(LampKind.class);

    static {
        for (LampKind kind : LampKind.values()) {
            boolean moves = kind != LampKind.LASER && kind != LampKind.STROBE;
            boolean yoke = moves && kind != LampKind.DISCO_BALL;
            PARTS.put(kind, new Parts(yoke ? part(kind, "yoke") : null, moves ? part(kind, "head") : null,
                    kind != LampKind.DISCO_BALL ? part(kind, "lens") : null));
        }
    }

    private static StandaloneModel part(LampKind kind, String part) {
        return new StandaloneModel(ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID,
                "stage/" + kind.id() + "_" + part));
    }

    public static void onRegisterModels(ModelEvent.RegisterAdditional event) {
        for (Parts parts : PARTS.values()) {
            for (StandaloneModel model : new StandaloneModel[] { parts.yoke, parts.head, parts.lens }) {
                if (model != null) {
                    model.register(event);
                }
            }
        }
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(StageBlocks.LAMP.get(), context -> new StageLightRenderer());
    }

    @Override
    public void render(StageLightBlockEntity lamp, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int light, int overlay) {
        LampView view = LampView.of(lamp, partialTick);
        StageBeams.queue(lamp, view);
        Parts parts = PARTS.get(lamp.kind());
        Direction facing = lamp.getBlockState().hasProperty(StageLightBlock.FACING)
                ? lamp.getBlockState().getValue(StageLightBlock.FACING) : Direction.UP;
        double pivot = LampView.pivot(lamp.kind()) / 16.0;
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(facing.getRotation());
        pose.translate(-0.5, -0.5, -0.5);
        boolean disco = lamp.kind() == LampKind.DISCO_BALL;
        if (parts.yoke != null || parts.head != null) {
            pose.translate(0.5, 0.0, 0.5);
            pose.mulPose(Axis.YP.rotation((float) (disco ? view.roll : view.pan)));
            pose.translate(-0.5, 0.0, -0.5);
        }
        if (parts.yoke != null) {
            parts.yoke.draw(pose, buffers, light);
        }
        if (!disco && parts.head != null) {
            pose.translate(0.0, pivot, 0.5);
            pose.mulPose(Axis.XP.rotation((float) view.tilt));
            pose.translate(0.0, -pivot, -0.5);
        }
        if (parts.head != null) {
            parts.head.draw(pose, buffers, light);
        }
        float glow = (float) StageBeams.strength(lamp, view);
        if (parts.lens != null && glow > 0.0F) {
            parts.lens.glow(pose, buffers, StageBeams.color(lamp, view, 0), Math.min(1.0F, 0.35F + glow));
        }
        if (disco && parts.head != null && glow > 0.0F) {
            parts.head.glow(pose, buffers, StageBeams.color(lamp, view, 0), 0.25F * Math.min(1.0F, glow));
        }
        pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(StageLightBlockEntity lamp) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public boolean shouldRender(StageLightBlockEntity lamp, Vec3 camera) {
        return Vec3.atCenterOf(lamp.getBlockPos()).closerThan(camera, this.getViewDistance());
    }

    @Override
    public AABB getRenderBoundingBox(StageLightBlockEntity lamp) {
        return AABB.INFINITE;
    }
}
