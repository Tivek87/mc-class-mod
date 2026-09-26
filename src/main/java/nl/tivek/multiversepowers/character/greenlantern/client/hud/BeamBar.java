package nl.tivek.multiversepowers.character.greenlantern.client.hud;

import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.client.MouseHold;
import nl.tivek.multiversepowers.character.greenlantern.RingPayload;
import nl.tivek.multiversepowers.character.greenlantern.ability.LightBeam;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientConstructs;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientRing;
import nl.tivek.multiversepowers.engine.client.gui.GuiShapes;

// The beam's hold bar: one piece per stage, the first filled by the hold itself, the rest while the beam grows, each
// piece a little thicker than the one before, up to the fifth.
final class BeamBar {
    private static final int GREEN = 0x3CE86A;
    private static final int BRIGHT = 0xCFFFDC;
    private static final int WHITE = 0xE6FFEC;
    private static final float FROM = 30.0F;
    private static final float SPAN = 120.0F;
    private static final float GAP = 3.0F;
    private static final float INNER = 13.0F;
    private static final float OUTER = 17.0F;
    private static final float GROW = 1.1F;
    private static final float SHOWN = 0.1F;
    private static final float FLASH_MS = 450.0F;
    private static int lastStage = -1;
    private static long stageAt;

    private BeamBar() {
    }

    static boolean render(GuiGraphics graphics, Player player, CharacterAbility bolt, float middleX, float middleY,
            float partialTick, List<Runnable> labels) {
        int hold = Math.max(1, bolt.holdTicks());
        int total = LightBeam.stageFrom(LightBeam.LAST, hold);
        boolean firing = ClientRing.has(player, RingPayload.BEAM);
        float ticks;
        int stage;
        if (firing) {
            stage = ClientConstructs.beamStage(player.getId());
            float age = Math.max(0.0F, ClientConstructs.beamAge(player.getId(), partialTick));
            ticks = Mth.clamp(hold + age, LightBeam.stageFrom(stage, hold),
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
        float throb = 0.75F + 0.25F * Mth.sin((now % 100000L) / (top ? 60.0F : 90.0F));
        float piece = (SPAN - GAP * (LightBeam.LAST)) / LightBeam.STAGES;
        for (int k = 0; k < LightBeam.STAGES; k++) {
            float from = FROM + k * (piece + GAP);
            float to = from + piece;
            float outer = OUTER + GROW * k;
            float start = k == 0 ? 0.0F : LightBeam.stageFrom(k - 1, hold);
            float end = LightBeam.stageFrom(k, hold);
            float fill = Mth.clamp((ticks - start) / Math.max(1.0F, end - start), 0.0F, 1.0F);
            GuiShapes.arc(graphics, middleX, middleY, INNER - 1.0F, outer + 1.0F, from, to,
                    GuiShapes.fade(0x000000, 0.45F * appear));
            GuiShapes.arc(graphics, middleX, middleY, INNER, outer, from, to, GuiShapes.fade(0x0E3A1E, 0.8F * appear));
            if (fill <= 0.0F) {
                continue;
            }
            float edge = from + piece * fill;
            if (fill >= 1.0F) {
                float glow = top ? throb : 0.9F;
                int shade = GuiShapes.mix(GREEN, BRIGHT, 0.35F + 0.65F * k / LightBeam.LAST);
                GuiShapes.arc(graphics, middleX, middleY, INNER - 0.5F, outer + 0.5F, from, to,
                        GuiShapes.fade(shade, glow * appear));
                if (k == stage && flash < 1.0F) {
                    float out = outer + 3.0F + 7.0F * flash;
                    GuiShapes.arc(graphics, middleX, middleY, out - 2.0F, out, from, to,
                            GuiShapes.fade(WHITE, 1.0F - flash));
                }
            } else {
                GuiShapes.arc(graphics, middleX, middleY, INNER, outer, from, edge,
                        GuiShapes.fade(GuiShapes.mix(GREEN, BRIGHT, fill), 0.95F * appear));
                GuiShapes.arc(graphics, middleX, middleY, INNER - 1.0F, outer + 1.0F, Math.max(from, edge - 3.0F), edge,
                        GuiShapes.fade(WHITE, appear));
            }
        }
        String prefix = "screen." + MultiversePowers.MODID + ".hold.";
        Component name = stage < 0 ? Component.translatable(prefix + "beam")
                : top ? Component.translatable(prefix + "beam_max")
                : Component.translatable(prefix + "beam_stage", stage + 1, LightBeam.STAGES);
        float labelX = middleX + OUTER + GROW * LightBeam.LAST + 6.0F;
        int color = top ? GuiShapes.mix(BRIGHT, 0xFFFFFF, throb) : stage < 0 ? GREEN : BRIGHT;
        labels.add(() -> graphics.drawString(Minecraft.getInstance().font, name, Mth.floor(labelX),
                Mth.floor(middleY) - 4, (int) (255 * appear) << 24 | color));
        return true;
    }
}
