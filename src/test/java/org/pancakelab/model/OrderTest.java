package org.pancakelab.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.pancakelab.dto.PancakeDetails;
import org.pancakelab.exception.EmptyOrderException;
import org.pancakelab.exception.InvalidOrderStateException;
import org.pancakelab.exception.PancakeNotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class OrderTest {
    @Test
    public void GivenDeliveryAddress_WhenCreatingOrder_ThenOrderKeepsTheAddress() {
        // setup
        DeliveryAddress address = new DeliveryAddress(10, 20);

        // exercise
        Order order = new Order(address);

        // verify
        assertEquals(address, order.getAddress());
    }

    @Test
    public void GivenNewOrder_WhenCheckingStatus_ThenOrderIsCreated() {
        // exercise
        Order order = new Order(new DeliveryAddress(10, 20));

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
        Order order = new Order(new DeliveryAddress(10, 20));

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

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "CREATED")
    public void GivenOrderNotCreated_WhenAddingPancake_ThenInvalidOrderStateExceptionThrown(OrderStatus status) {
        // setup
        Order order = orderIn(status);

        // exercise
        InvalidOrderStateException exception =
                assertThrows(InvalidOrderStateException.class, () -> order.addPancake(Pancake.plain()));

        // verify
        assertEquals(order.getId(), exception.getOrderId());
        assertEquals(status.name(), exception.getStatus());
    }

    @Test
    public void GivenPancakesAdded_WhenGettingPancakes_ThenIdsAndDescriptionsInOrderOfAddition() {
        // setup
        Order order = new Order(new DeliveryAddress(10, 20));
        UUID first = order.addPancake(Pancake.plain().with(new Ingredient("milk chocolate")));
        UUID second = order.addPancake(Pancake.plain());

        // exercise
        List<PancakeDetails> pancakes = order.getPancakes();

        // verify
        List<PancakeDetails> expected = List.of(
                new PancakeDetails(first, "Delicious pancake with milk chocolate!"),
                new PancakeDetails(second, "Delicious pancake!"));
        assertEquals(expected, pancakes);
    }

    @Test
    public void GivenPancakeInOrder_WhenAddingIngredientsById_ThenPancakeDescriptionIncludesThem() {
        // setup
        Order order = new Order(new DeliveryAddress(10, 20));
        UUID pancakeId = order.addPancake(Pancake.plain());

        // exercise
        order.addIngredient(pancakeId, new Ingredient("dark chocolate"));
        order.addIngredient(pancakeId, new Ingredient("hazelnuts"));

        // verify
        PancakeDetails expected = new PancakeDetails(pancakeId, "Delicious pancake with dark chocolate, hazelnuts!");
        assertEquals(List.of(expected), order.getPancakes());
    }

    @ParameterizedTest
    @MethodSource("unknownPancakeIds")
    public void GivenUnknownPancakeId_WhenAddingIngredient_ThenPancakeNotFoundExceptionThrown(UUID pancakeId) {
        // setup
        Order order = orderIn(OrderStatus.CREATED);

        // exercise
        PancakeNotFoundException exception = assertThrows(PancakeNotFoundException.class,
                () -> order.addIngredient(pancakeId, new Ingredient("dark chocolate")));

        // verify
        assertEquals(pancakeId, exception.getPancakeId());
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "CREATED")
    public void GivenOrderNotCreated_WhenAddingIngredient_ThenInvalidOrderStateExceptionThrown(OrderStatus status) {
        // setup
        Order order = orderIn(status);

        // exercise
        InvalidOrderStateException exception = assertThrows(InvalidOrderStateException.class,
                () -> order.addIngredient(UUID.randomUUID(), new Ingredient("dark chocolate")));

        // verify
        assertEquals(order.getId(), exception.getOrderId());
        assertEquals(status.name(), exception.getStatus());
    }

    @Test
    public void GivenIdenticalPancakesInOrder_WhenRemovingOneById_ThenOnlyThatPancakeRemoved() {
        // setup
        Order order = new Order(new DeliveryAddress(10, 20));
        UUID first = order.addPancake(Pancake.plain());
        UUID second = order.addPancake(Pancake.plain());
        UUID third = order.addPancake(Pancake.plain());

        // exercise
        order.removePancake(second);

        // verify
        List<UUID> remainingIds = order.getPancakes().stream().map(PancakeDetails::id).toList();
        assertEquals(List.of(first, third), remainingIds);
    }

    @ParameterizedTest
    @MethodSource("unknownPancakeIds")
    public void GivenUnknownPancakeId_WhenRemovingPancake_ThenPancakeNotFoundExceptionThrown(UUID pancakeId) {
        // setup
        Order order = orderIn(OrderStatus.CREATED);

        // exercise
        PancakeNotFoundException exception =
                assertThrows(PancakeNotFoundException.class, () -> order.removePancake(pancakeId));

        // verify
        assertEquals(pancakeId, exception.getPancakeId());
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "CREATED")
    public void GivenOrderNotCreated_WhenRemovingPancake_ThenInvalidOrderStateExceptionThrown(OrderStatus status) {
        // setup
        Order order = orderIn(status);

        // exercise
        InvalidOrderStateException exception =
                assertThrows(InvalidOrderStateException.class, () -> order.removePancake(UUID.randomUUID()));

        // verify
        assertEquals(order.getId(), exception.getOrderId());
        assertEquals(status.name(), exception.getStatus());
    }

    @Test
    public void GivenManyPancakesAdded_WhenGettingPancakes_ThenTheyKeepTheOrderOfAddition() {
        // setup
        Order order = new Order(new DeliveryAddress(10, 20));
        List<UUID> addedIds = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            addedIds.add(order.addPancake(Pancake.plain()));
        }

        // exercise
        List<UUID> ids = order.getPancakes().stream().map(PancakeDetails::id).toList();

        // verify
        assertEquals(addedIds, ids);
    }

    @Test
    public void GivenOrderWithPancake_WhenModifyingReturnedPancakes_ThenUnsupportedOperationExceptionThrown() {
        // setup
        List<PancakeDetails> pancakes = orderIn(OrderStatus.CREATED).getPancakes();

        // exercise & verify
        assertThrows(UnsupportedOperationException.class, pancakes::clear);
    }

    @Test
    public void GivenEmptyOrderCancelled_WhenCompleting_ThenInvalidOrderStateExceptionThrown() {
        // setup
        Order order = new Order(new DeliveryAddress(10, 20));
        order.cancel();

        // exercise
        InvalidOrderStateException exception = assertThrows(InvalidOrderStateException.class, order::complete);

        // verify
        assertEquals("CANCELLED", exception.getStatus());
    }

    private static Stream<UUID> unknownPancakeIds() {
        return Stream.of(UUID.randomUUID(), null);
    }

    private Order orderIn(OrderStatus status) {
        Order order = new Order(new DeliveryAddress(10, 20));
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
