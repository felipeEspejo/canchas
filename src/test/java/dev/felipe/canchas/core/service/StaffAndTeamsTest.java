package dev.felipe.canchas.core.service;

import dev.felipe.canchas.core.port.in.StaffDirectoryUseCase.NewStaff;
import dev.felipe.canchas.core.port.in.StaffDirectoryUseCase.StaffChange;
import dev.felipe.canchas.core.port.in.TeamRegistryUseCase.TeamCommand;
import dev.felipe.canchas.core.BusinessRuleException;
import dev.felipe.canchas.core.model.StaffAccount;
import dev.felipe.canchas.core.model.StaffRole;
import dev.felipe.canchas.core.model.Team;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StaffAndTeamsTest {
    private final TestComplex complex = new TestComplex();

    @Test
    void staffPasswordsAreHashedAndChecked() {
        StaffAccount admin = complex.app.staff().create(new NewStaff("admin", "secreto1", StaffRole.ADMIN));
        assertNotEquals("secreto1", admin.passwordHash());
        assertTrue(complex.app.staff().authenticate("ADMIN", "secreto1").isPresent());
        assertTrue(complex.app.staff().authenticate("admin", "otra").isEmpty());
        assertThrows(BusinessRuleException.class, () -> complex.app.staff().create(new NewStaff("admin", "secreto1", StaffRole.ADMIN)));
        assertThrows(BusinessRuleException.class, () -> complex.app.staff().create(new NewStaff("otro", "123", StaffRole.RECEPTION)));
    }

    @Test
    void lastAdminStaysAndNobodyDeletesThemselves() {
        StaffAccount admin = complex.app.staff().create(new NewStaff("admin", "secreto1", StaffRole.ADMIN));
        StaffAccount desk = complex.app.staff().create(new NewStaff("recepcion", "secreto1", StaffRole.RECEPTION));

        assertThrows(BusinessRuleException.class, () -> complex.app.staff().change(admin.id(), new StaffChange(StaffRole.RECEPTION, "")));
        assertThrows(BusinessRuleException.class, () -> complex.app.staff().remove(admin.id(), "recepcion"));
        assertThrows(BusinessRuleException.class, () -> complex.app.staff().remove(desk.id(), "RECEPCION"));

        complex.app.staff().change(desk.id(), new StaffChange(StaffRole.RECEPTION, "nueva123"));
        assertTrue(complex.app.staff().authenticate("recepcion", "nueva123").isPresent());
        complex.app.staff().remove(desk.id(), "admin");
        assertTrue(complex.app.staff().byUsername("recepcion").isEmpty());
    }

    @Test
    void teamNamesAreUniqueAndSearchable() {
        Team tigers = complex.team("Los Tigres FC");
        complex.team("Real Barrio");
        assertThrows(BusinessRuleException.class, () -> complex.team("los  tigres fc"));

        assertEquals(1, complex.app.teams().search("tig").size());
        assertEquals(2, complex.app.teams().search("").size());

        Team renamed = complex.app.teams().update(tigers.id(), new TeamCommand("Tigres del Norte", "Ana Pérez", "3110000000"));
        assertEquals("TIGRES DEL NORTE", renamed.name());
        assertThrows(BusinessRuleException.class,
                () -> complex.app.teams().update(tigers.id(), new TeamCommand("Real Barrio", "Ana Pérez", "3110000000")));
    }
}
