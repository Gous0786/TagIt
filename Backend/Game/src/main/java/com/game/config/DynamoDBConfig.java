package com.game.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import java.util.Arrays;

import com.game.model.Room;
import com.game.model.Comment;

@Configuration
@Slf4j
public class DynamoDBConfig {

    @Value("${amazon.aws.accesskey}")
    private String amazonAWSAccessKey;

    @Value("${amazon.aws.secretkey}")
    private String amazonAWSSecretKey;

    @Value("${amazon.aws.region}")
    private String amazonAWSRegion;

    @Bean
    public AmazonDynamoDB amazonDynamoDB() {
        return AmazonDynamoDBClientBuilder.standard()
            .withCredentials(new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(amazonAWSAccessKey, amazonAWSSecretKey)))
            .withRegion(amazonAWSRegion)
            .build();
    }

    @Bean
    public DynamoDBMapper dynamoDBMapper() {
        return new DynamoDBMapper(amazonDynamoDB());
    }

    @PostConstruct
    public void createTables() {
        try {
            AmazonDynamoDB dynamoDB = amazonDynamoDB();
            DynamoDBMapper dynamoDBMapper = new DynamoDBMapper(dynamoDB);

            // Create Rooms table
            CreateTableRequest roomsRequest = dynamoDBMapper
                .generateCreateTableRequest(Room.class)
                .withProvisionedThroughput(new ProvisionedThroughput(5L, 5L));
            createTableIfNotExists(dynamoDB, roomsRequest, "Rooms");

            // Create Comments table with roomId-index
            CreateTableRequest commentsRequest = dynamoDBMapper
                .generateCreateTableRequest(Comment.class)
                .withProvisionedThroughput(new ProvisionedThroughput(5L, 5L))
                .withGlobalSecondaryIndexes(
                    new GlobalSecondaryIndex()
                        .withIndexName("roomId-index")
                        .withKeySchema(new KeySchemaElement("roomId", KeyType.HASH))
                        .withProjection(new Projection().withProjectionType(ProjectionType.ALL))
                        .withProvisionedThroughput(new ProvisionedThroughput(5L, 5L))
                );
            createTableIfNotExists(dynamoDB, commentsRequest, "Comments");

        } catch (Exception e) {
            log.error("Error creating DynamoDB tables: " + e.getMessage());
            throw new RuntimeException("Failed to create/verify tables", e);
        }
    }

    private void createTableIfNotExists(AmazonDynamoDB dynamoDB, CreateTableRequest request, String tableName) {
        try {
            dynamoDB.describeTable(tableName);
            log.info("Table {} already exists", tableName);
        } catch (ResourceNotFoundException e) {
            try {
                dynamoDB.createTable(request);
                log.info("Created table {}", tableName);
            } catch (Exception ex) {
                log.error("Error creating table {}: {}", tableName, ex.getMessage());
                throw new RuntimeException("Failed to create table " + tableName, ex);
            }
        }
    }
} 