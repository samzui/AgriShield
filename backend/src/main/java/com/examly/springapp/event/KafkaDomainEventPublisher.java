package com.examly.springapp.event;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
@Component @ConditionalOnProperty(name="app.kafka.enabled",havingValue="true") public class KafkaDomainEventPublisher implements DomainEventPublisher {
    private final KafkaTemplate<String,Object> kafka;
    public KafkaDomainEventPublisher(KafkaTemplate<String,Object> kafka) {
        this.kafka=kafka;
    }public void publish(String topic,String key,Object payload) {
        kafka.send(topic,key,payload);
    }
}
