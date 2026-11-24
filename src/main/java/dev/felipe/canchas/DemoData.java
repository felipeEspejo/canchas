package dev.felipe.canchas;

import dev.felipe.canchas.core.port.in.BookingDeskUseCase.BookingRequest;
import dev.felipe.canchas.core.port.in.CourtCatalogUseCase.CourtCommand;
import dev.felipe.canchas.core.port.in.StaffDirectoryUseCase.NewStaff;
import dev.felipe.canchas.core.port.in.TeamRegistryUseCase.TeamCommand;
import dev.felipe.canchas.core.port.in.UseCases;
import dev.felipe.canchas.core.port.out.BookingStore;
import dev.felipe.canchas.core.port.out.CourtStore;
import dev.felipe.canchas.core.port.out.PasswordHashing;
import dev.felipe.canchas.core.port.out.RatePlanStore;
import dev.felipe.canchas.core.port.out.StaffAccountStore;
import dev.felipe.canchas.core.port.out.TeamStore;
import dev.felipe.canchas.core.model.Booking;
import dev.felipe.canchas.core.model.Court;
import dev.felipe.canchas.core.model.CourtFormat;
import dev.felipe.canchas.core.model.StaffRole;
import dev.felipe.canchas.core.model.Team;
import dev.felipe.canchas.core.model.TimeSlot;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

/**
 * Carga una semana de ejemplo (si no hay cuentas del personal): canchas, tarifas, equipos, partidos jugados,
 * inasistencias, una cancelación y reservas para hoy y los próximos días.
 * Las reservas del pasado se crean con un reloj propio que "viaja" a esas fechas, así pasan por las mismas reglas.
 */
final class DemoData {
    private final UseCases app;
    private final StaffAccountStore staffStore;
    private final CourtStore courtStore;
    private final TeamStore teamStore;
    private final BookingStore bookingStore;
    private final RatePlanStore rateStore;
    private final String adminUser;
    private final String adminPassword;

    private final Map<String, Court> courts = new HashMap<>();
    private final Map<String, Team> teams = new HashMap<>();

    DemoData(UseCases app, StaffAccountStore staffStore, CourtStore courtStore, TeamStore teamStore,
             BookingStore bookingStore, RatePlanStore rateStore, String adminUser, String adminPassword) {
        this.app = app;
        this.staffStore = staffStore;
        this.courtStore = courtStore;
        this.teamStore = teamStore;
        this.bookingStore = bookingStore;
        this.rateStore = rateStore;
        this.adminUser = adminUser;
        this.adminPassword = adminPassword;
    }

