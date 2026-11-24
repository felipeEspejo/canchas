package dev.felipe.canchas.adapter.persistence.sqlserver;

import dev.felipe.canchas.core.port.out.BookingStore;
import dev.felipe.canchas.core.model.Booking;
import dev.felipe.canchas.core.model.BookingStatus;
import dev.felipe.canchas.core.model.TimeSlot;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.SqlStatement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SqlBookingStore implements BookingStore {
    private static final String COLUMNS_ORDER = " order by fecha, hora_inicio";
    private static final RowMapper<Booking> ROW = (rs, ctx) -> new Booking(
            SqlServerSchema.uuid(rs, "reserva_id"),
            SqlServerSchema.uuid(rs, "cancha_id"),
            SqlServerSchema.uuid(rs, "equipo_id"),
            new TimeSlot(rs.getObject("fecha", LocalDate.class), rs.getInt("hora_inicio"), rs.getInt("horas")),
            rs.getBigDecimal("precio"),
            BookingStatus.valueOf(rs.getString("estado")),
            rs.getObject("reservada_en", LocalDateTime.class),
            rs.getObject("cerrada_en", LocalDateTime.class));

    private final Jdbi jdbi;

    public SqlBookingStore(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    @Override
    public void save(Booking booking) {
        jdbi.useTransaction(handle -> {
            int changed = bindAll(handle.createUpdate("update reserva set cancha_id = :cancha, equipo_id = :equipo, fecha = :fecha, "
                    + "hora_inicio = :inicio, horas = :horas, precio = :precio, estado = :estado, reservada_en = :reservada, "
                    + "cerrada_en = :cerrada where reserva_id = :id"), booking).execute();
            if (changed == 0) {
                bindAll(handle.createUpdate("insert into reserva (reserva_id, cancha_id, equipo_id, fecha, hora_inicio, horas, precio, "
                        + "estado, reservada_en, cerrada_en) values (:id, :cancha, :equipo, :fecha, :inicio, :horas, :precio, :estado, "
                        + ":reservada, :cerrada)"), booking).execute();
            }
        });
    }

    private static <S extends SqlStatement<S>> S bindAll(S statement, Booking b) {
        return statement.bind("id", b.id())
                .bind("cancha", b.courtId())
                .bind("equipo", b.teamId())
                .bind("fecha", b.slot().date())
                .bind("inicio", b.slot().startHour())
                .bind("horas", b.slot().hours())
                .bind("precio", b.price())
                .bind("estado", b.status().name())
                .bind("reservada", b.bookedAt())
                .bindByType("cerrada", b.settledAt(), LocalDateTime.class);
    }

    @Override
    public Optional<Booking> findById(UUID id) {
        return jdbi.withHandle(h -> h.createQuery("select * from reserva where reserva_id = :id").bind("id", id).map(ROW).findOne());
    }

    @Override
    public List<Booking> findOn(LocalDate date) {
        return jdbi.withHandle(h -> h.createQuery("select * from reserva where fecha = :fecha" + COLUMNS_ORDER)
                .bind("fecha", date).map(ROW).list());
    }

    @Override
    public List<Booking> findBetween(LocalDate from, LocalDate to) {
        return jdbi.withHandle(h -> h.createQuery("select * from reserva where fecha between :desde and :hasta" + COLUMNS_ORDER)
                .bind("desde", from).bind("hasta", to).map(ROW).list());
    }

    @Override
    public List<Booking> findByTeam(UUID teamId) {
        return jdbi.withHandle(h -> h.createQuery("select * from reserva where equipo_id = :equipo" + COLUMNS_ORDER)
                .bind("equipo", teamId).map(ROW).list());
    }

    @Override
    public List<Booking> findByCourt(UUID courtId) {
        return jdbi.withHandle(h -> h.createQuery("select * from reserva where cancha_id = :cancha" + COLUMNS_ORDER)
                .bind("cancha", courtId).map(ROW).list());
    }
}
