package nl.tivek.multiversepowers.character.greenlantern.ability.hands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.character.greenlantern.ability.light.LightBubble;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandPose;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandVictimPayload;
import nl.tivek.multiversepowers.engine.entity.HeldMobs;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;
import static nl.tivek.multiversepowers.character.greenlantern.ability.hands.GiantHands.GRABBED;
import static nl.tivek.multiversepowers.character.greenlantern.ability.hands.GiantHands.GRAB_TALL;
import static nl.tivek.multiversepowers.character.greenlantern.ability.hands.GiantHands.GRAB_WIDE;
import static nl.tivek.multiversepowers.engine.math.Vectors.flat;

// What the evil eye and the megaphone do.
abstract class GiantHandMarvels extends GiantHandFeats {
    private static final double EYE_REACH = 16.0;
    private static final int EYE_CHOICES = 5;
    private static final double SHATTER_DAMAGE = 1.8;
    private static final double HORN_RANGE = 14.0;
    private static final double HORN_CONE = Math.cos(0.8);
    private static final double BLARE_DAMAGE = 0.12;
    private static final double POP_DAMAGE = 1.0;

    private record Puppet(LivingEntity living, Vec3 from, double lift, int start, int k) {
    }

    private final List<Puppet> puppets = new ArrayList<>();
    private final Set<LivingEntity> deafened = new LinkedHashSet<>();
    // The evil eye's puppeteer, up beside it: the strings run from its fingers.
    @Nullable
    GiantHandMarvels partner;

    GiantHandMarvels(GiantHands storm, int variant, Vec3 base, LivingEntity target) {
        super(storm, variant, base, target);
    }

    GiantHandMarvels(GiantHands storm, int variant, Vec3 base, LivingEntity target, Vec3 facing) {
        super(storm, variant, base, target, facing);
    }

    @Override
    void feat(ServerLevel level) {
        switch (this.move) {
            case HandPose.EYE -> this.eye(level);
            case HandPose.PUPPETEER -> this.puppeteer(level);
            case HandPose.MEGAPHONE -> this.megaphone(level);
            default -> super.feat(level);
        }
    }

    @Override
    void letGo() {
        super.letGo();
        for (Puppet puppet : this.puppets) {
            GRABBED.remove(puppet.living().getId(), this);
            if (puppet.living() instanceof Mob mob) {
                HeldMobs.release(mob);
            }
        }
        this.puppets.clear();
    }

    private Vec3 eyeAt() {
        return this.place().at(HandPose.EYE_POINT);
    }

    private int strings() {
        return this.partner != null ? this.partner.id() : this.id();
    }

    private void puppeteer(ServerLevel level) {
        Vec3 palm = this.place().at(new Vec3(0.0, 2.0, 0.0));
        if (this.t == HandPose.EYE_STRINGS) {
            this.storm.sound(level, palm, SoundEvents.CROSSBOW_SHOOT, 1.6F, 0.6F);
            this.storm.sound(level, palm, SoundEvents.BEACON_POWER_SELECT, 1.0F, 1.4F);
        }
        if (this.t > HandPose.EYE_STRINGS && this.t < HandPose.EYE_STONE && (this.t - HandPose.EYE_STRINGS) % 6 == 0) {
            this.storm.sound(level, palm, SoundEvents.LEASH_KNOT_PLACE, 0.9F, 0.8F + 0.1F * (this.t % 4));
        }
    }

