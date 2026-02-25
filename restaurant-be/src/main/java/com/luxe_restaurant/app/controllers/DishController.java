    package com.luxe_restaurant.app.controllers;

import com.luxe_restaurant.app.requests.dish.DishRequest;
import com.luxe_restaurant.app.responses.dish.DishResponse;
import com.luxe_restaurant.domain.services.DishService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
@RestController
@RequestMapping("/api/dish")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Dish Management", description = "APIs for managing restaurant dishes")
public class DishController {

    private final DishService dishService;

    @Operation(summary = "Create a new dish", description = "Creates a new dish in the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Dish created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(value = "dishes", allEntries = true)
    public ResponseEntity<DishResponse> createDish(@Valid @RequestBody DishRequest dishRequest) {
        log.info("Creating new dish: {}", dishRequest.getDishName());
        DishResponse response = dishService.createDish(dishRequest);
        log.info("Successfully created dish with ID: {}", response.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get all dishes", description = "Retrieves all dishes from the system")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved dishes")
    @GetMapping("/getall")
    @Cacheable(value = "dishes")
    public ResponseEntity<List<DishResponse>> getAllDishes() {
        log.debug("Fetching all dishes");
        List<DishResponse> dishes = dishService.getAllDishes();
        return ResponseEntity.ok(dishes);
    }

    @Operation(summary = "Update a dish", description = "Updates an existing dish")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dish updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Dish not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    @PutMapping("/update/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(value = "dishes", allEntries = true)
    public ResponseEntity<DishResponse> updateDish(
            @Parameter(description = "Dish ID", required = true) @PathVariable Long id,
            @Valid @RequestBody DishRequest dishRequest) {
        log.info("Updating dish with ID: {}", id);
        DishResponse response = dishService.updateDish(id, dishRequest);
        log.info("Successfully updated dish with ID: {}", id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete a dish", description = "Deletes a dish from the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Dish deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Dish not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(value = "dishes", allEntries = true)
    public ResponseEntity<Void> deleteDish(
            @Parameter(description = "Dish ID", required = true) @PathVariable Long id) {
        log.info("Deleting dish with ID: {}", id);
        dishService.deleteDish(id);
        log.info("Successfully deleted dish with ID: {}", id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get dish by ID", description = "Retrieves a specific dish by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dish found"),
            @ApiResponse(responseCode = "404", description = "Dish not found")
    })
    @GetMapping("/find/{id}")
    public ResponseEntity<DishResponse> findDish(
            @Parameter(description = "Dish ID", required = true) @PathVariable Long id) {
        log.debug("Finding dish with ID: {}", id);
        DishResponse response = dishService.getDishById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Toggle dish status", description = "Toggles the active status of a dish")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dish status toggled successfully"),
            @ApiResponse(responseCode = "404", description = "Dish not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    @PutMapping("/toggle/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(value = "dishes", allEntries = true)
    public ResponseEntity<DishResponse> toggleDish(
            @Parameter(description = "Dish ID", required = true) @PathVariable Long id) {
        log.info("Toggling status for dish with ID: {}", id);
        DishResponse response = dishService.toggleDishStatus(id);
        log.info("Successfully toggled status for dish with ID: {}", id);
        return ResponseEntity.ok(response);
    }
}
