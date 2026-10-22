package ru.itmo.ticketing.venue;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    Slice<Seat> findAllByHallId(Long hallId, Pageable pageable);
}
