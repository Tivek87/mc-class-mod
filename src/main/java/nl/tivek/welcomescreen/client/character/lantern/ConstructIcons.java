package nl.tivek.welcomescreen.client.character.lantern;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;
import nl.tivek.welcomescreen.character.lantern.Construct;
import nl.tivek.welcomescreen.client.GuiShapes;

/**
 * The pictures of the constructs, on the construct wheel and on the bar above your hotbar.
 * <ul>
 * <li>A construct that exists is shown as itself: its own hard-light model, solid and glowing like in the world, turning
 * gently to and fro. The sword and shield: the shield with its emblem towards you and the sword crossed behind it, its
 * point up to the right.</li>
 * <li>A slot still kept free shows the lantern emblem, faint: a ring of light between two bars.</li>
 * </ul>
 */
final class ConstructIcons {
    // How far before the eye the models are set up to be drawn, in blocks, and how far out of the screen they come
    // (above everything drawn flat).
    private static final double AWAY = 3.0;
    private static final float DEPTH = 400.0F;
    // How big the sword and shield are against the size of the picture: together they are about this many blocks across.
    private static final double SWORD_SHIELD_ACROSS = 0.92;
    // How far the shield turns to and fro, in radians, and how quickly (radians per tick).
    private static final double SWAY = 0.38;
    private static final double SWAY_SPEED = 0.045;

    private ConstructIcons() {
    }

    /**
     * The picture of {@code construct} with its middle at {@code x}, {@code y}, about {@code size} across.
     *
     * @param lit   how far the mouse points at it, 0 to 1: it comes up a little bigger and brighter
     * @param rgb   the colour a flat picture is drawn in
     * @param alpha how strongly a flat picture shows
     */
    static void draw(GuiGraphics graphics, Construct construct, float x, float y, float size, float lit, int rgb,
            float alpha) {
        if (construct == Construct.SWORD_SHIELD) {
            swordShield(graphics, x, y, size * (1.0F + 0.1F * lit));
        } else if (construct != Construct.NONE) {
            emblem(graphics, x, y, size, rgb, alpha);
        }
    }

    /** The sword and shield, as hard light: the shield turning to and fro, the sword crossed behind it. */
    private static void swordShield(GuiGraphics graphics, float x, float y, float size) {
        // Whatever flat was handed in before is drawn first, so the model lands on top of it.
        GuiShapes.flush(graphics);
        float time = (Util.getMillis() % 3_600_000L) / 50.0F;
        float scale = (float) (size / SWORD_SHIELD_ACROSS);
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, DEPTH);
        pose.scale(scale, -scale, scale);
        pose.translate(0.0F, 0.0F, (float) AWAY);
        ConstructPainter painter = ConstructPainter.hand(pose, time);
        double sway = SWAY * Math.sin(time * SWAY_SPEED);
        Vec3 blade = new Vec3(0.67, 0.74, -0.08).normalize();
        Vec3 edge = new Vec3(-0.74, 0.67, 0.0);
        Vec3 grip = new Vec3(0.0, 0.0, -AWAY - 0.16).subtract(blade.scale(0.58 * 0.73));
        SwordPainter.sword(painter, grip, blade, edge, 0.73, 1.0, 0.0);
        SwordPainter.shield(painter, new Vec3(0.0, -0.01, -AWAY), new Vec3(Math.sin(sway), 0.1, Math.cos(sway)),
                new Vec3(0.0, 1.0, 0.0), 0.6, 1.0, 0.0);
        painter.finish(graphics.bufferSource());
        pose.popPose();
    }

    /**
     * The lantern emblem, for a slot still kept free: a ring with a bar over it and a bar under it, both a little wider
     * than the ring, and a faint glow round it.
     */
    private static void emblem(GuiGraphics graphics, float x, float y, float size, int rgb, float alpha) {
        float half = size * 0.5F;
        float thick = Math.max(1.0F, size * 0.08F);
        int line = GuiShapes.fade(rgb, alpha);
        GuiShapes.disc(graphics, x, y, half * 0.95F, GuiShapes.fade(rgb, alpha * 0.08F));
        GuiShapes.ring(graphics, x, y, half * 0.42F, thick, line);
        float bar = half * 0.78F;
        float gap = half * 0.62F;
        GuiShapes.stroke(graphics, x - bar, y - gap, x + bar, y - gap, thick, line);
        GuiShapes.stroke(graphics, x - bar, y + gap, x + bar, y + gap, thick, line);
        GuiShapes.disc(graphics, x, y, thick * 0.7F, GuiShapes.fade(rgb, alpha * 0.9F));
    }
}