    private void eye(ServerLevel level) {
        if (this.t == HandPose.EYE_OPENS) {
            Vec3 eye = this.eyeAt();
            this.storm.sound(level, eye, SoundEvents.ENDER_EYE_DEATH, 2.0F, 0.5F);
            this.storm.sound(level, eye, SoundEvents.BEACON_ACTIVATE, 1.4F, 0.6F);
            this.storm.sound(level, eye, SoundEvents.WARDEN_SONIC_CHARGE, 1.6F, 0.8F);
            ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.5F), eye, 30, 0.5);
        }
        if (this.t > HandPose.EYE_STRINGS && this.t < HandPose.EYE_STONE && (this.t - HandPose.EYE_STRINGS) % 10 == 0) {
            this.storm.sound(level, this.eyeAt(), SoundEvents.WARDEN_HEARTBEAT, 2.0F, 0.8F);
        }
        if (this.t == HandPose.EYE_STRINGS) {
            this.string(level);
        }
        if (this.t == HandPose.EYE_CRAZY) {
            for (Puppet puppet : this.puppets) {
                HandVictimPayload.send(puppet.living(), this.id(), HandVictimPayload.GLARE);
            }
            Vec3 eye = this.eyeAt();
            this.storm.sound(level, eye, SoundEvents.ELDER_GUARDIAN_CURSE, 1.2F, 1.4F);
            this.storm.sound(level, eye, SoundEvents.AMETHYST_BLOCK_RESONATE, 2.0F, 0.5F);
        }
        this.puppets.removeIf(puppet -> {
            if (puppet.living().isAlive() && puppet.living().level() == level) {
                return false;
            }
            GRABBED.remove(puppet.living().getId(), this);
            if (puppet.living() instanceof Mob mob) {
                HeldMobs.release(mob);
            }
            return true;
        });
        for (Puppet puppet : this.puppets) {
            hold(puppet.living(), this.dangling(puppet));
        }
        if (this.t == HandPose.EYE_STONE) {
            for (Puppet puppet : this.puppets) {
                HandVictimPayload.send(puppet.living(), this.strings(), HandVictimPayload.STATUE);
                Vec3 at = puppet.living().getBoundingBox().getCenter();
                ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 1.4F), at, 16, 0.4, 0.02);
                this.storm.sound(level, at, SoundEvents.DEEPSLATE_PLACE, 1.6F, 0.6F);
                this.storm.sound(level, at, SoundEvents.AMETHYST_BLOCK_PLACE, 1.6F, 0.5F);
            }
        }
        if (this.t == HandPose.EYE_SHATTERS) {
            this.shatter(level);
        }
    }

    // Three creatures near the caster, picked at random from the nearest, each on strings from a finger.
    private void string(ServerLevel level) {
        Vec3 home = this.storm.owner.position();
        List<LivingEntity> near = new ArrayList<>();
        for (LivingEntity living : this.near(level, EYE_REACH)) {
            if (living.getBbWidth() <= GRAB_WIDE && living.getBbHeight() <= GRAB_TALL
                    && !HeldMobs.isHeldByAnyone(living) && !LightBubble.trapped(living)) {
                near.add(living);
            }
        }
        near.sort(Comparator.comparingDouble(living -> living.distanceToSqr(home)));
        List<LivingEntity> picked = new ArrayList<>(near.subList(0, Math.min(EYE_CHOICES, near.size())));
        Collections.shuffle(picked, new Random(this.storm.owner.getRandom().nextLong()));
        int k = 0;
        for (LivingEntity living : picked.subList(0, Math.min(HandPose.EYE_VICTIMS, picked.size()))) {
            GRABBED.put(living.getId(), this);
            if (living instanceof Mob mob) {
                HeldMobs.hold(mob);
            }
            this.puppets.add(new Puppet(living, living.position(), this.room(level, living), HandPose.puppetStart(k),
                    k));
            HandVictimPayload.send(living, this.strings(), HandVictimPayload.PUPPET);
            this.storm.sound(level, living.getEyePosition(), SoundEvents.LEASH_KNOT_PLACE, 1.6F, 0.7F);
            k++;
        }
    }

    // How high a creature can be lifted before its head meets a ceiling.
    private double room(ServerLevel level, LivingEntity living) {
        Vec3 top = living.position().add(0.0, living.getBbHeight(), 0.0);
        BlockHitResult hit = LoadedWorld.clip(level, new ClipContext(top, top.add(0.0, HandPose.PUPPET_LIFT + 0.3,
                0.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        return hit.getType() == HitResult.Type.MISS ? HandPose.PUPPET_LIFT
                : Math.max(0.0, hit.getLocation().y - top.y - 0.3);
    }

    // Lifted and swaying on its strings while the hand works them; still as stone once it is a statue.
    private Vec3 dangling(Puppet puppet) {
        int t = Math.min(this.t, HandPose.EYE_STONE);
        double up = Math.min(1.0, Math.max(0.0, (t - puppet.start()) / (double) HandPose.PUPPET_RISES));
        up = up * up * (3.0 - 2.0 * up);
        double dance = up * Math.min(1.0, Math.max(0.0, (t - puppet.start() - HandPose.PUPPET_RISES) / 6.0));
        double phase = t * 0.35 + puppet.k() * 2.1;
        double bob = 0.35 * dance * Math.sin(phase);
        double swayX = 0.25 * dance * Math.sin(phase * 0.7 + 1.3);
        double swayZ = 0.25 * dance * Math.cos(phase * 0.6 + 0.4);
        double lift = Math.min(puppet.lift(), puppet.lift() * up + bob);
        return puppet.from().add(swayX, puppet.living().getBbHeight() * 0.5 + Math.max(0.0, lift), swayZ);
    }

    private void shatter(ServerLevel level) {
        for (Puppet puppet : this.puppets) {
            LivingEntity living = puppet.living();
            Vec3 at = living.getBoundingBox().getCenter();
            HandVictimPayload.send(living, this.strings(), HandVictimPayload.SHATTER);
            this.hit(level, living, this.storm.ability.getDamage() * SHATTER_DAMAGE, Vec3.ZERO, 0.0, 0.0);
            ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.5F), at, 30, 0.45);
            ParticleFx.cloud(level, ParticleTypes.END_ROD, at, 8, 0.4, 0.15);
            this.storm.sound(level, at, SoundEvents.GLASS_BREAK, 2.0F, 0.6F);
            this.storm.sound(level, at, SoundEvents.AMETHYST_CLUSTER_BREAK, 2.0F, 0.6F);
            this.storm.sound(level, at, SoundEvents.DEEPSLATE_BREAK, 1.6F, 0.7F);
        }
        this.letGo();
        Vec3 eye = this.eyeAt();
        this.storm.sound(level, eye, SoundEvents.PLAYER_ATTACK_CRIT, 1.6F, 0.5F);
        this.storm.sound(level, eye, SoundEvents.WARDEN_SONIC_BOOM, 1.4F, 1.2F);
        ParticleFx.shockwave(level, ParticleFx.dust(PowerRing.PALE, 1.6F), eye, 36, 0.6);
    }

    private void megaphone(ServerLevel level) {
        if (this.t == HandPose.HORN_FORMED - 8) {
            this.storm.sound(level, this.place().at(HandPose.HORN_HANDLE), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.4F,
                    1.2F);
        }
        for (int k = 0; k < HandPose.BLARES.length; k++) {
            if (this.t == HandPose.BLARES[k]) {
                this.blare(level, k);
            }
        }
        if (this.t == HandPose.HORN_BREAKS) {
            Vec3 at = this.place().at(HandPose.HORN_HANDLE);
            this.storm.sound(level, at, SoundEvents.GLASS_BREAK, 1.6F, 0.8F);
            this.storm.sound(level, at, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.6F, 0.9F);
        }
        if (this.t == HandPose.POPS) {
            this.pop(level);
        }
    }

    // The megaphone blares along its horn: everything in its cone is struck and clasps its ears.
    private void blare(ServerLevel level, int k) {
        HandPose.Place place = this.place();
        Vec3 way = place.up().normalize();
        Vec3 bell = place.at(new Vec3(HandPose.HORN_AXIS.x, HandPose.HORN_BELL, HandPose.HORN_AXIS.z));
        for (LivingEntity living : this.near(level, HORN_RANGE + 8.0)) {
            Vec3 to = living.getBoundingBox().getCenter().subtract(bell);
            double along = to.dot(way);
            double far = to.length();
            if (along < -1.0 || far > HORN_RANGE || far > 2.0 && along < far * HORN_CONE) {
                continue;
            }
            this.hit(level, living, this.storm.ability.getDamage() * BLARE_DAMAGE, flat(way), 0.25, 0.05);
            if (this.deafened.add(living)) {
                HandVictimPayload.send(living, this.id(), HandVictimPayload.DEAF);
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, HandPose.POPS - this.t + 2, 4),
                        this.storm.owner);
            }
        }
        float pitch = 0.9F + 0.15F * k;
        this.storm.sound(level, bell, SoundEvents.WARDEN_SONIC_BOOM, 2.4F, pitch + 0.3F);
        this.storm.sound(level, bell, SoundEvents.RAID_HORN.value(), 2.0F, pitch + 0.4F);
        this.storm.sound(level, bell, SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(), 2.0F, pitch);
        ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 1.2F), bell, 10, 0.5, 0.1);
    }

    // Every creature the blares caught bursts in a small green blast.
    private void pop(ServerLevel level) {
        for (LivingEntity living : this.deafened) {
            if (!living.isAlive() || living.level() != level) {
                continue;
            }
            Vec3 at = living.getBoundingBox().getCenter();
            HandVictimPayload.send(living, this.id(), HandVictimPayload.POP);
            this.hit(level, living, this.storm.ability.getDamage() * POP_DAMAGE, Vec3.ZERO, 0.0, 0.4);
            ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.3F), at, 24, 0.35);
            ParticleFx.cloud(level, ParticleFx.dust(PowerRing.PALE, 1.0F), at, 10, 0.3, 0.05);
            this.storm.sound(level, at, SoundEvents.FIREWORK_ROCKET_BLAST, 1.6F, 1.6F);
            this.storm.sound(level, at, SoundEvents.GENERIC_EXPLODE.value(), 0.7F, 1.9F);
        }
        this.deafened.clear();
    }

}
