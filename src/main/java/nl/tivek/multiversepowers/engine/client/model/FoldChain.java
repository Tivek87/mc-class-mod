package nl.tivek.multiversepowers.engine.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import javax.annotation.Nullable;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// How one part is drawn bent at its joints, in its own pixels: cut across at each joint (a knee, then an ankle), each
// piece past a cut turned about that joint's middle by the joint's turn and carried on by every joint before it. Each
// piece reaches on past its cuts and is trimmed on the plane halfway between it and its neighbour, so up to a square
// bend they meet in a mitred corner and past it each ends flat, as two blocks would; a sharp fold neither thins nor
// stretches. A joint's twist about the bone is shared out along the pieces on either side of it, none of it at the
// part's ends, so the joint itself only folds. Only the render thread draws, so the scratch is shared.
final class FoldChain {
    // A joint: its cut (a point lies past it where cut . (x, y, z) + cut[3] is above 0) and middle, its whole turn;
    // seen with the cut turned by half its twist it only folds: the planes halfway between the pieces as the near one
    // lies and as the far one lay before it turned, the way into the fold, how far past the cut each piece reaches for
    // every pixel a cube stands out on the outside of the fold (never further than square), and whether the fold is
    // sharper than square, so the pieces end flat.
    private record Joint(float[] cut, float[] knee, Matrix3f turn, float[] near, float[] far, float[] in, float reach,
            boolean sharp) {
    }

    // A piece: how far along the bone it runs, counted from the first cut; its twist at its start and at its end;
    // where it is moved from there, and so its faces; the planes it is trimmed on at its start and its end (in its
    // twisted pixels, keeping what lies past the start and before the end).
    private record Piece(float from, float to, float twistFrom, float twistTo, Matrix4f move, Matrix3f faces,
            @Nullable float[] start, @Nullable float[] end) {
    }

    private static final float CUT = 1.0E-3F;
    private static final int MOST = 12;
    // A face as it is cut and trimmed, one plane after another.
    private static final float[][] ROUND_AT = new float[6][3 * MOST];
    private static final float[][] ROUND_UV = new float[6][2 * MOST];
    private static final float[] PLANE = new float[4];
    private static final float[] REACH = new float[4];
    private static final float[] LOW = new float[4];
    private static final float[] HIGH = new float[4];
    private static final Vector3f POINT = new Vector3f();
    private static final Vector3f NORMAL = new Vector3f();
    private static final Vector3f FACING = new Vector3f();
    private static final Quaternionf SPIN = new Quaternionf();

    private final float[] bone;
    private final Joint[] joints;
    private final Piece[] pieces;
    // Every joint's turn carried along the chain: the frame of each piece past a joint as a whole.
    private final Matrix4f[] frames;

    private FoldChain(float[] bone, Joint[] joints, Piece[] pieces, Matrix4f[] frames) {
        this.bone = bone;
        this.joints = joints;
        this.pieces = pieces;
        this.frames = frames;
    }

