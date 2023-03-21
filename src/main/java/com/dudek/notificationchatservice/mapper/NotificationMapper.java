package com.dudek.notificationchatservice.mapper;

import com.dudek.notificationchatservice.model.dto.NotificationDto;
import com.dudek.notificationchatservice.model.entity.Notification;

public interface NotificationMapper {

    NotificationDto notificationToDto(Notification notification);
}
