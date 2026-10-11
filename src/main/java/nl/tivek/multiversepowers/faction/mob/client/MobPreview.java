package nl.tivek.multiversepowers.faction.mob.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.client.render.entity.EntityPass;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// A creature of the Mobs page as it looks: one of its kind made in the player's game but never added to the world,
// drawn turning slowly in a box, turned by dragging. The same one gives the page its numbers as the game has them
// (`MobFacts`). A kind that cannot be made or drawn shows nothing and is not tried again. A kind of the mod's own
// does something now and then (its acts, at random, a few seconds apart) rather than stand stock still.
public final class MobPreview {
    private static final float TURN = 0.6F;
    private static final float TILT = -12.0F;
    private static final long FIRST_REST = 1200L;
    private static final long REST = 2500L;
    private static final int REST_SPREAD = 3500;
    private static final Map<EntityType<?>, Drawer> DRAWERS = new HashMap<>();
    private static final Map<EntityType<?>, List<Idle>> IDLES = new HashMap<>();
    @Nullable
    private static Entity drawing;
    // Every kind with acts also looks round of itself: to one side and the other, and up.
    private static final Idle LOOK = new Idle(70, new Act<>() {
        @Override
        public void at(LivingEntity living, double ticks) {
            float s = (float) (ticks / 70.0);
            turnHead(living, 55.0F * Mth.sin(s * Mth.TWO_PI) * Mth.sin(s * Mth.PI), 8.0F * Mth.sin(s * Mth.PI));
        }
    });
    private static final Idle PEER = new Idle(50, new Act<>() {
        @Override
        public void at(LivingEntity living, double ticks) {
            float up = Mth.sin((float) (ticks / 50.0) * Mth.PI);
            turnHead(living, 14.0F * up, -28.0F * up * up);
        }
    });

    // A kind its game's own renderer leaves undrawn in a screen (drawn with the world, as a construct is) draws
    // itself: in `pose`, where it stands at the origin as in the world, seen from `camera`.
    public interface Drawer {
        void draw(PoseStack pose, Vec3 camera, LivingEntity living, float partialTick,
                MultiBufferSource.BufferSource buffers);
    }

    // Something a creature does while shown: started on it, posed every frame by its age in ticks, then ended.
    public interface Act<T extends LivingEntity> {
        default void start(T living) {
        }

        default void at(T living, double ticks) {
        }

        default void end(T living) {
        }
    }

    private record Idle(int ticks, Act<LivingEntity> act) {
    }

    // Whether `entity` is being drawn in a preview right now, so its own pose may stand for its picture.
    public static boolean drawing(Entity entity) {
        return entity == drawing;
    }

    public static void drawWith(EntityType<?> type, Drawer drawer) {
        DRAWERS.put(type, drawer);
    }

    // A kind that only looks round.
    public static void looks(EntityType<?> type) {
        IDLES.computeIfAbsent(type, k -> new ArrayList<>(List.of(LOOK, PEER)));
    }

    @SuppressWarnings("unchecked")
    public static <T extends LivingEntity> void act(EntityType<T> type, int ticks, Act<? super T> act) {
        looks(type);
        IDLES.get(type).add(new Idle(ticks, (Act<LivingEntity>) act));
    }

    // A move of its own, started and ended in this game alone, as the server would tell it.
    public static <T extends LivingEntity> void move(EntityType<T> type, int ticks, Consumer<T> start,
            Consumer<T> end) {
        act(type, ticks, new Act<T>() {
            @Override
            public void start(T living) {
                start.accept(living);
            }

            @Override
            public void end(T living) {
                end.accept(living);
            }
        });
    }

    private static void turnHead(LivingEntity living, float yaw, float pitch) {
        living.yHeadRot += yaw;
        living.yHeadRotO = living.yHeadRot;
        living.setXRot(pitch);
        living.xRotO = pitch;
    }

    // As InventoryScreen.renderEntityInInventory, at a partial tick of its own so it moves smoothly.
    private static void entity(GuiGraphics graphics, float x, float y, float scale, Quaternionf pose,
            Quaternionf camera, LivingEntity living, float partialTick) {
        PoseStack stack = graphics.pose();
        stack.pushPose();
        stack.translate(x, y, 50.0F);
        stack.scale(scale, scale, -scale);
        stack.mulPose(pose);
        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        dispatcher.overrideCameraOrientation(camera.conjugate(new Quaternionf()).rotateY(Mth.PI));
        dispatcher.setRenderShadow(false);
        RenderSystem.runAsFancy(() -> dispatcher.render(living, 0.0, 0.0, 0.0, 0.0F, partialTick, stack,
                graphics.bufferSource(), LightTexture.FULL_BRIGHT));
        graphics.flush();
        dispatcher.setRenderShadow(true);
        stack.popPose();
        Lighting.setupFor3DItems();
    }

    private static void own(GuiGraphics graphics, Drawer drawer, LivingEntity living, float middleX, float feetY,
            float scale, Quaternionf pose, float tall, float partialTick) {
        graphics.flush();
        Vector3f toward = new Quaternionf(pose).conjugate().transform(new Vector3f(0.0F, 0.0F, -1.0F));
        Vec3 camera = new Vec3(toward.x() * 60.0, tall * 0.5 + toward.y() * 60.0, toward.z() * 60.0);
        PoseStack stack = graphics.pose();
        stack.pushPose();
        stack.translate(middleX, feetY, 50.0F);
        stack.scale(scale, scale, -scale);
        stack.mulPose(pose);
        stack.translate(camera.x, camera.y, camera.z);
        drawer.draw(stack, camera, living, partialTick, graphics.bufferSource());
        stack.popPose();
        graphics.flush();
    }