    // `part` bent at `bends` (along one bone, near to far) by `turns`, each the turn of the piece past it in the piece
    // before it's own axes.
    static FoldChain of(ModelPart part, ModelBends.Bend[] bends, Quaternionf[] turns) {
        int m = bends.length;
        ModelBends.Bend first = bends[0];
        int axis = first.axis();
        float sign = first.farSign();
        Vector3f bone = new Vector3f();
        bone.setComponent(axis, sign);
        // How far the part reaches along its bone either way from the first cut, a pixel at least past each end cut.
        float low = -1.0F;
        float high = sign * (bends[m - 1].at() - first.at()) + 1.0F;
        for (ModelPart.Cube cube : part.cubes) {
            float a = sign * ((axis == 0 ? cube.minX : axis == 1 ? cube.minY : cube.minZ) - first.at());
            float b = sign * ((axis == 0 ? cube.maxX : axis == 1 ? cube.maxY : cube.maxZ) - first.at());
            low = Math.min(low, Math.min(a, b));
            high = Math.max(high, Math.max(a, b));
        }
        Joint[] joints = new Joint[m];
        float[] twists = new float[m];
        Quaternionf[] halves = new Quaternionf[m];
        Quaternionf[] swings = new Quaternionf[m];
        for (int j = 0; j < m; j++) {
            ModelBends.Bend bend = bends[j];
            Quaternionf q = new Quaternionf(turns[j]);
            if (q.w < 0.0F) {
                q.set(-q.x, -q.y, -q.z, -q.w);
            }
            float twist = 2.0F * (float) Math.atan2(q.x * bone.x + q.y * bone.y + q.z * bone.z, q.w);
            Quaternionf half = new Quaternionf().fromAxisAngleRad(bone, twist * 0.5F);
            Quaternionf swing = new Quaternionf(q).mul(new Quaternionf().fromAxisAngleRad(bone, -twist));
            Quaternionf seen = new Quaternionf(half).conjugate().mul(swing).mul(half);
            if (seen.w < 0.0F) {
                seen.set(-seen.x, -seen.y, -seen.z, -seen.w);
            }
            Vector3f hinge = new Vector3f(seen.x, seen.y, seen.z);
            float angle = 2.0F * (float) Math.acos(Math.min(1.0F, seen.w));
            if (hinge.lengthSquared() < 1.0E-12F) {
                hinge.set(bend.hinge()[0], bend.hinge()[1], bend.hinge()[2]);
                angle = 0.0F;
            }
            hinge.normalize();
            // Towards the inside of the fold, where the far piece goes.
            Vector3f in = new Vector3f(hinge).cross(bone).normalize();
            float c = (float) Math.cos(angle * 0.5F);
            float s = (float) Math.sin(angle * 0.5F);
            float[] k = bend.knee();
            joints[j] = new Joint(cutPlane(bend), k, new Matrix3f().set(turns[j]),
                    through(k, new Vector3f(bone).mul(c).add(new Vector3f(in).mul(s))),
                    through(k, new Vector3f(bone).mul(c).sub(new Vector3f(in).mul(s))),
                    new float[] { in.x, in.y, in.z }, Math.min(1.0F, s / c), s > c);
            twists[j] = twist;
            halves[j] = half;
            swings[j] = swing;
        }
        float[] b = { bone.x, bone.y, bone.z };
        Piece[] pieces = new Piece[m + 1];
        Matrix4f[] frames = new Matrix4f[m + 1];
        frames[0] = new Matrix4f();
        for (int j = 0; j <= m; j++) {
            float from = j == 0 ? low : sign * (bends[j - 1].at() - first.at());
            float to = j == m ? high : sign * (bends[j].at() - first.at());
            float twistFrom = j < m ? -(0.5F * twists[j]) : 0.0F;
            float twistTo = j > 0 ? 0.5F * twists[j - 1] : 0.0F;
            // The first piece turns by half the first joint's twist; each past a joint as that joint turns it less
            // the other half of its twist, and by half the next joint's twist, carried on by every joint before.
            Quaternionf own = j == 0 ? new Quaternionf(halves[0]) : new Quaternionf(swings[j - 1]).mul(halves[j - 1]);
            if (j > 0 && j < m) {
                own.mul(halves[j]);
            }
            Matrix3f faces = new Matrix3f().set(own);
            Matrix4f move = about(bends[Math.max(0, j - 1)].knee(), faces);
            if (j > 1) {
                move = new Matrix4f(frames[j - 1]).mul(move);
                faces = frames[j - 1].get3x3(new Matrix3f()).mul(faces);
            }
            float[] start = j == 0 ? null : spun(joints[j - 1].far(), joints[j - 1].knee(), b, twistFrom);
            float[] end = j == m ? null : spun(joints[j].near(), joints[j].knee(), b, twistTo);
            pieces[j] = new Piece(from, to, twistFrom, twistTo, move, faces, start, end);
            if (j < m) {
                frames[j + 1] = new Matrix4f(frames[j]).mul(about(bends[j].knee(), joints[j].turn()));
            }
        }
        return new FoldChain(b, joints, pieces, frames);
    }

    // A turn about the knee, in pixels.
    private static Matrix4f about(float[] k, Matrix3f turn) {
        return new Matrix4f().translation(k[0], k[1], k[2]).mul(new Matrix4f().set(turn)).translate(-k[0], -k[1],
                -k[2]);
    }

    // The cut across a bent part, in its own pixels.
    private static float[] cutPlane(ModelBends.Bend bend) {
        float[] plane = new float[4];
        plane[bend.axis()] = bend.farSign();
        plane[3] = -bend.at() * bend.farSign();
        return plane;
    }

