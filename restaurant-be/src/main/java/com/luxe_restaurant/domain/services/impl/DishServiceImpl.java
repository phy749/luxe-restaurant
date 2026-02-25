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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DishServiceImpl implements DishService {

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
    @CacheEvict(value = "dishes", allEntries = true)
    public DishResponse createDish(DishRequest dishRequest) {
        log.info("Creating dish: {}", dishRequest.getDishName());
        
        // Validate category exists
        Category category = null;
        if (dishRequest.getCategoryId() != null) {
            category = categoryRepository.findById(dishRequest.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + dishRequest.getCategoryId()));
        }

        Dish dish = modelMapper.map(dishRequest, Dish.class);
        dish.setId(null);
        dish.setCategory(category);
        dish.setDes(dishRequest.getDes());
        dish.setActive(true);

        Dish savedDish = dishRepository.save(dish);
        log.info("Successfully created dish with ID: {}", savedDish.getId());
        
        return mapToResponse(savedDish);
    }

    @Override
    @Cacheable(value = "dishes")
    @Transactional(readOnly = true)
    public List<DishResponse> getAllDishes() {
        log.debug("Fetching all dishes from database");
        return dishRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @CacheEvict(value = "dishes", allEntries = true)
    public DishResponse updateDish(Long id, DishRequest dishRequest) {
        log.info("Updating dish with ID: {}", id);
        
        Dish dish = dishRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Dish not found with ID: " + id));

        // Validate category if provided
        Category category = null;
        if (dishRequest.getCategoryId() != null) {
            category = categoryRepository.findById(dishRequest.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + dishRequest.getCategoryId()));
        }

        dish.setNameDish(dishRequest.getDishName());
        dish.setPrice(dishRequest.getPrice());
        dish.setCategory(category);
        dish.setUrlImage(dishRequest.getUrlImage());
        dish.setDes(dishRequest.getDes());

        Dish updatedDish = dishRepository.save(dish);
        log.info("Successfully updated dish with ID: {}", id);
        
        return mapToResponse(updatedDish);
    }

    @Override
    @CacheEvict(value = "dishes", allEntries = true)
    public void deleteDish(Long id) {
        log.info("Deleting dish with ID: {}", id);
        
        Dish dish = dishRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Dish not found with ID: " + id));
        
        dishRepository.delete(dish);
        log.info("Successfully deleted dish with ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public DishResponse getDishById(Long id) {
        log.debug("Finding dish with ID: {}", id);
        
        Dish dish = dishRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Dish not found with ID: " + id));
        
        return mapToResponse(dish);
    }

    @Override
    @CacheEvict(value = "dishes", allEntries = true)
    public DishResponse toggleDishStatus(Long id) {
        log.info("Toggling status for dish with ID: {}", id);
        
        Dish dish = dishRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Dish not found with ID: " + id));
        
        dish.setActive(!dish.isActive());
        Dish updatedDish = dishRepository.save(dish);
        
        log.info("Successfully toggled status for dish with ID: {} to {}", id, updatedDish.isActive());
        return mapToResponse(updatedDish);
    }
}