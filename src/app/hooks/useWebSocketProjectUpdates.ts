import { useEffect, useRef } from 'react';
import { ENABLE_REALTIME, WS_ENDPOINT } from '../services/runtimeConfig';

export const useWebSocketProjectUpdates = (onProjectsChanged: () => void, projectIds?: string[]) => {
  const socketRef = useRef<WebSocket | null>(null);

  useEffect(() => {
    if (!ENABLE_REALTIME) return;

    // Connect using Native WebSocket
    const socket = new WebSocket(WS_ENDPOINT);
    socketRef.current = socket;

    socket.onopen = () => {
      console.log('[WS-Projects] Connected to API Gateway');
      // Subscribe to workspace events
      socket.send(JSON.stringify({ 
        action: 'subscribe_workspace', 
        projectIds: projectIds 
      }));
    };

    socket.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data);
        
        // Trigger the callback if a project change event arrives
        if (['PROJECT_CREATED', 'PROJECT_DELETED', 'PROJECT_UPDATED'].includes(message.type)) {
          onProjectsChanged();
        }
      } catch (err) {
        console.error('[WS-Projects] Error parsing message', err);
      }
    };

    return () => socket.close();
  }, [onProjectsChanged, projectIds]);

  return socketRef.current;
};