package ru.itmo.ticketing.booking;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Page<Booking> findAllByCustomerId(Long customerId, Pageable pageable);

    List<Booking> findAllByEventIdAndStatus(Long eventId, BookingStatus status);

    long countByEventIdAndStatus(Long eventId, BookingStatus status);
}
