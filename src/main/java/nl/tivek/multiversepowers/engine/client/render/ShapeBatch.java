package nl.tivek.multiversepowers.engine.client.render;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

// The shapes a painter held back (see ConstructPainter.batch), worked out together: split into runs of about equal
// work, each drawn by a painter of its own, all at once on other threads, and joined back in the order they were drawn.
final class ShapeBatch {
    enum Kind {
        WHOLE,
        SHATTERED,
        SEE_THROUGH
    }

    private record Held(Kind kind, ConstructPainter.Shape shape, ConstructPainter.Frame frame, double a, double b,
            int seed, double[] state, Material material, int work) {
    }

    private static final int STATE = 13;
    // Fewer sides than this are drawn on the render thread alone: handing them out would cost more than it saves.
    private static final int ALONE = 6000;
    private static final int RUNS = Math.max(1, Math.min(8, Runtime.getRuntime().availableProcessors()));
    private final List<Held> held = new ArrayList<>();
    boolean open;

    void open() {
        this.open = true;
    }

    void close() {
        this.open = false;
    }

    void hold(ConstructPainter painter, Kind kind, ConstructPainter.Shape shape, ConstructPainter.Frame frame,
            double a, double b, int seed) {
        double[] state = new double[STATE];
        painter.saveState(state);
        int work = 0;
        for (Mesh mesh : shape.meshes()) {
            work += mesh.sides.length;
        }
        this.held.add(new Held(kind, shape, frame, a, b, seed, state, painter.material, work));
    }

    void run(ConstructPainter painter) {
        if (this.held.isEmpty()) {
            return;
        }
        long total = 0;
        for (Held shape : this.held) {
            total += shape.work();
        }
        int runs = total < ALONE ? 1 : Math.min(RUNS, this.held.size());
        int[] from = new int[runs + 1];
        long done = 0;
        int run = 1;
        for (int i = 0; i < this.held.size() && run < runs; i++) {
            done += this.held.get(i).work();
            if (done * runs >= total * run) {
                from[run++] = i + 1;
            }
        }
        while (run <= runs) {
            from[run++] = this.held.size();
        }
        ConstructPainter[] workers = new ConstructPainter[runs];
        for (int r = 0; r < runs; r++) {
            workers[r] = painter.worker();
        }
        CompletableFuture<?>[] others = new CompletableFuture<?>[runs - 1];
        for (int r = 1; r < runs; r++) {
            int k = r;
            others[r - 1] = CompletableFuture.runAsync(() -> this.draw(workers[k], from[k], from[k + 1]),
                    ForkJoinPool.commonPool());
        }
        this.draw(workers[0], from[0], from[1]);
        CompletableFuture.allOf(others).join();
        for (ConstructPainter worker : workers) {
            painter.append(worker);
        }
        this.held.clear();
    }

    private void draw(ConstructPainter worker, int from, int to) {
        for (int i = from; i < to; i++) {
            Held shape = this.held.get(i);
            worker.loadState(shape.state());
            worker.material = shape.material();
            switch (shape.kind()) {
                case WHOLE -> worker.shape(shape.shape(), shape.frame(), shape.a(), shape.b());
                case SHATTERED -> worker.shattered(shape.shape(), shape.frame(), shape.a(), shape.b(), shape.seed());
                case SEE_THROUGH -> worker.seeThrough(shape.shape(), shape.frame(), shape.a(), shape.b());
            }
        }
    }
}
