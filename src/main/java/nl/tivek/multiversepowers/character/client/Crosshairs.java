package nl.tivek.multiversepowers.character.client;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.engine.client.gui.GuiShapes;
import nl.tivek.multiversepowers.faction.Standing;
import nl.tivek.multiversepowers.faction.client.ClientStandings;
import nl.tivek.multiversepowers.killconfirm.client.KillMarker;

// A character's own crosshair in place of the game's: each draws its own round the middle of the screen, in its own
// colours, moving with what happens (Feel): it blooms out as you strike, run or leave the ground and springs back,
// closes in on a creature under it in that creature's colour (red, yellow, green), flashes as a blow of yours lands,
// and the kill's red cross flicks out round its own edge. The game's comes back with the debug screen, outside first
// person and without a character.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class Crosshairs {
    // Draws a crosshair round (cx, cy) and says how far out it reaches (gui pixels), for the kill's cross to sit round.
    @FunctionalInterface
    public interface Drawer {
        float draw(GuiGraphics graphics, LocalPlayer player, float cx, float cy, Feel feel);
    }

    // `spread` 0 at rest to about 1 blooming out, `aim` 0 to 1 on a creature of colour `target`, `hit` and `kill`
    // 0 to 1 as a blow landed or a kill was made just now.
    public record Feel(float spread, float aim, int target, float hit, float kill, float partialTick) {
    }

    private static final Map<GameCharacter, Drawer> DRAWERS = new EnumMap<>(GameCharacter.class);
    // How far a click blooms it out, how fast that springs back (kept a tick), and how fast it follows.
    private static final float KICK = 0.55F;
    private static final float KICK_KEPT = 0.78F;
    private static final float FOLLOW = 0.45F;
    private static float spread;
    private static float spreadBefore;
    private static float aim;
    private static float aimBefore;
    private static float kick;
    private static float hitSeen;
    private static int target = Standing.NEUTRAL.rgb();
    private static boolean attackWas;
    private static boolean useWas;

    private Crosshairs() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            spread = spreadBefore = aim = aimBefore = kick = 0.0F;
            return;
        }
        spreadBefore = spread;
        aimBefore = aim;
        boolean attack = minecraft.options.keyAttack.isDown();
        boolean use = minecraft.options.keyUse.isDown();
        if (attack && !attackWas || use && !useWas) {
            kick = Math.min(1.0F, kick + KICK);
        }
        attackWas = attack;
        useWas = use;
        float hit = KillMarker.hitFlash();
        if (hit > hitSeen + 0.5F) {
            kick = Math.min(1.0F, kick + 0.25F);
        }
        hitSeen = hit;
        kick *= KICK_KEPT;
        Vec3 motion = player.getDeltaMovement();
        float moving = Mth.clamp((float) Math.sqrt(motion.x * motion.x + motion.z * motion.z) / 0.28F, 0.0F, 1.0F);
        float goal = 0.3F * moving * (player.isSprinting() ? 1.5F : 1.0F) + (player.onGround() ? 0.0F : 0.3F) + kick;
        spread += (Math.min(1.4F, goal) - spread) * FOLLOW;
        Entity picked = minecraft.crosshairPickEntity;
        boolean on = picked instanceof LivingEntity living && living.isAlive() && !living.isInvisibleTo(player);
        if (on) {
            target = ClientStandings.of(picked).rgb();
        }
        aim += ((on ? 1.0F : 0.0F) - aim) * (on ? 0.5F : 0.25F);
    }

    public static void add(GameCharacter character, Drawer drawer) {
        DRAWERS.put(character, drawer);
    }

    @SubscribeEvent
    public static void onGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (!event.getName().equals(VanillaGuiLayers.CROSSHAIR) || event.isCanceled()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        GameCharacter character = ClientCharacter.active();
        Drawer drawer = character == null ? null : DRAWERS.get(character);
        if (drawer == null || player == null || player.isSpectator() || minecraft.options.hideGui
                || !minecraft.options.getCameraType().isFirstPerson()
                || minecraft.getDebugOverlay().showDebugScreen()) {
            return;
        }
        event.setCanceled(true);
        GuiGraphics graphics = event.getGuiGraphics();
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Feel feel = new Feel(Mth.lerp(partialTick, spreadBefore, spread), Mth.lerp(partialTick, aimBefore, aim),
                target, KillMarker.hitFlash(), KillMarker.killed(), partialTick);
        float cx = graphics.guiWidth() / 2.0F;
        float cy = graphics.guiHeight() / 2.0F;
        float reach = drawer.draw(graphics, player, cx, cy, feel);
        KillMarker.claim();
        KillMarker.cross(graphics, cx, cy, reach + 1.0F, reach + 4.0F);
        GuiShapes.flush(graphics);
    }

    // The colour a crosshair part takes: its own, toward the creature's under it as it closes in, white as a blow
    // lands and red as a kill is made.
    public static int tint(int own, Feel feel, float aimed) {
        int color = GuiShapes.mix(own, feel.target(), feel.aim() * aimed);
        color = GuiShapes.mix(color, 0xFFFFFF, feel.hit() * 0.8F);
        return GuiShapes.mix(color, KillMarker.RED, feel.kill() * 0.85F);
    }

    // A stroke with a dark edge under it, so it reads on snow and sky alike.
    public static void stroke(GuiGraphics graphics, float x0, float y0, float x1, float y1, float width, int rgb,
            float alpha) {
        GuiShapes.stroke(graphics, x0, y0, x1, y1, width + 1.0F, GuiShapes.fade(0x000000, 0.4F * alpha));
        GuiShapes.stroke(graphics, x0, y0, x1, y1, width, GuiShapes.fade(rgb, alpha));
    }

    public static void ring(GuiGraphics graphics, float cx, float cy, float radius, float width, int rgb,
            float alpha) {
        GuiShapes.ring(graphics, cx, cy, radius, width + 1.0F, GuiShapes.fade(0x000000, 0.4F * alpha));
        GuiShapes.ring(graphics, cx, cy, radius, width, GuiShapes.fade(rgb, alpha));
    }

    public static void dot(GuiGraphics graphics, float cx, float cy, float radius, int rgb, float alpha) {
        GuiShapes.disc(graphics, cx, cy, radius + 0.5F, GuiShapes.fade(0x000000, 0.4F * alpha));
        GuiShapes.disc(graphics, cx, cy, radius, GuiShapes.fade(rgb, alpha));
    }
}
