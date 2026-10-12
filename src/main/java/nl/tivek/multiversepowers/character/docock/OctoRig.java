package nl.tivek.multiversepowers.character.docock;

import static nl.tivek.multiversepowers.character.docock.TentacleReach.COUNT;

import java.util.Arrays;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.engine.effect.Effect;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.entity.HeldMobs;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.target.Targeting;
import nl.tivek.multiversepowers.faction.Factions;

// One Doctor Octopus's tentacles on the server, for as long as he is him: which stand on the ground for his stance,
// which hold a creature (one each) and squeeze it, and the moves they make. A holding tentacle never goes down to the
// ground: an empty one goes instead, or the stance waits. A held creature that dies, goes or is left far behind is let
// go of by itself.
final class OctoRig implements Effect {
    private static final int SQUEEZE_EVERY = 10;
    // How much a hurl with no creature aimed at is lifted, so what is hurled flies in a bow.
    private static final double LOB = 0.3;

    private final ServerPlayer player;
    private int stance;
    private final Mob[] held = new Mob[COUNT];
    private final long[] grabbedAt = new long[COUNT];
    private final boolean[] ground = new boolean[COUNT];
    private final boolean[] squeezing = new boolean[COUNT];
    private final int[] move = new int[COUNT];
    private final int[] seq = new int[COUNT];
    private final int[] moveAge = new int[COUNT];
    private final Vec3[] way = new Vec3[COUNT];
    private final int[] target = new int[COUNT];
    private long clock;
    private boolean done;

    OctoRig(ServerPlayer player) {
        this.player = player;
        Arrays.fill(this.moveAge, -1);
        Arrays.fill(this.target, -1);
        Arrays.fill(this.way, Vec3.ZERO);
    }

    static CharacterAbility ability(String name) {
        return GameCharacter.DOC_OCK.byName(name);
    }

    boolean holds(Entity entity) {
        for (Mob mob : this.held) {
            if (mob == entity) {
                return true;
            }
        }
        return false;
    }

    int grounded() {
        int count = 0;
        for (boolean down : this.ground) {
            count += down ? 1 : 0;
        }
        return count;
    }

    void stop() {
        this.releaseAll();
        this.done = true;
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(this.player, OctoStatePayload.gone(this.player.getId()));
    }

    @Override
    public boolean tick(ServerLevel level, int age) {
        if (this.done) {
            return false;
        }
        if (this.player.isRemoved()) {
            this.releaseAll();
            return false;
        }
        this.clock++;
        if (age % 20 == 0) {
            DocOckPowers.reach(this.player, true);
        }
        boolean changed = false;
        for (int i = 0; i < COUNT; i++) {
            Mob mob = this.held[i];
            if (mob != null && (!mob.isAlive() || mob.isRemoved() || mob.level() != this.player.level()
                    || !this.reaches(mob))) {
                this.drop(i);
                changed = true;
                continue;
            }
            if (this.moveAge[i] >= 0) {
                this.moveAge[i]++;
                if (this.moveAge[i] == TentacleReach.STRIKE_TICK) {
                    this.strike(this.player.serverLevel(), i);
                }
                if (this.moveAge[i] > TentacleReach.MOVE_TICKS) {
                    this.moveAge[i] = -1;
                    this.move[i] = TentacleReach.NONE;
                    this.target[i] = -1;
                }
            }
            mob = this.held[i];
            if (mob != null) {
                this.place(i, mob);
                if (this.squeezing[i] && this.clock % SQUEEZE_EVERY == 0) {
                    this.hurt(mob, ability("squeeze").getDamage());
                    this.sound(mob.position(), SoundEvents.CHAIN_STEP, 0.6F);
                }
            }
        }
        if (changed) {
            this.assignGround();
            this.send();
        }
        return true;
    }

    private void place(int i, Mob mob) {
        Vec3 middle = this.holdPoint(i, mob).add(TentacleReach.swing(this.move[i], this.moveAge[i], this.way[i]));
        mob.setPos(middle.x, middle.y - mob.getBbHeight() * 0.5, middle.z);
        mob.setDeltaMovement(Vec3.ZERO);
        mob.resetFallDistance();
    }

