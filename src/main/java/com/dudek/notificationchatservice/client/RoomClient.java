package com.dudek.notificationchatservice.client;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class RoomClient {
    private final WebClient webClient;
    private final String baseUrl;

    public RoomClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
        this.baseUrl = "http://localhost:8080";
    }

    public Mono<Boolean> isUserMemberOfRoom(Long userId, Long roomId) {
        return webClient.get()
                .uri(baseUrl + "/api/room/user-management/validate-user-for-room?userId={userId}&roomId={roomId}", userId, roomId)
                .retrieve()
                .bodyToMono(Boolean.class);
    }

    public Mono<Boolean> isAdminOfRoom(Long adminId, Long roomId) {
        return webClient.get()
                .uri(baseUrl + "/api/room/user-management/validate-admin-for-room?adminId={adminId}&roomId={roomId}", adminId, roomId)
                .retrieve()
                .bodyToMono(Boolean.class);
    }
}
