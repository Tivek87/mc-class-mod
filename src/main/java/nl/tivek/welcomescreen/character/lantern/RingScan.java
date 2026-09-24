package nl.tivek.welcomescreen.character.lantern;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.welcomescreen.WelcomeScreenMod;
import nl.tivek.welcomescreen.character.CharacterAbility;
import nl.tivek.welcomescreen.network.ConstructPayload;
import nl.tivek.welcomescreen.spell.SpellCasting;
import nl.tivek.welcomescreen.spell.SpellEffect;

/**
 * The ring scans the area around Green Lantern: a wave of its light rolls out from him through everything, walls and
 * all, as far as the setting {@code rangeBlocks}; every creature it passes is marked for him for {@code markSeconds}:
 * it glows through walls, red when it is out to hurt him and green otherwise, in a frame of light with its name and
 * health (see the client's RingSight). His action bar says what it found. Everyone around sees the wave go by, not what
 * it marked.
 */
public final class RingScan implements SpellEffect {
    /** How fast the wave rolls out, in blocks per tick. */
    public static final double SPEED = 1.6;
    /** How long the wave takes to die away once it reached its end, in ticks. */
    static final int FADE = 10;
    private static final double VIEW_RANGE = 96.0;

    private final int id = PowerRing.newId();
    private final ServerPlayer owner;
    private final Vec3 center;
    private final Vec3 facing;
    private final float radius;
    private final float marks;
    private int age;

    private RingScan(ServerPlayer owner, CharacterAbility ability) {
        this.owner = owner;
        this.center = owner.position().add(0.0, owner.getBbHeight() * 0.5, 0.0);
        this.facing = owner.getLookAngle();
        this.radius = (float) ability.value("rangeBlocks");
        this.marks = (float) ability.value("markSeconds");
    }

    /**
     * The scan key: the wave rolls out, as long as the ring is not busy at the lantern and can pay for it.
     *
     * @return true when it went out
     */
    static boolean use(ServerPlayer owner, ServerLevel level, CharacterAbility ability) {
        if (Lantern.busy(owner)) {
            PowerRing.tell(owner, "busy_lantern");
            return false;
        }
        float cost = (float) ability.value("powerCost");
        float power = PowerRing.power(owner);
        if (power + 1.0E-4F < cost) {
            PowerRing.tell(owner, "no_power");
            return false;
        }
        PowerRing.setPower(owner, power - cost);
        RingScan scan = new RingScan(owner, ability);
        SpellCasting.start(level, scan);
        scan.sound(level, SoundEvents.CONDUIT_ACTIVATE, 1.2F, 1.6F);
        scan.sound(level, SoundEvents.BEACON_POWER_SELECT, 0.9F, 1.9F);
        scan.sound(level, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 1.4F);
        scan.report(level);
        scan.send(level);
        return true;
    }

    /** What the scan will find, on his action bar: how many creatures out to hurt him, and how many others. */
    private void report(ServerLevel level) {
        int hostile = 0;
        int other = 0;
        AABB area = new AABB(this.center, this.center).inflate(this.radius);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != this.owner && entity.isAlive() && !entity.isSpectator()
                        && !(entity instanceof ArmorStand)
                        && entity.distanceToSqr(this.center) <= (double) this.radius * this.radius)) {
            if (living instanceof Enemy || living instanceof Mob mob && mob.getTarget() == this.owner) {
                hostile++;
            } else {
                other++;
            }
        }
        this.owner.displayClientMessage(Component.translatable("ring." + WelcomeScreenMod.MODID + ".scan", hostile,
                other), true);
    }

    @Override
    public boolean tick(ServerLevel level, int tick) {
        this.age++;
        if (this.age > this.radius / SPEED + FADE || this.owner.level() != level) {
            PacketDistributor.sendToPlayersInDimension(level, ConstructPayload.remove(this.id));
            return false;
        }
        this.send(level);
        return true;
    }

    private void send(ServerLevel level) {
        PacketDistributor.sendToPlayersNear(level, null, this.center.x, this.center.y, this.center.z,
                VIEW_RANGE, new ConstructPayload(this.id, this.owner.getId(), this.center, this.facing, this.radius,
                        1.0F, this.marks, false, ConstructPayload.SCAN, 0, this.age, null));
    }

    private void sound(ServerLevel level, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, this.center.x, this.center.y, this.center.z, sound, SoundSource.PLAYERS, volume, pitch);
    }
}
