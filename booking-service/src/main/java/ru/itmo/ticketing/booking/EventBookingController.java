package ru.itmo.ticketing.booking;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import ru.itmo.ticketing.booking.client.UserInfo;
import ru.itmo.ticketing.booking.client.UserRole;
import ru.itmo.ticketing.booking.common.Blocking;
import ru.itmo.ticketing.booking.common.CurrentUser;
import ru.itmo.ticketing.booking.common.SliceResponse;
import ru.itmo.ticketing.booking.dto.CancelledBookings;
import ru.itmo.ticketing.booking.dto.EventSeatResponse;
import ru.itmo.ticketing.booking.dto.SalesReport;

/**
 * Эндпоинты под /api/events/{id}/..., которые gateway отправляет сюда, а не в event-service.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/events/{id}")
public class EventBookingController {

    private final BookingService bookings;
    private final EventStatsService stats;

    @GetMapping("/seats")
    public Mono<SliceResponse<EventSeatResponse>> seats(@PathVariable Long id,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "50") int size) {
        return Blocking.call(() -> stats.seatMap(id, page, size));
    }

    @GetMapping("/sales")
    public Mono<SalesReport> sales(@CurrentUser({UserRole.ORGANIZER, UserRole.ADMIN}) UserInfo actor,
                                   @PathVariable Long id) {
        return Blocking.call(() -> stats.sales(actor, id));
    }

    @PostMapping("/bookings/cancel")
    public Mono<CancelledBookings> cancelAll(@CurrentUser({UserRole.ORGANIZER, UserRole.ADMIN}) UserInfo actor,
                                             @PathVariable Long id) {
        return Blocking.call(() -> bookings.cancelByEvent(actor, id));
    }
}
