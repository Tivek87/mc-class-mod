import java.util.ArrayList;
import java.util.List;

// In model pixels, one to a picture pixel: the pommel at y 0 (its tip a pixel below), the peak at y 32, centred on
// x 8 and z 8, the rune faces looking along +z and -z.
final class Hammer {
    // x of Sheet.FACE's first column.
    private static final double LEFT = -3.5;
    private static final double FRONT = 11;

    private Hammer() {
    }

    static List<Cube> body() {
        List<Cube> cubes = head(Sheet.FRONT);
        cubes.get(0).show(Cube.Side.EAST, Sheet.END).show(Cube.Side.WEST, Sheet.END).show(Cube.Side.UP, Sheet.TOP)
                .show(Cube.Side.DOWN, Sheet.BOTTOM);
        for (int i = 1; i < 4; i++) {
            Cube step = cubes.get(i);
            step.show(Cube.Side.EAST, Sheet.RISER).show(Cube.Side.WEST, Sheet.RISER)
                    .show(Cube.Side.UP, Sheet.TOP.sub(column(step), 0, step.width(Cube.Side.UP), 6));
        }
        List<Cube> face = new ArrayList<>();
        face.add(gilt(new Cube("medallion", 6.5, 24, FRONT, 9.5, 27, FRONT + 1).show(Cube.Side.SOUTH, Sheet.MEDAL)));
        List<Cube> half = new ArrayList<>();
        half.add(gilt(at("stud", 7, 28, 1, 1).show(Cube.Side.SOUTH, Sheet.STUD)));
        half.add(gilt(at("horn", 4, 23, 5, 1).show(Cube.Side.SOUTH, Sheet.HORN.sub(0, 0, 5, 1))));
        half.add(gilt(at("horn", 2, 22, 2, 1).show(Cube.Side.SOUTH, Sheet.HORN.sub(0, 0, 2, 1))));
        half.add(gilt(at("horn_curl", 0, 21, 2, 2).show(Cube.Side.SOUTH, Sheet.CURL)));
        half.add(gilt(at("band", 9, 22, 1, 3).show(Cube.Side.SOUTH, Sheet.SCROLL)));
        for (Cube cube : half) {
            face.add(cube);
            face.add(cube.mirrorX(cube.name));
        }
        for (Cube cube : face) {
            cubes.add(cube);
            cubes.add(cube.mirrorZ(cube.name));
        }
        cubes.add(new Cube("collar", 5.5, 19, 5.5, 10.5, 22, 10.5).show(Cube.Side.SOUTH, Sheet.COLLAR)
                .show(Cube.Side.NORTH, Sheet.COLLAR).show(Cube.Side.EAST, Sheet.COLLAR_SIDE)
                .show(Cube.Side.WEST, Sheet.COLLAR_SIDE).show(Cube.Side.DOWN, Sheet.COLLAR_DOWN));
        cubes.add(new Cube("handle", 6.5, 4, 6.5, 9.5, 19, 9.5).show(Cube.Side.SOUTH, Sheet.HANDLE)
                .show(Cube.Side.NORTH, Sheet.HANDLE).show(Cube.Side.EAST, Sheet.HANDLE_SIDE)
                .show(Cube.Side.WEST, Sheet.HANDLE_SIDE).show(Cube.Side.DOWN, Sheet.HANDLE_DOWN));
        Cube hook = gilt(new Cube("hook", 5.5, 8, 8.5, 6.5, 10, 9.5), Cube.Side.EAST);
        cubes.add(hook);
        cubes.add(hook.mirrorX("hook"));
        cubes.add(hook.mirrorZ("hook"));
        cubes.add(hook.mirrorX("hook").mirrorZ("hook"));
        Cube snake = new Cube("serpent", 6, 2, 7, 8, 4, 9).turn("z", -22.5, 8, 4, 8)
                .show(Cube.Side.SOUTH, Sheet.SNAKE).show(Cube.Side.NORTH, Sheet.SNAKE).flip(Cube.Side.NORTH)
                .show(Cube.Side.EAST, Sheet.SNAKE_SIDE).show(Cube.Side.WEST, Sheet.SNAKE_SIDE)
                .show(Cube.Side.DOWN, Sheet.SNAKE_DOWN);
        cubes.add(snake);
        cubes.add(snake.mirrorX("serpent"));
        // A flat sprite, as the game draws a flower: seen from the front and back only.
        cubes.add(new Cube("tip", 5.5, -1, 8, 10.5, 2, 8).show(Cube.Side.SOUTH, Sheet.TIP)
                .show(Cube.Side.NORTH, Sheet.TIP));
        return cubes;
    }

    // The same rune faces again, for the glow drawn over the head's own.
    static List<Cube> runes() {
        return head(Sheet.GLOW);
    }

    private static List<Cube> head(Sheet.Rect art) {
        List<Cube> cubes = new ArrayList<>();
        cubes.add(face(new Cube("head", LEFT, 22, 5, LEFT + 23, 29, FRONT), art.sub(0, 3, 23, 7)));
        int[][] steps = {{4, 15}, {7, 9}, {10, 3}};
        for (int i = 0; i < steps.length; i++) {
            int column = steps[i][0], wide = steps[i][1];
            cubes.add(face(new Cube("roof", LEFT + column, 29 + i, 5, LEFT + column + wide, 30 + i, FRONT),
                    art.sub(column, 2 - i, wide, 1)));
        }
        return cubes;
    }

    private static Cube face(Cube cube, Sheet.Rect rect) {
        return cube.show(Cube.Side.SOUTH, rect).show(Cube.Side.NORTH, rect);
    }

    private static int column(Cube cube) {
        return (int) Math.round(cube.from[0] - LEFT);
    }

    private static Cube at(String name, int column, double y, int wide, int high) {
        return new Cube(name, LEFT + column, y, FRONT, LEFT + column + wide, y + high, FRONT + 1);
    }

    private static Cube gilt(Cube cube, Cube.Side... hidden) {
        for (Cube.Side side : Cube.Side.values()) {
            boolean skip = cube.shown(side) != null || side == Cube.Side.NORTH && cube.from[2] == FRONT;
            for (Cube.Side h : hidden) {
                skip |= h == side;
            }
            if (skip) {
                continue;
            }
            int w = cube.width(side), h = cube.height(side);
            int dx = side == Cube.Side.UP ? 0 : side == Cube.Side.DOWN ? Sheet.GOLD.w() - w : (Sheet.GOLD.w() - w) / 2;
            int dy = side == Cube.Side.UP ? 0 : side == Cube.Side.DOWN ? Sheet.GOLD.h() - h : (Sheet.GOLD.h() - h) / 2;
            cube.show(side, Sheet.GOLD.sub(dx, dy, w, h));
        }
        return cube;
    }
}
