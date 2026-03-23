import { useState } from 'react';
import { useNavigate } from 'react-router';
import { MoreVertical } from 'lucide-react';
import { Button } from './ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from './ui/dropdown-menu';
import { useProjectStore, type Project } from '../store/projectStore';
import { EditProjectModal } from './EditProjectModal';
import { toast } from 'sonner';
import { apiService, mapProjectResponseToProject } from '../services/api';

interface ProjectCardProps {
  project: Project;
}

export function ProjectCard({ project }: ProjectCardProps) {
  const navigate = useNavigate();
  const { deleteProject, addProject } = useProjectStore();
  const [showEdit, setShowEdit] = useState(false);

  const handleDuplicate = async () => {
    try {
      const duplicated = await apiService.createProject({
        name: `${project.name} (Copy)`,
        description: project.description,
      });
      addProject(mapProjectResponseToProject(duplicated));
      toast.success(`"${project.name}" duplicated`);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to duplicate project');
    }
  };

  const handleDelete = async () => {
    if (window.confirm(`Delete "${project.name}"? This cannot be undone.`)) {
      try {
        await apiService.deleteProject(project.id);
        deleteProject(project.id);
        toast.success(`"${project.name}" deleted`);
      } catch (error) {
        toast.error(error instanceof Error ? error.message : 'Failed to delete project');
      }
    }
  };

  return (
    <>
      <div
        className="bg-white rounded-xl p-6 border border-gray-200 hover:shadow-lg transition-all cursor-pointer group"
        onClick={() => navigate(`/project/${project.id}`)}
      >
        <div className="flex items-start justify-between mb-3">
          <h3 className="text-lg font-semibold text-gray-900 group-hover:text-blue-600 transition-colors">
            {project.name}
          </h3>
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <div onClick={(e) => e.stopPropagation()}>
                <Button 
                  variant="ghost" 
                  size="icon" 
                  className="h-8 w-8 opacity-0 group-hover:opacity-100 transition-opacity"
                  aria-label={`Menu for project ${project.name}`}
                  aria-haspopup="menu"
                >
                  <MoreVertical className="h-4 w-4" />
                </Button>
              </div>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem onClick={(e) => { e.stopPropagation(); setShowEdit(true); }}>
                Edit
              </DropdownMenuItem>
              <DropdownMenuItem onClick={(e) => { e.stopPropagation(); void handleDuplicate(); }}>
                Duplicate
              </DropdownMenuItem>
              <DropdownMenuItem className="text-red-600" onClick={(e) => { e.stopPropagation(); void handleDelete(); }}>
                Delete
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>

        <p className="text-gray-600 text-sm line-clamp-2">{project.description}</p>
      </div>

      {showEdit && (
        <EditProjectModal project={project} onClose={() => setShowEdit(false)} />
      )}
    </>
  );
}