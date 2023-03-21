package com.dudek.notificationchatservice.webSocketHandler;

import com.dudek.notificationchatservice.validation.RoomWebSocketValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.core.publisher.Sinks;
import reactor.util.annotation.NonNull;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RoomWebSocketHandler implements WebSocketHandler {

    private final Map<String, Sinks.Many<WebSocketMessage>> sinkPoolForAdmins;
    private final Map<String, Sinks.Many<WebSocketMessage>> sinkPoolForUsers;
    private final RoomWebSocketValidator roomWebSocketValidator;
    private static final Map<String,Map<String, AtomicInteger>> connectionCountMapPerUserOrAdmin = new ConcurrentHashMap<>();
    private final Logger logger = LoggerFactory.getLogger(RoomWebSocketHandler.class);

    public RoomWebSocketHandler(final @Qualifier("roomAdmins") Map<String, Sinks.Many<WebSocketMessage>> sinkPoolForAdmins, final @Qualifier("roomUsers") Map<String, Sinks.Many<WebSocketMessage>> sinkPoolForUsers, final RoomWebSocketValidator roomWebSocketValidator) {
        this.sinkPoolForAdmins = sinkPoolForAdmins;
        this.sinkPoolForUsers = sinkPoolForUsers;
        this.roomWebSocketValidator = roomWebSocketValidator;
    }

    @Override
    @NonNull
    public Mono<Void> handle(WebSocketSession session) {
        logger.debug("New session [{}] opened.", session.getId());
        URI uri = session.getHandshakeInfo().getUri();
        String roomId = extractRoomIdFromUri(uri);
        String userId = extractUserIdFromSession(session);
        boolean isSessionForAdmin = isSessionForAdmin(session);
        Map<String, Sinks.Many<WebSocketMessage>> sinkPool = isSessionForAdmin ? sinkPoolForAdmins : sinkPoolForUsers;

        String sinkName = isSessionForAdmin ? "sinkPoolForAdmins" : "sinkPoolForUsers";

        Mono<Boolean> shouldHandleSession = isSessionForAdmin ?
                roomWebSocketValidator.validateFirstConnectionPerAdmin(roomId, userId, connectionCountMapPerUserOrAdmin) :
                roomWebSocketValidator.validateFirstConnectionPerUser(roomId, userId, connectionCountMapPerUserOrAdmin);

        return shouldHandleSession
                .filter(shouldHandle -> shouldHandle)
                .flatMap(shouldHandle -> {
                    Sinks.Many<WebSocketMessage> sink = sinkPool.get(roomId);
                    if (sink == null) {
                        sink = Sinks.many().multicast().onBackpressureBuffer();
                        sinkPool.put(roomId, sink);
                        logger.debug("New sink [{}] has been created for room: {}", sink, roomId);
                    } else {
                        logger.debug("Sink [{}] found for room: {}", sink, roomId);
                    }

                    Sinks.Many<WebSocketMessage> finalSink = sink;
                    return sessionSendMessageFlux(session, finalSink, sinkName, logger)
                            .doOnSubscribe(sub -> finalSink.asFlux().subscribe());
                });
    }

    @NotNull
    static Mono<Void> sessionSendMessageFlux(final WebSocketSession session, final Sinks.Many<WebSocketMessage> sink, String sinkName, final Logger logger) {
        logger.debug("Current subscribers count [{}] to sink: {}", sink.currentSubscriberCount(), sinkName);
        Flux<WebSocketMessage> messageFlux = sink
                .asFlux()
                .map(webSocketMessage -> session.textMessage(webSocketMessage.getPayloadAsText()))
                .onErrorResume(throwable -> {
                            logger.debug("MessageFlux has not been send due to error: " + throwable.getMessage());
                            return Mono.empty();
                        });

        return session
                .send(messageFlux)
                .then()
                .doFinally(signalType -> {
                    if (signalType == SignalType.ON_COMPLETE || signalType == SignalType.CANCEL) {
                        logger.debug("Connection closed for session ID: {}", session.getId());
                    }
                });
    }

    private String extractRoomIdFromUri(final URI uri) {
        String[] pathSegments = uri.getPath().split("/");
        return pathSegments[pathSegments.length - 1];
    }

    private String extractUserIdFromSession(final WebSocketSession session) {
        String query = session.getHandshakeInfo().getUri().getQuery();
        return query.substring(query.lastIndexOf("userId=") + 7).split("&")[0];
    }

    private boolean isSessionForAdmin(final WebSocketSession session) {
        return session.getHandshakeInfo().getUri().getPath().contains("as-admin");
    }
}
