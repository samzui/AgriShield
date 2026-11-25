package com.examly.springapp.event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
@Component @Profile( {
    "default","dev","test"
}) public class LocalDomainEventPublisher implements DomainEventPublisher {
    private static final Logger log=LoggerFactory.getLogger(LocalDomainEventPublisher.class);
    public void publish(String topic,String key,Object payload) {
        log.info("domain-event topic={} key={}",topic,key);
    }
}
