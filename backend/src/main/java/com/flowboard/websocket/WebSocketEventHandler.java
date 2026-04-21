package com.flowboard.websocket;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

/**
 * AWS Lambda handler for WebSocket API Gateway events.
 * Routes $connect, $disconnect, and custom message actions.
 * Ensures the connection is NEVER prematurely terminated.
 */
public class WebSocketEventHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebSocketConnectionManager connectionManager;

    public WebSocketEventHandler() {
        this.connectionManager = new WebSocketConnectionManager();
        // This handler is instantiated directly by Lambda, not Spring.
        // Invoke initialization explicitly because @PostConstruct is not triggered here.
        this.connectionManager.initManagementApi();
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        context.getLogger().log("WebSocket Event Handler - Received event: " + event);
        
        // DEFENSIVE CHECK: Prevent crash if event is empty/null
        if (event == null || event.get("requestContext") == null) {
            context.getLogger().log("ERROR: Invalid event structure received");
            return errorResponse(400, "Invalid request context");
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> requestContext = (Map<String, Object>) event.get("requestContext");
            
            String routeKey = (String) requestContext.get("routeKey");
            String connectionId = (String) requestContext.get("connectionId");
            String domainName = (String) requestContext.get("domainName");
            String stage = (String) requestContext.get("stage");

            context.getLogger().log("Route: " + routeKey + ", ConnectionId: " + connectionId);

            // Initialize API Gateway Management API with domain and stage
            if (domainName != null && stage != null) {
                context.getLogger().log("Initializing API Gateway Management API");
                connectionManager.initManagementApi(domainName, stage);
            } else {
                context.getLogger().log("WARNING: domainName or stage is null");
            }

            // Route to appropriate handler based on routeKey
            if ("$connect".equals(routeKey)) {
                context.getLogger().log("Handling $connect for connectionId: " + connectionId);
                return handleConnect(connectionId, context);
            } else if ("$disconnect".equals(routeKey)) {
                context.getLogger().log("Handling $disconnect for connectionId: " + connectionId);
                return handleDisconnect(connectionId, context);
            } else if ("$default".equals(routeKey)) {
                context.getLogger().log("Handling $default (custom action) for connectionId: " + connectionId);
                String body = (String) event.get("body");
                return handleMessage(connectionId, body, context);
            } else {
                context.getLogger().log("Unknown routeKey: " + routeKey + ". Accepting to keep connection open.");
                return successResponse(200, "Unknown route - connection kept open");
            }
            
        } catch (Exception e) {
            context.getLogger().log("ERROR in handler: " + e.getMessage());
            e.printStackTrace();
            // Return success to keep connection OPEN - do not drop on error
            return successResponse(200, "Handled with error tolerance");
        }
    }

    private Map<String, Object> handleConnect(String connectionId, Context context) {
        try {
            context.getLogger().log("Storing connection in DynamoDB: " + connectionId);
            connectionManager.storeConnection(connectionId);
            context.getLogger().log("Successfully stored connection: " + connectionId);
            return successResponse(200, "Connected");
        } catch (Exception e) {
            context.getLogger().log("ERROR storing connection: " + e.getMessage());
            e.printStackTrace();
            // Still return success to keep connection open
            return successResponse(200, "Connection processed (error tolerance)");
        }
    }

    private Map<String, Object> handleDisconnect(String connectionId, Context context) {
        try {
            context.getLogger().log("Removing connection from DynamoDB: " + connectionId);
            connectionManager.removeConnection(connectionId);
            context.getLogger().log("Successfully removed connection: " + connectionId);
            return successResponse(200, "Disconnected");
        } catch (Exception e) {
            context.getLogger().log("ERROR removing connection: " + e.getMessage());
            e.printStackTrace();
            // Still return success
            return successResponse(200, "Disconnect processed (error tolerance)");
        }
    }

    private Map<String, Object> handleMessage(String connectionId, String body, Context context) {
        try {
            context.getLogger().log("Processing message for connectionId: " + connectionId);
            
            if (body == null || body.trim().isEmpty()) {
                context.getLogger().log("Body is empty for connectionId: " + connectionId);
                return successResponse(200, "Empty body accepted");
            }
            
            context.getLogger().log("Body: " + body);
            JsonNode message = objectMapper.readTree(body);
            String action = message.has("action") ? message.get("action").asText() : "unknown";
            
            context.getLogger().log("Action: " + action);
            
            if ("subscribe".equals(action)) {
                String boardId = message.has("boardId") ? message.get("boardId").asText() : null;
                
                if (boardId == null || boardId.trim().isEmpty()) {
                    context.getLogger().log("ERROR: Subscribe action missing boardId");
                    return successResponse(400, "Subscribe requires boardId");
                }
                
                context.getLogger().log("Subscribe action: connectionId=" + connectionId + ", boardId=" + boardId);
                connectionManager.updateConnectionBoardId(connectionId, boardId);
                context.getLogger().log("Successfully updated connection board subscription");
                return successResponse(200, "Subscribed to board: " + boardId);
            } else if ("ping".equals(action)) {
                context.getLogger().log("Ping received from: " + connectionId);
                return successResponse(200, "Pong");
            } else {
                context.getLogger().log("Unknown action: " + action + ". Keeping connection open.");
                return successResponse(200, "Action received: " + action);
            }
            
        } catch (Exception e) {
            context.getLogger().log("ERROR processing message: " + e.getMessage());
            e.printStackTrace();
            // Keep connection open despite error
            return successResponse(200, "Message processed with error tolerance");
        }
    }

    private Map<String, Object> successResponse(int statusCode, String body) {
        Map<String, Object> response = new HashMap<>();
        response.put("statusCode", statusCode);
        response.put("body", body);
        return response;
    }

    private Map<String, Object> errorResponse(int statusCode, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("statusCode", statusCode);
        response.put("body", message);
        return response;
    }
}