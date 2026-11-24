package dev.felipe.canchas.adapter.persistence;

import dev.felipe.canchas.adapter.persistence.sqlserver.SqlServerSchema;
import org.jdbi.v3.core.Jdbi;

import java.util.UUID;

/** Siempre corre: H2 en memoria con MODE=MSSQLServer, una base nueva por prueba (no hace falta instalar nada). */
class H2AsSqlServerStoreTest extends StoreContractTest {
    @Override
    protected Jdbi emptyDatabase() {
        return SqlServerSchema.connect("jdbc:h2:mem:canchas" + UUID.randomUUID() + ";MODE=MSSQLServer;DB_CLOSE_DELAY=-1", "sa", "");
    }
}
