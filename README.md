# Canchas – Proyecto Integrador

Desarrollo Orientado a Servicios 2026-2

| Integrante | GitHub |
|---|---|
| Juan Felipe Quintero Espejo| FelipeEspejo |


Repositorio: https://github.com/felipeEspejo/canchas

---

## Descripción del proyecto

Sistema web para administrar un complejo de canchas sintéticas de fútbol 5 y fútbol 7. La recepción reserva una
cancha para un equipo en un día y una hora (de 1 a 3 horas). El sistema cobra cada hora según su franja (diurna,
nocturna o fin de semana) y no deja cruzar reservas. Cuando llega la hora, la reserva se cierra como **jugada** (se
cobra) o como **no se presentó** (cuenta para sancionar al equipo).

**Problema que resuelve:** en un complejo que lleva las reservas en un cuaderno se cruzan dos equipos en la misma
cancha, no se sabe cuánto cobrar cuando el partido pasa de la tarde a la noche, los equipos que no llegan dejan la
cancha vacía sin consecuencia y nadie sabe qué franjas se venden más. El sistema lleva la agenda por franjas, calcula
el precio con una regla fija, sanciona a los equipos que faltan y genera el balance del periodo.

**Usuarios:**

| Usuario | Qué hace |
|---|---|
| Visitante (sin cuenta) | Consulta cuántas canchas libres hay por hora, hoy y los próximos 6 días, antes de llamar a reservar |
| Recepción | Maneja la agenda, cotiza y registra reservas, las cierra como jugadas o no presentadas, y registra equipos |
| Administración | Todo lo de recepción, más canchas, tarifas, cuentas del personal y el balance |

**Funcionalidades principales:**

- Agenda del día con las canchas en filas y las horas en columnas. Cada hora libre es un enlace para reservar.
- Reserva en dos pasos: primero se cotiza hora por hora y después se confirma. El precio queda fijo al reservar.
- Cierre de reservas: jugada, no se presentó o cancelada (hasta 2 horas antes).
- Historial de reservas con filtros por fechas, equipo y estado.
- Equipos con su ficha: partidos jugados, pendientes, inasistencias, total pagado y si está sancionado.
- Administración de canchas (abrir, editar, sacar de servicio y volver a servicio), tarifas y cuentas del personal.
- Balance por rango de fechas: ingresos, asistencia, horas por franja, reparto por formato, ingresos por día y los
  equipos que más pagaron.
- Página pública de horarios libres, sin datos privados (no muestra equipos).
- Inicio de sesión con roles. El login no tiene botón visible: se entra por la ruta `/ingreso`.
- API REST en JSON con los mismos datos, para que otros programas usen el sistema.

---

## Pasos para correr el proyecto

### Requisitos previos

| Requisito | Versión | Nota |
|---|---|---|
| JDK | 21 | Es la que pide `build.gradle.kts` (`JavaLanguageVersion.of(21)`) |
| Gradle | 9.6 | No hay que instalarlo: viene con el proyecto (`gradlew` / `gradlew.bat`) |
| Microsoft SQL Server | 2019 o superior (Express o Developer) | Con TCP/IP habilitado |
| SQL Server Management Studio (SSMS) | 20 o superior | Para crear la base y ver las tablas |
| IDE recomendado | IntelliJ IDEA | Community o Ultimate |
| Internet | — | Solo la primera vez, para descargar dependencias; y para las fuentes de Google Fonts |

### Base de datos (se prepara una sola vez)

1. **Modo mixto (usuario y contraseña).** En SSMS, conectarse con *Windows Authentication*, clic derecho en el
   servidor → *Properties* → *Security* → **SQL Server and Windows Authentication mode** → OK. Luego clic derecho en
   el servidor → *Restart*.
2. **TCP/IP.** En *SQL Server Configuration Manager* → *SQL Server Network Configuration* → *Protocols for (instancia)*
   → **TCP/IP: Enabled**. Si la instancia tiene nombre (por ejemplo `MSSQLSERVER01`), en *SQL Server Services* el
   servicio **SQL Server Browser** debe estar iniciado y en automático. Después reiniciar el servicio de SQL Server.
