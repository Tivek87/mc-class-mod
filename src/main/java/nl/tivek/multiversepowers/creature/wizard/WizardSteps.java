package nl.tivek.multiversepowers.creature.wizard;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.MonsterBlows;
import nl.tivek.multiversepowers.creature.wizard.close.StaffBlows;
import nl.tivek.multiversepowers.creature.wizard.rift.WizardPortal;
import nl.tivek.multiversepowers.creature.wizard.spell.ArcaneBolt;
import nl.tivek.multiversepowers.creature.wizard.staff.WizardStaff;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;

// The Wizard's ways of going and his staff's moves, on the server. The flicker: three short hops round his foe, a spark
// flicked at them after each, no image left. The rift step: a rift opens before him and another by his foe, and he
// walks into the one and out of the other. The veil: a solid image of him stands on where he was while he is unseen,
// then he is behind his foe and swings his staff. The rift volley: rifts open round his foe and sparks fly out of
// them. His staff: hurled as a spear, called home, set free to fight on its own (staff/WizardStaff).
final class WizardSteps {
    // The rifts of a rift volley, their sparks every VOLLEY_EVERY ticks from just after they open.
    private static final int VOLLEY_RIFTS = 4;
    private static final int VOLLEY_EVERY = 5;
    private static final int VOLLEY_STOP = 22;
    private final Wizard wizard;
    private final WizardFlight flight;
    private final List<WizardPortal> rifts = new ArrayList<>();
    // Where the rift step comes out, and the way it faces.
    @Nullable
    private Vec3 stepTo;
    private Vec3 stepWay = Vec3.ZERO;

    WizardSteps(Wizard wizard, WizardFlight flight) {
        this.wizard = wizard;
        this.flight = flight;
    }

    // The moves whose release is done here.
    static boolean takes(int kind) {
        return switch (kind) {
            case WizardMove.STAFF_THROW, WizardMove.STAFF_CALL, WizardMove.FLICKER,
                    WizardMove.RIFT_STEP, WizardMove.VEIL, WizardMove.RIFT_VOLLEY -> true;
            default -> false;
        };
    }

    void release(ServerLevel level, int kind, @Nullable LivingEntity target) {
        switch (kind) {
            case WizardMove.STAFF_THROW -> WizardStaff.hurl(level, this.wizard, target);
            case WizardMove.STAFF_CALL -> {
                WizardStaff staff = WizardStaff.of(this.wizard);
                if (staff != null) {
                    staff.call();
                }
            }
            case WizardMove.FLICKER -> this.hop(level, target);
            case WizardMove.RIFT_STEP -> this.stepIn(level);
            case WizardMove.VEIL -> this.vanish(level);
            case WizardMove.RIFT_VOLLEY -> this.openVolley(level, target);
            default -> {
            }
        }
    }

    void during(ServerLevel level, int kind, int t, @Nullable LivingEntity target) {
        int release = WizardMove.release(kind);
        switch (kind) {
            case WizardMove.FLICKER -> {
                int k = (t - release) / WizardMove.FLICKER_EVERY;
                if (t > release && (t - release) % WizardMove.FLICKER_EVERY == 0 && k < WizardMove.FLICKERS) {
                    this.hop(level, target);
                }
            }
            case WizardMove.RIFT_STEP -> {
                if (t == 8) {
                    this.openStep(level, target);
                } else if (t == WizardMove.RIFT_OUT) {
                    this.stepOut(level, target);
                }
            }
            case WizardMove.VEIL -> {
                if (t == WizardMove.VEIL_BACK) {
                    this.behind(level, target);
                } else if (t == WizardMove.VEIL_STRIKE) {
                    this.veilStrike(level);
                }
            }
            case WizardMove.RIFT_VOLLEY -> {
                if (target != null && t > release && t < release + VOLLEY_STOP && (t - release) % VOLLEY_EVERY == 2) {
                    for (WizardPortal rift : this.rifts) {
                        ArcaneBolt.spark(level, this.wizard, rift.position().add(rift.facing().scale(0.3)), target,
                                this.wizard.harm(WizardMove.RIFT_VOLLEY));
                    }
                }
            }
            case WizardMove.WATCH -> {
                if (t == 2 && this.wizard.getRandom().nextBoolean()) {
                    this.wizard.playSound(SoundEvents.EVOKER_AMBIENT, 0.9F, 0.65F);
                }
            }
            default -> {
            }
        }
    }

