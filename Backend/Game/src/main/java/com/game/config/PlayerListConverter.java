package com.game.config;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.model.Player;
import java.util.List;

public class PlayerListConverter implements DynamoDBTypeConverter<String, List<Player>> {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String convert(List<Player> players) {
        try {
            return mapper.writeValueAsString(players);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Player> unconvert(String playerString) {
        try {
            return mapper.readValue(playerString, new TypeReference<List<Player>>() {});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
} 