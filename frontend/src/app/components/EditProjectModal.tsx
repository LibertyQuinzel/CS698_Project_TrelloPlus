import { useEffect, useState } from 'react';
import { UserPlus, Trash2, Check, Pencil } from 'lucide-react';
import { Dialog, DialogContent, DialogTitle, DialogDescription } from './ui/dialog';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { Label } from './ui/label';
import { Avatar, AvatarFallback } from './ui/avatar';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from './ui/select';
import { Badge } from './ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from './ui/tabs';
import { type Project, type ProjectMember, useProjectStore } from '../store/projectStore';
import { toast } from 'sonner';
import { apiService } from '../services/api';

interface EditProjectModalProps {
  project: Project;
  onClose: () => void;
}

export function EditProjectModal({ project, onClose }: EditProjectModalProps) {
  const { updateProject, addMemberToProject, removeMemberFromProject, renameColumn } = useProjectStore();
  const [projectName, setProjectName] = useState(project.name);
  const [projectDescription, setProjectDescription] = useState(project.description);
  const [newMemberName, setNewMemberName] = useState('');
  const [newMemberEmail, setNewMemberEmail] = useState('');
  const [newMemberRole, setNewMemberRole] = useState<ProjectMember['role']>('viewer');
  const [editingColumnId, setEditingColumnId] = useState<string | null>(null);
  const [editingColumnTitle, setEditingColumnTitle] = useState('');
  const [isLoadingMembers, setIsLoadingMembers] = useState(false);
  const [membersLoadFailed, setMembersLoadFailed] = useState(false);
  const [isSavingGeneral, setIsSavingGeneral] = useState(false);
  const [isAddingMember, setIsAddingMember] = useState(false);
  const [activeMemberMutationId, setActiveMemberMutationId] = useState<string | null>(null);
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  const currentUserId = (() => {
    try {
      const rawUser = localStorage.getItem('user');
      return rawUser ? JSON.parse(rawUser)?.id ?? null : null;
    } catch {
      return null;
    }
  })();
  const ownerPermissionMessage = 'Only the project owner can manage members.';

  const normalizeMembers = (backendMembers: Array<{ id: string; email: string; username?: string; fullName?: string; role?: string }>): ProjectMember[] => {
    return backendMembers.map((member) => {
      const backendRole = String(member.role || 'viewer').toLowerCase();
      const normalizedRole: ProjectMember['role'] =
        backendRole === 'owner' ? 'owner' : backendRole === 'viewer' ? 'viewer' : 'editor';

      return {
        id: member.id,
        name: member.fullName || member.username || member.email,
        email: member.email,
        role: normalizedRole,
      };
    });
  };

  useEffect(() => {
    let isMounted = true;

    const loadMembers = async () => {
      setIsLoadingMembers(true);
      setMembersLoadFailed(false);

      try {
        const backendMembers = await apiService.getProjectMembers(project.id);
        if (!isMounted) return;
        updateProject(project.id, { members: normalizeMembers(backendMembers) });
      } catch {
        try {
          const latestProject = await apiService.getProject(project.id);
          if (!isMounted) return;
          updateProject(project.id, { members: normalizeMembers(latestProject.members || []) });
        } catch {
          if (!isMounted) return;
          setMembersLoadFailed(true);
          toast.error('Failed to refresh project members. Please reopen this dialog and try again.');
        }
      } finally {
        if (isMounted) {
          setIsLoadingMembers(false);
        }
      }
    };

    void loadMembers();

    return () => {
      isMounted = false;
    };
  }, [project.id, updateProject]);

  const handleSaveGeneral = async () => {
    if (!projectName.trim()) {
      toast.error('Project name is required');
      return;
    }
    if (projectName.trim().length > 255) {
      toast.error('Project name must be 255 characters or fewer');
      return;
    }
    if (projectDescription.trim().length > 5000) {
      toast.error('Project description must be 5000 characters or fewer');
      return;
    }

    setIsSavingGeneral(true);
    try {
      const updated = await apiService.updateProject(project.id, {
        name: projectName,
        description: projectDescription,
      });
      updateProject(project.id, {
        name: updated.name,
        description: updated.description,
      });
      toast.success('Project updated successfully');
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to update project');
    } finally {
      setIsSavingGeneral(false);
    }
  };

  const handleAddMember = async () => {
    if (!canManageMembers) {
      toast.error(ownerPermissionMessage);
      return;
    }

    if (!newMemberEmail.trim()) {
      toast.error('Please enter an email address');
      return;
    }
    if (!emailRegex.test(newMemberEmail.trim())) {
      toast.error('Please enter a valid email address');
      return;
    }
    if (currentProject.members.some((member) => member.email.toLowerCase() === newMemberEmail.trim().toLowerCase())) {
      toast.error('This user is already a member of the project');
      return;
    }

    setIsAddingMember(true);
    try {
      const createdMember = await apiService.addTeamMember(project.id, newMemberEmail.trim(), newMemberName.trim(), newMemberRole);
      const resolvedName = createdMember.fullName || createdMember.username || newMemberName.trim() || createdMember.email;
      const newMember: ProjectMember = {
        id: createdMember.id,
        name: resolvedName,
        email: createdMember.email,
        role: createdMember.role || newMemberRole,
      };
      addMemberToProject(project.id, newMember);
      setNewMemberName('');
      setNewMemberEmail('');
      setNewMemberRole('viewer');
      toast.success(`${newMember.name} added to project`);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to add member');
    } finally {
      setIsAddingMember(false);
    }
  };

  const handleRemoveMember = async (memberId: string, memberName: string) => {
    if (!canManageMembers) {
      toast.error(ownerPermissionMessage);
      return;
    }

    setActiveMemberMutationId(memberId);
    try {
      await apiService.removeTeamMember(project.id, memberId);
      removeMemberFromProject(project.id, memberId);
      toast.success(`${memberName} removed from project`);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to remove member');
    } finally {
      setActiveMemberMutationId(null);
    }
  };

  const handleUpdateMemberRole = async (memberId: string, memberName: string, role: 'editor' | 'viewer') => {
    if (!canManageMembers) {
      toast.error(ownerPermissionMessage);
      return;
    }

    setActiveMemberMutationId(memberId);
    try {
      const updatedMember = await apiService.updateTeamMemberRole(project.id, memberId, role);
      updateProject(project.id, {
        members: currentProject.members.map((member) =>
          member.id === memberId
            ? { ...member, role: updatedMember.role === 'viewer' ? 'viewer' : 'editor' }
            : member,
        ),
      });
      toast.success(`${memberName} is now ${role}`);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to update member role');
    } finally {
      setActiveMemberMutationId(null);
    }
  };

  const handleStartEditColumn = (columnId: string, currentTitle: string) => {
    setEditingColumnId(columnId);
    setEditingColumnTitle(currentTitle);
  };

  const handleSaveColumnName = async () => {
    if (editingColumnId && editingColumnTitle.trim()) {
      try {
        await apiService.renameStage(editingColumnId, { title: editingColumnTitle.trim() });
        renameColumn(project.id, editingColumnId, editingColumnTitle.trim());
        toast.success('Column renamed');
      } catch (error) {
        toast.error(error instanceof Error ? error.message : 'Failed to rename column');
      }
    }
    setEditingColumnId(null);
    setEditingColumnTitle('');
  };

  // Get fresh project data from store
  const currentProject = useProjectStore((s) => s.projects.find((p) => p.id === project.id));
  if (!currentProject) return null;
  const canManageMembers = currentUserId != null
    && currentProject.members.some((member) => member.id === currentUserId && member.role === 'owner');

  return (
    <Dialog open={true} onOpenChange={onClose}>
      <DialogContent className="max-w-full md:max-w-2xl max-h-[85vh] overflow-y-auto p-0 mx-4" aria-describedby="edit-project-description">
        <DialogTitle className="sr-only">Edit Project - {project.name}</DialogTitle>
        <DialogDescription className="sr-only" id="edit-project-description">
          Edit project details, manage team members, and configure columns
        </DialogDescription>
        
        {/* Header */}
        <div className="sticky top-0 bg-white border-b border-gray-200 px-4 md:px-6 py-3 md:py-4 z-10">
          <h2 className="text-lg md:text-xl font-semibold text-gray-900">Edit Project</h2>
        </div>

        <div className="px-4 md:px-6 pb-6">
          <Tabs defaultValue="general" className="mt-4">
            <TabsList className="grid w-full grid-cols-3">
              <TabsTrigger value="general">General</TabsTrigger>
              <TabsTrigger value="members">Members</TabsTrigger>
              <TabsTrigger value="columns">Columns</TabsTrigger>
            </TabsList>

            {/* General Tab */}
            <TabsContent value="general" className="space-y-4 mt-4">
              <div>
                <Label htmlFor="edit-name">Project Name</Label>
                <Input
                  id="edit-name"
                  value={projectName}
                  onChange={(e) => setProjectName(e.target.value)}
                  className="mt-2"
                />
              </div>
              <div>
                <Label htmlFor="edit-desc">Description</Label>
                <Input
                  id="edit-desc"
                  value={projectDescription}
                  onChange={(e) => setProjectDescription(e.target.value)}
                  className="mt-2"
                />
              </div>
              <Button onClick={() => void handleSaveGeneral()} className="w-full" disabled={isSavingGeneral}>
                Save Changes
              </Button>
            </TabsContent>

            {/* Members Tab */}
            <TabsContent value="members" className="space-y-4 mt-4">
              {/* Current Members */}
              <div>
                <Label className="text-sm mb-3 block">Current Members ({isLoadingMembers ? '...' : currentProject.members.length})</Label>
                {membersLoadFailed && (
                  <p className="text-xs text-red-600 mb-2">
                    Could not load live members from server. Showing cached data.
                  </p>
                )}
                <div className="space-y-2 max-h-[200px] overflow-y-auto">
                  {currentProject.members.map((member) => (
                    <div key={member.id} className="flex items-center justify-between p-2 rounded-lg hover:bg-gray-50">
                      <div className="flex items-center gap-2">
                        <Avatar className="w-8 h-8">
                          <AvatarFallback>{member.name.split(' ').map((n) => n[0]).join('')}</AvatarFallback>
                        </Avatar>
                        <div className="flex-1">
                          <p className="text-sm font-medium text-gray-900">{member.name}</p>
                          <p className="text-xs text-gray-500">{member.email}</p>
                        </div>
                      </div>
                      <div className="flex items-center gap-2">
                        {member.role === 'owner' || member.id === currentUserId || !canManageMembers ? (
                          <Badge variant="outline" className="text-xs">
                            {member.role}
                          </Badge>
                        ) : (
                          <Select
                            value={member.role}
                            onValueChange={(value: 'editor' | 'viewer') => void handleUpdateMemberRole(member.id, member.name, value)}
                            disabled={activeMemberMutationId === member.id}
                          >
                            <SelectTrigger className="h-8 w-28 text-xs">
                              <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                              <SelectItem value="editor">editor</SelectItem>
                              <SelectItem value="viewer">viewer</SelectItem>
                            </SelectContent>
                          </Select>
                        )}
                        {member.role !== 'owner' && canManageMembers && (
                          <Button
                            variant="ghost"
                            size="icon"
                            className="h-7 w-7 text-red-500 hover:text-red-700"
                            onClick={() => void handleRemoveMember(member.id, member.name)}
                            aria-label={`Remove ${member.name} from project`}
                            disabled={activeMemberMutationId === member.id}
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </Button>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* Add New Member */}
              <div className="border-t pt-4">
                <Label className="text-sm mb-3 block">Add New Member</Label>
                {!canManageMembers && (
                  <p className="text-xs text-amber-700 mb-2">
                    {ownerPermissionMessage}
                  </p>
                )}
                <div className="space-y-3">
                  <Input
                    placeholder="Name (optional)"
                    value={newMemberName}
                    onChange={(e) => setNewMemberName(e.target.value)}
                    disabled={!canManageMembers}
                  />
                  <Input
                    placeholder="Email"
                    type="email"
                    value={newMemberEmail}
                    onChange={(e) => setNewMemberEmail(e.target.value)}
                    disabled={!canManageMembers}
                  />
                  <Select value={newMemberRole} onValueChange={(v: ProjectMember['role']) => setNewMemberRole(v)} disabled={!canManageMembers}>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="editor">Editor</SelectItem>
                      <SelectItem value="viewer">Viewer</SelectItem>
                    </SelectContent>
                  </Select>
                  <Button onClick={() => void handleAddMember()} className="w-full" disabled={!canManageMembers || isAddingMember}>
                    <UserPlus className="w-4 h-4 mr-2" />
                    {isAddingMember ? 'Adding...' : 'Add Member'}
                  </Button>
                </div>
              </div>
            </TabsContent>

            {/* Columns Tab */}
            <TabsContent value="columns" className="space-y-3 mt-4">
              <Label className="text-sm mb-3 block">Board Columns</Label>
              {currentProject.columns.map((column) => (
                <div key={column.id} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                  {editingColumnId === column.id ? (
                    <div className="flex items-center gap-2 flex-1">
                      <Input
                        value={editingColumnTitle}
                        onChange={(e) => setEditingColumnTitle(e.target.value)}
                        className="h-8"
                        autoFocus
                        onKeyDown={(e) => e.key === 'Enter' && void handleSaveColumnName()}
                      />
                      <Button 
                        size="icon" 
                        className="h-8 w-8" 
                        onClick={() => void handleSaveColumnName()}
                        aria-label="Save column name"
                      >
                        <Check className="w-4 h-4" />
                      </Button>
                    </div>
                  ) : (
                    <>
                      <div className="flex items-center gap-2">
                        <div className={`w-3 h-3 rounded-full ${column.color}`}></div>
                        <span className="text-sm font-medium">{column.title}</span>
                      </div>
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-7 w-7"
                        onClick={() => handleStartEditColumn(column.id, column.title)}
                        aria-label={`Edit column: ${column.title}`}
                      >
                        <Pencil className="w-3.5 h-3.5" />
                      </Button>
                    </>
                  )}
                </div>
              ))}
            </TabsContent>
          </Tabs>
        </div>
      </DialogContent>
    </Dialog>
  );
}