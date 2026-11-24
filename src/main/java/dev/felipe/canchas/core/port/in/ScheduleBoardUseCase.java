package dev.felipe.canchas.core.port.in;

import dev.felipe.canchas.core.model.Court;
import dev.felipe.canchas.core.model.CourtFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Puerto de entrada: la agenda del día (interna) y los horarios libres (públicos). */
public interface ScheduleBoardUseCase {

    /** Agenda: una fila por cancha activa y una columna por hora de atención. */
    record DayAgenda(LocalDate date, List<Integer> hours, List<AgendaRow> rows,
                     int bookedHours, int openHours, int pendingCount, BigDecimal expectedRevenue) {
        public int occupancyPercent() {
            return openHours == 0 ? 0 : bookedHours * 100 / openHours;
        }
    }

    record AgendaRow(Court court, List<AgendaCell> cells) {
    }

    /** Celda de la agenda: libre (booking null, span 1) o una reserva que ocupa "span" horas seguidas. */
    record AgendaCell(int hour, BookingDeskUseCase.BookingView booking, int span, boolean past) {
        public boolean free() {
            return booking == null;
        }
    }

    /** Lo que ve el público: cuántas canchas libres hay por hora, sin equipos ni nombres. */
    record PublicAvailability(LocalDate date, List<FormatAvailability> formats) {
    }

    record FormatAvailability(CourtFormat format, int courts, List<HourAvailability> hours) {
        public long freeHours() {
            return hours.stream().filter(h -> !h.past()).mapToLong(HourAvailability::free).sum();
        }
    }

    record HourAvailability(int hour, int free, int total, boolean past) {
    }

    DayAgenda agenda(LocalDate date);

    PublicAvailability availability(LocalDate date);
}
