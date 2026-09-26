package nl.tivek.multiversepowers.network;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.AbilityActionPayload;
import nl.tivek.multiversepowers.character.CharacterLookPayload;
import nl.tivek.multiversepowers.character.CharacterStatePayload;
import nl.tivek.multiversepowers.character.Characters;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.TransformPayload;
import nl.tivek.multiversepowers.character.docock.ArmPayload;
import nl.tivek.multiversepowers.character.docock.GrabStatePayload;
import nl.tivek.multiversepowers.character.docock.OctopusArms;
import nl.tivek.multiversepowers.character.docock.PortalPayload;
import nl.tivek.multiversepowers.character.docock.ThrowGrabPayload;
import nl.tivek.multiversepowers.character.greenlantern.Construct;
import nl.tivek.multiversepowers.character.greenlantern.ConstructHoldPayload;
import nl.tivek.multiversepowers.character.greenlantern.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.ConstructPickPayload;
import nl.tivek.multiversepowers.character.greenlantern.FlattenPayload;
import nl.tivek.multiversepowers.character.greenlantern.RingPayload;
import nl.tivek.multiversepowers.character.greenlantern.ability.EnergyWhip;
import nl.tivek.multiversepowers.character.greenlantern.ability.Flamethrower;
import nl.tivek.multiversepowers.character.greenlantern.ability.LandingSlam;
import nl.tivek.multiversepowers.character.greenlantern.ability.SwordShield;
import nl.tivek.multiversepowers.classes.ChoosingState;
import nl.tivek.multiversepowers.classes.ClassData;
import nl.tivek.multiversepowers.classes.ClassGear;
import nl.tivek.multiversepowers.classes.ClassGroup;
import nl.tivek.multiversepowers.classes.ClassSyncPayload;
import nl.tivek.multiversepowers.classes.OpenWelcomePayload;
import nl.tivek.multiversepowers.classes.PlayerClass;
import nl.tivek.multiversepowers.classes.SelectClassPayload;
import nl.tivek.multiversepowers.classes.TestEffectPayload;
import nl.tivek.multiversepowers.classes.ceremony.Ceremonies;
import nl.tivek.multiversepowers.config.WorldSettingsPayload;
import nl.tivek.multiversepowers.engine.fx.ParticlesPayload;
import nl.tivek.multiversepowers.faction.StandingsPayload;
import nl.tivek.multiversepowers.network.client.ClientPayloadHandler;
import nl.tivek.multiversepowers.spell.CastSpellPayload;
import nl.tivek.multiversepowers.spell.Spell;
import nl.tivek.multiversepowers.spell.SpellCasting;
import nl.tivek.multiversepowers.spell.SpellCooldownPayload;
import nl.tivek.multiversepowers.spell.SpellFxPayload;
import nl.tivek.multiversepowers.spell.VoidStatePayload;
import nl.tivek.multiversepowers.stamina.StaminaCostPayload;

