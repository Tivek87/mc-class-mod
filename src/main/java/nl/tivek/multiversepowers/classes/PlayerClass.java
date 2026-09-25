package nl.tivek.multiversepowers.classes;

import net.minecraft.network.chat.Component;
import nl.tivek.multiversepowers.MultiversePowers;

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

    public int getColor() {
        return this.group.getColor();
    }

    public Component getDisplayName() {
        return Component.translatable(key(""));
    }

    public Component getTagline() {
        return Component.translatable(key(".tagline"));
    }

    public Component getLore() {
        return Component.translatable(key(".lore"));
    }

    public Component getSpecial() {
        return Component.translatable(key(".special"));
    }

    public Component getStrong() {
        return Component.translatable(key(".strong"));
    }

    public Component getWeak() {
        return Component.translatable(key(".weak"));
    }

    public Component getPick() {
        return Component.translatable(key(".pick"));
    }

    private String key(String suffix) {
        return "class." + MultiversePowers.MODID + "." + this.id + suffix;
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
