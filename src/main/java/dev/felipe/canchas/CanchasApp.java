package dev.felipe.canchas;

import dev.felipe.canchas.core.port.in.UseCases;
import dev.felipe.canchas.core.port.out.BookingStore;
import dev.felipe.canchas.core.port.out.CourtStore;
import dev.felipe.canchas.core.port.out.PasswordHashing;
import dev.felipe.canchas.core.port.out.RatePlanStore;
import dev.felipe.canchas.core.port.out.StaffAccountStore;
import dev.felipe.canchas.core.port.out.TeamStore;
import dev.felipe.canchas.core.service.BookingDesk;
import dev.felipe.canchas.core.service.CourtCatalog;
import dev.felipe.canchas.core.service.RateBook;
import dev.felipe.canchas.core.service.RevenueAnalyst;
import dev.felipe.canchas.core.service.ScheduleBoard;
import dev.felipe.canchas.core.service.StaffDirectory;
import dev.felipe.canchas.core.service.TeamRegistry;
import dev.felipe.canchas.core.policy.AttendancePolicy;
import dev.felipe.canchas.core.policy.PricingPolicy;
import dev.felipe.canchas.adapter.persistence.sqlserver.SqlBookingStore;
import dev.felipe.canchas.adapter.persistence.sqlserver.SqlCourtStore;
import dev.felipe.canchas.adapter.persistence.sqlserver.SqlRatePlanStore;
import dev.felipe.canchas.adapter.persistence.sqlserver.SqlServerSchema;
import dev.felipe.canchas.adapter.persistence.sqlserver.SqlStaffAccountStore;
import dev.felipe.canchas.adapter.persistence.sqlserver.SqlTeamStore;
import dev.felipe.canchas.adapter.rest.RestApi;
import dev.felipe.canchas.adapter.security.BCryptHashing;
import dev.felipe.canchas.adapter.web.WebServer;
import org.jdbi.v3.core.ConnectionException;
import org.jdbi.v3.core.Jdbi;

import java.time.Clock;

/**
 * Punto de arranque y raíz de composición de la arquitectura hexagonal:
 *   adaptadores de salida (SQL Server, BCrypt) -> casos de uso (core) -> adaptadores de entrada (web y REST).
 * Es la única clase que conoce las implementaciones concretas.
 */
public final class CanchasApp {
    private CanchasApp() {
    }

    public static void main(String[] args) {
        AppSettings settings = new AppSettings();
        Clock clock = Clock.systemDefaultZone();

        // 1. Adaptadores de salida
        Jdbi jdbi = SqlServerSchema.connect(
                settings.get("DB_URL", "app.db.url", "jdbc:sqlserver://localhost:1433;databaseName=canchas;encrypt=true;trustServerCertificate=true"),
                settings.get("DB_USER", "app.db.username", "canchas"),
                settings.get("DB_PASSWORD", "app.db.password", ""));
        verifyConnection(jdbi);
        SqlServerSchema.createMissingTables(jdbi);

        CourtStore courtStore = new SqlCourtStore(jdbi);
        TeamStore teamStore = new SqlTeamStore(jdbi);
        BookingStore bookingStore = new SqlBookingStore(jdbi);
        RatePlanStore rateStore = new SqlRatePlanStore(jdbi);
        StaffAccountStore staffStore = new SqlStaffAccountStore(jdbi);
        PasswordHashing hashing = new BCryptHashing();

        // 2. Casos de uso (reciben solo puertos)
        UseCases useCases = wire(courtStore, teamStore, bookingStore, rateStore, staffStore, hashing, clock);

        // 3. Datos de demostración, solo si la base está vacía
        new DemoData(useCases, staffStore, courtStore, teamStore, bookingStore, rateStore,
                settings.get("ADMIN_USER", "app.admin.username", "admin"),
                settings.get("ADMIN_PASSWORD", "app.admin.password", "admin123")).loadIfEmpty();

        // 4. Adaptadores de entrada
        new RestApi(useCases).start(settings.getInt("REST_PORT", "app.rest.port", 7070));
        new WebServer(useCases).start(args);
    }

    /** Arma los casos de uso sobre cualquier implementación de los puertos de salida (también la de memoria). */
    static UseCases wire(CourtStore courts, TeamStore teams, BookingStore bookings, RatePlanStore rates,
                         StaffAccountStore staff, PasswordHashing hashing, Clock clock) {
        AttendancePolicy attendance = new AttendancePolicy();
        return new UseCases(
                new CourtCatalog(courts, bookings),
                new TeamRegistry(teams, bookings, courts, attendance, clock),
                new BookingDesk(courts, teams, bookings, rates, new PricingPolicy(), attendance, clock),
                new ScheduleBoard(courts, teams, bookings, clock),
                new RateBook(rates),
                new RevenueAnalyst(bookings, courts, teams),
                new StaffDirectory(staff, hashing));
    }

    private static void verifyConnection(Jdbi jdbi) {
        try {
            jdbi.useHandle(handle -> handle.execute("select 1"));
        } catch (ConnectionException e) {
            throw new IllegalStateException("""
                    No se pudo conectar a SQL Server. Revisa:
                      1) que el servicio de SQL Server (y SQL Server Browser si usas instanceName) esté iniciado con TCP/IP,
                      2) que exista la base 'canchas' y su login (database/crear-base.sql),
                      3) que el servidor acepte usuario y contraseña (modo mixto),
                      4) app.db.url, app.db.username y app.db.password en application.properties.
                    Causa: """ + e.getCause().getMessage(), e);
        }
    }
}
