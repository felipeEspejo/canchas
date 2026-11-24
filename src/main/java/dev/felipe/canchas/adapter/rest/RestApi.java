package dev.felipe.canchas.adapter.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.felipe.canchas.core.port.in.BookingDeskUseCase.BookingFilter;
import dev.felipe.canchas.core.port.in.BookingDeskUseCase.BookingRequest;
import dev.felipe.canchas.core.port.in.CourtCatalogUseCase.CourtCommand;
import dev.felipe.canchas.core.port.in.StaffDirectoryUseCase.NewStaff;
import dev.felipe.canchas.core.port.in.StaffDirectoryUseCase.StaffChange;
import dev.felipe.canchas.core.port.in.TeamRegistryUseCase.TeamCommand;
import dev.felipe.canchas.core.port.in.UseCases;
import dev.felipe.canchas.core.BusinessRuleException;
import dev.felipe.canchas.core.NotFoundException;
import dev.felipe.canchas.core.model.BookingStatus;
import dev.felipe.canchas.core.model.CourtFormat;
import dev.felipe.canchas.core.model.StaffAccount;
import dev.felipe.canchas.core.model.StaffRole;
import dev.felipe.canchas.core.policy.PriceQuote;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.HttpStatus;
import io.javalin.http.UnauthorizedResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.UUID;

/**
 * Adaptador de entrada REST (Javalin), versión 1. Traduce HTTP/JSON a llamadas de los casos de uso.
 * Autenticación HTTP Basic con las mismas cuentas del personal; lo administrativo exige rol ADMIN.
 */
public class RestApi {
    private static final String ACCOUNT = "account";

