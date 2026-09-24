package nl.tivek.multiversepowers.character.client;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.GameCharacter;

/**
 * Everyone the power screen shows under its characters, from docs/CHARACTERS.md, sorted by who owns them. A
 * character that is already in the game (a {@link GameCharacter} with the same id) can be picked; everyone else is
 * shown as coming soon. A character that is in the game but missing here is added under {@link Franchise#OTHER}, so
 * nobody you can play is ever left out.
 */
public final class Roster {
    /** Who owns a character: the tabs of the power screen, in their order. */
    public enum Franchise {
        MARVEL("marvel", 0xEC1D24),
        DC("dc", 0x2F8DFF),
        DISNEY("disney", 0x9D8CFF),
        WARNER_BROS("warner_bros", 0xD9B44A),
        OTHER("other", 0xA8AEB8);

        private final String id;
        private final int color;

        Franchise(String id, int color) {
            this.id = id;
            this.color = color;
        }

        public int getColor() {
            return this.color;
        }

        public Component getDisplayName() {
            return Component.translatable("screen." + MultiversePowers.MODID + ".spell_wheel.franchise." + this.id);
        }
    }

    /**
     * One character of the roster.
     *
     * @param id        its id: the same as its {@link GameCharacter} once it is in the game
     * @param franchise who owns it
     */
    public record Entry(String id, Franchise franchise) {
        /** The character in the game, or null while it is still coming. */
        @Nullable
        public GameCharacter character() {
            return GameCharacter.byId(this.id);
        }

        public boolean available() {
            return this.character() != null;
        }

        public Component getDisplayName() {
            GameCharacter character = this.character();
            return character != null ? character.getDisplayName()
                    : Component.translatable("roster." + MultiversePowers.MODID + "." + this.id);
        }
    }

    // In the order of docs/CHARACTERS.md. Terminator (StudioCanal, Skydance) and Spawn (Image Comics) belong to none
    // of the big four.
    private static final List<Entry> ALL = List.of(
            new Entry("doc_ock", Franchise.MARVEL),
            new Entry("green_lantern", Franchise.DC),
            new Entry("mysterio", Franchise.MARVEL),
            new Entry("doctor_strange", Franchise.MARVEL),
            new Entry("doctor_doom", Franchise.MARVEL),
            new Entry("thanos", Franchise.MARVEL),
            new Entry("loki", Franchise.MARVEL),
            new Entry("rick_sanchez", Franchise.WARNER_BROS),
            new Entry("iron_man", Franchise.MARVEL),
            new Entry("flash", Franchise.DC),
            new Entry("magneto", Franchise.MARVEL),
            new Entry("galactus", Franchise.MARVEL),
            new Entry("thor", Franchise.MARVEL),
            new Entry("spider_man", Franchise.MARVEL),
            new Entry("hulk", Franchise.MARVEL),
            new Entry("venom", Franchise.MARVEL),
            new Entry("wolverine", Franchise.MARVEL),
            new Entry("silver_surfer", Franchise.MARVEL),
            new Entry("black_adam", Franchise.DC),
            new Entry("terminator", Franchise.OTHER),
            new Entry("spawn", Franchise.OTHER),
            new Entry("bill_cipher", Franchise.DISNEY),
            new Entry("darth_vader", Franchise.DISNEY),
            new Entry("doctor_manhattan", Franchise.DC),
            new Entry("sentry", Franchise.MARVEL),
            new Entry("darkseid", Franchise.DC),
            new Entry("doctor_fate", Franchise.DC));

    private static final Map<Franchise, List<Entry>> BY_FRANCHISE = new EnumMap<>(Franchise.class);

    static {
        for (Franchise franchise : Franchise.values()) {
            BY_FRANCHISE.put(franchise, new ArrayList<>());
        }
        for (Entry entry : ALL) {
            BY_FRANCHISE.get(entry.franchise()).add(entry);
        }
        for (GameCharacter character : GameCharacter.values()) {
            if (ALL.stream().noneMatch(entry -> entry.id().equals(character.getId()))) {
                BY_FRANCHISE.get(Franchise.OTHER).add(new Entry(character.getId(), Franchise.OTHER));
            }
        }
    }

    private Roster() {
    }

    /** Everyone of one franchise: the ones you can pick first, then the ones still coming, each in roster order. */
    public static List<Entry> of(Franchise franchise) {
        List<Entry> sorted = new ArrayList<>(BY_FRANCHISE.get(franchise));
        sorted.sort((a, b) -> Boolean.compare(b.available(), a.available()));
        return sorted;
    }

    /** How many characters of one franchise can be picked already. */
    public static int available(Franchise franchise) {
        return (int) BY_FRANCHISE.get(franchise).stream().filter(Entry::available).count();
    }
}
