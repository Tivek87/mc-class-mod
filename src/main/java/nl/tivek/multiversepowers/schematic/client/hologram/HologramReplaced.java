package nl.tivek.multiversepowers.schematic.client.hologram;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.schematic.Schematic;
import nl.tivek.multiversepowers.schematic.SchematicPlacement;
import nl.tivek.multiversepowers.schematic.paste.SchematicPastes;

// The blocks of the world the hologram in hand would replace where it stands now, marked red: looked for a share of
// its box each tick, from the start again whenever it moves, turns or what it replaces changes. Only solid blocks
// count: grass and flowers simply give way. A blueprint replaces nothing itself; it shows what Build it would.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class HologramReplaced {
    private static final int SCAN = 60000;
    // Marked at most; the count goes on past it.
    private static final int SHOWN = 4096;

    private record Key(Schematic schematic, SchematicPlacement placement, int mode, int layer) {
    }

    @Nullable
    private static Key key;
    private static int cursor;
    private static int count;
    private static boolean complete;
    private static final LongArrayList FOUND = new LongArrayList();
    private static final int[] XZ = new int[2];
    private static final BlockPos.MutableBlockPos AT = new BlockPos.MutableBlockPos();

    private HologramReplaced() {
    }

    // How many blocks it replaces, as far as it has looked; whether it has looked at all of it.
    public static int count() {
        return count;
    }

    public static boolean complete() {
        return complete;
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        ClientLevel level = Minecraft.getInstance().level;
        Schematic shown = Hologram.schematic;
        if (level == null || shown == null) {
            reset(null);
            return;
        }
        int mode = Hologram.blueprint ? SchematicPastes.SOLID : Hologram.mode;
        Key now = new Key(shown, Hologram.placement(), mode, Hologram.layer);
        if (!now.equals(key)) {
            reset(now);
        }
        if (complete || mode == SchematicPastes.EMPTY) {
            complete = true;
            return;
        }
        SchematicPlacement placement = now.placement();
        int wideX = placement.sizeX(shown);
        int wideZ = placement.sizeZ(shown);
        int top = now.layer() < 0 ? shown.sizeY : now.layer() + 1;
        int volume = wideX * wideZ * top;
        BlockPos origin = placement.origin();
        for (int k = 0; k < SCAN && cursor < volume; k++, cursor++) {
            int u = cursor % wideX;
            int rest = cursor / wideX;
            int w = rest % wideZ;
            int y = rest / wideZ;
            placement.source(shown, u, w, XZ);
            BlockState planned = shown.state(shown.index(XZ[0], y, XZ[1]));
            if (planned == null || planned.isAir() && mode != SchematicPastes.ALL) {
                continue;
            }
            AT.set(origin.getX() + u, origin.getY() + y, origin.getZ() + w);
            if (!level.hasChunk(AT.getX() >> 4, AT.getZ() >> 4)) {
                continue;
            }
            BlockState world = level.getBlockState(AT);
            if (world.isAir() || world.canBeReplaced()) {
                continue;
            }
            BlockState turned = planned.mirror(placement.mirror()).rotate(placement.rotation());
            if (world == turned) {
                continue;
            }
            count++;
            if (FOUND.size() < SHOWN) {
                FOUND.add(AT.asLong());
            }
        }
        complete = cursor >= volume;
    }

    private static void reset(@Nullable Key next) {
        key = next;
        cursor = 0;
        count = 0;
        complete = false;
        FOUND.clear();
    }

    // Each block it replaces as a red box a little bigger than the block.
    static void draw(RenderLevelStageEvent event) {
        if (FOUND.isEmpty()) {
            return;
        }
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer out = buffers.getBuffer(RenderType.debugFilledBox());
        PoseStack pose = event.getPoseStack();
        for (int k = 0; k < FOUND.size(); k++) {
            long at = FOUND.getLong(k);
            double x = BlockPos.getX(at) - camera.x;
            double y = BlockPos.getY(at) - camera.y;
            double z = BlockPos.getZ(at) - camera.z;
            LevelRenderer.addChainedFilledBoxVertices(pose, out, x - 0.01, y - 0.01, z - 0.01, x + 1.01, y + 1.01,
                    z + 1.01, 1.0F, 0.15F, 0.1F, 0.38F);
        }
        buffers.endBatch(RenderType.debugFilledBox());
    }
}
