package ru.itmo.ticketing.venue;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "seats")
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hall_id", nullable = false)
    private Hall hall;

    @Column(name = "row_no", nullable = false)
    private int rowNo;

    @Column(name = "seat_no", nullable = false)
    private int seatNo;

    protected Seat() {
    }

    Seat(Hall hall, int rowNo, int seatNo) {
        this.hall = hall;
        this.rowNo = rowNo;
        this.seatNo = seatNo;
    }

    public Long getId() {
        return id;
    }

    public Hall getHall() {
        return hall;
    }

    public int getRowNo() {
        return rowNo;
    }

    public int getSeatNo() {
        return seatNo;
    }
}
