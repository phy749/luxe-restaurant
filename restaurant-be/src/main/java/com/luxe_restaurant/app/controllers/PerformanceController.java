package com.luxe_restaurant.app.controllers;

import com.luxe_restaurant.domain.services.PerformanceMonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/performance")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
public class PerformanceController {

    private final PerformanceMonitoringService performanceService;

    /**
     * Get cache statistics - Admin only
     */
    @GetMapping("/cache/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getCacheStatistics() {
        return ResponseEntity.ok(performanceService.getCacheStatistics());
    }

    /**
     * Clear all caches - Admin only
     */
    @PostMapping("/cache/clear-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> clearAllCaches() {
        performanceService.clearAllCaches();
        return ResponseEntity.ok("All caches cleared successfully");
    }

    /**
     * Clear specific cache - Admin only
     */
    @PostMapping("/cache/clear/{cacheName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> clearCache(@PathVariable String cacheName) {
        performanceService.clearCache(cacheName);
        return ResponseEntity.ok("Cache '" + cacheName + "' cleared successfully");
    }

    /**
     * Warm up caches - Admin only
     */
    @PostMapping("/cache/warmup")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> warmUpCaches() {
        performanceService.warmUpCaches();
        return ResponseEntity.ok("Cache warm-up completed");
    }

    /**
     * Get memory statistics - Admin only
     */
    @GetMapping("/memory/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getMemoryStatistics() {
        return ResponseEntity.ok(performanceService.getMemoryStatistics());
    }
}