    // The plane through the knee square to `normal`.
    private static float[] through(float[] k, Vector3f normal) {
        return new float[] { normal.x, normal.y, normal.z, -(normal.x * k[0] + normal.y * k[1] + normal.z * k[2]) };
    }

    // A plane through the knee turned about the bone by `angle`, as the piece it trims is twisted there.
    private static float[] spun(float[] plane, float[] k, float[] bone, float angle) {
        if (angle == 0.0F) {
            return plane;
        }
        Vector3f normal = new Quaternionf().fromAxisAngleRad(bone[0], bone[1], bone[2], angle)
                .transform(new Vector3f(plane[0], plane[1], plane[2]));
        return through(k, normal);
    }

    int joints() {
        return this.joints.length;
    }

    Matrix3f turn(int joint) {
        return this.joints[joint].turn();
    }

    // The frame of the piece past `joint` joints as a whole: every turn before it, about its knee (pixels).
    Matrix4f frame(int piece) {
        return this.frames[piece];
    }

    // The far end's frame: every joint's turn about its knee, one after another, in blocks.
    void farEnd(PoseStack pose) {
        Quaternionf turn = new Quaternionf();
        for (Joint joint : this.joints) {
            float[] k = joint.knee();
            pose.translate(k[0] / 16.0F, k[1] / 16.0F, k[2] / 16.0F);
            pose.mulPose(joint.turn().getNormalizedRotation(turn));
            pose.translate(-k[0] / 16.0F, -k[1] / 16.0F, -k[2] / 16.0F);
        }
    }

    private static float side(float[] plane, float x, float y, float z) {
        return plane[0] * x + plane[1] * y + plane[2] * z + plane[3];
    }

    // Which piece a point lies in: past how many cuts.
    private int pieceOf(float x, float y, float z) {
        int piece = 0;
        for (Joint joint : this.joints) {
            piece += side(joint.cut(), x, y, z) > 0.0F ? 1 : 0;
        }
        return piece;
    }

    // How far a point `along` the bone from the first cut is twisted in its piece: from the piece's start to its end.
    private float twisted(int piece, float along) {
        Piece p = this.pieces[piece];
        float from = Math.max(0.0F, Math.min(1.0F, (p.to() - along) / (p.to() - p.from())));
        float to = Math.max(0.0F, Math.min(1.0F, (along - p.from()) / (p.to() - p.from())));
        return p.twistFrom() * from + p.twistTo() * to;
    }

    // Turns a point about the bone through the knees, or a direction when there is no knee.
    private void spin(@Nullable float[] k, Vector3f point, float angle) {
        if (angle == 0.0F) {
            return;
        }
        SPIN.fromAxisAngleRad(this.bone[0], this.bone[1], this.bone[2], angle);
        if (k == null) {
            SPIN.transform(point);
            return;
        }
        SPIN.transform(point.sub(k[0], k[1], k[2])).add(k[0], k[1], k[2]);
    }

    // Where a point (the bent part's pixels) is drawn: moved with the piece it lies in, twisted as the part's faces are
    // there.
    Vector3f place(Vector3f point) {
        int piece = this.pieceOf(point.x, point.y, point.z);
        this.spin(this.joints[0].knee(), point, this.twisted(piece, side(this.joints[0].cut(), point.x, point.y,
                point.z)));
        return this.pieces[piece].move().transformPosition(point);
    }

