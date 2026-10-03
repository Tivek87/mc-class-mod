package nl.tivek.multiversepowers.character.thor.client;

import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.client.AbilityPanel;
import nl.tivek.multiversepowers.character.client.ClientCharacter;
import nl.tivek.multiversepowers.character.client.MouseHold;
import nl.tivek.multiversepowers.engine.client.gui.GuiShapes;
import nl.tivek.multiversepowers.engine.math.Noise;

// Thor's hold of the attack button round the crosshair (the thunderclap, the hammer's uppercut, the shockwave in
// flight): a small ring charging from the top while the button is held, crackling at its tip; when full it pops with
// a flash and bolts leaping out. While it cools down a thin arc counts the wait, and a glint shows the moment it is
// ready again.
@Mod(value = MultiversePowers.MODID, dist = Dist.CLIENT)
public final class ThunderGauge {
    private static final ResourceLocation LAYER_ID = ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID,
            "thunder_gauge");
    private static final float INNER = 7.5F;
    private static final float OUTER = 9.5F;
    private static final float POP_MS = 380.0F;
    private static final float READY_MS = 450.0F;
    private static final float SHOWN = 0.08F;
    private static final int DARK = 0x0B1A2E;
    private static final int STORM = 0x3F8CFF;
    private static final int BOLT = 0x9FE8FF;
    private static final int WHITE = 0xF4FBFF;
    private static long fullAt;
    private static long readyAt;
    private static int lastCooldown;
    private static int cooldownFrom;

    public ThunderGauge(IEventBus modEventBus) {
        modEventBus.addListener(ThunderGauge::onRegisterLayers);
        AbilityPanel.rules(GameCharacter.THOR, new ThorPanel());
    }

    private static void onRegisterLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(LAYER_ID, ThunderGauge::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        CharacterAbility clap = minecraft.player == null ? null : leftHold(minecraft.player);
        if (minecraft.player == null || minecraft.options.hideGui || clap == null
                || ClientCharacter.active() != GameCharacter.THOR || minecraft.screen != null) {
            fullAt = 0L;
            return;
        }
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
        float x = graphics.guiWidth() * 0.5F;
        float y = graphics.guiHeight() * 0.5F;
        long now = Util.getMillis();
        int cooldown = ClientCharacter.cooldownLeft(clap.slot());
        if (cooldown > lastCooldown) {
            cooldownFrom = cooldown;
        }
        if (cooldown == 0 && lastCooldown > 0) {
            readyAt = now;
        }
        lastCooldown = cooldown;
        float progress = MouseHold.progress(clap, partialTick);
        boolean drawn = false;
        if (cooldown > 0) {
            float left = cooldownFrom <= 0 ? 0.0F : (cooldown - partialTick) / cooldownFrom;
            GuiShapes.arc(graphics, x, y, OUTER + 0.5F, OUTER + 2.0F, 0.0F, 360.0F * Mth.clamp(left, 0.0F, 1.0F),
                    GuiShapes.fade(STORM, progress >= 0.0F ? 0.85F : 0.6F));
            drawn = true;
        } else if (progress >= SHOWN) {
            drawn = charge(graphics, x, y, progress, now);
        }
        drawn |= pop(graphics, x, y, now);
        drawn |= ready(graphics, x, y, now);
        if (drawn) {
            GuiShapes.flush(graphics);
        }
    }

    // What holding the attack button does now: the thunderclap, the hammer's uppercut or the shockwave in flight.
    @Nullable
    private static CharacterAbility leftHold(LocalPlayer player) {
        for (CharacterAbility ability : GameCharacter.THOR.abilities()) {
            if (ability.input() == CharacterAbility.Input.LEFT && ability.holdTicks() > 0
                    && ClientCharacter.inPlay(ability, player)) {
                return ability;
            }
        }
        return null;
    }

    private static boolean charge(GuiGraphics graphics, float x, float y, float progress, long now) {
        float filling = Mth.clamp((progress - SHOWN) / (1.0F - SHOWN), 0.0F, 1.0F);
        float appear = Mth.clamp((progress - SHOWN) * 10.0F, 0.0F, 1.0F);
        GuiShapes.arc(graphics, x, y, INNER - 1.0F, OUTER + 1.0F, 0.0F, 360.0F, GuiShapes.fade(0x000000,
                0.35F * appear));
        GuiShapes.arc(graphics, x, y, INNER, OUTER, 0.0F, 360.0F, GuiShapes.fade(DARK, 0.85F * appear));
        if (progress >= 1.0F) {
            if (fullAt == 0L) {
                fullAt = now;
            }
            return true;
        }
        fullAt = 0L;
        float end = 360.0F * filling;
        GuiShapes.arc(graphics, x, y, INNER, OUTER, 0.0F, end, GuiShapes.fade(GuiShapes.mix(STORM, BOLT, filling),
                0.95F * appear));
        // Static crackles at the charging tip.
        long flicker = now / 45L;
        for (int k = 0; k < 2; k++) {
            float angle = (end - 6.0F + 12.0F * (float) Noise.of((int) flicker, k, 3)) * Mth.DEG_TO_RAD;
            float r0 = INNER - 1.5F;
            float r1 = OUTER + 2.5F * (float) Noise.of((int) flicker, k, 4) + 1.0F;
            GuiShapes.stroke(graphics, x + Mth.sin(angle) * r0, y - Mth.cos(angle) * r0, x + Mth.sin(angle) * r1,
                    y - Mth.cos(angle) * r1, 0.8F, GuiShapes.fade(WHITE, 0.9F * appear));
        }
        return true;
    }

    // Full: a white flash round the ring and jagged bolts leaping out of it.
    private static boolean pop(GuiGraphics graphics, float x, float y, long now) {
        if (fullAt == 0L) {
            return false;
        }
        float u = (now - fullAt) / POP_MS;
        if (u >= 1.0F) {
            return false;
        }
        float fade = 1.0F - u;
        GuiShapes.arc(graphics, x, y, INNER - 1.0F, OUTER + 1.0F, 0.0F, 360.0F, GuiShapes.fade(WHITE, fade));
        float out = OUTER + 10.0F * u;
        GuiShapes.arc(graphics, x, y, out - 1.2F, out, 0.0F, 360.0F, GuiShapes.fade(BOLT, 0.8F * fade));
        for (int k = 0; k < 6; k++) {
            float angle = (k * 60.0F + 30.0F * (float) Noise.of(k, (int) (fullAt / 7L), 1)) * Mth.DEG_TO_RAD;
            float reach = OUTER + (6.0F + 6.0F * (float) Noise.of(k, 2, 2)) * Mth.sqrt(u + 0.1F);
            bolt(graphics, x, y, angle, OUTER + 0.5F, reach, k, GuiShapes.fade(k % 2 == 0 ? WHITE : BOLT, fade));
        }
        return true;
    }

    // Cooled down: a glint spins once round the ring.
    private static boolean ready(GuiGraphics graphics, float x, float y, long now) {
        float u = (now - readyAt) / READY_MS;
        if (readyAt == 0L || u >= 1.0F) {
            return false;
        }
        float fade = 1.0F - u;
        float at = 360.0F * u;
        GuiShapes.arc(graphics, x, y, OUTER + 0.5F, OUTER + 2.0F, at - 50.0F, at, GuiShapes.fade(BOLT, fade));
        GuiShapes.arc(graphics, x, y, OUTER + 0.5F, OUTER + 2.0F, at - 8.0F, at, GuiShapes.fade(WHITE, fade));
        return true;
    }

    // A zigzag from one radius to another along an angle.
    private static void bolt(GuiGraphics graphics, float x, float y, float angle, float from, float to, int seed,
            int argb) {
        float sin = Mth.sin(angle);
        float cos = Mth.cos(angle);
        float lastX = x + sin * from;
        float lastY = y - cos * from;
        int steps = 3;
        for (int i = 1; i <= steps; i++) {
            float r = Mth.lerp((float) i / steps, from, to);
            float jog = i == steps ? 0.0F : (float) (Noise.of(seed, i, 7) - 0.5) * 3.0F;
            float nextX = x + sin * r + cos * jog;
            float nextY = y - cos * r + sin * jog;
            GuiShapes.stroke(graphics, lastX, lastY, nextX, nextY, 0.9F, argb);
            lastX = nextX;
            lastY = nextY;
        }
    }
}
