package nl.tivek.welcomescreen.server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.tivek.welcomescreen.WelcomeScreenMod;
import nl.tivek.welcomescreen.classes.ClassData;
import nl.tivek.welcomescreen.classes.ClassGroup;
import nl.tivek.welcomescreen.classes.PlayerClass;
import org.joml.Vector3f;

/**
 * Class ceremonies. Every class has its own, completely different animation built around what
 * that class does (shield wall, heartbeat, arrow volley, storm cloud, greatsword from the sky...).
 *
 * Grand (first time): 200 ticks = 10 s, climax and class title on tick 150.
 * Light (every respawn): 50 ticks = 2.5 s, its own shorter animation that fits the class, no title.
 * Everything is centred on the player and happens all around them, never only in front.
 *
 * Also: a death animation per group (20 ticks = 1 s, where the player died) and one level-up
 * animation for everyone (50 ticks = 2.5 s, on a vanilla experience level-up).
 */
@EventBusSubscriber(modid = WelcomeScreenMod.MODID)
public final class ClassEffects {
    private static final Mode GRAND = new Mode(200, true, 0.6F);
    private static final Mode LIGHT = new Mode(50, false, 0.5F);
    private static final Mode DEATH = new Mode(20, false, 0.6F);
    private static final Mode LEVEL_UP = new Mode(50, false, 0.5F);
    private static final int TITLE_TICK = 150;

    private static final double FLOOR = 0.08;

    private static final boolean MAIN = false;
    private static final boolean ACC = true;

    private static final int TITLE_FADE_IN = 8;
    private static final int TITLE_STAY = 35;
    private static final int TITLE_FADE_OUT = 15;
    private static final int SUBTITLE_COLOR = 0xA8A090;

    // Six shard colours representing the six groups The Forsaken is forged from.
    private static final int[] FORSAKEN_SHARDS = {0xD8D8E0, 0x7FD46B, 0xE0B45A, 0xA88BE8, 0xF2E6A0, 0x5FD0C8};

    private static final Glyph WIZARD_SIGIL = new Glyph().circle(MAIN, 1.0).circle(MAIN, 0.89)
            .star(ACC, 5, 2, 0.89, 0).circle(ACC, 0.34).rays(MAIN, 24, 0.89, 1.0, 0);
    private static final Glyph DUELIST_ROSE = new Glyph().rose(MAIN, 4);
    private static final Glyph INQUISITOR_BRAND = new Glyph().polygon(MAIN, 3, 1.0, 0).circle(ACC, 0.5);
    private static final Glyph TRANSMUTER_SEAL = new Glyph().circle(MAIN, 1.0).polygon(MAIN, 4, 1.0, Math.PI / 4)
            .polygon(ACC, 3, 0.7, 0).circle(ACC, 0.35);
    private static final double[][][] FORSAKEN_CRACKS = forsakenCracks();

    private static final List<Ceremony> ACTIVE = new ArrayList<>();

    private ClassEffects() {
    }

    private record Mode(int duration, boolean grand, float volume) {
    }

    private static final class Ceremony {
        private final UUID playerId;
        // Null for the level-up animation, which is the same for everyone.
        private final PlayerClass playerClass;
        private final ClassGroup group;
        private final Mode mode;
        private final ResourceKey<Level> dimension;
        private final double x;
        private final double y;
        private final double z;
        private final float yaw;
        private int age;

        private Ceremony(ServerPlayer player, PlayerClass playerClass, Mode mode) {
            this(player, playerClass, playerClass == null ? null : playerClass.getGroup(), mode);
        }

        private Ceremony(ServerPlayer player, PlayerClass playerClass, ClassGroup group, Mode mode) {
            this.playerId = player.getUUID();
            this.playerClass = playerClass;
            this.group = group;
            this.mode = mode;
            this.dimension = player.level().dimension();
            this.x = player.getX();
            this.y = player.getY();
            this.z = player.getZ();
            this.yaw = player.getYRot();
        }
    }

    @FunctionalInterface
    private interface Animation {
        void tick(Fx fx);
    }

    public static void play(ServerPlayer player, PlayerClass playerClass, boolean firstTime) {
        ACTIVE.add(new Ceremony(player, playerClass, firstTime ? GRAND : LIGHT));
    }

    /** One-second death animation of the group, on the spot where the player is now. */
    public static void playDeath(ServerPlayer player, ClassGroup group) {
        ACTIVE.add(new Ceremony(player, null, group, DEATH));
    }

