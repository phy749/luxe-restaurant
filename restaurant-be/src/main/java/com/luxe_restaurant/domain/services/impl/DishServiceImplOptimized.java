package com.luxe_restaurant.domain.services.impl;

import com.luxe_restaurant.app.requests.dish.DishRequest;
import com.luxe_restaurant.app.responses.dish.DishResponse;
import com.luxe_restaurant.domain.entities.Category;
import com.luxe_restaurant.domain.entities.Dish;
import com.luxe_restaurant.domain.repositories.CategoryRepository;
import com.luxe_restaurant.domain.repositories.DishRepository;
import com.luxe_restaurant.domain.services.DishService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service("dishServiceOptimized")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DishServiceImplOptimized implements DishService {

    private final DishRepository dishRepository;
    private final CategoryRepository categoryRepository;
    private final ModelMapper modelMapper;

    private DishResponse mapToResponse(Dish dish) {
        DishResponse response = modelMapper.map(dish, DishResponse.class);
        if (dish.getCategory() != null) {
            response.setCategoryName(dish.getCategory().getName());
            response.setCategoryId(dish.getCategory().getId());
        } else {
            response.setCategoryName("Chưa phân loại");
        }
        response.setActive(dish.isActive());
        return response;
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "dishes", allEntries = true),
        @CacheEvict(value = "statistics", allEntries = true)
    })
    public DishResponse createDish(DishRequest dishRequest) {
        log.info("Creating new dish: {}", dishRequest.getDishName());
        
        // Check if dish name already exists
        if (dishRepository.existsByNameDish(dishRequest.getDishName())) {
            throw new RuntimeException("Dish name already exists: " + dishRequest.getDishName());
        }

        // Find category with caching
        Category category = null;
        if (dishRequest.getCategoryId() != null) {
            category = categoryRepository.findById(dishRequest.getCategoryId()).orElse(null);
        }

        Dish dish = modelMapper.map(dishRequest, Dish.class);
        dish.setId(null);
        dish.setCategory(category);
        dish.setDes(dishRequest.getDes());
        dish.setActive(true);

        Dish savedDish = dishRepository.save(dish);
        log.info("Created dish with ID: {}", savedDish.getId());
        
        return mapToResponse(savedDish);
    }

    @Override
    @Cacheable(value = "dishes", key = "'all'")
    public List<DishResponse> getAllDishes() {
        log.info("Fetching all dishes from database");
        return dishRepository.findAllWithCategory()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<DishResponse> getActiveDishes() {
        log.info("Fetching active dishes from database");
        return dishRepository.findByActiveTrueOrderByNameDishAsc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<DishResponse> getDishesByCategory(Long categoryId) {
        log.info("Fetching dishes for category ID: {}", categoryId);
        return dishRepository.findByCategoryIdAndActiveTrue(categoryId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "dishes", allEntries = true),
        @CacheEvict(value = "statistics", allEntries = true)
    })
    @CachePut(value = "dishes", key = "#id")
    public DishResponse updateDish(Long id, DishRequest dishRequest) {
        log.info("Updating dish with ID: {}", id);
        
        Dish dish = dishRepository.findByIdWithCategory(id)
                .orElseThrow(() -> new RuntimeException("Dish Not Found with ID: " + id));

        // Check if new name conflicts with existing dishes (excluding current dish)
        if (!dish.getNameDish().equals(dishRequest.getDishName()) && 
            dishRepository.existsByNameDish(dishRequest.getDishName())) {
            throw new RuntimeException("Dish name already exists: " + dishRequest.getDishName());
        }

        Category category = null;
        if (dishRequest.getCategoryId() != null) {
            category = categoryRepository.findById(dishRequest.getCategoryId()).orElse(null);
        }

        dish.setNameDish(dishRequest.getDishName());
        dish.setPrice(dishRequest.getPrice());
        dish.setCategory(category);
        dish.setUrlImage(dishRequest.getUrlImage());
        dish.setDes(dishRequest.getDes());

        Dish updatedDish = dishRepository.save(dish);
        log.info("Updated dish with ID: {}", updatedDish.getId());
        
        return mapToResponse(updatedDish);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "dishes", allEntries = true),
        @CacheEvict(value = "statistics", allEntries = true)
    })
    public void deleteDish(Long id) {
        log.info("Attempting to delete dish with ID: {}", id);
        
        Dish dish = dishRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dish Not Found with ID: " + id));

        // Check if dish is used in any orders
        if (dishRepository.isUsedInOrders(id)) {
            // Instead of deleting, deactivate the dish
            dish.setActive(false);
            dishRepository.save(dish);
            log.info("Deactivated dish with ID: {} (used in orders)", id);
        } else {
            dishRepository.delete(dish);
            log.info("Deleted dish with ID: {}", id);
        }
    }

    @Override
    @Cacheable(value = "dishes", key = "#id")
    public DishResponse getDishById(Long id) {
        log.info("Fetching dish with ID: {}", id);
        Dish dish = dishRepository.findByIdWithCategory(id)
                .orElseThrow(() -> new RuntimeException("Dish Not Found with ID: " + id));
        return mapToResponse(dish);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "dishes", allEntries = true),
        @CacheEvict(value = "statistics", allEntries = true)
    })
    @CachePut(value = "dishes", key = "#id")
    public DishResponse toggleDishStatus(Long id) {
        log.info("Toggling status for dish with ID: {}", id);
        
        Dish dish = dishRepository.findByIdWithCategory(id)
                .orElseThrow(() -> new RuntimeException("Dish Not Found with ID: " + id));
        
        dish.setActive(!dish.isActive());
        Dish updatedDish = dishRepository.save(dish);
        
        log.info("Toggled dish status - ID: {}, Active: {}", updatedDish.getId(), updatedDish.isActive());
        return mapToResponse(updatedDish);
    }

    @Cacheable(value = "dishes", key = "'search_' + #keyword")
    public List<DishResponse> searchDishes(String keyword) {
        log.info("Searching dishes with keyword: {}", keyword);
        return dishRepository.findByNameDishContainingIgnoreCaseAndActiveTrue(keyword)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "statistics", key = "'dish_stats'")
    public DishStatistics getDishStatistics() {
        log.info("Calculating dish statistics");
        long activeDishes = dishRepository.countActiveDishes();
        long inactiveDishes = dishRepository.countInactiveDishes();
        long totalDishes = activeDishes + inactiveDishes;
        
        return DishStatistics.builder()
                .totalDishes(totalDishes)
                .activeDishes(activeDishes)
                .inactiveDishes(inactiveDishes)
                .build();
    }

    // Inner class for statistics
    @lombok.Data
    @lombok.Builder
    public static class DishStatistics {
        private long totalDishes;
        private long activeDishes;
        private long inactiveDishes;
    }
}