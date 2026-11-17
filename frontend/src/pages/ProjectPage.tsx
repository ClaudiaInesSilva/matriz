import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { ApiError, apiClient, type Project, type Task, type TaskPriority, type TaskStatus } from '../api/apiClient'
import { Feedback } from '../components/Feedback'
import { apiErrorMessage, isUnauthorized } from '../utils/apiMessages'
import { priorityLabels, statusLabels, taskPriorities, taskStatuses } from '../utils/labels'

const visibleTaskPageSize = 10

export function ProjectPage({
  projectId,
  userId,
  revision,
  onDataChanged,
  onUnauthorized,
}: {
  projectId: string
  userId: string
  revision: number
  onDataChanged: () => void
  onUnauthorized: () => void
}) {
  const [project, setProject] = useState<Project | null>(null)
  const [tasks, setTasks] = useState<Task[]>([])
  const [statusFilter, setStatusFilter] = useState<TaskStatus | ''>('')
  const [priorityFilter, setPriorityFilter] = useState<TaskPriority | ''>('')
  const [taskSearch, setTaskSearch] = useState('')
  const [taskPage, setTaskPage] = useState(0)
  const [loadingProject, setLoadingProject] = useState(true)
  const [loadingTasks, setLoadingTasks] = useState(true)
  const [deletingProject, setDeletingProject] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  const [isEditingProject, setIsEditingProject] = useState(false)
  const [projectName, setProjectName] = useState('')
  const [projectDescription, setProjectDescription] = useState('')
  const [savingProject, setSavingProject] = useState(false)
  const [projectFormError, setProjectFormError] = useState<string | null>(null)

  const [showTaskForm, setShowTaskForm] = useState(false)
  const [taskTitle, setTaskTitle] = useState('')
  const [taskDescription, setTaskDescription] = useState('')
  const [creatingTask, setCreatingTask] = useState(false)
  const [taskFormError, setTaskFormError] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    // oxlint-disable-next-line react/set-state-in-effect
    setLoadingProject(true)
    setError(null)

    async function load() {
      try {
        const projectResponse = await apiClient.getProject(projectId)
        if (!active) return
        setProject(projectResponse)
        setProjectName(projectResponse.name)
        setProjectDescription(projectResponse.description ?? '')
      } catch (loadError) {
        if (!active) return
        if (isUnauthorized(loadError)) onUnauthorized()
        setError(apiErrorMessage(loadError, 'Não foi possível carregar o projeto.'))
      } finally {
        if (active) setLoadingProject(false)
      }
    }

    void load()
    return () => {
      active = false
    }
  }, [projectId, onUnauthorized])

  useEffect(() => {
    let active = true
    // oxlint-disable-next-line react/set-state-in-effect
    setLoadingTasks(true)
    setError(null)

    async function loadTasks() {
      try {
        const taskResponses = await apiClient.listAllTasks(projectId, {
          status: statusFilter || undefined,
          priority: priorityFilter || undefined,
        })
        if (active) setTasks(taskResponses)
      } catch (loadError) {
        if (!active) return
        if (isUnauthorized(loadError)) onUnauthorized()
        setError(apiErrorMessage(loadError, 'Não foi possível carregar as tarefas.'))
      } finally {
        if (active) setLoadingTasks(false)
      }
    }

    void loadTasks()
    return () => {
      active = false
    }
  }, [projectId, statusFilter, priorityFilter, revision, onUnauthorized])

  const searchedTasks = useMemo(() => {
    const query = taskSearch.trim().toLocaleLowerCase()
    return query ? tasks.filter((task) => task.title.toLocaleLowerCase().includes(query)) : tasks
  }, [tasks, taskSearch])
  const pageCount = Math.max(1, Math.ceil(searchedTasks.length / visibleTaskPageSize))
  const pageTasks = searchedTasks.slice(taskPage * visibleTaskPageSize, (taskPage + 1) * visibleTaskPageSize)
  const isOwner = project?.owner.userId === userId

  async function handleSaveProject(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setProjectFormError(null)
    setSuccess(null)
    const name = projectName.trim()
    if (name.length < 3 || name.length > 150) {
      setProjectFormError('O nome do projeto deve ter entre 3 e 150 caracteres.')
      return
    }
    if (projectDescription.length > 2000) {
      setProjectFormError('A descrição não pode exceder 2.000 caracteres.')
      return
    }

    setSavingProject(true)
    try {
      const updatedProject = await apiClient.updateProject(projectId, {
        name,
        description: projectDescription || '',
      })
      setProject(updatedProject)
      setProjectName(updatedProject.name)
      setProjectDescription(updatedProject.description ?? '')
      setIsEditingProject(false)
      setSuccess('Projeto atualizado com sucesso.')
      onDataChanged()
    } catch (saveError) {
      if (isUnauthorized(saveError)) onUnauthorized()
      setProjectFormError(apiErrorMessage(saveError, 'Não foi possível atualizar o projeto.'))
    } finally {
      setSavingProject(false)
    }
  }

  async function handleDeleteProject() {
    if (!window.confirm(`Tem a certeza de que pretende excluir o projeto "${project?.name}"?`)) return
    setError(null)
    setSuccess(null)
    setDeletingProject(true)
    try {
      await apiClient.deleteProject(projectId)
      onDataChanged()
      window.location.hash = '#/'
    } catch (deleteError) {
      if (isUnauthorized(deleteError)) onUnauthorized()
      const conflict = deleteError instanceof ApiError && deleteError.status === 409
      setError(conflict
        ? 'Este projeto ainda tem tarefas e não pode ser excluído. Exclua as tarefas primeiro.'
        : apiErrorMessage(deleteError, 'Não foi possível excluir o projeto.'))
    } finally {
      setDeletingProject(false)
    }
  }

  async function handleCreateTask(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setTaskFormError(null)
    setError(null)
    setSuccess(null)
    const title = taskTitle.trim()
    if (title.length < 3 || title.length > 150) {
      setTaskFormError('O título da tarefa deve ter entre 3 e 150 caracteres.')
      return
    }
    if (taskDescription.length > 2000) {
      setTaskFormError('A descrição não pode exceder 2.000 caracteres.')
      return
    }

    setCreatingTask(true)
    try {
      await apiClient.createTask(projectId, { title, description: taskDescription || '' })
      setTaskTitle('')
      setTaskDescription('')
      setShowTaskForm(false)
      setTaskPage(0)
      setSuccess('Tarefa criada com sucesso.')
      onDataChanged()
    } catch (createError) {
      if (isUnauthorized(createError)) onUnauthorized()
      setTaskFormError(apiErrorMessage(createError, 'Não foi possível criar a tarefa.'))
    } finally {
      setCreatingTask(false)
    }
  }

  return (
    <section className="page-section" aria-labelledby="project-title">
      <a className="back-link" href="#/">← Painel e projetos</a>
      {loadingProject ? (
        <p className="loading-message" role="status">A carregar projeto e tarefas…</p>
      ) : error && !project ? (
        <Feedback>{error}</Feedback>
      ) : project ? (
        <>
          <div className="page-heading">
            <div>
              <p className="eyebrow">Projeto</p>
              <h1 id="project-title">{project.name}</h1>
              <p className="supporting-copy">{project.description || 'Sem descrição.'}</p>
              <p className="owner-line">Responsável: {project.owner.displayName}</p>
            </div>
            {isOwner && (
              <div className="action-row">
                <button
                  className="button button-secondary"
                  type="button"
                  onClick={() => {
                    setProjectFormError(null)
                    setIsEditingProject((current) => !current)
                  }}
                >
                  {isEditingProject ? 'Cancelar edição' : 'Editar projeto'}
                </button>
                <button
                  className="button button-danger"
                  type="button"
                  onClick={() => void handleDeleteProject()}
                  disabled={deletingProject}
                >
                  {deletingProject ? 'A excluir…' : 'Excluir projeto'}
                </button>
              </div>
            )}
          </div>

          {error && <Feedback>{error}</Feedback>}
          {success && <Feedback kind="success">{success}</Feedback>}

          {isEditingProject && isOwner && (
            <section className="panel" aria-labelledby="edit-project-title">
              <h2 id="edit-project-title">Editar projeto</h2>
              <form className="form-grid" onSubmit={(event) => void handleSaveProject(event)}>
                <div className="field">
                  <label htmlFor="edit-project-name">Nome</label>
                  <input
                    id="edit-project-name"
                    value={projectName}
                    onChange={(event) => setProjectName(event.target.value)}
                    minLength={3}
                    maxLength={150}
                    required
                    disabled={savingProject}
                  />
                </div>
                <div className="field">
                  <label htmlFor="edit-project-description">Descrição (opcional)</label>
                  <textarea
                    id="edit-project-description"
                    value={projectDescription}
                    onChange={(event) => setProjectDescription(event.target.value)}
                    maxLength={2000}
                    rows={3}
                    disabled={savingProject}
                  />
                  <span className="field-hint">{projectDescription.length}/2000</span>
                </div>
                {projectFormError && <Feedback>{projectFormError}</Feedback>}
                <div className="action-row">
                  <button className="button button-primary" type="submit" disabled={savingProject}>
                    {savingProject ? 'A guardar…' : 'Guardar alterações'}
                  </button>
                  <button className="button button-secondary" type="button" onClick={() => setIsEditingProject(false)}>
                    Cancelar
                  </button>
                </div>
              </form>
            </section>
          )}

          <section className="panel" aria-labelledby="task-list-title">
            <div className="panel-heading">
              <div>
                <h2 id="task-list-title">Tarefas</h2>
                <p className="muted-copy">{tasks.length} tarefa(s) correspondem aos filtros.</p>
              </div>
              <button
                className="button button-primary"
                type="button"
                onClick={() => {
                  setTaskFormError(null)
                  setShowTaskForm((current) => !current)
                }}
              >
                {showTaskForm ? 'Fechar formulário' : 'Criar tarefa'}
              </button>
            </div>

            {showTaskForm && (
              <form className="form-grid task-create-form" onSubmit={(event) => void handleCreateTask(event)}>
                <div className="field">
                  <label htmlFor="new-task-title">Título</label>
                  <input
                    id="new-task-title"
                    value={taskTitle}
                    onChange={(event) => setTaskTitle(event.target.value)}
                    minLength={3}
                    maxLength={150}
                    required
                    disabled={creatingTask}
                  />
                </div>
                <div className="field">
                  <label htmlFor="new-task-description">Descrição (opcional)</label>
                  <textarea
                    id="new-task-description"
                    value={taskDescription}
                    onChange={(event) => setTaskDescription(event.target.value)}
                    maxLength={2000}
                    rows={3}
                    disabled={creatingTask}
                  />
                  <span className="field-hint">{taskDescription.length}/2000</span>
                </div>
                {taskFormError && <Feedback>{taskFormError}</Feedback>}
                <button className="button button-primary" type="submit" disabled={creatingTask}>
                  {creatingTask ? 'A criar…' : 'Criar tarefa'}
                </button>
              </form>
            )}

            <div className="filter-grid">
              <div className="field">
                <label htmlFor="task-search">Pesquisar pelo título</label>
                <input
                  id="task-search"
                  type="search"
                  value={taskSearch}
                  onChange={(event) => {
                    setTaskSearch(event.target.value)
                    setTaskPage(0)
                  }}
                  placeholder="Pesquisar tarefas"
                />
              </div>
              <div className="field">
                <label htmlFor="task-status-filter">Estado</label>
                <select
                  id="task-status-filter"
                  className={statusFilter ? `status-${statusFilter.toLowerCase()}` : undefined}
                  value={statusFilter}
                  onChange={(event) => {
                    setStatusFilter(event.target.value as TaskStatus | '')
                    setTaskPage(0)
                  }}
                >
                  <option value="">Todos os estados</option>
                  {taskStatuses.map((status) => (
                    <option className={`status-${status.toLowerCase()}`} value={status} key={status}>
                      {statusLabels[status]}
                    </option>
                  ))}
                </select>
              </div>
              <div className="field">
                <label htmlFor="task-priority-filter">Prioridade</label>
                <select
                  id="task-priority-filter"
                  value={priorityFilter}
                  onChange={(event) => {
                    setPriorityFilter(event.target.value as TaskPriority | '')
                    setTaskPage(0)
                  }}
                >
                  <option value="">Todas as prioridades</option>
                  {taskPriorities.map((priority) => (
                    <option value={priority} key={priority}>{priorityLabels[priority]}</option>
                  ))}
                </select>
              </div>
            </div>

            {loadingTasks ? (
              <p className="loading-message" role="status">A carregar tarefas…</p>
            ) : pageTasks.length === 0 ? (
              <p className="empty-state">
                {tasks.length === 0 ? 'Este projeto ainda não tem tarefas para estes filtros.' : 'Nenhuma tarefa corresponde à pesquisa.'}
              </p>
            ) : (
              <div className="task-list">
                {pageTasks.map((task) => (
                  <a className="task-row" href={`#/tasks/${encodeURIComponent(task.taskId)}`} key={task.taskId}>
                    <span className="task-main">
                      <strong>{task.title}</strong>
                      <span className="muted-copy">{task.description || 'Sem descrição.'}</span>
                    </span>
                    <span className="tag-row">
                      <span className={`tag status-${task.status.toLowerCase()}`}>{statusLabels[task.status]}</span>
                      <span className="tag">{priorityLabels[task.priority]}</span>
                    </span>
                  </a>
                ))}
              </div>
            )}

            {searchedTasks.length > visibleTaskPageSize && (
              <nav className="pagination" aria-label="Paginação das tarefas">
                <button
                  className="button button-secondary"
                  type="button"
                  onClick={() => setTaskPage((page) => Math.max(0, page - 1))}
                  disabled={taskPage === 0}
                >
                  Anterior
                </button>
                <span>Página {taskPage + 1} de {pageCount}</span>
                <button
                  className="button button-secondary"
                  type="button"
                  onClick={() => setTaskPage((page) => Math.min(pageCount - 1, page + 1))}
                  disabled={taskPage >= pageCount - 1}
                >
                  Seguinte
                </button>
              </nav>
            )}
          </section>
        </>
      ) : null}
    </section>
  )
}
