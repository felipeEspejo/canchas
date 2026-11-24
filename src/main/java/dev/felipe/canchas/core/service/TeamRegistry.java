package dev.felipe.canchas.core.service;

import dev.felipe.canchas.core.port.in.TeamRegistryUseCase;
import dev.felipe.canchas.core.port.out.BookingStore;
import dev.felipe.canchas.core.port.out.CourtStore;
import dev.felipe.canchas.core.port.out.TeamStore;
import dev.felipe.canchas.core.BusinessRuleException;
import dev.felipe.canchas.core.NotFoundException;
import dev.felipe.canchas.core.model.Booking;
import dev.felipe.canchas.core.model.BookingStatus;
import dev.felipe.canchas.core.model.Team;
import dev.felipe.canchas.core.policy.AttendancePolicy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class TeamRegistry implements TeamRegistryUseCase {
    private final TeamStore teams;
    private final BookingStore bookings;
    private final CourtStore courts;
    private final AttendancePolicy attendance;
    private final Clock clock;

    public TeamRegistry(TeamStore teams, BookingStore bookings, CourtStore courts, AttendancePolicy attendance, Clock clock) {
        this.teams = teams;
        this.bookings = bookings;
        this.courts = courts;
        this.attendance = attendance;
        this.clock = clock;
    }

    @Override
    public Team register(TeamCommand command) {
        Team team = new Team(UUID.randomUUID(), command.name(), command.captain(), command.phone());
        requireFreeName(team);
        teams.save(team);
        return team;
    }

    @Override
    public Team update(UUID teamId, TeamCommand command) {
        Team updated = existing(teamId).withDetails(command.name(), command.captain(), command.phone());
        requireFreeName(updated);
        teams.save(updated);
        return updated;
    }

    @Override
    public List<Team> all() {
        return teams.findAll().stream().sorted(Comparator.comparing(Team::name)).toList();
    }

    @Override
    public List<Team> search(String text) {
        String needle = Team.normalizeName(text);
        return all().stream().filter(team -> needle.isEmpty() || team.name().contains(needle)).toList();
    }

    @Override
    public TeamRecord record(UUID teamId) {
        Team team = existing(teamId);
        List<Booking> history = bookings.findByTeam(teamId).stream().sorted(Views.NEWEST_FIRST).toList();
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        BigDecimal spent = history.stream().map(Booking::revenue).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new TeamRecord(
                team,
                new Views(courts, teams).of(history),
                count(history, BookingStatus.PLAYED),
                count(history, BookingStatus.NO_SHOW),
                count(history, BookingStatus.CANCELLED),
                attendance.pendingAhead(history, now),
                spent,
                attendance.isSanctioned(history, today));
    }

    private static long count(List<Booking> history, BookingStatus status) {
        return history.stream().filter(b -> b.status() == status).count();
    }

    private Team existing(UUID teamId) {
        return teams.findById(teamId).orElseThrow(() -> new NotFoundException("El equipo no existe"));
    }

    private void requireFreeName(Team team) {
        teams.findByName(team.name())
                .filter(other -> !other.id().equals(team.id()))
                .ifPresent(other -> {
                    throw new BusinessRuleException("Ya hay un equipo registrado con el nombre " + team.name());
                });
    }
}
