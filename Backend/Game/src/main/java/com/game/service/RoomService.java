package com.game.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.context.ApplicationEventPublisher;
import com.game.event.RoomUpdateEvent;
import com.game.model.Room;
import com.amazonaws.services.kms.model.NotFoundException;
import com.game.model.Player;
import com.game.repository.RoomRepository;
import com.game.repository.PlayerRepository;    
import com.game.service.ImageGenerationService;
import java.util.Optional;
import com.game.model.Comment;
import com.game.repository.CommentRepository;
import java.util.Map;
import java.util.HashMap;
import com.game.controller.WebSocketController;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Timer;
import java.util.TimerTask;
import com.game.service.MemeService;
import java.util.Random;

@Service
@Slf4j
public class RoomService {
    
    @Autowired
    private RoomRepository roomRepository;
    
    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private ImageGenerationService imageGenerationService;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    private MemeService memeService;

    public RoomService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }
    public int getRoomTimeLeft(String roomId, String phase) {
        Room room = getRoom(roomId);
        if (room == null) {
            return 0;
        }

        long currentTime = System.currentTimeMillis();
        long phaseStartTime = room.getPhaseStartTime(); // You'll need to add this to your Room model
        int phaseDuration = phase.equals("COMMENTING") ? 60 : 60; // Duration in seconds

        int timeLeft = (int) ((phaseStartTime + (phaseDuration * 1000) - currentTime) / 1000);
        return Math.max(0, timeLeft);
    }

    public Room assignRoom(Player player) {
        // Check if player name is provided
        if (player.getPlayerName() == null || player.getPlayerName().isEmpty()) {
            log.warn("Player name is missing, setting default name");
            player.setPlayerName("Default Name");
        }

        // Add player to the room
        Room availableRoom = roomRepository.findOptimalWaitingRoom()
            .orElseGet(this::createNewRoom);

        availableRoom.getPlayers().add(player);
        player.setCurrentRoomId(availableRoom.getRoomId());
        
        // Save updates
        playerRepository.save(player);
        Room savedRoom = roomRepository.save(availableRoom);

        // Send WebSocket update
        sendRoomUpdate(savedRoom);

        // Check if room should start (4 players)
        if (savedRoom.getPlayers().size() >= savedRoom.getMinPlayers()) {
            startGame(savedRoom);
        }

        return savedRoom;
    }

    private Room createNewRoom() {
        Room room = new Room();
        room.setRoomId(UUID.randomUUID().toString());
        room.setStatus(Room.RoomStatus.WAITING);
        room.setCreatedAt(System.currentTimeMillis());
        room.setPlayers(new ArrayList<>());
        return roomRepository.save(room);
    }

    private void sendRoomUpdate(Room room) {
        Map<String, Object> roomUpdate = new HashMap<>();
        roomUpdate.put("roomId", room.getRoomId());
        roomUpdate.put("players", room.getPlayers());
        roomUpdate.put("status", room.getStatus());
        roomUpdate.put("playerCount", room.getPlayers().size());
        
        messagingTemplate.convertAndSend(
            "/topic/room/" + room.getRoomId(), 
            roomUpdate
        );
    }

    private void startGame(Room room) {
        room.setStatus(Room.RoomStatus.IN_GAME);
        roomRepository.save(room);

        // Assign a meme to the room
        assignMemeToRoom(room.getRoomId());

        // Send game start notification via WebSocket
        Map<String, Object> gameStart = new HashMap<>();
        gameStart.put("type", "GAME_START");
        gameStart.put("roomId", room.getRoomId());
        gameStart.put("imageUrl", room.getGameImageUrl());

        messagingTemplate.convertAndSend(
            "/topic/room/" + room.getRoomId() + "/game",
            gameStart
        );
    }

    // ... other service methods ...
      public void leaveRoom(String playerId) {
        Player player = playerRepository.findById(playerId)
            .orElseThrow(() -> new NotFoundException("Player not found"));

        String roomId = player.getCurrentRoomId();
        if (roomId != null) {
            Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException("Room not found"));

            // Remove player from room
            room.getPlayers().removeIf(p -> p.getPlayerId().equals(playerId));
            player.setCurrentRoomId(null);
            player.setReady(false);

            // If room is empty, delete it
            if (room.getPlayers().isEmpty()) {
                roomRepository.delete(room);
            } else {
                roomRepository.save(room);
            }
            
            playerRepository.save(player);
        }
        
        // Notify all clients about the room update
        eventPublisher.publishEvent(new RoomUpdateEvent(this, roomId));
    }

    public List<Room> getActiveRooms(int page, int size) {
        return roomRepository.findActiveRooms(page, size);
    }

    public List<Player> getRoomPlayers(String roomId) {
        return playerRepository.findByRoomId(roomId);
    }

    public void setPlayerReady(String playerId, boolean ready) {
        Player player = playerRepository.findById(playerId)
            .orElseThrow(() -> new NotFoundException("Player not found"));

        String roomId = player.getCurrentRoomId();
        if (roomId == null) {
            throw new IllegalStateException("Player is not in any room");
        }

        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new NotFoundException("Room not found"));

        // Update player ready status
        player.setReady(ready);
        playerRepository.save(player);

        // Check if all players are ready to start the game
        checkAndStartGame(room);
    }

    private void checkAndStartGame(Room room) {
        if (room.getStatus() != Room.RoomStatus.WAITING) {
            log.info("Room {} not in WAITING state, current state: {}", room.getRoomId(), room.getStatus());
            return;
        }

        List<Player> players = getRoomPlayers(room.getRoomId());
        long timeElapsed = System.currentTimeMillis() - room.getCreatedAt();
        
        log.info("Room {} status check - Players: {}/{}, Time elapsed: {}ms", 
            room.getRoomId(), 
            players.size(), 
            room.getMinPlayers(),
            timeElapsed);

        if (players.size() >= room.getMinPlayers() && timeElapsed > 5000) {
            log.info("Starting game for room {} with {} players", room.getRoomId(), players.size());
            
            String imagePrompt = String.format(
                "A vibrant multiplayer game scene with %d players in a fantasy arena, " +
                "digital art style, high energy, dramatic lighting, 4k, highly detailed", 
                players.size()
            );
            
            String imageUrl = imageGenerationService.generateImage(imagePrompt);
            
            if (imageUrl != null) {
                log.info("Generated image for room {}: {}", room.getRoomId(), imageUrl);
                room.setGameImageUrl(imageUrl);
                roomRepository.save(room);
            } else {
                log.warn("Failed to generate image for room {}", room.getRoomId());
            }
            
            startGame(room);
        }
    }

    @Async
    private void notifyGameStart(Room room) {
        // TODO: Implement WebSocket notification
        log.info("Game started in room: {}", room.getRoomId());
    }

    public void endGame(String roomId) {
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new NotFoundException("Room not found"));

        room.setStatus(Room.RoomStatus.FINISHED);
        roomRepository.save(room);

        // Reset players' status
        getRoomPlayers(roomId).forEach(player -> {
            player.setStatus(Player.PlayerStatus.ONLINE);
            player.setReady(false);
            playerRepository.save(player);
        });
    }

    public boolean isRoomReady(String roomId) {
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new NotFoundException("Room not found"));

        List<Player> players = getRoomPlayers(roomId);
        return players.size() >= room.getMinPlayers() && 
               players.stream().allMatch(Player::isReady);
    }

    public void kickPlayer(String roomId, String playerId) {
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new NotFoundException("Room not found"));

        if (room.getStatus() == Room.RoomStatus.IN_GAME) {
            throw new IllegalStateException("Cannot kick player during game");
        }

        leaveRoom(playerId);
    }    // Delete this simpler version
   
    // Keep the more detailed version that's already present

    public Room resetRoom(String roomId) {
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new NotFoundException("Room not found"));

        // Reset room to initial state
        room.setStatus(Room.RoomStatus.WAITING);
        room.getPlayers().clear();
        room.setCreatedAt(System.currentTimeMillis());

        // Save and return reset room
        return roomRepository.save(room);
    }

    public List<Room> getAllRooms(int page, int size) {
        return roomRepository.findAll(page, size);
    }

    public void deleteAllRooms() {
        List<Room> rooms = roomRepository.findAllRooms(0, Integer.MAX_VALUE);
        rooms.forEach(room -> {
            // Clear players from room
            room.getPlayers().forEach(player -> {
                player.setCurrentRoomId(null);
                playerRepository.save(player);
            });
            // Delete room
            roomRepository.delete(room);
        });
        log.info("Deleted all rooms");
    }

    public Room getRoom(String roomId) {
        if (roomId == null || roomId.trim().isEmpty()) {
            throw new IllegalArgumentException("Room ID cannot be null or empty");
        }
        
        log.info("Looking for room with ID: {}", roomId);
        Optional<Room> room = roomRepository.findById(roomId);
        
        if (room.isEmpty()) {
            log.error("Room not found with ID: {}", roomId);
            throw new NotFoundException("Room not found with ID: " + roomId);
        }
        
        return room.get();
    }

    public String generateRoomImage(String roomId, String customPrompt) {
        try {
            Room room = getRoom(roomId);
            log.info("Generating image for room: {}", roomId);
            
            String prompt = customPrompt != null ? customPrompt :
                String.format("A vibrant multiplayer game scene with %d players in a fantasy arena, " +
                    "digital art style, high energy, dramatic lighting, 4k, highly detailed", 
                    room.getPlayers().size());
                
            String imageUrl = imageGenerationService.generateImage(prompt);
            if (imageUrl != null) {
                room.setGameImageUrl(imageUrl);
                roomRepository.save(room);
                log.info("Successfully generated and saved image for room: {}", roomId);
            } else {
                log.error("Failed to generate image for room: {}", roomId);
            }
            
            return imageUrl;
        } catch (Exception e) {
            log.error("Error generating image for room {}: {}", roomId, e.getMessage());
            throw e;
        }
    }

    public void deleteRoom(String roomId) {
        Room room = getRoom(roomId);
        
        // Clear players from room
        room.getPlayers().forEach(player -> {
            player.setCurrentRoomId(null);
            playerRepository.save(player);
        });
        
        // Delete all comments for this room
        List<Comment> comments = commentRepository.findByRoomId(roomId);
        commentRepository.deleteAll(comments);  // Using batch delete
        
        // Delete the room
        roomRepository.delete(room);
        
        log.info("Room and associated data deleted: {}", roomId);
    }

    public void startTimer(String roomId, long duration) {
        Map<String, Object> timerStart = new HashMap<>();
        timerStart.put("type", "TIMER_START");
        timerStart.put("roomId", roomId);
        timerStart.put("duration", duration); // Duration in milliseconds

        messagingTemplate.convertAndSend(
            "/topic/room/" + roomId + "/timer",
            timerStart
        );

        // Schedule a task to stop the timer after the duration
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                stopTimer(roomId);
            }
        }, duration);
    }

    public void stopTimer(String roomId) {
        Map<String, Object> timerStop = new HashMap<>();
        timerStop.put("type", "TIMER_STOP");
        timerStop.put("roomId", roomId);

        messagingTemplate.convertAndSend(
            "/topic/room/" + roomId + "/timer",
            timerStop
        );
    }

    public void assignMemeToRoom(String roomId) {
        List<Map<String, Object>> memes = memeService.fetchMemes();
        if (!memes.isEmpty()) {
            // Select a random meme
            Random random = new Random();
            Map<String, Object> meme = memes.get(random.nextInt(memes.size()));
            
            Room room = getRoom(roomId);
            room.setGameImageUrl((String) meme.get("url"));
            roomRepository.save(room);
            log.info("Assigned meme to room {}: {}", roomId, meme.get("name"));
        } else {
            log.warn("No memes available to assign to room {}", roomId);
        }
    }
}