    void loadIfEmpty() {
        if (!staffStore.findAll().isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        app.staff().create(new NewStaff(adminUser, adminPassword, StaffRole.ADMIN));
        app.staff().create(new NewStaff("recepcion", "recepcion123", StaffRole.RECEPTION));

        app.rates().update(CourtFormat.FUTBOL_5, new BigDecimal("70000"), new BigDecimal("95000"));
        app.rates().update(CourtFormat.FUTBOL_7, new BigDecimal("110000"), new BigDecimal("140000"));

        court("F5-A", "Cancha Norte", CourtFormat.FUTBOL_5);
        court("F5-B", "Cancha Sur", CourtFormat.FUTBOL_5);
        court("F5-C", "Cancha Techada", CourtFormat.FUTBOL_5);
        court("F7-A", "Cancha Principal", CourtFormat.FUTBOL_7);
        court("F7-B", "Cancha Oriente", CourtFormat.FUTBOL_7);

        team("Los Tigres FC", "Andrés Gómez", "3001234567");
        team("Real Barrio", "Camila Torres", "3109876543");
        team("Deportivo Amigos", "Julián Rojas", "3205551234");
        team("Atlético Oficina", "Sofía Martínez", "3157778899");
        team("Juventud Unida", "Mateo Herrera", "3014443322");
        team("Los Halcones", "Valentina Ruiz", "3186660011");

        // Semana pasada: partidos cerrados (jugados, inasistencias y una cancelación)
        past(today.minusDays(6), "F5-A", "LOS TIGRES FC", 19, 2, Outcome.PLAYED);
        past(today.minusDays(6), "F7-A", "DEPORTIVO AMIGOS", 20, 1, Outcome.PLAYED);
        past(today.minusDays(5), "F5-B", "REAL BARRIO", 17, 2, Outcome.PLAYED);
        past(today.minusDays(5), "F5-A", "ATLÉTICO OFICINA", 20, 1, Outcome.NO_SHOW);
        past(today.minusDays(4), "F7-B", "LOS HALCONES", 10, 2, Outcome.PLAYED);
        past(today.minusDays(4), "F5-C", "JUVENTUD UNIDA", 19, 1, Outcome.CANCELLED);
        past(today.minusDays(3), "F5-A", "LOS TIGRES FC", 20, 1, Outcome.PLAYED);
        past(today.minusDays(3), "F7-A", "ATLÉTICO OFICINA", 21, 1, Outcome.NO_SHOW); // segunda inasistencia: sancionado
        past(today.minusDays(2), "F5-B", "DEPORTIVO AMIGOS", 9, 1, Outcome.PLAYED);
        past(today.minusDays(2), "F7-A", "REAL BARRIO", 19, 2, Outcome.PLAYED);
        past(today.minusDays(1), "F5-C", "LOS HALCONES", 18, 2, Outcome.PLAYED);
        past(today.minusDays(1), "F5-A", "JUVENTUD UNIDA", 21, 1, Outcome.PLAYED);

        // Hoy: un partido que ya empezó (pendiente de marcar como jugado) y otro más tarde
        int hourNow = now.getHour();
        if (hourNow >= TimeSlot.OPENING_HOUR && hourNow < TimeSlot.CLOSING_HOUR) {
            past(today, "F5-A", "LOS TIGRES FC", hourNow, 1, Outcome.STILL_PENDING);
        }
        if (hourNow + 2 >= TimeSlot.OPENING_HOUR && hourNow + 2 < TimeSlot.CLOSING_HOUR) {
            upcoming(today, "F7-A", "DEPORTIVO AMIGOS", Math.max(hourNow + 2, TimeSlot.OPENING_HOUR), 1);
        }

        // Próximos días
        upcoming(today.plusDays(1), "F5-B", "REAL BARRIO", 19, 2);
        upcoming(today.plusDays(1), "F7-B", "JUVENTUD UNIDA", 20, 1);
        upcoming(today.plusDays(1), "F5-A", "LOS HALCONES", 8, 1);
        upcoming(today.plusDays(2), "F5-C", "LOS TIGRES FC", 18, 1);
    }

    private enum Outcome { PLAYED, NO_SHOW, CANCELLED, STILL_PENDING }

    private void court(String code, String name, CourtFormat format) {
        courts.put(code, app.courts().open(new CourtCommand(code, name, format)));
    }

    private void team(String name, String captain, String phone) {
        Team team = app.teams().register(new TeamCommand(name, captain, phone));
        teams.put(team.name(), team);
    }

    private void upcoming(LocalDate date, String court, String team, int hour, int hours) {
        app.bookings().book(new BookingRequest(courts.get(court).id(), teams.get(team).id(), date, hour, hours));
    }

    /** Reserva hecha el día anterior a las 10:00 y cerrada según el resultado, con un reloj que viaja en el tiempo. */
    private void past(LocalDate date, String court, String team, int hour, int hours, Outcome outcome) {
        TravelClock clock = new TravelClock();
        UseCases history = CanchasApp.wire(courtStore, teamStore, bookingStore, rateStore, staffStore, noHashing(), clock);
        TimeSlot slot = new TimeSlot(date, hour, hours);

        clock.travelTo(date.minusDays(1).atTime(10, 0));
        Booking booking = history.bookings().book(new BookingRequest(courts.get(court).id(), teams.get(team).id(), date, hour, hours));
        switch (outcome) {
            case PLAYED -> {
                clock.travelTo(slot.end());
                history.bookings().markPlayed(booking.id());
            }
            case NO_SHOW -> {
                clock.travelTo(slot.start().plusMinutes(20));
                history.bookings().markNoShow(booking.id());
            }
            case CANCELLED -> {
                clock.travelTo(slot.start().minusHours(5));
                history.bookings().cancel(booking.id());
            }
            case STILL_PENDING -> {
            }
        }
    }

    private static PasswordHashing noHashing() {
        return new PasswordHashing() {
            public String hash(String raw) {
                throw new UnsupportedOperationException();
            }

            public boolean matches(String raw, String hash) {
                return false;
            }
        };
    }

    /** Reloj que se puede mover a cualquier fecha (solo para cargar el historial de ejemplo). */
    private static final class TravelClock extends Clock {
        private Instant instant = Instant.now();

        void travelTo(LocalDateTime moment) {
            instant = moment.atZone(ZoneId.systemDefault()).toInstant();
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.systemDefault();
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
