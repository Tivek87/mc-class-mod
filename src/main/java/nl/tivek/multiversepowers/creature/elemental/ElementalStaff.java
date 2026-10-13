package nl.tivek.multiversepowers.creature.elemental;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;

// The Elemental Staff, made of all five of the Elemental Blaze's rods: it casts whichever of the Blaze's elements it
// is set to (use), and sneaking and using it turns it to the next. Each element has its own cooldown and every cast
// wears the staff a little; blaze rods mend it. It hurts anyone but its holder and their own faction, its lightning
// leaping on only to who is red to them.
public final class ElementalStaff extends Item {
    private static final String ELEMENT = "Element";
    private static final int FROST_LIFE = 16;
    private static final int LIFT = 8;
    private static final double SEEK_CONE = 0.97;

    public ElementalStaff(Properties properties) {
        super(properties.attributes(ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 4.0,
                        AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -2.6,
                        AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build()));
    }

    public static Element element(ItemStack stack) {
        return Element.of(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt(ELEMENT));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Element element = element(stack);
        if (player.isSecondaryUseActive()) {
            Element next = Element.of(element.ordinal() + 1);
            if (!level.isClientSide) {
                CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(ELEMENT, next.ordinal()));
                player.displayClientMessage(Component.translatable("item." + MultiversePowers.MODID
                        + ".elemental_staff.turned", Component.translatable(next.key()).withColor(next.light)), true);
            }
            level.playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS, 1.0F, 0.8F + next.ordinal() * 0.15F);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (player instanceof ServerPlayer caster && level instanceof ServerLevel server) {
            cast(server, caster, element);
            stack.hurtAndBreak(1, caster, LivingEntity.getSlotForHand(hand));
        }
        player.getCooldowns().addCooldown(this, element.staffCooldown);
        player.awardStat(Stats.ITEM_USED.get(this));
        player.swing(hand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static void cast(ServerLevel level, ServerPlayer player, Element element) {
        Vec3 tip = BlazeCasts.tip(player, 1.0F);
        ParticleFx.send(level, ParticleFx.dust(element.light, 0.5F), tip.x, tip.y, tip.z, 4, 0.08, 0.08, 0.08, 0.0);
        switch (element) {
            case ICE -> BlazeCasts.frost(level, player, FROST_LIFE);
            case EARTH -> BlazeCasts.heave(level, player, LIFT);
            case FIRE -> BlazeCasts.scorch(level, player);
            case WIND -> BlazeCasts.gale(level, player);
            case STORM -> {
                Vec3 eye = player.getEyePosition();
                Vec3 far = eye.add(player.getLookAngle().scale(BlazeCasts.RANGE));
                Vec3 end = LoadedWorld.clip(level, new ClipContext(eye, far, ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE, player)).getLocation();
                BlazeCasts.chain(level, player, first(level, player, eye, end), end);
            }
        }
        level.playSound(null, tip.x, tip.y, tip.z, SoundEvents.BLAZE_AMBIENT, SoundSource.PLAYERS, 0.5F, 1.6F);
    }

    // What the staff's lightning strikes first: what the holder looks straight at, else the nearest foe of theirs
    // close to that line.
    @Nullable
    private static LivingEntity first(ServerLevel level, ServerPlayer player, Vec3 eye, Vec3 end) {
        LivingEntity best = null;
        double closest = Double.MAX_VALUE;
        for (Entity entity : level.getEntities(player, new AABB(eye, end).inflate(1.5),
                entity -> BlazeCasts.hits(player, entity))) {
            Optional<Vec3> hit = entity.getBoundingBox().inflate(0.4).clip(eye, end);
            if (hit.isPresent() && hit.get().distanceToSqr(eye) < closest) {
                closest = hit.get().distanceToSqr(eye);
                best = (LivingEntity) entity;
            }
        }
        if (best != null) {
            return best;
        }
        Vec3 look = player.getLookAngle();
        double nearest = Double.MAX_VALUE;
        for (Entity entity : level.getEntities(player, player.getBoundingBox().inflate(BlazeCasts.RANGE),
                entity -> BlazeCasts.seeks(player, entity))) {
            Vec3 to = entity.getBoundingBox().getCenter().subtract(eye);
            double far = to.length();
            if (far < BlazeCasts.RANGE && far > 1.0E-3 && to.scale(1.0 / far).dot(look) > SEEK_CONE && far < nearest
                    && player.hasLineOfSight(entity)) {
                nearest = far;
                best = (LivingEntity) entity;
            }
        }
        return best;
    }

    @Override
    public Component getName(ItemStack stack) {
        Element element = element(stack);
        return Component.translatable(this.getDescriptionId() + ".named", Component.translatable(this
                .getDescriptionId()), Component.translatable(element.key()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Element element = element(stack);
        String key = "item." + MultiversePowers.MODID + ".elemental_staff.";
        tooltip.add(Component.translatable("ability." + MultiversePowers.MODID + ".elemental_blaze." + element.id)
                .withColor(element.light));
        tooltip.add(Component.translatable("ability." + MultiversePowers.MODID + ".elemental_blaze." + element.id
                + ".desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(key + "use").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return repair.is(Items.BLAZE_ROD);
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 12;
    }

    // The holder's staff sheds a little of its element.
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide || !selected || level.random.nextInt(5) != 0 || !(entity instanceof LivingEntity)) {
            return;
        }
        Element element = element(stack);
        Vec3 tip = BlazeCasts.tip(entity, 1.0F).add(0.0, 0.25, 0.0);
        level.addParticle(element.particle(), tip.x, tip.y, tip.z, 0.0, element == Element.FIRE ? 0.01 : 0.0, 0.0);
    }
}
