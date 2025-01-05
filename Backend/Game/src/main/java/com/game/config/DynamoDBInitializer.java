package com.game.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;

@Component
public class DynamoDBInitializer implements InitializingBean {
    
    @Autowired
    private AmazonDynamoDB amazonDynamoDB;

    @Override
    public void afterPropertiesSet() {
        createTableIfNotExists("Rooms", "roomId");
        createTableIfNotExists("Players", "playerId");
    }

    private void createTableIfNotExists(String tableName, String hashKeyName) {
        try {
            CreateTableRequest request = new CreateTableRequest()
                .withTableName(tableName)
                .withKeySchema(new KeySchemaElement(hashKeyName, KeyType.HASH))
                .withAttributeDefinitions(new AttributeDefinition(hashKeyName, ScalarAttributeType.S))
                .withProvisionedThroughput(new ProvisionedThroughput(5L, 5L));

            amazonDynamoDB.createTable(request);
        } catch (ResourceInUseException e) {
            // Table already exists
        }
    }
} 