    private final UseCases app;
    private final ObjectMapper json = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(MapperFeature.AUTO_DETECT_IS_GETTERS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    /** Cuerpos JSON que recibe la API. */
    record RatesBody(BigDecimal diurna, BigDecimal nocturna) {
    }

    record StaffBody(String usuario, String clave, StaffRole rol) {
    }

    public RestApi(UseCases app) {
        this.app = app;
    }

    public Javalin start(int port) {
        return Javalin.create(config -> {
            config.routes.before("/api/*", this::signIn);

            // agenda y disponibilidad
            config.routes.get("/api/v1/agenda", ctx -> send(ctx, app.schedule().agenda(date(ctx, "fecha", LocalDate.now()))));
            config.routes.get("/api/v1/horarios", ctx -> send(ctx, app.schedule().availability(date(ctx, "fecha", LocalDate.now()))));

            // equipos
            config.routes.get("/api/v1/equipos", ctx -> send(ctx, app.teams().search(ctx.queryParam("q"))));
            config.routes.get("/api/v1/equipos/{id}", ctx -> send(ctx, app.teams().record(id(ctx))));
            config.routes.post("/api/v1/equipos", ctx -> created(ctx, app.teams().register(read(ctx, TeamCommand.class))));
            config.routes.put("/api/v1/equipos/{id}", ctx -> send(ctx, app.teams().update(id(ctx), read(ctx, TeamCommand.class))));

            // reservas
            config.routes.post("/api/v1/reservas/cotizar", ctx -> {
                PriceQuote quote = app.bookings().quote(read(ctx, BookingRequest.class));
                send(ctx, Map.of("charges", quote.charges(), "total", quote.total()));
            });
            config.routes.post("/api/v1/reservas", ctx -> created(ctx, app.bookings().book(read(ctx, BookingRequest.class))));
            config.routes.get("/api/v1/reservas", ctx -> send(ctx, app.bookings().search(new BookingFilter(
                    date(ctx, "desde", null), date(ctx, "hasta", null), optionalId(ctx, "equipo"), status(ctx)))));
            config.routes.get("/api/v1/reservas/{id}", ctx -> send(ctx, app.bookings().view(id(ctx))));
            config.routes.post("/api/v1/reservas/{id}/jugada", ctx -> send(ctx, app.bookings().markPlayed(id(ctx))));
            config.routes.post("/api/v1/reservas/{id}/no-show", ctx -> send(ctx, app.bookings().markNoShow(id(ctx))));
            config.routes.post("/api/v1/reservas/{id}/cancelar", ctx -> send(ctx, app.bookings().cancel(id(ctx))));

            // canchas (consultar: todos; cambiar: ADMIN)
            config.routes.get("/api/v1/canchas", ctx -> send(ctx, app.courts().all()));
            config.routes.post("/api/v1/canchas", ctx -> {
                admin(ctx);
                created(ctx, app.courts().open(read(ctx, CourtCommand.class)));
            });
            config.routes.put("/api/v1/canchas/{id}", ctx -> {
                admin(ctx);
                send(ctx, app.courts().update(id(ctx), read(ctx, CourtCommand.class)));
            });
            config.routes.post("/api/v1/canchas/{id}/activar", ctx -> {
                admin(ctx);
                send(ctx, app.courts().activate(id(ctx)));
            });
            config.routes.post("/api/v1/canchas/{id}/desactivar", ctx -> {
                admin(ctx);
                send(ctx, app.courts().deactivate(id(ctx)));
            });

            // tarifas
            config.routes.get("/api/v1/tarifas", ctx -> send(ctx, app.rates().all()));
            config.routes.put("/api/v1/tarifas/{formato}", ctx -> {
                admin(ctx);
                RatesBody body = read(ctx, RatesBody.class);
                send(ctx, app.rates().update(format(ctx), body.diurna(), body.nocturna()));
            });

            // balance
            config.routes.get("/api/v1/balance", ctx -> {
                admin(ctx);
                LocalDate to = date(ctx, "hasta", LocalDate.now());
                send(ctx, app.reports().report(date(ctx, "desde", to.minusDays(6)), to));
            });

            // personal (nunca se devuelve el hash de la contraseña)
            config.routes.get("/api/v1/personal", ctx -> {
                admin(ctx);
                send(ctx, app.staff().all().stream().map(RestApi::publicView).toList());
            });
            config.routes.post("/api/v1/personal", ctx -> {
                admin(ctx);
                StaffBody body = read(ctx, StaffBody.class);
                created(ctx, publicView(app.staff().create(new NewStaff(body.usuario(), body.clave(), body.rol()))));
            });
            config.routes.put("/api/v1/personal/{id}", ctx -> {
                admin(ctx);
                StaffBody body = read(ctx, StaffBody.class);
                send(ctx, publicView(app.staff().change(id(ctx), new StaffChange(body.rol(), body.clave()))));
            });
            config.routes.delete("/api/v1/personal/{id}", ctx -> {
                admin(ctx);
                StaffAccount me = ctx.attribute(ACCOUNT);
                app.staff().remove(id(ctx), me.username());
                ctx.status(HttpStatus.NO_CONTENT);
            });

            config.routes.exception(NotFoundException.class, (e, ctx) -> problem(ctx, HttpStatus.NOT_FOUND, e.getMessage()));
            config.routes.exception(BusinessRuleException.class, (e, ctx) -> problem(ctx, HttpStatus.UNPROCESSABLE_CONTENT, e.getMessage()));
            config.routes.exception(JsonProcessingException.class, (e, ctx) -> problem(ctx, HttpStatus.BAD_REQUEST, "El cuerpo no es un JSON válido"));
            config.routes.exception(IllegalArgumentException.class, (e, ctx) -> problem(ctx, HttpStatus.BAD_REQUEST, e.getMessage()));
        }).start(port);
    }

    private void signIn(Context ctx) {
        var credentials = ctx.basicAuthCredentials();
        if (credentials == null) {
            ctx.header("WWW-Authenticate", "Basic realm=\"canchas\"");
            throw new UnauthorizedResponse("Faltan las credenciales");
        }
        StaffAccount account = app.staff().authenticate(credentials.getUsername(), credentials.getPassword())
                .orElseThrow(() -> new UnauthorizedResponse("Usuario o contraseña incorrectos"));
        ctx.attribute(ACCOUNT, account);
    }

    private void admin(Context ctx) {
        StaffAccount account = ctx.attribute(ACCOUNT);
        if (account == null || account.role() != StaffRole.ADMIN) {
            throw new ForbiddenResponse("Solo administración puede hacer esto");
        }
    }

    private static Map<String, Object> publicView(StaffAccount account) {
        return Map.of("id", account.id(), "usuario", account.username(), "rol", account.role());
    }

    private <T> T read(Context ctx, Class<T> type) throws JsonProcessingException {
        return json.readValue(ctx.body(), type);
    }

    private void send(Context ctx, Object body) {
        try {
            ctx.contentType("application/json").result(json.writeValueAsString(body));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private void created(Context ctx, Object body) {
        ctx.status(HttpStatus.CREATED);
        send(ctx, body);
    }

    private void problem(Context ctx, HttpStatus status, String message) {
        ctx.status(status);
        send(ctx, Map.of("error", message == null ? status.getMessage() : message));
    }

    private static UUID id(Context ctx) {
        try {
            return UUID.fromString(ctx.pathParam("id"));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("El id no es válido");
        }
    }

    private static UUID optionalId(Context ctx, String name) {
        String value = ctx.queryParam(name);
        try {
            return value == null || value.isBlank() ? null : UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("El parámetro '" + name + "' no es un id válido");
        }
    }

    private static LocalDate date(Context ctx, String name, LocalDate fallback) {
        String value = ctx.queryParam(name);
        try {
            return value == null || value.isBlank() ? fallback : LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("La fecha '" + name + "' debe tener formato yyyy-MM-dd");
        }
    }

    private static BookingStatus status(Context ctx) {
        String value = ctx.queryParam("estado");
        try {
            return value == null || value.isBlank() ? null : BookingStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Estado no válido: usa RESERVED, PLAYED, NO_SHOW o CANCELLED");
        }
    }

    private static CourtFormat format(Context ctx) {
        try {
            return CourtFormat.valueOf(ctx.pathParam("formato").toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Formato no válido: usa FUTBOL_5 o FUTBOL_7");
        }
    }
}
