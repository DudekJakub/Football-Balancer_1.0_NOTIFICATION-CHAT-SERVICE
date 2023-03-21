package com.dudek.notificationchatservice.service;

public interface NotificationService {

    void mapMessageToNotificationAndSaveToDatabase(String message);
}
