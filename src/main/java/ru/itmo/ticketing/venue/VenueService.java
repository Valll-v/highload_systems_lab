package ru.itmo.ticketing.venue;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.ticketing.common.NotFoundException;
import ru.itmo.ticketing.venue.dto.CreateHallRequest;
import ru.itmo.ticketing.venue.dto.CreateVenueRequest;
import ru.itmo.ticketing.venue.dto.HallResponse;
import ru.itmo.ticketing.venue.dto.SeatResponse;
import ru.itmo.ticketing.venue.dto.VenueResponse;

import java.util.List;

@Service
@Transactional
public class VenueService {

    private final VenueRepository venues;
    private final HallRepository halls;
    private final SeatRepository seats;

    public VenueService(VenueRepository venues, HallRepository halls, SeatRepository seats) {
        this.venues = venues;
        this.halls = halls;
        this.seats = seats;
    }

    public VenueResponse create(CreateVenueRequest request) {
        Venue venue = venues.save(new Venue(request.name().trim(), request.city().trim(), request.address().trim()));
        return VenueResponse.from(venue);
    }

    public VenueResponse update(Long id, CreateVenueRequest request) {
        Venue venue = getVenue(id);
        venue.setName(request.name().trim());
        venue.setCity(request.city().trim());
        venue.setAddress(request.address().trim());
        return VenueResponse.from(venue);
    }

    @Transactional(readOnly = true)
    public VenueResponse getVenueResponse(Long id) {
        return VenueResponse.from(getVenue(id));
    }

    @Transactional(readOnly = true)
    public List<VenueResponse> listVenues() {
        return venues.findAll().stream().map(VenueResponse::from).toList();
    }

    public void deleteVenue(Long id) {
        venues.delete(getVenue(id));
    }

    public HallResponse addHall(Long venueId, CreateHallRequest request) {
        Venue venue = getVenue(venueId);
        Hall hall = venue.addHall(request.name().trim());
        hall.generateSeats(request.rows(), request.seatsPerRow());
        venues.flush();
        return HallResponse.from(hall);
    }

    @Transactional(readOnly = true)
    public HallResponse getHallResponse(Long id) {
        return HallResponse.from(getHall(id));
    }

    @Transactional(readOnly = true)
    public List<SeatResponse> listSeatResponses(Long hallId) {
        return listSeats(hallId).stream().map(SeatResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public Venue getVenue(Long id) {
        return venues.findById(id).orElseThrow(() -> new NotFoundException("Venue", id));
    }

    @Transactional(readOnly = true)
    public Hall getHall(Long id) {
        return halls.findById(id).orElseThrow(() -> new NotFoundException("Hall", id));
    }

    @Transactional(readOnly = true)
    public List<Seat> listSeats(Long hallId) {
        getHall(hallId);
        return seats.findAllByHallIdOrderByRowNoAscSeatNoAsc(hallId);
    }
}