    // A cube's faces (see BentParts.faces), each face cut into its pieces, each piece reaching on past its cuts as far
    // as the fold needs, twisted as far as it lies along the bone and trimmed on the planes halfway between it and its
    // neighbours. Folded sharper than square, the pieces on either side of a joint end flat at it, closed with the
    // cube's own end: the far end's face for the near piece, the near end's for the far one. `into` takes the pixels
    // of a part drawn inside the bent one to the bent part's, `back` the other way (null for the bent part itself).
    void draw(VertexConsumer buffer, PoseStack.Pose pose, float[][] faces, @Nullable Matrix4f into,
            @Nullable Matrix4f back, @Nullable Matrix3f intoFaces, @Nullable Matrix3f backFaces, int light, int overlay,
            int color) {
        int m = this.joints.length;
        boolean[] across = new boolean[m];
        for (int j = 0; j < m; j++) {
            Joint joint = this.joints[j];
            float[] k = joint.knee();
            float[] in = joint.in();
            float low = Float.POSITIVE_INFINITY;
            float high = Float.NEGATIVE_INFINITY;
            float out = 0.0F;
            for (float[] face : faces) {
                for (int i = 0; i < corners(face); i++) {
                    corner(face, i, into, POINT);
                    float along = side(joint.cut(), POINT.x, POINT.y, POINT.z);
                    low = Math.min(low, along);
                    high = Math.max(high, along);
                    out = Math.max(out, -(in[0] * (POINT.x - k[0]) + in[1] * (POINT.y - k[1])
                            + in[2] * (POINT.z - k[2])));
                }
            }
            // Only a cube across the joint reaches on past it.
            across[j] = low < -CUT && high > CUT;
            REACH[j] = across[j] ? out * joint.reach() : 0.0F;
            LOW[j] = low;
            HIGH[j] = high;
        }
        for (float[] face : faces) {
            int n = corners(face);
            FACING.set(face[0], face[1], face[2]);
            if (intoFaces != null) {
                intoFaces.transform(FACING).normalize();
            }
            for (int p = 0; p <= m; p++) {
                load(face, n, into, -1, Float.NaN);
                int round = 0;
                int count = n;
                if (p > 0) {
                    // Past the joint before it, less its reach back over the cut.
                    System.arraycopy(this.joints[p - 1].cut(), 0, PLANE, 0, 4);
                    PLANE[3] += REACH[p - 1];
                    count = clip(round, count, PLANE, -1.0F, round + 1);
                    round++;
                }
                if (p < m) {
                    // Short of the joint after it, and its reach on past the cut.
                    System.arraycopy(this.joints[p].cut(), 0, PLANE, 0, 4);
                    PLANE[3] -= REACH[p];
                    count = clip(round, count, PLANE, 1.0F, round + 1);
                    round++;
                }
                this.piece(buffer, pose, back, backFaces, p, round, count, light, overlay, color);
            }
            for (int j = 0; j < m; j++) {
                if (!across[j] || !this.joints[j].sharp()) {
                    continue;
                }
                boolean atLow = true;
                boolean atHigh = true;
                load(face, n, into, -1, Float.NaN);
                for (int i = 0; i < n; i++) {
                    float along = side(this.joints[j].cut(), ROUND_AT[0][3 * i], ROUND_AT[0][3 * i + 1],
                            ROUND_AT[0][3 * i + 2]);
                    atLow &= Math.abs(along - LOW[j]) <= CUT;
                    atHigh &= Math.abs(along - HIGH[j]) <= CUT;
                }
                if (atHigh) {
                    load(face, n, into, j, REACH[j]);
                    this.piece(buffer, pose, back, backFaces, j, 0, n, light, overlay, color);
                } else if (atLow) {
                    load(face, n, into, j, -REACH[j]);
                    this.piece(buffer, pose, back, backFaces, j + 1, 0, n, light, overlay, color);
                }
            }
        }
    }

    // A face: its normal, then each corner's x, y, z (pixels) and u, v.
    private static int corners(float[] face) {
        return (face.length - 3) / 5;
    }

    // Corner i of a face, in the bent part's pixels.
    private static void corner(float[] face, int i, @Nullable Matrix4f into, Vector3f out) {
        out.set(face[3 + 5 * i], face[4 + 5 * i], face[5 + 5 * i]);
        if (into != null) {
            into.transformPosition(out);
        }
    }

    // A face's corners into the first round of the scratch, in the bent part's pixels, slid along the bone onto `at`
    // past joint `joint`'s cut unless that is -1.
    private void load(float[] face, int n, @Nullable Matrix4f into, int joint, float at) {
        for (int i = 0; i < n; i++) {
            corner(face, i, into, POINT);
            if (joint >= 0) {
                float[] cut = this.joints[joint].cut();
                float slide = side(cut, POINT.x, POINT.y, POINT.z) - at;
                POINT.sub(cut[0] * slide, cut[1] * slide, cut[2] * slide);
            }
            ROUND_AT[0][3 * i] = POINT.x;
            ROUND_AT[0][3 * i + 1] = POINT.y;
            ROUND_AT[0][3 * i + 2] = POINT.z;
            System.arraycopy(face, 6 + 5 * i, ROUND_UV[0], 2 * i, 2);
        }
    }

