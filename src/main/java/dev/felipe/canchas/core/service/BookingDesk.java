package dev.felipe.canchas.core.service;

import dev.felipe.canchas.core.port.in.BookingDeskUseCase;
import dev.felipe.canchas.core.port.out.BookingStore;
import dev.felipe.canchas.core.port.out.CourtStore;
import dev.felipe.canchas.core.port.out.RatePlanStore;
import dev.felipe.canchas.core.port.out.TeamStore;
import dev.felipe.canchas.core.BusinessRuleException;
import dev.felipe.canchas.core.NotFoundException;
import dev.felipe.canchas.core.model.Booking;
import dev.felipe.canchas.core.model.Court;
import dev.felipe.canchas.core.model.Team;
import dev.felipe.canchas.core.model.TimeSlot;
import dev.felipe.canchas.core.policy.AttendancePolicy;
import dev.felipe.canchas.core.policy.PriceQuote;
import dev.felipe.canchas.core.policy.PricingPolicy;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * Recepción: reservar y cerrar reservas. Antes de reservar revisa, en este orden:
 * cancha activa, franja válida y futura, equipo sin sanción y sin exceso de pendientes,
 * que la cancha esté libre y que el equipo no tenga otro partido a la misma hora.
 */
public class BookingDesk implements BookingDeskUseCase {
    private static final LocalDate FAR_PAST = LocalDate.of(2000, 1, 1);
    private static final LocalDate FAR_FUTURE = LocalDate.of(2999, 12, 31);

    private final CourtStore courts;
    private final TeamStore teams;
    private final BookingStore bookings;
    private final RatePlanStore rates;
    private final PricingPolicy pricing;
    private final AttendancePolicy attendance;
    private final Clock clock;

    public BookingDesk(CourtStore courts, TeamStore teams, BookingStore bookings, RatePlanStore rates,
                       PricingPolicy pricing, AttendancePolicy attendance, Clock clock) {
        this.courts = courts;
        this.teams = teams;
        this.bookings = bookings;
        this.rates = rates;
        this.pricing = pricing;
        this.attendance = attendance;
        this.clock = clock;
    }

    @Override
    public PriceQuote quote(BookingRequest request) {
        Court court = activeCourt(request.courtId());
        return pricing.quote(rates.findByFormat(court.format()).orElse(null), slotOf(request));
    }

    @Override
    public Booking book(BookingRequest request) {
        Court court = activeCourt(request.courtId());
        TimeSlot slot = slotOf(request);
        if (request.teamId() == null) {
            throw new BusinessRuleException("Elige el equipo que reserva");
        }
        Team team = teams.findById(request.teamId()).orElseThrow(() -> new NotFoundException("El equipo no existe"));
        LocalDateTime now = LocalDateTime.now(clock);

        List<Booking> teamHistory = bookings.findByTeam(team.id());
        if (attendance.isSanctioned(teamHistory, now.toLocalDate())) {
            throw new BusinessRuleException("El equipo " + team.name() + " está sancionado: tiene "
                    + attendance.recentNoShows(teamHistory, now.toLocalDate()) + " inasistencias en los últimos "
                    + AttendancePolicy.SANCTION_WINDOW_DAYS + " días");
        }
        if (attendance.pendingAhead(teamHistory, now) >= AttendancePolicy.MAX_PENDING_BOOKINGS) {
            throw new BusinessRuleException("El equipo " + team.name() + " ya tiene " + AttendancePolicy.MAX_PENDING_BOOKINGS
                    + " reservas pendientes: debe jugar o cancelar alguna antes de reservar otra");
        }

        List<Booking> sameDay = bookings.findOn(slot.date()).stream()
                .filter(b -> b.status().holdsCourt() && b.slot().overlaps(slot))
                .toList();
        sameDay.stream().filter(b -> b.courtId().equals(court.id())).findFirst().ifPresent(taken -> {
            throw new BusinessRuleException("La cancha " + court.code() + " ya está reservada de " + taken.slot().label());
        });
        sameDay.stream().filter(b -> b.teamId().equals(team.id())).findFirst().ifPresent(clash -> {
            throw new BusinessRuleException("El equipo " + team.name() + " ya tiene un partido de " + clash.slot().label() + " ese día");
        });

        PriceQuote quote = pricing.quote(rates.findByFormat(court.format()).orElse(null), slot);
        Booking booking = Booking.reserve(UUID.randomUUID(), court.id(), team.id(), slot, quote.total(), now);
        bookings.save(booking);
        return booking;
    }

    @Override
    public Booking markPlayed(UUID bookingId) {
        return close(bookingId, b -> b.markPlayed(LocalDateTime.now(clock)));
    }

    @Override
    public Booking markNoShow(UUID bookingId) {
        return close(bookingId, b -> b.markNoShow(LocalDateTime.now(clock)));
    }

    @Override
    public Booking cancel(UUID bookingId) {
        return close(bookingId, b -> b.cancel(LocalDateTime.now(clock)));
    }

    @Override
    public BookingView view(UUID bookingId) {
        return new Views(courts, teams).of(existing(bookingId));
    }

    @Override
    public List<BookingView> search(BookingFilter filter) {
        LocalDate from = filter.from() == null ? FAR_PAST : filter.from();
        LocalDate to = filter.to() == null ? FAR_FUTURE : filter.to();
        if (to.isBefore(from)) {
            throw new BusinessRuleException("La fecha final no puede ser anterior a la inicial");
        }
        List<Booking> found = bookings.findBetween(from, to).stream()
                .filter(b -> filter.teamId() == null || b.teamId().equals(filter.teamId()))
                .filter(b -> filter.status() == null || b.status() == filter.status())
                .sorted(Views.NEWEST_FIRST)
                .toList();
        return new Views(courts, teams).of(found);
    }

    private Booking close(UUID bookingId, UnaryOperator<Booking> transition) {
        Booking closed = transition.apply(existing(bookingId));
        bookings.save(closed);
        return closed;
    }

    private Booking existing(UUID bookingId) {
        return bookings.findById(bookingId).orElseThrow(() -> new NotFoundException("La reserva no existe"));
    }

    private Court activeCourt(UUID courtId) {
        if (courtId == null) {
            throw new BusinessRuleException("Elige la cancha");
        }
        Court court = courts.findById(courtId).orElseThrow(() -> new NotFoundException("La cancha no existe"));
        if (!court.active()) {
            throw new BusinessRuleException("La cancha " + court.code() + " está fuera de servicio");
        }
        return court;
    }

    private static TimeSlot slotOf(BookingRequest request) {
        if (request.startHour() == null || request.hours() == null) {
            throw new BusinessRuleException("Elige la hora de inicio y cuántas horas se juega");
        }
        return new TimeSlot(request.date(), request.startHour(), request.hours());
    }
}
