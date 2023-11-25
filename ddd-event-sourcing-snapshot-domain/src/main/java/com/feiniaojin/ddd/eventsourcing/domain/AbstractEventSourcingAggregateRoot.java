package com.feiniaojin.ddd.eventsourcing.domain;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 事件溯源的抽象AggregateRoot
 */
public abstract class AbstractEventSourcingAggregateRoot extends AbstractAggregateRoot {

    private static final Map<Class<?>, Method> applyMethodMap = new ConcurrentHashMap<>();

    private Boolean takeSnapshot = Boolean.FALSE;

    {
        if (applyMethodMap.isEmpty()) {
            Method[] methods = getClass().getMethods();
            for (Method method : methods) {
                //跳过不包含apply的方法
                if (!method.getName().equals("apply")) {
                    continue;
                }
                Class<?>[] parameterTypes = method.getParameterTypes();
                applyMethodMap.put(parameterTypes[0], method);
            }
        }
    }

    public void rebuild(List<DomainEvent> events) {
        try {
            for (DomainEvent domainEvent : events) {
                Method method = applyMethodMap.get(domainEvent.getClass());
                method.invoke(this, domainEvent);
            }
            //如果有20个以上的领域事件需要回放，则生成快照
            if (events.size() > 20) {
                this.takeSnapshot = Boolean.TRUE;
            }
            //清空领域事件，因为重建领域事件不需要记录下来存库
            super.clearDomainEvents();
        } catch (Exception e) {
            throw new RuntimeException("找不到对应的apply方法");
        }
    }

    public void apply(DomainEvent domainEvent) {
        try {
            Method method = applyMethodMap.get(domainEvent.getClass());
            method.invoke(this, domainEvent);
        } catch (Exception e) {
            throw new RuntimeException("找不到对应的apply方法");
        }
    }

    public Boolean getTakeSnapshot() {
        return takeSnapshot;
    }

    public void setTakeSnapshot(Boolean takeSnapshot) {
        this.takeSnapshot = takeSnapshot;
    }
}