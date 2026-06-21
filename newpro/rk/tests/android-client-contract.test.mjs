import test from 'node:test'
import assert from 'node:assert/strict'
import { access, readFile, stat } from 'node:fs/promises'

const androidRoot = new URL('../../../android-client/rk-club-android/', import.meta.url)
const repoRoot = new URL('../../../', import.meta.url)

async function assertExists(relativePath) {
  await access(new URL(relativePath, androidRoot))
}

async function readAndroidSource(relativePath) {
  return readFile(new URL(relativePath, androidRoot), 'utf8')
}

async function readRepoSource(relativePath) {
  return readFile(new URL(relativePath, repoRoot), 'utf8')
}

test('android client project should expose expected files and APK output convention', async () => {
  await assertExists('app/src/main/AndroidManifest.xml')
  await assertExists('app/src/main/java/com/rkclub/app/MainActivity.java')
  await assertExists('app/src/main/java/com/rkclub/app/ApkProvider.java')
  await assertExists('app/src/main/res/values/strings.xml')
  await assertExists('app/src/main/res/values/colors.xml')
  await assertExists('app/src/main/res/drawable/brand_mark.xml')
  await assertExists('app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml')
  await assertExists('build-apk.ps1')

  const buildScript = await readAndroidSource('build-apk.ps1')
  assert.equal(buildScript.includes('dist/rk-club-debug.apk'), true)
})

test('android client should publish as 楂樻牎淇变箰閮?with generated bitmap launcher icons', async () => {
  const strings = await readAndroidSource('app/src/main/res/values/strings.xml')
  const manifest = await readAndroidSource('app/src/main/AndroidManifest.xml')
  const icon = await stat(new URL('app/src/main/res/drawable-nodpi/app_icon.png', androidRoot))
  const roundIcon = await stat(new URL('app/src/main/res/drawable-nodpi/app_icon_round.png', androidRoot))

  assert.match(strings, /<string name="app_name">楂樻牎淇变箰閮?\/string>/)
  assert.match(manifest, /android:icon="@drawable\/app_icon"/)
  assert.match(manifest, /android:roundIcon="@drawable\/app_icon_round"/)
  assert.ok(icon.size > 100 * 1024)
  assert.ok(roundIcon.size > 100 * 1024)
})

test('android client should include native bottom navigation and readable Chinese labels', async () => {
  const source = await readAndroidSource('app/src/main/java/com/rkclub/app/MainActivity.java')
  const strings = await readAndroidSource('app/src/main/res/values/strings.xml')
  const combined = `${source}\n${strings}`
  const mojibakePattern = new RegExp([
    [0xfffd],
    [0x7ec0, 0x60e7, 0x6d1f],
    [0x68e3, 0x682d, 0x3009],
    [0x5a11, 0x581f, 0x4f05],
    [0x93b4, 0x621c],
    [0x6d93, 0xe043, 0x6c49],
    [0x7eef, 0x8364, 0x7cba],
    [0x5bb8, 0x30e4, 0x7d94],
    [0x9427, 0x8bf2, 0x7d8d],
    [0x7ec9, 0x71b8, 0x57db],
    [0x5a32, 0x8bf2, 0x59e9],
    [0x59e3, 0x65c7, 0x798c],
    [0x74a7, 0x6d97, 0x7c28],
    [0x93c8, 0xe046, 0x6ae5],
    [0x935a, 0x5ea1, 0x5f74]
  ].map((codes) => String.fromCodePoint(...codes)).join('|'))

  assert.match(combined, /棣栭〉/)
  assert.match(combined, /绀惧洟/)
  assert.match(combined, /娑堟伅/)
  assert.match(combined, /鏇村/)
  assert.match(combined, /鎴戠殑/)
  assert.match(combined, /涓汉涓績|Profile Center/)
  assert.match(combined, /绀惧洟绉诲姩宸ヤ綔鍙?)
  assert.equal(mojibakePattern.test(combined), false)
  assert.equal(source.includes('showHome'), true)
  assert.equal(source.includes('showContent'), true)
  assert.equal(source.includes('showMessages'), true)
  assert.equal(source.includes('showMyServices'), true)
  assert.equal(source.includes('showProfile'), true)
})

