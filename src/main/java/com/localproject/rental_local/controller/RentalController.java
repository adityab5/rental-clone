package com.localproject.rental_local.controller;

import com.localproject.rental_local.dto.CreateRentalRequest;
import com.localproject.rental_local.dto.RentalDto;
import com.localproject.rental_local.service.RentalService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rentals")
@RequiredArgsConstructor
public class RentalController {

    private final RentalService rentalService;

    @PostMapping("/")
    @ResponseStatus(HttpStatus.CREATED)
    public RentalDto createRental(@Valid @RequestBody CreateRentalRequest request) {
        return rentalService.createRental(request);
    }

    @GetMapping("/{id}")
    public RentalDto getRentalById(@PathVariable Long id) {
        return rentalService.getRentalById(id);
    }

    @PostMapping("/{id}/return")
    public RentalDto returnRental(@PathVariable Long id) {
        return rentalService.markRentalAsReturned(id);
    }

    @GetMapping("/")
    public List<RentalDto> getAllRentals() {
        return rentalService.listAllRentals();
    }

    @GetMapping("/overdue")
    public List<RentalDto> getOverdueRentals() {
        return rentalService.listOverdueRentals();
    }
}

