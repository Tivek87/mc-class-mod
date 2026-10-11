package nl.tivek.multiversepowers.character.greenlantern.client.body.heavy;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.character.greenlantern.client.render.weapon.GunParts;
import nl.tivek.multiversepowers.character.greenlantern.heavy.HeavyShots;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Frame;
import nl.tivek.multiversepowers.engine.math.Ease;
import static nl.tivek.multiversepowers.character.greenlantern.heavy.HeavyMoves.*;

// The revolvers, the arm cannon and the minigun drawn with their moving parts: each revolver's cylinder turning a
// chamber on with every shot and its hammer falling and cocked again, the cylinders swung out on their cranes to be
// shaken empty and filled with rounds of light; the cannon's vents opening and its core blazing as it charges or
// pours out its rapid stream; the minigun's barrels spinning up, glowing with heat and
// throwing out spent casings.
final class GunPainter {
    private static final double CHAMBER = Math.PI / 3.0;
    private static final double COCKED = -0.65;
    private static final double CRANE_OPEN = -1.45;
    private static final double VENT_OPEN = 0.55;
    // The minigun's barrels at full speed, radians a tick.
    private static final double SPIN = 1.35;
    private static final Vec3 PORT = new Vec3(-0.11, 0.02, -0.2);
    // A round leaving the barrels in his own view: how fast (the gun's lengths a tick) and how far it is drawn.
    private static final double LEAVE = 4.0;
    private static final double LEAVE_MOST = 4.0;

    private GunPainter() {
    }

    // `side`'s gun (0 right, 1 left; the cannon and minigun have only the right) in `frame`, grown `formed` of the
    // way or broken `apart`.
    static void draw(LanternPainter painter, ClientHeavy.Held held, int side, Frame frame, double formed, double apart,
            float partialTick, int seed) {
        double age = held.age(partialTick);
        switch (held.weapon) {
            case REVOLVERS -> revolver(painter, held, side, frame, formed, apart, age, seed + 31 * side);
            case CANNON -> cannon(painter, held, frame, formed, apart, age, partialTick, seed);
            default -> minigun(painter, held, frame, formed, apart, age, partialTick, seed);
        }
    }

    // Where a gun's shots leave it, in its frame.
    static Vec3 muzzle(int weapon) {
        return switch (weapon) {
            case REVOLVERS -> GunParts.REVOLVER_MUZZLE;
            case CANNON -> GunParts.CANNON_MUZZLE;
            default -> GunParts.MINIGUN_MUZZLE;
        };
    }

    private static boolean grow(LanternPainter painter, int weapon, Frame frame, double formed) {
        if (formed >= 1.0) {
            return false;
        }
        double[] ends = HeavyPainter.ends(weapon);
        painter.clip(frame.at(0.0, 0.0, Mth.lerp(Ease.smooth(formed), ends[0], ends[1])), frame.forward().scale(-1.0),
                1.0);
        return true;
    }

