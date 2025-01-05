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

@Configuration
@Slf4j
public class DynamoDBConfig {

    @Value("${amazon.dynamodb.endpoint}")
    private String amazonDynamoDBEndpoint;

    @Value("${amazon.aws.accesskey}")
    private String amazonAWSAccessKey;

    @Value("${amazon.aws.secretkey}")
    private String amazonAWSSecretKey;

    @Value("${amazon.aws.region}")
    private String amazonAWSRegion;

    @Bean
    @Primary
    public AmazonDynamoDB amazonDynamoDB() {
        return AmazonDynamoDBClientBuilder.standard()
            .withEndpointConfiguration(
                new AwsClientBuilder.EndpointConfiguration(
                    "http://localhost:8000", 
                    "local"
                )
            )
            .withCredentials(
                new AWSStaticCredentialsProvider(
                    new BasicAWSCredentials("dummy", "dummy")
                )
            )
            .build();
    }

    @Bean
    @Primary
    public DynamoDBMapper dynamoDBMapper(AmazonDynamoDB amazonDynamoDB) {
        return new DynamoDBMapper(amazonDynamoDB);
    }

    @PostConstruct
    public void createTables() {
        try {
            AmazonDynamoDB dynamoDB = amazonDynamoDB();
            
            // Create tables if they don't exist
            createCommentsTable(dynamoDB);
            createRoomsTable(dynamoDB);
            createPlayersTable(dynamoDB);
            
            log.info("DynamoDB tables created/verified successfully");
        } catch (Exception e) {
            log.error("Error creating DynamoDB tables: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize DynamoDB tables", e);
        }
    }

    private void createCommentsTable(AmazonDynamoDB dynamoDB) {
        CreateTableRequest request = new CreateTableRequest()
            .withTableName("Comments")
            .withKeySchema(new KeySchemaElement("commentId", KeyType.HASH))
            .withAttributeDefinitions(
                new AttributeDefinition("commentId", ScalarAttributeType.S),
                new AttributeDefinition("roomId", ScalarAttributeType.S)
            )
            .withGlobalSecondaryIndexes(
                new GlobalSecondaryIndex()
                    .withIndexName("roomId-index")
                    .withKeySchema(new KeySchemaElement("roomId", KeyType.HASH))
                    .withProjection(new Projection().withProjectionType(ProjectionType.ALL))
                    .withProvisionedThroughput(new ProvisionedThroughput(5L, 5L))
            )
            .withProvisionedThroughput(new ProvisionedThroughput(5L, 5L));

        createTableAndWaitForActive(dynamoDB, request);
    }

    private void createRoomsTable(AmazonDynamoDB dynamoDB) {
        CreateTableRequest request = new CreateTableRequest()
            .withTableName("Rooms")
            .withKeySchema(new KeySchemaElement("roomId", KeyType.HASH))
            .withAttributeDefinitions(
                new AttributeDefinition("roomId", ScalarAttributeType.S)
            )
            .withProvisionedThroughput(new ProvisionedThroughput(5L, 5L));

        createTableAndWaitForActive(dynamoDB, request);
    }

    private void createPlayersTable(AmazonDynamoDB dynamoDB) {
        CreateTableRequest request = new CreateTableRequest()
            .withTableName("Players")
            .withKeySchema(new KeySchemaElement("playerId", KeyType.HASH))
            .withAttributeDefinitions(
                new AttributeDefinition("playerId", ScalarAttributeType.S),
                new AttributeDefinition("currentRoomId", ScalarAttributeType.S)
            )
            .withGlobalSecondaryIndexes(
                new GlobalSecondaryIndex()
                    .withIndexName("currentRoomId-index")
                    .withKeySchema(new KeySchemaElement("currentRoomId", KeyType.HASH))
                    .withProjection(new Projection().withProjectionType(ProjectionType.ALL))
                    .withProvisionedThroughput(new ProvisionedThroughput(5L, 5L))
            )
            .withProvisionedThroughput(new ProvisionedThroughput(5L, 5L));

        createTableAndWaitForActive(dynamoDB, request);
    }

    private void createTableAndWaitForActive(AmazonDynamoDB dynamoDB, CreateTableRequest request) {
        String tableName = request.getTableName();
        try {
            if (isTableActive(dynamoDB, tableName)) {
                log.info("Table {} already exists and is active", tableName);
                return;
            }

            log.info("Creating table: {}", tableName);
            dynamoDB.createTable(request);

            log.info("Waiting for table {} to become active...", tableName);
            long startTime = System.currentTimeMillis();
            while (!isTableActive(dynamoDB, tableName)) {
                if (System.currentTimeMillis() - startTime > 60000) { // 1 minute timeout
                    throw new RuntimeException("Timeout waiting for table " + tableName + " to become active");
                }
                Thread.sleep(1000);
            }
            log.info("Table {} is now active", tableName);

        } catch (Exception e) {
            log.error("Error creating/verifying table {}: {}", tableName, e.getMessage());
            throw new RuntimeException("Failed to create/verify table " + tableName, e);
        }
    }

    private boolean isTableActive(AmazonDynamoDB dynamoDB, String tableName) {
        try {
            TableDescription table = dynamoDB.describeTable(tableName).getTable();
            return table != null && table.getTableStatus().equals("ACTIVE");
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }
} 