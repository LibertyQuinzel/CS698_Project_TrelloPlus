import { useEffect, useRef } from 'react';
import { useProjectStore } from '../store/projectStore';
import { ENABLE_REALTIME, WS_ENDPOINT } from '../services/runtimeConfig';

export const useWebSocketBoardUpdates = (boardId: string | null, projectId: string | null = null) => {
  const socketRef = useRef<WebSocket | null>(null);

  const {
    updateCardFromRealTime,
    deleteCardFromRealTime,
    addCardToBoard,
    deleteStageFromBoard,
    updateStageFromRealTime,
    addStageToBoard,
    addTeamMemberToProject,
    removeTeamMemberFromProject,
    updateTeamMemberRole,
  } = useProjectStore();

  useEffect(() => {
    if (!boardId || !ENABLE_REALTIME) return;

    // Connect using Native WebSocket
    const socket = new WebSocket(WS_ENDPOINT);
    socketRef.current = socket;

    socket.onopen = () => {
      console.log('[WS] Connected to API Gateway');
      // Subscribe to the board immediately after opening
      socket.send(JSON.stringify({ 
        action: 'subscribe', 
        boardId: boardId 
      }));
    };

    socket.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data);
        
        // Route the message to your store
        switch (message.type) {
          case 'CARD_CREATED': addCardToBoard(message.data); break;
          case 'CARD_UPDATED': updateCardFromRealTime(message.data); break;
          case 'CARD_DELETED': deleteCardFromRealTime(message.stageId, message.cardId); break;
          // Add your other cases here...
        }
      } catch (err) {
        console.error('[WS] Error parsing message', err);
      }
    };

    return () => socket.close();
  }, [boardId]);

  return socketRef.current;
};