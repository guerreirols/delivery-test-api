package com.deliverytest.api.order.mapper;

import com.deliverytest.api.order.dto.OrderItemRequest;
import com.deliverytest.api.order.dto.OrderRequest;
import com.deliverytest.api.order.dto.OrderResponse;
import com.deliverytest.api.order.entity.Order;
import com.deliverytest.api.order.entity.OrderItem;
import com.deliverytest.api.order.entity.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderMapperTest {
    private final OrderItemMapper itemMapper = new OrderItemMapper();
    private final OrderMapper mapper = new OrderMapper(itemMapper);

    @Test
    void mapsRequestAndLinksEveryItemWithoutCreatingPersistenceMetadata() {
        OrderRequest request = new OrderRequest("Maria", "Rua A", List.of(
                new OrderItemRequest("Pizza", 2, new BigDecimal("29.90")),
                new OrderItemRequest("Suco", 1, new BigDecimal("8.50"))));

        Order order = mapper.toEntity(request);

        assertThat(order.getCustomerName()).isEqualTo("Maria");
        assertThat(order.getDeliveryAddress()).isEqualTo("Rua A");
        assertThat(order.getId()).isNull();
        assertThat(order.getCreatedAt()).isNull();
        assertThat(order.getItems()).extracting(OrderItem::getName).containsExactly("Pizza", "Suco");
        assertThat(order.getItems()).allSatisfy(item -> assertThat(item.getOrder()).isSameAs(order));
        assertThat(order.getItems().getFirst().getQuantity()).isEqualTo(2);
        assertThat(order.getItems().getFirst().getUnitPrice()).isEqualByComparingTo("29.90");
    }

    @Test
    void mapsAllResponseFieldsIncludingMonetaryValuesAndTimestamps() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 12, 0);
        OrderItem item = OrderItem.builder().id("item-id").name("Pizza").quantity(2)
                .unitPrice(new BigDecimal("29.90")).build();
        Order order = Order.builder().id("order-id").customerName("Maria").deliveryAddress("Rua A")
                .status(OrderStatus.EM_PREPARO).items(List.of(item))
                .createdAt(createdAt).updatedAt(createdAt.plusMinutes(5)).build();

        OrderResponse response = mapper.toResponse(order);

        assertThat(response.id()).isEqualTo("order-id");
        assertThat(response.customerName()).isEqualTo("Maria");
        assertThat(response.deliveryAddress()).isEqualTo("Rua A");
        assertThat(response.status()).isEqualTo(OrderStatus.EM_PREPARO);
        assertThat(response.createdAt()).isEqualTo(createdAt);
        assertThat(response.updatedAt()).isEqualTo(createdAt.plusMinutes(5));
        assertThat(response.items()).containsExactly(itemMapper.toResponse(item));
        assertThat(response.items().getFirst().id()).isEqualTo("item-id");
        assertThat(response.items().getFirst().name()).isEqualTo("Pizza");
        assertThat(response.items().getFirst().quantity()).isEqualTo(2);
        assertThat(response.items().getFirst().unitPrice()).isEqualByComparingTo("29.90");
    }

    @Test
    void mapsEmptyCollectionsWithoutInventingItems() {
        Order order = mapper.toEntity(new OrderRequest("Maria", "Rua A", List.of()));

        assertThat(order.getItems()).isEmpty();
        assertThat(mapper.toResponse(order).items()).isEmpty();
    }
}
