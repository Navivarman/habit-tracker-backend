package com.habittracker.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // The endpoint where our React frontend will initiate the connection handshake
        registry.addEndpoint("/ws-challenge")
                .setAllowedOrigins("http://localhost:5173") // Allow your React Dev Server
                .withSockJS(); // Fallback option if the browser doesn't natively support WebSockets
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enforce application destination prefixes for incoming messages routed to @MessageMapping methods
        registry.setApplicationDestinationPrefixes("/app");

        // Setup a simple memory broker prefix path for clients to subscribe to topic rooms
        registry.enableSimpleBroker("/topic");
    }
}