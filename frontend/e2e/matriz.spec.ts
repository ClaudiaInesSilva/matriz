import { expect, test } from '@playwright/test'

function requiredEnvironment(name: string): string {
  const value = process.env[name]?.trim()
  if (!value) {
    throw new Error(`Missing required environment variable: ${name}`)
  }
  return value
}

test('login, open project and create task', async ({ page }, testInfo) => {
  const username = requiredEnvironment('MATRIZ_E2E_USERNAME')
  const password = requiredEnvironment('MATRIZ_E2E_PASSWORD')
  const projectId = requiredEnvironment('MATRIZ_E2E_PROJECT_ID')
  const titlePrefix = process.env.MATRIZ_E2E_TASK_TITLE_PREFIX?.trim() || 'E2E task'
  const taskTitle = `${titlePrefix} ${Date.now()}`

  await page.goto('/')

  await page.locator('#username').fill(username)
  await page.locator('#password').fill(password)
  await page.getByRole('button', { name: 'Sign in', exact: true }).click()

  await expect(page.locator('#dashboard-title')).toBeVisible()

  await page.goto(`/#/projects/${encodeURIComponent(projectId)}`)
  await expect(page.locator('#project-title')).toBeVisible()

  const taskPanel = page.locator('section[aria-labelledby="task-list-title"]')
  await taskPanel.locator('.panel-heading').getByRole('button', {
    name: 'Criar tarefa',
    exact: true,
  }).click()

  await page.locator('#new-task-title').fill(taskTitle)
  await page.locator('#new-task-description').fill('Created by the Matriz Playwright E2E scenario.')
  await page.locator('form.task-create-form').getByRole('button', {
    name: 'Criar tarefa',
    exact: true,
  }).click()

  let originalFailure: unknown
  let taskDeleted = false
  let taskId: string | undefined

  try {
    const taskRow = page.locator('.task-row').filter({ hasText: taskTitle })
    await expect(taskRow).toBeVisible()

    const taskHref = await taskRow.getAttribute('href')
    if (!taskHref) {
      throw new Error('Created task row did not expose a task route.')
    }

    const taskRouteMatch = /^#\/tasks\/([^/]+)$/.exec(taskHref)
    if (!taskRouteMatch) {
      throw new Error(`Unexpected task route: ${taskHref}`)
    }

    taskId = decodeURIComponent(taskRouteMatch[1])

    await taskRow.click()
    await expect(page).toHaveURL(
      new RegExp(`#/tasks/${taskId.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}$`),
    )
    await expect(page.locator('#task-page-title')).toHaveText(taskTitle)

    page.once('dialog', (dialog) => {
      void dialog.accept()
    })
    await page.getByRole('button', { name: 'Excluir tarefa', exact: true }).click()

    await expect(page).toHaveURL(
      new RegExp(`#/projects/${projectId.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}$`),
    )
    await expect(page.locator('.task-row').filter({ hasText: taskTitle })).toHaveCount(0)
    taskDeleted = true
  } catch (error) {
    originalFailure = error
    throw error
  } finally {
    if (!taskDeleted) {
      try {
        if (taskId) {
          await page.goto(`/#/tasks/${encodeURIComponent(taskId)}`)
          await expect(page.locator('#task-page-title')).toBeVisible()
        } else {
          await page.goto(`/#/projects/${encodeURIComponent(projectId)}`)
          const createdTaskRow = page.locator('.task-row').filter({ hasText: taskTitle })
          await expect(createdTaskRow).toBeVisible()
          await createdTaskRow.click()
          await expect(page.locator('#task-page-title')).toHaveText(taskTitle)
        }

        page.once('dialog', (dialog) => {
          void dialog.accept()
        })
        await page.getByRole('button', { name: 'Excluir tarefa', exact: true }).click()

        await expect(page).toHaveURL(
          new RegExp(`#/projects/${projectId.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}$`),
        )
        await expect(page.locator('.task-row').filter({ hasText: taskTitle })).toHaveCount(0)
      } catch (cleanupError) {
        if (!originalFailure) {
          throw cleanupError
        }
        testInfo.annotations.push({
          type: 'cleanup-failure',
          description: cleanupError instanceof Error
            ? `${cleanupError.name}: ${cleanupError.message}`
            : String(cleanupError),
        })
      }
    }
  }
})