    private static void revolver(LanternPainter painter, ClientHeavy.Held held, int side, Frame frame, double formed,
            double apart, double age, int seed) {
        double reload = HeavyPoses.reload(held, 0.0F) >= 0.0 ? age : -1.0;
        double open = reload < 0.0 ? 0.0 : Ease.smooth((reload - 3.0) / 3.0) * (1.0 - Ease.smooth((reload - 23.0)
                / 2.0));
        Frame crane = frame.turned(GunParts.CRANE.x, GunParts.CRANE.y, GunParts.CRANE.z, 0.0, 0.0, 1.0,
                CRANE_OPEN * open);
        double last = GunFire.last(held, side, age);
        int spent = ammo(REVOLVERS) - held.ammo;
        int mine = side == 0 ? (spent + 1) / 2 : spent / 2;
        double turning = last < 2.5 ? Ease.smooth(last / 2.5) - 1.0 : 0.0;
        // A snapped-shut cylinder spins a turn and a half on its own before it stops.
        double spun = reload >= 25.0 ? 9.0 * CHAMBER * Ease.smooth((reload - 25.0) / 6.0) : 0.0;
        double roll = -CHAMBER * (reload >= 0.0 ? 0.0 : mine + turning) - spun;
        Frame cylinder = crane.turned(0.0, GunParts.CYLINDER_Y, 0.0, 0.0, 0.0, 1.0, roll);
        double fall = last < 3.0 ? 1.0 - Ease.smooth((last - 0.8) / 2.2) : 0.0;
        Frame hammer = frame.turned(GunParts.HAMMER.x, GunParts.HAMMER.y, GunParts.HAMMER.z, 1.0, 0.0, 0.0,
                COCKED * (1.0 - fall) * (reload >= 0.0 ? 0.5 : 1.0));
        if (apart >= 0.0) {
            if (apart < 1.0) {
                painter.shattered(GunParts.REVOLVER_FRAME, frame, apart, 1.0, seed);
                painter.shattered(GunParts.REVOLVER_CYLINDER, cylinder, apart, 1.0, seed + 1);
                painter.shattered(GunParts.REVOLVER_HAMMER, hammer, apart, 1.0, seed + 2);
            }
            return;
        }
        grow(painter, REVOLVERS, frame, formed);
        painter.shape(GunParts.REVOLVER_FRAME, frame, 1.0, 1.0);
        painter.shape(GunParts.REVOLVER_CYLINDER, cylinder, 1.0, 1.0 + 0.5 * flash(last));
        painter.shape(GunParts.REVOLVER_HAMMER, hammer, 1.0, 1.0);
        painter.noClip();
        if (reload >= 0.0) {
            rounds(painter, cylinder, reload, seed);
        }
        double flash = flash(last);
        if (flash > 0.01) {
            Vec3 muzzle = frame.at(GunParts.REVOLVER_MUZZLE.x, GunParts.REVOLVER_MUZZLE.y,
                    GunParts.REVOLVER_MUZZLE.z + 0.04);
            painter.flare(muzzle, 0.1 + 0.16 * flash, flash);
            painter.flare(frame.at(0.0, GunParts.CYLINDER_Y, 0.08), 0.05 + 0.05 * flash, 0.6 * flash);
        }
        // Dead-eye: the hammers held back and the guns' light drawn tight round their muzzles.
        if (held.move == BRACE) {
            double on = Ease.smooth(age / 4.0) * (0.75 + 0.25 * Math.sin(age * 0.9 + side));
            painter.flare(frame.at(GunParts.REVOLVER_MUZZLE.x, GunParts.REVOLVER_MUZZLE.y, GunParts.REVOLVER_MUZZLE.z),
                    0.07, 0.6 * on);
        }
    }

    // The rounds as a revolver reloads: the spent six slide out of the opened cylinder and fall away breaking up,
    // then new ones of light grow into the chambers one after the other.
    private static void rounds(LanternPainter painter, Frame cylinder, double reload, int seed) {
        for (int k = 0; k < ammo(REVOLVERS) / 2; k++) {
            double angle = Math.PI / 2.0 + CHAMBER * k;
            double x = Math.cos(angle) * GunParts.CHAMBER_RING;
            double y = GunParts.CYLINDER_Y + Math.sin(angle) * GunParts.CHAMBER_RING;
            double out = reload - 9.0 - 0.25 * k;
            if (out > -3.0 && out < 6.0) {
                double s = Math.max(0.0, out);
                Frame round = cylinder.moved(x, y - 0.012 * s * s, -0.075 - 0.07 * s);
                if (s < 2.5) {
                    painter.shape(GunParts.ROUND, round, 1.0, 0.8);
                } else {
                    painter.shattered(GunParts.ROUND, round, (s - 2.5) / 3.5, 0.8, seed + 7 + k);
                }
            }
            double in = reload - 13.0 - 1.4 * k;
            if (in > 0.0 && reload < 27.0) {
                double grown = Ease.smooth(in / 1.6);
                Frame round = cylinder.moved(x, y, -0.075 - 0.05 * (1.0 - grown));
                painter.shape(GunParts.ROUND, new Frame(round.center(), round.right(), round.up(), round.forward(),
                        round.scale() * Math.max(0.05, grown)), 1.0, 1.0 + 0.8 * (1.0 - grown));
            }
        }
    }

    private static double flash(double since) {
        return since < 0.0 || since > 2.2 ? 0.0 : 1.0 - Ease.smooth(since / 2.2);
    }

