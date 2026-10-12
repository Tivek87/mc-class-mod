package nl.tivek.multiversepowers.creature.wizard;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.MonsterBlows;
import nl.tivek.multiversepowers.creature.MovingMonster;
import nl.tivek.multiversepowers.creature.wizard.spell.ArcaneBolt;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// The circle of mirrors, his third phase's ultimate: sixteen images of himself burst out round his foe and he hides
// among them, seventeen alike racing round them faster and faster. One after another dashes in to strike and back,
// the others flick sparks from their staffs; at the end they all close in at once and burst. Only the real one casts a
// shadow and his crystal burns his phase's gold (client/WizardRenderer): strike him and every image shatters and he
// is stunned. Strike an image and it shatters.
final class WizardMirrors {
    private static final int CLONES = 16;
    private static final int RACE = 200;
    private static final int CLOSE = 14;
    private static final int DASH = 12;
    private static final double RADIUS = 6.5;
    private static final double DASH_REACH = 1.9;

    private final ServerLevel level;
    private final Wizard wizard;
    // Everyone in the circle, the wizard at `real`.
    private final List<MovingMonster> members = new ArrayList<>();
    private final int real;
    private int t;
    private double spin;
    private int dasher = -1;
    private int dashT;
    private int sparker = -1;
    private int sparkAt;
    private boolean ended;

    private WizardMirrors(ServerLevel level, Wizard wizard, int real) {
        this.level = level;
        this.wizard = wizard;
        this.real = real;
    }

    static WizardMirrors begin(ServerLevel level, Wizard wizard, LivingEntity target) {
        int count = CLONES + 1;
        WizardMirrors mirrors = new WizardMirrors(level, wizard, wizard.getRandom().nextInt(count));
        mirrors.spin = wizard.getRandom().nextDouble() * Mth.TWO_PI;
        for (int i = 0; i < count; i++) {
            if (i == mirrors.real) {
                mirrors.members.add(wizard);
                continue;
            }
            WizardDecoy clone = WizardDecoy.image(level, wizard, mirrors.place(i, target));
            if (clone != null) {
                mirrors.members.add(clone);
            }
        }
        wizard.setNoGravity(true);
        for (MovingMonster member : mirrors.members) {
            member.start(WizardMove.GLIDE);
        }
        Vec3 middle = target.position().add(0.0, 1.0, 0.0);
        ParticleFx.sphereOut(level, ParticleTypes.ELECTRIC_SPARK, middle, 30, 0.6);
        ParticleFx.shockwave(level, Wizard.dust(Wizard.STARLIGHT, 2.0F), middle, 72, 0.8);
        WizardSparks.blink(level, wizard.position(), mirrors.place(mirrors.real, target));
        level.playSound(null, middle.x, middle.y, middle.z, SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.HOSTILE,
                2.0F, 0.6F);
        level.playSound(null, middle.x, middle.y, middle.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE,
                2.0F, 0.5F);
        mirrors.place(target);
        return mirrors;
    }

    // Whether they still race round (and so whether a blow on him finds him out).
    boolean racing() {
        return !this.ended && this.t < RACE;
    }

