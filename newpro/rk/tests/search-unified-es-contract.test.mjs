import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import path from 'node:path'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

async function readSource(relativePath) {
  return readFile(path.resolve(__dirname, relativePath), 'utf8')
}

test('frontend global search page must use the unified rk-search api instead of business api aggregation', async () => {
  const page = await readSource('../src/views/SearchResults.vue')
  const api = await readSource('../src/api/search.js')

  assert.match(api, /export function searchGlobal\(/, 'search api must expose searchGlobal')
  assert.match(api, /url:\s*['"`]\/api\/search\/global['"`]/, 'searchGlobal must call /api/search/global through the public gateway route')
  assert.doesNotMatch(api, /url:\s*['"`]\/search\/global['"`]/, 'searchGlobal must not bypass the public /api gateway prefix')

  assert.match(page, /import\s*\{\s*searchGlobal\s*\}\s*from\s*['"]@\/api\/search['"]/, 'SearchResults.vue must import the unified search api')
  assert.match(page, /searchGlobal\(/, 'SearchResults.vue must call searchGlobal')

  for (const forbidden of [
    '@/api/news',
    '@/api/notice',
    '@/api/activity',
    '@/api/competition',
    '@/api/works',
    '@/api/tenant',
    '@/api/member',
    '@/api/alumni',
    '@/api/history',
    'searchNews(',
    'getPublishedNoticeList(',
    'searchActivities(',
    'searchCompetitions(',
    'searchWorks(',
    'getPublicTenantList(',
    'getMembersList(',
    'searchAlumni(',
    'searchEvents(',
    'matchesKeyword(',
    'filterByKeyword('
  ]) {
    assert.equal(page.includes(forbidden), false, `SearchResults.vue must not aggregate business search marker ${forbidden}`)
  }

  for (const marker of [
    'entityType',
    'entityId',
    'tenantId',
    'highlight',
    'route',
    'updatedAt'
  ]) {
    assert.equal(page.includes(marker), true, `SearchResults.vue must consume unified result field ${marker}`)
  }
})

test('rk-search backend must expose a global ES search endpoint and DTOs', async () => {
  const controller = await readSource('../../../rk-search/src/main/java/com/tianji/search/controller/GlobalSearchController.java')
  const service = await readSource('../../../rk-search/src/main/java/com/tianji/search/service/ISearchService.java')
  const impl = await readSource('../../../rk-search/src/main/java/com/tianji/search/service/impl/SearchServiceImpl.java')
  const query = await readSource('../../../rk-search/src/main/java/com/tianji/search/domain/query/GlobalSearchQuery.java')
  const vo = await readSource('../../../rk-search/src/main/java/com/tianji/search/domain/vo/GlobalSearchResultVO.java')

  assert.match(controller, /@RequestMapping\("global"\)/, 'global search endpoint is exposed behind the gateway as /api/search/global')
  assert.match(controller, /queryGlobalSearch\(GlobalSearchQuery query\)/, 'controller must accept GlobalSearchQuery')
  assert.match(service, /PageDTO<GlobalSearchResultVO>\s+queryGlobalSearch\(GlobalSearchQuery query\)/, 'service contract missing global search method')

  for (const marker of [
    'GLOBAL_INDEX_NAME = "rk_global_search"',
    'restClient.search',
    'QueryBuilders.boolQuery()',
    'QueryBuilders.multiMatchQuery',
    'QueryBuilders.termQuery("tenantId"',
    'QueryBuilders.termsQuery("entityType"',
    'new HighlightBuilder()',
    'title',
    'content',
    'summary',
    'route',
    'highlight'
  ]) {
    assert.equal(impl.includes(marker), true, `SearchServiceImpl missing ES global search marker ${marker}`)
  }

  for (const marker of ['keyword', 'entityTypes', 'tenantId', 'includeAllTenants']) {
    assert.equal(query.includes(marker), true, `GlobalSearchQuery missing ${marker}`)
  }

  for (const marker of ['entityType', 'entityId', 'tenantId', 'title', 'summary', 'route', 'updatedAt', 'highlight']) {
    assert.equal(vo.includes(marker), true, `GlobalSearchResultVO missing ${marker}`)
  }
})

test('global search index must be written through rk-search Elasticsearch APIs', async () => {
  const client = await readSource('../../../rk-api/src/main/java/com/tianji/api/client/search/SearchClient.java')
  const dto = await readSource('../../../rk-api/src/main/java/com/tianji/api/dto/search/GlobalSearchDocumentDTO.java')
  const controller = await readSource('../../../rk-search/src/main/java/com/tianji/search/controller/GlobalSearchController.java')
  const service = await readSource('../../../rk-search/src/main/java/com/tianji/search/service/ISearchService.java')
  const impl = await readSource('../../../rk-search/src/main/java/com/tianji/search/service/impl/SearchServiceImpl.java')

  for (const field of [
    'entityType',
    'entityId',
    'tenantId',
    'title',
    'summary',
    'content',
    'tags',
    'route',
    'coverUrl',
    'updatedAt',
    'visible'
  ]) {
    assert.equal(dto.includes(field), true, `GlobalSearchDocumentDTO missing ${field}`)
  }

  for (const marker of [
    'upsertGlobalDocument',
    'upsertGlobalDocuments',
    'deleteGlobalDocument'
  ]) {
    assert.equal(client.includes(marker), true, `SearchClient missing ${marker}`)
    assert.equal(service.includes(marker), true, `ISearchService missing ${marker}`)
  }

  for (const marker of [
    '@PostMapping("/index")',
    '@PostMapping("/index/bulk")',
    '@DeleteMapping("/index/{entityType}/{entityId}")',
    '@RequestParam(value = "tenantId", required = false) Long tenantId',
    'GlobalSearchDocumentDTO'
  ]) {
    assert.equal(controller.includes(marker), true, `GlobalSearchController missing ${marker}`)
  }

  for (const marker of [
    'new IndexRequest(GLOBAL_INDEX_NAME)',
    'new BulkRequest(GLOBAL_INDEX_NAME)',
    'new DeleteRequest(GLOBAL_INDEX_NAME',
    'restClient.index',
    'restClient.bulk',
    'restClient.delete',
    'globalDocumentId',
    'QueryBuilders.termQuery("visible", true)'
  ]) {
    assert.equal(impl.includes(marker), true, `SearchServiceImpl missing ES index marker ${marker}`)
  }
})

test('global search index rebuild must pull business export endpoints and bulk write Elasticsearch', async () => {
  const clientFiles = [
    ['content', '../../../rk-api/src/main/java/com/tianji/api/client/content/ContentSearchDocumentClient.java'],
    ['activity', '../../../rk-api/src/main/java/com/tianji/api/client/activity/ActivitySearchDocumentClient.java'],
    ['user', '../../../rk-api/src/main/java/com/tianji/api/client/user/UserSearchDocumentClient.java'],
    ['message', '../../../rk-api/src/main/java/com/tianji/api/client/message/MessageSearchDocumentClient.java']
  ]

  for (const [name, file] of clientFiles) {
    const source = await readSource(file)
    assert.match(source, /@FeignClient/, `${name} search export client must be a Feign client`)
    assert.match(source, /\/api\/search-documents\/internal\/export/, `${name} client must call the internal search document export endpoint`)
    assert.match(source, /List<GlobalSearchDocumentDTO>\s+exportSearchDocuments/, `${name} client must return unified search document DTOs`)
  }

  const service = await readSource('../../../rk-search/src/main/java/com/tianji/search/service/ISearchService.java')
  const controller = await readSource('../../../rk-search/src/main/java/com/tianji/search/controller/GlobalSearchController.java')
  const impl = await readSource('../../../rk-search/src/main/java/com/tianji/search/service/impl/SearchServiceImpl.java')

  assert.match(service, /GlobalSearchRebuildResultVO\s+rebuildGlobalSearchIndex\(\)/, 'search service must expose rebuild contract')
  assert.match(controller, /@PostMapping\("\/index\/rebuild"\)/, 'rk-search must expose POST /search/global/index/rebuild')

  for (const marker of [
    'contentSearchDocumentClient.exportSearchDocuments()',
    'activitySearchDocumentClient.exportSearchDocuments()',
    'userSearchDocumentClient.exportSearchDocuments()',
    'messageSearchDocumentClient.exportSearchDocuments()',
    'DeleteByQueryRequest',
    'QueryBuilders.matchAllQuery()',
    'upsertGlobalDocuments(documents)'
  ]) {
    assert.equal(impl.includes(marker), true, `SearchServiceImpl rebuild missing marker ${marker}`)
  }

  const exportControllers = [
    ['content', '../../../rk-content/src/main/java/com/tianji/content/controller/SearchDocumentInternalController.java', ['INewsService', 'IWorksService']],
    ['activity', '../../../rk-activity/src/main/java/com/tianji/activity/controller/SearchDocumentInternalController.java', ['IActivityService', 'ICompetitionService']],
    ['user', '../../../rk-user/src/main/java/com/tianji/user/controller/SearchDocumentInternalController.java', ['IRKTenantService', 'IClubMemberService', 'IClubAlumniService', 'IHistoryService']],
    ['message', '../../../rk-message/rk-message-service/src/main/java/com/tianji/message/controller/SearchDocumentInternalController.java', ['INoticeService']]
  ]

  for (const [name, file, markers] of exportControllers) {
    const source = await readSource(file)
    assert.match(source, /@RequestMapping\("\/api\/search-documents\/internal"\)/, `${name} internal export controller must use the agreed internal path`)
    assert.match(source, /@GetMapping\("\/export"\)/, `${name} internal export controller must expose GET /export`)
    assert.match(source, /List<GlobalSearchDocumentDTO>\s+exportSearchDocuments\(\)/, `${name} internal export controller must return unified documents`)
    for (const marker of markers) {
      assert.equal(source.includes(marker), true, `${name} export controller missing service marker ${marker}`)
    }
  }
})

test('business services must push public documents to the unified ES global search index', async () => {
  const files = [
    ['news', '../../../rk-content/src/main/java/com/tianji/content/service/impl/NewsServiceImpl.java', ['syncNewsSearchIndex', 'NEWS', '/news/']],
    ['notice', '../../../rk-message/rk-message-service/src/main/java/com/tianji/message/service/impl/NoticeServiceImpl.java', ['syncNoticeSearchIndex', 'NOTICE', '/notices']],
    ['works', '../../../rk-content/src/main/java/com/tianji/content/service/impl/WorksServiceImpl.java', ['syncWorkSearchIndex', 'WORKS', '/works/']],
    ['activity', '../../../rk-activity/src/main/java/com/tianji/activity/service/impl/ActivityServiceImpl.java', ['syncActivitySearchIndex', 'ACTIVITY', '/activities/']],
    ['competition', '../../../rk-activity/src/main/java/com/tianji/activity/service/impl/CompetitionServiceImpl.java', ['syncCompetitionSearchIndex', 'COMPETITION', '/competition/']],
    ['tenant', '../../../rk-user/src/main/java/com/tianji/user/service/impl/RKTenantServiceImpl.java', ['syncTenantSearchIndex', 'TENANT', '/join']],
    ['member', '../../../rk-user/src/main/java/com/tianji/user/service/impl/ClubMemberServiceImpl.java', ['syncMemberSearchIndex', 'MEMBER', '/alumni']],
    ['alumni', '../../../rk-user/src/main/java/com/tianji/user/service/impl/ClubAlumniServiceImpl.java', ['syncAlumniSearchIndex', 'ALUMNI', '/alumni']],
    ['history', '../../../rk-user/src/main/java/com/tianji/user/service/impl/HistoryServiceImpl.java', ['syncHistorySearchIndex', 'HISTORY', '/history']]
  ]

  for (const [name, file, markers] of files) {
    const source = await readSource(file)
    assert.equal(source.includes('SearchClient'), true, `${name} service must inject SearchClient`)
    assert.equal(source.includes('GlobalSearchDocumentDTO'), true, `${name} service must build GlobalSearchDocumentDTO`)
    assert.equal(source.includes('upsertGlobalDocument'), true, `${name} service must upsert ES index documents`)
    assert.equal(source.includes('deleteGlobalDocument'), true, `${name} service must delete stale ES index documents`)
    for (const marker of markers) {
      assert.equal(source.includes(marker), true, `${name} service missing search sync marker ${marker}`)
    }
  }
})
