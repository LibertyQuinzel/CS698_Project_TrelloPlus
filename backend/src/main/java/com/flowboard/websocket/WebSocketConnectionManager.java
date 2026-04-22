package com.flowboard.websocket;

import com.amazonaws.services.apigatewaymanagementapi.AmazonApiGatewayManagementApi;
import com.amazonaws.services.apigatewaymanagementapi.AmazonApiGatewayManagementApiClientBuilder;
import com.amazonaws.services.apigatewaymanagementapi.model.PostToConnectionRequest;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.client.builder.AwsClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages WebSocket connections stored in DynamoDB.
 * Provides methods to store/remove connections and broadcast messages to subscribed clients.
 * Handles all communication with API Gateway Management API for pushing messages to clients.
 */
@Slf4j
@Service
public class WebSocketConnectionManager {

    private AmazonDynamoDB dynamoDb;
    private AmazonApiGatewayManagementApi apiGateway;
    private final String TABLE_NAME = "flowboard-websocket-connections";
    private final String GSI_NAME = "BoardIdIndex";
    private String currentRegion = "us-east-2"; // Default region

    @PostConstruct
    public void initManagementApi() {
        String domainName = System.getenv("DOMAIN_NAME");
        String stage = System.getenv("STAGE");
        String region = "us-east-2";
        
        if (region != null && !region.isBlank()) {
            this.currentRegion = region;
        }
        
        // Initialize DynamoDB client with explicit region configuration
        initializeDynamoDbClient();
        
        if (domainName == null || domainName.isBlank()) {
            log.warn("DOMAIN_NAME environment variable not set. WebSocket API Gateway initialization deferred.");
            return;
        }
        
        if (stage == null || stage.isBlank()) {
            log.warn("STAGE environment variable not set. WebSocket API Gateway initialization deferred.");
            return;
        }
        
        initManagementApi(domainName, stage);
    }

    /**
     * Initialize DynamoDB client with explicit region configuration.
     * Ensures the client is properly configured for the Lambda execution environment.
     */
    private void initializeDynamoDbClient() {
        try {
            log.info("Initializing DynamoDB client for region: {}", currentRegion);
            this.dynamoDb = AmazonDynamoDBClientBuilder.standard()
                    .withRegion(currentRegion)
                    .build();
            log.info("DynamoDB client initialized successfully for region: {}", currentRegion);
        } catch (Exception e) {
            log.error("Failed to initialize DynamoDB client: {}", e.getMessage(), e);
            this.dynamoDb = null;
        }
    }

    /**
     * Initialize the API Gateway Management API client.
     * This is called during Lambda invocation with actual domain and stage.
     */
    public void initManagementApi(String domainName, String stage) {
        try {
            String endpoint = "https://" + domainName + "/" + stage;
            log.info("Initializing API Gateway Management API with endpoint: {}, region: {}", endpoint, currentRegion);
            
            this.apiGateway = AmazonApiGatewayManagementApiClientBuilder.standard()
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, currentRegion))
                    .build();
            
