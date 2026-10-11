package nl.tivek.multiversepowers.character.client;

import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;

// Whose guide it is: a character's, or a tool's held with none (the Schematic Wand). `key` names its lines:
// `character.<mod>.<key>.summary` and `.about`, and `guide.<mod>.<key>.<mode>.…`.
public record GuideOwner(String key, Component name, int color, @Nullable GameCharacter character) {
    public static GuideOwner of(GameCharacter character) {
        return new GuideOwner(character.getId(), character.getDisplayName(), character.getColor(), character);
    }

    @Nullable
    public CharacterAbility byName(String ability) {
        return this.character == null ? null : this.character.byName(ability);
    }
}
