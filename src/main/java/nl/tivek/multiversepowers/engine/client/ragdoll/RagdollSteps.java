package nl.tivek.multiversepowers.engine.client.ragdoll;

import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import net.minecraft.client.multiplayer.ClientLevel;
import nl.tivek.multiversepowers.engine.client.world.LevelBlocks;

// The limp bodies to step this tick, stepped on every core at once. Each body steps on its own against the blocks and
// against where the other bodies lay as the tick began (RagdollCrowd), so the order they are stepped in never matters.
final class RagdollSteps {
    private static final int CORES = Math.max(1, Math.min(8, Runtime.getRuntime().availableProcessors()));
    // Less work than this (substeps times bodies) is stepped on the render thread alone: handing it out costs more.
    private static final int ALONE = 400;
    private static final LevelBlocks[] BLOCKS = new LevelBlocks[CORES];

    static {
        for (int i = 0; i < CORES; i++) {
            BLOCKS[i] = new LevelBlocks();
        }
    }

    private final List<Ragdoll> dolls = new ArrayList<>();
    private final IntArrayList substeps = new IntArrayList();
    private final BooleanArrayList detailed = new BooleanArrayList();

    void add(Ragdoll doll, int substeps, boolean detailed) {
        this.dolls.add(doll);
        this.substeps.add(substeps);
        this.detailed.add(detailed);
    }

    int size() {
        return this.dolls.size();
    }

    Ragdoll doll(int i) {
        return this.dolls.get(i);
    }

    boolean detailed(int i) {
        return this.detailed.getBoolean(i);
    }

    void clear() {
        this.dolls.clear();
        this.substeps.clear();
        this.detailed.clear();
    }

    void run(ClientLevel level) {
        int n = this.dolls.size();
        if (n == 0) {
            return;
        }
        long[] work = new long[n];
        long total = 0;
        for (int i = 0; i < n; i++) {
            work[i] = this.dolls.get(i).work(this.substeps.getInt(i));
            total += work[i];
        }
        int runs = total < ALONE ? 1 : (int) Math.min(CORES, n);
        int[] from = new int[runs + 1];
        long done = 0;
        int run = 1;
        for (int i = 0; i < n && run < runs; i++) {
            done += work[i];
            if (done * runs >= total * run) {
                from[run++] = i + 1;
            }
        }
        while (run <= runs) {
            from[run++] = n;
        }
        CompletableFuture<?>[] others = new CompletableFuture<?>[runs - 1];
        for (int r = 1; r < runs; r++) {
            int k = r;
            others[r - 1] = CompletableFuture.runAsync(() -> this.step(BLOCKS[k].in(level), from[k], from[k + 1]),
                    ForkJoinPool.commonPool());
        }
        this.step(BLOCKS[0].in(level), from[0], from[1]);
        CompletableFuture.allOf(others).join();
    }

    private void step(LevelBlocks blocks, int from, int to) {
        for (int i = from; i < to; i++) {
            this.dolls.get(i).step(this.substeps.getInt(i), blocks);
        }
    }
}
