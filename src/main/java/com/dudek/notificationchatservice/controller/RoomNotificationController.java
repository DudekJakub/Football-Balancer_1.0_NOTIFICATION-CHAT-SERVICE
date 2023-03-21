package com.dudek.notificationchatservice.controller;

import com.dudek.notificationchatservice.client.RoomClient;
import com.dudek.notificationchatservice.mapper.NotificationMapper;
import com.dudek.notificationchatservice.model.RecipientType;
import com.dudek.notificationchatservice.model.dto.NotificationDto;
import com.dudek.notificationchatservice.repository.NotificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@CrossOrigin("http://localhost:3000")
@RequestMapping("/api/notification/from-db/room")
public class RoomNotificationController {

    private final NotificationRepository repository;
    private final NotificationMapper mapper;
    private final RoomClient roomClient;

    public RoomNotificationController(final NotificationRepository repository, final NotificationMapper mapper, final RoomClient roomClient) {
        this.repository = repository;
        this.mapper = mapper;
        this.roomClient = roomClient;
    }

    @GetMapping(value = "/as-admin")
    @ResponseStatus(HttpStatus.OK)
    public Flux<NotificationDto> getAdminNotificationsForRoom(@RequestParam("roomId") String roomId, @RequestParam("adminId") Long adminId) {
        return roomClient.isAdminOfRoom(adminId, Long.valueOf(roomId))
                .flatMapMany(isAdminOfRoom -> {
                    if (isAdminOfRoom) {
                        return repository.findAllByRecipientIdAndRecipientTypes(roomId, List.of(RecipientType.ADMIN.name()))
                                .map(mapper::notificationToDto);
                    } else {
                        return Flux.error(new ResponseStatusException(HttpStatus.FORBIDDEN));
                    }
                });
    }

    @GetMapping(value = "/as-user")
    @ResponseStatus(HttpStatus.OK)
    public Flux<NotificationDto> getUserNotificationsForRoom(@RequestParam("roomId") String roomId, @RequestParam("userId") Long userId) {
        return roomClient.isUserMemberOfRoom(userId, Long.valueOf(roomId))
                .flatMapMany(isUserMemberOfRoom -> {
                    if (isUserMemberOfRoom) {
                        return repository.findAllByRecipientIdAndRecipientTypes(roomId, List.of(RecipientType.USER.name()))
                                .map(mapper::notificationToDto);
                    } else {
                        return Flux.error(new ResponseStatusException(HttpStatus.FORBIDDEN));
                    }
                });
    }
}
