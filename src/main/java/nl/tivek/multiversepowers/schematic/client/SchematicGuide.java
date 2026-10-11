package nl.tivek.multiversepowers.schematic.client;

import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import nl.tivek.multiversepowers.schematic.client.blueprint.ClientBlueprints;

// Under the crosshair while a blueprint's guide is on: an arrow turning towards the nearest block still to place (or
// to clear), how far it is and how far up or down, and which block goes there.
final class SchematicGuide {
    private static final int GOLD = 0xFFFFD54A;

    private SchematicGuide() {
    }

    static void render(GuiGraphics graphics, Minecraft minecraft) {
        ClientBlueprints.Guide guide = ClientBlueprints.guide();
        LocalPlayer player = minecraft.player;
        if (guide == null || player == null || minecraft.screen != null) {
            return;
        }
        BlockPos at = guide.pos();
        double dx = at.getX() + 0.5 - player.getX();
        double dz = at.getZ() + 0.5 - player.getZ();
        int dy = at.getY() - Mth.floor(player.getY());
        double along = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float turn = Mth.wrapDegrees(yaw - player.getYRot());
        int cx = graphics.guiWidth() / 2;
        int cy = graphics.guiHeight() / 2 + 22;
        graphics.pose().pushPose();
        graphics.pose().translate(cx, cy, 0.0F);
        if (along > 0.7) {
            graphics.pose().mulPose(Axis.ZP.rotationDegrees(turn));
            arrow(graphics);
        } else {
            // Right above or below: a dot, the text saying which way.
            graphics.fill(-2, -2, 2, 2, GOLD);
        }
        graphics.pose().popPose();
        Font font = minecraft.font;
        boolean clear = guide.state().isAir();
        Component what = clear ? Component.translatable(SchematicClient.PREFIX + "guide.clear")
                : guide.state().getBlock().getName();
        int distance = (int) Math.round(Math.sqrt(dx * dx + dz * dz + dy * dy));
        Component where = dy == 0 ? Component.translatable(SchematicClient.PREFIX + "guide.far", distance)
                : Component.translatable(SchematicClient.PREFIX + (dy > 0 ? "guide.up" : "guide.down"), distance,
                        Math.abs(dy));
        int y = cy + 10;
        int width = font.width(what) + (clear ? 0 : 18);
        int x = cx - width / 2;
        if (!clear) {
            ItemStack item = new ItemStack(guide.item());
            graphics.pose().pushPose();
            graphics.pose().translate(x, y - 2, 0.0F);
            graphics.pose().scale(0.75F, 0.75F, 1.0F);
            graphics.renderItem(item, 0, 0);
            graphics.pose().popPose();
            x += 16;
        }
        graphics.drawString(font, what, x, y, clear ? 0xFFEF7B6E : 0xFFFFFFFF, true);
        graphics.drawCenteredString(font, where, cx, y + 10, GOLD);
    }

    // An arrow pointing up from the middle, in rows of shrinking width.
    private static void arrow(GuiGraphics graphics) {
        graphics.fill(-2, -2, 2, 7, 0xFF000000);
        graphics.fill(-1, -1, 1, 6, GOLD);
        for (int row = 0; row < 6; row++) {
            int half = 6 - row;
            graphics.fill(-half - 1, -3 - row, half + 1, -2 - row, 0xFF000000);
        }
        for (int row = 0; row < 6; row++) {
            int half = 5 - row;
            graphics.fill(-half, -3 - row, half, -2 - row, GOLD);
        }
    }
}
