package nl.tivek.multiversepowers.stage.client;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.stage.LampEditPayload;
import nl.tivek.multiversepowers.stage.LampSettings;
import nl.tivek.multiversepowers.stage.StageLightBlockEntity;

// Picking what a stage light shines on: with its settings closed, a right click on a block aims it at that spot, on a
// creature makes it follow that creature; a left click gives up. Its settings open again either way.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class LampPicker {
    private static final double REACH = 96.0;
    @Nullable
    private static BlockPos picking;

    private LampPicker() {
    }

    static void start(BlockPos lamp) {
        picking = lamp;
        Minecraft.getInstance().setScreen(null);
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (picking == null) {
            return;
        }
        if (minecraft.player == null || minecraft.level == null
                || !(minecraft.level.getBlockEntity(picking) instanceof StageLightBlockEntity)) {
            picking = null;
            return;
        }
        if (minecraft.screen == null) {
            minecraft.player.displayClientMessage(Component.translatable("stage.welcomescreen.pick.hint"), true);
        }
    }

    @SubscribeEvent
    public static void onClick(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        BlockPos lamp = picking;
        if (lamp == null || minecraft.screen != null || minecraft.player == null) {
            return;
        }
        event.setCanceled(true);
        event.setSwingHand(false);
        picking = null;
        if (event.isUseItem()) {
            HitResult hit = minecraft.player.pick(REACH, 1.0F, false);
            Entity looked = looked(minecraft.player, hit);
            LampSettings change = null;
            if (minecraft.level.getBlockEntity(lamp) instanceof StageLightBlockEntity found) {
                change = found.settings().copy();
            }
            if (change != null) {
                if (looked != null) {
                    change.aim = LampSettings.Aim.FOLLOW;
                    change.follow = looked.getUUID();
                } else if (hit.getType() == HitResult.Type.BLOCK) {
                    change.aim = LampSettings.Aim.SPOT;
                    change.spot = ((BlockHitResult) hit).getLocation();
                }
                PacketDistributor.sendToServer(new LampEditPayload(lamp, change.write(), false));
            }
        }
        LampScreen.open(lamp);
    }

    // The creature under the crosshair, as far as a beam reaches.
    @Nullable
    private static Entity looked(LocalPlayer player, HitResult blocks) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(REACH));
        double most = blocks.getType() == HitResult.Type.MISS ? REACH * REACH : blocks.getLocation().distanceToSqr(eye);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end,
                player.getBoundingBox().expandTowards(look.scale(REACH)).inflate(1.0),
                entity -> !entity.isSpectator() && entity.isPickable(), most);
        return hit == null ? null : hit.getEntity();
    }
}
