package org.pancakelab.model;

import org.pancakelab.exception.EmptyOrderException;
import org.pancakelab.exception.InvalidOrderStateException;
import org.pancakelab.exception.InvalidPancakeCountException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Order {
    private final UUID id;
    private final int building;
    private final int room;
    private final List<Pancake> pancakes = new ArrayList<>();
    private OrderStatus status = OrderStatus.CREATED;

    public Order(int building, int room) {
        this.id = UUID.randomUUID();
        this.building = building;
        this.room = room;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void addPancake(Pancake pancake) {
        requireStatus(OrderStatus.CREATED);
        pancakes.add(pancake);
    }

    public int removePancakes(String description, int count) {
        requireStatus(OrderStatus.CREATED);
        if (count <= 0) {
            throw new InvalidPancakeCountException(count);
        }
        int removed = 0;
        Iterator<Pancake> iterator = pancakes.iterator();
        while (iterator.hasNext() && removed < count) {
            if (iterator.next().description().equals(description)) {
                iterator.remove();
                removed++;
            }
        }
        return removed;
    }

    public List<String> getPancakeDescriptions() {
        return pancakes.stream().map(Pancake::description).toList();
    }

    public void complete() {
        requireStatus(OrderStatus.CREATED);
        if (pancakes.isEmpty()) {
            throw new EmptyOrderException(id);
        }
        status = OrderStatus.COMPLETED;
    }

    public void prepare() {
        requireStatus(OrderStatus.COMPLETED);
        status = OrderStatus.PREPARED;
    }

    public void deliver() {
        requireStatus(OrderStatus.PREPARED);
        status = OrderStatus.DELIVERED;
    }

    public void cancel() {
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

    public int getBuilding() {
        return building;
    }

    public int getRoom() {
        return room;
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
