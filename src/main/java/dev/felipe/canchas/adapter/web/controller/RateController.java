package dev.felipe.canchas.adapter.web.controller;

import dev.felipe.canchas.core.port.in.RateBookUseCase;
import dev.felipe.canchas.core.model.CourtFormat;
import dev.felipe.canchas.core.policy.PricingPolicy;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

/** Solo administración: tarifas diurna y nocturna de cada formato. */
@Controller
@RequestMapping("/tarifas")
public class RateController {
    private final RateBookUseCase rates;

    public RateController(RateBookUseCase rates) {
        this.rates = rates;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("plans", rates.all());
        model.addAttribute("nightFrom", PricingPolicy.NIGHT_STARTS_AT);
        return "tarifas/lista";
    }

    @PutMapping("/{formato}")
    public String update(@PathVariable CourtFormat formato, @RequestParam BigDecimal diurna, @RequestParam BigDecimal nocturna,
                         RedirectAttributes redirect) {
        Feedback.done(redirect, "Tarifas de " + formato.getLabel().toLowerCase() + " actualizadas (aplican a las reservas nuevas)",
                () -> rates.update(formato, diurna, nocturna));
        return "redirect:/tarifas";
    }
}
