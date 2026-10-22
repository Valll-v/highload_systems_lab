package ru.itmo.ticketing.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface SeatReservationRepository extends JpaRepository<SeatReservation, Long> {

    @Query("select r.seatId from SeatReservation r where r.eventId = :eventId and r.seatId in :seatIds")
    List<Long> findReservedSeatIds(@Param("eventId") Long eventId, @Param("seatIds") Collection<Long> seatIds);

    long deleteByBookingId(Long bookingId);

    long countByEventId(Long eventId);
}
