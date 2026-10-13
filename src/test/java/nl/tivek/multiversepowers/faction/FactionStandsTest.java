package nl.tivek.multiversepowers.faction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import nl.tivek.multiversepowers.faction.rules.Faction;
import nl.tivek.multiversepowers.faction.rules.FactionTable;
import org.junit.jupiter.api.Test;

class FactionStandsTest {
    private static Faction faction(String... lines) {
        return FactionTable.parse(List.of(lines), List.of(), List.of()).get("f");
    }

    @Test
    void aPlayerNewToAFactionIsNothingSpecialToIt() {
        assertNull(FactionStands.personal(faction("f|name=F"), null, null, null, 0));
        assertNull(FactionStands.personal(faction("f|attitude=hostile"), null, null, null, 0));
        assertNull(FactionStands.personal(faction("f|attitude=hostile"), null, null, null, 30),
                "still in the zone it started in");
    }

    @Test
    void reputationPastAThresholdTurnsIt() {
        assertEquals(Standing.FRIENDLY, FactionStands.personal(faction("f|name=F"), null, null, null, 30));
        assertEquals(Standing.HOSTILE, FactionStands.personal(faction("f|name=F"), null, null, null, -30));
        assertEquals(Standing.NEUTRAL, FactionStands.personal(faction("f|attitude=hostile"), null, null, null, 31));
        assertEquals(Standing.NEUTRAL, FactionStands.personal(faction("f|attitude=friendly"), null, null, null,
                -31));
    }

    @Test
    void theMostSpecificWins() {
        Faction faction = faction("f|attitude=hostile");
        assertEquals(Standing.FRIENDLY, FactionStands.personal(faction, "f", Standing.HOSTILE, Standing.HOSTILE,
                -500), "its own member, whatever else");
        assertEquals(Standing.NEUTRAL, FactionStands.personal(faction, "other", Standing.NEUTRAL, Standing.FRIENDLY,
                500), "a pinned standing before the player's faction and reputation");
        assertEquals(Standing.FRIENDLY, FactionStands.personal(faction, "other", null, Standing.FRIENDLY, -500),
                "how it stands to the player's faction before their reputation");
        assertEquals(Standing.FRIENDLY, FactionStands.personal(faction, "other", null, null, 500));
    }
}