    // Within a tentacle's length of his eye, as his own game measures it along the crosshair.
    private boolean reaches(Entity entity) {
        double most = TentacleReach.LENGTH + 0.5;
        return entity.getBoundingBox().distanceToSqr(this.player.getEyePosition()) <= most * most;
    }

    private Vec3 holdPoint(int i, Mob mob) {
        return TentacleReach.hold(this.player.position(), this.player.yBodyRot, i, mob.getBbWidth());
    }

    // The next stance in turn (4 free, 2 and 2, 1 and 3, all down); refused while too few tentacles are empty.
    void cycleStance() {
        int next = (this.stance + 1) % TentacleReach.STANCES;
        int empty = 0;
        for (int i = 0; i < COUNT; i++) {
            empty += this.held[i] == null ? 1 : 0;
        }
        if (empty < TentacleReach.GROUNDED[next]) {
            this.tell("too_full");
            return;
        }
        this.stance = next;
        this.assignGround();
        this.player.serverLevel().playSound(null, this.player.getX(), this.player.getY(), this.player.getZ(),
                SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS, 0.7F, 0.8F + 0.15F * next);
        this.send();
    }

    // The stance's tentacles go down in their order (3, 4, 1, 2), a holding one passed over for the next.
    private void assignGround() {
        int needed = TentacleReach.GROUNDED[this.stance];
        Arrays.fill(this.ground, false);
        for (int index : TentacleReach.GROUND_ORDER) {
            if (needed > 0 && this.held[index] == null) {
                this.ground[index] = true;
                needed--;
            }
        }
    }

    private boolean free(int i) {
        return !this.ground[i] && this.held[i] == null && this.moveAge[i] < 0;
    }

    void grab(int targetId, int mask) {
        Entity entity = this.player.serverLevel().getEntity(targetId);
        if (!(entity instanceof Mob mob) || this.holds(mob) || !Targeting.isTargetable(this.player, mob)
                || !Factions.mayHit(this.player, mob) || !this.reaches(mob)) {
            return;
        }
        int i = -1;
        for (int index = 0; index < COUNT && mask != 0; index++) {
            if ((mask & 1 << index) != 0 && this.free(index)) {
                i = index;
                break;
            }
        }
        for (int index : TentacleReach.GRAB_ORDER) {
            if (mask == 0 && i < 0 && this.free(index)) {
                i = index;
            }
        }
        if (i < 0) {
            this.tell("no_free");
            return;
        }
        if (!HeldMobs.hold(mob, true)) {
            return;
        }
        this.held[i] = mob;
        this.grabbedAt[i] = this.clock;
        this.start(i, TentacleReach.GRAB, Vec3.ZERO, targetId);
        this.sound(mob.position(), SoundEvents.CHAIN_PLACE, 1.2F);
        this.send();
    }

    void squeeze(int mask, boolean on) {
        for (int i = 0; i < COUNT; i++) {
            if (this.held[i] != null && (mask == 0 || (mask & 1 << i) != 0)) {
                this.squeezing[i] = on;
            } else if (!on) {
                this.squeezing[i] = false;
            }
        }
        this.send();
    }

    void slam(int mask, Vec3 toward) {
        if (toward.lengthSqr() < 1.0E-6) {
            return;
        }
        Vec3 unit = toward.normalize();
        for (int i = 0; i < COUNT; i++) {
            if (this.held[i] != null && this.moveAge[i] < 0 && (mask == 0 || (mask & 1 << i) != 0)) {
                this.start(i, TentacleReach.SLAM, unit, -1);
            }
        }
        this.send();
    }

