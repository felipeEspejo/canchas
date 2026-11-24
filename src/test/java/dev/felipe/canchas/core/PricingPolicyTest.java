package dev.felipe.canchas.core;

import dev.felipe.canchas.core.model.CourtFormat;
import dev.felipe.canchas.core.model.RatePlan;
import dev.felipe.canchas.core.model.TimeSlot;
import dev.felipe.canchas.core.policy.PriceQuote;
import dev.felipe.canchas.core.policy.PricingPolicy;
import dev.felipe.canchas.core.policy.RateBand;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PricingPolicyTest {
    private static final LocalDate TUESDAY = LocalDate.of(2026, 11, 24);
    private static final LocalDate SATURDAY = LocalDate.of(2026, 11, 28);
    private static final RatePlan FIVE = new RatePlan(CourtFormat.FUTBOL_5, new BigDecimal("70000"), new BigDecimal("95000"));

    private final PricingPolicy pricing = new PricingPolicy();

    @Test
    void weekdayAfternoonIsDayRate() {
        PriceQuote quote = pricing.quote(FIVE, new TimeSlot(TUESDAY, 10, 2));
        assertEquals(new BigDecimal("140000"), quote.total());
        assertTrue(quote.charges().stream().allMatch(c -> c.band() == RateBand.DAY));
    }

    @Test
    void slotCrossingSixPmMixesBothRates() {
        PriceQuote quote = pricing.quote(FIVE, new TimeSlot(TUESDAY, 17, 2)); // 17 diurna + 18 nocturna
        assertEquals(RateBand.DAY, quote.charges().get(0).band());
        assertEquals(RateBand.NIGHT, quote.charges().get(1).band());
        assertEquals(new BigDecimal("165000"), quote.total());
    }

    @Test
    void weekendIsNightRateAllDay() {
        PriceQuote quote = pricing.quote(FIVE, new TimeSlot(SATURDAY, 9, 1));
        assertEquals(RateBand.WEEKEND, quote.charges().get(0).band());
        assertEquals(new BigDecimal("95000"), quote.total());
    }

    @Test
    void needsARatePlan() {
        assertThrows(BusinessRuleException.class, () -> pricing.quote(null, new TimeSlot(TUESDAY, 10, 1)));
    }

    @Test
    void nightRateCannotBeCheaperThanDayRate() {
        assertThrows(BusinessRuleException.class,
                () -> new RatePlan(CourtFormat.FUTBOL_7, new BigDecimal("100000"), new BigDecimal("90000")));
        assertThrows(BusinessRuleException.class, () -> new RatePlan(CourtFormat.FUTBOL_7, BigDecimal.ZERO, BigDecimal.ONE));
    }
}
