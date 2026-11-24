package dev.felipe.canchas.adapter.persistence.sqlserver;

import dev.felipe.canchas.core.port.out.RatePlanStore;
import dev.felipe.canchas.core.model.CourtFormat;
import dev.felipe.canchas.core.model.RatePlan;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapper;

import java.util.List;
import java.util.Optional;

public class SqlRatePlanStore implements RatePlanStore {
    private static final RowMapper<RatePlan> ROW = (rs, ctx) -> new RatePlan(
            CourtFormat.valueOf(rs.getString("formato")),
            rs.getBigDecimal("valor_diurno"),
            rs.getBigDecimal("valor_nocturno"));

    private final Jdbi jdbi;

    public SqlRatePlanStore(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    @Override
    public void save(RatePlan plan) {
        jdbi.useTransaction(handle -> {
            int changed = handle.createUpdate("update tarifa set valor_diurno = :diurno, valor_nocturno = :nocturno where formato = :formato")
                    .bind("formato", plan.format().name()).bind("diurno", plan.dayRate()).bind("nocturno", plan.nightRate())
                    .execute();
            if (changed == 0) {
                handle.createUpdate("insert into tarifa (formato, valor_diurno, valor_nocturno) values (:formato, :diurno, :nocturno)")
                        .bind("formato", plan.format().name()).bind("diurno", plan.dayRate()).bind("nocturno", plan.nightRate())
                        .execute();
            }
        });
    }

    @Override
    public Optional<RatePlan> findByFormat(CourtFormat format) {
        return jdbi.withHandle(h -> h.createQuery("select * from tarifa where formato = :formato")
                .bind("formato", format.name()).map(ROW).findOne());
    }

    @Override
    public List<RatePlan> findAll() {
        return jdbi.withHandle(h -> h.createQuery("select * from tarifa order by formato").map(ROW).list());
    }
}
