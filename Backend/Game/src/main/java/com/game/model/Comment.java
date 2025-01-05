package com.game.model;

import com.amazonaws.services.dynamodbv2.datamodeling.*;

@DynamoDBTable(tableName = "Comments")
public class Comment {
    private String commentId;
    private String roomId;
    private String playerId;
    private String playerName;
    private String content;
    private int likes;
    private long timestamp;

    @DynamoDBHashKey
    public String getCommentId() { return commentId; }
    public void setCommentId(String commentId) { this.commentId = commentId; }

    @DynamoDBAttribute
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    @DynamoDBAttribute
    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }

    @DynamoDBAttribute
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    @DynamoDBAttribute
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    @DynamoDBAttribute
    public int getLikes() { return likes; }
    public void setLikes(int likes) { this.likes = likes; }

    @DynamoDBAttribute
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
} 