package nl.tivek.multiversepowers.faction.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import nl.tivek.multiversepowers.faction.Standing;
import org.junit.jupiter.api.Test;

class FactionTableTest {
    private static FactionTable table(List<String> factions, List<String> members, List<String> relations) {
        return FactionTable.parse(factions, members, relations);
    }

    @Test
    void aFactionReadsItsWordsAndFillsInTheRest() {
        FactionTable table = table(List.of("bandits|name=Bandits", "bandits|attitude=hostile",
                "bandits|color=ff0000", "bandits|hostileAt=-50", "bandits|open=true", "plain|lore=Old"), List.of(),
                List.of());
        Faction bandits = table.get("bandits");
        assertNotNull(bandits);
        assertEquals("Bandits", bandits.name());
        assertEquals(Standing.HOSTILE, bandits.attitude());
        assertEquals(0xFF0000, bandits.color());
        assertEquals(-50, bandits.hostileAt());
        assertEquals(Faction.FRIENDLY_AT, bandits.friendlyAt());
        assertTrue(bandits.open());
        assertTrue(bandits.help());
        assertTrue(bandits.reputation());
        Faction plain = table.get("plain");
        assertEquals("plain", plain.name(), "a faction with no name goes by its id");
        assertNull(plain.attitude(), "no attitude leaves each creature as the game has it");
        assertFalse(plain.open());
        assertEquals("Old", plain.lore());
        assertNull(table.get("nobody"));
        assertEquals(2, table.all().size());
    }

    @Test
    void thresholdsStayOnTheirOwnSideOfZero() {
        Faction odd = table(List.of("odd|hostileAt=40", "odd|friendlyAt=-200"), List.of(), List.of()).get("odd");
        assertEquals(-1, odd.hostileAt());
        assertEquals(1, odd.friendlyAt());
        Faction wild = table(List.of("wild|hostileAt=-900", "wild|friendlyAt=x"), List.of(), List.of()).get("wild");
        assertEquals(Faction.LOWEST, wild.hostileAt());
        assertEquals(Faction.FRIENDLY_AT, wild.friendlyAt());
    }

    @Test
    void onlyLinesOfTheRightShapeAreTaken() {
        assertTrue(FactionTable.validFaction("bandits|name=The Bandits"));
        assertFalse(FactionTable.validFaction("bandits|power=9"));
        assertFalse(FactionTable.validFaction("a faction|name=x"));
        assertFalse(FactionTable.validFaction(7));
        assertTrue(FactionTable.validMember("minecraft:zombie=undead"));
        assertTrue(FactionTable.validMember("minecraft:zombie="), "nothing after the = is no faction");
        assertFalse(FactionTable.validMember("zombie=undead"));
        assertTrue(FactionTable.validRelation("illagers>villagers=hostile"));
        assertFalse(FactionTable.validRelation("illagers>villagers=angry"));
        assertFalse(FactionTable.validRelation("illagers=hostile"));
    }

    @Test
    void membersAndRelationsAreFound() {
        FactionTable table = table(List.of("undead|name=Undead", "pack|name=Pack"),
                List.of("minecraft:zombie=undead", "minecraft:skeleton=undead", "minecraft:bat="),
                List.of("undead>pack=hostile", "pack>pack=friendly"));
        assertEquals("undead", table.member("minecraft:zombie"));
        assertEquals("", table.member("minecraft:bat"));
        assertNull(table.member("minecraft:cow"));
        assertEquals(List.of("minecraft:skeleton", "minecraft:zombie"), table.kinds("undead"));
        assertEquals(Standing.HOSTILE, table.relation("undead", "pack"));
        assertNull(table.relation("pack", "undead"), "a relation goes one way");
        assertEquals(Standing.FRIENDLY, table.relation("pack", "pack"));
        assertTrue(table.hunts("undead"));
        assertFalse(table.hunts("pack"));
    }

