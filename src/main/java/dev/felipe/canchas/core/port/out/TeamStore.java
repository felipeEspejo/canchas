package dev.felipe.canchas.core.port.out;

import dev.felipe.canchas.core.model.Team;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: dónde se guardan los equipos. */
public interface TeamStore {
    void save(Team team);

    Optional<Team> findById(UUID id);

    Optional<Team> findByName(String name);

    List<Team> findAll();
}
