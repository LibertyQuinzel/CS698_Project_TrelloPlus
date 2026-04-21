package com.flowboard.websocket;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

/**
 * Main Entry Point for API Gateway v2 WebSocket events.
 */
public class WebSocketEventHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final WebSocketConnectionManager connectionManager;

    public WebSocketEventHandler() {
        this.connectionManager = new WebSocketConnectionManager();
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        try {
            // 1. Extract context
            Map<String, Object> requestContext = (Map<String, Object>) event.get("requestContext");
            String routeKey = (String) requestContext.get("routeKey");
            String connectionId = (String) requestContext.get("connectionId");
            String domainName = (String) requestContext.get("domainName");
            String stage = (String) requestContext.get("stage");

            // 2. Initialize Management API for callbacks (broadcasting)
            connectionManager.initManagementApi(domainName, stage);

            // 3. Route events
            switch (routeKey) {
                case "$connect":
                    return handleConnect(connectionId);
                case "$disconnect":
                    return handleDisconnect(connectionId);
                case "$default":
                    String body = (String) event.get("body");
                    return handleMessage(connectionId, body);
                default:
                    return errorResponse("Unknown route: " + routeKey);
            }
        } catch (Exception e) {
            System.err.println("Critical error: " + e.getMessage());
            return errorResponse("Internal server error");
        }
    }

    private Map<String, Object> handleConnect(String connectionId) {
        try {
            // Store connection in DynamoDB
            connectionManager.storeConnection(connectionId);
            return successResponse();
        } catch (Exception e) {
            return errorResponse("Failed to connect");
        }
    }

    private Map<String, Object> handleDisconnect(String connectionId) {
        try {
            connectionManager.removeConnection(connectionId);
            return successResponse();
        } catch (Exception e) {
            return errorResponse("Failed to disconnect");
        }
    }

    private Map<String, Object> handleMessage(String connectionId, String body) {
        try {
            JsonNode message = objectMapper.readTree(body);
            String action = message.has("action") ? message.get("action").asText() : "unknown";

            // ACTION: SUBSCRIBE
            // Link this specific connectionId to a boardId
            if ("subscribe".equals(action)) {
                String boardId = message.has("boardId") ? message.get("boardId").asText() : null;
                if (boardId != null) {
                    connectionManager.updateConnectionBoardId(connectionId, boardId);
                    System.out.println("Connection " + connectionId + " subscribed to board: " + boardId);
                    return successResponse();
                }
            }

            // ACTION: OTHER (e.g., MOVE_CARD)
            // You can add more routing logic here for your business actions
            
            return successResponse();
        } catch (Exception e) {
            return errorResponse("Failed to process message");
        }
    }

    private Map<String, Object> successResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("statusCode", 200);
        response.put("body", "OK");
        return response;
    }

    private Map<String, Object> errorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("statusCode", 500);
        response.put("body", message);
        return response;
    }
}