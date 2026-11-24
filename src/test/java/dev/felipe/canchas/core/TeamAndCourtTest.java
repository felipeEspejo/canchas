package dev.felipe.canchas.core;

import dev.felipe.canchas.core.model.Court;
import dev.felipe.canchas.core.model.CourtFormat;
import dev.felipe.canchas.core.model.Team;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TeamAndCourtTest {

    @Test
    void teamDataIsNormalized() {
        Team team = new Team(UUID.randomUUID(), "  atlético   ñañas ", " Ana  María ", "300 123-4567");
        assertEquals("ATLÉTICO ÑAÑAS", team.name());
        assertEquals("Ana María", team.captain());
        assertEquals("3001234567", team.phone());
    }

    @Test
    void teamRejectsBadData() {
        assertThrows(BusinessRuleException.class, () -> new Team(UUID.randomUUID(), "AB", "Ana María", "3001234567"));
        assertThrows(BusinessRuleException.class, () -> new Team(UUID.randomUUID(), "Equipo #1", "Ana María", "3001234567"));
        assertThrows(BusinessRuleException.class, () -> new Team(UUID.randomUUID(), "Los Tigres", "A1", "3001234567"));
        assertThrows(BusinessRuleException.class, () -> new Team(UUID.randomUUID(), "Los Tigres", "Ana María", "12345"));
    }

    @Test
    void courtIsOpenedActiveAndCanLeaveAndReturnToService() {
        Court court = Court.open(UUID.randomUUID(), " f5-a ", "Cancha  Norte", CourtFormat.FUTBOL_5);
        assertEquals("F5-A", court.code());
        assertEquals("Cancha Norte", court.name());
        assertTrue(court.active());

        Court off = court.deactivate();
        assertFalse(off.active());
        assertThrows(BusinessRuleException.class, off::deactivate);
        assertTrue(off.activate().active());
    }

    @Test
    void courtRejectsBadData() {
        assertThrows(BusinessRuleException.class, () -> Court.open(UUID.randomUUID(), "X", "Cancha Norte", CourtFormat.FUTBOL_5));
        assertThrows(BusinessRuleException.class, () -> Court.open(UUID.randomUUID(), "F5-A", "No", CourtFormat.FUTBOL_5));
        assertThrows(BusinessRuleException.class, () -> Court.open(UUID.randomUUID(), "F5-A", "Cancha Norte", null));
    }
}
