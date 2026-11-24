# Canchas — reservas de canchas sintéticas por franja horaria

Proyecto Integrador · Desarrollo Orientado a Servicios 2026-2

## El problema

Un complejo de canchas de fútbol 5 y fútbol 7 lleva sus reservas en un cuaderno: se cruzan dos equipos en la misma
cancha, no se sabe cuánto cobrar cuando el partido pasa de la tarde a la noche, los equipos que no llegan dejan la
cancha vacía sin consecuencia y nadie sabe qué franjas se venden más.

**La solución:** una agenda por franjas horarias. La recepción reserva una cancha para un equipo en un día y una hora
(de 1 a 3 horas), el sistema cotiza hora por hora según la franja (diurna, nocturna o fin de semana), no deja reservar
dos veces la misma cancha ni poner a un equipo en dos partidos a la vez, y al llegar la hora la reserva se cierra como
**jugada** (se cobra) o **no se presentó** (cuenta para la sanción). La administración maneja canchas, tarifas, personal
y el balance del periodo.

## Reglas del negocio (todas viven en `core`)

| Regla | Dónde |
|---|---|
| Se atiende de 8:00 a 23:00 y se reserva por horas completas, de 1 a 3 seguidas | `TimeSlot` |
| No se reserva una franja que ya empezó | `Booking.reserve` |
| Hora diurna: lunes a viernes antes de las 18:00. Nocturna: desde las 18:00 y sábados y domingos todo el día. Una reserva de 17:00 a 19:00 cobra una hora de cada una | `PricingPolicy` |
| La tarifa nocturna no puede ser menor que la diurna | `RatePlan` |
| El precio se fija al reservar: si después cambian las tarifas, la reserva conserva su precio | `BookingDesk.book` |
| Una cancha no se reserva dos veces a la misma hora, y un equipo no puede tener dos partidos que se crucen | `BookingDesk.book` |
| Se cancela hasta 2 horas antes; desde la hora de inicio se marca como jugada o como no presentada | `Booking` |
| Solo las reservas jugadas generan ingreso | `Booking.revenue` |
| 2 inasistencias en los últimos 30 días suspenden al equipo; nadie puede tener más de 3 reservas pendientes | `AttendancePolicy` |
| Una cancha no se borra: sale de servicio (si no tiene reservas pendientes) y conserva su historial. Su formato no cambia si ya tiene reservas | `Court`, `CourtCatalog` |
| El nombre del equipo es único (mayúsculas y un solo espacio: `los  tigres` = `LOS TIGRES`) | `Team`, `TeamRegistry` |
| Siempre queda al menos una cuenta de administración y nadie elimina su propia cuenta | `StaffDirectory` |

## Arquitectura hexagonal (puertos y adaptadores)

```
src/main/java/dev/felipe/canchas/
├── CanchasApp.java                 raíz de composición: conecta adaptadores de salida -> casos de uso -> adaptadores de entrada
├── AppSettings.java                configuración (variables de entorno o application.properties)
├── DemoData.java                   semana de ejemplo (solo si la base está vacía)
├── core/                           NÚCLEO: Java puro, sin Spring, sin JDBI, sin HTTP
│   ├── model/                      Court, Team, Booking, TimeSlot, RatePlan, StaffAccount y sus enums
│   ├── policy/                     PricingPolicy (precio por franja) y AttendancePolicy (sanciones y límites)
│   ├── port/in/                    PUERTOS DE ENTRADA: un caso de uso por interfaz, con sus comandos y vistas
│   │                               CourtCatalogUseCase, TeamRegistryUseCase, BookingDeskUseCase, ScheduleBoardUseCase,
│   │                               RateBookUseCase, RevenueReportUseCase, StaffDirectoryUseCase y UseCases (todos juntos)
│   ├── port/out/                   PUERTOS DE SALIDA: CourtStore, TeamStore, BookingStore, RatePlanStore,
│   │                               StaffAccountStore, PasswordHashing
│   ├── service/                    implementación de los casos de uso (solo conocen puertos)
│   └── BusinessRuleException, NotFoundException
└── adapter/                        ADAPTADORES (dependen del core, nunca al revés)
    ├── persistence/sqlserver/      Microsoft SQL Server con JDBI (API fluida) + creación del esquema
    ├── persistence/memory/         los mismos puertos en memoria (pruebas o correr sin base)
    ├── security/                   BCryptHashing
    ├── web/                        Spring MVC + Thymeleaf + Spring Security (WebServer, controladores)
    └── rest/                       API REST v1 con Javalin
```

