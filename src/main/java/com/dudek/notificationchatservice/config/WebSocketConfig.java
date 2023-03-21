package com.dudek.notificationchatservice.config;

import com.dudek.notificationchatservice.webSocketHandler.RoomWebSocketHandler;
import com.dudek.notificationchatservice.webSocketHandler.UserWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class WebSocketConfig {

    private final RoomWebSocketHandler roomWebSocketHandler;
    private final UserWebSocketHandler userWebSocketHandler;

    @Autowired
    public WebSocketConfig(final RoomWebSocketHandler roomWebSocketHandler, final UserWebSocketHandler userWebSocketHandler) {
        this.roomWebSocketHandler = roomWebSocketHandler;
        this.userWebSocketHandler = userWebSocketHandler;
    }

    @Bean
    public HandlerMapping webSocketHandlerMapping() {
        Map<String, WebSocketHandler> handlerMap = new HashMap<>();
        handlerMap.put("/api/notification/stream/room/as-admin/{roomId}", roomWebSocketHandler);
        handlerMap.put("/api/notification/stream/room/as-user/{roomId}", roomWebSocketHandler);
        handlerMap.put("/api/notification/stream/private-user/{userId}", userWebSocketHandler);

        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setUrlMap(handlerMap);
        mapping.setOrder(1);
        return mapping;
    }
}
