package org.pancakelab.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.pancakelab.exception.OrderNotFoundException;
import org.pancakelab.model.Order;
import org.pancakelab.model.OrderStatus;
import org.pancakelab.model.Pancake;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class OrderRepositoryTest {
    private final OrderRepository repository = new OrderRepository();

    @Test
    public void GivenSavedOrder_WhenFindingById_ThenOrderReturned() {
        // setup
        Order order = new Order(10, 20);
        repository.save(order);

        // exercise
        Order found = repository.find(order.getId());

        // verify
        assertSame(order, found);
    }

    @ParameterizedTest
    @MethodSource("unknownOrderIds")
    public void GivenUnknownOrderId_WhenFindingById_ThenOrderNotFoundExceptionThrown(UUID orderId) {
        // exercise
        OrderNotFoundException exception =
                assertThrows(OrderNotFoundException.class, () -> repository.find(orderId));

        // verify
        assertEquals(orderId, exception.getOrderId());
    }

    @Test
    public void GivenSavedOrder_WhenRemoving_ThenOrderNoLongerFound() {
        // setup
        Order order = new Order(10, 20);
        repository.save(order);

        // exercise
        repository.remove(order.getId());

        // verify
        assertThrows(OrderNotFoundException.class, () -> repository.find(order.getId()));
    }

    @ParameterizedTest
    @MethodSource("unknownOrderIds")
    public void GivenUnknownOrderId_WhenRemoving_ThenOrderNotFoundExceptionThrown(UUID orderId) {
        // exercise
        OrderNotFoundException exception =
                assertThrows(OrderNotFoundException.class, () -> repository.remove(orderId));

        // verify
        assertEquals(orderId, exception.getOrderId());
    }

    @Test
    public void GivenOrdersInDifferentStatuses_WhenFindingByStatus_ThenOnlyMatchingOrdersReturned() {
        // setup
        Order created = new Order(10, 20);
        Order firstCompleted = completedOrder();
        Order secondCompleted = completedOrder();
        repository.save(created);
        repository.save(firstCompleted);
        repository.save(secondCompleted);

        // exercise
        List<Order> found = repository.findByStatus(OrderStatus.COMPLETED);

        // verify
        assertEquals(2, found.size());
        assertEquals(Set.of(firstCompleted, secondCompleted), Set.copyOf(found));
    }

    private static Stream<UUID> unknownOrderIds() {
        return Stream.of(UUID.randomUUID(), null);
    }

    private static Order completedOrder() {
        Order order = new Order(10, 20);
        order.addPancake(Pancake.plain());
        order.complete();
        return order;
    }
}