3. **Base y login.** Abrir `database/crear-base.sql` en SSMS y ejecutarlo (F5), o desde la terminal:

   ```powershell
   sqlcmd -S localhost\MSSQLSERVER01 -E -C -i database\crear-base.sql
   ```

   Crea la base `canchas`, el login `canchas` con contraseña `Canchas#2026dev` y el usuario dentro de la base con
   permiso para crear tablas.

Las **tablas no se crean a mano**: la aplicación las crea al arrancar (`SqlServerSchema.createMissingTables`), en orden
y solo si no existen. La primera vez `DataSeed` carga datos de ejemplo: 5 canchas, tarifas, 6 equipos, una semana de
partidos (jugados, inasistencias y una cancelación) y reservas para hoy y los próximos días.

### Archivo de configuración y variables de entorno

La configuración está en `src/main/resources/application.properties`:

```properties
server.port=8081
# permite PUT/DELETE desde formularios HTML
spring.mvc.hiddenmethod.filter.enabled=true

# Microsoft SQL Server
app.db.url=jdbc:sqlserver://localhost;instanceName=MSSQLSERVER01;databaseName=canchas;encrypt=true;trustServerCertificate=true
app.db.username=canchas
app.db.password=Canchas#2026dev

# Cuenta de administración que se crea la primera vez
app.admin.username=admin
app.admin.password=admin123

# API REST (Javalin)
app.rest.port=7071
```

Si SQL Server es la instancia por defecto (no tiene nombre), la URL va con el puerto:
`jdbc:sqlserver://localhost:1433;databaseName=canchas;encrypt=true;trustServerCertificate=true`.

Variables de entorno **opcionales** (si existen, tienen prioridad sobre el archivo):

| Variable | Valor por defecto | Para qué sirve |
|---|---|---|
| `DB_URL` | la de `app.db.url` | Dirección de SQL Server y nombre de la base |
| `DB_USER` | `canchas` | Usuario de la base |
| `DB_PASSWORD` | `Canchas#2026dev` | Contraseña de la base |
| `ADMIN_USER` | `admin` | Usuario administrador que se crea la primera vez |
| `ADMIN_PASSWORD` | `admin123` | Contraseña de ese administrador |
| `REST_PORT` | `7071` | Puerto de la API REST |

### Credenciales de prueba y puertos

| Qué | Usuario | Contraseña |
|---|---|---|
| Administración | `admin` | `admin123` |
| Recepción | `recepcion` | `recepcion123` |
| Base de datos SQL Server | `canchas` | `Canchas#2026dev` |

| Servidor | Puerto |
|---|---|
| Vistas web (Spring + Thymeleaf) | 8081 |
| API REST (Javalin) | 7071 |

Se usan 8081 y 7071 para que el proyecto pueda correr al mismo tiempo que otro que use 8080 y 7070.

### Ejecutar en IntelliJ IDEA

1. *File → Open* y elegir la carpeta del proyecto (la que contiene `build.gradle.kts`). Si pregunta, pulsar
   **Trust Project**.
2. Esperar a que Gradle descargue las dependencias (barra de progreso abajo a la derecha).
3. Revisar el JDK en *File → Project Structure → Project → SDK* (JDK 21).
4. Arriba a la derecha elegir **Canchas (iniciar)** y pulsar ▶. También se puede abrir
   `src/main/java/dev/felipe/canchas/Application.java` y pulsar el triángulo verde junto a `main`.

### Ejecutar desde la terminal

```bash
# Windows
gradlew.bat build      # descarga dependencias, compila y corre las pruebas
gradlew.bat bootRun    # ejecuta la aplicación

# Linux o Mac
./gradlew build
./gradlew bootRun
```

