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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.itmo.ticketing.user.User;
import ru.itmo.ticketing.venue.Hall;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String title;

    @Setter
    @Size(max = 10000)
    @Column(columnDefinition = "text")
    private String description;

    @Setter
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private Category category;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private User organizer;

    @Setter
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private Hall hall;

    @Setter
    @NotNull
    @Column(nullable = false)
    private OffsetDateTime startsAt;

    @Setter
    @NotNull
    @Column(nullable = false)
    private OffsetDateTime endsAt;

    @Setter
    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 10, fraction = 2)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Setter
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EventStatus status;

    @ManyToMany
    @JoinTable(name = "event_tags",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    @OrderBy("name")
    private Set<Tag> tags = new LinkedHashSet<>();

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

    @AssertTrue(message = "endsAt must be after startsAt")
    public boolean isTimeWindowValid() {
        return startsAt != null && endsAt != null && endsAt.isAfter(startsAt);
    }

    public boolean isOwnedBy(User user) {
        return organizer.getId().equals(user.getId());
    }

    public void setTags(Collection<Tag> newTags) {
        tags.clear();
        tags.addAll(newTags);
    }
}
