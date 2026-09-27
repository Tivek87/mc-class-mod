package nl.tivek.multiversepowers.spell;

import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import nl.tivek.multiversepowers.MultiversePowers;

public enum Spell {
    WIND_GUST("wind_gust", MagicSchool.AIR, 100, 0xDDEEF2, WindGustSpell::cast),
    FIREBALL("fireball", MagicSchool.FIRE, 40, 0xFF8A2A, FireballSpell::cast),
    VOID_WALK("void_walk", MagicSchool.DARK, 600, 0x9B5CFF, VoidWalkSpell::cast),
    LIGHTNING_STRIKE("lightning_strike", MagicSchool.LIGHTNING, 160, 0x48DBFB, LightningSpell::cast),
    POISON_AREA("poison_area", MagicSchool.NATURE, 240, 0x7FD46B, PoisonSpell::cast);

    @FunctionalInterface
    public interface Caster {
        boolean cast(ServerPlayer player, ServerLevel level);
    }

    private final String id;
    private final MagicSchool school;
    private final int cooldown;
    private final int color;
    private final Caster caster;

    Spell(String id, MagicSchool school, int cooldown, int color, Caster caster) {
        this.id = id;
        this.school = school;
        this.cooldown = cooldown;
        this.color = color;
        this.caster = caster;
    }

    public boolean cast(ServerPlayer player, ServerLevel level) {
        return this.caster.cast(player, level);
    }

    public MagicSchool getSchool() {
        return this.school;
    }

    public String getId() {
        return this.id;
    }

    public int getCooldown() {
        return this.cooldown;
    }

    public int getColor() {
        return this.color;
    }

    public Component getDisplayName() {
        return Component.translatable("spell." + MultiversePowers.MODID + "." + this.id);
    }

    public Component getDescription() {
        return Component.translatable("spell." + MultiversePowers.MODID + "." + this.id + ".desc");
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
