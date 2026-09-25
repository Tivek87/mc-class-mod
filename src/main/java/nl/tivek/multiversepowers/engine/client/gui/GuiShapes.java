package nl.tivek.multiversepowers.engine.client.gui;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public final class GuiShapes {
    private static final float STEP_DEGREES = 5.0F;

    private GuiShapes() {
    }

    public static void flush(GuiGraphics graphics) {
        graphics.flush();
    }

    public static void quad(GuiGraphics graphics, float x0, float y0, float x1, float y1,
            float x2, float y2, float x3, float y3, int argb) {
        if ((argb >>> 24) == 0) {
            return;
        }
        Matrix4f matrix = graphics.pose().last().pose();
        VertexConsumer buffer = graphics.bufferSource().getBuffer(RenderType.gui());
        // RenderType.gui() culls back-facing quads; flip the winding so either input order shows.
        float turn = (x0 * y1 - x1 * y0) + (x1 * y2 - x2 * y1) + (x2 * y3 - x3 * y2) + (x3 * y0 - x0 * y3);
        if (turn > 0.0F) {
            buffer.addVertex(matrix, x3, y3, 0.0F).setColor(argb);
            buffer.addVertex(matrix, x2, y2, 0.0F).setColor(argb);
            buffer.addVertex(matrix, x1, y1, 0.0F).setColor(argb);
            buffer.addVertex(matrix, x0, y0, 0.0F).setColor(argb);
        } else {
            buffer.addVertex(matrix, x0, y0, 0.0F).setColor(argb);
            buffer.addVertex(matrix, x1, y1, 0.0F).setColor(argb);
            buffer.addVertex(matrix, x2, y2, 0.0F).setColor(argb);
            buffer.addVertex(matrix, x3, y3, 0.0F).setColor(argb);
        }
    }

    public static void triangle(GuiGraphics graphics, float x0, float y0, float x1, float y1,
            float x2, float y2, int argb) {
        quad(graphics, x0, y0, x1, y1, x2, y2, x2, y2, argb);
    }

    public static void arc(GuiGraphics graphics, float cx, float cy, float inner, float outer,
            float fromDegrees, float toDegrees, int argb) {
        if ((argb >>> 24) == 0 || outer <= inner) {
            return;
        }
        float span = toDegrees - fromDegrees;
        int steps = Math.max(1, Mth.ceil(Math.abs(span) / STEP_DEGREES));
        float stepped = span / steps;
        float sinA = Mth.sin(fromDegrees * Mth.DEG_TO_RAD);
        float cosA = Mth.cos(fromDegrees * Mth.DEG_TO_RAD);
        for (int i = 0; i < steps; i++) {
            float next = (fromDegrees + stepped * (i + 1)) * Mth.DEG_TO_RAD;
            float sinB = Mth.sin(next);
            float cosB = Mth.cos(next);
            quad(graphics,
                    cx + sinA * inner, cy - cosA * inner,
                    cx + sinB * inner, cy - cosB * inner,
                    cx + sinB * outer, cy - cosB * outer,
                    cx + sinA * outer, cy - cosA * outer, argb);
            sinA = sinB;
            cosA = cosB;
        }
    }

    public static void disc(GuiGraphics graphics, float cx, float cy, float radius, int argb) {
        arc(graphics, cx, cy, 0.0F, radius, 0.0F, 360.0F, argb);
    }

    public static void ring(GuiGraphics graphics, float cx, float cy, float radius, float width, int argb) {
        arc(graphics, cx, cy, radius - width * 0.5F, radius + width * 0.5F, 0.0F, 360.0F, argb);
    }

    public static void stroke(GuiGraphics graphics, float x0, float y0, float x1, float y1,
            float width, int argb) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float length = Mth.sqrt(dx * dx + dy * dy);
        if (length < 1.0E-4F) {
            return;
        }
        float half = width * 0.5F;
        float alongX = dx / length * half;
        float alongY = dy / length * half;
        float sideX = -alongY;
        float sideY = alongX;
        float ax = x0 - alongX;
        float ay = y0 - alongY;
        float bx = x1 + alongX;
        float by = y1 + alongY;
        quad(graphics, ax + sideX, ay + sideY, bx + sideX, by + sideY,
                bx - sideX, by - sideY, ax - sideX, ay - sideY, argb);
    }

    public static void roundRect(GuiGraphics graphics, float x, float y, float width, float height,
            float radius, int argb) {
        float r = Math.min(radius, Math.min(width, height) * 0.5F);
        quad(graphics, x + r, y, x + width - r, y, x + width - r, y + height, x + r, y + height, argb);
        quad(graphics, x, y + r, x + r, y + r, x + r, y + height - r, x, y + height - r, argb);
        quad(graphics, x + width - r, y + r, x + width, y + r, x + width, y + height - r,
                x + width - r, y + height - r, argb);
        arc(graphics, x + width - r, y + r, 0.0F, r, 0.0F, 90.0F, argb);
        arc(graphics, x + width - r, y + height - r, 0.0F, r, 90.0F, 180.0F, argb);
        arc(graphics, x + r, y + height - r, 0.0F, r, 180.0F, 270.0F, argb);
        arc(graphics, x + r, y + r, 0.0F, r, 270.0F, 360.0F, argb);
    }

    public static int fade(int rgb, float alpha) {
        int a = Mth.clamp(Mth.floor(alpha * 255.0F + 0.5F), 0, 255);
        return a << 24 | rgb & 0xFFFFFF;
    }

    public static int mix(int from, int to, float part) {
        float t = Mth.clamp(part, 0.0F, 1.0F);
        int r = Mth.lerpInt(t, from >> 16 & 0xFF, to >> 16 & 0xFF);
        int g = Mth.lerpInt(t, from >> 8 & 0xFF, to >> 8 & 0xFF);
        int b = Mth.lerpInt(t, from & 0xFF, to & 0xFF);
        return r << 16 | g << 8 | b;
    }
}
