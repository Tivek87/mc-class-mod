package nl.tivek.multiversepowers.spell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import nl.tivek.multiversepowers.MultiversePowers;

public enum MagicSchool {
    EARTH("earth", 0x8B5A2B),
    AIR("air", 0xDDEEF2),
    FIRE("fire", 0xFF5511),
    WATER("water", 0x2E86DE),
    HOLY("holy", 0xFFEAA7),
    DARK("dark", 0x6C5CE7),
    ICE("ice", 0x74B9FF),
    LIGHTNING("lightning", 0x00D2FF),
    NATURE("nature", 0x2ED573),
    BLOOD("blood", 0xD63031),
    METAL("metal", 0xB0BEC5),
    GRAVITY("gravity", 0x5E35B1),
    TIME("time", 0xF1C40F),
    ILLUSION("illusion", 0xD980FA),
    COSMIC("cosmic", 0x4834D4);

    private final String id;
    private final int color;

    MagicSchool(String id, int color) {
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
        return Component.translatable("school." + MultiversePowers.MODID + "." + this.id);
    }

    public List<Spell> getSpells() {
        List<Spell> list = new ArrayList<>();
        for (Spell spell : Spell.values()) {
            if (spell.getSchool() == this) {
                list.add(spell);
            }
        }
        return Collections.unmodifiableList(list);
    }

    @Nullable
    public static MagicSchool byId(String id) {
        for (MagicSchool school : values()) {
            if (school.id.equalsIgnoreCase(id)) {
                return school;
            }
        }
        return null;
    }
}
