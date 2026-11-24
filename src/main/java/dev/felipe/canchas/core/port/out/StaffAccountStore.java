package dev.felipe.canchas.core.port.out;

import dev.felipe.canchas.core.model.StaffAccount;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: dónde se guardan las cuentas del personal. */
public interface StaffAccountStore {
    void save(StaffAccount account);

    Optional<StaffAccount> findById(UUID id);

    Optional<StaffAccount> findByUsername(String username);

    List<StaffAccount> findAll();

    void delete(UUID id);
}
