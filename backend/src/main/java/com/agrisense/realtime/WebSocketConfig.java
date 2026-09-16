package com.agrisense.realtime;

import com.agrisense.security.JwtService;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.*;

import java.security.Principal;

/**
 * STOMP over WebSocket. Broker in-memory (đủ cho 1 instance MVP).
 * Auth: client gửi CONNECT kèm header "token" (JWT) → gắn userId làm Principal.
 * ponytail: broker in-memory; scale nhiều instance thì gắn RabbitMQ/Redis relay.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwt;

    public WebSocketConfig(JwtService jwt) {
        this.jwt = jwt;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:5173")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String token = accessor.getFirstNativeHeader("token");
                    if (token != null) {
                        Long userId = jwt.parseUserId(token); // ném nếu sai → CONNECT bị từ chối
                        accessor.setUser(principalOf(userId));
                    }
                }
                return message;
            }
        });
    }

    private Principal principalOf(Long userId) {
        String name = String.valueOf(userId);
        return () -> name;
    }
}
