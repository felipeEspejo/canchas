package dev.felipe.canchas.core.service;

import dev.felipe.canchas.core.port.in.BookingDeskUseCase.BookingView;
import dev.felipe.canchas.core.port.in.ScheduleBoardUseCase;
import dev.felipe.canchas.core.port.out.BookingStore;
import dev.felipe.canchas.core.port.out.CourtStore;
import dev.felipe.canchas.core.port.out.TeamStore;
import dev.felipe.canchas.core.model.Booking;
import dev.felipe.canchas.core.model.BookingStatus;
import dev.felipe.canchas.core.model.Court;
import dev.felipe.canchas.core.model.CourtFormat;
import dev.felipe.canchas.core.model.TimeSlot;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class ScheduleBoard implements ScheduleBoardUseCase {
    private final CourtStore courts;
    private final TeamStore teams;
    private final BookingStore bookings;
    private final Clock clock;

    public ScheduleBoard(CourtStore courts, TeamStore teams, BookingStore bookings, Clock clock) {
        this.courts = courts;
        this.teams = teams;
        this.bookings = bookings;
        this.clock = clock;
    }

    @Override
    public DayAgenda agenda(LocalDate date) {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Court> active = activeCourts();
        // Las canceladas no ocupan la agenda; las de "no se presentó" se ven para dejar constancia
        List<Booking> ofDay = bookings.findOn(date).stream().filter(b -> b.status() != BookingStatus.CANCELLED).toList();
        Views views = new Views(courts, teams);

        List<AgendaRow> rows = active.stream()
                .map(court -> new AgendaRow(court, cellsFor(court, date, ofDay, views, now)))
                .toList();

        List<Booking> holding = ofDay.stream()
                .filter(b -> b.status().holdsCourt())
                .filter(b -> active.stream().anyMatch(c -> c.id().equals(b.courtId())))
                .toList();
        int bookedHours = holding.stream().mapToInt(b -> b.slot().hours()).sum();
        BigDecimal expected = holding.stream().map(Booking::price).reduce(BigDecimal.ZERO, BigDecimal::add);
        int pending = (int) ofDay.stream().filter(Booking::isPending).count();
        int openHours = active.size() * TimeSlot.openingHours().size();
        return new DayAgenda(date, TimeSlot.openingHours(), rows, bookedHours, openHours, pending, expected);
    }

    @Override
    public PublicAvailability availability(LocalDate date) {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Court> active = activeCourts();
        List<Booking> holding = bookings.findOn(date).stream().filter(b -> b.status().holdsCourt()).toList();

        List<FormatAvailability> formats = Arrays.stream(CourtFormat.values())
                .map(format -> {
                    List<Court> ofFormat = active.stream().filter(c -> c.format() == format).toList();
                    List<HourAvailability> hours = TimeSlot.openingHours().stream()
                            .map(hour -> {
                                long taken = ofFormat.stream()
                                        .filter(c -> holding.stream().anyMatch(b -> b.courtId().equals(c.id()) && b.slot().covers(hour)))
                                        .count();
                                return new HourAvailability(hour, (int) (ofFormat.size() - taken), ofFormat.size(), isPast(date, hour, now));
                            })
                            .toList();
                    return new FormatAvailability(format, ofFormat.size(), hours);
                })
                .filter(f -> f.courts() > 0)
                .toList();
        return new PublicAvailability(date, formats);
    }

    /**
     * Recorre las horas de la cancha: si a esa hora hay una reserva, la celda ocupa todas sus horas (span) y se salta
     * hasta que termina; si no, es una celda libre de una hora. Se prefieren las reservas que ocupan la cancha.
     */
    private static List<AgendaCell> cellsFor(Court court, LocalDate date, List<Booking> ofDay, Views views, LocalDateTime now) {
        List<Booking> mine = ofDay.stream()
                .filter(b -> b.courtId().equals(court.id()))
                .sorted(Comparator.comparing((Booking b) -> !b.status().holdsCourt()).thenComparing(b -> b.slot().startHour()))
                .toList();
        List<AgendaCell> cells = new ArrayList<>();
        int hour = TimeSlot.OPENING_HOUR;
        while (hour < TimeSlot.CLOSING_HOUR) {
            int current = hour;
            Booking here = mine.stream().filter(b -> b.slot().covers(current)).findFirst().orElse(null);
            if (here == null) {
                cells.add(new AgendaCell(hour, null, 1, isPast(date, hour, now)));
                hour++;
            } else {
                int span = here.slot().endHour() - hour;
                BookingView view = views.of(here);
                cells.add(new AgendaCell(hour, view, span, isPast(date, hour, now)));
                hour += span;
            }
        }
        return cells;
    }

    /** Una hora ya no se puede reservar cuando su inicio llegó o pasó. */
    private static boolean isPast(LocalDate date, int hour, LocalDateTime now) {
        return !date.atTime(hour, 0).isAfter(now);
    }

    private List<Court> activeCourts() {
        return courts.findAll().stream().filter(Court::active).sorted(CourtCatalog.BY_FORMAT_AND_CODE).toList();
    }
}