Para correr solo las pruebas: `gradlew.bat test` o `./gradlew test`. Mientras la aplicación corre, la consola se
queda en `EXECUTING`: es normal. Se apaga con `Ctrl+C`.

### URL una vez iniciado

| URL | Qué es |
|---|---|
| http://localhost:8081/horarios | Horarios libres públicos (también se ve al abrir http://localhost:8081 sin sesión) |
| http://localhost:8081/ingreso | Inicio de sesión del personal |
| http://localhost:8081/agenda | Agenda del día (con sesión) |
| http://localhost:7071/api/v1/canchas | API REST (pide usuario y contraseña con HTTP Basic) |

### Errores comunes al ejecutar por primera vez

| Error | Solución |
|---|---|
| `JAVA_HOME is set to an invalid directory` | `JAVA_HOME` apunta a un JDK que ya no existe. Apuntarlo a un JDK 21 (por ejemplo `C:\Users\usuario\.jdks\ms-21...`) y abrir una terminal nueva |
| `Login failed for user 'canchas'` | Falta el modo mixto (paso 1) o la contraseña no coincide con la de `crear-base.sql` |
| `The TCP/IP connection to the host localhost, port 1433 has failed` | TCP/IP apagado (paso 2), o se usó la URL con puerto para una instancia con nombre |
| Se demora y falla con `instanceName` | El servicio SQL Server Browser está apagado o el nombre de la instancia está mal escrito |
| `Cannot open database "canchas"` | Falta ejecutar `database/crear-base.sql` (paso 3) |
| `Failed to bind to 0.0.0.0:7071` o `Port 8081 was already in use` | Ya hay otra copia corriendo. Detenerla (`Ctrl+C` o el cuadro rojo en IntelliJ) y volver a ejecutar |
| Las letras se ven distintas | No hay internet: las fuentes vienen de Google Fonts. El sistema funciona igual |

---

## Estructura de carpetas

```
src/
├── main/
│   ├── java/dev/felipe/canchas/
│   │   ├── Application.java              punto de entrada: crea adaptadores y servicios y arranca los servidores
│   │   ├── DataSeed.java                 datos de ejemplo (solo si la base está vacía)
│   │   ├── core/                         NÚCLEO: Java puro, sin frameworks
│   │   │   ├── constant/                 BookingStatus, CourtFormat, StaffRole, RateBand
│   │   │   ├── entity/                   Booking, Court, Team, TimeSlot, RatePlan, StaffAccount
│   │   │   ├── dto/                      BookingDTO, BookingFilterDTO, BookingViewDTO, CourtDTO, TeamDTO,
│   │   │   │                             TeamRecordDTO, StaffDTO, StaffUpdateDTO, PriceQuoteDTO, HourChargeDTO,
│   │   │   │                             DayAgendaDTO, AgendaRowDTO, AgendaCellDTO, AvailabilityDTO,
│   │   │   │                             FormatAvailabilityDTO, HourAvailabilityDTO, RevenueReportDTO,
│   │   │   │                             DailyRevenueDTO, TeamRankingDTO
│   │   │   ├── exception/                BusinessRuleException y NotFoundException
│   │   │   ├── port/                     CourtRepository, TeamRepository, BookingRepository,
│   │   │   │                             RatePlanRepository, StaffAccountRepository, PasswordHasher
│   │   │   └── service/                  BookingService, CourtService, TeamService, ScheduleService, RateService,
│   │   │                                 ReportService, StaffService, PricingPolicy y AttendancePolicy
│   │   └── adapter/                      ADAPTADORES: conectan el núcleo con el exterior
│   │       ├── repository/
│   │       │   ├── sqlserver/            SqlServerCourtRepository, SqlServerTeamRepository,
│   │       │   │   │                     SqlServerBookingRepository, SqlServerRatePlanRepository,
│   │       │   │   │                     SqlServerStaffAccountRepository y SqlServerSchema (tablas)
│   │       │   │   └── dao/              CourtDAO, TeamDAO, BookingDAO, RatePlanDAO, StaffAccountDAO (SQL con JDBI)
│   │       │   └── inmemory/             repositorios en memoria (para pruebas)
│   │       ├── security/                 BCryptPasswordHasher
│   │       └── ui/
│   │           ├── javalin/              JavalinUI (API REST v1)
│   │           └── spring/               SpringUI, SpringConfig
│   │               ├── config/           SecurityConfig (login y roles) y WebErrors (páginas de error)
│   │               └── controller/       Agenda, Booking, Court, Entry, Rate, Report, Staff y TeamController
│   └── resources/
│       ├── application.properties
│       ├── static/css/app.css            única hoja de estilos (componentes reutilizables)
│       └── templates/
│           ├── fragments/layout.html     piezas comunes (cabecera, menú, mensajes, dinero)
│           ├── ingreso.html              inicio de sesión
│           ├── agenda.html               agenda del día
│           ├── publico/                  horarios libres
│           ├── reservas/                 listado, nueva reserva y detalle
│           ├── equipos/  canchas/  tarifas/  personal/  reportes/
│           └── error/                    400, 403, 404, 500 y función no disponible
└── test/java/dev/felipe/canchas/
    ├── core/entity/                      pruebas de las entidades (TimeSlot, Booking, Team y Court)
    ├── core/service/                     pruebas de los servicios con repositorios en memoria y reloj manual
    └── adapter/repository/sqlserver/     pruebas del SQL (H2 en modo SQL Server y SQL Server real)

database/crear-base.sql                   crea la base canchas y su login (una sola vez)
```

