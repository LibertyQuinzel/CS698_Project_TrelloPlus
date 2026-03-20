import { useEffect, useMemo, useState } from 'react';
import { useParams, useNavigate } from 'react-router';
import { type ChangeRequest } from '../store/changeStore';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { Textarea } from '../components/ui/textarea';
import { ChangeDetailModal } from '../components/ChangeDetailModal';
import { toast } from 'sonner';
import {
  ArrowLeft,
  Calendar,
  Clock,
  CheckCircle2,
  XCircle,
  AlertCircle,
  ListChecks,
} from 'lucide-react';
import {
  apiService,
  type MeetingResponse,
  type MeetingSummaryResponse,
  type ApprovalStatusResponse,
} from '../services/api';

const statusConfig: Record<string, { label: string; color: string }> = {
  SCHEDULED: { label: 'Scheduled', color: 'bg-blue-50 text-blue-700 border-blue-200' },
  IN_PROGRESS: { label: 'In Progress', color: 'bg-purple-50 text-purple-700 border-purple-200' },
  PENDING_APPROVAL: { label: 'Pending Approval', color: 'bg-yellow-50 text-yellow-700 border-yellow-200' },
  APPROVED: { label: 'Approved', color: 'bg-green-50 text-green-700 border-green-200' },
  REJECTED: { label: 'Rejected', color: 'bg-red-50 text-red-700 border-red-200' },
};

