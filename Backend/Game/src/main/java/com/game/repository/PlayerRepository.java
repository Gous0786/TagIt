package com.game.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.game.model.Player;

@Repository
public class PlayerRepository {
    
    @Autowired
    private DynamoDBMapper dynamoDBMapper;

    public Player save(Player player) {
        dynamoDBMapper.save(player);
        return player;
    }

    public Optional<Player> findById(String playerId) {
        Player player = dynamoDBMapper.load(Player.class, playerId);
        return Optional.ofNullable(player);
    }

    public List<Player> findByRoomId(String roomId) {
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
            .withFilterExpression("currentRoomId = :roomId")
            .withExpressionAttributeValues(Map.of(
                ":roomId", new AttributeValue(roomId)
            ));

        return dynamoDBMapper.scan(Player.class, scanExpression);
    }

    public void updatePlayerStatus(String playerId, Player.PlayerStatus status) {
        Player player = findById(playerId).orElseThrow();
        player.setStatus(status);
        save(player);
    }

    public void delete(Player player) {
        dynamoDBMapper.delete(player);
    }

    public void updatePlayerRoom(String playerId, String roomId) {
        Player player = findById(playerId).orElseThrow();
        player.setCurrentRoomId(roomId);
        save(player);
    }
} 