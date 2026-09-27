package nl.tivek.multiversepowers.engine.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TimelineTest {
    private static int run(Timeline timeline, int most) {
        int age = 0;
        while (age < most && timeline.tick(null, age)) {
            age++;
        }
        return age;
    }

    @Test
    void momentsRunOnTheirTicksAndItEndsAfterTheLast() {
        List<String> done = new ArrayList<>();
        Timeline timeline = new Timeline().at(3, (level, age) -> done.add("at " + age))
                .during(5, 7, (level, age) -> done.add("during " + age));
        int ended = run(timeline, 100);
        assertEquals(List.of("at 3", "during 5", "during 6", "during 7"), done);
        assertEquals(7, ended, "it stops on its last tick");
    }

    @Test
    void aWaitRunsItsOwnMomentsFromWhenItHappened() {
        List<String> done = new ArrayList<>();
        int[] landsAt = { 9 };
        Timeline landed = new Timeline().at(0, (level, age) -> done.add("land")).at(2, (level, age) -> done.add(
                "dust"));
        Timeline timeline = new Timeline().when((level, age) -> age >= landsAt[0], 40, landed);
        int ended = run(timeline, 100);
        assertEquals(List.of("land", "dust"), done);
        assertEquals(11, ended, "it lasts until the landing's last moment");
    }

    @Test
    void aWaitThatNeverComesGivesUpAndAStopEndsAtOnce() {
        Timeline never = new Timeline().when((level, age) -> false, 20, new Timeline().at(0, (level, age) -> {
        }));
        assertEquals(20, run(never, 100));
        Timeline stopped = new Timeline().during(0, 50, (level, age) -> {
        });
        assertTrue(stopped.tick(null, 0));
        stopped.stop();
        assertFalse(stopped.tick(null, 1));
    }
}
