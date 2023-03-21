package com.dudek.notificationchatservice.repository;

import com.dudek.notificationchatservice.model.entity.Notification;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.List;

@Repository
public interface NotificationRepository extends ReactiveMongoRepository<Notification, String> {
    Flux<Notification> findAllByRecipientId(String recipientId);

    @Query("{ recipientId: ?0, recipientTypes: { $in: ?1 } }")
    Flux<Notification> findAllByRecipientIdAndRecipientTypes(String recipientId, List<String> recipientTypes);


}
