package nl.tivek.multiversepowers.character.greenlantern.ability;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.greenlantern.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.MechScript;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.config.PowerRules;
import nl.tivek.multiversepowers.engine.ability.Cooldowns;
import nl.tivek.multiversepowers.engine.effect.Effect;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.fx.Sounds;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;

public final class MechAssembly implements Effect {
    private static final String KEY = "mech";
    private static final double VIEW_RANGE = 128.0;
    private static final double GROUND_BELOW = 12.0;
    private static final double CLAP_RADIUS = 3.5;
    private static final double DRILL_RADIUS = 4.5;
    private static final double CLAP_PUSH = 1.2;
    private static final double DRILL_PUSH = 1.4;
    private static final SoundEvent STOMP = Sounds.of("mech.stomp");
    private static final SoundEvent CLAP = Sounds.of("mech.clap");
    private static final SoundEvent CLACK = Sounds.of("mech.clack");
    private static final SoundEvent DRILL = Sounds.of("mech.drill");

    private static final Map<UUID, MechAssembly> ACTIVE = new HashMap<>();
    private static final Cooldowns<String> COOLDOWNS = new Cooldowns<>(1);

    private final ServerPlayer owner;
    private final CharacterAbility ability;
    private final int id = PowerRing.newId();
    private final MechScript.Stage stage;
    private final double startY;
    private int t;
    private int breaking = -1;

    private MechAssembly(ServerPlayer owner, CharacterAbility ability, Vec3 base, Vec3 facing) {
        this.owner = owner;
        this.ability = ability;
        this.stage = MechScript.Stage.of(base, facing);
        this.startY = Mth.clamp(owner.getY() - base.y, 0.0, MechScript.SEAT_Y);
    }

