package nl.tivek.multiversepowers.schematic.client;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.schematic.SchematicRules;
import nl.tivek.multiversepowers.schematic.client.hologram.Hologram;
import nl.tivek.multiversepowers.schematic.client.hologram.HologramRenderer;

// An area picked in the world with the wand, to save as a schematic: a left click sets its first corner on the block
// looked at (as far as the hologram reaches), a right click its second; both corners' blocks are in it. Drawn as a
// yellow frame (red while it is too big to save), the corners green and blue, the second following the look until set.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class AreaSelection {
    private static final double REACH = 160.0;

    static boolean on;
    @Nullable
    public static BlockPos first;
    @Nullable
    public static BlockPos second;

    private AreaSelection() {
    }

    public static boolean on() {
        return on;
    }

    // Picking corners in the world; the hologram in hand goes, as the clicks now set corners.
    public static void start() {
        on = true;
        Hologram.hide();
    }

    public static void stop() {
        on = false;
    }

    public static void clear() {
        first = null;
        second = null;
    }

    public static boolean complete() {
        return first != null && second != null;
    }

    static BlockPos min() {
        return BlockPos.min(first, second);
    }

    static BlockPos max() {
        return BlockPos.max(first, second);
    }

    // Its size in blocks across, up and along; zero while a corner is missing.
    public static int[] size() {
        if (!complete()) {
            return new int[3];
        }
        BlockPos low = min();
        BlockPos high = max();
        return new int[] { high.getX() - low.getX() + 1, high.getY() - low.getY() + 1, high.getZ() - low.getZ() + 1 };
    }

    public static long volume() {
        int[] size = size();
        return (long) size[0] * size[1] * size[2];
    }

    public static boolean tooBig() {
        return volume() > SchematicRules.largestSave();
    }

    // The block the look falls on, as far as the hologram reaches.
    @Nullable
    static BlockPos looked(LocalPlayer player) {
        Vec3 eye = player.getEyePosition();
        BlockHitResult hit = player.level().clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(REACH)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK ? hit.getBlockPos() : null;
    }

    // Sets corner 1 or 2 on the block looked at.
    static void corner(LocalPlayer player, boolean firstCorner) {
        BlockPos at = looked(player);
        if (at == null) {
            SchematicClient.say(Component.translatable(SchematicClient.PREFIX + "select.nothing"), true);
            return;
        }
        if (firstCorner) {
            first = at;
        } else {
            second = at;
        }
        SchematicClient.say(Component.translatable(SchematicClient.PREFIX + (firstCorner ? "select.first"
                : "select.second"), at.getX(), at.getY(), at.getZ()), false);
        WandInput.click();
    }

    @SubscribeEvent
    static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || first == null && second == null || !on && !SchematicClient.holding(player)) {
            return;
        }
        BlockPos a = first;
        BlockPos b = second;
        if (on && (a == null || b == null)) {
            BlockPos look = looked(player);
            if (a == null) {
                a = look;
            } else {
                b = look;
            }
        }
        if (a != null && b != null) {
            BlockPos low = BlockPos.min(a, b);
            BlockPos high = BlockPos.max(a, b);
            boolean big = (long) (high.getX() - low.getX() + 1) * (high.getY() - low.getY() + 1) * (high.getZ()
                    - low.getZ() + 1) > SchematicRules.largestSave();
            AABB box = new AABB(low.getX(), low.getY(), low.getZ(), high.getX() + 1, high.getY() + 1, high.getZ() + 1)
                    .inflate(0.02);
            if (big) {
                HologramRenderer.box(event, box, 1.0F, 0.3F, 0.25F);
            } else {
                HologramRenderer.box(event, box, 1.0F, 0.85F, 0.25F);
            }
        }
        if (first != null) {
            HologramRenderer.box(event, new AABB(first).inflate(0.04), 0.4F, 1.0F, 0.45F);
        }
        if (second != null) {
            HologramRenderer.box(event, new AABB(second).inflate(0.04), 0.4F, 0.7F, 1.0F);
        }
    }
}
