import { ApiError } from '../api/apiClient'

export function isUnauthorized(error: unknown) {
  return error instanceof ApiError && error.status === 401
}

export function apiErrorMessage(error: unknown, fallback: string) {
  if (!(error instanceof ApiError)) {
    return error instanceof Error ? error.message : fallback
  }

  if (error.status === 400) return error.message || 'Verifique os dados informados.'
  if (error.status === 403) return 'Você não tem permissão para realizar esta ação.'
  if (error.status === 404) return 'O item solicitado não foi encontrado. Atualize a página e tente novamente.'
  if (error.status === 409) return error.message || 'A operação não pode ser concluída devido a um conflito.'
  if (error.status === 401) return 'A sessão expirou. Inicie sessão novamente.'
  return error.message || fallback
}