test('android client should use a productized mobile home shell instead of text-heavy shortcuts', async () => {
  const source = await readAndroidSource('app/src/main/java/com/rkclub/app/MainActivity.java')

  assert.match(source, /private enum NavTab/)
  assert.match(source, /private NavTab activeTab/)
  assert.match(source, /navItem\(/)
  assert.match(source, /renderBottomNavigation\(/)
  assert.match(source, /quickActionGrid\(/)
  assert.match(source, /quickAction\(/)
  assert.match(source, /iconBadge\(/)
  assert.match(source, /avatarView\(/)
  assert.match(source, /鐑棬娲诲姩/)
  assert.match(source, /鎴戠殑瀛﹀垎/)
  assert.match(source, /鎺掕姒?)
  assert.match(source, /鍙戠幇鏇村绀惧洟/)
  assert.equal(source.includes('new Button(this);\n        button.setText(label);\n        button.setTextColor(PRIMARY);'), false)
})

test('android ui redesign should follow open-source material app references without new build dependencies', async () => {
  const source = await readAndroidSource('app/src/main/java/com/rkclub/app/MainActivity.java')
  const settings = await readAndroidSource('settings.gradle')
  const windowsBuild = await readAndroidSource('build-apk.ps1')
  const linuxBuild = await readAndroidSource('build-apk.sh')
  const buildInputs = `${settings}\n${windowsBuild}\n${linuxBuild}`

  assert.match(source, /mobileBannerCard\(/)
  assert.match(source, /mobileQuickGrid\(/)
  assert.match(source, /mobileSearchBar\(/)
  assert.match(source, /feedCard\(/)
  assert.match(source, /showHome\(\)[\s\S]*mobileSearchBar\(/)
  assert.match(source, /showHome\(\)[\s\S]*mobileBannerCard\(/)
  assert.match(source, /showHome\(\)[\s\S]*mobileQuickGrid\(/)
  assert.match(source, /renderBottomNavigation\(\)[\s\S]*dp\(60\)/)
  assert.match(source, /nav\.setElevation\(dp\(8\)\)/)
  assert.equal(source.includes('implementation "com.github.xuexiangjys:XUI'), false)
  assert.equal(source.includes('implementation "com.github.ibrahimsn98:SmoothBottomBar'), false)
  assert.equal(buildInputs.includes('com.github.xuexiangjys:XUI'), false)
  assert.equal(buildInputs.includes('SmoothBottomBar'), false)
})

test('android client should persist and restore the 15-day mobile session after process restart', async () => {
  const source = await readAndroidSource('app/src/main/java/com/rkclub/app/MainActivity.java')

  assert.match(source, /import android\.content\.SharedPreferences;/)
  assert.match(source, /AUTH_PREFS/)
  assert.match(source, /KEY_TOKEN/)
  assert.match(source, /KEY_REFRESH_TOKEN/)
  assert.match(source, /KEY_TENANT_ID/)
  assert.match(source, /KEY_TENANT_NAME/)
  assert.match(source, /KEY_DISPLAY_NAME/)
  assert.match(source, /KEY_ROLE_ID/)
  assert.match(source, /restoreAuthSession\(\)/)
  assert.match(source, /persistAuthSession\(\)/)
  assert.match(source, /clearAuthSession\(\)/)
  assert.match(source, /onCreate\(Bundle savedInstanceState\)[\s\S]*restoreAuthSession\(\)[\s\S]*showShell\(\)/)
  assert.match(source, /parseLogin\(String response, String username\)[\s\S]*persistAuthSession\(\)/)
  assert.match(source, /refreshAuthSession\(\)[\s\S]*persistAuthSession\(\)/)
})

test('android client should call login and notification APIs', async () => {
  const source = await readAndroidSource('app/src/main/java/com/rkclub/app/MainActivity.java')

  assert.equal(source.includes('/auth/login'), true)
  assert.equal(source.includes('/api/notifications'), true)
  assert.equal(source.includes('/api/news'), true)
  assert.equal(source.includes('/api/activity/my'), true)
  assert.equal(source.includes('/api/credit/my/summary'), true)
  assert.equal(source.includes('HttpURLConnection'), true)
  assert.equal(source.includes('->'), false)
  assert.equal(source.includes('::'), false)
})

test('frontend should expose mobile release admin API and MinIO upload target', async () => {
  const uploadTargets = await readRepoSource('newpro/rk/src/utils/uploadTargets.js')
  const api = await readRepoSource('newpro/rk/src/api/mobile-release.js')
  const notificationCenter = await readRepoSource('newpro/rk/src/views/admin/operation/NotificationCenter.vue')

  assert.match(uploadTargets, /'mobile-apk'/)
  assert.match(uploadTargets, /bizType: 'mobile-apk'/)
  assert.match(api, /getMobileReleaseConfig/)
  assert.match(api, /saveMobileReleaseConfig/)
  assert.match(api, /getLatestMobileRelease/)
  assert.match(api, /getMobileReleaseHistory/)
  assert.match(api, /withdrawMobileRelease/)
  assert.match(api, /rollbackMobileRelease/)
  assert.match(api, /autoBuildMobileRelease/)
  assert.match(api, /\/api\/admin\/mobile\/releases\/config/)
  assert.match(api, /\/api\/mobile\/releases\/latest/)
  assert.match(api, /\/api\/admin\/mobile\/releases\/history/)
  assert.match(api, /\/api\/admin\/mobile\/releases\/withdraw/)
  assert.match(api, /\/api\/admin\/mobile\/releases\/rollback/)
  assert.match(api, /\/api\/admin\/mobile\/releases\/auto-build/)
  assert.match(notificationCenter, /mobileReleaseForm/)
  assert.match(notificationCenter, /uploadManagedFile/)
  assert.match(notificationCenter, /mobileReleaseHistory/)
  assert.match(notificationCenter, /handleAutoBuildMobileRelease/)
  assert.match(notificationCenter, /handleWithdrawMobileRelease/)
  assert.match(notificationCenter, /handleRollbackMobileRelease/)
  assert.match(notificationCenter, /鍙戝竷鏉ユ簮/)
})

test('android client should default to HTTPS tenant directory and update endpoint', async () => {
  const source = await readAndroidSource('app/src/main/java/com/rkclub/app/MainActivity.java')
  const manifest = await readAndroidSource('app/src/main/AndroidManifest.xml')

  assert.equal(source.includes('https://example.com'), true)
  assert.equal(source.includes('/tenants/list'), true)
  assert.equal(source.includes('/api/mobile/releases/latest'), true)
  assert.equal(source.includes('downloadApk'), true)
  assert.equal(source.includes('ACTION_VIEW'), true)
  assert.equal(manifest.includes('android.permission.REQUEST_INSTALL_PACKAGES'), true)
  assert.equal(manifest.includes('com.rkclub.app.ApkProvider'), true)
})

test('android login should use the public URL without exposing a manual service address field', async () => {
  const source = await readAndroidSource('app/src/main/java/com/rkclub/app/MainActivity.java')

  assert.equal(source.includes('private static final String DEFAULT_BASE_URL = "https://example.com"'), true)
  assert.equal(source.includes('baseUrlInput = input('), false)
  assert.equal(source.includes('loginCard.addView(baseUrlInput)'), false)
  assert.equal(source.includes('normalizeBaseUrl(baseUrlInput'), false)
  assert.match(source, /baseUrl\s*=\s*normalizeBaseUrl\(DEFAULT_BASE_URL\)/)
})

test('android login should auto-select a tenant after loading the tenant list', async () => {
  const source = await readAndroidSource('app/src/main/java/com/rkclub/app/MainActivity.java')

  assert.match(source, /selectDefaultTenant\(tenants\)/)
  assert.match(source, /DEFAULT_TENANT_ID\s*=\s*"1"/)
  assert.match(source, /tenantSelectorShownOnLogin/)
  assert.match(source, /showTenantSelector\(tenantDirectory\)/)
  assert.equal(source.includes('tenantInput = input('), false)
  assert.equal(source.includes('绉熸埛 ID'), false)
  assert.match(source, /tenantDisplay/)
  assert.match(source, /showTenantSelector\(/)
  assert.match(source, /AlertDialog\.Builder/)
  assert.match(source, /tenantName\s*=\s*tenant\.optString\("tenantName"/)
})

test('android tenant page should show current account tenant memberships', async () => {
  const source = await readAndroidSource('app/src/main/java/com/rkclub/app/MainActivity.java')

  assert.match(source, /\/auth\/switchable-tenants/)
  assert.match(source, /renderAccountTenantCards/)
  assert.match(source, /renderAccountTenantCard/)
  assert.match(source, /isCurrentTenant/)
  assert.match(source, /tenantNameFromDirectory/)
})

test('android login and tenant switch should support email-code multi-role identities', async () => {
  const source = await readAndroidSource('app/src/main/java/com/rkclub/app/MainActivity.java')

  assert.match(source, /emailInput/)
  assert.match(source, /emailCodeInput/)
  assert.match(source, /sendEmailLoginCode\(/)
  assert.match(source, /emailCodeLogin\(/)
  assert.match(source, /\/api\/email-verification\/send/)
  assert.match(source, /\/auth\/email-login\/prepare/)
  assert.match(source, /\/auth\/email-login\/confirm/)
  assert.match(source, /showEmailLoginCandidateDialog\(/)
  assert.match(source, /confirmEmailLogin\((?:final\s+)?String loginTicket,\s*(?:final\s+)?JSONObject candidate\)/)
  assert.match(source, /candidate\.optString\("roleId"/)
  assert.match(source, /roleId/)
  assert.match(source, /switchAccountTenant\((?:final\s+)?JSONObject item\)/)
  assert.match(source, /\/auth\/switch-tenant\/" \+ urlEncode\(itemTenantId\)/)
  assert.match(source, /\?roleId=" \+ urlEncode\(itemRoleId\)/)
  assert.match(source, /isCurrentTenant\(itemTenantId,\s*itemRoleId\)/)
})

test('android activity registration should verify business success and refresh my services', async () => {
  const source = await readAndroidSource('app/src/main/java/com/rkclub/app/MainActivity.java')

  assert.match(source, /ensureApiSuccess/)
  assert.match(source, /request\("POST", "\/api\/activity\/" \+ urlEncode\(id\) \+ "\/register"/)
  assert.match(source, /ensureApiSuccess\(response,\s*"娲诲姩鎶ュ悕澶辫触"\)/)
  assert.match(source, /showMyServices\(\)/)
  assert.match(source, /\/api\/activity\/my\/registrations/)
  assert.match(source, /activityName/)
})

test('android command-line build should stamp release version 0.0.43 into the APK', async () => {
  const buildScript = await readAndroidSource('build-apk.ps1')
  const linuxBuildScript = await readAndroidSource('build-apk.sh')

  assert.match(buildScript, /\$VersionName\s*=\s*'0\.0\.43'/)
  assert.match(buildScript, /\[int\]\$VersionCode\s*=\s*44/)
  assert.match(linuxBuildScript, /VERSION_NAME="0\.0\.43"/)
  assert.match(linuxBuildScript, /VERSION_CODE="44"/)
  assert.match(buildScript, /'--version-name', \$VersionName/)
  assert.match(buildScript, /'--version-code', \$VersionCode/)
})

test('android login should reject missing token and normalize bearer tokens', async () => {
  const source = await readAndroidSource('app/src/main/java/com/rkclub/app/MainActivity.java')

  assert.match(source, /private String extractLoginToken\(/)
  assert.match(source, /鐧诲綍鍝嶅簲缂哄皯浠ょ墝/)
  assert.equal(source.includes('replaceFirst("(?i)^Bearer\\\\s+", "")'), true)
  assert.match(source, /if \(authorized && token\.isEmpty\(\)\)/)
  assert.match(source, /throw new IOException\("鐧诲綍宸插け鏁堬紝璇烽噸鏂扮櫥褰?\)/)
  assert.match(source, /connection\.setRequestProperty\("Authorization", "Bearer " \+ token\)/)
})

test('android update download should render a real progress bar and percent text', async () => {
  const source = await readAndroidSource('app/src/main/java/com/rkclub/app/MainActivity.java')

  assert.match(source, /import android\.widget\.ProgressBar;/)
  assert.match(source, /interface DownloadProgressCallback/)
  assert.match(source, /new ProgressBar\(this, null, android\.R\.attr\.progressBarStyleHorizontal\)/)
  assert.match(source, /progressBar\.setMax\(100\)/)
  assert.match(source, /connection\.getContentLengthLong\(\)/)
  assert.match(source, /onDownloadProgress\(downloaded, total\)/)
  assert.match(source, /progressBar\.setProgress\(percent\)/)
  assert.match(source, /formatBytes\(downloaded\)/)
})

test('android notification upgrade cards should expose metadata download action', async () => {
  const source = await readAndroidSource('app/src/main/java/com/rkclub/app/MainActivity.java')

  assert.match(source, /private JSONObject notificationMetadata\(/)
  assert.match(source, /private String notificationValue\(/)
  assert.match(source, /String downloadUrl = notificationValue\(item, metadata, "downloadUrl"\)/)
  assert.match(source, /Button downloadButton = primaryButton\("涓嬭浇骞跺畨瑁?\)/)
  assert.match(source, /final String finalDownloadUrl = downloadUrl/)
  assert.match(source, /downloadApkFromDialog\(finalDownloadUrl, finalForceUpgrade\)/)
  assert.match(source, /metadata\.optString\(key, ""\)/)
})

test('android auto build should use the single canonical android project from the current Gitee branch', async () => {
  const service = await readRepoSource('rk-user/src/main/java/com/tianji/user/service/impl/MobileReleaseServiceImpl.java')

  assert.equal(service.includes('@Value("${rk.mobile.release.android-project:android-client/rk-club-android}")'), true)
  assert.equal(service.includes('android-client/rk-club-android'), true)
  assert.equal(service.includes('rk.mobile.release.k8s.git-url:https://gitee.com'), true)
  assert.equal(service.includes('http://rk-gogs:3000/tjxt/rk-web.git'), false)
  assert.match(service, /cd android-client\/rk-club-android/)
  assert.match(service, /git rev-parse --short=12 HEAD/)
  assert.match(service, /__RK_BUILD_PROJECT__=android-client\/rk-club-android/)
  assert.match(service, /__RK_GIT_URL__=\$\{RK_GIT_URL\}/)
})

test('android detail comments should load publicly and only require login when posting', async () => {
  const source = await readAndroidSource('app/src/main/java/com/rkclub/app/MainActivity.java')
  const gatewayFilter = await readRepoSource('rk-gateway/src/main/java/com/tianji/gateway/filter/AccountAuthFilter.java')
  const gatewayAuth = await readRepoSource('rk-gateway/src/main/java/com/tianji/gateway/config/AuthProperties.java')

  assert.match(source, /renderCommentsSection\(/)
  assert.match(source, /loadComments\("news", id, body\)/)
  assert.match(source, /loadComments\("activity", id, body\)/)
  assert.match(source, /requestCommentList\(commentPath\)/)
  assert.match(source, /request\("GET", commentPath, null, false\)/)
  assert.match(source, /isHttpUnauthorized\(/)
  assert.match(source, /comment public request returned 401, retrying with current mobile token/)
  assert.match(source, /request\("GET", commentPath, null, true\)/)
  assert.match(source, /request\("POST", "\/api\/comments", payload, true\)/)
  assert.match(gatewayAuth, /excludePath\.add\("GET:\/api\/comments"\)/)
  assert.match(gatewayFilter, /if\s*\(\s*isExcludePath\(antPath\)\s*\)\s*\{/)
  assert.equal(
    gatewayFilter.split(/\r?\n/).some(line => line.includes('//') && line.includes('if(isExcludePath(antPath))')),
    false,
    'GET /api/comments whitelist check must not be hidden in a line comment'
  )
  assert.match(source, /ensureApiSuccess\(response,\s*"璇勮鍙戝竷澶辫触"\)/)
  assert.match(source, /璇勮鍔犺浇澶辫触/)
  assert.match(source, /璇峰厛鐧诲綍鍚庡啀璇勮/)
})

test('android comment posting should lock the submit button against duplicate taps', async () => {
  const source = await readAndroidSource('app/src/main/java/com/rkclub/app/MainActivity.java')

  assert.match(source, /private final HashSet<String> pendingCommentSubmissions/)
  assert.match(source, /private boolean markCommentSubmitting\(String commentKey\)/)
  assert.match(source, /private void finishCommentSubmission\(String commentKey,\s*Button submit\)/)
  assert.match(source, /postComment\(targetType,\s*targetId,\s*contentText,\s*section,\s*input,\s*submit\)/)
  assert.match(source, /submit\.setEnabled\(false\)/)
  assert.match(source, /submit\.setText\("鍙戝竷涓璡.\.\."\)/)
  assert.match(source, /finishCommentSubmission\(commentKey,\s*submit\)/)
})

test('android hardware back should return to the previous in-app screen before exiting', async () => {
  const source = await readAndroidSource('app/src/main/java/com/rkclub/app/MainActivity.java')

  assert.match(source, /private interface ScreenAction/)
  assert.match(source, /private final ArrayList<ScreenAction> screenBackStack/)
  assert.match(source, /private void setCurrentScreen\(ScreenAction action,\s*boolean clearHistory\)/)
  assert.match(source, /private void openMainScreen\(ScreenAction action\)/)
  assert.match(source, /private void openChildScreen\(ScreenAction action\)/)
  assert.match(source, /@Override\s+public void onBackPressed\(\)/)
  assert.match(source, /screenBackStack\.remove\(screenBackStack\.size\(\) - 1\)/)
  assert.match(source, /navigatingBack = true/)
  assert.match(source, /showNewsDetail\(finalId\)/)
  assert.match(source, /showActivityDetail\(finalId\)/)
  assert.match(source, /showCompetitionDetail\(finalId\)/)
})

test('android visible UI text should be normal Chinese, not mojibake', async () => {
  const source = await readAndroidSource('app/src/main/java/com/rkclub/app/MainActivity.java')
  const strings = await readAndroidSource('app/src/main/res/values/strings.xml')
  const combined = `${source}\n${strings}`

  for (const label of ['棣栭〉', '绀惧洟', '娑堟伅', '鏇村', '鎴戠殑', '涓汉涓績', '绀惧洟绉诲姩宸ヤ綔鍙?, '鐑棬娲诲姩', '鎴戠殑瀛﹀垎', '鎺掕姒?, '鍙戠幇鏇村绀惧洟']) {
    assert.equal(combined.includes(label), true, `missing readable label: ${label}`)
  }

  const mojibakeMarkers = ['缁夎', '濞戝牊', '閹存垹', '閸愬懎', '閺堝秴', '瀹搞儰', '閺備即', '濞茶', '濮ｆ棁', '缁狅紕', '閻ц', '闁氨', '閺嗗倹']
  for (const marker of mojibakeMarkers) {
    assert.equal(combined.includes(marker), false, `mojibake marker found: ${marker}`)
  }
})

test('android update flow should prompt on login and block navigation during force upgrade', async () => {
  const source = await readAndroidSource('app/src/main/java/com/rkclub/app/MainActivity.java')

  assert.equal(source.includes('showBlockingUpdateDialog'), true)
  assert.equal(source.includes('forceUpgradeActive'), true)
  assert.equal(source.includes('updateCheckPending'), true)
  assert.equal(source.includes('enforceForceUpgrade'), true)
  assert.equal(source.includes('enforceUpdateGate'), true)
  assert.equal(source.includes('setCancelable(!forceUpgrade)'), true)
  assert.equal(source.includes('forceUpgrade && (hasUpdate || !'), false)
  assert.equal(source.includes('forceUpgrade && hasUpdate'), true)
  assert.equal(source.includes('selectedTenantId.isEmpty() ? "1"'), false)
  assert.match(source, /tenantId\s*=\s*selectedTenantId\.isEmpty\(\)\s*\?\s*DEFAULT_TENANT_ID\s*:\s*selectedTenantId/)
  assert.match(source, /private boolean requireSuccessfulUpdateCheckBeforeLogin\(\)/)
  assert.match(source, /showLogin\(\)[\s\S]*latestRelease = null[\s\S]*checkLatestRelease\(true\)/)
  assert.match(source, /login\(\)[\s\S]*enforceUpdateGate\(\)/)
  assert.match(source, /login\(\)[\s\S]*requireSuccessfulUpdateCheckBeforeLogin\(\)/)
  assert.match(source, /navItem[\s\S]*enforceUpdateGate\(\)/)
  assert.match(source, /shortcut[\s\S]*enforceUpdateGate\(\)/)
  assert.match(source, /chooseTenant\.setOnClickListener[\s\S]*enforceUpdateGate\(\)/)
  assert.match(source, /refreshTenants\.setOnClickListener[\s\S]*enforceUpdateGate\(\)/)
  assert.match(source, /installApk\(File apkFile\)[\s\S]*content != null/)
})

test('android client should include service hub with activity and club directory pages', async () => {
  const source = await readAndroidSource('app/src/main/java/com/rkclub/app/MainActivity.java')

  assert.equal(source.includes('showServiceHub'), true)
  assert.equal(source.includes('showActivityCenter'), true)
  assert.equal(source.includes('showClubDirectory'), true)
  assert.equal(source.includes('renderActivityCenter'), true)
  assert.equal(source.includes('renderClubDirectory'), true)
  assert.equal(source.includes('/api/activity/list'), true)
  assert.equal(source.includes('/api/members/list'), true)
  assert.equal(source.includes('/api/alumni/list'), true)
})

test('android content hub should expose news, activity, and competition centers', async () => {
  const source = await readAndroidSource('app/src/main/java/com/rkclub/app/MainActivity.java')

  assert.equal(source.includes('showNewsCenter'), true)
  assert.equal(source.includes('showActivityHighlights'), true)
  assert.equal(source.includes('showCompetitionCenter'), true)
  assert.equal(source.includes('renderNewsCenter'), true)
  assert.equal(source.includes('renderActivityHighlights'), true)
  assert.equal(source.includes('renderCompetitionCenter'), true)
  assert.equal(source.includes('/api/news/latest?limit=20'), true)
  assert.equal(source.includes('/api/activity/list'), true)
  assert.equal(source.includes('/api/activity/status/'), true)
  assert.equal(source.includes('/api/competition/status/'), true)
  assert.equal(source.includes('/api/competition/published'), true)
  assert.equal(source.includes('R.drawable.hero_mobile_news'), true)
  assert.equal(source.includes('R.drawable.hero_mobile_activity'), true)
  assert.equal(source.includes('R.drawable.hero_mobile_competition'), true)
})

test('android content lists should open full details and expose activity competition calendar', async () => {
  const source = await readAndroidSource('app/src/main/java/com/rkclub/app/MainActivity.java')

  assert.equal(source.includes('showNewsDetail'), true)
  assert.equal(source.includes('showActivityDetail'), true)
  assert.equal(source.includes('showCompetitionDetail'), true)
  assert.match(source, /request\("GET", "\/api\/news\/" \+ urlEncode\(id\)/)
  assert.match(source, /request\("GET", "\/api\/activity\/" \+ urlEncode\(id\)/)
  assert.match(source, /request\("GET", "\/api\/competition\/" \+ urlEncode\(id\)/)
  assert.match(source, /setOnClickListener[\s\S]*showNewsDetail\(itemId\)/)
  assert.match(source, /setOnClickListener[\s\S]*showActivityDetail\(itemId\)/)
  assert.match(source, /setOnClickListener[\s\S]*showCompetitionDetail\(itemId\)/)
  assert.equal(source.includes('showContentCalendar'), true)
  assert.equal(source.includes('renderContentCalendar'), true)
  assert.equal(source.includes('renderCalendarAgenda'), true)
  assert.match(source, /\/api\/activity\/list/)
  assert.match(source, /\/api\/competition\/published/)
})

test('android client should use a polished native mobile visual system', async () => {
  const source = await readAndroidSource('app/src/main/java/com/rkclub/app/MainActivity.java')

  assert.match(source, /private static final int PAGE_BACKGROUND/)
  assert.match(source, /private static final int SURFACE_ALT/)
  assert.match(source, /private static final int BORDER/)
  assert.equal(source.includes('metricPill('), true)
  assert.equal(source.includes('contentCard('), true)
  assert.equal(source.includes('tag('), true)
  assert.equal(source.includes('topBar('), true)
  assert.match(source, /new LinearLayout\.LayoutParams\(-1, dp\(60\)\)/)
  assert.match(source, /roundStroke\(Color\.rgb\(235, 243, 255\)/)
  assert.equal(source.includes('card().setPadding(dp(18), dp(18), dp(18), dp(18));'), false)
  assert.equal(source.includes('layout.setBackground(roundRect(SURFACE, dp(18)))'), false)
})

test('android admin workbench should expose read-only backend operations summaries', async () => {
  const source = await readAndroidSource('app/src/main/java/com/rkclub/app/MainActivity.java')

  assert.equal(source.includes('showAdminWorkbench'), true)
  assert.equal(source.includes('renderAdminWorkbench'), true)
  assert.equal(source.includes('/admin/ops/monitoring/overview'), true)
  assert.equal(source.includes('/admin/ops/traffic/overview'), true)
  assert.equal(source.includes('/admin/ops/traffic/online-users'), true)
  assert.equal(source.includes('/api/admin/notifications/overview'), true)
  assert.equal(source.includes('R.drawable.hero_mobile_admin'), true)
})

test('android client should ship richer generated mobile visual assets', async () => {
  const assets = [
    'app/src/main/res/drawable-nodpi/hero_mobile_news.png',
    'app/src/main/res/drawable-nodpi/hero_mobile_activity.png',
    'app/src/main/res/drawable-nodpi/hero_mobile_competition.png',
    'app/src/main/res/drawable-nodpi/hero_mobile_admin.png'
  ]

  for (const assetPath of assets) {
    const asset = await stat(new URL(assetPath, androidRoot))
    assert.ok(asset.size > 50 * 1024, `${assetPath} should be a real PNG asset`)
  }
})

test('frontend home should reference generated content visuals while admin dashboard keeps its existing ops visual', async () => {
  const home = await readRepoSource('newpro/rk/src/views/Home.vue')
  const dashboard = await readRepoSource('newpro/rk/src/views/admin/Dashboard.vue')
  const visuals = [
    'newpro/rk/public/assets/visuals/rk-content-news.png',
    'newpro/rk/public/assets/visuals/rk-activity-action.png',
    'newpro/rk/public/assets/visuals/rk-competition-arena.png',
    'newpro/rk/public/assets/visuals/rk-admin-ops.png'
  ]

  for (const visualPath of visuals) {
    const asset = await stat(new URL(visualPath, repoRoot))
    assert.ok(asset.size > 80 * 1024, `${visualPath} should be a generated visual asset`)
  }

  assert.equal(home.includes('/assets/visuals/rk-content-news.png'), true)
  assert.equal(home.includes('/assets/visuals/rk-activity-action.png'), true)
  assert.equal(home.includes('/assets/visuals/rk-competition-arena.png'), true)
  assert.equal(dashboard.includes('/assets/visuals/rk-admin-ops.png'), true)
  assert.equal(dashboard.includes('/assets/visuals/rk-admin-content-ops.png'), false)
})

test('gateway should route public mobile release API to rk-user', async () => {
  const gatewayConfig = await readRepoSource('rk-gateway/src/main/resources/bootstrap.yml')
  const userConfig = await readRepoSource('rk-user/src/main/resources/bootstrap.yml')
  const gatewayAuthProperties = await readRepoSource('rk-gateway/src/main/java/com/tianji/gateway/config/AuthProperties.java')
  const userAuthCustomization = await readRepoSource('rk-user/src/main/java/com/tianji/user/config/ResourceAuthCustomizationConfig.java')
  const frontendNginx = await readRepoSource('ci/docker/frontend/default.conf')
  const localNginx = await readRepoSource('newpro/rk/nginx.conf')

  assert.match(gatewayConfig, /\/api\/mobile\/\*\*/)
  assert.match(gatewayConfig, /RewritePath=\/api\/mobile\/\(\?<segment>\.\*\), \/mobile\/\$\{segment\}/)
  assert.match(gatewayConfig, /Path=\/mobile\/\*\*/)
  assert.match(gatewayConfig, /GET:\/mobile\/releases\/latest/)
  assert.match(gatewayConfig, /GET:\/api\/mobile\/releases\/latest/)
  assert.match(gatewayAuthProperties, /GET:\/mobile\/releases\/latest/)
  assert.match(gatewayAuthProperties, /GET:\/api\/mobile\/releases\/latest/)
  assert.match(userConfig, /\/mobile\/releases\/latest/)
  assert.match(userAuthCustomization, /\/mobile\/releases\/latest/)
  assert.match(frontendNginx, /location \/mobile/)
  assert.match(frontendNginx, /proxy_pass http:\/\/rk-gateway:10010/)
  assert.match(frontendNginx, /client_max_body_size\s+64m;/)
  assert.match(localNginx, /location \/mobile/)
  assert.match(localNginx, /client_max_body_size\s+64m;/)
})

test('android command-line build should be safe in non-ascii workspaces', async () => {
  const manifest = await readAndroidSource('app/src/main/AndroidManifest.xml')
  const buildScript = await readAndroidSource('build-apk.ps1')

  assert.equal(manifest.includes('package="com.rkclub.app"'), true)
  assert.equal(manifest.includes('android:icon="@drawable/app_icon"'), true)
  assert.equal(manifest.includes('android:roundIcon="@drawable/app_icon_round"'), true)
  assert.equal(buildScript.includes('$StageDir'), true)
  assert.equal(buildScript.includes('-Encoding ASCII'), true)
  assert.equal(buildScript.includes('$ClassFilesFile'), true)
  assert.equal(buildScript.includes('"@$ClassFilesFile"'), true)
  assert.equal(buildScript.includes('${LASTEXITCODE}'), true)
  assert.equal(buildScript.includes("'--lib', $AndroidJar"), true)
  assert.equal(buildScript.includes('Resolve-JavaHome'), true)
  assert.equal(buildScript.includes('$env:JAVA_HOME = $ResolvedJavaHome'), true)
})

test('android debug APK should not be a tiny placeholder package', async () => {
  const apk = await stat(new URL('dist/rk-club-debug.apk', androidRoot))

  assert.ok(apk.size > 150 * 1024, `expected APK > 150 KiB, got ${apk.size} bytes`)
})
