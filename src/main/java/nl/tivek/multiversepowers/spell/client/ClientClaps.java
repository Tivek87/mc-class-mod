package nl.tivek.multiversepowers.spell.client;

import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.client.pose.Stance;
import nl.tivek.multiversepowers.engine.client.render.FirstPersonArm;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.spell.ClapPayload;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// The thunderclap as a body does it: while the button is held the caster winds up, leaning back with the arms flung
// wide and static crackling in the hands; the moment it goes off the hands slam together ahead of the chest, the body
// driving forward over the knees, and rock back from the shock.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class ClientClaps {
    // The server's effect takes its first step a tick after the cast.
    private static final float MEET = ClapPayload.HANDS_MEET + 1.0F;
    // How long the wind-up takes to open all the way: the button's hold, less the ticks before it shows.
    private static final float WIND = 12.0F;
    private static final float HOLD = MEET + 8.0F;
    private static final float END = HOLD + 7.0F;
    private static final float HEAD_BACK = -0.5F;
    private static final float LEAN_BACK = -0.34F;
    private static final float ARCH_BACK = -0.24F;

    static final Vector3f CLAP = new Vector3f(0.05F, -0.1F, -0.75F);
    private static final Vector3f SPREAD = new Vector3f(1.02F, 0.08F, -0.38F);
    private static final Vector3f ARM_FROM = new Vector3f(0.75F, -1.1F, -0.15F);
    private static final Vector3f WIDE_FROM = new Vector3f(1.25F, -0.5F, 0.2F);

    // The hands in the chest's frame (pixels, +x the left hand's side): flung wide, a little high and back, and met
    // ahead of the chest.
    private static final Vector3f WIDE = new Vector3f(13.5F, -3.0F, 1.5F);
    private static final Vector3f MET = new Vector3f(2.0F, 0.5F, -8.5F);
    private static final Vector3f KNEE = new Vector3f(0.0F, 0.0F, -1.0F);
    private static final Vector3f WIDE_ELBOW = new Vector3f(0.3F, 1.0F, 0.6F);
    private static final Vector3f MET_ELBOW = new Vector3f(0.8F, 0.6F, 0.3F);
    private static final Quaternionf LEAN = new Quaternionf();
    private static final Quaternionf WAIST = new Quaternionf();
    private static final Quaternionf CHEST = new Quaternionf();
    private static final Vector3f HIPS = new Vector3f();
    private static final Vector3f NECK = new Vector3f();

    private static final Int2LongMap STARTED = new Int2LongOpenHashMap();
    private static final Int2LongMap CHARGED = new Int2LongOpenHashMap();

    private ClientClaps() {
    }

    public static void clap(int entity) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            STARTED.put(entity, minecraft.level.getGameTime());
            CHARGED.remove(entity);
        }
    }

    // A caster starts or stops winding up a clap.
    public static void charging(int entity, boolean on) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!on) {
            CHARGED.remove(entity);
        } else if (minecraft.level != null && !CHARGED.containsKey(entity)) {
            CHARGED.put(entity, minecraft.level.getGameTime());
        }
    }

    // Whether a clap has this body now, winding up or going off: nothing else poses its arms meanwhile.
    public static boolean active(Entity entity) {
        return STARTED.containsKey(entity.getId()) || CHARGED.containsKey(entity.getId());
    }

    // How far the wind-up is, 0 to 1, or -1 when there is none.
    private static float charge(Entity entity, float partialTick) {
        if (!CHARGED.containsKey(entity.getId())) {
            return -1.0F;
        }
        float held = entity.level().getGameTime() - CHARGED.get(entity.getId()) + partialTick;
        return Mth.clamp(held / WIND, 0.0F, 1.0F);
    }

    private static float age(Entity entity, float partialTick) {
        if (!STARTED.containsKey(entity.getId())) {
            return -1.0F;
        }
        float age = entity.level().getGameTime() - STARTED.get(entity.getId()) + partialTick;
        if (age >= END || age < 0.0F) {
            STARTED.remove(entity.getId());
            return -1.0F;
        }
        return age;
    }

    // The moment as a shape: how far the arms are up (1 while held wide or clapping), how far open (1 wide, 0 met),
    // how far the body has driven forward into the clap, and the jolt of its shock.
    private record Shape(float up, float open, float drive, float shock) {
    }

    @Nullable
    private static Shape shape(Entity entity, float partialTick) {
        float age = age(entity, partialTick);
        if (age >= 0.0F) {
            float shut = Math.min(1.0F, age / MEET);
            float up = age < HOLD ? 1.0F : 1.0F - (float) Ease.smooth((age - HOLD) / (END - HOLD));
            float drive = (float) (Ease.smooth(shut) * (1.0 - Ease.smooth((age - HOLD) / (END - HOLD))));
            float shock = (float) Ease.bump(Mth.clamp((age - MEET) / 6.0F, 0.0F, 1.0F));
            // Shut hard: slow off the wide mark, fastest as the palms meet.
            return new Shape(up, 1.0F - shut * shut, drive, shock);
        }
        float charge = charge(entity, partialTick);
        return charge < 0.0F ? null : new Shape((float) Ease.smooth(charge), 1.0F, 0.0F, 0.0F);
    }

    public static boolean pose(PlayerModel<?> model, Entity entity) {
        Shape shape = shape(entity, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
        if (shape == null) {
            return false;
        }
        float pitch = model.head.xRot;
        float back = shape.up() * shape.open() * (1.0F - shape.drive());
        model.head.xRot = Mth.lerp(back, pitch, Math.max(-1.4F, pitch + HEAD_BACK));
        body(model, shape);
        return true;
    }

    // The whole body: set wide and low, leaning back with the chest thrown open while the arms are flung wide, then
    // driving forward over the knees as they slam together, and rocking back from the shock.
    private static void body(PlayerModel<?> model, Shape shape) {
        float up = shape.up();
        float open = shape.open();
        float drive = shape.drive();
        float shock = shape.shock();
        Vector3f[] feet = { Stance.foot(model, true, new Vector3f()), Stance.foot(model, false, new Vector3f()) };
        float wound = up * open * (1.0F - drive);
        float drop = up * (1.6F + 1.8F * drive) - 0.6F * shock;
        LEAN.rotationX(LEAN_BACK * wound + up * (0.26F * drive - 0.08F * shock));
        WAIST.rotationX(ARCH_BACK * wound + up * 0.2F * drive);
        HIPS.set(0.0F, Stance.HIP_Y + drop, 0.3F * up);
        Stance.trunk(model, HIPS, LEAN, WAIST);
        for (int side = 0; side < 2; side++) {
            float sign = side == 0 ? -1.0F : 1.0F;
            Vector3f foot = feet[side].lerp(new Vector3f(sign * 3.3F, Stance.GROUND, side == 0 ? 1.2F : -1.0F), up);
            Stance.leg(model, side == 0, foot, KNEE);
        }
        Stance.neck(NECK);
        Stance.chest(CHEST);
        for (int side = 0; side < 2; side++) {
            boolean right = side == 0;
            float sign = right ? -1.0F : 1.0F;
            Vector3f aim = new Vector3f(MET).lerp(WIDE, open);
            aim.x *= sign;
            Vector3f target = CHEST.transform(aim).add(NECK);
            Vector3f hand = Stance.hand(model, right, new Vector3f()).lerp(target, up);
            Vector3f elbow = new Vector3f(MET_ELBOW).lerp(WIDE_ELBOW, open);
            elbow.x *= sign;
            Stance.arm(model, right, hand, elbow);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || player.isInvisible()) {
            return;
        }
        Shape shape = shape(player, event.getPartialTick());
        if (shape == null) {
            return;
        }
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        PlayerRenderer renderer = (PlayerRenderer) minecraft.getEntityRenderDispatcher().getRenderer(player);
        for (float side = -1.0F; side <= 1.0F; side += 2.0F) {
            Vector3f rest = side > 0.0F ? FirstPersonArm.HAND_RIGHT : FirstPersonArm.HAND_LEFT;
            Vector3f target = new Vector3f(CLAP).lerp(SPREAD, shape.open());
            target.x *= side;
            Vector3f hand = new Vector3f(rest).lerp(target, shape.up());
            Vector3f from = new Vector3f(ARM_FROM).lerp(WIDE_FROM, shape.open());
            from.x *= side;
            FirstPersonArm.arm(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), player,
                    renderer, side, hand, from);
        }
    }

    // A hum from every caster winding up, louder the further along.
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.isPaused() || CHARGED.isEmpty()) {
            return;
        }
        for (Int2LongMap.Entry entry : CHARGED.int2LongEntrySet()) {
            Entity caster = level.getEntity(entry.getIntKey());
            if (caster == null) {
                continue;
            }
            float charge = charge(caster, 0.0F);
            Vec3 chest = caster.getEyePosition().add(0.0, -0.3 + 0.1 * charge, 0.0);
            if ((level.getGameTime() + entry.getIntKey()) % 5 == 0) {
                level.playLocalSound(chest.x, chest.y, chest.z, SoundEvents.COPPER_BULB_TURN_ON, SoundSource.PLAYERS,
                        0.25F + 0.35F * charge, 1.5F + 0.5F * charge, false);
            }
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        STARTED.clear();
        CHARGED.clear();
    }
}
