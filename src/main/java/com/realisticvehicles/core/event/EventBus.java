package com.realisticvehicles.core.event;

import java.util.function.Consumer;

/**
 * Lightweight Observer / EventBus abstraction (no global singleton).
 */
public interface EventBus {
    <T> void subscribe(Class<T> eventType, Consumer<T> listener);

    void publish(Object event);
}
