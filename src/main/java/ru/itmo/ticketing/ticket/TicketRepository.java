package ru.itmo.ticketing.ticket;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findAllByBookingIdOrderByIdAsc(Long bookingId);

    List<Ticket> findAllByBookingCustomerIdOrderByIdDesc(Long customerId);

    long countByEventIdAndStatus(Long eventId, TicketStatus status);

    @Query("select coalesce(sum(t.price), 0) from Ticket t where t.event.id = :eventId and t.status = :status")
    BigDecimal sumPriceByEventIdAndStatus(@Param("eventId") Long eventId, @Param("status") TicketStatus status);
}