---

## Diagrama de arquitectura

El proyecto usa **arquitectura hexagonal (puertos y adaptadores)**:

- El **núcleo** (`core`) tiene las reglas de negocio y no depende de ningún framework.
- Los **puertos** (`core/port`) son interfaces que el núcleo define para lo que necesita del exterior: guardar
  datos y cifrar contraseñas.
- Los **adaptadores de salida** implementan esos puertos: repositorios de SQL Server con JDBI y BCrypt.
- Los **adaptadores de entrada** usan los servicios: los controladores Spring (vistas Thymeleaf) y `JavalinUI`
  (API REST). Ambos comparten los mismos servicios.
- **`Application`** es la raíz de composición: crea cada pieza y se la pasa al núcleo por el constructor
  (inyección de dependencias manual). `SpringConfig` le entrega a Spring esos mismos servicios.

Patrones usados: puertos y adaptadores, repositorio, DAO, DTO, inyección de dependencias por constructor, raíz de
composición y objetos inmutables (`record` con métodos que devuelven una copia nueva).

![Diagrama de arquitectura](docs/diagrama-arquitectura.png)

Diagrama de clases UML, de arriba hacia abajo: los controladores Spring y `JavalinUI` usan los servicios; los
servicios usan los puertos; los repositorios de SQL Server y BCrypt implementan esos puertos. Los atributos de cada
clase (`- desk: BookingService`, `- courts: CourtRepository`...) dicen exactamente qué recibe por el constructor. Las
flechas punteadas con punta abierta son dependencias y las punteadas con triángulo vacío son realizaciones (el
adaptador implementa la interfaz). Todas las dependencias apuntan hacia el núcleo.

El archivo editable está en [`docs/diagrama-arquitectura.drawio`](docs/diagrama-arquitectura.drawio) (se abre en
draw.io).

**Reglas del negocio (todas viven en `core`):**

