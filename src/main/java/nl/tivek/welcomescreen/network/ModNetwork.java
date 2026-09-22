package nl.tivek.welcomescreen.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.tivek.welcomescreen.WelcomeScreenMod;
import nl.tivek.welcomescreen.character.Characters;
import nl.tivek.welcomescreen.character.GameCharacter;
import nl.tivek.welcomescreen.character.docock.OctopusArms;
import nl.tivek.welcomescreen.classes.ClassData;
import nl.tivek.welcomescreen.classes.ClassGroup;
import nl.tivek.welcomescreen.classes.PlayerClass;
import nl.tivek.welcomescreen.client.ClientPayloadHandler;
import nl.tivek.welcomescreen.server.ChoosingState;
import nl.tivek.welcomescreen.server.ClassEffects;
import nl.tivek.welcomescreen.server.ClassGear;
import nl.tivek.welcomescreen.spell.Spell;
import nl.tivek.welcomescreen.spell.SpellCasting;

public final class ModNetwork {
    private static final String VERSION = "14";

    private ModNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToClient(OpenWelcomePayload.TYPE, OpenWelcomePayload.STREAM_CODEC, ModNetwork::onOpenWelcome);
        registrar.playToServer(SelectClassPayload.TYPE, SelectClassPayload.STREAM_CODEC, ModNetwork::onSelectClass);
        registrar.playToClient(ClassSyncPayload.TYPE, ClassSyncPayload.STREAM_CODEC, ModNetwork::onClassSync);
        registrar.playToServer(TestEffectPayload.TYPE, TestEffectPayload.STREAM_CODEC, ModNetwork::onTestEffect);
        registrar.playToServer(CastSpellPayload.TYPE, CastSpellPayload.STREAM_CODEC, ModNetwork::onCastSpell);
        registrar.playToClient(SpellCooldownPayload.TYPE, SpellCooldownPayload.STREAM_CODEC,
                ModNetwork::onSpellCooldown);
        registrar.playToClient(VoidStatePayload.TYPE, VoidStatePayload.STREAM_CODEC, ModNetwork::onVoidState);
        registrar.playToClient(GrabStatePayload.TYPE, GrabStatePayload.STREAM_CODEC, ModNetwork::onGrabState);
        registrar.playToServer(ThrowGrabPayload.TYPE, ThrowGrabPayload.STREAM_CODEC, ModNetwork::onThrowGrab);
        registrar.playToClient(ArmPayload.TYPE, ArmPayload.STREAM_CODEC, ModNetwork::onArm);
        registrar.playToClient(PortalPayload.TYPE, PortalPayload.STREAM_CODEC, ModNetwork::onPortal);
        registrar.playToClient(ConstructPayload.TYPE, ConstructPayload.STREAM_CODEC, ModNetwork::onConstruct);
        registrar.playToClient(RingPayload.TYPE, RingPayload.STREAM_CODEC, ModNetwork::onRing);
        registrar.playToServer(AbilityActionPayload.TYPE, AbilityActionPayload.STREAM_CODEC,
                ModNetwork::onAbilityAction);
        registrar.playToClient(CharacterStatePayload.TYPE, CharacterStatePayload.STREAM_CODEC,
                ModNetwork::onCharacterState);
        registrar.playToClient(CharacterLookPayload.TYPE, CharacterLookPayload.STREAM_CODEC,
                ModNetwork::onCharacterLook);
        registrar.playToServer(TransformPayload.TYPE, TransformPayload.STREAM_CODEC, ModNetwork::onTransform);
        registrar.playToClient(StaminaCostPayload.TYPE, StaminaCostPayload.STREAM_CODEC, ModNetwork::onStaminaCost);
    }

    /** An ability key, or the client telling that it is holding on to a wall. */
    private static void onAbilityAction(AbilityActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                return;
            }
            if (payload.action() == AbilityActionPayload.CLIMB) {
                OctopusArms.climb(serverPlayer, payload.on(), payload.data());
            } else if (payload.action() == AbilityActionPayload.PLACE) {
                OctopusArms.placeBlocks(serverPlayer);
            } else {
                Characters.action(serverPlayer, payload.action(), payload.on(), payload.data());
            }
        });
    }

    private static void onCharacterState(CharacterStatePayload payload, IPayloadContext context) {
        ClientPayloadHandler.handleCharacterState(payload, context);
    }

    private static void onCharacterLook(CharacterLookPayload payload, IPayloadContext context) {
        ClientPayloadHandler.handleCharacterLook(payload, context);
    }

    /** The wheel: turn into this character, or (with an empty id) back into yourself. */
    private static void onTransform(TransformPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                Characters.select(serverPlayer, GameCharacter.byId(payload.characterId()));
            }
        });
    }

    private static void onStaminaCost(StaminaCostPayload payload, IPayloadContext context) {
        ClientPayloadHandler.handleStaminaCost(payload, context);
    }

    private static void onPortal(PortalPayload payload, IPayloadContext context) {
        ClientPayloadHandler.handlePortal(payload, context);
    }

    private static void onConstruct(ConstructPayload payload, IPayloadContext context) {
        ClientPayloadHandler.handleConstruct(payload, context);
    }

    private static void onRing(RingPayload payload, IPayloadContext context) {
        ClientPayloadHandler.handleRing(payload, context);
    }

    private static void onArm(ArmPayload payload, IPayloadContext context) {
        ClientPayloadHandler.handleArm(payload, context);
    }

    private static void onGrabState(GrabStatePayload payload, IPayloadContext context) {
        ClientPayloadHandler.handleGrabState(payload, context);
    }

    private static void onThrowGrab(ThrowGrabPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                SpellCasting.throwHeld(serverPlayer);
            }
        });
    }

    private static void onVoidState(VoidStatePayload payload, IPayloadContext context) {
        ClientPayloadHandler.handleVoidState(payload, context);
    }

    private static void onCastSpell(CastSpellPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Spell spell = Spell.byId(payload.spellId());
            if (spell != null && context.player() instanceof ServerPlayer serverPlayer) {
                SpellCasting.tryCast(serverPlayer, spell);
            }
        });
    }

    private static void onSpellCooldown(SpellCooldownPayload payload, IPayloadContext context) {
        ClientPayloadHandler.handleSpellCooldown(payload, context);
    }

    /**
     * Developers may test ceremonies: always in a development run, otherwise only operators
     * (or a singleplayer world with cheats on), so normal players cannot spam them on a server.
     */
    public static boolean mayTestEffects(ServerPlayer player) {
        return !FMLEnvironment.production || player.hasPermissions(2);
    }

    private static void onTestEffect(TestEffectPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer) || !mayTestEffects(serverPlayer)) {
                return;
            }
            switch (payload.kind()) {
                case TestEffectPayload.CEREMONY -> {
                    PlayerClass playerClass = PlayerClass.byId(payload.id());
                    if (playerClass != null) {
                        ClassEffects.play(serverPlayer, playerClass, payload.showTitle());
                    }
                }
                case TestEffectPayload.DEATH -> {
                    for (ClassGroup group : ClassGroup.values()) {
                        if (group.getId().equals(payload.id())) {
                            ClassEffects.playDeath(serverPlayer, group);
                        }
                    }
                }
                case TestEffectPayload.LEVEL_UP -> ClassEffects.playLevelUp(serverPlayer);
                default -> {
                }
            }
        });
    }

    /** Tells the player's own client which class they have. */
    public static void syncClass(ServerPlayer player, PlayerClass playerClass) {
        PacketDistributor.sendToPlayer(player, new ClassSyncPayload(playerClass.getId()));
    }

    private static void onClassSync(ClassSyncPayload payload, IPayloadContext context) {
        ClientPayloadHandler.handleClassSync(payload, context);
    }

    // Only ever called on a client, so ClientPayloadHandler is not loaded on a dedicated server.
    private static void onOpenWelcome(OpenWelcomePayload payload, IPayloadContext context) {
        ClientPayloadHandler.handleOpenWelcome(context);
    }

    private static void onSelectClass(SelectClassPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                return;
            }
            // Ignore a second choice: the loadout is handed out once per player.
            if (ClassData.hasClass(serverPlayer)) {
                return;
            }

            PlayerClass chosen = PlayerClass.byId(payload.classId());
            if (chosen == null) {
                WelcomeScreenMod.LOGGER.warn("Unknown class id '{}' from {}",
                        payload.classId(), serverPlayer.getName().getString());
                return;
            }

            ClassData.setClass(serverPlayer, chosen);
            syncClass(serverPlayer, chosen);
            ChoosingState.leave(serverPlayer);
            ClassGear.apply(serverPlayer, chosen);
            WelcomeScreenMod.LOGGER.info("{} picked class {}", serverPlayer.getName().getString(), chosen.getId());
        });
    }
}
