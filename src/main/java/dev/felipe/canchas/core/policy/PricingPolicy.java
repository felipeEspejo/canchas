package dev.felipe.canchas.core.policy;

import dev.felipe.canchas.core.BusinessRuleException;
import dev.felipe.canchas.core.model.RatePlan;
import dev.felipe.canchas.core.model.TimeSlot;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/**
 * Regla de precios: cada hora se cobra según su franja.
 * - Lunes a viernes antes de las 18:00: tarifa diurna.
 * - Lunes a viernes desde las 18:00: tarifa nocturna (hay que prender la iluminación).
 * - Sábado y domingo: tarifa nocturna todo el día.
 * Así, una reserva de 17:00 a 19:00 un martes cobra una hora diurna y una nocturna.
 */
public final class PricingPolicy {
    public static final int NIGHT_STARTS_AT = 18;

    public PriceQuote quote(RatePlan plan, TimeSlot slot) {
        if (plan == null) {
            throw new BusinessRuleException("No hay tarifa configurada para ese formato de cancha");
        }
        List<PriceQuote.HourCharge> charges = slot.playedHours().stream()
                .map(hour -> {
                    RateBand band = bandOf(slot.date(), hour);
                    return new PriceQuote.HourCharge(hour, band, band == RateBand.DAY ? plan.dayRate() : plan.nightRate());
                })
                .toList();
        return new PriceQuote(charges);
    }

    public static RateBand bandOf(LocalDate date, int hour) {
        DayOfWeek day = date.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return RateBand.WEEKEND;
        }
        return hour >= NIGHT_STARTS_AT ? RateBand.NIGHT : RateBand.DAY;
    }
}
