package nl.tivek.multiversepowers.faction.mob.client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.client.render.entity.EntityPass;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// A creature of the Mobs page as it looks: one of its kind made in the player's game but never added to the world,
// drawn turning slowly in a box, turned by dragging. The same one gives the page its numbers as the game has them
// (`MobFacts`). A kind that cannot be made or drawn shows nothing and is not tried again.
final class MobPreview {
    private static final float TURN = 0.6F;
    private static final float TILT = -12.0F;
    private final Map<EntityType<?>, LivingEntity> made = new HashMap<>();
    private final Set<EntityType<?>> broken = new HashSet<>();
    private float yaw = 200.0F;
    private long last = Util.getMillis();
    private boolean held;

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

    void drag(double dragX) {
        this.yaw += (float) dragX * 2.5F;
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
        living.yBodyRot = this.yaw;
        living.yBodyRotO = this.yaw;
        living.setYRot(this.yaw);
        living.yRotO = this.yaw;
        living.yHeadRot = this.yaw;
        living.yHeadRotO = this.yaw;
        living.setXRot(0.0F);
        living.xRotO = 0.0F;
        Quaternionf pose = new Quaternionf().rotateZ(Mth.PI);
        Quaternionf camera = new Quaternionf().rotateX(TILT * Mth.DEG_TO_RAD);
        pose.mul(camera);
        float middleX = x + width / 2.0F;
        float feetY = y + height - 6 - (height - 12 - tall * scale) / 2.0F;
        graphics.enableScissor(x, y, x + width, y + height);
        // Drawn as the world's creatures are, so its elbows and knees bend as they do there.
        EntityPass.inWorld(true);
        try {
            InventoryScreen.renderEntityInInventory(graphics, middleX, feetY, scale, new Vector3f(), pose, camera,
                    living);
        } catch (RuntimeException e) {
            MultiversePowers.LOGGER.debug("Could not draw a {}", type, e);
            this.broken.add(type);
        } finally {
            EntityPass.inWorld(false);
        }
        graphics.disableScissor();
        return true;
    }

    void forget() {
        this.made.clear();
        this.broken.clear();
    }
}