    // Moves everyone on; false once it is over.
    boolean tick(@Nullable LivingEntity target) {
        if (this.ended) {
            return false;
        }
        if (target == null || !target.isAlive() || !this.wizard.isAlive()) {
            this.end();
            return false;
        }
        this.t++;
        this.spin += 0.05 + 0.06 * Math.min(1.0, this.t / 80.0);
        if (this.t < RACE) {
            this.attack(target);
        } else if (this.t == RACE) {
            for (MovingMonster member : this.members) {
                member.start(WizardMove.DASH);
            }
            this.level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ILLUSIONER_CAST_SPELL,
                    SoundSource.HOSTILE, 2.0F, 0.5F);
        }
        this.place(target);
        if (this.t >= RACE + CLOSE) {
            this.finale(target);
            return false;
        }
        return true;
    }

    // One dashes in and back now and then; another flicks a spark.
    private void attack(LivingEntity target) {
        int every = this.wizard.phase() > 2 ? 12 : 14;
        if (this.dasher < 0 && this.t % every == 0) {
            int pick = this.wizard.getRandom().nextInt(this.members.size());
            if (this.members.get(pick).isAlive() && pick != this.sparker) {
                this.dasher = pick;
                this.dashT = 0;
                this.members.get(pick).start(WizardMove.DASH);
            }
        }
        if (this.dasher >= 0) {
            this.dashT++;
            MovingMonster member = this.members.get(this.dasher);
            if (!member.isAlive()) {
                this.dasher = -1;
                return;
            }
            if (this.dashT == WizardMove.DASH_HIT && member.distanceTo(target) < DASH_REACH + target.getBbWidth()) {
                Vec3 way = MonsterBlows.toward(this.wizard, member.position(), target.position());
                MonsterBlows.hurt(target, this.wizard.damageSources().mobAttack(this.wizard),
                        this.wizard.harm(WizardMove.DASH), WizardKnock.knock(way.scale(0.7).add(0.0, 0.25, 0.0)));
                member.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 1.2F);
            }
            if (this.dashT >= DASH) {
                member.start(WizardMove.GLIDE);
                this.dasher = -1;
            }
        }
        if (this.sparker < 0 && this.t % 9 == 4) {
            int pick = this.wizard.getRandom().nextInt(this.members.size());
            if (pick != this.dasher && this.members.get(pick).isAlive()) {
                this.sparker = pick;
                this.sparkAt = this.t + WizardMove.release(WizardMove.SPARK);
                this.members.get(pick).start(WizardMove.SPARK);
            }
        }
        if (this.sparker >= 0 && this.t >= this.sparkAt) {
            MovingMonster member = this.members.get(this.sparker);
            if (member.isAlive()) {
                ArcaneBolt.spark(this.level, this.wizard, WizardDecoy.tip(member, this.wizard.hover()), target,
                        this.wizard.harm(WizardMove.SPARK) * 0.6F);
            }
            if (this.t >= this.sparkAt + 6) {
                if (member.isAlive()) {
                    member.start(WizardMove.GLIDE);
                }
                this.sparker = -1;
            }
        }
    }

    // Everyone where they are now on the circle, facing the middle; a dasher on its way in and back; at the end the
    // circle closes in.
    private void place(LivingEntity target) {
        for (int i = 0; i < this.members.size(); i++) {
            MovingMonster member = this.members.get(i);
            if (!member.isAlive()) {
                continue;
            }
            Vec3 at = this.place(i, target);
            if (i == this.dasher) {
                double f = this.dashT < WizardMove.DASH_HIT ? this.dashT / (double) WizardMove.DASH_HIT
                        : 1.0 - (this.dashT - WizardMove.DASH_HIT) / (double) (DASH - WizardMove.DASH_HIT);
                Vec3 into = target.position().add(at.subtract(target.position()).normalize().scale(1.1));
                at = at.lerp(into, Mth.clamp(f, 0.0, 1.0));
            }
            member.setPos(at.x, target.getY(), at.z);
            member.setDeltaMovement(Vec3.ZERO);
            face(member, target.position());
        }
    }

    private Vec3 place(int i, LivingEntity target) {
        double radius = RADIUS + 1.2 * Math.sin(this.t * 0.07);
        if (this.t > RACE) {
            radius *= 1.0 - 0.8 * Math.min(1.0, (this.t - RACE) / (double) CLOSE);
        }
        double angle = this.spin + i * Mth.TWO_PI / (CLONES + 1);
        return target.position().add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
    }

    private static void face(Mob member, Vec3 at) {
        double dx = at.x - member.getX();
        double dz = at.z - member.getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        member.setYRot(yaw);
        member.yBodyRot = yaw;
        member.yHeadRot = yaw;
    }

    // They all close in at once and burst round the foe; he is somewhere else again.
    private void finale(LivingEntity target) {
        Vec3 middle = target.position().add(0.0, 1.0, 0.0);
        for (LivingEntity living : MonsterBlows.foes(this.wizard, target.getBoundingBox().inflate(3.0))) {
            Vec3 away = MonsterBlows.toward(this.wizard, middle, living.position());
            MonsterBlows.hurt(living, Wizard.source(this.wizard, this.wizard, WizardEntities.ARCANE),
                    this.wizard.harm(WizardMove.MIRRORS) * 2.5F, WizardKnock.knock(away.scale(1.2).add(0.0, 0.5, 0.0)));
        }
        ParticleFx.sphereOut(this.level, ParticleTypes.ELECTRIC_SPARK, middle, 30, 0.6);
        ParticleFx.sphereOut(this.level, Wizard.dust(Wizard.STARLIGHT, 2.0F), middle, 90, 0.7);
        this.level.playSound(null, middle.x, middle.y, middle.z, SoundEvents.AMETHYST_CLUSTER_BREAK,
                SoundSource.HOSTILE, 2.5F, 0.5F);
        this.end();
        Vec3 from = this.wizard.position();
        WizardCasts.teleportNear(this.wizard, target.position(), target, from);
        WizardSparks.blink(this.level, from, this.wizard.position());
    }

    // Found out: every image shatters, he drops where he is.
    void foundOut() {
        Vec3 at = this.wizard.position().add(0.0, 1.0, 0.0);
        ParticleFx.sphereOut(this.level, ParticleTypes.ELECTRIC_SPARK, at, 30, 0.6);
        this.level.playSound(null, at.x, at.y, at.z, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2.0F, 0.6F);
        this.end();
    }

    void end() {
        if (this.ended) {
            return;
        }
        this.ended = true;
        this.wizard.setNoGravity(this.wizard.flies());
        for (MovingMonster member : this.members) {
            if (member instanceof WizardDecoy clone && clone.isAlive()) {
                clone.shatter(null);
            }
        }
    }
}
