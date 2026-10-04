package nl.tivek.multiversepowers.update.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class StageTest {
    @Test
    void theEndOfAVersionsNameSaysWhatItIs() {
        assertEquals(Stage.ALPHA, Stage.of("0.7.5-alpha"));
        assertEquals(Stage.ALPHA, Stage.of("1.0.0-ALPHA.2"));
        assertEquals(Stage.BETA, Stage.of("1.0.0-beta"));
        assertEquals(Stage.BETA, Stage.of("1.0.0-beta1"));
        assertEquals(Stage.RELEASE, Stage.of("1.0.0-release"));
        assertEquals(Stage.RELEASE, Stage.of("1.0.0"));
        assertNull(Stage.of("1.0.0-rc1"));
    }
}
