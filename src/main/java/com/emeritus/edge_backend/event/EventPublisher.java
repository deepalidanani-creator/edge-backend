package com.emeritus.edge_backend.event;

public interface EventPublisher {

    void publish(DomainEvent event);
}
