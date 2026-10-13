package nl.tivek.multiversepowers.creature.elemental;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import nl.tivek.multiversepowers.MultiversePowers;

// One of the Elemental Blaze's five rods, dropped where it dies, one at random. All five together make the Elemental
// Staff.
public final class ElementRod extends Item {
    private final Element element;

    public ElementRod(Element element, Properties properties) {
        super(properties);
        this.element = element;
    }

    public Element element() {
        return this.element;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(this.element.key()).withColor(this.element.light));
        tooltip.add(Component.translatable("item." + MultiversePowers.MODID + ".element_rod.combine")
                .withStyle(ChatFormatting.GRAY));
    }
}
