package com.dudek.notificationchatservice.repository;

import com.dudek.notificationchatservice.model.entity.PrivateUserNotification;
import com.dudek.notificationchatservice.model.entity.RoomUserNotification;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface PrivateUserNotificationRepository extends ReactiveMongoRepository<PrivateUserNotification, String> {

    @Query(value = "{ recipientId: ?0 }", sort = "{ sendTime: -1 }")
    Flux<PrivateUserNotification> findAllByRecipientIdOrderBySendTimeDesc(String recipientId);
}
