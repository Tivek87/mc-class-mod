package nl.tivek.welcomescreen.spell;

import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import nl.tivek.welcomescreen.WelcomeScreenMod;

/**
 * Every spell a player can pick from the wheel (hold G), next to the characters
 * they can turn into.
 * Anyone can cast them, whoever they are; each only has a cooldown.
 */
public enum Spell {
    WIND_GUST("wind_gust", MagicSchool.AIR, 100, 0xDDEEF2),
    FIREBALL("fireball", MagicSchool.FIRE, 40, 0xFF8A2A),
    VOID_WALK("void_walk", MagicSchool.DARK, 600, 0x9B5CFF),
    LIGHTNING_STRIKE("lightning_strike", MagicSchool.LIGHTNING, 160, 0x48DBFB),
    POISON_AREA("poison_area", MagicSchool.NATURE, 240, 0x7FD46B);

    private final String id;
    private final MagicSchool school;
    private final int cooldown;
    private final int color;

    Spell(String id, MagicSchool school, int cooldown, int color) {
        this.id = id;
        this.school = school;
        this.cooldown = cooldown;
        this.color = color;
    }

    public MagicSchool getSchool() {
        return this.school;
    }

    public String getId() {
        return this.id;
    }

    /** Ticks before the spell can be cast again (20 ticks = 1 second). */
    public int getCooldown() {
        return this.cooldown;
    }

    public int getColor() {
        return this.color;
    }

    public Component getDisplayName() {
        return Component.translatable("spell." + WelcomeScreenMod.MODID + "." + this.id);
    }

    public Component getDescription() {
        return Component.translatable("spell." + WelcomeScreenMod.MODID + "." + this.id + ".desc");
    }

    @Nullable
    public static Spell byId(String id) {
        for (Spell spell : values()) {
            if (spell.id.equals(id)) {
                return spell;
            }
        }
        return null;
    }
}
