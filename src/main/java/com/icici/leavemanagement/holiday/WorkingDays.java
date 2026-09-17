package com.icici.leavemanagement.holiday;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

/** Counts leave days: Saturdays, Sundays and company holidays are not counted. */
public final class WorkingDays {

    private WorkingDays() {
    }

    public static int count(LocalDate start, LocalDate end, Set<LocalDate> holidays) {
        int days = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            boolean weekend = d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY;
            if (!weekend && !holidays.contains(d)) {
                days++;
            }
        }
        return days;
    }
}