| Regla | Dónde |
|---|---|
| Se atiende de 8:00 a 23:00 y se reserva por horas completas, de 1 a 3 seguidas | `TimeSlot` |
| No se reserva una franja que ya empezó | `Booking.reserve` |
| Hora diurna: lunes a viernes antes de las 18:00. Nocturna: desde las 18:00, y sábados y domingos todo el día. Una reserva de 17:00 a 19:00 cobra una hora de cada una | `PricingPolicy` |
| La tarifa nocturna no puede ser menor que la diurna | `RatePlan` |
| El precio se fija al reservar: si después cambian las tarifas, la reserva conserva su precio | `BookingService.book` |
| Una cancha no se reserva dos veces a la misma hora y un equipo no puede tener dos partidos que se crucen | `BookingService.book` |
| Se cancela hasta 2 horas antes; desde la hora de inicio se marca como jugada o como no presentada | `Booking` |
| Solo las reservas jugadas generan ingreso | `Booking.revenue` |
| 2 inasistencias en los últimos 30 días suspenden al equipo; nadie puede tener más de 3 reservas pendientes | `AttendancePolicy` |
| Una cancha no se borra: sale de servicio (si no tiene reservas pendientes) y conserva su historial. Su formato no cambia si ya tiene reservas | `Court`, `CourtService` |
| El nombre del equipo es único (mayúsculas y un solo espacio: `los  tigres` = `LOS TIGRES`) | `Team`, `TeamService` |
| Siempre queda al menos una cuenta de administración y nadie elimina su propia cuenta | `StaffService` |

---

## Modelo Entidad/Relación

![Modelo entidad relación](docs/modelo-entidad-relacion.png)

| Tabla | Llave primaria | Restricciones |
|---|---|---|
| `cancha` | `cancha_id` | `codigo` único |
| `equipo` | `equipo_id` | `nombre` único |
| `reserva` | `reserva_id` | `cancha_id` es llave foránea a `cancha` (`fk_reserva_cancha`) y `equipo_id` a `equipo` (`fk_reserva_equipo`) |
| `tarifa` | `formato` | Una tarifa por formato de cancha (`FUTBOL_5`, `FUTBOL_7`) |
| `cuenta_personal` | `cuenta_id` | `usuario` único |

Una cancha y un equipo pueden tener muchas reservas a lo largo del tiempo. La tarifa se relaciona con la cancha por el
formato, sin llave foránea: el valor cobrado queda guardado en `reserva.precio`. Los ids son `uniqueidentifier` y los
textos `nvarchar`, para guardar tildes y ñ.

El código fuente del diagrama (Mermaid) está en [`docs/modelo-entidad-relacion.mmd`](docs/modelo-entidad-relacion.mmd).

---

## Endpoints o peticiones disponibles

### Vistas web (Spring MVC + Thymeleaf, puerto 8081)

