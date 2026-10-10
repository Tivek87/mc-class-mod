package nl.tivek.multiversepowers.character.greenlantern.ability.ring;

import net.minecraft.server.level.ServerPlayer;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.character.greenlantern.ability.airstrike.AirStrike;
import nl.tivek.multiversepowers.character.greenlantern.ability.fist.LightFists;
import nl.tivek.multiversepowers.character.greenlantern.ability.hands.GiantHands;
import nl.tivek.multiversepowers.character.greenlantern.ability.light.LightBeam;
import nl.tivek.multiversepowers.character.greenlantern.ability.slam.LandingSlam;
import nl.tivek.multiversepowers.character.greenlantern.ability.slam.Shockwave;

// One move of the hands at a time: while a fist blow, the Beam, the Giant Hands' wave, the Air Strike's call or a slam
// is going, every other move that needs his hands waits. A move never waits on itself (it keeps its own rules).
// Flight, the ram cone and the dome need no hands and go along with anything.
public final class RingHands {
    public enum Move { FISTS, BEAM, HANDS, STRIKE, SLAM, OTHER }

    private RingHands() {
    }

    public static boolean busy(ServerPlayer player, Move self) {
        return self != Move.FISTS && LightFists.busy(player)
                || self != Move.BEAM && LightBeam.firing(player)
                || self != Move.HANDS && GiantHands.waving(player)
                || self != Move.STRIKE && AirStrike.calling(player)
                || self != Move.SLAM && (LandingSlam.running(player) || Shockwave.dropping(player));
    }

    // Says why the move waits; true while it must.
    public static boolean refuse(ServerPlayer player, Move self) {
        if (!busy(player, self)) {
            return false;
        }
        PowerRing.tell(player, "busy_hands");
        return true;
    }
}
