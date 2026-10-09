package nl.tivek.multiversepowers.character.greenlantern.ability.mech;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechAttacks;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechScript;
import nl.tivek.multiversepowers.character.greenlantern.minion.MechMinion;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.fx.Sounds;

// The mech's helpers (MechMinion): out of the open hatch under its cockpit they drop one after another, as many as
// its pilot is short of MOST, each a little further to its right.
final class MechHelpers {
    static final int MOST = 3;
    // In front of the hatch, in the torso's places: where the middle of a helper's body forms.
    private static final Vec3 HATCH = new Vec3(0.0, 7.5, 1.9);
    private static final double APART = 0.9;
    private static final double SEARCH = 80.0;

    private MechHelpers() {
    }

    // How many of the pilot's helpers are out.
    static int out(ServerLevel level, ServerPlayer owner) {
        return level.getEntitiesOfClass(MechMinion.class, owner.getBoundingBox().inflate(SEARCH),
                minion -> minion.isAlive() && minion.ownedBy(owner)).size();
    }

    // At its moments of the hatch's move, the next helper drops out, if one is still missing.
    static void drop(ServerLevel level, ServerPlayer owner, MechScript.Stage torso, int t, CharacterAbility ability) {
        int since = t - MechAttacks.HATCH_DROP;
        int k = since / MechAttacks.HATCH_EVERY;
        if (since < 0 || since % MechAttacks.HATCH_EVERY != 0 || k >= MOST || out(level, owner) >= MOST) {
            return;
        }
        Vec3 middle = torso.point(HATCH.x + (k - 1) * APART, HATCH.y, HATCH.z);
        MechMinion minion = MechMinion.of(level, owner, ability.value("mechHelperHealth"),
                ability.value("mechHelperDamage"));
        float yaw = torso.yaw();
        minion.moveTo(middle.x, middle.y - minion.getBbHeight() * 0.5, middle.z, yaw, 0.0F);
        minion.setYHeadRot(yaw);
        minion.setYBodyRot(yaw);
        minion.setDeltaMovement(torso.ahead().scale(0.2).add(torso.right().scale((k - 1) * 0.12)));
        level.addFreshEntity(minion);
        Sounds.play(level, middle, SoundEvents.RESPAWN_ANCHOR_CHARGE, 1.4F, 1.3F);
        Sounds.play(level, middle, SoundEvents.IRON_GOLEM_REPAIR, 1.0F, 1.2F);
        ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 1.4F), middle, 16, 0.6, 0.1);
    }
}
