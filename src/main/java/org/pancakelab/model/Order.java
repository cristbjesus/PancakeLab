package org.pancakelab.model;

import org.pancakelab.dto.PancakeDetails;
import org.pancakelab.exception.EmptyOrderException;
import org.pancakelab.exception.InvalidOrderStateException;
import org.pancakelab.exception.PancakeNotFoundException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class Order {
    private final UUID id;
    private final DeliveryAddress address;
    private final Map<UUID, Pancake> pancakes = new LinkedHashMap<>();
    private OrderStatus status = OrderStatus.CREATED;

    public Order(DeliveryAddress address) {
        this.id = UUID.randomUUID();
        this.address = address;
    }

    public synchronized OrderStatus getStatus() {
        return status;
    }

    public synchronized UUID addPancake(Pancake pancake) {
        requireStatus(OrderStatus.CREATED);
        UUID pancakeId = UUID.randomUUID();
        pancakes.put(pancakeId, pancake);
        return pancakeId;
    }

    public synchronized void addIngredient(UUID pancakeId, Ingredient ingredient) {
        requireStatus(OrderStatus.CREATED);
        Pancake pancake = pancakes.get(pancakeId);
        if (pancake == null) {
            throw new PancakeNotFoundException(pancakeId);
        }
        pancakes.put(pancakeId, pancake.with(ingredient));
    }

    public synchronized void removePancake(UUID pancakeId) {
        requireStatus(OrderStatus.CREATED);
        if (pancakes.remove(pancakeId) == null) {
            throw new PancakeNotFoundException(pancakeId);
        }
    }

    public synchronized List<PancakeDetails> getPancakes() {
        return pancakes.entrySet().stream()
                .map(entry -> new PancakeDetails(entry.getKey(), entry.getValue().description()))
                .toList();
    }

    public synchronized void complete() {
        requireStatus(OrderStatus.CREATED);
        if (pancakes.isEmpty()) {
            throw new EmptyOrderException(id);
        }
        status = OrderStatus.COMPLETED;
    }

    public synchronized void prepare() {
        requireStatus(OrderStatus.COMPLETED);
        status = OrderStatus.PREPARED;
    }

    public synchronized void deliver() {
        requireStatus(OrderStatus.PREPARED);
        status = OrderStatus.DELIVERED;
    }

    public synchronized void cancel() {
        requireStatus(OrderStatus.CREATED, OrderStatus.COMPLETED);
        status = OrderStatus.CANCELLED;
    }

    private void requireStatus(OrderStatus... allowed) {
        if (!List.of(allowed).contains(status)) {
            throw new InvalidOrderStateException(id, status.name());
        }
    }

    public UUID getId() {
        return id;
    }

    public DeliveryAddress getAddress() {
        return address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(id, order.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
