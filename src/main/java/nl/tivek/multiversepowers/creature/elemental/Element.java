package nl.tivek.multiversepowers.creature.elemental;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.block.Blocks;
import nl.tivek.multiversepowers.MultiversePowers;

// The five elements the Elemental Blaze's rods carry, each with its colours, the particle its rods shed, the colour of
// the boss bar while it is in use, its own kind of harm and what a cast of it does: a share of the Blaze's damage, and
// the Elemental Staff's own damage and cooldown in ticks.
public enum Element {
    EARTH("earth", "earth_rod", 0x8A6A44, 0xD6A86A, BossEvent.BossBarColor.GREEN, 1.0, 7.0F, 40),
    FIRE("fire", "ember_rod", 0xF27A1A, 0xFFD27A, BossEvent.BossBarColor.RED, 0.75, 5.0F, 25),
    ICE("ice", "frost_rod", 0x8FD8FF, 0xEAF9FF, BossEvent.BossBarColor.BLUE, 0.5, 4.0F, 50),
    WIND("wind", "gale_rod", 0xC9D2DA, 0xFFFFFF, BossEvent.BossBarColor.WHITE, 0.4, 3.0F, 30),
    STORM("storm", "storm_rod", 0xFFD83A, 0xFFFBD8, BossEvent.BossBarColor.YELLOW, 0.75, 6.0F, 50);

    public static final Element[] ALL = values();

    public final String id;
    // The item of the rod it drops.
    public final String rod;
    public final int colour;
    public final int light;
    public final BossEvent.BossBarColor bar;
    public final double share;
    public final float staffDamage;
    public final int staffCooldown;
    public final ResourceKey<DamageType> harm;

    Element(String id, String rod, int colour, int light, BossEvent.BossBarColor bar, double share, float staffDamage,
            int staffCooldown) {
        this.id = id;
        this.rod = rod;
        this.colour = colour;
        this.light = light;
        this.bar = bar;
        this.share = share;
        this.staffDamage = staffDamage;
        this.staffCooldown = staffCooldown;
        this.harm = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(
                MultiversePowers.MODID, "elemental_" + id));
    }

    public static Element of(int index) {
        return ALL[Math.floorMod(index, ALL.length)];
    }

    // What a rod of it sheds as it circles.
    public ParticleOptions particle() {
        return switch (this) {
            case EARTH -> new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.COARSE_DIRT.defaultBlockState());
            case FIRE -> ParticleTypes.SMALL_FLAME;
            case ICE -> ParticleTypes.SNOWFLAKE;
            case WIND -> ParticleTypes.WHITE_SMOKE;
            case STORM -> ParticleTypes.ELECTRIC_SPARK;
        };
    }

    public String key() {
        return "element." + MultiversePowers.MODID + "." + this.id;
    }
}
