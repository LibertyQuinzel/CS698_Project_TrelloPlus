import { useProjectStore, type Project } from './projectStore';

function buildProject(overrides: Partial<Project> = {}): Project {
  return {
    id: 'project-1',
    name: 'U1 Project',
    description: 'AI-generated board project',
    boardId: 'board-1',
    members: [],
    columns: [
      { id: 'col-1', title: 'Backlog', color: '#ffffff' },
      { id: 'col-2', title: 'In Progress', color: '#f3f4f6' },
    ],
    tasks: [
      {
        id: 'task-1',
        title: 'Bootstrap board',
        description: 'Initial generated task',
        priority: 'HIGH',
        createdDate: '2026-04-05T00:00:00.000Z',
        columnId: 'col-1',
      },
    ],
    decisions: [],
    ...overrides,
  };
}

describe('useProjectStore (User Story 1)', () => {
  beforeEach(() => {
    localStorage.setItem('user', JSON.stringify({ username: 'owner', email: 'owner@example.com', role: 'owner' }));
    useProjectStore.setState({ projects: [], teamMembers: [], user: { name: 'owner', email: 'owner@example.com', role: 'owner' } });
  });

  it('adds and updates a project', () => {
    useProjectStore.getState().addProject(buildProject());
    expect(useProjectStore.getState().projects).toHaveLength(1);

    useProjectStore.getState().updateProject('project-1', { name: 'Updated U1 Project' });
    expect(useProjectStore.getState().projects[0]?.name).toBe('Updated U1 Project');
  });

  it('duplicates a project with copied tasks', () => {
    useProjectStore.getState().addProject(buildProject());

    useProjectStore.getState().duplicateProject('project-1');

    const projects = useProjectStore.getState().projects;
    expect(projects).toHaveLength(2);
    expect(projects[1]?.name).toContain('(Copy)');
    expect(projects[1]?.tasks[0]?.id).toContain('-copy-');
  });

  it('manages board columns and tasks for the project', () => {
    useProjectStore.getState().addProject(buildProject());

    useProjectStore.getState().addColumnToProject('project-1', {
      id: 'col-3',
      title: 'Done',
      color: '#dcfce7',
    });
    useProjectStore.getState().renameColumn('project-1', 'col-1', 'Ideas');
    useProjectStore.getState().moveTask('project-1', 'task-1', 'col-2');

    const project = useProjectStore.getState().projects[0];
    expect(project?.columns.some((c) => c.id === 'col-3')).toBe(true);
    expect(project?.columns.find((c) => c.id === 'col-1')?.title).toBe('Ideas');
    expect(project?.tasks.find((t) => t.id === 'task-1')?.columnId).toBe('col-2');

    useProjectStore.getState().deleteColumn('project-1', 'col-2');
    const updated = useProjectStore.getState().projects[0];
    expect(updated?.columns.some((c) => c.id === 'col-2')).toBe(false);
    expect(updated?.tasks.some((t) => t.id === 'task-1')).toBe(false);
  });
});
