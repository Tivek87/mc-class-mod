package nl.tivek.multiversepowers.space;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.config.PowerRules;
import nl.tivek.multiversepowers.engine.world.FlightLanes;

// The server's side of the way to the moon: whoever climbs past half-way is put in the other world as far from its
// ground as they had still to go, turned over (Space), flying on; on the moon everything alive weighs a fifth.
@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class SpaceTravel {
    private static final ResourceLocation LIGHT = ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID,
            "moon_gravity");
    private static final double PULL = -0.8;
    private static final double SAFE_FALL = 4.0;
    // Where each player rose into orbit (Space.orbit), in which world.
    private static final Map<UUID, Rose> ROSE = new HashMap<>();

    private record Rose(ResourceKey<Level> in, double x, double z) {
    }

    private SpaceTravel() {
    }

    public static void clear() {
        ROSE.clear();
    }

    public static void register() {
        FlightLanes.add(SpaceTravel::haste);
    }

    private static double haste(Entity flyer) {
        Level level = flyer.level();
        return PowerRules.spaceTravel() && Space.reaches(level) ? Space.haste(Space.height(level, flyer.getY())) : 1.0;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !PowerRules.spaceTravel()) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (!Space.reaches(level) || !player.isAlive() || player.isPassenger() || player.isVehicle()
                || player.isChangingDimension()) {
            return;
        }
        double height = Space.height(level, player.getY());
        Rose rose = ROSE.get(player.getUUID());
        double dx = player.getX() - player.xo;
        double dz = player.getZ() - player.zo;
        if (rose == null || rose.in() != level.dimension() || !Space.orbits(level, height)) {
            rose = new Rose(level.dimension(), player.getX(), player.getZ());
            ROSE.put(player.getUUID(), rose);
        } else if (Space.climbs(dx, player.getY() - player.yo, dz)) {
            rose = new Rose(rose.in(), rose.x() + dx, rose.z() + dz);
            ROSE.put(player.getUUID(), rose);
        }
        if (height <= Space.HALF_WAY + Space.PAST
                || Space.orbit(level, height, player.getX() - rose.x(), player.getZ() - rose.z()) > Space.CROSS) {
            return;
        }
        ServerLevel to = player.server.getLevel(Space.onMoon(level) ? Level.OVERWORLD : Space.MOON);
        if (to == null) {
            return;
        }
        Vec3 at = new Vec3(player.getX(), Space.ground(to) + Space.DISTANCE - height, -player.getZ());
        player.changeDimension(new DimensionTransition(to, at, Space.turned(player.getDeltaMovement()),
                Space.turnedYaw(player.getYRot()), -player.getXRot(), DimensionTransition.DO_NOTHING));
    }

    @SubscribeEvent
    public static void onJoinLevel(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity living) {
            boolean light = Space.onMoon(event.getLevel());
            weigh(living, Attributes.GRAVITY, light, PULL);
            weigh(living, Attributes.FALL_DAMAGE_MULTIPLIER, light, PULL);
            weigh(living, Attributes.SAFE_FALL_DISTANCE, light, SAFE_FALL);
        }
    }

    private static void weigh(LivingEntity living, Holder<Attribute> attribute, boolean light, double share) {
        AttributeInstance instance = living.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        if (light) {
            instance.addOrUpdateTransientModifier(new AttributeModifier(LIGHT, share,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else {
            instance.removeModifier(LIGHT);
        }
    }
}
