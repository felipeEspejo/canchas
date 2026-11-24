package dev.felipe.canchas.core.port.in;

import dev.felipe.canchas.core.model.CourtFormat;
import dev.felipe.canchas.core.policy.RateBand;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Puerto de entrada: balance de un periodo (ingresos, asistencia y uso de las canchas). */
public interface RevenueReportUseCase {

    record RevenueReport(
            LocalDate from,
            LocalDate to,
            BigDecimal total,
            long played,
            long noShows,
            long cancelled,
            Map<CourtFormat, BigDecimal> byFormat,
            Map<RateBand, Long> hoursByBand,
            List<DayRevenue> days,
            List<TeamRanking> topTeams,
            int occupancyPercent
    ) {
        /** De las reservas que llegaron a su hora, qué porcentaje no se presentó. */
        public int noShowPercent() {
            long due = played + noShows;
            return due == 0 ? 0 : (int) (noShows * 100 / due);
        }

        public BigDecimal averageTicket() {
            return played == 0 ? BigDecimal.ZERO : total.divide(BigDecimal.valueOf(played), 0, RoundingMode.HALF_UP);
        }

        public double percentOf(CourtFormat format) {
            if (total.signum() == 0) {
                return 0;
            }
            return byFormat.getOrDefault(format, BigDecimal.ZERO).multiply(BigDecimal.valueOf(100))
                    .divide(total, 1, RoundingMode.HALF_UP).doubleValue();
        }

        public long playedHours() {
            return hoursByBand.values().stream().mapToLong(Long::longValue).sum();
        }
    }

    /** Ingresos de un día; percentOfMax (0-100) es el alto de su barra frente al mejor día. */
    record DayRevenue(LocalDate day, BigDecimal amount, int percentOfMax) {
    }

    record TeamRanking(String team, long played, BigDecimal spent) {
    }

    RevenueReport report(LocalDate from, LocalDate to);
}
