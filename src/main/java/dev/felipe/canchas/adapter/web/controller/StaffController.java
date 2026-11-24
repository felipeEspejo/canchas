package dev.felipe.canchas.adapter.web.controller;

import dev.felipe.canchas.core.port.in.StaffDirectoryUseCase;
import dev.felipe.canchas.core.port.in.StaffDirectoryUseCase.NewStaff;
import dev.felipe.canchas.core.port.in.StaffDirectoryUseCase.StaffChange;
import dev.felipe.canchas.core.BusinessRuleException;
import dev.felipe.canchas.core.model.StaffAccount;
import dev.felipe.canchas.core.model.StaffRole;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

/** Solo administración: cuentas de recepción y de administración. */
@Controller
@RequestMapping("/personal")
public class StaffController {
    private final StaffDirectoryUseCase staff;

    public StaffController(StaffDirectoryUseCase staff) {
        this.staff = staff;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String nueva, @RequestParam(required = false) UUID editar,
                       Model model, Principal principal) {
        List<StaffAccount> all = staff.all();
        model.addAttribute("accounts", all);
        model.addAttribute("roles", StaffRole.values());
        model.addAttribute("me", principal.getName());
        model.addAttribute("adminCount", all.stream().filter(a -> a.role() == StaffRole.ADMIN).count());
        model.addAttribute("receptionCount", all.stream().filter(a -> a.role() == StaffRole.RECEPTION).count());
        model.addAttribute("showCreate", nueva != null);
        model.addAttribute("editing", editar == null ? null : staff.byId(editar).orElse(null));
        return "personal/lista";
    }

    @PostMapping
    public String create(@RequestParam String usuario, @RequestParam(required = false) StaffRole rol, @RequestParam String clave,
                         @RequestParam String confirmacion, RedirectAttributes redirect) {
        boolean saved = Feedback.done(redirect, "Cuenta creada", () -> {
            sameKey(clave, confirmacion);
            staff.create(new NewStaff(usuario, clave, rol));
        });
        return saved ? "redirect:/personal" : "redirect:/personal?nueva";
    }

    @PutMapping("/{id}")
    public String change(@PathVariable UUID id, @RequestParam(required = false) StaffRole rol,
                         @RequestParam(defaultValue = "") String clave, @RequestParam(defaultValue = "") String confirmacion,
                         RedirectAttributes redirect) {
        boolean saved = Feedback.done(redirect, "Cuenta actualizada", () -> {
            sameKey(clave, confirmacion);
            staff.change(id, new StaffChange(rol, clave));
        });
        return saved ? "redirect:/personal" : "redirect:/personal?editar=" + id;
    }

    @DeleteMapping("/{id}")
    public String remove(@PathVariable UUID id, Principal principal, RedirectAttributes redirect) {
        Feedback.done(redirect, "Cuenta eliminada", () -> staff.remove(id, principal.getName()));
        return "redirect:/personal";
    }

    private static void sameKey(String key, String confirmation) {
        if (!key.equals(confirmation)) {
            throw new BusinessRuleException("La contraseña y su confirmación no coinciden");
        }
    }
}
