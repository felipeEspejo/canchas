package dev.felipe.canchas.core.service;

import dev.felipe.canchas.core.port.in.BookingDeskUseCase.BookingRequest;
import dev.felipe.canchas.core.port.in.CourtCatalogUseCase.CourtCommand;
import dev.felipe.canchas.core.port.in.TeamRegistryUseCase.TeamCommand;
import dev.felipe.canchas.core.port.in.UseCases;
import dev.felipe.canchas.core.port.out.PasswordHashing;
import dev.felipe.canchas.core.service.BookingDesk;
import dev.felipe.canchas.core.service.CourtCatalog;
import dev.felipe.canchas.core.service.RateBook;
import dev.felipe.canchas.core.service.RevenueAnalyst;
import dev.felipe.canchas.core.service.ScheduleBoard;
import dev.felipe.canchas.core.service.StaffDirectory;
import dev.felipe.canchas.core.service.TeamRegistry;
import dev.felipe.canchas.core.model.Booking;
import dev.felipe.canchas.core.model.Court;
import dev.felipe.canchas.core.model.CourtFormat;
import dev.felipe.canchas.core.model.Team;
import dev.felipe.canchas.core.policy.AttendancePolicy;
import dev.felipe.canchas.core.policy.PricingPolicy;
import dev.felipe.canchas.adapter.persistence.memory.MemoryStores;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Complejo de prueba en memoria con un reloj que se mueve a mano.
 * Martes 24/11/2026 a las 9:00. Fútbol 5: 70.000 diurna / 95.000 nocturna. Fútbol 7: 110.000 / 140.000.
 */
class TestComplex {
    static final LocalDate TUESDAY = LocalDate.of(2026, 11, 24);

    final ManualClock clock = new ManualClock(TUESDAY.atTime(9, 0));
    final MemoryStores.Courts courts = new MemoryStores.Courts();
    final MemoryStores.Teams teams = new MemoryStores.Teams();
    final MemoryStores.Bookings bookings = new MemoryStores.Bookings();
    final MemoryStores.RatePlans rates = new MemoryStores.RatePlans();
    final MemoryStores.StaffAccounts staff = new MemoryStores.StaffAccounts();
    final UseCases app;

    TestComplex() {
        AttendancePolicy attendance = new AttendancePolicy();
        app = new UseCases(
                new CourtCatalog(courts, bookings),
                new TeamRegistry(teams, bookings, courts, attendance, clock),
                new BookingDesk(courts, teams, bookings, rates, new PricingPolicy(), attendance, clock),
                new ScheduleBoard(courts, teams, bookings, clock),
                new RateBook(rates),
                new RevenueAnalyst(bookings, courts, teams),
                new StaffDirectory(staff, plainHashing()));
        app.rates().update(CourtFormat.FUTBOL_5, new BigDecimal("70000"), new BigDecimal("95000"));
        app.rates().update(CourtFormat.FUTBOL_7, new BigDecimal("110000"), new BigDecimal("140000"));
    }

    Court court(String code, CourtFormat format) {
        return app.courts().open(new CourtCommand(code, "Cancha " + code, format));
    }

    Team team(String name) {
        return app.teams().register(new TeamCommand(name, "Capitán Prueba", "3001234567"));
    }

    Booking book(Court court, Team team, LocalDate date, int hour, int hours) {
        return app.bookings().book(new BookingRequest(court.id(), team.id(), date, hour, hours));
    }

    /** Hashing de mentira para pruebas: "x" se guarda como "#x". */
    static PasswordHashing plainHashing() {
        return new PasswordHashing() {
            public String hash(String raw) {
                return "#" + raw;
            }

            public boolean matches(String raw, String hash) {
                return hash.equals("#" + raw);
            }
        };
    }

    static final class ManualClock extends Clock {
        private Instant now;

        ManualClock(LocalDateTime start) {
            set(start);
        }

        void set(LocalDateTime moment) {
            now = moment.toInstant(ZoneOffset.UTC);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
