package com.luxe_restaurant.domain.repositories;

import com.luxe_restaurant.domain.entities.Dish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DishRepository extends JpaRepository<Dish, Long> {

    // Check if dish name exists
    boolean existsByNameDish(String nameDish);
    
    // Find dishes by category with active status
    List<Dish> findByCategoryIdAndActiveTrue(Long categoryId);
    
    // Search dishes by name (case insensitive) and active status
    List<Dish> findByNameDishContainingIgnoreCaseAndActiveTrue(String keyword);
    
    // Find dish with category (optimized query)
    @Query("SELECT d FROM Dish d LEFT JOIN FETCH d.category WHERE d.id = :id")
    Optional<Dish> findByIdWithCategory(@Param("id") Long id);
    
    // Find all dishes with category (optimized query)
    @Query("SELECT d FROM Dish d LEFT JOIN FETCH d.category ORDER BY d.id DESC")
    List<Dish> findAllWithCategory();
    
    // Check if dish is used in any orders
    @Query("SELECT COUNT(od) > 0 FROM OrderDetail od WHERE od.dish.id = :dishId")
    boolean isUsedInOrders(@Param("dishId") Long dishId);
    
    // Find active dishes only
    List<Dish> findByActiveTrueOrderByNameDishAsc();
    
    // Find dishes by price range
    @Query("SELECT d FROM Dish d WHERE d.price BETWEEN :minPrice AND :maxPrice AND d.active = true")
    List<Dish> findByPriceRangeAndActive(@Param("minPrice") java.math.BigDecimal minPrice, 
                                        @Param("maxPrice") java.math.BigDecimal maxPrice);
    
    // Get dish statistics
    @Query("SELECT COUNT(d) FROM Dish d WHERE d.active = true")
    long countActiveDishes();
    
    @Query("SELECT COUNT(d) FROM Dish d WHERE d.active = false")
    long countInactiveDishes();
}
