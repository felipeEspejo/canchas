package dev.felipe.canchas.adapter.web.controller;

import dev.felipe.canchas.core.port.in.TeamRegistryUseCase;
import dev.felipe.canchas.core.port.in.TeamRegistryUseCase.TeamCommand;
import dev.felipe.canchas.core.model.Team;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

/** Equipos clientes: registro, edición y ficha con su historial. */
@Controller
@RequestMapping("/equipos")
public class TeamController {
    private final TeamRegistryUseCase teams;

    public TeamController(TeamRegistryUseCase teams) {
        this.teams = teams;
    }

    /** ?nuevo abre el panel de registro y ?editar=<id> el de edición (lo dibuja el servidor). */
    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) String nuevo,
                       @RequestParam(required = false) UUID editar,
                       Model model) {
        model.addAttribute("teams", teams.search(q));
        model.addAttribute("q", q);
        model.addAttribute("total", teams.all().size());
        model.addAttribute("showCreate", nuevo != null);
        model.addAttribute("editing", editar == null ? null : teams.record(editar).team());
        return "equipos/lista";
    }

    @GetMapping("/{id}")
    public String record(@PathVariable UUID id, Model model) {
        model.addAttribute("record", teams.record(id));
        return "equipos/ficha";
    }

    @PostMapping
    public String register(@RequestParam String nombre, @RequestParam String capitan, @RequestParam String telefono,
                           RedirectAttributes redirect) {
        Team team = Feedback.result(redirect, "Equipo registrado", () -> teams.register(new TeamCommand(nombre, capitan, telefono)));
        return team == null ? "redirect:/equipos?nuevo" : "redirect:/equipos/" + team.id();
    }

    @PutMapping("/{id}")
    public String update(@PathVariable UUID id, @RequestParam String nombre, @RequestParam String capitan,
                         @RequestParam String telefono, RedirectAttributes redirect) {
        boolean saved = Feedback.done(redirect, "Datos del equipo actualizados",
                () -> teams.update(id, new TeamCommand(nombre, capitan, telefono)));
        return saved ? "redirect:/equipos/" + id : "redirect:/equipos?editar=" + id;
    }
}
