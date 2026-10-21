package com.deliverytest.api.order.entity;

import com.deliverytest.api.order.exception.InvalidOrderStatusException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {
    private static final Set<String> ALLOWED = Set.of(
            "RECEBIDO:EM_PREPARO", "RECEBIDO:CANCELADO",
            "EM_PREPARO:SAIU_PARA_ENTREGA", "EM_PREPARO:CANCELADO",
            "SAIU_PARA_ENTREGA:ENTREGUE");

    @ParameterizedTest
    @MethodSource("transitions")
    void enforcesEveryStatusTransition(OrderStatus from, OrderStatus to, boolean allowed) {
        Order order = Order.builder().status(from).build();

        assertThat(from.canTransitionTo(to)).isEqualTo(allowed);
        if (allowed) {
            order.updateStatus(to);
            assertThat(order.getStatus()).isEqualTo(to);
        } else {
            assertThatThrownBy(() -> order.updateStatus(to))
                    .isInstanceOfSatisfying(InvalidOrderStatusException.class, exception -> {
                        assertThat(exception.getFrom()).isEqualTo(from);
                        assertThat(exception.getTo()).isEqualTo(to);
                    });
            assertThat(order.getStatus()).isEqualTo(from);
        }
    }

    @Test
    void initializesIdentityStatusItemsAndTimestamps() {
        Order order = Order.builder().build();
        LocalDateTime before = LocalDateTime.now();

        order.onCreate();

        assertThat(order.getId()).matches("[0-9A-HJKMNP-TV-Z]{26}");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.RECEBIDO);
        assertThat(order.getItems()).isEmpty();
        assertThat(order.getCreatedAt()).isBetween(before, LocalDateTime.now());
        assertThat(order.getUpdatedAt()).isEqualTo(order.getCreatedAt());
    }

    @Test
    void preservesExistingIdentityStatusAndItemsOnCreate() {
        OrderItem item = OrderItem.builder().name("Pizza").build();
        Order order = Order.builder().id("existing-id").status(OrderStatus.EM_PREPARO)
                .items(new ArrayList<>(List.of(item))).build();

        order.onCreate();

        assertThat(order.getId()).isEqualTo("existing-id");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.EM_PREPARO);
        assertThat(order.getItems()).containsExactly(item);
    }

    @Test
    void addingItemMaintainsBothSidesOfRelationship() {
        Order order = Order.builder().items(new ArrayList<>()).build();
        OrderItem item = OrderItem.builder().name("Pizza").build();

        order.addItem(item);

        assertThat(order.getItems()).containsExactly(item);
        assertThat(item.getOrder()).isSameAs(order);
    }

    @Test
    void updateChangesOnlyModificationTimestamp() {
        LocalDateTime initial = LocalDateTime.of(2020, 1, 1, 12, 0);
        Order order = Order.builder().createdAt(initial).updatedAt(initial).build();
        LocalDateTime before = LocalDateTime.now();

        order.onUpdate();

        assertThat(order.getCreatedAt()).isEqualTo(initial);
        assertThat(order.getUpdatedAt()).isBetween(before, LocalDateTime.now());
    }

    @Test
    void itemGeneratesIdentityOnlyWhenAbsent() {
        OrderItem generated = OrderItem.builder().build();
        OrderItem existing = OrderItem.builder().id("existing-id").build();

        generated.onCreate();
        String generatedId = generated.getId();
        generated.onCreate();
        existing.onCreate();

        assertThat(generatedId).matches("[0-9A-HJKMNP-TV-Z]{26}");
        assertThat(generated.getId()).isEqualTo(generatedId);
        assertThat(existing.getId()).isEqualTo("existing-id");
    }

    static Stream<Arguments> transitions() {
        return Stream.of(OrderStatus.values()).flatMap(from ->
                Stream.concat(Stream.of(OrderStatus.values()), Stream.of((OrderStatus) null))
                        .map(to -> Arguments.of(from, to, ALLOWED.contains(from + ":" + to))));
    }
}
