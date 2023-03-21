package com.dudek.notificationchatservice.service;

import com.dudek.notificationchatservice.mapper.MessageMapper;
import com.dudek.notificationchatservice.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketMessage;
import reactor.core.publisher.Sinks;

import java.util.Map;

@Service
public class RoomNotificationService implements NotificationService {
    private final Map<String, Sinks.Many<WebSocketMessage>> sinkPoolForAdmins;
    private final Map<String, Sinks.Many<WebSocketMessage>> sinkPoolForUsers;
    private final NotificationRepository repository;
    private final MessageMapper messageMapper;
    private final Logger logger = LoggerFactory.getLogger(RoomNotificationService.class);

    public RoomNotificationService(final @Qualifier("roomAdmins") Map<String, Sinks.Many<WebSocketMessage>> sinkPoolForAdmins, final @Qualifier("roomUsers") Map<String, Sinks.Many<WebSocketMessage>> sinkPoolForUsers, final NotificationRepository repository, final MessageMapper messageMapper) {
        this.sinkPoolForAdmins = sinkPoolForAdmins;
        this.sinkPoolForUsers = sinkPoolForUsers;
        this.repository = repository;
        this.messageMapper = messageMapper;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "roomAdminQueue", durable = "true"),
            exchange = @Exchange(value = "roomExchange", type = ExchangeTypes.TOPIC),
            key = "room.admins"
    ))
    public void receiveMessageForAdmins(String message, @Header("roomId") String roomId) {
        mapMessageToNotificationAndSaveToDatabase(message);
        processMessageForRoom(roomId, message, sinkPoolForAdmins);
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "roomUserQueue", durable = "true"),
            exchange = @Exchange(value = "roomExchange", type = ExchangeTypes.TOPIC),
            key = "room.users"
    ))
    public void receiveMessageForUsers(String message, @Header("roomId") String roomId) {
        mapMessageToNotificationAndSaveToDatabase(message);
        processMessageForRoom(roomId, message, sinkPoolForUsers);
    }

    public void processMessageForRoom(final String roomId, final String message, final Map<String, Sinks.Many<WebSocketMessage>> targetSinkPool) {
        Sinks.Many<WebSocketMessage> sink = targetSinkPool.get(roomId);
        if (sink != null) {
            DataBuffer bufferedMessage = DefaultDataBufferFactory.sharedInstance.wrap(message.getBytes());
            if (sink.tryEmitNext(new WebSocketMessage(WebSocketMessage.Type.TEXT, bufferedMessage)).isSuccess()) {
                logger.debug("New message [{}] for room [{}] has been sent.", message, roomId);
            } else {
                logger.debug("There was a problem sending a message [{}] for room [{}].", message, roomId);
            }
        } else {
            logger.debug("No WebSocket sessions found for room [{}].", roomId);
        }
    }

    public void mapMessageToNotificationAndSaveToDatabase(final String message) {
        messageMapper.messageToNotification(message).ifPresent(n -> {
            repository.save(n).subscribe();
            logger.debug("New message [{}] saved as mapped notification for recipientId [{}] from senderId [{}].", message, n.getRecipientId(), n.getSenderId());
        });
    }
}
