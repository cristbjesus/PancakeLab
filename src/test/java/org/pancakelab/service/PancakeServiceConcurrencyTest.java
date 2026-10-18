package org.pancakelab.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.pancakelab.ConcurrentRunner;
import org.pancakelab.exception.InvalidOrderStateException;
import org.pancakelab.exception.OrderNotFoundException;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PancakeServiceConcurrencyTest {
    private static final int THREADS = 16;
    private static final Set<String> MENU = Set.of("dark chocolate", "milk chocolate", "whipped cream", "hazelnuts");
    private static final Set<Integer> BUILDINGS = Set.of(10, 20);

    private final InMemoryOrderLog log = new InMemoryOrderLog();
    private final PancakeService service = new PancakeService(MENU, BUILDINGS, log);

    @Test
    @Timeout(30)
    public void GivenPreparedOrder_WhenManyThreadsDeliverIt_ThenDeliveredAndLoggedOnlyOnce() throws Exception {
        for (int round = 0; round < 100; round++) {
            // setup
            UUID orderId = preparedOrder();
            List<String> before = log.entries();
            AtomicInteger deliveries = new AtomicInteger();

            // exercise
            ConcurrentRunner.run(THREADS, thread -> {
                try {
                    service.deliverOrder(orderId);
                    deliveries.incrementAndGet();
                } catch (InvalidOrderStateException | OrderNotFoundException exception) {
                    // another thread delivered the order first
                }
            });

            // verify
            assertEquals(1, deliveries.get());
            assertEquals(1, log.entries().size() - before.size());
        }
    }

    private UUID preparedOrder() {
        UUID orderId = service.createOrder(10, 5);
        service.addPancake(orderId);
        service.completeOrder(orderId);
        service.prepareOrder(orderId);
        return orderId;
    }
}
