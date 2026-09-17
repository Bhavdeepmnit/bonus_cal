package com.icici.leavemanagement.holiday;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;

class WorkingDaysTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 3, 2);

    @Test
    void mondayToFriday_isFiveDays() {
        assertThat(WorkingDays.count(MONDAY, MONDAY.plusDays(4), Set.of())).isEqualTo(5);
    }

    @Test
    void fridayToMonday_skipsTheWeekend() {
        assertThat(WorkingDays.count(MONDAY.plusDays(4), MONDAY.plusDays(7), Set.of())).isEqualTo(2);
    }

    @Test
    void weekendOnly_isZero() {
        assertThat(WorkingDays.count(MONDAY.plusDays(5), MONDAY.plusDays(6), Set.of())).isZero();
    }

    @Test
    void holidaysAreNotCounted() {
        assertThat(WorkingDays.count(MONDAY, MONDAY.plusDays(4), Set.of(MONDAY.plusDays(2)))).isEqualTo(4);
    }

    @Test
    void holidayOnAWeekend_isNotSubtractedTwice() {
        assertThat(WorkingDays.count(MONDAY, MONDAY.plusDays(6), Set.of(MONDAY.plusDays(5)))).isEqualTo(5);
    }

    @Test
    void singleDay() {
        assertThat(WorkingDays.count(MONDAY, MONDAY, Set.of())).isEqualTo(1);
    }
}