    // One piece of a face (`count` corners in round `from`): each corner twisted as far as it lies along the bone,
    // the piece trimmed on the planes between it and its neighbours and drawn where it is moved, as squares with a
    // corner doubled where needed.
    private void piece(VertexConsumer buffer, PoseStack.Pose pose, @Nullable Matrix4f back,
            @Nullable Matrix3f backFaces, int p, int from, int count, int light, int overlay, int color) {
        if (count < 3) {
            return;
        }
        Piece piece = this.pieces[p];
        float[] first = this.joints[0].cut();
        float[] k = this.joints[0].knee();
        float[] at = ROUND_AT[from];
        for (int i = 0; i < count; i++) {
            POINT.set(at[3 * i], at[3 * i + 1], at[3 * i + 2]);
            this.spin(k, POINT, this.twisted(p, side(first, POINT.x, POINT.y, POINT.z)));
            at[3 * i] = POINT.x;
            at[3 * i + 1] = POINT.y;
            at[3 * i + 2] = POINT.z;
        }
        int round = from;
        int trimmed = count;
        if (piece.start() != null) {
            trimmed = clip(round, trimmed, piece.start(), -1.0F, round + 1);
            round++;
        }
        if (piece.end() != null) {
            trimmed = clip(round, trimmed, piece.end(), 1.0F, round + 1);
            round++;
        }
        for (int q = 1; q + 1 < trimmed; q += 2) {
            this.point(buffer, pose, back, backFaces, piece, p, round, 0, light, overlay, color);
            this.point(buffer, pose, back, backFaces, piece, p, round, q, light, overlay, color);
            this.point(buffer, pose, back, backFaces, piece, p, round, q + 1, light, overlay, color);
            this.point(buffer, pose, back, backFaces, piece, p, round, Math.min(q + 2, trimmed - 1), light, overlay,
                    color);
        }
    }

    // Keeps what lies where keep * (plane . p + plane[3]) is at most 0, from one round of the scratch into another.
    private static int clip(int from, int count, float[] plane, float keep, int to) {
        float[] at = ROUND_AT[from];
        float[] uv = ROUND_UV[from];
        float[] outAt = ROUND_AT[to];
        float[] outUv = ROUND_UV[to];
        int out = 0;
        for (int i = 0; i < count && out < MOST - 1; i++) {
            int j = (i + 1) % count;
            float di = keep * side(plane, at[3 * i], at[3 * i + 1], at[3 * i + 2]);
            float dj = keep * side(plane, at[3 * j], at[3 * j + 1], at[3 * j + 2]);
            if (di <= CUT) {
                System.arraycopy(at, 3 * i, outAt, 3 * out, 3);
                System.arraycopy(uv, 2 * i, outUv, 2 * out, 2);
                out++;
            }
            if (di < -CUT && dj > CUT || di > CUT && dj < -CUT) {
                float u = di / (di - dj);
                for (int a = 0; a < 3; a++) {
                    outAt[3 * out + a] = at[3 * i + a] + (at[3 * j + a] - at[3 * i + a]) * u;
                }
                for (int a = 0; a < 2; a++) {
                    outUv[2 * out + a] = uv[2 * i + a] + (uv[2 * j + a] - uv[2 * i + a]) * u;
                }
                out++;
            }
        }
        return out;
    }

    // One corner of a trimmed piece where the piece is moved, with its face's normal twisted as it is.
    private void point(VertexConsumer buffer, PoseStack.Pose pose, @Nullable Matrix4f back,
            @Nullable Matrix3f backFaces, Piece piece, int p, int round, int i, int light, int overlay, int color) {
        POINT.set(ROUND_AT[round][3 * i], ROUND_AT[round][3 * i + 1], ROUND_AT[round][3 * i + 2]);
        NORMAL.set(FACING);
        this.spin(null, NORMAL, this.twisted(p, side(this.joints[0].cut(), POINT.x, POINT.y, POINT.z)));
        piece.move().transformPosition(POINT);
        piece.faces().transform(NORMAL);
        if (back != null) {
            back.transformPosition(POINT);
            backFaces.transform(NORMAL);
        }
        pose.transformNormal(NORMAL.normalize(), NORMAL);
        pose.pose().transformPosition(POINT.x / 16.0F, POINT.y / 16.0F, POINT.z / 16.0F, POINT);
        buffer.addVertex(POINT.x, POINT.y, POINT.z, color, ROUND_UV[round][2 * i], ROUND_UV[round][2 * i + 1], overlay,
                light, NORMAL.x, NORMAL.y, NORMAL.z);
    }
}
