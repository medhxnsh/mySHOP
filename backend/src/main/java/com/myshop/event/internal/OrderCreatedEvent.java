package com.myshop.event.internal;

import com.myshop.model.entity.Order;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Internal Spring event published when an order is successfully created.
 */
@Getter
public class OrderCreatedEvent extends ApplicationEvent {

    private final Order order;

    public OrderCreatedEvent(Object source, Order order) {
        super(source);
        this.order = order;
    }
}
