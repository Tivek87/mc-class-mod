package nl.tivek.multiversepowers.testfight.client;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import javax.annotation.Nullable;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
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
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.client.PowerInputs;
import nl.tivek.multiversepowers.engine.client.fx.CameraShake;
import nl.tivek.multiversepowers.engine.client.fx.Cinematic;
import nl.tivek.multiversepowers.engine.client.gui.GuiShapes;
import nl.tivek.multiversepowers.engine.client.pose.Stance;
import nl.tivek.multiversepowers.engine.client.ragdoll.Downed;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.testfight.TestFight;
import nl.tivek.multiversepowers.testfight.TestFightPayload;
import nl.tivek.multiversepowers.testfight.TestFightRequest;

// The test fight in this game: the gesture that asks for one (crouched, the scroll wheel held 3 seconds on a creature
// built as a person, by a host or an operator, a ring round the crosshair filling meanwhile), every fight near played
// on the clock its news started (FightPoses), and your own filmed from the side while your keys rest.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class FightClient {
    private static final String LAYER_ID = "test_fight";
    // Ticks the wheel is held to start one, and how long before the ring shows (a plain click shows nothing).
    private static final int HOLD = 60;
    private static final int SHOWN = 5;
    private static final int GRACE = 6;
    // A fight whose last news was lost is forgotten this long after it should have ended.
    private static final int SPARE = 40;
    private static final int GOLD = 0xFFD86B;
    private static final int WHITE = 0xF4F4F4;
    private static final int DARK = 0x101418;
    private static final Int2ObjectOpenHashMap<Fight> FIGHTS = new Int2ObjectOpenHashMap<>();
    private static int ticks;
    private static int held;
    private static int lost;
    private static boolean armed;
    @Nullable
    private static LivingEntity aimed;

    private static final class Fight {
        final int fighter;
        final int target;
        final int start;
        boolean letGo;
        // The way from the player to the creature as it began, for the camera to stay on one side.
        @Nullable
        Vec3 way;

        Fight(int fighter, int target, int start) {
            this.fighter = fighter;
            this.target = target;
            this.start = start;
        }

        float age(float partialTick) {
            return ticks - this.start + partialTick;
        }
    }

    private FightClient() {
    }

    public static void onRegisterLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR,
                ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, LAYER_ID), FightClient::render);
    }

    public static void told(int fighter, int target, int phase) {
        if (phase == TestFightPayload.START) {
            FIGHTS.put(fighter, new Fight(fighter, target, ticks));
            return;
        }
        Fight fight = fighter >= 0 ? FIGHTS.get(fighter) : byTarget(target);
        if (fight == null) {
            return;
        }
        if (phase == TestFightPayload.LET_GO) {
            fight.letGo = true;
        } else {
            FIGHTS.remove(fight.fighter);
        }
    }

    @Nullable
    private static Fight byTarget(int target) {
        for (Fight fight : FIGHTS.values()) {
            if (fight.target == target) {
                return fight;
            }
        }
        return null;
    }

    // Your own player in a test fight now: its keys rest.
    public static boolean busy() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && FIGHTS.containsKey(player.getId());
    }

    // Crouched over a creature built as a person, where you may start one: the scroll wheel's hold is the fight's.
    public static boolean armed() {
        return armed;
    }

    // A creature held for a fight's blows is posed here, not limp.
    public static boolean claims(Entity entity) {
        Fight fight = byTarget(entity.getId());
        return fight != null && !fight.letGo;
    }

    // The pose layer: both bodies of every fight near.
    public static boolean pose(EntityModel<?> model, LivingEntity entity, float partialTick) {
        if (FIGHTS.isEmpty() || !(model instanceof HumanoidModel<?> person)) {
            return false;
        }
        Fight fight = FIGHTS.get(entity.getId());
        boolean fighter = fight != null;
        if (fight == null) {
            fight = byTarget(entity.getId());
            if (fight == null || fight.letGo) {
                return false;
            }
        }
        float age = fight.age(partialTick);
        Entity other = entity.level().getEntity(fighter ? fight.target : fight.fighter);
        if (age > TestFight.LENGTH || !(other instanceof LivingEntity partner)) {
            return false;
        }
        return FightPoses.pose(person, entity, partner, fighter, age, partialTick);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            FIGHTS.clear();
            held = 0;
            armed = false;
            return;
        }
        if (minecraft.isPaused()) {
            return;
        }
        ticks++;
        FIGHTS.values().removeIf(fight -> fight.age(0.0F) > TestFight.LENGTH + SPARE);
        for (Fight fight : FIGHTS.values()) {
            face(minecraft, fight, 1.0F);
            if (fight.fighter == player.getId()) {
                jolt(ticks - fight.start);
            }
        }
        LivingEntity now = busy() || !allowed(minecraft, player) ? null : aimed(minecraft, player);
        // A blow taken or the aim slipping off for a moment does not start the count again.
        if (now != null) {
            aimed = now;
            lost = 0;
        } else if (aimed != null && ++lost > GRACE) {
            aimed = null;
        }
        armed = aimed != null;
        if (aimed != null && !busy() && PowerInputs.SCROLL_HOLD.isDown()) {
            held++;
            if (held == HOLD) {
                PacketDistributor.sendToServer(new TestFightRequest(aimed.getId()));
            }
        } else {
            held = 0;
        }
    }

    // The two face each other as they are drawn: your own player turned for the server to know it too.
    @SubscribeEvent
    public static void onRenderFrame(RenderFrameEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || FIGHTS.isEmpty()) {
            return;
        }
        for (Fight fight : FIGHTS.values()) {
            face(minecraft, fight, event.getPartialTick().getGameTimeDeltaPartialTick(false));
        }
    }

    private static void face(Minecraft minecraft, Fight fight, float partialTick) {
        Entity fighter = minecraft.level.getEntity(fight.fighter);
        Entity target = minecraft.level.getEntity(fight.target);
        if (!(fighter instanceof LivingEntity one) || !(target instanceof LivingEntity two)) {
            return;
        }
        Vec3 from = one.getPosition(partialTick);
        Vec3 to = two.getPosition(partialTick);
        if (fight.way == null) {
            Vec3 flat = new Vec3(to.x - from.x, 0.0, to.z - from.z);
            fight.way = flat.lengthSqr() < 1.0E-6 ? null : flat.normalize();
        }
        float yaw = (float) Math.toDegrees(Math.atan2(-(to.x - from.x), to.z - from.z));
        turn(one, yaw);
        if (!fight.letGo) {
            turn(two, yaw + 180.0F);
        }
        if (one == minecraft.player) {
            one.setXRot(0.0F);
            one.xRotO = 0.0F;
        }
    }

    private static void turn(LivingEntity entity, float yaw) {
        entity.setYRot(yaw);
        entity.yRotO = yaw;
        entity.setYHeadRot(yaw);
        entity.yHeadRotO = yaw;
        entity.setYBodyRot(yaw);
        entity.yBodyRotO = yaw;
    }

    // Each blow of your own lands with a jolt of the camera.
    private static void jolt(int age) {
        switch (age) {
            case TestFight.BODY -> CameraShake.add(1.2F, 5);
            case TestFight.UPPERCUT -> CameraShake.add(2.4F, 7);
            case TestFight.CROSS -> CameraShake.add(1.8F, 6);
            case TestFight.KICK -> CameraShake.add(3.5F, 9);
            default -> {
            }
        }
    }

    private static boolean allowed(Minecraft minecraft, LocalPlayer player) {
        return (minecraft.hasSingleplayerServer() || player.hasPermissions(2)) && minecraft.screen == null
                && !Downed.now() && player.isShiftKeyDown() && !player.isPassenger() && !player.isSpectator();
    }

    // The creature built as a person under the crosshair, near enough and not behind a wall; null when none.
    @Nullable
    private static LivingEntity aimed(Minecraft minecraft, LocalPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(TestFight.REACH));
        HitResult wall = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        double far = wall.getType() == HitResult.Type.MISS ? TestFight.REACH : wall.getLocation().distanceTo(eye);
        Vec3 reach = eye.add(look.scale(far));
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, reach,
                player.getBoundingBox().expandTowards(look.scale(far)).inflate(1.0),
                entity -> entity instanceof Mob mob && mob.isAlive() && !mob.isBaby(), far * far);
        if (hit == null || !(hit.getEntity() instanceof Mob mob) || !person(minecraft, mob)) {
            return null;
        }
        return mob;
    }

    private static boolean person(Minecraft minecraft, LivingEntity entity) {
        EntityRenderer<?> renderer = minecraft.getEntityRenderDispatcher().getRenderer(entity);
        return renderer instanceof LivingEntityRenderer<?, ?> living
                && living.getModel() instanceof HumanoidModel<?> model && Stance.person(model);
    }

    // Your own fight filmed from your right side, a little behind, closing in as the blows land and drawing back for
    // the kick to follow the creature thrown.
    @Nullable
    public static Cinematic.Shot shot(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        Fight fight = player == null ? null : FIGHTS.get(player.getId());
        if (fight == null || fight.way == null) {
            return null;
        }
        float age = fight.age(partialTick);
        if (age > TestFight.LENGTH) {
            return null;
        }
        Vec3 me = player.getPosition(partialTick);
        Vec3 stood = me.add(fight.way.scale(TestFight.GAP));
        Entity target = minecraft.level.getEntity(fight.target);
        Vec3 it = target == null ? stood : target.getPosition(partialTick);
        // Past the kick the middle leans after the creature thrown only so far, so the player stays in the shot.
        Vec3 after = it.subtract(stood).scale(0.35);
        if (after.length() > 1.6) {
            after = after.normalize().scale(1.6);
        }
        Vec3 mid = me.add(stood).scale(0.5).add(after);
        float in = (float) (smooth(age, 8.0F, 30.0F) * (1.0 - smooth(age, 76.0F, 92.0F)));
        float out = (float) (smooth(age, 90.0F, 110.0F) * (1.0 - smooth(age, 125.0F, 148.0F)));
        double distance = 3.9 - 0.6 * in + 1.4 * out;
        double angle = Math.toRadians(80.0 - 15.0 * smooth(age, 0.0F, TestFight.LENGTH));
        Vec3 right = new Vec3(-fight.way.z, 0.0, fight.way.x);
        Vec3 side = right.scale(Math.sin(angle)).add(fight.way.scale(-Math.cos(angle)));
        Vec3 eye = mid.add(side.scale(distance)).add(0.0, 1.35 + 0.3 * out, 0.0);
        return Cinematic.Shot.looking(Cinematic.clear(mid.add(0.0, 1.1, 0.0), eye), mid.add(0.0, 1.05, 0.0), 0.0F,
                70.0);
    }

    private static double smooth(float age, float from, float to) {
        return Ease.smooth((age - from) / (to - from));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onInput(MovementInputUpdateEvent event) {
        if (!busy()) {
            return;
        }
        Input input = event.getInput();
        input.forwardImpulse = 0.0F;
        input.leftImpulse = 0.0F;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
        input.shiftKeyDown = false;
    }

    // In a fight no click does anything; crouched over a creature the wheel no longer picks it as a block would.
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (busy() || armed && event.isPickBlock()) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (held < SHOWN || aimed == null || busy() || minecraft.options.hideGui || minecraft.screen != null) {
            return;
        }
        float x = graphics.guiWidth() / 2.0F;
        float y = graphics.guiHeight() / 2.0F;
        float filled = Mth.clamp((held + deltaTracker.getGameTimeDeltaPartialTick(false)) / HOLD, 0.0F, 1.0F);
        float appear = Mth.clamp((held - SHOWN) / 4.0F, 0.0F, 1.0F);
        GuiShapes.arc(graphics, x, y, 9.0F, 13.0F, 0.0F, 360.0F, GuiShapes.fade(DARK, 0.55F * appear));
        GuiShapes.arc(graphics, x, y, 10.0F, 12.0F, 0.0F, 360.0F * filled,
                GuiShapes.fade(GuiShapes.mix(WHITE, GOLD, filled), 0.95F * appear));
        GuiShapes.flush(graphics);
        // Text all but see-through is drawn whole by the font, so it waits until the ring is half in.
        if (appear >= 0.5F) {
            Component text = Component.translatable("testfight." + MultiversePowers.MODID + ".hold",
                    aimed.getName());
            graphics.drawCenteredString(minecraft.font, text, (int) x, (int) (y + 18.0F),
                    GuiShapes.fade(WHITE, appear));
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        FIGHTS.clear();
        held = 0;
        armed = false;
        aimed = null;
    }
}
