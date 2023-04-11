package com.dudek.notificationchatservice.mapper;

import com.dudek.notificationchatservice.model.dto.NotificationDto;
import com.dudek.notificationchatservice.model.entity.Notification;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

@Component
public class NotificationMapperImpl implements NotificationMapper {

    private final ObjectMapper objectMapper;
    private final static String SENDER_NAME = "senderName";
    private final static String SENDER_ID = "senderId";
    private final static String RECIPIENT_ID = "recipientId";
    private final static String SEND_TIME = "sendTime";
    private final static String MESSAGE = "message";
    private final static String MESSAGE_STATUS = "messageStatus";
    private final static String MESSAGE_TYPE = "messageType";
    private final static String MESSAGE_SUB_TYPE = "messageSubType";
    private final Logger logger = LoggerFactory.getLogger(NotificationMapperImpl.class);

    @Autowired
    public NotificationMapperImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public <T extends Notification> Optional<T> messageToNotification(final String message, final Class<T> notificationType) {
        JsonNode messageToTree;
        try {
            messageToTree = objectMapper.readTree(message);
        } catch (JsonProcessingException e) {
            logger.error("Error parsing JSON message: {}", message, e);
            return Optional.empty();
        }

        T newNotification;
        try {
            newNotification = notificationType.getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            logger.error("Error creating instance of notification type [{}]: [{}]", notificationType.getSimpleName(), e);
            throw new RuntimeException(e);
        }

        newNotification.setSenderName(messageToTree.get(SENDER_NAME).asText());
        newNotification.setSenderId(messageToTree.get(SENDER_ID).asText());
        newNotification.setRecipientId(messageToTree.get(RECIPIENT_ID).asText());
        newNotification.setMessage(messageToTree.get(MESSAGE).asText());
        newNotification.setMessageStatus(messageToTree.get(MESSAGE_STATUS).asText());
        newNotification.setMessageType(messageToTree.get(MESSAGE_TYPE).asText());
        newNotification.setMessageSubType(messageToTree.get(MESSAGE_SUB_TYPE).asText());

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            String dataFromMessage = messageToTree.get(SEND_TIME).asText();
            String dataFromMessageWithoutMilliseconds = dataFromMessage.substring(0, dataFromMessage.indexOf('.'));
            LocalDateTime dateTime = LocalDateTime.parse(dataFromMessageWithoutMilliseconds, formatter);
            logger.debug("Formatted date: [{}]", dateTime);
            newNotification.setSendTime(dateTime);
        } catch (DateTimeParseException e) {
            logger.debug("There was a problem parsing message date! [{}]", e.getMessage());
            newNotification.setSendTime(LocalDateTime.now());
        }

        return Optional.of(newNotification);
    }

    @Override
    public NotificationDto notificationToDto(final Notification notification) {
        NotificationDto dto = new NotificationDto();
        BeanUtils.copyProperties(notification, dto);
        return dto;
    }

    @Override
    public Optional<String> notificationToJson(final Notification notification) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        try {
            return Optional.of(objectMapper.writeValueAsString(notification));
        } catch (JsonProcessingException | NullPointerException e) {
            logger.debug("There was a problem mapping notification [{}] to JSON. | Exception message: [{}]", notification, e.getMessage());
            return Optional.empty();
        }
    }
}