            log.info("API Gateway Management API initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize API Gateway Management API: {}", e.getMessage(), e);
            this.apiGateway = null;
        }
    }

    /**
     * Store a new WebSocket connection in DynamoDB.
     * Called immediately upon $connect event.
     */
    public void storeConnection(String connectionId) {
        try {
            if (connectionId == null || connectionId.isBlank()) {
                log.warn("Cannot store connection with null/blank connectionId");
                return;
            }
            
            if (dynamoDb == null) {
                log.error("DynamoDB client not initialized. Cannot store connection: {}", connectionId);
                return;
            }
            
            log.info("Storing connection: {}", connectionId);
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("connectionId", new AttributeValue(connectionId));
            item.put("timestamp", new AttributeValue().withN(String.valueOf(System.currentTimeMillis())));
            
            dynamoDb.putItem(new PutItemRequest(TABLE_NAME, item));
            log.info("Successfully stored connection: {}", connectionId);
        } catch (Exception e) {
            log.error("Failed to store connection {}: {}", connectionId, e.getMessage(), e);
        }
    }

    /**
     * Remove a WebSocket connection from DynamoDB.
     * Called upon $disconnect event or when posting to connection fails.
     */
    public void removeConnection(String connectionId) {
        try {
            if (connectionId == null || connectionId.isBlank()) {
                log.warn("Cannot remove connection with null/blank connectionId");
                return;
            }
            
            if (dynamoDb == null) {
                log.error("DynamoDB client not initialized. Cannot remove connection: {}", connectionId);
                return;
            }
            
            log.info("Removing connection: {}", connectionId);
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("connectionId", new AttributeValue(connectionId));
            
            dynamoDb.deleteItem(new DeleteItemRequest(TABLE_NAME, key));
            log.info("Successfully removed connection: {}", connectionId);
        } catch (Exception e) {
            log.error("Failed to remove connection {}: {}", connectionId, e.getMessage(), e);
        }
    }

    /**
     * Update a connection's boardId/subscription.
     * Called when client sends subscribe action.
     */
    public void updateConnectionBoardId(String connectionId, String boardId) {
        try {
            if (connectionId == null || connectionId.isBlank()) {
                log.warn("Cannot update connection with null/blank connectionId");
                return;
            }
            
            if (boardId == null || boardId.isBlank()) {
                log.warn("Cannot update connection {} with null/blank boardId", connectionId);
                return;
            }
            
            if (dynamoDb == null) {
                log.error("DynamoDB client not initialized. Cannot update connection: {}", connectionId);
                return;
            }
            
            log.info("Updating connection: {} to boardId: {}", connectionId, boardId);
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("connectionId", new AttributeValue(connectionId));

            UpdateItemRequest update = new UpdateItemRequest()
                    .withTableName(TABLE_NAME)
                    .withKey(key)
                    .withUpdateExpression("SET boardId = :bid, subscriptionTime = :ts")
                    .withExpressionAttributeValues(Map.of(
                            ":bid", new AttributeValue(boardId),
                            ":ts", new AttributeValue().withN(String.valueOf(System.currentTimeMillis()))
                    ));
            
            dynamoDb.updateItem(update);
            log.info("Successfully updated connection subscription");
        } catch (Exception e) {
            log.error("Failed to update connection {} boardId: {}", connectionId, e.getMessage(), e);
        }
    }

    /**
     * Broadcast a message to all connections subscribed to a specific boardId.
     * Automatically handles connection cleanup for failed sends.
     */
    public void broadcastToBoard(String boardId, String message) {
        try {
            if (boardId == null || boardId.isBlank()) {
                log.warn("Cannot broadcast with null/blank boardId");
                return;
            }
            
            if (message == null || message.isBlank()) {
                log.warn("Cannot broadcast null/blank message");
                return;
            }
            
            // Defensive check: dynamoDb might not be initialized
            if (dynamoDb == null) {
                log.error("DynamoDB client not initialized. Cannot broadcast to boardId: {}", boardId);
                return;
            }
            
            // Defensive check: apiGateway might still be initializing
            if (apiGateway == null) {
                log.warn("API Gateway Management API not initialized. Deferring broadcast for boardId: {}", boardId);
                return;
            }
            
            log.info("Broadcasting to boardId: {} with message length: {}", boardId, message.length());
            
            // Query for all connections linked to this boardId
            Map<String, AttributeValue> attrValues = Map.of(":bid", new AttributeValue(boardId));
            QueryRequest query = new QueryRequest()
                    .withTableName(TABLE_NAME)
                    .withIndexName(GSI_NAME)
                    .withKeyConditionExpression("boardId = :bid")
                    .withExpressionAttributeValues(attrValues);

            QueryResult results = dynamoDb.query(query);
            log.info("Found {} connections subscribed to boardId: {}", results.getItems().size(), boardId);

            // Push message to every connection
            int successCount = 0;
            int failureCount = 0;
            
            for (Map<String, AttributeValue> item : results.getItems()) {
                String connectionId = null;
                try {
                    connectionId = item.get("connectionId").getS();
                    
                    log.debug("Sending message to connection: {}", connectionId);
                    PostToConnectionRequest post = new PostToConnectionRequest()
                            .withConnectionId(connectionId)
                            .withData(ByteBuffer.wrap(message.getBytes()));
                    
                    apiGateway.postToConnection(post);
                    successCount++;
                    log.debug("Successfully sent message to connection: {}", connectionId);
                    
                } catch (Exception e) {
                    failureCount++;
                    log.warn("Failed to send message to connection {}: {}", connectionId, e.getMessage());
                    
                    // If postToConnection fails, the client is likely disconnected
                    // Remove stale connection from DynamoDB
                    if (connectionId != null) {
                        removeConnection(connectionId);
                    }
                }
            }
            
            log.info("Broadcast complete for boardId: {}. Success: {}, Failure: {}", boardId, successCount, failureCount);
            
        } catch (Exception e) {
            log.error("Error during broadcast for boardId {}: {}", boardId, e.getMessage(), e);
        }
    }

    /**
     * Get API Gateway Management API client.
     * Used for testing or advanced operations.
     */
    public AmazonApiGatewayManagementApi getApiGatewayClient() {
        return apiGateway;
    }

    /**
     * Check if API Gateway Management API is initialized.
     */
    public boolean isApiGatewayInitialized() {
        return apiGateway != null;
    }
}