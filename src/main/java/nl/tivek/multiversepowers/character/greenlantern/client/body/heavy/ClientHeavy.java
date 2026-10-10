package nl.tivek.multiversepowers.character.greenlantern.client.body.heavy;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.construct.Construct;
import nl.tivek.multiversepowers.character.greenlantern.heavy.HeavyMoves;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Frame;
import nl.tivek.multiversepowers.engine.math.Ease;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// The heavy weapons every player near holds, as the server told them (HeavyPayload): which weapon, its move and since
// when, the move before it (each flows out of the last), and since when it formed or broke up. Each frame his pose
// leaves where his hands really are on it, for the weapon to be drawn between them.
public final class ClientHeavy {
    // Ticks the weapon takes to grow out of the ring's light.
    static final double FORM = 8.0;
    // Without a word from the server this long, a weapon is taken to be gone.
    private static final double STALE = 70.0;
    // A pick the server has not answered this long is taken as refused.
    private static final double PICK_WAIT = 30.0;

    public static final class Held {
        final int weapon;
        int move = HeavyMoves.FORM;
        double start;
        int last = HeavyMoves.IDLE;
        double lastStart;
        float yaw;
        double formedAt;
        double brokeAt = -1.0;
        double told;
        // Where the earthbreaker struck, once it has.
        @Nullable
        Vec3 impact;
        double impactAt;
        // The weapon as last posed, in his model's space (blocks), and whether it is.
        Frame frame = new Frame(Vec3.ZERO, new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 1.0, 0.0), new Vec3(0.0, 0.0, 1.0),
                1.0);
        boolean posed;
        // His chest's turn and his neck as last posed, for where the weapon was a moment ago (its trail).
        final Quaternionf chest = new Quaternionf();
        final Vector3f neck = new Vector3f();

        Held(int weapon) {
            this.weapon = weapon;
        }

        public int weapon() {
            return this.weapon;
        }

        public int move() {
            return this.move;
        }

        double age(float partialTick) {
            return now(partialTick) - this.start;
        }

        double lastAge(float partialTick) {
            return now(partialTick) - this.lastStart;
        }

        // How far the weapon has grown, 0 to 1.
        double formed(float partialTick) {
            return Ease.smooth((now(partialTick) - this.formedAt) / FORM);
        }

        // How far it has broken apart, or below 0 while whole.
        double apart(float partialTick) {
            return this.brokeAt < 0.0 ? -1.0 : (now(partialTick) - this.brokeAt) / HeavyMoves.BREAK_TICKS;
        }
    }

    private static final Map<Integer, Held> HELD = new HashMap<>();
    private static int pickedWeapon = -1;
    private static double pickedAt;

    private ClientHeavy() {
    }

    public static void told(int owner, int weapon, int move, int age, float yaw) {
        double now = now(0.0F);
        Held held = HELD.get(owner);
        if (move == HeavyMoves.BREAK) {
            if (held != null && held.brokeAt < 0.0) {
                held.brokeAt = now - age;
            }
            return;
        }
        if (held == null || held.brokeAt >= 0.0 || held.weapon != weapon) {
            held = new Held(weapon);
            held.formedAt = move == HeavyMoves.FORM ? now - age : now - FORM;
            held.move = move;
            held.start = now - age;
            HELD.put(owner, held);
        } else if (held.move != move || Math.abs(held.start - (now - age)) > 3.0) {
            boolean fresh = held.move != move;
            if (fresh) {
                held.last = held.move;
                held.lastStart = held.start;
                if (move == HeavyMoves.LEAP) {
                    held.impact = null;
                }
            }
            held.move = move;
            held.start = now - age;
        }
        held.told = now;
        held.yaw = yaw;
    }

    // The weapon in this entity's hands, while it or its pieces are about.
    @Nullable
    public static Held view(Entity entity) {
        Held held = HELD.get(entity.getId());
        if (held == null) {
            return null;
        }
        double now = now(0.0F);
        if (held.apart(0.0F) >= 1.0 || now - held.told > STALE) {
            HELD.remove(entity.getId());
            return null;
        }
        return held;
    }

    // The weapon the own player holds whole (HeavyMoves.AXE or SAW), or -1.
    public static int holding() {
        Entity player = Minecraft.getInstance().player;
        Held held = player == null ? null : view(player);
        return held == null || held.brokeAt >= 0.0 ? -1 : held.weapon;
    }

    public static int weaponOf(Construct construct) {
        return construct == Construct.BATTLEAXE ? HeavyMoves.AXE
                : construct == Construct.CHAINSAW ? HeavyMoves.SAW : -1;
    }

    // The own player took this from the wheel.
    public static void picked(Construct construct) {
        pickedWeapon = weaponOf(construct);
        pickedAt = now(0.0F);
    }

    // Whether the own player has this weapon, or has just asked for it.
    public static boolean present(Construct construct) {
        int weapon = weaponOf(construct);
        return holding() == weapon || pickedWeapon == weapon && now(0.0F) - pickedAt < PICK_WAIT;
    }

    // Every weapon still about; one whose owner went out of sight is dropped here once its time is up.
    static Map<Integer, Held> all() {
        double now = now(0.0F);
        HELD.values().removeIf(held -> held.apart(0.0F) >= 1.0 || now - held.told > STALE);
        return HELD;
    }

    public static void clear() {
        HELD.clear();
        pickedWeapon = -1;
    }

    static double now(float partialTick) {
        ClientLevel level = Minecraft.getInstance().level;
        return level == null ? 0.0 : level.getGameTime() + partialTick;
    }
}
