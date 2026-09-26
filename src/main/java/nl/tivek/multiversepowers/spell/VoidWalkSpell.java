package nl.tivek.multiversepowers.spell;

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
import net.minecraft.server.MinecraftServer;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.effect.Effect;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.fx.ParticleBatch;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.faction.Factions;

final class VoidWalkSpell {
    static final int DURATION = 200;
    static final double MARK_RADIUS = 32.0;
    private static final double SPEED_BONUS = 0.5;
    private static final float AMBUSH = 1.5F;
    private static final int DAZED = 40;

    private static final ResourceLocation SPEED_ID =
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "void_walk_speed");
    private static final EquipmentSlot[] WORN = {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private static final int VIOLET = 0x9B5CFF;
    private static final int PALE = 0xD9B8FF;
    private static final int ABYSS = 0x0A0012;

    private static final Map<UUID, Walk> ACTIVE = new HashMap<>();

    private VoidWalkSpell() {
    }

    private static final class Walk {
        private final boolean wasSilent;
        @Nullable
        private final MobEffectInstance invisibility;
        private final int since;
        private boolean equipmentChanged;

        private Walk(boolean wasSilent, @Nullable MobEffectInstance invisibility, int since) {
            this.wasSilent = wasSilent;
            this.invisibility = invisibility;
            this.since = since;
        }
    }

    static boolean isInVoid(@Nullable Entity entity) {
        return entity != null && ACTIVE.containsKey(entity.getUUID());
    }

    static boolean cast(ServerPlayer player, ServerLevel level) {
        if (isInVoid(player)) {
            return false;
        }
        Vec3 at = player.position();
        ParticleFx.implosion(level, ParticleTypes.PORTAL, at.add(0, 1.0, 0), 3.0, 50, 0.6);
        SpellFxPayload.send(level, SpellFxPayload.VOID_IN, at, at, -1, 0);
        Effects.start(level, explosion(at));
        enter(player, level);
        Effects.start(level, (lvl, age) -> tick(player, age));
        return true;
    }

    private static void enter(ServerPlayer player, ServerLevel level) {
        MobEffectInstance invisibility = player.getEffect(MobEffects.INVISIBILITY);
        ACTIVE.put(player.getUUID(), new Walk(player.isSilent(),
                invisibility == null ? null : new MobEffectInstance(invisibility), player.server.getTickCount()));
        player.setSilent(true);
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, DURATION + 5, 0, false, false, true));
        addModifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID, SPEED_BONUS);
        hideEquipment(player);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(64),
                mob -> mob.getTarget() == player)) {
            mob.setTarget(null);
        }
        PacketDistributor.sendToPlayer(player, new VoidStatePayload(DURATION));
    }

    private static boolean tick(ServerPlayer player, int age) {
        Walk walk = ACTIVE.get(player.getUUID());
        if (walk == null) {
            return false;
        }
        // Milk or a light removing invisibility also ends the void walk early.
        if (player.isRemoved() || !player.isAlive() || age >= DURATION || !player.hasEffect(MobEffects.INVISIBILITY)) {
            leave(player);
            return false;
        }
        if (walk.equipmentChanged) {
            walk.equipmentChanged = false;
            hideEquipment(player);
        }
        if (age % 10 == 0) {
            markEnemies(player);
        }
        whispers(player, age);
        return true;
    }

    // Only the walker sees faint wisps where the feet fall, and hears a warning two seconds before it ends.
    private static void whispers(ServerPlayer player, int age) {
        if (age == DURATION - 40) {
            player.playNotifySound(SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.5F, 1.6F);
        }
        if (age % 3 == 0 && player.getDeltaMovement().horizontalDistanceSqr() > 1.0E-3) {
            ParticleBatch.add(player, ParticleTypes.REVERSE_PORTAL, false, player.getX(), player.getY() + 0.1,
                    player.getZ(), 2, 0.15, 0.02, 0.15, 0.01);
        }
    }

    // The first blow struck out of the void lands harder and leaves the victim reeling; it also pulls the walker out.
    static void ambush(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || event.getSource().getDirectEntity()
                != player || !isInVoid(player) || event.getEntity() == player) {
            return;
        }
        LivingEntity victim = event.getEntity();
        event.setAmount(event.getAmount() * AMBUSH);
        victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, DAZED, 0), player);
        victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DAZED, 1), player);
        ServerLevel level = player.serverLevel();
        Vec3 hit = victim.getBoundingBox().getCenter();
        SpellFxPayload.send(level, SpellFxPayload.AMBUSH, hit, player.getEyePosition(), -1, 0);
        ParticleFx.sphereOut(level, ParticleTypes.REVERSE_PORTAL, hit, 30, 0.3);
        ParticleFx.cloud(level, ParticleTypes.SQUID_INK, hit, 10, 0.3, 0.05);
        ParticleFx.cloud(level, ParticleFx.dust(VIOLET, 1.4F), hit, 14, 0.4, 0.0);
        level.playSound(null, hit.x, hit.y, hit.z, SoundEvents.PHANTOM_BITE, SoundSource.PLAYERS, 1.0F, 0.6F);
        level.playSound(null, hit.x, hit.y, hit.z, SoundEvents.ENDERMAN_SCREAM, SoundSource.PLAYERS, 0.4F, 1.6F);
        leave(player);
    }

    static void leave(ServerPlayer player) {
        Walk walk = ACTIVE.remove(player.getUUID());
        if (walk == null) {
            return;
        }
        restore(player, walk);
        if (player.isAlive()) {
            SpellFxPayload.send(player.serverLevel(), SpellFxPayload.VOID_OUT, player.position(), player.position(),
                    -1, 0);
            Effects.start(player.serverLevel(), reappear(player.position()));
        }
    }

    static void clear(MinecraftServer server) {
        Map<UUID, Walk> walks = new HashMap<>(ACTIVE);
        ACTIVE.clear();
        walks.forEach((id, walk) -> {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) {
                restore(player, walk);
            }
        });
    }

    private static void restore(ServerPlayer player, Walk walk) {
        player.setSilent(walk.wasSilent);
        removeModifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID);
        // Restore any earlier invisibility potion with its time left, if still there.
        if (player.hasEffect(MobEffects.INVISIBILITY)) {
            player.removeEffect(MobEffects.INVISIBILITY);
            MobEffectInstance before = walk.invisibility;
            if (before != null) {
                int left = before.getDuration() - (player.server.getTickCount() - walk.since);
                if (before.isInfiniteDuration() || left > 0) {
                    player.addEffect(new MobEffectInstance(before.getEffect(),
                            before.isInfiniteDuration() ? MobEffectInstance.INFINITE_DURATION : left,
                            before.getAmplifier(), before.isAmbient(), before.isVisible(), before.showIcon()));
                }
            }
        }
        List<Pair<EquipmentSlot, ItemStack>> real = new ArrayList<>();
        for (EquipmentSlot slot : WORN) {
            real.add(Pair.of(slot, player.getItemBySlot(slot).copy()));
        }
        player.serverLevel().getChunkSource().broadcast(player,
                new ClientboundSetEquipmentPacket(player.getId(), real));
        PacketDistributor.sendToPlayer(player, new VoidStatePayload(0));
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

    private static void hideEquipment(ServerPlayer player) {
        player.serverLevel().getChunkSource().broadcast(player, emptyEquipment(player));
    }

    static void seenBy(ServerPlayer player, ServerPlayer viewer) {
        if (isInVoid(player)) {
            viewer.connection.send(emptyEquipment(player));
        }
    }

    static void equipmentChanged(ServerPlayer player) {
        Walk walk = ACTIVE.get(player.getUUID());
        if (walk != null) {
            walk.equipmentChanged = true;
        }
    }

    private static ClientboundSetEquipmentPacket emptyEquipment(ServerPlayer player) {
        List<Pair<EquipmentSlot, ItemStack>> empty = new ArrayList<>();
        for (EquipmentSlot slot : WORN) {
            empty.add(Pair.of(slot, ItemStack.EMPTY));
        }
        return new ClientboundSetEquipmentPacket(player.getId(), empty);
    }

    private static boolean isEnemy(Entity entity, ServerPlayer caster) {
        return entity.isAlive() && !entity.isSpectator() && Factions.hostile(caster, entity);
    }

    private static void markEnemies(ServerPlayer caster) {
        ServerLevel level = caster.serverLevel();
        for (LivingEntity enemy : level.getEntitiesOfClass(LivingEntity.class,
                caster.getBoundingBox().inflate(MARK_RADIUS), entity -> isEnemy(entity, caster))) {
            if (enemy.distanceTo(caster) > MARK_RADIUS) {
                continue;
            }
            double top = enemy.getY() + enemy.getBbHeight() + 0.5;
            level.sendParticles(caster, ParticleFx.dust(VIOLET, 1.2F), true, enemy.getX(), top, enemy.getZ(),
                    8, 0.15, 0.08, 0.15, 0);
            level.sendParticles(caster, ParticleTypes.REVERSE_PORTAL, true, enemy.getX(), top, enemy.getZ(),
                    3, 0.1, 0.1, 0.1, 0.02);
        }
    }

    private static Effect explosion(Vec3 feet) {
        Vec3 core = feet.add(0, 1.0, 0);
        return (level, age) -> {
            if (age == 0) {
                ParticleFx.at(level, ParticleTypes.SONIC_BOOM, core);
                ParticleFx.sphereOut(level, ParticleTypes.REVERSE_PORTAL, core, 80, 0.6);
                ParticleFx.sphereOut(level, ParticleTypes.SQUID_INK, core, 40, 0.35);
                ParticleFx.sphere(level, ParticleFx.dust(ABYSS, 3.0F), core, 1.1, 30, 0);
                ParticleFx.sphere(level, ParticleFx.dust(VIOLET, 1.5F), core, 0.7, 20, 0.5);
                ParticleFx.cloud(level, ParticleTypes.PORTAL, core, 60, 1.2, 1.0);
                ParticleFx.shockwave(level, ParticleTypes.LARGE_SMOKE, feet.add(0, 0.2, 0), 32, 0.3);
                ParticleFx.shockwave(level, ParticleTypes.SCULK_SOUL, feet.add(0, 0.3, 0), 16, 0.2);
                level.playSound(null, feet.x, feet.y, feet.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS,
                        1.0F, 0.5F);
                level.playSound(null, feet.x, feet.y, feet.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS,
                        0.5F, 1.6F);
                level.playSound(null, feet.x, feet.y, feet.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS,
                        1.0F, 0.6F);
                level.playSound(null, feet.x, feet.y, feet.z, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(),
                        SoundSource.PLAYERS, 0.8F, 0.7F);
            }
            if (age <= 10) {
                double radius = 0.6 + age * 0.5;
                ParticleFx.ring(level, ParticleFx.fade(PALE, ABYSS, 1.6F - age * 0.1F), feet.add(0, 0.1, 0), radius,
                        20 + age * 4, age * 0.25);
                ParticleFx.ring(level, ParticleFx.dust(VIOLET, 1.0F), core, radius * 0.7, 12 + age * 2, -age * 0.3);
            }
            if (age > 2 && age < 25 && ParticleFx.chance(1.0 - age / 25.0)) {
                ParticleFx.fly(level, ParticleTypes.SMOKE,
                        feet.add(ParticleFx.spread(1.0), 0.1, ParticleFx.spread(1.0)), new Vec3(0, 1, 0), 0.03);
            }
            return age < 25;
        };
    }

    private static Effect reappear(Vec3 feet) {
        Vec3 core = feet.add(0, 1.0, 0);
        return (level, age) -> {
            if (age == 0) {
                ParticleFx.at(level, ParticleTypes.FLASH, core);
                ParticleFx.implosion(level, ParticleTypes.PORTAL, core, 2.0, 30, 0.4);
                ParticleFx.sphereOut(level, ParticleTypes.REVERSE_PORTAL, core, 40, 0.35);
                ParticleFx.cloud(level, ParticleTypes.SQUID_INK, core, 16, 0.4, 0.05);
                ParticleFx.sphere(level, ParticleFx.dust(VIOLET, 1.4F), core, 0.9, 24, 0);
                level.playSound(null, feet.x, feet.y, feet.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS,
                        0.8F, 0.8F);
                level.playSound(null, feet.x, feet.y, feet.z, SoundEvents.ILLUSIONER_MIRROR_MOVE,
                        SoundSource.PLAYERS, 1.0F, 0.7F);
            }
            if (age <= 6) {
                ParticleFx.ring(level, ParticleFx.fade(PALE, ABYSS, 1.2F), feet.add(0, 0.1, 0), 1.6 - age * 0.2,
                        24, age * 0.4);
            }
            return age < 6;
        };
    }
}
