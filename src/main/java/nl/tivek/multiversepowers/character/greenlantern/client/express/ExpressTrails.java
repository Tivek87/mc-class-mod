package nl.tivek.multiversepowers.character.greenlantern.client.express;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.construct.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.express.ExpressScript;

public final class ExpressTrails {
    private static final Map<Integer, Trail> TRAILS = new HashMap<>();
    private static final double KEEP = (ExpressScript.TRAIN_LENGTH + 8.0) * ExpressScript.SCALE;

    private ExpressTrails() {
    }

    public static final class Trail {
        private final List<double[]> points = new ArrayList<>();
        private final int[] since = new int[ExpressScript.BOOM + 1];
        private Vec3 way;
        private int phase = -1;
        private int rams;
        private int ramAt = Integer.MIN_VALUE;
        private double derailedAt = Double.NaN;

        private Trail(ConstructPayload first) {
            this.way = first.facing();
            Arrays.fill(this.since, Integer.MIN_VALUE);
            // Seen for the first time halfway: what came before is long over.
            this.phase = ExpressScript.phase(first.variant());
            for (int p = 0; p < this.phase; p++) {
                this.since[p] = first.age() - 1000;
            }
            this.since[this.phase] = first.age();
            if (this.phase >= ExpressScript.TIP) {
                this.derailedAt = first.charge();
            }
            this.rams = ExpressScript.rams(first.variant());
        }

        private void add(ConstructPayload payload) {
            int phase = ExpressScript.phase(payload.variant());
            if (phase != this.phase) {
                for (int p = this.phase + 1; p <= phase; p++) {
                    this.since[p] = payload.age();
                }
                if (phase >= ExpressScript.TIP && Double.isNaN(this.derailedAt)) {
                    this.derailedAt = payload.charge();
                }
                this.phase = phase;
            }
            int rams = ExpressScript.rams(payload.variant());
            if (rams > this.rams) {
                this.rams = rams;
                this.ramAt = payload.age();
            }
            double odometer = payload.charge();
            if (!this.points.isEmpty() && odometer <= this.points.getLast()[3] + 1.0E-4) {
                return;
            }
            Vec3 at = payload.center();
            this.points.add(new double[] { at.x, at.y, at.z, odometer });
            this.way = payload.facing();
            while (this.points.size() > 2 && this.points.get(1)[3] < odometer - KEEP) {
                this.points.removeFirst();
            }
        }

        public Vec3 at(double odometer) {
            int count = this.points.size();
            if (count == 0) {
                return Vec3.ZERO;
            }
            double[] first = this.points.getFirst();
            if (odometer <= first[3] || count == 1) {
                Vec3 back = count > 1 ? direction(first, this.points.get(1)) : this.way;
                return point(first).add(back.scale(odometer - first[3]));
            }
            double[] last = this.points.getLast();
            if (odometer >= last[3]) {
                return point(last).add(direction(this.points.get(count - 2), last).scale(odometer - last[3]));
            }
            int low = 0;
            int high = count - 1;
            while (high - low > 1) {
                int middle = (low + high) >>> 1;
                if (this.points.get(middle)[3] <= odometer) {
                    low = middle;
                } else {
                    high = middle;
                }
            }
            double[] a = this.points.get(low);
            double[] b = this.points.get(high);
            double t = (odometer - a[3]) / Math.max(1.0E-9, b[3] - a[3]);
            return new Vec3(a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t);
        }

        private static Vec3 point(double[] p) {
            return new Vec3(p[0], p[1], p[2]);
        }

        private static Vec3 direction(double[] a, double[] b) {
            Vec3 way = new Vec3(b[0] - a[0], b[1] - a[1], b[2] - a[2]);
            return way.lengthSqr() < 1.0E-12 ? new Vec3(0.0, 0.0, 1.0) : way.normalize();
        }

        // The age its phase began, or MIN_VALUE while it has not come yet.
        public int since(int phase) {
            return this.since[phase];
        }

        public int ramAt() {
            return this.ramAt;
        }

        // How far it had come when it left the rails, or NaN while it is still on them.
        public double derailedAt() {
            return this.derailedAt;
        }
    }

    public static void heard(ConstructPayload payload) {
        TRAILS.computeIfAbsent(payload.id(), id -> new Trail(payload)).add(payload);
    }

    @Nullable
    public static Trail of(int id) {
        return TRAILS.get(id);
    }

    public static void forget(int id) {
        TRAILS.remove(id);
    }

    public static void clear() {
        TRAILS.clear();
    }
}
