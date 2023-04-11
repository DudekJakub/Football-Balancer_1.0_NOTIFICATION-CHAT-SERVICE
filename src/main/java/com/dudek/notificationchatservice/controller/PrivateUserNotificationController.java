package com.dudek.notificationchatservice.controller;

import com.dudek.notificationchatservice.mapper.NotificationMapper;
import com.dudek.notificationchatservice.model.dto.NotificationDto;
import com.dudek.notificationchatservice.repository.PrivateUserNotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

@RestController
@CrossOrigin("http://localhost:3000")
@RequestMapping("/api/notification/from-db/private-user")
public class PrivateUserNotificationController {

    private final PrivateUserNotificationRepository privateUserNotificationRepository;
    private final NotificationMapper mapper;

    @Autowired
    public PrivateUserNotificationController(final PrivateUserNotificationRepository privateUserNotificationRepository, final NotificationMapper mapper) {
        this.privateUserNotificationRepository = privateUserNotificationRepository;
        this.mapper = mapper;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Flux<NotificationDto> getUserPrivateNotifications(@RequestParam("userId") String userId) {
        return privateUserNotificationRepository.findAllByRecipientIdOrderBySendTimeDesc(userId).map(mapper::notificationToDto);
    }

    @PatchMapping
    @ResponseStatus(HttpStatus.OK)
    public Mono<Void> markNotificationAsRead(@RequestParam("userId") String userId, @RequestParam("notificationId") String notificationId) {
        return privateUserNotificationRepository.findById(notificationId).flatMap(
                notification -> {
                    if (Objects.equals(notification.getRecipientId(), userId)) {
                        notification.setIsRead(true);
                        return privateUserNotificationRepository.save(notification);
                    } else {
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Notification does not belong to the specified user"));
                    }
                }
        ).then();
    }
}
