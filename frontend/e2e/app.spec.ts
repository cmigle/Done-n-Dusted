import { expect, test, type Page } from '@playwright/test'

const PASSWORD = 'Demo123!'
const unique = (prefix: string) => `${prefix} ${Date.now().toString(36)}`

async function login(page: Page, username: string) {
  await page.goto('/prisijungti')
  await page.getByLabel('Naudotojo vardas').fill(username)
  await page.getByLabel('Slaptažodis', { exact: true }).fill(PASSWORD)
  await page.getByRole('button', { name: 'Prisijungti' }).click()
  await expect(page).toHaveURL(/\/taverna$/)
  await expect(page).toHaveTitle('Taverna – Done n Dusted')
}

async function logout(page: Page) {
  await page.getByRole('button', { name: /Atsijungti/ }).click()
  await expect(page.getByRole('link', { name: /Prisijungti/ })).toBeVisible()
}

test('svecias mato taisykliu modalini langa ir uzdaro ji Esc', async ({ page }) => {
  await page.goto('/')
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Tavo veikėjas auga tarp žaidimų')
  await page.getByRole('button', { name: 'Kaip skaičiuojama patirtis?' }).click()
  const dialog = page.getByRole('dialog', { name: 'Kaip skaičiuojama patirtis' })
  await expect(dialog).toBeVisible()
  await expect(dialog.getByRole('cell', { name: 'Legendinė' })).toBeVisible()
  await page.keyboard.press('Escape')
  await expect(dialog).toBeHidden()
})

test('neprisijungus apsaugotas puslapis veda i prisijungima', async ({ page }) => {
  await page.goto('/taverna')
  await expect(page).toHaveURL(/\/prisijungti$/)
})

test('neteisingas slaptazodis rodo serverio klaida', async ({ page }) => {
  await page.goto('/prisijungti')
  await expect(page).toHaveTitle('Prisijungimas – Done n Dusted')
  await page.getByLabel('Naudotojo vardas').fill('jonas')
  await page.getByLabel('Slaptažodis', { exact: true }).fill('blogas-slaptazodis')
  await page.getByRole('button', { name: 'Prisijungti' }).click()
  await expect(page.getByRole('alert')).toContainText(/Neteisingas naudotojo vardas arba slapta[zž]odis/)
})

test('zaidejas registruoja zygi, DM ji patvirtina, XP matosi korteleje', async ({ page }) => {
  const note = unique('E2E bėgimas')
  await login(page, 'jonas')
  await page.goto('/grupes/2/uzduotys/4')
  await page.getByLabel('Ką padarei?').fill(note)
  await page.getByLabel('Įrodymo nuoroda').fill('https://example.org/e2e.png')
  await page.getByRole('button', { name: 'Užregistruoti' }).click()
  await expect(page.getByText('Įrašas užregistruotas – laukia žaidimo vedėjo patvirtinimo')).toBeVisible()
  const entry = page.getByRole('listitem').filter({ hasText: note })
  await expect(entry.getByText('Laukia vedėjo')).toBeVisible()
  await logout(page)

  await login(page, 'rokas')
  await page.goto('/grupes/2?skirtukas=perziura')
  const pending = page.getByRole('listitem').filter({ hasText: note })
  await pending.getByRole('button', { name: 'Peržiūrėti' }).click()
  const dialog = page.getByRole('dialog', { name: 'Peržiūrėti įrašą' })
  await dialog.getByLabel(/Patirtis/).fill('60')
  await dialog.getByLabel('Įkvėpimas').fill('1')
  await dialog.getByLabel('Pastaba žaidėjui').fill('Puikus tempas!')
  await dialog.getByRole('button', { name: /Patvirtinti ir skirti 60\sPT/ }).click()
  await expect(page.getByText(/jonas gauna 60\sPT/)).toBeVisible()

  await page.goto('/grupes/2/nariai/4')
  const sheet = page.getByRole('article', { name: 'jonas veikėjo kortelė' })
  await expect(sheet.getByRole('heading', { name: 'jonas' })).toBeVisible()
  await expect(sheet.getByText('Paskutiniai patvirtinti žygiai')).toBeVisible()
  await expect(sheet.getByRole('progressbar', { name: /Ištvermė/ })).toBeVisible()
})