    private static void cannon(LanternPainter painter, ClientHeavy.Held held, Frame frame, double formed,
            double apart, double age, float partialTick, int seed) {
        double charge = charge(held, age);
        double last = GunFire.last(held, 0, age);
        double flash = flash(last * 0.8);
        double vent = VENT_OPEN * Math.max(charge, flash * 0.8);
        Frame right = frame.turned(GunParts.VENT_HINGE.x, GunParts.VENT_HINGE.y, GunParts.VENT_HINGE.z, 0.0, 1.0, 0.0,
                vent);
        Frame left = frame.turned(-GunParts.VENT_HINGE.x, GunParts.VENT_HINGE.y, GunParts.VENT_HINGE.z, 0.0, 1.0, 0.0,
                -vent);
        Frame core = frame.moved(GunParts.CANNON_CORE.x, GunParts.CANNON_CORE.y, GunParts.CANNON_CORE.z);
        if (apart >= 0.0) {
            if (apart < 1.0) {
                painter.shattered(GunParts.CANNON_BODY, frame, apart, 1.0, seed);
                painter.shattered(GunParts.CANNON_VENT, right, apart, 1.0, seed + 1);
                painter.shattered(GunParts.CANNON_VENT_LEFT, left, apart, 1.0, seed + 2);
            }
            return;
        }
        grow(painter, CANNON, frame, formed);
        painter.shape(GunParts.CANNON_BODY, frame, 1.0, 1.0);
        painter.shape(GunParts.CANNON_VENT, right, 1.0, 1.0 + 0.6 * charge);
        painter.shape(GunParts.CANNON_VENT_LEFT, left, 1.0, 1.0 + 0.6 * charge);
        double time = held.age(partialTick);
        double pulse = 1.0 + 0.15 * charge * Math.sin(time * (1.2 + 1.6 * charge));
        painter.shape(GunParts.CANNON_CORE_BALL, scaled(core, (0.7 + 0.6 * charge) * pulse), 1.0,
                1.2 + 0.8 * Math.max(charge, flash));
        painter.noClip();
        if (formed < 1.0) {
            return;
        }
        painter.flare(core.center(), 0.08 + 0.22 * charge * pulse, 0.3 + 0.7 * Math.max(charge, flash));
        // Light drawn in from round the muzzle as it charges, faster and closer the fuller it is.
        if (charge > 0.02) {
            for (int k = 0; k < 6; k++) {
                double phase = (time * (0.08 + 0.1 * charge) + k / 6.0) % 1.0;
                double angle = k * 1.047 + time * 0.25;
                double r = 0.42 * (1.0 - phase);
                painter.flare(frame.at(Math.cos(angle) * r, Math.sin(angle) * r, GunParts.CANNON_MUZZLE.z + 0.25
                        * (1.0 - phase)), 0.03 + 0.03 * charge, charge * Ease.bump(phase * 2.0 - 1.0));
            }
        }
        if (flash > 0.01) {
            double big = held.move == LOOSE ? 1.0 + 1.5 * charged(held) : 1.0;
            painter.flare(frame.at(0.0, 0.0, GunParts.CANNON_MUZZLE.z + 0.06), (0.18 + 0.22 * flash) * big, flash);
        }
    }

    // How full the cannon's charge is: rising as he holds, let go, draining as it fires.
    private static double charge(ClientHeavy.Held held, double age) {
        return switch (held.move) {
            case AIM -> Mth.clamp(age / HeavyShots.CHARGE, 0.0, 1.0);
            case LOOSE -> charged(held) * (1.0 - Ease.smooth((age - 1.0) / 3.0));
            default -> 0.0;
        };
    }

    // How full the charge was as he let go of it.
    static double charged(ClientHeavy.Held held) {
        return held.move == LOOSE && held.last == AIM
                ? Mth.clamp((held.start - held.lastStart) / HeavyShots.CHARGE, 0.0, 1.0) : 0.0;
    }

