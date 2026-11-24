package dev.felipe.canchas.core.service;

import dev.felipe.canchas.core.port.in.RevenueReportUseCase;
import dev.felipe.canchas.core.port.out.BookingStore;
import dev.felipe.canchas.core.port.out.CourtStore;
import dev.felipe.canchas.core.port.out.TeamStore;
import dev.felipe.canchas.core.BusinessRuleException;
import dev.felipe.canchas.core.model.Booking;
import dev.felipe.canchas.core.model.BookingStatus;
import dev.felipe.canchas.core.model.Court;
import dev.felipe.canchas.core.model.CourtFormat;
import dev.felipe.canchas.core.model.Team;
import dev.felipe.canchas.core.model.TimeSlot;
import dev.felipe.canchas.core.policy.PricingPolicy;
import dev.felipe.canchas.core.policy.RateBand;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Balance de un periodo a partir de las reservas jugadas, canceladas y no presentadas. */
public class RevenueAnalyst implements RevenueReportUseCase {
    private static final int MAX_RANGE_DAYS = 366;
    private static final int TOP_TEAMS = 5;

    private final BookingStore bookings;
    private final CourtStore courts;
    private final TeamStore teams;

    public RevenueAnalyst(BookingStore bookings, CourtStore courts, TeamStore teams) {
        this.bookings = bookings;
        this.courts = courts;
        this.teams = teams;
    }

    @Override
    public RevenueReport report(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new BusinessRuleException("Elige las dos fechas del balance");
        }
        if (to.isBefore(from)) {
            throw new BusinessRuleException("La fecha final no puede ser anterior a la inicial");
        }
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        if (days > MAX_RANGE_DAYS) {
            throw new BusinessRuleException("El balance puede cubrir como máximo " + MAX_RANGE_DAYS + " días");
        }

        List<Booking> inRange = bookings.findBetween(from, to);
        List<Booking> played = inRange.stream().filter(b -> b.status() == BookingStatus.PLAYED).toList();
        Map<UUID, Court> courtById = courts.findAll().stream().collect(Collectors.toMap(Court::id, Function.identity()));
        Map<UUID, Team> teamById = teams.findAll().stream().collect(Collectors.toMap(Team::id, Function.identity()));

        BigDecimal total = played.stream().map(Booking::price).reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<CourtFormat, BigDecimal> byFormat = new EnumMap<>(CourtFormat.class);
        for (CourtFormat format : CourtFormat.values()) {
            byFormat.put(format, BigDecimal.ZERO);
        }
        Map<RateBand, Long> hoursByBand = new EnumMap<>(RateBand.class);
        for (RateBand band : RateBand.values()) {
            hoursByBand.put(band, 0L);
        }
        Map<LocalDate, BigDecimal> perDay = new TreeMap<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            perDay.put(day, BigDecimal.ZERO);
        }
        for (Booking booking : played) {
            Court court = courtById.get(booking.courtId());
            if (court != null) {
                byFormat.merge(court.format(), booking.price(), BigDecimal::add);
            }
            for (int hour : booking.slot().playedHours()) {
                hoursByBand.merge(PricingPolicy.bandOf(booking.slot().date(), hour), 1L, Long::sum);
            }
            perDay.merge(booking.slot().date(), booking.price(), BigDecimal::add);
        }

        BigDecimal best = perDay.values().stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        List<DayRevenue> dayRevenues = perDay.entrySet().stream()
                .map(e -> new DayRevenue(e.getKey(), e.getValue(), best.signum() == 0 ? 0
                        : e.getValue().multiply(BigDecimal.valueOf(100)).divide(best, 0, RoundingMode.HALF_UP).intValue()))
                .toList();

        List<TeamRanking> topTeams = played.stream()
                .collect(Collectors.groupingBy(Booking::teamId))
                .entrySet().stream()
                .map(e -> new TeamRanking(
                        teamById.containsKey(e.getKey()) ? teamById.get(e.getKey()).name() : "-",
                        e.getValue().size(),
                        e.getValue().stream().map(Booking::price).reduce(BigDecimal.ZERO, BigDecimal::add)))
                .sorted(Comparator.comparing(TeamRanking::spent).reversed().thenComparing(TeamRanking::team))
                .limit(TOP_TEAMS)
                .toList();

        long activeCourts = courtById.values().stream().filter(Court::active).count();
        long capacity = activeCourts * TimeSlot.openingHours().size() * days;
        long playedHours = hoursByBand.values().stream().mapToLong(Long::longValue).sum();
        int occupancy = capacity == 0 ? 0 : (int) (playedHours * 100 / capacity);

        return new RevenueReport(from, to, total, played.size(),
                inRange.stream().filter(b -> b.status() == BookingStatus.NO_SHOW).count(),
                inRange.stream().filter(b -> b.status() == BookingStatus.CANCELLED).count(),
                byFormat, hoursByBand, dayRevenues, topTeams, occupancy);
    }
}