export function MeetingSummary() {
  const { meetingId } = useParams<{ meetingId: string }>();
  const navigate = useNavigate();

  const [meeting, setMeeting] = useState<MeetingResponse | null>(null);
  const [summary, setSummary] = useState<MeetingSummaryResponse | null>(null);
  const [approval, setApproval] = useState<ApprovalStatusResponse | null>(null);
  const [selectedChange, setSelectedChange] = useState<ChangeRequest | null>(null);
  const [comments, setComments] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isGeneratingSummary, setIsGeneratingSummary] = useState(false);
  const [approvingItemId, setApprovingItemId] = useState<string | null>(null);

  const reloadSummaryAndApproval = async (id: string) => {
    const [summaryData, approvalData] = await Promise.all([
      apiService.getSummaryByMeeting(id),
      apiService.getApprovalStatus(id),
    ]);
    setSummary(summaryData);
    setApproval(approvalData);
  };

  useEffect(() => {
    if (!meetingId) return;

    let isMounted = true;

    const loadData = async () => {
      try {
        const [meetingData, summaryData, approvalData] = await Promise.all([
          apiService.getMeeting(meetingId),
          apiService.getSummaryByMeeting(meetingId),
          apiService.getApprovalStatus(meetingId),
        ]);

        if (!isMounted) return;
        setMeeting(meetingData);
        setSummary(summaryData);
        setApproval(approvalData);
      } catch (error) {
        if (isMounted) {
          toast.error(error instanceof Error ? error.message : 'Failed to load meeting summary');
        }
      }
    };

    void loadData();

    return () => {
      isMounted = false;
    };
  }, [meetingId]);

  const changeRequests = useMemo<ChangeRequest[]>(() => {
    if (!summary || !meeting) return [];

    return (summary.changes || []).map((c) => {
      let before: any;
      let after: any;

      try {
        before = c.beforeState ? JSON.parse(c.beforeState) : undefined;
      } catch {
        before = undefined;
      }

      try {
        after = c.afterState ? JSON.parse(c.afterState) : undefined;
      } catch {
        after = undefined;
      }

      return {
        id: c.id,
        meetingId: c.meetingId,
        meetingTitle: meeting.title,
        type: c.changeType as ChangeRequest['type'],
        status: c.status as ChangeRequest['status'],
        requestedBy: 'system',
        requestedAt: c.createdAt,
        projectId: meeting.projectId,
        before,
        after,
        affectedCards: [],
        affectedStages: [],
        affectedMembers: [],
        riskLevel: 'LOW',
        approvals: [],
        requiredApprovals: 0,
        rollbackAvailable: false,
      };
    });
  }, [summary, meeting]);

  if (!meeting) {
    return (
      <div className="p-8 pt-24 text-center">
        <h2 className="text-xl font-semibold text-gray-900 mb-2">Meeting not found</h2>
        <Button onClick={() => navigate('/meetings')}>Back to Meetings</Button>
      </div>
    );
  }

  const statusInfo = statusConfig[meeting.status] || statusConfig.SCHEDULED;

  const submitDecision = async (decision: 'APPROVED' | 'REJECTED') => {
    if (!meetingId) return;

    setIsSubmitting(true);
    try {
      await apiService.submitSummaryApproval(meetingId, decision, comments || undefined);
      toast.success(decision === 'APPROVED' ? 'Summary approved' : 'Changes requested');

      const [meetingData, approvalData] = await Promise.all([
        apiService.getMeeting(meetingId),
        apiService.getApprovalStatus(meetingId),
      ]);
      setMeeting(meetingData);
      setApproval(approvalData);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to submit approval');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleGenerateSummary = async () => {
    if (!meetingId) return;

    setIsGeneratingSummary(true);
    try {
      const summaryData = await apiService.generateSummary(meetingId);
      setSummary(summaryData);

      const [meetingData, approvalData] = await Promise.all([
        apiService.getMeeting(meetingId),
        apiService.getApprovalStatus(meetingId),
      ]);
      setMeeting(meetingData);
      setApproval(approvalData);
      toast.success('Summary and approval items generated');
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to generate summary');
    } finally {
      setIsGeneratingSummary(false);
    }
  };

  const approveItem = async (itemId: string, itemType: 'action' | 'decision') => {
    if (!meetingId) return;

    setApprovingItemId(itemId);
    try {
      if (itemType === 'action') {
        await apiService.approveActionItem(itemId);
      } else {
        await apiService.approveDecisionItem(itemId);
      }

      await reloadSummaryAndApproval(meetingId);
      toast.success('Item approved');
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to approve item');
    } finally {
      setApprovingItemId(null);
    }
  };

  return (
    <div className="p-4 md:p-8 pt-20 md:pt-24 min-h-screen bg-gray-50">
      <div className="max-w-6xl mx-auto space-y-4">
        <Button variant="ghost" className="-ml-2" onClick={() => navigate(`/project/${meeting.projectId}?tab=meetings`)}>
          <ArrowLeft className="w-4 h-4 mr-2" />
          Back to Meetings
        </Button>

        <div className="bg-white rounded-xl border border-gray-200 p-6">
          <div className="flex items-start justify-between gap-3">
            <div>
              <h1 className="text-2xl font-bold text-gray-900">{meeting.title}</h1>
              <div className="flex items-center gap-4 text-sm text-gray-600 mt-2">
                <div className="flex items-center gap-1"><Calendar className="w-4 h-4" />{new Date(meeting.meetingDate).toLocaleDateString()}</div>
                <div className="flex items-center gap-1"><Clock className="w-4 h-4" />{(meeting.meetingTime || '').slice(0, 5)}</div>
              </div>
            </div>
            <Badge variant="outline" className={statusInfo.color}>{statusInfo.label}</Badge>
          </div>
        </div>

        <div className="grid lg:grid-cols-2 gap-4">
          <div className="bg-white rounded-xl border border-gray-200 p-5">
            <h2 className="font-semibold text-gray-900 mb-3">Decisions</h2>
            <ul className="space-y-2 text-sm text-gray-700">
              {(summary?.decisions || []).map((d) => (
                <li key={d.id} className="border rounded-lg p-3 flex items-start justify-between gap-3">
                  <div>
                    <p>{d.description}</p>
                    <p className="text-xs text-gray-500 mt-1">Approval: {d.approvalStatus || 'PENDING'}</p>
                  </div>
                  <Button
                    size="sm"
                    variant="outline"
                    disabled={(d.approvalStatus || 'PENDING') === 'APPROVED' || approvingItemId === d.id}
                    onClick={() => approveItem(d.id, 'decision')}
                  >
                    {(d.approvalStatus || 'PENDING') === 'APPROVED' ? 'Approved' : 'Approve'}
                  </Button>
                </li>
              ))}
              {(summary?.decisions || []).length === 0 && <li className="text-gray-500">No decisions yet</li>}
            </ul>
          </div>

          <div className="bg-white rounded-xl border border-gray-200 p-5">
            <h2 className="font-semibold text-gray-900 mb-3">Action Items</h2>
            <ul className="space-y-2 text-sm text-gray-700">
              {(summary?.actionItems || []).map((a) => (
                <li key={a.id} className="border rounded-lg p-3 flex items-start justify-between gap-3">
                  <div>
                    <p>{a.description}</p>
                    <p className="text-xs text-gray-500 mt-1">Approval: {a.approvalStatus || 'PENDING'}</p>
                  </div>
                  <Button
                    size="sm"
                    variant="outline"
                    disabled={(a.approvalStatus || 'PENDING') === 'APPROVED' || approvingItemId === a.id}
                    onClick={() => approveItem(a.id, 'action')}
                  >
                    {(a.approvalStatus || 'PENDING') === 'APPROVED' ? 'Approved' : 'Approve'}
                  </Button>
                </li>
              ))}
              {(summary?.actionItems || []).length === 0 && <li className="text-gray-500">No action items yet</li>}
            </ul>
          </div>
        </div>

        <div className="bg-white rounded-xl border border-gray-200 p-5">
          <div className="flex items-start justify-between gap-3">
            <div>
              <h2 className="font-semibold text-gray-900">AI Summary</h2>
              <p className="text-sm text-gray-600 mt-1">Generate or refresh mocked summary and approval items from transcript.</p>
            </div>
            <Button onClick={handleGenerateSummary} disabled={isGeneratingSummary}>
              {isGeneratingSummary ? 'Generating...' : 'Generate Summary and Approval Items'}
            </Button>
          </div>
          <div className="mt-4 rounded-lg border border-gray-200 p-4 bg-gray-50 text-sm whitespace-pre-wrap">
            {summary?.aiGeneratedContent || 'No summary generated yet.'}
          </div>
        </div>

        <div className="bg-white rounded-xl border border-gray-200 p-5">
          <div className="flex items-center justify-between mb-3">
            <h2 className="font-semibold text-gray-900">Changes</h2>
            <Button size="sm" variant="outline" onClick={() => navigate(`/meetings/${meetingId}/changes`)}>
              <ListChecks className="w-4 h-4 mr-2" />
              Review Changes
            </Button>
          </div>
          <div className="space-y-2 text-sm">
            {changeRequests.map((c) => (
              <button
                key={c.id}
                type="button"
                className="w-full text-left border rounded-lg p-3 hover:bg-gray-50"
                onClick={() => setSelectedChange(c)}
              >
                {c.type.replace(/_/g, ' ')}
              </button>
            ))}
            {changeRequests.length === 0 && <p className="text-gray-500">No changes yet</p>}
          </div>
        </div>

        <div className="bg-white rounded-xl border border-gray-200 p-5">
          <h2 className="font-semibold text-gray-900 mb-3">Approvals</h2>
          <div className="flex flex-wrap gap-2 mb-4">
            {(approval?.responses || []).map((r) => (
              <div key={r.userId} className="px-3 py-2 rounded-lg border bg-gray-50 text-sm">
                {r.userName}: {r.response}
              </div>
            ))}
            {(approval?.responses || []).length === 0 && <p className="text-gray-500 text-sm">No approvals submitted yet</p>}
          </div>

          <Textarea
            value={comments}
            onChange={(e) => setComments(e.target.value)}
            placeholder="Optional comments"
            className="mb-3"
          />

          <div className="flex gap-3">
            <Button
              variant="outline"
              className="flex-1 border-red-300 text-red-700"
              onClick={() => submitDecision('REJECTED')}
              disabled={isSubmitting}
            >
              <XCircle className="w-4 h-4 mr-2" /> Request Changes
            </Button>
            <Button
              className="flex-1 bg-green-600 hover:bg-green-700"
              onClick={() => submitDecision('APPROVED')}
              disabled={isSubmitting}
            >
              <CheckCircle2 className="w-4 h-4 mr-2" /> Approve Summary
            </Button>
          </div>
        </div>
      </div>

      <ChangeDetailModal change={selectedChange} open={!!selectedChange} onClose={() => setSelectedChange(null)} />
    </div>
  );
}
