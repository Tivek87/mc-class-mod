package nl.tivek.welcomescreen.character.lantern;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.welcomescreen.WelcomeScreenMod;
import nl.tivek.welcomescreen.character.CharacterAbility;
import nl.tivek.welcomescreen.character.Characters;
import nl.tivek.welcomescreen.character.GameCharacter;
import nl.tivek.welcomescreen.network.ConstructPayload;
import nl.tivek.welcomescreen.spell.SpellCasting;
import nl.tivek.welcomescreen.spell.SpellEffect;
import nl.tivek.welcomescreen.spell.SpellFx;

/**
 * The first construct of the wheel: a sword of hard light in the ring hand and a shield on the other arm. They take
 * shape when he picks them (the sword tossed up spinning and caught, and knocked on the shield) and break into solid
 * pieces when he puts them away. While he holds them the mouse is theirs:
 * <ul>
 * <li><b>Left click:</b> one of twelve cuts and thrusts (see {@link SwordMove}), picked at random each time and
 * flowing on from the last.</li>
 * <li><b>Left held 2 seconds:</b> the shield before his chest (it takes most of what comes from the front) and twelve
 * quick stabs all over the front.</li>
 * <li><b>Right click:</b> one of six shield bashes that shove what they strike away from him.</li>
 * <li><b>Right held 2 seconds:</b> bent forward behind the locked shield he charges straight ahead, shoving everyone in
 * his way aside with a light hit; running into a wall or letting go ends it, and he slams the shield into the ground
 * for a small shockwave.</li>
 * </ul>
 * The numbers are settings of the Construct Wheel. His client plays every move the moment he clicks; the server strikes
 * on the move's own ticks and shows the moves to everyone else.
 */
@EventBusSubscriber(modid = WelcomeScreenMod.MODID)
public final class SwordShield implements SpellEffect {
    /** How long the sword and shield take to break up once he puts them away, in ticks. */
    public static final int BREAK_TICKS = 8;
    // How high up his body the moves strike from, as a part of his height.
    private static final double CHEST = 0.62;
    // A creature this close is struck whatever the arc; one further above or below his chest than this is out of reach.
    private static final double CLOSE = 0.9;
    private static final double TALL = 2.2;
    // How far to his sides a charge shoves creatures aside, in blocks, and how far ahead of him it looks for them.
    private static final double CHARGE_WIDE = 1.3;
    private static final double CHARGE_AHEAD = 1.4;
    // How far ahead of him the shield strikes the ground when a charge ends.
    private static final double SLAM_AHEAD = 1.3;
    // How close to the line of a stab of the flurry a creature has to be to be struck, in blocks.
    private static final double STAB_WIDE = 0.45;
    // A move may start this many ticks before the last one is ready: the click came over the network a little later.
    private static final int SLACK = 2;
    // What counts as in front of him for the guard: straight ahead is 1, straight behind -1.
    private static final double FRONT = 0.2;
    // What part of a hit from the front still gets through the shield locked before him while he charges.
    private static final float CHARGE_KEPT = 0.25F;
    private static final double VIEW_RANGE = 96.0;

    private static final Map<UUID, SwordShield> HELD = new HashMap<>();

    private final int id = PowerRing.newId();
    private final ServerPlayer owner;
    private final Set<Integer> shoved = new HashSet<>();
    private int age;
    private SwordMove move = SwordMove.EQUIP;
    private int moveStart;
    private boolean flurry;
    private boolean charging;
    private Vec3 way = new Vec3(0.0, 0.0, 1.0);
    private int breaking = -1;

    private SwordShield(ServerPlayer owner) {
        this.owner = owner;
    }

