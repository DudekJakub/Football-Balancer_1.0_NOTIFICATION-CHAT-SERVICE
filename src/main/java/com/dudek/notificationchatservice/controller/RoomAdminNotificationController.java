package com.dudek.notificationchatservice.controller;

import com.dudek.notificationchatservice.client.RoomClient;
import com.dudek.notificationchatservice.mapper.NotificationMapper;
import com.dudek.notificationchatservice.model.dto.NotificationDto;
import com.dudek.notificationchatservice.repository.RoomAdminNotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@CrossOrigin("http://localhost:3000")
@RequestMapping("/api/notification/from-db/room/as-admin")
public class RoomAdminNotificationController {
    private final RoomAdminNotificationRepository roomAdminNotificationRepository;
    private final NotificationMapper mapper;
    private final RoomClient roomClient;

    @Autowired
    public RoomAdminNotificationController(final RoomAdminNotificationRepository roomAdminNotificationRepository, final NotificationMapper mapper, final RoomClient roomClient) {
        this.roomAdminNotificationRepository = roomAdminNotificationRepository;
        this.mapper = mapper;
        this.roomClient = roomClient;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Flux<NotificationDto> getAdminNotificationsForRoom(@RequestParam("roomId") String roomId, @RequestParam("adminId") Long adminId) {
        return roomClient.isAdminOfRoom(adminId, Long.valueOf(roomId))
                .flatMapMany(isAdminOfRoom -> {
                    if (isAdminOfRoom) {
                        return roomAdminNotificationRepository.findAllByRecipientIdOrderBySendTimeDesc(roomId)
                                .map(mapper::notificationToDto);
                    } else {
                        return Flux.error(new ResponseStatusException(HttpStatus.FORBIDDEN));
                    }
                });
    }

    @PatchMapping(value = "/mark-as-processed")
    @ResponseStatus(HttpStatus.OK)
    public Mono<Void> markNotificationAsProcessed(@RequestParam("roomId") String roomId, @RequestParam("adminId") Long adminId, @RequestParam("notificationId") String notificationId) {
        return roomClient.isAdminOfRoom(adminId, Long.valueOf(roomId))
                .flatMap(isAdminOfRoom -> {
                    if (isAdminOfRoom) {
                        return roomAdminNotificationRepository.findById(notificationId).flatMap(notification -> {
                            notification.setIsProcessed(true);
                            return roomAdminNotificationRepository.save(notification);
                        }).then();
                    } else {
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN));
                    }
                });
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.OK)
    public Mono<Void> deleteNotification(@RequestParam("roomId") String roomId, @RequestParam("adminId") Long adminId, @RequestParam("notificationId") String notificationId) {
        return roomClient.isAdminOfRoom(adminId, Long.valueOf(roomId))
                .flatMap(isAdminOfRoom -> {
                    if (isAdminOfRoom) {
                        return roomAdminNotificationRepository.deleteById(notificationId).then();
                    } else {
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN));
                    }
                });
    }
}
