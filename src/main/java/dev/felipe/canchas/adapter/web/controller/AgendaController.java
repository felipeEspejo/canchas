package dev.felipe.canchas.adapter.web.controller;

import dev.felipe.canchas.core.port.in.ScheduleBoardUseCase;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Agenda del día: canchas en filas y horas en columnas. Cada hora libre es un enlace para reservar. */
@Controller
public class AgendaController {
    private final ScheduleBoardUseCase schedule;

    public AgendaController(ScheduleBoardUseCase schedule) {
        this.schedule = schedule;
    }

    @GetMapping("/agenda")
    public String agenda(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha, Model model) {
        LocalDate today = LocalDate.now();
        LocalDate day = fecha == null ? today : fecha;
        model.addAttribute("agenda", schedule.agenda(day));
        model.addAttribute("day", day);
        model.addAttribute("today", today);
        model.addAttribute("currentHour", day.equals(today) ? LocalDateTime.now().getHour() : -1);
        return "agenda";
    }
}
