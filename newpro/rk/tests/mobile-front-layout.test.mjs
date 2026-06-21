import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

async function readSource(relativePath) {
  return readFile(new URL(relativePath, import.meta.url), 'utf8')
}

test('notifications page keeps compact mobile card and title sizing', async () => {
  const source = await readSource('../src/views/Notifications.vue')

  assert.match(source, /@media\s*\(max-width:\s*640px\)/, 'notifications page should define a phone-specific breakpoint')
  assert.match(source, /\.notice-title\s*\{[\s\S]*font-size:\s*15px;/, 'notification titles should shrink on small phones')
  assert.match(source, /\.notice-footer\s*\{[\s\S]*font-size:\s*11px;/, 'notification metadata should use denser phone sizing')
})

test('notices page keeps a dedicated mobile stack and denser typography', async () => {
  const source = await readSource('../src/views/Notices.vue')

  assert.match(source, /@media\s*\(max-width:\s*\$breakpoint-md\)/, 'notices page should define tablet/mobile breakpoint rules')
  assert.match(source, /\.notice-card__head\s*\{[\s\S]*flex-direction:\s*column;/, 'notice header should stack on smaller screens')
  assert.match(source, /\.notice-content\s*\{[\s\S]*font-size:\s*14px;/, 'notice content should use smaller mobile/body copy')
})

test('news and works portal pages keep compact phone typography and spacing', async () => {
  const [newsSource, worksSource] = await Promise.all([
    readSource('../src/views/News.vue'),
    readSource('../src/views/WorksFront.vue')
  ])

  assert.match(newsSource, /@media\s*\(max-width:\s*640px\)/, 'news page should define a phone-specific breakpoint')
  assert.match(newsSource, /\.article-card h3\s*\{[\s\S]*font-size:\s*18px;/, 'news titles should shrink on phones')
  assert.match(newsSource, /\.feature-placeholder\s*\{[\s\S]*font-size:\s*38px;/, 'news hero placeholder should be reduced for phones')

  assert.match(worksSource, /@media\s*\(max-width:\s*640px\)/, 'works page should define a phone-specific breakpoint')
  assert.match(worksSource, /\.work-title\s*\{[\s\S]*font-size:\s*18px;/, 'works titles should shrink on phones')
  assert.match(worksSource, /\.spotlight-copy h3\s*\{[\s\S]*font-size:\s*22px;/, 'works spotlight heading should be reduced on phones')
})

test('shared user and admin shells keep smaller phone hero sizing', async () => {
  const [userHubShellSource, userHubPanelSource, adminShellSource] = await Promise.all([
    readSource('../src/components/user-hub/UserHubShell.vue'),
    readSource('../src/components/user-hub/UserHubPanel.vue'),
    readSource('../src/components/admin-shell/AdminModuleShell.vue')
  ])

  assert.match(userHubShellSource, /\.hero-title\s*\{[\s\S]*font-size:\s*22px;/, 'user hub shell should reduce hero title size on phones')
  assert.match(userHubShellSource, /\.stat-value\s*\{[\s\S]*font-size:\s*24px;/, 'user hub shell should reduce stat value size on phones')
  assert.match(userHubPanelSource, /@media\s*\(max-width:\s*640px\)/, 'user hub panel should define a phone breakpoint')
  assert.match(userHubPanelSource, /\.panel-header\s*\{[\s\S]*flex-direction:\s*column;/, 'panel header actions should stack on phones')

  assert.match(adminShellSource, /\.hero-title\s*\{[\s\S]*font-size:\s*22px;/, 'admin shell should reduce hero title size on phones')
  assert.match(adminShellSource, /\.stat-value\s*\{[\s\S]*font-size:\s*24px;/, 'admin shell should reduce stat value size on phones')
})

test('shared portal components keep dedicated phone breakpoints and compact copy sizing', async () => {
  const [portalHeroSource, filterBarSource, sectionHeaderSource, infoCardSource] = await Promise.all([
    readSource('../src/components/portal/PortalHero.vue'),
    readSource('../src/components/portal-content/PortalContentFilterBar.vue'),
    readSource('../src/components/portal/PortalSectionHeader.vue'),
    readSource('../src/components/portal-content/PortalContentInfoCard.vue')
  ])

  assert.match(portalHeroSource, /@media\s*\(max-width:\s*640px\)/, 'portal hero should define a phone breakpoint')
  assert.match(portalHeroSource, /\.hero-main h1\s*\{[\s\S]*font-size:\s*26px;/, 'portal hero heading should shrink on phones')
  assert.match(portalHeroSource, /\.metric-value\s*\{[\s\S]*font-size:\s*24px;/, 'portal hero metrics should shrink on phones')

  assert.match(filterBarSource, /@media\s*\(max-width:\s*640px\)/, 'portal filter bar should define a phone breakpoint')
  assert.match(filterBarSource, /\.filter-copy h2\s*\{[\s\S]*font-size:\s*22px;/, 'portal filter bar heading should shrink on phones')

  assert.match(sectionHeaderSource, /@media\s*\(max-width:\s*640px\)/, 'portal section header should define a phone breakpoint')
  assert.match(sectionHeaderSource, /\.portal-section-header h2\s*\{[\s\S]*font-size:\s*22px;/, 'portal section heading should shrink on phones')

  assert.match(infoCardSource, /@media\s*\(max-width:\s*640px\)/, 'portal info card should define a phone breakpoint')
  assert.match(infoCardSource, /\.info-head h3\s*\{[\s\S]*font-size:\s*18px;/, 'portal info card heading should shrink on phones')
})

test('about and contact pages keep portal test ids and compact mobile sections', async () => {
  const [aboutSource, contactSource] = await Promise.all([
    readSource('../src/views/About.vue'),
    readSource('../src/views/Contact.vue')
  ])

  assert.equal(aboutSource.includes('data-testid="portal-about-page"'), true)
  assert.match(aboutSource, /@media\s*\(max-width:\s*640px\)/, 'about page should define a phone breakpoint')
  assert.match(aboutSource, /\.mission-card h3[\s\S]*font-size:\s*18px;/, 'about mission cards should shrink on phones')

  assert.equal(contactSource.includes('data-testid="portal-contact-page"'), true)
  assert.equal(contactSource.includes('data-testid="portal-contact-form"'), true)
  assert.match(contactSource, /@media\s*\(max-width:\s*640px\)/, 'contact page should define a phone breakpoint')
  assert.match(contactSource, /\.hero-copy h1[\s\S]*font-size:\s*24px;/, 'contact title should shrink on phones')
})
