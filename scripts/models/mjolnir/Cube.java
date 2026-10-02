import java.util.Locale;

// One element of a vanilla block model: the game turns it about one axis only, by 0, 22.5 or 45 degrees either way.
final class Cube {
    enum Side {
        NORTH, EAST, SOUTH, WEST, UP, DOWN
    }

    final String name;
    final double[] from;
    final double[] to;
    private final Sheet.Rect[] shown = new Sheet.Rect[6];
    private final boolean[] flipU = new boolean[6];
    private final boolean[] flipV = new boolean[6];
    private String axis;
    private double angle;
    private double[] origin;

    Cube(String name, double x1, double y1, double z1, double x2, double y2, double z2) {
        this.name = name;
        this.from = new double[] {x1, y1, z1};
        this.to = new double[] {x2, y2, z2};
    }

    int width(Side side) {
        return (int) Math.round(switch (side) {
            case NORTH, SOUTH, UP, DOWN -> this.to[0] - this.from[0];
            case EAST, WEST -> this.to[2] - this.from[2];
        });
    }

    int height(Side side) {
        return (int) Math.round(switch (side) {
            case NORTH, SOUTH, EAST, WEST -> this.to[1] - this.from[1];
            case UP, DOWN -> this.to[2] - this.from[2];
        });
    }

    Cube show(Side side, Sheet.Rect rect) {
        if (rect.w() != this.width(side) || rect.h() != this.height(side)) {
            throw new IllegalArgumentException(this.name + " " + side + " is " + this.width(side) + "x"
                    + this.height(side) + ", its picture " + rect.w() + "x" + rect.h());
        }
        this.shown[side.ordinal()] = rect;
        return this;
    }

    Cube flip(Side side) {
        this.flipU[side.ordinal()] = !this.flipU[side.ordinal()];
        return this;
    }

    Cube turn(String axis, double angle, double ox, double oy, double oz) {
        this.axis = axis;
        this.angle = angle;
        this.origin = new double[] {ox, oy, oz};
        return this;
    }

    Cube mirrorX(String name) {
        Cube copy = new Cube(name, 16 - this.to[0], this.from[1], this.from[2], 16 - this.from[0], this.to[1],
                this.to[2]);
        for (Side side : Side.values()) {
            Side there = side == Side.EAST ? Side.WEST : side == Side.WEST ? Side.EAST : side;
            copy.shown[there.ordinal()] = this.shown[side.ordinal()];
            copy.flipU[there.ordinal()] = !this.flipU[side.ordinal()];
            copy.flipV[there.ordinal()] = this.flipV[side.ordinal()];
        }
        if (this.axis != null) {
            copy.turn(this.axis, this.axis.equals("x") ? this.angle : -this.angle, 16 - this.origin[0],
                    this.origin[1], this.origin[2]);
        }
        return copy;
    }

    Cube mirrorZ(String name) {
        Cube copy = new Cube(name, this.from[0], this.from[1], 16 - this.to[2], this.to[0], this.to[1],
                16 - this.from[2]);
        for (Side side : Side.values()) {
            Side there = side == Side.NORTH ? Side.SOUTH : side == Side.SOUTH ? Side.NORTH : side;
            boolean upright = side != Side.UP && side != Side.DOWN;
            copy.shown[there.ordinal()] = this.shown[side.ordinal()];
            copy.flipU[there.ordinal()] = upright != this.flipU[side.ordinal()];
            copy.flipV[there.ordinal()] = !upright != this.flipV[side.ordinal()];
        }
        if (this.axis != null) {
            copy.turn(this.axis, this.axis.equals("z") ? this.angle : -this.angle, this.origin[0], this.origin[1],
                    16 - this.origin[2]);
        }
        return copy;
    }

    Sheet.Rect shown(Side side) {
        return this.shown[side.ordinal()];
    }

    String json(String texture) {
        StringBuilder out = new StringBuilder();
        out.append("\t\t{\n\t\t\t\"name\": \"").append(this.name).append("\",\n");
        out.append("\t\t\t\"from\": ").append(list(this.from)).append(",\n");
        out.append("\t\t\t\"to\": ").append(list(this.to)).append(",\n");
        if (this.axis != null) {
            out.append("\t\t\t\"rotation\": {\"angle\": ").append(number(this.angle)).append(", \"axis\": \"")
                    .append(this.axis).append("\", \"origin\": ").append(list(this.origin)).append("},\n");
        }
        out.append("\t\t\t\"faces\": {\n");
        boolean first = true;
        for (Side side : Side.values()) {
            Sheet.Rect rect = this.shown[side.ordinal()];
            if (rect == null) {
                continue;
            }
            double u1 = rect.x(), v1 = rect.y(), u2 = rect.x() + rect.w(), v2 = rect.y() + rect.h();
            if (this.flipU[side.ordinal()]) {
                double t = u1;
                u1 = u2;
                u2 = t;
            }
            if (this.flipV[side.ordinal()]) {
                double t = v1;
                v1 = v2;
                v2 = t;
            }
            double unit = 16.0 / Sheet.SIZE;
            out.append(first ? "" : ",\n").append("\t\t\t\t\"").append(side.name().toLowerCase(Locale.ROOT))
                    .append("\": {\"uv\": ").append(list(u1 * unit, v1 * unit, u2 * unit, v2 * unit))
                    .append(", \"texture\": \"#").append(texture).append("\"}");
            first = false;
        }
        out.append("\n\t\t\t}\n\t\t}");
        return out.toString();
    }

    private static String list(double... values) {
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < values.length; i++) {
            out.append(i == 0 ? "" : ", ").append(number(values[i]));
        }
        return out.append("]").toString();
    }

    private static String number(double value) {
        return value == Math.rint(value) ? Long.toString((long) value)
                : String.format(Locale.ROOT, "%s", value).replaceAll("0+$", "");
    }
}
