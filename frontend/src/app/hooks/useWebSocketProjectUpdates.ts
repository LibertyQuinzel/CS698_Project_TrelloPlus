import { useEffect, useRef } from 'react';
import { ENABLE_REALTIME, WS_ENDPOINT } from '../services/runtimeConfig';

/**
 * Hook to listen for real-time project updates and trigger a refetch callback.
 * @param onProjectsChanged - Function to call when a project event is received.
 * @param workspaceId - The ID of the current workspace to subscribe to.
 */
export const useWebSocketProjectUpdates = (onProjectsChanged: () => void, workspaceId?: string) => {
  const socketRef = useRef<WebSocket | null>(null);

  // 1. Manage the WebSocket lifecycle
  useEffect(() => {
    if (!ENABLE_REALTIME || !workspaceId) return;

    const socket = new WebSocket(WS_ENDPOINT);
    socketRef.current = socket;

    socket.onopen = () => {
      console.log('[WS-Projects] Connected to API Gateway');
      // Subscribe to the global workspace channel
      socket.send(JSON.stringify({ 
        action: 'subscribe',
        boardId: 'all',
      }));
    };

    socket.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data);
        
        // Trigger the callback if a project change event arrives
        if (['PROJECT_CREATED', 'PROJECT_DELETED', 'PROJECT_UPDATED'].includes(message.type)) {
          console.log(`[WS-Projects] ${message.type} received. Triggering re-fetch.`);
          onProjectsChanged();
        }
      } catch (err) {
        console.error('[WS-Projects] Error parsing message', err);
      }
    };

    socket.onerror = (err) => console.error('[WS-Projects] Socket error:', err);

    return () => {
      if (socket.readyState === WebSocket.OPEN) {
        socket.close();
      }
    };
  }, [onProjectsChanged, workspaceId]);

  return socketRef.current;
};