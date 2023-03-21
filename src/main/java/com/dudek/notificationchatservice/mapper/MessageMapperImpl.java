package com.dudek.notificationchatservice.mapper;

import com.dudek.notificationchatservice.model.entity.Notification;
import com.dudek.notificationchatservice.model.RecipientType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class MessageMapperImpl implements MessageMapper {

    private final ObjectMapper objectMapper;
    private final Logger logger = LoggerFactory.getLogger(MessageMapperImpl.class);
    private final static String SENDER_NAME = "senderName";
    private final static String SENDER_ID = "senderId";
    private final static String RECIPIENT_ID = "recipientId";
    private final static String RECIPIENT_TYPES = "recipientTypes";
    private final static String SEND_TIME = "sendTime";
    private final static String MESSAGE = "message";
    private final static String MESSAGE_STATUS = "messageStatus";
    private final static String MESSAGE_TYPE = "messageType";
    private final static String MESSAGE_SUB_TYPE = "messageSubType";

    @Autowired
    public MessageMapperImpl(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<Notification> messageToNotification(String message) {
        JsonNode messageToTree;
        try {
            messageToTree = objectMapper.readTree(message);
        } catch (JsonProcessingException e) {
            logger.error("Error parsing JSON message: {}", message, e);
            return Optional.empty();
        }

        Notification newNotification = Notification.builder()
                .senderName(messageToTree.get(SENDER_NAME).asText())
                .senderId(messageToTree.get(SENDER_ID).asText())
                .recipientId(messageToTree.get(RECIPIENT_ID).asText())
                .sendTime(messageToTree.get(SEND_TIME).asText())
                .message(messageToTree.get(MESSAGE).asText())
                .messageStatus(messageToTree.get(MESSAGE_STATUS).asText())
                .messageType(messageToTree.get(MESSAGE_TYPE).asText())
                .messageSubType((messageToTree.get(MESSAGE_SUB_TYPE).asText()))
                .build();

        List<String> recipientTypes = new ArrayList<>();
        JsonNode recipientTypesNode = messageToTree.get(RECIPIENT_TYPES);
        if (recipientTypesNode.isArray()) {
            recipientTypesNode.forEach(node -> {
                if (node.asText().equals(RecipientType.ADMIN.name()) || node.asText().equals(RecipientType.USER.name())) {
                    recipientTypes.add(node.asText());
                }
            });
        }
        newNotification.setRecipientTypes(recipientTypes);

        return Optional.of(newNotification);
    }
}
