package com.game.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.game.model.Comment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class CommentRepository {
    private DynamoDBMapper dynamoDBMapper;

    @Autowired
    public void setDynamoDBMapper(DynamoDBMapper dynamoDBMapper) {
        this.dynamoDBMapper = dynamoDBMapper;
    }

    public Comment save(Comment comment) {
        dynamoDBMapper.save(comment);
        return comment;
    }

    public List<Comment> findByRoomId(String roomId) {
        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":roomId", new AttributeValue().withS(roomId));

        DynamoDBQueryExpression<Comment> queryExpression = new DynamoDBQueryExpression<Comment>()
            .withIndexName("roomId-index")
            .withConsistentRead(false)
            .withKeyConditionExpression("roomId = :roomId")
            .withExpressionAttributeValues(eav);

        return dynamoDBMapper.query(Comment.class, queryExpression);
    }

    public Optional<Comment> findById(String commentId) {
        return Optional.ofNullable(dynamoDBMapper.load(Comment.class, commentId));
    }

    public List<Comment> findAllById(List<String> commentIds) {
        if (commentIds == null || commentIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Use DynamoDB batch load operation
        Map<String, List<Object>> results = dynamoDBMapper.batchLoad(
            commentIds.stream()
                .map(id -> {
                    Comment comment = new Comment();
                    comment.setCommentId(id);
                    return comment;
                })
                .collect(Collectors.toList())
        );

        // Extract comments from results
        List<Comment> comments = results.values().stream()
            .flatMap(List::stream)
            .map(obj -> (Comment) obj)
            .collect(Collectors.toList());

        return comments;
    }

    public List<Comment> saveAll(List<Comment> comments) {
        // Use DynamoDB batch write operation
        List<DynamoDBMapper.FailedBatch> failures = dynamoDBMapper.batchSave(comments);
        if (!failures.isEmpty()) {
            log.error("Failed to save some comments: {}", failures);
            throw new RuntimeException("Failed to save all comments");
        }
        return comments;
    }

    public void delete(Comment comment) {
        if (comment != null) {
            dynamoDBMapper.delete(comment);
            log.info("Deleted comment: {}", comment.getCommentId());
        }
    }

    public void deleteAll(List<Comment> comments) {
        if (comments != null && !comments.isEmpty()) {
            List<DynamoDBMapper.FailedBatch> failures = dynamoDBMapper.batchDelete(comments);
            if (!failures.isEmpty()) {
                log.error("Failed to delete some comments: {}", failures);
                throw new RuntimeException("Failed to delete all comments");
            }
            log.info("Deleted {} comments", comments.size());
        }
    }
}