| Funcionalidad | Ruta | Método | Descripción |
|---|---|---|---|
| Horarios públicos | `/horarios?fecha=` | GET | Canchas libres por hora, hoy y los próximos 6 días, sin datos privados |
| Inicio | `/` | GET | Sin sesión redirige a `/horarios`; con sesión, a `/agenda` |
| Iniciar sesión | `/ingreso` | GET / POST | Formulario de login (el POST lo procesa Spring Security) |
| Cerrar sesión | `/salir` | POST | Termina la sesión |
| Agenda | `/agenda?fecha=` | GET | Canchas por horas del día; cada hora libre es un enlace para reservar |
| Reservas | `/reservas?desde=&hasta=&equipo=&estado=` | GET | Listado con filtros y totales (por defecto, 7 días atrás y 14 adelante) |
| Nueva reserva | `/reservas/nueva?cancha=&fecha=&hora=&horas=&equipo=` | GET | Paso 1: elegir franja y equipo. Con los datos completos muestra la cotización hora por hora |
| Confirmar reserva | `/reservas` | POST | Paso 2: crea la reserva con el precio cotizado |
| Detalle de reserva | `/reservas/{id}` | GET | Ficha de la reserva con sus acciones según la hora |
| Marcar jugada | `/reservas/{id}/jugada` | POST | Cierra la reserva y la cobra |
| No se presentó | `/reservas/{id}/no-show` | POST | Cierra la reserva sin cobro; cuenta para la sanción |
| Cancelar | `/reservas/{id}/cancelar` | POST | Solo hasta 2 horas antes |
| Equipos | `/equipos?q=` (`?nuevo`, `?editar={id}`) | GET | Lista con búsqueda y ventanas de registrar o editar |
| Registrar equipo | `/equipos` | POST | Nuevo equipo |
| Editar equipo | `/equipos/{id}` | PUT | Cambia nombre, capitán y teléfono |
| Ficha del equipo | `/equipos/{id}` | GET | Historial, pagado, pendientes, inasistencias y sanción |
| Canchas | `/canchas` (`?nueva`, `?editar={id}`) | GET | Lista y ventanas de abrir o editar |
| Abrir cancha | `/canchas` | POST | Nueva cancha en servicio |
| Editar cancha | `/canchas/{id}` | PUT | Cambia código, nombre y formato (el formato, solo si no tiene reservas) |
| Sacar de servicio / volver | `/canchas/{id}/desactivar`, `/canchas/{id}/activar` | POST | No se puede sacar si tiene reservas pendientes |
| Tarifas | `/tarifas` | GET | Hora diurna y nocturna de cada formato |
| Actualizar tarifa | `/tarifas/{formato}` | PUT | Nuevos valores; aplican a las reservas nuevas |
| Personal | `/personal` (`?nueva`, `?editar={id}`) | GET | Cuentas del personal y ventanas de crear o editar |
| Crear cuenta | `/personal` | POST | Nueva cuenta de recepción o administración |
| Editar cuenta | `/personal/{id}` | PUT | Cambia el rol y, opcionalmente, la contraseña |
| Eliminar cuenta | `/personal/{id}` | DELETE | No permite borrar la propia cuenta ni el último administrador |
| Balance | `/reportes?desde=&hasta=` | GET | Ingresos, asistencia y uso de las canchas (por defecto, últimos 7 días) |
| Balance en PDF | `/reportes/balance.pdf` | GET | Pendiente: responde "función no disponible" (501) |

`/canchas`, `/tarifas`, `/personal` y `/reportes` son solo para administración. Las demás rutas internas piden sesión
de recepción o administración. Los formularios HTML envían PUT y DELETE con el campo oculto `_method`.

### API REST v1 (Javalin, puerto 7071, autenticación HTTP Basic)

| Funcionalidad | Ruta | Método | Descripción |
|---|---|---|---|
| Agenda | `/api/v1/agenda?fecha=yyyy-MM-dd` | GET | Agenda del día |
| Horarios libres | `/api/v1/horarios?fecha=` | GET | Disponibilidad por formato y hora |
| Equipos | `/api/v1/equipos?q=` | GET | Búsqueda por parte del nombre |
| Registrar equipo | `/api/v1/equipos` | POST | `{"name":"Los Pumas","captain":"Ana Ruiz","phone":"3001234567"}`, responde 201 |
| Ficha / editar equipo | `/api/v1/equipos/{id}` | GET / PUT | Ficha con historial / cambia los datos |
| Cotizar | `/api/v1/reservas/cotizar` | POST | `{"courtId":"...","date":"2026-11-25","startHour":17,"hours":2}` |
| Reservar | `/api/v1/reservas` | POST | `{"courtId":"...","teamId":"...","date":"2026-11-25","startHour":17,"hours":2}`, responde 201 |
| Reservas | `/api/v1/reservas?desde=&hasta=&equipo=&estado=` | GET | Listado con filtros |
| Una reserva | `/api/v1/reservas/{id}` | GET | Reserva con su cancha y equipo |
| Cerrar reserva | `/api/v1/reservas/{id}/jugada`, `/no-show`, `/cancelar` | POST | Jugada, no se presentó o cancelada |
| Canchas | `/api/v1/canchas` | GET | Todas las canchas |
| Abrir / editar cancha | `/api/v1/canchas`, `/api/v1/canchas/{id}` | POST / PUT | `{"code":"F5-D","name":"Cancha Occidente","format":"FUTBOL_5"}` (ADMIN) |
| Activar / desactivar | `/api/v1/canchas/{id}/activar`, `/desactivar` | POST | (ADMIN) |
| Tarifas | `/api/v1/tarifas` | GET | Tarifa de cada formato |
| Actualizar tarifa | `/api/v1/tarifas/{FUTBOL_5 \| FUTBOL_7}` | PUT | `{"diurna":75000,"nocturna":99000}` (ADMIN) |
| Balance | `/api/v1/balance?desde=&hasta=` | GET | Balance del periodo (ADMIN) |
| Personal | `/api/v1/personal` | GET / POST | Sin hash de contraseña; `{"usuario":"caja2","clave":"clave123","rol":"RECEPTION"}` (ADMIN) |
| Editar / eliminar cuenta | `/api/v1/personal/{id}` | PUT / DELETE | `{"rol":"ADMIN","clave":"nueva123"}`; DELETE responde 204 (ADMIN) |

