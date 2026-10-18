package org.pancakelab;

import org.pancakelab.dto.DeliveryDetails;
import org.pancakelab.dto.PancakeDetails;
import org.pancakelab.exception.PancakeLabException;
import org.pancakelab.service.InMemoryOrderLog;
import org.pancakelab.service.PancakeService;

import java.io.PrintStream;
import java.util.Set;
import java.util.UUID;

/**
 * Demo client of the Pancake Lab API: runs a complete, hard-coded use case.
 * Change it freely to simulate other ways of using {@link PancakeService}.
 */
public class Main {
    private static final Set<String> DOJO_MENU =
            Set.of("dark chocolate", "milk chocolate", "whipped cream", "hazelnuts");
    private static final Set<Integer> DOJO_BUILDINGS = Set.of(1, 2, 3, 10);

    public static void main(String[] args) {
        run(System.out);
    }

    static void run(PrintStream out) {
        InMemoryOrderLog log = new InMemoryOrderLog();
        PancakeService service = new PancakeService(DOJO_MENU, DOJO_BUILDINGS, log);

        out.println("== A disciple orders pancakes to building 10, room 5");
        UUID orderId = service.createOrder(10, 5);
        UUID chocolatePancake = service.addPancake(orderId);
        service.addIngredient(orderId, chocolatePancake, "dark chocolate");
        service.addIngredient(orderId, chocolatePancake, "whipped cream");
        service.addIngredient(orderId, chocolatePancake, "hazelnuts");
        UUID milkPancake = service.addPancake(orderId);
        service.addIngredient(orderId, milkPancake, "milk chocolate");
        UUID plainPancake = service.addPancake(orderId);
        printOrder(out, service, orderId);

        out.println("== The disciple changes their mind about the plain pancake");
        service.removePancake(orderId, plainPancake);
        printOrder(out, service, orderId);

        out.println("== Dr. Fu Man Chu tries to wreak havoc");
        expectRejected(out, "add mustard", () -> service.addIngredient(orderId, milkPancake, "mustard"));
        expectRejected(out, "add hazelnuts twice", () -> service.addIngredient(orderId, chocolatePancake, "hazelnuts"));
        expectRejected(out, "order to building 99", () -> service.createOrder(99, 1));
        expectRejected(out, "order to room 0", () -> service.createOrder(10, 0));
        expectRejected(out, "deliver before it is prepared", () -> service.deliverOrder(orderId));

        out.println("== The disciple completes the order and the chef prepares it");
        service.completeOrder(orderId);
        out.println("Completed orders: " + service.listCompletedOrders());
        service.prepareOrder(orderId);
        out.println("Completed orders: " + service.listCompletedOrders());
        out.println("Prepared orders: " + service.listPreparedOrders());
        expectRejected(out, "add a pancake after preparing", () -> service.addPancake(orderId));

        out.println("== The delivery service delivers it");
        DeliveryDetails delivery = service.deliverOrder(orderId);
        out.printf("Delivering to building %d, room %d:%n", delivery.building(), delivery.room());
        delivery.pancakes().forEach(pancake -> out.println("  " + pancake));
        expectRejected(out, "deliver it again", () -> service.deliverOrder(orderId));

        out.println("== Another disciple completes an order and then cancels it");
        UUID cancelledOrderId = service.createOrder(2, 7);
        service.addPancake(cancelledOrderId);
        service.completeOrder(cancelledOrderId);
        service.cancelOrder(cancelledOrderId);
        expectRejected(out, "view the cancelled order", () -> service.viewOrder(cancelledOrderId));

        out.println("== Order log");
        log.entries().forEach(entry -> out.println("  " + entry));
    }

    private static void printOrder(PrintStream out, PancakeService service, UUID orderId) {
        out.println("Order " + orderId + ":");
        for (PancakeDetails pancake : service.viewOrder(orderId)) {
            out.println("  " + pancake.id() + "  " + pancake.description());
        }
    }

    private static void expectRejected(PrintStream out, String action, Runnable operation) {
        try {
            operation.run();
        } catch (PancakeLabException exception) {
            out.println("  " + action + ": rejected - " + exception.getMessage());
            return;
        }
        throw new IllegalStateException("Expected '%s' to be rejected, but it was accepted.".formatted(action));
    }
}
