package com.feiniaojin.ddd.eventsourcing.infrastructure.persistence;

import com.feiniaojin.ddd.eventsourcing.domain.*;
import com.feiniaojin.ddd.eventsourcing.infrastructure.persistence.jdbc.data.Entity;
import com.feiniaojin.ddd.eventsourcing.infrastructure.persistence.jdbc.data.Event;
import com.feiniaojin.ddd.eventsourcing.infrastructure.persistence.jdbc.data.Snapshot;
import com.feiniaojin.ddd.eventsourcing.infrastructure.persistence.jdbc.repository.EntityJdbcRepository;
import com.feiniaojin.ddd.eventsourcing.infrastructure.persistence.jdbc.repository.EventJdbcRepository;
import com.feiniaojin.ddd.eventsourcing.infrastructure.persistence.jdbc.repository.SnapshotJdbcRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class ProductRepositoryImpl implements ProductRepository {

    ProductFactory productFactory = new ProductFactory();

    @Resource
    private EventJdbcRepository eventJdbcRepository;

    @Resource
    private EntityJdbcRepository entityJdbcRepository;

    @Resource
    private SnapshotJdbcRepository snapshotJdbcRepository;

    @Override
    @Transactional
    public void save(Product product) {

        //1.存事件
        List<DomainEvent> domainEvents = product.getDomainEvents();
        List<Event> eventList = domainEvents.stream().map(de -> {
            Event event = new Event();
            event.setEventTime(de.getEventTime());
            event.setEventType(de.getEventType());
            event.setEventData(JSON.toJsonString(de));
            event.setEntityId(de.getEntityId());
            event.setDeleted(0);
            event.setEventId(de.getEventId());
            return event;
        }).collect(Collectors.toList());
        eventJdbcRepository.saveAll(eventList);

        //2.存实体
        Entity entity = new Entity();
        entity.setId(product.getId());
        entity.setDeleted(product.getDeleted());
        entity.setEntityId(product.getProductId().getValue());
        entity.setDeleted(0);
        entity.setVersion(product.getVersion());

        entityJdbcRepository.save(entity);

        //3.快照生成
        if (product.getTakeSnapshot()) {
            Snapshot snapshot = snapshotJdbcRepository.queryOneByEntityId(product.getProductId().getValue());
            if (Objects.isNull(snapshot)) {
                snapshot = new Snapshot();
                snapshot.setDeleted(0);
                snapshot.setEntityId(product.getProductId().getValue());
            }
            //最后一个事件的信息
            DomainEvent lastDomainEvent = domainEvents.get(domainEvents.size() - 1);
            snapshot.setEventId(lastDomainEvent.getEventId());
            snapshot.setEventTime(lastDomainEvent.getEventTime());
            //快照中不保存本次的领域事件
            product.getDomainEvents().clear();
            //生成聚合根快照
            snapshot.setEntityData(JSON.toJsonString(product));
            snapshotJdbcRepository.save(snapshot);
        }
    }

    @Override
    public Product load(ProductId productId) {

        //1.获得聚合根和待回放事件
        Product product;
        List<Event> events;
        //查询快照，如果能拿到快照则直接用快照进行反序列化，如果拿不到快照则创建空的聚合根
        Snapshot snapshot = snapshotJdbcRepository.queryOneByEntityId(productId.getValue());
        if (Objects.isNull(snapshot)) {
            product = productFactory.newInstance();
            events = eventJdbcRepository.loadHistoryEvents(productId.getValue());
        } else {
            product = JSON.toObject(snapshot.getEntityData(), Product.class);
            events = eventJdbcRepository.loadEventsAfter(productId.getValue(), snapshot.getEventTime());
        }

        List<DomainEvent> domainEvents = this.toDomainEvent(events);
        //2.回放事件重建聚合根
        product.rebuild(domainEvents);

        //3.加载entity获得乐观锁
        Entity entity = entityJdbcRepository.queryOneByEntityId(productId.getValue());
        product.setVersion(entity.getVersion());
        product.setDeleted(entity.getDeleted());
        product.setId(entity.getId());
        //省略其他属性
        return product;
    }

    private List<DomainEvent> toDomainEvent(List<Event> events) {
        List<DomainEvent> domainEvents = events.stream().map(e -> {
            String eventType = e.getEventType();
            Class<? extends DomainEvent> typeClass = EventTypeMapping.getEventTypeClass(eventType);
            DomainEvent domainEvent = JSON.toObject(e.getEventData(), typeClass);
            return domainEvent;
        }).collect(Collectors.toList());
        return domainEvents;
    }
}
