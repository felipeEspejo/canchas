package dev.felipe.canchas.core.model;

import dev.felipe.canchas.core.BusinessRuleException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Reserva de una cancha para un equipo en una franja. El precio se fija al reservar (con las tarifas de ese momento)
 * y se cobra cuando el partido se marca como jugado.
 */
public record Booking(
        UUID id,
        UUID courtId,
        UUID teamId,
        TimeSlot slot,
        BigDecimal price,
        BookingStatus status,
        LocalDateTime bookedAt,
        LocalDateTime settledAt
) {
    /** Se puede cancelar sin problema hasta 2 horas antes de empezar. */
    public static final int CANCELLATION_NOTICE_HOURS = 2;

    public Booking {
        Objects.requireNonNull(id, "La reserva necesita un id");
        Objects.requireNonNull(courtId, "La reserva necesita una cancha");
        Objects.requireNonNull(teamId, "La reserva necesita un equipo");
        Objects.requireNonNull(slot, "La reserva necesita una franja");
        Objects.requireNonNull(status, "La reserva necesita un estado");
        Objects.requireNonNull(bookedAt, "La reserva necesita la fecha en que se hizo");
        if (price == null || price.signum() <= 0) {
            throw new BusinessRuleException("El precio de la reserva debe ser mayor a cero");
        }
        if (status == BookingStatus.RESERVED && settledAt != null) {
            throw new BusinessRuleException("Una reserva pendiente no puede tener fecha de cierre");
        }
        if (status != BookingStatus.RESERVED && settledAt == null) {
            throw new BusinessRuleException("Una reserva cerrada necesita su fecha de cierre");
        }
    }

    public static Booking reserve(UUID id, UUID courtId, UUID teamId, TimeSlot slot, BigDecimal price, LocalDateTime now) {
        if (!slot.start().isAfter(now)) {
            throw new BusinessRuleException("No se puede reservar una franja que ya empezó o ya pasó");
        }
        return new Booking(id, courtId, teamId, slot, price, BookingStatus.RESERVED, now, null);
    }

    public boolean isPending() {
        return status == BookingStatus.RESERVED;
    }

    /** El partido se jugó: se cobra el precio pactado. Solo desde la hora de inicio. */
    public Booking markPlayed(LocalDateTime now) {
        requirePending();
        if (now.isBefore(slot.start())) {
            throw new BusinessRuleException("El partido todavía no empieza (" + slot.label() + ")");
        }
        return settle(BookingStatus.PLAYED, now);
    }

    /** El equipo no llegó. Solo se puede marcar una vez que pasó la hora de inicio. */
    public Booking markNoShow(LocalDateTime now) {
        requirePending();
        if (now.isBefore(slot.start())) {
            throw new BusinessRuleException("Todavía no es la hora del partido: no se puede marcar como no presentado");
        }
        return settle(BookingStatus.NO_SHOW, now);
    }

    /** Cancelar libera la cancha, pero solo con al menos 2 horas de anticipación. */
    public Booking cancel(LocalDateTime now) {
        requirePending();
        if (now.isAfter(slot.start().minusHours(CANCELLATION_NOTICE_HOURS))) {
            throw new BusinessRuleException("Solo se puede cancelar hasta " + CANCELLATION_NOTICE_HOURS
                    + " horas antes del partido; después se marca como jugado o como no presentado");
        }
        return settle(BookingStatus.CANCELLED, now);
    }

    /** Lo que esta reserva le dejó al complejo. */
    public BigDecimal revenue() {
        return status == BookingStatus.PLAYED ? price : BigDecimal.ZERO;
    }

    private void requirePending() {
        if (!isPending()) {
            throw new BusinessRuleException("La reserva ya está cerrada (" + status.getLabel().toLowerCase() + ")");
        }
    }

    private Booking settle(BookingStatus newStatus, LocalDateTime now) {
        return new Booking(id, courtId, teamId, slot, price, newStatus, bookedAt, now);
    }
}
