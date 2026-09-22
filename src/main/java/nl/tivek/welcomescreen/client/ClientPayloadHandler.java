package nl.tivek.welcomescreen.client;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import nl.tivek.welcomescreen.classes.PlayerClass;
import nl.tivek.welcomescreen.client.character.ClientCharacter;
import nl.tivek.welcomescreen.client.character.docock.ClientArms;
import nl.tivek.welcomescreen.client.character.docock.ClientGrabState;
import nl.tivek.welcomescreen.client.character.lantern.ClientConstructs;
import nl.tivek.welcomescreen.client.character.lantern.ClientLooks;
import nl.tivek.welcomescreen.client.character.lantern.ClientRing;
import nl.tivek.welcomescreen.client.classes.ClientClassData;
import nl.tivek.welcomescreen.client.spell.ClientSpellCooldowns;
import nl.tivek.welcomescreen.client.spell.ClientVoidState;
import nl.tivek.welcomescreen.client.stamina.StaminaClient;
import nl.tivek.welcomescreen.network.ArmPayload;
import nl.tivek.welcomescreen.network.CharacterLookPayload;
import nl.tivek.welcomescreen.network.CharacterStatePayload;
import nl.tivek.welcomescreen.network.ClassSyncPayload;
import nl.tivek.welcomescreen.network.ConstructPayload;
import nl.tivek.welcomescreen.network.GrabStatePayload;
import nl.tivek.welcomescreen.network.PortalPayload;
import nl.tivek.welcomescreen.network.RingPayload;
import nl.tivek.welcomescreen.network.SpellCooldownPayload;
import nl.tivek.welcomescreen.network.StaminaCostPayload;
import nl.tivek.welcomescreen.network.VoidStatePayload;
import nl.tivek.welcomescreen.spell.Spell;

/**
 * Client-only side of the network handling. Never loaded on a dedicated server.
 */
public final class ClientPayloadHandler {
    private ClientPayloadHandler() {
    }

    public static void handleOpenWelcome(IPayloadContext context) {
        context.enqueueWork(ClientEvents::requestWelcomeScreen);
    }

    public static void handleSpellCooldown(SpellCooldownPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Spell spell = Spell.byId(payload.spellId());
            if (spell != null) {
                ClientSpellCooldowns.set(spell, payload.ticks());
            }
        });
    }

    public static void handleVoidState(VoidStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientVoidState.set(payload.ticks()));
    }

    public static void handleGrabState(GrabStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientGrabState.set(payload.holding(), payload.blocks()));
    }

    public static void handleArm(ArmPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientArms.update(payload));
    }

    public static void handleCharacterState(CharacterStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientCharacter.set(payload));
    }

    public static void handleCharacterLook(CharacterLookPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientLooks.update(payload));
    }

    public static void handleStaminaCost(StaminaCostPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> StaminaClient.use(payload.amount()));
    }

    public static void handlePortal(PortalPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientArms.updatePortal(payload));
    }

    public static void handleConstruct(ConstructPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientConstructs.update(payload));
    }

    public static void handleRing(RingPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientRing.update(payload));
    }

    public static void handleClassSync(ClassSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientClassData.set(PlayerClass.byId(payload.classId())));
    }
}