    // The move is over or cut short: he is seen again and his rifts close.
    void end(int kind) {
        if (kind == WizardMove.RIFT_STEP || kind == WizardMove.VEIL) {
            this.wizard.setInvisible(false);
        }
        if (kind == WizardMove.RIFT_STEP || kind == WizardMove.RIFT_VOLLEY) {
            for (WizardPortal rift : this.rifts) {
                rift.close();
            }
            this.rifts.clear();
            this.stepTo = null;
        }
    }

    // One hop of the flicker: a few blocks round his foe, then a spark at them.
    private void hop(ServerLevel level, @Nullable LivingEntity target) {
        Vec3 from = this.wizard.position();
        if (this.wizard.flies()) {
            this.flight.hop(level, target);
        } else {
            WizardCasts.teleportNear(this.wizard, target == null ? from : target.position(), target, from, 5.0, 11.0);
        }
        Vec3 to = this.wizard.position();
        WizardSparks.blink(level, from.add(0.0, this.wizard.hover(), 0.0), to.add(0.0, this.wizard.hover(), 0.0));
        level.playSound(null, from.x, from.y, from.z, SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.HOSTILE, 0.9F,
                1.6F);
        if (target != null && target.isAlive()) {
            MonsterBlows.face(this.wizard, target.position().subtract(to));
            ArcaneBolt.spark(level, this.wizard, this.wizard.staffTip(), target,
                    this.wizard.harm(WizardMove.SPARK) * 0.8F);
        }
    }

    // A rift before him, facing him, and another by his foe facing them, out of which he will step.
    private void openStep(ServerLevel level, @Nullable LivingEntity target) {
        int style = WizardPortal.styleOf(this.wizard.phase());
        Vec3 here = this.wizard.at(0.0, 1.1, 1.3);
        this.rifts.add(WizardPortal.open(level, here, this.wizard.yBodyRot + 180.0F, 1.15F, 30, style, true));
        Vec3 middle = target == null ? this.wizard.position() : target.position();
        Vec3 to = this.wizard.flies()
                ? this.flight.airSpot(level, middle, 6.0, 12.0, 2.0, 5.0, target, 5.0)
                : this.groundSpot(level, middle, target);
        if (to == null) {
            return;
        }
        Vec3 way = middle.subtract(to).multiply(1.0, 0.0, 1.0);
        this.stepWay = way.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : way.normalize();
        this.stepTo = to;
        float yaw = (float) (Mth.atan2(-this.stepWay.x, this.stepWay.z) * Mth.RAD_TO_DEG);
        this.rifts.add(WizardPortal.open(level, to.add(0.0, 1.1 + this.wizard.hover(), 0.0), yaw, 1.15F, 34, style,
                true));
    }

    // A spot on the ground 6 to 12 blocks from `middle` with room for him, in sight of his foe; null if none.
    @Nullable
    private Vec3 groundSpot(ServerLevel level, Vec3 middle, @Nullable LivingEntity target) {
        for (int tries = 0; tries < 16; tries++) {
            double angle = this.wizard.getRandom().nextDouble() * Mth.TWO_PI;
            double far = 6.0 + this.wizard.getRandom().nextDouble() * 6.0;
            Vec3 near = Wizard.inReach(this.wizard.position(), middle.add(Math.cos(angle) * far, 0.0,
                    Math.sin(angle) * far));
            Vec3 spot = WizardCasts.ground(level, near.add(0.0, 3.0, 0.0));
            if (Math.abs(spot.y - middle.y) > 4.0
                    || spot.distanceTo(this.wizard.position()) > Wizard.TELEPORT_RANGE
                    || !level.isLoaded(BlockPos.containing(spot))
                    || !level.noCollision(this.wizard, this.wizard.getBoundingBox().move(spot.subtract(
                            this.wizard.position())))) {
                continue;
            }
            if (target == null || this.sees(level, spot.add(0.0, 1.6, 0.0), target)) {
                return spot;
            }
        }
        return null;
    }

    private boolean sees(ServerLevel level, Vec3 eye, LivingEntity target) {
        return LoadedWorld.clip(level, new ClipContext(eye, target.getEyePosition(), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this.wizard)).getType() == HitResult.Type.MISS;
    }

