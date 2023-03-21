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
public class UserNotificationService implements NotificationService {

    private final Map<String, Sinks.Many<WebSocketMessage>> sinkPoolPrivateUsers;
    private final NotificationRepository repository;
    private final MessageMapper messageMapper;
    private final Logger logger = LoggerFactory.getLogger(UserNotificationService.class);

    public UserNotificationService(final @Qualifier("privateUsers") Map<String, Sinks.Many<WebSocketMessage>> sinkPoolPrivateUsers, final NotificationRepository repository, final MessageMapper messageMapper) {
        this.sinkPoolPrivateUsers = sinkPoolPrivateUsers;
        this.repository = repository;
        this.messageMapper = messageMapper;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "userPrivateQueue", durable = "true"),
            exchange = @Exchange(value = "privateUserExchange", type = ExchangeTypes.TOPIC),
            key = "private.user"
    ))
    public void receiveMessageForPrivateUsers(String message, @Header("userId") String userId) {
        mapMessageToNotificationAndSaveToDatabase(message);
        processMessageForRoom(userId, message);
    }

    private void processMessageForRoom(String userId, String message) {
        Sinks.Many<WebSocketMessage> sink = sinkPoolPrivateUsers.get(userId);
        if (sink != null) {
            DataBuffer bufferedMessage = DefaultDataBufferFactory.sharedInstance.wrap(message.getBytes());
            sink.tryEmitNext(new WebSocketMessage(WebSocketMessage.Type.TEXT, bufferedMessage));
            logger.debug("New message [{}] for user [{}] has been sent.", message, userId);
        }
    }
    public void mapMessageToNotificationAndSaveToDatabase(final String message) {
        messageMapper.messageToNotification(message).ifPresent(n -> {
            repository.save(n).subscribe();
            logger.debug("New message [{}] saved as mapped notification for recipientId [{}] from senderId [{}].", message, n.getRecipientId(), n.getSenderId());
        });
    }
}
