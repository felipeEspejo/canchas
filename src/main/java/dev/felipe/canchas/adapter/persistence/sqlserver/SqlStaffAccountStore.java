package dev.felipe.canchas.adapter.persistence.sqlserver;

import dev.felipe.canchas.core.port.out.StaffAccountStore;
import dev.felipe.canchas.core.model.StaffAccount;
import dev.felipe.canchas.core.model.StaffRole;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SqlStaffAccountStore implements StaffAccountStore {
    private static final RowMapper<StaffAccount> ROW = (rs, ctx) -> new StaffAccount(
            SqlServerSchema.uuid(rs, "cuenta_id"),
            rs.getString("usuario"),
            rs.getString("clave_hash"),
            StaffRole.valueOf(rs.getString("rol")));

    private final Jdbi jdbi;

    public SqlStaffAccountStore(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    @Override
    public void save(StaffAccount account) {
        jdbi.useTransaction(handle -> {
            int changed = handle.createUpdate("update cuenta_personal set usuario = :usuario, clave_hash = :clave, rol = :rol where cuenta_id = :id")
                    .bind("id", account.id()).bind("usuario", account.username()).bind("clave", account.passwordHash())
                    .bind("rol", account.role().name())
                    .execute();
            if (changed == 0) {
                handle.createUpdate("insert into cuenta_personal (cuenta_id, usuario, clave_hash, rol) values (:id, :usuario, :clave, :rol)")
                        .bind("id", account.id()).bind("usuario", account.username()).bind("clave", account.passwordHash())
                        .bind("rol", account.role().name())
                        .execute();
            }
        });
    }

    @Override
    public Optional<StaffAccount> findById(UUID id) {
        return jdbi.withHandle(h -> h.createQuery("select * from cuenta_personal where cuenta_id = :id").bind("id", id).map(ROW).findOne());
    }

    @Override
    public Optional<StaffAccount> findByUsername(String username) {
        return jdbi.withHandle(h -> h.createQuery("select * from cuenta_personal where lower(usuario) = lower(:usuario)")
                .bind("usuario", username).map(ROW).findOne());
    }

    @Override
    public List<StaffAccount> findAll() {
        return jdbi.withHandle(h -> h.createQuery("select * from cuenta_personal order by usuario").map(ROW).list());
    }

    @Override
    public void delete(UUID id) {
        jdbi.useHandle(h -> h.createUpdate("delete from cuenta_personal where cuenta_id = :id").bind("id", id).execute());
    }
}