test('DM kuria kaupiamaja uzduoti per modalini langa', async ({ page }) => {
  const title = unique('Plaukti')
  await login(page, 'rokas')
  await page.goto('/grupes/2')
  await page.getByRole('button', { name: 'Nauja užduotis' }).click()
  const dialog = page.getByRole('dialog', { name: 'Nauja užduotis' })
  await dialog.getByLabel('Pavadinimas').fill(title)
  await dialog.getByLabel('Aprašymas').fill('Kiekvieną savaitę po kilometrą')
  await dialog.getByLabel('Charakteristika').selectOption('CON')
  await dialog.getByLabel('Tipas').selectOption('GRIND')
  await dialog.getByLabel('Legendinė').check()
  await dialog.getByLabel(/Tikslas/).fill('12')
  await dialog.getByRole('button', { name: 'Paskelbti užduotį' }).click()
  await expect(page.getByText(`Užduotis „${title}“ paskelbta`)).toBeVisible()
  await expect(page.getByRole('link', { name: new RegExp(title) })).toBeVisible()
})

test('uzimtas grupes pavadinimas rodomas prie lauko', async ({ page }) => {
  await login(page, 'jonas')
  await page.getByRole('button', { name: 'Įkurti grupę' }).click()
  const dialog = page.getByRole('dialog', { name: 'Įkurti grupę' })
  await dialog.getByLabel('Pavadinimas').fill('Iron Vow')
  await dialog.getByRole('button', { name: 'Įkurti grupę' }).click()
  await expect(dialog.getByRole('alert')).toContainText(/Iron Vow/)
})

test('draugas mato vieša kortele, o ne irasus', async ({ page }) => {
  await login(page, 'ieva')
  await page.goto('/draugai')
  const friend = page.getByRole('listitem').filter({ hasText: 'jonas' })
  await friend.getByRole('button', { name: 'Kortelė' }).click()
  const dialog = page.getByRole('dialog', { name: 'jonas kortelė' })
  await expect(dialog.getByText(/Viešas vaizdas/)).toBeVisible()
  await expect(dialog.getByText('Paskutiniai patvirtinti žygiai')).toHaveCount(0)
})

test('administravimas pasiekiamas tik administratoriui', async ({ page }) => {
  await login(page, 'jonas')
  await expect(page.getByRole('link', { name: /Administravimas/ })).toHaveCount(0)
  await page.goto('/administravimas')
  await expect(page).toHaveURL(/\/taverna$/)
  await logout(page)

  await login(page, 'admin')
  await page.getByRole('link', { name: /Administravimas/ }).click()
  await expect(page.getByRole('cell', { name: 'rokas', exact: true })).toBeVisible()
})

test('nezinomas kelias rodo 404', async ({ page }) => {
  await page.goto('/nera-tokio')
  await expect(page.getByRole('heading', { name: 'Šis kelias veda niekur' })).toBeVisible()
  await expect(page).toHaveTitle('Puslapis nerastas – Done n Dusted')
})

test.describe('telefonas', () => {
  test.use({ viewport: { width: 390, height: 844 } })

  test('hamburger meniu atsidaro ir uzsidaro pasirinkus punkta', async ({ page }) => {
    await login(page, 'rokas')
    const toggle = page.getByRole('button', { name: 'Atidaryti meniu' })
    await expect(toggle).toBeVisible()
    await expect(page.getByRole('link', { name: /Draugai/ })).toBeHidden()
    await toggle.click()
    await page.getByRole('link', { name: /Draugai/ }).click()
    await expect(page).toHaveURL(/\/draugai$/)
    await expect(page.getByRole('button', { name: 'Atidaryti meniu' })).toBeVisible()
  })
})
