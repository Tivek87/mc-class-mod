package nl.tivek.multiversepowers.character.docock;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.Characters;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.entity.HeldMobs;
import nl.tivek.multiversepowers.stamina.StaminaCostPayload;

@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class OctopusArms {
    static final ResourceLocation REACH_ID = id("tentacle_reach");
    static final ResourceLocation BLOCK_REACH_ID = id("tentacle_block_reach");
    public static final ResourceLocation BLOCKING_ID = id("tentacle_block_slow");
    static final ResourceLocation RAMPAGE_ID = id("octopus_rampage");
    static final ResourceLocation STEP_ID = id("tentacle_step");
    static final ResourceLocation LEG_RUN_ID = id("tentacle_run");
    // Normal reach is 3 blocks for hitting and 4.5 for blocks.
    static final double REACH_BONUS = 4.5;
    static final double BLOCK_REACH_BONUS = 2.0;
    static final double STEP_BONUS = 1.0;
    private static final float BLOCK_STAMINA_PER_DAMAGE = 2.0F;
    private static final int SET_DOWN_SAFE = 80;

    private static final Map<UUID, OctoRig> RIGS = new HashMap<>();
    private static final Map<UUID, Integer> SAFE_FALL = new HashMap<>();

    static {
        HeldMobs.addHolder(OctopusArms::isHeld);
    }

    private OctopusArms() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, path);
    }

    public static void armsOut(ServerPlayer player, ServerLevel level) {
        OctoRig old = RIGS.get(player.getUUID());
        if (old != null) {
            old.fold();
        }
        OctoRig rig = new OctoRig(player, level);
        RIGS.put(player.getUUID(), rig);
        Effects.start(level, rig);
        modifier(player, Attributes.ENTITY_INTERACTION_RANGE, REACH_ID,
                REACH_BONUS, AttributeModifier.Operation.ADD_VALUE, true);
        modifier(player, Attributes.BLOCK_INTERACTION_RANGE,
                BLOCK_REACH_ID, BLOCK_REACH_BONUS, AttributeModifier.Operation.ADD_VALUE, true);
        modifier(player, Attributes.STEP_HEIGHT, STEP_ID, STEP_BONUS, AttributeModifier.Operation.ADD_VALUE, true);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PISTON_EXTEND,
                SoundSource.PLAYERS, 1.0F, 0.6F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CHAIN_PLACE,
                SoundSource.PLAYERS, 1.0F, 0.8F);
        player.displayClientMessage(Component.translatable("octopus." + MultiversePowers.MODID + ".out"), true);
    }

    public static void armsIn(ServerPlayer player) {
        OctoRig rig = RIGS.get(player.getUUID());
        if (rig != null) {
            rig.fold();
        }
    }

    public static boolean use(ServerPlayer player, CharacterAbility ability, boolean on, int data) {
        OctoRig rig = RIGS.get(player.getUUID());
        if (rig == null || rig.isFolding()) {
            if (on) {
                player.displayClientMessage(Component.translatable("octopus." + MultiversePowers.MODID + ".not_out"),
                        true);
            }
            return false;
        }
        ServerLevel level = player.serverLevel();
        boolean sneaking = (data & Characters.SNEAKING) != 0;
        return switch (ability.id()) {
            case "grab" -> {
                if (sneaking) {
                    rig.letGoAll();
                    yield false;
                }
                yield rig.grab(level);
            }
            case "multi_tentacle" -> rig.multiStrike(level);
            case "dash" -> rig.dash(level);
            case "block" -> {
                rig.setBlocking(on);
                yield false;
            }
            case "ground_slam" -> rig.heavy(level, sneaking);
            case "portal" -> rig.startPortal(level);
            case "rampage" -> rig.rampage(level);
            case "placeholder", "placeholder_2" -> {
                if (on) {
                    player.displayClientMessage(
                            Component.translatable("octopus." + MultiversePowers.MODID + ".placeholder"), true);
                }
                yield false;
            }
            case "stance" -> rig.cycleStance(sneaking);
            case "ground_strike" -> {
                if (sneaking) {
                    rig.clearMarks();
                    yield false;
                }
                yield rig.groundStrike(level);
            }
            default -> false;
        };
    }

    public static void climb(ServerPlayer player, boolean on, int faceIndex) {
        OctoRig rig = RIGS.get(player.getUUID());
        if (rig != null) {
            Direction face = faceIndex >= 0 && faceIndex < Direction.values().length
                    ? Direction.values()[faceIndex]
                    : Direction.NORTH;
            rig.setClimbing(on, face);
        }
    }

    public static void placeBlocks(ServerPlayer player) {
        OctoRig rig = RIGS.get(player.getUUID());
        if (rig != null && !rig.isFolding()) {
            rig.placeBlocks(player.serverLevel());
        }
    }

    public static void throwHeld(ServerPlayer player) {
        OctoRig rig = RIGS.get(player.getUUID());
        if (rig != null) {
            rig.requestThrow();
        }
    }

    public static boolean isHeld(Entity entity) {
        for (OctoRig rig : RIGS.values()) {
            if (rig.holds(entity)) {
                return true;
            }
        }
        return false;
    }

    public static int ultimateLeft(ServerPlayer player) {
        OctoRig rig = RIGS.get(player.getUUID());
        return rig == null ? 0 : rig.rampageLeft();
    }

    public static int legCount(ServerPlayer player) {
        OctoRig rig = RIGS.get(player.getUUID());
        return rig == null || rig.isFolding() ? 0 : rig.legCount();
    }

    public static int markCount(ServerPlayer player) {
        OctoRig rig = RIGS.get(player.getUUID());
        return rig == null || rig.isFolding() ? 0 : rig.markCount();
    }

    public static void clear() {
        for (OctoRig rig : RIGS.values().toArray(new OctoRig[0])) {
            rig.shutDown(rig.level());
        }
        RIGS.clear();
        SAFE_FALL.clear();
        RobotArm.clear();
    }

    static void removed(ServerPlayer player, OctoRig rig) {
        // Only the player's current arms take their powers with them: an old pair that finishes
        // folding in after you turned into Doctor Octopus again leaves the new pair alone.
        if (!RIGS.remove(player.getUUID(), rig)) {
            return;
        }
        SAFE_FALL.remove(player.getUUID());
        modifier(player, Attributes.ENTITY_INTERACTION_RANGE, REACH_ID,
                0, AttributeModifier.Operation.ADD_VALUE, false);
        modifier(player, Attributes.BLOCK_INTERACTION_RANGE,
                BLOCK_REACH_ID, 0, AttributeModifier.Operation.ADD_VALUE, false);
        modifier(player, Attributes.MOVEMENT_SPEED, BLOCKING_ID, 0,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, false);
        modifier(player, Attributes.ATTACK_SPEED, RAMPAGE_ID, 0,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, false);
        modifier(player, Attributes.STEP_HEIGHT, STEP_ID, 0, AttributeModifier.Operation.ADD_VALUE, false);
        modifier(player, Attributes.MOVEMENT_SPEED, LEG_RUN_ID, 0,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, false);
        Characters.lost(player, GameCharacter.DOC_OCK);
        sync(player);
    }

    static void modifier(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id, double amount,
            AttributeModifier.Operation operation, boolean on) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        if (on) {
            instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, operation));
        } else {
            instance.removeModifier(id);
        }
    }

    static void sync(ServerPlayer player) {
        if (!player.hasDisconnected()) {
            Characters.sync(player);
        }
    }

    static void safeFall(ServerPlayer player, int ticks) {
        SAFE_FALL.put(player.getUUID(), player.server.getTickCount() + ticks);
    }

    static void setDown(LivingEntity target) {
        if (target instanceof ServerPlayer player && player.isAlive() && !player.isRemoved()) {
            player.resetFallDistance();
            safeFall(player, SET_DOWN_SAFE);
        }
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getTarget() instanceof LivingEntity target)) {
            return;
        }
        OctoRig rig = RIGS.get(player.getUUID());
        if (rig != null && rig.meleeStrike(target)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        OctoRig rig = RIGS.get(player.getUUID());
        Vec3 from = event.getSource().getSourcePosition();
        if (rig == null || !rig.isBlocking() || from == null) {
            return;
        }
        Vec3 look = new Vec3(player.getLookAngle().x, 0, player.getLookAngle().z);
        Vec3 toSource = new Vec3(from.x - player.getX(), 0, from.z - player.getZ());
        if (look.lengthSqr() < 1.0E-4 || toSource.lengthSqr() < 1.0E-4
                || look.normalize().dot(toSource.normalize()) < 0.1) {
            return;
        }
        float kept = (float) OctoRig.ability("block").value("damageKept");
        float blocked;
        if (event.getSource().getDirectEntity() instanceof Projectile) {
            blocked = event.getAmount();
            event.setCanceled(true);
        } else {
            blocked = event.getAmount() * (1.0F - kept);
            event.setAmount(event.getAmount() * kept);
        }
        rig.blocked(player.serverLevel(), from);
        PacketDistributor.sendToPlayer(player, new StaminaCostPayload(blocked * BLOCK_STAMINA_PER_DAMAGE));
    }

    @SubscribeEvent
    public static void onKnockBack(LivingKnockBackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            OctoRig rig = RIGS.get(player.getUUID());
            if (rig != null && rig.justBlocked()) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        OctoRig rig = RIGS.get(player.getUUID());
        Integer until = SAFE_FALL.get(player.getUUID());
        if ((rig != null && (rig.isClimbing() || rig.legCount() > 0))
                || (until != null && until >= player.server.getTickCount())) {
            SAFE_FALL.remove(player.getUUID());
            event.setDamageMultiplier(0.0F);
        }
    }

}
