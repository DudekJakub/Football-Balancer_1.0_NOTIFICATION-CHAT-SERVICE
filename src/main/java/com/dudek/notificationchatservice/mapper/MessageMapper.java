package com.dudek.notificationchatservice.mapper;

import com.dudek.notificationchatservice.model.entity.Notification;

import java.util.Optional;

public interface MessageMapper {

    Optional<Notification> messageToNotification(String message);
}
