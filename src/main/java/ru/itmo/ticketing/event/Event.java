package ru.itmo.ticketing.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.itmo.ticketing.user.User;
import ru.itmo.ticketing.venue.Hall;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(nullable = false)
    private String title;

    @Setter
    @Column(columnDefinition = "text")
    private String description;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private User organizer;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private Hall hall;

    @Setter
    @Column(nullable = false)
    private OffsetDateTime startsAt;

    @Setter
    @Column(nullable = false)
    private OffsetDateTime endsAt;

    @Setter
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EventStatus status;

    @Column(nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public Event(String title, String description, Category category, User organizer, Hall hall,
                 OffsetDateTime startsAt, OffsetDateTime endsAt, BigDecimal price) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.organizer = organizer;
        this.hall = hall;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.price = price;
        this.status = EventStatus.DRAFT;
    }

    public boolean isOwnedBy(User user) {
        return organizer.getId().equals(user.getId());
    }
}
