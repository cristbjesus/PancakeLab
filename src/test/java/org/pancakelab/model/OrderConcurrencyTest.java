package org.pancakelab.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.pancakelab.ConcurrentRunner;
import org.pancakelab.exception.InvalidOrderStateException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OrderConcurrencyTest {
    private static final int THREADS = 16;

    @Test
    @Timeout(30)
    public void GivenManyThreads_WhenAddingPancakesToSameOrder_ThenNoPancakeIsLost() throws Exception {
        // setup
        int pancakesPerThread = 1000;
        Order order = new Order(new DeliveryAddress(10, 20));

        // exercise
        ConcurrentRunner.run(THREADS, thread -> {
            for (int i = 0; i < pancakesPerThread; i++) {
                order.addPancake(Pancake.plain());
            }
        });

        // verify
        assertEquals(THREADS * pancakesPerThread, order.getPancakes().size());
    }

    @Test
    @Timeout(30)
    public void GivenManyThreads_WhenAddingDifferentIngredientsToSamePancake_ThenNoIngredientIsLost() throws Exception {
        Set<String> expected = IntStream.range(0, THREADS).mapToObj(thread -> "ingredient " + thread).collect(toSet());
        for (int round = 0; round < 50; round++) {
            // setup
            Order order = new Order(new DeliveryAddress(10, 20));
            UUID pancakeId = order.addPancake(Pancake.plain());

            // exercise
            ConcurrentRunner.run(THREADS,
                    thread -> order.addIngredient(pancakeId, new Ingredient("ingredient " + thread)));

            // verify
            // compared as a set: the order of the ingredients depends on thread scheduling
            assertEquals(expected, ingredientsOf(order.getPancakes().get(0).description()));
        }
    }

    @Test
    @Timeout(30)
    public void GivenPreparedOrder_WhenManyThreadsDeliverIt_ThenOnlyOneSucceeds() throws Exception {
        for (int round = 0; round < 200; round++) {
            // setup
            Order order = new Order(new DeliveryAddress(10, 20));
            order.addPancake(Pancake.plain());
            order.complete();
            order.prepare();
            AtomicInteger deliveries = new AtomicInteger();

            // exercise
            ConcurrentRunner.run(THREADS, thread -> {
                try {
                    order.deliver();
                    deliveries.incrementAndGet();
                } catch (InvalidOrderStateException exception) {
                    // another thread delivered the order first
                }
            });

            // verify
            assertEquals(1, deliveries.get());
        }
    }

    @Test
    @Timeout(30)
    public void GivenCreatedOrder_WhenCompletingAndCancellingConcurrently_ThenOrderEndsCancelled() throws Exception {
        for (int round = 0; round < 200; round++) {
            // setup
            Order order = new Order(new DeliveryAddress(10, 20));
            order.addPancake(Pancake.plain());
            AtomicInteger completions = new AtomicInteger();
            AtomicInteger cancellations = new AtomicInteger();

            // exercise
            ConcurrentRunner.run(THREADS, thread -> {
                try {
                    if (thread % 2 == 0) {
                        order.complete();
                        completions.incrementAndGet();
                    } else {
                        order.cancel();
                        cancellations.incrementAndGet();
                    }
                } catch (InvalidOrderStateException exception) {
                    // another thread changed the status first
                }
            });

            // verify
            assertEquals(1, cancellations.get());
            assertTrue(completions.get() <= 1);
            assertEquals(OrderStatus.CANCELLED, order.getStatus());
        }
    }

    @Test
    @Timeout(30)
    public void GivenCompletedOrder_WhenManyThreadsPrepareIt_ThenOnlyOneSucceeds() throws Exception {
        for (int round = 0; round < 1000; round++) {
            // setup
            Order order = new Order(new DeliveryAddress(10, 20));
            order.addPancake(Pancake.plain());
            order.complete();
            AtomicInteger preparations = new AtomicInteger();

            // exercise
            ConcurrentRunner.run(THREADS, thread -> {
                try {
                    order.prepare();
                    preparations.incrementAndGet();
                } catch (InvalidOrderStateException exception) {
                    // another thread prepared the order first
                }
            });

            // verify
            assertEquals(1, preparations.get());
        }
    }

    @Test
    @Timeout(30)
    public void GivenManyThreads_WhenRemovingDifferentPancakesFromSameOrder_ThenAllAreRemoved() throws Exception {
        int pancakesPerThread = 500;
        for (int round = 0; round < 20; round++) {
            // setup
            Order order = new Order(new DeliveryAddress(10, 20));
            List<List<UUID>> idsPerThread = new ArrayList<>();
            for (int thread = 0; thread < THREADS; thread++) {
                List<UUID> ids = new ArrayList<>();
                for (int i = 0; i < pancakesPerThread; i++) {
                    ids.add(order.addPancake(Pancake.plain()));
                }
                idsPerThread.add(ids);
            }

            // exercise
            ConcurrentRunner.run(THREADS, thread -> idsPerThread.get(thread).forEach(order::removePancake));

            // verify
            assertEquals(List.of(), order.getPancakes());
        }
    }

    @Test
    @Timeout(30)
    public void GivenManyThreads_WhenReadingPancakesWhileOthersAddThem_ThenNoReadFails() throws Exception {
        // setup
        int operationsPerThread = 500;
        Order order = new Order(new DeliveryAddress(10, 20));

        // exercise
        ConcurrentRunner.run(THREADS, thread -> {
            for (int i = 0; i < operationsPerThread; i++) {
                if (thread % 2 == 0) {
                    order.addPancake(Pancake.plain());
                } else {
                    order.getPancakes();
                }
            }
        });

        // verify
        assertEquals(THREADS / 2 * operationsPerThread, order.getPancakes().size());
    }

    private static Set<String> ingredientsOf(String description) {
        String names = description.substring("Delicious pancake with ".length(), description.length() - 1);
        return Set.of(names.split(", "));
    }
}
