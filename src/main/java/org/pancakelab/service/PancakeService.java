package org.pancakelab.service;

import org.pancakelab.dto.DeliveryDetails;
import org.pancakelab.dto.PancakeDetails;
import org.pancakelab.model.BuildingRegistry;
import org.pancakelab.model.DeliveryAddress;
import org.pancakelab.model.Menu;
import org.pancakelab.model.Order;
import org.pancakelab.model.OrderStatus;
import org.pancakelab.model.Pancake;
import org.pancakelab.repository.OrderRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class PancakeService {
    private final OrderRepository orders = new OrderRepository();
    private final Menu menu;
    private final BuildingRegistry buildingRegistry;
    private final OrderLog log;

    public PancakeService(Set<String> menuIngredients, Set<Integer> buildings) {
        this(menuIngredients, buildings, new InMemoryOrderLog());
    }

    public PancakeService(Set<String> menuIngredients, Set<Integer> buildings, OrderLog log) {
        this.menu = new Menu(menuIngredients);
        this.buildingRegistry = new BuildingRegistry(buildings);
        this.log = log;
    }

    public UUID createOrder(int building, int room) {
        Order order = new Order(buildingRegistry.addressFor(building, room));
        orders.save(order);
        UUID orderId = order.getId();
        log.log("Created order %s for %s.".formatted(orderId, destination(order)));
        return orderId;
    }

    public UUID addPancake(UUID orderId) {
        Order order = orders.find(orderId);
        UUID pancakeId = order.addPancake(Pancake.plain());
        log.log("Added pancake %s to order %s containing %d pancake(s), for %s."
                .formatted(pancakeId, orderId, order.getPancakes().size(), destination(order)));
        return pancakeId;
    }

    public void addIngredient(UUID orderId, UUID pancakeId, String ingredient) {
        orders.find(orderId).addIngredient(pancakeId, menu.ingredient(ingredient));
        log.log("Added ingredient '%s' to pancake %s in order %s.".formatted(ingredient, pancakeId, orderId));
    }

    public void removePancake(UUID orderId, UUID pancakeId) {
        Order order = orders.find(orderId);
        order.removePancake(pancakeId);
        log.log("Removed pancake %s from order %s now containing %d pancake(s), for %s."
                .formatted(pancakeId, orderId, order.getPancakes().size(), destination(order)));
    }

    public List<PancakeDetails> viewOrder(UUID orderId) {
        return orders.find(orderId).getPancakes();
    }

    public void completeOrder(UUID orderId) {
        Order order = orders.find(orderId);
        order.complete();
        log.log("Completed order %s with %s pancake(s) for %s."
                .formatted(orderId, order.getPancakes().size(), destination(order)));
    }

    public Set<UUID> listCompletedOrders() {
        return orderIdsWithStatus(OrderStatus.COMPLETED);
    }

    public void prepareOrder(UUID orderId) {
        Order order = orders.find(orderId);
        order.prepare();
        log.log("Prepared order %s with %s pancake(s) for %s."
                .formatted(orderId, order.getPancakes().size(), destination(order)));
    }

    public Set<UUID> listPreparedOrders() {
        return orderIdsWithStatus(OrderStatus.PREPARED);
    }

    public DeliveryDetails deliverOrder(UUID orderId) {
        Order order = orders.find(orderId);
        order.deliver();
        orders.remove(orderId);
        List<String> pancakes = order.getPancakes().stream().map(PancakeDetails::description).toList();
        log.log("Order %s with %d pancake(s) for %s out for delivery."
                .formatted(orderId, pancakes.size(), destination(order)));
        DeliveryAddress address = order.getAddress();
        return new DeliveryDetails(orderId, address.building(), address.room(), pancakes);
    }

    public void cancelOrder(UUID orderId) {
        Order order = orders.find(orderId);
        order.cancel();
        orders.remove(orderId);
        log.log("Cancelled order %s with %d pancake(s) for %s."
                .formatted(orderId, order.getPancakes().size(), destination(order)));
    }

    private Set<UUID> orderIdsWithStatus(OrderStatus status) {
        return orders.findByStatus(status).stream().map(Order::getId).collect(Collectors.toUnmodifiableSet());
    }

    private static String destination(Order order) {
        DeliveryAddress address = order.getAddress();
        return "building %d, room %d".formatted(address.building(), address.room());
    }
}
