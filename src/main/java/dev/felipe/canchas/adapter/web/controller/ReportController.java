package dev.felipe.canchas.adapter.web.controller;

import dev.felipe.canchas.core.port.in.RevenueReportUseCase;
import dev.felipe.canchas.core.BusinessRuleException;
import dev.felipe.canchas.core.model.CourtFormat;
import dev.felipe.canchas.core.policy.RateBand;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDate;

/** Solo administración: balance de un periodo (por defecto, los últimos 7 días). */
@Controller
@RequestMapping("/reportes")
public class ReportController {
    private final RevenueReportUseCase reports;

    public ReportController(RevenueReportUseCase reports) {
        this.reports = reports;
    }

    @GetMapping
    public String balance(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                          Model model) {
        LocalDate to = hasta == null ? LocalDate.now() : hasta;
        LocalDate from = desde == null ? to.minusDays(6) : desde;
        model.addAttribute("desde", from);
        model.addAttribute("hasta", to);
        model.addAttribute("formats", CourtFormat.values());
        model.addAttribute("bands", RateBand.values());
        try {
            model.addAttribute("report", reports.report(from, to));
        } catch (BusinessRuleException e) {
            model.addAttribute("problem", e.getMessage());
        }
        return "reportes/balance";
    }

    /** El PDF todavía no se genera: página de "función no disponible". */
    @GetMapping("/balance.pdf")
    public ModelAndView pdf() {
        return new ModelAndView("error/unavailable", HttpStatus.NOT_IMPLEMENTED);
    }
}
