package com.deliverytest.api.order.service;

import com.deliverytest.api.order.dto.OrderItemRequest;
import com.deliverytest.api.order.dto.OrderRequest;
import com.deliverytest.api.order.dto.OrderResponse;
import com.deliverytest.api.order.dto.OrderStatusUpdateRequest;
import com.deliverytest.api.order.entity.Order;
import com.deliverytest.api.order.entity.OrderStatus;
import com.deliverytest.api.order.exception.InvalidOrderStatusException;
import com.deliverytest.api.order.exception.OrderNotFoundException;
import com.deliverytest.api.order.mapper.OrderItemMapper;
import com.deliverytest.api.order.mapper.OrderMapper;
import com.deliverytest.api.order.repository.OrderRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderServiceTest {
    private final OrderRepository repository = mock(OrderRepository.class);
    private final OrderService service = new OrderService(repository, new OrderMapper(new OrderItemMapper()));
    private Order order;

    @BeforeEach
    void setUp() {
        order = Order.builder().id("order-id").customerName("Maria").deliveryAddress("Rua A")
                .status(OrderStatus.RECEBIDO).items(List.of()).build();
    }

    @Test
    void createsOrderAndReturnsPersistedRepresentation() {
        OrderRequest request = new OrderRequest("Maria", "Rua A",
                List.of(new OrderItemRequest("Pizza", 2, new BigDecimal("29.90"))));
        when(repository.save(any(Order.class))).thenReturn(order);

        OrderResponse response = service.create(request);

        ArgumentCaptor<Order> captured = ArgumentCaptor.forClass(Order.class);
        verify(repository).save(captured.capture());
        assertThat(captured.getValue().getCustomerName()).isEqualTo("Maria");
        assertThat(captured.getValue().getItems()).hasSize(1);
        assertThat(captured.getValue().getItems().getFirst().getOrder()).isSameAs(captured.getValue());
        assertThat(response.id()).isEqualTo("order-id");
        assertThat(response.status()).isEqualTo(OrderStatus.RECEBIDO);
    }

    @Test
    void findsExistingOrder() {
        when(repository.findById("order-id")).thenReturn(Optional.of(order));

        assertThat(service.findById("order-id").customerName()).isEqualTo("Maria");
    }

    @Test
    void missingOrderReportsRequestedIdentifier() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById("missing"))
                .isInstanceOfSatisfying(OrderNotFoundException.class,
                        exception -> assertThat(exception.getId()).isEqualTo("missing"));
    }

    @Test
    void updatesExistingOrderThroughDomainRules() {
        when(repository.findById("order-id")).thenReturn(Optional.of(order));

        service.updateStatus("order-id", new OrderStatusUpdateRequest(OrderStatus.EM_PREPARO));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.EM_PREPARO);
    }

    @Test
    void rejectsInvalidStatusWithoutChangingOrder() {
        when(repository.findById("order-id")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.updateStatus("order-id", new OrderStatusUpdateRequest(OrderStatus.ENTREGUE)))
                .isInstanceOf(InvalidOrderStatusException.class);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.RECEBIDO);
    }

    @Test
    void cannotUpdateMissingOrder() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateStatus("missing", new OrderStatusUpdateRequest(OrderStatus.EM_PREPARO)))
                .isInstanceOf(OrderNotFoundException.class);
        verify(repository, never()).save(any(Order.class));
    }

    @Test
    void preservesPaginationMetadataWhenMappingResults() {
        PageRequest page = PageRequest.of(2, 5);
        when(repository.findAll(page)).thenReturn(new PageImpl<>(List.of(order), page, 11));

        Page<OrderResponse> response = service.findAll(page);

        assertThat(response.getContent()).extracting(OrderResponse::id).containsExactly("order-id");
        assertThat(response.getNumber()).isEqualTo(2);
        assertThat(response.getSize()).isEqualTo(5);
        assertThat(response.getTotalElements()).isEqualTo(11);
    }

    @Test
    void returnsEmptyPageWhenNoOrdersExist() {
        PageRequest page = PageRequest.of(0, 10);
        when(repository.findAll(page)).thenReturn(Page.empty(page));

        assertThat(service.findAll(page)).isEmpty();
    }
}
