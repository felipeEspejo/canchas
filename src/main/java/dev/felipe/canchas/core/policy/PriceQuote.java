package dev.felipe.canchas.core.policy;

import java.math.BigDecimal;
import java.util.List;

/** Cotización de una reserva: lo que vale cada hora y el total. */
public record PriceQuote(List<HourCharge> charges) {
    public PriceQuote {
        charges = List.copyOf(charges);
    }

    public BigDecimal total() {
        return charges.stream().map(HourCharge::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Una hora de juego con su franja y su valor. */
    public record HourCharge(int hour, RateBand band, BigDecimal amount) {
        public String label() {
            return String.format("%02d:00 – %02d:00", hour, hour + 1);
        }
    }
}
