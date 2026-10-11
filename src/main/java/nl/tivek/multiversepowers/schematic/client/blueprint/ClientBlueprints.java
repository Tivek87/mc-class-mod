package nl.tivek.multiversepowers.schematic.client.blueprint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.config.client.ClientSettings;
import nl.tivek.multiversepowers.schematic.Schematic;
import nl.tivek.multiversepowers.schematic.SchematicRules;
import nl.tivek.multiversepowers.schematic.blueprint.Blueprint;
import nl.tivek.multiversepowers.schematic.client.hologram.HologramModel;
import nl.tivek.multiversepowers.schematic.client.hologram.HologramRenderer;
import nl.tivek.multiversepowers.schematic.format.SchematicReader;
import nl.tivek.multiversepowers.schematic.net.BlueprintAskPayload;
import nl.tivek.multiversepowers.schematic.net.BlueprintListPayload;

// Every blueprint of this world as this game has it: the list the server sends, and for each one near (or looked at
// in the menu) its schematic, read from the file the server sent, its check (BlueprintCheck) and its model. Its model is
// made once the check has looked at every cell once, so it is not made twice; after that only the cubes whose cells
// change are made again. One far off and not looked at for a while is let go again, to free the memory.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class ClientBlueprints {
    // Cells looked at each tick over every blueprint.
    private static final int SCAN = 40000;
    private static final double NEAR = 48.0;
    private static final long FORGET = 30000L;
    private static final long RETRY = 20000L;
    private static final float[] EDGE = { 0.45F, 0.8F, 1.0F };
    // Cells the guide looks through each tick, for each blueprint with its guide on.
    private static final int SEEK = 60000;

    static final class Ghost {
        // Its name may change while it stays the same plan.
        Blueprint blueprint;
        @Nullable
        Schematic schematic;
        @Nullable
        BlueprintCheck check;
        @Nullable
        HologramModel model;
        boolean asked;
        long askedAt;
        boolean failed;
        long wanted;
        // The layer its model was last made for while layered, -1 while it shows every layer.
        int shownLayer = -1;

        Ghost(Blueprint blueprint) {
            this.blueprint = blueprint;
        }

        void release() {
            if (this.model != null) {
                this.model.release();
                this.model = null;
            }
            this.schematic = null;
            this.check = null;
            this.asked = false;
            this.shownLayer = -1;
        }
    }

    // The block the guide points the player to: where, and what goes there (air when it is to be cleared).
    public record Guide(String blueprint, BlockPos pos, BlockState state, Item item, int layer, int layers) {
    }

    private static List<Blueprint> list = List.of();
    private static final Map<Integer, Ghost> GHOSTS = new HashMap<>();
    static long listed;
    @Nullable
    private static Guide guide;

    private ClientBlueprints() {
    }

    // Called once as the game's own client starts.
    public static void register() {
        HologramRenderer.others(ClientBlueprints::draw);
    }

    public static List<Blueprint> list() {
        return list;
    }

    public static void onList(BlueprintListPayload payload) {
        list = List.copyOf(payload.blueprints());
        listed = Util.getMillis();
        Set<Integer> kept = new HashSet<>();
        for (Blueprint blueprint : list) {
            kept.add(blueprint.id());
            Ghost ghost = GHOSTS.get(blueprint.id());
            if (ghost != null && !ghost.blueprint.samePlan(blueprint)) {
                ghost.release();
                GHOSTS.remove(blueprint.id());
            } else if (ghost != null) {
                ghost.blueprint = blueprint;
            }
        }
        GHOSTS.entrySet().removeIf(entry -> {
            if (!kept.contains(entry.getKey())) {
                entry.getValue().release();
                return true;
            }
            return false;
        });
        BlueprintViews.keepOnly(kept);
    }

    // What one kind of item a blueprint needs, and how many of it are still to be placed.
    public record Line(Item item, int needed, int left) {
    }

    // How far a blueprint is built, as far as this game has seen it; `schematic` null while its file is not in yet.
    public record Progress(@Nullable Schematic schematic, boolean failed, int needed, int placed, int unseen,
            int wrong, int turned, int inTheWay, List<Line> lines) {
    }

    public static Progress progress(int id) {
        Ghost ghost = GHOSTS.get(id);
        BlueprintCheck check = ghost == null ? null : ghost.check;
        if (check == null) {
            return new Progress(null, ghost != null && ghost.failed, 0, 0, 0, 0, 0, 0, List.of());
        }
        List<Line> lines = new ArrayList<>();
        for (int k = 0; k < check.items.size(); k++) {
            lines.add(new Line(check.items.get(k), check.needed[k], Math.max(0, check.needed[k] - check.placed[k])));
        }
        lines.sort(Comparator.comparingInt((Line line) -> line.left() == 0 ? 1 : 0).thenComparingInt(line ->
                -line.left()).thenComparingInt(line -> -line.needed()));
        return new Progress(check.schematic, false, check.neededTotal(), check.placedTotal(), check.passes > 0
                ? check.unseen : -1, check.wrong, check.turnedWrong, check.inTheWay, lines);
    }

    public static long listed() {
        return listed;
    }

    // The menu looks at it: it is read and checked wherever it is.
    public static void want(int id) {
        for (Blueprint blueprint : list) {
            if (blueprint.id() == id) {
                Ghost ghost = GHOSTS.computeIfAbsent(id, k -> new Ghost(blueprint));
                ghost.wanted = Util.getMillis();
                ask(ghost, ghost.wanted);
            }
        }
    }

    // The block the guide points to now, of the nearest blueprint with its guide on.
    @Nullable
    public static Guide guide() {
        return guide;
    }

    // The layer it shows while layered (from 0), and how many it has; -1 while it is not read yet.
    public static int layer(int id) {
        Ghost ghost = GHOSTS.get(id);
        return ghost == null || ghost.check == null ? -1 : ghost.check.layer();
    }

    // Asked again when its file has not come within RETRY (lost on the way, or refused).
    private static void ask(Ghost ghost, long now) {
        if (ghost.asked && ghost.schematic == null && now - ghost.askedAt > RETRY) {
            ghost.asked = false;
        }
        if (!ghost.asked && !ghost.failed) {
            ghost.asked = true;
            ghost.askedAt = now;
            PacketDistributor.sendToServer(new BlueprintAskPayload(BlueprintAskPayload.FILE, ghost.blueprint.id()));
        }
    }

    private record Read(Schematic schematic, BlueprintCheck check) {
    }

    // A blueprint's file is in: it is read, and its check laid out, away from the game's thread.
    public static void onFile(String file, byte[] bytes) {
        for (Ghost ghost : GHOSTS.values()) {
            if (!ghost.blueprint.file().equals(file) || ghost.schematic != null) {
                continue;
            }
            int largest = SchematicRules.largest();
            CompletableFuture.supplyAsync(() -> {
                try {
                    Schematic schematic = SchematicReader.read(bytes, file, largest);
                    return new Read(schematic, new BlueprintCheck(ghost.blueprint, schematic));
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }, Util.backgroundExecutor()).whenComplete((read, error) -> Minecraft.getInstance().execute(() -> {
                if (GHOSTS.get(ghost.blueprint.id()) != ghost) {
                    return;
                }
                if (read == null) {
                    ghost.failed = true;
                    MultiversePowers.LOGGER.warn("Could not read blueprint {}", file);
                    return;
                }
                ghost.schematic = read.schematic();
                ghost.check = read.check();
            }));
            return;
        }
    }

    private static boolean here(ClientLevel level, Blueprint blueprint) {
        return level.dimension().location().equals(blueprint.level());
    }

    private static AABB box(Blueprint blueprint) {
        BlockPos origin = blueprint.origin();
        return new AABB(origin.getX(), origin.getY(), origin.getZ(), origin.getX() + blueprint.wideX(),
                origin.getY() + blueprint.sizeY(), origin.getZ() + blueprint.wideZ());
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null) {
            return;
        }
        long now = Util.getMillis();
        double reach = minecraft.options.getEffectiveRenderDistance() * 16.0 + NEAR;
        List<Ghost> checking = new ArrayList<>();
        for (Blueprint blueprint : list) {
            boolean near = here(level, blueprint) && Math.sqrt(box(blueprint).distanceToSqr(player.position()))
                    < reach;
            Ghost ghost = GHOSTS.get(blueprint.id());
            boolean wanted = ghost != null && now - ghost.wanted < FORGET;
            if (!near && !wanted) {
                if (ghost != null && (ghost.schematic != null || ghost.asked)) {
                    ghost.release();
                }
                continue;
            }
            if (ghost == null) {
                ghost = new Ghost(blueprint);
                GHOSTS.put(blueprint.id(), ghost);
            }
            ask(ghost, now);
            if (ghost.check != null && here(level, blueprint)) {
                checking.add(ghost);
            }
        }
        guide = null;
        if (checking.isEmpty()) {
            return;
        }
        int each = Math.max(1000, SCAN / checking.size());
        double nearest = Double.MAX_VALUE;
        for (Ghost ghost : checking) {
            BlueprintCheck check = ghost.check;
            int id = ghost.blueprint.id();
            check.layered = BlueprintViews.has(id, BlueprintViews.LAYERS);
            check.scan(level, each, ghost.model);
            if (ghost.model == null && check.passes > 0 && ghost.schematic != null) {
                ghost.model = new HologramModel(ghost.schematic, level, check::look);
                refreshInTheWay(ghost.model, check);
            }
            layers(ghost, check);
            if (BlueprintViews.has(id, BlueprintViews.GUIDE) && !BlueprintViews.has(id, BlueprintViews.HIDDEN)
                    && check.passes > 0) {
                check.seek(player.getX(), player.getEyeY(), player.getZ(), SEEK);
                BlockPos at = check.targetPos();
                double distance = at == null ? Double.MAX_VALUE : at.distToCenterSqr(player.position());
                if (at != null && distance < nearest) {
                    nearest = distance;
                    BlockState wanted = check.targetState();
                    guide = new Guide(ghost.blueprint.name(), at, wanted, Materials.item(wanted), check.layer(),
                            check.schematic.sizeY);
                }
            }
        }
    }

    // Layered, the layers that come into sight or go out of it as the lowest unfinished one moves are made again.
    private static void layers(Ghost ghost, BlueprintCheck check) {
        HologramModel model = ghost.model;
        if (model == null) {
            return;
        }
        int now = check.layered ? check.layer() : -1;
        if (now == ghost.shownLayer) {
            return;
        }
        ghost.shownLayer = now;
        model.top(now);
    }

    // Blocks in the way where the plan has only air lie in cubes the model does not make by itself.
    private static void refreshInTheWay(HologramModel model, BlueprintCheck check) {
        Schematic schematic = check.schematic;
        for (int index = 0; index < check.status.length; index++) {
            if (check.status[index] == BlueprintCheck.IN_THE_WAY) {
                int x = index % schematic.sizeX;
                int rest = index / schematic.sizeX;
                model.refresh(x, rest / schematic.sizeZ, rest % schematic.sizeZ);
            }
        }
    }

    // A block of the world changed (LevelRendererMixin): the blueprints round it look at it at once.
    public static void blockChanged(BlockPos pos) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || GHOSTS.isEmpty()) {
            return;
        }
        for (Ghost ghost : GHOSTS.values()) {
            BlueprintCheck check = ghost.check;
            if (check != null && here(level, ghost.blueprint) && check.contains(pos)) {
                check.changed(level, pos, ghost.model);
            }
        }
    }

    private static void draw(RenderLevelStageEvent event, int room) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || GHOSTS.isEmpty() || ClientSettings.get(ClientSettings.SCHEMATIC_BLUEPRINTS) == 0) {
            return;
        }
        float alpha = (float) ClientSettings.get(ClientSettings.BLUEPRINT_OPACITY);
        for (Ghost ghost : GHOSTS.values()) {
            HologramModel model = ghost.model;
            int id = ghost.blueprint.id();
            if (model == null || !here(level, ghost.blueprint) || BlueprintViews.has(id, BlueprintViews.HIDDEN)) {
                continue;
            }
            room = Math.max(1, room - HologramRenderer.work(model, room));
            model.draw(event, ghost.blueprint.placement(), 1.0F, 1.0F, 1.0F, alpha);
            if (ClientSettings.get(ClientSettings.SCHEMATIC_OUTLINE) != 0 && !BlueprintViews.has(id,
                    BlueprintViews.NO_BOX)) {
                HologramRenderer.outline(event, ghost.blueprint.placement(), model.schematic(), EDGE[0], EDGE[1],
                        EDGE[2]);
            }
        }
        Guide shown = guide;
        if (shown != null) {
            // A pulsing yellow frame round the block the guide points to.
            float pulse = 0.6F + 0.4F * (float) Math.sin(Util.getMillis() / 160.0);
            HologramRenderer.box(event, new AABB(shown.pos()).inflate(0.02), 1.0F, 0.85F * pulse + 0.15F, 0.2F);
        }
    }

    @SubscribeEvent
    static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        for (Ghost ghost : GHOSTS.values()) {
            ghost.release();
        }
        GHOSTS.clear();
        guide = null;
        list = List.of();
    }
}
