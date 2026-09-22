package nl.tivek.welcomescreen.classes;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import nl.tivek.welcomescreen.WelcomeScreenMod;

/**
 * The first step of choosing: where a class gets its power from. The Forsaken stands outside
 * the six real groups, but is picked the same way so every class is reachable.
 */
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

    /** Theme colour shared by the group and all of its classes. */
    public int getColor() {
        return this.color;
    }

    public Component getDisplayName() {
        return Component.translatable(key(""));
    }

    public Component getTagline() {
        return Component.translatable(key(".tagline"));
    }

    /** One line of story: what kind of people this group is. */
    public Component getLore() {
        return Component.translatable(key(".lore"));
    }

    /** Where the group's power comes from and how it plays. */
    public Component getPower() {
        return Component.translatable(key(".power"));
    }

    /** Why a player would pick this group over another one. */
    public Component getPick() {
        return Component.translatable(key(".pick"));
    }

    /** The classes of this group, in the order the docs list them. */
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
        return "group." + WelcomeScreenMod.MODID + "." + this.id + suffix;
    }
}
