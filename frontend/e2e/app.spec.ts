import { expect, test, type Page } from '@playwright/test'

const PASSWORD = 'Demo123!'
const unique = (prefix: string) => `${prefix} ${Date.now().toString(36)}`

async function login(page: Page, username: string) {
  await page.goto('/login')
  await page.getByLabel('Username').fill(username)
  await page.getByLabel('Password', { exact: true }).fill(PASSWORD)
  await page.getByRole('button', { name: 'Log in' }).click()
  await expect(page).toHaveURL(/\/tavern$/)
  await expect(page).toHaveTitle('Tavern – Done n Dusted')
}

async function logout(page: Page) {
  await page.getByRole('button', { name: /Log out/ }).click()
  await expect(page.getByRole('link', { name: /Log in/ })).toBeVisible()
}

test('guest opens the XP rules modal and closes it with Esc', async ({ page }) => {
  await page.goto('/')
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Your character grows between sessions')
  await page.getByRole('button', { name: 'How does XP work?' }).click()
  const dialog = page.getByRole('dialog', { name: 'How XP works' })
  await expect(dialog).toBeVisible()
  await expect(dialog.getByRole('cell', { name: 'Legendary' })).toBeVisible()
  await page.keyboard.press('Escape')
  await expect(dialog).toBeHidden()
})

test('a protected page sends a guest to the login form', async ({ page }) => {
  await page.goto('/tavern')
  await expect(page).toHaveURL(/\/login$/)
})

test('a wrong password shows the server error', async ({ page }) => {
  await page.goto('/login')
  await expect(page).toHaveTitle('Log in – Done n Dusted')
  await page.getByLabel('Username').fill('jonas')
  await page.getByLabel('Password', { exact: true }).fill('wrong-password')
  await page.getByRole('button', { name: 'Log in' }).click()
  await expect(page.getByRole('alert')).toContainText('Invalid username or password')
})

test('a player logs progress, the game master approves it, XP shows on the sheet', async ({ page }) => {
  const note = unique('E2E run')
  await login(page, 'jonas')
  await page.goto('/guilds/2/quests/4')
  await page.getByLabel('What did you do?').fill(note)
  await page.getByLabel('Evidence link').fill('https://example.org/e2e.png')
  await page.getByRole('button', { name: 'Log entry' }).click()
  await expect(page.getByText('Entry logged – waiting for the game master to approve it')).toBeVisible()
  const entry = page.getByRole('listitem').filter({ hasText: note })
  await expect(entry.getByText('Awaiting GM')).toBeVisible()
  await logout(page)

  await login(page, 'rokas')
  await page.goto('/guilds/2?tab=review')
  const pending = page.getByRole('listitem').filter({ hasText: note })
  await pending.getByRole('button', { name: 'Review' }).click()
  const dialog = page.getByRole('dialog', { name: 'Review entry' })
  await dialog.getByLabel(/Experience/).fill('60')
  await dialog.getByLabel('Inspiration').fill('1')
  await dialog.getByLabel('Note to the player').fill('Great pace!')
  await dialog.getByRole('button', { name: /Approve and award 60\sXP/ }).click()
  await expect(page.getByText(/jonas earns 60\sXP/)).toBeVisible()

  await page.goto('/guilds/2/members/4')
  const sheet = page.getByRole('article', { name: "jonas's character sheet" })
  await expect(sheet.getByRole('heading', { name: 'jonas' })).toBeVisible()
  await expect(sheet.getByText('Recently approved entries')).toBeVisible()
  await expect(sheet.getByRole('progressbar', { name: /Constitution/ })).toBeVisible()
})

test('the game master creates a grind quest in the modal', async ({ page }) => {
  const title = unique('Swim')
  await login(page, 'rokas')
  await page.goto('/guilds/2')
  await page.getByRole('button', { name: 'New quest' }).click()
  const dialog = page.getByRole('dialog', { name: 'New quest' })
  await dialog.getByLabel('Title').fill(title)
  await dialog.getByLabel('Description').fill('One more kilometre every week')
  await dialog.getByLabel('Stat').selectOption('CON')
  await dialog.getByLabel('Type').selectOption('GRIND')
  await dialog.getByLabel('Legendary').check()
  await dialog.getByLabel(/Target/).fill('12')
  await dialog.getByRole('button', { name: 'Post quest' }).click()
  await expect(page.getByText(`Quest “${title}” posted`)).toBeVisible()
  await expect(page.getByRole('link', { name: new RegExp(title) })).toBeVisible()
})

test('a taken guild name is shown next to the field', async ({ page }) => {
  await login(page, 'jonas')
  await page.getByRole('button', { name: 'Found a guild' }).click()
  const dialog = page.getByRole('dialog', { name: 'Found a guild' })
  await dialog.getByLabel('Name').fill('Iron Vow')
  await dialog.getByRole('button', { name: 'Found guild' }).click()
  await expect(dialog.getByRole('alert')).toContainText(/Iron Vow/)
})

test('a friend sees the public sheet, not the log entries', async ({ page }) => {
  await login(page, 'ieva')
  await page.goto('/friends')
  const friend = page.getByRole('listitem').filter({ hasText: 'jonas' })
  await friend.getByRole('button', { name: 'Sheet' }).click()
  const dialog = page.getByRole('dialog', { name: "jonas's sheet" })
  await expect(dialog.getByText(/Public view/)).toBeVisible()
  await expect(dialog.getByText('Recently approved entries')).toHaveCount(0)
})

test('only an administrator can open the admin page', async ({ page }) => {
  await login(page, 'jonas')
  await expect(page.getByRole('link', { name: /Admin/ })).toHaveCount(0)
  await page.goto('/admin')
  await expect(page).toHaveURL(/\/tavern$/)
  await logout(page)

  await login(page, 'admin')
  await page.getByRole('link', { name: /Admin/ }).click()
  await expect(page.getByRole('cell', { name: 'rokas', exact: true })).toBeVisible()
})

test('an unknown path shows the 404 page', async ({ page }) => {
  await page.goto('/no-such-page')
  await expect(page.getByRole('heading', { name: 'This road leads nowhere' })).toBeVisible()
  await expect(page).toHaveTitle('Page not found – Done n Dusted')
})

test.describe('phone', () => {
  test.use({ viewport: { width: 390, height: 844 } })

  test('the hamburger menu opens and closes after picking a link', async ({ page }) => {
    await login(page, 'rokas')
    const toggle = page.getByRole('button', { name: 'Open menu' })
    await expect(toggle).toBeVisible()
    await expect(page.getByRole('link', { name: /Friends/ })).toBeHidden()
    await toggle.click()
    await page.getByRole('link', { name: /Friends/ }).click()
    await expect(page).toHaveURL(/\/friends$/)
    await expect(page.getByRole('button', { name: 'Open menu' })).toBeVisible()
  })
})
