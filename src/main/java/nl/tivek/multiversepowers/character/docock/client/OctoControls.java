package nl.tivek.multiversepowers.character.docock.client;

import static nl.tivek.multiversepowers.character.docock.TentacleReach.COUNT;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.AbilitySlot;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.client.AbilityKeys;
import nl.tivek.multiversepowers.character.client.ClientCharacter;
import nl.tivek.multiversepowers.character.client.PowerInputs;
import nl.tivek.multiversepowers.character.docock.OctoInputPayload;
import nl.tivek.multiversepowers.character.docock.TentacleReach;
import nl.tivek.multiversepowers.engine.client.ragdoll.Downed;

// Doctor Octopus's own controls. Right click on a creature takes hold of it with the next free tentacle (2, 1, 4, 3),
// anywhere else it does what the game does; holding it squeezes all that is held (while holding, right never uses an
// item); a quick turn of the view slams it all that way; his throw key hurls it (at a creature in the crosshair, if
// any); a tap of shift lets go of the last one taken; left click on a creature strikes it, on a block it mines. Shift
// held with right click leaves the click to the game (trading, riding). His selection key opens Selection Mode: the
// hotbar stays put, keys 1 to 4 pick tentacles, 5 all of them, the scroll wheel one after another, and every action
// is only for the picked ones (one holding: a blow with it, two or more: they clash). Squeezing ones drop out of the
// pick, and the mode closes with all four on the ground, on death and in another dimension.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class OctoControls {
    private static final int SQUEEZE_AFTER = 6;
    private static final int TAP_TICKS = 6;
    private static final float SLAM_DEGREES = 28.0F;
    private static final int SLAM_REST = 10;
    private static final double THROW_AIM = 32.0;

    private static boolean selecting;
    private static int picked;
    private static int cursor = -1;
    private static int rightTicks;
    private static boolean squeezeSent;
    private static boolean pressUsed;
    private static int shiftTicks;
    private static boolean shiftUsed;
    private static float lastYaw = Float.NaN;
    private static float lastPitch;
    private static Vec3 lastLook = Vec3.ZERO;
    private static int slamRest;

    private OctoControls() {
    }

    static boolean selecting() {
        return selecting;
    }

    static int picked() {
        return picked;
    }

    private static boolean playing() {
        return ClientCharacter.active() == GameCharacter.DOC_OCK && Minecraft.getInstance().player != null;
    }

    @Nullable
    private static ClientOcto.View view() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? null : ClientOcto.of(player);
    }

    // The tentacles an action is for: the picked ones in Selection Mode, else 0 (the rules pick).
    private static int mask() {
        return selecting ? picked : 0;
    }

    // In Selection Mode with nothing picked no tentacle may act: a mask of 0 would leave the pick to the rules.
    private static boolean noneTell() {
        if (selecting && picked == 0) {
            OctoHud.tell("none_picked");
            return true;
        }
        return false;
    }

    private static void send(int action, int mask, int target, Vec3 way) {
        PacketDistributor.sendToServer(new OctoInputPayload(action, mask, target, (float) way.x, (float) way.y,
                (float) way.z));
    }

    private static boolean anyHeld(ClientOcto.View view, int mask) {
        for (int i = 0; i < COUNT; i++) {
            if (view.holding(i) && (mask == 0 || (mask & 1 << i) != 0)) {
                return true;
            }
        }
        return false;
    }

    private static void close() {
        selecting = false;
        picked = 0;
        cursor = -1;
    }

    // Before the game reads its keys, so the hotbar keys and these keys are taken first.
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientOcto.View view = view();
        if (!playing() || view == null || !player.isAlive()) {
            close();
            endSqueeze();
            return;
        }
        boolean inGame = minecraft.screen == null && !Downed.now();
        while (AbilityKeys.of(AbilitySlot.ABILITY_10).consumeClick()) {
            if (selecting) {
                close();
            } else if (view.stance() == TentacleReach.STANCES - 1) {
                OctoHud.tell("all_down");
            } else {
                selecting = true;
            }
        }
        if (selecting && view.stance() == TentacleReach.STANCES - 1) {
            close();
        }
        if (selecting) {
            pick(minecraft, view);
        }
        while (AbilityKeys.of(AbilitySlot.ABILITY_9).consumeClick()) {
            if (inGame && noneTell()) {
                continue;
            }
            if (inGame && anyHeld(view, mask())) {
                Entity aimed = aimed(player, THROW_AIM);
                send(OctoInputPayload.THROW, mask(), aimed == null ? -1 : aimed.getId(), Vec3.ZERO);
            } else if (inGame) {
                OctoHud.tell("nothing_held");
            }
        }
        squeeze(minecraft, view, inGame);
        shift(inGame);
        slam(player, view, inGame);
    }

    // Selection Mode: number keys and the scroll wheel pick tentacles; squeezing and grounded ones drop out.
    private static void pick(Minecraft minecraft, ClientOcto.View view) {
        for (int k = 0; k < minecraft.options.keyHotbarSlots.length; k++) {
            while (minecraft.options.keyHotbarSlots[k].consumeClick()) {
                if (k < COUNT) {
                    if (view.ground(k)) {
                        OctoHud.tell("on_ground", k + 1);
                    } else {
                        picked ^= 1 << k;
                    }
                } else if (k == COUNT) {
                    int all = 0;
                    for (int i = 0; i < COUNT; i++) {
                        all |= view.ground(i) ? 0 : 1 << i;
                    }
                    picked = picked == all ? 0 : all;
                }
            }
        }
        for (int i = 0; i < COUNT; i++) {
            if (view.ground(i) || view.squeezing(i)) {
                picked &= ~(1 << i);
            }
        }
    }

    private static void squeeze(Minecraft minecraft, ClientOcto.View view, boolean inGame) {
        boolean down = inGame && minecraft.options.keyUse.isDown() && !PowerInputs.HOLD_SHIFT.isDown();
        if (!down) {
            rightTicks = 0;
            pressUsed = false;
            endSqueeze();
            return;
        }
        rightTicks++;
        if (rightTicks >= SQUEEZE_AFTER && !squeezeSent && !(selecting && picked == 0) && anyHeld(view, mask())) {
            send(OctoInputPayload.SQUEEZE, mask(), -1, Vec3.ZERO);
            squeezeSent = true;
        }
    }

    private static void endSqueeze() {
        if (squeezeSent && Minecraft.getInstance().getConnection() != null) {
            send(OctoInputPayload.SQUEEZE_END, 0, -1, Vec3.ZERO);
        }
        squeezeSent = false;
    }

    // A tap of shift, with no right click while it was down, lets go.
    private static void shift(boolean inGame) {
        if (inGame && PowerInputs.HOLD_SHIFT.isDown()) {
            shiftTicks++;
            return;
        }
        if (shiftTicks > 0 && shiftTicks <= TAP_TICKS && !shiftUsed && inGame && !noneTell()) {
            send(OctoInputPayload.RELEASE, mask(), -1, Vec3.ZERO);
        }
        shiftTicks = 0;
        shiftUsed = false;
    }

    // A quick turn of the view while holding slams what is held the way the crosshair went.
    private static void slam(LocalPlayer player, ClientOcto.View view, boolean inGame) {
        Vec3 look = player.getLookAngle();
        float yaw = player.getYRot();
        float pitch = player.getXRot();
        slamRest = Math.max(0, slamRest - 1);
        if (!Float.isNaN(lastYaw) && inGame && slamRest == 0 && !(selecting && picked == 0)
                && anyHeld(view, mask())) {
            float turned = (float) Math.hypot(Mth.wrapDegrees(yaw - lastYaw), pitch - lastPitch);
            Vec3 way = look.subtract(lastLook);
            if (turned > SLAM_DEGREES && way.lengthSqr() > 1.0E-6) {
                send(OctoInputPayload.SLAM, mask(), -1, way.normalize());
                slamRest = SLAM_REST;
            }
        }
        lastYaw = yaw;
        lastPitch = pitch;
        lastLook = look;
    }

    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientOcto.View view = view();
        if (!playing() || view == null || event.isPickBlock()) {
            return;
        }
        double reach = GameCharacter.DOC_OCK.byName("grab").value("reachBlocks");
        if (event.isUseItem()) {
            if (PowerInputs.HOLD_SHIFT.isDown()) {
                shiftUsed = true;
                return;
            }
            Entity aimed = aimed(player, reach);
            boolean fresh = !pressUsed;
            pressUsed = true;
            if (aimed instanceof Mob) {
                if (fresh && !noneTell()) {
                    send(OctoInputPayload.GRAB, mask(), aimed.getId(), Vec3.ZERO);
                }
                event.setSwingHand(false);
                event.setCanceled(true);
            } else if (anyHeld(view, 0)) {
                event.setSwingHand(false);
                event.setCanceled(true);
            }
        } else if (event.isAttack()) {
            Entity aimed = aimed(player, reach);
            if (aimed instanceof LivingEntity) {
                if (!noneTell()) {
                    send(OctoInputPayload.STRIKE, mask(), aimed.getId(), Vec3.ZERO);
                }
                event.setSwingHand(false);
                event.setCanceled(true);
            }
        }
    }

    // In Selection Mode the scroll wheel steps the pick from one tentacle off the ground to the next.
    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        ClientOcto.View view = view();
        if (!selecting || view == null || Minecraft.getInstance().screen != null) {
            return;
        }
        event.setCanceled(true);
        int step = event.getScrollDeltaY() > 0 ? -1 : 1;
        for (int tries = 0; tries < COUNT; tries++) {
            cursor = Math.floorMod(cursor + step, COUNT);
            if (!view.ground(cursor)) {
                picked = 1 << cursor;
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onRespawn(ClientPlayerNetworkEvent.Clone event) {
        close();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        close();
        squeezeSent = false;
        lastYaw = Float.NaN;
    }

    // The creature in the crosshair within `reach`, unless a block stands before it; his own held ones are looked past.
    @Nullable
    private static Entity aimed(LocalPlayer player, double reach) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(reach));
        HitResult block = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        double far = block.getType() == HitResult.Type.MISS ? reach : block.getLocation().distanceTo(eye);
        Vec3 stop = eye.add(player.getLookAngle().scale(far));
        AABB box = player.getBoundingBox().expandTowards(stop.subtract(eye)).inflate(1.0);
        ClientOcto.View view = ClientOcto.of(player);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, stop, box,
                entity -> !entity.isSpectator() && entity.isPickable() && entity != player
                        && (view == null || !view.holds(entity.getId())), far * far);
        return hit == null ? null : hit.getEntity();
    }
}
