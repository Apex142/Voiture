package com.realisticvehicles.core.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * In-memory synchronous EventBus implementation injected per simulation context.
 */
public final class SimpleEventBus implements EventBus {
    private final Map<Class<?>, List<Consumer<?>>> listeners = new HashMap<>();

    @Override
    public <T> void subscribe(Class<T> eventType, Consumer<T> listener) {
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(listener, "listener");
        listeners.computeIfAbsent(eventType, ignored -> new ArrayList<>()).add(listener);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void publish(Object event) {
        if (event == null) {
            return;
        }
        List<Consumer<?>> handlers = listeners.get(event.getClass());
        if (handlers == null || handlers.isEmpty()) {
            return;
        }
        for (Consumer<?> handler : List.copyOf(handlers)) {
            ((Consumer<Object>) handler).accept(event);
        }
    }
}
