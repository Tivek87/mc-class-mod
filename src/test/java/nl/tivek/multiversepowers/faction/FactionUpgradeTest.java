package nl.tivek.multiversepowers.faction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import nl.tivek.multiversepowers.faction.rules.Faction;
import nl.tivek.multiversepowers.faction.rules.FactionDefaults;
import org.junit.jupiter.api.Test;

class FactionUpgradeTest {
    @Test
    void anEarlierNameBecomesTodays() {
        List<String> lines = new ArrayList<>(List.of("legion|name=The Hollow Legion",
                "blight|lore=It began in a mine. The Blight does not march.", "legion|color=8bd45a"));
        FactionUpgrade.retell(lines);
        assertEquals("legion|name=The Undead Legion", lines.get(0));
        assertTrue(lines.get(1).contains("The Infected do not march"), lines.get(1));
        assertEquals("legion|color=8bd45a", lines.get(2));
    }

    @Test
    void wordsAPlayerWroteStay() {
        List<String> lines = new ArrayList<>(List.of("legion|name=The Bone Army",
                "blight|lore=A story of my own.", "mine|name=The Hollow Legion"));
        List<String> before = List.copyOf(lines);
        FactionUpgrade.retell(lines);
        assertEquals(before, lines);
    }

    @Test
    void todaysLinesAreLeftAsTheyAre() {
        List<String> lines = new ArrayList<>(FactionDefaults.factions());
        FactionUpgrade.retell(lines);
        assertEquals(FactionDefaults.factions(), lines);
    }

    @Test
    void aMemberStartsClearOfBeingCastOut() {
        Faction hostile = new Faction("a", "A", "", "", 0, Standing.HOSTILE, true, Faction.HOSTILE_AT,
                Faction.FRIENDLY_AT, true, true);
        assertEquals(-60, hostile.reputation(0));
        assertEquals(0, hostile.reputation(hostile.member()));
        assertEquals(Standing.NEUTRAL, hostile.zone(hostile.member()));
        Faction neutral = new Faction("b", "B", "", "", 0, Standing.NEUTRAL, true, -50, 10, true, true);
        assertEquals(-20, neutral.reputation(neutral.member()));
        Faction friendly = new Faction("c", "C", "", "", 0, Standing.FRIENDLY, true, Faction.HOSTILE_AT,
                Faction.FRIENDLY_AT, true, true);
        assertTrue(friendly.member() < 0);
    }
}