public final class ModNetwork {
    private static final String VERSION = "17";

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
        registrar.playToClient(SpellFxPayload.TYPE, SpellFxPayload.STREAM_CODEC, ModNetwork::onSpellFx);
        registrar.playToClient(GrabStatePayload.TYPE, GrabStatePayload.STREAM_CODEC, ModNetwork::onGrabState);
        registrar.playToServer(ThrowGrabPayload.TYPE, ThrowGrabPayload.STREAM_CODEC, ModNetwork::onThrowGrab);
        registrar.playToClient(ArmPayload.TYPE, ArmPayload.STREAM_CODEC, ModNetwork::onArm);
        registrar.playToClient(PortalPayload.TYPE, PortalPayload.STREAM_CODEC, ModNetwork::onPortal);
        registrar.playToClient(ConstructPayload.TYPE, ConstructPayload.STREAM_CODEC, ModNetwork::onConstruct);
        registrar.playToClient(FlattenPayload.TYPE, FlattenPayload.STREAM_CODEC, ModNetwork::onFlatten);
        registrar.playToClient(RingPayload.TYPE, RingPayload.STREAM_CODEC, ModNetwork::onRing);
        registrar.playToServer(AbilityActionPayload.TYPE, AbilityActionPayload.STREAM_CODEC,
                ModNetwork::onAbilityAction);
        registrar.playToClient(CharacterStatePayload.TYPE, CharacterStatePayload.STREAM_CODEC,
                ModNetwork::onCharacterState);
        registrar.playToClient(CharacterLookPayload.TYPE, CharacterLookPayload.STREAM_CODEC,
                ModNetwork::onCharacterLook);
        registrar.playToServer(TransformPayload.TYPE, TransformPayload.STREAM_CODEC, ModNetwork::onTransform);
        registrar.playToClient(StaminaCostPayload.TYPE, StaminaCostPayload.STREAM_CODEC, ModNetwork::onStaminaCost);
        registrar.playToServer(ConstructPickPayload.TYPE, ConstructPickPayload.STREAM_CODEC,
                ModNetwork::onConstructPick);
        registrar.playToServer(ConstructHoldPayload.TYPE, ConstructHoldPayload.STREAM_CODEC,
                ModNetwork::onConstructHold);
        registrar.playToClient(WorldSettingsPayload.TYPE, WorldSettingsPayload.STREAM_CODEC,
                ModNetwork::onWorldSettings);
        registrar.playToClient(ParticlesPayload.TYPE, ParticlesPayload.STREAM_CODEC, ModNetwork::onParticles);
        registrar.playToClient(StandingsPayload.TYPE, StandingsPayload.STREAM_CODEC, ModNetwork::onStandings);
    }

    private static void onParticles(ParticlesPayload payload, IPayloadContext context) {
        ClientPayloadHandler.handleParticles(payload, context);
    }

    private static void onStandings(StandingsPayload payload, IPayloadContext context) {
        ClientPayloadHandler.handleStandings(payload, context);
    }

    private static void onWorldSettings(WorldSettingsPayload payload, IPayloadContext context) {
        ClientPayloadHandler.handleWorldSettings(payload, context);
    }

    private static void onConstructHold(ConstructHoldPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                Construct construct = Construct.byIndex(payload.construct());
                SwordShield.hold(serverPlayer, construct);
                Flamethrower.hold(serverPlayer, construct);
                EnergyWhip.hold(serverPlayer, construct);
            }
        });
    }

    private static void onConstructPick(ConstructPickPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer
                    && !LandingSlam.pick(serverPlayer, payload.variant())) {
                serverPlayer.displayClientMessage(Component.translatable("ring." + MultiversePowers.MODID
                        + ".pick_denied"), true);
            }
        });
    }

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

    private static void onTransform(TransformPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                Characters.pick(serverPlayer, GameCharacter.byId(payload.characterId()));
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

    private static void onFlatten(FlattenPayload payload, IPayloadContext context) {
        ClientPayloadHandler.handleFlatten(payload, context);
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
                OctopusArms.throwHeld(serverPlayer);
            }
        });
    }

    private static void onSpellFx(SpellFxPayload payload, IPayloadContext context) {
        ClientPayloadHandler.handleSpellFx(payload, context);
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
                        Ceremonies.play(serverPlayer, playerClass, payload.showTitle());
                    }
                }
                case TestEffectPayload.DEATH -> {
                    for (ClassGroup group : ClassGroup.values()) {
                        if (group.getId().equals(payload.id())) {
                            Ceremonies.playDeath(serverPlayer, group);
                        }
                    }
                }
                case TestEffectPayload.LEVEL_UP -> Ceremonies.playLevelUp(serverPlayer);
                default -> {
                }
            }
        });
    }

    public static void syncClass(ServerPlayer player, PlayerClass playerClass) {
        PacketDistributor.sendToPlayer(player, new ClassSyncPayload(playerClass.getId()));
    }

    private static void onClassSync(ClassSyncPayload payload, IPayloadContext context) {
        ClientPayloadHandler.handleClassSync(payload, context);
    }

    // Client-only: keeps ClientPayloadHandler from loading on a dedicated server.
    private static void onOpenWelcome(OpenWelcomePayload payload, IPayloadContext context) {
        ClientPayloadHandler.handleOpenWelcome(context);
    }

    private static void onSelectClass(SelectClassPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                return;
            }
            if (ClassData.hasClass(serverPlayer)) {
                return;
            }

            PlayerClass chosen = PlayerClass.byId(payload.classId());
            if (chosen == null) {
                MultiversePowers.LOGGER.warn("Unknown class id from {}", serverPlayer.getName().getString());
                return;
            }

            ClassData.setClass(serverPlayer, chosen);
            syncClass(serverPlayer, chosen);
            ChoosingState.leave(serverPlayer);
            ClassGear.apply(serverPlayer, chosen);
            MultiversePowers.LOGGER.info("{} picked class {}", serverPlayer.getName().getString(), chosen.getId());
        });
    }
}
