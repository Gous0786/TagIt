package com.game.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTyped;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperFieldModel.DynamoDBAttributeType;

@DynamoDBTable(tableName = "Players")
public class Player {
    private String playerId;
    private String playerName;
    private String currentRoomId;
    private boolean ready;
    private PlayerStatus status;

    @DynamoDBHashKey
    public String getPlayerId() { return playerId; }

    @DynamoDBAttribute
    public String getPlayerName() { return playerName; }

    @DynamoDBAttribute
    public String getCurrentRoomId() { return currentRoomId; }

    @DynamoDBAttribute
    public boolean isReady() { return ready; }

    @DynamoDBTyped(DynamoDBAttributeType.S)
    @DynamoDBAttribute
    public PlayerStatus getStatus() { return status; }

    public void setPlayerId(String playerId) { this.playerId = playerId; }

    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public void setCurrentRoomId(String currentRoomId) { this.currentRoomId = currentRoomId; }

    public void setReady(boolean ready) { this.ready = ready; }

    public void setStatus(PlayerStatus status) { this.status = status; }

    public enum PlayerStatus {
        ONLINE, IN_GAME, OFFLINE
    }
} 