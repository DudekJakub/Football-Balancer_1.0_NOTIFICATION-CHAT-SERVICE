package com.dudek.notificationchatservice.mapper;

import com.dudek.notificationchatservice.model.dto.NotificationDto;
import com.dudek.notificationchatservice.model.entity.Notification;

import java.util.Optional;

public interface NotificationMapper {

    <T extends Notification> Optional<T> messageToNotification(String message, Class<T> notificationType);
    NotificationDto notificationToDto(Notification notification);
    Optional<String> notificationToJson(Notification notification);
}
