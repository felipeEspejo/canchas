package dev.felipe.canchas.adapter.persistence.sqlserver;

import dev.felipe.canchas.core.port.out.CourtStore;
import dev.felipe.canchas.core.model.Court;
import dev.felipe.canchas.core.model.CourtFormat;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SqlCourtStore implements CourtStore {
    private static final RowMapper<Court> ROW = (rs, ctx) -> new Court(
            SqlServerSchema.uuid(rs, "cancha_id"),
            rs.getString("codigo"),
            rs.getString("nombre"),
            CourtFormat.valueOf(rs.getString("formato")),
            rs.getBoolean("activa"));

    private final Jdbi jdbi;

    public SqlCourtStore(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    @Override
    public void save(Court court) {
        jdbi.useTransaction(handle -> {
            int changed = handle.createUpdate("update cancha set codigo = :codigo, nombre = :nombre, formato = :formato, activa = :activa "
                            + "where cancha_id = :id")
                    .bind("id", court.id()).bind("codigo", court.code()).bind("nombre", court.name())
                    .bind("formato", court.format().name()).bind("activa", court.active())
                    .execute();
            if (changed == 0) {
                handle.createUpdate("insert into cancha (cancha_id, codigo, nombre, formato, activa) values (:id, :codigo, :nombre, :formato, :activa)")
                        .bind("id", court.id()).bind("codigo", court.code()).bind("nombre", court.name())
                        .bind("formato", court.format().name()).bind("activa", court.active())
                        .execute();
            }
        });
    }

    @Override
    public Optional<Court> findById(UUID id) {
        return jdbi.withHandle(h -> h.createQuery("select * from cancha where cancha_id = :id").bind("id", id).map(ROW).findOne());
    }

    @Override
    public Optional<Court> findByCode(String code) {
        return jdbi.withHandle(h -> h.createQuery("select * from cancha where upper(codigo) = upper(:codigo)")
                .bind("codigo", code).map(ROW).findOne());
    }

    @Override
    public List<Court> findAll() {
        return jdbi.withHandle(h -> h.createQuery("select * from cancha order by formato, codigo").map(ROW).list());
    }
}
