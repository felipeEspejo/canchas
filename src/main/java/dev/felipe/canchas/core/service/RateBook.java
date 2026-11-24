package dev.felipe.canchas.core.service;

import dev.felipe.canchas.core.port.in.RateBookUseCase;
import dev.felipe.canchas.core.port.out.RatePlanStore;
import dev.felipe.canchas.core.model.CourtFormat;
import dev.felipe.canchas.core.model.RatePlan;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

public class RateBook implements RateBookUseCase {
    private final RatePlanStore plans;

    public RateBook(RatePlanStore plans) {
        this.plans = plans;
    }

    @Override
    public List<RatePlan> all() {
        return plans.findAll().stream().sorted(Comparator.comparing(RatePlan::format)).toList();
    }

    @Override
    public RatePlan update(CourtFormat format, BigDecimal dayRate, BigDecimal nightRate) {
        RatePlan plan = plans.findByFormat(format)
                .map(current -> current.withRates(dayRate, nightRate))
                .orElseGet(() -> new RatePlan(format, dayRate, nightRate));
        plans.save(plan);
        return plan;
    }
}
