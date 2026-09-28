package nl.tivek.multiversepowers.character.greenlantern.client.express;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.construct.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.express.ExpressScript;
import nl.tivek.multiversepowers.config.client.ClientSettings;
import nl.tivek.multiversepowers.engine.fx.Sounds;

// What each game hears of a running Express on its own: a low rumble that rides along the train at the point nearest
// the listener, loud while it runs and dying away as it stops, and the clatter of each bogie over the rail joints.
public final class ExpressSounds {
    private static final SoundEvent RUMBLE = Sounds.of("express.rumble");
    private static final SoundEvent CLACK = Sounds.of("express.clack");
    private static final double JOINT = 6.0;
    private static final double HEARD = 48.0;
    private static final int MOST_CLACKS = 3;
    private static final Map<Integer, Rumble> RUMBLES = new HashMap<>();
    private static final Map<Integer, Double> LAST = new HashMap<>();
    // Every bogie of the train, back from its nose in model blocks.
    private static final double[] BOGIES = bogies();

    private ExpressSounds() {
    }

    private static double[] bogies() {
        double[] bogies = new double[4 + ExpressScript.COACHES * 2];
        bogies[0] = ExpressScript.BOGIE;
        bogies[1] = ExpressScript.DRIVER;
        bogies[2] = ExpressScript.TENDER_MIDDLE - 2.2;
        bogies[3] = ExpressScript.TENDER_MIDDLE + 2.2;
        for (int k = 0; k < ExpressScript.COACHES; k++) {
            bogies[4 + k * 2] = ExpressScript.coachMiddle(k) - ExpressScript.COACH_BOGIE;
            bogies[5 + k * 2] = ExpressScript.coachMiddle(k) + ExpressScript.COACH_BOGIE;
        }
        return bogies;
    }

    private static final class Rumble extends AbstractTickableSoundInstance {
        private final int id;
        private float wanted;
        private boolean done;

        private Rumble(int id, Vec3 at) {
            super(RUMBLE, SoundSource.PLAYERS, RandomSource.create());
            this.id = id;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.01F;
            this.x = at.x;
            this.y = at.y;
            this.z = at.z;
        }

        @Override
        public void tick() {
            if (this.done || ExpressTrails.of(this.id) == null) {
                this.stop();
                return;
            }
            this.volume += Mth.clamp(this.wanted - this.volume, -0.12F, 0.2F);
            if (this.wanted <= 0.0F && this.volume <= 0.02F) {
                this.done = true;
                this.stop();
            }
        }

        private void at(Vec3 at, float volume, float pitch) {
            this.x = at.x;
            this.y = at.y;
            this.z = at.z;
            this.wanted = volume * ClientSettings.factor(ClientSettings.POWER_HUM);
            this.pitch = pitch;
        }
    }

    public static void tick(Minecraft minecraft, ConstructPayload was, ConstructPayload now) {
        ExpressTrails.Trail trail = ExpressTrails.of(now.id());
        ClientLevel level = minecraft.level;
        if (trail == null || level == null) {
            return;
        }
        Vec3 ear = minecraft.gameRenderer.getMainCamera().getPosition();
        double s = Math.max(0.1, now.size());
        double o = now.charge();
        double pace = Mth.clamp((o - was.charge()) / ExpressScript.TOP_SPEED, 0.0, 1.0);
        boolean rolling = ExpressScript.phase(now.variant()) <= ExpressScript.BRAKE;
        Vec3 nearest = null;
        double best = Double.MAX_VALUE;
        for (int k = 0; k <= 16; k++) {
            double back = Math.min(o, ExpressScript.TRAIN_LENGTH * s * k / 16.0);
            Vec3 point = trail.at(o - back);
            double far = point.distanceToSqr(ear);
            if (far < best) {
                best = far;
                nearest = point;
            }
        }
        Rumble rumble = RUMBLES.get(now.id());
        float loud = rolling ? (float) (0.5 + 2.2 * pace) : 0.0F;
        if (rumble == null && loud > 0.05F && nearest != null) {
            rumble = new Rumble(now.id(), nearest);
            RUMBLES.put(now.id(), rumble);
            minecraft.getSoundManager().play(rumble);
        }
        if (rumble != null && nearest != null) {
            rumble.at(nearest.add(0.0, 1.5, 0.0), loud, (float) (0.8 + 0.35 * pace));
        }
        Double before = LAST.put(now.id(), o);
        if (before == null || !rolling || o <= before) {
            return;
        }
        int clacks = 0;
        for (double bogie : BOGIES) {
            double from = before - bogie * s;
            double to = o - bogie * s;
            if (to < 0.0 || Math.floor(to / JOINT) == Math.floor(from / JOINT)) {
                continue;
            }
            Vec3 at = trail.at(to);
            if (at.distanceToSqr(ear) > HEARD * HEARD || clacks >= MOST_CLACKS) {
                continue;
            }
            clacks++;
            level.playLocalSound(at.x, at.y, at.z, CLACK, SoundSource.PLAYERS, (float) (0.5 + 0.7 * pace),
                    (float) (0.9 + 0.2 * level.random.nextDouble()), false);
        }
    }

    public static void forget(int id) {
        Rumble rumble = RUMBLES.remove(id);
        if (rumble != null) {
            rumble.wanted = 0.0F;
        }
        LAST.remove(id);
    }

    public static void clear() {
        for (Iterator<Rumble> it = RUMBLES.values().iterator(); it.hasNext();) {
            it.next().done = true;
            it.remove();
        }
        LAST.clear();
    }
}
