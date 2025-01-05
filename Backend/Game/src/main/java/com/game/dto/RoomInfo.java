package com.game.dto;

import com.game.model.Room;

public class RoomInfo {
    private String roomId;
    private Room.RoomStatus status;
    private int currentPlayers;
    private int minPlayers;
    private int maxPlayers;

    public RoomInfo(String roomId, Room.RoomStatus status, int currentPlayers, int minPlayers, int maxPlayers) {
        this.roomId = roomId;
        this.status = status;
        this.currentPlayers = currentPlayers;
        this.minPlayers = minPlayers;
        this.maxPlayers = maxPlayers;
    }

    // Getters and setters
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public Room.RoomStatus getStatus() { return status; }
    public void setStatus(Room.RoomStatus status) { this.status = status; }

    public int getCurrentPlayers() { return currentPlayers; }
    public void setCurrentPlayers(int currentPlayers) { this.currentPlayers = currentPlayers; }

    public int getMinPlayers() { return minPlayers; }
    public void setMinPlayers(int minPlayers) { this.minPlayers = minPlayers; }

    public int getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }
} 