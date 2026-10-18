package org.pancakelab.service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryOrderLog implements OrderLog {
    private final List<String> entries = new CopyOnWriteArrayList<>();

    @Override
    public void log(String message) {
        entries.add(message);
    }

    public List<String> entries() {
        return List.copyOf(entries);
    }
}