    // Hurls what the tentacles hold the way he looks, lifted into a bow, or straight at the creature he aims at, each
    // tentacle lashing out after what it threw; whether anything flew.
    boolean hurl(int mask, int targetId) {
        Entity aimed = targetId < 0 ? null : this.player.serverLevel().getEntity(targetId);
        LivingEntity at = aimed instanceof LivingEntity living && !this.holds(living)
                && Factions.mayHit(this.player, living) ? living : null;
        CharacterAbility hurl = ability("throw");
        boolean any = false;
        for (int i = 0; i < COUNT; i++) {
            Mob mob = this.held[i];
            if (mob == null || mask != 0 && (mask & 1 << i) == 0) {
                continue;
            }
            this.drop(i);
            Vec3 way = at == null ? this.player.getLookAngle().add(0.0, LOB, 0.0).normalize()
                    : at.position().add(0.0, at.getBbHeight() * 0.5, 0.0).subtract(mob.position()).normalize();
            mob.setDeltaMovement(way.scale(hurl.value("speed") / 20.0));
            mob.hurtMarked = true;
            this.start(i, TentacleReach.THROW, way, -1);
            Effects.start(this.player.serverLevel(), new Flung(this.player, mob, at, hurl.getDamage()));
            any = true;
        }
        if (any) {
            this.assignGround();
            this.sound(this.player.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 0.8F);
            this.send();
        }
        return any;
    }

    // Lets go of what the tentacles in `mask` hold, or with none given of the last creature taken hold of.
    void release(int mask) {
        int last = -1;
        for (int i = 0; i < COUNT; i++) {
            if (this.held[i] == null) {
                continue;
            }
            if (mask != 0 && (mask & 1 << i) != 0) {
                this.drop(i);
            } else if (mask == 0 && (last < 0 || this.grabbedAt[i] > this.grabbedAt[last])) {
                last = i;
            }
        }
        if (last >= 0) {
            this.drop(last);
        }
        this.assignGround();
        this.send();
    }

    void releaseAll() {
        for (int i = 0; i < COUNT; i++) {
            this.drop(i);
        }
        this.assignGround();
        if (!this.done && !this.player.hasDisconnected()) {
            this.send();
        }
    }

    // A left click on a creature: chosen tentacles (`mask`) holding one swing it into the creature, two or more
    // smash theirs together; else a free tentacle slaps it.
    void strikeAt(int targetId, int mask) {
        int holding = 0;
        for (int i = 0; i < COUNT; i++) {
            if ((mask & 1 << i) != 0 && this.held[i] != null && this.moveAge[i] < 0) {
                holding |= 1 << i;
            }
        }
        if (Integer.bitCount(holding) >= 2) {
            this.clash(holding);
            return;
        }
        Entity entity = this.player.serverLevel().getEntity(targetId);
        if (!(entity instanceof LivingEntity living) || this.holds(living) || !Factions.mayHit(this.player, living)
                || !this.reaches(living)) {
            return;
        }
        Vec3 middle = living.position().add(0.0, living.getBbHeight() * 0.5, 0.0);
        if (holding != 0) {
            int i = Integer.numberOfTrailingZeros(holding);
            this.start(i, TentacleReach.WEAPON, middle.subtract(this.holdPoint(i, this.held[i])), targetId);
            this.send();
            return;
        }
        int i = -1;
        for (int index = 0; index < COUNT && mask != 0; index++) {
            if ((mask & 1 << index) != 0 && this.free(index)) {
                i = index;
                break;
            }
        }
        for (int index : TentacleReach.GRAB_ORDER) {
            if (mask == 0 && i < 0 && this.free(index)) {
                i = index;
            }
        }
        if (i < 0) {
            this.tell("no_free");
            return;
        }
        this.start(i, TentacleReach.SLAP, Vec3.ZERO, targetId);
        this.send();
    }

    private void clash(int holding) {
        Vec3 middle = Vec3.ZERO;
        int count = Integer.bitCount(holding);
        for (int i = 0; i < COUNT; i++) {
            if ((holding & 1 << i) != 0) {
                middle = middle.add(this.holdPoint(i, this.held[i]).scale(1.0 / count));
            }
        }
        for (int i = 0; i < COUNT; i++) {
            if ((holding & 1 << i) != 0) {
                Vec3 from = this.holdPoint(i, this.held[i]);
                Vec3 to = middle.subtract(from);
                double reach = Math.max(0.0, to.length() - this.held[i].getBbWidth() * 0.5);
                this.start(i, TentacleReach.CLASH, to.lengthSqr() < 1.0E-6 ? Vec3.ZERO : to.normalize().scale(reach),
                        -1);
            }
        }
        this.send();
    }

    private void start(int i, int kind, Vec3 way, int targetId) {
        this.move[i] = kind;
        this.moveAge[i] = 0;
        this.way[i] = way;
        this.target[i] = targetId;
        this.seq[i] = (this.seq[i] + 1) & 0xFF;
    }

