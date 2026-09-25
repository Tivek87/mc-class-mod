package nl.tivek.multiversepowers.classes;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import nl.tivek.multiversepowers.MultiversePowers;

public enum ClassGroup {
    WARRIORS("warriors", 0xD8D8E0),
    RANGERS("rangers", 0x7FD46B),
    ROGUES("rogues", 0xE0B45A),
    MAGES("mages", 0xA88BE8),
    FAITHFUL("faithful", 0xF2E6A0),
    ALCHEMISTS("alchemists", 0x5FD0C8),
    FORSAKEN("forsaken", 0x9A9A9A);

    private final String id;
    private final int color;

    ClassGroup(String id, int color) {
        this.id = id;
        this.color = color;
    }

    public String getId() {
        return this.id;
    }

    public int getColor() {
        return this.color;
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

    public Component getPower() {
        return Component.translatable(key(".power"));
    }

    public Component getPick() {
        return Component.translatable(key(".pick"));
    }

    public List<PlayerClass> getClasses() {
        List<PlayerClass> classes = new ArrayList<>();
        for (PlayerClass playerClass : PlayerClass.values()) {
            if (playerClass.getGroup() == this) {
                classes.add(playerClass);
            }
        }
        return classes;
    }

    private String key(String suffix) {
        return "group." + MultiversePowers.MODID + "." + this.id + suffix;
    }
}
