package nl.tivek.multiversepowers.engine.client.cloth;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.config.client.ClientSettings;
import nl.tivek.multiversepowers.engine.client.model.ModelParts;
import nl.tivek.multiversepowers.engine.client.render.EntityPass;
import nl.tivek.multiversepowers.engine.physics.Blocks;
import nl.tivek.multiversepowers.engine.physics.Strands;
import org.joml.Matrix4f;
import org.joml.Vector3f;

// A player's cape as a sheet of cloth: it hangs from the shoulders, trails and swings as they run and turn, and folds
// against their back and legs instead of passing through them. Drawn with the player's own cape picture on both sides.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class CapeCloth {
    private static final int COLUMNS = 6;
    private static final int ROWS = 9;
    // The cape's size and where it hangs, in pixels of the player model: across the back, and its top edge.
    private static final float WIDE = 10.0F;
    private static final float LONG = 16.0F;
    private static final float TOP = 0.5F;
    private static final float BACK = 2.2F;
    private static final int SUBSTEPS = 8;
    private static final double TICK = 0.05;
    private static final double NEAR = 32.0;
    private static final double LEAP = 4.0;
    private static final int FORGET = 20;
    // The round bodies the cape keeps off: three down the back, one down each leg and arm (from, to, radius; pixels).
    private static final float[][] TORSO = { { -2.6F, 1.0F, 0.0F, -2.6F, 11.0F, 0.0F, 2.3F },
            { 0.0F, 1.0F, 0.0F, 0.0F, 11.0F, 0.0F, 2.3F }, { 2.6F, 1.0F, 0.0F, 2.6F, 11.0F, 0.0F, 2.3F } };
    private static final float[] LIMB = { 0.0F, 1.5F, 0.0F, 0.0F, 10.5F, 0.0F, 2.2F };
    private static final int CAPSULES = 7;

    private static final Int2ObjectOpenHashMap<Cape> CAPES = new Int2ObjectOpenHashMap<>();
    private static final Matrix4f FRAME = new Matrix4f();
    private static final Matrix4f BACKWARD = new Matrix4f();
    private static final Vector3f POINT = new Vector3f();
    private static final double[] AT = new double[3];
    private static final float[] MODEL = new float[COLUMNS * ROWS * 3];
    private static final double[] TOP_NOW = new double[3];
    private static int ticks;

    private static final class Cape {
        final Strands cloth = Strands.cloth(COLUMNS, ROWS, WIDE / 16.0, LONG / 16.0, 0.0, 0.0, 0.0);
        final double[] anchors = new double[COLUMNS * 3];
        final double[] bodies = new double[CAPSULES * 7];
        boolean placed;
        int seen;

        Cape() {
            // Light cloth catches the air: running lifts it well out behind.
            this.cloth.drag = 6.0;
            this.cloth.slide = 0.5;
        }
    }

    private CapeCloth() {
    }

    // Draws the player's cape as cloth, in the cape layer's own pose; false leaves it to the game.
    public static boolean draw(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
            PlayerModel<?> model, float partialTick) {
        // Only in the world: a player drawn in a screen (the inventory) keeps the stiff cape, or its cloth would be
        // pulled there and back every frame.
        if (!ClientSettings.capeCloth() || !EntityPass.inWorld() || player.isInvisible()
                || !player.isModelPartShown(PlayerModelPart.CAPE)
                || player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
            return false;
        }
        ResourceLocation texture = player.getSkin().capeTexture();
        Minecraft minecraft = Minecraft.getInstance();
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        if (texture == null || player.distanceToSqr(camera) > NEAR * NEAR) {
            return false;
        }
        Cape cape = CAPES.get(player.getId());
        if (cape == null) {
            cape = new Cape();
            CAPES.put(player.getId(), cape);
        }
        cape.seen = ticks;
        Matrix4f drawn = pose.last().pose();
        remember(cape, model, drawn, camera);
        if (!cape.placed) {
            for (int r = 0; r < ROWS; r++) {
                for (int c = 0; c < COLUMNS; c++) {
                    cape.cloth.place(r * COLUMNS + c, cape.anchors[c * 3], cape.anchors[c * 3 + 1] - LONG / 16.0 * r
                            / (ROWS - 1), cape.anchors[c * 3 + 2]);
                }
            }
            cape.placed = true;
        }
        BACKWARD.set(drawn).invert();
        for (int i = 0; i < COLUMNS * ROWS; i++) {
            cape.cloth.point(i, partialTick, AT);
            BACKWARD.transformPosition(POINT.set((float) (AT[0] - camera.x), (float) (AT[1] - camera.y),
                    (float) (AT[2] - camera.z)));
            MODEL[i * 3] = POINT.x;
            MODEL[i * 3 + 1] = POINT.y;
            MODEL[i * 3 + 2] = POINT.z;
        }
        VertexConsumer buffer = buffers.getBuffer(RenderType.entitySolid(texture));
        PoseStack.Pose last = pose.last();
        for (int r = 0; r + 1 < ROWS; r++) {
            for (int c = 0; c + 1 < COLUMNS; c++) {
                int a = r * COLUMNS + c;
                int b = a + 1;
                int d = a + COLUMNS;
                int e = d + 1;
                // Seen from behind, the outside shows the picture's front; against the back, its inside.
                vertex(buffer, last, a, outerU(c), v(r), light, 1.0F);
                vertex(buffer, last, b, outerU(c + 1), v(r), light, 1.0F);
                vertex(buffer, last, e, outerU(c + 1), v(r + 1), light, 1.0F);
                vertex(buffer, last, d, outerU(c), v(r + 1), light, 1.0F);
                vertex(buffer, last, a, innerU(c), v(r), light, -1.0F);
                vertex(buffer, last, d, innerU(c), v(r + 1), light, -1.0F);
                vertex(buffer, last, e, innerU(c + 1), v(r + 1), light, -1.0F);
                vertex(buffer, last, b, innerU(c + 1), v(r), light, -1.0F);
            }
        }
        return true;
    }

    private static float x(int column) {
        return -WIDE * 0.5F + WIDE * column / (COLUMNS - 1);
    }

    private static float outerU(int column) {
        return (6.0F - x(column)) / 64.0F;
    }

    private static float innerU(int column) {
        return (17.0F + x(column)) / 64.0F;
    }

    private static float v(int row) {
        return (1.0F + LONG * row / (ROWS - 1)) / 32.0F;
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, int i, float u, float v, int light,
            float side) {
        int c = i % COLUMNS;
        int r = i / COLUMNS;
        int left = r * COLUMNS + Math.max(0, c - 1);
        int right = r * COLUMNS + Math.min(COLUMNS - 1, c + 1);
        int up = Math.max(0, r - 1) * COLUMNS + c;
        int down = Math.min(ROWS - 1, r + 1) * COLUMNS + c;
        float ax = MODEL[right * 3] - MODEL[left * 3];
        float ay = MODEL[right * 3 + 1] - MODEL[left * 3 + 1];
        float az = MODEL[right * 3 + 2] - MODEL[left * 3 + 2];
        float bx = MODEL[down * 3] - MODEL[up * 3];
        float by = MODEL[down * 3 + 1] - MODEL[up * 3 + 1];
        float bz = MODEL[down * 3 + 2] - MODEL[up * 3 + 2];
        float nx = ay * bz - az * by;
        float ny = az * bx - ax * bz;
        float nz = ax * by - ay * bx;
        float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (length < 1.0E-6F) {
            nx = 0.0F;
            ny = 0.0F;
            nz = 1.0F;
        } else {
            nx /= length;
            ny /= length;
            nz /= length;
        }
        buffer.addVertex(pose, MODEL[i * 3], MODEL[i * 3 + 1], MODEL[i * 3 + 2]).setColor(-1).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx * side, ny * side,
                        nz * side);
    }

    // Where the cape's top edge and the round bodies it keeps off are in the world as the player is drawn now, for
    // the cloth's next step.
    private static void remember(Cape cape, PlayerModel<?> model, Matrix4f drawn, Vec3 camera) {
        FRAME.set(drawn);
        ModelParts.apply(FRAME, model.body);
        for (int c = 0; c < COLUMNS; c++) {
            FRAME.transformPosition(POINT.set(x(c) / 16.0F, TOP / 16.0F, BACK / 16.0F));
            cape.anchors[c * 3] = POINT.x + camera.x;
            cape.anchors[c * 3 + 1] = POINT.y + camera.y;
            cape.anchors[c * 3 + 2] = POINT.z + camera.z;
        }
        int k = 0;
        for (float[] line : TORSO) {
            body(cape, k++, FRAME, line, camera);
        }
        ModelPart[] limbs = { model.rightLeg, model.leftLeg, model.rightArm, model.leftArm };
        for (ModelPart limb : limbs) {
            FRAME.set(drawn);
            ModelParts.apply(FRAME, limb);
            float[] line = LIMB.clone();
            line[0] = limb == model.rightArm ? -1.0F : limb == model.leftArm ? 1.0F : 0.0F;
            line[3] = line[0];
            body(cape, k++, FRAME, line, camera);
        }
    }

    private static void body(Cape cape, int k, Matrix4f frame, float[] line, Vec3 camera) {
        int o = k * 7;
        frame.transformPosition(POINT.set(line[0] / 16.0F, line[1] / 16.0F, line[2] / 16.0F));
        cape.bodies[o] = POINT.x + camera.x;
        cape.bodies[o + 1] = POINT.y + camera.y;
        cape.bodies[o + 2] = POINT.z + camera.z;
        frame.transformPosition(POINT.set(line[3] / 16.0F, line[4] / 16.0F, line[5] / 16.0F));
        cape.bodies[o + 3] = POINT.x + camera.x;
        cape.bodies[o + 4] = POINT.y + camera.y;
        cape.bodies[o + 5] = POINT.z + camera.z;
        cape.bodies[o + 6] = line[6] / 16.0F * (float) Math.sqrt(frame.m00() * frame.m00() + frame.m01() * frame.m01()
                + frame.m02() * frame.m02());
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            CAPES.clear();
            return;
        }
        if (minecraft.isPaused()) {
            return;
        }
        ticks++;
        ObjectIterator<Cape> capes = CAPES.values().iterator();
        while (capes.hasNext()) {
            Cape cape = capes.next();
            if (ticks - cape.seen > FORGET || !cape.placed) {
                if (ticks - cape.seen > FORGET) {
                    capes.remove();
                }
                continue;
            }
            Strands cloth = cape.cloth;
            cloth.point(0, 1.0, TOP_NOW);
            double dx = cape.anchors[0] - TOP_NOW[0];
            double dy = cape.anchors[1] - TOP_NOW[1];
            double dz = cape.anchors[2] - TOP_NOW[2];
            if (dx * dx + dy * dy + dz * dz > LEAP * LEAP) {
                cloth.shift(dx, dy, dz);
            }
            for (int c = 0; c < COLUMNS; c++) {
                cloth.pin(c, cape.anchors[c * 3], cape.anchors[c * 3 + 1], cape.anchors[c * 3 + 2], TICK);
            }
            cloth.clearCapsules();
            for (int k = 0; k < CAPSULES; k++) {
                int o = k * 7;
                cloth.capsule(cape.bodies[o], cape.bodies[o + 1], cape.bodies[o + 2], cape.bodies[o + 3],
                        cape.bodies[o + 4], cape.bodies[o + 5], cape.bodies[o + 6]);
            }
            cloth.step(TICK, SUBSTEPS, Blocks.NONE);
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        CAPES.clear();
    }
}
