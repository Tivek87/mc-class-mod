package nl.tivek.multiversepowers.engine.entity;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.RelativeMovement;

// A player a power holds where it wants them: their own game moves them, so the server puts them back there every
// tick, leaving their look their own, and never takes them for flying meanwhile. Whoever holds one also answers for
// them to HeldMobs.addHolder, so nothing else takes them and their knockdown waits (PlayerKnockdowns).
public final class HeldPlayers {
    private HeldPlayers() {
    }

    public static void holdAt(ServerPlayer player, double x, double y, double z) {
        player.connection.teleport(x, y, z, player.getYRot(), player.getXRot(), RelativeMovement.ROTATION);
        player.connection.aboveGroundTickCount = 0;
    }
}
