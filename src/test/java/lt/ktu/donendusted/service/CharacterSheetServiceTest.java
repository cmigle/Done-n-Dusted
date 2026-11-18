package lt.ktu.donendusted.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Korteles formules be Spring konteksto: charakteristikos reiksme ir serija. */
class CharacterSheetServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 18);

    @Test
    void scoreGrowsEveryFiftyXpAndIsCappedAtTwenty() {
        assertThat(CharacterSheetService.score(0)).isEqualTo(10);
        assertThat(CharacterSheetService.score(49)).isEqualTo(10);
        assertThat(CharacterSheetService.score(50)).isEqualTo(11);
        assertThat(CharacterSheetService.score(499)).isEqualTo(19);
        assertThat(CharacterSheetService.score(500)).isEqualTo(20);
        assertThat(CharacterSheetService.score(100_000)).isEqualTo(20);
    }

    @Test
    void streakWithoutDaysIsZero() {
        assertThat(CharacterSheetService.streakDays(List.of(), TODAY)).isZero();
    }

    @Test
    void streakCountsConsecutiveDaysEndingToday() {
        List<LocalDate> days = List.of(TODAY, TODAY.minusDays(1), TODAY.minusDays(2), TODAY.minusDays(4));
        assertThat(CharacterSheetService.streakDays(days, TODAY)).isEqualTo(3);
    }

    @Test
    void streakEndingYesterdayIsStillAlive() {
        List<LocalDate> days = List.of(TODAY.minusDays(1), TODAY.minusDays(2));
        assertThat(CharacterSheetService.streakDays(days, TODAY)).isEqualTo(2);
    }

    @Test
    void streakEndingTwoDaysAgoIsBroken() {
        List<LocalDate> days = List.of(TODAY.minusDays(2), TODAY.minusDays(3));
        assertThat(CharacterSheetService.streakDays(days, TODAY)).isZero();
    }
}