    public static boolean use(ServerPlayer owner, ServerLevel level, CharacterAbility ability) {
        MechAssembly mech = ACTIVE.get(owner.getUUID());
        if (mech != null) {
            mech.dismantle(level);
            return true;
        }
        if (Recharge.busy(owner)) {
            PowerRing.tell(owner, "busy_lantern");
            return false;
        }
        if (AirStrike.calling(owner) || GiantHands.waving(owner)) {
            return false;
        }
        if (SwordShield.equipped(owner) || Flamethrower.equipped(owner) || EnergyWhip.equipped(owner)) {
            PowerRing.tell(owner, "mech_hands");
            return false;
        }
        int left = COOLDOWNS.left(owner, KEY, 0);
        if (left > 0) {
            PowerRing.tell(owner, "mech_wait", (left + 19) / 20);
            return false;
        }
        float cost = (float) ability.value("mechPowerCost");
        float power = PowerRing.power(owner);
        if (power + 1.0E-4F < cost) {
            PowerRing.tell(owner, "no_power");
            return false;
        }
        Vec3 feet = owner.position();
        BlockHitResult ground = LoadedWorld.clip(level, new ClipContext(feet.add(0.0, 0.5, 0.0),
                feet.subtract(0.0, GROUND_BELOW, 0.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY,
                CollisionContext.empty()));
        if (ground.getType() == HitResult.Type.MISS) {
            PowerRing.tell(owner, "mech_no_ground");
            return false;
        }
        PowerRing.setPower(owner, power - cost);
        int ticks = (int) Math.round(ability.value("mechCooldown") * PowerRules.cooldowns());
        if (ticks > 0) {
            COOLDOWNS.start(owner, KEY, 0, ticks);
        }
        Flight.stop(owner);
        LightShield.stop(owner);
        Vec3 base = new Vec3(feet.x, ground.getLocation().y, feet.z);
        mech = new MechAssembly(owner, ability, base, owner.getLookAngle());
        ACTIVE.put(owner.getUUID(), mech);
        Effects.start(level, mech);
        PowerRing.tell(owner, "mech");
        PowerRing.sync(owner);
        Vec3 eye = owner.getEyePosition();
        Sounds.play(level, eye, SoundEvents.BEACON_POWER_SELECT, 1.4F, 0.6F);
        Sounds.play(level, eye, SoundEvents.RESPAWN_ANCHOR_CHARGE, 1.4F, 0.7F);
        Sounds.play(level, eye, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.6F, 0.5F);
        mech.send(level);
        return true;
    }

    public static boolean piloting(ServerPlayer player) {
        return ACTIVE.containsKey(player.getUUID());
    }

    public static void clear() {
        ACTIVE.clear();
        COOLDOWNS.clear();
    }

    @Override
    public boolean tick(ServerLevel level, int tick) {
        if (ACTIVE.get(this.owner.getUUID()) != this) {
            return false;
        }
        if (this.owner.isRemoved() || !this.owner.isAlive() || this.owner.level() != level) {
            this.end(level);
            return false;
        }
        if (this.breaking < 0 && !PowerRing.fuels(this.owner, level)) {
            this.dismantle(level);
        }
        if (this.breaking >= 0) {
            this.breaking++;
            this.hold(this.stage.point(0.0, MechScript.lowered(this.breaking), 0.0), true);
            if (this.breaking >= MechScript.BREAK_TICKS) {
                this.end(level);
                return false;
            }
            this.send(level);
            return true;
        }
        this.t++;
        this.beats(level);
        this.hold(this.stage.point(0.0, MechScript.seatHeight(this.startY, this.t), 0.0), this.t <= MechScript.LIFT_END);
        this.send(level);
        return true;
    }

    private void dismantle(ServerLevel level) {
        if (this.breaking >= 0) {
            return;
        }
        this.breaking = 0;
        Vec3 chest = this.stage.point(0.0, 7.0, 0.0);
        Sounds.play(level, chest, SoundEvents.AMETHYST_CLUSTER_BREAK, 3.0F, 0.6F);
        Sounds.play(level, chest, SoundEvents.GLASS_BREAK, 2.0F, 0.6F);
        Sounds.play(level, chest, SoundEvents.BEACON_DEACTIVATE, 2.0F, 0.7F);
        ParticleFx.cloud(level, ParticleFx.dust(PowerRing.GREEN, 2.0F), chest, 60, 3.0, 0.1);
        ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 1.4F), chest, 30, 2.5, 0.15);
    }

    // The pilot hangs where the mech needs them: their own game moves them there, the server only puts them back if not.
    private void hold(Vec3 at, boolean loose) {
        ServerPlayer player = this.owner;
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
        player.connection.aboveGroundTickCount = 0;
        if (player.position().distanceToSqr(at) > (loose ? 1.0 : 0.09)) {
            player.connection.teleport(at.x, at.y, at.z, player.getYRot(), player.getXRot(), RelativeMovement.ROTATION);
            player.connection.send(new ClientboundSetEntityMotionPacket(player));
        }
    }

    private void beats(ServerLevel level) {
        int t = this.t;
        if (t == MechScript.BOOT_FORM || t == MechScript.BOOT_FORM + MechScript.BOOT_GAP) {
            Vec3 at = this.stage.point(t == MechScript.BOOT_FORM ? 4.2 : -4.2, 3.4, 2.4);
            Sounds.play(level, at, SoundEvents.BEACON_ACTIVATE, 1.4F, 1.5F);
            Sounds.play(level, at, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.6F, 0.8F);
        }
        for (int side = 0; side < 2; side++) {
            boolean right = side == 0;
            double landed = MechScript.landed(right, t);
            if (landed == 0.0) {
                boolean stomp = t == MechScript.STOMPS[side];
                this.stomp(level, this.stage.point(MechScript.foot(right, t).at()), stomp);
            }
            if (t == MechScript.KNEES[side]) {
                this.click(level, this.stage.point(right ? MechScript.KNEE : mirror(MechScript.KNEE)), 1.2F);
            }
            if (t == MechScript.THIGHS[side] + MechScript.FORM_TICKS) {
                Sounds.play(level, this.stage.point(right ? MechScript.HIP : mirror(MechScript.HIP)),
                        SoundEvents.PISTON_CONTRACT, 1.6F, 0.6F);
            }
        }
        if (t == MechScript.HIPS_LOCK) {
            this.click(level, this.stage.point(0.0, 5.2, 0.0), 1.6F);
            Sounds.play(level, this.stage.point(0.0, 5.2, 0.0), SoundEvents.IRON_DOOR_CLOSE, 1.6F, 0.6F);
        }
        if (t == MechScript.ARMS_FORM) {
            Vec3 at = this.stage.point(0.0, 6.6, 1.5);
            Sounds.play(level, at, SoundEvents.BEACON_ACTIVATE, 2.0F, 1.2F);
            Sounds.play(level, at, SoundEvents.AMETHYST_BLOCK_RESONATE, 2.0F, 0.6F);
        }
        if (t == MechScript.SWING) {
            Sounds.play(level, this.stage.point(MechScript.CLAP_AT), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(),
                    1.6F, 0.6F);
        }
        if (t == MechScript.CLAP) {
            this.clap(level);
        }
        if (t == MechScript.FLY) {
            Vec3 at = this.stage.point(0.0, 8.0, 2.0);
            Sounds.play(level, at, SoundEvents.TRIDENT_RIPTIDE_3.value(), 2.0F, 0.8F);
            Sounds.play(level, at, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 1.6F, 0.8F);
        }
        if (t == MechScript.TORSO) {
            Vec3 at = this.stage.point(0.0, 7.0, 0.0);
            Sounds.play(level, at, SoundEvents.BEACON_POWER_SELECT, 2.0F, 0.5F);
            Sounds.play(level, at, SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, 1.6F, 0.7F);
        }
        if (t == MechScript.GLASS) {
            Sounds.play(level, this.stage.point(0.0, 7.2, 1.1), SoundEvents.AMETHYST_BLOCK_CHIME, 2.0F, 1.2F);
        }
        if (t == MechScript.ARMS_LOCK) {
            this.click(level, this.stage.point(MechScript.ELBOW), 3.0F);
            this.click(level, this.stage.point(mirror(MechScript.ELBOW)), 3.0F);
            Sounds.play(level, this.stage.point(0.0, 7.0, 0.0), SoundEvents.ANVIL_LAND, 0.6F, 1.4F);
        }
        if (t == MechScript.HEAD_FORM) {
            Sounds.play(level, this.stage.point(0.0, 13.0, 0.0), SoundEvents.BEACON_ACTIVATE, 2.0F, 0.9F);
        }
        if (t == MechScript.HEAD_AHEAD) {
            Sounds.play(level, this.stage.point(0.0, 13.0, 2.0), DRILL, 4.0F, 1.0F);
        }
        if (t == MechScript.IMPACT) {
            this.drill(level);
        }
        if (t > MechScript.IMPACT && t < MechScript.GRIND) {
            Vec3 at = this.stage.point(MechScript.DRILL_AT);
            BlockState under = this.under(level, at);
            if (!under.isAir()) {
                ParticleFx.cloud(level, new BlockParticleOption(ParticleTypes.BLOCK, under), at, 10, 0.5, 0.35);
            }
            ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, at, 8, 0.4, 0.4);
        }
        if (t == MechScript.GRIND) {
            Sounds.play(level, this.stage.point(MechScript.DRILL_AT), SoundEvents.TRIDENT_RIPTIDE_2.value(), 2.0F,
                    0.9F);
        }
        if (t == MechScript.CLACK) {
            Vec3 at = this.stage.point(MechScript.NECK);
            Sounds.play(level, at, CLACK, 5.0F, 0.8F);
            Sounds.play(level, at, SoundEvents.ANVIL_LAND, 1.2F, 0.7F);
            Sounds.play(level, at, SoundEvents.IRON_DOOR_CLOSE, 2.0F, 0.5F);
            ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.6F), at, 30, 0.3);
        }
        if (t == MechScript.DONE) {
            Vec3 at = this.stage.point(0.0, 7.0, 0.0);
            Sounds.play(level, at, SoundEvents.BEACON_ACTIVATE, 2.5F, 0.7F);
            Sounds.play(level, at, SoundEvents.AMETHYST_BLOCK_RESONATE, 2.5F, 0.6F);
        }
    }

    private static Vec3 mirror(Vec3 local) {
        return new Vec3(-local.x, local.y, local.z);
    }

    private void stomp(ServerLevel level, Vec3 at, boolean hard) {
        Sounds.play(level, at, STOMP, hard ? 4.0F : 2.8F, hard ? 0.9F : 1.05F);
        Sounds.play(level, at, SoundEvents.IRON_GOLEM_STEP, 2.0F, 0.5F);
        if (hard) {
            Sounds.play(level, at, SoundEvents.MACE_SMASH_GROUND, 1.6F, 0.6F);
        }
        BlockState under = this.under(level, at);
        if (!under.isAir()) {
            ParticleFx.cloud(level, new BlockParticleOption(ParticleTypes.BLOCK, under), at, hard ? 30 : 16, 0.9, 0.2);
        }
        ParticleFx.shockwave(level, ParticleTypes.CLOUD, at.add(0.0, 0.1, 0.0), hard ? 24 : 12, hard ? 0.35 : 0.2);
        ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 1.3F), at, 10, 0.8, 0.1);
    }

    private void click(ServerLevel level, Vec3 at, float volume) {
        Sounds.play(level, at, CLACK, volume, 1.3F);
        Sounds.play(level, at, SoundEvents.PISTON_EXTEND, volume * 0.6F, 0.8F);
        ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.1F), at, 12, 0.2);
    }

    private BlockState under(ServerLevel level, Vec3 at) {
        BlockPos pos = BlockPos.containing(at.x, at.y - 0.5, at.z);
        return level.isLoaded(pos) ? level.getBlockState(pos) : Blocks.AIR.defaultBlockState();
    }

    private void clap(ServerLevel level) {
        Vec3 at = this.stage.point(MechScript.CLAP_AT);
        Sounds.play(level, at, CLAP, 5.0F, 1.0F);
        Sounds.play(level, at, SoundEvents.MACE_SMASH_GROUND_HEAVY, 2.0F, 0.8F);
        Sounds.play(level, at, SoundEvents.FIREWORK_ROCKET_BLAST, 2.0F, 0.6F);
        ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.8F), at, 50, 0.45);
        ParticleFx.cloud(level, ParticleTypes.CLOUD, at, 16, 0.5, 0.25);
        ParticleFx.at(level, ParticleTypes.FLASH, at);
        this.shockwave(level, at, CLAP_RADIUS, this.ability.value("mechClapDamage"), CLAP_PUSH);
    }

    private void drill(ServerLevel level) {
        Vec3 at = this.stage.point(MechScript.DRILL_AT);
        BlockState under = this.under(level, at);
        Sounds.play(level, at, STOMP, 5.0F, 0.7F);
        Sounds.play(level, at, SoundEvents.GENERIC_EXPLODE.value(), 2.5F, 1.2F);
        Sounds.play(level, at, SoundEvents.MACE_SMASH_GROUND_HEAVY, 2.5F, 0.7F);
        if (!under.isAir()) {
            Sounds.play(level, at, under.getSoundType(level, BlockPos.containing(at.x, at.y - 0.5, at.z), null)
                    .getBreakSound(), 2.5F, 0.6F);
            ParticleFx.cloud(level, new BlockParticleOption(ParticleTypes.BLOCK, under), at.add(0.0, 0.5, 0.0), 60,
                    1.2, 0.5);
        }
        ParticleFx.shockwave(level, ParticleFx.dust(PowerRing.BRIGHT, 1.8F), at.add(0.0, 0.2, 0.0), 40, 0.7);
        ParticleFx.shockwave(level, ParticleTypes.CLOUD, at.add(0.0, 0.2, 0.0), 30, 0.45);
        ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, at, 30, 0.6, 0.6);
        this.shockwave(level, at, DRILL_RADIUS, this.ability.value("mechDrillDamage"), DRILL_PUSH);
    }

    private void shockwave(ServerLevel level, Vec3 at, double radius, double damage, double push) {
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius),
                entity -> GiantHands.fair(this.owner, entity))) {
            Vec3 to = living.getBoundingBox().getCenter().subtract(at);
            double far = to.length();
            if (far > radius) {
                continue;
            }
            double near = 1.0 - far / radius;
            if (damage > 0.0) {
                living.invulnerableTime = 0;
                living.hurt(level.damageSources().playerAttack(this.owner), (float) (damage * (0.5 + 0.5 * near)));
            }
            double resist = Mth.clamp(living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
            double flat = Math.sqrt(to.x * to.x + to.z * to.z);
            Vec3 out = flat < 1.0E-3 ? Vec3.ZERO : new Vec3(to.x / flat, 0.0, to.z / flat);
            double strength = push * (0.5 + 0.5 * near);
            living.setDeltaMovement(new Vec3(out.x * strength, 0.35 + 0.3 * near, out.z * strength)
                    .scale(1.0 - resist));
            living.hasImpulse = true;
            living.hurtMarked = true;
        }
    }

    private void send(ServerLevel level) {
        Vec3 base = this.stage.base();
        PacketDistributor.sendToPlayersNear(level, null, base.x, base.y, base.z, VIEW_RANGE,
                new ConstructPayload(this.id, this.owner.getId(), base, this.stage.ahead(), (float) this.startY, 1.0F,
                        (float) this.breaking, false, ConstructPayload.MECH,
                        this.breaking >= 0 ? MechScript.BREAKING : MechScript.BUILDING, this.t, null));
    }

    private void end(ServerLevel level) {
        ACTIVE.remove(this.owner.getUUID(), this);
        ConstructPayload.sendRemove(level, this.id, this.stage.base());
        this.owner.resetFallDistance();
    }
}
