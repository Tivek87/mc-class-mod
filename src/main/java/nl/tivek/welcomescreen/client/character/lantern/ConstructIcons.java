package nl.tivek.welcomescreen.client.character.lantern;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;
import nl.tivek.welcomescreen.character.lantern.Construct;
import nl.tivek.welcomescreen.client.GuiShapes;

/**
 * The pictures of the constructs, on the construct wheel and on the bar above your hotbar. Every construct is shown as
 * itself: its own hard-light model, solid and glowing like in the world, turning gently to and fro.
 * <ul>
 * <li>The sword and shield: the shield with its emblem towards you and the sword crossed behind it, its point up to the
 * right; only the shield turns.</li>
 * <li>The other weapons (see {@link WeaponShapes}): a long weapon lies across the picture from low at the left to high
 * at the right, a gun lies on its side with its muzzle to the right, tipped a little so you see its top. A pair (the
 * gloves, the daggers, the revolvers, the submachine guns) is shown as two, crossed or side by side. A shot charges in
 * the muzzle of the arm cannon and a pilot flame burns at the nozzle of the flamethrower: light, not construct.</li>
 * </ul>
 */
final class ConstructIcons {
    // How far before the eye the models are set up to be drawn, in blocks, and how far out of the screen they come
    // (above everything drawn flat).
    private static final double AWAY = 3.0;
    private static final float DEPTH = 400.0F;
    // How many blocks across a picture is: a model this big fills it.
    private static final double ACROSS = 0.92;
    // How far a picture turns to and fro, in radians, and how quickly (radians per tick).
    private static final double SWAY = 0.38;
    private static final double SWAY_SPEED = 0.045;
    private static final Vec3 UPRIGHT = new Vec3(0.0, 1.0, 0.0);

    private static final Model WHIP = Model.of(WeaponShapes.WHIP);
    private static final Model GLOVE = Model.of(WeaponShapes.GLOVE);
    private static final Model LEFT_GLOVE = Model.of(WeaponShapes.LEFT_GLOVE);
    private static final Model DAGGER = Model.of(WeaponShapes.DAGGER);
    private static final Model BATTLEAXE = Model.of(WeaponShapes.BATTLEAXE);
    private static final Model WAR_HAMMER = Model.of(WeaponShapes.WAR_HAMMER);
    private static final Model HALBERD = Model.of(WeaponShapes.HALBERD);
    private static final Model CHAINSAW = Model.of(WeaponShapes.CHAINSAW);
    private static final Model REVOLVER = Model.of(WeaponShapes.REVOLVER);
    private static final Model SHOTGUN = Model.of(WeaponShapes.SHOTGUN);
    private static final Model SMG = Model.of(WeaponShapes.SMG);
    private static final Model ARM_CANNON = Model.of(WeaponShapes.ARM_CANNON);
    private static final Model GRENADE_LAUNCHER = Model.of(WeaponShapes.GRENADE_LAUNCHER);
    private static final Model MINIGUN = Model.of(WeaponShapes.MINIGUN);
    private static final Model ROCKET_LAUNCHER = Model.of(WeaponShapes.ROCKET_LAUNCHER);
    private static final Model FLAMETHROWER = Model.of(WeaponShapes.FLAMETHROWER);

    private ConstructIcons() {
    }

