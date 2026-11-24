package dev.felipe.canchas.core.service;

import dev.felipe.canchas.core.port.in.StaffDirectoryUseCase;
import dev.felipe.canchas.core.port.out.PasswordHashing;
import dev.felipe.canchas.core.port.out.StaffAccountStore;
import dev.felipe.canchas.core.BusinessRuleException;
import dev.felipe.canchas.core.NotFoundException;
import dev.felipe.canchas.core.model.StaffAccount;
import dev.felipe.canchas.core.model.StaffRole;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Cuentas del personal. Siempre debe quedar al menos un administrador. */
public class StaffDirectory implements StaffDirectoryUseCase {
    private static final int MIN_PASSWORD = 6;

    private final StaffAccountStore accounts;
    private final PasswordHashing hashing;

    public StaffDirectory(StaffAccountStore accounts, PasswordHashing hashing) {
        this.accounts = accounts;
        this.hashing = hashing;
    }

    @Override
    public StaffAccount create(NewStaff command) {
        StaffAccount account = new StaffAccount(UUID.randomUUID(), command.username(),
                hashing.hash(checked(command.password())), command.role());
        if (accounts.findByUsername(account.username()).isPresent()) {
            throw new BusinessRuleException("El usuario " + account.username() + " ya existe");
        }
        accounts.save(account);
        return account;
    }

    @Override
    public StaffAccount change(UUID accountId, StaffChange command) {
        StaffAccount current = existing(accountId);
        if (command.role() == null) {
            throw new BusinessRuleException("Elige el rol de la cuenta");
        }
        if (current.role() == StaffRole.ADMIN && command.role() != StaffRole.ADMIN && admins() <= 1) {
            throw new BusinessRuleException("Tiene que quedar al menos un administrador");
        }
        StaffAccount changed = current.withRole(command.role());
        if (command.password() != null && !command.password().isBlank()) {
            changed = changed.withPasswordHash(hashing.hash(checked(command.password())));
        }
        accounts.save(changed);
        return changed;
    }

    @Override
    public void remove(UUID accountId, String actingUsername) {
        StaffAccount account = existing(accountId);
        if (account.username().equalsIgnoreCase(actingUsername)) {
            throw new BusinessRuleException("No puedes eliminar tu propia cuenta");
        }
        if (account.role() == StaffRole.ADMIN && admins() <= 1) {
            throw new BusinessRuleException("Tiene que quedar al menos un administrador");
        }
        accounts.delete(accountId);
    }

    @Override
    public List<StaffAccount> all() {
        return accounts.findAll().stream().sorted(Comparator.comparing(StaffAccount::username)).toList();
    }

    @Override
    public Optional<StaffAccount> byId(UUID accountId) {
        return accounts.findById(accountId);
    }

    @Override
    public Optional<StaffAccount> byUsername(String username) {
        return username == null ? Optional.empty() : accounts.findByUsername(username.trim());
    }

    @Override
    public Optional<StaffAccount> authenticate(String username, String password) {
        if (password == null) {
            return Optional.empty();
        }
        return byUsername(username).filter(account -> hashing.matches(password, account.passwordHash()));
    }

    private long admins() {
        return accounts.findAll().stream().filter(a -> a.role() == StaffRole.ADMIN).count();
    }

    private StaffAccount existing(UUID accountId) {
        return accounts.findById(accountId).orElseThrow(() -> new NotFoundException("La cuenta no existe"));
    }

    private static String checked(String password) {
        if (password == null || password.length() < MIN_PASSWORD) {
            throw new BusinessRuleException("La contraseña debe tener mínimo " + MIN_PASSWORD + " caracteres");
        }
        return password;
    }
}