    private final Map<EntityType<?>, LivingEntity> made = new HashMap<>();
    private final Set<EntityType<?>> broken = new HashSet<>();
    private final RandomSource random = RandomSource.create();
    private float yaw = 200.0F;
    private long last = Util.getMillis();
    private boolean held;
    @Nullable
    private EntityType<?> acting;
    @Nullable
    private LivingEntity actor;
    @Nullable
    private Idle act;
    @Nullable
    private Idle lastAct;
    private long actFrom;
    private long nextAct;

    @Nullable
    LivingEntity of(EntityType<?> type) {
        if (this.broken.contains(type)) {
            return null;
        }
        LivingEntity living = this.made.get(type);
        if (living != null) {
            return living;
        }
        Minecraft minecraft = Minecraft.getInstance();
        try {
            Entity entity = minecraft.level == null ? null : type.create(minecraft.level);
            if (entity instanceof LivingEntity made) {
                this.made.put(type, made);
                return made;
            }
        } catch (RuntimeException | LinkageError e) {
            MultiversePowers.LOGGER.debug("Could not make a {} to show", type, e);
        }
        this.broken.add(type);
        return null;
    }

    void hold(boolean held) {
        this.held = held;
    }

    boolean held() {
        return this.held;
    }

    // Its near side follows the mouse, as a thing turned by hand does.
    void drag(double dragX) {
        this.yaw -= (float) dragX * 2.5F;
    }

    // Draws `type` standing in the box, as large as fits; whether it could.
    boolean draw(GuiGraphics graphics, EntityType<?> type, int x, int y, int width, int height) {
        LivingEntity living = this.of(type);
        if (living == null) {
            return false;
        }
        long now = Util.getMillis();
        if (!this.held) {
            this.yaw += TURN * Math.min(100L, now - this.last) / 16.0F;
        }
        this.last = now;
        float tall = Math.max(0.3F, living.getBbHeight());
        float wide = Math.max(0.3F, living.getBbWidth());
        float scale = Math.min((height - 14) / tall, (width - 12) / (wide * 1.5F));
        scale = Mth.clamp(scale, 2.0F, 60.0F);
        living.tickCount = (int) (now / 50L);
        float partialTick = (now % 50L) / 50.0F;
        living.yBodyRot = this.yaw;
        living.yBodyRotO = this.yaw;
        living.setYRot(this.yaw);
        living.yRotO = this.yaw;
        living.yHeadRot = this.yaw;
        living.yHeadRotO = this.yaw;
        living.setXRot(0.0F);
        living.xRotO = 0.0F;
        this.idle(type, living, now);
        Quaternionf pose = new Quaternionf().rotateZ(Mth.PI);
        Quaternionf camera = new Quaternionf().rotateX(TILT * Mth.DEG_TO_RAD);
        pose.mul(camera);
        float middleX = x + width / 2.0F;
        float feetY = y + height - 6 - (height - 12 - tall * scale) / 2.0F;
        graphics.enableScissor(x, y, x + width, y + height);
        // Drawn as the world's creatures are, so its elbows and knees bend as they do there.
        EntityPass.inWorld(true);
        drawing = living;
        try {
            Drawer drawer = DRAWERS.get(type);
            if (drawer != null) {
                own(graphics, drawer, living, middleX, feetY, scale, pose, tall, partialTick);
            } else {
                entity(graphics, middleX, feetY, scale, pose, camera, living, partialTick);
            }
        } catch (RuntimeException e) {
            MultiversePowers.LOGGER.debug("Could not draw a {}", type, e);
            this.broken.add(type);
        } finally {
            EntityPass.inWorld(false);
            drawing = null;
        }
        graphics.disableScissor();
        return true;
    }

    // Plays the shown kind's acts: one at a time, a rest between them, never the same twice in a row.
    private void idle(EntityType<?> type, LivingEntity living, long now) {
        if (type != this.acting || living != this.actor) {
            this.stop();
            this.acting = type;
            this.actor = living;
            this.nextAct = now + FIRST_REST;
        }
        List<Idle> acts = IDLES.get(type);
        if (acts == null) {
            return;
        }
        if (this.act != null) {
            double ticks = (now - this.actFrom) / 50.0;
            if (ticks < this.act.ticks()) {
                this.act.act().at(living, ticks);
                return;
            }
            this.act.act().end(living);
            this.act = null;
            this.nextAct = now + REST + this.random.nextInt(REST_SPREAD);
        }
        if (now < this.nextAct) {
            return;
        }
        Idle next = acts.get(this.random.nextInt(acts.size()));
        if (next == this.lastAct) {
            next = acts.get((acts.indexOf(next) + 1 + this.random.nextInt(acts.size() - 1)) % acts.size());
        }
        this.act = next;
        this.lastAct = next;
        this.actFrom = now;
        next.act().start(living);
        next.act().at(living, 0.0);
    }

    private void stop() {
        if (this.act != null && this.actor != null) {
            this.act.act().end(this.actor);
        }
        this.act = null;
        this.lastAct = null;
    }

    void forget() {
        this.stop();
        this.actor = null;
        this.acting = null;
        this.made.clear();
        this.broken.clear();
    }
}
