package dev.felipe.canchas.adapter.web.controller;

import dev.felipe.canchas.core.port.in.CourtCatalogUseCase;
import dev.felipe.canchas.core.port.in.CourtCatalogUseCase.CourtCommand;
import dev.felipe.canchas.core.model.Court;
import dev.felipe.canchas.core.model.CourtFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

/** Solo administración: abrir canchas, editarlas y sacarlas de servicio (no se borran). */
@Controller
@RequestMapping("/canchas")
public class CourtController {
    private final CourtCatalogUseCase catalog;

    public CourtController(CourtCatalogUseCase catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String nueva, @RequestParam(required = false) UUID editar, Model model) {
        List<Court> all = catalog.all();
        model.addAttribute("courts", all);
        model.addAttribute("formats", CourtFormat.values());
        model.addAttribute("activeCount", all.stream().filter(Court::active).count());
        model.addAttribute("fiveCount", all.stream().filter(c -> c.active() && c.format() == CourtFormat.FUTBOL_5).count());
        model.addAttribute("sevenCount", all.stream().filter(c -> c.active() && c.format() == CourtFormat.FUTBOL_7).count());
        model.addAttribute("showCreate", nueva != null);
        model.addAttribute("editing", editar == null ? null : catalog.byId(editar).orElse(null));
        return "canchas/lista";
    }

    @PostMapping
    public String open(@RequestParam String codigo, @RequestParam String nombre, @RequestParam(required = false) CourtFormat formato,
                       RedirectAttributes redirect) {
        boolean saved = Feedback.done(redirect, "Cancha abierta", () -> catalog.open(new CourtCommand(codigo, nombre, formato)));
        return saved ? "redirect:/canchas" : "redirect:/canchas?nueva";
    }

    @PutMapping("/{id}")
    public String update(@PathVariable UUID id, @RequestParam String codigo, @RequestParam String nombre,
                         @RequestParam(required = false) CourtFormat formato, RedirectAttributes redirect) {
        boolean saved = Feedback.done(redirect, "Cancha actualizada", () -> catalog.update(id, new CourtCommand(codigo, nombre, formato)));
        return saved ? "redirect:/canchas" : "redirect:/canchas?editar=" + id;
    }

    @PostMapping("/{id}/activar")
    public String activate(@PathVariable UUID id, RedirectAttributes redirect) {
        Feedback.done(redirect, "La cancha volvió a servicio", () -> catalog.activate(id));
        return "redirect:/canchas";
    }

    @PostMapping("/{id}/desactivar")
    public String deactivate(@PathVariable UUID id, RedirectAttributes redirect) {
        Feedback.done(redirect, "La cancha quedó fuera de servicio", () -> catalog.deactivate(id));
        return "redirect:/canchas";
    }
}
