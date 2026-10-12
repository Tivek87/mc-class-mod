package nl.tivek.multiversepowers.space.client;

import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionTransitionScreenEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.config.PowerRules;
import nl.tivek.multiversepowers.engine.client.world.LocalSky;
import nl.tivek.multiversepowers.engine.client.world.WorldTurn;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.space.Space;

// The way to the moon as the flyer's own game shows it: the sky drains to black with height, the land below is left
// undrawn once it is out of sight, and towards half-way the view rolls over, a quarter turn on each side of it, so the
// world the flyer comes out in (turned over to the one they left, Space) is seen exactly as the last one was. That
// change of worlds has no loading screen, and whatever flies them goes on as it went. Flying sideways high up takes
// the eye round the world below (Space.orbit), the whole sky turning with it.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class SpaceView {
    // Blocks before half-way over which the view rolls its quarter turn.
    private static final double ROLL_OVER = 800.0;
    private static final long HALF_DAY = 12000L;
    // The height above the ground at which the land below is gone into its haze, at most; the shares of it at which
    // that begins and from which the land is seen as from above; the far picture is clear a little higher still.
    private static final double EARTH_GONE = 950.0;
    private static final double MOON_GONE = 460.0;
    private static final double FADES = 0.5;
    private static final double ABOVE = 0.3;
    private static final double CLEARS = 220.0;
    // How many times deeper than its view distance the game draws land below the eye: read off the render thread.
    private static final int DEEPER = 4;
    private static volatile boolean deep;
    // Where the player rose into orbit (Space.orbit), in which world.
    private static ResourceKey<Level> roseIn;
    private static double roseX;
    private static double roseZ;

    private SpaceView() {
    }

    // No screen at all: the game is never shown waiting for the other world.
    private static final class Unseen extends ReceivingLevelScreen {
        Unseen(BooleanSupplier received, Reason reason) {
            super(received, reason);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        }
    }

    public static void onTransitionScreens(RegisterDimensionTransitionScreenEvent event) {
        event.registerConditionalEffect(Space.MOON, Level.OVERWORLD, Unseen::new);
        event.registerConditionalEffect(Level.OVERWORLD, Space.MOON, Unseen::new);
    }

    public static void onEffects(RegisterDimensionSpecialEffectsEvent event) {
        // No clouds, no red at sunrise; the far ground fades to its own dust, darker by night.
        event.register(ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "moon"),
                new DimensionSpecialEffects(Float.NaN, true, DimensionSpecialEffects.SkyType.NORMAL, false, false) {
                    @Override
                    public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) {
                        return color.scale(brightness * 0.88 + 0.12);
                    }

                    @Override
                    public boolean isFoggyAt(int x, int y) {
                        return false;
                    }

                    @Override
                    public float[] getSunriseColor(float timeOfDay, float partialTick) {
                        return null;
                    }
                });
    }

    @SubscribeEvent
    public static void onOpening(ScreenEvent.Opening event) {
        if (event.getNewScreen() instanceof Unseen) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        deep = level != null && Space.reaches(level)
                && height(level) > minecraft.gameRenderer.getRenderDistance() * 0.5;
        if (level != null && Space.reaches(level)) {
            LocalSky.thin(Space.thin(level, height(level)));
            Entity player = minecraft.player;
            if (player != null && (roseIn != level.dimension()
                    || !Space.orbits(level, Space.height(level, player.getY())))) {
                roseIn = level.dimension();
                roseX = player.getX();
                roseZ = player.getZ();
            } else if (Space.climbs(player.getX() - player.xo, player.getY() - player.yo, player.getZ() - player.zo)) {
                roseX += player.getX() - player.xo;
                roseZ += player.getZ() - player.zo;
            }
        }
    }

    // How far round the world below the eye has gone (Space.orbit), as a turn of the eye about that world's middle,
    // in the world's own axes: about its axis by its length in radians.
    public static Vec3 orbit(Level level) {
        if (level != Minecraft.getInstance().level || roseIn != level.dimension() || !PowerRules.spaceTravel()) {
            return Vec3.ZERO;
        }
        Vec3 eye = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        double dx = eye.x - roseX;
        double dz = eye.z - roseZ;
        double angle = Space.orbit(level, Space.height(level, eye.y), dx, dz);
        double away = Math.hypot(dx, dz);
        return angle < 1.0E-6 || away < 1.0E-6 ? Vec3.ZERO : new Vec3(dz, 0.0, -dx).scale(angle / away);
    }

    // The far land fades to black as the sky does; the moon's own dust keeps its colour.
    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !Space.reaches(level) || Space.onMoon(level)) {
            return;
        }
        float left = 1.0F - (float) Space.thin(level, height(level));
        event.setRed(event.getRed() * left);
        event.setGreen(event.getGreen() * left);
        event.setBlue(event.getBlue() * left);
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        float roll = (float) Math.toDegrees(roll());
        if (roll != 0.0F) {
            event.setRoll(event.getRoll() + roll);
        }
    }

    // Whatever flew the player into the other world flies on, its way turned as that world lies.
    @SubscribeEvent
    public static void onClone(ClientPlayerNetworkEvent.Clone event) {
        Level from = event.getOldPlayer().level();
        Level to = event.getNewPlayer().level();
        if (from.dimension() != to.dimension() && Space.reaches(from) && Space.reaches(to)) {
            WorldTurn.turn(Space::turned);
            event.getNewPlayer().setDeltaMovement(Space.turned(event.getOldPlayer().getDeltaMovement()));
        }
    }

    private static double height(ClientLevel level) {
        return Space.height(level, Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().y);
    }

    // How far the view has rolled over, in radians: up to a quarter turn at half-way, the other way on the moon's
    // side of it, so the two meet as one half turn.
    private static double roll() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !Space.reaches(level) || !PowerRules.spaceTravel()) {
            return 0.0;
        }
        // Off the line between the two worlds no crossing comes, so the view does not roll either.
        double share = Ease.smooth((height(level) - (Space.HALF_WAY - ROLL_OVER)) / ROLL_OVER)
                * (1.0 - Ease.smooth(orbit(level).length() / Space.CROSS));
        return (Space.onMoon(level) ? -0.5 : 0.5) * Math.PI * share;
    }

    // The roll of the view `entity` looks through: the mouse must turn them as the screen lies.
    public static double roll(Entity entity) {
        return entity == Minecraft.getInstance().player ? roll() : 0.0;
    }

    // How many ticks this world's sky runs ahead of its clock: the moon's day is the night below it.
    public static long clock(Level level) {
        return Space.onMoon(level) ? HALF_DAY : 0L;
    }

    // From above, the land in sight is a patch far below: it fades to its haze round its edge, not all at once by
    // how high the eye is, and then wholly as the eye climbs on, while the far picture of it comes out of that haze.
    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !Space.reaches(level) || event.getMode() != FogRenderer.FogMode.FOG_TERRAIN
                || event.getType() != FogType.NONE || event.getCamera().getEntity() instanceof LivingEntity living
                        && (living.hasEffect(MobEffects.BLINDNESS) || living.hasEffect(MobEffects.DARKNESS))) {
            return;
        }
        double height = height(level);
        double from = fades(level) * ABOVE;
        if (height <= from) {
            return;
        }
        double reach = event.getFarPlaneDistance();
        double inner = Mth.lerp(Ease.smooth((height - from) / (fades(level) - from)), event.getNearPlaneDistance(),
                reach * 0.35);
        double fade = Ease.smooth((height - fades(level)) / (gone(level) - fades(level)));
        event.setNearPlaneDistance((float) Mth.lerp(fade, Math.hypot(inner, height), height * 0.4));
        event.setFarPlaneDistance((float) Mth.lerp(fade, Math.hypot(reach, height), height * 0.7));
        event.setFogShape(FogShape.SPHERE);
        event.setCanceled(true);
    }

    // The height from which the land below is all haze: no higher than the game draws land at all, which is as far
    // as four times how far the player has it show.
    public static double gone(Level level) {
        return Math.min(Space.onMoon(level) ? MOON_GONE : EARTH_GONE, Minecraft.getInstance().gameRenderer
                .getRenderDistance() * 3.4);
    }

    private static double fades(Level level) {
        return gone(level) * FADES;
    }

    // How much of the far picture of the land below is still only the colour that land fades to: it comes out of
    // the haze once the land itself is gone into it, or the land would show as a hole in it.
    public static double haze(Level level, double height) {
        return 1.0 - Ease.smooth((height - gone(level)) / CLEARS);
    }

    public static int deeper() {
        return deep ? DEEPER : 1;
    }

    // Whether the land below is past that: it need not be drawn.
    public static boolean groundGone() {
        ClientLevel level = Minecraft.getInstance().level;
        return level != null && Space.reaches(level) && height(level) > gone(level) + 40.0;
    }
}
