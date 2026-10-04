package nl.tivek.multiversepowers.character.greenlantern.client.hud;

import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.client.AbilityKeys;
import nl.tivek.multiversepowers.character.client.AbilityPanel;
import nl.tivek.multiversepowers.character.client.MouseHold;
import nl.tivek.multiversepowers.character.client.PowerInputs;
import nl.tivek.multiversepowers.character.greenlantern.RingPayload;
import nl.tivek.multiversepowers.character.greenlantern.ability.light.LightBeam;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientConstructs;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientRing;
import nl.tivek.multiversepowers.engine.client.gui.GuiShapes;

// The beam's gauge: one numbered piece per stage, the first filled by the hold itself, the rest while the beam grows,
// each a little thicker than the one before; a padlock where it stops while the stage is locked.
final class BeamGauge {
    private static final float GAP = 3.5F;
    private static final float GROW = 1.6F;
    // Room past the outermost piece for its number.
    private static final float NUMBERS = 11.0F;
    private static final float SHOWN = 0.1F;
    private static final float FLASH_MS = 450.0F;
    private static int lastStage = -1;
    private static long stageAt;

    private BeamGauge() {
    }

    static boolean render(GuiGraphics graphics, Player player, CharacterAbility bolt, float x, float y,
            float partialTick, List<Runnable> labels) {
        int hold = Math.max(1, bolt.holdTicks());
        int total = LightBeam.stageFrom(LightBeam.LAST, hold);
        boolean firing = ClientRing.has(player, RingPayload.BEAM);
        float ticks;
        int stage;
        boolean locked = false;
        if (firing) {
            stage = ClientConstructs.beamStage(player.getId());
            float sent = ClientConstructs.beamClock(player.getId());
            float clock;
            if (Float.isNaN(sent)) {
                clock = Math.max(0.0F, ClientConstructs.beamAge(player.getId(), partialTick));
            } else {
                locked = LightBeam.lockedIn(sent);
                clock = LightBeam.clockOf(sent) + (locked ? 0.0F : partialTick);
            }
            ticks = Mth.clamp(hold + clock, LightBeam.stageFrom(stage, hold),
                    stage < LightBeam.LAST ? LightBeam.stageFrom(stage + 1, hold) : total);
        } else {
            float progress = MouseHold.progress(bolt, partialTick);
            if (progress < SHOWN) {
                lastStage = -1;
                return false;
            }
            stage = -1;
            ticks = Math.min(progress, 1.0F) * hold;
        }
        long now = Util.getMillis();
        if (stage != lastStage) {
            if (stage > lastStage) {
                stageAt = now;
            }
            lastStage = stage;
        }
        float appear = Mth.clamp((ticks / hold - SHOWN) * 8.0F, 0.0F, 1.0F);
        float flash = stage < 0 ? 1.0F : Mth.clamp((now - stageAt) / FLASH_MS, 0.0F, 1.0F);
        boolean top = stage == LightBeam.LAST;
        float throb = 0.5F + 0.5F * Mth.sin((now % 100000L) / (top || locked ? 70.0F : 110.0F));
        int count = LightBeam.STAGES;
        float piece = ArcGauge.piece(count, GAP);
        float inner = ArcGauge.claim(x, y, GROW * (count - 1) + NUMBERS);
        float thick = ArcGauge.OUTER - ArcGauge.INNER;
        Font font = Minecraft.getInstance().font;
        for (int k = 0; k < count; k++) {
            float from = ArcGauge.pieceFrom(k, count, GAP);
            float to = from + piece;
            float outer = inner + thick + GROW * k;
            float start = k == 0 ? 0.0F : LightBeam.stageFrom(k - 1, hold);
            float end = LightBeam.stageFrom(k, hold);
            float fill = Mth.clamp((ticks - start) / Math.max(1.0F, end - start), 0.0F, 1.0F);
            ArcGauge.empty(graphics, x, y, inner, outer, from, to, appear);
            int shade = GuiShapes.mix(ArcGauge.GREEN, ArcGauge.BRIGHT, 0.3F + 0.7F * k / LightBeam.LAST);
            int full = top ? GuiShapes.mix(shade, 0xFFFFFF, 0.35F * throb) : shade;
            ArcGauge.filled(graphics, x, y, inner, outer, from, to, fill, full,
                    GuiShapes.mix(ArcGauge.GREEN, ArcGauge.BRIGHT, fill), appear);
            if (k == stage) {
                ArcGauge.flash(graphics, x, y, outer, from, to, flash);
                if (locked) {
                    GuiShapes.arc(graphics, x, y, outer + 1.5F, outer + 3.0F, from, to,
                            GuiShapes.fade(ArcGauge.AMBER, (0.6F + 0.4F * throb) * appear));
                }
            }
            // Where it stops: a padlock on the piece it does not grow into.
            if (locked && k == stage + 1) {
                double rad = Math.toRadians((from + to) * 0.5F);
                float radius = (inner + outer) * 0.5F;
                int px = Mth.floor(x + (float) Math.sin(rad) * radius) - 2;
                int py = Mth.floor(y - (float) Math.cos(rad) * radius) - 3;
                graphics.fill(px - 1, py - 1, px + 6, py + 8, GuiShapes.fade(0x000000, 0.65F * appear));
                ArcGauge.padlock(graphics, px, py, ArcGauge.AMBER, appear);
            }
        }
        GuiShapes.flush(graphics);
        for (int k = 0; k < count; k++) {
            float mid = ArcGauge.pieceFrom(k, count, GAP) + piece * 0.5F;
            float outer = inner + thick + GROW * k;
            boolean reached = k <= stage;
            int color = k == stage ? 0xFFFFFF : reached ? ArcGauge.BRIGHT : 0x6F8A78;
            ArcGauge.number(graphics, font, x, y, outer + 6.5F, mid, String.valueOf(k + 1),
                    (int) (255 * appear) << 24 | color);
        }
        Component title;
        Component line;
        int titleColor;
        int lineColor = 0xB9D8C2;
        if (stage < 0) {
            title = Component.translatable(ArcGauge.PREFIX + "beam");
            line = Component.translatable(ArcGauge.PREFIX + "beam_charging");
            titleColor = ArcGauge.GREEN;
        } else {
            title = top ? Component.translatable(ArcGauge.PREFIX + "beam_top")
                    : Component.translatable(ArcGauge.PREFIX + "beam_stage_of", stage + 1, LightBeam.STAGES);
            titleColor = top ? GuiShapes.mix(ArcGauge.BRIGHT, 0xFFFFFF, throb) : ArcGauge.BRIGHT;
            float left = (LightBeam.stageFrom(stage + 1, hold) - ticks) / 20.0F;
            line = top || locked ? null : Component.translatable(ArcGauge.PREFIX + "beam_next",
                    String.format(Locale.ROOT, "%.1f", Math.max(0.0F, left)));
        }
        ArcGauge.Lock lock = locked ? new ArcGauge.Lock(throb, unlockKey()) : null;
        labels.add(() -> ArcGauge.label(graphics, font, x, y, title, titleColor, line, lineColor, appear, lock));
        return true;
    }

    // The key that locks and unlocks the beam's stage, or null while it has none.
    @Nullable
    private static Component unlockKey() {
        CharacterAbility lock = GameCharacter.GREEN_LANTERN.byName("beam_lock");
        KeyMapping key = lock == null ? null : AbilityKeys.of(lock);
        return key == null || key.isUnbound() ? null : AbilityPanel.brief(PowerInputs.keyName(key));
    }
}
