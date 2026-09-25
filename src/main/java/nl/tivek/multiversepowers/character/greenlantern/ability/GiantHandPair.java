package nl.tivek.multiversepowers.character.greenlantern.ability;

import java.util.List;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.HandDuo;
import nl.tivek.multiversepowers.character.greenlantern.HandPose;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import static nl.tivek.multiversepowers.character.greenlantern.ability.GiantHands.SCALE;
import static nl.tivek.multiversepowers.character.greenlantern.ability.GiantHands.head;

abstract class GiantHandPair extends GiantHandBase {
    private static final double AXE_REACH = 6.0;
    private static final double AXE_MIDDLE = 3.0;
    private static final double AXE_EDGE = 1.5;
    private static final double AXE_OUT = 2.4;
    private static final double AXE_UP = 1.4;
    private static final int CHOP_HEARD = 5;

    GiantHandPair(GiantHands storm, int variant, Vec3 base, LivingEntity target) {
        super(storm, variant, base, target);
    }

    private HandDuo duo() {
        return HandDuo.at(this.base, this.variant, this.aim, this.t, SCALE);
    }

    void pair(ServerLevel level) {
        int t = this.t;
        if (t == HandDuo.ARRIVES) {
            HandDuo duo = this.duo();
            this.opens(level, duo.leftPortal, 1.5F);
            this.opens(level, duo.rightPortal, 1.7F);
        }
        if (t == HandDuo.OUT) {
            HandDuo duo = this.duo();
            for (HandPose.Place hand : List.of(duo.leftPlace, duo.rightPlace)) {
                Vec3 wrist = hand.wrist();
                ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 1.1F), wrist, 8, 0.6, 0.03);
                this.storm.sound(level, wrist, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2F, 1.3F);
            }
        }
        if (t == HandDuo.SNAP) {
            Vec3 snap = this.duo().rightPlace.at(HandDuo.SNAP_AT);
            ParticleFx.sphereOut(level, ParticleTypes.ELECTRIC_SPARK, snap, 12, 0.3);
            ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.0F), snap, 14, 0.22);
            this.storm.sound(level, snap, SoundEvents.WOODEN_BUTTON_CLICK_ON, 2.4F, 1.9F);
            this.storm.sound(level, snap, SoundEvents.AMETHYST_BLOCK_CHIME, 1.8F, 1.5F);
        }
        if (t == HandDuo.OK) {
            Vec3 ok = this.duo().leftPlace.at(HandDuo.OK_AT);
            ParticleFx.cloud(level, ParticleTypes.END_ROD, ok, 6, 0.2, 0.02);
            ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 0.9F), ok, 12, 0.12);
            this.storm.sound(level, ok, SoundEvents.NOTE_BLOCK_CHIME.value(), 1.8F, 1.6F);
            this.storm.sound(level, ok, SoundEvents.AMETHYST_BLOCK_CHIME, 1.4F, 1.9F);
        }
        if (t == HandDuo.AXE_OPENS) {
            this.opens(level, this.duo().axePortal, 0.8F);
        }
        if (t == HandDuo.GRAB) {
            HandDuo duo = this.duo();
            for (double along : new double[] { HandDuo.GRIP_LOW, HandDuo.GRIP_HIGH }) {
                Vec3 grip = duo.axeEnd.add(duo.axeUp.scale(along * SCALE));
                ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.2F), grip, 12, 0.25);
                this.storm.sound(level, grip, SoundEvents.ARMOR_EQUIP_NETHERITE.value(), 1.8F, 0.6F);
            }
            Vec3 middle = duo.axeEnd.add(duo.axeUp.scale((HandDuo.GRIP_LOW + HandDuo.GRIP_HIGH) * 0.5 * SCALE));
            this.storm.sound(level, middle, SoundEvents.ANVIL_PLACE, 1.4F, 0.6F);
        }
        if (t == HandDuo.AXE_FREE) {
            HandDuo duo = this.duo();
            Vec3 head = head(duo);
            this.shuts(level, duo.axePortal, 1.2F);
            this.storm.sound(level, head, SoundEvents.PLAYER_ATTACK_SWEEP, 1.6F, 1.3F);
            this.storm.sound(level, head, SoundEvents.AMETHYST_BLOCK_HIT, 1.6F, 0.7F);
        }
        if (t == HandDuo.RAISED) {
            Vec3 head = head(this.duo());
            ParticleFx.cloud(level, ParticleFx.dust(PowerRing.PALE, 1.2F), head, 10, 0.8, 0.02);
            this.storm.sound(level, head, SoundEvents.ENDER_DRAGON_FLAP, 1.8F, 0.6F);
            this.storm.sound(level, head, SoundEvents.AMETHYST_BLOCK_RESONATE, 2.0F, 0.5F);
        }
        if (t == Math.max(HandDuo.RAISED + 1, HandDuo.IMPACT - CHOP_HEARD)) {
            Vec3 head = head(this.duo());
            this.storm.sound(level, head, SoundEvents.PLAYER_ATTACK_SWEEP, 2.4F, 0.5F);
            this.storm.sound(level, head, SoundEvents.ENDER_DRAGON_FLAP, 2.0F, 0.9F);
        }
        if (t == HandDuo.IMPACT) {
            this.chop(level);
        }
        if (t == HandDuo.THUMBS) {
            HandDuo duo = this.duo();
            HandPose.Place[] both = { duo.leftPlace, duo.rightPlace };
            float[] notes = { 1.19F, 1.5F };
            for (int i = 0; i < both.length; i++) {
                Vec3 thumb = both[i].at(HandDuo.FIST_HOLE).add(0.0, 2.4 * SCALE, 0.0);
                ParticleFx.cloud(level, ParticleTypes.HAPPY_VILLAGER, thumb, 8, 0.5, 0.0);
                ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.0F), thumb, 10, 0.15);
                this.storm.sound(level, thumb, SoundEvents.NOTE_BLOCK_BELL.value(), 1.6F, notes[i]);
            }
            Vec3 between = duo.leftPlace.wrist().lerp(duo.rightPlace.wrist(), 0.5);
            this.storm.sound(level, between, SoundEvents.PLAYER_LEVELUP, 0.6F, 1.4F);
        }
        if (t == HandDuo.RETRACT) {
            HandDuo duo = this.duo();
            this.storm.sound(level, duo.leftPlace.wrist(), SoundEvents.ENDER_DRAGON_FLAP, 1.2F, 1.6F);
            this.storm.sound(level, duo.rightPlace.wrist(), SoundEvents.ENDER_DRAGON_FLAP, 1.2F, 1.7F);
        }
        if (t == HandDuo.HANDS_GONE) {
            HandDuo duo = this.duo();
            this.shuts(level, duo.leftPortal, 1.6F);
            this.shuts(level, duo.rightPortal, 1.8F);
        }
        if (t == HandDuo.AXE_BREAKS) {
            this.breakAxe(level);
        }
    }

    private void opens(ServerLevel level, HandDuo.Portal portal, float pitch) {
        Vec3 middle = portal.center();
        this.rim(level, portal, false);
        ParticleFx.cloud(level, ParticleTypes.END_ROD, middle, 8, portal.radius() * 0.3, 0.06);
        this.storm.sound(level, middle, SoundEvents.BEACON_ACTIVATE, 1.6F, pitch);
        this.storm.sound(level, middle, SoundEvents.ENDER_DRAGON_FLAP, 1.4F, pitch * 0.9F);
        this.storm.sound(level, middle, SoundEvents.AMETHYST_BLOCK_CHIME, 1.6F, pitch * 0.7F);
    }

    private void shuts(ServerLevel level, HandDuo.Portal portal, float pitch) {
        Vec3 middle = portal.center();
        this.rim(level, portal, true);
        ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 1.2F), middle, 8, 0.3, 0.05);
        this.storm.sound(level, middle, SoundEvents.ITEM_PICKUP, 1.6F, pitch * 0.35F);
        this.storm.sound(level, middle, SoundEvents.BEACON_DEACTIVATE, 1.2F, pitch);
    }

    private void rim(ServerLevel level, HandDuo.Portal portal, boolean in) {
        ParticleOptions light = ParticleFx.dust(PowerRing.BRIGHT, 1.3F);
        int points = Math.max(12, (int) Math.round(portal.radius() * 10.0));
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0 * i / points;
            Vec3 out = portal.a().scale(Math.cos(angle)).add(portal.b().scale(Math.sin(angle)));
            Vec3 at = portal.center().add(out.scale(portal.radius()));
            ParticleFx.fly(level, light, at, in ? out.scale(-1.0) : out, in ? 0.3 : 0.4);
        }
    }

    private void chop(ServerLevel level) {
        Vec3 edge = HandDuo.strike(this.base, this.variant, this.aim, SCALE);
        Vec3 middle = new Vec3(edge.x, this.base.y, edge.z);
        Vec3 ahead = new Vec3(middle.x - this.storm.owner.getX(), 0.0,
                middle.z - this.storm.owner.getZ());
        ahead = ahead.lengthSqr() < 1.0E-4 ? HandPose.axeWay(this.variant) : ahead.normalize();
        double reach = AXE_REACH * SCALE;
        double damage = this.storm.ability.getDamage();
        for (LivingEntity living : this.near(level, HandDuo.REACH + AXE_REACH + 3.0)) {
            Vec3 to = living.position().subtract(middle);
            Vec3 flat = new Vec3(to.x, 0.0, to.z);
            double out = flat.length();
            if (out > reach + living.getBbWidth() * 0.5 || to.y < -2.5 || to.y > 4.0 * SCALE) {
                continue;
            }
            double share = Mth.lerp(Math.min(1.0, out / reach), AXE_MIDDLE, AXE_EDGE);
            Vec3 away = out < 1.0E-2 ? ahead : flat.scale(1.0 / out);
            this.hit(level, living, damage * share, away, AXE_OUT, AXE_UP);
            ParticleFx.send(level, ParticleTypes.EXPLOSION, living.getX(), living.getY() + 0.6, living.getZ(), 1,
                    0.0, 0.0, 0.0, 0.0);
        }
        Vec3 low = middle.add(0.0, 0.2, 0.0);
        this.dustAt(level, middle, 70, 2.2);
        ParticleFx.send(level, ParticleTypes.EXPLOSION_EMITTER, middle.x, middle.y + 0.4, middle.z, 1, 0.0, 0.0,
                0.0, 0.0);
        ParticleFx.send(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, middle.x, middle.y + 0.4, middle.z, 14, 2.2,
                0.5, 2.2, 0.03);
        ParticleFx.shockwave(level, ParticleFx.dust(PowerRing.PALE, 2.0F), low, 64, 1.1);
        ParticleFx.shockwave(level, ParticleFx.dust(PowerRing.BRIGHT, 1.5F), low, 40, 0.6);
        ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.6F), middle.add(0.0, 1.2, 0.0), 30, 0.5);
        ParticleOptions crack = ParticleFx.dust(PowerRing.BRIGHT, 1.1F);
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI * 2.0 * (i + 0.2 + ParticleFx.RANDOM.nextDouble() * 0.6) / 6.0;
            Vec3 end = low.add(Math.cos(angle) * reach * 0.8, 0.0, Math.sin(angle) * reach * 0.8);
            ParticleFx.zigzag(level, crack, low, end, 5, 0.6, 0.45);
        }
        this.storm.sound(level, middle, SoundEvents.GENERIC_EXPLODE.value(), 3.0F, 0.55F);
        this.storm.sound(level, middle, SoundEvents.MACE_SMASH_GROUND_HEAVY, 2.4F, 0.6F);
        this.storm.sound(level, middle, SoundEvents.ANVIL_LAND, 1.6F, 0.45F);
        this.storm.sound(level, middle, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, 2.0F, 0.6F);
        this.storm.sound(level, middle, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 2.6F, 0.5F);
        this.storm.sound(level, middle, SoundEvents.ROOTED_DIRT_BREAK, 2.4F, 0.4F);
    }

    private void breakAxe(ServerLevel level) {
        HandDuo duo = this.duo();
        ParticleOptions chips = new BlockParticleOption(ParticleTypes.BLOCK,
                Blocks.EMERALD_BLOCK.defaultBlockState());
        ParticleOptions light = ParticleFx.dust(PowerRing.BRIGHT, 1.2F);
        for (int i = 0; i <= 4; i++) {
            Vec3 at = duo.axeEnd.add(duo.axeUp.scale(HandDuo.AXE_LENGTH * SCALE * i / 4.0));
            ParticleFx.send(level, chips, at.x, at.y, at.z, 10, 0.5, 0.5, 0.5, 0.15);
            ParticleFx.cloud(level, light, at, 5, 0.5, 0.05);
        }
        Vec3 head = head(duo);
        this.dustAt(level, new Vec3(head.x, this.base.y, head.z), 18);
        Vec3 middle = duo.axeEnd.add(duo.axeUp.scale(HandDuo.AXE_LENGTH * 0.5 * SCALE));
        this.storm.sound(level, middle, SoundEvents.AMETHYST_BLOCK_BREAK, 2.0F, 0.6F);
        this.storm.sound(level, middle, SoundEvents.GLASS_BREAK, 1.6F, 0.7F);
        this.storm.sound(level, head, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.4F, 0.8F);
    }
}
