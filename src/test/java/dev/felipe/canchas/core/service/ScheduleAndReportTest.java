package dev.felipe.canchas.core.service;

import dev.felipe.canchas.core.port.in.CourtCatalogUseCase.CourtCommand;
import dev.felipe.canchas.core.port.in.ScheduleBoardUseCase.AgendaCell;
import dev.felipe.canchas.core.port.in.ScheduleBoardUseCase.DayAgenda;
import dev.felipe.canchas.core.BusinessRuleException;
import dev.felipe.canchas.core.model.Booking;
import dev.felipe.canchas.core.model.Court;
import dev.felipe.canchas.core.model.CourtFormat;
import dev.felipe.canchas.core.model.Team;
import dev.felipe.canchas.core.policy.RateBand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static dev.felipe.canchas.core.service.TestComplex.TUESDAY;
import static org.junit.jupiter.api.Assertions.*;

class ScheduleAndReportTest {
    private TestComplex complex;
    private Court five;
    private Court seven;
    private Team tigers;

    @BeforeEach
    void setUp() {
        complex = new TestComplex();
        five = complex.court("F5-A", CourtFormat.FUTBOL_5);
        seven = complex.court("F7-A", CourtFormat.FUTBOL_7);
        tigers = complex.team("Los Tigres");
    }

    @Test
    void agendaSpansBookingsOverTheirHoursAndMarksPastHours() {
        complex.book(five, tigers, TUESDAY, 19, 2);
        DayAgenda agenda = complex.app.schedule().agenda(TUESDAY);

        assertEquals(2, agenda.rows().size());
        List<AgendaCell> cells = agenda.rows().get(0).cells(); // F5-A
        assertEquals(14, cells.size(), "15 horas, pero la reserva de 2 horas es una sola celda");
        AgendaCell booked = cells.stream().filter(c -> !c.free()).findFirst().orElseThrow();
        assertEquals(19, booked.hour());
        assertEquals(2, booked.span());
        assertTrue(cells.get(0).past(), "a las 9:00, la hora de las 8 ya pasó");
        assertEquals(2, agenda.bookedHours());
        assertEquals(30, agenda.openHours());
        assertEquals(6, agenda.occupancyPercent());
        assertEquals(new BigDecimal("190000"), agenda.expectedRevenue());
    }

    @Test
    void publicAvailabilityCountsFreeCourtsPerHourWithoutTeams() {
        complex.court("F5-B", CourtFormat.FUTBOL_5);
        complex.book(five, tigers, TUESDAY, 19, 1);

        var availability = complex.app.schedule().availability(TUESDAY);
        var fiveAside = availability.formats().stream().filter(f -> f.format() == CourtFormat.FUTBOL_5).findFirst().orElseThrow();
        var at19 = fiveAside.hours().stream().filter(h -> h.hour() == 19).findFirst().orElseThrow();
        assertEquals(1, at19.free());
        assertEquals(2, at19.total());
    }

    @Test
    void inactiveCourtsLeaveTheAgendaButKeepHistory() {
        complex.app.courts().deactivate(seven.id());
        assertEquals(1, complex.app.schedule().agenda(TUESDAY).rows().size());
        assertThrows(BusinessRuleException.class, () -> complex.app.courts().deactivate(seven.id()));
    }

    @Test
    void courtWithPendingBookingsCannotLeaveServiceNorChangeFormat() {
        complex.book(seven, tigers, TUESDAY, 19, 1);
        assertThrows(BusinessRuleException.class, () -> complex.app.courts().deactivate(seven.id()));
        assertThrows(BusinessRuleException.class,
                () -> complex.app.courts().update(seven.id(), new CourtCommand("F7-A", "Cancha F7-A", CourtFormat.FUTBOL_5)));
        assertThrows(BusinessRuleException.class,
                () -> complex.app.courts().open(new CourtCommand("f7-a", "Otra cancha", CourtFormat.FUTBOL_7)), "código repetido");
    }

    @Test
    void balanceCountsOnlyPlayedMatches() {
        Booking day = complex.book(five, tigers, TUESDAY, 10, 2);                         // 140.000 diurna
        Booking night = complex.book(seven, complex.team("Real Barrio"), TUESDAY, 19, 1); // 140.000 nocturna
        Booking missed = complex.book(five, complex.team("Los Pumas"), TUESDAY, 20, 1);
        complex.clock.set(TUESDAY.atTime(22, 0));
        complex.app.bookings().markPlayed(day.id());
        complex.app.bookings().markPlayed(night.id());
        complex.app.bookings().markNoShow(missed.id());

        var report = complex.app.reports().report(TUESDAY.minusDays(1), TUESDAY);
        assertEquals(new BigDecimal("280000"), report.total());
        assertEquals(2, report.played());
        assertEquals(1, report.noShows());
        assertEquals(33, report.noShowPercent());
        assertEquals(2L, report.hoursByBand().get(RateBand.DAY));
        assertEquals(1L, report.hoursByBand().get(RateBand.NIGHT));
        assertEquals(new BigDecimal("140000"), report.byFormat().get(CourtFormat.FUTBOL_5));
        assertEquals(50.0, report.percentOf(CourtFormat.FUTBOL_7));
        assertEquals(2, report.days().size());
        assertEquals(100, report.days().get(1).percentOfMax());
        assertEquals("LOS TIGRES", report.topTeams().getFirst().team());
        assertEquals(5, report.occupancyPercent(), "3 horas jugadas de 60 posibles (2 canchas × 15 horas × 2 días)");
    }

    @Test
    void balanceValidatesTheRange() {
        assertThrows(BusinessRuleException.class, () -> complex.app.reports().report(TUESDAY, TUESDAY.minusDays(1)));
        assertThrows(BusinessRuleException.class, () -> complex.app.reports().report(TUESDAY.minusYears(2), TUESDAY));
    }
}
