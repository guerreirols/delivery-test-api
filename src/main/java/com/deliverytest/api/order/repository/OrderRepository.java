package com.deliverytest.api.order.repository;

import com.deliverytest.api.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, String> {
}
