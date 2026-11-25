package com.examly.springapp.event;
public interface DomainEventPublisher {
    void publish(String topic,String key,Object payload);
}
