package dev.felipe.canchas.core;

import dev.felipe.canchas.core.model.Booking;
import dev.felipe.canchas.core.model.BookingStatus;
import dev.felipe.canchas.core.model.TimeSlot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BookingTest {
    private static final LocalDate DAY = LocalDate.of(2026, 11, 24);
    private static final TimeSlot EVENING = new TimeSlot(DAY, 19, 2);
    private static final LocalDateTime MORNING = DAY.atTime(9, 0);

    private Booking reserved() {
        return Booking.reserve(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), EVENING, new BigDecimal("190000"), MORNING);
    }

    @Test
    void cannotReserveASlotThatAlreadyStarted() {
        assertThrows(BusinessRuleException.class, () -> Booking.reserve(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                EVENING, BigDecimal.TEN, DAY.atTime(19, 0)));
    }

    @Test
    void startsPendingAndWithoutRevenue() {
        Booking booking = reserved();
        assertTrue(booking.isPending());
        assertEquals(BigDecimal.ZERO, booking.revenue());
    }

    @Test
    void canBeCancelledUntilTwoHoursBefore() {
        assertEquals(BookingStatus.CANCELLED, reserved().cancel(DAY.atTime(17, 0)).status());
        assertThrows(BusinessRuleException.class, () -> reserved().cancel(DAY.atTime(17, 1)));
    }

    @Test
    void isPlayedOrNoShowOnlyFromItsStartTime() {
        assertThrows(BusinessRuleException.class, () -> reserved().markPlayed(DAY.atTime(18, 59)));
        assertThrows(BusinessRuleException.class, () -> reserved().markNoShow(DAY.atTime(18, 59)));

        Booking played = reserved().markPlayed(DAY.atTime(21, 0));
        assertEquals(BookingStatus.PLAYED, played.status());
        assertEquals(new BigDecimal("190000"), played.revenue());
        assertEquals(DAY.atTime(21, 0), played.settledAt());

        Booking missed = reserved().markNoShow(DAY.atTime(19, 15));
        assertEquals(BookingStatus.NO_SHOW, missed.status());
        assertEquals(BigDecimal.ZERO, missed.revenue());
    }

    @Test
    void aClosedBookingCannotChangeAgain() {
        Booking played = reserved().markPlayed(DAY.atTime(21, 0));
        assertThrows(BusinessRuleException.class, () -> played.markNoShow(DAY.atTime(21, 5)));
        assertThrows(BusinessRuleException.class, () -> played.cancel(MORNING));
    }

    @Test
    void onlyHoldsTheCourtWhileReservedOrPlayed() {
        assertTrue(BookingStatus.RESERVED.holdsCourt());
        assertTrue(BookingStatus.PLAYED.holdsCourt());
        assertFalse(BookingStatus.CANCELLED.holdsCourt());
        assertFalse(BookingStatus.NO_SHOW.holdsCourt());
    }
}
