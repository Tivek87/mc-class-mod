package nl.tivek.multiversepowers.schematic;

import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import nl.tivek.multiversepowers.MultiversePowers;

// The Schematic Wand: sneak and right click opens the schematic menu, a right click pins the hologram where it is or
// lets it follow again, a left click places it. Only for the host, an owner or an operator without a character.
public final class SchematicWandItem extends Item {
    // What a right click does in the player's own game (the menu when sneaking); the client sets it.
    public static BiConsumer<Player, Boolean> clientUse = (player, sneaking) -> {
    };

    public SchematicWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide && hand == InteractionHand.MAIN_HAND) {
            clientUse.accept(player, player.isShiftKeyDown());
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return false;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        String key = "item." + MultiversePowers.MODID + ".schematic_wand.";
        lines.add(Component.translatable(key + "use").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable(key + "who").withStyle(ChatFormatting.DARK_GRAY));
    }
}
