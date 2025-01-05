package com.game.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.game.model.Room;
import com.game.model.Player;
import com.game.service.RoomService;
import com.game.dto.RoomInfo;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {
    
    @Autowired
    private RoomService roomService;

    // Find/Join available room
    @PostMapping("/join")
    public ResponseEntity<Room> joinRoom(@RequestBody Player player) {
        return ResponseEntity.ok(roomService.assignRoom(player));
    }

    // Get list of active rooms
    @GetMapping
    public ResponseEntity<List<Room>> getRooms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(roomService.getActiveRooms(page, size));
    }

    // Leave current room
    @PostMapping("/leave")
    public ResponseEntity<Void> leaveRoom(
            @RequestParam String playerId) {
        roomService.leaveRoom(playerId);
        return ResponseEntity.ok().build();
    }

    // Get players in a room
    @GetMapping("/{roomId}/players")
    public ResponseEntity<List<Player>> getPlayers(@PathVariable String roomId) {
        return ResponseEntity.ok(roomService.getRoomPlayers(roomId));
    }

    // Set player ready status
    @PostMapping("/ready")
    public ResponseEntity<Void> setReady(
            @RequestParam String playerId,
            @RequestParam boolean ready) {
        roomService.setPlayerReady(playerId, ready);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{roomId}/reset")
    public ResponseEntity<Room> resetRoom(@PathVariable String roomId) {
        return ResponseEntity.ok(roomService.resetRoom(roomId));
    }

    // Development/debug endpoint only
    @GetMapping("/debug/all")
    public ResponseEntity<List<Room>> getAllRooms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(roomService.getAllRooms(page, size));
    }
    @GetMapping("/{roomId}/time")
    public ResponseEntity<Map<String, Integer>> getRoomTime(
            @PathVariable String roomId,
            @RequestParam String phase) {
        int timeLeft = roomService.getRoomTimeLeft(roomId, phase);
        return ResponseEntity.ok(Map.of("timeLeft", timeLeft));
    }
    @DeleteMapping("/debug/clear-all")
public ResponseEntity<Void> deleteAllRooms() {
    roomService.deleteAllRooms();
    return ResponseEntity.ok().build();
}

    @GetMapping("/list")
    public ResponseEntity<List<RoomInfo>> listAllRooms() {
        List<Room> rooms = roomService.getAllRooms(0, Integer.MAX_VALUE);
        List<RoomInfo> roomInfos = rooms.stream()
            .map(room -> new RoomInfo(
                room.getRoomId(),
                room.getStatus(),
                room.getPlayers().size(),
                room.getMinPlayers(),
                room.getMaxPlayers()
            ))
            .collect(Collectors.toList());
        return ResponseEntity.ok(roomInfos);
    }

    // Get room's generated image
    @GetMapping("/{roomId}/image")
    public ResponseEntity<Map<String, String>> getRoomImage(@PathVariable String roomId) {
        Room room = roomService.getRoom(roomId);
        if (room.getGameImageUrl() == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(Map.of("imageUrl", room.getGameImageUrl()));
    }

    // Force generate/regenerate room image (admin/debug endpoint)
    @PostMapping("/{roomId}/generate-image")
    public ResponseEntity<?> generateRoomImage(
            @PathVariable String roomId,
            @RequestParam(required = false) String customPrompt) {
        String imageUrl = roomService.generateRoomImage(roomId, customPrompt);
        if (imageUrl == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to generate image"));
        }
        return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
    }
} 