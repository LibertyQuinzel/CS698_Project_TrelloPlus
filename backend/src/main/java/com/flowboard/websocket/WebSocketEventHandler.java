package com.flowboard.websocket;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles WebSocket events from API Gateway v2.
 * Routes $connect, $disconnect, and $default events to appropriate handlers.
 */
public class WebSocketEventHandler {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final WebSocketConnectionManager connectionManager;

    public WebSocketEventHandler() {
        this.connectionManager = new WebSocketConnectionManager();
    }

    /**
     * Handle an API Gateway v2 WebSocket event.
     * Returns a Lambda proxy response.
     */
    public Map<String, Object> handleWebSocketEvent(Map<String, Object> event, Context context) {
        try {
            String routeKey = (String) event.get("routeKey");
            String connectionId = (String) event.get("requestContext");
            if (connectionId == null && event.get("requestContext") instanceof Map) {
                Map<String, Object> requestContext = (Map<String, Object>) event.get("requestContext");
                connectionId = (String) requestContext.get("connectionId");
            }

            String domainName = null;
            String stage = null;
            if (event.get("requestContext") instanceof Map) {
                Map<String, Object> requestContext = (Map<String, Object>) event.get("requestContext");
                domainName = (String) requestContext.get("domainName");
                stage = (String) requestContext.get("stage");
            }

            if (domainName != null && stage != null) {
                connectionManager.initManagementApi(domainName, stage);
            }

            if ("$connect".equals(routeKey)) {
                return handleConnect(connectionId);
            } else if ("$disconnect".equals(routeKey)) {
                return handleDisconnect(connectionId);
            } else if ("$default".equals(routeKey)) {
                String body = (String) event.get("body");
                return handleMessage(connectionId, body);
            }

            return errorResponse("Unknown route: " + routeKey);
        } catch (Exception e) {
            System.err.println("Error handling WebSocket event: " + e.getMessage());
            e.printStackTrace();
            return errorResponse("Internal server error: " + e.getMessage());
        } finally {
            connectionManager.close();
        }
    }

    private Map<String, Object> handleConnect(String connectionId) {
        try {
            connectionManager.storeConnection(connectionId);
            System.out.println("Client connected: " + connectionId);
            return successResponse();
        } catch (Exception e) {
            System.err.println("Error storing connection: " + e.getMessage());
            return errorResponse("Failed to register connection");
        }
    }

    private Map<String, Object> handleDisconnect(String connectionId) {
        try {
            connectionManager.removeConnection(connectionId);
            System.out.println("Client disconnected: " + connectionId);
            return successResponse();
        } catch (Exception e) {
            System.err.println("Error removing connection: " + e.getMessage());
            return errorResponse("Failed to unregister connection");
        }
    }

    private Map<String, Object> handleMessage(String connectionId, String body) {
        try {
            if (body == null || body.isEmpty()) {
                return errorResponse("Empty message");
            }

            // Parse message as JSON
            JsonNode message = objectMapper.readTree(body);

            // Extract action and payload
            String action = message.has("action") ? message.get("action").asText() : "unknown";

            System.out.println("Message from " + connectionId + " with action: " + action);

            // For now, just echo the message back
            // Later, broadcast board updates or other WebSocket messages
            connectionManager.sendMessage(connectionId, body);

            return successResponse();
        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
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
        response.put("statusCode", 400);
        response.put("body", message);
        return response;
    }
}
