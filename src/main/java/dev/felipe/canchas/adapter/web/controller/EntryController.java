package dev.felipe.canchas.adapter.web.controller;

import dev.felipe.canchas.core.port.in.ScheduleBoardUseCase;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import dev.felipe.canchas.core.model.TimeSlot;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Puertas de entrada: la raíz, el ingreso del personal y los horarios públicos. */
@Controller
public class EntryController {
    private static final int PUBLIC_DAYS_AHEAD = 6;

    private final ScheduleBoardUseCase schedule;

    public EntryController(ScheduleBoardUseCase schedule) {
        this.schedule = schedule;
    }

    /** Sin sesión se ven los horarios públicos; el ingreso no tiene botón, se escribe /ingreso. */
    @GetMapping("/")
    public String root(Authentication auth) {
        boolean signedIn = auth != null && !(auth instanceof AnonymousAuthenticationToken);
        return signedIn ? "redirect:/agenda" : "redirect:/horarios";
    }

    @GetMapping("/ingreso")
    public String signIn() {
        return "ingreso";
    }

    /** Horas libres por formato para hoy y los próximos 6 días. Solo cantidades: nunca equipos. */
    @GetMapping("/horarios")
    public String publicSchedule(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                 Model model, HttpServletResponse response) {
        // Si ya pasó la última franja del día (22:00), el público ve desde mañana
        LocalDate today = LocalDateTime.now().getHour() >= TimeSlot.CLOSING_HOUR - 1 ? LocalDate.now().plusDays(1) : LocalDate.now();
        LocalDate day = fecha == null || fecha.isBefore(today) || fecha.isAfter(today.plusDays(PUBLIC_DAYS_AHEAD)) ? today : fecha;
        response.setHeader("Refresh", "60");
        model.addAttribute("availability", schedule.availability(day));
        model.addAttribute("day", day);
        model.addAttribute("week", today.datesUntil(today.plusDays(PUBLIC_DAYS_AHEAD + 1)).toList());
        return "publico/horarios";
    }
}
