package nl.tivek.multiversepowers.creature.wizard.ritual;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.WizardEntities;
import nl.tivek.multiversepowers.engine.effect.Effect;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// The ritual that calls the Wizard: an Arcane Tome laid on an enchanting table ringed with bookshelves, at night. The
// shelves' glyphs stream into the table, a circle of runes burns out round it and turns, light winds up off the table
// into a column, lightning strikes round the circle, and in a last flash he comes down out of a rift over the table
// (Wizard.callDown). Broken off if the table goes.
public final class WizardRitual implements Effect {
    public static final int SHELVES = 15;
    private static final int LENGTH = 120;
    private static final int[] BOLTS = { 52, 82, 104 };
    private static final double CIRCLE = 3.6;
    private static final Set<GlobalPos> BURNING = new HashSet<>();
    private final GlobalPos at;
    private final BlockPos table;
    private final List<BlockPos> shelves;
    private final float facing;

    private WizardRitual(GlobalPos at, List<BlockPos> shelves, float facing) {
        this.at = at;
        this.table = at.pos();
        this.shelves = shelves;
        this.facing = facing;
    }

    // Whether the ritual could start at this table now; tells the player why not.
    static boolean begin(ServerLevel level, BlockPos table, ServerPlayer player) {
        GlobalPos at = GlobalPos.of(level.dimension(), table);
        if (BURNING.contains(at)) {
            refuse(player, "busy");
            return false;
        }
        if (!level.dimensionType().hasFixedTime() && !level.isNight()) {
            refuse(player, "day");
            return false;
        }
        List<BlockPos> shelves = shelves(level, table);
        if (shelves.size() < SHELVES) {
            player.displayClientMessage(Component.translatable("ritual.welcomescreen.wizard.shelves", SHELVES,
                    shelves.size()), true);
            return false;
        }
        BURNING.add(at);
        float facing = (float) (Mth.atan2(player.getZ() - table.getZ() - 0.5, player.getX() - table.getX() - 0.5)
                * Mth.RAD_TO_DEG) - 90.0F;
        Effects.start(level, new WizardRitual(at, shelves, facing));
        return true;
    }

    public static void clear() {
        BURNING.clear();
    }

    private static void refuse(ServerPlayer player, String why) {
        player.displayClientMessage(Component.translatable("ritual.welcomescreen.wizard." + why), true);
    }

