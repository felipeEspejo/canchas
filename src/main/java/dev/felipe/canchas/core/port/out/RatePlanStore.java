package dev.felipe.canchas.core.port.out;

import dev.felipe.canchas.core.model.CourtFormat;
import dev.felipe.canchas.core.model.RatePlan;

import java.util.List;
import java.util.Optional;

/** Puerto de salida: dónde se guardan las tarifas (una por formato). */
public interface RatePlanStore {
    void save(RatePlan plan);

    Optional<RatePlan> findByFormat(CourtFormat format);

    List<RatePlan> findAll();
}
