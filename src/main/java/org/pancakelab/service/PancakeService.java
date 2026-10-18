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

    public PancakeService(Set<String> menuIngredients, Set<Integer> buildings) {
        this.menu = new Menu(menuIngredients);
        this.buildingRegistry = new BuildingRegistry(buildings);
    }

    public UUID createOrder(int building, int room) {
        DeliveryAddress address = buildingRegistry.addressFor(building, room);
        Order order = new Order(address.building(), address.room());
        orders.save(order);
        return order.getId();
    }

    public UUID addPancake(UUID orderId) {
        return orders.find(orderId).addPancake(Pancake.plain());
    }

    public void addIngredient(UUID orderId, UUID pancakeId, String ingredient) {
        orders.find(orderId).addIngredient(pancakeId, menu.ingredient(ingredient));
    }

    public void removePancake(UUID orderId, UUID pancakeId) {
        orders.find(orderId).removePancake(pancakeId);
    }

    public List<PancakeDetails> viewOrder(UUID orderId) {
        return orders.find(orderId).getPancakes();
    }

    public void completeOrder(UUID orderId) {
        orders.find(orderId).complete();
    }

    public Set<UUID> listCompletedOrders() {
        return orderIdsWithStatus(OrderStatus.COMPLETED);
    }

    public void prepareOrder(UUID orderId) {
        orders.find(orderId).prepare();
    }

    public Set<UUID> listPreparedOrders() {
        return orderIdsWithStatus(OrderStatus.PREPARED);
    }

    public DeliveryDetails deliverOrder(UUID orderId) {
        Order order = orders.find(orderId);
        order.deliver();
        orders.remove(orderId);
        List<String> pancakes = order.getPancakes().stream().map(PancakeDetails::description).toList();
        return new DeliveryDetails(orderId, order.getBuilding(), order.getRoom(), pancakes);
    }

    public void cancelOrder(UUID orderId) {
        orders.find(orderId).cancel();
        orders.remove(orderId);
    }

    private Set<UUID> orderIdsWithStatus(OrderStatus status) {
        return orders.findByStatus(status).stream().map(Order::getId).collect(Collectors.toUnmodifiableSet());
    }
}
