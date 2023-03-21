package com.dudek.notificationchatservice.validation;

import com.dudek.notificationchatservice.client.RoomClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RoomWebSocketValidator {

    private final RoomClient roomClient;
    private final Logger logger = LoggerFactory.getLogger(RoomWebSocketValidator.class);


    public RoomWebSocketValidator(RoomClient roomClient) {
        this.roomClient = roomClient;
    }

    public Mono<Boolean> validateFirstConnectionPerAdmin(final String roomId, final String adminId, final Map<String,Map<String, AtomicInteger>> connectionCountMapPerUserOrAdmin) {
        long roomIdLong = Long.parseLong(roomId);
        long adminIdLong = Long.parseLong(adminId);

        if (!connectionCountMapPerUserOrAdmin.containsKey(roomId)) {
            return roomClient.isAdminOfRoom(adminIdLong, roomIdLong).flatMap(
                    isAdminOfRoom -> {
                        if (isAdminOfRoom) {
                            connectionCountMapPerUserOrAdmin.put(roomId, new ConcurrentHashMap<>(Map.of(adminId, new AtomicInteger(1))));
                            logger.debug("Admin [{}] validated successfully for room [{}].", adminId, roomId);
                            return Mono.just(true);
                        }
                        logger.debug("Admin [{}] validated unsuccessfully for room [{}].", adminId, roomId);
                        return Mono.just(false);
                    });
        }
        if (connectionCountMapPerUserOrAdmin.get(roomId).computeIfAbsent(adminId, k -> new AtomicInteger()).getAndIncrement() == 0) {
            return roomClient.isAdminOfRoom(adminIdLong, roomIdLong).flatMap(
                    isAdminOfRoom -> {
                        if (isAdminOfRoom) {
                            logger.debug("Admin [{}] validated successfully for room [{}].", adminId, roomId);
                            return Mono.just(true);
                        }
                        logger.debug("Admin [{}] validated unsuccessfully for room [{}].", adminId, roomId);
                        return Mono.just(false);
                    });
        }
        logger.debug("Admin [{}] validated successfully for room [{}].", adminId, roomId);
        return Mono.just(true);
    }

    public Mono<Boolean> validateFirstConnectionPerUser(final String roomId, final String userId, final Map<String,Map<String, AtomicInteger>> connectionCountMapPerUserOrAdmin) {
        long roomIdLong = Long.parseLong(roomId);
        long userIdLong = Long.parseLong(userId);

        if (!connectionCountMapPerUserOrAdmin.containsKey(roomId)) {
            return roomClient.isUserMemberOfRoom(userIdLong, roomIdLong).flatMap(
                    isUserMemberOfRoom -> {
                        if (isUserMemberOfRoom) {
                            connectionCountMapPerUserOrAdmin.put(roomId, new ConcurrentHashMap<>(Map.of(userId, new AtomicInteger(1))));
                            logger.debug("User [{}] validated successfully for room [{}].", userId, roomId);
                            return Mono.just(true);
                        }
                        logger.debug("User [{}] validated unsuccessfully for room [{}].", userId, roomId);
                        return Mono.just(false);
                    });
        }
        if (connectionCountMapPerUserOrAdmin.get(roomId).computeIfAbsent(userId, k -> new AtomicInteger()).getAndIncrement() == 0) {
            return roomClient.isUserMemberOfRoom(userIdLong, roomIdLong).flatMap(
                    isUserMemberOfRoom -> {
                        if (isUserMemberOfRoom) {
                            logger.debug("User [{}] validated successfully for room [{}].", userId, roomId);
                            return Mono.just(true);
                        }
                        logger.debug("User [{}] validated unsuccessfully for room [{}].", userId, roomId);
                        return Mono.just(false);
                    });
        }
        logger.debug("User [{}] validated successfully for room [{}].", userId, roomId);
        return Mono.just(true);
    }
}
