package com.game.service;

import com.game.model.Comment;
import com.game.model.Room;
import com.game.repository.CommentRepository;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.stream.Collectors;
import org.springframework.scheduling.annotation.Async;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

@Service
@Slf4j
public class CommentService {
    private final CommentRepository commentRepository;
    private final RoomService roomService;
    private final Map<String, Set<String>> leaderboardRequests = new ConcurrentHashMap<>();

    public CommentService(CommentRepository commentRepository, RoomService roomService) {
        this.commentRepository = commentRepository;
        this.roomService = roomService;
    }

    public Comment addComment(String roomId, String playerId, String playerName, String content) {
        Room room = roomService.getRoom(roomId);
        if (room == null) {
            throw new IllegalArgumentException("Room not found");
        }

        Comment comment = new Comment();
        comment.setCommentId(UUID.randomUUID().toString());
        comment.setRoomId(roomId);
        comment.setPlayerId(playerId);
        comment.setPlayerName(playerName);
        comment.setContent(content);
        comment.setLikes(0);
        comment.setTimestamp(System.currentTimeMillis());

        return commentRepository.save(comment);
    }

    public List<Comment> getRoomComments(String roomId) {
        return commentRepository.findByRoomId(roomId);
    }

    public Comment likeComment(String commentId, String playerId) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new IllegalArgumentException("Comment not found"));
            
        comment.setLikes(comment.getLikes() + 1);
        return commentRepository.save(comment);
    }

    public Map<String, Object> getLeaderboard(String roomId, String playerId) {
        List<Comment> leaderboard = new ArrayList<>(getRoomComments(roomId));
        
        // Track who requested the leaderboard
        leaderboardRequests.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(playerId);
        
        // Check if all players have seen the leaderboard
        Room room = roomService.getRoom(roomId);
        int totalPlayers = room.getPlayers().size();
        int playersWhoChecked = leaderboardRequests.get(roomId).size();
        
        log.info("Leaderboard request for room {}: {}/{} players checked", 
            roomId, playersWhoChecked, totalPlayers);

        // If all players have checked, schedule room deletion
        if (playersWhoChecked >= totalPlayers) {
            scheduleRoomDeletion(roomId);
            leaderboardRequests.remove(roomId); // Cleanup
        }
        
        // Sort leaderboard
        leaderboard.sort((c1, c2) -> {
            int likesCompare = Integer.compare(c2.getLikes(), c1.getLikes());
            if (likesCompare != 0) {
                return likesCompare;
            }
            return Long.compare(c1.getTimestamp(), c2.getTimestamp());
        });

        // Determine the winning comment
        Comment winningComment = leaderboard.isEmpty() ? null : leaderboard.get(0);
        String winningPlayerName = winningComment != null ? winningComment.getPlayerName() : null;

        // Prepare response
        Map<String, Object> response = new HashMap<>();
        response.put("leaderboard", leaderboard);
        response.put("winningComment", winningComment);
        response.put("winningPlayerName", winningPlayerName);

        return response;
    }

    @Async
    protected void scheduleRoomDeletion(String roomId) {
        try {
            log.info("Scheduling deletion for room: {} in 5 seconds", roomId);
            TimeUnit.SECONDS.sleep(5);
            roomService.deleteRoom(roomId);
            log.info("Room deleted: {}", roomId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Room deletion interrupted for room: {}", roomId);
        } catch (Exception e) {
            log.error("Failed to delete room: {}", roomId, e);
        }
    }

    // Optional: Add a method to get top N comments
    public List<Comment> getTopComments(String roomId, int limit) {
        // Use a dummy playerId since this is just for viewing
        String dummyPlayerId = "system";
        Map<String, Object> leaderboardData = getLeaderboard(roomId, dummyPlayerId);
        List<Comment> leaderboard = (List<Comment>) leaderboardData.get("leaderboard");
        return leaderboard.subList(0, Math.min(limit, leaderboard.size()));
    }

    @Transactional
    public List<Comment> batchLikeComments(String playerId, List<String> commentIds) {
        if (commentIds == null || commentIds.isEmpty()) {
            return Collections.emptyList();
        }

        log.info("Processing batch likes for {} comments by player {}", commentIds.size(), playerId);
        
        // Get all comments in one query
        List<Comment> comments = commentRepository.findAllById(commentIds);
        
        // Validate all comments exist
        if (comments.size() != commentIds.size()) {
            Set<String> foundIds = comments.stream()
                .map(Comment::getCommentId)
                .collect(Collectors.toSet());
            List<String> missingIds = commentIds.stream()
                .filter(id -> !foundIds.contains(id))
                .collect(Collectors.toList());
            throw new IllegalArgumentException("Comments not found: " + missingIds);
        }

        // Update likes count for all comments
        comments.forEach(comment -> comment.setLikes(comment.getLikes() + 1));
        
        // Save all updates in batch
        return commentRepository.saveAll(comments);
    }
} 