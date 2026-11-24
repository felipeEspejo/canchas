package dev.felipe.canchas.adapter.persistence;

import dev.felipe.canchas.core.model.Booking;
import dev.felipe.canchas.core.model.BookingStatus;
import dev.felipe.canchas.core.model.Court;
import dev.felipe.canchas.core.model.CourtFormat;
import dev.felipe.canchas.core.model.RatePlan;
import dev.felipe.canchas.core.model.StaffAccount;
import dev.felipe.canchas.core.model.StaffRole;
import dev.felipe.canchas.core.model.Team;
import dev.felipe.canchas.core.model.TimeSlot;
import dev.felipe.canchas.adapter.persistence.sqlserver.SqlBookingStore;
import dev.felipe.canchas.adapter.persistence.sqlserver.SqlCourtStore;
import dev.felipe.canchas.adapter.persistence.sqlserver.SqlRatePlanStore;
import dev.felipe.canchas.adapter.persistence.sqlserver.SqlServerSchema;
import dev.felipe.canchas.adapter.persistence.sqlserver.SqlStaffAccountStore;
import dev.felipe.canchas.adapter.persistence.sqlserver.SqlTeamStore;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contrato de los adaptadores SQL: las mismas pruebas corren en H2 imitando a SQL Server (siempre)
 * y en un SQL Server real (si se configura SQLSERVER_TEST_URL). Ver las clases hijas.
 */
abstract class StoreContractTest {
    private static final LocalDate DAY = LocalDate.of(2026, 11, 24);
    private static final LocalDateTime BOOKED_AT = DAY.minusDays(1).atTime(10, 0);

    private Jdbi jdbi;
    private SqlCourtStore courts;
    private SqlTeamStore teams;
    private SqlBookingStore bookings;
    private SqlRatePlanStore rates;
    private SqlStaffAccountStore staff;

    /** Una base sin las tablas del proyecto. */
    protected abstract Jdbi emptyDatabase();

    @BeforeEach
    void setUp() {
        jdbi = emptyDatabase();
        SqlServerSchema.createMissingTables(jdbi);
        courts = new SqlCourtStore(jdbi);
        teams = new SqlTeamStore(jdbi);
        bookings = new SqlBookingStore(jdbi);
        rates = new SqlRatePlanStore(jdbi);
        staff = new SqlStaffAccountStore(jdbi);
    }

    private Court court(String code) {
        Court court = Court.open(UUID.randomUUID(), code, "Cancha " + code, CourtFormat.FUTBOL_5);
        courts.save(court);
        return court;
    }

    private Team team(String name) {
        Team team = new Team(UUID.randomUUID(), name, "Capitán Prueba", "3001234567");
        teams.save(team);
        return team;
    }

    private Booking booking(Court court, Team team, LocalDate date, int hour, int hours) {
        Booking booking = Booking.reserve(UUID.randomUUID(), court.id(), team.id(), new TimeSlot(date, hour, hours),
                new BigDecimal("95000"), date.minusDays(1).atTime(10, 0));
        bookings.save(booking);
        return booking;
    }

    @Test
    void schemaCanBeCreatedTwiceWithoutLosingData() {
        Court court = court("F5-A");
        SqlServerSchema.createMissingTables(jdbi);
        assertEquals(court, courts.findById(court.id()).orElseThrow());
    }

    @Test
    void courtRoundTripAndUpdate() {
        Court court = court("F5-A");
        assertEquals(court, courts.findByCode("f5-a").orElseThrow());
        courts.save(court.deactivate());
        assertFalse(courts.findById(court.id()).orElseThrow().active());
        assertEquals(1, courts.findAll().size());
    }

    @Test
    void teamIsFoundByNormalizedName() {
        Team team = team("Atlético Ñañas");
        assertEquals(team, teams.findByName("  atlético   ñañas").orElseThrow());
        teams.save(team.withDetails("Atlético Ñañas", "Otra Persona", "3110000000"));
        assertEquals("Otra Persona", teams.findById(team.id()).orElseThrow().captain());
        assertEquals(1, teams.findAll().size());
    }

    @Test
    void bookingRoundTripWithNullCloseDateAndStatusChange() {
        Court court = court("F5-A");
        Team team = team("Los Tigres");
        Booking open = booking(court, team, DAY, 19, 2);

        Booking loaded = bookings.findById(open.id()).orElseThrow();
        // el precio vuelve como 95000.00 (decimal(12,2)): se compara por valor, no con equals del record
        assertEquals(open.slot(), loaded.slot());
        assertEquals(open.courtId(), loaded.courtId());
        assertEquals(open.teamId(), loaded.teamId());
        assertEquals(BookingStatus.RESERVED, loaded.status());
        assertEquals(0, open.price().compareTo(loaded.price()));
        assertNull(loaded.settledAt());

        Booking played = open.markPlayed(DAY.atTime(21, 0));
        bookings.save(played);
        Booking reloaded = bookings.findById(open.id()).orElseThrow();
        assertEquals(BookingStatus.PLAYED, reloaded.status());
        assertEquals(DAY.atTime(21, 0), reloaded.settledAt());
        assertEquals(BOOKED_AT, reloaded.bookedAt());
        assertEquals(0, new BigDecimal("95000").compareTo(reloaded.price()));
    }

    @Test
    void bookingsAreFoundByDateRangeTeamAndCourt() {
        Court a = court("F5-A");
        Court b = court("F5-B");
        Team tigers = team("Los Tigres");
        Team lions = team("Los Leones");
        booking(a, tigers, DAY, 10, 1);
        booking(b, lions, DAY, 10, 1);
        booking(a, lions, DAY.plusDays(3), 19, 1);

        assertEquals(2, bookings.findOn(DAY).size());
        assertEquals(3, bookings.findBetween(DAY, DAY.plusDays(3)).size());
        assertEquals(2, bookings.findBetween(DAY, DAY.plusDays(2)).size());
        assertEquals(2, bookings.findByTeam(lions.id()).size());
        assertEquals(2, bookings.findByCourt(a.id()).size());
    }

    @Test
    void bookingNeedsExistingCourtAndTeam() {
        Team team = team("Los Tigres");
        Booking orphan = Booking.reserve(UUID.randomUUID(), UUID.randomUUID(), team.id(), new TimeSlot(DAY, 10, 1),
                BigDecimal.TEN, BOOKED_AT);
        assertThrows(RuntimeException.class, () -> bookings.save(orphan));
    }

    @Test
    void ratePlanIsOnePerFormat() {
        rates.save(new RatePlan(CourtFormat.FUTBOL_5, new BigDecimal("70000"), new BigDecimal("95000")));
        rates.save(new RatePlan(CourtFormat.FUTBOL_5, new BigDecimal("75000"), new BigDecimal("99000")));
        assertEquals(1, rates.findAll().size());
        assertEquals(0, new BigDecimal("99000").compareTo(rates.findByFormat(CourtFormat.FUTBOL_5).orElseThrow().nightRate()));
        assertTrue(rates.findByFormat(CourtFormat.FUTBOL_7).isEmpty());
    }

    @Test
    void staffAccountsRoundTripAndDelete() {
        StaffAccount account = new StaffAccount(UUID.randomUUID(), "Recepcion", "hash", StaffRole.RECEPTION);
        staff.save(account);
        assertEquals(account, staff.findByUsername("recepcion").orElseThrow());
        staff.save(account.withRole(StaffRole.ADMIN));
        assertEquals(StaffRole.ADMIN, staff.findById(account.id()).orElseThrow().role());
        staff.delete(account.id());
        assertTrue(staff.findAll().isEmpty());
    }
}
