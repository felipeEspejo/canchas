package dev.felipe.canchas.core.model;

import dev.felipe.canchas.core.BusinessRuleException;

import java.math.BigDecimal;

/**
 * Tarifas por hora de un formato. La hora diurna es la de lunes a viernes antes de las 18:00; la nocturna, la de
 * las 18:00 en adelante y todo el fin de semana (ver PricingPolicy). La nocturna nunca es más barata que la diurna.
 */
public record RatePlan(CourtFormat format, BigDecimal dayRate, BigDecimal nightRate) {
    public RatePlan {
        if (format == null) {
            throw new BusinessRuleException("La tarifa necesita un formato de cancha");
        }
        if (dayRate == null || dayRate.signum() <= 0 || nightRate == null || nightRate.signum() <= 0) {
            throw new BusinessRuleException("Las tarifas diurna y nocturna deben ser mayores a cero");
        }
        if (nightRate.compareTo(dayRate) < 0) {
            throw new BusinessRuleException("La tarifa nocturna no puede ser menor que la diurna");
        }
    }

    public RatePlan withRates(BigDecimal newDayRate, BigDecimal newNightRate) {
        return new RatePlan(format, newDayRate, newNightRate);
    }
}
