import { create } from 'zustand';

export interface ActionItem {
  id: string;
  description: string;
  sourceContext: string;
  comment?: string;
}

export interface Decision {
  id: string;
  description: string;
  sourceContext: string;
  comment?: string;
}

export interface Change {
  id: string;
  description: string;
  sourceContext: string;
  comment?: string;
}

export interface OtherNote {
  id: string;
  description: string;
  sourceContext?: string;
  comment?: string;
}

export interface Approval {
  userId: string;
  userName: string;
  status: 'pending' | 'approved' | 'rejected';
  timestamp?: string;
}

export interface Meeting {
  id: string;
  projectId: string;
  title: string;
  date: string;
  time: string;
  members: string[];
  agenda: string;
  platform?: string;
  link?: string;
  transcript?: string;
  status: 'scheduled' | 'in-progress' | 'pending-approval' | 'approved' | 'rejected';
  actionItems: ActionItem[];
  decisions: Decision[];
  changes: Change[];
  otherNotes: OtherNote[];
  approvals: Approval[];
  totalApprovers: number;
  userHasApproved: boolean;
  approvalComments?: string;
}

interface MeetingStore {
  meetings: Meeting[];
  setMeetings: (meetings: Meeting[]) => void;
  addMeeting: (meeting: Meeting) => void;
  updateMeeting: (id: string, updates: Partial<Meeting>) => void;
  deleteMeeting: (id: string) => void;
  updateItemDescription: (meetingId: string, itemType: 'actionItem' | 'decision' | 'change', itemId: string, newDescription: string) => void;
  approveItem: (meetingId: string, itemType: 'actionItem' | 'decision' | 'change', itemId: string, approved: boolean, comment?: string) => void;
  submitApproval: (meetingId: string, status: 'approved' | 'rejected', comments?: string) => void;
  generateSummary: (meetingId: string) => void;
  addOtherNote: (meetingId: string, description: string) => void;
  removeOtherNote: (meetingId: string, noteId: string) => void;
  processMeetingApproval: (meetingId: string) => { actionItems: ActionItem[]; decisions: Decision[]; changes: Change[] };
}

export const useMeetingStore = create<MeetingStore>((set, get) => ({
  meetings: [],

  setMeetings: (meetings) => set({ meetings }),

  addMeeting: (meeting) => set((state) => ({ meetings: [...state.meetings, meeting] })),

  updateMeeting: (id, updates) =>
    set((state) => ({
      meetings: state.meetings.map((m) => (m.id === id ? { ...m, ...updates } : m)),
    })),

  deleteMeeting: (id) =>
    set((state) => ({
      meetings: state.meetings.filter((m) => m.id !== id),
    })),

  updateItemDescription: (meetingId, itemType, itemId, newDescription) =>
    set((state) => ({
      meetings: state.meetings.map((meeting) => {
        if (meeting.id !== meetingId) return meeting;

        const updateItems = <T extends { id: string; description: string }>(items: T[]): T[] =>
          items.map((item) => (item.id === itemId ? { ...item, description: newDescription } : item));

        return {
          ...meeting,
          actionItems: itemType === 'actionItem' ? updateItems(meeting.actionItems) : meeting.actionItems,
          decisions: itemType === 'decision' ? updateItems(meeting.decisions) : meeting.decisions,
          changes: itemType === 'change' ? updateItems(meeting.changes) : meeting.changes,
        };
      }),
    })),

  approveItem: (meetingId, itemType, itemId, approved, comment) =>
    set((state) => ({
      meetings: state.meetings.map((meeting) => {
        if (meeting.id !== meetingId) return meeting;

        const updateItems = (items: any[]) =>
          items.map((item) => (item.id === itemId ? { ...item, approved, comment } : item));

        return {
          ...meeting,
          actionItems: itemType === 'actionItem' ? updateItems(meeting.actionItems) : meeting.actionItems,
          decisions: itemType === 'decision' ? updateItems(meeting.decisions) : meeting.decisions,
          changes: itemType === 'change' ? updateItems(meeting.changes) : meeting.changes,
        };
      }),
    })),

  submitApproval: (meetingId, status, comments) =>
    set((state) => ({
      meetings: state.meetings.map((meeting) => {
        if (meeting.id !== meetingId) return meeting;

        const updatedApprovals = meeting.approvals.map((approval) =>
          approval.userId === 'u1'
            ? { ...approval, status, timestamp: new Date().toISOString(), comments }
            : approval
        );

        const approvedCount = updatedApprovals.filter((a) => a.status === 'approved').length;
        const newStatus = approvedCount === meeting.totalApprovers ? 'approved' : 'pending-approval';

        return {
          ...meeting,
          approvals: updatedApprovals,
          status: newStatus,
          userHasApproved: true,
          approvalComments: comments,
        };
      }),
    })),

  processMeetingApproval: (meetingId) => {
    const meeting = get().meetings.find((m) => m.id === meetingId);
    if (!meeting || meeting.status !== 'approved') {
      return { actionItems: [], decisions: [], changes: [] };
    }

    return {
      actionItems: meeting.actionItems,
      decisions: meeting.decisions,
      changes: meeting.changes,
    };
  },

  generateSummary: (meetingId) =>
    set((state) => ({
      meetings: state.meetings.map((meeting) =>
        meeting.id === meetingId
          ? {
              ...meeting,
              status: 'pending-approval',
            }
          : meeting
      ),
    })),

  addOtherNote: (meetingId, description) =>
    set((state) => ({
      meetings: state.meetings.map((meeting) => {
        if (meeting.id !== meetingId) return meeting;

        const newNote: OtherNote = {
          id: typeof crypto !== 'undefined' && crypto.randomUUID ? crypto.randomUUID() : `${Date.now()}`,
          description,
        };

        return {
          ...meeting,
          otherNotes: [...meeting.otherNotes, newNote],
        };
      }),
    })),

  removeOtherNote: (meetingId, noteId) =>
    set((state) => ({
      meetings: state.meetings.map((meeting) => {
        if (meeting.id !== meetingId) return meeting;

        return {
          ...meeting,
          otherNotes: meeting.otherNotes.filter((note) => note.id !== noteId),
        };
      }),
    })),
}));