    @Test
    void editsKeepEveryOtherLine() {
        List<String> lines = List.of("a|name=A", "a|open=true", "b|name=B");
        assertEquals(List.of("a|name=Alpha", "a|open=true", "b|name=B"),
                FactionTable.withWord(lines, "a", "name", "Alpha"));
        assertEquals(List.of("a|name=A", "a|open=true", "a|lore=Old", "b|name=B"),
                FactionTable.withWord(lines, "a", "lore", "Old"), "a new word goes beside the faction's others");
        assertEquals(List.of("a|name=A", "a|open=true", "b|name=B", "c|name=C"),
                FactionTable.withWord(lines, "c", "name", "C"));
        assertEquals(List.of("b|name=B"), FactionTable.withoutFaction(lines, "a"));

        List<String> members = List.of("minecraft:zombie=a", "minecraft:cow=b");
        assertEquals(List.of("minecraft:cow=b", "minecraft:zombie=b"),
                FactionTable.withMember(members, "minecraft:zombie", "b"));
        assertEquals(List.of("minecraft:cow=b", "minecraft:zombie="),
                FactionTable.withMember(members, "minecraft:zombie", ""));
        assertEquals(List.of("minecraft:cow=b"), FactionTable.withMember(members, "minecraft:zombie", null));
        assertEquals(List.of("minecraft:zombie=a"), FactionTable.withoutMembers(members, "b"));

        List<String> relations = List.of("a>b=hostile", "b>a=friendly", "b>b=friendly", "c>d=neutral");
        assertEquals(List.of("b>a=friendly", "b>b=friendly", "c>d=neutral", "a>b=neutral"),
                FactionTable.withRelation(relations, "a", "b", Standing.NEUTRAL));
        assertEquals(List.of("b>a=friendly", "b>b=friendly", "c>d=neutral"),
                FactionTable.withRelation(relations, "a", "b", null));
        assertEquals(List.of("c>d=neutral"), FactionTable.withoutRelations(relations, "b"));
    }

    @Test
    void textIsKeptToOneShortLine() {
        assertEquals("Two words", FactionTable.clean("  Two\n\twords  ", 40));
        assertEquals("abcde", FactionTable.clean("abcdefgh", 5));
        assertEquals("game", FactionTable.name(null));
        assertEquals(Standing.NEUTRAL, FactionTable.standing("neutral"));
        assertNull(FactionTable.standing("game"));
    }

    @Test
    void theFactionsAWorldStartsWithAreSound() {
        for (String line : FactionDefaults.factions()) {
            assertTrue(FactionTable.validFaction(line), line);
        }
        for (String line : FactionDefaults.members()) {
            assertTrue(FactionTable.validMember(line), line);
        }
        for (String line : FactionDefaults.relations()) {
            assertTrue(FactionTable.validRelation(line), line);
        }
        FactionTable table = table(FactionDefaults.factions(), FactionDefaults.members(), FactionDefaults.relations());
        assertEquals(FactionDefaults.ids().size(), table.all().size());
        for (String id : FactionDefaults.ids()) {
            Faction faction = table.get(id);
            assertNotNull(faction, id);
            assertNull(faction.attitude(), id + " leaves its creatures as the game has them");
            assertNull(faction.zone(0), id + " changes nothing for a player new to it");
        }
        for (String line : FactionDefaults.members()) {
            String faction = line.substring(line.indexOf('=') + 1);
            assertTrue(faction.isEmpty() || table.get(faction) != null, line);
        }
        assertEquals("undead", table.member("minecraft:zombie"));
        assertEquals(Standing.HOSTILE, table.relation("illagers", "villagers"));
    }

    @Test
    void reputationPutsAPlayerInAZone() {
        Faction game = table(List.of("g|name=G"), List.of(), List.of()).get("g");
        assertEquals(0, game.start());
        assertNull(game.zone(0));
        assertNull(game.zone(29));
        assertEquals(Standing.FRIENDLY, game.zone(30));
        assertEquals(Standing.HOSTILE, game.zone(-30));
        assertNull(game.zone(-29));

        Faction neutral = table(List.of("n|attitude=neutral"), List.of(), List.of()).get("n");
        assertEquals(Standing.NEUTRAL, neutral.zone(0));
        assertEquals(Standing.HOSTILE, neutral.zone(-40));

        Faction hostile = table(List.of("h|attitude=hostile"), List.of(), List.of()).get("h");
        assertEquals(-60, hostile.start());
        assertEquals(Standing.HOSTILE, hostile.zone(0));
        assertEquals(Standing.HOSTILE, hostile.zone(30), "at its threshold it is still hostile");
        assertEquals(Standing.NEUTRAL, hostile.zone(31));
        assertEquals(Standing.FRIENDLY, hostile.zone(90));
        assertEquals(160, hostile.clamp(500));
        assertEquals(-40, hostile.clamp(-500));
        assertEquals(100, hostile.reputation(500));

        Faction friendly = table(List.of("f|attitude=friendly", "f|reputation=false"), List.of(), List.of())
                .get("f");
        assertEquals(60, friendly.start());
        assertEquals(Standing.FRIENDLY, friendly.zone(-500), "deeds change nothing where reputation is off");
    }
}
