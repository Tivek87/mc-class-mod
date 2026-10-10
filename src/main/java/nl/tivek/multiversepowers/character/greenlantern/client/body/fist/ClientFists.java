package nl.tivek.multiversepowers.character.greenlantern.client.body.fist;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.fist.FistMoves;
import nl.tivek.multiversepowers.character.greenlantern.fist.FistPayload;
import nl.tivek.multiversepowers.engine.client.world.ClientClock;
import nl.tivek.multiversepowers.engine.math.Ease;

// The fist blows every player near throws, as the server told them (FistPayload): which blow, since when, the one
// before it, and where he stood and faced for a heavy one's giant fist.
public final class ClientFists {
    // Ticks the gloves take to grow out of the ring's light, and to break apart once the guard is down.
    static final double FORM = 3.0;
    static final double BREAK = 10.0;

    public record View(int move, double start, int last, double lastStart, Vec3 at, float yaw) {
        public double age(float partialTick) {
            return now(partialTick) - this.start;
        }

        double lastAge(float partialTick) {
            return now(partialTick) - this.lastStart;
        }

        // Ticks since the blow and its guard are over (below 0 while they last).
        double after(float partialTick) {
            return this.age(partialTick) - FistMoves.length(this.move) - FistMoves.GUARD;
        }

        // How far the gloves have formed, 0 to 1; out of a guard still up they are whole at once.
        double formed(float partialTick) {
            return this.last >= 0 ? 1.0 : Ease.smooth(this.age(partialTick) / FORM);
        }

        // How far the gloves have broken apart, or below 0 while whole.
        double apart(float partialTick) {
            double after = this.after(partialTick);
            return after < 0.0 ? -1.0 : after / BREAK;
        }
    }

    private static final Map<Integer, View> VIEWS = new HashMap<>();

    private ClientFists() {
    }

    public static void told(int owner, int move, float yaw) {
        ClientLevel level = Minecraft.getInstance().level;
        Entity entity = level == null ? null : level.getEntity(owner);
        Vec3 at = entity == null ? Vec3.ZERO : entity.position();
        View was = VIEWS.get(owner);
        boolean on = was != null && was.after(0.0F) < 0.0;
        if (move == FistPayload.AWAY) {
            if (on) {
                VIEWS.put(owner, new View(was.move(), now(0.0F) - FistMoves.length(was.move()) - FistMoves.GUARD,
                        -1, 0.0, was.at(), was.yaw()));
            }
            return;
        }
        VIEWS.put(owner, new View(move, now(0.0F), on ? was.move() : -1, on ? was.start() : 0.0, at, yaw));
    }

    // The fists of this entity, while they or their pieces are still about.
    @Nullable
    public static View view(Entity entity) {
        View view = VIEWS.get(entity.getId());
        if (view == null) {
            return null;
        }
        double age = view.age(0.0F);
        if (age < -1.0 || view.apart(0.0F) >= 1.0) {
            VIEWS.remove(entity.getId());
            return null;
        }
        return view;
    }

    // Whether this entity's gloves are on: in a blow or the guard after it.
    public static boolean gloved(Entity entity) {
        View view = view(entity);
        return view != null && view.after(0.0F) < 0.0;
    }

    // Every view still about; one whose owner went out of sight is dropped here once its time is up.
    static Map<Integer, View> all() {
        VIEWS.values().removeIf(view -> view.age(0.0F) < -1.0 || view.apart(0.0F) >= 1.0);
        return VIEWS;
    }

    public static void clear() {
        VIEWS.clear();
    }

    static double now(float partialTick) {
        return ClientClock.now(partialTick);
    }
}