    /**
     * The construct wheel: he picked something. The sword and shield take shape when he picks them, and break up when he
     * picks anything else (empty hands too).
     */
    public static void hold(ServerPlayer player, Construct construct) {
        SwordShield now = HELD.get(player.getUUID());
        boolean want = construct == Construct.SWORD_SHIELD && Characters.of(player) == GameCharacter.GREEN_LANTERN
                && player.isAlive() && !Arrival.busy(player);
        if (want && (now == null || now.breaking >= 0)) {
            SwordShield sword = new SwordShield(player);
            // One that was still breaking up finishes that on its own.
            HELD.put(player.getUUID(), sword);
            SpellCasting.start(player.serverLevel(), sword);
            sword.sound(SoundEvents.BEACON_POWER_SELECT, 0.8F, 1.6F);
            sword.sound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 0.9F);
            sword.send(player.serverLevel());
        } else if (!want && now != null) {
            now.breakUp();
        }
    }

    /** True while this player holds the sword and shield, whole: the mouse is theirs. */
    public static boolean equipped(ServerPlayer player) {
        SwordShield sword = HELD.get(player.getUUID());
        return sword != null && sword.breaking < 0;
    }

    /**
     * The button of the hand that attacks, while he holds the sword: a tap cuts or thrusts (the move his client picked),
     * holding it does the flurry, and letting go of a held button ends the flurry.
     *
     * @return true when something happened
     */
    static boolean attack(ServerPlayer player, ServerLevel level, boolean on, int data) {
        SwordShield sword = HELD.get(player.getUUID());
        if (sword == null || sword.breaking >= 0) {
            return false;
        }
        if (!on) {
            return sword.stopFlurry();
        }
        if ((data & Characters.HOLD) != 0) {
            return sword.startFlurry();
        }
        if ((data & Characters.TAP) == 0) {
            return false;
        }
        SwordMove move = SwordMove.byIndex(data >> Characters.MOVE_SHIFT);
        return sword.begin(move != null && move.kind() == SwordMove.Kind.ATTACK ? move
                : SwordMove.randomAttack(player.getRandom(), null));
    }

    /**
     * The button of the hand that defends, while he holds the shield: a tap bashes (the bash his client picked), holding
     * it charges, and letting go (or running into a wall) ends the charge with a slam.
     *
     * @return true when something happened
     */
    static boolean defend(ServerPlayer player, ServerLevel level, boolean on, int data) {
        SwordShield sword = HELD.get(player.getUUID());
        if (sword == null || sword.breaking >= 0) {
            return false;
        }
        if (!on) {
            return sword.stopCharge();
        }
        if ((data & Characters.HOLD) != 0) {
            return sword.startCharge();
        }
        if ((data & Characters.TAP) == 0) {
            return false;
        }
        SwordMove move = SwordMove.byIndex(data >> Characters.MOVE_SHIFT);
        return sword.begin(move != null && move.kind() == SwordMove.Kind.BASH ? move
                : SwordMove.randomBash(player.getRandom(), null));
    }

    /** The server stops: nobody holds a sword any more. */
    static void clear() {
        HELD.clear();
    }

    /** The settings of the sword and shield: those of the Construct Wheel. */
    private static CharacterAbility wheel() {
        return GameCharacter.GREEN_LANTERN.byName("construct_wheel");
    }

    /** True when he may start a new move: the last one has come far enough, and he is not charging. */
    private boolean free() {
        return !this.charging && this.age - this.moveStart >= this.move.ready() - SLACK;
    }

    private boolean begin(SwordMove next) {
        if (!this.free()) {
            return false;
        }
        this.move = next;
        this.moveStart = this.age;
        this.flurry = false;
        if (next.kind() == SwordMove.Kind.BASH) {
            this.sound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.7F, 0.8F);
        } else {
            this.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.6F, 1.3F + 0.3F * this.owner.getRandom().nextFloat());
        }
        return true;
    }

    private boolean startFlurry() {
        if (!this.free()) {
            return false;
        }
        float cost = (float) wheel().value("flurryPowerCost");
        float power = PowerRing.power(this.owner);
        if (power + 1.0E-4F < cost) {
            PowerRing.tell(this.owner, "no_power");
            return false;
        }
        PowerRing.setPower(this.owner, power - cost);
        this.move = SwordMove.FLURRY;
        this.moveStart = this.age;
        this.flurry = true;
        this.sound(SoundEvents.SHIELD_BLOCK, 0.8F, 1.2F);
        this.sound(SoundEvents.BEACON_POWER_SELECT, 0.5F, 2.0F);
        return true;
    }

    private boolean stopFlurry() {
        if (!this.flurry) {
            return false;
        }
        this.flurry = false;
        // Let go early: the flurry is over there and then.
        if (this.move == SwordMove.FLURRY && this.age - this.moveStart < SwordMove.FLURRY.ticks()) {
            this.moveStart = this.age - SwordMove.FLURRY.ticks();
        }
        return true;
    }

    private boolean startCharge() {
        if (!this.free() || Flight.flying(this.owner)) {
            return false;
        }
        float cost = (float) wheel().value("chargePowerCost");
        float power = PowerRing.power(this.owner);
        if (power + 1.0E-4F < cost) {
            PowerRing.tell(this.owner, "no_power");
            return false;
        }
        PowerRing.setPower(this.owner, power - cost);
        this.move = SwordMove.CHARGE;
        this.moveStart = this.age;
        this.charging = true;
        this.way = flat(this.owner.getLookAngle());
        this.shoved.clear();
        this.sound(SoundEvents.ARMOR_EQUIP_NETHERITE.value(), 1.0F, 0.7F);
        this.sound(SoundEvents.RAVAGER_ROAR, 0.35F, 1.6F);
        return true;
    }

    /** The charge ends (he let go, or ran into a wall): he slams the shield into the ground before him. */
    private boolean stopCharge() {
        if (!this.charging) {
            return false;
        }
        this.charging = false;
        this.move = SwordMove.SLAM;
        this.moveStart = this.age;
        return true;
    }

    @Override
    public boolean tick(ServerLevel level, int tick) {
        if (this.breaking >= 0) {
            this.breaking++;
            if (this.breaking >= BREAK_TICKS) {
                PacketDistributor.sendToPlayersInDimension(level, ConstructPayload.remove(this.id));
                HELD.remove(this.owner.getUUID(), this);
                return false;
            }
            this.send(level);
            return true;
        }
        if (HELD.get(this.owner.getUUID()) != this || !PowerRing.fuels(this.owner, level)) {
            this.breakUp();
            this.send(level);
            return true;
        }
        this.age++;
        int t = this.age - this.moveStart;
        switch (this.move.kind()) {
            case ATTACK, BASH, SLAM -> {
                int[] hits = this.move.hits();
                for (int k = 0; k < hits.length; k++) {
                    if (t == hits[k]) {
                        this.strike(level);
                    }
                }
                if (this.move == SwordMove.LUNGE && t == 6) {
                    // The step of the lunge: he is thrown forward a little way.
                    Vec3 ahead = flat(this.owner.getLookAngle());
                    this.owner.setDeltaMovement(this.owner.getDeltaMovement().add(ahead.scale(0.75)).add(0.0, 0.06,
                            0.0));
                    this.owner.hurtMarked = true;
                }
            }
            case FLURRY -> {
                int stab = (t - SwordMove.FIRST_STAB) / SwordMove.STAB_EVERY;
                if (this.flurry && t >= SwordMove.FIRST_STAB && (t - SwordMove.FIRST_STAB) % SwordMove.STAB_EVERY == 0
                        && stab < SwordMove.STABS) {
                    this.stab(level, stab);
                }
                if (t >= SwordMove.FLURRY.ticks()) {
                    this.flurry = false;
                }
            }
            case CHARGE -> this.charge(level, t);
            case EQUIP -> this.equipping(t);
        }
        this.send(level);
        return true;
    }

    /** The sounds of their taking shape: the toss, the catch, and two knocks on the shield. */
    private void equipping(int t) {
        switch (t) {
            case 10 -> this.sound(SoundEvents.TRIDENT_THROW.value(), 0.6F, 1.5F);
            case 20 -> this.sound(SoundEvents.ARMOR_EQUIP_IRON.value(), 0.9F, 1.3F);
            case 24, 28 -> {
                this.sound(SoundEvents.SHIELD_BLOCK, 0.8F, t == 24 ? 1.3F : 1.5F);
                this.sound(SoundEvents.AMETHYST_BLOCK_HIT, 0.8F, t == 24 ? 1.1F : 1.3F);
            }
            default -> {
            }
        }
    }

    /**
     * A cut, a thrust or a bash lands: everything fair game in its arc and within its reach, not behind a wall, is
     * struck and thrown the way the move goes. The end of a charge slams the shield into the ground instead.
     */
    private void strike(ServerLevel level) {
        CharacterAbility wheel = wheel();
        if (this.move.kind() == SwordMove.Kind.SLAM) {
            this.slam(level, wheel);
            return;
        }
        boolean bash = this.move.kind() == SwordMove.Kind.BASH;
        Vec3 origin = this.owner.position().add(0.0, this.owner.getBbHeight() * CHEST, 0.0);
        Vec3 look = flat(this.owner.getLookAngle());
        Vec3 right = new Vec3(-look.z, 0.0, look.x);
        double damage = (bash ? wheel.value("bashDamage") : wheel.value("swordDamage")) * this.move.power();
        double reach = this.move.reach() * (bash ? 1.0 : wheel.value("swordReach") / 3.2);
        int struck = 0;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(origin, origin)
                .inflate(reach + 1.5), this::fair)) {
            Vec3 middle = target.getBoundingBox().getCenter();
            Vec3 to = middle.subtract(origin);
            double flat = Math.sqrt(to.x * to.x + to.z * to.z) - target.getBbWidth() * 0.5;
            if (flat > reach || Math.abs(to.y) > TALL + target.getBbHeight() * 0.5) {
                continue;
            }
            double angle = Math.toDegrees(Math.atan2(to.dot(right), to.dot(look)));
            if (flat > CLOSE && !this.move.inArc(angle)) {
                continue;
            }
            if (level.clip(new ClipContext(origin, middle, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                    this.owner)).getType() != HitResult.Type.MISS) {
                continue;
            }
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().playerAttack(this.owner), (float) damage);
            push(target, this.move, look, right, to, bash ? wheel.value("bashKnockback") : 0.35);
            level.sendParticles(bash ? ParticleTypes.CRIT : ParticleTypes.ENCHANTED_HIT, middle.x, middle.y, middle.z,
                    8, 0.2, 0.2, 0.2, 0.25);
            struck++;
        }
        Vec3 front = origin.add(look.scale(1.6));
        if (bash) {
            this.sound(struck > 0 ? SoundEvents.SHIELD_BLOCK : SoundEvents.PLAYER_ATTACK_NODAMAGE, 1.0F,
                    struck > 0 ? 0.8F : 1.2F);
            if (struck > 0) {
                this.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8F, 1.4F);
            }
        } else {
            if (this.move != SwordMove.STAB && this.move != SwordMove.LUNGE) {
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, front.x, front.y, front.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
            this.sound(struck > 0 ? SoundEvents.PLAYER_ATTACK_STRONG : SoundEvents.PLAYER_ATTACK_SWEEP, 0.9F,
                    struck > 0 ? 1.0F : 1.5F);
            if (struck > 0) {
                this.sound(SoundEvents.AMETHYST_BLOCK_HIT, 1.0F, 1.2F);
            }
        }
        SpellFx.cloud(level, SpellFx.dust(PowerRing.BRIGHT, 0.8F), front, 5, 0.3, 0.05);
    }

    /**
     * Throws a creature the move struck the way the move goes: a cut across sweeps it aside, an uppercut or an upward
     * bash throws it up, a bash from above knocks it down (and slows it a moment), anything else straight away.
     */
    private static void push(LivingEntity target, SwordMove move, Vec3 look, Vec3 right, Vec3 to, double strength) {
        Vec3 away = new Vec3(to.x, 0.0, to.z);
        away = away.lengthSqr() < 1.0E-4 ? look : away.normalize();
        Vec3 push = switch (move) {
            case SLASH, LOW_SWEEP -> away.subtract(right).normalize();
            case BACKHAND -> away.add(right).normalize();
            case UPPERCUT -> away.scale(0.3).add(0.0, 1.0, 0.0);
            case OVERHEAD -> away.scale(0.6).add(0.0, -0.2, 0.0);
            case BASH_SWEEP -> right.add(away.scale(0.5)).normalize();
            case BASH_BACKHAND -> right.scale(-1.0).add(away.scale(0.5)).normalize();
            case BASH_UP -> away.scale(0.4).add(0.0, 1.1, 0.0);
            case BASH_DOWN -> away.add(0.0, -0.1, 0.0);
            default -> away;
        };
        double resist = Mth.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
        double lift = move == SwordMove.BASH_DOWN || move == SwordMove.OVERHEAD ? 0.0 : 0.15;
        target.setDeltaMovement(target.getDeltaMovement().add(push.scale(strength * (1.0 - resist)))
                .add(0.0, lift * strength * (1.0 - resist), 0.0));
        target.hasImpulse = true;
        target.hurtMarked = true;
        if (move == SwordMove.BASH_DOWN) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
        }
    }

    /** One stab of the flurry: straight along its own way, striking the first few creatures along it. */
    private void stab(ServerLevel level, int k) {
        CharacterAbility wheel = wheel();
        double[] turn = SwordMove.stab(k);
        Vec3 way = Vec3.directionFromRotation(this.owner.getXRot() * 0.3F - (float) turn[1],
                this.owner.getYRot() + (float) turn[0]);
        Vec3 origin = this.owner.position().add(0.0, this.owner.getBbHeight() * CHEST, 0.0);
        Vec3 tip = origin.add(way.scale(SwordMove.FLURRY.reach() * wheel.value("swordReach") / 3.2));
        boolean hit = false;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(origin, tip).inflate(1.0),
                this::fair)) {
            if (target.getBoundingBox().inflate(STAB_WIDE).clip(origin, tip).isEmpty()) {
                continue;
            }
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().playerAttack(this.owner), (float) wheel.value("flurryDamage"));
            target.setDeltaMovement(target.getDeltaMovement().add(way.x * 0.12, 0.02, way.z * 0.12));
            target.hurtMarked = true;
            Vec3 at = target.getBoundingBox().getCenter();
            level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 4, 0.15, 0.15, 0.15, 0.2);
            hit = true;
        }
        this.sound(hit ? SoundEvents.PLAYER_ATTACK_CRIT : SoundEvents.PLAYER_ATTACK_WEAK, 0.55F,
                1.4F + 0.05F * k);
    }

    /** A tick of the charge: whoever stands in his way is shoved aside to the side of him it stood on, with a light hit. */
    private void charge(ServerLevel level, int t) {
        CharacterAbility wheel = wheel();
        if (t >= Math.round(wheel.value("chargeSeconds") * 20.0) + 4) {
            // His own game stops it by itself; this only makes sure a charge never runs on for ever.
            this.stopCharge();
            return;
        }
        Vec3 at = this.owner.position();
        Vec3 right = new Vec3(-this.way.z, 0.0, this.way.x);
        AABB box = this.owner.getBoundingBox().expandTowards(this.way.scale(CHARGE_AHEAD)).inflate(CHARGE_WIDE, 0.3,
                CHARGE_WIDE);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, this::fair)) {
            Vec3 to = target.position().subtract(at);
            if (to.dot(this.way) < -0.4 || this.shoved.contains(target.getId())) {
                continue;
            }
            this.shoved.add(target.getId());
            double side = to.dot(right);
            double sign = Math.abs(side) < 0.05 ? (this.owner.getRandom().nextBoolean() ? 1.0 : -1.0) : Math.signum(side);
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().playerAttack(this.owner), (float) wheel.value("chargeDamage"));
            double resist = Mth.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
            Vec3 push = right.scale(sign * 1.05).add(this.way.scale(0.35)).add(0.0, 0.32, 0.0).scale(1.0 - resist);
            target.setDeltaMovement(target.getDeltaMovement().add(push));
            target.hasImpulse = true;
            target.hurtMarked = true;
            Vec3 middle = target.getBoundingBox().getCenter();
            level.sendParticles(ParticleTypes.CRIT, middle.x, middle.y, middle.z, 10, 0.3, 0.3, 0.3, 0.3);
            this.sound(SoundEvents.SHIELD_BLOCK, 1.0F, 0.7F);
            this.sound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0F, 0.9F);
        }
        if (t % 3 == 0) {
            BlockState ground = level.getBlockState(BlockPos.containing(at.subtract(0.0, 0.2, 0.0)));
            if (!ground.isAir()) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), at.x, at.y + 0.1, at.z, 4,
                        0.3, 0.05, 0.3, 0.1);
            }
            SpellFx.cloud(level, SpellFx.dust(PowerRing.GREEN, 1.0F), at.add(0.0, 0.9, 0.0).add(this.way.scale(0.9)),
                    3, 0.3, 0.02);
        }
    }

    /**
     * The end of a charge: the shield slammed into the ground before him. A small shockwave runs out over the ground:
     * what stands in it is hurt (most in the middle) and thrown away.
     */
    private void slam(ServerLevel level, CharacterAbility wheel) {
        Vec3 at = this.owner.position().add(flat(this.owner.getLookAngle()).scale(SLAM_AHEAD));
        double radius = wheel.value("slamRadius");
        double damage = wheel.value("slamDamage");
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius + 1.0,
                2.5, radius + 1.0), this::fair)) {
            Vec3 to = target.position().subtract(at);
            double flat = Math.sqrt(to.x * to.x + to.z * to.z);
            if (flat > radius + target.getBbWidth() * 0.5 || Math.abs(to.y) > 2.0) {
                continue;
            }
            double near = 1.0 - 0.5 * Mth.clamp(flat / Math.max(0.1, radius), 0.0, 1.0);
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().playerAttack(this.owner), (float) (damage * near));
            Vec3 away = flat < 1.0E-3 ? Vec3.ZERO : new Vec3(to.x / flat, 0.0, to.z / flat);
            double resist = Mth.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
            target.setDeltaMovement(target.getDeltaMovement().add(away.scale(1.0 * near * (1.0 - resist)))
                    .add(0.0, 0.4 * near * (1.0 - resist), 0.0));
            target.hasImpulse = true;
            target.hurtMarked = true;
        }
        BlockState ground = level.getBlockState(BlockPos.containing(at.subtract(0.0, 0.2, 0.0)));
        if (!ground.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), at.x, at.y + 0.1, at.z, 40,
                    radius * 0.4, 0.1, radius * 0.4, 0.3);
        }
        SpellFx.shockwave(level, SpellFx.dust(PowerRing.GREEN, 1.8F), at.add(0.0, 0.2, 0.0), 56, 0.5);
        SpellFx.shockwave(level, SpellFx.dust(PowerRing.PALE, 1.3F), at.add(0.0, 0.4, 0.0), 40, 0.35);
        this.sound(SoundEvents.ANVIL_LAND, 0.9F, 0.8F);
        this.sound(SoundEvents.GENERIC_EXPLODE.value(), 0.6F, 1.4F);
        this.sound(SoundEvents.AMETHYST_CLUSTER_BREAK, 1.1F, 0.9F);
    }

    /** He put them away, or can no longer hold them: they break into solid pieces. */
    private void breakUp() {
        if (this.breaking >= 0) {
            return;
        }
        this.breaking = 0;
        this.flurry = false;
        this.charging = false;
        this.sound(SoundEvents.AMETHYST_CLUSTER_BREAK, 0.9F, 1.3F);
    }

    /**
     * Who the sword and shield may strike: anything the ring may hurt, but never his own pets.
     */
    private boolean fair(LivingEntity living) {
        return PowerRing.canHit(this.owner, living)
                && !(living instanceof OwnableEntity pet && pet.getOwner() == this.owner);
    }

    /** The way flat along the ground, one long. */
    private static Vec3 flat(Vec3 way) {
        Vec3 flat = new Vec3(way.x, 0.0, way.z);
        return flat.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
    }

    /**
     * What everyone around needs to play it: which move it is, from which of its ticks, the way a charge goes, and how
     * far it has broken up ({@code size}: -1 while whole).
     */
    private void send(ServerLevel level) {
        Vec3 at = this.owner.position();
        PacketDistributor.sendToPlayersNear(level, null, at.x, at.y, at.z, VIEW_RANGE,
                new ConstructPayload(this.id, this.owner.getId(), at, this.charging ? this.way
                        : this.owner.getLookAngle(), this.breaking, 1.0F, this.moveStart, true, ConstructPayload.SWORD,
                        this.move.ordinal(), this.age, null));
    }

    private void sound(SoundEvent sound, float volume, float pitch) {
        this.owner.level().playSound(null, this.owner.getX(), this.owner.getY() + 1.0, this.owner.getZ(), sound,
                SoundSource.PLAYERS, volume, pitch);
    }

    /**
     * A hit on someone holding the shield up before him: the flurry's guard stops most of what comes from the front,
     * the shield locked before him in a charge nearly all of it. Damage that goes through armour anyway goes through.
     */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        SwordShield sword = HELD.get(player.getUUID());
        if (sword == null || sword.breaking >= 0 || event.getAmount() <= 0.0F
                || LightShield.goesThrough(event.getSource())) {
            return;
        }
        boolean guarding = sword.flurry && sword.move == SwordMove.FLURRY;
        if (!guarding && !sword.charging) {
            return;
        }
        Vec3 from = event.getSource().getSourcePosition();
        if (from != null) {
            Vec3 front = flat(player.getLookAngle());
            Vec3 toSource = new Vec3(from.x - player.getX(), 0.0, from.z - player.getZ());
            if (toSource.lengthSqr() > 1.0E-4 && front.dot(toSource.normalize()) < FRONT) {
                return;
            }
        }
        float kept = sword.charging ? CHARGE_KEPT : (float) wheel().value("guardDamageKept");
        event.setAmount(event.getAmount() * kept);
        sword.sound(SoundEvents.SHIELD_BLOCK, 1.0F, 1.0F);
    }
}
