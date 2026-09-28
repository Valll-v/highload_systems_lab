package ru.itmo.ticketing.venue;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "halls")
public class Hall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "hall", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("rowNo, seatNo")
    private List<Seat> seats = new ArrayList<>();

    protected Hall() {
    }

    Hall(Venue venue, String name) {
        this.venue = venue;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public Venue getVenue() {
        return venue;
    }

    public String getName() {
        return name;
    }

    public List<Seat> getSeats() {
        return seats;
    }

    public void generateSeats(int rows, int seatsPerRow) {
        for (int row = 1; row <= rows; row++) {
            for (int seat = 1; seat <= seatsPerRow; seat++) {
                seats.add(new Seat(this, row, seat));
            }
        }
    }
}
