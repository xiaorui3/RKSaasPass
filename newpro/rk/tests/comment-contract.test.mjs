import { readFileSync } from 'node:fs'
import assert from 'node:assert/strict'

const commentsComponent = readFileSync(new URL('../src/components/NewsComments.vue', import.meta.url), 'utf8')
const newsDetail = readFileSync(new URL('../src/views/NewsDetail.vue', import.meta.url), 'utf8')
const activityDetail = readFileSync(new URL('../src/views/ActivityDetail.vue', import.meta.url), 'utf8')
const userApi = readFileSync(new URL('../src/api/user.js', import.meta.url), 'utf8')

assert.match(
  commentsComponent,
  /from ['"]@\/api\/comments['"]/,
  'NewsComments.vue must use the persisted comments API'
)
assert.match(
  commentsComponent,
  /getComments\s*\(/,
  'NewsComments.vue must fetch existing comments by target'
)
assert.match(
  commentsComponent,
  /createComment\s*\(/,
  'NewsComments.vue must POST new comments instead of only mutating local memory'
)
assert.doesNotMatch(
  commentsComponent,
  /id:\s*Date\.now\(\)/,
  'NewsComments.vue must not create fake local comment ids'
)
assert.match(
  commentsComponent,
  /userAvatar|avatar/,
  'NewsComments.vue must render a user avatar field'
)
assert.match(
  commentsComponent,
  /userName|author/,
  'NewsComments.vue must render the commenter name'
)
assert.match(
  commentsComponent,
  /el-popover[\s\S]*trigger="hover"/,
  'NewsComments.vue must show commenter profile information on avatar/name hover'
)
assert.match(
  commentsComponent,
  /getUsersByAuthIds\(/,
  'NewsComments.vue must load commenter profile details by auth user id'
)
assert.match(
  commentsComponent,
  /comment\.userId|comment\.authUserId/,
  'NewsComments.vue must use the persisted commenter identity'
)
assert.match(
  userApi,
  /export function getUsersByAuthIds\(/,
  'user API must expose internal auth-id lookup for comment profile hover cards'
)
assert.match(
  userApi,
  /\/users\/internal\/by-auth-ids/,
  'user API auth-id lookup must call the existing internal endpoint'
)

assert.match(
  newsDetail,
  /<NewsComments[^>]*target-type="news"/,
  'NewsDetail.vue must render comments for news targets'
)
assert.match(
  newsDetail,
  /:target-id="newsId"/,
  'NewsDetail.vue must pass the news id to comments'
)
assert.match(
  activityDetail,
  /<NewsComments[^>]*target-type="activity"/,
  'ActivityDetail.vue must render comments for activity targets'
)
assert.match(
  activityDetail,
  /:target-id="activityId"/,
  'ActivityDetail.vue must pass the activity id to comments'
)

console.log('comment frontend contract passed')
