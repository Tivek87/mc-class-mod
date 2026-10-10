package nl.tivek.multiversepowers.character.greenlantern.ability.mech;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechAttacks;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechScript;
import nl.tivek.multiversepowers.engine.entity.Knockdowns;
import nl.tivek.multiversepowers.engine.fx.Sounds;

// Where the combo's later blows land: the jab and the hook strike what their left fist passes through, the uppercut
// throws what its right fist rises through high, the knee bursts on what stands before it, the clap's blast runs on
// ahead, and each fist of the pound strikes the ground round it.
final class MechCombo {
    private static final double JAB_RADIUS = 2.6;
    private static final double HOOK_RADIUS = 3.0;
    private static final double UPPERCUT_RADIUS = 2.8;
    private static final double KNEE_AHEAD = 3.4;
    private static final double KNEE_RADIUS = 4.5;
    private static final double CLAP_AHEAD = 6.5;
    private static final double CLAP_RADIUS = 7.0;
    private static final double POUND_RADIUS = 3.6;

    private MechCombo() {
    }

    static void land(MechAttack attack, ServerLevel level, ServerPlayer owner, MechScript.Stage frame,
            MechScript.Stage torso, MechScript.Stage legs, CharacterAbility ability) {
        int t = attack.t;
        switch (attack.kind()) {
            case MechAttacks.JAB -> attack.swing(level, owner, frame, torso, false, MechAttacks.JAB_HIT - 2,
                    MechAttacks.JAB_HIT + 1, JAB_RADIUS, ability.value("mechJabDamage"), 0.0, 1.5, 0.4);
            case MechAttacks.HOOK -> attack.swing(level, owner, frame, torso, false, MechAttacks.HOOK_FROM,
                    MechAttacks.HOOK_TO, HOOK_RADIUS, ability.value("mechHookDamage"), 0.4, 2.2, 0.5);
            case MechAttacks.UPPERCUT -> attack.swing(level, owner, frame, torso, true, MechAttacks.UPPERCUT_FROM,
                    MechAttacks.UPPERCUT_HIT, UPPERCUT_RADIUS, ability.value("mechUppercutDamage"), 0.0, 0.5, 1.5);
            case MechAttacks.KNEE -> {
                if (t == MechAttacks.KNEE_HIT) {
                    Vec3 at = legs.point(MechScript.ANKLE.x * 0.5, 1.0, KNEE_AHEAD);
                    Sounds.play(level, at, SoundEvents.ANVIL_LAND, 1.4F, 0.5F);
                    MechBlows.blast(level, owner, null, at, KNEE_RADIUS, ability.value("mechKneeDamage"), 1.8);
                }
            }
            case MechAttacks.CLAP -> {
                if (t == MechAttacks.CLAP_HIT) {
                    MechBlows.clap(level, torso.point(0.0, 6.6, 4.8));
                    Vec3 at = frame.point(0.0, 0.5, CLAP_AHEAD);
                    MechBlows.blast(level, owner, null, at, CLAP_RADIUS, ability.value("mechClapBlowDamage"), 2.4);
                    knockAll(level, owner, at, CLAP_RADIUS);
                }
            }
            case MechAttacks.POUND -> {
                int strike = MechAttacks.pound(t);
                if (strike >= 0) {
                    Vec3 fist = attack.fist(strike % 2 == 0, frame, torso);
                    Vec3 ground = new Vec3(fist.x, legs.base().y, fist.z);
                    MechBlows.stomp(level, ground, strike == MechAttacks.POUNDS.length - 1);
                    MechBlows.blast(level, owner, null, ground, POUND_RADIUS, ability.value("mechPoundDamage"),
                            1.2);
                }
            }
            default -> {
            }
        }
    }

    // The clap's blast throws everything it reaches off its feet.
    private static void knockAll(ServerLevel level, ServerPlayer owner, Vec3 at, double radius) {
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius),
                entity -> PowerRing.canHit(owner, entity))) {
            if (living.position().distanceTo(at) <= radius) {
                Knockdowns.knock(living);
            }
        }
    }
}