Las pruebas siguen la misma estructura: `src/test/java/dev/felipe/canchas/core/...` y `.../adapter/persistence/`.

**Cómo fluye una reserva:** `BookingController` (web) o `RestApi` (REST) reciben la petición y llaman al puerto de
entrada `BookingDeskUseCase`. Su implementación, `BookingDesk`, usa las reglas del `core` y los puertos de salida
(`BookingStore`, `CourtStore`, ...). El `core` no sabe si detrás hay SQL Server o memoria, ni si lo llama una página o
un JSON.

**Cómo se conectan:** `CanchasApp.wire(...)` arma los casos de uso sobre cualquier implementación de los puertos de
salida. `WebServer` registra esos casos de uso como beans antes de arrancar Spring, así los controladores los reciben
por constructor sin conocer las clases concretas.

## Pantallas

| Ruta | Quién | Qué es |
|---|---|---|
| `/horarios` | Público, sin sesión | Canchas libres por hora para hoy y los próximos 6 días (solo cantidades, nunca equipos). Es lo que se ve al abrir `/` sin sesión |
| `/ingreso` | Personal | Inicio de sesión (no hay botón visible: se escribe la ruta) |
| `/agenda` | Recepción y administración | Canchas en filas y horas en columnas. Cada hora libre es un enlace para reservar; cada reserva ocupa tantas columnas como horas dure |
| `/reservas` | Recepción y administración | Listado con filtros (fechas, equipo, estado) y totales |
| `/reservas/nueva` | Recepción y administración | Paso 1: franja y equipo, y se cotiza. Paso 2: precio hora por hora y confirmación |
| `/reservas/{id}` | Recepción y administración | Ficha de la reserva con sus acciones: jugada, no se presentó, cancelar (según la hora) |
| `/equipos` y `/equipos/{id}` | Recepción y administración | Registro de equipos y ficha con historial, total pagado, pendientes e inasistencias (y si está sancionado) |
| `/canchas` | Administración | Abrir, editar, sacar de servicio y volver a servicio |
| `/tarifas` | Administración | Hora diurna y nocturna de cada formato |
| `/personal` | Administración | Cuentas de recepción y de administración |
| `/reportes` | Administración | Balance: ingresos cobrados, asistencia, horas por franja, reparto por formato, ingresos por día y equipos que más pagaron |

**Sin JavaScript:** los paneles de crear y editar se abren con `?nueva`, `?nuevo` o `?editar=<id>`. La cotización es un
formulario GET y la confirmación un POST. Las tarifas se editan con `<details>`. La agenda es una grilla CSS donde la
reserva ocupa `grid-column: span N`.

**Diseño "Estadio de noche":** fondo grafito, verde lima para acciones, ámbar para lo pendiente, números estilo
marcador. Las fuentes (Bebas Neue, Barlow y JetBrains Mono) vienen de Google Fonts. Hay un solo archivo:
`static/css/app.css`.

## Ejecutar

Requiere Java 21 y Microsoft SQL Server (ver la sección de base de datos para prepararlo una vez).

**En IntelliJ:** abre la carpeta del proyecto (la que contiene `build.gradle.kts`, no una carpeta de más arriba) y espera a
que cargue Gradle. Arriba a la derecha quedan dos configuraciones listas (vienen en la carpeta `.run/`):

- **Canchas (iniciar)**: arranca la aplicación (`bootRun`).
- **Canchas (pruebas)**: corre todas las pruebas (`test`).

**En la terminal:**

```bash
./gradlew bootRun     # web en http://localhost:8080  ·  REST en http://localhost:7070/api/v1
./gradlew test        # core (modelo, políticas y casos de uso) y adaptadores SQL
```

> `gradle.properties` arranca Gradle con `-Dfile.encoding=COMPAT`. Sin eso, en Windows las pruebas fallan con
> `ClassNotFoundException` si la ruta del proyecto tiene tildes (por ejemplo `...\Imágenes\...`): Gradle escribe la lista
> de clases en UTF-8 y Java la lee con la codificación del sistema.

Cuentas de prueba (se crean la primera vez):

| Usuario     | Contraseña     | Rol            |
|-------------|----------------|----------------|
| `admin`     | `admin123`     | Administración |
| `recepcion` | `recepcion123` | Recepción      |

Datos de ejemplo: 3 canchas de fútbol 5 y 2 de fútbol 7, tarifas, 6 equipos, una semana de partidos jugados, dos
inasistencias de ATLÉTICO OFICINA (queda sancionado), una cancelación y reservas para hoy y los próximos días.

