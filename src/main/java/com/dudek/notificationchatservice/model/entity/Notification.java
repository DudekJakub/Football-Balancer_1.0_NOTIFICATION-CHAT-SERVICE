package com.dudek.notificationchatservice.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;
import java.util.List;

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
    private List<String> recipientTypes;
    private String sendTime;
    private String message;
    private String messageStatus;
    private String messageType;
    private String messageSubType;
    private Boolean isRead;
    private LocalDateTime receiptDate;
}
