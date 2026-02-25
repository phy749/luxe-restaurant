package com.luxe_restaurant.domain.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class PerformanceMonitoringService {

    private final CacheManager cacheManager;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Get cache statistics for monitoring
     */
    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // Get cache names
        Collection<String> cacheNames = cacheManager.getCacheNames();
        stats.put("totalCaches", cacheNames.size());
        stats.put("cacheNames", cacheNames);
        stats.put("cacheType", cacheManager.getClass().getSimpleName());
        
        // Get Redis info if available
        try {
            if (redisTemplate != null) {
                Map<String, Object> redisStats = new HashMap<>();
                
                // Get Redis memory info
                Set<String> keys = redisTemplate.keys("*");
                redisStats.put("totalKeys", keys != null ? keys.size() : 0);
                redisStats.put("status", "connected");
                
                stats.put("redis", redisStats);
            } else {
                stats.put("redis", "not configured");
            }
            
        } catch (Exception e) {
            log.warn("Could not retrieve Redis statistics: {}", e.getMessage());
            stats.put("redis", "unavailable - using simple cache");
        }
        
        return stats;
    }

    /**
     * Clear all caches
     */
    public void clearAllCaches() {
        log.info("Clearing all caches");
        cacheManager.getCacheNames().forEach(cacheName -> {
            try {
                Objects.requireNonNull(cacheManager.getCache(cacheName)).clear();
                log.info("Cleared cache: {}", cacheName);
            } catch (Exception e) {
                log.warn("Failed to clear cache {}: {}", cacheName, e.getMessage());
            }
        });
    }

    /**
     * Clear specific cache
     */
    public void clearCache(String cacheName) {
        log.info("Clearing cache: {}", cacheName);
        try {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                log.info("Cache cleared: {}", cacheName);
            } else {
                log.warn("Cache not found: {}", cacheName);
            }
        } catch (Exception e) {
            log.warn("Failed to clear cache {}: {}", cacheName, e.getMessage());
        }
    }

    /**
     * Warm up caches with frequently accessed data
     */
    public void warmUpCaches() {
        log.info("Starting cache warm-up process");
        // This would typically pre-load frequently accessed data
        // Implementation depends on your specific use case
        log.info("Cache warm-up completed");
    }

    /**
     * Get memory usage statistics
     */
    public Map<String, Object> getMemoryStatistics() {
        Map<String, Object> memStats = new HashMap<>();
        
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        memStats.put("maxMemoryMB", maxMemory / (1024 * 1024));
        memStats.put("totalMemoryMB", totalMemory / (1024 * 1024));
        memStats.put("usedMemoryMB", usedMemory / (1024 * 1024));
        memStats.put("freeMemoryMB", freeMemory / (1024 * 1024));
        memStats.put("memoryUsagePercent", Math.round((double) usedMemory / maxMemory * 100 * 100.0) / 100.0);
        
        return memStats;
    }
}