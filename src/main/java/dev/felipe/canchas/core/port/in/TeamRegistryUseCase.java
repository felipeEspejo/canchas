package dev.felipe.canchas.core.port.in;

import dev.felipe.canchas.core.model.Team;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Puerto de entrada: registrar equipos y consultar su historial. */
public interface TeamRegistryUseCase {

    record TeamCommand(String name, String captain, String phone) {
    }

    /** Ficha del equipo con sus números y si está sancionado por no presentarse. */
    record TeamRecord(
            Team team,
            List<BookingDeskUseCase.BookingView> bookings,
            long played,
            long noShows,
            long cancelled,
            long pending,
            BigDecimal spent,
            boolean sanctioned
    ) {
    }

    Team register(TeamCommand command);

    Team update(UUID teamId, TeamCommand command);

    List<Team> all();

    /** Busca por parte del nombre (sin importar mayúsculas). Texto vacío = todos. */
    List<Team> search(String text);

    TeamRecord record(UUID teamId);
}
