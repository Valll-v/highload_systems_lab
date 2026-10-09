package ru.itmo.ticketing.venue;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ru.itmo.ticketing.common.RequireRole;
import ru.itmo.ticketing.user.UserRole;
import ru.itmo.ticketing.venue.dto.CreateHallRequest;
import ru.itmo.ticketing.venue.dto.CreateVenueRequest;
import ru.itmo.ticketing.venue.dto.HallResponse;
import ru.itmo.ticketing.venue.dto.SeatResponse;
import ru.itmo.ticketing.venue.dto.VenueResponse;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class VenueController {

    private final VenueService service;

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
    public ResponseEntity<VenueResponse> create(@Valid @RequestBody CreateVenueRequest request) {
        VenueResponse venue = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(venue.id()).toUri();
        return ResponseEntity.created(location).body(venue);
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
    public ResponseEntity<HallResponse> addHall(@PathVariable Long id, @Valid @RequestBody CreateHallRequest request) {
        HallResponse hall = service.addHall(id, request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/halls/{id}").buildAndExpand(hall.id()).toUri();
        return ResponseEntity.created(location).body(hall);
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
