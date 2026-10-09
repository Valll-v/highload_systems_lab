package ru.itmo.ticketing.event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    @Query("""
            select e from Event e
            join fetch e.category
            join fetch e.hall h
            join fetch h.venue v
            where e.status = ru.itmo.ticketing.event.EventStatus.PUBLISHED
              and (:categoryId is null or e.category.id = :categoryId)
              and (:city is null or lower(v.city) = :city)
              and (cast(:from as timestamp) is null or e.startsAt >= :from)
              and (cast(:to as timestamp) is null or e.startsAt <= :to)
            """)
    Page<Event> findPublished(@Param("categoryId") Long categoryId,
                              @Param("city") String city,
                              @Param("from") OffsetDateTime from,
                              @Param("to") OffsetDateTime to,
                              Pageable pageable);

    List<Event> findAllByOrganizerIdOrderByStartsAtDesc(Long organizerId);

    List<Event> findAllByStatusOrderByCreatedAtAsc(EventStatus status);

    @Query("""
            select count(e) > 0 from Event e
            where e.hall.id = :hallId
              and e.status = ru.itmo.ticketing.event.EventStatus.PUBLISHED
              and e.id <> :excludeId
              and e.startsAt < :endsAt
              and e.endsAt > :startsAt
            """)
    boolean existsHallConflict(@Param("hallId") Long hallId,
                               @Param("startsAt") OffsetDateTime startsAt,
                               @Param("endsAt") OffsetDateTime endsAt,
                               @Param("excludeId") Long excludeId);
}
