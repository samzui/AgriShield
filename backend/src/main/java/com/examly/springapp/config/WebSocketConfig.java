package com.examly.springapp.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;
import java.util.Arrays;
@Configuration @EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final String origins;
    private final boolean relay;
    private final String host;
    private final int port;
    public WebSocketConfig(@Value("${app.cors-origins}")String origins,@Value("${app.messaging.relay-enabled:false}")boolean relay,@Value("${app.messaging.relay-host:localhost}")String host,@Value("${app.messaging.relay-port:61613}")int port) {
        this.origins=origins;
        this.relay=relay;
        this.host=host;
        this.port=port;
    }
    @Override public void configureMessageBroker(MessageBrokerRegistry registry) {
        if(relay)registry.enableStompBrokerRelay("/topic","/queue").setRelayHost(host).setRelayPort(port);
        else registry.enableSimpleBroker("/topic","/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }
    @Override public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns(Arrays.stream(origins.split(",")).map(String::trim).toArray(String[]::new));
    }
}
