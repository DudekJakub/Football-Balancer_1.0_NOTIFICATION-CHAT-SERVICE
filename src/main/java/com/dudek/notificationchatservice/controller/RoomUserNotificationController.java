package com.dudek.notificationchatservice.controller;

import com.dudek.notificationchatservice.client.RoomClient;
import com.dudek.notificationchatservice.mapper.NotificationMapper;
import com.dudek.notificationchatservice.model.dto.NotificationDto;
import com.dudek.notificationchatservice.repository.RoomUserNotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@CrossOrigin("http://localhost:3000")
@RequestMapping("/api/notification/from-db/room/as-user")
public class RoomUserNotificationController {

    private final RoomUserNotificationRepository roomUserNotificationRepository;
    private final NotificationMapper mapper;
    private final RoomClient roomClient;

    @Autowired
    public RoomUserNotificationController(final RoomUserNotificationRepository roomUserNotificationRepository, final NotificationMapper mapper, final RoomClient roomClient) {
        this.roomUserNotificationRepository = roomUserNotificationRepository;
        this.mapper = mapper;
        this.roomClient = roomClient;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Flux<NotificationDto> getUserNotificationsForRoom(@RequestParam("roomId") String roomId, @RequestParam("userId") Long userId) {
        return roomClient.isUserMemberOfRoom(userId, Long.valueOf(roomId))
                .flatMapMany(isUserMemberOfRoom -> {
                    if (isUserMemberOfRoom) {
                        return roomUserNotificationRepository.findAllByRecipientIdOrderBySendTimeDesc(roomId)
                                .map(mapper::notificationToDto);
                    } else {
                        return Flux.error(new ResponseStatusException(HttpStatus.FORBIDDEN));
                    }
                });
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.OK)
    public Mono<Void> deleteNotification(@RequestParam("roomId") String roomId, @RequestParam("adminId") Long adminId, @RequestParam("notificationId") String notificationId) {
        return roomClient.isAdminOfRoom(adminId, Long.valueOf(roomId))
                .flatMap(isAdminOfRoom -> {
                    if (isAdminOfRoom) {
                        return roomUserNotificationRepository.deleteById(notificationId).then();
                    } else {
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN));
                    }
                });
    }
}
