package ru.itmo.ticketing.venue;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.ticketing.common.RequireRole;
import ru.itmo.ticketing.user.UserRole;
import ru.itmo.ticketing.venue.dto.CreateHallRequest;
import ru.itmo.ticketing.venue.dto.CreateVenueRequest;
import ru.itmo.ticketing.venue.dto.HallResponse;
import ru.itmo.ticketing.venue.dto.SeatResponse;
import ru.itmo.ticketing.venue.dto.VenueResponse;

import java.util.List;

@RestController
@RequestMapping("/api")
public class VenueController {

    private final VenueService service;

    public VenueController(VenueService service) {
        this.service = service;
    }

    @GetMapping("/venues")
    public List<VenueResponse> list() {
        return service.listVenues();
    }

    @GetMapping("/venues/{id}")
    public VenueResponse get(@PathVariable Long id) {
        return service.getVenueResponse(id);
    }

    @PostMapping("/venues")
    @RequireRole(UserRole.ADMIN)
    @ResponseStatus(HttpStatus.CREATED)
    public VenueResponse create(@Valid @RequestBody CreateVenueRequest request) {
        return service.create(request);
    }

    @PutMapping("/venues/{id}")
    @RequireRole(UserRole.ADMIN)
    public VenueResponse update(@PathVariable Long id, @Valid @RequestBody CreateVenueRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/venues/{id}")
    @RequireRole(UserRole.ADMIN)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.deleteVenue(id);
    }

    @PostMapping("/venues/{id}/halls")
    @RequireRole(UserRole.ADMIN)
    @ResponseStatus(HttpStatus.CREATED)
    public HallResponse addHall(@PathVariable Long id, @Valid @RequestBody CreateHallRequest request) {
        return service.addHall(id, request);
    }

    @GetMapping("/halls/{id}")
    public HallResponse getHall(@PathVariable Long id) {
        return service.getHallResponse(id);
    }

    @GetMapping("/halls/{id}/seats")
    public List<SeatResponse> seats(@PathVariable Long id) {
        return service.listSeatResponses(id);
    }
}
