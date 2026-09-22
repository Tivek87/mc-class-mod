package nl.tivek.welcomescreen.spell;

import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.welcomescreen.WelcomeScreenMod;
import nl.tivek.welcomescreen.network.VoidStatePayload;

/**
 * Void Walk: a dark implosion swallows you and you step into the void for 10 seconds. Nobody can
 * see or hear you and no creature can target you; you run faster and hit a little harder, and every
 * enemy around you is marked. Your own client turns the world into black silhouettes meanwhile.
 */
final class VoidWalkSpell {
    static final int DURATION = 200;
    /** Enemies (hostile mobs and other players) this close are marked for the caster. */
    static final double MARK_RADIUS = 32.0;
    private static final double SPEED_BONUS = 0.5;
    private static final double DAMAGE_BONUS = 0.2;

    private static final ResourceLocation SPEED_ID =
            ResourceLocation.fromNamespaceAndPath(WelcomeScreenMod.MODID, "void_walk_speed");
    private static final ResourceLocation DAMAGE_ID =
            ResourceLocation.fromNamespaceAndPath(WelcomeScreenMod.MODID, "void_walk_damage");
    private static final EquipmentSlot[] WORN = {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private static final int VIOLET = 0x9B5CFF;
    private static final int PALE = 0xD9B8FF;
    private static final int ABYSS = 0x0A0012;

    // Players in the void right now, with whether they were already silent before.
    private static final Map<UUID, Boolean> ACTIVE = new HashMap<>();

    private VoidWalkSpell() {
    }

    static boolean isInVoid(@Nullable Entity entity) {
        return entity != null && ACTIVE.containsKey(entity.getUUID());
    }

    static boolean cast(ServerPlayer player, ServerLevel level) {
        if (isInVoid(player)) {
            return false;
        }
        Vec3 at = player.position();
        SpellCasting.start(level, explosion(at));
        enter(player, level);
        SpellCasting.start(level, (lvl, age) -> tick(player, lvl, age));
        return true;
    }

    private static void enter(ServerPlayer player, ServerLevel level) {
        ACTIVE.put(player.getUUID(), player.isSilent());
        player.setSilent(true);
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, DURATION + 5, 0, false, false, true));
        addModifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID, SPEED_BONUS);
        addModifier(player, Attributes.ATTACK_DAMAGE, DAMAGE_ID, DAMAGE_BONUS);
        hideEquipment(player, level);
        // Everything that was hunting you loses you.
        for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(64),
                mob -> mob.getTarget() == player)) {
            mob.setTarget(null);
        }
        PacketDistributor.sendToPlayer(player, new VoidStatePayload(DURATION));
    }

    private static boolean tick(ServerPlayer player, ServerLevel level, int age) {
        if (!isInVoid(player)) {
            return false;
        }
        if (player.isRemoved() || !player.isAlive() || age >= DURATION) {
            leave(player);
            return false;
        }
        // Switching items would show them again, so keep telling everyone your hands are empty.
        hideEquipment(player, player.serverLevel());
        if (age % 10 == 0) {
            markEnemies(player);
        }
        return true;
    }

    /** Back into the world: everything undone, with a burst where you reappear. */
    static void leave(ServerPlayer player) {
        Boolean wasSilent = ACTIVE.remove(player.getUUID());
        if (wasSilent == null) {
            return;
        }
        player.setSilent(wasSilent);
        removeModifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID);
        removeModifier(player, Attributes.ATTACK_DAMAGE, DAMAGE_ID);
        player.removeEffect(MobEffects.INVISIBILITY);
        ServerLevel level = player.serverLevel();
        List<Pair<EquipmentSlot, ItemStack>> real = new ArrayList<>();
        for (EquipmentSlot slot : WORN) {
            real.add(Pair.of(slot, player.getItemBySlot(slot).copy()));
        }
        level.getChunkSource().broadcast(player, new ClientboundSetEquipmentPacket(player.getId(), real));
        PacketDistributor.sendToPlayer(player, new VoidStatePayload(0));
        if (player.isAlive()) {
            SpellCasting.start(level, reappear(player.position()));
        }
    }

    static void clear() {
        ACTIVE.clear();
    }

    private static void addModifier(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id,
                                    double amount) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(id);
            instance.addTransientModifier(new AttributeModifier(id, amount,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private static void removeModifier(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(id);
        }
    }

    /** Tells every other player you hold and wear nothing, so not even your armour floats in the air. */
    private static void hideEquipment(ServerPlayer player, ServerLevel level) {
        List<Pair<EquipmentSlot, ItemStack>> empty = new ArrayList<>();
        for (EquipmentSlot slot : WORN) {
            empty.add(Pair.of(slot, ItemStack.EMPTY));
        }
        level.getChunkSource().broadcast(player, new ClientboundSetEquipmentPacket(player.getId(), empty));
    }

    static boolean isEnemy(Entity entity, Player caster) {
        if (entity == caster || !entity.isAlive() || entity.isSpectator()) {
            return false;
        }
        return entity instanceof Enemy || entity instanceof Player;
    }

    /** A purple mark above every enemy's head that only the caster can see. */
    private static void markEnemies(ServerPlayer caster) {
        ServerLevel level = caster.serverLevel();
        for (LivingEntity enemy : level.getEntitiesOfClass(LivingEntity.class,
                caster.getBoundingBox().inflate(MARK_RADIUS), entity -> isEnemy(entity, caster))) {
            if (enemy.distanceTo(caster) > MARK_RADIUS) {
                continue;
            }
            double top = enemy.getY() + enemy.getBbHeight() + 0.5;
            level.sendParticles(caster, SpellFx.dust(VIOLET, 1.2F), true, enemy.getX(), top, enemy.getZ(),
                    8, 0.15, 0.08, 0.15, 0);
            level.sendParticles(caster, ParticleTypes.REVERSE_PORTAL, true, enemy.getX(), top, enemy.getZ(),
                    3, 0.1, 0.1, 0.1, 0.02);
        }
    }

    /** The dark explosion when you step into the void: ink and portal matter blasting out, a ring of night. */
    private static SpellEffect explosion(Vec3 feet) {
        Vec3 core = feet.add(0, 1.0, 0);
        return (level, age) -> {
            if (age == 0) {
                SpellFx.at(level, ParticleTypes.SONIC_BOOM, core);
                SpellFx.sphereOut(level, ParticleTypes.REVERSE_PORTAL, core, 80, 0.6);
                SpellFx.sphereOut(level, ParticleTypes.SQUID_INK, core, 40, 0.35);
                SpellFx.sphere(level, SpellFx.dust(ABYSS, 3.0F), core, 1.1, 30, 0);
                SpellFx.sphere(level, SpellFx.dust(VIOLET, 1.5F), core, 0.7, 20, 0.5);
                SpellFx.cloud(level, ParticleTypes.PORTAL, core, 60, 1.2, 1.0);
                SpellFx.shockwave(level, ParticleTypes.LARGE_SMOKE, feet.add(0, 0.2, 0), 32, 0.3);
                SpellFx.shockwave(level, ParticleTypes.SCULK_SOUL, feet.add(0, 0.3, 0), 16, 0.2);
                level.playSound(null, feet.x, feet.y, feet.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS,
                        1.0F, 0.5F);
                level.playSound(null, feet.x, feet.y, feet.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS,
                        0.5F, 1.6F);
                level.playSound(null, feet.x, feet.y, feet.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS,
                        1.0F, 0.6F);
            }
            if (age <= 10) {
                double radius = 0.6 + age * 0.5;
                SpellFx.ring(level, SpellFx.fade(PALE, ABYSS, 1.6F - age * 0.1F), feet.add(0, 0.1, 0), radius,
                        20 + age * 4, age * 0.25);
                SpellFx.ring(level, SpellFx.dust(VIOLET, 1.0F), core, radius * 0.7, 12 + age * 2, -age * 0.3);
            }
            if (age > 2 && age < 25 && SpellFx.chance(1.0 - age / 25.0)) {
                SpellFx.fly(level, ParticleTypes.SMOKE, feet.add(SpellFx.spread(1.0), 0.1, SpellFx.spread(1.0)),
                        new Vec3(0, 1, 0), 0.03);
            }
            return age < 25;
        };
    }

    /** Coming back: the void tears open for a moment and lets you out. */
    private static SpellEffect reappear(Vec3 feet) {
        Vec3 core = feet.add(0, 1.0, 0);
        return (level, age) -> {
            if (age == 0) {
                SpellFx.sphereOut(level, ParticleTypes.REVERSE_PORTAL, core, 40, 0.35);
                SpellFx.cloud(level, ParticleTypes.SQUID_INK, core, 16, 0.4, 0.05);
                SpellFx.sphere(level, SpellFx.dust(VIOLET, 1.4F), core, 0.9, 24, 0);
                level.playSound(null, feet.x, feet.y, feet.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS,
                        0.8F, 0.8F);
                level.playSound(null, feet.x, feet.y, feet.z, SoundEvents.ILLUSIONER_MIRROR_MOVE,
                        SoundSource.PLAYERS, 1.0F, 0.7F);
            }
            if (age <= 6) {
                SpellFx.ring(level, SpellFx.fade(PALE, ABYSS, 1.2F), feet.add(0, 0.1, 0), 1.6 - age * 0.2,
                        24, age * 0.4);
            }
            return age < 6;
        };
    }
}