Respuestas de error: **422** si se rompe una regla del negocio (`{"error": "mensaje"}`), **400** si el JSON o un dato
no es válido, **401** sin credenciales válidas, **403** si recepción usa una ruta de administración y **404** si no
existe. Ejemplo: `curl -u admin:admin123 http://localhost:7071/api/v1/canchas`.

---

## Stack de tecnologías

**Versión de Java:** Java 21 (`toolchain` en `build.gradle.kts`). **Herramienta de construcción:** Gradle 9.6 con
Kotlin DSL. **Base de datos:** Microsoft SQL Server.

### Plugins de Gradle

| Plugin | Versión | Para qué |
|---|---|---|
| `java`, `application` | (incluidos en Gradle) | Compilar Java y la clase principal |
| `org.springframework.boot` | 4.1.0 | Spring Boot y la tarea `bootRun` |
| `io.spring.dependency-management` | 1.1.7 | Versiones compatibles de las dependencias de Spring |

### Dependencias de `build.gradle.kts`

| Dependencia | Versión | Para qué se usa |
|---|---|---|
| `spring-boot-starter-webmvc` | 4.1.0 (Spring MVC 7.0.8, Tomcat 11.0.22) | Servidor web y controladores |
| `spring-boot-starter-thymeleaf` | 4.1.0 (Thymeleaf 3.1.5) | Vistas HTML |
| `spring-boot-starter-security` | 4.1.0 (Spring Security 7.1.0) | Login, roles, CSRF y BCrypt |
| `thymeleaf-extras-springsecurity6` | 3.1.5.RELEASE | `sec:authorize` en las plantillas |
| `io.javalin:javalin` | 7.2.4 (Jetty 12.1.10) | API REST |
| `jackson-databind` | 2.21.4 | Conversión entre objetos Java y JSON |
| `jackson-datatype-jsr310` | 2.21.4 | Fechas (`LocalDate`, `LocalDateTime`) en JSON |
| `org.jdbi:jdbi3-core` | 3.55.0 | Acceso a la base con SQL |
| `org.jdbi:jdbi3-sqlobject` | 3.55.0 | DAOs como interfaces con `@SqlQuery` y `@SqlUpdate` |
| `com.microsoft.sqlserver:mssql-jdbc` | 13.4.0.jre11 | Driver de Microsoft SQL Server |
| `spring-boot-starter-test` | 4.1.0 (JUnit Jupiter 6.0.3) | Pruebas unitarias |
| `com.h2database:h2` (solo pruebas) | 2.4.240 | Probar el SQL en modo SQL Server sin instalar nada |

### Interfaz

| Tecnología | Uso |
|---|---|
| HTML con Thymeleaf | Plantillas con fragmentos reutilizables |
| CSS propio (`app.css`) | Una sola hoja con variables y componentes reutilizables; **sin JavaScript** |
| Google Fonts | Fuentes Bebas Neue, Barlow y JetBrains Mono |

---

## Capturas UI/UX

Capturas del sistema en ejecución con los datos de ejemplo, las mismas pantallas del video.

**Horarios libres** (público, sin iniciar sesión)

