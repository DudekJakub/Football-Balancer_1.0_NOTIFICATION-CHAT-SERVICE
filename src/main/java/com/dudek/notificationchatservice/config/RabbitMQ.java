package com.dudek.notificationchatservice.config;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.socket.WebSocketMessage;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@EnableWebFlux
public class RabbitMQ {

    private String rabbitMQHost = "localhost";
    private int rabbitMQPort = 5672;
    private String rabbitMQUsername = "guest";
    private String rabbitMQPassword = "guest";

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost(rabbitMQHost);
        connectionFactory.setPort(rabbitMQPort);
        connectionFactory.setUsername(rabbitMQUsername);
        connectionFactory.setPassword(rabbitMQPassword);
        return connectionFactory;
    }

    @Bean
    public RabbitTemplate rabbitTemplate() {
        return new RabbitTemplate(connectionFactory());
    }

    @Bean("roomAdmins")
    public Map<String, Sinks.Many<WebSocketMessage>> sinkMapForRoomAdmins() {
        return new ConcurrentHashMap<>();
    }

    @Bean("roomUsers")
    public Map<String, Sinks.Many<WebSocketMessage>> sinkMapForRoomUsers() {
        return new ConcurrentHashMap<>();
    }

    @Bean("privateUsers")
    public Map<String, Sinks.Many<WebSocketMessage>> sinkMapForPrivateUsers() {
        return new ConcurrentHashMap<>();
    }
}
