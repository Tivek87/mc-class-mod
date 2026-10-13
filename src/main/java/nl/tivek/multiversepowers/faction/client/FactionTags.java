package nl.tivek.multiversepowers.faction.client;

import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.config.client.ClientSettings;
import nl.tivek.multiversepowers.faction.MobFactions;
import nl.tivek.multiversepowers.faction.rules.Faction;
import nl.tivek.multiversepowers.faction.rules.FactionRules;
import org.joml.Matrix4f;

// The faction of every creature and player near, in small see-through letters of its colour over their head: above
// the name where a name shows, else where the name would be.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class FactionTags {
    private static final double RANGE = 24.0;
    private static final float SIZE = 0.016F;
    private static final int ALPHA = 0x99;

    private FactionTags() {
    }

    @SubscribeEvent
    public static void onRender(RenderLivingEvent.Post<?, ?> event) {
        Minecraft minecraft = Minecraft.getInstance();
        LivingEntity entity = event.getEntity();
        if (!ClientSettings.factionTags() || !Minecraft.renderNames() || entity instanceof ArmorStand
                || entity.isInvisibleTo(minecraft.player) || entity.isDeadOrDying()
                || entity == minecraft.getCameraEntity() && minecraft.options.getCameraType().isFirstPerson()
                || minecraft.getEntityRenderDispatcher().distanceToSqr(entity) > RANGE * RANGE) {
            return;
        }
        Faction faction = of(entity);
        if (faction == null) {
            return;
        }
        Font font = minecraft.font;
        Component text = Component.literal(faction.name());
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(0.0, entity.getBbHeight() + (named(minecraft, entity) ? 0.85 : 0.5), 0.0);
        pose.mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());
        pose.scale(SIZE, -SIZE, SIZE);
        Matrix4f matrix = pose.last().pose();
        font.drawInBatch(text, -font.width(text) / 2.0F, 0.0F, ALPHA << 24 | faction.color(), false, matrix,
                event.getMultiBufferSource(), Font.DisplayMode.NORMAL, 0, event.getPackedLight());
        pose.popPose();
    }

    // A player's faction is their team's; a creature's its own or its kind's, none for a pet or a summon.
    @Nullable
    public static Faction of(LivingEntity entity) {
        if (entity instanceof Player player) {
            PlayerTeam team = player.getTeam();
            return team == null ? null : FactionRules.get(team.getName());
        }
        return entity instanceof Mob mob && MobFactions.free(mob) ? MobFactions.of(mob) : null;
    }

    // Whether the game draws a name over it this frame, so the faction goes above it.
    private static boolean named(Minecraft minecraft, LivingEntity entity) {
        if (entity instanceof Player) {
            return entity != minecraft.getCameraEntity() && !entity.isDiscrete();
        }
        return entity.shouldShowName() || entity.hasCustomName() && entity == minecraft.crosshairPickEntity;
    }
}