    private static void minigun(LanternPainter painter, ClientHeavy.Held held, Frame frame, double formed,
            double apart, double age, float partialTick, int seed) {
        double now = ClientHeavy.now(partialTick);
        double speed = held.brokeAt >= 0.0 ? 0.0 : HeavyShots.spin(held.move, age, held.spun());
        if (held.spinAt >= 0.0 && now > held.spinAt) {
            held.spinAngle = (held.spinAngle + (now - held.spinAt) * speed * SPIN) % (Math.PI * 2.0);
        }
        held.spinAt = now;
        double heat = held.move == RELOAD ? 1.0 - Ease.smooth(age / 30.0) : held.ammo / 100.0;
        Frame barrels = frame.turned(0.0, 0.0, 0.0, 0.0, 0.0, 1.0, held.spinAngle);
        if (apart >= 0.0) {
            if (apart < 1.0) {
                painter.shattered(GunParts.MINIGUN_BODY, frame, apart, 1.0, seed);
                painter.shattered(GunParts.MINIGUN_BARRELS, barrels, apart, 1.0, seed + 1);
            }
            return;
        }
        grow(painter, MINIGUN, frame, formed);
        painter.shape(GunParts.MINIGUN_BODY, frame, 1.0, 1.0);
        painter.shape(GunParts.MINIGUN_BARRELS, barrels, 1.0, 1.0 + 0.9 * heat * heat);
        painter.noClip();
        if (formed < 1.0) {
            return;
        }
        if (heat > 0.5) {
            double glow = (heat - 0.5) * 2.0;
            painter.flare(frame.at(0.0, 0.0, GunParts.MINIGUN_MUZZLE.z - 0.15), 0.12 + 0.1 * glow,
                    0.5 * glow * (0.8 + 0.2 * Math.sin(age * 1.7)));
        }
        double last = GunFire.last(held, 0, age);
        if (last < 1.0) {
            double flicker = 0.75 + 0.25 * Math.sin(age * 9.1);
            painter.flare(frame.at(0.0, 0.0, GunParts.MINIGUN_MUZZLE.z + 0.06), (0.14 + 0.08 * flicker) * (1.0 - last),
                    flicker * (1.0 - last));
        }
        if (held.move == KICK) {
            double vent = Ease.jolt((age - hit(MINIGUN, KICK) + 1.0) / 8.0);
            painter.flare(frame.at(0.0, 0.0, GunParts.MINIGUN_MUZZLE.z + 0.1), 0.25 + 0.35 * vent, vent);
        }
        casings(painter, held, frame, age, seed);
    }

    // Rounds just out of the minigun's barrels as he sees it himself: the gun in his hands is drawn over the world, so
    // the world's rounds only show once clear of it.
    static void leaving(LanternPainter painter, ClientHeavy.Held held, Frame frame, double age) {
        if (held.weapon != MINIGUN || held.brokeAt >= 0.0 || held.move < 0 || held.move >= MOVES) {
            return;
        }
        int now = (int) Math.floor(age);
        for (int back = 0; back < 2; back++) {
            int tick = now - back;
            if (tick < 0 || !HeavyShots.fires(MINIGUN, held.move, tick, held.spun(), 0)) {
                continue;
            }
            double out = (age - tick) * LEAVE;
            if (out > LEAVE_MOST) {
                continue;
            }
            double z = GunParts.MINIGUN_MUZZLE.z + out;
            painter.shape(GunParts.ROUND, frame.moved(0.0, 0.0, z), 1.0, 1.6);
            painter.flare(frame.at(0.0, 0.0, z), 0.08, 0.9);
            painter.edge(frame.at(0.0, 0.0, Math.max(GunParts.MINIGUN_MUZZLE.z, z - 0.5)), frame.at(0.0, 0.0, z),
                    0.025, 0.9);
        }
    }

    // Spent casings thrown out of the minigun's right side, tumbling down and breaking up.
    private static void casings(LanternPainter painter, ClientHeavy.Held held, Frame frame, double age, int seed) {
        if (held.move < 0 || held.move >= MOVES) {
            return;
        }
        int now = (int) Math.floor(age);
        for (int back = 0; back < 7; back++) {
            int tick = now - back;
            if (tick < 0 || !HeavyShots.fires(MINIGUN, held.move, tick, held.spun(), 0)) {
                continue;
            }
            double s = age - tick;
            double tumble = s * (2.2 + 0.4 * ((tick * 7) % 5));
            Frame casing = frame.moved(PORT.x - 0.09 * s, PORT.y + 0.05 * s - 0.035 * s * s, PORT.z - 0.02 * s)
                    .turned(0.0, 0.0, 0.0, 0.3, 1.0, 0.2, tumble);
            if (s < 4.0) {
                painter.shape(GunParts.CASING, casing, 1.0, 1.0);
            } else {
                painter.shattered(GunParts.CASING, casing, (s - 4.0) / 3.0, 1.0, seed + tick);
            }
        }
    }

    private static Frame scaled(Frame frame, double by) {
        return new Frame(frame.center(), frame.right(), frame.up(), frame.forward(), frame.scale() * by);
    }
}
