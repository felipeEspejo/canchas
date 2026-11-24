package dev.felipe.canchas.core.port.in;

import dev.felipe.canchas.core.model.CourtFormat;
import dev.felipe.canchas.core.model.RatePlan;

import java.math.BigDecimal;
import java.util.List;

/** Puerto de entrada: consultar y ajustar las tarifas diurna y nocturna de cada formato. */
public interface RateBookUseCase {

    List<RatePlan> all();

    /** Los cambios valen para las reservas nuevas: las ya hechas conservan su precio. */
    RatePlan update(CourtFormat format, BigDecimal dayRate, BigDecimal nightRate);
}
