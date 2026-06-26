package com.habittracker.backend.controller;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import java.util.Map;

@Controller
public class ChallengeRoomMessageController {

    /**
     * Listens to incoming payloads at: /app/room/{roomId}/action
     * Automatically broadcasts the returned map to everyone subscribed to: /topic/room/{roomId}
     */
    @MessageMapping("/room/{roomId}/action")
    @SendTo("/topic/room/{roomId}")
    public Map<String, Object> broadcastRoomProgression(
            @DestinationVariable String roomId,
            Map<String, Object> payload) {

        // Expecting keys like "username", "actionType" ("COMPLETED" / "UNDO"), and "habitTitle"
        return payload;
    }
}