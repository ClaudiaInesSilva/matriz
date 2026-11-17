export interface AuthenticatedUser {
  userId: string
  username: string
  displayName: string
}

export interface UserSummary {
  userId: string
  displayName: string
}

export interface Project {
  projectId: string
  name: string
  description: string | null
  owner: UserSummary
}

export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE'
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH'

export interface Task {
  taskId: string
  projectId: string
  title: string
  description: string | null
  status: TaskStatus
  priority: TaskPriority
}

export interface TaskPage {
  tasks: Task[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface TaskFilters {
  status?: TaskStatus
  priority?: TaskPriority
}

export interface Comment {
  commentId: string
  taskId: string
  author: UserSummary
  content: string
  createdAt: string
}

interface CsrfTokenResponse {
  token: string
  headerName: string
}

interface ApiErrorBody {
  message?: unknown
}

export class ApiError extends Error {
  readonly status: number

  constructor(message: string, status: number) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/+$/, '')

function apiUrl(path: string) {
  return `${apiBaseUrl}/${path.replace(/^\/+/, '')}`
}

async function responseError(response: Response): Promise<ApiError> {
  const body = await response.text()
  let message = `Request failed with status ${response.status}.`

  if (body) {
    try {
      const parsed = JSON.parse(body) as ApiErrorBody
      if (typeof parsed.message === 'string' && parsed.message.trim()) {
        message = parsed.message
      }
    } catch {
      // Use the status-based message for non-JSON error responses.
    }
  }

  return new ApiError(message, response.status)
}

async function getCsrfToken(): Promise<CsrfTokenResponse> {
  const response = await fetch(apiUrl('/auth/csrf'), {
    method: 'GET',
    credentials: 'include',
    headers: { Accept: 'application/json' },
  })

  if (!response.ok) {
    throw await responseError(response)
  }

  const csrf = (await response.json()) as CsrfTokenResponse
  if (!csrf.token || !csrf.headerName) {
    throw new Error('The server returned an invalid CSRF token response.')
  }

  return csrf
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const method = (init.method ?? 'GET').toUpperCase()
  const headers = new Headers(init.headers)
  headers.set('Accept', 'application/json')

  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    const csrf = await getCsrfToken()
    headers.set(csrf.headerName, csrf.token)
  }

  const response = await fetch(apiUrl(path), {
    ...init,
    method,
    credentials: 'include',
    headers,
  })

  if (!response.ok) {
    throw await responseError(response)
  }

  if (response.status === 204) {
    return undefined as T
  }

  const body = await response.text()
  return body ? (JSON.parse(body) as T) : (undefined as T)
}

export const apiClient = {
  getCurrentUser() {
    return request<AuthenticatedUser>('/auth/me')
  },

  listProjects(search?: string) {
    const query = search?.trim()
    const suffix = query ? `?${new URLSearchParams({ search: query })}` : ''
    return request<Project[]>(`/projects${suffix}`)
  },

  getProject(projectId: string) {
    return request<Project>(`/projects/${encodeURIComponent(projectId)}`)
  },

  createProject(input: { name: string; description: string }) {
    return request<Project>('/projects', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(input),
    })
  },

  updateProject(projectId: string, input: { name: string; description: string }) {
    return request<Project>(`/projects/${encodeURIComponent(projectId)}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(input),
    })
  },

  deleteProject(projectId: string) {
    return request<void>(`/projects/${encodeURIComponent(projectId)}`, { method: 'DELETE' })
  },

  listTaskPage(projectId: string, filters: TaskFilters = {}, page = 0, size = 100) {
    const query = new URLSearchParams({ page: String(page), size: String(size) })
    if (filters.status) query.set('status', filters.status)
    if (filters.priority) query.set('priority', filters.priority)
    return request<TaskPage>(`/projects/${encodeURIComponent(projectId)}/tasks?${query}`)
  },

  async listAllTasks(projectId: string, filters: TaskFilters = {}) {
    const firstPage = await this.listTaskPage(projectId, filters, 0, 100)
    const tasks = [...firstPage.tasks]
    for (let page = 1; page < firstPage.totalPages; page += 1) {
      const nextPage = await this.listTaskPage(projectId, filters, page, 100)
      tasks.push(...nextPage.tasks)
    }
    return tasks
  },

  createTask(projectId: string, input: {
    title: string
    description: string
    status?: TaskStatus
    priority?: TaskPriority
  }) {
    return request<Task>(`/projects/${encodeURIComponent(projectId)}/tasks`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(input),
    })
  },

  getTask(taskId: string) {
    return request<Task>(`/tasks/${encodeURIComponent(taskId)}`)
  },

  updateTask(taskId: string, input: {
    title: string
    description: string
    status: TaskStatus
    priority: TaskPriority
  }) {
    return request<Task>(`/tasks/${encodeURIComponent(taskId)}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(input),
    })
  },

  deleteTask(taskId: string) {
    return request<void>(`/tasks/${encodeURIComponent(taskId)}`, { method: 'DELETE' })
  },

  listEligibleAssignees(projectId: string) {
    return request<UserSummary[]>(`/projects/${encodeURIComponent(projectId)}/eligible-assignees`)
  },

  listAssignees(taskId: string) {
    return request<UserSummary[]>(`/tasks/${encodeURIComponent(taskId)}/assignees`)
  },

  addAssignee(taskId: string, userId: string) {
    return request<void>(`/tasks/${encodeURIComponent(taskId)}/assignees`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ userId }),
    })
  },

  removeAssignee(taskId: string, userId: string) {
    return request<void>(
      `/tasks/${encodeURIComponent(taskId)}/assignees/${encodeURIComponent(userId)}`,
      { method: 'DELETE' },
    )
  },

  listComments(taskId: string) {
    return request<Comment[]>(`/tasks/${encodeURIComponent(taskId)}/comments`)
  },

  createComment(taskId: string, content: string) {
    return request<Comment>(`/tasks/${encodeURIComponent(taskId)}/comments`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ content }),
    })
  },

  async login(username: string, password: string) {
    try {
      return await request<AuthenticatedUser>('/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password }),
      })
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) {
        throw new ApiError('Incorrect username or password.', 401)
      }
      throw error
    }
  },

  logout() {
    return request<void>('/auth/logout', { method: 'POST' })
  },
}