    private void strike(ServerLevel level, int i) {
        Mob mob = this.held[i];
        switch (this.move[i]) {
            case TentacleReach.SLAP -> {
                if (level.getEntity(this.target[i]) instanceof LivingEntity living && living.isAlive()) {
                    this.hurt(living, ability("slap").getDamage());
                    Vec3 away = living.position().subtract(this.player.position()).multiply(1.0, 0.0, 1.0);
                    if (away.lengthSqr() > 1.0E-6) {
                        living.push(away.normalize().scale(0.6).add(0.0, 0.25, 0.0));
                        living.hurtMarked = true;
                    }
                    this.sound(living.position(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0F);
                }
            }
            case TentacleReach.SLAM -> {
                if (mob != null) {
                    this.hurt(mob, ability("grab").value("slamDamage"));
                    Vec3 at = mob.position();
                    BlockState below = level.getBlockState(mob.blockPosition().below());
                    if (!below.isAir()) {
                        ParticleFx.send(level, new BlockParticleOption(ParticleTypes.BLOCK, below), at.x, at.y, at.z,
                                20, 0.4, 0.1, 0.4, 0.15);
                    }
                    this.sound(at, SoundEvents.ANVIL_LAND, 0.6F);
                }
            }
            case TentacleReach.WEAPON -> {
                if (mob != null && level.getEntity(this.target[i]) instanceof LivingEntity living && living.isAlive()) {
                    double damage = ability("slap").value("weaponDamage");
                    this.hurt(living, damage);
                    this.hurt(mob, damage * 0.5);
                    this.sound(living.position(), SoundEvents.PLAYER_ATTACK_STRONG, 1.0F);
                }
            }
            case TentacleReach.CLASH -> {
                if (mob != null) {
                    this.hurt(mob, ability("slap").value("clashDamage"));
                    this.sound(mob.position(), SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, 0.9F);
                }
            }
            default -> {
            }
        }
    }

    // A held creature keeps its place: the blow's push is undone.
    private void hurt(LivingEntity entity, double damage) {
        if (damage <= 0.0 || !Factions.mayHit(this.player, entity)) {
            return;
        }
        entity.hurt(this.player.damageSources().playerAttack(this.player), (float) damage);
        if (this.holds(entity)) {
            entity.setDeltaMovement(Vec3.ZERO);
        }
    }

    private void drop(int i) {
        Mob mob = this.held[i];
        this.held[i] = null;
        this.squeezing[i] = false;
        if (this.move[i] != TentacleReach.SLAP) {
            this.moveAge[i] = -1;
            this.move[i] = TentacleReach.NONE;
        }
        if (mob != null) {
            HeldMobs.release(mob);
        }
    }

    private void tell(String key) {
        this.player.displayClientMessage(Component.translatable("octopus." + MultiversePowers.MODID + "." + key),
                true);
    }

    private void sound(Vec3 at, SoundEvent sound, float pitch) {
        this.player.serverLevel().playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, 1.0F, pitch);
    }

    void send() {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(this.player, this.state());
    }

    void showTo(ServerPlayer viewer) {
        PacketDistributor.sendToPlayer(viewer, this.state());
    }

    private OctoStatePayload state() {
        int[] flags = new int[COUNT];
        int[] heldIds = new int[COUNT];
        float[] ways = new float[COUNT * 3];
        for (int i = 0; i < COUNT; i++) {
            flags[i] = (this.ground[i] ? OctoStatePayload.GROUND : 0)
                    | (this.held[i] != null ? OctoStatePayload.HOLDING : 0)
                    | (this.squeezing[i] ? OctoStatePayload.SQUEEZING : 0);
            heldIds[i] = this.held[i] == null ? -1 : this.held[i].getId();
            ways[i * 3] = (float) this.way[i].x;
            ways[i * 3 + 1] = (float) this.way[i].y;
            ways[i * 3 + 2] = (float) this.way[i].z;
        }
        return new OctoStatePayload(this.player.getId(), this.stance, flags, heldIds, this.move.clone(),
                this.seq.clone(), ways, this.target.clone());
    }
}
