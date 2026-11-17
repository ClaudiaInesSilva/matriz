import type { TaskPriority, TaskStatus } from '../api/apiClient'

export const taskStatuses: TaskStatus[] = ['TODO', 'IN_PROGRESS', 'DONE']
export const taskPriorities: TaskPriority[] = ['LOW', 'MEDIUM', 'HIGH']

export const statusLabels: Record<TaskStatus, string> = {
  TODO: 'Por fazer',
  IN_PROGRESS: 'Em progresso',
  DONE: 'Concluída',
}

export const priorityLabels: Record<TaskPriority, string> = {
  LOW: 'Baixa',
  MEDIUM: 'Média',
  HIGH: 'Alta',
}