## Base de datos (Microsoft SQL Server)

La conexión está en `application.properties` (`app.db.url`, `app.db.username`, `app.db.password`) y se puede cambiar
con las variables `DB_URL`, `DB_USER` y `DB_PASSWORD`.

**Preparar el servidor (una sola vez):**

1. Modo mixto (usuario y contraseña): en SSMS, *Propiedades del servidor* > *Seguridad* > *Modo de autenticación de SQL
   Server y de Windows*. Después hay que reiniciar el servicio.
2. TCP/IP habilitado (en *SQL Server Configuration Manager*). Para una instancia con nombre, el servicio **SQL Server
   Browser** debe estar iniciado.
3. Base y login:

   ```powershell
   sqlcmd -S localhost\MSSQLSERVER01 -E -C -i database\crear-base.sql
   ```

Las tablas se crean solas al arrancar (`SqlServerSchema.createMissingTables`), en orden y solo si no existen:

| Tabla | Contenido |
|---|---|
| `cancha` | `cancha_id`, `codigo` (único), `nombre`, `formato`, `activa` |
| `equipo` | `equipo_id`, `nombre` (único), `capitan`, `telefono` |
| `tarifa` | `formato` (clave), `valor_diurno`, `valor_nocturno` |
| `reserva` | `reserva_id`, `cancha_id` y `equipo_id` (llaves foráneas), `fecha`, `hora_inicio`, `horas`, `precio`, `estado`, `reservada_en`, `cerrada_en` |
| `cuenta_personal` | `cuenta_id`, `usuario` (único), `clave_hash`, `rol` |

Los ids son `uniqueidentifier` y los textos `nvarchar` (guardan tildes y ñ). Guardar es un `update` y, si no cambió
ninguna fila, un `insert`.

## API REST v1 (HTTP Basic con las cuentas del personal)

| Método | Ruta | Rol |
|---|---|---|
| GET | `/api/v1/agenda?fecha=yyyy-MM-dd`, `/api/v1/horarios?fecha=` | cualquiera |
| GET / POST | `/api/v1/equipos?q=`, `/api/v1/equipos` `{"name","captain","phone"}` | cualquiera |
| GET / PUT | `/api/v1/equipos/{id}` (ficha con historial) | cualquiera |
| POST | `/api/v1/reservas/cotizar` `{"courtId","date","startHour","hours"}` | cualquiera |
| POST | `/api/v1/reservas` `{"courtId","teamId","date","startHour","hours"}` | cualquiera |
| GET | `/api/v1/reservas?desde=&hasta=&equipo=&estado=`, `/api/v1/reservas/{id}` | cualquiera |
| POST | `/api/v1/reservas/{id}/jugada`, `/no-show`, `/cancelar` | cualquiera |
| GET | `/api/v1/canchas`, `/api/v1/tarifas` | cualquiera |
| POST / PUT | `/api/v1/canchas`, `/api/v1/canchas/{id}`, `/{id}/activar`, `/{id}/desactivar` | ADMIN |
| PUT | `/api/v1/tarifas/{FUTBOL_5 \| FUTBOL_7}` `{"diurna","nocturna"}` | ADMIN |
| GET | `/api/v1/balance?desde=&hasta=` | ADMIN |
| GET / POST / PUT / DELETE | `/api/v1/personal` `{"usuario","clave","rol"}` (sin hash de contraseña) | ADMIN |

Si una regla del negocio no se cumple, la respuesta es **422** con `{"error": "..."}`. Si algo no existe, **404**.

## Pruebas

| Capa | Pruebas |
|---|---|
| Dominio | `TimeSlotTest`, `BookingTest`, `PricingPolicyTest`, `TeamAndCourtTest` |
| Casos de uso (con los adaptadores en memoria y un reloj manual) | `BookingDeskTest`, `ScheduleAndReportTest`, `StaffAndTeamsTest` |
| Adaptadores SQL | `StoreContractTest`. Corre siempre en H2 con `MODE=MSSQLServer` (`H2AsSqlServerStoreTest`) y contra un SQL Server real (`RealSqlServerStoreTest`) si se definen `SQLSERVER_TEST_URL`, `SQLSERVER_TEST_USER` y `SQLSERVER_TEST_PASSWORD`. La base debe terminar en `_test`, porque se borran sus tablas |

## Pendiente

- Balance y comprobante en PDF: hoy muestran la página "VAR revisando" (función no disponible).
