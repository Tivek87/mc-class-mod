package nl.tivek.welcomescreen.classes;

import net.minecraft.network.chat.Component;
import nl.tivek.welcomescreen.WelcomeScreenMod;

/**
 * Every class a player can pick when first entering a world: four per group, plus The Forsaken.
 */
public enum PlayerClass {
    KNIGHT("knight", ClassGroup.WARRIORS),
    BERSERKER("berserker", ClassGroup.WARRIORS),
    HALBERDIER("halberdier", ClassGroup.WARRIORS),
    DUELIST("duelist", ClassGroup.WARRIORS),

    ARCHER("archer", ClassGroup.RANGERS),
    BEASTMASTER("beastmaster", ClassGroup.RANGERS),
    TRAPPER("trapper", ClassGroup.RANGERS),
    SCOUT("scout", ClassGroup.RANGERS),

    ASSASSIN("assassin", ClassGroup.ROGUES),
    THIEF("thief", ClassGroup.ROGUES),
    HIGHWAYMAN("highwayman", ClassGroup.ROGUES),
    INFILTRATOR("infiltrator", ClassGroup.ROGUES),

    WIZARD("wizard", ClassGroup.MAGES),
    SORCERER("sorcerer", ClassGroup.MAGES),
    WARLOCK("warlock", ClassGroup.MAGES),
    NECROMANCER("necromancer", ClassGroup.MAGES),

    CLERIC("cleric", ClassGroup.FAITHFUL),
    PALADIN("paladin", ClassGroup.FAITHFUL),
    INQUISITOR("inquisitor", ClassGroup.FAITHFUL),
    MONK("monk", ClassGroup.FAITHFUL),

    APOTHECARY("apothecary", ClassGroup.ALCHEMISTS),
    PLAGUE_DOCTOR("plague_doctor", ClassGroup.ALCHEMISTS),
    BOMBARDIER("bombardier", ClassGroup.ALCHEMISTS),
    TRANSMUTER("transmuter", ClassGroup.ALCHEMISTS),

    FORSAKEN("forsaken", ClassGroup.FORSAKEN);

    private final String id;
    private final ClassGroup group;

    PlayerClass(String id, ClassGroup group) {
        this.id = id;
        this.group = group;
    }

    public String getId() {
        return this.id;
    }

    public ClassGroup getGroup() {
        return this.group;
    }

    /** Theme colour used for the card border and the class name. */
    public int getColor() {
        return this.group.getColor();
    }

    public Component getDisplayName() {
        return Component.translatable(key(""));
    }

    /** Short role, shown on the card, e.g. "Tank · melee". */
    public Component getTagline() {
        return Component.translatable(key(".tagline"));
    }

    /** One line of story: who this class is. */
    public Component getLore() {
        return Component.translatable(key(".lore"));
    }

    /** What its power actually does. */
    public Component getSpecial() {
        return Component.translatable(key(".special"));
    }

    public Component getStrong() {
        return Component.translatable(key(".strong"));
    }

    public Component getWeak() {
        return Component.translatable(key(".weak"));
    }

    /** Why a player would pick this class over another one. */
    public Component getPick() {
        return Component.translatable(key(".pick"));
    }

    private String key(String suffix) {
        return "class." + WelcomeScreenMod.MODID + "." + this.id + suffix;
    }

    public static PlayerClass byId(String id) {
        for (PlayerClass playerClass : values()) {
            if (playerClass.id.equals(id)) {
                return playerClass;
            }
        }
        return null;
    }
}
