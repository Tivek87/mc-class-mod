package nl.tivek.multiversepowers.character.greenlantern.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.character.client.Crosshairs;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientRing;
import nl.tivek.multiversepowers.character.greenlantern.client.body.heavy.ClientHeavy;
import nl.tivek.multiversepowers.character.greenlantern.heavy.HeavyMoves;
import nl.tivek.multiversepowers.engine.client.gui.GuiShapes;
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.killconfirm.client.KillMarker;

// Green Lantern's crosshair: the lantern's emblem, a ring between two bars, in the ring's green. It reacts as every
// crosshair does (Crosshairs.Feel), closes in on a creature under it, its middle dot in that creature's colour, and a
// landed blow slams the bars in. Its ring flickers as the ring's power runs low. The shotgun adds the ring its pellets
// spread over, the rocket launcher a drop mark under it; a gun shows its rounds under the emblem, each going out with a
// flash as it is fired, and while it reloads an arc fills round the ring and the rounds light up as they go in.
final class LanternCrosshair {
    private static final int GREEN = 0x7CFF9C;
    private static final int PALE = 0xE4FFE9;
    private static final float RING = 3.4F;
    private static final float BAR = 6.2F;
    private static final float BAR_HALF = 4.6F;
    private static final float WIDTH = 1.1F;
    // About how far the shotgun's pellets stray (radians), as a ring round the emblem.
    private static final double PELLET_SPREAD = 0.09;
    // Below this share of power the ring flickers.
    private static final float LOW_POWER = 0.2F;
    // When each of a reloading gun's rounds goes in, as a share of the reload.
    private static final float[][] LOADED_AT = { {}, {}, { 0.77F }, { 0.47F, 0.65F } };
    // The revolvers' new rounds grow in pair by pair from this tick of the reload, one pair every REVOLVER_EVERY.
    private static final float REVOLVER_FROM = 14.0F;
    private static final float REVOLVER_EVERY = 1.4F;
    private static final int HOT = 0xFF7A3C;
    private static int ammoWas = -1;
    private static float firedAt = -100.0F;

    private LanternCrosshair() {
    }

    static float draw(GuiGraphics graphics, LocalPlayer player, float cx, float cy, Crosshairs.Feel feel) {
        float spread = feel.spread();
        float aim = feel.aim();
        float ring = feel.radius(RING + 1.6F * spread - 0.6F * aim);
        float bar = BAR + 2.8F * spread - 1.4F * aim - 1.2F * feel.hit();
        float half = BAR_HALF - 0.8F * aim;
        float width = WIDTH + feel.thick();
        int own = GuiShapes.mix(GREEN, PALE, 0.5F * aim);
        int color = Crosshairs.tint(own, feel, 0.0F);
        float power = ClientRing.charge(player);
        float flicker = power < LOW_POWER ? 0.45F + 0.55F * (float) Noise.of((int) (feel.time() * 0.7F), 3, 17)
                * (power / LOW_POWER) : 1.0F;
        float rx = feel.swayX();
        float ry = feel.swayY();
        Crosshairs.ring(graphics, cx + rx, cy + ry, ring, width, color, 0.9F * flicker);
        for (int end = -1; end <= 1; end += 2) {
            Crosshairs.stroke(graphics, feel.x(cx, -half, end * bar), feel.y(cy, -half, end * bar),
                    feel.x(cx, half, end * bar), feel.y(cy, half, end * bar), width, color, 0.9F);
        }
        Crosshairs.dot(graphics, cx, cy, 0.7F + 0.3F * aim, Crosshairs.tint(own, feel, 1.0F), 0.95F);
        float reach = bar * feel.grow() * Math.max(feel.wide(), feel.tall());
        Crosshairs.health(graphics, cx + rx, cy + ry, ring + 2.0F, feel);
        int heavy = ClientHeavy.holding();
        if (heavy == HeavyMoves.SHOTGUN) {
            float spreadRing = feel.radius(pixels(graphics, PELLET_SPREAD) * (1.0F + 0.5F * spread));
            for (int dash = 0; dash < 8; dash++) {
                float from = dash * 45.0F + 8.0F + feel.turn() * Mth.RAD_TO_DEG;
                GuiShapes.arc(graphics, cx + rx, cy + ry, spreadRing - 1.0F, spreadRing + 1.0F, from, from + 29.0F,
                        GuiShapes.fade(0x000000, 0.3F));
                GuiShapes.arc(graphics, cx + rx, cy + ry, spreadRing - 0.45F, spreadRing + 0.45F, from, from + 29.0F,
                        GuiShapes.fade(color, 0.75F));
            }
            reach = Math.max(reach, spreadRing);
        } else if (heavy == HeavyMoves.RPG) {
            float y = feel.y(cy, 0.0F, bar + 3.0F);
            float x = feel.x(cx, 0.0F, bar + 3.0F);
            Crosshairs.stroke(graphics, x - 2.6F, y, x, y + 2.0F, WIDTH, color, 0.85F);
            Crosshairs.stroke(graphics, x, y + 2.0F, x + 2.6F, y, WIDTH, color, 0.85F);
            Crosshairs.stroke(graphics, x - 1.4F, y + 5.0F, x + 1.4F, y + 5.0F, WIDTH, color, 0.6F);
        }
        ammo(graphics, cx + rx, cy + ry, ring, bar * feel.grow() * feel.tall(), heavy, feel);
        gauge(graphics, cx + rx, cy + ry, ring, heavy, feel);
        return reach;
    }

