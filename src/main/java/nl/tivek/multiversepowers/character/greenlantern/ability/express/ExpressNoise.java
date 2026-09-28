package nl.tivek.multiversepowers.character.greenlantern.ability.express;

import net.minecraft.sounds.SoundEvent;
import nl.tivek.multiversepowers.engine.fx.Sounds;

// The game's own sounds the train makes, played half as loud (sounds.json: express.quiet.*) and heard as far.
final class ExpressNoise {
    static final SoundEvent AMETHYST_BLOCK_RESONATE = Sounds.of("express.quiet.amethyst_block_resonate");
    static final SoundEvent AMETHYST_CLUSTER_BREAK = Sounds.of("express.quiet.amethyst_cluster_break");
    static final SoundEvent ANVIL_LAND = Sounds.of("express.quiet.anvil_land");
    static final SoundEvent ANVIL_PLACE = Sounds.of("express.quiet.anvil_place");
    static final SoundEvent BEACON_ACTIVATE = Sounds.of("express.quiet.beacon_activate");
    static final SoundEvent BEACON_DEACTIVATE = Sounds.of("express.quiet.beacon_deactivate");
    static final SoundEvent BEACON_POWER_SELECT = Sounds.of("express.quiet.beacon_power_select");
    static final SoundEvent FIREWORK_ROCKET_LARGE_BLAST = Sounds.of("express.quiet.firework_rocket_large_blast");
    static final SoundEvent FIRE_EXTINGUISH = Sounds.of("express.quiet.fire_extinguish");
    static final SoundEvent GENERIC_EXPLODE = Sounds.of("express.quiet.generic_explode");
    static final SoundEvent GRINDSTONE_USE = Sounds.of("express.quiet.grindstone_use");
    static final SoundEvent IRON_GOLEM_DAMAGE = Sounds.of("express.quiet.iron_golem_damage");
    static final SoundEvent LAVA_EXTINGUISH = Sounds.of("express.quiet.lava_extinguish");
    static final SoundEvent MACE_SMASH_GROUND = Sounds.of("express.quiet.mace_smash_ground");
    static final SoundEvent MACE_SMASH_GROUND_HEAVY = Sounds.of("express.quiet.mace_smash_ground_heavy");
    static final SoundEvent PLAYER_ATTACK_KNOCKBACK = Sounds.of("express.quiet.player_attack_knockback");
    static final SoundEvent RESPAWN_ANCHOR_CHARGE = Sounds.of("express.quiet.respawn_anchor_charge");

    private ExpressNoise() {
    }
}