    /** Level-up animation, the same for everyone. Not stacked when several levels come at once. */
    public static void playLevelUp(ServerPlayer player) {
        for (Ceremony ceremony : ACTIVE) {
            if (ceremony.mode == LEVEL_UP && ceremony.playerId.equals(player.getUUID())) {
                return;
            }
        }
        ACTIVE.add(new Ceremony(player, null, null, LEVEL_UP));
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerClass playerClass = ClassData.getClass(player);
            if (playerClass != null) {
                playDeath(player, playerClass.getGroup());
            }
        }
    }

    @SubscribeEvent
    public static void onLevelChange(PlayerXpEvent.LevelChange event) {
        if (event.getLevels() > 0 && event.getEntity() instanceof ServerPlayer player) {
            playLevelUp(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.isEndConquered() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        PlayerClass playerClass = ClassData.getClass(player);
        if (playerClass != null) {
            play(player, playerClass, false);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (ACTIVE.isEmpty()) {
            return;
        }

        Iterator<Ceremony> iterator = ACTIVE.iterator();
        while (iterator.hasNext()) {
            Ceremony ceremony = iterator.next();
            ServerLevel level = event.getServer().getLevel(ceremony.dimension);
            if (level == null || ceremony.age > ceremony.mode.duration()) {
                iterator.remove();
                continue;
            }

            Fx fx = new Fx(level, ceremony);
            if (ceremony.mode == DEATH) {
                death(ceremony.group).tick(fx);
            } else if (ceremony.mode == LEVEL_UP) {
                levelUp(fx);
            } else {
                animation(ceremony.playerClass, ceremony.mode.grand()).tick(fx);
            }
            if (ceremony.mode.grand() && ceremony.age == TITLE_TICK) {
                fx.sound(SoundEvents.PLAYER_LEVELUP, 0.6F, 1.2F);
                ServerPlayer player = event.getServer().getPlayerList().getPlayer(ceremony.playerId);
                if (player != null) {
                    showTitle(player, ceremony.playerClass);
                }
            }
            ceremony.age++;
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        ACTIVE.clear();
    }

    private static void showTitle(ServerPlayer player, PlayerClass playerClass) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(TITLE_FADE_IN, TITLE_STAY, TITLE_FADE_OUT));
        Component subtitle = playerClass.getGroup().getDisplayName().copy().withColor(SUBTITLE_COLOR);
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        Component title = playerClass.getDisplayName().copy().withColor(playerClass.getColor());
        player.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    private static Animation animation(PlayerClass playerClass, boolean grand) {
        return switch (playerClass) {
            case KNIGHT -> grand ? ClassEffects::knightGrand : ClassEffects::knightLight;
            case BERSERKER -> grand ? ClassEffects::berserkerGrand : ClassEffects::berserkerLight;
            case HALBERDIER -> grand ? ClassEffects::halberdierGrand : ClassEffects::halberdierLight;
            case DUELIST -> grand ? ClassEffects::duelistGrand : ClassEffects::duelistLight;
            case ARCHER -> grand ? ClassEffects::archerGrand : ClassEffects::archerLight;
            case BEASTMASTER -> grand ? ClassEffects::beastmasterGrand : ClassEffects::beastmasterLight;
            case TRAPPER -> grand ? ClassEffects::trapperGrand : ClassEffects::trapperLight;
            case SCOUT -> grand ? ClassEffects::scoutGrand : ClassEffects::scoutLight;
            case ASSASSIN -> grand ? ClassEffects::assassinGrand : ClassEffects::assassinLight;
            case THIEF -> grand ? ClassEffects::thiefGrand : ClassEffects::thiefLight;
            case HIGHWAYMAN -> grand ? ClassEffects::highwaymanGrand : ClassEffects::highwaymanLight;
            case INFILTRATOR -> grand ? ClassEffects::infiltratorGrand : ClassEffects::infiltratorLight;
            case WIZARD -> grand ? ClassEffects::wizardGrand : ClassEffects::wizardLight;
            case SORCERER -> grand ? ClassEffects::sorcererGrand : ClassEffects::sorcererLight;
            case WARLOCK -> grand ? ClassEffects::warlockGrand : ClassEffects::warlockLight;
            case NECROMANCER -> grand ? ClassEffects::necromancerGrand : ClassEffects::necromancerLight;
            case CLERIC -> grand ? ClassEffects::clericGrand : ClassEffects::clericLight;
            case PALADIN -> grand ? ClassEffects::paladinGrand : ClassEffects::paladinLight;
            case INQUISITOR -> grand ? ClassEffects::inquisitorGrand : ClassEffects::inquisitorLight;
            case MONK -> grand ? ClassEffects::monkGrand : ClassEffects::monkLight;
            case APOTHECARY -> grand ? ClassEffects::apothecaryGrand : ClassEffects::apothecaryLight;
            case PLAGUE_DOCTOR -> grand ? ClassEffects::plagueDoctorGrand : ClassEffects::plagueDoctorLight;
            case BOMBARDIER -> grand ? ClassEffects::bombardierGrand : ClassEffects::bombardierLight;
            case TRANSMUTER -> grand ? ClassEffects::transmuterGrand : ClassEffects::transmuterLight;
            case FORSAKEN -> grand ? ClassEffects::forsakenGrand : ClassEffects::forsakenLight;
        };
    }

    // =========================================================================
    // Small shared helpers
    // =========================================================================

    private static double smooth(double t) {
        t = Mth.clamp(t, 0, 1);
        return t * t * (3 - 2 * t);
    }

    /** 1 until {@code from}, then fading linearly to 0 at {@code to}. */
    private static double fadeOut(int t, int from, int to) {
        return t < from ? 1 : Mth.clamp(1 - (t - from) / (double) (to - from), 0, 1);
    }

    /** Stable pseudo random 0..1 for index {@code i}, so positions stay the same every tick. */
    private static double hash(int i, int salt) {
        double v = Math.sin(i * 12.9898 + salt * 78.233) * 43758.5453;
        return v - Math.floor(v);
    }

    private static int lerpColor(double t, int from, int to) {
        t = Mth.clamp(t, 0, 1);
        int r = (int) Mth.lerp(t, (from >> 16) & 0xFF, (to >> 16) & 0xFF);
        int g = (int) Mth.lerp(t, (from >> 8) & 0xFF, (to >> 8) & 0xFF);
        int b = (int) Mth.lerp(t, from & 0xFF, to & 0xFF);
        return (r << 16) | (g << 8) | b;
    }

    // =========================================================================
    // Warriors
    // =========================================================================

    /**
     * Knight: eight shields rise one by one around you, a gold ring is traced under them, they circle faster,
     * climb to chest height, slam shut into a shield wall, are thrown outward, and you raise a sword of light.
     */
    private static void knightGrand(Fx fx) {
        int t = fx.age();
        ParticleOptions steel = fx.dust(0xC8D0E0, 1.1F);
        ParticleOptions gold = fx.dust(0xF2C84B, 1.0F);
        if (t < 185) {
            double spin = t < 60 ? 0 : Math.pow((Math.min(t, 140) - 60) / 80.0, 2) * Math.PI * 3;
            double radius;
            double lift;
            if (t < 100) {
                radius = 2.0;
                lift = 0;
            } else if (t < 140) {
                double w = smooth((t - 100) / 40.0);
                radius = Mth.lerp(w, 2.0, 1.5);
                lift = w * 0.9;
            } else if (t < 150) {
                double w = (t - 140) / 10.0;
                radius = Mth.lerp(w * w, 1.5, 1.05);
                lift = 0.9;
            } else {
                double w = (t - 150) / 35.0;
                radius = Mth.lerp(w, 1.05, 2.6);
                lift = Mth.lerp(w, 0.9, -1.2);
            }
            for (int i = 0; i < 8; i++) {
                int born = i * 7;
                if (t < born) {
                    continue;
                }
                if (t == born) {
                    fx.sound(SoundEvents.SHIELD_BLOCK, 0.6F, 0.8F + i * 0.06F);
                }
                double base = Mth.lerp(Math.min(1, (t - born) / 10.0), -1.0, 0.15) + lift;
                double angle = i * Math.PI / 4 + spin;
                fx.shield(steel, gold, angle, radius, base, 1.1, fadeOut(t, 160, 185));
                if (t >= 60 && t < 150 && fx.chance(0.25)) {
                    fx.at(ParticleTypes.CRIT, Math.sin(angle) * radius, base + 0.1, Math.cos(angle) * radius);
                }
            }
            if (t >= 60 && t < 140 && t % 8 == 0) {
                fx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.4F, 0.6F + (t - 60) / 100F);
            }
        }
        if (t >= 20 && t < 160 && fx.every(2)) {
            double w = Math.min(1, (t - 20) / 40.0);
            fx.arcAt(fx.dust(0xF2C84B, 0.9F), 0, FLOOR, 0, 2.0, 0, 2 * Math.PI * w, 0.1, fadeOut(t, 145, 160));
        }
        if (t == 100) {
            fx.sound(SoundEvents.ARMOR_EQUIP_IRON, 0.8F, 0.8F);
        }
        if (t == 150) {
            fx.sound(SoundEvents.ANVIL_LAND, 0.8F, 0.9F);
            fx.sound(SoundEvents.SHIELD_BLOCK, 1.0F, 0.6F);
            fx.flash(1.4);
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4;
                fx.burstBlock(Blocks.IRON_BLOCK, Math.sin(a) * 1.05, 1.4, Math.cos(a) * 1.05, 4, 0.2);
            }
        }
        double w = fx.span(150, 164);
        if (w >= 0) {
            fx.ring(gold, Mth.lerp(w, 1.1, 2.4), 1.4, 0.12, 1 - w * 0.5);
            fx.ring(steel, Mth.lerp(w, 0.8, 2.0), FLOOR, 0.14, 1 - w * 0.5);
        }
        if (t >= 152) {
            double rise = smooth((t - 152) / 10.0);
            fx.sword(steel, gold, 2.1 + rise * 0.4, 1, 1.4, 0.32, 0.07, fadeOut(t, 185, 200));
            if (t == 160) {
                fx.sound(SoundEvents.ARMOR_EQUIP_IRON, 0.8F, 1.2F);
                fx.cloud(ParticleTypes.END_ROD, 0, 3.9, 0, 8, 0.05, 0.05, 0.05, 0.05);
            }
            if (fx.chance(fadeOut(t, 170, 200))) {
                fx.at(gold, fx.spread(1.2), 0.3 + fx.rand() * 2.4, fx.spread(1.2));
            }
        }
    }

    /** A straight sword from bottom (bx, by, bz) along unit direction (dx, dy, dz), guard across (tx, tz). */
    private static void knightBlade(Fx fx, double bx, double by, double bz, double dx, double dy, double dz,
                                    double tx, double tz, double len, double keep) {
        ParticleOptions steel = fx.dust(0xC8D0E0, 1.1F);
        ParticleOptions gold = fx.dust(0xF2C84B, 1.1F);
        double gx = bx + dx * 0.3;
        double gy = by + dy * 0.3;
        double gz = bz + dz * 0.3;
        fx.line(gold, bx, by, bz, gx, gy, gz, 0.08, keep);
        fx.line(gold, gx - tx * 0.22, gy, gz - tz * 0.22, gx + tx * 0.22, gy, gz + tz * 0.22, 0.07, keep);
        fx.line(steel, gx, gy, gz, bx + dx * len, by + dy * len, bz + dz * len, 0.08, keep);
        if (fx.chance(keep)) {
            fx.at(ParticleTypes.END_ROD, bx + dx * len, by + dy * len, bz + dz * len);
        }
    }

    /** Knight respawn: four swords burst up out of the ground around you, clash, and fall outward. */
    private static void knightLight(Fx fx) {
        int t = fx.age();
        if (t < 40) {
            double keep = fadeOut(t, 24, 40);
            for (int k = 0; k < 4; k++) {
                double a = Math.PI / 4 + k * Math.PI / 2;
                double ox = Math.sin(a);
                double oz = Math.cos(a);
                double tx = Math.cos(a);
                double tz = -Math.sin(a);
                double bx = ox * 1.0;
                double bz = oz * 1.0;
                if (t < 8) {
                    double by = Mth.lerp(smooth(t / 6.0), -1.3, 0.1);
                    knightBlade(fx, bx, by, bz, 0, 1, 0, tx, tz, 1.4, 1);
                } else {
                    double th = Math.min(1, (t - 8) / 12.0) * 1.25;
                    knightBlade(fx, bx, 0.1, bz, ox * Math.sin(th), Math.cos(th), oz * Math.sin(th), tx, tz, 1.4,
                            keep);
                }
            }
        }
        if (t == 0) {
            fx.groundDebris(10, 1.2);
        }
        if (t == 6) {
            fx.sound(SoundEvents.ANVIL_LAND, 0.6F, 1.3F);
            fx.sound(SoundEvents.ARMOR_EQUIP_IRON, 0.8F, 1.0F);
            fx.cloud(ParticleTypes.CRIT, 0, 1.5, 0, 20, 0.8, 0.1, 0.8, 0.2);
        }
        double pulse = fx.span(6, 14);
        if (pulse >= 0) {
            fx.ring(fx.dust(0xF2C84B, 1.2F), Mth.lerp(pulse, 0.3, 1.2), 1.0, 0.1, 1 - pulse * 0.5);
        }
        if (t == 20) {
            fx.sound(SoundEvents.ANVIL_PLACE, 0.4F, 1.6F);
            fx.cloud(ParticleTypes.CRIT, 0, 0.3, 0, 16, 1.2, 0.05, 1.2, 0.1);
        }
        double w = fx.span(20, 30);
        if (w >= 0) {
            fx.ring(fx.dust(0xC8D0E0, 1.2F), Mth.lerp(w, 1.0, 2.0), FLOOR, 0.14, 1 - w);
        }
    }

    private static void berserkerCracks(Fx fx, double len, double keep) {
        ParticleOptions blood = fx.dust(0xC01818, 1.2F);
        ParticleOptions ember = fx.dust(0xFF7A1A, 1.2F);
        for (int k = 0; k < 6; k++) {
            double angle = k * Math.PI / 3 + 0.3;
            int n = (int) Math.ceil(len / 0.1);
            for (int i = 1; i <= n; i++) {
                double d = i * 0.1;
                double side = Math.sin(i * 1.7 + k * 2.3) * 0.12;
                if (fx.chance(keep)) {
                    fx.at(i % 3 == 0 ? blood : ember, Math.sin(angle) * d + Math.cos(angle) * side, FLOOR,
                            Math.cos(angle) * d - Math.sin(angle) * side);
                }
            }
        }
    }

    private static void berserkerJets(Fx fx, double len) {
        for (int k = 0; k < 6; k++) {
            double angle = k * Math.PI / 3 + 0.3;
            fx.fly(ParticleTypes.FLAME, Math.sin(angle) * len, FLOOR, Math.cos(angle) * len,
                    fx.spread(0.1), 1, fx.spread(0.1), 0.25);
        }
    }

    private static final int[] BERSERKER_BEATS = {0, 24, 44, 62, 78, 92, 104, 114, 122, 129, 135, 140, 144, 147};

    /**
     * Berserker: a heartbeat speeds up for seven seconds, red pulses and cracks spread over the ground,
     * rocks lift and blood mist rises, then a roar blows fire out of every crack.
     */
    private static void berserkerGrand(Fx fx) {
        int t = fx.age();
        ParticleOptions blood = fx.dust(0xB01818, 1.5F);
        ParticleOptions ember = fx.dust(0xFF7A1A, 1.0F);
        for (int k = 0; k < BERSERKER_BEATS.length; k++) {
            int b = BERSERKER_BEATS[k];
            if (t == b) {
                fx.sound(SoundEvents.WARDEN_HEARTBEAT, 1.0F, 0.8F + k * 0.04F);
                fx.cloud(blood, 0, 1.0, 0, 10, 0.25, 0.5, 0.25, 0);
            }
            double w = fx.span(b, b + 6);
            if (w >= 0) {
                fx.ring(blood, Mth.lerp(w, 0.3, 1.0 + k * 0.08), 1.0, 0.12, 1 - w * 0.4);
                fx.ring(ember, Mth.lerp(w, 0.2, 1.3 + k * 0.08), FLOOR, 0.14, 1 - w * 0.4);
            }
        }
        double len = Mth.clamp((t - 50) / 80.0, 0, 1) * 2.4;
        if (t >= 50 && t < 195 && fx.every(2)) {
            berserkerCracks(fx, len, fadeOut(t, 170, 195));
        }
        if (t >= 50 && t < 150 && fx.every(3)) {
            double angle = (int) (fx.rand() * 6) * Math.PI / 3 + 0.3;
            double d = fx.rand() * len;
            fx.fly(ParticleTypes.SMOKE, Math.sin(angle) * d, FLOOR, Math.cos(angle) * d, 0, 1, 0, 0.05);
        }
        if (t >= 100 && t < 150) {
            BlockParticleOption rock = new BlockParticleOption(ParticleTypes.BLOCK, fx.ground());
            double a = fx.rand() * 2 * Math.PI;
            double d = 0.6 + fx.rand() * 1.6;
            fx.cloud(rock, Math.sin(a) * d, 0.2 + (t - 100) / 60.0, Math.cos(a) * d, 1, 0.05, 0.3, 0.05, 0.02);
            fx.at(fx.dust(0x8A0A0A, 2.0F), Math.sin(a + 1) * 0.5, 0.2 + fx.rand() * 1.8, Math.cos(a + 1) * 0.5);
            if (t % 10 == 0) {
                fx.sound(SoundEvents.RAVAGER_STEP, 0.4F, 0.6F);
            }
        }
        if (t == 150) {
            fx.sound(SoundEvents.RAVAGER_ROAR, 1.0F, 1.0F);
            fx.burstBlock(Blocks.NETHERRACK, 0, 0.3, 0, 20, 0.2);
            fx.cloud(ParticleTypes.LAVA, 0, 0.2, 0, 10, 1.0, 0.05, 1.0, 0);
            fx.flash(1.0);
        }
        if (t >= 150 && t < 166) {
            berserkerJets(fx, 2.4);
            berserkerJets(fx, 2.4);
        }
        double dome = fx.span(150, 158);
        if (dome >= 0) {
            fx.sphere(blood, 0, 0.2, 0, Mth.lerp(dome, 0.5, 2.4), 50, 1 - dome * 0.5);
        }
        if (t >= 150 && fx.chance(fadeOut(t, 165, 200))) {
            double a = fx.rand() * 2 * Math.PI;
            fx.at(blood, Math.sin(a) * 0.45, 0.2 + fx.rand() * 1.6, Math.cos(a) * 0.45);
            fx.at(ParticleTypes.SMALL_FLAME, Math.sin(a + 2) * 0.5, 0.2 + fx.rand() * 1.6, Math.cos(a + 2) * 0.5);
        }
    }

    /** Berserker respawn: you slam the ground, debris flies out, then three wild axe sweeps around you. */
    private static void berserkerLight(Fx fx) {
        int t = fx.age();
        ParticleOptions blood = fx.dust(0xB01818, 1.4F);
        double gather = fx.span(0, 6);
        if (gather >= 0) {
            fx.ring(blood, Mth.lerp(gather, 1.2, 0.3), FLOOR, 0.1, 1);
        }
        if (t == 6) {
            fx.sound(SoundEvents.ANVIL_LAND, 0.6F, 0.5F);
            fx.sound(SoundEvents.RAVAGER_ROAR, 0.7F, 1.2F);
            fx.groundDebris(16, 1.4);
            fx.radial(new BlockParticleOption(ParticleTypes.BLOCK, fx.ground()), 0.2, 16, 0.3);
            fx.cloud(ParticleTypes.LAVA, 0, 0.2, 0, 4, 0.5, 0.05, 0.5, 0);
        }
        double shock = fx.span(6, 14);
        if (shock >= 0) {
            fx.ring(fx.dust(0xFF7A1A, 1.2F), Mth.lerp(shock, 0.3, 2.0), FLOOR, 0.12, 1 - shock * 0.5);
            fx.ring(blood, Mth.lerp(shock, 0.2, 1.6), FLOOR, 0.12, 1 - shock * 0.5);
        }
        double[] heights = {0.9, 1.4, 1.1};
        for (int k = 0; k < 3; k++) {
            int start = 10 + k * 6;
            double w = fx.span(start, start + 6);
            if (w < 0) {
                continue;
            }
            double dir = k % 2 == 0 ? 1 : -1;
            double a0 = k * 2.1;
            double lead = a0 + dir * w * 1.5 * Math.PI;
            fx.arcAt(blood, 0, heights[k], 0, 1.1, lead - dir * 1.2, lead, 0.09, 1);
            fx.at(ParticleTypes.FLAME, Math.sin(lead) * 1.1, heights[k], Math.cos(lead) * 1.1);
            if (t == start) {
                fx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.6F, 0.7F + k * 0.15F);
            }
        }
        if (t >= 6 && fx.chance(fadeOut(t, 24, 45))) {
            double a = fx.rand() * 2 * Math.PI;
            fx.at(blood, Math.sin(a) * 0.45, 0.2 + fx.rand() * 1.6, Math.cos(a) * 0.45);
        }
    }

    /** A halberd lying flat at height {@code y}, centred on the player, turned by {@code angle}. */
    private static void halberd(Fx fx, double angle, double y, double len, double reveal, double keep) {
        ParticleOptions wood = fx.dust(0x8B6B40, 1.0F);
        ParticleOptions steel = fx.dust(0xD0D8E8, 1.1F);
        ParticleOptions blade = fx.dust(0x6F9FD0, 1.1F);
        double dx = Math.sin(angle);
        double dz = Math.cos(angle);
        double px = Math.cos(angle);
        double pz = -Math.sin(angle);
        double shown = len * reveal;
        fx.line(wood, -dx * shown, y, -dz * shown, dx * shown, y, dz * shown, 0.1, keep);
        if (reveal < 1) {
            return;
        }
        double[][] head = {{0.72, 0.05}, {0.64, 0.42}, {0.97, 0.46}, {0.9, 0.05}};
        for (int i = 0; i < head.length - 1; i++) {
            fx.line(blade, dx * head[i][0] * len + px * head[i][1], y, dz * head[i][0] * len + pz * head[i][1],
                    dx * head[i + 1][0] * len + px * head[i + 1][1], y,
                    dz * head[i + 1][0] * len + pz * head[i + 1][1], 0.07, keep);
        }
        fx.line(steel, dx * 0.82 * len, y, dz * 0.82 * len, dx * 0.78 * len - px * 0.2, y,
                dz * 0.78 * len - pz * 0.2, 0.07, keep);
        fx.line(steel, dx * len, y, dz * len, dx * (len + 0.3), y, dz * (len + 0.3), 0.07, keep);
        fx.at(ParticleTypes.CRIT, dx * (len + 0.3), y, dz * (len + 0.3));
    }

    private static double halberdAngle(int t) {
        if (t <= 140) {
            return 0.04 * t + 0.8 * Math.pow(t, 3) / (3.0 * 140 * 140);
        }
        if (t <= 150) {
            return 42.93 + 0.84 * (t - 140);
        }
        int s = Math.min(t - 150, 25);
        return 51.33 + 0.5 * s - 0.01 * s * s;
    }

    /**
     * Halberdier: a halberd forms and spins around you faster and faster, its blade leaving a trail and
     * carving a circle, it rises overhead, then slams down flat with a 360 degree sweep and shockwave.
     */
    private static void halberdierGrand(Fx fx) {
        int t = fx.age();
        double len = 2.0;
        double angle = halberdAngle(t);
        double prev = halberdAngle(Math.max(0, t - 1));
        double y;
        if (t < 110) {
            y = 1.05;
        } else if (t < 147) {
            y = Mth.lerp(smooth((t - 110) / 37.0), 1.05, 2.6);
        } else if (t < 150) {
            y = Mth.lerp((t - 147) / 3.0, 2.6, 0.3);
        } else {
            y = 0.3;
        }
        halberd(fx, angle, y, len, Math.min(1, t / 30.0), fadeOut(t, 170, 190));
        if (t >= 60 && t < 147) {
            fx.arcAt(fx.dust(0xD0D8E8, 0.7F), 0, y, 0, len + 0.3, angle - 0.9, angle, 0.1, 0.6);
        }
        if (t > 0 && t < 147 && Math.floor(angle / Math.PI) != Math.floor(prev / Math.PI)) {
            fx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.35F, 0.6F + t / 200F);
        }
        ParticleOptions trench = fx.dust(0x6B5030, 1.0F);
        if (t >= 80 && t < 150) {
            fx.debrisAt(Math.sin(angle) * len, Math.cos(angle) * len, 1);
            if (fx.every(2)) {
                fx.ring(trench, len, FLOOR, 0.14, (t - 80) / 70.0);
            }
        }
        if (t == 110) {
            fx.sound(SoundEvents.ARMOR_EQUIP_CHAIN, 0.7F, 0.8F);
        }
        if (t == 150) {
            for (int i = 0; i < 16; i++) {
                double a = 2.0 * Math.PI * i / 16;
                fx.at(ParticleTypes.SWEEP_ATTACK, Math.sin(a) * len, 0.6, Math.cos(a) * len);
                fx.debrisAt(Math.sin(a) * len, Math.cos(a) * len, 3);
            }
            fx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.6F);
            fx.sound(SoundEvents.ANVIL_LAND, 0.5F, 0.6F);
            fx.groundDebris(12, 2.4);
        }
        double shock = fx.span(150, 165);
        if (shock >= 0) {
            fx.ring(fx.dust(0xD0D8E8, 1.3F), Mth.lerp(shock, 0.5, 2.8), FLOOR + 0.05, 0.14, 1 - shock * 0.6);
        }
        if (t >= 150 && t < 185 && fx.every(2)) {
            fx.ring(fx.dust(0xD0D8E8, 1.2F), len, FLOOR, 0.12, fadeOut(t, 160, 185));
            double a = fx.rand() * 2 * Math.PI;
            fx.at(ParticleTypes.CRIT, Math.sin(a) * len, 0.15, Math.cos(a) * len);
        }
    }

    /** Halberdier respawn: four halberds rise around you, lean in to meet above your head and clash. */
    private static void halberdierLight(Fx fx) {
        int t = fx.age();
        if (t >= 36) {
            return;
        }
        ParticleOptions wood = fx.dust(0x8B6B40, 1.0F);
        ParticleOptions blade = fx.dust(0x6F9FD0, 1.1F);
        double keep = fadeOut(t, 18, 36);
        for (int k = 0; k < 4; k++) {
            double a = Math.PI / 4 + k * Math.PI / 2;
            double x = Math.sin(a) * 1.2;
            double z = Math.cos(a) * 1.2;
            double tx;
            double ty;
            double tz;
            if (t < 8) {
                tx = x;
                ty = Mth.lerp(smooth(t / 7.0), -0.2, 2.0);
                tz = z;
            } else {
                double w = smooth(Math.min(1, (t - 8) / 6.0));
                tx = Mth.lerp(w, x, x * 0.08);
                ty = Mth.lerp(w, 2.0, 2.6);
                tz = Mth.lerp(w, z, z * 0.08);
            }
            fx.line(wood, x, FLOOR, z, tx, ty, tz, 0.1, keep);
            double bx = Math.cos(a) * 0.25;
            double bz = -Math.sin(a) * 0.25;
            double mx = Mth.lerp(0.85, x, tx);
            double my = Mth.lerp(0.85, FLOOR, ty);
            double mz = Mth.lerp(0.85, z, tz);
            fx.line(blade, mx, my, mz, mx + bx, my - 0.15, mz + bz, 0.06, keep);
            if (t == 0) {
                fx.debrisAt(x, z, 4);
            }
        }
        if (t == 0) {
            fx.sound(SoundEvents.ARMOR_EQUIP_CHAIN, 0.7F, 1.0F);
        }
        if (t == 14) {
            fx.cloud(ParticleTypes.CRIT, 0, 2.6, 0, 20, 0.15, 0.15, 0.15, 0.3);
            fx.sound(SoundEvents.ANVIL_LAND, 0.5F, 1.6F);
            fx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.7F, 0.9F);
        }
        double ring = fx.span(14, 22);
        if (ring >= 0) {
            fx.ring(fx.dust(0xD0D8E8, 1.2F), Mth.lerp(ring, 0.2, 1.3), 2.6, 0.1, 1 - ring);
        }
    }

    private static final int[] DUELIST_ORDER = {0, 3, 6, 1, 4, 7, 2, 5};

    /** One rapier thrust from the chest outward, visible for ten ticks. */
    private static void thrust(Fx fx, double angle, int start, double reach, int index) {
        int t = fx.age();
        double w = fx.span(start, start + 10);
        if (w < 0) {
            return;
        }
        double r = Mth.lerp(Math.min(1, (t - start) / 2.0), 0.4, reach);
        fx.line(fx.dust(0xF0F0F8, 0.8F), Math.sin(angle) * 0.4, 1.35, Math.cos(angle) * 0.4, Math.sin(angle) * r,
                1.35, Math.cos(angle) * r, 0.07, 1 - w);
        if (t == start) {
            fx.cloud(ParticleTypes.ENCHANTED_HIT, Math.sin(angle) * reach, 1.35, Math.cos(angle) * reach, 6, 0.05,
                    0.05, 0.05, 0.15);
            fx.sound(SoundEvents.PLAYER_ATTACK_CRIT, 0.5F, 1.2F + index * 0.04F);
        }
    }

    /**
     * Duelist: a big rose is drawn on the floor, two rounds of eight thrusts form a sixteen-pointed star,
     * the rapier twirls around you, then a flourish of light and cherry petals.
     */
    private static void duelistGrand(Fx fx) {
        int t = fx.age();
        ParticleOptions crimson = fx.dust(0xD02848, 0.8F);
        double radius = 2.2;
        if (t < 195) {
            double d = Math.min(1, t / 60.0);
            if (fx.every(2)) {
                DUELIST_ROSE.draw(fx, crimson, crimson, radius, FLOOR, 0, 0, d, fadeOut(t, 170, 195), 0.1);
            } else if (d < 1) {
                DUELIST_ROSE.draw(fx, crimson, crimson, radius, FLOOR, 0, Math.max(0, d - 0.03), d, 1, 0.1);
            }
            if (d < 1) {
                double[] tip = DUELIST_ROSE.point(d);
                fx.glyphPoint(ParticleTypes.CRIT, tip[0], tip[1], radius, FLOOR + 0.05, 0);
                fx.glyphPoint(fx.dust(0xFFFFFF, 1.6F), tip[0], tip[1], radius, FLOOR + 0.05, 0);
                if (fx.every(4)) {
                    fx.sound(SoundEvents.AMETHYST_CLUSTER_STEP, 0.4F, 0.9F + (float) d * 0.8F);
                }
            }
        }
        if (fx.chance(t < 150 ? 0.6 : fadeOut(t, 160, 200))) {
            double a = fx.rand() * 2 * Math.PI;
            double r = 0.6 + fx.rand() * 1.6;
            fx.at(ParticleTypes.CHERRY_LEAVES, Math.sin(a) * r, 1.8 + fx.rand() * 1.2, Math.cos(a) * r);
        }
        for (int i = 0; i < 8; i++) {
            thrust(fx, DUELIST_ORDER[i] * Math.PI / 4, 60 + i * 5, 2.2, i);
            thrust(fx, DUELIST_ORDER[i] * Math.PI / 4 + Math.PI / 8, 100 + i * 5, 2.2, i + 8);
        }
        double twirl = fx.span(140, 150);
        if (twirl >= 0) {
            double a = t * 0.9;
            fx.line(fx.dust(0xF0F0F8, 0.9F), -Math.sin(a) * 1.6, 1.35, -Math.cos(a) * 1.6, Math.sin(a) * 1.6, 1.35,
                    Math.cos(a) * 1.6, 0.08, 1);
            fx.at(ParticleTypes.CRIT, Math.sin(a) * 1.6, 1.35, Math.cos(a) * 1.6);
            fx.at(ParticleTypes.CRIT, -Math.sin(a) * 1.6, 1.35, -Math.cos(a) * 1.6);
            if (t % 3 == 0) {
                fx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.3F, 1.6F);
            }
        }
        if (t == 150) {
            for (int i = 0; i < 16; i++) {
                double a = i * Math.PI / 8;
                fx.line(ParticleTypes.END_ROD, Math.sin(a) * 0.4, 1.35, Math.cos(a) * 0.4, Math.sin(a) * 2.2, 1.35,
                        Math.cos(a) * 2.2, 0.16, 1);
            }
            fx.radial(ParticleTypes.ENCHANTED_HIT, 1.35, 24, 0.3);
            fx.cloud(ParticleTypes.CHERRY_LEAVES, 0, 1.6, 0, 40, 1.4, 0.8, 1.4, 0);
            fx.sound(SoundEvents.PLAYER_ATTACK_STRONG, 0.8F, 1.2F);
            fx.sound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.5F);
        }
        double w = fx.span(150, 166);
        if (w >= 0) {
            fx.ring(fx.dust(0xD02848, 1.2F), Mth.lerp(w, 1.2, 2.2), 1.35, 0.1, 1 - w);
        }
    }

    /** Duelist respawn: a small rose spins up from your feet to above your head and bursts into petals. */
    private static void duelistLight(Fx fx) {
        int t = fx.age();
        if (t == 0) {
            fx.sound(SoundEvents.AMETHYST_CLUSTER_STEP, 0.6F, 1.2F);
        }
        if (t < 16) {
            double y = Mth.lerp(smooth(t / 16.0), FLOOR, 2.3);
            ParticleOptions crimson = fx.dust(0xD02848, 1.0F);
            DUELIST_ROSE.draw(fx, crimson, crimson, 1.0, y, t * 0.3, 0, 1, 1, 0.1);
            fx.at(ParticleTypes.CRIT, fx.spread(0.3), y, fx.spread(0.3));
            if (t % 4 == 0) {
                fx.sound(SoundEvents.PLAYER_ATTACK_CRIT, 0.3F, 1.4F + t * 0.03F);
            }
        }
        if (t == 16) {
            fx.cloud(ParticleTypes.CHERRY_LEAVES, 0, 2.3, 0, 25, 0.8, 0.3, 0.8, 0);
            fx.sphereOut(ParticleTypes.ENCHANTED_HIT, 0, 2.3, 0, 20, 0.25);
            fx.sound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.4F);
            fx.sound(SoundEvents.PLAYER_ATTACK_STRONG, 0.6F, 1.4F);
        }
        double ring = fx.span(16, 24);
        if (ring >= 0) {
            fx.ring(fx.dust(0xD02848, 1.2F), Mth.lerp(ring, 0.5, 1.4), 2.3, 0.1, 1 - ring);
        }
        if (t > 16 && fx.chance(fadeOut(t, 28, 50))) {
            double a = fx.rand() * 2 * Math.PI;
            fx.at(ParticleTypes.CHERRY_LEAVES, Math.sin(a) * 1.1, 2.1, Math.cos(a) * 1.1);
        }
    }

    // =========================================================================
    // Rangers
    // =========================================================================

    /** One arrow falling onto a spot and sticking there with a small target around it. */
    private static void archerArrow(Fx fx, int i, int land, double minDist, double maxDist, int fadeFrom,
                                    int fadeTo) {
        int t = fx.age();
        double a = hash(i, 1) * 2 * Math.PI;
        double d = minDist + hash(i, 2) * (maxDist - minDist);
        double x = Math.sin(a) * d;
        double z = Math.cos(a) * d;
        ParticleOptions straw = fx.dust(0xF0E0A0, 0.9F);
        if (t >= land - 8 && t < land) {
            double y = FLOOR + (land - t) * 0.45;
            fx.line(straw, x, y, z, x, y + 0.7, z, 0.1, 1);
            fx.at(ParticleTypes.END_ROD, x, y, z);
            fx.at(fx.dust(0xD04040, 1.2F), x, y + 0.7, z);
        }
        if (t == land) {
            fx.sound(SoundEvents.ARROW_HIT, 0.5F, 0.9F + (float) hash(i, 3) * 0.4F);
            fx.cloud(ParticleTypes.CRIT, x, 0.2, z, 4, 0.1, 0.1, 0.1, 0.1);
            fx.debrisAt(x, z, 2);
        }
        if (t >= land && t < fadeTo && fx.every(2)) {
            double keep = fadeOut(t, fadeFrom, fadeTo);
            fx.ringAt(fx.dust(0xD04040, 1.0F), x, FLOOR, z, 0.36, 0.08, keep);
            fx.ringAt(fx.dust(0xF0F0F0, 1.0F), x, FLOOR, z, 0.24, 0.08, keep);
            fx.ringAt(fx.dust(0xD04040, 1.0F), x, FLOOR, z, 0.1, 0.08, keep);
            fx.line(straw, x, FLOOR, z, x + Math.sin(a) * 0.12, 0.5, z + Math.cos(a) * 0.12, 0.08, keep);
        }
    }

    /** Big target on the floor around the player, rings alternating red and white. */
    private static void archerTarget(Fx fx, int rings, double step, double keep) {
        for (int k = 1; k <= rings; k++) {
            fx.ring(fx.dust(k % 2 == 1 ? 0xD04040 : 0xF0F0F0, 1.3F), k * step, FLOOR, 0.1, keep);
        }
    }

    /**
     * Archer: you draw, twelve arrows shoot into the sky, thirty rain down around you onto small targets,
     * then one golden arrow falls straight down and a huge target lights up.
     */
    private static void archerGrand(Fx fx) {
        int t = fx.age();
        if (t == 4) {
            fx.sound(SoundEvents.CROSSBOW_LOADING_END, 0.7F, 0.9F);
        }
        if (t < 12 && fx.every(2)) {
            fx.radialIn(ParticleTypes.END_ROD, 1.2, 1.4, 8, 0.1);
        }
        for (int i = 0; i < 12; i++) {
            int launch = 12 + i * 5;
            if (t >= launch && t < launch + 8) {
                double a = i * Math.PI / 6;
                double x = Math.sin(a) * 0.35;
                double z = Math.cos(a) * 0.35;
                double y = 1.5 + (t - launch) * 0.8;
                fx.line(fx.dust(0xF0E0A0, 0.9F), x, y, z, x, y + 0.7, z, 0.1, 1);
                fx.at(ParticleTypes.END_ROD, x, y + 0.7, z);
                if (t == launch) {
                    fx.sound(SoundEvents.ARROW_SHOOT, 0.6F, 0.8F + i * 0.04F);
                }
            }
        }
        for (int i = 0; i < 30; i++) {
            archerArrow(fx, i, 80 + i * 2, 0.8, 2.6, 170, 190);
        }
        double fall = fx.span(138, 150);
        if (fall >= 0) {
            double y = Mth.lerp(fall * fall, 9.0, FLOOR);
            fx.line(fx.dust(0xF2C84B, 1.2F), 0, y, 0, 0, y + 1.0, 0, 0.08, 1);
            fx.at(ParticleTypes.END_ROD, 0, y, 0);
            fx.at(fx.dust(0xFFFFFF, 1.6F), 0, y + 1.0, 0);
            if (t == 138) {
                fx.sound(SoundEvents.ARROW_SHOOT, 1.0F, 0.5F);
            }
        }
        if (t == 150) {
            fx.sound(SoundEvents.ARROW_HIT_PLAYER, 1.0F, 1.0F);
            fx.burstItem(Items.FEATHER, 0, 2.0, 0, 14, 0.15);
            fx.sphereOut(ParticleTypes.END_ROD, 0, 1.2, 0, 24, 0.14);
            fx.flash(1.0);
        }
        if (t >= 150 && t < 185 && fx.every(2)) {
            archerTarget(fx, 4, 0.6, fadeOut(t, 165, 185));
        }
    }

    /** Archer respawn: eight arrows fly out from your chest in every direction and stick in the ground. */
    private static void archerLight(Fx fx) {
        int t = fx.age();
        ParticleOptions straw = fx.dust(0xF0E0A0, 0.9F);
        if (t == 2) {
            fx.sound(SoundEvents.ARROW_SHOOT, 0.8F, 1.0F);
            fx.sound(SoundEvents.CROSSBOW_SHOOT, 0.5F, 1.2F);
        }
        for (int k = 0; k < 8; k++) {
            double a = k * Math.PI / 4 + Math.PI / 8;
            double sx = Math.sin(a);
            double sz = Math.cos(a);
            double w = fx.span(2, 10);
            if (w >= 0) {
                double d = Mth.lerp(w, 0.3, 1.6);
                double y = Mth.lerp(w, 1.4, FLOOR) + 0.8 * Math.sin(Math.PI * w);
                double pw = Math.max(0, w - 0.15);
                double pd = Mth.lerp(pw, 0.3, 1.6);
                double py = Mth.lerp(pw, 1.4, FLOOR) + 0.8 * Math.sin(Math.PI * pw);
                fx.line(straw, sx * pd, py, sz * pd, sx * d, y, sz * d, 0.08, 1);
                fx.at(ParticleTypes.END_ROD, sx * d, y, sz * d);
            }
            if (t >= 10 && t < 44 && fx.every(2)) {
                double keep = fadeOut(t, 26, 44);
                fx.line(straw, sx * 1.6, FLOOR, sz * 1.6, sx * 1.35, 0.45, sz * 1.35, 0.08, keep);
                fx.ringAt(fx.dust(0xD04040, 1.0F), sx * 1.6, FLOOR, sz * 1.6, 0.2, 0.07, keep);
            }
            if (t == 10) {
                fx.cloud(ParticleTypes.CRIT, sx * 1.6, 0.2, sz * 1.6, 3, 0.05, 0.05, 0.05, 0.1);
            }
        }
        if (t == 10) {
            fx.sound(SoundEvents.ARROW_HIT, 0.7F, 1.0F);
        }
        if (t == 12) {
            fx.sound(SoundEvents.ARROW_HIT_PLAYER, 0.8F, 1.3F);
            fx.burstItem(Items.FEATHER, 0, 1.6, 0, 6, 0.12);
        }
    }

    /** A footprint: 0 wolf, 1 bird, 2 deer, 3 bear. {@code heading} is the walking direction. */
    private static void pawPrint(Fx fx, int type, double x, double z, double heading, double keep) {
        if (!fx.chance(keep)) {
            return;
        }
        ParticleOptions mark = fx.dust(type == 1 ? 0xE8E0C8 : 0x5A3A1C, 1.0F);
        double fx1 = Math.sin(heading);
        double fz1 = Math.cos(heading);
        double sx = Math.cos(heading);
        double sz = -Math.sin(heading);
        switch (type) {
            case 1 -> {
                for (int k = -1; k <= 1; k++) {
                    fx.line(mark, x, FLOOR, z, x + fx1 * 0.16 + sx * k * 0.1, FLOOR, z + fz1 * 0.16 + sz * k * 0.1,
                            0.05, 1);
                }
                fx.line(mark, x, FLOOR, z, x - fx1 * 0.07, FLOOR, z - fz1 * 0.07, 0.05, 1);
            }
            case 2 -> {
                for (int k = -1; k <= 1; k += 2) {
                    fx.line(mark, x - fx1 * 0.08 + sx * k * 0.05, FLOOR, z - fz1 * 0.08 + sz * k * 0.05,
                            x + fx1 * 0.08 + sx * k * 0.02, FLOOR, z + fz1 * 0.08 + sz * k * 0.02, 0.04, 1);
                }
            }
            default -> {
                double size = type == 3 ? 1.4 : 1.0;
                int toes = type == 3 ? 5 : 4;
                fx.at(mark, x, FLOOR, z);
                fx.at(mark, x + sx * 0.05 * size, FLOOR, z + sz * 0.05 * size);
                fx.at(mark, x - sx * 0.05 * size, FLOOR, z - sz * 0.05 * size);
                for (int k = 0; k < toes; k++) {
                    double side = (k - (toes - 1) / 2.0) * 0.07 * size;
                    double ahead = (0.14 - Math.abs(side) * 0.4) * size;
                    fx.at(mark, x + fx1 * ahead + sx * side, FLOOR, z + fz1 * ahead + sz * side);
                }
            }
        }
    }

    /** Tracks of four animals walking in towards the player from four sides. */
    private static void beastTracks(Fx fx, int steps, int first, int interval, double from, double to,
                                    double keep) {
        int t = fx.age();
        for (int k = 0; k < 4; k++) {
            double a0 = Math.PI / 4 + k * Math.PI / 2;
            for (int j = 0; j < steps; j++) {
                int at = first + j * interval;
                if (t < at) {
                    break;
                }
                if (t == at && k == 0) {
                    fx.sound(SoundEvents.GRASS_STEP, 0.4F, 0.9F + j * 0.03F);
                }
                double dist = Mth.lerp(j / (double) (steps - 1), from, to);
                double angle = a0 + 0.35 * Math.sin(j * 0.9 + k);
                double side = (j % 2 == 0 ? 0.1 : -0.1);
                double x = Math.sin(angle) * dist + Math.cos(angle) * side;
                double z = Math.cos(angle) * dist - Math.sin(angle) * side;
                pawPrint(fx, k, x, z, angle + Math.PI, keep);
            }
        }
    }

    /** A glowing spirit with a fading tail, circling the player. */
    private static void spiritOrb(Fx fx, int rgb, double angle, double radius, double y, double dir, double keep) {
        if (!fx.chance(keep)) {
            return;
        }
        for (int m = 0; m < 6; m++) {
            double a = angle - dir * 0.12 * m;
            fx.at(fx.dust(rgb, 2.0F - m * 0.28F), Math.sin(a) * radius, y, Math.cos(a) * radius);
        }
    }

    /**
     * Beastmaster: tracks of a wolf, bird, deer and bear walk in from far around you, four animal spirits
     * gather and run with you, then they leap into you with a howl.
     */
    private static void beastmasterGrand(Fx fx) {
        int t = fx.age();
        if (t < 130 && fx.every(2)) {
            beastTracks(fx, 16, 2, 5, 4.0, 0.9, fadeOut(t, 100, 130));
        }
        double in = fx.span(70, 150);
        if (in >= 0) {
            double keep = Math.min(1, (t - 70) / 10.0);
            double pull = t < 144 ? 1 : 1 - (t - 144) / 6.0;
            double birdY = t < 100 ? 3.2 : Mth.lerp(Math.min(1, (t - 100) / 20.0), 3.2, 2.5);
            spiritOrb(fx, 0x9EE07A, t * 0.25, 1.4 * pull, 0.45 + 0.1 * Math.abs(Math.sin(t * 0.5)), 1, keep);
            spiritOrb(fx, 0xF0E0A0, 1 - t * 0.18, 1.8 * pull, 1.0, -1, keep);
            spiritOrb(fx, 0xE08A3A, 3 - t * 0.3, 1.0 * pull, 0.35, -1, keep);
            spiritOrb(fx, 0xFFFFFF, 2 + t * 0.3, 0.9 * pull, birdY + 0.2 * Math.sin(t * 0.3), 1, keep);
        }
        if (t == 80) {
            fx.sound(SoundEvents.WOLF_AMBIENT, 0.5F, 1.0F);
        }
        if (t == 95) {
            fx.sound(SoundEvents.FOX_AMBIENT, 0.5F, 1.1F);
        }
        if (t == 110) {
            fx.sound(SoundEvents.PARROT_AMBIENT, 0.5F, 1.0F);
        }
        if (t == 125) {
            fx.sound(SoundEvents.WOLF_PANT, 0.5F, 1.0F);
        }
        if (t == 150) {
            fx.sound(SoundEvents.WOLF_HOWL, 1.0F, 1.0F);
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4;
                fx.at(ParticleTypes.HEART, Math.sin(a) * 0.7, 2.1, Math.cos(a) * 0.7);
            }
            fx.cloud(ParticleTypes.HAPPY_VILLAGER, 0, 1.0, 0, 30, 0.8, 0.8, 0.8, 0);
            fx.burstItem(Items.FEATHER, 0, 1.8, 0, 10, 0.15);
            fx.flash(1.0);
        }
        if (t > 150) {
            double w = (t - 150) / 50.0;
            spiritOrb(fx, 0xFFFFFF, t * 0.2, 0.8, 2.3 + w * 2.5, 1, 1 - w);
            if (fx.every(2)) {
                double a = fx.rand() * 2 * Math.PI;
                fx.fly(ParticleTypes.COMPOSTER, Math.sin(a) * 0.9, 0.3, Math.cos(a) * 0.9, 0, 1, 0, 0.04);
            }
        }
    }

    /** Beastmaster respawn: leaves whirl up around you in three strands, then a howl and hearts. */
    private static void beastmasterLight(Fx fx) {
        int t = fx.age();
        if (t < 16) {
            double h = Mth.lerp(t / 16.0, 0.2, 2.2);
            for (int k = 0; k < 3; k++) {
                for (double y = 0; y <= h; y += 0.12) {
                    double a = y * 3 + k * 2 * Math.PI / 3 + t * 0.25;
                    double r = 0.9 - y * 0.2;
                    fx.at(fx.dust(k == 1 ? 0x7A5028 : 0x9EE07A, 1.1F), Math.sin(a) * r, y, Math.cos(a) * r);
                }
            }
            fx.at(ParticleTypes.COMPOSTER, fx.spread(0.4), h, fx.spread(0.4));
            if (t % 5 == 0) {
                fx.sound(SoundEvents.GRASS_STEP, 0.5F, 1.2F);
            }
        }
        if (t == 16) {
            fx.sound(SoundEvents.WOLF_HOWL, 0.9F, 1.1F);
            for (int i = 0; i < 5; i++) {
                double a = i * 2 * Math.PI / 5;
                fx.at(ParticleTypes.HEART, Math.sin(a) * 0.5, 2.1, Math.cos(a) * 0.5);
            }
            fx.cloud(ParticleTypes.HAPPY_VILLAGER, 0, 1.4, 0, 16, 0.6, 0.6, 0.6, 0);
        }
        if (t > 16 && fx.chance(fadeOut(t, 26, 48))) {
            double a = fx.rand() * 2 * Math.PI;
            double r = 0.4 + fx.rand() * 1.0;
            fx.at(ParticleTypes.COMPOSTER, Math.sin(a) * r, 0.5 + fx.rand() * 1.8, Math.cos(a) * r);
        }
    }

    /** Two bear-trap jaws hinged left and right; {@code close} 0 lies flat, 1 is shut above the player. */
    private static void trapJaws(Fx fx, double radius, double close, double keep) {
        ParticleOptions iron = fx.dust(0xA0A0A8, 1.1F);
        double phi = close * Math.PI / 2;
        int count = (int) Math.ceil(Math.PI * radius / 0.08);
        for (int k = -1; k <= 1; k += 2) {
            for (int i = 0; i <= count; i++) {
                if (!fx.chance(keep)) {
                    continue;
                }
                double th = Math.PI * i / count;
                double along = Math.cos(th) * radius;
                double out = Math.sin(th) * radius;
                double fwd = k * Math.cos(phi) * out;
                double up = FLOOR + Math.sin(phi) * out;
                fx.local(iron, fwd, up, along);
                if (i % 5 == 2) {
                    fx.localLine(iron, fwd, up, along, fwd * 0.82, FLOOR + (up - FLOOR) * 0.82, along * 0.82, 0.05, 1);
                }
            }
        }
    }

    private static void trapSpikes(Fx fx, int count, double radius, int first, int fadeFrom, int fadeTo) {
        int t = fx.age();
        ParticleOptions iron = fx.dust(0x8A8A92, 1.0F);
        for (int j = 0; j < count; j++) {
            int born = first + j;
            if (t < born || t >= fadeTo) {
                continue;
            }
            double a = 2.0 * Math.PI * j / count;
            double x = Math.sin(a) * radius;
            double z = Math.cos(a) * radius;
            double h = Math.min(1, (t - born) / 3.0) * 0.35;
            if (t == born) {
                fx.at(ParticleTypes.CRIT, x, h + 0.1, z);
                if (j % 2 == 0) {
                    fx.sound(SoundEvents.TRIPWIRE_CLICK_ON, 0.5F, 1.0F + j * 0.03F);
                }
            }
            if (t < born + 4 || fx.every(2)) {
                fx.line(iron, x, FLOOR, z, x, FLOOR + h, z, 0.07, fadeOut(t, fadeFrom, fadeTo));
            }
        }
    }

    /**
     * Trapper: a lasso spins over your head, drops and pulls tight round your feet, two rings of spikes pop up,
     * tripwires stretch between them, then huge bear-trap jaws rise and snap shut over you.
     */
    private static void trapperGrand(Fx fx) {
        int t = fx.age();
        ParticleOptions rope = fx.dust(0x9B7B48, 1.1F);
        double cy;
        double r;
        if (t < 50) {
            cy = 2.6;
            r = 0.9;
        } else if (t < 70) {
            double w = (t - 50) / 20.0;
            cy = Mth.lerp(w, 2.6, FLOOR);
            r = Mth.lerp(w, 0.9, 2.4);
        } else {
            cy = FLOOR;
            r = Mth.lerp(smooth((t - 70) / 18.0), 2.4, 0.45);
        }
        if (t < 88 || t < 185 && fx.every(2)) {
            fx.ring(rope, r, cy, 0.1, fadeOut(t, 160, 185));
        }
        if (t < 70) {
            double a = t * 0.6;
            fx.at(fx.dust(0x9B7B48, 1.8F), Math.sin(a) * r, cy, Math.cos(a) * r);
            if (t < 50) {
                fx.line(rope, Math.sin(a) * r, cy, Math.cos(a) * r, 0, 1.5, 0, 0.1, 1);
            }
            if (t % 6 == 0) {
                fx.sound(SoundEvents.PLAYER_ATTACK_NODAMAGE, 0.4F, 1.1F + t / 200F);
            }
        }
        if (t == 88) {
            fx.sound(SoundEvents.LEASH_KNOT_PLACE, 0.8F, 0.8F);
        }
        trapSpikes(fx, 16, 1.4, 90, 170, 190);
        trapSpikes(fx, 20, 2.2, 106, 170, 190);
        if (t >= 126 && t < 175 && fx.every(2)) {
            ParticleOptions wire = fx.dust(0xE8E8E0, 0.7F);
            double keep = Math.min(1, (t - 126) / 8.0) * fadeOut(t, 150, 175);
            for (int k = 0; k < 5; k++) {
                double a1 = k * 2 * Math.PI / 5;
                double a2 = a1 + 4 * Math.PI / 5;
                fx.line(wire, Math.sin(a1) * 2.2, 0.22, Math.cos(a1) * 2.2, Math.sin(a2) * 2.2, 0.22,
                        Math.cos(a2) * 2.2, 0.12, keep);
            }
            if (t < 136 && t % 3 == 0) {
                fx.sound(SoundEvents.TRIPWIRE_ATTACH, 0.4F, 1.2F);
            }
        }
        double close = fx.span(135, 150);
        if (close >= 0 || t >= 150 && t < 185) {
            trapJaws(fx, 1.3, close >= 0 ? close * close : 1, fadeOut(t, 162, 185));
        }
        if (t == 135) {
            fx.sound(SoundEvents.CHAIN_PLACE, 0.6F, 0.7F);
        }
        if (t == 150) {
            fx.sound(SoundEvents.IRON_TRAPDOOR_CLOSE, 1.0F, 0.7F);
            fx.sound(SoundEvents.ANVIL_LAND, 0.5F, 1.8F);
            fx.burstItem(Items.IRON_NUGGET, 0, 1.3, 0, 14, 0.2);
            fx.cloud(ParticleTypes.CRIT, 0, 1.3, 0, 24, 0.3, 0.1, 0.3, 0.3);
        }
    }

    /** Trapper respawn: a rope net drops over you from above and pulls tight. */
    private static void trapperLight(Fx fx) {
        int t = fx.age();
        if (t >= 38) {
            return;
        }
        ParticleOptions rope = fx.dust(0x9B7B48, 1.1F);
        double fall = Math.min(1, t / 10.0);
        double cy = Mth.lerp(fall * fall, 2.8, 0.15);
        double rad = t < 10 ? 1.2 : Mth.lerp(smooth((t - 10) / 10.0), 1.2, 0.8);
        double keep = fadeOut(t, 22, 38);
        if (t < 12 || fx.every(2)) {
            for (int k = 0; k < 10; k++) {
                double angle = k * Math.PI / 5;
                for (int i = 0; i <= 7; i++) {
                    double lat = i / 7.0 * Math.PI / 2;
                    if (fx.chance(keep)) {
                        fx.at(rope, Math.sin(angle) * Math.cos(lat) * rad, cy + Math.sin(lat) * 1.6,
                                Math.cos(angle) * Math.cos(lat) * rad);
                    }
                }
            }
            for (double h = 0.35; h < 1.0; h += 0.3) {
                fx.ring(rope, rad * Math.cos(h * Math.PI / 2), cy + Math.sin(h * Math.PI / 2) * 1.6, 0.12, keep);
            }
        }
        if (t == 0) {
            fx.sound(SoundEvents.PLAYER_ATTACK_NODAMAGE, 0.7F, 0.8F);
        }
        if (t == 10) {
            fx.sound(SoundEvents.LEASH_KNOT_PLACE, 0.9F, 0.8F);
            fx.sound(SoundEvents.CHAIN_PLACE, 0.5F, 1.0F);
            fx.groundDebris(8, 1.2);
        }
    }

    /** Whirlwind: rings of wind streaks, wider higher up. */
    private static void scoutWind(Fx fx, int layers, double keep) {
        int t = fx.age();
        ParticleOptions wind = fx.dust(0xE8FAFF, 0.9F);
        ParticleOptions sky = fx.dust(0xB8F0FF, 0.8F);
        for (int l = 0; l < layers; l++) {
            double y = 0.2 + l * 0.5;
            double r = 0.6 + l * 0.22;
            for (int j = 0; j < 3; j++) {
                double phase = t * 0.45 * (1 + l * 0.1) + j * 2 * Math.PI / 3 + l;
                fx.arcAt(j == 0 ? sky : wind, 0, y, 0, r, phase, phase + 0.8, 0.1, keep);
            }
            if (fx.chance(0.3 * keep)) {
                double a = fx.rand() * 2 * Math.PI;
                fx.fly(ParticleTypes.WHITE_SMOKE, Math.sin(a) * r, y, Math.cos(a) * r, Math.cos(a), 0.1, -Math.sin(a),
                        0.12);
            }
        }
    }

    private static void scoutCompass(Fx fx, double len, double keep) {
        ParticleOptions pointer = fx.dust(0x7FD46B, 1.1F);
        for (int k = 0; k < 4; k++) {
            double angle = k * Math.PI / 2;
            double dx = Math.sin(angle);
            double dz = Math.cos(angle);
            fx.line(pointer, dx * 0.3, FLOOR, dz * 0.3, dx * len, FLOOR, dz * len, 0.1, keep);
            fx.line(pointer, dx * len, FLOOR, dz * len, dx * (len - 0.22) + dz * 0.16, FLOOR,
                    dz * (len - 0.22) - dx * 0.16, 0.08, keep);
            fx.line(pointer, dx * len, FLOOR, dz * len, dx * (len - 0.22) - dz * 0.16, FLOOR,
                    dz * (len - 0.22) + dx * 0.16, 0.08, keep);
            fx.at(ParticleTypes.END_ROD, dx * len, FLOOR + 0.03, dz * len);
        }
    }

    /** A hawk made of two flapping wings, flying tangent to a circle around the player. */
    private static void hawk(Fx fx, double angle, double radius, double y, double keep) {
        if (!fx.chance(keep)) {
            return;
        }
        ParticleOptions body = fx.dust(0x7A5028, 1.4F);
        ParticleOptions tip = fx.dust(0xF0F0F0, 1.1F);
        double x = Math.sin(angle) * radius;
        double z = Math.cos(angle) * radius;
        double tx = Math.cos(angle);
        double tz = -Math.sin(angle);
        double ox = Math.sin(angle);
        double oz = Math.cos(angle);
        double flap = 0.22 * Math.sin(fx.age() * 0.9);
        fx.at(body, x, y, z);
        fx.at(body, x - tx * 0.2, y, z - tz * 0.2);
        for (int side = -1; side <= 1; side += 2) {
            double mx = x + ox * side * 0.25;
            double mz = z + oz * side * 0.25;
            double ex = x + ox * side * 0.5;
            double ez = z + oz * side * 0.5;
            fx.line(body, x, y, z, mx, y + flap * 0.5, mz, 0.06, 1);
            fx.line(body, mx, y + flap * 0.5, mz, ex - tx * 0.1, y + flap, ez - tz * 0.1, 0.06, 1);
            fx.at(tip, ex - tx * 0.1, y + flap, ez - tz * 0.1);
        }
    }

    /**
     * Scout: a tall whirlwind builds around you for seven seconds while a hawk spirals down from high above
     * to your shoulder, then a gust bursts out and compass arrows point to all four sides.
     */
    private static void scoutGrand(Fx fx) {
        int t = fx.age();
        if (t < 170) {
            scoutWind(fx, 8, t < 150 ? Math.min(1, t / 60.0) : fadeOut(t, 150, 170));
        }
        if (t < 150 && t % 10 == 0) {
            fx.sound(SoundEvents.PHANTOM_FLAP, 0.4F, 1.2F + t / 300F);
        }
        double q = fx.span(20, 150);
        if (q >= 0) {
            double e = smooth(q);
            hawk(fx, t * 0.12, Mth.lerp(e, 2.8, 0.6), Mth.lerp(e, 6.0, 1.9), 1);
            if (fx.chance(0.05)) {
                fx.burstItem(Items.FEATHER, Math.sin(t * 0.12) * 2, 3, Math.cos(t * 0.12) * 2, 1, 0.02);
            }
        }
        if (t == 150) {
            fx.at(ParticleTypes.GUST, 0, 1.6, 0);
            fx.radial(ParticleTypes.CLOUD, 0.3, 24, 0.3);
            fx.radial(ParticleTypes.CLOUD, 1.4, 16, 0.25);
            fx.burstItem(Items.FEATHER, 0, 1.8, 0, 14, 0.15);
            fx.sound(SoundEvents.BREEZE_WIND_CHARGE_BURST, 1.0F, 1.0F);
        }
        if (t > 150) {
            double w = (t - 150) / 50.0;
            hawk(fx, t * 0.12, Mth.lerp(w, 0.6, 1.6), Mth.lerp(w, 1.9, 6.5), 1 - w);
        }
        if (t >= 152 && t < 190 && fx.every(2)) {
            scoutCompass(fx, 0.4 + Math.min(1, (t - 152) / 8.0) * 1.4, fadeOut(t, 175, 190));
        }
    }

    /** Scout respawn: a green comet races two laps around you, then feathers and compass arrows. */
    private static void scoutLight(Fx fx) {
        int t = fx.age();
        if (t == 0) {
            fx.sound(SoundEvents.PHANTOM_FLAP, 0.6F, 1.5F);
        }
        if (t < 16) {
            double a = t * 0.8;
            double y = 0.9 + 0.3 * Math.sin(t * 0.4);
            spiritOrb(fx, 0x7FD46B, a, 1.1, y, 1, 1);
            fx.at(ParticleTypes.WHITE_SMOKE, Math.sin(a - 0.4) * 1.1, y, Math.cos(a - 0.4) * 1.1);
            if (t % 4 == 0) {
                fx.debrisAt(Math.sin(a) * 1.1, Math.cos(a) * 1.1, 2);
                fx.sound(SoundEvents.GRASS_STEP, 0.4F, 1.4F);
            }
        }
        if (t == 16) {
            fx.burstItem(Items.FEATHER, 0, 1.6, 0, 8, 0.15);
            fx.radial(ParticleTypes.CLOUD, 0.3, 12, 0.2);
            fx.sound(SoundEvents.PHANTOM_SWOOP, 0.6F, 1.4F);
        }
        if (t >= 16 && t < 42 && fx.every(2)) {
            scoutCompass(fx, 0.4 + Math.min(1, (t - 16) / 5.0) * 0.9, fadeOut(t, 30, 42));
        }
    }

    // =========================================================================
    // Rogues
    // =========================================================================

    /** X-shaped slash close to the player on the side at {@code angle}. */
    private static void crossSlash(Fx fx, double angle, double dist, double y, double half) {
        double cx = Math.sin(angle) * dist;
        double cz = Math.cos(angle) * dist;
        double tx = Math.cos(angle);
        double tz = -Math.sin(angle);
        for (int s = -1; s <= 1; s += 2) {
            fx.panelLine(fx.dust(0x9A3AD8, 1.1F), cx, cz, tx, tz, -half, y + s * half, half, y - s * half, 0.07, 1);
            fx.panelLine(ParticleTypes.CRIT, cx, cz, tx, tz, -half, y + s * half, half, y - s * half, 0.2, 1);
        }
    }

    /** A shadow clone at {@code angle} that dashes into the player at tick {@code dash}. */
    private static void shadowClone(Fx fx, double angle, double dist, int appear, int dash) {
        int t = fx.age();
        if (t < appear || t > dash + 3) {
            return;
        }
        ParticleOptions shadow = fx.dust(0x201828, 1.3F);
        ParticleOptions violet = fx.dust(0x9A3AD8, 0.9F);
        double d = t < dash ? dist : Mth.lerp((t - dash) / 3.0, dist, 0.35);
        double x = Math.sin(angle) * d;
        double z = Math.cos(angle) * d;
        if (t < dash + 3 && (t < dash || fx.every(1))) {
            double keep = Math.min(1, (t - appear) / 8.0) * (fx.chance(0.15) ? 0.3 : 1);
            if (t >= dash || fx.every(2)) {
                fx.figure(shadow, x, z, keep * 0.9);
                fx.figure(violet, x, z, keep * 0.3);
            }
        }
        if (t >= dash) {
            fx.line(violet, Math.sin(angle) * dist, 1.0, Math.cos(angle) * dist, x, 1.0, z, 0.1, 1);
        }
        if (t == dash + 3) {
            crossSlash(fx, angle, 0.4, 1.1, 0.35);
            fx.cloud(ParticleTypes.SMOKE, x, 1.0, z, 6, 0.1, 0.3, 0.1, 0.02);
            fx.sound(SoundEvents.PLAYER_ATTACK_CRIT, 0.6F, 1.3F);
            fx.sound(SoundEvents.ENDERMAN_TELEPORT, 0.3F, 1.8F);
        }
    }

    private static void glintingEyes(Fx fx, int from, int to) {
        int t = fx.age();
        if (t < from || t >= to || t % 6 >= 4) {
            return;
        }
        int pair = t / 6;
        double a = hash(pair, 7) * 2 * Math.PI;
        double r = 1.0 + hash(pair, 8) * 0.8;
        double x = Math.sin(a) * r;
        double z = Math.cos(a) * r;
        double tx = Math.cos(a) * 0.06;
        double tz = -Math.sin(a) * 0.06;
        ParticleOptions eye = fx.dust(0xC060FF, 1.2F);
        fx.at(eye, x + tx, 1.5, z + tz);
        fx.at(eye, x - tx, 1.5, z - tz);
    }

    /**
     * Assassin: darkness closes in from far around you, eight shadow clones step out of it in two rings,
     * they dash through you one by one, then you vanish in smoke and ink and eyes glint in the dark.
     */
    private static void assassinGrand(Fx fx) {
        int t = fx.age();
        ParticleOptions black = fx.dust(0x18181E, 2.5F);
        ParticleOptions violet = fx.dust(0x9A3AD8, 1.2F);
        if (t == 0) {
            fx.sound(SoundEvents.PHANTOM_AMBIENT, 0.4F, 0.6F);
        }
        if (t < 150) {
            double r = t < 50 ? Mth.lerp(smooth(t / 50.0), 4.0, 2.8) : 2.8;
            if (t < 50 || fx.every(2)) {
                fx.ring(black, r, 0.3, 0.2, t < 50 ? 0.7 : 0.45);
                fx.ring(violet, r * 0.97, 0.9, 0.2, t < 50 ? 0.4 : 0.2);
            }
            if (t < 50 && fx.every(4)) {
                fx.cloud(ParticleTypes.SQUID_INK, 0, 0.5, 0, 3, r * 0.5, 0.2, r * 0.5, 0.01);
            }
        }
        for (int k = 0; k < 8; k++) {
            boolean inner = k % 2 == 0;
            double angle = inner ? Math.PI / 4 + (k / 2) * Math.PI / 2 : (k / 2) * Math.PI / 2;
            shadowClone(fx, angle, inner ? 1.8 : 2.4, inner ? 50 : 80, 100 + k * 6);
        }
        if (t == 50 || t == 80) {
            fx.sound(SoundEvents.ENDERMAN_AMBIENT, 0.3F, 0.6F);
        }
        if (t == 150) {
            fx.cloud(ParticleTypes.LARGE_SMOKE, 0, 1.0, 0, 24, 0.4, 0.7, 0.4, 0.02);
            fx.sphereOut(ParticleTypes.SQUID_INK, 0, 1.0, 0, 28, 0.14);
            fx.cloud(ParticleTypes.REVERSE_PORTAL, 0, 1.0, 0, 30, 0.3, 0.6, 0.3, 0.05);
            fx.sound(SoundEvents.ENDERMAN_TELEPORT, 0.9F, 0.6F);
            for (int k = 0; k < 8; k++) {
                crossSlash(fx, k * Math.PI / 4, k % 2 == 0 ? 1.0 : 1.5, 1.2, 0.5);
            }
        }
        if (t > 150 && t < 190 && fx.every(3)) {
            fx.at(ParticleTypes.SMOKE, fx.spread(1.0), 0.2 + fx.rand() * 1.6, fx.spread(1.0));
        }
        glintingEyes(fx, 154, 198);
    }

    /** Assassin respawn: you blink between three points around you, slashing a triangle, then back with an X. */
    private static void assassinLight(Fx fx) {
        int t = fx.age();
        ParticleOptions violet = fx.dust(0x9A3AD8, 1.1F);
        if (t == 0) {
            fx.cloud(ParticleTypes.LARGE_SMOKE, 0, 1.0, 0, 8, 0.2, 0.5, 0.2, 0.02);
            fx.sound(SoundEvents.ENDERMAN_TELEPORT, 0.6F, 1.2F);
        }
        double[][] points = new double[4][];
        points[0] = new double[] {0, 0};
        for (int k = 0; k < 3; k++) {
            double a = k * 2 * Math.PI / 3;
            points[k + 1] = new double[] {Math.sin(a) * 1.3, Math.cos(a) * 1.3};
        }
        for (int k = 1; k <= 3; k++) {
            int at = 3 + (k - 1) * 4;
            double[] p = points[k];
            if (t == at) {
                fx.cloud(ParticleTypes.REVERSE_PORTAL, p[0], 1.0, p[1], 12, 0.15, 0.5, 0.15, 0.05);
                fx.ringAt(violet, p[0], 0.1, p[1], 0.3, 0.07, 1);
                fx.sound(SoundEvents.ENDERMAN_TELEPORT, 0.3F, 1.6F + k * 0.1F);
                fx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.4F, 1.5F);
            }
            if (k > 1 && t >= at && t < 34 && (t == at || fx.every(2))) {
                double[] q = points[k - 1];
                double keep = fadeOut(t, 20, 34);
                fx.line(violet, q[0], 1.1, q[1], p[0], 1.1, p[1], 0.07, keep);
                fx.line(ParticleTypes.CRIT, q[0], 1.1, q[1], p[0], 1.1, p[1], 0.25, keep);
            }
        }
        if (t >= 11 && t < 34 && fx.every(2)) {
            fx.line(violet, points[3][0], 1.1, points[3][1], points[1][0], 1.1, points[1][1], 0.07,
                    fadeOut(t, 20, 34));
        }
        if (t == 15) {
            fx.cloud(ParticleTypes.LARGE_SMOKE, 0, 1.0, 0, 10, 0.3, 0.6, 0.3, 0.02);
            fx.sphereOut(ParticleTypes.SQUID_INK, 0, 1.0, 0, 12, 0.12);
            fx.sound(SoundEvents.ENDERMAN_TELEPORT, 0.7F, 0.7F);
            for (int k = 0; k < 4; k++) {
                crossSlash(fx, Math.PI / 4 + k * Math.PI / 2, 0.7, 1.2, 0.4);
            }
        }
        glintingEyes(fx, 20, 48);
    }

    /** A spinning coin standing on its edge at (x, y, z). */
    private static void coin(Fx fx, double x, double y, double z, double spin, double radius) {
        fx.ring3(fx.dust(0xF0C040, 0.9F), x, y, z, radius, Math.cos(spin), 0, Math.sin(spin), 0, 2 * Math.PI, 0.05,
                1);
    }

    /** Coin {@code i}: pops out of the ground, spins, then flies in an arc into the player's belt. */
    private static void thiefCoin(Fx fx, int i, int pop, int hop, int flyStart, int flyTime, double minDist,
                                  double maxDist) {
        int t = fx.age();
        if (t < pop || t > flyStart + flyTime) {
            return;
        }
        double a = hash(i, 11) * 2 * Math.PI;
        double d = minDist + hash(i, 12) * (maxDist - minDist);
        double x = Math.sin(a) * d;
        double z = Math.cos(a) * d;
        double spin = t * 0.6 + i;
        if (t == pop) {
            fx.sound(SoundEvents.ITEM_PICKUP, 0.4F, 0.8F + (i % 10) * 0.08F);
            fx.debrisAt(x, z, 2);
        }
        if (t < flyStart) {
            double y = 0.2 + 0.6 * Math.sin(Math.PI * Math.min(1, (t - pop) / (double) hop));
            coin(fx, x, y, z, spin, 0.12);
            return;
        }
        double w = (t - flyStart) / (double) flyTime;
        double px = Mth.lerp(w, x, 0);
        double pz = Mth.lerp(w, z, 0);
        double py = Mth.lerp(w, 0.2, 1.0) + 1.2 * Math.sin(Math.PI * w);
        coin(fx, px, py, pz, spin, 0.12);
        fx.at(ParticleTypes.WAX_ON, px, py, pz);
        if (t == flyStart + flyTime) {
            fx.sound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.4F, 1.0F + (i % 10) * 0.06F);
        }
    }

    private static void coinFountain(Fx fx, int nuggets, int emeralds) {
        fx.cloud(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.GOLD_NUGGET)), 0, 2.3, 0, nuggets,
                0.2, 0.1, 0.2, 0.3);
        if (emeralds > 0) {
            fx.cloud(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.EMERALD)), 0, 2.3, 0, emeralds,
                    0.2, 0.1, 0.2, 0.3);
            fx.cloud(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.DIAMOND)), 0, 2.3, 0,
                    emeralds / 2, 0.2, 0.1, 0.2, 0.3);
        }
        fx.cloud(fx.dust(0xF0C040, 1.5F), 0, 2.3, 0, 24, 0.5, 0.3, 0.5, 0);
        fx.sound(SoundEvents.BUNDLE_DROP_CONTENTS, 1.0F, 1.0F);
    }

    private static void coinRain(Fx fx, double keep, double radius) {
        if (fx.chance(keep)) {
            double a = fx.rand() * 2 * Math.PI;
            double r = fx.rand() * radius;
            fx.at(fx.dust(0xF0C040, 1.3F), Math.sin(a) * r, 0.3 + fx.rand() * 2.2, Math.cos(a) * r);
            if (fx.chance(0.4)) {
                fx.at(ParticleTypes.WAX_ON, fx.spread(radius), 0.3 + fx.rand() * 2, fx.spread(radius));
            }
        }
    }

    /**
     * Thief: twenty coins pop out of the ground around you and fly into your pocket, a padlock appears
     * above you, you pick it click by click, it springs open and a fountain of gold and gems pours out.
     */
    private static void thiefGrand(Fx fx) {
        int t = fx.age();
        for (int i = 0; i < 20; i++) {
            thiefCoin(fx, i, 2 + i * 4, 8, (int) (60 + i * 3.5), 10, 0.9, 2.6);
        }
        if (t >= 110 && t < 172) {
            double keep = Math.min(1, (t - 110) / 8.0) * fadeOut(t, 160, 172);
            double angle = t * 0.07;
            double tx = Math.cos(angle);
            double tz = -Math.sin(angle);
            double lift = t >= 150 ? 0.14 : 0;
            ParticleOptions gold = fx.dust(0xF0C040, 1.1F);
            ParticleOptions violet = fx.dust(0xB080E8, 1.0F);
            fx.panelLine(gold, 0, 0, tx, tz, -0.25, 2.3, 0.25, 2.3, 0.06, keep);
            fx.panelLine(gold, 0, 0, tx, tz, -0.25, 2.7, 0.25, 2.7, 0.06, keep);
            fx.panelLine(gold, 0, 0, tx, tz, -0.25, 2.3, -0.25, 2.7, 0.06, keep);
            fx.panelLine(gold, 0, 0, tx, tz, 0.25, 2.3, 0.25, 2.7, 0.06, keep);
            fx.panelEllipse(violet, 0, 0, tx, tz, 0, 2.7 + lift, 0.16, 0.2, 0, Math.PI, 0.05, keep);
            fx.panelEllipse(violet, 0, 0, tx, tz, 0, 2.55, 0.05, 0.05, 0, 2 * Math.PI, 0.03, keep);
            fx.panelLine(violet, 0, 0, tx, tz, 0, 2.5, 0, 2.38, 0.04, keep);
        }
        if (t >= 128 && t < 150 && t % 3 == 0) {
            fx.cloud(ParticleTypes.WAX_ON, 0, 2.5, 0, 3, 0.05, 0.05, 0.05, 0.05);
            fx.sound(SoundEvents.TRIPWIRE_CLICK_OFF, 0.5F, 1.4F + (t - 128) / 30F);
        }
        if (t == 146) {
            fx.sound(SoundEvents.CHEST_LOCKED, 0.6F, 1.2F);
        }
        if (t == 150) {
            fx.sound(SoundEvents.CHEST_OPEN, 0.7F, 1.4F);
            coinFountain(fx, 30, 8);
            fx.flash(2.5);
        }
        if (t > 150) {
            coinRain(fx, fadeOut(t, 165, 200), 2.0);
            coinRain(fx, fadeOut(t, 165, 200), 2.0);
        }
    }

    /** Thief respawn: a coin flips high over your head, you catch it, and five coins circle you. */
    private static void thiefLight(Fx fx) {
        int t = fx.age();
        if (t < 18) {
            double w = t / 18.0;
            double y = 1.3 + 1.7 * Math.sin(Math.PI * w);
            coin(fx, 0, y, 0, t * 0.9, 0.18);
            fx.at(ParticleTypes.WAX_ON, fx.spread(0.1), y - 0.2, fx.spread(0.1));
        }
        if (t == 0) {
            fx.sound(SoundEvents.ARMOR_EQUIP_GOLD, 0.7F, 1.6F);
        }
        if (t == 18) {
            fx.cloud(ParticleTypes.WAX_ON, 0, 1.3, 0, 12, 0.2, 0.2, 0.2, 0.05);
            fx.cloud(fx.dust(0xF0C040, 1.5F), 0, 1.3, 0, 10, 0.2, 0.2, 0.2, 0);
            fx.sound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8F, 1.2F);
        }
        if (t > 18 && t < 46) {
            double keep = fadeOut(t, 34, 46);
            for (int k = 0; k < 5; k++) {
                double a = t * 0.2 + k * 2 * Math.PI / 5;
                if (fx.chance(keep)) {
                    coin(fx, Math.sin(a) * 0.8, 1.0 + 0.1 * Math.sin(t * 0.3 + k), Math.cos(a) * 0.8, t * 0.6 + k,
                            0.1);
                }
            }
        }
    }

    /** Revolver cylinder hovering above the head: six chambers, {@code loaded} of them filled. */
    private static void revolver(Fx fx, double y, double rot, int loaded, int fired, double keep) {
        ParticleOptions leather = fx.dust(0x8B5A30, 1.0F);
        ParticleOptions steel = fx.dust(0xB8B8C0, 0.9F);
        fx.ring(leather, 0.55, y, 0.08, keep);
        for (int c = 0; c < 6; c++) {
            double a = rot + c * Math.PI / 3;
            double cx = Math.sin(a) * 0.36;
            double cz = Math.cos(a) * 0.36;
            fx.ringAt(steel, cx, y, cz, 0.1, 0.05, keep);
            if (c < loaded && c >= fired) {
                fx.at(fx.dust(0xF0C040, 1.5F), cx, y, cz);
            }
        }
    }

    private static void gunshot(Fx fx, double angle, double muzzle, double end) {
        double dx = Math.sin(angle);
        double dz = Math.cos(angle);
        fx.line(ParticleTypes.CRIT, dx * muzzle, 1.25, dz * muzzle, dx * end, 1.3, dz * end, 0.15, 1);
        fx.line(fx.dust(0xE8D8A8, 0.9F), dx * muzzle, 1.25, dz * muzzle, dx * end, 1.3, dz * end, 0.1, 1);
        fx.cloud(ParticleTypes.FLAME, dx * muzzle, 1.25, dz * muzzle, 5, 0.05, 0.05, 0.05, 0.04);
        fx.cloud(ParticleTypes.SMOKE, dx * muzzle, 1.25, dz * muzzle, 3, 0.05, 0.05, 0.05, 0.02);
        fx.cloud(ParticleTypes.CRIT, dx * end, 1.3, dz * end, 6, 0.1, 0.1, 0.1, 0.2);
        fx.sound(SoundEvents.FIREWORK_ROCKET_BLAST, 0.5F, 0.8F);
        fx.sound(SoundEvents.CROSSBOW_SHOOT, 0.4F, 0.6F);
    }

    /**
     * Highwayman: a horse gallops two and a half laps around you, kicking up dust, a revolver loads chamber
     * by chamber above your head, spins, and six shots fan out all around you, then gunsmoke rings rise.
     */
    private static void highwaymanGrand(Fx fx) {
        int t = fx.age();
        int step = t / 2;
        if (t == 0) {
            fx.sound(SoundEvents.HORSE_AMBIENT, 0.6F, 1.0F);
        }
        if (t < 100 && fx.every(2)) {
            ParticleOptions hoof = fx.dust(0x3A2A1A, 1.1F);
            double keep = fadeOut(t, 80, 100);
            for (int s = Math.max(0, step - 14); s <= Math.min(step, 40); s++) {
                double a = s * 0.4;
                for (int side = -1; side <= 1; side += 2) {
                    if (fx.chance(keep)) {
                        double r = 2.2 + side * 0.08;
                        fx.at(hoof, Math.sin(a - side * 0.03) * r, FLOOR, Math.cos(a - side * 0.03) * r);
                    }
                }
            }
            if (step <= 40) {
                double a = step * 0.4;
                fx.debrisAt(Math.sin(a) * 2.2, Math.cos(a) * 2.2, 3);
                fx.cloud(ParticleTypes.POOF, Math.sin(a) * 2.2, 0.2, Math.cos(a) * 2.2, 1, 0.1, 0.05, 0.1, 0.01);
            }
        }
        if (t < 80 && t % 5 == 0) {
            fx.sound(SoundEvents.HORSE_GALLOP, 0.5F, 1.0F);
        }
        if (t >= 70 && t < 178) {
            int loaded = t < 80 ? 0 : Math.min(6, (t - 80) / 9 + 1);
            int fired = t < 150 ? 0 : Math.min(6, (t - 150) / 3 + 1);
            double rot = t < 125 ? Math.PI / 3 * Mth.clamp((t - 80) / 9.0, 0, 5)
                    : 5 * Math.PI / 3 + Math.pow(Math.min(t, 150) - 125, 2) * 0.012 + Math.max(0, t - 150) * 0.3;
            double keep = Math.min(1, (t - 70) / 6.0) * fadeOut(t, 170, 178);
            revolver(fx, 2.5, rot, loaded, fired, keep);
            if (t >= 80 && t < 130 && (t - 80) % 9 == 0) {
                fx.sound(SoundEvents.CROSSBOW_LOADING_MIDDLE, 0.5F, 1.4F);
            }
            if (t >= 125 && t < 150 && fx.every(2)) {
                fx.sound(SoundEvents.LEVER_CLICK, 0.3F, 1.5F + (t - 125) / 50F);
            }
        }
        for (int i = 0; i < 6; i++) {
            if (t == 150 + i * 3) {
                gunshot(fx, i * Math.PI / 3, 0.45, 2.2);
            }
        }
        for (int k = 0; k < 2; k++) {
            double w = fx.span(170 + k * 8, 200);
            if (w >= 0) {
                fx.ring(fx.dust(0x9A9AA0, 1.4F), 0.3 + w * 0.7, 2.0 + w * 1.2, 0.12, 1 - w);
            }
        }
    }

    /** Highwayman respawn: a whinny, then two pistol shots straight up into the air and rising smoke rings. */
    private static void highwaymanLight(Fx fx) {
        int t = fx.age();
        if (t == 0) {
            fx.sound(SoundEvents.HORSE_AMBIENT, 0.4F, 1.1F);
        }
        for (int k = 0; k < 2; k++) {
            int at = 4 + k * 6;
            double side = k == 0 ? 0.35 : -0.35;
            double mx = fx.lx(0, side);
            double mz = fx.lz(0, side);
            if (t == at) {
                fx.line(ParticleTypes.CRIT, mx, 1.7, mz, mx, 4.2, mz, 0.15, 1);
                fx.line(fx.dust(0xE8D8A8, 0.9F), mx, 1.7, mz, mx, 4.2, mz, 0.1, 1);
                fx.cloud(ParticleTypes.FLAME, mx, 1.7, mz, 6, 0.05, 0.05, 0.05, 0.05);
                fx.cloud(ParticleTypes.CRIT, mx, 4.2, mz, 6, 0.1, 0.1, 0.1, 0.2);
                fx.sound(SoundEvents.FIREWORK_ROCKET_BLAST, 0.7F, 0.8F + k * 0.1F);
                fx.sound(SoundEvents.CROSSBOW_SHOOT, 0.5F, 0.6F);
            }
            double w = fx.span(at, at + 30);
            if (w >= 0) {
                fx.ringAt(fx.dust(0x9A9AA0, 1.3F), mx, 1.8 + w * 1.6, mz, 0.12 + w * 0.35, 0.08, 1 - w);
            }
        }
        if (t >= 10 && t < 30 && fx.every(3)) {
            fx.debrisAt(fx.spread(1.2), fx.spread(1.2), 2);
        }
    }

    private static void infiltratorScan(Fx fx, double y) {
        fx.ring(fx.dust(0x40D8B0, 1.2F), 0.55, y, 0.08, 1);
        fx.ring(fx.dust(0x40D8B0, 0.7F), 0.62, y - 0.04, 0.12, 0.6);
    }

    /** Body turned into jittering pixels; {@code amount} 0..1 is how far they are displaced. */
    private static void infiltratorGlitch(Fx fx, int count, double amount) {
        int[] colors = {0x40D8B0, 0x202830, 0x9AA8C0};
        for (int i = 0; i < count; i++) {
            double a = fx.rand() * 2 * Math.PI;
            double r = Math.sqrt(fx.rand()) * 0.3;
            fx.at(fx.dust(colors[i % 3], 1.4F), Math.sin(a) * r + fx.spread(0.35 * amount), 0.1 + fx.rand() * 1.8,
                    Math.cos(a) * r + fx.spread(0.35 * amount));
        }
        if (fx.every(5)) {
            double y = 0.2 + fx.rand() * 1.6;
            fx.ringAt(fx.dust(0x40D8B0, 1.0F), fx.spread(0.3), y, fx.spread(0.3), 0.35, 0.08, 1);
        }
    }

    /**
     * Infiltrator: a wide laser grid appears under you and a scanner sweeps over you three times,
     * you glitch apart into pixels that scatter, then snap back together and a watching eye opens.
     */
    private static void infiltratorGrand(Fx fx) {
        int t = fx.age();
        if (t < 135 && fx.every(3)) {
            ParticleOptions grid = fx.dust(0x40D8B0, 0.7F);
            double keep = Math.min(1, t / 15.0) * 0.6 * fadeOut(t, 120, 135);
            for (int k = -3; k <= 3; k++) {
                fx.line(grid, k * 0.8, FLOOR, -2.4, k * 0.8, FLOOR, 2.4, 0.12, keep);
                fx.line(grid, -2.4, FLOOR, k * 0.8, 2.4, FLOOR, k * 0.8, 0.12, keep);
            }
        }
        if (t < 60) {
            int phase = t % 20;
            double y = phase < 10 ? Mth.lerp(phase / 10.0, FLOOR, 2.0) : Mth.lerp((phase - 10) / 10.0, 2.0, FLOOR);
            infiltratorScan(fx, y);
            if (fx.every(3)) {
                fx.sound(SoundEvents.NOTE_BLOCK_BIT, 0.3F, 1.2F + (float) y * 0.3F);
            }
        }
        double g = fx.span(60, 120);
        if (g >= 0) {
            infiltratorGlitch(fx, (int) (10 + 35 * g), g);
            if (fx.every(4)) {
                fx.sound(SoundEvents.SCULK_CLICKING, 0.4F, 1.3F + (float) g * 0.5F);
            }
        }
        double s = fx.span(120, 140);
        if (s >= 0) {
            infiltratorGlitch(fx, (int) (45 * (1 - s)), 1);
            for (int i = 0; i < 3; i++) {
                double a = fx.rand() * 2 * Math.PI;
                fx.fly(ParticleTypes.ELECTRIC_SPARK, 0, 0.3 + fx.rand() * 1.5, 0, Math.sin(a), 0, Math.cos(a), 0.2);
            }
            if (t == 120) {
                fx.sound(SoundEvents.ILLUSIONER_MIRROR_MOVE, 0.8F, 1.0F);
            }
        }
        if (t >= 140 && t < 150 && fx.every(2)) {
            for (double y = 0.2; y < 1.9; y += 0.35) {
                fx.ring(fx.dust(0x40D8B0, 0.8F), 0.42, y, 0.14, 0.15);
            }
        }
        if (t == 150) {
            for (double y = 0.4; y < 2.0; y += 0.5) {
                fx.radialIn(ParticleTypes.ELECTRIC_SPARK, 1.5, y, 12, 0.35);
            }
            fx.cloud(fx.dust(0x40D8B0, 1.2F), 0, 1.0, 0, 40, 0.25, 0.6, 0.25, 0);
            fx.flash(1.0);
            fx.sound(SoundEvents.ILLUSIONER_CAST_SPELL, 0.8F, 1.3F);
            fx.sound(SoundEvents.ENDERMAN_TELEPORT, 0.4F, 1.6F);
        }
        if (t > 150 && fx.every(2)) {
            double open = Math.sin(Math.min(1, (t - 150) / 10.0) * Math.PI / 2) * fadeOut(t, 185, 200);
            fx.eye(fx.dust(0x40D8B0, 1.1F), fx.dust(0x9AA8C0, 2.2F), 2.5, 0.5, open, t * 0.06);
            for (double y = 0.2; y < 1.9; y += 0.35) {
                fx.ring(fx.dust(0x40D8B0, 0.8F), 0.42, y, 0.14, 0.2 * fadeOut(t, 170, 200));
            }
        }
    }

    /** Infiltrator respawn: a hologram of you builds up ring by ring from head to feet, flickers, and turns solid. */
    private static void infiltratorLight(Fx fx) {
        int t = fx.age();
        ParticleOptions holo = fx.dust(0x40D8B0, 1.0F);
        if (t < 30) {
            int rings = Math.min(14, t + 1);
            double keep = t < 16 ? (fx.chance(0.2) ? 0.3 : 1) : fadeOut(t, 16, 30);
            for (int i = 0; i < rings; i++) {
                double y = 2.0 - i * 0.14;
                double r = y > 1.4 ? 0.2 : 0.33;
                fx.ring(holo, r, y, 0.1, keep * 0.8);
            }
            if (t < 14) {
                fx.ring(fx.dust(0xFFFFFF, 1.0F), 0.45, 2.0 - t * 0.14, 0.08, 1);
                if (t % 2 == 0) {
                    fx.sound(SoundEvents.NOTE_BLOCK_BIT, 0.3F, 2.0F - t * 0.06F);
                }
            }
        }
        if (t < 16 && fx.every(3)) {
            fx.ring(fx.dust(0x40D8B0, 0.7F), 0.9, FLOOR, 0.12, 0.6);
            fx.ring(fx.dust(0x40D8B0, 0.7F), 1.3, FLOOR, 0.12, 0.4);
        }
        if (t == 16) {
            fx.radialIn(ParticleTypes.ELECTRIC_SPARK, 1.2, 1.0, 14, 0.3);
            fx.cloud(holo, 0, 1.0, 0, 24, 0.25, 0.6, 0.25, 0);
            fx.sound(SoundEvents.ILLUSIONER_CAST_SPELL, 0.7F, 1.4F);
        }
    }

    // =========================================================================
    // Mages
    // =========================================================================

    /**
     * Wizard: a huge thin magic circle is traced line by line (outer circle, second circle, pentagram,
     * inner ring, runes), runes pour into it while a second circle turns around your waist, then the
     * whole circle lifts up to your head as a crown and bursts into stars.
     */
    private static void wizardGrand(Fx fx) {
        int t = fx.age();
        double radius = 3.0;
        int drawEnd = 90;
        int riseStart = 120;
        int peak = 150;
        ParticleOptions azure = fx.dust(0x4A8CFF, 0.8F);
        ParticleOptions gold = fx.dust(0xF0C850, 0.8F);
        Glyph g = WIZARD_SIGIL;
        if (t < riseStart) {
            double d = Math.min(1, t / (double) drawEnd);
            double prev = Math.min(1, Math.max(0, t - 1) / (double) drawEnd);
            if (t <= drawEnd && t > 0) {
                g.draw(fx, ParticleTypes.END_ROD, ParticleTypes.END_ROD, radius, FLOOR, 0, prev, d, 1, 0.12);
                double[] tip = g.point(d);
                fx.glyphPoint(fx.dust(0xFFFFFF, 1.8F), tip[0], tip[1], radius, FLOOR + 0.05, 0);
                fx.glyphPoint(ParticleTypes.ENCHANT, tip[0], tip[1], radius, FLOOR + 0.3, 0);
                if (fx.every(3)) {
                    fx.sound(SoundEvents.ENCHANTMENT_TABLE_USE, 0.3F, 0.9F + (float) d * 0.6F);
                }
                if (g.strokeAt(d) != g.strokeAt(prev)) {
                    fx.sound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.7F, 0.9F + g.strokeAt(d) * 0.15F);
                }
            }
            if (fx.every(2)) {
                g.draw(fx, azure, gold, radius, FLOOR + 0.01, 0, 0, d, 0.6, 0.25);
            }
            if (t > drawEnd * 0.4) {
                for (int i = 0; i < 4; i++) {
                    double a = fx.rand() * 2 * Math.PI;
                    fx.fly(ParticleTypes.ENCHANT, Math.sin(a) * radius, FLOOR + 0.1, Math.cos(a) * radius,
                            fx.spread(1.2), 1.0 + fx.rand(), fx.spread(1.2), 1.0);
                }
            }
            if (t == drawEnd) {
                fx.sound(SoundEvents.ILLUSIONER_PREPARE_BLINDNESS, 0.8F, 1.0F);
            }
            if (t > drawEnd) {
                double keep = Math.min(1, (t - drawEnd) / 8.0);
                g.draw(fx, fx.dust(0xA070FF, 0.9F), gold, 1.2, 1.0, t * 0.08, 0, 1, keep, 0.15);
                for (int i = 0; i < 4; i++) {
                    double[] p = g.point(fx.rand());
                    double right = p[0] * radius;
                    double fwd = p[1] * radius;
                    fx.fly(ParticleTypes.END_ROD, fx.lx(fwd, right), FLOOR, fx.lz(fwd, right), 0, 1, 0, 0.04);
                }
                if ((t - drawEnd) % 6 == 0) {
                    fx.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.4F, 0.8F + (t - drawEnd) / 40F);
                }
            }
        } else if (t < peak) {
            double q = (t - riseStart) / (double) (peak - riseStart);
            double e = smooth(q);
            double y = Mth.lerp(e, FLOOR, 2.2);
            double rr = Mth.lerp(e, radius, 0.6);
            double rot = q * q * Math.PI * 1.4;
            g.draw(fx, fx.dust(0x4A8CFF, 1.1F), fx.dust(0xF0C850, 1.1F), rr, y, rot, 0, 1, 1, 0.25);
            g.draw(fx, fx.dust(0xA070FF, 0.9F), gold, Mth.lerp(e, 1.2, 0.4), Mth.lerp(e, 1.0, 2.2), -rot, 0, 1,
                    1 - e, 0.15);
            if (fx.every(3)) {
                fx.sound(SoundEvents.ILLUSIONER_CAST_SPELL, 0.5F, 0.8F + (float) q * 0.6F);
            }
        }
        if (t == peak) {
            fx.flash(2.2);
            fx.sphereOut(ParticleTypes.FIREWORK, 0, 2.2, 0, 50, 0.16);
            fx.sphereOut(ParticleTypes.END_ROD, 0, 2.2, 0, 20, 0.1);
            fx.sound(SoundEvents.EVOKER_CAST_SPELL, 1.0F, 1.2F);
            fx.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 1.2F);
        }
        if (t > peak) {
            double w = (t - peak) / 50.0;
            if (fx.every(2)) {
                g.draw(fx, fx.dust(0x4A8CFF, 1.1F), fx.dust(0xF0C850, 1.1F), 0.6, 2.2, Math.PI * 1.4 + w * 3, 0, 1,
                        1 - w, 0.12);
            }
            if (w < 0.3) {
                fx.ring(azure, Mth.lerp(w / 0.3, 0.6, 2.2), 2.2, 0.12, 1 - w * 2);
            }
            for (int k = 0; k < 5; k++) {
                double a = w * 9 + k * 2 * Math.PI / 5;
                if (fx.chance(1 - w)) {
                    fx.at(fx.dust(k % 2 == 0 ? 0xF0C850 : 0xA070FF, 1.6F), Math.sin(a) * 0.9, 2.2, Math.cos(a) * 0.9);
                }
            }
        }
    }

    /** Wizard respawn: three rune rings orbit you, spiralling up to your head, and burst into stars. */
    private static void wizardLight(Fx fx) {
        int t = fx.age();
        int[] colors = {0x4A8CFF, 0xF0C850, 0xA070FF};
        if (t == 0) {
            fx.sound(SoundEvents.ENCHANTMENT_TABLE_USE, 0.7F, 1.0F);
        }
        if (t < 18) {
            double w = smooth(t / 18.0);
            double orbit = Mth.lerp(w, 1.0, 0.3);
            double y = Mth.lerp(w, 0.4, 2.2);
            for (int k = 0; k < 3; k++) {
                double a = t * 0.35 + k * 2 * Math.PI / 3;
                double cx = Math.sin(a) * orbit;
                double cz = Math.cos(a) * orbit;
                double cy = y + 0.2 * Math.sin(t * 0.4 + k);
                fx.ring3(fx.dust(colors[k], 1.0F), cx, cy, cz, 0.28, Math.cos(a + t * 0.2), 0.3, Math.sin(a + t * 0.2),
                        0, 2 * Math.PI, 0.06, 1);
                fx.at(ParticleTypes.ENCHANT, cx, cy, cz);
                fx.at(fx.dust(0xFFFFFF, 1.4F), cx, cy, cz);
            }
            if (t % 4 == 0) {
                fx.sound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.5F, 1.0F + t * 0.04F);
            }
        }
        if (t == 18) {
            fx.sphereOut(ParticleTypes.FIREWORK, 0, 2.2, 0, 24, 0.15);
            fx.sphereOut(ParticleTypes.END_ROD, 0, 2.2, 0, 10, 0.1);
            fx.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 1.3F);
        }
        if (t > 18) {
            double w = (t - 18) / 28.0;
            for (int k = 0; k < 5; k++) {
                double a = w * 7 + k * 2 * Math.PI / 5;
                if (fx.chance(1 - w)) {
                    fx.at(fx.dust(colors[k % 3], 1.5F), Math.sin(a) * 0.7, 2.2, Math.cos(a) * 0.7);
                }
            }
        }
    }

    /** Storm cloud hovering above the player. */
    private static void stormCloud(Fx fx, double y, double radius, int count) {
        for (int i = 0; i < count; i++) {
            double a = fx.rand() * 2 * Math.PI;
            double d = Math.sqrt(fx.rand()) * radius;
            fx.at(fx.dust(fx.chance(0.5) ? 0x505868 : 0x383C48, 3.0F), Math.sin(a) * d, y + fx.spread(0.3),
                    Math.cos(a) * d);
        }
        if (fx.every(4) && count > 4) {
            fx.cloud(fx.dust(0x5CE8FF, 1.2F), 0, y, 0, 3, radius * 0.5, 0.1, radius * 0.5, 0);
            fx.cloud(ParticleTypes.ELECTRIC_SPARK, 0, y, 0, 3, radius * 0.5, 0.1, radius * 0.5, 0.02);
        }
    }

    /** A lightning strike from the cloud onto a fixed spot, flickering for three ticks, leaving a scorch mark. */
    private static void sorcererStrike(Fx fx, int i, int at, double cloudY, double minDist, double maxDist,
                                       int scorchEnd) {
        int t = fx.age();
        double a = hash(i, 21) * 2 * Math.PI;
        double d = minDist + hash(i, 22) * (maxDist - minDist);
        double x = Math.sin(a) * d;
        double z = Math.cos(a) * d;
        if (t >= at && t < at + 3) {
            fx.bolt(0x5CE8FF, x * 0.4, cloudY, z * 0.4, x, FLOOR, z);
        }
        if (t == at) {
            fx.sound(SoundEvents.LIGHTNING_BOLT_IMPACT, 0.5F, 1.2F + (float) hash(i, 23) * 0.4F);
            fx.cloud(ParticleTypes.ELECTRIC_SPARK, x, 0.2, z, 8, 0.1, 0.1, 0.1, 0.2);
            fx.debrisAt(x, z, 3);
        }
        if (t >= at && t < scorchEnd && fx.every(2)) {
            double keep = fadeOut(t, scorchEnd - 12, scorchEnd);
            fx.ringAt(fx.dust(0x202020, 1.2F), x, FLOOR, z, 0.22, 0.07, keep);
            fx.at(fx.dust(0x202020, 1.6F), x, FLOOR, z);
        }
    }

    private static void bodyArcs(Fx fx) {
        double a1 = fx.rand() * 2 * Math.PI;
        double a2 = a1 + 1 + fx.rand() * 2;
        fx.zigzag(ParticleTypes.ELECTRIC_SPARK, Math.sin(a1) * 0.45, 0.3 + fx.rand() * 1.4, Math.cos(a1) * 0.45,
                Math.sin(a2) * 0.45, 0.3 + fx.rand() * 1.4, Math.cos(a2) * 0.45, 3, 0.1, 0.1);
    }

    /**
     * Sorcerer: a wide storm cloud gathers above you, twelve bolts strike the ground around you, static
     * crawls over your body, then six bolts hit a ring around you at once and the last one hits you.
     */
    private static void sorcererGrand(Fx fx) {
        int t = fx.age();
        if (t < 185) {
            double r = t < 80 ? Mth.lerp(smooth(t / 80.0), 0.3, 2.2) : 2.2;
            stormCloud(fx, 5.5, r, (int) (18 * fadeOut(t, 160, 185)));
        }
        if (t == 10 || t == 40 || t == 70) {
            fx.sound(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.25F, 0.5F + t / 200F);
        }
        for (int i = 0; i < 12; i++) {
            sorcererStrike(fx, i, 80 + i * 5, 5.2, 1.2, 2.6, 185);
        }
        if (t >= 135 && t < 150) {
            bodyArcs(fx);
            fx.fly(ParticleTypes.ELECTRIC_SPARK, fx.spread(0.3), fx.rand() * 1.8, fx.spread(0.3), 0, 1, 0, 0.1);
            if (t == 135) {
                fx.sound(SoundEvents.BEACON_POWER_SELECT, 0.7F, 1.6F);
            }
        }
        if (t >= 150 && t < 153) {
            fx.bolt(0x5CE8FF, 0, 5.2, 0, 0, 2.0, 0);
            fx.bolt(0xB48CFF, 0, 2.0, 0, 0, 0.1, 0);
            for (int k = 0; k < 6; k++) {
                double a = k * Math.PI / 3;
                fx.bolt(0xB48CFF, Math.sin(a) * 0.8, 5.2, Math.cos(a) * 0.8, Math.sin(a) * 2.0, FLOOR,
                        Math.cos(a) * 2.0);
            }
        }
        if (t == 150) {
            fx.flash(2.0);
            fx.sphereOut(ParticleTypes.ELECTRIC_SPARK, 0, 2.0, 0, 40, 0.2);
            fx.sound(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.5F, 1.1F);
            fx.groundDebris(12, 2.0);
        }
        if (t > 150 && fx.every(2) && fx.chance(fadeOut(t, 170, 200))) {
            bodyArcs(fx);
        }
    }

    /** Sorcerer respawn: ball lightning grows in your hands, rises over your head and fires bolts all around. */
    private static void sorcererLight(Fx fx) {
        int t = fx.age();
        double y = t < 14 ? 1.2 : Mth.lerp(Math.min(1, (t - 14) / 4.0), 1.2, 2.6);
        if (t < 18) {
            double r = t < 14 ? Mth.lerp(t / 14.0, 0.1, 0.45) : 0.45;
            fx.sphere(fx.dust(0x5CE8FF, 1.1F), 0, y, 0, r, 22, 0.8);
            for (int i = 0; i < 3; i++) {
                double a = fx.rand() * 2 * Math.PI;
                fx.at(ParticleTypes.ELECTRIC_SPARK, Math.sin(a) * r, y + fx.spread(r), Math.cos(a) * r);
            }
            if (t % 4 == 0) {
                fx.sound(SoundEvents.LIGHTNING_BOLT_IMPACT, 0.15F, 2.0F);
            }
        }
        if (t >= 18 && t < 21) {
            for (int k = 0; k < 6; k++) {
                double a = k * Math.PI / 3 + 0.3;
                fx.bolt(0x5CE8FF, 0, 2.6, 0, Math.sin(a) * 1.5, FLOOR, Math.cos(a) * 1.5);
            }
        }
        if (t == 18) {
            fx.sphereOut(ParticleTypes.ELECTRIC_SPARK, 0, 2.6, 0, 24, 0.2);
            fx.sound(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.35F, 1.4F);
            for (int k = 0; k < 6; k++) {
                double a = k * Math.PI / 3 + 0.3;
                fx.debrisAt(Math.sin(a) * 1.5, Math.cos(a) * 1.5, 2);
            }
        }
        if (t > 18 && fx.every(2) && fx.chance(fadeOut(t, 30, 45))) {
            bodyArcs(fx);
        }
    }

    /** Warlock rite: five soul candles, a pact seal between them, chains to the chest that shatter. */
    private static void warlockRite(Fx fx, double radius, int candleStep, int sealFrom, int sealTo, int chainFrom,
                                    int peak, int eyeFrom, int end) {
        int t = fx.age();
        ParticleOptions wax = fx.dust(0xE8DCC0, 1.1F);
        ParticleOptions green = fx.dust(0x50FF90, 1.0F);
        ParticleOptions violet = fx.dust(0x9A40D8, 0.9F);
        double[][] tops = new double[5][];
        for (int c = 0; c < 5; c++) {
            double a = c * 2 * Math.PI / 5;
            double x = Math.sin(a) * radius;
            double z = Math.cos(a) * radius;
            tops[c] = new double[] {x, 0.45, z};
            int born = c * candleStep;
            if (t < born || t >= end - 2) {
                continue;
            }
            double h = Math.min(1, (t - born) / 5.0) * 0.45;
            tops[c][1] = h;
            if (fx.every(2)) {
                fx.line(wax, x, FLOOR, z, x, h, z, 0.08, 1);
            }
            if (t == born + 5) {
                fx.sound(SoundEvents.SOUL_ESCAPE, 0.5F, 1.0F + c * 0.1F);
            }
            if (t >= born + 5) {
                if (t < end - 15) {
                    fx.at(ParticleTypes.SOUL_FIRE_FLAME, x, h + 0.08, z);
                    fx.at(green, x, h + 0.05, z);
                } else if (fx.every(3)) {
                    fx.fly(ParticleTypes.SMOKE, x, h + 0.05, z, 0, 1, 0, 0.03);
                }
            }
        }
        if (t >= sealFrom && t < end - 5 && fx.every(2)) {
            double w = Math.min(1, (t - sealFrom) / (double) (sealTo - sealFrom));
            double keep = fadeOut(t, end - 20, end - 5);
            for (int c = 0; c < 5; c++) {
                double[] a = tops[c];
                double[] b = tops[(c + 1) % 5];
                double part = Mth.clamp(w * 5 - c, 0, 1);
                if (part > 0) {
                    fx.line(violet, a[0], FLOOR, a[2], Mth.lerp(part, a[0], b[0]), FLOOR, Mth.lerp(part, a[2], b[2]),
                            0.1, keep);
                }
                fx.line(green, a[0], FLOOR, a[2], a[0] * (1 - w), FLOOR, a[2] * (1 - w), 0.12, keep * 0.6);
                if (w >= 1 && t < peak) {
                    double f = (t * 0.07 + c * 0.2) % 1.0;
                    fx.at(fx.dust(0x50FF90, 1.5F), a[0] * (1 - f), FLOOR + 0.02, a[2] * (1 - f));
                }
            }
        }
        if (t >= chainFrom && t < peak) {
            double sag = 0.4 * (1 - (t - chainFrom) / (double) (peak - chainFrom));
            ParticleOptions linkA = fx.dust(0x3A1850, 1.2F);
            ParticleOptions linkB = fx.dust(0x9A40D8, 1.2F);
            for (double[] top : tops) {
                for (int i = 0; i <= 14; i++) {
                    double f = i / 14.0;
                    fx.at(i % 2 == 0 ? linkA : linkB, Mth.lerp(f, top[0], 0), Mth.lerp(f, top[1], 1.1)
                            - Math.sin(f * Math.PI) * sag, Mth.lerp(f, top[2], 0));
                }
            }
            if ((t - chainFrom) % 6 == 0) {
                fx.sound(SoundEvents.CHAIN_STEP, 0.6F, 0.8F);
            }
        }
        if (t == peak) {
            for (double[] top : tops) {
                double mx = top[0] / 2;
                double mz = top[2] / 2;
                fx.cloud(ParticleTypes.CRIT, mx, 0.8, mz, 6, 0.1, 0.1, 0.1, 0.2);
                fx.burstItem(Items.CHAIN, mx, 0.8, mz, 3, 0.15);
            }
            fx.sphereOut(ParticleTypes.REVERSE_PORTAL, 0, 1.2, 0, 40, 0.15);
            fx.cloud(ParticleTypes.DRAGON_BREATH, 0, 1.2, 0, 16, 0.4, 0.4, 0.4, 0.02);
            fx.flash(1.2);
            fx.sound(SoundEvents.CHAIN_BREAK, 1.0F, 0.8F);
            fx.sound(SoundEvents.ENDER_DRAGON_GROWL, 0.5F, 1.5F);
        }
        if (t >= peak && t < peak + 12) {
            for (double[] top : tops) {
                fx.fly(ParticleTypes.SOUL_FIRE_FLAME, top[0], top[1], top[2], 0, 1, 0, 0.2);
            }
        }
        if (t >= eyeFrom && t < end && fx.every(2)) {
            int mid = (eyeFrom + end) / 2;
            double open = t < mid ? smooth((t - eyeFrom) / (double) (mid - eyeFrom)) : fadeOut(t, end - 15, end);
            fx.eye(fx.dust(0x9A40D8, 1.1F), fx.dust(0x50FF90, 2.4F), 2.7, 0.65, open, t * 0.05);
        }
    }

    /**
     * Warlock: five soul candles light up one by one far around you, a pact seal links them, chains rise
     * from the candles and pull tight on your chest while a great eye opens above you, then they shatter.
     */
    private static void warlockGrand(Fx fx) {
        if (fx.age() == 0) {
            fx.sound(SoundEvents.EVOKER_PREPARE_SUMMON, 0.6F, 0.8F);
        }
        warlockRite(fx, 2.2, 8, 44, 90, 80, 150, 110, 200);
    }

    /** Warlock respawn: soul fire spirals up around you and a seven-pointed pact seal flares above your head. */
    private static void warlockLight(Fx fx) {
        int t = fx.age();
        if (t == 0) {
            fx.sound(SoundEvents.SOUL_ESCAPE, 0.8F, 0.8F);
        }
        if (t < 16) {
            double h = Mth.lerp(t / 16.0, 0.3, 2.4);
            for (int k = 0; k < 2; k++) {
                for (double y = 0; y <= h; y += 0.15) {
                    double a = y * 3.5 + k * Math.PI + t * 0.3;
                    fx.at(fx.dust(k == 0 ? 0x50FF90 : 0x9A40D8, 1.1F), Math.sin(a) * 0.7, y, Math.cos(a) * 0.7);
                }
                double a = h * 3.5 + k * Math.PI + t * 0.3;
                fx.at(ParticleTypes.SOUL_FIRE_FLAME, Math.sin(a) * 0.7, h, Math.cos(a) * 0.7);
            }
        }
        if (t == 16) {
            fx.sphereOut(ParticleTypes.REVERSE_PORTAL, 0, 2.4, 0, 20, 0.12);
            fx.sound(SoundEvents.EVOKER_CAST_SPELL, 0.8F, 0.7F);
        }
        if (t >= 16 && t < 42) {
            double keep = fadeOut(t, 28, 42);
            double rot = t * 0.08;
            ParticleOptions green = fx.dust(0x50FF90, 1.0F);
            fx.ring(fx.dust(0x9A40D8, 1.0F), 0.75, 2.4, 0.08, keep);
            for (int i = 0; i < 7; i++) {
                double a1 = rot + i * 2 * Math.PI / 7;
                double a2 = rot + (i + 3) * 2 * Math.PI / 7;
                fx.line(green, Math.sin(a1) * 0.72, 2.4, Math.cos(a1) * 0.72, Math.sin(a2) * 0.72, 2.4,
                        Math.cos(a2) * 0.72, 0.08, keep);
            }
            if (fx.chance(keep)) {
                fx.at(ParticleTypes.SOUL_FIRE_FLAME, fx.spread(0.6), 2.4, fx.spread(0.6));
            }
        }
    }

    /** Skull outline above the player, slowly turning so everyone sees it. */
    private static void skull(Fx fx, double y, double keep, double jaw) {
        double angle = fx.age() * 0.06;
        double tx = Math.cos(angle);
        double tz = -Math.sin(angle);
        ParticleOptions bone = fx.dust(0xE8E8D0, 1.0F);
        ParticleOptions glow = fx.dust(0x40E0A0, 1.6F);
        fx.panelEllipse(bone, 0, 0, tx, tz, 0, y, 0.3, 0.3, -0.5, Math.PI + 0.5, 0.06, keep);
        fx.panelEllipse(bone, 0, 0, tx, tz, -0.11, y - 0.02, 0.07, 0.07, 0, 2 * Math.PI, 0.04, keep);
        fx.panelEllipse(bone, 0, 0, tx, tz, 0.11, y - 0.02, 0.07, 0.07, 0, 2 * Math.PI, 0.04, keep);
        fx.panelLine(glow, 0, 0, tx, tz, -0.11, y - 0.02, -0.11, y - 0.02, 0.1, keep);
        fx.panelLine(glow, 0, 0, tx, tz, 0.11, y - 0.02, 0.11, y - 0.02, 0.1, keep);
        fx.panelLine(bone, 0, 0, tx, tz, -0.03, y - 0.12, 0.03, y - 0.12, 0.03, keep);
        fx.panelLine(bone, 0, 0, tx, tz, -0.18, y - 0.2 - jaw, -0.12, y - 0.32 - jaw, 0.04, keep);
        fx.panelLine(bone, 0, 0, tx, tz, -0.12, y - 0.32 - jaw, 0.12, y - 0.32 - jaw, 0.04, keep);
        fx.panelLine(bone, 0, 0, tx, tz, 0.12, y - 0.32 - jaw, 0.18, y - 0.2 - jaw, 0.04, keep);
    }

    /** Grave outlines around the player, souls spiralling from them into the chest. */
    private static void necroGraves(Fx fx, int graves, double dist, double offset, int drawEnd, int handFrom,
                                    int soulFrom, int soulTo, double soulSpeed, int fadeFrom, int fadeTo) {
        int t = fx.age();
        ParticleOptions soil = fx.dust(0x3A2A1A, 1.2F);
        ParticleOptions bone = fx.dust(0xE8E8D0, 1.0F);
        ParticleOptions soul = fx.dust(0x40E0A0, 1.4F);
        double keep = fadeOut(t, fadeFrom, fadeTo);
        for (int g = 0; g < graves; g++) {
            double a = offset + g * 2 * Math.PI / graves;
            double rx = Math.sin(a);
            double rz = Math.cos(a);
            double tx = Math.cos(a);
            double tz = -Math.sin(a);
            double cx = rx * dist;
            double cz = rz * dist;
            if (t < fadeTo && (t <= drawEnd || fx.every(2))) {
                double w = Math.min(1, t / (double) drawEnd);
                double[][] c = {{0.45, 0.25}, {0.45, -0.25}, {-0.45, -0.25}, {-0.45, 0.25}, {0.45, 0.25}};
                for (int i = 0; i < 4; i++) {
                    double part = Mth.clamp(w * 4 - i, 0, 1);
                    if (part <= 0) {
                        break;
                    }
                    double x1 = cx + rx * c[i][0] + tx * c[i][1];
                    double z1 = cz + rz * c[i][0] + tz * c[i][1];
                    double x2 = cx + rx * c[i + 1][0] + tx * c[i + 1][1];
                    double z2 = cz + rz * c[i + 1][0] + tz * c[i + 1][1];
                    fx.line(soil, x1, FLOOR, z1, Mth.lerp(part, x1, x2), FLOOR, Mth.lerp(part, z1, z2), 0.08, keep);
                }
            }
            if (t < drawEnd + 8 && t % 4 == g % 4) {
                fx.debrisAt(cx, cz, 2);
                if (g == 0) {
                    fx.sound(SoundEvents.ROOTED_DIRT_BREAK, 0.5F, 0.8F);
                }
            }
            if (t >= handFrom && t < fadeTo && fx.every(2)) {
                double grow = Math.min(1, (t - handFrom) / 12.0) * keep;
                for (int hand = -1; hand <= 1; hand += 2) {
                    double hx = cx + tx * hand * 0.12;
                    double hz = cz + tz * hand * 0.12;
                    double top = 0.5 * grow;
                    fx.line(bone, hx, FLOOR, hz, hx, top, hz, 0.08, 1);
                    for (int finger = -1; finger <= 1; finger++) {
                        fx.line(bone, hx, top, hz, hx + tx * finger * 0.08, top + 0.13 * grow, hz + tz * finger * 0.08,
                                0.05, 1);
                    }
                }
            }
            if (t >= soulFrom && t < soulTo) {
                for (int j = 0; j < 4; j++) {
                    double s = ((t - soulFrom) * soulSpeed + j * 0.25) % 1.0;
                    double sa = a + s * Math.PI * 1.2;
                    double sr = dist * (1 - s);
                    fx.at(soul, Math.sin(sa) * sr, FLOOR + s * 1.1 + 0.1 * Math.sin(t * 0.3 + j), Math.cos(sa) * sr);
                }
                if (fx.chance(0.5)) {
                    fx.fly(ParticleTypes.SOUL, cx, FLOOR + 0.1, cz, 0, 1, 0, 0.03);
                }
            }
        }
    }

    /**
     * Necromancer: six graves crack open in a wide ring, bony hands claw up, souls spiral out of them into you,
     * soul mist rises, a skull forms above your head and screams.
     */
    private static void necromancerGrand(Fx fx) {
        int t = fx.age();
        necroGraves(fx, 6, 2.4, 0, 30, 40, 60, 145, 0.03, 165, 195);
        if (t >= 60 && t < 145 && (t - 60) % 8 == 0) {
            fx.sound(SoundEvents.SOUL_ESCAPE, 0.6F, 0.8F + (t - 60) / 170F);
        }
        if (t >= 100 && t < 150) {
            double a = fx.rand() * 2 * Math.PI;
            double r = fx.rand() * 2.4;
            fx.fly(ParticleTypes.SOUL, Math.sin(a) * r, FLOOR, Math.cos(a) * r, 0, 1, 0, 0.04);
        }
        if (t >= 90 && t < 192) {
            double keep = Math.min(1, (t - 90) / 12.0) * fadeOut(t, 178, 192);
            skull(fx, 2.7, keep, t >= 150 && t < 162 ? 0.1 : 0);
        }
        if (t == 150) {
            fx.sphereOut(ParticleTypes.SCULK_SOUL, 0, 2.5, 0, 40, 0.16);
            fx.burstItem(Items.BONE, 0, 2.5, 0, 14, 0.2);
            fx.flash(2.5);
            fx.sound(SoundEvents.WITHER_AMBIENT, 0.6F, 1.3F);
            fx.sound(SoundEvents.SOUL_ESCAPE, 1.0F, 0.8F);
        }
        if (t > 150) {
            double w = (t - 150) / 50.0;
            for (int k = 0; k < 4; k++) {
                double a = w * 4 * Math.PI + k * Math.PI / 2;
                double d = 0.6 + w * 1.2;
                fx.at(ParticleTypes.SOUL_FIRE_FLAME, Math.sin(a) * d, 1.0 + w * 2.5, Math.cos(a) * d);
            }
        }
    }

    /** Necromancer respawn: bones burst out of the ground in a ring and whirl up into a vortex of souls. */
    private static void necromancerLight(Fx fx) {
        int t = fx.age();
        ParticleOptions bone = fx.dust(0xE8E8D0, 1.1F);
        if (t == 0) {
            fx.sound(SoundEvents.SKELETON_AMBIENT, 0.7F, 0.8F);
            fx.groundDebris(12, 1.3);
        }
        if (t < 20) {
            double w = t < 6 ? 0 : smooth((t - 6) / 12.0);
            for (int k = 0; k < 10; k++) {
                double a = k * Math.PI / 5 + w * 3;
                double r = Mth.lerp(w, 1.2, 0.2);
                double base = t < 6 ? Mth.lerp(t / 6.0, -0.3, 0.2) : Mth.lerp(w, 0.2, 2.4);
                double x = Math.sin(a) * r;
                double z = Math.cos(a) * r;
                fx.line(bone, x, Math.max(FLOOR, base), z, x + Math.cos(a) * 0.12, base + 0.3, z - Math.sin(a) * 0.12,
                        0.06, 1);
                if (t >= 6) {
                    fx.at(ParticleTypes.SOUL, x, base, z);
                }
            }
            if (t == 6) {
                fx.burstItem(Items.BONE, 0, 0.3, 0, 8, 0.15);
                fx.sound(SoundEvents.SOUL_ESCAPE, 0.7F, 1.0F);
            }
        }
        if (t == 20) {
            fx.sphereOut(ParticleTypes.SCULK_SOUL, 0, 2.4, 0, 22, 0.15);
            fx.sound(SoundEvents.SOUL_ESCAPE, 1.0F, 0.7F);
            fx.sound(SoundEvents.WITHER_SKELETON_AMBIENT, 0.5F, 1.2F);
        }
        if (t > 20) {
            double w = (t - 20) / 26.0;
            for (int k = 0; k < 3; k++) {
                double a = w * 4 * Math.PI + k * 2 * Math.PI / 3;
                if (fx.chance(1 - w)) {
                    fx.at(ParticleTypes.SOUL_FIRE_FLAME, Math.sin(a) * (0.5 + w), 2.0 + w, Math.cos(a) * (0.5 + w));
                }
            }
        }
    }

    // =========================================================================
    // Faithful
    // =========================================================================

    /** Four glowing streaks from the sky down onto the player; only the moving front is placed (end rods linger). */
    private static void lightBeam(Fx fx, int from, int to, double top) {
        int t = fx.age();
        double q = fx.span(from, to);
        if (q < 0) {
            return;
        }
        double front = Mth.lerp(q, top, 0);
        double prev = Mth.lerp(Math.max(0, (t - 1 - from) / (double) (to - from)), top, 0);
        for (int k = 0; k < 4; k++) {
            double a = Math.PI / 4 + k * Math.PI / 2;
            double x = Math.sin(a) * 0.55;
            double z = Math.cos(a) * 0.55;
            fx.line(ParticleTypes.END_ROD, x, prev, z, x, front, z, 0.25, 1);
        }
        fx.line(fx.dust(0xFFF4C0, 2.0F), 0, front, 0, 0, top, 0, 0.3, 0.5);
    }

    private static final float[] CLERIC_MELODY = {1.0F, 1.26F, 1.5F, 1.33F, 1.68F, 1.5F, 1.26F, 1.5F, 1.68F, 2.0F};

    /**
     * Cleric: heaven opens high above you, a beam of light comes down, ten healing ripples roll out to a
     * little melody while hearts and feathers of light drift down, then a halo forms with a bell.
     */
    private static void clericGrand(Fx fx) {
        int t = fx.age();
        ParticleOptions gold = fx.dust(0xFFC830, 1.2F);
        if (t < 80 && fx.every(2)) {
            fx.ring(fx.dust(0xFFC830, 0.9F), Math.min(1, t / 30.0) * 1.4, 7.5, 0.1, fadeOut(t, 60, 80));
            fx.ring(fx.dust(0xFFF4C0, 0.9F), Math.min(1, t / 40.0) * 0.9, 7.5, 0.1, fadeOut(t, 60, 80));
            if (fx.chance(0.6)) {
                fx.at(ParticleTypes.END_ROD, fx.spread(1.4), 7.5, fx.spread(1.4));
            }
        }
        if (t == 0) {
            fx.sound(SoundEvents.BEACON_ACTIVATE, 0.6F, 1.4F);
        }
        lightBeam(fx, 40, 70, 7.5);
        lightBeam(fx, 110, 124, 7.5);
        if (t >= 70 && t < 150 && fx.every(2)) {
            fx.cloud(fx.dust(0xFFF4C0, 1.4F), 0, 1.8, 0, 6, 0.25, 1.4, 0.25, 0);
        }
        for (int k = 0; k < 10; k++) {
            int start = 70 + k * 8;
            double w = fx.span(start, start + 14);
            if (w >= 0) {
                fx.ring(fx.fade(0xFFC830, 0xFFF4C0, 1.2F), Mth.lerp(w, 0.3, 2.6), FLOOR, 0.14, 1 - w * 0.6);
            }
            if (t == start) {
                fx.sound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.8F, CLERIC_MELODY[k]);
            }
        }
        if (t >= 70 && t < 150 && t % 5 == 0) {
            double a = fx.rand() * 2 * Math.PI;
            fx.at(ParticleTypes.HEART, Math.sin(a) * 0.8, 1.0 + fx.rand() * 0.8, Math.cos(a) * 0.8);
        }
        if (t >= 70 && t < 150 && fx.every(3)) {
            double a = fx.rand() * 2 * Math.PI;
            double r = 0.5 + fx.rand() * 1.2;
            fx.fly(ParticleTypes.END_ROD, Math.sin(a) * r, 4.0, Math.cos(a) * r, 0, -1, 0, 0.05);
        }
        if (t == 150) {
            fx.ring(ParticleTypes.END_ROD, 0.45, 2.25, 0.12, 1);
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4;
                fx.at(ParticleTypes.HEART, Math.sin(a) * 0.8, 1.9, Math.cos(a) * 0.8);
            }
            fx.cloud(gold, 0, 2.0, 0, 30, 0.6, 0.4, 0.6, 0);
            fx.flash(2.2);
            fx.sound(SoundEvents.BELL_BLOCK, 1.0F, 1.5F);
            fx.sound(SoundEvents.BELL_RESONATE, 0.6F, 1.5F);
        }
        if (t >= 150) {
            fx.ring(fx.dust(0xFFC830, 1.5F), 0.45, 2.25, 0.07, fadeOut(t, 180, 200));
            if (fx.chance(fadeOut(t, 160, 200))) {
                fx.at(fx.dust(0xFFF4C0, 1.2F), fx.spread(1.5), 0.3 + fx.rand() * 2.5, fx.spread(1.5));
            }
        }
    }

    /** Cleric respawn: six small pillars of light rise around you one by one, a warm wave and a halo. */
    private static void clericLight(Fx fx) {
        int t = fx.age();
        for (int k = 0; k < 6; k++) {
            int at = k * 2;
            double a = k * Math.PI / 3;
            double x = Math.sin(a) * 1.3;
            double z = Math.cos(a) * 1.3;
            if (t == at) {
                fx.line(ParticleTypes.END_ROD, x, FLOOR, z, x, 1.2, z, 0.12, 1);
                fx.sound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.6F, CLERIC_MELODY[k]);
            }
            if (t >= at && t < 34 && fx.every(2)) {
                fx.line(fx.dust(0xFFF4C0, 1.3F), x, FLOOR, z, x, 1.2, z, 0.15, fadeOut(t, 20, 34));
            }
        }
        double w = fx.span(14, 26);
        if (w >= 0) {
            fx.ring(fx.fade(0xFFC830, 0xFFF4C0, 1.3F), Mth.lerp(w, 0.3, 1.8), FLOOR, 0.12, 1 - w * 0.6);
        }
        if (t == 14) {
            for (int i = 0; i < 4; i++) {
                double a = i * Math.PI / 2 + Math.PI / 4;
                fx.at(ParticleTypes.HEART, Math.sin(a) * 0.6, 1.8, Math.cos(a) * 0.6);
            }
            fx.sound(SoundEvents.BELL_BLOCK, 0.9F, 1.6F);
        }
        if (t >= 14 && t < 44) {
            fx.ring(fx.dust(0xFFC830, 1.5F), 0.4, 2.2, 0.07, fadeOut(t, 30, 44));
        }
    }

    /** Four arms of light spreading over the floor from the player, with flared ends. */
    private static void holyCross(Fx fx, double len, double keep) {
        ParticleOptions gold = fx.dust(0xF2D060, 1.2F);
        for (int k = 0; k < 4; k++) {
            double a = k * Math.PI / 2;
            double dx = Math.sin(a);
            double dz = Math.cos(a);
            fx.line(gold, dx * 0.3, FLOOR, dz * 0.3, dx * len, FLOOR, dz * len, 0.09, keep);
            fx.line(gold, dx * len + dz * 0.25, FLOOR, dz * len - dx * 0.25, dx * len - dz * 0.25, FLOOR,
                    dz * len + dx * 0.25, 0.07, keep);
        }
    }

    /**
     * Paladin: a giant greatsword of light slowly descends point-first from high in the sky and plants itself
     * through you, a cross of light spreads over the ground and glows, the sword dissolves and wings unfold.
     */
    private static void paladinGrand(Fx fx) {
        int t = fx.age();
        int land = 80;
        ParticleOptions blade = fx.dust(0xE8F0FF, 1.2F);
        ParticleOptions hilt = fx.dust(0xF2D060, 1.3F);
        if (t < 150) {
            double tip = t < land ? Mth.lerp(Math.pow(t / (double) land, 3), 14.0, 0) : 0;
            double keep = t < 140 ? 1 : fadeOut(t, 140, 150);
            fx.sword(blade, hilt, tip + 3.2, -1, 3.2, 0.8, 0.15, keep);
            if (t < land) {
                fx.at(ParticleTypes.END_ROD, 0, tip, 0);
            } else if (fx.chance(0.6)) {
                fx.fly(ParticleTypes.END_ROD, 0, fx.rand() * 3.2, 0, 0, 1, 0, t < 140 ? 0.01 : 0.1);
            }
        }
        if (t == 0) {
            fx.sound(SoundEvents.BEACON_ACTIVATE, 0.6F, 1.0F);
        }
        if (t == 50) {
            fx.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8F, 0.7F);
        }
        if (t == land) {
            fx.sound(SoundEvents.ANVIL_LAND, 0.9F, 0.7F);
            fx.sound(SoundEvents.BELL_BLOCK, 0.9F, 0.6F);
            fx.groundDebris(16, 1.0);
            fx.cloud(ParticleTypes.CRIT, 0, 0.2, 0, 24, 0.5, 0.1, 0.5, 0.3);
        }
        if (t >= land && t < 190 && fx.every(2)) {
            double w = Math.min(1, (t - land) / 30.0);
            holyCross(fx, Mth.lerp(w, 0.3, 3.0), fadeOut(t, 172, 190));
        }
        if (t >= 110 && t < 150 && fx.every(2)) {
            double a = (int) (fx.rand() * 4) * Math.PI / 2;
            double d = 0.4 + fx.rand() * 2.6;
            fx.fly(ParticleTypes.END_ROD, Math.sin(a) * d, FLOOR, Math.cos(a) * d, 0, 1, 0, 0.05);
        }
        if (t == 110 || t == 130) {
            fx.sound(SoundEvents.BELL_BLOCK, 0.5F, 0.8F + (t - 110) / 100F);
        }
        if (t == 150) {
            fx.sound(SoundEvents.BELL_BLOCK, 1.0F, 1.0F);
            fx.sound(SoundEvents.ENDER_DRAGON_FLAP, 0.5F, 1.6F);
            fx.cloud(hilt, 0, 1.5, 0, 30, 0.6, 0.6, 0.6, 0);
            fx.flash(1.6);
        }
        if (t >= 150 && fx.every(2)) {
            double spread = Math.min(1, (t - 150) / 12.0);
            fx.wings(fx.dust(0xF2D060, 1.6F), fx.dust(0xFFFFFF, 1.3F), spread, 1.3, fadeOut(t, 182, 200));
            if (fx.chance(0.7)) {
                fx.local(ParticleTypes.END_ROD, -0.4, 1.2 + fx.rand() * 1.0, fx.spread(1.6));
            }
        }
    }

    /** Paladin respawn: a golden dome of light builds up around you, pulses, and shatters into light. */
    private static void paladinLight(Fx fx) {
        int t = fx.age();
        ParticleOptions gold = fx.dust(0xF2D060, 1.0F);
        double radius = 1.3;
        if (t < 18) {
            double eMax = Math.min(1, t / 10.0) * Math.PI / 2;
            double keep = t < 10 ? 1 : 0.9;
            for (int m = 0; m < 8; m++) {
                double a = m * Math.PI / 4;
                for (double e = 0; e <= eMax; e += 0.1) {
                    if (fx.chance(keep)) {
                        fx.at(gold, Math.sin(a) * Math.cos(e) * radius, FLOOR + Math.sin(e) * radius,
                                Math.cos(a) * Math.cos(e) * radius);
                    }
                }
            }
            for (double e = 0.35; e < eMax; e += 0.4) {
                fx.ring(gold, Math.cos(e) * radius, FLOOR + Math.sin(e) * radius, 0.1, keep);
            }
            if (t >= 10 && t % 3 == 0) {
                fx.ring(fx.dust(0xFFFFFF, 1.3F), radius, FLOOR, 0.1, 1);
            }
        }
        if (t == 0) {
            fx.sound(SoundEvents.BEACON_POWER_SELECT, 0.6F, 1.4F);
        }
        if (t == 10) {
            fx.sound(SoundEvents.BELL_BLOCK, 0.7F, 1.2F);
        }
        if (t == 18) {
            for (int i = 0; i < 40; i++) {
                double[] d = Fx.sphereDirection(i, 40);
                if (d[1] < 0) {
                    continue;
                }
                fx.fly(ParticleTypes.END_ROD, d[0] * radius, FLOOR + d[1] * radius, d[2] * radius, d[0], d[1], d[2],
                        0.12);
            }
            fx.sound(SoundEvents.GLASS_BREAK, 0.7F, 1.3F);
            fx.sound(SoundEvents.BELL_BLOCK, 0.8F, 1.6F);
        }
        if (t > 18 && fx.chance(fadeOut(t, 26, 46))) {
            fx.at(fx.dust(0xF2D060, 1.3F), fx.spread(1.3), 0.3 + fx.rand() * 1.8, fx.spread(1.3));
        }
    }

    /** Inquisitor rite: a burning triangle is branded, a wall of fire rises, it collapses into a pillar. */
    private static void inquisitorRite(Fx fx, double brand, int brandEnd, double wallRadius, int wallFrom,
                                       double wallHeight, int peak, int end) {
        int t = fx.age();
        ParticleOptions orange = fx.dust(0xFF8A20, 1.1F);
        ParticleOptions scorch = fx.dust(0x3A2A20, 1.2F);
        if (t < end - 5) {
            double d = Math.min(1, t / (double) brandEnd);
            ParticleOptions ink = t < peak ? orange : scorch;
            if (fx.every(2)) {
                INQUISITOR_BRAND.draw(fx, ink, ink, brand, FLOOR, 0, 0, d, fadeOut(t, end - 25, end - 5), 0.1);
            }
            if (d < 1) {
                double[] tip = INQUISITOR_BRAND.point(d);
                fx.glyphPoint(ParticleTypes.FLAME, tip[0], tip[1], brand, FLOOR + 0.05, 0);
                fx.glyphPoint(ParticleTypes.LAVA, tip[0], tip[1], brand, FLOOR + 0.05, 0);
                if (fx.every(5)) {
                    fx.sound(SoundEvents.FIRE_AMBIENT, 0.5F, 1.0F);
                }
            }
            if (t < peak && fx.chance(0.5)) {
                double[] p = INQUISITOR_BRAND.point(fx.rand() * d);
                fx.glyphPoint(ParticleTypes.SMALL_FLAME, p[0], p[1], brand, FLOOR + 0.05, 0);
            }
        }
        if (t == 0 || t == brandEnd / 3 || t == brandEnd * 2 / 3) {
            fx.sound(SoundEvents.FLINTANDSTEEL_USE, 0.7F, 0.9F);
        }
        double w = fx.span(wallFrom, peak);
        if (w >= 0) {
            double h = smooth(w / 0.9) * wallHeight;
            for (int i = 0; i < 20; i++) {
                double a = fx.rand() * 2 * Math.PI;
                fx.fly(ParticleTypes.FLAME, Math.sin(a) * wallRadius, fx.rand() * h, Math.cos(a) * wallRadius, 0, 1, 0,
                        0.02);
            }
            for (double y = 0.3; y < h; y += 0.5) {
                fx.ring(orange, wallRadius, y, 0.15, 0.35);
            }
            if ((t - wallFrom) % 6 == 0) {
                fx.sound(SoundEvents.BLAZE_BURN, 0.5F, 0.8F);
            }
        }
        if (t == peak) {
            for (double y = 0.5; y < wallHeight; y += 0.6) {
                fx.radialIn(ParticleTypes.FLAME, wallRadius, y, 18, 0.18);
            }
            if (fx.grand()) {
                fx.flash(1.5);
            }
            fx.sound(SoundEvents.BLAZE_SHOOT, 1.0F, 0.6F);
            fx.sound(SoundEvents.FIRECHARGE_USE, 0.8F, 0.8F);
        }
        if (t >= peak && t < peak + 14) {
            for (int i = 0; i < 5; i++) {
                fx.fly(ParticleTypes.FLAME, fx.spread(0.15), 0.1, fx.spread(0.15), fx.spread(0.1), 1, fx.spread(0.1),
                        0.4);
            }
        }
        if (t > peak + 8 && fx.every(2)) {
            fx.cloud(ParticleTypes.WHITE_ASH, 0, 2.0, 0, 5, 1.4, 0.8, 1.4, 0);
            fx.cloud(ParticleTypes.ASH, 0, 2.0, 0, 4, 1.4, 0.8, 1.4, 0);
        }
        int eyeFrom = wallFrom + (peak - wallFrom) / 2;
        if (t >= eyeFrom && t < end && fx.every(2)) {
            double open = Math.min(1, (t - eyeFrom) / 10.0) * fadeOut(t, end - 12, end);
            double angle = t * 0.06;
            double tx = Math.cos(angle);
            double tz = -Math.sin(angle);
            double up = wallHeight + 0.4;
            double s = 0.45;
            ParticleOptions cream = fx.dust(0xFFF0B0, 1.0F);
            fx.panelLine(cream, 0, 0, tx, tz, 0, up + s, -s * 0.9, up - s * 0.5, 0.07, open);
            fx.panelLine(cream, 0, 0, tx, tz, -s * 0.9, up - s * 0.5, s * 0.9, up - s * 0.5, 0.07, open);
            fx.panelLine(cream, 0, 0, tx, tz, s * 0.9, up - s * 0.5, 0, up + s, 0.07, open);
            fx.eye(fx.dust(0xFF8A20, 1.0F), fx.dust(0xFF8A20, 2.0F), up - 0.05, 0.22, open, angle);
        }
    }

    /**
     * Inquisitor: a large burning triangle is branded around you, embers rain down while a tall wall of fire
     * rises, the eye of judgement watches, then the fire collapses into a pillar and ash falls.
     */
    private static void inquisitorGrand(Fx fx) {
        int t = fx.age();
        inquisitorRite(fx, 2.4, 60, 1.8, 60, 3.0, 150, 200);
        if (t >= 60 && t < 150 && fx.every(2)) {
            double a = fx.rand() * 2 * Math.PI;
            double r = fx.rand() * 1.6;
            fx.fly(ParticleTypes.SMALL_FLAME, Math.sin(a) * r, 4.0, Math.cos(a) * r, 0, -1, 0, 0.08);
        }
    }

    /** Inquisitor respawn: a spiral of fire runs down around you from head to feet and brands a triangle. */
    private static void inquisitorLight(Fx fx) {
        int t = fx.age();
        if (t < 16) {
            double front = Mth.lerp(t / 14.0, 2.4, 0);
            for (double y = 2.4; y >= Math.max(0, front); y -= 0.12) {
                double a = y * 4;
                if (fx.chance(0.7)) {
                    fx.at(fx.dust(0xFF8A20, 1.1F), Math.sin(a) * 0.7, y, Math.cos(a) * 0.7);
                }
            }
            double a = Math.max(0, front) * 4;
            fx.at(ParticleTypes.FLAME, Math.sin(a) * 0.7, Math.max(0, front), Math.cos(a) * 0.7);
            if (t % 4 == 0) {
                fx.sound(SoundEvents.FIRE_AMBIENT, 0.6F, 1.2F);
            }
        }
        if (t == 16) {
            fx.radial(ParticleTypes.FLAME, 0.2, 16, 0.15);
            fx.sound(SoundEvents.BLAZE_SHOOT, 0.8F, 0.8F);
            fx.sound(SoundEvents.FLINTANDSTEEL_USE, 0.7F, 0.8F);
        }
        if (t >= 16 && t < 42 && fx.every(2)) {
            int rgb = lerpColor((t - 16) / 14.0, 0xFF8A20, 0x3A2A20);
            ParticleOptions ink = fx.dust(rgb, 1.2F);
            INQUISITOR_BRAND.draw(fx, ink, ink, 1.1, FLOOR, 0, 0, 1, fadeOut(t, 30, 42), 0.1);
        }
        if (t > 20 && fx.every(2) && fx.chance(fadeOut(t, 30, 48))) {
            fx.cloud(ParticleTypes.WHITE_ASH, 0, 2.0, 0, 3, 1.0, 0.6, 1.0, 0);
        }
    }

    private static final float[] MONK_NOTES = {0.9F, 1.0F, 1.12F, 1.35F, 1.5F, 1.8F, 1.5F, 1.35F};

    /** One lotus petal on the floor, pointing outward at {@code angle}; {@code grow} 0..1 unfolds it. */
    private static void lotusPetal(Fx fx, double angle, double grow, double length, int rgb, double keep) {
        ParticleOptions edge = fx.dust(rgb, 1.0F);
        double dx = Math.sin(angle);
        double dz = Math.cos(angle);
        double px = Math.cos(angle);
        double pz = -Math.sin(angle);
        int steps = (int) Math.ceil(length * 9);
        for (int i = 0; i <= steps; i++) {
            double s = i / (double) steps * grow;
            double dist = Mth.lerp(s, 0.35, length);
            double half = 0.28 * length / 1.6 * Math.sin(Math.PI * s / Math.max(grow, 0.01)) * grow;
            for (int side = -1; side <= 1; side += 2) {
                if (fx.chance(keep)) {
                    fx.at(edge, dx * dist + px * half * side, FLOOR, dz * dist + pz * half * side);
                }
            }
        }
    }

    /** Horizontal yin-yang. */
    private static void yinYang(Fx fx, double y, double radius, double rot) {
        ParticleOptions white = fx.dust(0xFFFFFF, 1.0F);
        ParticleOptions orange = fx.dust(0xF2A640, 1.0F);
        fx.ring(white, radius, y, 0.07, 1);
        double half = radius / 2;
        double cx = Math.sin(rot) * half;
        double cz = Math.cos(rot) * half;
        fx.arcAt(orange, cx, y, cz, half, rot + Math.PI, rot + 2 * Math.PI, 0.06, 1);
        fx.arcAt(orange, -cx, y, -cz, half, rot, rot + Math.PI, 0.06, 1);
        fx.at(fx.dust(0xF2A640, 1.6F), cx, y, cz);
        fx.at(fx.dust(0xFFFFFF, 1.6F), -cx, y, -cz);
    }

    /**
     * Monk: five calm breaths swell a sphere of light, a double lotus opens petal by petal around you,
     * a yin-yang rises from the floor to your chest, then a palm strike rolls out like a temple gong.
     */
    private static void monkGrand(Fx fx) {
        int t = fx.age();
        if (t < 100 && fx.every(2)) {
            double r = 0.7 + 0.7 * (0.5 - 0.5 * Math.cos(2 * Math.PI * t / 20.0));
            for (int i = 0; i < 40; i++) {
                double[] d = Fx.sphereDirection(i, 40);
                double a = t * 0.05;
                double x = d[0] * Math.cos(a) - d[2] * Math.sin(a);
                double z = d[0] * Math.sin(a) + d[2] * Math.cos(a);
                fx.at(fx.dust(i % 5 == 0 ? 0x5FD0A0 : 0xFFFFFF, 0.9F), x * r, 1.0 + d[1] * r, z * r);
            }
        }
        if (t < 100 && t % 20 == 10) {
            fx.sound(SoundEvents.NOTE_BLOCK_FLUTE, 0.5F, 0.7F + t / 300F);
        }
        for (int k = 0; k < 16; k++) {
            boolean inner = k < 8;
            int born = inner ? 10 + k * 6 : 56 + (k - 8) * 6;
            if (t < born || t >= 195) {
                continue;
            }
            if (t == born) {
                fx.sound(SoundEvents.NOTE_BLOCK_CHIME, 0.5F, MONK_NOTES[k % 8] * (inner ? 1.0F : 0.75F));
            }
            if (t < born + 5 || fx.every(2)) {
                double angle = k * Math.PI / 4 + (inner ? 0 : Math.PI / 8);
                lotusPetal(fx, angle, Math.min(1, (t - born) / 6.0), inner ? 1.3 : 2.2,
                        inner ? 0xF090B0 : 0xF2A640, fadeOut(t, 170, 195));
            }
        }
        double yy = fx.span(100, 150);
        if (yy >= 0) {
            yinYang(fx, Mth.lerp(smooth(yy), 0.2, 1.1), 0.8, t * (0.1 + yy * 0.3));
            fx.at(fx.dust(0xFFFFFF, 1.5F), fx.spread(0.3), Mth.lerp(yy, 0.2, 2.0), fx.spread(0.3));
            if (t % 10 == 0) {
                fx.sound(SoundEvents.NOTE_BLOCK_CHIME, 0.4F, 0.8F + (float) yy);
            }
        }
        if (t == 150 || t == 160) {
            fx.radial(ParticleTypes.CLOUD, 1.1, 32, 0.3);
            fx.sound(SoundEvents.BELL_BLOCK, 1.0F, t == 150 ? 0.5F : 0.6F);
        }
        for (int k = 0; k < 2; k++) {
            double w = fx.span(150 + k * 10, 164 + k * 10);
            if (w >= 0) {
                fx.ring(fx.dust(0xFFFFFF, 1.2F), Mth.lerp(w, 0.5, 2.6), 1.1, 0.12, 1 - w * 0.5);
            }
        }
        if (t > 150 && fx.chance(fadeOut(t, 165, 200))) {
            double a = fx.rand() * 2 * Math.PI;
            fx.at(fx.dust(fx.chance(0.5) ? 0xFFFFFF : 0xF2A640, 1.3F), Math.sin(a) * 0.6, 0.2 + (t - 150) * 0.05,
                    Math.cos(a) * 0.6);
        }
    }

    /** Monk respawn: two chi orbs spiral up around your body from feet to head, then a calm pulse. */
    private static void monkLight(Fx fx) {
        int t = fx.age();
        if (t < 16) {
            double y = Mth.lerp(smooth(t / 16.0), 0.1, 2.1);
            for (int k = 0; k < 2; k++) {
                for (int m = 0; m < 5; m++) {
                    double yy = Math.max(0.1, y - m * 0.1);
                    double a = yy * 5 + k * Math.PI;
                    fx.at(fx.dust(k == 0 ? 0xFFFFFF : 0xF2A640, 1.8F - m * 0.3F), Math.sin(a) * 0.5, yy,
                            Math.cos(a) * 0.5);
                }
            }
            if (t % 4 == 0) {
                fx.sound(SoundEvents.NOTE_BLOCK_CHIME, 0.4F, MONK_NOTES[t / 4]);
            }
        }
        if (t == 16) {
            fx.sphereOut(ParticleTypes.END_ROD, 0, 2.1, 0, 12, 0.08);
            fx.radial(ParticleTypes.CLOUD, 1.1, 14, 0.2);
            fx.sound(SoundEvents.BELL_BLOCK, 0.6F, 0.8F);
        }
        double w = fx.span(16, 26);
        if (w >= 0) {
            fx.ring(fx.dust(0xFFFFFF, 1.2F), Mth.lerp(w, 0.4, 1.6), 1.1, 0.12, 1 - w * 0.5);
        }
        if (t > 16 && fx.chance(fadeOut(t, 26, 46))) {
            double a = fx.rand() * 2 * Math.PI;
            fx.at(fx.dust(0xF2A640, 1.2F), Math.sin(a) * 0.5, 1.0 + (t - 16) * 0.05, Math.cos(a) * 0.5);
        }
    }

    // =========================================================================
    // Alchemists
    // =========================================================================

    private static final int[] BREW_COLORS = {0x5FD0C8, 0xA8FF70, 0xFF70C0, 0xF2C84B, 0xFF8A20, 0xB080E8, 0x5FD0C8};
    private static final Item[] INGREDIENTS = {Items.NETHER_WART, Items.GLOWSTONE_DUST, Items.SUGAR,
            Items.GHAST_TEAR, Items.BLAZE_POWDER, Items.GOLDEN_CARROT};

    /** Apothecary rite: a bubbling pool, ingredients drop in, the brew spirals up into an orb that splashes. */
    private static void apothecaryRite(Fx fx, double pool, int[] drops, int streamFrom, int peak, int end) {
        int t = fx.age();
        int landed = 0;
        for (int i = 0; i < drops.length; i++) {
            int drop = drops[i];
            double a = hash(i, 31) * 2 * Math.PI;
            double d = hash(i, 32) * pool * 0.6;
            double x = Math.sin(a) * d;
            double z = Math.cos(a) * d;
            ItemParticleOption item = new ItemParticleOption(ParticleTypes.ITEM,
                    new ItemStack(INGREDIENTS[i % INGREDIENTS.length]));
            if (t == drop) {
                fx.fly(item, x, 2.8, z, 0, -1, 0, 0.1);
                fx.fly(item, x, 2.9, z, 0, -1, 0, 0.1);
            }
            if (t == drop + 6) {
                fx.cloud(fx.dust(BREW_COLORS[Math.min(BREW_COLORS.length - 1, i + 1)], 1.5F), x, 0.2, z, 14, 0.3,
                        0.1, 0.3, 0);
                fx.sound(SoundEvents.GENERIC_SPLASH, 0.5F, 1.4F);
            }
            if (t >= drop + 6) {
                landed = i + 1;
            }
        }
        int color = BREW_COLORS[Math.min(BREW_COLORS.length - 1, landed)];
        if (t < streamFrom + 5) {
            for (int i = 0; i < 5; i++) {
                double a = fx.rand() * 2 * Math.PI;
                double r = Math.sqrt(fx.rand()) * pool;
                fx.at(fx.dust(color, 1.5F), Math.sin(a) * r, FLOOR + fx.rand() * 0.3, Math.cos(a) * r);
            }
            if (fx.every(3)) {
                double a = fx.rand() * 2 * Math.PI;
                double r = Math.sqrt(fx.rand()) * pool;
                fx.ringAt(fx.dust(0xFFFFFF, 0.7F), Math.sin(a) * r, FLOOR + 0.1, Math.cos(a) * r, 0.08, 0.04, 1);
            }
            if (fx.every(2)) {
                fx.sound(SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, 0.3F, 0.8F + (float) fx.rand() * 0.6F);
            }
        }
        double s = fx.span(streamFrom, peak);
        if (s >= 0) {
            double h = Mth.lerp(smooth(s / 0.8), 0.1, 2.9);
            for (int k = 0; k < 3; k++) {
                int n = 22;
                for (int i = 0; i <= n; i++) {
                    double f = i / (double) n;
                    double y = f * h;
                    double r = Mth.lerp(f, pool * 0.6, 0.3);
                    double a = f * 4 * Math.PI + t * 0.3 + k * 2 * Math.PI / 3;
                    fx.at(fx.dust(k == 0 ? color : BREW_COLORS[k + 1], 1.2F), Math.sin(a) * r, y, Math.cos(a) * r);
                }
            }
            if (s > 0.4) {
                fx.sphere(fx.dust(color, 1.3F), 0, 2.9, 0, 0.4 * (s - 0.4) / 0.6, 24, 1);
            }
            if (t == streamFrom) {
                fx.sound(SoundEvents.BREWING_STAND_BREW, 0.8F, 1.0F);
            }
        }
        if (t == peak) {
            fx.burstItem(Items.SPLASH_POTION, 0, 2.9, 0, 12, 0.18);
            for (int i = 0; i < 36; i++) {
                int c = BREW_COLORS[i % BREW_COLORS.length];
                fx.at(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFF000000 | c),
                        fx.spread(1.8), 1.8 + fx.rand() * 1.4, fx.spread(1.8));
            }
            fx.cloud(ParticleTypes.HAPPY_VILLAGER, 0, 1.2, 0, 20, 0.9, 0.9, 0.9, 0);
            fx.flash(2.9);
            fx.sound(SoundEvents.SPLASH_POTION_BREAK, 1.0F, 1.0F);
        }
        if (t > peak) {
            double keep = fadeOut(t, peak + (end - peak) / 2, end);
            for (int i = 0; i < 5; i++) {
                if (fx.chance(keep)) {
                    double a = fx.rand() * 2 * Math.PI;
                    double r = fx.rand() * 1.8;
                    fx.at(fx.dust(BREW_COLORS[i], 1.1F), Math.sin(a) * r, fx.rand() * 2.6, Math.cos(a) * r);
                }
            }
            double w = (t - peak) / (double) (end - peak);
            for (int k = 0; k < 2; k++) {
                double a = w * 14 + k * Math.PI;
                fx.at(fx.fade(0x5FD0C8, 0xA8FF70, 1.3F), Math.sin(a) * 0.65, Mth.lerp(w, 2.2, 0.1), Math.cos(a) * 0.65);
            }
        }
    }

    /**
     * Apothecary: a wide pool bubbles at your feet, six ingredients drop in one by one and change its colour,
     * the brew rises around you in three spiralling streams into an orb above your head that splashes.
     */
    private static void apothecaryGrand(Fx fx) {
        apothecaryRite(fx, 2.2, new int[] {10, 30, 50, 70, 90, 110}, 115, 150, 200);
    }

    /** Apothecary respawn: you toss three potions around you, they splash and healing mist rises to you. */
    private static void apothecaryLight(Fx fx) {
        int t = fx.age();
        int[] colors = {0x5FD0C8, 0xA8FF70, 0xFF70C0};
        for (int k = 0; k < 3; k++) {
            int thrown = k * 4;
            double a = k * 2 * Math.PI / 3 + 0.4;
            double sx = Math.sin(a) * 1.4;
            double sz = Math.cos(a) * 1.4;
            if (t == thrown) {
                fx.sound(SoundEvents.SPLASH_POTION_THROW, 0.6F, 0.9F + k * 0.1F);
            }
            double w = fx.span(thrown, thrown + 6);
            if (w >= 0) {
                double x = sx * w;
                double z = sz * w;
                double y = Mth.lerp(w, 1.4, FLOOR) + 1.0 * Math.sin(Math.PI * w);
                fx.ringAt(fx.dust(0xE0F0FF, 0.7F), x, y, z, 0.1, 0.04, 1);
                fx.at(fx.dust(colors[k], 1.5F), x, y, z);
            }
            if (t == thrown + 6) {
                fx.cloud(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFF000000 | colors[k]), sx, 0.3, sz,
                        10, 0.3, 0.2, 0.3, 0);
                fx.burstItem(Items.SPLASH_POTION, sx, 0.3, sz, 3, 0.1);
                fx.cloud(fx.dust(colors[k], 1.4F), sx, 0.2, sz, 10, 0.3, 0.1, 0.3, 0);
                fx.sound(SoundEvents.SPLASH_POTION_BREAK, 0.6F, 0.9F + k * 0.15F);
            }
            if (t > thrown + 6 && t < 44) {
                double m = (t - thrown - 6) / 24.0;
                double r = 1.4 * (1 - m);
                double aa = a + m * 3;
                if (fx.chance(fadeOut(t, 30, 44))) {
                    fx.at(fx.dust(colors[k], 1.3F), Math.sin(aa) * r, 0.2 + m * 1.6, Math.cos(aa) * r);
                }
            }
        }
        if (t == 20) {
            fx.cloud(ParticleTypes.HAPPY_VILLAGER, 0, 1.0, 0, 12, 0.4, 0.6, 0.4, 0);
            fx.sound(SoundEvents.BREWING_STAND_BREW, 0.5F, 1.5F);
        }
    }

    /** Plague doctor rite: toxic fog rolls in, flies buzz, a swinging censer, then a purifying blast. */
    private static void plagueRite(Fx fx, double from, int fogEnd, int fliesFrom, int censerFrom, int peak, int end) {
        int t = fx.age();
        ParticleOptions fog = fx.dust(0x4A5A20, 3.2F);
        ParticleOptions fog2 = fx.dust(0x6A8A28, 2.6F);
        if (t < peak) {
            double r = Mth.lerp(Math.min(1, t / (double) fogEnd), from, 0.7);
            for (int i = 0; i < 7; i++) {
                double a = fx.rand() * 2 * Math.PI;
                double d = r * (0.8 + fx.rand() * 0.4);
                fx.at(i % 2 == 0 ? fog : fog2, Math.sin(a) * d, 0.1 + fx.rand() * 0.4, Math.cos(a) * d);
            }
            if (t == 3) {
                fx.sound(SoundEvents.PUFFER_FISH_BLOW_UP, 0.6F, 0.6F);
            }
        }
        if (t >= fliesFrom && t < peak) {
            ParticleOptions fly = fx.dust(0x101010, 0.6F);
            for (int i = 0; i < 8; i++) {
                double a = t * 0.35 * (i % 2 == 0 ? 1 : -1) + i;
                double r = 0.7 + 0.3 * Math.sin(t * 0.3 + i);
                double y = 1.2 + 0.5 * Math.sin(t * 0.47 + i * 2);
                fx.at(fly, Math.sin(a) * r + fx.spread(0.05), y, Math.cos(a) * r + fx.spread(0.05));
            }
            if ((t - fliesFrom) % 7 == 0) {
                fx.sound(SoundEvents.SILVERFISH_AMBIENT, 0.2F, 2.0F);
            }
        }
        if (t >= censerFrom && t < peak) {
            double a = t * 0.3;
            double x = Math.sin(a) * 0.8;
            double z = Math.cos(a) * 0.8;
            double y = 0.9 + 0.2 * Math.sin(t * 0.6);
            fx.at(fx.dust(0xE0B45A, 1.5F), x, y, z);
            fx.line(fx.dust(0x707070, 0.7F), x, y, z, x * 0.5, 1.5, z * 0.5, 0.1, 1);
            fx.fly(ParticleTypes.WHITE_SMOKE, x, y, z, 0, 1, 0, 0.02);
            if ((t - censerFrom) % 5 == 0) {
                fx.sound(SoundEvents.CHAIN_STEP, 0.4F, 1.4F);
            }
        }
        if (t == peak) {
            fx.radial(ParticleTypes.WHITE_SMOKE, 0.4, 28, 0.25);
            fx.radial(ParticleTypes.WHITE_SMOKE, 1.2, 20, 0.2);
            fx.ring(ParticleTypes.END_ROD, 0.6, 1.0, 0.2, 1);
            fx.sound(SoundEvents.FIRE_EXTINGUISH, 0.8F, 1.0F);
            fx.sound(SoundEvents.BREWING_STAND_BREW, 0.6F, 1.4F);
            if (fx.grand()) {
                fx.flash(1.0);
            }
        }
        if (t > peak) {
            double w = (t - peak) / (double) (end - peak);
            if (fx.every(2)) {
                fx.ring(fog, Mth.lerp(w, 0.8, from), 0.3, 0.25, 1 - w);
            }
            if (fx.chance(1 - w)) {
                fx.at(ParticleTypes.WHITE_ASH, fx.spread(1.4), 1.5 + fx.rand(), fx.spread(1.4));
            }
        }
    }

    /**
     * Plague Doctor: thick toxic fog rolls in from far around you, a swarm of flies buzzes around you,
     * then you swing a smoking censer until a purifying blast pushes the fog all the way back.
     */
    private static void plagueDoctorGrand(Fx fx) {
        plagueRite(fx, 3.4, 80, 30, 90, 150, 200);
    }

    /** Plague Doctor respawn: herbal smoke rings rise off your head, herbs swirl, dead flies drop around you. */
    private static void plagueDoctorLight(Fx fx) {
        int t = fx.age();
        if (t == 0) {
            fx.sound(SoundEvents.FIRE_EXTINGUISH, 0.5F, 0.8F);
        }
        if (t == 10) {
            fx.sound(SoundEvents.BREWING_STAND_BREW, 0.5F, 1.4F);
        }
        for (int k = 0; k < 3; k++) {
            double w = fx.span(k * 5, k * 5 + 18);
            if (w >= 0) {
                fx.ring(fx.fade(0xE8F0E0, 0x86A83C, 1.3F), 0.2 + w * 0.6, 2.0 + w * 1.5, 0.08, 1 - w);
            }
        }
        if (t < 28) {
            double keep = fadeOut(t, 16, 28);
            for (int i = 0; i < 4; i++) {
                double a = t * 0.3 + i * Math.PI / 2;
                if (fx.chance(keep)) {
                    fx.at(fx.dust(0x86A83C, 1.0F), Math.sin(a) * 0.5, 2.0 + 0.1 * Math.sin(t * 0.5 + i),
                            Math.cos(a) * 0.5);
                }
            }
        }
        if (t < 14) {
            for (int i = 0; i < 5; i++) {
                double a = i * 2 * Math.PI / 5 + 0.5;
                double y = Mth.lerp(t / 14.0, 1.6, FLOOR);
                fx.at(fx.dust(0x101010, 0.7F), Math.sin(a) * 1.0, y, Math.cos(a) * 1.0);
            }
        }
        if (t == 14) {
            fx.radial(ParticleTypes.WHITE_SMOKE, 0.3, 14, 0.15);
        }
    }

    /** Point on the fuse spiral at fraction {@code s} (0 = outer end, 1 = at the feet). */
    private static double[] fusePoint(double s, double radius, double turns) {
        double r = Mth.lerp(s, radius, 0.25);
        double a = s * turns * 2 * Math.PI;
        return new double[] {Math.sin(a) * r, Math.cos(a) * r};
    }

    /**
     * Bombardier: a long fuse spirals three times around you and burns slowly to your feet while firecrackers
     * pop all around, you flash white like lit TNT, then a triple explosion over your head.
     */
    private static void bombardierGrand(Fx fx) {
        int t = fx.age();
        double radius = 2.8;
        double turns = 3.0;
        int drawn = 20;
        int lit = 25;
        int flashFrom = 138;
        int peak = 150;
        ParticleOptions fuse = fx.dust(0x505058, 1.1F);
        ParticleOptions ash = fx.dust(0x202020, 0.9F);
        double length = turns * 2 * Math.PI * (radius + 0.25) / 2;
        int points = (int) Math.ceil(length / 0.1);
        double front = t < lit ? 0 : Mth.clamp((t - lit) / (double) (flashFrom - lit), 0, 1);
        double shown = t < drawn ? t / (double) drawn : 1;
        if (t < peak && (t < drawn || fx.every(2))) {
            for (int i = 0; i <= points; i++) {
                double s = i / (double) points;
                if (s > shown) {
                    break;
                }
                double[] p = fusePoint(s, radius, turns);
                if (s >= front) {
                    fx.at(fuse, p[0], FLOOR, p[1]);
                } else if (fx.chance(0.12)) {
                    fx.at(ash, p[0], FLOOR, p[1]);
                }
            }
        }
        if (t == lit) {
            fx.sound(SoundEvents.FLINTANDSTEEL_USE, 0.8F, 1.0F);
            fx.sound(SoundEvents.TNT_PRIMED, 0.6F, 1.2F);
        }
        if (t >= lit && t < flashFrom) {
            double[] p = fusePoint(front, radius, turns);
            fx.at(ParticleTypes.FLAME, p[0], FLOOR + 0.05, p[1]);
            fx.cloud(ParticleTypes.SMALL_FLAME, p[0], FLOOR + 0.05, p[1], 2, 0.03, 0.03, 0.03, 0.02);
            fx.fly(ParticleTypes.SMOKE, p[0], FLOOR + 0.1, p[1], 0, 1, 0, 0.03);
            if (fx.every(3)) {
                fx.at(ParticleTypes.LAVA, p[0], FLOOR + 0.1, p[1]);
                fx.sound(SoundEvents.FIRE_EXTINGUISH, 0.15F, 1.8F);
            }
        }
        if (t >= 45 && t < 128 && t % 4 == 1) {
            double a = hash(t, 41) * 2 * Math.PI;
            double d = 1.0 + hash(t, 42) * 1.8;
            fx.cloud(ParticleTypes.FIREWORK, Math.sin(a) * d, 0.3, Math.cos(a) * d, 6, 0.05, 0.05, 0.05, 0.1);
            fx.cloud(ParticleTypes.CRIT, Math.sin(a) * d, 0.3, Math.cos(a) * d, 4, 0.05, 0.05, 0.05, 0.2);
            fx.sound(SoundEvents.FIREWORK_ROCKET_BLAST, 0.3F, 1.5F + (float) hash(t, 43) * 0.4F);
        }
        if (t >= flashFrom && t < peak && fx.every(2)) {
            fx.sphere(fx.dust(0xFFFFFF, 1.4F), 0, 1.0, 0, 0.6, 30, 1);
        }
        if (t == flashFrom) {
            fx.sound(SoundEvents.TNT_PRIMED, 1.0F, 1.0F);
        }
        if (t == peak) {
            fx.at(ParticleTypes.EXPLOSION, 0, 2.4, 0);
            fx.at(ParticleTypes.EXPLOSION, 0.6, 2.0, 0.4);
            fx.at(ParticleTypes.EXPLOSION, -0.5, 1.9, -0.5);
            fx.radial(ParticleTypes.LARGE_SMOKE, 0.4, 24, 0.16);
            fx.burstItem(Items.GUNPOWDER, 0, 2.0, 0, 14, 0.2);
            fx.groundDebris(18, radius);
            fx.cloud(ParticleTypes.LAVA, 0, 2.0, 0, 8, 0.4, 0.3, 0.4, 0);
            fx.flash(2.0);
            fx.sound(SoundEvents.GENERIC_EXPLODE, 1.0F, 1.0F);
        }
        if (t > peak) {
            double w = (t - peak) / 50.0;
            if (w < 0.7 && fx.every(2)) {
                fx.ring(fx.dust(0x404048, 2.2F), Mth.lerp(w / 0.7, 0.6, radius), 0.3, 0.2, 1 - w / 0.7);
            }
            if (fx.chance(1 - w)) {
                fx.cloud(ParticleTypes.ASH, 0, 2.0, 0, 4, 1.2, 0.6, 1.2, 0);
            }
        }
    }

    /** Bombardier respawn: you lob six small bombs out around you and they pop one after another. */
    private static void bombardierLight(Fx fx) {
        int t = fx.age();
        for (int k = 0; k < 6; k++) {
            int thrown = k * 3;
            double a = k * Math.PI / 3 + 0.2;
            double sx = Math.sin(a) * 1.5;
            double sz = Math.cos(a) * 1.5;
            if (t == thrown) {
                fx.sound(SoundEvents.EGG_THROW, 0.5F, 0.6F);
            }
            double w = fx.span(thrown, thrown + 6);
            if (w >= 0) {
                double y = Mth.lerp(w, 1.4, FLOOR + 0.1) + 1.0 * Math.sin(Math.PI * w);
                fx.at(fx.dust(0x303038, 2.2F), sx * w, y, sz * w);
                fx.at(ParticleTypes.SMALL_FLAME, sx * w, y + 0.15, sz * w);
            }
            if (t == thrown + 6) {
                fx.cloud(ParticleTypes.POOF, sx, 0.3, sz, 6, 0.1, 0.1, 0.1, 0.05);
                fx.cloud(ParticleTypes.FLAME, sx, 0.3, sz, 6, 0.1, 0.1, 0.1, 0.06);
                fx.at(ParticleTypes.LAVA, sx, 0.3, sz);
                fx.debrisAt(sx, sz, 3);
                fx.sound(SoundEvents.GENERIC_EXPLODE, 0.25F, 1.6F);
            }
            if (t > thrown + 6 && t < 44 && fx.every(3)) {
                fx.fly(ParticleTypes.SMOKE, sx, 0.2, sz, 0, 1, 0, 0.03);
            }
        }
    }

    /**
     * Transmuter: three gyroscope rings trace themselves around you, spin faster and faster while elemental
     * orbs ride them, turn from lead to copper to gold, collapse into you, and a golden alchemy seal glows below.
     */
    private static void transmuterGrand(Fx fx) {
        int t = fx.age();
        int traceEnd = 60;
        int collapse = 144;
        int peak = 150;
        double spin = t <= collapse ? 0.02 * t + 0.0016 * t * t : 0.02 * collapse + 0.0016 * collapse * collapse;
        double color = t / (double) collapse;
        int rgb = color < 0.5 ? lerpColor(color * 2, 0x7A7A82, 0xD08050) : lerpColor(color * 2 - 1, 0xD08050, 0xF2C84B);
        ParticleOptions metal = fx.dust(rgb, 1.1F);
        if (t < peak) {
            double shrink = t < collapse ? 1 : 1 - (t - collapse) / (double) (peak - collapse) * 0.85;
            for (int k = 0; k < 3; k++) {
                double r = (1.2 + k * 0.3) * shrink;
                double[] n = switch (k) {
                    case 0 -> new double[] {Math.sin(spin), 0, Math.cos(spin)};
                    case 1 -> new double[] {0, Math.cos(spin * 1.3), Math.sin(spin * 1.3)};
                    default -> new double[] {Math.cos(spin * 0.8), Math.sin(spin * 0.8), 0};
                };
                double part = Mth.clamp((t - k * 20) / 20.0, 0, 1);
                if (part > 0) {
                    fx.ring3(metal, 0, 1.1, 0, r, n[0], n[1], n[2], 0, 2 * Math.PI * part, 0.08, 1);
                }
                if (t > traceEnd && t < collapse) {
                    int[] elements = {0xE04030, 0x4080FF, 0x40C050};
                    double[] p = Fx.ring3Point(0, 1.1, 0, r, n[0], n[1], n[2], t * 0.3 + k * 2);
                    fx.at(fx.dust(elements[k], 2.0F), p[0], p[1], p[2]);
                    fx.at(ParticleTypes.WAX_ON, p[0], p[1], p[2]);
                }
            }
            if (t > traceEnd && t < collapse && t % 6 == 0) {
                fx.sound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.5F, 0.7F + (float) color);
            }
            if (t == 72 || t == 120) {
                fx.sound(SoundEvents.SMITHING_TABLE_USE, 0.6F, 1.2F);
                fx.cloud(fx.dust(rgb, 1.5F), 0, 1.1, 0, 20, 0.8, 0.8, 0.8, 0);
            }
        }
        if (t == peak) {
            fx.burstItem(Items.GOLD_INGOT, 0, 1.2, 0, 12, 0.22);
            fx.burstItem(Items.GOLD_NUGGET, 0, 1.2, 0, 18, 0.22);
            fx.burstItem(Items.EMERALD, 0, 1.2, 0, 5, 0.2);
            fx.cloud(ParticleTypes.WAX_ON, 0, 1.2, 0, 24, 0.6, 0.6, 0.6, 0);
            fx.sphere(fx.dust(0xF2C84B, 1.5F), 0, 1.1, 0, 0.7, 40, 1);
            fx.flash(1.2);
            fx.sound(SoundEvents.AMETHYST_CLUSTER_BREAK, 1.0F, 0.8F);
            fx.sound(SoundEvents.ANVIL_USE, 0.3F, 1.8F);
            TRANSMUTER_SEAL.draw(fx, ParticleTypes.END_ROD, ParticleTypes.END_ROD, 2.2, FLOOR, 0, 0, 1, 0.6, 0.2);
        }
        if (t > peak && fx.every(2)) {
            TRANSMUTER_SEAL.draw(fx, fx.dust(0xF2C84B, 1.0F), fx.dust(0xB48CFF, 1.0F), 2.2, FLOOR,
                    (t - peak) * 0.01, 0, 1, fadeOut(t, 170, 200), 0.12);
        }
    }

    /** Transmuter respawn: the ground under you turns to gold in a spreading circle, and gold springs up. */
    private static void transmuterLight(Fx fx) {
        int t = fx.age();
        if (t == 0) {
            fx.burstBlock(Blocks.IRON_BLOCK, 0, 0.2, 0, 10, 0.1);
            fx.sound(SoundEvents.SMITHING_TABLE_USE, 0.7F, 1.3F);
        }
        if (t < 16) {
            double r = Mth.lerp(smooth(t / 16.0), 0.2, 1.6);
            fx.ring(fx.dust(0xF2C84B, 1.3F), r, FLOOR, 0.08, 1);
            fx.ring(fx.dust(0xB48CFF, 0.9F), r + 0.1, FLOOR, 0.12, 0.6);
            for (int i = 0; i < 10; i++) {
                double a = fx.rand() * 2 * Math.PI;
                double d = Math.sqrt(fx.rand()) * r;
                fx.at(fx.dust(0xF2C84B, 1.0F), Math.sin(a) * d, FLOOR, Math.cos(a) * d);
            }
            for (int i = 0; i < 2; i++) {
                double a = fx.rand() * 2 * Math.PI;
                fx.burstBlock(Blocks.GOLD_BLOCK, Math.sin(a) * r, 0.1, Math.cos(a) * r, 2, 0.08);
            }
            if (t % 4 == 0) {
                fx.sound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.5F, 0.8F + t * 0.05F);
            }
        }
        if (t == 16) {
            fx.burstItem(Items.GOLD_NUGGET, 0, 0.3, 0, 12, 0.25);
            fx.cloud(ParticleTypes.WAX_ON, 0, 0.8, 0, 16, 0.8, 0.6, 0.8, 0);
            fx.sound(SoundEvents.AMETHYST_CLUSTER_BREAK, 0.8F, 1.0F);
        }
        if (t >= 16 && t < 42 && fx.every(2)) {
            TRANSMUTER_SEAL.draw(fx, fx.dust(0xF2C84B, 1.0F), fx.dust(0xB48CFF, 1.0F), 1.6, FLOOR, 0, 0, 1,
                    fadeOut(t, 26, 42), 0.12);
        }
    }

    // =========================================================================
    // The Forsaken
    // =========================================================================

    private static double[] sphereAt(double lat, double lon, double radius) {
        return new double[] {Math.cos(lat) * Math.sin(lon) * radius, Math.sin(lat) * radius,
                Math.cos(lat) * Math.cos(lon) * radius};
    }

    private static final int[] FORSAKEN_BEATS = {0, 20, 38, 54, 68, 80, 90, 99, 107, 114, 120, 125, 129, 133, 136,
            139, 142, 145, 147};

    /**
     * Forsaken: a dark shell slowly closes around you from far away while your heartbeat speeds up, cracks of
     * light spread over it, six shards in the group colours glow inside, it trembles and shatters with a sonic boom.
     */
    private static void forsakenGrand(Fx fx) {
        int t = fx.age();
        double cy = 1.0;
        double radius = 1.6;
        int shellEnd = 80;
        int crackEnd = 135;
        int peak = 150;
        for (int b : FORSAKEN_BEATS) {
            if (t == b) {
                fx.sound(SoundEvents.WARDEN_HEARTBEAT, 1.0F, 1.0F);
            }
        }
        if (t < peak) {
            double r = Mth.lerp(smooth(t / (double) shellEnd), 3.0, radius);
            double maxY = t < shellEnd ? Mth.lerp(t / (double) shellEnd, -1, 1) : 1;
            double jitter = t >= crackEnd ? 0.08 : 0;
            for (int i = 0; i < 80; i++) {
                double yy = fx.rand() * (maxY + 1) - 1;
                double ring = Math.sqrt(1 - yy * yy);
                double a = fx.rand() * 2 * Math.PI;
                double py = cy + yy * r;
                if (py < 0.02) {
                    continue;
                }
                fx.at(fx.dust(i % 3 == 0 ? 0x3A3A48 : 0x18181E, i % 3 == 0 ? 1.4F : 2.0F),
                        Math.sin(a) * ring * r + fx.spread(jitter), py, Math.cos(a) * ring * r + fx.spread(jitter));
            }
            if (t >= shellEnd) {
                int n = (int) Math.min(14, 1 + 14 * (t - shellEnd) / (double) (crackEnd - shellEnd));
                ParticleOptions crack = fx.dust(0xC8C8D8, 1.0F);
                for (double[][] path : FORSAKEN_CRACKS) {
                    for (int i = 1; i < n; i++) {
                        double[] p1 = sphereAt(path[i - 1][0], path[i - 1][1], r * 1.01);
                        double[] p2 = sphereAt(path[i][0], path[i][1], r * 1.01);
                        fx.line(crack, p1[0], cy + p1[1], p1[2], p2[0], cy + p2[1], p2[2], 0.08, 1);
                    }
                    if (n < 14 && fx.chance(0.3)) {
                        double[] tip = sphereAt(path[n - 1][0], path[n - 1][1], r);
                        fx.at(ParticleTypes.END_ROD, tip[0], cy + tip[1], tip[2]);
                    }
                }
                for (int k = 0; k < 6; k++) {
                    double a = t * 0.12 + k * Math.PI / 3;
                    fx.at(fx.dust(FORSAKEN_SHARDS[k], 2.0F), Math.sin(a) * 0.6, 1.6, Math.cos(a) * 0.6);
                }
            }
            if (t >= crackEnd) {
                fx.radialIn(ParticleTypes.SMOKE, r, cy, 8, 0.1);
                if (t == crackEnd) {
                    fx.sound(SoundEvents.SCULK_SHRIEKER_SHRIEK, 0.4F, 0.8F);
                }
            }
        }
        if (t == peak) {
            for (int i = 0; i < 50; i++) {
                double[] d = Fx.sphereDirection(i, 50);
                fx.fly(ParticleTypes.SCULK_SOUL, d[0] * radius, cy + d[1] * radius, d[2] * radius, d[0], d[1], d[2],
                        0.2);
            }
            fx.at(ParticleTypes.SONIC_BOOM, 0, 1.2, 0);
            fx.flash(1.2);
            fx.sound(SoundEvents.WARDEN_SONIC_BOOM, 0.8F, 1.0F);
        }
        double s = fx.span(peak, peak + 10);
        if (s >= 0) {
            fx.sphere(fx.dust(0x18181E, 1.8F), 0, cy, 0, Mth.lerp(s, radius, radius + 1.2), 60, 1 - s);
        }
        if (t > peak) {
            double w = (t - peak) / 50.0;
            double d = w < 0.3 ? Mth.lerp(w / 0.3, 0.6, 2.0) : 2.0;
            for (int k = 0; k < 6; k++) {
                double a = (w < 0.3 ? peak * 0.12 : peak * 0.12 + (t - peak - 15) * 0.08) + k * Math.PI / 3;
                if (fx.chance(1 - w)) {
                    ParticleOptions shard = fx.dust(FORSAKEN_SHARDS[k], 2.0F);
                    fx.at(shard, Math.sin(a) * d, 1.6, Math.cos(a) * d);
                    fx.at(shard, Math.sin(a - 0.08) * d, 1.6, Math.cos(a - 0.08) * d);
                }
            }
            if (fx.chance(1 - w)) {
                fx.cloud(ParticleTypes.ASH, 0, 2.0, 0, 4, 1.4, 0.6, 1.4, 0);
            }
        }
    }

    /** Forsaken respawn: six shards in the group colours spiral in and slam into you with a dark pulse. */
    private static void forsakenLight(Fx fx) {
        int t = fx.age();
        if (t == 0) {
            fx.sound(SoundEvents.WARDEN_HEARTBEAT, 1.0F, 1.2F);
        }
        if (t < 14) {
            double w = smooth(t / 14.0);
            for (int k = 0; k < 6; k++) {
                for (int m = 0; m < 4; m++) {
                    double ww = Math.max(0, w - m * 0.05);
                    double a = k * Math.PI / 3 + ww * 3;
                    double r = Mth.lerp(ww, 2.5, 0.2);
                    double y = Mth.lerp(ww, 2.2, 1.2);
                    fx.at(fx.dust(FORSAKEN_SHARDS[k], 2.0F - m * 0.4F), Math.sin(a) * r, y, Math.cos(a) * r);
                }
            }
        }
        if (t == 14) {
            fx.sphereOut(ParticleTypes.SCULK_SOUL, 0, 1.2, 0, 24, 0.15);
            fx.sound(SoundEvents.WARDEN_HEARTBEAT, 1.0F, 0.8F);
            fx.sound(SoundEvents.SCULK_SHRIEKER_SHRIEK, 0.3F, 1.2F);
        }
        double pulse = fx.span(14, 24);
        if (pulse >= 0) {
            fx.ring(fx.dust(0x18181E, 2.2F), Mth.lerp(pulse, 0.3, 2.0), 1.2, 0.15, 1 - pulse * 0.5);
            fx.ring(fx.dust(0x6A6A78, 1.4F), Mth.lerp(pulse, 0.2, 1.6), FLOOR, 0.15, 1 - pulse * 0.5);
        }
        if (t > 14) {
            double w = (t - 14) / 32.0;
            for (int k = 0; k < 6; k++) {
                double a = t * 0.15 + k * Math.PI / 3;
                if (fx.chance((1 - w) * 0.6)) {
                    fx.at(fx.dust(FORSAKEN_SHARDS[k], 1.4F), Math.sin(a) * 0.7, 1.6, Math.cos(a) * 0.7);
                }
            }
            if (fx.chance(1 - w)) {
                fx.cloud(ParticleTypes.ASH, 0, 2.0, 0, 2, 1.0, 0.5, 1.0, 0);
            }
        }
    }

    // =========================================================================
    // Death (per group, 20 ticks) and level-up (everyone, 50 ticks)
    // =========================================================================

    private static Animation death(ClassGroup group) {
        return switch (group) {
            case WARRIORS -> ClassEffects::deathWarriors;
            case RANGERS -> ClassEffects::deathRangers;
            case ROGUES -> ClassEffects::deathRogues;
            case MAGES -> ClassEffects::deathMages;
            case FAITHFUL -> ClassEffects::deathFaithful;
            case ALCHEMISTS -> ClassEffects::deathAlchemists;
            case FORSAKEN -> ClassEffects::deathForsaken;
        };
    }

    /** Warriors: the armour shatters, a sword falls over and clatters onto the ground. */
    private static void deathWarriors(Fx fx) {
        int t = fx.age();
        ParticleOptions steel = fx.dust(0xC8D0E0, 1.2F);
        ParticleOptions gold = fx.dust(0xF2C84B, 1.1F);
        if (t == 0) {
            fx.burstBlock(Blocks.IRON_BLOCK, 0, 0.8, 0, 20, 0.25);
            fx.cloud(ParticleTypes.CRIT, 0, 0.8, 0, 14, 0.3, 0.4, 0.3, 0.3);
            fx.sound(SoundEvents.SHIELD_BREAK, 0.9F, 0.8F);
            fx.sound(SoundEvents.ARMOR_EQUIP_IRON, 0.8F, 0.6F);
        }
        double fall = smooth(Math.min(1, t / 10.0));
        double th = fall * Math.PI / 2;
        double keep = fadeOut(t, 12, 20);
        double bx = fx.lx(0, 0.6);
        double bz = fx.lz(0, 0.6);
        double dx = fx.lx(0, Math.sin(th));
        double dz = fx.lz(0, Math.sin(th));
        double dy = Math.cos(th);
        fx.line(gold, bx, FLOOR, bz, bx + dx * 0.3, FLOOR + dy * 0.3, bz + dz * 0.3, 0.07, keep);
        fx.line(steel, bx + dx * 0.3, FLOOR + dy * 0.3, bz + dz * 0.3, bx + dx * 1.4, FLOOR + dy * 1.4,
                bz + dz * 1.4, 0.07, keep);
        if (t == 10) {
            fx.sound(SoundEvents.ANVIL_LAND, 0.4F, 1.6F);
            fx.cloud(ParticleTypes.CRIT, bx + dx, 0.15, bz + dz, 8, 0.4, 0.05, 0.4, 0.15);
        }
        double w = fx.span(0, 12);
        if (w >= 0) {
            fx.ring(steel, Mth.lerp(w, 1.4, 0.3), 0.6, 0.12, 1 - w * 0.5);
        }
    }

    /** Rangers: a green spirit rises out of the body in a swirl of leaves and feathers. */
    private static void deathRangers(Fx fx) {
        int t = fx.age();
        if (t == 0) {
            fx.burstItem(Items.FEATHER, 0, 0.6, 0, 8, 0.12);
            fx.cloud(ParticleTypes.CHERRY_LEAVES, 0, 1.0, 0, 12, 0.5, 0.4, 0.5, 0);
            fx.sound(SoundEvents.ITEM_BREAK, 0.7F, 0.9F);
            fx.sound(SoundEvents.PHANTOM_FLAP, 0.5F, 0.8F);
        }
        double y = Mth.lerp(t / 20.0, 0.4, 3.0);
        for (int k = 0; k < 3; k++) {
            double a = t * 0.5 + k * 2 * Math.PI / 3;
            double r = 0.6 * (1 - t / 24.0);
            fx.at(fx.dust(0x7FD46B, 1.4F), Math.sin(a) * r, y - k * 0.2, Math.cos(a) * r);
            fx.at(ParticleTypes.COMPOSTER, Math.sin(a) * r, y - k * 0.2, Math.cos(a) * r);
        }
        fx.at(fx.dust(0xE8FFD8, 2.2F), 0, y, 0);
        if (t == 14) {
            fx.cloud(ParticleTypes.HAPPY_VILLAGER, 0, y, 0, 8, 0.2, 0.2, 0.2, 0);
        }
    }

    /** Rogues: gone in a puff of smoke and ink, a few coins left on the ground. */
    private static void deathRogues(Fx fx) {
        int t = fx.age();
        if (t == 0) {
            fx.cloud(ParticleTypes.LARGE_SMOKE, 0, 0.8, 0, 16, 0.3, 0.4, 0.3, 0.03);
            fx.sphereOut(ParticleTypes.SQUID_INK, 0, 0.8, 0, 16, 0.12);
            fx.burstItem(Items.GOLD_NUGGET, 0, 0.6, 0, 8, 0.15);
            fx.sound(SoundEvents.ENDERMAN_TELEPORT, 0.7F, 0.6F);
        }
        double w = fx.span(0, 14);
        if (w >= 0) {
            fx.ring(fx.dust(0x18181E, 2.4F), Mth.lerp(w, 0.3, 1.5), 0.2, 0.18, 1 - w);
            fx.ring(fx.dust(0x9A3AD8, 1.1F), Mth.lerp(w, 0.2, 1.2), 0.5, 0.14, 1 - w);
        }
        if (t == 8) {
            fx.sound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.3F, 0.6F);
        }
        if (t > 4 && t < 20 && t % 5 == 0) {
            double a = hash(t, 51) * 2 * Math.PI;
            double tx = Math.cos(a) * 0.06;
            double tz = -Math.sin(a) * 0.06;
            double x = Math.sin(a) * 1.1;
            double z = Math.cos(a) * 1.1;
            fx.at(fx.dust(0xC060FF, 1.2F), x + tx, 1.0, z + tz);
            fx.at(fx.dust(0xC060FF, 1.2F), x - tx, 1.0, z - tz);
        }
    }

    /** Mages: the magic collapses inward to one point, then the spell fizzles out in a burst of runes. */
    private static void deathMages(Fx fx) {
        int t = fx.age();
        double w = fx.span(0, 8);
        if (w >= 0) {
            fx.ring(fx.dust(0xA88BE8, 1.2F), Mth.lerp(w, 1.6, 0.1), 0.8, 0.1, 1);
            fx.ring(fx.dust(0x4A8CFF, 1.0F), Mth.lerp(w, 1.2, 0.1), FLOOR, 0.1, 1);
            fx.radialIn(ParticleTypes.ENCHANT, 1.4, 0.8, 6, 0.1);
        }
        if (t == 0) {
            fx.sound(SoundEvents.ILLUSIONER_MIRROR_MOVE, 0.7F, 0.6F);
        }
        if (t == 8) {
            fx.sphere(fx.dust(0xA88BE8, 1.4F), 0, 0.8, 0, 0.5, 30, 1);
            fx.sphereOut(ParticleTypes.WITCH, 0, 0.8, 0, 20, 0.2);
            fx.sphereOut(ParticleTypes.END_ROD, 0, 0.8, 0, 12, 0.1);
            fx.cloud(ParticleTypes.ENCHANT, 0, 1.2, 0, 30, 0.6, 0.6, 0.6, 0.5);
            fx.sound(SoundEvents.AMETHYST_CLUSTER_BREAK, 0.9F, 0.7F);
            fx.sound(SoundEvents.FIRE_EXTINGUISH, 0.5F, 1.2F);
        }
        if (t > 8) {
            fx.at(ParticleTypes.SMOKE, fx.spread(0.3), 0.8 + fx.rand() * 0.4, fx.spread(0.3));
        }
    }

    /** Faithful: the halo breaks, a column of soft light carries the soul upward. */
    private static void deathFaithful(Fx fx) {
        int t = fx.age();
        if (t == 0) {
            fx.cloud(fx.dust(0xFFC830, 1.4F), 0, 2.0, 0, 16, 0.3, 0.05, 0.3, 0);
            fx.sound(SoundEvents.BELL_BLOCK, 0.9F, 0.5F);
            fx.sound(SoundEvents.GLASS_BREAK, 0.4F, 1.5F);
        }
        double y = Mth.lerp(t / 20.0, 0.5, 4.0);
        fx.at(fx.dust(0xFFF4C0, 2.4F), 0, y, 0);
        fx.ring(fx.dust(0xFFC830, 1.0F), 0.3, y - 0.2, 0.1, 1 - t / 20.0);
        for (int k = 0; k < 4; k++) {
            double a = Math.PI / 4 + k * Math.PI / 2;
            fx.fly(ParticleTypes.END_ROD, Math.sin(a) * 0.5, 0.2, Math.cos(a) * 0.5, 0, 1, 0, 0.12);
        }
        if (t == 10) {
            fx.burstItem(Items.FEATHER, 0, y, 0, 4, 0.08);
        }
    }

    /** Alchemists: flasks shatter, coloured potion gas billows out. */
    private static void deathAlchemists(Fx fx) {
        int t = fx.age();
        if (t == 0) {
            fx.burstItem(Items.SPLASH_POTION, 0, 0.8, 0, 12, 0.2);
            fx.burstItem(Items.GLASS_BOTTLE, 0, 0.8, 0, 6, 0.15);
            fx.sound(SoundEvents.SPLASH_POTION_BREAK, 1.0F, 0.8F);
            fx.sound(SoundEvents.GLASS_BREAK, 0.6F, 1.0F);
        }
        int[] colors = {0x5FD0C8, 0xA8FF70, 0xFF70C0, 0xF2C84B};
        if (t < 16) {
            double r = Mth.lerp(t / 16.0, 0.3, 1.5);
            for (int i = 0; i < 6; i++) {
                double a = fx.rand() * 2 * Math.PI;
                fx.at(fx.dust(colors[i % 4], 2.4F), Math.sin(a) * r, 0.2 + fx.rand() * 0.8, Math.cos(a) * r);
            }
        }
        if (t == 4) {
            for (int i = 0; i < 16; i++) {
                fx.at(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFF000000 | colors[i % 4]),
                        fx.spread(0.8), 0.4 + fx.rand(), fx.spread(0.8));
            }
            fx.sound(SoundEvents.FIRE_EXTINGUISH, 0.5F, 0.8F);
        }
    }

    /** The Forsaken: the body is pulled into a dark point, six coloured shards flee from it. */
    private static void deathForsaken(Fx fx) {
        int t = fx.age();
        if (t == 0) {
            fx.sound(SoundEvents.WARDEN_HEARTBEAT, 1.0F, 0.6F);
            fx.sound(SoundEvents.SCULK_SHRIEKER_SHRIEK, 0.3F, 0.6F);
        }
        double w = fx.span(0, 10);
        if (w >= 0) {
            fx.sphere(fx.dust(0x18181E, 2.2F), 0, 0.8, 0, Mth.lerp(w, 1.2, 0.05), 40, 1);
            fx.radialIn(ParticleTypes.SMOKE, 1.4, 0.8, 6, 0.12);
        }
        if (t == 10) {
            fx.sphereOut(ParticleTypes.SCULK_SOUL, 0, 0.8, 0, 16, 0.15);
            fx.sound(SoundEvents.WARDEN_HEARTBEAT, 1.0F, 0.4F);
        }
        double s = fx.span(10, 20);
        if (s >= 0) {
            for (int k = 0; k < 6; k++) {
                double a = k * Math.PI / 3;
                double d = Mth.lerp(s, 0.2, 1.8);
                ParticleOptions shard = fx.dust(FORSAKEN_SHARDS[k], 2.0F);
                fx.at(shard, Math.sin(a) * d, 0.8 + s * 0.4, Math.cos(a) * d);
                fx.at(shard, Math.sin(a) * (d - 0.15), 0.8 + s * 0.4, Math.cos(a) * (d - 0.15));
            }
        }
    }

    private static final float[] LEVEL_NOTES = {1.0F, 1.26F, 1.5F, 1.68F, 2.0F};

    /**
     * Level up (the same for everyone): green and gold strands spiral up around you with a rising arpeggio,
     * a burst of stars above your head, a ring at your feet, and a crown of stars that fades.
     */
    private static void levelUp(Fx fx) {
        int t = fx.age();
        ParticleOptions green = fx.dust(0x7FFF40, 1.2F);
        ParticleOptions gold = fx.dust(0xFFD84A, 1.2F);
        if (t < 20) {
            double top = Mth.lerp(smooth(t / 20.0), 0.1, 2.4);
            for (int k = 0; k < 2; k++) {
                for (double y = Math.max(0.1, top - 0.8); y <= top; y += 0.1) {
                    double a = y * 4 + k * Math.PI + t * 0.15;
                    fx.at(k == 0 ? green : gold, Math.sin(a) * 0.7, y, Math.cos(a) * 0.7);
                }
                double a = top * 4 + k * Math.PI + t * 0.15;
                fx.at(ParticleTypes.END_ROD, Math.sin(a) * 0.7, top, Math.cos(a) * 0.7);
            }
            if (t % 4 == 0) {
                fx.sound(SoundEvents.NOTE_BLOCK_CHIME, 0.6F, LEVEL_NOTES[t / 4]);
            }
        }
        if (t == 20) {
            fx.sphereOut(ParticleTypes.FIREWORK, 0, 2.4, 0, 24, 0.14);
            fx.sphereOut(ParticleTypes.END_ROD, 0, 2.4, 0, 10, 0.08);
            fx.cloud(gold, 0, 2.4, 0, 16, 0.3, 0.2, 0.3, 0);
            fx.sound(SoundEvents.PLAYER_LEVELUP, 0.8F, 1.0F);
        }
        double ring = fx.span(20, 32);
        if (ring >= 0) {
            fx.ring(green, Mth.lerp(ring, 0.3, 1.8), FLOOR, 0.12, 1 - ring * 0.6);
            fx.ring(gold, Mth.lerp(ring, 0.2, 1.4), FLOOR, 0.12, 1 - ring * 0.6);
        }
        if (t > 20) {
            double w = (t - 20) / 30.0;
            for (int k = 0; k < 8; k++) {
                double a = t * 0.12 + k * Math.PI / 4;
                if (fx.chance(1 - w)) {
                    fx.at(k % 2 == 0 ? gold : green, Math.sin(a) * 0.5, 2.3 + 0.08 * Math.sin(t * 0.4 + k),
                            Math.cos(a) * 0.5);
                }
            }
            if (fx.chance(1 - w)) {
                double a = fx.rand() * 2 * Math.PI;
                double r = 0.3 + fx.rand() * 0.8;
                fx.at(green, Math.sin(a) * r, 0.4 + fx.rand() * 1.8, Math.cos(a) * r);
            }
        }
    }

    // =========================================================================
    // Sigil geometry
    // =========================================================================

    /**
     * A floor drawing made of strokes in unit coordinates: x = right, z = forward, outer radius 1.
     * Strokes are traced in the order they were added.
     */
    private static final class Glyph {
        private final List<Stroke> strokes = new ArrayList<>();
        private double total;

        private record Stroke(double[] xs, double[] zs, boolean accent, double start, double length) {
        }

        private Glyph path(boolean accent, double... xz) {
            int n = xz.length / 2;
            double[] xs = new double[n];
            double[] zs = new double[n];
            double length = 0;
            for (int i = 0; i < n; i++) {
                xs[i] = xz[2 * i];
                zs[i] = xz[2 * i + 1];
                if (i > 0) {
                    length += Math.hypot(xs[i] - xs[i - 1], zs[i] - zs[i - 1]);
                }
            }
            this.strokes.add(new Stroke(xs, zs, accent, this.total, length));
            this.total += length;
            return this;
        }

        /** Arc around (cx, cz); angle 0 points forward, positive turns to the right. */
        private Glyph arc(boolean accent, double cx, double cz, double radius, double from, double to) {
            int n = Math.max(4, (int) Math.ceil(Math.abs(to - from) / (Math.PI / 32)));
            double[] xz = new double[(n + 1) * 2];
            for (int i = 0; i <= n; i++) {
                double angle = from + (to - from) * i / n;
                xz[2 * i] = cx + Math.sin(angle) * radius;
                xz[2 * i + 1] = cz + Math.cos(angle) * radius;
            }
            return this.path(accent, xz);
        }

        private Glyph circle(boolean accent, double radius) {
            return this.arc(accent, 0, 0, radius, 0, 2.0 * Math.PI);
        }

        private Glyph polygon(boolean accent, int sides, double radius, double rot) {
            double[] xz = new double[(sides + 1) * 2];
            for (int i = 0; i <= sides; i++) {
                double angle = rot + 2.0 * Math.PI * i / sides;
                xz[2 * i] = Math.sin(angle) * radius;
                xz[2 * i + 1] = Math.cos(angle) * radius;
            }
            return this.path(accent, xz);
        }

        /** Star polygon {n/k}; splits into several strokes when n and k share a divisor. */
        private Glyph star(boolean accent, int n, int k, double radius, double rot) {
            int groups = gcd(n, k);
            int per = n / groups;
            for (int j = 0; j < groups; j++) {
                double[] xz = new double[(per + 1) * 2];
                for (int i = 0; i <= per; i++) {
                    double angle = rot + 2.0 * Math.PI * ((j + i * k) % n) / n;
                    xz[2 * i] = Math.sin(angle) * radius;
                    xz[2 * i + 1] = Math.cos(angle) * radius;
                }
                this.path(accent, xz);
            }
            return this;
        }

        private Glyph rays(boolean accent, int n, double r1, double r2, double rot) {
            for (int i = 0; i < n; i++) {
                double angle = rot + 2.0 * Math.PI * i / n;
                this.path(accent, Math.sin(angle) * r1, Math.cos(angle) * r1, Math.sin(angle) * r2,
                        Math.cos(angle) * r2);
            }
            return this;
        }

        /** Rose curve r = cos(k * angle): 2k petals for even k. */
        private Glyph rose(boolean accent, int k) {
            int n = 240;
            double[] xz = new double[(n + 1) * 2];
            for (int i = 0; i <= n; i++) {
                double angle = 2.0 * Math.PI * i / n;
                double radius = Math.cos(k * angle);
                xz[2 * i] = Math.sin(angle) * radius;
                xz[2 * i + 1] = Math.cos(angle) * radius;
            }
            return this.path(accent, xz);
        }

        private static int gcd(int a, int b) {
            return b == 0 ? a : gcd(b, a % b);
        }

        private int strokeAt(double progress) {
            double at = Mth.clamp(progress, 0, 1) * this.total;
            for (int i = 0; i < this.strokes.size(); i++) {
                Stroke stroke = this.strokes.get(i);
                if (at <= stroke.start() + stroke.length()) {
                    return i;
                }
            }
            return this.strokes.size() - 1;
        }

        /** Unit point {x, z} at a fraction of the total pen length. */
        private double[] point(double progress) {
            double at = Mth.clamp(progress, 0, 1) * this.total;
            for (Stroke stroke : this.strokes) {
                if (at > stroke.start() + stroke.length()) {
                    continue;
                }
                double[] xs = stroke.xs();
                double[] zs = stroke.zs();
                double pos = stroke.start();
                for (int i = 1; i < xs.length; i++) {
                    double seg = Math.hypot(xs[i] - xs[i - 1], zs[i] - zs[i - 1]);
                    if (at <= pos + seg || i == xs.length - 1) {
                        double f = seg > 0 ? Mth.clamp((at - pos) / seg, 0, 1) : 0;
                        return new double[] {Mth.lerp(f, xs[i - 1], xs[i]), Mth.lerp(f, zs[i - 1], zs[i])};
                    }
                    pos += seg;
                }
            }
            Stroke last = this.strokes.get(this.strokes.size() - 1);
            return new double[] {last.xs()[last.xs().length - 1], last.zs()[last.zs().length - 1]};
        }

        /** Draws the part between two fractions of the pen length, one particle every {@code step} blocks. */
        private void draw(Fx fx, ParticleOptions main, ParticleOptions accent, double radius, double dy, double rot,
                          double from, double to, double keep, double step) {
            double lo = from * this.total;
            double hi = to * this.total;
            for (Stroke stroke : this.strokes) {
                if (stroke.start() > hi || stroke.start() + stroke.length() < lo) {
                    continue;
                }
                ParticleOptions particle = stroke.accent() ? accent : main;
                double[] xs = stroke.xs();
                double[] zs = stroke.zs();
                double pos = stroke.start();
                for (int i = 1; i < xs.length; i++) {
                    double seg = Math.hypot(xs[i] - xs[i - 1], zs[i] - zs[i - 1]);
                    double a = Math.max(lo, pos);
                    double b = Math.min(hi, pos + seg);
                    if (b > a && seg > 0) {
                        int n = Math.max(1, (int) Math.ceil((b - a) * radius / step));
                        for (int k = 0; k < n; k++) {
                            if (keep < 1 && !fx.chance(keep)) {
                                continue;
                            }
                            double f = (a - pos + (b - a) * k / n) / seg;
                            fx.glyphPoint(particle, Mth.lerp(f, xs[i - 1], xs[i]), Mth.lerp(f, zs[i - 1], zs[i]),
                                    radius, dy, rot);
                        }
                    }
                    pos += seg;
                }
            }
        }
    }

    /** Six crack paths over a sphere, as {latitude, longitude} points, the same every run. */
    private static double[][][] forsakenCracks() {
        double[][][] cracks = new double[6][14][2];
        for (int c = 0; c < 6; c++) {
            double lat = (hash(c, 3) - 0.5) * 1.2;
            double lon = c * Math.PI / 3 + hash(c, 4) * 0.5;
            for (int i = 0; i < 14; i++) {
                cracks[c][i][0] = lat;
                cracks[c][i][1] = lon;
                lat = Mth.clamp(lat + (hash(c * 20 + i, 5) - 0.5) * 0.45, -1.3, 1.3);
                lon += 0.16 + (hash(c * 20 + i, 6) - 0.5) * 0.2;
            }
        }
        return cracks;
    }

    // =========================================================================
    // Drawing toolkit. All offsets are in blocks, relative to where the ceremony started (feet).
    // Angle 0 points south (+z), positive turns towards east (+x).
    // =========================================================================

    private static final class Fx {
        private final ServerLevel level;
        private final Mode mode;
        private final double x;
        private final double y;
        private final double z;
        private final double forwardX;
        private final double forwardZ;
        private final int age;
        private final RandomSource random = RandomSource.create();

        private Fx(ServerLevel level, Ceremony ceremony) {
            this.level = level;
            this.mode = ceremony.mode;
            this.x = ceremony.x;
            this.y = ceremony.y;
            this.z = ceremony.z;
            float rad = (float) Math.toRadians(ceremony.yaw);
            this.forwardX = -Math.sin(rad);
            this.forwardZ = Math.cos(rad);
            this.age = ceremony.age;
        }

        // ---- Timeline ----

        private boolean grand() {
            return this.mode.grand();
        }

        private int age() {
            return this.age;
        }

        private boolean every(int interval) {
            return this.age % interval == 0;
        }

        /** 0..1 while {@code from <= age < to}, -1 otherwise. */
        private double span(int from, int to) {
            if (this.age < from || this.age >= to) {
                return -1;
            }
            return (double) (this.age - from) / (to - from);
        }

        // ---- Randomness ----

        private double rand() {
            return this.random.nextDouble();
        }

        private double spread(double range) {
            return (this.random.nextDouble() * 2.0 - 1.0) * range;
        }

        private boolean chance(double probability) {
            return this.random.nextDouble() < probability;
        }

        // ---- Particle options ----

        private static Vector3f color(int rgb) {
            return new Vector3f(((rgb >> 16) & 0xFF) / 255.0F, ((rgb >> 8) & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F);
        }

        private ParticleOptions dust(int rgb, float size) {
            return new DustParticleOptions(color(rgb), size);
        }

        private ParticleOptions fade(int fromRgb, int toRgb, float size) {
            return new DustColorTransitionOptions(color(fromRgb), color(toRgb), size);
        }

        private BlockState ground() {
            BlockState state = this.level.getBlockState(BlockPos.containing(this.x, this.y - 0.5, this.z));
            return state.isAir() ? Blocks.DIRT.defaultBlockState() : state;
        }

        // ---- Local frame (only used for shapes that are symmetric around the player) ----

        private double lx(double forward, double right) {
            return this.forwardX * forward - this.forwardZ * right;
        }

        private double lz(double forward, double right) {
            return this.forwardZ * forward + this.forwardX * right;
        }

        private void local(ParticleOptions particle, double forward, double up, double right) {
            this.at(particle, this.lx(forward, right), up, this.lz(forward, right));
        }

        private void localLine(ParticleOptions particle, double forward1, double up1, double right1,
                               double forward2, double up2, double right2, double spacing, double keep) {
            this.line(particle, this.lx(forward1, right1), up1, this.lz(forward1, right1),
                    this.lx(forward2, right2), up2, this.lz(forward2, right2), spacing, keep);
        }

        /** Places a unit glyph point, scaled by {@code radius}, turned by {@code rot}, at height {@code dy}. */
        private void glyphPoint(ParticleOptions particle, double ux, double uz, double radius, double dy, double rot) {
            double cos = Math.cos(rot);
            double sin = Math.sin(rot);
            double right = (ux * cos - uz * sin) * radius;
            double forward = (ux * sin + uz * cos) * radius;
            this.local(particle, forward, dy, right);
        }

        // ---- Primitives ----

        private void at(ParticleOptions particle, double dx, double dy, double dz) {
            this.level.sendParticles(particle, this.x + dx, this.y + dy, this.z + dz, 1, 0.0, 0.0, 0.0, 0.0);
        }

        /** One particle moving along (vx, vy, vz) * speed. */
        private void fly(ParticleOptions particle, double dx, double dy, double dz,
                         double vx, double vy, double vz, double speed) {
            this.level.sendParticles(particle, this.x + dx, this.y + dy, this.z + dz, 0, vx, vy, vz, speed);
        }

        private void cloud(ParticleOptions particle, double dx, double dy, double dz, int count,
                           double spreadX, double spreadY, double spreadZ, double speed) {
            this.level.sendParticles(particle, this.x + dx, this.y + dy, this.z + dz, count,
                    spreadX, spreadY, spreadZ, speed);
        }

        private void flash(double dy) {
            this.at(ParticleTypes.FLASH, 0, dy, 0);
        }

        private void line(ParticleOptions particle, double x1, double y1, double z1,
                          double x2, double y2, double z2, double spacing, double keep) {
            double length = Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1) + (z2 - z1) * (z2 - z1));
            int count = Math.max(1, (int) Math.ceil(length / spacing));
            for (int i = 0; i <= count; i++) {
                if (keep < 1 && !this.chance(keep)) {
                    continue;
                }
                double f = (double) i / count;
                this.at(particle, Mth.lerp(f, x1, x2), Mth.lerp(f, y1, y2), Mth.lerp(f, z1, z2));
            }
        }

        /** Flat ring around the player; {@code keep} below 1 randomly leaves points out to fade it. */
        private void ring(ParticleOptions particle, double radius, double dy, double spacing, double keep) {
            this.ringAt(particle, 0, dy, 0, radius, spacing, keep);
        }

        private void ringAt(ParticleOptions particle, double cx, double cy, double cz, double radius, double spacing,
                            double keep) {
            this.arcAt(particle, cx, cy, cz, radius, 0, 2.0 * Math.PI, spacing, keep);
        }

        private void arcAt(ParticleOptions particle, double cx, double cy, double cz, double radius, double from,
                           double to, double spacing, double keep) {
            int count = Math.max(4, (int) Math.ceil(Math.abs(to - from) * radius / spacing));
            for (int i = 0; i < count; i++) {
                if (keep < 1 && !this.chance(keep)) {
                    continue;
                }
                double angle = from + (to - from) * i / count;
                this.at(particle, cx + Math.sin(angle) * radius, cy, cz + Math.cos(angle) * radius);
            }
        }

        /** Unit vectors u, v spanning the plane with normal (nx, ny, nz). */
        private static double[] basis(double nx, double ny, double nz) {
            double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
            nx /= len;
            ny /= len;
            nz /= len;
            double ux = -nz;
            double uz = nx;
            double ul = Math.sqrt(ux * ux + uz * uz);
            if (ul < 1e-4) {
                ux = 1;
                uz = 0;
                ul = 1;
            }
            ux /= ul;
            uz /= ul;
            double vx = ny * uz;
            double vy = nz * ux - nx * uz;
            double vz = -ny * ux;
            return new double[] {ux, 0, uz, vx, vy, vz};
        }

        /** Ring in any orientation: centre (cx, cy, cz), plane normal (nx, ny, nz), drawn from angle a1 to a2. */
        private void ring3(ParticleOptions particle, double cx, double cy, double cz, double radius,
                           double nx, double ny, double nz, double a1, double a2, double spacing, double keep) {
            double[] b = basis(nx, ny, nz);
            int count = Math.max(4, (int) Math.ceil(Math.abs(a2 - a1) * radius / spacing));
            for (int i = 0; i < count; i++) {
                if (keep < 1 && !this.chance(keep)) {
                    continue;
                }
                double a = a1 + (a2 - a1) * i / count;
                double c = Math.cos(a) * radius;
                double s = Math.sin(a) * radius;
                double py = cy + c * b[1] + s * b[4];
                if (py < 0.02) {
                    continue;
                }
                this.at(particle, cx + c * b[0] + s * b[3], py, cz + c * b[2] + s * b[5]);
            }
        }

        /** Point on a ring3 at angle {@code a}: {x, y, z}. */
        private static double[] ring3Point(double cx, double cy, double cz, double radius, double nx, double ny,
                                           double nz, double a) {
            double[] b = basis(nx, ny, nz);
            double c = Math.cos(a) * radius;
            double s = Math.sin(a) * radius;
            return new double[] {cx + c * b[0] + s * b[3], cy + c * b[1] + s * b[4], cz + c * b[2] + s * b[5]};
        }

        /**
         * Line in an upright panel: panel centre (cx, cz) at floor level, sideways axis (tx, tz).
         * {@code u} is sideways, {@code v} is height. Points under the floor are skipped.
         */
        private void panelLine(ParticleOptions particle, double cx, double cz, double tx, double tz,
                               double u1, double v1, double u2, double v2, double spacing, double keep) {
            double length = Math.hypot(u2 - u1, v2 - v1);
            int count = Math.max(1, (int) Math.ceil(length / spacing));
            for (int i = 0; i <= count; i++) {
                double f = (double) i / count;
                double u = Mth.lerp(f, u1, u2);
                double v = Mth.lerp(f, v1, v2);
                if (v < 0.02 || keep < 1 && !this.chance(keep)) {
                    continue;
                }
                this.at(particle, cx + tx * u, v, cz + tz * u);
            }
        }

        /** Ellipse in an upright panel around (cu, cv), half sizes {@code w} and {@code h}. */
        private void panelEllipse(ParticleOptions particle, double cx, double cz, double tx, double tz,
                                  double cu, double cv, double w, double h, double from, double to,
                                  double spacing, double keep) {
            int count = Math.max(6, (int) Math.ceil(Math.abs(to - from) * Math.max(w, h) / spacing));
            for (int i = 0; i <= count; i++) {
                double a = from + (to - from) * i / count;
                double v = cv + Math.sin(a) * h;
                if (v < 0.02 || keep < 1 && !this.chance(keep)) {
                    continue;
                }
                double u = cu + Math.cos(a) * w;
                this.at(particle, cx + tx * u, v, cz + tz * u);
            }
        }

        /** Heater shield standing on a circle at {@code angle}, facing outward, bottom tip at {@code base}. */
        private void shield(ParticleOptions edge, ParticleOptions cross, double angle, double dist, double base,
                            double scale, double keep) {
            double cx = Math.sin(angle) * dist;
            double cz = Math.cos(angle) * dist;
            double tx = Math.cos(angle);
            double tz = -Math.sin(angle);
            double[][] outline = {{-0.35, 1.0}, {0.35, 1.0}, {0.35, 0.45}, {0, 0}, {-0.35, 0.45}, {-0.35, 1.0}};
            for (int i = 0; i < outline.length - 1; i++) {
                this.panelLine(edge, cx, cz, tx, tz, outline[i][0] * scale, base + outline[i][1] * scale,
                        outline[i + 1][0] * scale, base + outline[i + 1][1] * scale, 0.08, keep);
            }
            this.panelLine(cross, cx, cz, tx, tz, 0, base + 0.15 * scale, 0, base + 0.9 * scale, 0.08, keep);
            this.panelLine(cross, cx, cz, tx, tz, -0.22 * scale, base + 0.7 * scale, 0.22 * scale,
                    base + 0.7 * scale, 0.08, keep);
        }

        /** Humanoid silhouette standing at (cx, cz), facing the player. */
        private void figure(ParticleOptions particle, double cx, double cz, double keep) {
            double a = Math.atan2(cx, cz);
            double tx = Math.cos(a);
            double tz = -Math.sin(a);
            this.panelLine(particle, cx, cz, tx, tz, -0.1, 0.02, -0.07, 0.75, 0.09, keep);
            this.panelLine(particle, cx, cz, tx, tz, 0.1, 0.02, 0.07, 0.75, 0.09, keep);
            this.panelLine(particle, cx, cz, tx, tz, 0, 0.75, 0, 1.35, 0.09, keep);
            this.panelLine(particle, cx, cz, tx, tz, -0.25, 1.3, 0.25, 1.3, 0.09, keep);
            this.panelLine(particle, cx, cz, tx, tz, -0.25, 1.3, -0.3, 0.8, 0.09, keep);
            this.panelLine(particle, cx, cz, tx, tz, 0.25, 1.3, 0.3, 0.8, 0.09, keep);
            this.panelEllipse(particle, cx, cz, tx, tz, 0, 1.55, 0.17, 0.19, 0, 2.0 * Math.PI, 0.08, keep);
        }

        /** Upright eye centred above the player, turned by {@code angle} so everyone around sees it. */
        private void eye(ParticleOptions lid, ParticleOptions pupil, double up, double width, double open,
                         double angle) {
            double tx = Math.cos(angle);
            double tz = -Math.sin(angle);
            this.panelEllipse(lid, 0, 0, tx, tz, 0, up, width, width * 0.45 * open, 0, 2.0 * Math.PI, 0.07, 1);
            if (open > 0.3) {
                this.at(pupil, 0, up, 0);
                this.at(pupil, 0, up + 0.06, 0);
                this.at(pupil, 0, up - 0.06, 0);
            }
        }

        /** Vertical sword centred on the player. Guard at {@code guardY}; {@code dir} +1 blade up, -1 down. */
        private void sword(ParticleOptions blade, ParticleOptions hilt, double guardY, int dir, double length,
                           double guardHalf, double width, double keep) {
            double tip = guardY + dir * length;
            this.localLine(blade, 0, guardY, -width, 0, tip, 0, 0.08, keep);
            this.localLine(blade, 0, guardY, width, 0, tip, 0, 0.08, keep);
            this.localLine(hilt, 0, guardY, -guardHalf, 0, guardY, guardHalf, 0.08, keep);
            this.localLine(hilt, 0, guardY, 0, 0, guardY - dir * 0.35, 0, 0.08, keep);
            this.localLine(hilt, 0, guardY - 0.06, -guardHalf, 0, guardY + 0.06, -guardHalf, 0.06, keep);
            this.localLine(hilt, 0, guardY - 0.06, guardHalf, 0, guardY + 0.06, guardHalf, 0.06, keep);
            this.at(hilt, 0, guardY - dir * 0.42, 0);
        }

        private void sphere(ParticleOptions particle, double dx, double dy, double dz, double radius, int count,
                            double keep) {
            for (int i = 0; i < count; i++) {
                if (keep < 1 && !this.chance(keep)) {
                    continue;
                }
                double[] d = sphereDirection(i, count);
                this.at(particle, dx + d[0] * radius, dy + d[1] * radius, dz + d[2] * radius);
            }
        }

        private void sphereOut(ParticleOptions particle, double dx, double dy, double dz, int count, double speed) {
            for (int i = 0; i < count; i++) {
                double[] d = sphereDirection(i, count);
                this.fly(particle, dx, dy, dz, d[0], d[1], d[2], speed);
            }
        }

        private static double[] sphereDirection(int i, int count) {
            double yy = 1.0 - 2.0 * (i + 0.5) / count;
            double r = Math.sqrt(1.0 - yy * yy);
            double angle = i * 2.399963;
            return new double[] {Math.cos(angle) * r, yy, Math.sin(angle) * r};
        }

        /** Particles flying horizontally outward from the centre. */
        private void radial(ParticleOptions particle, double dy, int count, double speed) {
            for (int i = 0; i < count; i++) {
                double angle = 2.0 * Math.PI * i / count;
                this.fly(particle, 0, dy, 0, Math.sin(angle), 0, Math.cos(angle), speed);
            }
        }

        /** Particles flying horizontally inward from a ring. */
        private void radialIn(ParticleOptions particle, double radius, double dy, int count, double speed) {
            for (int i = 0; i < count; i++) {
                double angle = 2.0 * Math.PI * i / count + this.spread(0.3);
                this.fly(particle, Math.sin(angle) * radius, dy, Math.cos(angle) * radius,
                        -Math.sin(angle), 0, -Math.cos(angle), speed);
            }
        }

        private void zigzag(ParticleOptions particle, double x1, double y1, double z1,
                            double x2, double y2, double z2, int segments, double jitter, double spacing) {
            double px = x1;
            double py = y1;
            double pz = z1;
            for (int i = 1; i <= segments; i++) {
                double f = (double) i / segments;
                double j = i == segments ? 0.0 : jitter;
                double nx = Mth.lerp(f, x1, x2) + this.spread(j);
                double ny = Mth.lerp(f, y1, y2) + (y1 == y2 ? 0 : this.spread(j * 0.5));
                double nz = Mth.lerp(f, z1, z2) + this.spread(j);
                this.line(particle, px, py, pz, nx, ny, nz, spacing, 1);
                px = nx;
                py = ny;
                pz = nz;
            }
        }

        /** Lightning bolt: bright core with a coloured glow. */
        private void bolt(int glowRgb, double x1, double y1, double z1, double x2, double y2, double z2) {
            int segments = Math.max(3, (int) Math.ceil(Math.abs(y1 - y2) / 0.6));
            this.zigzag(this.dust(0xFFFFFF, 1.0F), x1, y1, z1, x2, y2, z2, segments, 0.3, 0.07);
            this.zigzag(this.dust(glowRgb, 1.6F), x1, y1, z1, x2, y2, z2, segments, 0.3, 0.12);
        }

        private void burstBlock(Block block, double dx, double dy, double dz, int count, double speed) {
            this.cloud(new BlockParticleOption(ParticleTypes.BLOCK, block.defaultBlockState()),
                    dx, dy, dz, count, 0.2, 0.2, 0.2, speed);
        }

        private void burstItem(Item item, double dx, double dy, double dz, int count, double speed) {
            this.cloud(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(item)),
                    dx, dy, dz, count, 0.15, 0.15, 0.15, speed);
        }

        /** Chunks of the block under the player kicked up at (dx, dz). */
        private void debrisAt(double dx, double dz, int count) {
            this.cloud(new BlockParticleOption(ParticleTypes.BLOCK, this.ground()), dx, 0.1, dz, count,
                    0.08, 0.03, 0.08, 0.12);
        }

        /** Chunks of the block under the player kicked up anywhere inside {@code radius}. */
        private void groundDebris(int count, double radius) {
            for (int i = 0; i < count; i++) {
                double angle = this.rand() * 2.0 * Math.PI;
                double d = Math.sqrt(this.rand()) * radius;
                this.debrisAt(Math.sin(angle) * d, Math.cos(angle) * d, 2);
            }
        }

        /** Two wings of light behind the player; {@code spread} 0..1 unfolds them. */
        private void wings(ParticleOptions feather, ParticleOptions edge, double spread, double scale, double keep) {
            double span = 1.4 * spread * scale;
            int feathers = 16;
            for (int i = 0; i <= feathers; i++) {
                double f = (double) i / feathers;
                double up = 1.3 + Math.sin(f * Math.PI * 0.9) * 0.7 * scale;
                double back = -0.3 - 0.12 * Math.sin(f * Math.PI);
                double right = 0.15 + span * f;
                for (int side = -1; side <= 1; side += 2) {
                    if (!this.chance(keep)) {
                        continue;
                    }
                    this.local(edge, back, up, right * side);
                    this.local(feather, back - 0.04, up - 0.25 - 0.3 * f, right * side * 0.95);
                    if (f > 0.4) {
                        this.local(feather, back - 0.06, up - 0.55 - 0.5 * f, right * side * 0.9);
                    }
                }
            }
        }

        private void sound(SoundEvent sound, float volume, float pitch) {
            this.level.playSound(null, this.x, this.y, this.z, sound, SoundSource.PLAYERS,
                    volume * this.mode.volume(), pitch);
        }

        private void sound(Holder<SoundEvent> sound, float volume, float pitch) {
            this.sound(sound.value(), volume, pitch);
        }
    }
}
