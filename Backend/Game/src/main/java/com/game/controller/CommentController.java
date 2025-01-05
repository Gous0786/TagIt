package com.game.controller;

import com.game.model.Comment;
import com.game.service.CommentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("/room/{roomId}")
    public ResponseEntity<Comment> addComment(
            @PathVariable String roomId,
            @RequestParam String playerId,
            @RequestParam String playerName,
            @RequestBody Map<String, String> request) {
        Comment comment = commentService.addComment(
            roomId, playerId, playerName, request.get("content"));
        return ResponseEntity.ok(comment);
    }

    @GetMapping("/room/{roomId}")
    public ResponseEntity<List<Comment>> getRoomComments(@PathVariable String roomId) {
        return ResponseEntity.ok(commentService.getRoomComments(roomId));
    }

    @PostMapping("/{commentId}/like")
    public ResponseEntity<Comment> likeComment(
            @PathVariable String commentId,
            @RequestParam String playerId) {
        return ResponseEntity.ok(commentService.likeComment(commentId, playerId));
    }

    @GetMapping("/room/{roomId}/leaderboard")
    public ResponseEntity<Map<String, Object>> getLeaderboard(
            @PathVariable String roomId,
            @RequestParam String playerId) {
        Map<String, Object> leaderboardData = commentService.getLeaderboard(roomId, playerId);
        if (((List<?>) leaderboardData.get("leaderboard")).isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(leaderboardData);
    }

    @GetMapping("/room/{roomId}/top")
    public ResponseEntity<List<Comment>> getTopComments(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "3") int limit) {
        List<Comment> topComments = commentService.getTopComments(roomId, limit);
        if (topComments.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(topComments);
    }

    @PostMapping("/batch-like")
    public ResponseEntity<List<Comment>> batchLikeComments(
            @RequestParam String playerId,
            @RequestBody List<String> commentIds) {
        List<Comment> updatedComments = commentService.batchLikeComments(playerId, commentIds);
        return ResponseEntity.ok(updatedComments);
    }
} 