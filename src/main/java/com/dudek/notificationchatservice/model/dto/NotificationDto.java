package com.dudek.notificationchatservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {

    private String id;
    private String senderName;
    private String senderId;
    private String recipientId;
    private LocalDateTime sendTime;
    private String message;
    private String messageStatus;
    private String messageType;
    private String messageSubType;
    private Boolean isRead;
    private Boolean isProcessed;
}
