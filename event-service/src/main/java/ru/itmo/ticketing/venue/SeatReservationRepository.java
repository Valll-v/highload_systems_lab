package ru.itmo.ticketing.venue;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface SeatReservationRepository extends JpaRepository<SeatReservation, Long> {

    @Query("select r.seat.id from SeatReservation r where r.event.id = :eventId")
    List<Long> findReservedSeatIds(@Param("eventId") Long eventId);

    @Query("select r.seat.id from SeatReservation r where r.event.id = :eventId and r.seat.id in :seatIds")
    List<Long> findReservedSeatIds(@Param("eventId") Long eventId, @Param("seatIds") Collection<Long> seatIds);

    long deleteByBookingId(Long bookingId);

    long countByEventId(Long eventId);
}
