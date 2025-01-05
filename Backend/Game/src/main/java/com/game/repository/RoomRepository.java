package com.game.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.game.model.Room;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Comparator;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Repository
public class RoomRepository {
    
    private static final Logger log = LoggerFactory.getLogger(RoomRepository.class);

    @Autowired
    private DynamoDBMapper dynamoDBMapper;

    public Optional<Room> findAvailableRoom() {
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
            .withFilterExpression("roomStatus = :status and size(players) < :maxPlayers")
            .withExpressionAttributeValues(Map.of(
                ":status", new AttributeValue(Room.RoomStatus.WAITING.toString()),
                ":maxPlayers", new AttributeValue().withN("10")
            ));

        List<Room> availableRooms = dynamoDBMapper.scan(Room.class, scanExpression);
        return availableRooms.stream().findFirst();
    }

    public Room save(Room room) {
        dynamoDBMapper.save(room);
        return room;
    }

    public Optional<Room> findById(String roomId) {
        Room room = dynamoDBMapper.load(Room.class, roomId);
        return Optional.ofNullable(room);
    }

    public List<Room> findActiveRooms(int page, int size) {
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
            .withFilterExpression("roomStatus IN (:waiting, :inGame)")
            .withExpressionAttributeValues(Map.of(
                ":waiting", new AttributeValue().withS(Room.RoomStatus.WAITING.toString()),
                ":inGame", new AttributeValue().withS(Room.RoomStatus.IN_GAME.toString())
            ));

        try {
            List<Room> rooms = dynamoDBMapper.scan(Room.class, scanExpression);
            if (rooms == null) return new ArrayList<>();
            
            int start = page * size;
            int end = Math.min(start + size, rooms.size());
            return start >= rooms.size() ? new ArrayList<>() : rooms.subList(start, end);
        } catch (Exception e) {
            log.error("Error fetching active rooms: ", e);
            return new ArrayList<>();
        }
    }

    public void delete(Room room) {
        dynamoDBMapper.delete(room);
    }

    public Optional<Room> findOptimalRoom() {
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
            .withFilterExpression("roomStatus = :status and size(players) < :maxPlayers")
            .withExpressionAttributeValues(Map.of(
                ":status", new AttributeValue(Room.RoomStatus.WAITING.toString()),
                ":maxPlayers", new AttributeValue().withN("10")
            ));

        List<Room> availableRooms = dynamoDBMapper.scan(Room.class, scanExpression);
        
        // First try to find a room with players
        Optional<Room> roomWithPlayers = availableRooms.stream()
            .filter(r -> !r.getPlayers().isEmpty())
            .max(Comparator.comparingInt(r -> r.getPlayers().size()));

        // If no room with players exists, then look for empty rooms or return empty to create new
        return roomWithPlayers.isPresent() ? roomWithPlayers : availableRooms.stream().findFirst();
    }

    public List<Room> findAllRooms(int page, int size) {
        try {
            DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
            List<Room> rooms = dynamoDBMapper.scan(Room.class, scanExpression);
            
            log.info("Found {} total rooms", rooms.size());
            
            // Apply pagination
            int start = page * size;
            int end = Math.min(start + size, rooms.size());
            return start >= rooms.size() ? new ArrayList<>() : rooms.subList(start, end);
        } catch (Exception e) {
            log.error("Error fetching rooms: ", e);
            return new ArrayList<>();
        }
    }

    public List<Room> findAll(int page, int size) {
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
        List<Room> rooms = dynamoDBMapper.scan(Room.class, scanExpression);
        
        int start = page * size;
        int end = Math.min(start + size, rooms.size());
        return start >= rooms.size() ? new ArrayList<>() : rooms.subList(start, end);
    }

    private Map<String, AttributeValue> calculateStartKey(int page, int size) {
        if (page == 0) return null;
        // Implement pagination logic here if needed
        return null;
    }

    public Optional<Room> findOptimalWaitingRoom() {
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
        
        log.info("Scanning for all rooms...");
        List<Room> allRooms = dynamoDBMapper.scan(Room.class, scanExpression);
        log.info("Found {} total rooms", allRooms.size());
        
        // Filter in memory instead of in DynamoDB query
        List<Room> waitingRooms = allRooms.stream()
            .filter(r -> r.getStatus() == Room.RoomStatus.WAITING)
            .filter(r -> r.getPlayers().size() < r.getMaxPlayers())
            .toList();
            
        log.info("Found {} waiting rooms with space", waitingRooms.size());
        waitingRooms.forEach(room -> 
            log.info("Room ID: {}, Players: {}/{}, Status: {}", 
                room.getRoomId(), 
                room.getPlayers().size(), 
                room.getMaxPlayers(), 
                room.getStatus())
        );
        
        // Return room with most players that still has space
        return waitingRooms.stream()
            .max(Comparator.comparingInt(r -> r.getPlayers().size()));
    }
    
    
    public Optional<Room> findEmptyWaitingRoom() {
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
            .withFilterExpression("roomStatus = :status AND size(players) = :empty")
            .withExpressionAttributeValues(Map.of(
                ":status", new AttributeValue().withS(Room.RoomStatus.WAITING.toString()),
                ":empty", new AttributeValue().withN("0")
            ));

        List<Room> emptyRooms = dynamoDBMapper.scan(Room.class, scanExpression);
        return emptyRooms.stream().findFirst();
    }
} 