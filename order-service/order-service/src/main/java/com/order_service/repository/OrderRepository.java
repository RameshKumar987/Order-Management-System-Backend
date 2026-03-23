package com.order_service.repository;

import com.order_service.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order,Long>{
    List<Order> findByUserId(Long userId);
}
