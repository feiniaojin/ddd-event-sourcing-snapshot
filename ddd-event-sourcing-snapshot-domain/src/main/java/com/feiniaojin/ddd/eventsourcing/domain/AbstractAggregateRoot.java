package com.feiniaojin.ddd.eventsourcing.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * 抽象聚合根
 */
public abstract class AbstractAggregateRoot extends AbstractDomainMask {
    private List<DomainEvent> domainEvents = new ArrayList<>();

    public List<DomainEvent> getDomainEvents() {
        return domainEvents;
    }

    protected final void registerDomainEvents(DomainEvent event) {
        this.domainEvents.add(event);
    }

    protected final void registerDomainEvents(List<DomainEvent> events) {
        this.domainEvents.addAll(events);
    }

    protected final void clearDomainEvents() {
        this.domainEvents = new ArrayList<>();
    }

}