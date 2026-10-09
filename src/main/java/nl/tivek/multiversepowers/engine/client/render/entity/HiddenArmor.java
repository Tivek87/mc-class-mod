package nl.tivek.multiversepowers.engine.client.render.entity;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;

// Whose worn armor is left undrawn: a power that dresses a body in a suit of its own shows the suit over it instead
// (mixin/client/render/HumanoidArmorLayerMixin). The armor still protects.
public final class HiddenArmor {
    private static final List<Rule> RULES = new ArrayList<>();

    public interface Rule {
        boolean hides(LivingEntity entity, float partialTick);
    }

    private HiddenArmor() {
    }

    public static void when(Rule rule) {
        RULES.add(rule);
    }

    public static boolean hidden(LivingEntity entity, float partialTick) {
        for (Rule rule : RULES) {
            if (rule.hides(entity, partialTick)) {
                return true;
            }
        }
        return false;
    }
}