    /**
     * The picture of {@code construct} with its middle at {@code x}, {@code y}, about {@code size} across. Empty hands
     * have none.
     *
     * @param lit how far the mouse points at it, 0 to 1: it comes up a little bigger
     */
    static void draw(GuiGraphics graphics, Construct construct, float x, float y, float size, float lit) {
        if (construct == Construct.NONE) {
            return;
        }
        // Whatever flat was handed in before is drawn first, so the model lands on top of it.
        GuiShapes.flush(graphics);
        float time = (Util.getMillis() % 3_600_000L) / 50.0F;
        float scale = (float) (size * (1.0F + 0.1F * lit) / ACROSS);
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, DEPTH);
        pose.scale(scale, -scale, scale);
        pose.translate(0.0F, 0.0F, (float) AWAY);
        ConstructPainter painter = ConstructPainter.hand(pose, time);
        double sway = SWAY * Math.sin(time * SWAY_SPEED);
        switch (construct) {
            case SWORD_SHIELD -> swordShield(painter, sway);
            case ENERGY_WHIP -> place(painter, WHIP, 0.0, 0.0, 0.0, way(20.0, 0.0), way(110.0, 0.0), 1.0, sway);
            case GAUNTLETS -> {
                place(painter, GLOVE, 0.17, -0.05, -0.05, way(150.0, 10.0), way(60.0, 0.0), 0.56, sway);
                place(painter, LEFT_GLOVE, -0.17, -0.05, 0.0, way(30.0, 10.0), way(120.0, 0.0), 0.56, sway);
            }
            case DAGGERS -> {
                place(painter, DAGGER, 0.0, 0.0, -0.05, way(128.0, 0.0), way(218.0, 0.0), 0.95, sway);
                place(painter, DAGGER, 0.0, 0.0, 0.0, way(52.0, 0.0), way(142.0, 0.0), 0.95, sway);
            }
            case BATTLEAXE -> place(painter, BATTLEAXE, 0.0, 0.0, 0.0, way(50.0, 0.0), way(140.0, 0.0), 0.95, sway);
            case WAR_HAMMER -> place(painter, WAR_HAMMER, 0.0, 0.0, 0.0, way(50.0, 0.0), way(140.0, 0.0), 0.97, sway);
            case HALBERD -> place(painter, HALBERD, 0.0, 0.0, 0.0, way(50.0, 0.0), way(140.0, 0.0), 1.04, sway);
            case CHAINSAW -> place(painter, CHAINSAW, 0.0, 0.0, 0.0, way(18.0, 0.0), way(108.0, 18.0), 1.0, sway);
            case REVOLVERS -> {
                place(painter, REVOLVER, -0.02, 0.06, -0.05, way(140.0, 0.0), way(50.0, 0.0), 0.78, sway);
                place(painter, REVOLVER, 0.02, 0.06, 0.0, way(40.0, 0.0), way(130.0, 0.0), 0.78, sway);
            }
            case SHOTGUN -> place(painter, SHOTGUN, 0.0, 0.0, 0.0, way(15.0, 12.0), way(105.0, 45.0), 0.98, sway);
            case SMGS -> {
                place(painter, SMG, -0.2, 0.02, -0.02, way(165.0, 0.0), way(75.0, 10.0), 0.5, sway);
                place(painter, SMG, 0.2, 0.02, 0.0, way(15.0, 0.0), way(105.0, 10.0), 0.5, sway);
            }
            case ARM_CANNON -> {
                // A shot charging in the muzzle.
                ConstructPainter.Frame frame = place(painter, ARM_CANNON, 0.0, 0.0, 0.0, way(15.0, 38.0),
                        way(105.0, 5.0), 0.9, sway);
                painter.flare(frame.at(0.0, 0.0, 0.35), 0.18 * frame.scale(), 0.85);
            }
            case GRENADE_LAUNCHER -> place(painter, GRENADE_LAUNCHER, 0.0, 0.0, 0.0, way(15.0, 0.0), way(105.0, 12.0),
                    1.02, sway);
            case MINIGUN -> place(painter, MINIGUN, 0.0, 0.0, 0.0, way(20.0, 10.0), way(110.0, 12.0), 1.05, sway);
            case ROCKET_LAUNCHER -> place(painter, ROCKET_LAUNCHER, 0.0, 0.0, 0.0, way(30.0, 0.0), way(120.0, 10.0),
                    1.1, sway);
            case FLAMETHROWER -> {
                // The pilot flame burning at the nozzle.
                ConstructPainter.Frame frame = place(painter, FLAMETHROWER, 0.0, 0.0, 0.0, way(20.0, 0.0),
                        way(110.0, 10.0), 1.0, sway);
                painter.flare(frame.at(0.0, 0.02, 0.63), 0.12 * frame.scale(), 0.8);
            }
            default -> {
            }
        }
        painter.finish(graphics.bufferSource());
        pose.popPose();
    }

    /** The sword and shield, as hard light: the shield turning to and fro, the sword crossed behind it. */
    private static void swordShield(ConstructPainter painter, double sway) {
        Vec3 blade = new Vec3(0.67, 0.74, -0.08).normalize();
        Vec3 edge = new Vec3(-0.74, 0.67, 0.0);
        Vec3 grip = new Vec3(0.0, 0.0, -AWAY - 0.16).subtract(blade.scale(0.58 * 0.73));
        SwordPainter.sword(painter, grip, blade, edge, 0.73, 1.0, 0.0);
        SwordPainter.shield(painter, new Vec3(0.0, -0.01, -AWAY), new Vec3(Math.sin(sway), 0.1, Math.cos(sway)),
                new Vec3(0.0, 1.0, 0.0), 0.6, 1.0, 0.0);
    }

    /**
     * One model in the picture: the middle of its box at ({@code x}, {@code y}) and {@code depth} out of the screen, its
     * length along {@code forward} and its top towards {@code up} (as seen on the screen: x to the right, y up, z out of
     * it towards you), and {@code span} long. The whole picture turns by {@code sway} about the upright line through its
     * middle.
     *
     * @return where the model was drawn, for light that goes with it
     */
    private static ConstructPainter.Frame place(ConstructPainter painter, Model model, double x, double y, double depth,
            Vec3 forward, Vec3 up, double span, double sway) {
        Vec3 at = ConstructPainter.spin(new Vec3(x, y, depth), UPRIGHT, sway).add(0.0, 0.0, -AWAY);
        ConstructPainter.Frame turned = ConstructPainter.Frame.of(Vec3.ZERO,
                ConstructPainter.spin(forward, UPRIGHT, sway), ConstructPainter.spin(up, UPRIGHT, sway),
                span / model.length());
        Vec3 middle = turned.at(model.middle().x, model.middle().y, model.middle().z);
        ConstructPainter.Frame frame = new ConstructPainter.Frame(at.subtract(middle), turned.right(), turned.up(),
                turned.forward(), turned.scale());
        painter.ambient(0.12);
        painter.shape(model.shape(), frame, 1.0, 1.0);
        painter.ambient(0.0);
        return frame;
    }

    /** The way at {@code degrees} along the screen (0 = right, 90 = up), tipped {@code tip} degrees towards you. */
    private static Vec3 way(double degrees, double tip) {
        double along = Math.toRadians(degrees);
        double out = Math.toRadians(tip);
        return new Vec3(Math.cos(along) * Math.cos(out), Math.sin(along) * Math.cos(out), Math.sin(out));
    }

    /** A model as a picture shows it: its shape, the middle of the box round it, and its longest way across. */
    private record Model(ConstructPainter.Shape shape, Vec3 middle, double length) {
        static Model of(ConstructPainter.Shape shape) {
            double[] low = { Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE };
            double[] high = { -Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE };
            for (Mesh mesh : shape.meshes()) {
                for (int i = 0; i < mesh.px.length; i++) {
                    double[] point = { mesh.px[i], mesh.py[i], mesh.pz[i] };
                    for (int k = 0; k < 3; k++) {
                        low[k] = Math.min(low[k], point[k]);
                        high[k] = Math.max(high[k], point[k]);
                    }
                }
            }
            Vec3 middle = new Vec3((low[0] + high[0]) * 0.5, (low[1] + high[1]) * 0.5, (low[2] + high[2]) * 0.5);
            double length = Math.max(high[0] - low[0], Math.max(high[1] - low[1], high[2] - low[2]));
            return new Model(shape, middle, length);
        }
    }
}
