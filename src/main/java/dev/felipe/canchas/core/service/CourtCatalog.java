package dev.felipe.canchas.core.service;

import dev.felipe.canchas.core.port.in.CourtCatalogUseCase;
import dev.felipe.canchas.core.port.out.BookingStore;
import dev.felipe.canchas.core.port.out.CourtStore;
import dev.felipe.canchas.core.BusinessRuleException;
import dev.felipe.canchas.core.NotFoundException;
import dev.felipe.canchas.core.model.Booking;
import dev.felipe.canchas.core.model.Court;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class CourtCatalog implements CourtCatalogUseCase {
    static final Comparator<Court> BY_FORMAT_AND_CODE = Comparator.comparing(Court::format).thenComparing(Court::code);

    private final CourtStore courts;
    private final BookingStore bookings;

    public CourtCatalog(CourtStore courts, BookingStore bookings) {
        this.courts = courts;
        this.bookings = bookings;
    }

    @Override
    public Court open(CourtCommand command) {
        Court court = Court.open(UUID.randomUUID(), command.code(), command.name(), command.format());
        requireFreeCode(court);
        courts.save(court);
        return court;
    }

    @Override
    public Court update(UUID courtId, CourtCommand command) {
        Court current = existing(courtId);
        Court updated = current.withDetails(command.code(), command.name(), command.format());
        requireFreeCode(updated);
        if (updated.format() != current.format() && !bookings.findByCourt(courtId).isEmpty()) {
            throw new BusinessRuleException("La cancha " + current.code() + " ya tiene reservas: no se puede cambiar su formato");
        }
        courts.save(updated);
        return updated;
    }

    @Override
    public Court activate(UUID courtId) {
        Court active = existing(courtId).activate();
        courts.save(active);
        return active;
    }

    @Override
    public Court deactivate(UUID courtId) {
        Court current = existing(courtId);
        long pending = bookings.findByCourt(courtId).stream().filter(Booking::isPending).count();
        if (pending > 0) {
            throw new BusinessRuleException("La cancha " + current.code() + " tiene " + pending
                    + " reserva(s) pendiente(s): cancélalas o ciérralas antes de sacarla de servicio");
        }
        Court inactive = current.deactivate();
        courts.save(inactive);
        return inactive;
    }

    @Override
    public List<Court> all() {
        return courts.findAll().stream().sorted(BY_FORMAT_AND_CODE).toList();
    }

    @Override
    public Optional<Court> byId(UUID courtId) {
        return courts.findById(courtId);
    }

    private Court existing(UUID courtId) {
        return courts.findById(courtId).orElseThrow(() -> new NotFoundException("La cancha no existe"));
    }

    private void requireFreeCode(Court court) {
        courts.findByCode(court.code())
                .filter(other -> !other.id().equals(court.id()))
                .ifPresent(other -> {
                    throw new BusinessRuleException("Ya existe una cancha con el código " + court.code());
                });
    }
}
