package dev.felipe.canchas.core.port.out;

import dev.felipe.canchas.core.model.Booking;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: dónde se guardan las reservas. */
public interface BookingStore {
    void save(Booking booking);

    Optional<Booking> findById(UUID id);

    List<Booking> findOn(LocalDate date);

    /** Reservas con fecha entre from y to (ambos incluidos). */
    List<Booking> findBetween(LocalDate from, LocalDate to);

    List<Booking> findByTeam(UUID teamId);

    List<Booking> findByCourt(UUID courtId);
}
