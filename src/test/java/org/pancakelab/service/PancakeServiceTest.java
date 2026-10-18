package org.pancakelab.service;

import org.junit.jupiter.api.Test;
import org.pancakelab.dto.DeliveryDetails;
import org.pancakelab.dto.PancakeDetails;
import org.pancakelab.exception.InvalidOrderStateException;
import org.pancakelab.exception.InvalidRoomException;
import org.pancakelab.exception.OrderNotFoundException;
import org.pancakelab.exception.UnknownBuildingException;
import org.pancakelab.exception.UnknownIngredientException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PancakeServiceTest {
    private static final Set<String> MENU = Set.of("dark chocolate", "milk chocolate", "whipped cream", "hazelnuts");
    private static final Set<Integer> BUILDINGS = Set.of(10, 20);

    private final PancakeService service = new PancakeService(MENU, BUILDINGS);

    @Test
    public void GivenRegisteredBuilding_WhenCreatingOrder_ThenEmptyOrderCreated() {
        // exercise
        UUID orderId = service.createOrder(10, 5);

        // verify
        assertEquals(List.of(), service.viewOrder(orderId));
    }

    @Test
    public void GivenUnregisteredBuilding_WhenCreatingOrder_ThenUnknownBuildingExceptionThrown() {
        // exercise
        UnknownBuildingException exception =
                assertThrows(UnknownBuildingException.class, () -> service.createOrder(99, 5));

        // verify
        assertEquals(99, exception.getBuilding());
    }

    @Test
    public void GivenNonPositiveRoom_WhenCreatingOrder_ThenInvalidRoomExceptionThrown() {
        // exercise
        InvalidRoomException exception = assertThrows(InvalidRoomException.class, () -> service.createOrder(10, 0));

        // verify
        assertEquals(0, exception.getRoom());
    }

    @Test
    public void GivenOrder_WhenAddingPancakeIngredientByIngredient_ThenOrderShowsThatPancake() {
        // setup
        UUID orderId = service.createOrder(10, 5);

        // exercise
        UUID pancakeId = service.addPancake(orderId);
        service.addIngredient(orderId, pancakeId, "dark chocolate");
        service.addIngredient(orderId, pancakeId, "whipped cream");

        // verify
        PancakeDetails expected =
                new PancakeDetails(pancakeId, "Delicious pancake with dark chocolate, whipped cream!");
        assertEquals(List.of(expected), service.viewOrder(orderId));
    }

    @Test
    public void GivenIngredientNotOnMenu_WhenAddingIt_ThenUnknownIngredientExceptionThrown() {
        // setup
        UUID orderId = service.createOrder(10, 5);
        UUID pancakeId = service.addPancake(orderId);

        // exercise
        UnknownIngredientException exception = assertThrows(UnknownIngredientException.class,
                () -> service.addIngredient(orderId, pancakeId, "mustard"));

        // verify
        assertEquals("mustard", exception.getIngredient());
    }

    @Test
    public void GivenOrderWithPancakes_WhenRemovingOneById_ThenOrderShowsOnlyTheOthers() {
        // setup
        UUID orderId = service.createOrder(10, 5);
        UUID removedId = service.addPancake(orderId);
        UUID keptId = service.addPancake(orderId);

        // exercise
        service.removePancake(orderId, removedId);

        // verify
        assertEquals(List.of(new PancakeDetails(keptId, "Delicious pancake!")), service.viewOrder(orderId));
    }

    @Test
    public void GivenOrderWithPancake_WhenCompleting_ThenOrderListedAsCompleted() {
        // setup
        UUID orderId = orderWithPancake();

        // exercise
        service.completeOrder(orderId);

        // verify
        assertEquals(Set.of(orderId), service.listCompletedOrders());
    }

    @Test
    public void GivenCompletedOrders_WhenListing_ThenReturnedSetCannotBeModified() {
        // setup
        service.completeOrder(orderWithPancake());

        // exercise
        Set<UUID> completedOrders = service.listCompletedOrders();

        // verify
        assertThrows(UnsupportedOperationException.class, completedOrders::clear);
    }

    @Test
    public void GivenCompletedOrder_WhenPreparing_ThenOrderMovesFromCompletedToPrepared() {
        // setup
        UUID orderId = orderWithPancake();
        service.completeOrder(orderId);

        // exercise
        service.prepareOrder(orderId);

        // verify
        assertEquals(Set.of(), service.listCompletedOrders());
        assertEquals(Set.of(orderId), service.listPreparedOrders());
    }

    @Test
    public void GivenPreparedOrder_WhenDelivering_ThenDeliveryDetailsReturnedAndOrderRemoved() {
        // setup
        UUID orderId = service.createOrder(10, 5);
        UUID pancakeId = service.addPancake(orderId);
        service.addIngredient(orderId, pancakeId, "milk chocolate");
        service.completeOrder(orderId);
        service.prepareOrder(orderId);

        // exercise
        DeliveryDetails delivery = service.deliverOrder(orderId);

        // verify
        assertEquals(new DeliveryDetails(orderId, 10, 5, List.of("Delicious pancake with milk chocolate!")), delivery);
        assertEquals(Set.of(), service.listPreparedOrders());
        assertThrows(OrderNotFoundException.class, () -> service.viewOrder(orderId));
    }

    @Test
    public void GivenOrder_WhenCancelling_ThenOrderRemoved() {
        // setup
        UUID orderId = orderWithPancake();

        // exercise
        service.cancelOrder(orderId);

        // verify
        assertThrows(OrderNotFoundException.class, () -> service.viewOrder(orderId));
    }

    @Test
    public void GivenCompletedOrder_WhenCancelling_ThenOrderNoLongerListedAsCompleted() {
        // setup
        UUID orderId = orderWithPancake();
        service.completeOrder(orderId);

        // exercise
        service.cancelOrder(orderId);

        // verify
        assertEquals(Set.of(), service.listCompletedOrders());
        assertThrows(OrderNotFoundException.class, () -> service.viewOrder(orderId));
    }

    @Test
    public void GivenPreparedOrder_WhenCancelling_ThenInvalidOrderStateExceptionThrownAndOrderKept() {
        // setup
        UUID orderId = orderWithPancake();
        service.completeOrder(orderId);
        service.prepareOrder(orderId);

        // exercise
        InvalidOrderStateException exception =
                assertThrows(InvalidOrderStateException.class, () -> service.cancelOrder(orderId));

        // verify
        assertEquals("PREPARED", exception.getStatus());
        assertEquals(Set.of(orderId), service.listPreparedOrders());
    }

    @Test
    public void GivenUnknownOrderId_WhenViewingOrder_ThenOrderNotFoundExceptionThrown() {
        // setup
        UUID unknownId = UUID.randomUUID();

        // exercise
        OrderNotFoundException exception =
                assertThrows(OrderNotFoundException.class, () -> service.viewOrder(unknownId));

        // verify
        assertEquals(unknownId, exception.getOrderId());
    }

    @Test
    public void GivenOrderNotPrepared_WhenDelivering_ThenInvalidOrderStateExceptionThrownAndOrderKept() {
        // setup
        UUID orderId = orderWithPancake();
        service.completeOrder(orderId);

        // exercise
        InvalidOrderStateException exception =
                assertThrows(InvalidOrderStateException.class, () -> service.deliverOrder(orderId));

        // verify
        assertEquals("COMPLETED", exception.getStatus());
        assertEquals(Set.of(orderId), service.listCompletedOrders());
    }

    private UUID orderWithPancake() {
        UUID orderId = service.createOrder(10, 5);
        service.addPancake(orderId);
        return orderId;
    }
}
