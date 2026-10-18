package org.pancakelab.repository;

import org.pancakelab.exception.OrderNotFoundException;
import org.pancakelab.model.Order;
import org.pancakelab.model.OrderStatus;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class OrderRepository {
    private final Map<UUID, Order> orders = new ConcurrentHashMap<>();

    public void save(Order order) {
        orders.put(order.getId(), order);
    }

    public Order find(UUID orderId) {
        Order order = orderId == null ? null : orders.get(orderId);
        if (order == null) {
            throw new OrderNotFoundException(orderId);
        }
        return order;
    }

    public List<Order> findByStatus(OrderStatus status) {
        return orders.values().stream().filter(order -> order.getStatus() == status).toList();
    }

    public void remove(UUID orderId) {
        if (orderId == null || orders.remove(orderId) == null) {
            throw new OrderNotFoundException(orderId);
        }
    }
}
