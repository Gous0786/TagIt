package com.game.controller;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import com.game.event.RoomUpdateEvent;
import com.game.service.RoomService;

@Controller
public class WebSocketController {
    private final SimpMessagingTemplate messagingTemplate;
    private final RoomService roomService;

    public WebSocketController(SimpMessagingTemplate messagingTemplate, RoomService roomService) {
        this.messagingTemplate = messagingTemplate;
        this.roomService = roomService;
    }

    @EventListener
    public void handleRoomUpdate(RoomUpdateEvent event) {
        String roomId = event.getRoomId();
        messagingTemplate.convertAndSend("/topic/room/" + roomId, roomService.getRoom(roomId));
    }
}
