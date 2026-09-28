package ru.itmo.ticketing.booking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findAllByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<Booking> findAllByEventIdAndStatus(Long eventId, BookingStatus status);

    long countByEventIdAndStatus(Long eventId, BookingStatus status);
}
