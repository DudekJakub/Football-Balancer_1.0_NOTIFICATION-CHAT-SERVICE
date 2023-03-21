package com.dudek.notificationchatservice.controller;

import com.dudek.notificationchatservice.mapper.NotificationMapper;
import com.dudek.notificationchatservice.model.dto.NotificationDto;
import com.dudek.notificationchatservice.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@CrossOrigin("http://localhost:3000")
@RequestMapping("/api/notification/from-db/private-user")
public class UserNotificationController {

    private final NotificationRepository repository;
    private final NotificationMapper mapper;

    @Autowired
    public UserNotificationController(NotificationRepository repository, NotificationMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Flux<NotificationDto> getUserPrivateNotifications(@RequestParam("userId") String userId) {
        return repository.findAllByRecipientId(userId).map(mapper::notificationToDto);
    }
}
