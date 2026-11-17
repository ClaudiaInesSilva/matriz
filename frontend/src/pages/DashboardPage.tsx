import {useEffect, useMemo, useState, type FormEvent} from 'react'
import {
    ApiError,
    apiClient,
    type AuthenticatedUser,
    type Project,
    type Task,
    type TaskStatus,
    type UserSummary,
} from '../api/apiClient'
import {Feedback} from '../components/Feedback'
import {apiErrorMessage, isUnauthorized} from '../utils/apiMessages'
import {statusLabels} from '../utils/labels'

interface ProjectSummary {
    project: Project
    tasks: Task[]
    assignedTaskCount: number
}

interface DashboardData {
    summaries: ProjectSummary[]
    taskCounts: Record<TaskStatus, number>
}

const emptyCounts: Record<TaskStatus, number> = {
    TODO: 0,
    IN_PROGRESS: 0,
    DONE: 0,
}

const assignmentBatchSize = 10

export function DashboardPage({
                                  user,
                                  revision,
                                  onDataChanged,
                                  onUnauthorized,
                              }: {
    user: AuthenticatedUser
    revision: number
    onDataChanged: () => void
    onUnauthorized: () => void
}) {
    const [dashboard, setDashboard] = useState<DashboardData | null>(null)
    const [searchResults, setSearchResults] = useState<Project[] | null>(null)
    const [search, setSearch] = useState('')
    const [projectName, setProjectName] = useState('')
    const [projectDescription, setProjectDescription] = useState('')
    const [loading, setLoading] = useState(true)
    const [searching, setSearching] = useState(false)
    const [creating, setCreating] = useState(false)
    const [isCreateOpen, setIsCreateOpen] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [formError, setFormError] = useState<string | null>(null)
    const [success, setSuccess] = useState<string | null>(null)

    useEffect(() => {
        let active = true
        // oxlint-disable-next-line react/set-state-in-effect
        setLoading(true)
        setError(null)
        setSearchResults(null)
        setSearch('')

        async function load() {
            try {
                const projects = await apiClient.listProjects()
                const taskGroups = await Promise.all(
                    projects.map((project) => apiClient.listAllTasks(project.projectId)),
                )
                const distinctTaskGroups = taskGroups.map((tasks) => [...new Map(tasks.map((task) => [task.taskId, task])).values()])
                const taskCounts = {...emptyCounts}
                for (const tasks of distinctTaskGroups) {
                    for (const task of tasks) taskCounts[task.status] += 1
                }

                const assignmentCounts = new Map<string, number>()
                for (let index = 0; index < projects.length; index += 1) {
                    const tasks = distinctTaskGroups[index]
                    const assignedTaskIds = new Set<string>()
                    for (let start = 0; start < tasks.length; start += assignmentBatchSize) {
                        const batch = tasks.slice(start, start + assignmentBatchSize)
                        const assignmentGroups = await Promise.all(
                            batch.map((task) => apiClient.listAssignees(task.taskId)),
                        )
                        assignmentGroups.forEach((assignees: UserSummary[], batchIndex) => {
                          if (assignees.some((assignee) => assignee.userId === user.userId)) {
                            assignedTaskIds.add(batch[batchIndex].taskId)
                          }
                        })
                    }
                    assignmentCounts.set(projects[index].projectId, assignedTaskIds.size)
                }

                if (!active) return
                const summaries = projects.map((project, index) => ({
                    project,
                    tasks: distinctTaskGroups[index],
                    assignedTaskCount: assignmentCounts.get(project.projectId) ?? 0,
                }))
                setDashboard({summaries, taskCounts})
            } catch (loadError) {
                if (!active) return
                if (isUnauthorized(loadError)) onUnauthorized()
                setDashboard(null)
                setError(dashboardErrorMessage(loadError, 'Não foi possível carregar o painel e os dados dos projetos.'))
            } finally {
                if (active) setLoading(false)
            }
        }

        void load()
        return () => {
            active = false
        }
    }, [revision, user.userId, onUnauthorized])

    useEffect(() => {
        if (!isCreateOpen) return

        function handleKeyDown(event: KeyboardEvent) {
            if (event.key === 'Escape' && !creating) setIsCreateOpen(false)
        }

        window.addEventListener('keydown', handleKeyDown)
        return () => window.removeEventListener('keydown', handleKeyDown)
    }, [isCreateOpen, creating])

    async function handleSearch(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        setSearching(true)
        setError(null)
        try {
            setSearchResults(await apiClient.listProjects(search))
        } catch (searchError) {
            if (isUnauthorized(searchError)) onUnauthorized()
            setError(dashboardErrorMessage(searchError, 'Não foi possível pesquisar projetos.'))
        } finally {
            setSearching(false)
        }
    }

    async function handleCreateProject(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        setFormError(null)

        const name = projectName.trim()
        if (!name || name.length < 3 || name.length > 150) {
            setFormError('Informe um nome com 3 a 150 caracteres.')
            return
        }
        if (projectDescription.length > 2000) {
            setFormError('A descrição não pode exceder 2.000 caracteres.')
            return
        }

        setCreating(true)
        try {
            await apiClient.createProject({name, description: projectDescription || ''})
            setProjectName('')
            setProjectDescription('')
            setIsCreateOpen(false)
            setSuccess('Projeto criado com sucesso.')
            onDataChanged()
        } catch (createError) {
            if (isUnauthorized(createError)) onUnauthorized()
            setFormError(dashboardErrorMessage(createError, 'Não foi possível criar o projeto.'))
        } finally {
            setCreating(false)
        }
    }

    const summaries = dashboard?.summaries ?? []
    const projectsById = searchResults
        ? new Map(searchResults.map((project) => [project.projectId, project]))
        : null
    const displayedSummaries = projectsById
        ? summaries.filter(({project}) => projectsById.has(project.projectId))
        : summaries
    const myProjects = displayedSummaries.filter(({project}) => project.owner.userId === user.userId)
    const otherProjects = displayedSummaries.filter(({project}) => project.owner.userId !== user.userId)
    const displayName = user.displayName.trim() || user.username

    return (
        <section className="page-section dashboard-page" aria-labelledby="dashboard-title">
            <div className="dashboard-welcome">
                <p className="eyebrow">Visão geral</p>
                <h1 id="dashboard-title">Olá, {displayName}!</h1>
                <p className="supporting-copy">Acompanhe os seus projetos e o trabalho da equipa.</p>
            </div>

            {error && <Feedback>{error}</Feedback>}
            {success && <Feedback kind="success">{success}</Feedback>}

            {dashboard && (
                <section className="dashboard-summary" aria-label="Resumo de tarefas em todos os projetos">
                    {(['TODO', 'IN_PROGRESS', 'DONE'] as const).map((status) => (
                        <article className={`summary-item status-${status.toLowerCase()}`} key={status}>
                            <span>{statusLabels[status]}</span>
                            <strong>{dashboard.taskCounts[status]}</strong>
                        </article>
                    ))}
                </section>
            )}

            <section className="project-section my-projects-section" aria-labelledby="my-projects-title">
                <div className="project-section-heading">
                    <div>
                        <p className="eyebrow">Os seus espaços</p>
                        <h2 id="my-projects-title">Os meus projetos</h2>
                    </div>
                    <button
                        className="button button-primary"
                        type="button"
                        onClick={() => {
                            setFormError(null)
                            setIsCreateOpen(true)
                        }}
                    >
                        Criar Projeto
                    </button>
                </div>

                {loading ? (
                    <p className="loading-message" role="status">A carregar projetos e tarefas…</p>
                ) : error && !dashboard ? null : myProjects.length === 0 ? (
                    <p className="empty-state">
                        {searchResults ? 'Nenhum dos seus projetos corresponde à pesquisa.' : 'Ainda não criou nenhum projeto.'}
                    </p>
                ) : (
                    <div className="dashboard-project-grid">
                        {myProjects.map((summary) => (
                            <ProjectCard key={summary.project.projectId} summary={summary} kind="owned"/>
                        ))}
                    </div>
                )}
            </section>

            <section className="project-section other-projects-section" aria-labelledby="other-projects-title">
                <div className="project-section-heading other-projects-heading">
                    <div>
                        <p className="eyebrow">Espaços partilhados</p>
                        <h2 id="other-projects-title">Outros projetos</h2>
                        <p className="muted-copy">As tarefas atribuídas contam apenas tarefas em que está incluído.</p>
                    </div>
                    <form className="inline-search dashboard-search" onSubmit={(event) => void handleSearch(event)}>
                        <label className="visually-hidden" htmlFor="project-search">Pesquisar projetos pelo nome</label>
                        <input
                            id="project-search"
                            type="search"
                            value={search}
                            onChange={(event) => setSearch(event.target.value)}
                            placeholder="Pesquisar projetos"
                        />
                        <button className="button button-secondary" type="submit" disabled={searching}>
                            {searching ? 'A pesquisar…' : 'Pesquisar'}
                        </button>
                        {searchResults && (
                            <button
                                className="button button-text"
                                type="button"
                                onClick={() => {
                                    setSearch('')
                                    setSearchResults(null)
                                }}
                            >
                                Limpar
                            </button>
                        )}
                    </form>
                </div>

                {loading ? (
                    <p className="loading-message" role="status">A carregar projetos partilhados…</p>
                ) : error && !dashboard ? null : otherProjects.length === 0 ? (
                    <p className="empty-state">
                        {searchResults
                            ? 'Nenhum outro projeto corresponde à pesquisa.'
                            : 'Não há outros projetos disponíveis.'}
                    </p>
                ) : (
                    <div className="dashboard-project-grid">
                        {otherProjects.map((summary) => (
                            <ProjectCard key={summary.project.projectId} summary={summary} kind="shared"/>
                        ))}
                    </div>
                )}
            </section>

            {isCreateOpen && (
                <div
                    className="modal-backdrop"
                    onMouseDown={(event) => {
                        if (event.target === event.currentTarget && !creating) setIsCreateOpen(false)
                    }}
                >
                    <section
                        className="create-project-modal"
                        role="dialog"
                        aria-modal="true"
                        aria-labelledby="create-project-title"
                    >
                        <div className="modal-heading">
                            <div>
                                <p className="eyebrow">Novo espaço de trabalho</p>
                                <h2 id="create-project-title">Criar Projeto</h2>
                            </div>
                            <button
                                className="button button-text modal-close"
                                type="button"
                                aria-label="Fechar formulário"
                                onClick={() => setIsCreateOpen(false)}
                                disabled={creating}
                            >
                                ×
                            </button>
                        </div>
                        <form className="form-grid" onSubmit={(event) => void handleCreateProject(event)}>
                            <div className="field">
                                <label htmlFor="new-project-name">Nome do projeto</label>
                                <input
                                    id="new-project-name"
                                    value={projectName}
                                    onChange={(event) => setProjectName(event.target.value)}
                                    minLength={3}
                                    maxLength={150}
                                    required
                                    autoFocus
                                    disabled={creating}
                                />
                                <span className="field-hint">3 a 150 caracteres</span>
                            </div>
                            <div className="field">
                                <label htmlFor="new-project-description">Descrição (opcional)</label>
                                <textarea
                                    id="new-project-description"
                                    value={projectDescription}
                                    onChange={(event) => setProjectDescription(event.target.value)}
                                    maxLength={2000}
                                    rows={4}
                                    disabled={creating}
                                />
                                <span className="field-hint">{projectDescription.length}/2000</span>
                            </div>
                            {formError && <Feedback>{formError}</Feedback>}
                            <div className="modal-actions">
                                <button
                                    className="button button-secondary"
                                    type="button"
                                    onClick={() => setIsCreateOpen(false)}
                                    disabled={creating}
                                >
                                    Cancelar
                                </button>
                                <button className="button button-primary" type="submit" disabled={creating}>
                                    {creating ? 'A criar…' : 'Criar projeto'}
                                </button>
                            </div>
                        </form>
                    </section>
                </div>
            )}
        </section>
    )
}

