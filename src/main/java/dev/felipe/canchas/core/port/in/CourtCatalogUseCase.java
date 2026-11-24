package dev.felipe.canchas.core.port.in;

import dev.felipe.canchas.core.model.Court;
import dev.felipe.canchas.core.model.CourtFormat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de entrada: administrar el catálogo de canchas. */
public interface CourtCatalogUseCase {

    record CourtCommand(String code, String name, CourtFormat format) {
    }

    Court open(CourtCommand command);

    /** Cambia código, nombre y formato. El formato no cambia si la cancha ya tiene reservas. */
    Court update(UUID courtId, CourtCommand command);

    Court activate(UUID courtId);

    /** Fuera de servicio: no se puede si tiene reservas pendientes. */
    Court deactivate(UUID courtId);

    List<Court> all();

    Optional<Court> byId(UUID courtId);
}
