package ru.itmo.ticketing.booking.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "event-service", configuration = EventClientConfig.class, fallbackFactory = EventClientFallback.class)
public interface EventClient {

    /** Мероприятие глазами анонима: только опубликованные. */
    @GetMapping("/api/events/{id}")
    EventInfo event(@PathVariable("id") long id);

    /** Мероприятие глазами пользователя: организатор и админ видят любой статус. */
    @GetMapping("/api/events/{id}")
    EventInfo eventAs(@PathVariable("id") long id, @RequestHeader("X-User-Id") long userId);

    @GetMapping("/api/halls/{id}")
    HallInfo hall(@PathVariable("id") long id);

    @GetMapping("/api/halls/{id}/seats")
    SeatPage seats(@PathVariable("id") long hallId, @RequestParam("page") int page, @RequestParam("size") int size);
}
