package dev.felipe.canchas.core;

import dev.felipe.canchas.core.model.TimeSlot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TimeSlotTest {
    private static final LocalDate DAY = LocalDate.of(2026, 11, 24);

    @ParameterizedTest(name = "{0}:00 por {1} h no es válida")
    @CsvSource({"7, 1", "22, 2", "20, 4", "10, 0", "23, 1"})
    void rejectsSlotsOutsideOpeningHoursOrTooLong(int start, int hours) {
        assertThrows(BusinessRuleException.class, () -> new TimeSlot(DAY, start, hours));
    }

    @Test
    void acceptsFirstAndLastPossibleSlots() {
        assertDoesNotThrow(() -> new TimeSlot(DAY, 8, 3));
        assertDoesNotThrow(() -> new TimeSlot(DAY, 22, 1));
    }

    @Test
    void overlapsOnlyWhenSharingAnHourTheSameDay() {
        TimeSlot evening = new TimeSlot(DAY, 19, 2); // 19-21
        assertTrue(evening.overlaps(new TimeSlot(DAY, 20, 1)));
        assertTrue(evening.overlaps(new TimeSlot(DAY, 18, 2)));
        assertFalse(evening.overlaps(new TimeSlot(DAY, 21, 1)), "termina a las 21: la de las 21 no choca");
        assertFalse(evening.overlaps(new TimeSlot(DAY, 17, 2)), "17-19 termina justo cuando empieza");
        assertFalse(evening.overlaps(new TimeSlot(DAY.plusDays(1), 19, 2)));
    }

    @Test
    void knowsItsHoursAndLabel() {
        TimeSlot slot = new TimeSlot(DAY, 19, 2);
        assertEquals(List.of(19, 20), slot.playedHours());
        assertEquals("19:00 – 21:00", slot.label());
        assertEquals(DAY.atTime(21, 0), slot.end());
        assertEquals(15, TimeSlot.openingHours().size());
    }
}
