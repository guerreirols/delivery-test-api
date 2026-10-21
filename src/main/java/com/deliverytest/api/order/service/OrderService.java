package com.deliverytest.api.order.service;

import com.deliverytest.api.order.dto.OrderRequest;
import com.deliverytest.api.order.dto.OrderResponse;
import com.deliverytest.api.order.dto.OrderStatusUpdateRequest;
import com.deliverytest.api.order.entity.Order;
import com.deliverytest.api.order.exception.OrderNotFoundException;
import com.deliverytest.api.order.mapper.OrderMapper;
import com.deliverytest.api.order.repository.OrderRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    public OrderResponse create(OrderRequest orderRequest) {
        Order order = orderMapper.toEntity(orderRequest);
        Order savedOrder = orderRepository.save(order);
        log.debug("Criação de pedido concluída");
        return orderMapper.toResponse(savedOrder);
    }

    @Transactional
    public void updateStatus(String id, OrderStatusUpdateRequest request) {
        Order order = orderRepository.findById(id).orElseThrow(
                () -> new OrderNotFoundException(id));

        order.updateStatus(request.status());
        log.debug("Transição de status aplicada ao pedido: status={}", request.status());
    }

    public OrderResponse findById(String id) {
        return orderRepository.findById(id)
                .map(orderMapper::toResponse)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    public Page<OrderResponse> findAll(Pageable pageable) {
        return orderRepository.findAll(pageable).map(orderMapper::toResponse);
    }
}