![Horarios públicos](docs/capturas/01-horarios-publico.png)

**Inicio de sesión**

![Ingreso](docs/capturas/02-ingreso.png)

**Agenda del día**

![Agenda](docs/capturas/03-agenda.png)

**Nueva reserva con la cotización hora por hora**

![Nueva reserva](docs/capturas/04-nueva-reserva-cotizacion.png)

**Reserva confirmada**

![Reserva confirmada](docs/capturas/05-reserva-confirmada.png)

**Listado de reservas**

![Reservas](docs/capturas/06-reservas.png)

**Reservas filtradas** (no se presentó)

![Reservas filtradas](docs/capturas/07-reservas-filtradas.png)

**Equipos**

![Equipos](docs/capturas/08-equipos.png)

**Ficha de un equipo sancionado** (dos inasistencias)

![Ficha del equipo](docs/capturas/09-ficha-equipo-sancionado.png)

**Registrar equipo**

![Registrar equipo](docs/capturas/10-registrar-equipo.png)

**Canchas del complejo**

![Canchas](docs/capturas/11-canchas.png)

**Abrir cancha**

![Abrir cancha](docs/capturas/12-abrir-cancha.png)

**Tarifas por franja**

![Tarifas](docs/capturas/13-tarifas.png)

**Personal**

![Personal](docs/capturas/14-personal.png)

**Crear cuenta**

![Crear cuenta](docs/capturas/15-crear-cuenta.png)

**Balance del periodo**

![Balance](docs/capturas/16-balance.png)

**Página no encontrada (404)**

![Error 404](docs/capturas/17-error-404.png)

**Recepción intentando entrar a administración (403)**

![Error 403](docs/capturas/18-error-403-recepcion.png)

**Horarios en el celular**

![Horarios en celular](docs/capturas/19-celular-horarios.png)

---

## Análisis personal

**Retos más grandes**

- Organizar el proyecto por capas. Al principio el código tenía otra estructura y la cambiamos a la misma del
  parqueadero: las reglas en el núcleo, las pantallas y la API afuera, y la base de datos detrás de los repositorios.
  Lo difícil fue mover todo sin dañar nada; las pruebas nos ayudaron a comprobarlo.
- Conectar la aplicación con SQL Server. Hubo que activar el usuario y la contraseña, encender TCP/IP y el servicio
  SQL Server Browser, crear la base con su login y escribir bien la dirección de conexión. Cada paso que faltaba daba
  un error distinto.
- Preparar los computadores: Java apuntaba a una versión que ya no existía y dos aplicaciones querían usar el mismo
  puerto. Aprendimos a leer el error y a encontrar la causa.
- Calcular bien el precio. Una reserva que empieza de tarde y termina de noche cobra una hora a cada tarifa, y si
  después cambian las tarifas, la reserva conserva el precio con el que se hizo.
- Que la agenda nunca se cruce: ni dos equipos en la misma cancha a la misma hora, ni un equipo en dos partidos al
  mismo tiempo.

**Aprendizajes más valiosos**

- Tener la lógica del negocio separada de lo demás vale la pena: la página web y la API usan las mismas reglas, y si
  mañana cambiamos de base de datos, solo cambia el adaptador.
- Validar los datos desde el principio evita muchos errores. Por ejemplo, no se puede crear una reserva de 4 horas ni
  registrar dos equipos con el mismo nombre.
- Las reglas del negocio también sirven para ser justos: un equipo que falta dos veces queda sancionado y nadie puede
  acaparar la agenda.
- Una interfaz clara se puede hacer con un solo archivo de estilos reutilizable y sin JavaScript.
- Hacer pruebas da tranquilidad. Cada vez que cambiábamos algo, las pruebas avisaban si habíamos dañado otra parte.

---

## Pendiente

- Balance y comprobante en PDF: hoy muestran la página "función no disponible".
## Pendiente

- Balance y comprobante en PDF: hoy muestran la página "VAR revisando" (función no disponible).
