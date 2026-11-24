package dev.felipe.canchas.adapter.persistence.memory;

import dev.felipe.canchas.core.port.out.BookingStore;
import dev.felipe.canchas.core.port.out.CourtStore;
import dev.felipe.canchas.core.port.out.RatePlanStore;
import dev.felipe.canchas.core.port.out.StaffAccountStore;
import dev.felipe.canchas.core.port.out.TeamStore;
import dev.felipe.canchas.core.model.Booking;
import dev.felipe.canchas.core.model.Court;
import dev.felipe.canchas.core.model.CourtFormat;
import dev.felipe.canchas.core.model.RatePlan;
import dev.felipe.canchas.core.model.StaffAccount;
import dev.felipe.canchas.core.model.Team;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adaptadores de salida en memoria (se pierden al cerrar). Sirven para las pruebas y para correr sin base de datos.
 */
public final class MemoryStores {
    private MemoryStores() {
    }

    public static class Courts implements CourtStore {
        private final Map<UUID, Court> data = new ConcurrentHashMap<>();

        public void save(Court court) { data.put(court.id(), court); }
        public Optional<Court> findById(UUID id) { return Optional.ofNullable(data.get(id)); }
        public Optional<Court> findByCode(String code) {
            return data.values().stream().filter(c -> c.code().equalsIgnoreCase(code)).findFirst();
        }
        public List<Court> findAll() { return List.copyOf(data.values()); }
    }

    public static class Teams implements TeamStore {
        private final Map<UUID, Team> data = new ConcurrentHashMap<>();

        public void save(Team team) { data.put(team.id(), team); }
        public Optional<Team> findById(UUID id) { return Optional.ofNullable(data.get(id)); }
        public Optional<Team> findByName(String name) {
            String wanted = Team.normalizeName(name);
            return data.values().stream().filter(t -> t.name().equals(wanted)).findFirst();
        }
        public List<Team> findAll() { return List.copyOf(data.values()); }
    }

    public static class Bookings implements BookingStore {
        private final Map<UUID, Booking> data = new ConcurrentHashMap<>();

        public void save(Booking booking) { data.put(booking.id(), booking); }
        public Optional<Booking> findById(UUID id) { return Optional.ofNullable(data.get(id)); }
        public List<Booking> findOn(LocalDate date) {
            return data.values().stream().filter(b -> b.slot().date().equals(date)).toList();
        }
        public List<Booking> findBetween(LocalDate from, LocalDate to) {
            return data.values().stream()
                    .filter(b -> !b.slot().date().isBefore(from) && !b.slot().date().isAfter(to))
                    .toList();
        }
        public List<Booking> findByTeam(UUID teamId) {
            return data.values().stream().filter(b -> b.teamId().equals(teamId)).toList();
        }
        public List<Booking> findByCourt(UUID courtId) {
            return data.values().stream().filter(b -> b.courtId().equals(courtId)).toList();
        }
    }

    public static class RatePlans implements RatePlanStore {
        private final Map<CourtFormat, RatePlan> data = new ConcurrentHashMap<>();

        public void save(RatePlan plan) { data.put(plan.format(), plan); }
        public Optional<RatePlan> findByFormat(CourtFormat format) { return Optional.ofNullable(data.get(format)); }
        public List<RatePlan> findAll() { return List.copyOf(data.values()); }
    }

    public static class StaffAccounts implements StaffAccountStore {
        private final Map<UUID, StaffAccount> data = new ConcurrentHashMap<>();

        public void save(StaffAccount account) { data.put(account.id(), account); }
        public Optional<StaffAccount> findById(UUID id) { return Optional.ofNullable(data.get(id)); }
        public Optional<StaffAccount> findByUsername(String username) {
            return data.values().stream().filter(a -> a.username().equalsIgnoreCase(username)).findFirst();
        }
        public List<StaffAccount> findAll() { return List.copyOf(data.values()); }
        public void delete(UUID id) { data.remove(id); }
    }
}