function dashboardErrorMessage(error: unknown, fallback: string) {
    if (!(error instanceof ApiError)) return fallback

    if (error.status === 400) {
        const validationMessages: Record<string, string> = {
            'Project name is required': 'O nome do projeto é obrigatório.',
            'Project name must contain 3 to 150 characters': 'O nome do projeto deve ter entre 3 e 150 caracteres.',
            'Project description must not exceed 2000 characters': 'A descrição do projeto não pode exceder 2.000 caracteres.',
        }
        return validationMessages[error.message] ?? 'Os dados do projeto não são válidos.'
    }

    return apiErrorMessage(error, fallback)
}

function ProjectCard({summary, kind}: { summary: ProjectSummary; kind: 'owned' | 'shared' }) {
    const counts = useMemo(() => {
        const result = {...emptyCounts}
        for (const task of summary.tasks) result[task.status] += 1
        return result
    }, [summary.tasks])

    return (
        <article
            className={`dashboard-project-card ${kind === 'owned' ? 'owned-project-card' : 'shared-project-card'}`}>
            <div className="dashboard-project-card-heading">
                <div>
                    {kind === 'shared' && (
                        <p className="project-owner-label">Responsável: {summary.project.owner.displayName}</p>
                    )}
                    <h3>{summary.project.name}</h3>
                </div>
                <span className="project-task-total">
          <strong>{summary.tasks.length}</strong>
          <span>{summary.tasks.length === 1 ? 'tarefa' : 'tarefas'}</span>
        </span>
            </div>

            <p className="assigned-task-count">
                <strong>{summary.assignedTaskCount}</strong>
                <span>{summary.assignedTaskCount === 1 ? 'tarefa atribuída a si' : 'tarefas atribuídas a si'}</span>
            </p>

            {kind === 'owned' && (
                <dl className="project-status-counts">
                    {(['TODO', 'IN_PROGRESS', 'DONE'] as const).map((status) => (
                        <div className={`status-${status.toLowerCase()}`} key={status}>
                            <dt>{statusLabels[status]}</dt>
                            <dd>{counts[status]}</dd>
                        </div>
                    ))}
                </dl>
            )}

            <a className="project-open-link" href={`#/projects/${encodeURIComponent(summary.project.projectId)}`}>
                Abrir projeto <span aria-hidden="true">→</span>
            </a>
        </article>
    )
}
