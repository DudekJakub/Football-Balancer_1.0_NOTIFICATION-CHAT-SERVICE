package com.dudek.notificationchatservice.service;

import com.dudek.notificationchatservice.mapper.NotificationMapper;
import com.dudek.notificationchatservice.model.entity.Notification;
import com.dudek.notificationchatservice.model.entity.PrivateUserNotification;
import com.dudek.notificationchatservice.model.entity.RoomAdminNotification;
import com.dudek.notificationchatservice.model.entity.RoomUserNotification;
import com.dudek.notificationchatservice.repository.PrivateUserNotificationRepository;
import com.dudek.notificationchatservice.repository.RoomAdminNotificationRepository;
import com.dudek.notificationchatservice.repository.RoomUserNotificationRepository;
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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class NotificationService {
    private final Map<String, Sinks.Many<WebSocketMessage>> sinkPoolForAdmins;
    private final Map<String, Sinks.Many<WebSocketMessage>> sinkPoolForUsers;
    private final Map<String, Sinks.Many<WebSocketMessage>> sinkPoolPrivateUsers;
    private final RoomUserNotificationRepository roomUserNotificationRepository;
    private final RoomAdminNotificationRepository roomAdminNotificationRepository;
    private final PrivateUserNotificationRepository privateUserNotificationRepository;
    private final NotificationMapper notificationMapper;
    private final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    public NotificationService(final @Qualifier("roomAdmins") Map<String, Sinks.Many<WebSocketMessage>> sinkPoolForAdmins, final @Qualifier("roomUsers") Map<String, Sinks.Many<WebSocketMessage>> sinkPoolForUsers, final @Qualifier("privateUsers") Map<String, Sinks.Many<WebSocketMessage>> sinkPoolForPrivateUsers,
                               final RoomUserNotificationRepository roomUserNotificationRepository, final RoomAdminNotificationRepository roomAdminNotificationRepository, final PrivateUserNotificationRepository privateUserNotificationRepository,
                               final NotificationMapper notificationMapper) {
        this.sinkPoolForAdmins = sinkPoolForAdmins;
        this.sinkPoolForUsers = sinkPoolForUsers;
        this.sinkPoolPrivateUsers = sinkPoolForPrivateUsers;
        this.roomUserNotificationRepository = roomUserNotificationRepository;
        this.roomAdminNotificationRepository = roomAdminNotificationRepository;
        this.privateUserNotificationRepository = privateUserNotificationRepository;
        this.notificationMapper = notificationMapper;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "roomAdminQueue", durable = "true"),
            exchange = @Exchange(value = "roomExchange", type = ExchangeTypes.TOPIC),
            key = "room.admins"
    ))
    public void receiveAndProcessMessageForAdmins(String message, @Header("roomId") String roomId) {
        notificationMapper.messageToNotification(message, RoomAdminNotification.class).ifPresentOrElse(
                notification -> {
                    AtomicReference<RoomAdminNotification> notificationReference = new AtomicReference<>(notification);
                    roomAdminNotificationRepository.save(notificationReference.get()).subscribe(savedNotification -> {
                        notificationReference.set(savedNotification);
                        logger.debug("New message [{}] saved as mapped notification for recipientId [{}] from senderId [{}].", message, notificationReference.get().getRecipientId(), notificationReference.get().getSenderId());
                        sendNotificationToRecipient(roomId, notificationReference.get(), sinkPoolForAdmins);
                    });
                },
                () -> logger.error("Unable to map message [{}] to a notification.", message)
        );
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "roomUserQueue", durable = "true"),
            exchange = @Exchange(value = "roomExchange", type = ExchangeTypes.TOPIC),
            key = "room.users"
    ))
    public void receiveMessageForUsers(String message, @Header("roomId") String roomId) {
        notificationMapper.messageToNotification(message, RoomUserNotification.class).ifPresentOrElse(
                notification -> {
                    AtomicReference<RoomUserNotification> notificationReference = new AtomicReference<>(notification);
                    roomUserNotificationRepository.save(notificationReference.get()).subscribe(savedNotification -> {
                                notificationReference.set(savedNotification);
                                logger.debug("New message [{}] saved as mapped notification for recipientId [{}] from senderId [{}].", message, notificationReference.get().getRecipientId(), notificationReference.get().getSenderId());
                                sendNotificationToRecipient(roomId, notificationReference.get(), sinkPoolForUsers);
                            });
                },
                () -> logger.error("Unable to map message [{}] to a notification.", message)
        );    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "userPrivateQueue", durable = "true"),
            exchange = @Exchange(value = "privateUserExchange", type = ExchangeTypes.TOPIC),
            key = "private.user"
    ))
    public void receiveMessageForPrivateUsers(String message, @Header("userId") String userId) {
        notificationMapper.messageToNotification(message, PrivateUserNotification.class).ifPresentOrElse(
                notification -> {
                    AtomicReference<PrivateUserNotification> notificationReference = new AtomicReference<>(notification);
                    privateUserNotificationRepository.save(notificationReference.get()).subscribe(savedNotification -> {
                                notificationReference.set(savedNotification);
                                logger.debug("New message [{}] saved as mapped notification for recipientId [{}] from senderId [{}].", message, notificationReference.get().getRecipientId(), notificationReference.get().getSenderId());
                                sendNotificationToRecipient(userId, notificationReference.get(), sinkPoolPrivateUsers);
                            });
                },
                () -> logger.error("Unable to map message [{}] to a notification.", message)
        );
    }

    public void sendNotificationToRecipient(final String recipientId, final Notification notification, final Map<String, Sinks.Many<WebSocketMessage>> targetSinkPool) {
        Optional<String> optionalNotificationAsJson = notificationMapper.notificationToJson(notification);
        String notificationAsJson;

        if (optionalNotificationAsJson.isEmpty()) {
            logger.debug("There was a problem obtaining notification as JSON. Processing message for room [{}] abandoned.", recipientId);
            return;
        }

        notificationAsJson = optionalNotificationAsJson.get();

        Sinks.Many<WebSocketMessage> sink = targetSinkPool.get(recipientId);
        if (sink != null) {
            DataBuffer bufferedMessage = DefaultDataBufferFactory.sharedInstance.wrap(notificationAsJson.getBytes());
            if (sink.tryEmitNext(new WebSocketMessage(WebSocketMessage.Type.TEXT, bufferedMessage)).isSuccess()) {
                logger.debug("New notification [{}] for recipient [{}] has been sent.", notification, recipientId);
            } else {
                logger.debug("There was a problem sending a notification [{}] for recipient [{}].", notification.getMessage(), recipientId);
            }
        } else {
            logger.debug("No WebSocket sessions found for recipient [{}].", recipientId);
        }
    }
}
