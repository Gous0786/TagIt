package com.game.event;

import org.springframework.context.ApplicationEvent;

public class RoomUpdateEvent extends ApplicationEvent {
    private final String roomId;

    public RoomUpdateEvent(Object source, String roomId) {
        super(source);
        this.roomId = roomId;
    }

    public String getRoomId() {
        return roomId;
    }
} 