package nl.tivek.multiversepowers.character.greenlantern.ability;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.greenlantern.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.engine.effect.Effect;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

public final class FlameBurn implements Effect {
    private static final double VIEW_RANGE = 80.0;
    private static final int SEND_EVERY = 5;
    private static final int HURT_EVERY = 20;

    private static final Map<Integer, FlameBurn> BURNING = new HashMap<>();

    private final int id = PowerRing.newId();
    private final LivingEntity target;
    private int left;
    private int age;

    private FlameBurn(LivingEntity target, int ticks) {
        this.target = target;
        this.left = ticks;
    }

    public static void ignite(ServerLevel level, LivingEntity target) {
        CharacterAbility wheel = GameCharacter.GREEN_LANTERN.byName("construct_wheel");
        int ticks = (int) Math.round(wheel.value("burnSeconds") * 20.0);
        if (ticks <= 0 || !target.isAlive() || target.isInWaterOrBubble()) {
            return;
        }
        FlameBurn burning = BURNING.get(target.getId());
        if (burning != null && burning.target == target) {
            burning.left = Math.max(burning.left, ticks);
            return;
        }
        FlameBurn burn = new FlameBurn(target, ticks);
        BURNING.put(target.getId(), burn);
        Effects.start(level, burn);
    }

    public static void clear() {
        BURNING.clear();
    }

    @Override
    public boolean tick(ServerLevel level, int tick) {
        LivingEntity target = this.target;
        if (BURNING.get(target.getId()) != this) {
            return false;
        }
        if (target.isRemoved() || !target.isAlive() || target.level() != level || this.left <= 0) {
            this.end(level);
            return false;
        }
        if (target.isInWaterOrBubble()) {
            level.playSound(null, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.6F, 1.4F);
            ParticleFx.cloud(level, ParticleFx.dust(0xD8FFE2, 1.2F), target.getBoundingBox().getCenter(), 8,
                    target.getBbWidth() * 0.6, 0.04);
            this.end(level);
            return false;
        }
        this.age++;
        this.left--;
        if (this.age % HURT_EVERY == 0) {
            target.invulnerableTime = 0;
            // Plasma, not fire: what fire cannot hurt still burns green.
            target.hurt(level.damageSources().magic(), (float) GameCharacter.GREEN_LANTERN
                    .byName("construct_wheel").value("burnDamage"));
        }
        if (this.age % 3 == 0) {
            double wide = target.getBbWidth() * 0.45;
            Vec3 at = target.position().add(ParticleFx.spread(wide), target.getBbHeight() * (0.2 + 0.6
                    * ParticleFx.RANDOM.nextDouble()), ParticleFx.spread(wide));
            ParticleFx.fly(level, ParticleFx.fade(0xC8FFD4, PowerRing.GREEN, 0.9F), at, new Vec3(0.0, 1.0, 0.0),
                    0.06);
        }
        if (this.age == 1 || this.age % SEND_EVERY == 0) {
            this.send(level);
        }
        return true;
    }

    private void send(ServerLevel level) {
        Vec3 at = this.target.position();
        PacketDistributor.sendToPlayersNear(level, null, at.x, at.y, at.z, VIEW_RANGE,
                new ConstructPayload(this.id, this.target.getId(), at, new Vec3(0.0, 0.0, 1.0),
                        this.target.getBbWidth(), Math.min(1.0F, this.left / 10.0F), this.target.getBbHeight(), false,
                        ConstructPayload.BURN, 0, this.age, null));
    }

    private void end(ServerLevel level) {
        BURNING.remove(this.target.getId(), this);
        ConstructPayload.sendRemove(level, this.id, this.target.position());
    }
}
