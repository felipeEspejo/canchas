package dev.felipe.canchas.core.port.out;

import dev.felipe.canchas.core.model.Court;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: dónde se guardan las canchas. */
public interface CourtStore {
    void save(Court court);

    Optional<Court> findById(UUID id);

    Optional<Court> findByCode(String code);

    List<Court> findAll();
}
