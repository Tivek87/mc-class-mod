package nl.tivek.multiversepowers.character.greenlantern.client;

import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.ability.airstrike.AirStrike;
import nl.tivek.multiversepowers.character.greenlantern.ability.slam.LandingSlam;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.MechPainter;
import nl.tivek.multiversepowers.character.greenlantern.client.render.BubblePainter;
import nl.tivek.multiversepowers.character.greenlantern.client.render.express.ExpressPainter;
import nl.tivek.multiversepowers.character.greenlantern.client.render.plane.PlanePainter;
import nl.tivek.multiversepowers.character.greenlantern.client.render.revolver.RevolverPainter;
import nl.tivek.multiversepowers.character.greenlantern.client.slam.SlamPainter;
import nl.tivek.multiversepowers.character.greenlantern.construct.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.duo.HandDuo;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandGroup;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandPose;
import nl.tivek.multiversepowers.character.greenlantern.plane.PlanePath;

abstract class ConstructShakes extends TrackedConstructs {
    private static final double SHAKE_TICKS = 8.0;
    private static final double CRASH_SHAKE_TICKS = 30.0;
    private static final double CRASH_SHAKE_RANGE = 140.0 * AirStrike.CRASH_SIZE;
    private static final double BLAST_SHAKE_TICKS = 10.0;
    private static final double BLAST_SHAKE_RANGE = 24.0;
    private static final double POUND_SHAKE_TICKS = 9.0;
    private static final double POUND_SHAKE_RANGE = 26.0;
    private static final double AXE_SHAKE_TICKS = 10.0;
    private static final double AXE_SHAKE_RANGE = 24.0;
    private static final double AXE_SHAKE = 0.8;
    private static final double FINGER_SHAKE_TICKS = 8.0;
    private static final double FINGER_SHAKE_RANGE = 18.0;
    private static final double FINGER_SHAKE = 0.5;

    ConstructShakes() {
    }

    public static float shake(Vec3 from, float partialTick) {
        float most = 0.0F;
        for (Track track : CONSTRUCTS.values()) {
            ConstructPayload slam = track.latest;
            if (slam.shape() == ConstructPayload.PLANE) {
                PlanePath path = PlanePainter.path(slam);
                double since = track.clock(partialTick) - path.crashTick();
                double near = 1.0 - from.distanceTo(path.crash()) / CRASH_SHAKE_RANGE;
                if (since >= 0.0 && since < CRASH_SHAKE_TICKS && near > 0.0) {
                    double fade = 1.0 - since / CRASH_SHAKE_TICKS;
                    most = Math.max(most, (float) (fade * fade * Math.min(1.0, near * 1.2)));
                }
                continue;
            }
            if (slam.shape() == ConstructPayload.BLAST) {
                double since = sinceSent(track, partialTick);
                double near = 1.0 - from.distanceTo(slam.center()) / BLAST_SHAKE_RANGE;
                if (since >= 0.0 && since < BLAST_SHAKE_TICKS && near > 0.0) {
                    double fade = 1.0 - since / BLAST_SHAKE_TICKS;
                    double hard = slam.variant() == AirStrike.SMALL_BLAST ? 0.3 : 0.65;
                    most = Math.max(most, (float) (hard * fade * fade * near));
                }
                continue;
            }
            if (slam.shape() == ConstructPayload.POUND) {
                double since = track.clock(partialTick);
                double near = 1.0 - from.distanceTo(slam.center()) / POUND_SHAKE_RANGE;
                if (since < POUND_SHAKE_TICKS && near > 0.0) {
                    double fade = 1.0 - since / POUND_SHAKE_TICKS;
                    double hard = BubblePainter.last(slam) ? 0.95 : 0.55;
                    most = Math.max(most, (float) (hard * fade * fade * Math.min(1.0, near * 1.3)));
                }
                continue;
            }
            if (slam.shape() == ConstructPayload.HAND) {
                most = Math.max(most, handShake(track, from, partialTick));
                continue;
            }
            if (slam.shape() == ConstructPayload.REVOLVER) {
                most = Math.max(most, RevolverPainter.shake(slam, track.clock(partialTick), from));
                continue;
            }
            if (slam.shape() == ConstructPayload.EXPRESS) {
                most = Math.max(most, ExpressPainter.shake(slam, track.clock(partialTick), from));
                continue;
            }
            if (slam.shape() == ConstructPayload.MECH) {
                most = Math.max(most, MechPainter.shake(slam, track.clock(partialTick), from));
                continue;
            }
            if (slam.shape() != ConstructPayload.SLAM) {
                continue;
            }
            double since = track.clock(partialTick) / SlamPainter.pace(slam) - LandingSlam.IMPACT_TICK;
            double near = 1.0 - from.distanceTo(slam.center()) / (slam.size() * 3.0 + 4.0);
            if (since < 0.0 || since >= SHAKE_TICKS || near <= 0.0) {
                continue;
            }
            double fade = 1.0 - since / SHAKE_TICKS;
            most = Math.max(most, (float) (fade * fade * Math.min(1.0, near * 1.5)));
        }
        return most;
    }

    private static float handShake(Track track, Vec3 from, float partialTick) {
        ConstructPayload hand = track.latest;
        int move = HandPose.move(hand.variant());
        double clock = track.clock(partialTick);
        double since;
        double ticks;
        double hard;
        double near;
        if (move == HandPose.AXE) {
            since = clock - HandDuo.IMPACT;
            if (since < 0.0 || since >= AXE_SHAKE_TICKS) {
                return 0.0F;
            }
            Vec3 strike = HandDuo.strike(hand.center(), hand.variant(), hand.center().add(hand.facing()),
                    Math.max(0.1, hand.size()));
            ticks = AXE_SHAKE_TICKS;
            hard = AXE_SHAKE;
            near = 1.0 - from.distanceTo(strike) / AXE_SHAKE_RANGE;
        } else if (move == HandPose.FINGER || move == HandPose.SNAP || move == HandPose.HAMMER
                || move == HandPose.RINGHOLD || move == HandPose.CLAP || move == HandPose.TEAR) {
            since = clock - switch (move) {
                case HandPose.FINGER -> HandPose.FINGER_BURSTS;
                case HandPose.SNAP -> HandPose.SNAP_HITS;
                case HandPose.RINGHOLD -> HandGroup.RING_BLASTS;
                case HandPose.CLAP -> HandGroup.CLAP_HITS;
                case HandPose.TEAR -> HandGroup.TEARS;
                default -> HandPose.HAMMER_HITS;
            };
            ticks = FINGER_SHAKE_TICKS;
            hard = FINGER_SHAKE * (move == HandPose.SNAP || move == HandPose.CLAP ? 0.6 : 1.0);
            near = 1.0 - from.distanceTo(hand.center()) / FINGER_SHAKE_RANGE;
        } else {
            return 0.0F;
        }
        if (since < 0.0 || since >= ticks || near <= 0.0) {
            return 0.0F;
        }
        double fade = 1.0 - since / ticks;
        return (float) (hard * fade * fade * Math.min(1.0, near * 1.2));
    }
}
