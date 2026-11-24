package dev.felipe.canchas.adapter.persistence;

import dev.felipe.canchas.adapter.persistence.sqlserver.SqlServerSchema;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Las mismas pruebas contra un SQL Server de verdad. Solo corre con SQLSERVER_TEST_URL, SQLSERVER_TEST_USER y
 * SQLSERVER_TEST_PASSWORD, y la base debe terminar en "_test" porque se borran sus tablas antes de cada prueba.
 */
@EnabledIfEnvironmentVariable(named = "SQLSERVER_TEST_URL", matches = ".+")
class RealSqlServerStoreTest extends StoreContractTest {
    @Override
    protected Jdbi emptyDatabase() {
        Jdbi jdbi = SqlServerSchema.connect(System.getenv("SQLSERVER_TEST_URL"),
                System.getenv("SQLSERVER_TEST_USER"), System.getenv("SQLSERVER_TEST_PASSWORD"));
        jdbi.useHandle(handle -> {
            String database = handle.createQuery("select db_name()").mapTo(String.class).one();
            if (!database.toLowerCase().endsWith("_test")) {
                throw new IllegalStateException("SQLSERVER_TEST_URL debe apuntar a una base de pruebas (terminada en _test), no a " + database);
            }
            for (String table : SqlServerSchema.tablesInDropOrder()) {
                handle.execute("if object_id('" + table + "', 'U') is not null drop table " + table);
            }
        });
        return jdbi;
    }
}
