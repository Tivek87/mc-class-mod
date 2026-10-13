package nl.tivek.multiversepowers.faction.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.config.client.ClientSettings;
import nl.tivek.multiversepowers.engine.client.gui.HudSpace;
import nl.tivek.multiversepowers.faction.net.ReputationNotePayload;
import nl.tivek.multiversepowers.faction.Standing;
import nl.tivek.multiversepowers.faction.rules.Faction;
import nl.tivek.multiversepowers.faction.rules.FactionRules;

// A change of the player's reputation, shown a few seconds at the left edge: the faction, how much, and what it
// stands at; changes with one faction close together add up to one line. A faction that from now on stands another
// way to the player says so, with a chime.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class ReputationHud {
    private static final String KEY = "faction." + MultiversePowers.MODID + ".";
    private static final long SHOWN_MS = 3500L;
    private static final long FADE_MS = 500L;
    private static final int MOST = 5;
    private static final int LINE = 11;

    private static final class Note {
        private final String faction;
        private int delta;
        private int turned = -1;
        private long at;

        private Note(String faction) {
            this.faction = faction;
        }
    }

    private static final List<Note> NOTES = new ArrayList<>();

    private ReputationHud() {
    }

    public static void note(ReputationNotePayload payload) {
        long now = Util.getMillis();
        Note note = null;
        for (Note shown : NOTES) {
            if (shown.faction.equals(payload.faction()) && now - shown.at < SHOWN_MS) {
                note = shown;
            }
        }
        if (note == null) {
            note = new Note(payload.faction());
            NOTES.add(note);
            while (NOTES.size() > MOST) {
                NOTES.remove(0);
            }
        }
        note.delta += payload.delta();
        note.at = now;
        if (payload.turned() >= 0) {
            note.turned = payload.turned();
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(
                    payload.turned() == Standing.HOSTILE.ordinal() ? SoundEvents.NOTE_BLOCK_BASS.value()
                            : SoundEvents.NOTE_BLOCK_CHIME.value(), payload.turned() == Standing.HOSTILE.ordinal()
                                    ? 0.7F : 1.2F));
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        long now = Util.getMillis();
        NOTES.removeIf(note -> now - note.at > SHOWN_MS);
        if (NOTES.isEmpty() || minecraft.options.hideGui || !ClientSettings.reputationNotes()) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int y = graphics.guiHeight() / 3;
        for (Note note : NOTES) {
            Faction faction = FactionRules.get(note.faction);
            if (faction == null) {
                continue;
            }
            float left = (SHOWN_MS - (now - note.at)) / (float) FADE_MS;
            int alpha = Math.max(8, Math.min(255, (int) (left * 255.0F)));
            Component text = line(faction, note);
            int width = font.width(text) + 10;
            HudSpace.Box box = HudSpace.place(4, y, width, LINE, HudSpace.Way.DOWN);
            int x = (int) box.x();
            int top = (int) box.y();
            HudSpace.claim(x, top, width, LINE);
            graphics.fill(x, top, x + width, top + LINE, (alpha * 150 / 255) << 24);
            graphics.fill(x, top, x + 2, top + LINE, alpha << 24 | faction.color());
            graphics.drawString(font, text, x + 6, top + 2, alpha << 24 | 0xFFFFFF, false);
            y = top + LINE + 1;
        }
    }

    private static Component line(Faction faction, Note note) {
        Component name = Component.literal(faction.name()).withColor(faction.color());
        if (note.turned >= 0) {
            String how = note.turned < Standing.values().length
                    ? Standing.values()[note.turned].name().toLowerCase(Locale.ROOT) : "game";
            int color = note.turned < Standing.values().length ? Standing.values()[note.turned].rgb() : 0xC8C8C8;
            return Component.translatable(KEY + "note.turned", name,
                    Component.translatable(KEY + "stand." + how).withColor(color));
        }
        String sign = note.delta > 0 ? "+" + note.delta : String.valueOf(note.delta);
        return Component.translatable(KEY + "note", name,
                Component.literal(sign).withColor(note.delta > 0 ? 0x6EE7A0 : 0xFF6B6B),
                String.valueOf(ClientFactions.reputation(faction)));
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        NOTES.clear();
    }
}
