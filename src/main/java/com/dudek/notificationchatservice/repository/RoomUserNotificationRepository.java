package com.dudek.notificationchatservice.repository;

import com.dudek.notificationchatservice.model.entity.RoomUserNotification;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface RoomUserNotificationRepository extends ReactiveMongoRepository<RoomUserNotification, String> {

    @Query(value = "{ recipientId: ?0 }", sort = "{ sendTime: -1 }")
    Flux<RoomUserNotification> findAllByRecipientIdOrderBySendTimeDesc(String recipientId);
}