    // The gun's rounds in a row under the emblem: lit while loaded, dim once spent, each flashing out as it fires;
    // while it reloads an arc fills round the ring and each round lights as it goes in.
    private static void ammo(GuiGraphics graphics, float cx, float cy, float ring, float bar, int heavy,
            Crosshairs.Feel feel) {
        ClientHeavy.Held gun = ClientHeavy.gun();
        if (gun == null) {
            ammoWas = -1;
            return;
        }
        int full = HeavyMoves.ammo(heavy);
        int ammo = gun.ammo();
        if (ammoWas >= 0 && ammo < ammoWas) {
            firedAt = feel.time();
        }
        ammoWas = ammo;
        double reload = ClientHeavy.reloading(feel.partialTick());
        float fired = Mth.clamp(1.0F - (feel.time() - firedAt) / 6.0F, 0.0F, 1.0F);
        float y = cy + bar + (heavy == HeavyMoves.RPG ? 10.0F : 4.0F);
        for (int i = 0; i < full; i++) {
            float x = cx + (i - (full - 1) * 0.5F) * 3.4F;
            boolean lit = reload >= 0.0 ? reload >= loadedAt(heavy, i) : i < ammo;
            int tone = lit ? PALE : reload < 0.0 && ammo == 0 ? KillMarker.RED : GREEN;
            float alpha = lit ? 0.95F : 0.3F;
            float tall = heavy == HeavyMoves.RPG ? 3.6F : heavy == HeavyMoves.REVOLVERS ? 2.0F : 2.6F;
            if (heavy == HeavyMoves.REVOLVERS) {
                x = cx + (i - (full - 1) * 0.5F) * 2.2F + (i < full / 2 ? -1.2F : 1.2F);
            }
            Crosshairs.stroke(graphics, x, y, x, y + tall, heavy == HeavyMoves.RPG ? 1.2F : 1.5F, tone, alpha);
            if (heavy == HeavyMoves.RPG) {
                Crosshairs.stroke(graphics, x - 1.1F, y + 1.1F, x, y - 0.4F, 0.8F, tone, alpha);
                Crosshairs.stroke(graphics, x, y - 0.4F, x + 1.1F, y + 1.1F, 0.8F, tone, alpha);
            }
            if (!lit && fired > 0.0F && i == ammo) {
                Crosshairs.ring(graphics, x, y + tall * 0.5F, 1.0F + 4.0F * (1.0F - fired), 0.5F, 0xFFFFFF,
                        0.8F * fired);
            }
        }
        if (reload >= 0.0) {
            float r = ring + 3.8F;
            GuiShapes.arc(graphics, cx, cy, r - 1.0F, r + 1.0F, 0.0F, 360.0F, GuiShapes.fade(0x000000, 0.3F));
            GuiShapes.arc(graphics, cx, cy, r - 0.5F, r + 0.5F, 0.0F, (float) (360.0 * reload),
                    GuiShapes.fade(PALE, 0.9F));
        }
    }

    // When round `i` of a reloading gun goes in, as a share of the reload.
    private static float loadedAt(int heavy, int i) {
        if (heavy == HeavyMoves.REVOLVERS) {
            float length = HeavyMoves.length(heavy, HeavyMoves.RELOAD) - HeavyMoves.RELOADED;
            return (REVOLVER_FROM + REVOLVER_EVERY * (i / 2)) / length;
        }
        return LOADED_AT[heavy][i];
    }

    // The cannon's charge filling round the ring as he holds it, white once full; the minigun's heat as an arc
    // under it, going from green to orange as the barrels heat and flashing red while they cool from overheating.
    private static void gauge(GuiGraphics graphics, float cx, float cy, float ring, int heavy, Crosshairs.Feel feel) {
        float r = ring + 3.8F;
        if (heavy == HeavyMoves.CANNON) {
            double charge = ClientHeavy.charge(feel.partialTick());
            if (charge < 0.0) {
                return;
            }
            int tone = charge >= 1.0 ? 0xFFFFFF : PALE;
            float pulse = charge >= 1.0 ? 0.7F + 0.3F * Mth.sin(feel.time() * 1.3F) : 0.9F;
            GuiShapes.arc(graphics, cx, cy, r - 1.0F, r + 1.0F, 0.0F, 360.0F, GuiShapes.fade(0x000000, 0.3F));
            GuiShapes.arc(graphics, cx, cy, r - 0.6F, r + 0.6F, 0.0F, (float) (360.0 * charge),
                    GuiShapes.fade(tone, pulse));
        } else if (heavy == HeavyMoves.MINIGUN) {
            ClientHeavy.Held gun = ClientHeavy.gun();
            if (gun == null) {
                return;
            }
            boolean cooling = ClientHeavy.reloading(feel.partialTick()) >= 0.0;
            float heat = gun.ammo() / 100.0F;
            if (heat <= 0.01F && !cooling) {
                return;
            }
            int tone = cooling ? KillMarker.RED : GuiShapes.mix(GREEN, HOT, heat);
            float alpha = cooling ? 0.5F + 0.4F * Mth.sin(feel.time() * 1.6F) : 0.9F;
            GuiShapes.arc(graphics, cx, cy, r - 1.0F, r + 1.0F, 120.0F, 240.0F, GuiShapes.fade(0x000000, 0.3F));
            GuiShapes.arc(graphics, cx, cy, r - 0.6F, r + 0.6F, 120.0F, 120.0F + 120.0F * (cooling ? 1.0F : heat),
                    GuiShapes.fade(tone, alpha));
        }
    }

    // An angle off the middle of the view as gui pixels from the crosshair.
    private static float pixels(GuiGraphics graphics, double angle) {
        double fov = Math.toRadians(Minecraft.getInstance().options.fov().get());
        return (float) (Math.tan(angle) / Math.tan(fov * 0.5) * graphics.guiHeight() * 0.5);
    }
}
