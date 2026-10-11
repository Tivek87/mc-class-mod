package nl.tivek.multiversepowers.creature.wizard.ritual;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;

// The Arcane Tome: used on an enchanting table it starts the ritual that calls the Wizard (WizardRitual), and is
// spent. Taken before the table's own use, which would open its menu.
public final class ArcaneTomeItem extends Item {
    public ArcaneTomeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        if (!context.getLevel().getBlockState(context.getClickedPos()).is(Blocks.ENCHANTING_TABLE)) {
            return InteractionResult.PASS;
        }
        if (context.getLevel() instanceof ServerLevel level && context.getPlayer() instanceof ServerPlayer player
                && WizardRitual.begin(level, context.getClickedPos(), player)) {
            stack.consume(1, player);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        String key = this.getDescriptionId() + ".";
        lines.add(Component.translatable(key + "use", WizardRitual.SHELVES).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable(key + "warn").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
