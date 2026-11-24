package dev.felipe.canchas.core.service;

import dev.felipe.canchas.core.port.in.BookingDeskUseCase.BookingFilter;
import dev.felipe.canchas.core.port.in.BookingDeskUseCase.BookingRequest;
import dev.felipe.canchas.core.BusinessRuleException;
import dev.felipe.canchas.core.model.Booking;
import dev.felipe.canchas.core.model.BookingStatus;
import dev.felipe.canchas.core.model.Court;
import dev.felipe.canchas.core.model.CourtFormat;
import dev.felipe.canchas.core.model.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static dev.felipe.canchas.core.service.TestComplex.TUESDAY;
import static org.junit.jupiter.api.Assertions.*;

class BookingDeskTest {
    private TestComplex complex;
    private Court north;
    private Court south;
    private Team tigers;
    private Team lions;

    @BeforeEach
    void setUp() {
        complex = new TestComplex();
        north = complex.court("F5-A", CourtFormat.FUTBOL_5);
        south = complex.court("F5-B", CourtFormat.FUTBOL_5);
        tigers = complex.team("Los Tigres");
        lions = complex.team("Los Leones");
    }

    @Test
    void bookingFixesThePriceWithTheRatesOfThatMoment() {
        Booking booking = complex.book(north, tigers, TUESDAY, 17, 2); // 70.000 + 95.000
        assertEquals(new BigDecimal("165000"), booking.price());
        assertTrue(booking.isPending());

        complex.app.rates().update(CourtFormat.FUTBOL_5, new BigDecimal("80000"), new BigDecimal("120000"));
        assertEquals(new BigDecimal("165000"), complex.app.bookings().view(booking.id()).booking().price());
    }

    @Test
    void courtCannotBeBookedTwiceAtTheSameTime() {
        complex.book(north, tigers, TUESDAY, 19, 2);
        BusinessRuleException e = assertThrows(BusinessRuleException.class, () -> complex.book(north, lions, TUESDAY, 20, 1));
        assertTrue(e.getMessage().contains("ya está reservada"));
        assertDoesNotThrow(() -> complex.book(north, lions, TUESDAY, 21, 1), "la de las 21 empieza cuando la otra termina");
        assertDoesNotThrow(() -> complex.book(south, lions, TUESDAY, 19, 1), "otra cancha a la misma hora sí se puede");
    }

    @Test
    void teamCannotPlayTwoMatchesAtTheSameTime() {
        complex.book(north, tigers, TUESDAY, 19, 2);
        assertThrows(BusinessRuleException.class, () -> complex.book(south, tigers, TUESDAY, 20, 1));
    }

    @Test
    void cancelledBookingFreesTheCourt() {
        Booking booking = complex.book(north, tigers, TUESDAY, 19, 1);
        complex.app.bookings().cancel(booking.id());
        assertDoesNotThrow(() -> complex.book(north, lions, TUESDAY, 19, 1));
    }

    @Test
    void cannotBookThePastNorAnInactiveCourt() {
        assertThrows(BusinessRuleException.class, () -> complex.book(north, tigers, TUESDAY, 8, 1));
        complex.app.courts().deactivate(south.id());
        assertThrows(BusinessRuleException.class, () -> complex.book(south, tigers, TUESDAY, 19, 1));
    }

    @Test
    void teamWithTwoRecentNoShowsIsSanctioned() {
        Booking first = complex.book(north, lions, TUESDAY, 10, 1);
        Booking second = complex.book(north, lions, TUESDAY, 12, 1);
        complex.clock.set(TUESDAY.atTime(12, 30));
        complex.app.bookings().markNoShow(first.id());
        complex.app.bookings().markNoShow(second.id());

        BusinessRuleException e = assertThrows(BusinessRuleException.class, () -> complex.book(north, lions, TUESDAY.plusDays(1), 10, 1));
        assertTrue(e.getMessage().contains("sancionado"));
        assertTrue(complex.app.teams().record(lions.id()).sanctioned());

        complex.clock.set(TUESDAY.plusDays(31).atTime(9, 0)); // pasan los 30 días
        assertDoesNotThrow(() -> complex.book(north, lions, TUESDAY.plusDays(32), 10, 1));
    }

    @Test
    void teamCanHaveAtMostThreePendingBookings() {
        complex.book(north, tigers, TUESDAY, 10, 1);
        complex.book(north, tigers, TUESDAY, 12, 1);
        complex.book(north, tigers, TUESDAY, 14, 1);
        assertThrows(BusinessRuleException.class, () -> complex.book(north, tigers, TUESDAY, 16, 1));
    }

    @Test
    void closingFlowChargesOnlyPlayedMatches() {
        Booking played = complex.book(north, tigers, TUESDAY, 10, 1);
        Booking missed = complex.book(south, lions, TUESDAY, 10, 1);
        assertThrows(BusinessRuleException.class, () -> complex.app.bookings().markPlayed(played.id()), "todavía no empieza");

        complex.clock.set(TUESDAY.atTime(11, 0));
        complex.app.bookings().markPlayed(played.id());
        complex.app.bookings().markNoShow(missed.id());

        var tigersRecord = complex.app.teams().record(tigers.id());
        assertEquals(1, tigersRecord.played());
        assertEquals(new BigDecimal("70000"), tigersRecord.spent());
        assertEquals(1, complex.app.teams().record(lions.id()).noShows());
    }

    @Test
    void quoteDoesNotReserveAnything() {
        var quote = complex.app.bookings().quote(new BookingRequest(north.id(), null, TUESDAY, 18, 1));
        assertEquals(new BigDecimal("95000"), quote.total());
        assertTrue(complex.bookings.findOn(TUESDAY).isEmpty());
    }

    @Test
    void searchFiltersByTeamStatusAndDates() {
        complex.book(north, tigers, TUESDAY, 19, 1);
        Booking other = complex.book(north, lions, TUESDAY.plusDays(2), 19, 1);
        complex.app.bookings().cancel(other.id());

        assertEquals(2, complex.app.bookings().search(new BookingFilter(null, null, null, null)).size());
        assertEquals(1, complex.app.bookings().search(new BookingFilter(null, null, tigers.id(), null)).size());
        assertEquals(1, complex.app.bookings().search(new BookingFilter(null, null, null, BookingStatus.CANCELLED)).size());
        assertEquals(1, complex.app.bookings().search(new BookingFilter(TUESDAY, TUESDAY, null, null)).size());
        assertThrows(BusinessRuleException.class, () -> complex.app.bookings().search(new BookingFilter(TUESDAY, TUESDAY.minusDays(1), null, null)));
    }
}
