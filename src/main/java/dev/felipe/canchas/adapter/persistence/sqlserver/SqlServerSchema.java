package dev.felipe.canchas.adapter.persistence.sqlserver;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Conexión y esquema de SQL Server. Las tablas se crean una sola vez, en orden (las que tienen llaves foráneas al
 * final). Los nombres de tablas y columnas están en español.
 */
public final class SqlServerSchema {
    private static final Map<String, String> TABLES = new LinkedHashMap<>();

    static {
        TABLES.put("cancha", """
                create table cancha (
                    cancha_id uniqueidentifier primary key,
                    codigo nvarchar(10) not null unique,
                    nombre nvarchar(40) not null,
                    formato nvarchar(12) not null,
                    activa bit not null
                )""");
        TABLES.put("equipo", """
                create table equipo (
                    equipo_id uniqueidentifier primary key,
                    nombre nvarchar(30) not null unique,
                    capitan nvarchar(60) not null,
                    telefono nvarchar(16) not null
                )""");
        TABLES.put("tarifa", """
                create table tarifa (
                    formato nvarchar(12) primary key,
                    valor_diurno decimal(12,2) not null,
                    valor_nocturno decimal(12,2) not null
                )""");
        TABLES.put("reserva", """
                create table reserva (
                    reserva_id uniqueidentifier primary key,
                    cancha_id uniqueidentifier not null,
                    equipo_id uniqueidentifier not null,
                    fecha date not null,
                    hora_inicio int not null,
                    horas int not null,
                    precio decimal(12,2) not null,
                    estado nvarchar(12) not null,
                    reservada_en datetime2 not null,
                    cerrada_en datetime2 null,
                    constraint fk_reserva_cancha foreign key (cancha_id) references cancha(cancha_id),
                    constraint fk_reserva_equipo foreign key (equipo_id) references equipo(equipo_id)
                )""");
        TABLES.put("cuenta_personal", """
                create table cuenta_personal (
                    cuenta_id uniqueidentifier primary key,
                    usuario nvarchar(30) not null unique,
                    clave_hash nvarchar(100) not null,
                    rol nvarchar(16) not null
                )""");
    }

    private SqlServerSchema() {
    }

    public static Jdbi connect(String url, String username, String password) {
        return configure(Jdbi.create(url, username, password));
    }

    /** Los UUID viajan como texto: SQL Server los guarda en columnas uniqueidentifier y los convierte solo. */
    public static Jdbi configure(Jdbi jdbi) {
        jdbi.registerArgument(new AbstractArgumentFactory<UUID>(Types.VARCHAR) {
            @Override
            protected Argument build(UUID value, ConfigRegistry config) {
                return (position, statement, context) -> statement.setString(position, value.toString());
            }
        });
        return jdbi;
    }

    /** SQL Server no tiene "create table if not exists": se pregunta a information_schema si ya existe. */
    public static void createMissingTables(Jdbi jdbi) {
        jdbi.useHandle(handle -> TABLES.forEach((table, ddl) -> {
            int found = handle.createQuery("select count(*) from information_schema.tables where upper(table_name) = upper(:name)")
                    .bind("name", table)
                    .mapTo(Integer.class)
                    .one();
            if (found == 0) {
                handle.execute(ddl);
            }
        }));
    }

    /** Orden para borrar todo (primero las tablas que apuntan a otras). */
    public static String[] tablesInDropOrder() {
        return new String[]{"reserva", "cuenta_personal", "tarifa", "equipo", "cancha"};
    }

    static UUID uuid(ResultSet rs, String column) throws SQLException {
        String value = rs.getString(column);
        return value == null ? null : UUID.fromString(value);
    }
}
