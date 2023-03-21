package com.dudek.notificationchatservice.mapper;

import com.dudek.notificationchatservice.model.dto.NotificationDto;
import com.dudek.notificationchatservice.model.entity.Notification;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapperImpl implements NotificationMapper {

    @Override
    public NotificationDto notificationToDto(Notification notification) {
        NotificationDto dto = new NotificationDto();
        BeanUtils.copyProperties(notification, dto);
        return dto;
    }
}
