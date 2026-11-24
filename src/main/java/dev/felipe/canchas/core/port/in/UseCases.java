package dev.felipe.canchas.core.port.in;

/**
 * Todos los puertos de entrada juntos. Es lo único que reciben los adaptadores de entrada (web y REST):
 * no conocen las clases que los implementan ni la base de datos.
 */
public record UseCases(
        CourtCatalogUseCase courts,
        TeamRegistryUseCase teams,
        BookingDeskUseCase bookings,
        ScheduleBoardUseCase schedule,
        RateBookUseCase rates,
        RevenueReportUseCase reports,
        StaffDirectoryUseCase staff
) {
}
