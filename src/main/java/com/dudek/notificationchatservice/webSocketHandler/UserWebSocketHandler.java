package com.dudek.notificationchatservice.webSocketHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.core.publisher.Sinks;
import reactor.util.annotation.NonNull;

import java.net.URI;
import java.util.Map;

import static com.dudek.notificationchatservice.webSocketHandler.RoomWebSocketHandler.sessionSendMessageFlux;

@Component
public class UserWebSocketHandler implements WebSocketHandler {

    private final Map<String, Sinks.Many<WebSocketMessage>> sinkPoolForPrivateUsers;
    private final Logger logger = LoggerFactory.getLogger(UserWebSocketHandler.class);

    public UserWebSocketHandler(final @Qualifier("privateUsers") Map<String, Sinks.Many<WebSocketMessage>> sinkPoolForPrivateUsers) {
        this.sinkPoolForPrivateUsers = sinkPoolForPrivateUsers;
    }

    @Override
    @NonNull
    public Mono<Void> handle(WebSocketSession session) {
        String userId = extractUserIdFromUri(session.getHandshakeInfo().getUri());

        Sinks.Many<WebSocketMessage> sink = sinkPoolForPrivateUsers.get(userId);
        if (sink == null) {
            sink = Sinks.many().unicast().onBackpressureBuffer();
            sinkPoolForPrivateUsers.put(userId, sink);
            logger.debug("New private sink has been created for user: {}", userId);

        }
        return sessionSendMessageFlux(session, sink, "private-userSink", logger).doFinally(signalType -> {
            if (signalType == SignalType.ON_COMPLETE || signalType == SignalType.CANCEL) {
                sinkPoolForPrivateUsers.remove(userId);
                logger.debug("Session closed with signal type [{}]. UserId [{}] removed from sinkPool.", signalType, userId);
            }
        });
    }

    private String extractUserIdFromUri(URI uri) {
        String path = uri.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
