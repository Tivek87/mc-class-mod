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

/**
 * Doctor Octopus: four robot tentacles from your back. You get them by turning into him in the wheel
 * (hold G); turning into someone else, or back into yourself, folds them in. While they are out:
 * <ul>
 * <li><b>Tentacle Reach</b> (always): you hit and use blocks from much farther away.</li>
 * <li><b>Tentacle Strike</b> (always): every melee hit is delivered by a tentacle lashing out.</li>
 * <li><b>Wall Climb</b> (always): walk into a wall to climb it, around edges and on over ceilings.</li>
 * <li><b>Stance</b>: walk on your own feet, or on 2, 3 or 4 tentacles that carry you above the
 * ground. Every tentacle you do not walk on is free for grabbing, fighting and building.</li>
 * <li>Keys (the same keys for every character, see AbilitySlot): Grab, Multi-Tentacle, Dash, Block,
 * Ground Slam, Portal, Rampage, Stance, Ground Strike and two placeholders. Several work at once.</li>
 * </ul>
 * The server decides everything; the client only sends key presses (see AbilityActionPayload).
 */
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
    // The legs simply step over anything this high.
    static final double STEP_BONUS = 1.0;
    private static final float BLOCK_STAMINA_PER_DAMAGE = 2.0F;
    // How long a player the tentacles let fall without meaning to (see setDown) lands unhurt.
    private static final int SET_DOWN_SAFE = 80;

    private static final Map<UUID, OctoRig> RIGS = new HashMap<>();
    // Per player: until which server tick a landing does not hurt (after a dash or an air slam).
    private static final Map<UUID, Integer> SAFE_FALL = new HashMap<>();

    static {
        // The claws also hold players and hold creatures their own way: every other power must know they are taken.
        HeldMobs.addHolder(OctopusArms::isHeld);
    }

    private OctopusArms() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, path);
    }

    // ---- Turning into Doctor Octopus ----

    /** The tentacles unfold out of the player's back. */
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

    /** The tentacles fold back in; they never drop what they hold by themselves. */
    public static void armsIn(ServerPlayer player) {
        OctoRig rig = RIGS.get(player.getUUID());
        if (rig != null) {
            rig.fold();
        }
    }

    // ---- Ability keys ----

    /**
     * One of Doctor Octopus's abilities, whichever key slot it sits in. The character system looks up
     * which ability a key means; this only has to do it.
     *
     * @param on   for a key you hold down: pressed or let go
     * @param data extra from the client, {@link Characters#SNEAKING} while crouching
     * @return true when the ability really ran, so its cooldown should start
     */
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
                    // Crouching is this ability's undo: let go, and never start a cooldown for that.
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
            // Crouching aims the slam straight down at what you hold instead of around you.
            case "ground_slam" -> rig.heavy(level, sneaking);
            case "portal" -> rig.startPortal(level);
            case "rampage" -> rig.rampage(level);
            case "placeholder", "placeholder_2" -> {
                // Nothing on this key yet; it is kept free for the next ability.
                if (on) {
                    player.displayClientMessage(
                            Component.translatable("octopus." + MultiversePowers.MODID + ".placeholder"), true);
                }
                yield false;
            }
            case "stance" -> rig.cycleStance(sneaking);
            case "ground_strike" -> {
                // Crouching is this ability's undo: forget the creatures you marked, free of charge.
                if (sneaking) {
                    rig.clearMarks();
                    yield false;
                }
                yield rig.groundStrike(level);
            }
            default -> false;
        };
    }

    /** The client says the player holds on to a surface (Wall Climb), or let go of it. */
    public static void climb(ServerPlayer player, boolean on, int faceIndex) {
        OctoRig rig = RIGS.get(player.getUUID());
        if (rig != null) {
            Direction face = faceIndex >= 0 && faceIndex < Direction.values().length
                    ? Direction.values()[faceIndex]
                    : Direction.NORTH;
            rig.setClimbing(on, face);
        }
    }

    /** Right click while the tentacles carry blocks: set them down where the player aims. */
    public static void placeBlocks(ServerPlayer player) {
        OctoRig rig = RIGS.get(player.getUUID());
        if (rig != null && !rig.isFolding()) {
            rig.placeBlocks(player.serverLevel());
        }
    }

    /** The attack button while a tentacle holds something: throw it (a creature, or its blocks). */
    public static void throwHeld(ServerPlayer player) {
        OctoRig rig = RIGS.get(player.getUUID());
        if (rig != null) {
            rig.requestThrow();
        }
    }

    // ---- What the rest of the mod asks ----

    /** True when some player's tentacle is holding this creature. */
    public static boolean isHeld(Entity entity) {
        for (OctoRig rig : RIGS.values()) {
            if (rig.holds(entity)) {
                return true;
            }
        }
        return false;
    }

    /** Ticks of Octopus Rampage left, for the HUD. */
    public static int ultimateLeft(ServerPlayer player) {
        OctoRig rig = RIGS.get(player.getUUID());
        return rig == null ? 0 : rig.rampageLeft();
    }

    /** How many tentacles the player walks on right now (0: on their own feet). */
    public static int legCount(ServerPlayer player) {
        OctoRig rig = RIGS.get(player.getUUID());
        return rig == null || rig.isFolding() ? 0 : rig.legCount();
    }

    /** How many creatures the player has marked for a Ground Strike, for the HUD. */
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

    /** Called by a rig that has fully folded in or stopped. */
    static void removed(ServerPlayer player, OctoRig rig) {
        // Only the player's current arms take their powers with them: an old pair that finishes folding
        // in after you turned into Doctor Octopus again leaves the new pair alone.
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
        // The arms can also end on their own (changing dimension): then you are yourself again.
        Characters.lost(player, GameCharacter.DOC_OCK);
        sync(player);
    }

    /** Adds ({@code on}) or removes a temporary attribute change; never saved with the player. */
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

    /** Sends the player everything their client needs to know (through the character system). */
    static void sync(ServerPlayer player) {
        if (!player.hasDisconnected()) {
            Characters.sync(player);
        }
    }

    /** The next landing within {@code ticks} does not hurt. */
    static void safeFall(ServerPlayer player, int ticks) {
        SAFE_FALL.put(player.getUUID(), player.server.getTickCount() + ticks);
    }

    /**
     * A player the tentacles let go of without throwing or slamming him: the arms fold in, the one holding
     * him logs out or dies, or he may no longer be hurt. The fall from where he was held does not hurt him.
     */
    static void setDown(LivingEntity target) {
        if (target instanceof ServerPlayer player && player.isAlive() && !player.isRemoved()) {
            player.resetFallDistance();
            safeFall(player, SET_DOWN_SAFE);
        }
    }

    // ---- Events ----

    /** Tentacle Strike: while the arms are out, a tentacle lashes out and delivers the hit. */
    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getTarget() instanceof LivingEntity target)) {
            return;
        }
        OctoRig rig = RIGS.get(player.getUUID());
        if (rig != null && rig.meleeStrike(target)) {
            // The hit itself comes when the claw arrives (see OctoRig).
            event.setCanceled(true);
        }
    }

    /** Block: hits from the front are caught by the tentacles. */
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

    /** No knockback from a hit the tentacles just caught. */
    @SubscribeEvent
    public static void onKnockBack(LivingKnockBackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            OctoRig rig = RIGS.get(player.getUUID());
            if (rig != null && rig.justBlocked()) {
                event.setCanceled(true);
            }
        }
    }

    /** No fall damage while climbing or carried by the legs, or landing from a dash or an air slam. */
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