    // Into the rift before him: gone, and already just behind the other.
    private void stepIn(ServerLevel level) {
        if (this.stepTo == null) {
            return;
        }
        Vec3 from = this.wizard.position().add(0.0, 1.0 + this.wizard.hover(), 0.0);
        ParticleFx.implosion(level, Wizard.dust(Wizard.light(this.wizard.phase()), 1.3F), from, 1.6, 24, 0.3);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 0.7F);
        this.wizard.setInvisible(true);
        Vec3 behind = this.stepTo.subtract(this.stepWay.scale(0.8));
        this.wizard.teleportTo(behind.x, behind.y, behind.z);
        this.wizard.setDeltaMovement(Vec3.ZERO);
    }

    // Out of the other rift, facing his foe.
    private void stepOut(ServerLevel level, @Nullable LivingEntity target) {
        if (this.stepTo == null) {
            return;
        }
        Vec3 to = this.stepTo;
        this.wizard.teleportTo(to.x, to.y, to.z);
        this.wizard.setInvisible(false);
        if (target != null) {
            MonsterBlows.face(this.wizard, target.position().subtract(to));
        }
        Vec3 at = to.add(0.0, 1.0 + this.wizard.hover(), 0.0);
        ParticleFx.sphereOut(level, Wizard.dust(Wizard.light(this.wizard.phase()), 1.4F), at, 30, 0.25);
        ParticleFx.send(level, ParticleTypes.REVERSE_PORTAL, at.x, at.y, at.z, 30, 0.3, 0.6, 0.3, 0.1);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 1.2F);
    }

    // The veil: a solid image left standing, and he is unseen.
    private void vanish(ServerLevel level) {
        WizardDecoy.veil(level, this.wizard);
        this.wizard.setInvisible(true);
        Vec3 at = this.wizard.position().add(0.0, 1.0 + this.wizard.hover(), 0.0);
        ParticleFx.send(level, ParticleTypes.SMOKE, at.x, at.y, at.z, 6, 0.2, 0.5, 0.2, 0.01);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.ILLUSIONER_PREPARE_MIRROR, SoundSource.HOSTILE, 0.6F,
                1.4F);
    }

    // Behind his foe, a step back from their back, where there is room.
    private void behind(ServerLevel level, @Nullable LivingEntity target) {
        this.wizard.setInvisible(false);
        if (target == null) {
            return;
        }
        Vec3 look = target.getViewVector(1.0F).multiply(1.0, 0.0, 1.0);
        look = look.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : look.normalize();
        Vec3 from = this.wizard.position();
        for (double back : new double[] { 2.2, 1.6, 3.0 }) {
            Vec3 spot = target.position().subtract(look.scale(back));
            if (!this.wizard.flies()) {
                spot = WizardCasts.ground(level, spot.add(0.0, 1.5, 0.0));
            }
            if (spot.distanceTo(from) <= Wizard.TELEPORT_RANGE
                    && level.noCollision(this.wizard, this.wizard.getBoundingBox().move(spot.subtract(from)))) {
                this.wizard.teleportTo(spot.x, spot.y, spot.z);
                break;
            }
        }
        MonsterBlows.face(this.wizard, target.position().subtract(this.wizard.position()));
        Vec3 at = this.wizard.position().add(0.0, 1.0 + this.wizard.hover(), 0.0);
        ParticleFx.implosion(level, Wizard.dust(Wizard.VIOLET, 1.2F), at, 1.4, 20, 0.25);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.HOSTILE, 1.2F, 0.6F);
    }

    // The blow out of nowhere: his staff swung round, or with it away a burst from his palm.
    private void veilStrike(ServerLevel level) {
        float harm = this.wizard.harm(WizardMove.VEIL);
        StaffBlows.strike(level, this.wizard, 3.2, 1.3, harm, 1.0, 0.35);
        Vec3 at = this.wizard.staffTip();
        ParticleFx.send(level, Wizard.dust(Wizard.VIOLET, 1.4F), at.x, at.y, at.z, 12, 0.3, 0.3, 0.3, 0.0);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.4F, 0.7F);
    }

    // Rifts round his foe, a little over them and facing them, each in its own kind's light.
    private void openVolley(ServerLevel level, @Nullable LivingEntity target) {
        if (target == null) {
            return;
        }
        Vec3 middle = target.position();
        double turn = this.wizard.getRandom().nextDouble() * Mth.TWO_PI;
        for (int k = 0; k < VOLLEY_RIFTS; k++) {
            double angle = turn + k * Mth.TWO_PI / VOLLEY_RIFTS;
            Vec3 at = middle.add(Math.cos(angle) * 6.0, 2.2 + (k % 2) * 1.4, Math.sin(angle) * 6.0);
            Vec3 way = middle.add(0.0, 1.0, 0.0).subtract(at).multiply(1.0, 0.0, 1.0).normalize();
            float yaw = (float) (Mth.atan2(-way.x, way.z) * Mth.RAD_TO_DEG);
            int style = (WizardPortal.styleOf(this.wizard.phase()) + k) % WizardPortal.STYLES;
            this.rifts.add(WizardPortal.open(level, at, yaw, 0.85F, VOLLEY_STOP + 14, style, true));
        }
    }
}
