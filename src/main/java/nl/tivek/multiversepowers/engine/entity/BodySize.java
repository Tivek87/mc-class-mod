package nl.tivek.multiversepowers.engine.entity;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

// A body made bigger or smaller by a factor while a power lasts: its model and bones, hitbox, eyes and camera, how high
// it steps and how far it reaches all change together. The game's own attributes carry it, so every player sees it.
public final class BodySize {
    private static final List<Holder<Attribute>> GROWS = List.of(Attributes.SCALE, Attributes.STEP_HEIGHT,
            Attributes.BLOCK_INTERACTION_RANGE, Attributes.ENTITY_INTERACTION_RANGE);

    private BodySize() {
    }

    public static void set(LivingEntity body, ResourceLocation id, double factor) {
        for (Holder<Attribute> attribute : GROWS) {
            AttributeInstance instance = body.getAttribute(attribute);
            if (instance != null) {
                instance.addOrUpdateTransientModifier(new AttributeModifier(id, factor - 1.0,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        }
    }

    public static void reset(LivingEntity body, ResourceLocation id) {
        for (Holder<Attribute> attribute : GROWS) {
            AttributeInstance instance = body.getAttribute(attribute);
            if (instance != null) {
                instance.removeModifier(id);
            }
        }
    }
}
