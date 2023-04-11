package com.dudek.notificationchatservice.repository;

import com.dudek.notificationchatservice.model.entity.RoomAdminNotification;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface RoomAdminNotificationRepository extends ReactiveMongoRepository<RoomAdminNotification, String> {

    @Query(value = "{ recipientId: ?0 }", sort = "{ sendTime: -1 }")
    Flux<RoomAdminNotification> findAllByRecipientIdOrderBySendTimeDesc(String recipientId);
}
