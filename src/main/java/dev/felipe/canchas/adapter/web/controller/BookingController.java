package dev.felipe.canchas.adapter.web.controller;

import dev.felipe.canchas.core.port.in.BookingDeskUseCase;
import dev.felipe.canchas.core.port.in.BookingDeskUseCase.BookingFilter;
import dev.felipe.canchas.core.port.in.BookingDeskUseCase.BookingRequest;
import dev.felipe.canchas.core.port.in.BookingDeskUseCase.BookingView;
import dev.felipe.canchas.core.port.in.CourtCatalogUseCase;
import dev.felipe.canchas.core.port.in.TeamRegistryUseCase;
import dev.felipe.canchas.core.BusinessRuleException;
import dev.felipe.canchas.core.model.Booking;
import dev.felipe.canchas.core.model.BookingStatus;
import dev.felipe.canchas.core.model.Court;
import dev.felipe.canchas.core.model.TimeSlot;
import dev.felipe.canchas.core.policy.PriceQuote;
import dev.felipe.canchas.core.policy.PricingPolicy;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Reservas: listado con filtros, nueva reserva con cotización y el detalle con sus acciones. */
@Controller
@RequestMapping("/reservas")
public class BookingController {
    private final BookingDeskUseCase desk;
    private final CourtCatalogUseCase courts;
    private final TeamRegistryUseCase teams;

    public BookingController(BookingDeskUseCase desk, CourtCatalogUseCase courts, TeamRegistryUseCase teams) {
        this.desk = desk;
        this.courts = courts;
        this.teams = teams;
    }

    @GetMapping
    public String list(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                       @RequestParam(required = false) UUID equipo,
                       @RequestParam(required = false) BookingStatus estado,
                       Model model) {
        LocalDate today = LocalDate.now();
        LocalDate from = desde == null ? today.minusDays(7) : desde;
        LocalDate to = hasta == null ? today.plusDays(14) : hasta;
        List<BookingView> found;
        try {
            found = desk.search(new BookingFilter(from, to, equipo, estado));
        } catch (BusinessRuleException e) {
            model.addAttribute("problem", e.getMessage());
            found = List.of();
        }
        model.addAttribute("bookings", found);
        model.addAttribute("desde", from);
        model.addAttribute("hasta", to);
        model.addAttribute("equipo", equipo);
        model.addAttribute("estado", estado);
        model.addAttribute("teams", teams.all());
        model.addAttribute("statuses", BookingStatus.values());
        model.addAttribute("reservedCount", count(found, BookingStatus.RESERVED));
        model.addAttribute("playedCount", count(found, BookingStatus.PLAYED));
        model.addAttribute("missedCount", count(found, BookingStatus.NO_SHOW) + count(found, BookingStatus.CANCELLED));
        model.addAttribute("collected", found.stream().map(v -> v.booking().revenue()).reduce(BigDecimal.ZERO, BigDecimal::add));
        return "reservas/lista";
    }

    /** Formulario de reserva. Si ya vienen cancha, fecha y hora (por ejemplo desde la agenda), muestra la cotización. */
    @GetMapping("/nueva")
    public String newBooking(@RequestParam(required = false) UUID cancha,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                             @RequestParam(required = false) Integer hora,
                             @RequestParam(defaultValue = "1") Integer horas,
                             @RequestParam(required = false) UUID equipo,
                             Model model) {
        LocalDate day = fecha == null ? LocalDate.now() : fecha;
        List<Court> active = courts.all().stream().filter(Court::active).toList();
        model.addAttribute("courts", active);
        model.addAttribute("teams", teams.all());
        model.addAttribute("hours", TimeSlot.openingHours());
        model.addAttribute("durations", List.of(1, 2, 3));
        model.addAttribute("cancha", cancha);
        model.addAttribute("fecha", day);
        model.addAttribute("hora", hora);
        model.addAttribute("horas", horas);
        model.addAttribute("equipo", equipo);
        if (cancha != null && hora != null) {
            try {
                PriceQuote quote = desk.quote(new BookingRequest(cancha, equipo, day, hora, horas));
                model.addAttribute("quote", quote);
                model.addAttribute("quoteSlot", new TimeSlot(day, hora, horas));
                active.stream().filter(c -> c.id().equals(cancha)).findFirst().ifPresent(c -> model.addAttribute("quoteCourt", c));
            } catch (BusinessRuleException e) {
                model.addAttribute("quoteProblem", e.getMessage());
            }
        }
        return "reservas/nueva";
    }

    @PostMapping
    public String book(@RequestParam(required = false) UUID cancha,
                       @RequestParam(required = false) UUID equipo,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                       @RequestParam(required = false) Integer hora,
                       @RequestParam(required = false) Integer horas,
                       RedirectAttributes redirect) {
        Booking booking = Feedback.result(redirect, "Reserva confirmada",
                () -> desk.book(new BookingRequest(cancha, equipo, fecha, hora, horas)));
        if (booking != null) {
            return "redirect:/reservas/" + booking.id();
        }
        return "redirect:" + UriComponentsBuilder.fromPath("/reservas/nueva")
                .queryParamIfPresent("cancha", java.util.Optional.ofNullable(cancha))
                .queryParamIfPresent("fecha", java.util.Optional.ofNullable(fecha))
                .queryParamIfPresent("hora", java.util.Optional.ofNullable(hora))
                .queryParamIfPresent("horas", java.util.Optional.ofNullable(horas))
                .queryParamIfPresent("equipo", java.util.Optional.ofNullable(equipo))
                .build().toUriString();
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable UUID id, Model model) {
        BookingView view = desk.view(id);
        Booking booking = view.booking();
        LocalDateTime now = LocalDateTime.now();
        boolean started = !now.isBefore(booking.slot().start());
        model.addAttribute("view", view);
        model.addAttribute("bands", booking.slot().playedHours().stream()
                .map(h -> new PriceQuote.HourCharge(h, PricingPolicy.bandOf(booking.slot().date(), h), null))
                .toList());
        model.addAttribute("canClose", booking.isPending() && started);
        model.addAttribute("canCancel", booking.isPending()
                && !now.isAfter(booking.slot().start().minusHours(Booking.CANCELLATION_NOTICE_HOURS)));
        model.addAttribute("noticeHours", Booking.CANCELLATION_NOTICE_HOURS);
        return "reservas/detalle";
    }

    @PostMapping("/{id}/jugada")
    public String played(@PathVariable UUID id, RedirectAttributes redirect) {
        Feedback.done(redirect, "Partido marcado como jugado: el valor quedó cobrado", () -> desk.markPlayed(id));
        return "redirect:/reservas/" + id;
    }

    @PostMapping("/{id}/no-show")
    public String noShow(@PathVariable UUID id, RedirectAttributes redirect) {
        Feedback.done(redirect, "Se registró la inasistencia del equipo", () -> desk.markNoShow(id));
        return "redirect:/reservas/" + id;
    }

    @PostMapping("/{id}/cancelar")
    public String cancel(@PathVariable UUID id, RedirectAttributes redirect) {
        Feedback.done(redirect, "Reserva cancelada: la cancha quedó libre", () -> desk.cancel(id));
        return "redirect:/reservas/" + id;
    }

    private static long count(List<BookingView> views, BookingStatus status) {
        return views.stream().filter(v -> v.booking().status() == status).count();
    }
}
