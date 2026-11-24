package dev.felipe.canchas.core.port.in;

import dev.felipe.canchas.core.model.StaffAccount;
import dev.felipe.canchas.core.model.StaffRole;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de entrada: cuentas del personal y su autenticación. */
public interface StaffDirectoryUseCase {

    record NewStaff(String username, String password, StaffRole role) {
    }

    /** password vacía = se conserva la actual. */
    record StaffChange(StaffRole role, String password) {
    }

    StaffAccount create(NewStaff command);

    StaffAccount change(UUID accountId, StaffChange command);

    /** actingUsername es quien hace la petición: nadie se elimina a sí mismo. */
    void remove(UUID accountId, String actingUsername);

    List<StaffAccount> all();

    Optional<StaffAccount> byId(UUID accountId);

    Optional<StaffAccount> byUsername(String username);

    Optional<StaffAccount> authenticate(String username, String password);
}
