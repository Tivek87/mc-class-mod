package nl.tivek.multiversepowers.classes.ceremony;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.classes.ClassData;
import nl.tivek.multiversepowers.classes.ClassGroup;
import nl.tivek.multiversepowers.classes.PlayerClass;
import static nl.tivek.multiversepowers.classes.ceremony.DeathAndLevelUp.death;
import static nl.tivek.multiversepowers.classes.ceremony.DeathAndLevelUp.levelUp;

@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class Ceremonies {
    static final Mode GRAND = new Mode(200, true, 0.6F);
    private static final Mode LIGHT = new Mode(50, false, 0.5F);
    private static final Mode DEATH = new Mode(20, false, 0.6F);
    private static final Mode LEVEL_UP = new Mode(50, false, 0.5F);
    private static final int TITLE_TICK = 150;

private static final int TITLE_FADE_IN = 8;
    private static final int TITLE_STAY = 35;
    private static final int TITLE_FADE_OUT = 15;
    private static final int SUBTITLE_COLOR = 0xA8A090;

private static final List<Ceremony> ACTIVE = new ArrayList<>();

    private Ceremonies() {
    }

    record Mode(int duration, boolean grand, float volume) {
    }

    static final class Ceremony {
        private final UUID playerId;
        // Null for the level-up animation, which is the same for everyone.
        private final PlayerClass playerClass;
        private final ClassGroup group;
        final Mode mode;
        private final ResourceKey<Level> dimension;
        final double x;
        final double y;
        final double z;
        final float yaw;
        final BlockState ground;
        int age;

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
            // The block under the player, read once while he stands there: later its chunk may no longer be loaded.
            this.ground = player.level().getBlockState(BlockPos.containing(this.x, this.y - 0.5, this.z));
        }
    }

    @FunctionalInterface
    interface Animation {
        void tick(Fx fx);
    }

    public static void play(ServerPlayer player, PlayerClass playerClass, boolean firstTime) {
        // Not stacked when a kill-and-respawn loop respawns him again before it is over.
        if (!firstTime && isPlaying(player, LIGHT)) {
            return;
        }
        ACTIVE.add(new Ceremony(player, playerClass, firstTime ? GRAND : LIGHT));
    }

    public static void playDeath(ServerPlayer player, ClassGroup group) {
        if (!isPlaying(player, DEATH)) {
            ACTIVE.add(new Ceremony(player, null, group, DEATH));
        }
    }

    public static void playLevelUp(ServerPlayer player) {
        if (!isPlaying(player, LEVEL_UP)) {
            ACTIVE.add(new Ceremony(player, null, null, LEVEL_UP));
        }
    }

    private static boolean isPlaying(ServerPlayer player, Mode mode) {
        for (Ceremony ceremony : ACTIVE) {
            if (ceremony.mode == mode && ceremony.playerId.equals(player.getUUID())) {
                return true;
            }
        }
        return false;
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

    public static void clear() {
        ACTIVE.clear();
    }

    private static void showTitle(ServerPlayer player, PlayerClass playerClass) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(TITLE_FADE_IN, TITLE_STAY, TITLE_FADE_OUT));
        Component subtitle = playerClass.getGroup().getDisplayName().copy().withColor(SUBTITLE_COLOR);
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        Component title = playerClass.getDisplayName().copy().withColor(playerClass.getColor());
        player.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    static Animation animation(PlayerClass playerClass, boolean grand) {
        return switch (playerClass) {
            case KNIGHT -> grand ? WarriorCeremonies::knightGrand : WarriorCeremonies::knightLight;
            case BERSERKER -> grand ? WarriorCeremonies::berserkerGrand : WarriorCeremonies::berserkerLight;
            case HALBERDIER -> grand ? WarriorCeremonies::halberdierGrand : WarriorCeremonies::halberdierLight;
            case DUELIST -> grand ? WarriorCeremonies::duelistGrand : WarriorCeremonies::duelistLight;
            case ARCHER -> grand ? RangerCeremonies::archerGrand : RangerCeremonies::archerLight;
            case BEASTMASTER -> grand ? RangerCeremonies::beastmasterGrand : RangerCeremonies::beastmasterLight;
            case TRAPPER -> grand ? RangerCeremonies::trapperGrand : RangerCeremonies::trapperLight;
            case SCOUT -> grand ? RangerCeremonies::scoutGrand : RangerCeremonies::scoutLight;
            case ASSASSIN -> grand ? RogueCeremonies::assassinGrand : RogueCeremonies::assassinLight;
            case THIEF -> grand ? RogueCeremonies::thiefGrand : RogueCeremonies::thiefLight;
            case HIGHWAYMAN -> grand ? RogueCeremonies::highwaymanGrand : RogueCeremonies::highwaymanLight;
            case INFILTRATOR -> grand ? RogueCeremonies::infiltratorGrand : RogueCeremonies::infiltratorLight;
            case WIZARD -> grand ? MageCeremonies::wizardGrand : MageCeremonies::wizardLight;
            case SORCERER -> grand ? MageCeremonies::sorcererGrand : MageCeremonies::sorcererLight;
            case WARLOCK -> grand ? MageCeremonies::warlockGrand : MageCeremonies::warlockLight;
            case NECROMANCER -> grand ? MageCeremonies::necromancerGrand : MageCeremonies::necromancerLight;
            case CLERIC -> grand ? FaithfulCeremonies::clericGrand : FaithfulCeremonies::clericLight;
            case PALADIN -> grand ? FaithfulCeremonies::paladinGrand : FaithfulCeremonies::paladinLight;
            case INQUISITOR -> grand ? FaithfulCeremonies::inquisitorGrand : FaithfulCeremonies::inquisitorLight;
            case MONK -> grand ? FaithfulCeremonies::monkGrand : FaithfulCeremonies::monkLight;
            case APOTHECARY -> grand ? AlchemistCeremonies::apothecaryGrand : AlchemistCeremonies::apothecaryLight;
            case PLAGUE_DOCTOR -> grand ? AlchemistCeremonies::plagueDoctorGrand
                    : AlchemistCeremonies::plagueDoctorLight;
            case BOMBARDIER -> grand ? AlchemistCeremonies::bombardierGrand : AlchemistCeremonies::bombardierLight;
            case TRANSMUTER -> grand ? AlchemistCeremonies::transmuterGrand : AlchemistCeremonies::transmuterLight;
            case FORSAKEN -> grand ? ForsakenCeremony::forsakenGrand : ForsakenCeremony::forsakenLight;
        };
    }
}
