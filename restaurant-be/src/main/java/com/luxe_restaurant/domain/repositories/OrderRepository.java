package com.luxe_restaurant.domain.repositories;

import com.luxe_restaurant.app.responses.dish.DishSalesResponse;
import com.luxe_restaurant.domain.entities.Order;
import com.luxe_restaurant.domain.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderDetails WHERE o.user.id = :userId ORDER BY o.orderDate DESC")
    List<Order> findByUserIdWithDetails(@Param("userId") Long userId);

    // Keep the old method name for backward compatibility
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderDetails WHERE o.user.id = :userId ORDER BY o.orderDate DESC")
    List<Order> findByUserId(@Param("userId") Long userId);

    // Find order with all details
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderDetails od LEFT JOIN FETCH od.dish WHERE o.id = :orderId")
    Optional<Order> findByIdWithDetails(@Param("orderId") Long orderId);

    // Paginated orders with details
    @Query(value = "SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderDetails",
           countQuery = "SELECT COUNT(DISTINCT o) FROM Order o")
    Page<Order> findAllWithDetails(Pageable pageable);

    // Orders by status with details
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderDetails WHERE o.status = :status ORDER BY o.orderDate DESC")
    List<Order> findByStatusWithDetails(@Param("status") OrderStatus status);

    // Orders by date range
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.orderDetails WHERE o.orderDate BETWEEN :startDate AND :endDate ORDER BY o.orderDate DESC")
    List<Order> findByDateRangeWithDetails(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Top selling dishes with optimized query
    @Query("""
        SELECT new com.luxe_restaurant.app.responses.dish.DishSalesResponse(
                COALESCE(d.nameDish, od.dishName), 
                COALESCE(c.name, 'Không rõ'), 
                SUM(od.quantity), 
                SUM(od.price * od.quantity)
        )
        FROM Order o
        JOIN o.orderDetails od
        LEFT JOIN od.dish d
        LEFT JOIN d.category c
        WHERE o.status = 'COMPLETED'
            AND o.orderDate >= :startDate
            AND o.orderDate <= :endDate
        GROUP BY COALESCE(d.nameDish, od.dishName), COALESCE(c.name, 'Không rõ')
        ORDER BY SUM(od.quantity) DESC
        """)
    List<DishSalesResponse> findTopSellingDishes(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // Revenue statistics
    @Query("""
        SELECT SUM(o.totalPrice) 
        FROM Order o 
        WHERE o.status = 'COMPLETED' 
        AND DATE(o.orderDate) = :date
        """)
    Double getTotalRevenueByDate(@Param("date") LocalDate date);

    @Query("""
        SELECT SUM(o.totalPrice) 
        FROM Order o 
        WHERE o.status = 'COMPLETED' 
        AND o.orderDate >= :startDate 
        AND o.orderDate <= :endDate
        """)
    Double getTotalRevenueByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Order count statistics
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
    long countByStatus(@Param("status") OrderStatus status);

    @Query("SELECT COUNT(o) FROM Order o WHERE DATE(o.orderDate) = :date")
    long countOrdersByDate(@Param("date") LocalDate date);

    // Customer statistics
    @Query("""
        SELECT o.user.id, o.user.userName, COUNT(o), SUM(o.totalPrice)
        FROM Order o 
        WHERE o.status = 'COMPLETED' 
        AND o.user IS NOT NULL
        GROUP BY o.user.id, o.user.userName
        ORDER BY SUM(o.totalPrice) DESC
        """)
    List<Object[]> findTopCustomersByRevenue();

    // Recent orders for dashboard
    @Query("SELECT o FROM Order o ORDER BY o.orderDate DESC")
    List<Order> findRecentOrders(Pageable pageable);

    // Orders pending processing
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status IN ('PENDING', 'PAID')")
    long countPendingOrders();

    // Monthly revenue trend
    @Query("""
        SELECT YEAR(o.orderDate), MONTH(o.orderDate), SUM(o.totalPrice)
        FROM Order o 
        WHERE o.status = 'COMPLETED'
        AND o.orderDate >= :startDate
        GROUP BY YEAR(o.orderDate), MONTH(o.orderDate)
        ORDER BY YEAR(o.orderDate), MONTH(o.orderDate)
        """)
    List<Object[]> getMonthlyRevenueTrend(@Param("startDate") LocalDateTime startDate);

    // Average order value
    @Query("SELECT AVG(o.totalPrice) FROM Order o WHERE o.status = 'COMPLETED'")
    Double getAverageOrderValue();

    // Peak hours analysis
    @Query("""
        SELECT HOUR(o.orderDate), COUNT(o)
        FROM Order o 
        WHERE DATE(o.orderDate) = :date
        GROUP BY HOUR(o.orderDate)
        ORDER BY HOUR(o.orderDate)
        """)
    List<Object[]> getOrdersByHour(@Param("date") LocalDate date);
}