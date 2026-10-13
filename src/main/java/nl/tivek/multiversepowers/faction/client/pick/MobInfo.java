package nl.tivek.multiversepowers.faction.client.pick;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.fml.ModList;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.faction.rules.FactionRules;

// What a list of creatures tells of each kind: its name, the mod it is from, a line on what it is and the numbers
// that matter (health, attack, armor, speed), read from the kind itself so it works with no world open.
public final class MobInfo {
    private static final String KEY = "mob." + MultiversePowers.MODID + ".";
    @Nullable
    private static List<Info> all;

    // `health`, `damage`, `armor` and `speed`: NaN where the kind has none.
    public record Info(EntityType<?> type, String id, Component name, String mod, ItemStack icon, double health,
            double damage, double armor, double speed) {
        public Component modName() {
            return MobInfo.modName(this.mod);
        }

        // What it is, in a line: its own text, or one made from its sort and mod.
        public Component about() {
            String key = KEY + "about." + this.id.replace(':', '.');
            return I18n.exists(key) ? Component.translatable(key)
                    : Component.translatable(KEY + "about.any", this.sort(), this.modName());
        }

        public Component sort() {
            return Component.translatable(KEY + "sort." + this.type.getCategory().getName()
                    .toLowerCase(Locale.ROOT));
        }

        public boolean monster() {
            return this.type.getCategory() == MobCategory.MONSTER;
        }

        public boolean matches(String wanted) {
            String query = wanted.strip().toLowerCase(Locale.ROOT);
            return query.isEmpty() || this.name.getString().toLowerCase(Locale.ROOT).contains(query)
                    || this.id.contains(query);
        }
    }

    private MobInfo() {
    }

    // Every kind of creature the game has, modded ones too, by name.
    public static synchronized List<Info> all() {
        if (all == null) {
            List<Info> infos = new ArrayList<>();
            for (EntityType<?> type : FactionRules.kinds()) {
                infos.add(of(type));
            }
            infos.sort(Comparator.comparing(info -> info.name().getString().toLowerCase(Locale.ROOT)));
            all = List.copyOf(infos);
        }
        return all;
    }

    @Nullable
    public static Info of(String id) {
        EntityType<?> type = FactionRules.type(id);
        return type == null ? null : of(type);
    }

    @SuppressWarnings("unchecked")
    public static Info of(EntityType<?> type) {
        String id = FactionRules.id(type);
        SpawnEggItem egg = SpawnEggItem.byId(type);
        AttributeSupplier attributes = DefaultAttributes.hasSupplier(type)
                ? DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>) type) : null;
        return new Info(type, id, type.getDescription(), id.substring(0, id.indexOf(':')),
                egg != null ? new ItemStack(egg) : new ItemStack(Items.NAME_TAG),
                base(attributes, Attributes.MAX_HEALTH), base(attributes, Attributes.ATTACK_DAMAGE),
                base(attributes, Attributes.ARMOR), base(attributes, Attributes.MOVEMENT_SPEED));
    }

    private static double base(@Nullable AttributeSupplier attributes, Holder<Attribute> attribute) {
        return attributes != null && attributes.hasAttribute(attribute) ? attributes.getBaseValue(attribute)
                : Double.NaN;
    }

    public static Component modName(String namespace) {
        if (namespace.equals("minecraft")) {
            return Component.literal("Minecraft");
        }
        return Component.literal(ModList.get().getModContainerById(namespace)
                .map(mod -> mod.getModInfo().getDisplayName()).orElse(namespace));
    }

    // A number of health or damage as hearts, a half shown as .5.
    public static String hearts(double points) {
        double hearts = points / 2.0;
        return hearts == Math.rint(hearts) ? String.valueOf((long) hearts) : String.format(Locale.ROOT, "%.1f", hearts);
    }
}
