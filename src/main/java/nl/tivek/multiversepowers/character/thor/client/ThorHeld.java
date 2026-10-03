package nl.tivek.multiversepowers.character.thor.client;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.thor.ThorBlow;
import nl.tivek.multiversepowers.character.thor.ThorGrab;
import nl.tivek.multiversepowers.character.thor.ThorStatePayload;
import nl.tivek.multiversepowers.character.thor.client.blow.ThorFists;
import nl.tivek.multiversepowers.engine.client.pose.Stance;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Spring;
import nl.tivek.multiversepowers.engine.math.Vectors;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// A creature Thor has grabbed, as every game shows it: not limp but held up by the throat in his right fist, facing
// him, clawing at his wrist and kicking, its body swinging as his fist moves and shaken by his punches; a slam swings
// it down onto its back, and a leap carries it across both his hands over his head. Let go, it goes limp from just
// where it was drawn.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class ThorHeld {
    // A fist measured longer ago than this (his model not drawn: your own first-person view) is not trusted.
    private static final long FRESH_MS = 120L;
    // Ticks it is still drawn where it was held once let go, so its limp body starts from there.
    private static final int LINGER = 3;
    // The swing of its body under the fist: turns a tick, and how soon it settles.
    private static final double SWING = 0.09;
    private static final double SETTLE = 0.45;
    private static final Vector3f KNEE = new Vector3f(0.0F, 0.0F, -1.0F);
    private static final Vector3f RIGHT_ELBOW = new Vector3f(-1.0F, 0.4F, 0.3F);
    private static final Vector3f LEFT_ELBOW = new Vector3f(1.0F, 0.4F, 0.3F);
    private static final Vector3f DOWN = new Vector3f(0.0F, -1.0F, 0.0F);

    private static final Int2ObjectOpenHashMap<Fist> FISTS = new Int2ObjectOpenHashMap<>();
    private static final Int2ObjectOpenHashMap<Held> HELD = new Int2ObjectOpenHashMap<>();
    private static final IntOpenHashSet DRAWN = new IntOpenHashSet();

    private static final class Fist {
        Vec3 from = Vec3.ZERO;
        long when;
    }

    private static final class Held {
        final int thor;
        // Its body's middle, swinging after the fist (world, blocks), the fist it hangs from, and when it was last
        // stepped (ticks, with the partial tick).
        final Spring[] body = { new Spring(), new Spring(), new Spring() };
        boolean placed;
        float steppedAt;
        // Let go at this tick (else -1): drawn on where it was for a moment.
        int letGo = -1;
        final Quaternionf turn = new Quaternionf();
        final Vector3f move = new Vector3f();
        float yaw;

        Held(int thor) {
            this.thor = thor;
        }
    }

    private ThorHeld() {
    }

    // Whether a Thor holds this creature now: posed here, never limp.
    public static boolean holds(Entity entity) {
        Held held = HELD.get(entity.getId());
        return held != null && held.letGo < 0;
    }

    // The fist a Thor holds a creature in, measured as his model is drawn.
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model model : event.getSkins()) {
            EntityRenderer<? extends Player> renderer = event.getSkin(model);
            if (renderer instanceof PlayerRenderer player) {
                player.addLayer(new FistLayer(player));
            }
        }
    }

    private static final class FistLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
        FistLayer(PlayerRenderer renderer) {
            super(renderer);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
                float headPitch) {
            ClientThor.View view = ClientThor.view(player);
            if (view == null || !view.has(ThorStatePayload.CARRYING)) {
                return;
            }
            pose.pushPose();
            this.getParentModel().translateToHand(HumanoidArm.RIGHT, pose);
            Vector3f at = pose.last().pose().transformPosition(-1.0F / 16.0F, 8.0F / 16.0F, -1.0F / 16.0F,
                    new Vector3f());
            pose.popPose();
            Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            Fist fist = FISTS.computeIfAbsent(player.getId(), id -> new Fist());
            fist.from = camera.add(at.x, at.y, at.z).subtract(player.getPosition(partialTick));
            fist.when = Util.getMillis();
        }
    }

    // Where the creature's throat is held this frame: in the fist as his model was last drawn, or in your own
    // first-person view in the fist your view draws.
    static Vec3 fist(LivingEntity thor, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vector3f seen = thor == minecraft.player && !camera.isDetached()
                ? ThorFists.holding(minecraft.player, partialTick) : null;
        if (seen != null) {
            double stretch = stretch(minecraft);
            Vector3f look = camera.getLookVector();
            Vector3f up = camera.getUpVector();
            Vector3f left = camera.getLeftVector();
            double x = seen.x / stretch;
            double y = seen.y / stretch;
            double z = -seen.z;
            return camera.getPosition().add(-left.x() * x + up.x() * y + look.x() * z,
                    -left.y() * x + up.y() * y + look.y() * z, -left.z() * x + up.z() * y + look.z() * z);
        }
        Fist fist = FISTS.get(thor.getId());
        if (fist != null && Util.getMillis() - fist.when <= FRESH_MS) {
            return thor.getPosition(partialTick).add(fist.from);
        }
        return ThorGrab.grip(thor, partialTick);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.isPaused()) {
            return;
        }
        int now = ClientThor.ticks();
        ClientThor.carriers((thor, view) -> {
            if (view.carried >= 0 && view.move() != ThorStatePayload.DIVE) {
                Held held = HELD.get(view.carried);
                if (held == null || held.thor != thor || held.letGo >= 0) {
                    HELD.put(view.carried, new Held(thor));
                }
            }
        });
        HELD.int2ObjectEntrySet().removeIf(entry -> {
            Held held = entry.getValue();
            Entity thor = level.getEntity(held.thor);
            ClientThor.View view = thor == null ? null : ClientThor.view(thor);
            boolean still = view != null && view.has(ThorStatePayload.CARRYING) && view.carried == entry.getIntKey();
            if (!still && held.letGo < 0) {
                held.letGo = now;
            }
            Entity creature = level.getEntity(entry.getIntKey());
            if (creature instanceof LivingEntity living && held.letGo < 0) {
                // Moved every tick, it would walk on the spot.
                living.walkAnimation.setSpeed(0.0F);
            }
            return creature == null || held.letGo >= 0 && now - held.letGo > LINGER;
        });
        FISTS.int2ObjectEntrySet().removeIf(entry -> Util.getMillis() - entry.getValue().when > 2000L);
    }

    // It hangs from his fist: turned to face him, its throat on the fist, its body swung the way it hangs.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        if (event.isCanceled()) {
            return;
        }
        LivingEntity entity = event.getEntity();
        Held held = HELD.get(entity.getId());
        if (held == null) {
            return;
        }
        float partialTick = event.getPartialTick();
        if (held.letGo < 0) {
            Entity thor = entity.level().getEntity(held.thor);
            if (!(thor instanceof LivingEntity living)) {
                return;
            }
            place(held, entity, living, partialTick);
        }
        if (!held.placed) {
            return;
        }
        entity.yBodyRot = held.yaw;
        entity.yBodyRotO = held.yaw;
        entity.yHeadRot = held.yaw;
        entity.yHeadRotO = held.yaw;
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(held.move.x, held.move.y, held.move.z);
        pose.mulPose(held.turn);
        pose.translate(0.0F, (float) -ThorGrab.throat(entity), 0.0F);
        DRAWN.add(entity.getId());
    }

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        if (DRAWN.remove(event.getEntity().getId())) {
            event.getPoseStack().popPose();
        }
    }

    // This frame's hold: the fist, the way its body hangs from it (swinging after the fist, and swung by what he does
    // with it), and where it is drawn from its own place.
    private static void place(Held held, LivingEntity entity, LivingEntity thor, float partialTick) {
        ClientThor.View view = ClientThor.view(thor);
        Vec3 fist = fist(thor, partialTick);
        double reach = Math.max(0.3, ThorGrab.throat(entity) * 0.55);
        Vec3 look = Vec3.directionFromRotation(0.0F, thor.getViewYRot(partialTick));
        Vec3 hang = hang(view, look, partialTick);
        Vec3 target = fist.add(hang.scale(reach));
        float time = ClientThor.ticks() + partialTick;
        double dt = held.placed ? Mth.clamp(time - held.steppedAt, 0.0F, 2.0F) : 0.0;
        held.steppedAt = time;
        double[] goal = { target.x, target.y, target.z };
        for (int i = 0; i < 3; i++) {
            if (!held.placed) {
                held.body[i].set(goal[i]);
            } else {
                held.body[i].step(goal[i], dt, SWING, SETTLE);
            }
        }
        held.placed = true;
        Vector3f way = new Vector3f((float) (held.body[0].value - fist.x), (float) (held.body[1].value - fist.y),
                (float) (held.body[2].value - fist.z));
        if (way.lengthSquared() < 1.0E-6F) {
            way.set(DOWN);
        }
        way.normalize();
        held.turn.rotationTo(DOWN, way);
        // Never drawn into the ground he stands on (slammed, its body would sink in, and go limp from inside it).
        if (thor.onGround()) {
            double floor = Mth.lerp(partialTick, thor.yo, thor.getY());
            double low = Math.min(fist.y, fist.y + way.y * ThorGrab.throat(entity)) - entity.getBbWidth() * 0.5;
            if (low < floor) {
                fist = fist.add(0.0, floor - low, 0.0);
            }
        }
        Vec3 drawn = entity.getPosition(partialTick);
        held.move.set((float) (fist.x - drawn.x), (float) (fist.y - drawn.y), (float) (fist.z - drawn.z));
        Vec3 to = thor.getPosition(partialTick).subtract(fist);
        held.yaw = (float) Math.toDegrees(Math.atan2(-to.x, to.z));
    }

    // Which way its body hangs from the fist: down, swung away from him onto its back as he slams it, or across both
    // his hands over his head as he leaps with it.
    private static Vec3 hang(@Nullable ClientThor.View view, Vec3 look, float partialTick) {
        Vec3 down = new Vec3(0.0, -1.0, 0.0);
        if (view == null) {
            return down;
        }
        if (view.move() == ThorStatePayload.HOIST) {
            return look.cross(Vectors.UP).normalize().scale(-1.0);
        }
        ThorBlow blow = ThorBlow.byIndex(view.blow);
        if (blow == ThorBlow.GRAB_SLAM) {
            float age = view.blowAge(partialTick) / ThorBlow.PACE;
            // Lifted high it hangs down; driven down from the top it swings out ahead, onto its back.
            float over = (float) Ease.smooth(Mth.clamp((age - 7.0F) / 3.0F, 0.0F, 1.0F));
            return down.scale(1.0 - over).add(look.scale(over)).normalize();
        }
        return down;
    }

    // Its body while held, if it is built as a person: hands up at its throat on his wrist, head thrown back, legs
    // kicking.
    public static boolean pose(EntityModel<?> model, LivingEntity entity, float partialTick) {
        if (!(model instanceof HumanoidModel<?> person) || !Stance.person(person) || !holds(entity)) {
            return false;
        }
        float time = entity.tickCount + partialTick + entity.getId() * 3.0F;
        float claw = (float) Math.sin(time * 0.7) * 0.5F;
        Stance.arm(person, true, new Vector3f(-1.4F + claw, -0.6F, -4.6F), RIGHT_ELBOW);
        Stance.arm(person, false, new Vector3f(1.4F - claw, -0.4F, -4.6F), LEFT_ELBOW);
        for (int side = 0; side < 2; side++) {
            float sign = side == 0 ? -1.0F : 1.0F;
            float kick = (float) Math.sin(time * 0.55 + side * Math.PI);
            Stance.leg(person, side == 0, new Vector3f(sign * Stance.HIP_X, Stance.GROUND - 2.0F - 1.8F * kick,
                    -1.0F - 2.2F * Math.max(0.0F, kick)), KNEE);
        }
        person.head.xRot = -0.45F + (float) Math.sin(time * 0.9) * 0.08F;
        person.head.yRot = (float) Math.sin(time * 0.37) * 0.25F;
        return true;
    }

    // Hands draw with a fixed 70 degree view; stretched by this, a hand meets what the world draws.
    private static double stretch(Minecraft minecraft) {
        return Math.tan(Math.toRadians(35.0)) / Math.tan(Math.toRadians(minecraft.options.fov().get() * 0.5));
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        HELD.clear();
        FISTS.clear();
        DRAWN.clear();
    }
}
