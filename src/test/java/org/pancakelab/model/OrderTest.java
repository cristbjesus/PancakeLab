package org.pancakelab.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.pancakelab.exception.EmptyOrderException;
import org.pancakelab.exception.InvalidOrderStateException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class OrderTest {
    @Test
    public void GivenNewOrder_WhenCheckingStatus_ThenOrderIsCreated() {
        // exercise
        Order order = new Order(10, 20);

        // verify
        assertEquals(OrderStatus.CREATED, order.getStatus());
    }

    @Test
    public void GivenOrderWithPancake_WhenCompleting_ThenOrderIsCompleted() {
        // setup
        Order order = orderIn(OrderStatus.CREATED);

        // exercise
        order.complete();

        // verify
        assertEquals(OrderStatus.COMPLETED, order.getStatus());
    }

    @Test
    public void GivenEmptyOrder_WhenCompleting_ThenEmptyOrderExceptionThrown() {
        // setup
        Order order = new Order(10, 20);

        // exercise
        EmptyOrderException exception = assertThrows(EmptyOrderException.class, order::complete);

        // verify
        assertEquals(order.getId(), exception.getOrderId());
    }

    @Test
    public void GivenCompletedOrder_WhenPreparing_ThenOrderIsPrepared() {
        // setup
        Order order = orderIn(OrderStatus.COMPLETED);

        // exercise
        order.prepare();

        // verify
        assertEquals(OrderStatus.PREPARED, order.getStatus());
    }

    @Test
    public void GivenPreparedOrder_WhenDelivering_ThenOrderIsDelivered() {
        // setup
        Order order = orderIn(OrderStatus.PREPARED);

        // exercise
        order.deliver();

        // verify
        assertEquals(OrderStatus.DELIVERED, order.getStatus());
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"CREATED", "COMPLETED"})
    public void GivenOrderNotYetPrepared_WhenCancelling_ThenOrderIsCancelled(OrderStatus status) {
        // setup
        Order order = orderIn(status);

        // exercise
        order.cancel();

        // verify
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "CREATED")
    public void GivenOrderNotCreated_WhenCompleting_ThenInvalidOrderStateExceptionThrown(OrderStatus status) {
        // setup
        Order order = orderIn(status);

        // exercise
        InvalidOrderStateException exception = assertThrows(InvalidOrderStateException.class, order::complete);

        // verify
        assertEquals(order.getId(), exception.getOrderId());
        assertEquals(status.name(), exception.getStatus());
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "COMPLETED")
    public void GivenOrderNotCompleted_WhenPreparing_ThenInvalidOrderStateExceptionThrown(OrderStatus status) {
        // setup
        Order order = orderIn(status);

        // exercise
        InvalidOrderStateException exception = assertThrows(InvalidOrderStateException.class, order::prepare);

        // verify
        assertEquals(order.getId(), exception.getOrderId());
        assertEquals(status.name(), exception.getStatus());
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "PREPARED")
    public void GivenOrderNotPrepared_WhenDelivering_ThenInvalidOrderStateExceptionThrown(OrderStatus status) {
        // setup
        Order order = orderIn(status);

        // exercise
        InvalidOrderStateException exception = assertThrows(InvalidOrderStateException.class, order::deliver);

        // verify
        assertEquals(order.getId(), exception.getOrderId());
        assertEquals(status.name(), exception.getStatus());
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"PREPARED", "DELIVERED", "CANCELLED"})
    public void GivenOrderAlreadyPrepared_WhenCancelling_ThenInvalidOrderStateExceptionThrown(OrderStatus status) {
        // setup
        Order order = orderIn(status);

        // exercise
        InvalidOrderStateException exception = assertThrows(InvalidOrderStateException.class, order::cancel);

        // verify
        assertEquals(order.getId(), exception.getOrderId());
        assertEquals(status.name(), exception.getStatus());
    }

    @Test
    public void GivenEmptyOrderCancelled_WhenCompleting_ThenInvalidOrderStateExceptionThrown() {
        // setup
        Order order = new Order(10, 20);
        order.cancel();

        // exercise
        InvalidOrderStateException exception = assertThrows(InvalidOrderStateException.class, order::complete);

        // verify
        assertEquals("CANCELLED", exception.getStatus());
    }

    private Order orderIn(OrderStatus status) {
        Order order = new Order(10, 20);
        order.addPancake(Pancake.plain());
        switch (status) {
            case CREATED -> {
            }
            case COMPLETED -> order.complete();
            case PREPARED -> {
                order.complete();
                order.prepare();
            }
            case DELIVERED -> {
                order.complete();
                order.prepare();
                order.deliver();
            }
            case CANCELLED -> order.cancel();
        }
        return order;
    }
}
