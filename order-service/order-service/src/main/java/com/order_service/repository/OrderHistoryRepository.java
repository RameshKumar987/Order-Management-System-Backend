package com.order_service.repository;

import com.order_service.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderHistoryRepository extends JpaRepository<OrderStatusHistory,Long>{
}
