package org.pancakelab.service;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.pancakelab.exception.InvalidOrderStateException;
import org.pancakelab.exception.PancakeLabException;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PancakeServiceLogTest {
    private static final Set<String> MENU = Set.of("dark chocolate", "milk chocolate", "whipped cream", "hazelnuts");
    private static final Set<Integer> BUILDINGS = Set.of(10, 20);

    private final InMemoryOrderLog log = new InMemoryOrderLog();
    private final PancakeService service = new PancakeService(MENU, BUILDINGS, log);

    @Test
    public void GivenRegisteredBuilding_WhenCreatingOrder_ThenCreationLoggedOnce() {
        // exercise
        UUID orderId = service.createOrder(10, 5);

        // verify
        String expected = "Created order %s for building 10, room 5.".formatted(orderId);
        assertEquals(List.of(expected), log.entries());
    }

    @Test
    public void GivenOrder_WhenAddingPancake_ThenAdditionLoggedOnce() {
        // setup
        UUID orderId = service.createOrder(10, 5);
        List<String> before = log.entries();

        // exercise
        UUID pancakeId = service.addPancake(orderId);

        // verify
        String expected = "Added pancake %s to order %s containing 1 pancake(s), for building 10, room 5."
                .formatted(pancakeId, orderId);
        assertEquals(List.of(expected), newEntriesSince(before));
    }

    @Test
    public void GivenPancake_WhenAddingIngredient_ThenIngredientLoggedOnce() {
        // setup
        UUID orderId = service.createOrder(10, 5);
        UUID pancakeId = service.addPancake(orderId);
        List<String> before = log.entries();

        // exercise
        service.addIngredient(orderId, pancakeId, "hazelnuts");

        // verify
        String expected = "Added ingredient 'hazelnuts' to pancake %s in order %s.".formatted(pancakeId, orderId);
        assertEquals(List.of(expected), newEntriesSince(before));
    }

    @Test
    public void GivenOrderWithPancakes_WhenRemovingOne_ThenRemovalLoggedOnce() {
        // setup
        UUID orderId = service.createOrder(10, 5);
        UUID removedId = service.addPancake(orderId);
        service.addPancake(orderId);
        List<String> before = log.entries();

        // exercise
        service.removePancake(orderId, removedId);

        // verify
        String expected = "Removed pancake %s from order %s now containing 1 pancake(s), for building 10, room 5."
                .formatted(removedId, orderId);
        assertEquals(List.of(expected), newEntriesSince(before));
    }

    @Test
    public void GivenOrder_WhenCancelling_ThenCancellationLoggedOnce() {
        // setup
        UUID orderId = service.createOrder(10, 5);
        service.addPancake(orderId);
        List<String> before = log.entries();

        // exercise
        service.cancelOrder(orderId);

        // verify
        String expected = "Cancelled order %s with 1 pancake(s) for building 10, room 5.".formatted(orderId);
        assertEquals(List.of(expected), newEntriesSince(before));
    }

    @Test
    public void GivenOrder_WhenCompleting_ThenCompletedLoggedOnce() {
        // setup
        UUID orderId = service.createOrder(10, 5);
        service.addPancake(orderId);
        List<String> before = log.entries();

        // exercise
        service.completeOrder(orderId);

        // verify
        String expected = "Completed order %s with 1 pancake(s) for building 10, room 5."
                .formatted(orderId);
        assertEquals(List.of(expected), newEntriesSince(before));
    }

    @Test
    public void GivenOrder_WhenPreparing_ThenPreparedLoggedOnce() {
        // setup
        UUID orderId = service.createOrder(10, 5);
        service.addPancake(orderId);
        service.completeOrder(orderId);
        List<String> before = log.entries();

        // exercise
        service.prepareOrder(orderId);

        // verify
        String expected = "Prepared order %s with 1 pancake(s) for building 10, room 5."
                .formatted(orderId);
        assertEquals(List.of(expected), newEntriesSince(before));
    }

    @Test
    public void GivenPreparedOrder_WhenDelivering_ThenDeliveryLoggedOnce() {
        // setup
        UUID orderId = service.createOrder(10, 5);
        service.addPancake(orderId);
        service.completeOrder(orderId);
        service.prepareOrder(orderId);
        List<String> before = log.entries();

        // exercise
        service.deliverOrder(orderId);

        // verify
        String expected = "Order %s with 1 pancake(s) for building 10, room 5 out for delivery.".formatted(orderId);
        assertEquals(List.of(expected), newEntriesSince(before));
    }

    @Test
    public void GivenPreparedOrder_WhenCancellingFails_ThenNothingLogged() {
        // setup
        UUID orderId = service.createOrder(10, 5);
        service.addPancake(orderId);
        service.completeOrder(orderId);
        service.prepareOrder(orderId);
        List<String> before = log.entries();

        // exercise
        assertThrows(InvalidOrderStateException.class, () -> service.cancelOrder(orderId));

        // verify
        assertEquals(List.of(), newEntriesSince(before));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("rejectedOperations")
    public void GivenRejectedOperation_WhenExecuted_ThenNothingLogged(RejectedOperation operation) {
        // setup
        UUID orderId = service.createOrder(10, 5);
        UUID pancakeId = service.addPancake(orderId);
        service.addIngredient(orderId, pancakeId, "hazelnuts");
        List<String> before = log.entries();

        // exercise
        assertThrows(PancakeLabException.class, () -> operation.execute(service, orderId, pancakeId));

        // verify
        assertEquals(List.of(), newEntriesSince(before));
    }

    private static Stream<Named<RejectedOperation>> rejectedOperations() {
        return Stream.of(
                rejected("create order for unknown building", (service, orderId, pancakeId) ->
                        service.createOrder(99, 5)),
                rejected("ingredient not on the menu", (service, orderId, pancakeId) ->
                        service.addIngredient(orderId, pancakeId, "mustard")),
                rejected("repeated ingredient", (service, orderId, pancakeId) ->
                        service.addIngredient(orderId, pancakeId, "hazelnuts")),
                rejected("ingredient for unknown pancake", (service, orderId, pancakeId) ->
                        service.addIngredient(orderId, UUID.randomUUID(), "hazelnuts")),
                rejected("removal of unknown pancake", (service, orderId, pancakeId) ->
                        service.removePancake(orderId, UUID.randomUUID())),
                rejected("pancake for unknown order", (service, orderId, pancakeId) ->
                        service.addPancake(UUID.randomUUID())),
                rejected("complete unknown order", (service, orderId, pancakeId) ->
                        service.completeOrder(UUID.randomUUID())),
                rejected("prepare unknown order", (service, orderId, pancakeId) ->
                        service.prepareOrder(UUID.randomUUID())),
                rejected("delivery of order not prepared", (service, orderId, pancakeId) ->
                        service.deliverOrder(orderId)));
    }

    private static Named<RejectedOperation> rejected(String rejection, RejectedOperation operation) {
        return Named.of(rejection, operation);
    }

    @FunctionalInterface
    private interface RejectedOperation {
        void execute(PancakeService service, UUID orderId, UUID pancakeId);
    }

    private List<String> newEntriesSince(List<String> before) {
        List<String> after = log.entries();
        return after.subList(before.size(), after.size());
    }
}
