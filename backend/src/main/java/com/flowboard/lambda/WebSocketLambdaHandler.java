package com.flowboard.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.flowboard.websocket.WebSocketEventHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * AWS Lambda handler for API Gateway v2 WebSocket events.
 * 
 * Handles $connect, $disconnect, and $default route events from WebSocket API Gateway.
 * Delegates to WebSocketEventHandler which manages connections in DynamoDB and
 * broadcasts messages via API Gateway Management API.
 * 
 * This is a separate handler from StreamLambdaHandler because:
 * - WebSocket events have a different format than REST API events
 * - WebSocket invocations are stateless (one per event)
 * - REST API needs Spring Boot container; WebSocket just needs event routing
 */
public class WebSocketLambdaHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final WebSocketEventHandler webSocketEventHandler;

    public WebSocketLambdaHandler() {
        this.webSocketEventHandler = new WebSocketEventHandler();
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        context.getLogger().log("Handling WebSocket event: " + event.toString());
        
        try {
            return webSocketEventHandler.handleWebSocketEvent(event, context);
        } catch (Exception e) {
            context.getLogger().log("Error handling WebSocket event: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("statusCode", 500);
            errorResponse.put("body", "{\"error\":\"Internal server error\"}");
            return errorResponse;
        }
    }
}
