package com.dudek.notificationchatservice.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    private String id;
    private String senderName;
    private String senderId;
    private String recipientId;
    private LocalDateTime sendTime;
    private String message;
    private String messageStatus;
    private String messageType;
    private String messageSubType;
}
