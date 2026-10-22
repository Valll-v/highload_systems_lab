package ru.itmo.ticketing.booking;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "booking-service", fallbackFactory = BookingClientFallback.class)
public interface BookingClient {

    /** Отменяет все активные брони мероприятия; booking-service сам проверит, что оно отменено. */
    @PostMapping("/api/events/{id}/bookings/cancel")
    CancelledBookings cancelByEvent(@PathVariable("id") long eventId, @RequestHeader("X-User-Id") long actorId);
}
