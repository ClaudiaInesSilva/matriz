import {useEffect, useMemo, useState, type FormEvent} from 'react'
import {
    ApiError,
    apiClient,
    type Comment,
    type Project,
    type Task,
    type TaskPriority,
    type TaskStatus,
    type UserSummary,
} from '../api/apiClient'
import {Feedback} from '../components/Feedback'
import {apiErrorMessage, isUnauthorized} from '../utils/apiMessages'
import {priorityLabels, statusLabels, taskPriorities, taskStatuses} from '../utils/labels'

export function TaskPage({
                             taskId,
                             revision,
                             onDataChanged,
                             onUnauthorized,
                         }: {
    taskId: string
    revision: number
    onDataChanged: () => void
    onUnauthorized: () => void
}) {
    const [task, setTask] = useState<Task | null>(null)
    const [project, setProject] = useState<Project | null>(null)
    const [assignees, setAssignees] = useState<UserSummary[]>([])
    const [eligibleUsers, setEligibleUsers] = useState<UserSummary[]>([])
    const [comments, setComments] = useState<Comment[]>([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)
    const [success, setSuccess] = useState<string | null>(null)
    const [isEditingTask, setIsEditingTask] = useState(false)
    const [savingTask, setSavingTask] = useState(false)
    const [deletingTask, setDeletingTask] = useState(false)
    const [taskFormError, setTaskFormError] = useState<string | null>(null)
    const [title, setTitle] = useState('')
    const [description, setDescription] = useState('')
    const [status, setStatus] = useState<TaskStatus>('TODO')
    const [priority, setPriority] = useState<TaskPriority>('MEDIUM')
    const [selectedUserId, setSelectedUserId] = useState('')
    const [assignmentBusy, setAssignmentBusy] = useState<string | null>(null)
    const [commentContent, setCommentContent] = useState('')
    const [addingComment, setAddingComment] = useState(false)
    const [commentError, setCommentError] = useState<string | null>(null)
    const [commentSuccess, setCommentSuccess] = useState<string | null>(null)

    const availableUsers = useMemo(
        () => eligibleUsers.filter((user) => !assignees.some((assignee) => assignee.userId === user.userId)),
        [eligibleUsers, assignees],
    )

    useEffect(() => {
        let active = true
        // oxlint-disable-next-line react/set-state-in-effect
        setLoading(true)
        setError(null)

        async function load() {
            try {
                const taskResponse = await apiClient.getTask(taskId)
                const [projectResponse, assigneeResponse, eligibleResponse, commentResponse] = await Promise.all([
                    apiClient.getProject(taskResponse.projectId),
                    apiClient.listAssignees(taskId),
                    apiClient.listEligibleAssignees(taskResponse.projectId),
                    apiClient.listComments(taskId),
                ])
                if (!active) return
                setTask(taskResponse)
                setIsEditingTask(false)
                setProject(projectResponse)
                setAssignees(assigneeResponse)
                setEligibleUsers(eligibleResponse)
                setComments(commentResponse)
                setTitle(taskResponse.title)
                setDescription(taskResponse.description ?? '')
                setStatus(taskResponse.status)
                setPriority(taskResponse.priority)
                setSelectedUserId(
                    eligibleResponse.find((user) => !assigneeResponse.some((assignee) => assignee.userId === user.userId))
                        ?.userId ?? '',
                )
            } catch (loadError) {
                if (!active) return
                if (isUnauthorized(loadError)) onUnauthorized()
                setError(apiErrorMessage(loadError, 'Não foi possível carregar a tarefa.'))
            } finally {
                if (active) setLoading(false)
            }
        }

        void load()
        return () => {
            active = false
        }
    }, [taskId, revision, onUnauthorized])

    async function handleSaveTask(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        setTaskFormError(null)
        setSuccess(null)
        const trimmedTitle = title.trim()
        if (trimmedTitle.length < 3 || trimmedTitle.length > 150) {
            setTaskFormError('O título da tarefa deve ter entre 3 e 150 caracteres.')
            return
        }
        if (description.length > 2000) {
            setTaskFormError('A descrição não pode exceder 2.000 caracteres.')
            return
        }

        setSavingTask(true)
        try {
            const updatedTask = await apiClient.updateTask(taskId, {
                title: trimmedTitle,
                description,
                status,
                priority,
            })
            setTask(updatedTask)
            setTitle(updatedTask.title)
            setDescription(updatedTask.description ?? '')
            setStatus(updatedTask.status)
            setPriority(updatedTask.priority)
            setIsEditingTask(false)
            setSuccess('Tarefa atualizada com sucesso.')
            onDataChanged()
        } catch (saveError) {
            if (isUnauthorized(saveError)) onUnauthorized()
            setTaskFormError(taskErrorMessage(saveError))
        } finally {
            setSavingTask(false)
        }
    }

    async function handleDeleteTask() {
        if (!window.confirm(`Tem a certeza de que pretende excluir a tarefa "${task?.title}"?`)) return
        setError(null)
        setSuccess(null)
        setDeletingTask(true)
        try {
            await apiClient.deleteTask(taskId)
            onDataChanged()
            if (task) window.location.hash = `#/projects/${encodeURIComponent(task.projectId)}`
        } catch (deleteError) {
            if (isUnauthorized(deleteError)) onUnauthorized()
            const forbidden = deleteError instanceof ApiError && deleteError.status === 403
            setError(forbidden
                ? 'Apenas quem criou a tarefa ou o responsável pelo projeto pode excluí-la.'
                : apiErrorMessage(deleteError, 'Não foi possível excluir a tarefa.'))
        } finally {
            setDeletingTask(false)
        }
    }

    async function handleAddAssignee(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        setError(null)
        setSuccess(null)
        if (!selectedUserId) return
        setAssignmentBusy(selectedUserId)
        try {
            await apiClient.addAssignee(taskId, selectedUserId)
            setSuccess('Pessoa atribuída à tarefa.')
            onDataChanged()
        } catch (assignmentError) {
            if (isUnauthorized(assignmentError)) onUnauthorized()
            const duplicate = assignmentError instanceof ApiError && assignmentError.status === 409
            setError(duplicate
                ? 'Esta pessoa já está atribuída à tarefa.'
                : apiErrorMessage(assignmentError, 'Não foi possível atribuir a pessoa.'))
        } finally {
            setAssignmentBusy(null)
        }
    }

    async function handleRemoveAssignee(assignee: UserSummary) {
        setError(null)
        setSuccess(null)
        setAssignmentBusy(assignee.userId)
        try {
            await apiClient.removeAssignee(taskId, assignee.userId)
            setSuccess('Atribuição removida.')
            onDataChanged()
        } catch (removeError) {
            if (isUnauthorized(removeError)) onUnauthorized()
            setError(apiErrorMessage(removeError, 'Não foi possível remover a atribuição.'))
        } finally {
            setAssignmentBusy(null)
        }
    }

    async function handleAddComment(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        setCommentError(null)
        setCommentSuccess(null)
        if (!commentContent.trim()) {
            setCommentError('O comentário não pode estar vazio.')
            return
        }
        if (commentContent.length > 2000) {
            setCommentError('O comentário não pode exceder 2.000 caracteres.')
            return
        }

        setAddingComment(true)
        try {
            await apiClient.createComment(taskId, commentContent)
            setCommentContent('')
            setCommentSuccess('Comentário adicionado.')
            onDataChanged()
        } catch (createError) {
            if (isUnauthorized(createError)) onUnauthorized()
            setCommentError(apiErrorMessage(createError, 'Não foi possível adicionar o comentário.'))
        } finally {
            setAddingComment(false)
        }
    }

    return (
        <section className="page-section" aria-labelledby="task-page-title">
            {project && <a className="back-link"
                           href={`#/projects/${encodeURIComponent(project.projectId)}`}>← {project.name}</a>}
            {loading ? (
                <p className="loading-message" role="status">A carregar tarefa…</p>
            ) : error && !task ? (
                <Feedback>{error}</Feedback>
            ) : task ? (
                <>
                    <div className="page-heading">
                        <div>
                            <p className="eyebrow">Detalhes da tarefa</p>
                            <h1 id="task-page-title">{task.title}</h1>
                            {project && <p className="supporting-copy">Projeto: {project.name}</p>}
                        </div>
                        <div className="action-row">
                            <button
                                className="button button-secondary"
                                type="button"
                                onClick={() => {
                                    setTaskFormError(null)
                                    setSuccess(null)
                                    if (isEditingTask) {
                                        setTitle(task.title)
                                        setDescription(task.description ?? '')
                                        setStatus(task.status)
                                        setPriority(task.priority)
                                    }
                                    setIsEditingTask((current) => !current)
                                }}
                                disabled={savingTask}
                            >
                                {isEditingTask ? 'Cancelar edição' : 'Editar tarefa'}
                            </button>
                            <button
                                className="button button-danger"
                                type="button"
                                onClick={() => void handleDeleteTask()}
                                disabled={deletingTask}
                            >
                                {deletingTask ? 'A excluir…' : 'Excluir tarefa'}
                            </button>
                        </div>
                    </div>
                    {error && <Feedback>{error}</Feedback>}
                    {success && <Feedback kind="success">{success}</Feedback>}

                    <div className="task-overview-grid">
                    {isEditingTask ? (
                        <section className="panel" aria-labelledby="task-edit-title">
                            <h2 id="task-edit-title">Editar tarefa</h2>
                            <form className="form-grid" onSubmit={(event) => void handleSaveTask(event)}>
                                <div className="field">
                                    <label htmlFor="task-title">Título</label>
                                    <input
                                        id="task-title"
                                        value={title}
                                        onChange={(event) => setTitle(event.target.value)}
                                        minLength={3}
                                        maxLength={150}
                                        required
                                        disabled={savingTask}
                                    />
                                </div>
                                <div className="field">
                                    <label htmlFor="task-description">Descrição (opcional)</label>
                                    <textarea
                                        id="task-description"
                                        value={description}
                                        onChange={(event) => setDescription(event.target.value)}
                                        maxLength={2000}
                                        rows={4}
                                        disabled={savingTask}
                                    />
                                    <span className="field-hint">{description.length}/2000</span>
                                </div>
                                <div className="filter-grid">
                                    <div className="field">
                                        <label htmlFor="task-status">Estado</label>
                                        <select
                                            id="task-status"
                                            className={`status-${status.toLowerCase()}`}
                                            value={status}
                                            onChange={(event) => setStatus(event.target.value as TaskStatus)}
                                            disabled={savingTask}
                                        >
                                            {taskStatuses.map((value) => (
                                                <option className={`status-${value.toLowerCase()}`} value={value}
                                                        key={value}>{statusLabels[value]}</option>
                                            ))}
                                        </select>
                                    </div>
                                    <div className="field">
                                        <label htmlFor="task-priority">Prioridade</label>
                                        <select
                                            id="task-priority"
                                            value={priority}
                                            onChange={(event) => setPriority(event.target.value as TaskPriority)}
                                            disabled={savingTask}
                                        >
                                            {taskPriorities.map((value) => <option value={value}
                                                                                   key={value}>{priorityLabels[value]}</option>)}
                                        </select>
                                    </div>
                                </div>
                                {taskFormError && <Feedback>{taskFormError}</Feedback>}
                                <div className="action-row">
                                    <button className="button button-primary" type="submit" disabled={savingTask}>
                                        {savingTask ? 'A guardar…' : 'Guardar alterações'}
                                    </button>
                                    <button
                                        className="button button-secondary"
                                        type="button"
                                        onClick={() => {
                                            setTitle(task.title)
                                            setDescription(task.description ?? '')
                                            setStatus(task.status)
                                            setPriority(task.priority)
                                            setTaskFormError(null)
                                            setIsEditingTask(false)
                                        }}
                                        disabled={savingTask}
                                    >
                                        Cancelar
                                    </button>
                                </div>
                            </form>
                        </section>
                    ) : (
                        <section className="panel task-details" aria-labelledby="task-details-title">
                            <h2 id="task-details-title">Informações da tarefa</h2>
                            <div>
                                <h3>Descrição</h3>
                                <p className="task-description">{task.description || 'Sem descrição.'}</p>
                            </div>
                            <div className="tag-row task-detail-tags">
                                <span
                                    className={`tag status-${task.status.toLowerCase()}`}>{statusLabels[task.status]}</span>
                                <span className="tag">{priorityLabels[task.priority]}</span>
                            </div>
                        </section>
                    )}

                    <section className="panel assignment-panel" aria-labelledby="assignees-title">
                        <div className="panel-heading">
                            <div>
                                <h2 id="assignees-title">Pessoas atribuídas</h2>
                            </div>
                        </div>
                        {assignees.length === 0 ? (
                            <p className="empty-state">Ainda não há pessoas atribuídas.</p>
                        ) : (
                            <ul className="person-list">
                                {assignees.map((assignee) => (
                                    <li key={assignee.userId}>
                                        <span>{assignee.displayName}</span>
                                        <button
                                            className="button button-text"
                                            type="button"
                                            onClick={() => void handleRemoveAssignee(assignee)}
                                            disabled={assignmentBusy === assignee.userId}
                                        >
                                            {assignmentBusy === assignee.userId ? 'A remover…' : 'Remover'}
                                        </button>
                                    </li>
                                ))}
                            </ul>
                        )}
                        {availableUsers.length > 0 ? (
                            <form className="inline-form" onSubmit={(event) => void handleAddAssignee(event)}>
                                <div className="field">
                                    <label htmlFor="assignee-select">Adicionar pessoa</label>
                                    <select
                                        id="assignee-select"
                                        value={selectedUserId}
                                        onChange={(event) => setSelectedUserId(event.target.value)}
                                        disabled={assignmentBusy !== null}
                                    >
                                        {availableUsers.map((user) => (
                                            <option value={user.userId} key={user.userId}>{user.displayName}</option>
                                        ))}
                                    </select>
                                </div>
                                <button className="button button-primary" type="submit"
                                        disabled={assignmentBusy !== null || !selectedUserId}>
                                    {assignmentBusy && !assignees.some((user) => user.userId === assignmentBusy) ? 'A atribuir…' : 'Atribuir'}
                                </button>
                            </form>
                        ) : (
                            <p className="muted-copy">Todas as contas elegíveis já estão atribuídas.</p>
                        )}
                    </section>
                    </div>

                    <section className="panel comments-panel" aria-labelledby="comments-title">
                        <h2 id="comments-title">Comentários</h2>
                        {comments.length === 0 ? (
                            <p className="empty-state">Ainda não há comentários.</p>
                        ) : (
                            <ol className="comment-list">
                                {comments.map((comment) => (
                                    <li className="comment-card" key={comment.commentId}>
                                        <div className="comment-meta">
                                            <strong>{comment.author.displayName}</strong>
                                            <time
                                                dateTime={comment.createdAt}>{formatTimestamp(comment.createdAt)}</time>
                                        </div>
                                        <p>{comment.content}</p>
                                    </li>
                                ))}
                            </ol>
                        )}
                        {commentSuccess && <Feedback kind="success">{commentSuccess}</Feedback>}
                        {commentError && <Feedback>{commentError}</Feedback>}
                        <form className="form-grid comment-form" onSubmit={(event) => void handleAddComment(event)}>
                            <div className="field">
                                <label htmlFor="new-comment">Adicionar comentário</label>
                                <textarea
                                    id="new-comment"
                                    value={commentContent}
                                    onChange={(event) => setCommentContent(event.target.value)}
                                    maxLength={2000}
                                    rows={4}
                                    required
                                    disabled={addingComment}
                                />
                                <span className="field-hint">{commentContent.length}/2000</span>
                            </div>
                            <button className="button button-primary" type="submit" disabled={addingComment}>
                                {addingComment ? 'A enviar…' : 'Adicionar comentário'}
                            </button>
                        </form>
                    </section>
                </>
            ) : null}
        </section>
    )
}

function formatTimestamp(value: string) {
    const date = new Date(value)
    return Number.isNaN(date.getTime()) ? value : date.toLocaleString()
}

function taskErrorMessage(error: unknown) {
    if (error instanceof ApiError && error.status === 400) {
        const validationMessages: Record<string, string> = {
            'Task title is required': 'O título da tarefa é obrigatório.',
            'Task title must contain 3 to 150 characters': 'O título da tarefa deve ter entre 3 e 150 caracteres.',
            'Task description must not exceed 2000 characters': 'A descrição não pode exceder 2.000 caracteres.',
            'Task status is required': 'O estado da tarefa é obrigatório.',
            'Task priority is required': 'A prioridade da tarefa é obrigatória.',
        }
        return validationMessages[error.message] ?? 'Os dados da tarefa não são válidos.'
    }

    return apiErrorMessage(error, 'Não foi possível atualizar a tarefa.')
}
