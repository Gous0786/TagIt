package com.game.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import java.util.List;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperFieldModel.DynamoDBAttributeType;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTyped;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverted;
import com.game.config.PlayerListConverter;

@DynamoDBTable(tableName = "Rooms")
public class Room {
    private String roomId;
    private int maxPlayers = 10;
    private List<Player> players;
    private RoomStatus status;
    private long createdAt;
    private int minPlayers = 4;
    private String gameImageUrl;
    private long phaseStartTime;

    @DynamoDBHashKey
    public String getRoomId() { return roomId; }

    @DynamoDBAttribute
    public int getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }

    @DynamoDBAttribute
    @DynamoDBTypeConverted(converter = PlayerListConverter.class)
    public List<Player> getPlayers() { return players; }
    public void setPlayers(List<Player> players) { this.players = players; }

    @DynamoDBTyped(DynamoDBAttributeType.S)
    @DynamoDBAttribute
    public RoomStatus getStatus() { return status; }
    public void setStatus(RoomStatus status) { this.status = status; }

    @DynamoDBAttribute
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    @DynamoDBAttribute
    public int getMinPlayers() { return minPlayers; }
    public void setMinPlayers(int minPlayers) { this.minPlayers = minPlayers; }

    @DynamoDBAttribute
    public String getGameImageUrl() { return gameImageUrl; }
    public void setGameImageUrl(String gameImageUrl) { this.gameImageUrl = gameImageUrl; }

    @DynamoDBAttribute
    public long getPhaseStartTime() { return phaseStartTime; }
    public void setPhaseStartTime(long phaseStartTime) { this.phaseStartTime = phaseStartTime; }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public enum RoomStatus {
        WAITING, IN_GAME, FINISHED
    }
} 