    private static List<BlockPos> shelves(Level level, BlockPos table) {
        List<BlockPos> found = new ArrayList<>();
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            if (EnchantingTableBlock.isValidBookShelf(level, table, offset)) {
                found.add(table.offset(offset));
            }
        }
        return found;
    }

    @Override
    public boolean tick(ServerLevel level, int age) {
        if (!level.isLoaded(this.table)) {
            BURNING.remove(this.at);
            return false;
        }
        Vec3 top = Vec3.atCenterOf(this.table).add(0.0, 0.3, 0.0);
        Vec3 ground = Vec3.atBottomCenterOf(this.table).add(0.0, 0.05, 0.0);
        if (!level.getBlockState(this.table).is(Blocks.ENCHANTING_TABLE)) {
            this.broken(level, top);
            return false;
        }
        float strength = Math.min(1.0F, age / 90.0F);
        this.sounds(level, top, age);
        if (age < LENGTH - 10) {
            for (int k = 0; k < 2 + (int) (strength * 4); k++) {
                BlockPos shelf = this.shelves.get(level.random.nextInt(this.shelves.size()));
                Vec3 from = Vec3.atCenterOf(shelf).add(0.0, 0.4, 0.0);
                ParticleFx.fly(level, ParticleTypes.ENCHANT, top.add(0.0, 0.5, 0.0), from.subtract(top), 1.0);
            }
        }
        double radius = CIRCLE * Mth.clamp(age / 30.0, 0.2, 1.0);
        if (age % 4 == 0) {
            int light = age < 60 ? Wizard.VIOLET : Wizard.STARLIGHT;
            ParticleFx.ring(level, Wizard.dust(light, 1.4F), ground, radius, 36, age * 0.05);
            ParticleFx.ring(level, Wizard.dust(Wizard.VIOLET, 1.0F), ground, radius * 0.72, 24, -age * 0.08);
        }
        if (age % 8 == 0 && age >= 16) {
            ParticleFx.groundStar(level, Wizard.dust(Wizard.STARLIGHT, 1.2F), ground, radius * 0.92, 5, 2, age * 0.05,
                    0.35);
        }
        if (age >= 30 && age % 5 == 0) {
            ParticleFx.helix(level, ParticleTypes.END_ROD, ground, 1.0 + strength * 0.6, 2.5 + strength * 2.0, 1.5,
                    14, age * 0.2);
        }
        if (age >= 60) {
            ParticleFx.send(level, ParticleTypes.END_ROD, top.x, top.y + 3.0, top.z, 3, 0.08, 2.5, 0.08, 0.01);
            ParticleFx.send(level, Wizard.dust(Wizard.VIOLET, 2.0F), top.x, top.y + 1.5, top.z, 4, 0.2, 1.2, 0.2,
                    0.0);
        }
        for (int bolt : BOLTS) {
            if (age == bolt) {
                this.lightning(level, ground);
            }
        }
        if (age == LENGTH - 6) {
            ParticleFx.sphereOut(level, ParticleTypes.ELECTRIC_SPARK, top, 30, 0.6);
            ParticleFx.shockwave(level, Wizard.dust(Wizard.STARLIGHT, 2.0F), ground, 64, 0.7);
            ParticleFx.sphereOut(level, ParticleTypes.END_ROD, top, 40, 0.3);
        }
        if (age >= LENGTH) {
            BURNING.remove(this.at);
            this.call(level, top);
            return false;
        }
        return true;
    }

    private void sounds(ServerLevel level, Vec3 top, int age) {
        if (age == 0) {
            level.playSound(null, top.x, top.y, top.z, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.HOSTILE, 1.5F,
                    0.6F);
            level.playSound(null, top.x, top.y, top.z, SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.5F, 0.7F);
        }
        if (age == 30) {
            level.playSound(null, top.x, top.y, top.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 2.0F,
                    0.6F);
        }
        if (age == 60) {
            level.playSound(null, top.x, top.y, top.z, SoundEvents.PORTAL_TRIGGER, SoundSource.HOSTILE, 0.8F, 1.4F);
        }
        if (age % 20 == 10 && age < LENGTH - 10) {
            level.playSound(null, top.x, top.y, top.z, SoundEvents.ILLUSIONER_PREPARE_MIRROR, SoundSource.HOSTILE,
                    1.0F, 0.5F + age / (float) LENGTH);
        }
        if (age == LENGTH - 6) {
            level.playSound(null, top.x, top.y, top.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 2.0F,
                    0.6F);
        }
    }

    // A bolt of lightning that harms nothing, somewhere on the circle.
    private void lightning(ServerLevel level, Vec3 ground) {
        double angle = level.random.nextDouble() * Math.PI * 2.0;
        double x = ground.x + Math.cos(angle) * (CIRCLE + 1.0);
        double z = ground.z + Math.sin(angle) * (CIRCLE + 1.0);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
        if (Math.abs(y - ground.y) > 8) {
            y = Mth.floor(ground.y);
        }
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.setVisualOnly(true);
            bolt.moveTo(x, y, z);
            level.addFreshEntity(bolt);
        }
    }

    private void call(ServerLevel level, Vec3 top) {
        Wizard wizard = WizardEntities.WIZARD.get().create(level);
        if (wizard == null) {
            return;
        }
        Vec3 ground = new Vec3(top.x, this.table.getY() + 0.75, top.z);
        wizard.moveTo(ground.x, ground.y, ground.z, this.facing, 0.0F);
        wizard.setYHeadRot(this.facing);
        wizard.setYBodyRot(this.facing);
        EventHooks.finalizeMobSpawn(wizard, level, level.getCurrentDifficultyAt(this.table), MobSpawnType.TRIGGERED,
                null);
        wizard.callDown(ground);
        level.addFreshEntity(wizard);
    }

    private void broken(ServerLevel level, Vec3 top) {
        BURNING.remove(this.at);
        ParticleFx.send(level, ParticleTypes.SMOKE, top.x, top.y, top.z, 30, 0.6, 0.4, 0.6, 0.02);
        level.playSound(null, top.x, top.y, top.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 1.0F, 0.6F);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(this.table).inflate(16.0))) {
            refuse(player, "broken");
        }
    }
}
