package dev.felipe.canchas.adapter.persistence.sqlserver;

import dev.felipe.canchas.core.port.out.TeamStore;
import dev.felipe.canchas.core.model.Team;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SqlTeamStore implements TeamStore {
    private static final RowMapper<Team> ROW = (rs, ctx) -> new Team(
            SqlServerSchema.uuid(rs, "equipo_id"),
            rs.getString("nombre"),
            rs.getString("capitan"),
            rs.getString("telefono"));

    private final Jdbi jdbi;

    public SqlTeamStore(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    @Override
    public void save(Team team) {
        jdbi.useTransaction(handle -> {
            int changed = handle.createUpdate("update equipo set nombre = :nombre, capitan = :capitan, telefono = :telefono where equipo_id = :id")
                    .bind("id", team.id()).bind("nombre", team.name()).bind("capitan", team.captain()).bind("telefono", team.phone())
                    .execute();
            if (changed == 0) {
                handle.createUpdate("insert into equipo (equipo_id, nombre, capitan, telefono) values (:id, :nombre, :capitan, :telefono)")
                        .bind("id", team.id()).bind("nombre", team.name()).bind("capitan", team.captain()).bind("telefono", team.phone())
                        .execute();
            }
        });
    }

    @Override
    public Optional<Team> findById(UUID id) {
        return jdbi.withHandle(h -> h.createQuery("select * from equipo where equipo_id = :id").bind("id", id).map(ROW).findOne());
    }

    @Override
    public Optional<Team> findByName(String name) {
        return jdbi.withHandle(h -> h.createQuery("select * from equipo where nombre = :nombre")
                .bind("nombre", Team.normalizeName(name)).map(ROW).findOne());
    }

    @Override
    public List<Team> findAll() {
        return jdbi.withHandle(h -> h.createQuery("select * from equipo order by nombre").map(ROW).list());
    }
}
