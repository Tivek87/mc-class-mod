package nl.tivek.multiversepowers.character.thor.client.motion;

import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.Characters;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.client.ClientCharacter;
import nl.tivek.multiversepowers.character.thor.ThorStatePayload;
import nl.tivek.multiversepowers.character.thor.client.ClientThor;
import nl.tivek.multiversepowers.character.thor.hammer.HammerRules;
import nl.tivek.multiversepowers.character.thor.hammer.ThrownHammer;

// Your own Thor pulled to his hammer (the server's PULL): first the wait, planted, his hand stretched to it, then the
// dash at a set pace, getting going over its first ticks and braking into the catch. A wall in the way lifts him;
// held up too long, or the hammer gone or called back, he gives up and drops. There, by the same rule as the server,
// he lands by it (ground close under it) or flies on with it; the server has him catch it.
public final class ThorPull {
    // He lands this far short of a hammer on the ground, on his own side of it.
    private static final double SHORT = 1.2;
    // Flying on he keeps this much of the dash's speed.
    private static final double KEPT = 0.4;
    // Longer than this past the trip's own time, he gives up.
    private static final int SLOWEST_LEFT = 20;
    private static final double SPEED = 20.0;

    private static int age = -1;
    private static int dashAge;
    private static int stuck;
    private static int longest;
    private static boolean told;

    private ThorPull() {
    }

    public static void start() {
        age = 0;
        dashAge = 0;
        stuck = 0;
        longest = HammerRules.WAIT + 2 * SLOWEST_LEFT;
        told = false;
    }

    static void stop() {
        age = -1;
    }

    // Ticks into the pull (the wait first), or -1.
    public static int age() {
        return age;
    }

    // The server's word: once it has said he is pulled, its ending the pull (a hard hit) ends it here too.
    static void told(boolean pulling) {
        if (pulling) {
            told = true;
        } else if (told && age >= 0) {
            stop();
        }
    }

    // Moves him on towards it; false once he is not being pulled.
    static boolean pulling(LocalPlayer player, Input input) {
        if (age < 0) {
            return false;
        }
        ThrownHammer hammer = ClientThor.hammer(player);
        if (hammer == null && age > HammerRules.WAIT || hammer != null && hammer.rest() == ThrownHammer.BACK
                || ++age > longest) {
            stop();
            return false;
        }
        ThorGroundMotion.still(input);
        player.resetFallDistance();
        Vec3 v = player.getDeltaMovement();
        if (age <= HammerRules.WAIT || hammer == null || !hammer.resting()) {
            // The wait, planted, or hanging still in the air.
            player.setDeltaMovement(0.0, player.onGround() ? v.y : 0.0, 0.0);
            return true;
        }
        double size = player.getScale();
        Vec3 at = hammer.position();
        boolean land = lands(player.level(), at);
        Vec3 goal = land ? landing(player, at) : at.subtract(0.0, 0.9 * size, 0.0);
        Vec3 to = goal.subtract(player.position());
        double gap = to.length();
        double there = player.position().add(0.0, 0.9 * size, 0.0).distanceTo(at);
        if (there < HammerRules.THERE * size || gap < 0.3) {
            arrive(player, land);
            return !land;
        }
        CharacterAbility leap = GameCharacter.THOR.byName("hammer_leap");
        double pace = (leap == null ? SPEED : leap.value("dashSpeed")) / 20.0;
        if (dashAge == 0) {
            longest = age + (int) Math.ceil(gap / pace) + SLOWEST_LEFT;
        }
        Vec3 velocity = to.scale(HammerRules.dashStep(dashAge++, gap, pace) / gap);
        if (player.horizontalCollision && dashAge > 1) {
            velocity = new Vec3(velocity.x, Math.max(velocity.y, HammerRules.LIFT), velocity.z);
            if (++stuck >= HammerRules.STUCK) {
                stop();
                return false;
            }
        } else {
            stuck = 0;
        }
        player.setDeltaMovement(velocity);
        return true;
    }

    // Where his feet come down by a hammer on the ground: a step short of it on his side, on the ground there.
    private static Vec3 landing(LocalPlayer player, Vec3 hammer) {
        Vec3 back = new Vec3(player.getX() - hammer.x, 0.0, player.getZ() - hammer.z);
        if (back.lengthSqr() < 1.0E-4) {
            Vec3 look = player.getLookAngle();
            back = new Vec3(-look.x, 0.0, -look.z);
        }
        Vec3 spot = hammer.add(back.normalize().scale(SHORT * player.getScale()));
        HitResult ground = player.level().clip(new ClipContext(spot.add(0.0, 1.0, 0.0),
                spot.add(0.0, -HammerRules.GROUND_BELOW - 1.0, 0.0), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        return ground.getType() == HitResult.Type.MISS ? spot : ground.getLocation();
    }

    // Whether he will land by this hammer when he reaches it, rather than catch it and fly on.
    public static boolean landsBy(ThrownHammer hammer) {
        return hammer.rest() == ThrownHammer.LYING || lands(hammer.level(), hammer.position());
    }

    // The arrival rule, as the server has it: ground this close straight under the hammer, he lands by it.
    static boolean lands(Level level, Vec3 hammer) {
        return level.clip(new ClipContext(hammer.add(0.0, 0.05, 0.0), hammer.add(0.0, -HammerRules.GROUND_BELOW, 0.0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, CollisionContext.empty())).getType()
                != HitResult.Type.MISS;
    }

    // There: the server is told, and he lands by it (dropping the last bit) or flies on, keeping part of his speed.
    private static void arrive(LocalPlayer player, boolean land) {
        stop();
        CharacterAbility follow = GameCharacter.THOR.byName("hammer_follow");
        if (follow != null) {
            ClientCharacter.sendAction(follow, true, Characters.SLAM);
        }
        Vec3 v = player.getDeltaMovement();
        ClientThor.set(player, ThorStatePayload.THROWN, false);
        if (land) {
            player.setDeltaMovement(v.x * 0.2, Math.min(v.y, 0.0), v.z * 0.2);
            ClientThor.set(player, ThorStatePayload.ARMED, true);
            ClientThor.predict(player, ThorStatePayload.CATCH, ThorStatePayload.RIGHT_HAND, ThorGroundMotion.flags());
        } else {
            ThorMotion.flyOn(player, v.scale(KEPT));
        }
    }
}
