package nl.tivek.multiversepowers.faction;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import nl.tivek.multiversepowers.MultiversePowers;

// The Faction Book: a right click opens it on every faction of the world, its lore, its creatures and players.
public final class FactionBookItem extends Item {
    // Opens the book in the player's own game; the client sets it.
    public static Runnable clientUse = () -> {
    };

    public FactionBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) {
            clientUse.run();
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item." + MultiversePowers.MODID + ".faction_book.use")
                .withStyle(ChatFormatting.GRAY));
    }
}
