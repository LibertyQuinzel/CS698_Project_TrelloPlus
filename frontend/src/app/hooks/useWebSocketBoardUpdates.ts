import { useEffect, useRef } from 'react';
import { useProjectStore } from '../store/projectStore';
import { ENABLE_REALTIME, WS_ENDPOINT } from '../services/runtimeConfig';

export const useWebSocketBoardUpdates = (boardId: string | null) => {
  const socketRef = useRef<WebSocket | null>(null);

  const {
    updateCardFromRealTime,
    deleteCardFromRealTime,
    addCardToBoard,
    deleteStageFromBoard,
    updateStageFromRealTime,
    addStageToBoard,
    updateProject,
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
        const cardData = message.cardData ?? message.data;
        console.log('[WS] Received Message:', message);
        // Route the message to your store
        switch (message.type) {
          case 'CARD_CREATED': 
  // Strategy: Add the stageId to the card object so the store has the full context
  const cardWithStage = { 
    ...message.data, 
    stageId: message.stageId 
  };
  addCardToBoard(cardWithStage); 
  break;
          case 'CARD_UPDATED': updateCardFromRealTime(message.data); break;
          case 'CARD_DELETED': deleteCardFromRealTime(message.stageId, message.cardId); break;
          case 'CARD_MOVED': 
    updateCardFromRealTime({ 
      ...cardData, 
      toStageId: message.toStageId 
    }); 
    break;
          case 'STAGE_CREATED': addStageToBoard(message.data); break;
          case 'STAGE_UPDATED': updateStageFromRealTime(message.data); break;
          case 'STAGE_DELETED': deleteStageFromBoard(message.stageId); break;
          case 'PROJECT_UPDATED': {
            const projectData = message.projectData;
            if (!projectData?.id) break;

            updateProject(projectData.id, {
              name: projectData.name,
              description: projectData.description,
              boardId: projectData.boardId,
              members: Array.isArray(projectData.members)
                ? projectData.members.map((member: any) => ({
                    id: member.id,
                    name: member.fullName || member.username || member.email || 'Unknown User',
                    email: member.email,
                    role: member.role === 'owner' || member.role === 'viewer' ? member.role : 'editor',
                  }))
                : [],
              columns: Array.isArray(projectData.columns)
                ? projectData.columns.map((column: any) => ({
                    id: column.id,
                    title: column.title,
                    color: column.color,
                  }))
                : [],
              tasks: Array.isArray(projectData.tasks)
                ? projectData.tasks.map((task: any) => ({
                    id: task.id,
                    title: task.title,
                    description: task.description || '',
                    assignee: task.assignee
                      ? {
                          id: task.assignee.id,
                          name: task.assignee.fullName || task.assignee.username || task.assignee.email || 'Unknown User',
                        }
                      : undefined,
                    priority: task.priority || 'MEDIUM',
                    createdDate: task.createdAt,
                    columnId: task.stageId,
                    position: task.position,
                  }))
                : [],
            });
            break;
          }
        }
      } catch (err) {
        console.error('[WS] Error parsing message', err);
      }
    };

    return () => socket.close();
  }, [boardId, addCardToBoard, addStageToBoard, deleteCardFromRealTime, deleteStageFromBoard, updateCardFromRealTime, updateProject, updateStageFromRealTime]);

  return socketRef.current;
};