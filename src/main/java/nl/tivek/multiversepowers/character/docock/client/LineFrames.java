package nl.tivek.multiversepowers.character.docock.client;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Frames carried along a line of points, for the tentacles {@link ArmPainter} draws: the direction and
 * the side of every piece, turned as little as possible from one piece to the next.
 */
abstract class LineFrames {
    // ---- Frames along a line ----

    /** {@code v} made square to the unit vector {@code d}; {@code null} when it lies along it. */
    @Nullable
    static Vec3 square(Vec3 v, Vec3 d) {
        Vec3 result = v.subtract(d.scale(v.dot(d)));
        return result.lengthSqr() < 1.0E-6 ? null : result.normalize();
    }

    /** Any unit vector square to the unit vector {@code d}; always the same one for the same {@code d}. */
    static Vec3 anySquare(Vec3 d) {
        Vec3 result = square(new Vec3(0, 1, 0), d);
        return result != null ? result : square(new Vec3(1, 0, 0), d);
    }

    /**
     * Direction and side of every piece of the line, the side carried from piece to piece with as
     * little turning as possible. {@code reference} sets the side at the mount (null: any).
     */
    static final class Frames {
        final double[] at;
        final Vec3[] direction;
        final Vec3[] side;
        final double length;
        private final List<Vec3> points;

        Frames(List<Vec3> points, @Nullable Vec3 reference) {
            this.points = points;
            int n = points.size();
            this.at = new double[n];
            this.direction = new Vec3[Math.max(1, n - 1)];
            this.side = new Vec3[Math.max(1, n - 1)];
            Vec3 d = null;
            for (int i = 1; i < n; i++) {
                this.at[i] = this.at[i - 1] + points.get(i - 1).distanceTo(points.get(i));
            }
            this.length = this.at[n - 1];
            // Pieces of zero length borrow the direction of the next real one.
            for (int i = n - 2; i >= 0; i--) {
                Vec3 delta = points.get(i + 1).subtract(points.get(i));
                if (delta.lengthSqr() > 1.0E-10) {
                    d = delta.normalize();
                }
                this.direction[i] = d;
            }
            if (d == null) {
                this.direction[0] = new Vec3(0, 1, 0);
            }
            Vec3 last = this.direction[0];
            for (int i = 0; i < this.direction.length; i++) {
                if (this.direction[i] == null) {
                    this.direction[i] = last;
                }
                last = this.direction[i];
            }
            Vec3 s = reference == null ? null : square(reference, this.direction[0]);
            this.side[0] = s != null ? s : anySquare(this.direction[0]);
            for (int i = 1; i < this.side.length; i++) {
                Vec3 carried = square(this.side[i - 1], this.direction[i]);
                this.side[i] = carried != null ? carried : anySquare(this.direction[i]);
            }
        }

        private int piece(double s) {
            int low = 0;
            int high = this.points.size() - 1;
            while (high - low > 1) {
                int mid = (low + high) >>> 1;
                if (this.at[mid] <= s) {
                    low = mid;
                } else {
                    high = mid;
                }
            }
            return Math.min(low, this.direction.length - 1);
        }

        Vec3 pointAt(double s) {
            if (this.points.size() < 2) {
                return this.points.get(0);
            }
            s = Mth.clamp(s, 0.0, this.length);
            int i = this.piece(s);
            double span = this.at[i + 1] - this.at[i];
            return span < 1.0E-9 ? this.points.get(i)
                    : this.points.get(i).lerp(this.points.get(i + 1), (s - this.at[i]) / span);
        }

        Vec3 directionAt(double s) {
            return this.direction[this.piece(Mth.clamp(s, 0.0, this.length))];
        }

        Vec3 sideAt(double s) {
            return this.side[this.piece(Mth.clamp(s, 0.0, this.length))];
        }
    }
}
