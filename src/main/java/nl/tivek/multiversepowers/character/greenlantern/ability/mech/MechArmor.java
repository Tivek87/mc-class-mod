package nl.tivek.multiversepowers.character.greenlantern.ability.mech;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import nl.tivek.multiversepowers.MultiversePowers;

// In a mech its pilot is as well guarded as in a full set of netherite armour, and has `mechHealthTimes` their health.
// The modifiers are never saved: a mech never outlives its server.
final class MechArmor {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID,
            "mech_armor");
    private static final double ARMOR = 20.0;
    private static final double TOUGHNESS = 12.0;
    private static final double KNOCKBACK_RESISTANCE = 0.4;

    private MechArmor() {
    }

    static void on(ServerPlayer player, double healthTimes) {
        float part = player.getHealth() / player.getMaxHealth();
        add(player, Attributes.ARMOR, ARMOR, AttributeModifier.Operation.ADD_VALUE);
        add(player, Attributes.ARMOR_TOUGHNESS, TOUGHNESS, AttributeModifier.Operation.ADD_VALUE);
        add(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE, AttributeModifier.Operation.ADD_VALUE);
        add(player, Attributes.MAX_HEALTH, Math.max(0.0, healthTimes - 1.0),
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        player.setHealth(part * player.getMaxHealth());
    }

    static void off(ServerPlayer player) {
        float part = player.getHealth() / player.getMaxHealth();
        for (Holder<Attribute> attribute : List.of(Attributes.ARMOR, Attributes.ARMOR_TOUGHNESS,
                Attributes.KNOCKBACK_RESISTANCE, Attributes.MAX_HEALTH)) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance != null) {
                instance.removeModifier(ID);
            }
        }
        if (player.isAlive()) {
            player.setHealth(Math.max(1.0F, part * player.getMaxHealth()));
        }
    }

    private static void add(ServerPlayer player, Holder<Attribute> attribute, double amount,
            AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(ID);
            instance.addTransientModifier(new AttributeModifier(ID, amount, operation));
        }
    }
}
