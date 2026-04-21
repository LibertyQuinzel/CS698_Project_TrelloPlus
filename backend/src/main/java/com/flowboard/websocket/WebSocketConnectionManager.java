package com.flowboard.websocket;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.apigatewaymanagementapi.ApiGatewayManagementApiClient;
import software.amazon.awssdk.services.apigatewaymanagementapi.model.PostToConnectionRequest;
import software.amazon.awssdk.services.apigatewaymanagementapi.model.GoneException;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages WebSocket connections stored in DynamoDB and broadcasts messages to clients
 * via API Gateway Management API.
 */
public class WebSocketConnectionManager {
    private static final String TABLE_NAME = "flowboard-websocket-connections";
    private static final String CONNECTION_ID_KEY = "connectionId";
    private final AmazonDynamoDB dynamoDb;
    private ApiGatewayManagementApiClient managementApiClient;

    public WebSocketConnectionManager() {
        this.dynamoDb = AmazonDynamoDBClientBuilder.defaultClient();
    }

    /**
     * InitializeManagementAPI client with the correct endpoint.
     */
    public void initManagementApi(String domainName, String stage) {
        String endpoint = "https://" + domainName + "/" + stage;
        this.managementApiClient = ApiGatewayManagementApiClient.builder()
            .endpointOverride(URI.create(endpoint))
            .build();
    }

    /**
     * Store a new WebSocket connection.
     */
    public void storeConnection(String connectionId) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put(CONNECTION_ID_KEY, new AttributeValue().withS(connectionId));
        item.put("timestamp", new AttributeValue().withN(String.valueOf(System.currentTimeMillis())));

        PutItemRequest request = new PutItemRequest()
            .withTableName(TABLE_NAME)
            .withItem(item);

        dynamoDb.putItem(request);
    }

    /**
     * Remove a disconnected WebSocket connection.
     */
    public void removeConnection(String connectionId) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put(CONNECTION_ID_KEY, new AttributeValue().withS(connectionId));

        DeleteItemRequest request = new DeleteItemRequest()
            .withTableName(TABLE_NAME)
            .withKey(key);

        dynamoDb.deleteItem(request);
    }

    /**
     * Send a message to a specific connection.
     */
    public void sendMessage(String connectionId, String message) {
        if (managementApiClient == null) {
            return;  // Management API not initialized
        }

        try {
            PostToConnectionRequest request = PostToConnectionRequest.builder()
                .connectionId(connectionId)
                .data(SdkBytes.fromUtf8String(message))
                .build();

            managementApiClient.postToConnection(request);
        } catch (GoneException e) {
            // Connection is gone, remove it from DynamoDB
            removeConnection(connectionId);
        } catch (Exception e) {
            System.err.println("Failed to send message to connection " + connectionId + ": " + e.getMessage());
        }
    }

    /**
     * Broadcast a message to all connected clients.
     */
    public void broadcastMessage(String message) {
        ScanRequest request = new ScanRequest()
            .withTableName(TABLE_NAME);

        ScanResult result = dynamoDb.scan(request);

        for (Map<String, AttributeValue> item : result.getItems()) {
            String connectionId = item.get(CONNECTION_ID_KEY).getS();
            sendMessage(connectionId, message);
        }
    }

    /**
     * Close the management API client.
     */
    public void close() {
        if (managementApiClient != null) {
            managementApiClient.close();
        }
    }
}
