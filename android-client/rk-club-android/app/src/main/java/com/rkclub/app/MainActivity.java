package com.rkclub.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String DEFAULT_BASE_URL = "https://example.com";
    private static final String DEFAULT_TENANT_ID = "1";
    private static final String APK_FILE_NAME = "rk-club-update.apk";
    private static final String MIGRATION_STATUS_PATH = "/api/ops/migration/active";
    private static final String AUTH_PREFS = "rkclub_auth_session";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_REFRESH_TOKEN = "refreshToken";
    private static final String KEY_TENANT_ID = "tenantId";
    private static final String KEY_TENANT_NAME = "tenantName";
    private static final String KEY_DISPLAY_NAME = "displayName";
    private static final String KEY_AVATAR_URL = "avatarUrl";
    private static final String KEY_ROLE_ID = "roleId";
    private static final String PROFILE_METRICS_CACHE_PREFIX = "profileMetrics:";
    private static final String PROFILE_SECTION_CACHE_PREFIX = "profileSection:";
    private static final String EMPTY_ARRAY_RESPONSE = "{\"data\":[]}";
    private static final String EMPTY_OBJECT_RESPONSE = "{\"data\":{}}";

    private static final int PAGE_BACKGROUND = Color.rgb(244, 248, 255);
    private static final int PRIMARY = Color.rgb(30, 111, 255);
    private static final int PRIMARY_DARK = Color.rgb(21, 88, 224);
    private static final int ACCENT = Color.rgb(255, 111, 82);
    private static final int BACKGROUND = PAGE_BACKGROUND;
    private static final int SURFACE = Color.WHITE;
    private static final int SURFACE_ALT = Color.rgb(239, 246, 255);
    private static final int BORDER = Color.rgb(224, 234, 248);
    private static final int TEXT = Color.rgb(20, 33, 61);
    private static final int MUTED = Color.rgb(96, 108, 120);
    private static final int DANGER = Color.rgb(185, 28, 28);
    private static final int INFO = Color.rgb(37, 99, 235);

    private LinearLayout root;
    private LinearLayout content;
    private LinearLayout tenantListContainer;
    private EditText usernameInput;
    private EditText passwordInput;
    private EditText emailInput;
    private EditText emailCodeInput;
    private TextView tenantDisplay;
    private TextView statusText;
    private String baseUrl = DEFAULT_BASE_URL;
    private String token = "";
    private String refreshToken = "";
    private String displayName = "未登录";
    private String avatarUrl = "";
    private String tenantId = DEFAULT_TENANT_ID;
    private String tenantName = "系统管理";
    private long roleId = 0L;
    private JSONObject latestRelease;
    private JSONArray tenantDirectory = new JSONArray();
    private volatile boolean forceUpgradeActive = false;
    private volatile boolean serviceMigrationActive = false;
    private volatile boolean migrationPollingActive = false;
    private volatile boolean updateCheckPending = false;
    private volatile boolean updateCheckCompleted = false;
    private volatile String updateCheckError = "";
    private boolean updateDialogShowing = false;
    private JSONObject activeMigrationRecord;
    private AlertDialog serviceMigrationDialog;
    private ProgressBar migrationProgressBar;
    private TextView migrationProgressText;
    private TextView migrationStepText;
    private TextView migrationElapsedText;
    private TextView migrationRemainingText;
    private TextView migrationLogText;
    private long migrationStartedAtMillis = 0L;
    private int migrationRemainingBaseSeconds = 0;
    private long migrationRemainingBaseAtMillis = 0L;
    private String migrationRemainingRecordId = "";
    private Runnable migrationTickTimer;
    private boolean tenantSelectorShownOnLogin = false;
    private boolean navigatingBack = false;
    private NavTab activeTab = NavTab.HOME;
    private ScreenAction currentScreenAction;
    private final ArrayList<ScreenAction> screenBackStack = new ArrayList<>();
    private final HashSet<String> pendingCommentSubmissions = new HashSet<>();

    private enum NavTab {
        HOME, CONTENT, MESSAGES, SERVICES, PROFILE
    }

    private enum MobileStatusFilter {
        ALL("全部"),
        ONGOING("进行中"),
        UPCOMING("即将开始"),
        ENDED("已结束");

        final String label;

        MobileStatusFilter(String label) {
            this.label = label;
        }
    }

    private interface ScreenAction {
        void open();
    }

    private static class CalendarEntry {
        final String date;
        final String type;
        final String title;
        final String meta;
        final String summary;
        final String footer;
        final String id;

        CalendarEntry(String date, String type, String title, String meta, String summary, String footer, String id) {
            this.date = date;
            this.type = type;
            this.title = title;
            this.meta = meta;
            this.summary = summary;
            this.footer = footer;
            this.id = id;
        }
    }

    private static class UpdateProgressViews {
        LinearLayout content;
        TextView progressText;
        TextView sizeText;
        ProgressBar progressBar;
    }

    private static class ProfileMetrics {
        int activityCount;
        int competitionCount;
        int clubCount;
        int notificationCount;

        ProfileMetrics() {
        }

        ProfileMetrics(int activityCount, int competitionCount, int clubCount, int notificationCount) {
            this.activityCount = activityCount;
            this.competitionCount = competitionCount;
            this.clubCount = clubCount;
            this.notificationCount = notificationCount;
        }
    }

    private static class RegistrationGate {
        final boolean enabled;
        final String label;
        final String reason;

        RegistrationGate(boolean enabled, String label, String reason) {
            this.enabled = enabled;
            this.label = label;
            this.reason = reason;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(PRIMARY_DARK);
        baseUrl = normalizeBaseUrl(DEFAULT_BASE_URL);
        startMigrationStatusPolling();
        if (restoreAuthSession()) {
            showShell();
            syncProfileIdentityAsync();
            return;
        }
        showLogin();
    }

    @Override
    protected void onDestroy() {
        migrationPollingActive = false;
        dismissMigrationBlockingDialog();
        super.onDestroy();
    }

    private ScreenAction homeScreenAction() {
        return new ScreenAction() {
            @Override
            public void open() {
                showHome();
            }
        };
    }

    private ScreenAction contentScreenAction() {
        return new ScreenAction() {
            @Override
            public void open() {
                showContent();
            }
        };
    }

    private ScreenAction messagesScreenAction() {
        return new ScreenAction() {
            @Override
            public void open() {
                showMessages();
            }
        };
    }

    private ScreenAction servicesScreenAction() {
        return new ScreenAction() {
            @Override
            public void open() {
                showServiceHub();
            }
        };
    }

    private ScreenAction profileScreenAction() {
        return new ScreenAction() {
            @Override
            public void open() {
                showProfile();
            }
        };
    }

    private ScreenAction newsCenterScreenAction() {
        return new ScreenAction() {
            @Override
            public void open() {
                showNewsCenter();
            }
        };
    }

    private ScreenAction activityHighlightsScreenAction() {
        return activityHighlightsScreenAction(MobileStatusFilter.ALL);
    }

    private ScreenAction activityHighlightsScreenAction(final MobileStatusFilter filter) {
        return new ScreenAction() {
            @Override
            public void open() {
                showActivityHighlights(filter);
            }
        };
    }

    private ScreenAction competitionCenterScreenAction() {
        return competitionCenterScreenAction(MobileStatusFilter.ALL);
    }

    private ScreenAction competitionCenterScreenAction(final MobileStatusFilter filter) {
        return new ScreenAction() {
            @Override
            public void open() {
                showCompetitionCenter(filter);
            }
        };
    }

    private ScreenAction contentCalendarScreenAction() {
        return new ScreenAction() {
            @Override
            public void open() {
                showContentCalendar();
            }
        };
    }

    private ScreenAction adminWorkbenchScreenAction() {
        return new ScreenAction() {
            @Override
            public void open() {
                showAdminWorkbench();
            }
        };
    }

    private ScreenAction activityCenterScreenAction() {
        return new ScreenAction() {
            @Override
            public void open() {
                showActivityCenter();
            }
        };
    }

    private ScreenAction clubDirectoryScreenAction() {
        return new ScreenAction() {
            @Override
            public void open() {
                showClubDirectory();
            }
        };
    }

    private ScreenAction myServicesScreenAction() {
        return new ScreenAction() {
            @Override
            public void open() {
                showMyServices();
            }
        };
    }

    private ScreenAction myCreditsScreenAction() {
        return new ScreenAction() {
            @Override
            public void open() {
                showMyCredits();
            }
        };
    }

    private ScreenAction myClubsScreenAction() {
        return new ScreenAction() {
            @Override
            public void open() {
                showMyClubs();
            }
        };
    }

    private ScreenAction myCompetitionsScreenAction() {
        return new ScreenAction() {
            @Override
            public void open() {
                showMyCompetitions();
            }
        };
    }

    private ScreenAction tenantsScreenAction() {
        return new ScreenAction() {
            @Override
            public void open() {
                showTenants();
            }
        };
    }

    private ScreenAction updateCenterScreenAction() {
        return new ScreenAction() {
            @Override
            public void open() {
                showUpdateCenter();
            }
        };
    }

    private ScreenAction newsDetailScreenAction(final String id) {
        final String finalId = id;
        return new ScreenAction() {
            @Override
            public void open() {
                showNewsDetail(finalId);
            }
        };
    }

    private ScreenAction activityDetailScreenAction(final String id) {
        final String finalId = id;
        return new ScreenAction() {
            @Override
            public void open() {
                showActivityDetail(finalId);
            }
        };
    }

    private ScreenAction competitionDetailScreenAction(final String id) {
        final String finalId = id;
        return new ScreenAction() {
            @Override
            public void open() {
                showCompetitionDetail(finalId);
            }
        };
    }

    private ScreenAction userProfileScreenAction(final String userId) {
        final String finalUserId = userId;
        return new ScreenAction() {
            @Override
            public void open() {
                showUserProfile(finalUserId);
            }
        };
    }

    private void setCurrentScreen(ScreenAction action, boolean clearHistory) {
        if (action == null) {
            return;
        }
        if (clearHistory) {
            screenBackStack.clear();
        } else if (!navigatingBack && currentScreenAction != null) {
            screenBackStack.add(currentScreenAction);
        }
        currentScreenAction = action;
    }

    private void openMainScreen(ScreenAction action) {
        if (enforceMigrationGate()) {
            return;
        }
        setCurrentScreen(action, true);
        action.open();
    }

    private void openChildScreen(ScreenAction action) {
        if (enforceMigrationGate()) {
            return;
        }
        setCurrentScreen(action, false);
        action.open();
    }

    private void navigateChild(ScreenAction action) {
        if (enforceMigrationGate()) {
            return;
        }
        if (enforceUpdateGate()) {
            return;
        }
        openChildScreen(action);
    }

    @Override
    public void onBackPressed() {
        if (serviceMigrationActive && enforceMigrationGate()) {
            return;
        }
        if (forceUpgradeActive && enforceUpdateGate()) {
            return;
        }
        if (!screenBackStack.isEmpty()) {
            final ScreenAction previous = screenBackStack.remove(screenBackStack.size() - 1);
            navigatingBack = true;
            try {
                previous.open();
                currentScreenAction = previous;
            } finally {
                navigatingBack = false;
            }
            return;
        }
        super.onBackPressed();
    }

    private void showLogin() {
        baseUrl = normalizeBaseUrl(DEFAULT_BASE_URL);
        latestRelease = null;
        forceUpgradeActive = false;
        updateCheckCompleted = false;
        updateCheckError = "";
        tenantSelectorShownOnLogin = false;
        currentScreenAction = null;
        screenBackStack.clear();
        pendingCommentSubmissions.clear();
        root = vertical();
        root.setBackgroundColor(BACKGROUND);
        setContentView(root);

        ScrollView scrollView = new ScrollView(this);
        root.addView(scrollView, new LinearLayout.LayoutParams(-1, -1));

        LinearLayout panel = vertical();
        panel.setPadding(dp(18), dp(22), dp(18), dp(18));
        scrollView.addView(panel);

        LinearLayout brandRow = horizontal();
        brandRow.setGravity(Gravity.CENTER_VERTICAL);
        panel.addView(brandRow);
        ImageView brand = image(R.drawable.brand_mark, "RK Club");
        brand.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        LinearLayout.LayoutParams brandParams = new LinearLayout.LayoutParams(dp(42), dp(42));
        brandParams.setMargins(0, 0, dp(10), 0);
        brandRow.addView(brand, brandParams);
        LinearLayout brandCopy = vertical();
        brandCopy.addView(text("RK Club", 14, ACCENT, Typeface.BOLD));
        brandCopy.addView(text("移动端工作台", 22, TEXT, Typeface.BOLD));
        brandRow.addView(brandCopy, new LinearLayout.LayoutParams(0, -2, 1));

        ImageView heroImage = image(R.drawable.hero_mobile_campus_v2, "社团移动工作台视觉图");
        LinearLayout.LayoutParams heroImageParams = new LinearLayout.LayoutParams(-1, dp(112));
        heroImageParams.setMargins(0, dp(16), 0, 0);
        panel.addView(heroImage, heroImageParams);

        LinearLayout loginCard = card();
        loginCard.setPadding(dp(16), dp(16), dp(16), dp(14));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.setMargins(0, dp(12), 0, 0);
        panel.addView(loginCard, cardParams);

        usernameInput = input("用户名", "", InputType.TYPE_CLASS_TEXT);
        passwordInput = input("密码", "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        emailInput = input("绑定邮箱", "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailCodeInput = input("邮箱验证码", "", InputType.TYPE_CLASS_NUMBER);

        LinearLayout tenantRow = horizontal();
        tenantRow.setGravity(Gravity.CENTER_VERTICAL);
        tenantRow.setPadding(0, 0, 0, dp(12));
        tenantDisplay = text("租户：" + tenantName, 13, PRIMARY, Typeface.BOLD);
        tenantDisplay.setSingleLine(true);
        tenantDisplay.setPadding(dp(10), dp(6), dp(10), dp(6));
        tenantDisplay.setBackground(roundStroke(SURFACE_ALT, BORDER, dp(8)));
        tenantRow.addView(tenantDisplay, new LinearLayout.LayoutParams(0, dp(34), 1));
        Button chooseTenant = secondaryButton("切换");
        chooseTenant.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (enforceUpdateGate()) {
                    return;
                }
                showTenantSelector(tenantDirectory);
            }
        });
        LinearLayout.LayoutParams chooseParams = new LinearLayout.LayoutParams(dp(76), dp(34));
        chooseParams.setMargins(dp(8), 0, 0, 0);
        tenantRow.addView(chooseTenant, chooseParams);
        loginCard.addView(tenantRow);
        tenantListContainer = vertical();
        loginCard.addView(tenantListContainer);
        loginCard.addView(label("账号"));
        loginCard.addView(usernameInput);
        loginCard.addView(label("密码"));
        loginCard.addView(passwordInput);

        Button loginButton = primaryButton("登录进入");
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                login();
            }
        });
        loginCard.addView(loginButton, buttonParams());

        loginCard.addView(sectionTitle("邮箱验证码登录"));
        loginCard.addView(label("邮箱"));
        loginCard.addView(emailInput);
        loginCard.addView(label("验证码"));
        LinearLayout emailCodeRow = horizontal();
        emailCodeRow.setGravity(Gravity.CENTER_VERTICAL);
        emailCodeRow.addView(emailCodeInput, new LinearLayout.LayoutParams(0, -2, 1));
        Button sendEmailCodeButton = secondaryButton("发送验证码");
        sendEmailCodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendEmailLoginCode();
            }
        });
        LinearLayout.LayoutParams sendCodeParams = new LinearLayout.LayoutParams(dp(104), dp(44));
        sendCodeParams.setMargins(dp(8), 0, 0, 0);
        emailCodeRow.addView(sendEmailCodeButton, sendCodeParams);
        loginCard.addView(emailCodeRow);

        Button emailLoginButton = primaryButton("邮箱登录");
        emailLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                emailCodeLogin();
            }
        });
        loginCard.addView(emailLoginButton, buttonParams());

        statusText = text("正在检查升级状态...", 14, MUTED, Typeface.NORMAL);
        statusText.setPadding(0, dp(12), 0, 0);
        loginCard.addView(statusText);

        Button refreshTenants = secondaryButton("刷新租户列表");
        refreshTenants.setTextSize(13);
        refreshTenants.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (enforceUpdateGate()) {
                    return;
                }
                loadTenants();
            }
        });
        panel.addView(refreshTenants, compactButtonParams());
        loadTenants();
        checkLatestRelease(true);
    }

    private void showShell() {
        root = vertical();
        root.setBackgroundColor(BACKGROUND);
        setContentView(root);

        content = vertical();
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));

        renderBottomNavigation();

        openMainScreen(homeScreenAction());
        checkLatestRelease(false);
    }

    private void showHome() {
        activeTab = NavTab.HOME;
        ScrollView scrollView = screenScaffold(tenantName, "精彩社团生活 · 从这里开始");
        LinearLayout body = (LinearLayout) scrollView.getChildAt(0);

        body.addView(mobileSearchBar());
        body.addView(mobileBannerCard());
        body.addView(mobileQuickGrid());
        body.addView(mobileSectionHeader("热门活动", "更多", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateChild(activityHighlightsScreenAction());
            }
        }));
        body.addView(mobileEventCard("校园歌手大赛", "进行中", "2024.05.20 18:00", "大学生活动中心", "社团联合会", R.drawable.hero_mobile_activity, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateChild(activityHighlightsScreenAction());
            }
        }));
        body.addView(mobileEventCard("摄影技巧分享会", "即将开始", "2024.05.22 14:00", "教学楼A101", "摄影社", R.drawable.hero_mobile_news, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateChild(activityHighlightsScreenAction());
            }
        }));
        body.addView(mobileSectionHeader("新闻资讯", "更多", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateChild(newsCenterScreenAction());
            }
        }));
        body.addView(mobileEventCard("我校社团在省级比赛中再创佳绩", "1.2k阅读", "2024.05.18", "社团联合会", "校园新闻", R.drawable.hero_mobile_competition, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateChild(newsCenterScreenAction());
            }
        }));
        if (roleId == 1L) {
            body.addView(mobileEventCard("管理员工作台", "运营概览", "后台运营、流量和通知摘要", "进入后台摘要", "管理服务", R.drawable.hero_mobile_admin, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    navigateChild(adminWorkbenchScreenAction());
                }
            }));
        }
    }

    private void showContent() {
        activeTab = NavTab.CONTENT;
        ScrollView scrollView = screenScaffold("内容中心", "新闻、活动、比赛统一浏览");
        LinearLayout body = (LinearLayout) scrollView.getChildAt(0);

        LinearLayout hero = card();
        hero.setPadding(dp(16), dp(16), dp(16), dp(16));
        body.addView(hero);
        ImageView newsHero = image(R.drawable.hero_mobile_news, "新闻动态视觉图");
        LinearLayout.LayoutParams newsHeroParams = new LinearLayout.LayoutParams(-1, dp(142));
        newsHeroParams.setMargins(0, 0, 0, dp(12));
        hero.addView(newsHero, newsHeroParams);
        hero.addView(text("社团内容看板", 22, TEXT, Typeface.BOLD));
        hero.addView(text("快速查看最新新闻、热门活动和已发布比赛。", 14, MUTED, Typeface.NORMAL));

        body.addView(sectionTitle("内容入口"));
        body.addView(shortcut("新闻动态", "读取 /api/news/latest，查看最新公开新闻", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateChild(newsCenterScreenAction());
            }
        }));
        body.addView(shortcut("活动热榜", "读取 /api/activity/hot，查看当前热门活动", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateChild(activityHighlightsScreenAction());
            }
        }));
        body.addView(shortcut("比赛中心", "读取 /api/competition/published，查看公开比赛", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateChild(competitionCenterScreenAction());
            }
        }));
        body.addView(shortcut("活动比赛日历", "按日期查看活动开始、报名截止和比赛安排", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateChild(contentCalendarScreenAction());
            }
        }));
        if (roleId == 1L) {
            body.addView(shortcut("后台概览", "管理员可查看运营摘要、在线用户和通知状态", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    navigateChild(adminWorkbenchScreenAction());
                }
            }));
        }
    }

    private void showNewsCenter() {
        final ScrollView scrollView = screenScaffold("新闻动态", "最新新闻与社团公告");
        final LinearLayout body = (LinearLayout) scrollView.getChildAt(0);
        ImageView hero = image(R.drawable.hero_mobile_news, "新闻动态视觉图");
        LinearLayout.LayoutParams heroParams = new LinearLayout.LayoutParams(-1, dp(148));
        heroParams.setMargins(0, 0, 0, dp(16));
        body.addView(hero, heroParams);
        final TextView loading = text("正在加载 /api/news/latest ...", 14, MUTED, Typeface.NORMAL);
        body.addView(loading);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String result = request("GET", "/api/news/latest?limit=20", null, true);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            renderNewsCenter(body, loading, result);
                        }
                    });
                } catch (Exception e) {
                    showLoadError(loading, e);
                }
            }
        }).start();
    }

    private void showActivityHighlights() {
        showActivityHighlights(MobileStatusFilter.ALL);
    }

    private void showActivityHighlights(final MobileStatusFilter filter) {
        final ScrollView scrollView = screenScaffold("活动", "热门活动与报名信息");
        final LinearLayout body = (LinearLayout) scrollView.getChildAt(0);
        body.addView(mobileStatusTabs(filter, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Object value = view.getTag();
                if (value instanceof MobileStatusFilter) {
                    navigateChild(activityHighlightsScreenAction((MobileStatusFilter) value));
                }
            }
        }));
        ImageView hero = image(R.drawable.hero_mobile_activity, "活动热榜视觉图");
        LinearLayout.LayoutParams heroParams = new LinearLayout.LayoutParams(-1, dp(148));
        heroParams.setMargins(0, 0, 0, dp(16));
        body.addView(hero, heroParams);
        final String requestPath = activityListPathForFilter(filter);
        final TextView loading = text("正在加载 " + requestPath + " ...", 14, MUTED, Typeface.NORMAL);
        body.addView(loading);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String result = request("GET", requestPath, null, true);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            renderActivityHighlights(body, loading, result, filter);
                        }
                    });
                } catch (Exception e) {
                    showLoadError(loading, e);
                }
            }
        }).start();
    }

    private void showCompetitionCenter() {
        showCompetitionCenter(MobileStatusFilter.ALL);
    }

    private void showCompetitionCenter(final MobileStatusFilter filter) {
        final ScrollView scrollView = screenScaffold("比赛", "竞赛发布、地点与报名信息");
        final LinearLayout body = (LinearLayout) scrollView.getChildAt(0);
        body.addView(mobileStatusTabs(filter, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Object value = view.getTag();
                if (value instanceof MobileStatusFilter) {
                    navigateChild(competitionCenterScreenAction((MobileStatusFilter) value));
                }
            }
        }));
        ImageView hero = image(R.drawable.hero_mobile_competition, "比赛中心视觉图");
        LinearLayout.LayoutParams heroParams = new LinearLayout.LayoutParams(-1, dp(148));
        heroParams.setMargins(0, 0, 0, dp(16));
        body.addView(hero, heroParams);
        final String requestPath = competitionListPathForFilter(filter);
        final TextView loading = text("正在加载 " + requestPath + " ...", 14, MUTED, Typeface.NORMAL);
        body.addView(loading);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String result = request("GET", requestPath, null, true);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            renderCompetitionCenter(body, loading, result, filter);
                        }
                    });
                } catch (Exception e) {
                    showLoadError(loading, e);
                }
            }
        }).start();
    }

    private void showContentCalendar() {
        final ScrollView scrollView = screenScaffold("活动比赛日历", "按日期查看近期活动和比赛安排");
        final LinearLayout body = (LinearLayout) scrollView.getChildAt(0);
        ImageView hero = image(R.drawable.hero_mobile_activity, "活动比赛日历视觉图");
        LinearLayout.LayoutParams heroParams = new LinearLayout.LayoutParams(-1, dp(132));
        heroParams.setMargins(0, 0, 0, dp(14));
        body.addView(hero, heroParams);
        final TextView loading = text("正在加载活动和比赛日程 ...", 14, MUTED, Typeface.NORMAL);
        body.addView(loading);

        new Thread(new Runnable() {
            @Override
            public void run() {
                final String activityResult = safeRequest("GET", "/api/activity/list", true);
                final String competitionResult = safeRequest("GET", "/api/competition/published", true);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        renderContentCalendar(body, loading, activityResult, competitionResult);
                    }
                });
            }
        }).start();
    }

    private void showNewsDetail(final String id) {
        if (id == null || id.trim().isEmpty()) {
            return;
        }
        final ScrollView scrollView = screenScaffold("新闻详情", "完整正文、发布时间和来源信息");
        final LinearLayout body = (LinearLayout) scrollView.getChildAt(0);
        final TextView loading = text("正在加载新闻详情 ...", 14, MUTED, Typeface.NORMAL);
        body.addView(loading);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String result = request("GET", "/api/news/" + urlEncode(id), null, true);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            renderDetail(body, loading, result, "新闻", R.drawable.hero_mobile_news,
                                    new String[]{"title", "name"},
                                    new String[]{"category", "categoryName", "author", "publisher", "createBy"},
                                    new String[]{"summary", "description", "content", "body"},
                                    new String[]{"publishTime", "createTime", "updateTime"});
                            loadComments("news", id, body);
                        }
                    });
                } catch (Exception e) {
                    showLoadError(loading, e);
                }
            }
        }).start();
    }

    private void showActivityDetail(final String id) {
        if (id == null || id.trim().isEmpty()) {
            return;
        }
        final ScrollView scrollView = screenScaffold("活动详情", "活动介绍、时间地点和报名信息");
        final LinearLayout body = (LinearLayout) scrollView.getChildAt(0);
        final TextView loading = text("正在加载活动详情 ...", 14, MUTED, Typeface.NORMAL);
        body.addView(loading);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String result = request("GET", "/api/activity/" + urlEncode(id) + "/detail", null, true);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            JSONObject detail = renderDetail(body, loading, result, "活动", R.drawable.hero_mobile_activity,
                                    new String[]{"title", "activityName", "name"},
                                    new String[]{"activityStatusName", "statusName", "activityStatus", "status", "location", "address", "venue"},
                                    new String[]{"content", "description", "summary", "intro"},
                                    new String[]{"registrationStartTime", "registrationEndTime", "startTime", "endTime"});
                            detailBottomActionBar(body, buildRegistrationGate(detail, true), new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    registerActivity(id);
                                }
                            });
                            loadComments("activity", id, body);
                        }
                    });
                } catch (Exception e) {
                    showLoadError(loading, e);
                }
            }
        }).start();
    }

    private void showCompetitionDetail(final String id) {
        if (id == null || id.trim().isEmpty()) {
            return;
        }
        final ScrollView scrollView = screenScaffold("比赛详情", "赛事介绍、赛程和报名规则");
        final LinearLayout body = (LinearLayout) scrollView.getChildAt(0);
        final TextView loading = text("正在加载比赛详情 ...", 14, MUTED, Typeface.NORMAL);
        body.addView(loading);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String result = request("GET", "/api/competition/" + urlEncode(id), null, true);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            JSONObject detail = renderDetail(body, loading, result, "比赛", R.drawable.hero_mobile_competition,
                                    new String[]{"title", "competitionName", "name"},
                                    new String[]{"competitionType", "type", "level", "statusName", "status"},
                                    new String[]{"description", "content", "summary", "rules"},
                                    new String[]{"registrationStart", "registrationEnd", "registrationDeadline", "competitionStart", "competitionEnd", "startTime", "endTime"});
                            detailBottomActionBar(body, buildRegistrationGate(detail, false), new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    registerCompetition(id);
                                }
                            });
                        }
                    });
                } catch (Exception e) {
                    showLoadError(loading, e);
                }
            }
        }).start();
    }

    private void showUserProfile(final String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            Toast.makeText(this, "评论用户信息缺少用户 ID", Toast.LENGTH_SHORT).show();
            return;
        }
        final ScrollView scrollView = screenScaffold("个人主页", "评论用户资料");
        final LinearLayout body = (LinearLayout) scrollView.getChildAt(0);
        final TextView loading = text("正在加载用户资料 ...", 14, MUTED, Typeface.NORMAL);
        body.addView(loading);
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String result = safeRequest("GET", "/users/internal/by-auth-ids?authUserIds=" + urlEncode(userId), true);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        renderUserProfile(body, loading, result, userId);
                    }
                });
            }
        }).start();
    }

    private void showAdminWorkbench() {
        final ScrollView scrollView = screenScaffold("管理工作台", "后台运营、流量和通知摘要");
        final LinearLayout body = (LinearLayout) scrollView.getChildAt(0);
        ImageView hero = image(R.drawable.hero_mobile_admin, "管理员后台视觉图");
        LinearLayout.LayoutParams heroParams = new LinearLayout.LayoutParams(-1, dp(152));
        heroParams.setMargins(0, 0, 0, dp(16));
        body.addView(hero, heroParams);
        final TextView loading = text("正在加载后台摘要 ...", 14, MUTED, Typeface.NORMAL);
        body.addView(loading);

        new Thread(new Runnable() {
            @Override
            public void run() {
                final String monitoringResult = safeRequest("GET", "/admin/ops/monitoring/overview", true);
                final String trafficResult = safeRequest("GET", "/admin/ops/traffic/overview?tenantId=" + urlEncode(tenantId), true);
                final String onlineResult = safeRequest("GET", "/admin/ops/traffic/online-users?tenantId=" + urlEncode(tenantId), true);
                final String notificationResult = safeRequest("GET", "/api/admin/notifications/overview", true);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        renderAdminWorkbench(body, loading, monitoringResult, trafficResult, onlineResult, notificationResult);
                    }
                });
            }
        }).start();
    }

    private void showServiceHub() {
        activeTab = NavTab.SERVICES;
        ScrollView scrollView = screenScaffold("服务", "移动端业务中心");
        LinearLayout body = (LinearLayout) scrollView.getChildAt(0);

        ImageView servicesHero = image(R.drawable.hero_mobile_services, "移动端服务中心视觉图");
        LinearLayout.LayoutParams servicesHeroParams = new LinearLayout.LayoutParams(-1, dp(164));
        servicesHeroParams.setMargins(0, 0, 0, dp(16));
        body.addView(servicesHero, servicesHeroParams);

        LinearLayout card = card();
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        body.addView(card);
        card.addView(text("常用功能", 22, TEXT, Typeface.BOLD));
        card.addView(text("活动、成员、校友、租户和升级都可以在移动端直接处理。", 14, MUTED, Typeface.NORMAL));

        body.addView(shortcut("新闻动态", "查看最新公开新闻和社团公告", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateChild(newsCenterScreenAction());
            }
        }));
        body.addView(shortcut("活动热榜", "查看热门活动与报名信息", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateChild(activityHighlightsScreenAction());
            }
        }));
        body.addView(shortcut("比赛中心", "查看已发布比赛、时间地点和报名人数", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateChild(competitionCenterScreenAction());
            }
        }));
        body.addView(shortcut("活动比赛日历", "把近期活动和比赛按日期排成日程", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateChild(contentCalendarScreenAction());
            }
        }));
        body.addView(shortcut("活动广场", "浏览当前租户活动列表", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateChild(activityCenterScreenAction());
            }
        }));
        body.addView(shortcut("我的服务", "查看我的活动、积分和个人待办", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateChild(myServicesScreenAction());
            }
        }));
        body.addView(shortcut("社团目录", "查看成员台账和校友名录", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateChild(clubDirectoryScreenAction());
            }
        }));
        body.addView(shortcut("租户目录", "切换和查看可登录租户", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateChild(tenantsScreenAction());
            }
        }));
        body.addView(shortcut("移动端升级", "检查后台发布的 APK 更新", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateChild(updateCenterScreenAction());
            }
        }));
    }

    private void showActivityCenter() {
        final ScrollView scrollView = screenScaffold("活动广场", "当前租户活动列表");
        final LinearLayout body = (LinearLayout) scrollView.getChildAt(0);
        final TextView loading = text("正在加载 /api/activity/list ...", 14, MUTED, Typeface.NORMAL);
        body.addView(loading);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String result = request("GET", "/api/activity/list", null, true);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            renderActivityCenter(body, loading, result);
                        }
                    });
                } catch (Exception e) {
                    showLoadError(loading, e);
                }
            }
        }).start();
    }

    private void showClubDirectory() {
        activeTab = NavTab.CONTENT;
        final ScrollView scrollView = screenScaffold("社团目录", "成员与校友");
        final LinearLayout body = (LinearLayout) scrollView.getChildAt(0);
        final TextView loading = text("正在加载成员和校友数据 ...", 14, MUTED, Typeface.NORMAL);
        body.addView(loading);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String membersResult = request("GET", "/api/members/list?tenantId=" + urlEncode(tenantId), null, true);
                    final String alumniResult = request("GET", "/api/alumni/list?tenantId=" + urlEncode(tenantId), null, true);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            renderClubDirectory(body, loading, membersResult, alumniResult);
                        }
                    });
                } catch (Exception e) {
                    showLoadError(loading, e);
                }
            }
        }).start();
    }

    private void showMyServices() {
        final ScrollView scrollView = screenScaffold("我的服务", "活动、积分和个人待办");
        final LinearLayout body = (LinearLayout) scrollView.getChildAt(0);
        final TextView refreshStatus = text("正在更新", 12, MUTED, Typeface.NORMAL);
        refreshStatus.setPadding(0, 0, 0, dp(8));
        body.addView(refreshStatus);
        final LinearLayout rows = vertical();
        body.addView(rows);
        final String cachedActivities = loadCachedProfileSection("services:activities", EMPTY_ARRAY_RESPONSE);
        final String cachedRegistrations = loadCachedProfileSection("services:registrations", EMPTY_ARRAY_RESPONSE);
        final String cachedCredits = loadCachedProfileSection("services:credits", EMPTY_OBJECT_RESPONSE);
        renderMyServices(rows, null, cachedActivities, cachedRegistrations, cachedCredits);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String activityResult = request("GET", "/api/activity/my", null, true);
                    final String registrationResult = safeRequest("GET", "/api/activity/my/registrations", true);
                    final String creditResult = request("GET", "/api/credit/my/summary", null, true);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            saveProfileSectionCache("services:activities", activityResult);
                            saveProfileSectionCache("services:registrations", registrationResult);
                            saveProfileSectionCache("services:credits", creditResult);
                            rows.removeAllViews();
                            renderMyServices(rows, null, activityResult, registrationResult, creditResult);
                            refreshStatus.setText("已更新");
                        }
                    });
                } catch (Exception e) {
                    updateRefreshStatus(refreshStatus, "更新失败，已显示本地缓存");
                }
            }
        }).start();
    }

    private void showMessages() {
        activeTab = NavTab.MESSAGES;
        final ScrollView scrollView = screenScaffold("消息", "站内通知和升级通知");
        final LinearLayout body = (LinearLayout) scrollView.getChildAt(0);
        final TextView refreshStatus = text("正在更新", 12, MUTED, Typeface.NORMAL);
        refreshStatus.setPadding(0, 0, 0, dp(8));
        body.addView(refreshStatus);
        final LinearLayout rows = vertical();
        body.addView(rows);
        final String cachedNotifications = loadCachedProfileSection("messages:notifications", EMPTY_ARRAY_RESPONSE);
        renderNotifications(rows, null, cachedNotifications);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String result = request("GET", "/api/notifications", null, true);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            saveProfileSectionCache("messages:notifications", result);
                            rows.removeAllViews();
                            renderNotifications(rows, null, result);
                            refreshStatus.setText("已更新");
                        }
                    });
                } catch (Exception e) {
                    updateRefreshStatus(refreshStatus, "更新失败，已显示本地缓存");
                }
            }
        }).start();
    }

    private void showTenants() {
        final ScrollView scrollView = screenScaffold("租户", "当前账号所在租户");
        final LinearLayout body = (LinearLayout) scrollView.getChildAt(0);
        final TextView loading = text("正在加载当前账号租户...", 14, MUTED, Typeface.NORMAL);
        body.addView(loading);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String directoryResult = request("GET", "/tenants/list", null, false);
                    final String accountTenantResult = token.isEmpty() ? "" : safeRequest("GET", "/auth/switchable-tenants", true);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            body.removeView(loading);
                            renderAccountTenantCards(body, directoryResult, accountTenantResult);
                        }
                    });
                } catch (Exception e) {
                    showLoadError(loading, e);
                }
            }
        }).start();
    }

    private void showUpdateCenter() {
        final ScrollView scrollView = screenScaffold("升级", "远程 APK 更新");
        final LinearLayout body = (LinearLayout) scrollView.getChildAt(0);
        final TextView loading = text("正在检查 /mobile/releases/latest ...", 14, MUTED, Typeface.NORMAL);
        body.addView(loading);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final JSONObject release = fetchLatestRelease();
                    latestRelease = release;
                    updateCheckCompleted = true;
                    updateCheckError = "";
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            body.removeView(loading);
                            renderRelease(body, release);
                        }
                    });
                } catch (Exception e) {
                    showLoadError(loading, e);
                }
            }
        }).start();
    }

    private void showProfile() {
        activeTab = NavTab.PROFILE;
        ScrollView scrollView = screenScaffold("我的", "个人中心");
        LinearLayout body = (LinearLayout) scrollView.getChildAt(0);

        LinearLayout card = card();
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        body.addView(card);
        LinearLayout profileRow = horizontal();
        profileRow.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(profileRow);
        profileRow.addView(avatarImageView(displayName, avatarUrl, dp(58)), new LinearLayout.LayoutParams(dp(58), dp(58)));
        LinearLayout profileCopy = vertical();
        profileCopy.setPadding(dp(14), 0, 0, 0);
        profileRow.addView(profileCopy, new LinearLayout.LayoutParams(0, -2, 1));
        profileCopy.addView(text(displayName, 23, TEXT, Typeface.BOLD));
        profileCopy.addView(text("个人中心", 14, PRIMARY, Typeface.BOLD));
        profileCopy.addView(text(roleLabel(), 13, MUTED, Typeface.NORMAL));

        LinearLayout stats = horizontal();
        stats.setPadding(0, dp(16), 0, dp(6));
        card.addView(stats);
        final TextView refreshStatus = text("正在更新", 12, MUTED, Typeface.NORMAL);
        refreshStatus.setPadding(0, 0, 0, dp(6));
        card.addView(refreshStatus);
        final ProfileMetrics cachedMetrics = loadCachedProfileMetrics();
        renderProfileMetricPills(stats, cachedMetrics);
        loadProfileMetrics(stats, refreshStatus, cachedMetrics);

        body.addView(sectionTitle("账号信息"));
        body.addView(contentCard("我的学分", "总学分与类别", "查看思想政治、学术科技、文化艺术和社会实践分数", "进入学分页", "学分", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateChild(myCreditsScreenAction());
            }
        }));
        body.addView(contentCard("我的社团", "我加入的社团", "查看社团身份、加入时间和发现更多社团", "进入社团页", "社团", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateChild(myClubsScreenAction());
            }
        }));
        body.addView(contentCard("当前租户", tenantName, "租户编号：" + tenantId, "点击查看账号所在租户", "租户", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateChild(tenantsScreenAction());
            }
        }));
        body.addView(contentCard("版本升级", "App 版本码：" + getInstalledVersionCode(), token.isEmpty() ? "未持有登录令牌" : "登录状态正常", "检查后台发布的更新", "升级", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateChild(updateCenterScreenAction());
            }
        }));

        Button updateButton = secondaryButton("检查更新");
        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateChild(updateCenterScreenAction());
            }
        });
        card.addView(updateButton, buttonParams());

        Button logoutButton = secondaryButton("退出登录");
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearAuthSession();
                showLogin();
            }
        });
        card.addView(logoutButton, buttonParams());
    }

    private void loadProfileMetrics(final LinearLayout stats, final TextView refreshStatus, final ProfileMetrics previousMetrics) {
        if (stats == null || token.isEmpty()) {
            if (refreshStatus != null) {
                refreshStatus.setText("已显示本地缓存");
            }
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                final ProfileMetrics metrics = new ProfileMetrics();
                String myActivities = safeRequest("GET", "/api/activity/my", true);
                String activityRegistrations = safeRequest("GET", "/api/activity/my/registrations", true);
                String myCompetitions = safeRequest("GET", "/api/competition/my", true);
                String competitionRegistrations = safeRequest("GET", "/api/competition/my/registrations", true);
                String membersResult = safeRequest("GET", "/api/members/list?tenantId=" + urlEncode(tenantId), true);
                String alumniResult = safeRequest("GET", "/api/alumni/list?tenantId=" + urlEncode(tenantId), true);
                String notificationsResult = safeRequest("GET", "/api/notifications", true);

                metrics.activityCount = Math.max(countDataRows(myActivities), countDataRows(activityRegistrations));
                metrics.competitionCount = Math.max(countDataRows(myCompetitions), countDataRows(competitionRegistrations));
                metrics.clubCount = countCurrentTenantRows(membersResult) + countCurrentTenantRows(alumniResult);
                metrics.notificationCount = countUnreadNotifications(notificationsResult);
                saveCachedProfileMetrics(metrics);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!sameProfileMetrics(previousMetrics, metrics)) {
                            renderProfileMetricPills(stats, metrics);
                        }
                        if (refreshStatus != null) {
                            refreshStatus.setText("已更新");
                        }
                    }
                });
            }
        }).start();
    }

    private void showMyCompetitions() {
        activeTab = NavTab.PROFILE;
        final ScrollView scrollView = screenScaffold("我的比赛", "参赛和报名记录");
        final LinearLayout body = (LinearLayout) scrollView.getChildAt(0);
        final TextView refreshStatus = text("正在更新", 12, MUTED, Typeface.NORMAL);
        refreshStatus.setPadding(0, 0, 0, dp(8));
        body.addView(refreshStatus);
        final LinearLayout rows = vertical();
        body.addView(rows);
        final String cachedCompetitions = loadCachedProfileSection("competitions:mine", EMPTY_ARRAY_RESPONSE);
        final String cachedRegistrations = loadCachedProfileSection("competitions:registrations", EMPTY_ARRAY_RESPONSE);
        renderMyCompetitions(rows, null, cachedCompetitions, cachedRegistrations);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String competitionResult = request("GET", "/api/competition/my", null, true);
                    final String registrationResult = safeRequest("GET", "/api/competition/my/registrations", true);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            saveProfileSectionCache("competitions:mine", competitionResult);
                            saveProfileSectionCache("competitions:registrations", registrationResult);
                            rows.removeAllViews();
                            renderMyCompetitions(rows, null, competitionResult, registrationResult);
                            refreshStatus.setText("已更新");
                        }
                    });
                } catch (Exception e) {
                    updateRefreshStatus(refreshStatus, "更新失败，已显示本地缓存");
                }
            }
        }).start();
    }

    private void renderProfileMetricPills(LinearLayout stats, ProfileMetrics metrics) {
        if (stats == null) {
            return;
        }
        if (metrics == null) {
            metrics = defaultProfileMetrics();
        }
        stats.removeAllViews();
        addProfileMetricPill(stats, "我的活动", String.valueOf(metrics.activityCount), PRIMARY, false, myServicesScreenAction());
        addProfileMetricPill(stats, "我的比赛", String.valueOf(metrics.competitionCount), INFO, true, myCompetitionsScreenAction());
        addProfileMetricPill(stats, "我的社团", String.valueOf(metrics.clubCount), Color.rgb(54, 203, 161), true, myClubsScreenAction());
        addProfileMetricPill(stats, "我的关注", String.valueOf(metrics.notificationCount), Color.rgb(250, 173, 20), true, messagesScreenAction());
    }

    private void addProfileMetricPill(LinearLayout stats, String label, String value, int tone, boolean withLeftMargin, final ScreenAction action) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1);
        if (withLeftMargin) {
            params.setMargins(dp(8), 0, 0, 0);
        }
        LinearLayout pill = metricPill(label, value, tone);
        if (action != null) {
            pill.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    navigateChild(action);
                }
            });
        }
        stats.addView(pill, params);
    }

    private ProfileMetrics defaultProfileMetrics() {
        return new ProfileMetrics(0, 0, 0, 0);
    }

    private String profileMetricsCacheKey() {
        return PROFILE_METRICS_CACHE_PREFIX + trim(tenantId) + ":" + roleId + ":" + trim(displayName);
    }

    private String profileSectionCacheKey(String section) {
        return PROFILE_SECTION_CACHE_PREFIX + trim(tenantId) + ":" + roleId + ":" + trim(displayName) + ":" + trim(section);
    }

    private String loadCachedProfileSection(String section, String fallback) {
        String raw = getSharedPreferences(AUTH_PREFS, MODE_PRIVATE)
                .getString(profileSectionCacheKey(section), "");
        if (raw == null || raw.trim().isEmpty()) {
            return fallback;
        }
        return raw;
    }

    private void saveProfileSectionCache(String section, String response) {
        if (response == null || response.trim().isEmpty()) {
            return;
        }
        getSharedPreferences(AUTH_PREFS, MODE_PRIVATE)
                .edit()
                .putString(profileSectionCacheKey(section), response)
                .apply();
    }

    private void updateRefreshStatus(final TextView refreshStatus, final String message) {
        if (refreshStatus == null) {
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                refreshStatus.setText(message);
            }
        });
    }

    private ProfileMetrics loadCachedProfileMetrics() {
        SharedPreferences preferences = getSharedPreferences(AUTH_PREFS, MODE_PRIVATE);
        String raw = preferences.getString(profileMetricsCacheKey(), "");
        if (raw == null || raw.trim().isEmpty()) {
            return defaultProfileMetrics();
        }
        try {
            JSONObject object = new JSONObject(raw);
            ProfileMetrics metrics = new ProfileMetrics();
            metrics.activityCount = object.optInt("activityCount", 0);
            metrics.competitionCount = object.optInt("competitionCount", 0);
            metrics.clubCount = object.optInt("clubCount", 0);
            metrics.notificationCount = object.optInt("notificationCount", 0);
            return metrics;
        } catch (Exception ignored) {
            return defaultProfileMetrics();
        }
    }

    private void saveCachedProfileMetrics(ProfileMetrics metrics) {
        if (metrics == null) {
            return;
        }
        try {
            JSONObject object = new JSONObject();
            object.put("activityCount", metrics.activityCount);
            object.put("competitionCount", metrics.competitionCount);
            object.put("clubCount", metrics.clubCount);
            object.put("notificationCount", metrics.notificationCount);
            getSharedPreferences(AUTH_PREFS, MODE_PRIVATE)
                    .edit()
                    .putString(profileMetricsCacheKey(), object.toString())
                    .apply();
        } catch (Exception ignored) {
        }
    }

    private boolean sameProfileMetrics(ProfileMetrics left, ProfileMetrics right) {
        if (left == null || right == null) {
            return false;
        }
        return left.activityCount == right.activityCount
                && left.competitionCount == right.competitionCount
                && left.clubCount == right.clubCount
                && left.notificationCount == right.notificationCount;
    }

    private void loadTenants() {
        baseUrl = normalizeBaseUrl(DEFAULT_BASE_URL);
        if (tenantListContainer != null) {
            tenantListContainer.removeAllViews();
            tenantListContainer.addView(text("正在加载租户列表...", 13, MUTED, Typeface.NORMAL));
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String result = request("GET", "/tenants/list", null, false);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            renderTenantPicker(result);
                        }
                    });
                } catch (Exception e) {
                    if (tenantListContainer != null) {
                        final String message = e.getMessage();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tenantListContainer.removeAllViews();
                                tenantListContainer.addView(text("租户加载失败：" + message, 13, DANGER, Typeface.NORMAL));
                            }
                        });
                    }
                }
            }
        }).start();
    }

    private void login() {
        if (enforceMigrationGate()) {
            return;
        }
        if (enforceUpdateGate()) {
            return;
        }
        if (requireSuccessfulUpdateCheckBeforeLogin()) {
            return;
        }
        baseUrl = normalizeBaseUrl(DEFAULT_BASE_URL);
        String selectedTenantId = tenantId == null ? "" : tenantId.trim();
        tenantId = selectedTenantId.isEmpty() ? DEFAULT_TENANT_ID : selectedTenantId;
        final String username = usernameInput.getText().toString().trim();
        final String password = passwordInput.getText().toString();

        if (username.isEmpty() || password.isEmpty()) {
            statusText.setText("请输入用户名和密码");
            statusText.setTextColor(DANGER);
            return;
        }

        statusText.setText("正在调用 /auth/login ...");
        statusText.setTextColor(MUTED);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String payload = "{\"username\":\"" + escapeJson(username) + "\",\"password\":\"" + escapeJson(password)
                            + "\",\"organizationId\":\"" + escapeJson(tenantId) + "\",\"clientType\":\"mobile\"}";
                    String response = request("POST", "/auth/login", payload, false);
                    parseLogin(response, username);
                    syncProfileIdentity();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showShell();
                        }
                    });
                } catch (Exception e) {
                    final String message = e.getMessage();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            statusText.setText("登录失败：" + message);
                            statusText.setTextColor(DANGER);
                        }
                    });
                }
            }
        }).start();
    }

    private void sendEmailLoginCode() {
        if (enforceMigrationGate() || enforceUpdateGate()) {
            return;
        }
        final String email = emailInput == null ? "" : emailInput.getText().toString().trim();
        if (email.isEmpty()) {
            statusText.setText("请输入邮箱");
            statusText.setTextColor(DANGER);
            return;
        }
        statusText.setText("正在发送邮箱验证码...");
        statusText.setTextColor(MUTED);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String payload = "{\"email\":\"" + escapeJson(email) + "\",\"tenantId\":0,\"scene\":\"LOGIN\"}";
                    request("POST", "/api/email-verification/send", payload, false);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            statusText.setText("验证码已发送，请查看邮箱");
                            statusText.setTextColor(PRIMARY);
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            statusText.setText("验证码发送失败：" + e.getMessage());
                            statusText.setTextColor(DANGER);
                        }
                    });
                }
            }
        }).start();
    }

    private void emailCodeLogin() {
        if (enforceMigrationGate() || enforceUpdateGate()) {
            return;
        }
        if (requireSuccessfulUpdateCheckBeforeLogin()) {
            return;
        }
        final String email = emailInput == null ? "" : emailInput.getText().toString().trim();
        final String emailCode = emailCodeInput == null ? "" : emailCodeInput.getText().toString().trim();
        if (email.isEmpty() || emailCode.isEmpty()) {
            statusText.setText("请输入邮箱和验证码");
            statusText.setTextColor(DANGER);
            return;
        }
        statusText.setText("正在验证邮箱登录...");
        statusText.setTextColor(MUTED);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String payload = "{\"email\":\"" + escapeJson(email) + "\",\"emailCode\":\"" + escapeJson(emailCode) + "\"}";
                    String response = request("POST", "/auth/email-login/prepare", payload, false);
                    JSONObject root = new JSONObject(response);
                    if (root.has("code") && root.optInt("code", 200) != 200) {
                        throw new IOException(root.optString("msg", root.optString("message", "邮箱登录失败")));
                    }
                    JSONObject data = root.optJSONObject("data");
                    if (data == null) {
                        data = root;
                    }
                    final String loginTicket = data.optString("loginTicket", "");
                    final JSONArray candidates = data.optJSONArray("candidates");
                    if (loginTicket.isEmpty() || candidates == null || candidates.length() == 0) {
                        throw new IOException("没有可登录的邮箱账号");
                    }
                    if (candidates.length() == 1) {
                        confirmEmailLogin(loginTicket, candidates.optJSONObject(0));
                        return;
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showEmailLoginCandidateDialog(loginTicket, candidates);
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            statusText.setText("邮箱登录失败：" + e.getMessage());
                            statusText.setTextColor(DANGER);
                        }
                    });
                }
            }
        }).start();
    }

    private void showEmailLoginCandidateDialog(final String loginTicket, final JSONArray candidates) {
        final String[] labels = new String[candidates.length()];
        for (int i = 0; i < candidates.length(); i++) {
            labels[i] = emailLoginCandidateLabel(candidates.optJSONObject(i));
        }
        new AlertDialog.Builder(this)
                .setTitle("选择登录身份")
                .setItems(labels, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int index) {
                        confirmEmailLogin(loginTicket, candidates.optJSONObject(index));
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void confirmEmailLogin(final String loginTicket, final JSONObject candidate) {
        if (candidate == null) {
            return;
        }
        statusText.setText("正在登录所选身份...");
        statusText.setTextColor(MUTED);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String candidateRoleId = candidate.optString("roleId", "");
                    String payload = "{\"loginTicket\":\"" + escapeJson(loginTicket)
                            + "\",\"authUserId\":" + candidate.optLong("authUserId", 0L)
                            + ",\"tenantId\":" + candidate.optLong("tenantId", 0L)
                            + (candidateRoleId.isEmpty() || "0".equals(candidateRoleId) ? "" : ",\"roleId\":" + escapeJson(candidateRoleId))
                            + "}";
                    String response = request("POST", "/auth/email-login/confirm", payload, false);
                    parseLogin(response, candidate.optString("username", "email"));
                    syncProfileIdentity();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showShell();
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            statusText.setText("邮箱登录失败：" + e.getMessage());
                            statusText.setTextColor(DANGER);
                        }
                    });
                }
            }
        }).start();
    }

    private String emailLoginCandidateLabel(JSONObject candidate) {
        if (candidate == null) {
            return "未知身份";
        }
        String itemTenantId = firstValue(candidate, new String[]{"tenantId", "organizationId", "id", "tenant_id"}, tenantId);
        String itemTenantName = tenantNameFromDirectory(tenantDirectory, itemTenantId, "租户 " + itemTenantId);
        String username = firstValue(candidate, new String[]{"displayName", "username", "userName", "name"}, "账号");
        String candidateRoleId = candidate.optString("roleId", "");
        return username + " · " + itemTenantName + " · " + roleLabel(candidateRoleId);
    }

    private void checkLatestRelease(final boolean showToast) {
        if (updateCheckPending) {
            return;
        }
        updateCheckPending = true;
        updateCheckCompleted = false;
        updateCheckError = "";
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    latestRelease = fetchLatestRelease();
                    updateCheckCompleted = true;
                    boolean hasUpdate = latestRelease.optBoolean("hasUpdate", false);
                    boolean forceUpgrade = latestRelease.optBoolean("forceUpgrade", false);
                    forceUpgradeActive = forceUpgrade && hasUpdate;
                    if (showToast || hasUpdate || forceUpgradeActive) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showBlockingUpdateDialog(latestRelease);
                            }
                        });
                    }
                } catch (final Exception e) {
                    updateCheckError = e.getMessage() == null ? "未知错误" : e.getMessage();
                    if (showToast && statusText != null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                statusText.setText("更新检查失败：" + updateCheckError);
                                statusText.setTextColor(DANGER);
                            }
                        });
                    }
                } finally {
                    updateCheckPending = false;
                }
            }
        }).start();
    }

    private boolean requireSuccessfulUpdateCheckBeforeLogin() {
        if (latestRelease != null && updateCheckCompleted) {
            return false;
        }
        if (statusText != null) {
            String suffix = updateCheckError.isEmpty() ? "请稍候" : "请重试：" + updateCheckError;
            statusText.setText("登录前必须先完成升级检查，" + suffix);
            statusText.setTextColor(updateCheckError.isEmpty() ? MUTED : DANGER);
        }
        checkLatestRelease(true);
        return true;
    }

    private boolean enforceUpdateGate() {
        if (enforceForceUpgrade()) {
            return true;
        }
        if (!updateCheckPending) {
            return false;
        }
        if (statusText != null) {
            statusText.setText("正在检查是否需要强制更新，请稍候");
            statusText.setTextColor(MUTED);
        }
        return true;
    }

    private boolean enforceMigrationGate() {
        if (!serviceMigrationActive) {
            return false;
        }
        showMigrationBlockingDialog(activeMigrationRecord);
        if (statusText != null) {
            statusText.setText("服务迁移正在进行，请等待迁移完成");
            statusText.setTextColor(MUTED);
        }
        return true;
    }

    private void startMigrationStatusPolling() {
        if (migrationPollingActive) {
            return;
        }
        migrationPollingActive = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (migrationPollingActive) {
                    try {
                        final JSONObject record = fetchActiveMigration();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (record != null && record.optLong("id", 0L) > 0L) {
                                    serviceMigrationActive = true;
                                    activeMigrationRecord = record;
                                    ensureMigrationStartedAt(record);
                                    showMigrationBlockingDialog(record);
                                } else {
                                    serviceMigrationActive = false;
                                    activeMigrationRecord = null;
                                    dismissMigrationBlockingDialog();
                                }
                            }
                        });
                    } catch (Exception ignored) {
                        // Keep the current dialog state on transient network failures.
                    }
                    try {
                        Thread.sleep(3000L);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }, "rk-mobile-migration-poller").start();
    }

    private JSONObject fetchActiveMigration() throws IOException {
        String response = request("GET", MIGRATION_STATUS_PATH, null, false);
        try {
            JSONObject root = new JSONObject(response);
            if (root.has("code") && root.optInt("code", 200) != 200) {
                return null;
            }
            return root.optJSONObject("data");
        } catch (Exception e) {
            throw new IOException("迁移状态解析失败：" + e.getMessage(), e);
        }
    }

    private void showMigrationBlockingDialog(final JSONObject record) {
        if (record == null) {
            return;
        }
        ensureMigrationStartedAt(record);
        if (serviceMigrationDialog != null && serviceMigrationDialog.isShowing()) {
            updateMigrationDialog(record);
            return;
        }
        migrationProgressBar = null;
        migrationProgressText = null;
        migrationStepText = null;
        migrationElapsedText = null;
        migrationRemainingText = null;
        migrationLogText = null;
        final LinearLayout contentView = buildMigrationDialogContent(record);
        serviceMigrationDialog = new AlertDialog.Builder(this)
                .setView(contentView)
                .setCancelable(false)
                .create();
        serviceMigrationDialog.setCanceledOnTouchOutside(false);
        serviceMigrationDialog.show();
        updateMigrationDialog(record);
        startMigrationElapsedTicker();
    }

    private LinearLayout buildMigrationDialogContent(JSONObject record) {
        LinearLayout wrapper = vertical();
        wrapper.setPadding(dp(22), dp(22), dp(22), dp(18));
        wrapper.setBackground(roundRect(Color.rgb(246, 250, 255), dp(18)));

        TextView badge = text("服务迁移", 12, PRIMARY, Typeface.BOLD);
        badge.setPadding(dp(10), dp(4), dp(10), dp(4));
        badge.setBackground(roundStroke(Color.rgb(239, 246, 255), Color.rgb(191, 219, 254), dp(14)));
        wrapper.addView(badge);

        TextView title = text("服务迁移正在进行", 21, TEXT, Typeface.BOLD);
        title.setPadding(0, dp(14), 0, dp(4));
        wrapper.addView(title);

        String targetCluster = firstValue(record, new String[]{"remoteClusterName", "targetNamespace", "clusterName"}, "目标集群");
        TextView route = text("当前集群 → " + targetCluster, 13, PRIMARY, Typeface.BOLD);
        route.setPadding(dp(10), dp(8), dp(10), dp(8));
        route.setBackground(roundStroke(Color.rgb(235, 243, 255), Color.rgb(199, 221, 255), dp(12)));
        wrapper.addView(route);

        TextView desc = text("迁移期间其他租户也会看到此提示，所有用户暂时不能操作，迁移完成后会自动关闭。", 14, MUTED, Typeface.NORMAL);
        desc.setPadding(0, 0, 0, dp(14));
        desc.setLineSpacing(dp(2), 1.0f);
        wrapper.addView(desc);

        migrationProgressText = text("迁移进度 0%", 16, TEXT, Typeface.BOLD);
        wrapper.addView(migrationProgressText);

        migrationProgressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        migrationProgressBar.setMax(100);
        migrationProgressBar.setIndeterminate(false);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(-1, dp(18));
        progressParams.setMargins(0, dp(12), 0, dp(10));
        wrapper.addView(migrationProgressBar, progressParams);

        migrationStepText = text("当前步骤：等待后端写入迁移进度", 14, TEXT, Typeface.NORMAL);
        migrationStepText.setPadding(dp(12), dp(10), dp(12), dp(10));
        migrationStepText.setBackground(roundStroke(Color.rgb(240, 249, 255), Color.rgb(191, 219, 254), dp(10)));
        wrapper.addView(migrationStepText);

        LinearLayout timeRow = horizontal();
        timeRow.setPadding(0, dp(12), 0, 0);
        LinearLayout.LayoutParams elapsedParams = new LinearLayout.LayoutParams(0, -2, 1);
        elapsedParams.setMargins(0, 0, dp(8), 0);
        migrationElapsedText = text("已用时间：0秒", 13, TEXT, Typeface.BOLD);
        migrationElapsedText.setPadding(dp(12), dp(10), dp(12), dp(10));
        migrationElapsedText.setBackground(roundStroke(Color.rgb(240, 249, 255), Color.rgb(191, 219, 254), dp(10)));
        timeRow.addView(migrationElapsedText, elapsedParams);

        migrationRemainingText = text("预计剩余：计算中", 13, PRIMARY, Typeface.BOLD);
        migrationRemainingText.setPadding(dp(12), dp(10), dp(12), dp(10));
        migrationRemainingText.setBackground(roundStroke(Color.rgb(236, 253, 245), Color.rgb(134, 239, 172), dp(10)));
        timeRow.addView(migrationRemainingText, new LinearLayout.LayoutParams(0, -2, 1));
        wrapper.addView(timeRow);

        migrationLogText = text("最近日志：\n> 等待迁移日志", 12, Color.rgb(187, 247, 208), Typeface.NORMAL);
        migrationLogText.setPadding(dp(12), dp(10), dp(12), dp(10));
        migrationLogText.setLineSpacing(dp(2), 1.0f);
        migrationLogText.setMaxLines(5);
        migrationLogText.setBackground(roundStroke(Color.rgb(2, 6, 23), Color.rgb(34, 197, 94), dp(10)));
        wrapper.addView(migrationLogText);
        return wrapper;
    }

    private void updateMigrationDialog(JSONObject record) {
        ensureMigrationStartedAt(record);
        int progress = Math.max(0, Math.min(100, record == null ? 0 : record.optInt("progress", 0)));
        if (migrationProgressBar != null) {
            migrationProgressBar.setIndeterminate(progress <= 0);
            migrationProgressBar.setProgress(progress);
        }
        if (migrationProgressText != null) {
            migrationProgressText.setText("迁移进度 " + progress + "%");
        }
        if (migrationStepText != null) {
            String step = firstValue(record, new String[]{"currentStep", "currentImage"}, latestMigrationLogLine(record));
            migrationStepText.setText("当前步骤：" + formatMigrationStep(step));
        }
        if (migrationRemainingText != null) {
            syncMigrationRemainingEstimate(record);
            updateMigrationRemainingText();
        }
        if (migrationLogText != null) {
            migrationLogText.setText(formatMigrationLogSnippet(record));
        }
        updateMigrationElapsedText();
    }

    private void ensureMigrationStartedAt(JSONObject record) {
        long serverStartedAt = parseMigrationStartedAt(record);
        if (serverStartedAt > 0L) {
            migrationStartedAtMillis = serverStartedAt;
            return;
        }
        if (migrationStartedAtMillis <= 0L) {
            migrationStartedAtMillis = System.currentTimeMillis();
        }
    }

    private long parseMigrationStartedAt(JSONObject record) {
        String millisValue = firstValue(record, new String[]{"migrationStartedAtMillis", "deployStartedAtMillis", "createTimeMillis", "startTimeMillis"}, "");
        if (!millisValue.isEmpty()) {
            try {
                long raw = Long.parseLong(millisValue);
                return raw > 100000000000L ? raw : raw * 1000L;
            } catch (Exception ignored) {
                // Try formatted timestamps below.
            }
        }
        String value = firstValue(record, new String[]{"migrationStartedAt", "deployStartedAt", "createTime", "startTime"}, "");
        if (value.isEmpty()) {
            return 0L;
        }
        try {
            long raw = Long.parseLong(value);
            return raw > 100000000000L ? raw : raw * 1000L;
        } catch (Exception ignored) {
            // Try formatted timestamps below.
        }
        String[] patterns = new String[]{"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss"};
        for (int i = 0; i < patterns.length; i++) {
            try {
                SimpleDateFormat formatter = new SimpleDateFormat(patterns[i], Locale.ROOT);
                Date parsed = formatter.parse(value);
                if (parsed != null) {
                    return parsed.getTime();
                }
            } catch (Exception ignored) {
                // Continue to the next timestamp pattern.
            }
        }
        return 0L;
    }

    private void startMigrationElapsedTicker() {
        if (migrationTickTimer != null) {
            return;
        }
        migrationTickTimer = new Runnable() {
            @Override
            public void run() {
                updateMigrationElapsedText();
                updateMigrationRemainingText();
                if (migrationElapsedText != null && serviceMigrationActive) {
                    migrationElapsedText.postDelayed(this, 1000L);
                }
            }
        };
        if (migrationElapsedText != null) {
            migrationElapsedText.post(migrationTickTimer);
        }
    }

    private void stopMigrationElapsedTicker() {
        if (migrationElapsedText != null && migrationTickTimer != null) {
            migrationElapsedText.removeCallbacks(migrationTickTimer);
        }
        migrationTickTimer = null;
        migrationStartedAtMillis = 0L;
        resetMigrationRemainingEstimate();
    }

    private void updateMigrationElapsedText() {
        if (migrationElapsedText == null) {
            return;
        }
        if (migrationStartedAtMillis <= 0L) {
            migrationStartedAtMillis = System.currentTimeMillis();
        }
        long elapsed = Math.max(0L, (System.currentTimeMillis() - migrationStartedAtMillis) / 1000L);
        migrationElapsedText.setText("已用时间：" + formatMigrationElapsed(elapsed));
    }

    private void resetMigrationRemainingEstimate() {
        migrationRemainingBaseSeconds = 0;
        migrationRemainingBaseAtMillis = 0L;
        migrationRemainingRecordId = "";
    }

    private void syncMigrationRemainingEstimate(JSONObject record) {
        String recordId = firstValue(record, new String[]{"id", "packageId"}, "");
        int nextRemaining = record == null ? 0 : record.optInt("estimatedRemainingSeconds", 0);
        if (recordId.isEmpty() || nextRemaining <= 0) {
            if (recordId.isEmpty()) {
                resetMigrationRemainingEstimate();
            }
            return;
        }
        if (!recordId.equals(migrationRemainingRecordId) || nextRemaining != migrationRemainingBaseSeconds) {
            migrationRemainingRecordId = recordId;
            migrationRemainingBaseSeconds = nextRemaining;
            migrationRemainingBaseAtMillis = System.currentTimeMillis();
        }
    }

    private int currentMigrationRemainingSeconds() {
        if (migrationRemainingBaseSeconds <= 0 || migrationRemainingBaseAtMillis <= 0L) {
            return 0;
        }
        long passed = Math.max(0L, (System.currentTimeMillis() - migrationRemainingBaseAtMillis) / 1000L);
        return (int) Math.max(0L, migrationRemainingBaseSeconds - passed);
    }

    private void updateMigrationRemainingText() {
        if (migrationRemainingText == null) {
            return;
        }
        migrationRemainingText.setText("预计剩余：" + formatRemainingSeconds(currentMigrationRemainingSeconds()));
    }

    private String formatMigrationElapsed(long seconds) {
        long value = Math.max(0L, seconds);
        long hours = value / 3600L;
        long minutes = (value % 3600L) / 60L;
        long rest = value % 60L;
        if (hours > 0L) {
            return hours + "小时" + minutes + "分" + rest + "秒";
        }
        if (minutes > 0L) {
            return minutes + "分" + rest + "秒";
        }
        return rest + "秒";
    }

    private String latestMigrationLogLine(JSONObject record) {
        String logs = firstValue(record, new String[]{"logs"}, "");
        if (logs.isEmpty()) {
            return "等待后端写入迁移进度";
        }
        String[] lines = logs.split("\\r?\\n");
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = trim(lines[i]);
            if (!line.isEmpty()) {
                return line;
            }
        }
        return "等待后端写入迁移进度";
    }

    private String formatMigrationLogSnippet(JSONObject record) {
        String logs = firstValue(record, new String[]{"logs"}, "");
        if (logs.isEmpty()) {
            return "最近日志：\n> 等待迁移日志";
        }
        String[] lines = logs.split("\\r?\\n");
        ArrayList<String> recent = new ArrayList<>();
        for (int i = Math.max(0, lines.length - 4); i < lines.length; i++) {
            String line = trim(lines[i]);
            if (!line.isEmpty()) {
                recent.add("> " + formatMigrationStep(line));
            }
        }
        if (recent.isEmpty()) {
            return "最近日志：\n> 等待迁移日志";
        }
        StringBuilder builder = new StringBuilder("最近日志：");
        for (String line : recent) {
            builder.append('\n').append(line);
        }
        return builder.toString();
    }

    private String formatMigrationStep(String rawStep) {
        String step = trim(rawStep);
        if (step.isEmpty()) {
            return "等待后端写入迁移进度";
        }
        if (step.matches(".*[\\u4e00-\\u9fa5].*")) {
            return step;
        }
        String lower = step.toLowerCase(Locale.ROOT);
        if (lower.contains("global migration lock")) {
            return "正在启用全局迁移锁";
        }
        if (lower.contains("preflight")) {
            return "正在执行部署前预检";
        }
        if (lower.contains("exporting current runtime images") || lower.contains("runtime image export")) {
            return "正在导出当前运行镜像";
        }
        if (lower.contains("image pull secret") || (lower.contains("pull") && lower.contains("secret"))) {
            return "正在配置目标集群镜像拉取凭据";
        }
        if (lower.contains("push") || lower.contains("upload") || lower.contains("image") || lower.contains("currentimage")) {
            return "正在迁移镜像";
        }
        if (lower.contains("payload") || lower.contains("staging")) {
            return "正在传输迁移数据包";
        }
        if (lower.contains("manifest") || lower.contains("apply -f") || lower.contains("applied")) {
            return "正在下发 Kubernetes 编排清单";
        }
        if (lower.contains("statefulset")) {
            return "正在等待 MySQL、Redis、MinIO 等有状态服务启动";
        }
        if (lower.contains("mysql") || lower.contains("database")) {
            return "正在导入 MySQL 数据";
        }
        if (lower.contains("minio")) {
            return "正在导入 MinIO 文件";
        }
        if (lower.contains("nacos")) {
            return "正在重启 Nacos 并加载配置";
        }
        if (lower.contains("workload") || lower.contains("restarting application")) {
            return "正在重启业务微服务";
        }
        if (lower.contains("rollout")) {
            return "正在校验服务启动状态";
        }
        if (lower.contains("cleanup") || lower.contains("delete temporary") || lower.contains("cleaning")) {
            return "正在清理临时迁移资源";
        }
        if (lower.contains("completed") || lower.contains("success")) {
            return "迁移完成，正在恢复访问";
        }
        if (lower.contains("failed") || lower.contains("error")) {
            return "迁移失败，等待管理员处理";
        }
        return step;
    }
    private void dismissMigrationBlockingDialog() {
        stopMigrationElapsedTicker();
        if (serviceMigrationDialog != null && serviceMigrationDialog.isShowing()) {
            serviceMigrationDialog.dismiss();
        }
        serviceMigrationDialog = null;
        migrationProgressBar = null;
        migrationProgressText = null;
        migrationStepText = null;
        migrationElapsedText = null;
        migrationRemainingText = null;
        migrationLogText = null;
    }
    private boolean enforceForceUpgrade() {
        if (!forceUpgradeActive) {
            return false;
        }
        if (latestRelease != null) {
            showBlockingUpdateDialog(latestRelease);
        } else {
            checkLatestRelease(true);
        }
        return true;
    }

    private void showBlockingUpdateDialog(final JSONObject release) {
        if (release == null || updateDialogShowing) {
            return;
        }
        showStyledUpdateDialog(release);
    }

    private LinearLayout buildUpdateDialogContent(final JSONObject release, final AlertDialog dialog) {
        final boolean hasUpdate = release.optBoolean("hasUpdate", false);
        final boolean forceUpgrade = release.optBoolean("forceUpgrade", false);
        final String downloadUrl = release.optString("downloadUrl", "");
        String version = release.optString("versionName", "-");
        String notes = release.optString("releaseNotes", release.optString("message", ""));
        LinearLayout wrapper = vertical();
        wrapper.setPadding(dp(22), dp(22), dp(22), dp(18));
        wrapper.setBackground(roundRect(SURFACE, dp(18)));

        LinearLayout badgeRow = horizontal();
        badgeRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView badge = text(forceUpgrade ? "强制更新" : "可选更新", 12, forceUpgrade ? DANGER : PRIMARY, Typeface.BOLD);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(10), dp(4), dp(10), dp(4));
        badge.setBackground(roundStroke(forceUpgrade ? Color.rgb(254, 242, 242) : Color.rgb(236, 253, 245), forceUpgrade ? Color.rgb(252, 165, 165) : Color.rgb(167, 243, 208), dp(14)));
        badgeRow.addView(badge);
        TextView versionBadge = text("v" + version, 12, MUTED, Typeface.BOLD);
        versionBadge.setPadding(dp(10), 0, 0, 0);
        badgeRow.addView(versionBadge);
        wrapper.addView(badgeRow);

        TextView title = text(forceUpgrade ? "需要完成升级后继续使用" : "发现新版本", 21, TEXT, Typeface.BOLD);
        title.setPadding(0, dp(14), 0, dp(4));
        wrapper.addView(title);

        TextView subtitle = text("当前版本码 " + getInstalledVersionCode() + "，最新版本 " + version, 14, MUTED, Typeface.NORMAL);
        subtitle.setPadding(0, 0, 0, dp(12));
        wrapper.addView(subtitle);

        if (!notes.isEmpty()) {
            TextView noteView = text(notes, 14, TEXT, Typeface.NORMAL);
            noteView.setPadding(dp(12), dp(10), dp(12), dp(10));
            noteView.setLineSpacing(dp(2), 1.0f);
            noteView.setBackground(roundStroke(SURFACE_ALT, BORDER, dp(10)));
            wrapper.addView(noteView, new LinearLayout.LayoutParams(-1, -2));
        }

        TextView hint = text(forceUpgrade
                ? "这是 immediate 风格的阻断更新，安装完成前其他功能会保持锁定。"
                : "这是 flexible 风格的可选更新，可以稍后处理，也可以现在下载。", 13, MUTED, Typeface.NORMAL);
        hint.setPadding(0, dp(12), 0, 0);
        wrapper.addView(hint);

        Button updateButton = primaryButton(downloadUrl.isEmpty() ? "查看详情" : "立即更新");
        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateDialogShowing = false;
                if (dialog != null) {
                    dialog.dismiss();
                }
                if (downloadUrl.isEmpty()) {
                    if (content != null) {
                        navigateChild(updateCenterScreenAction());
                    }
                    return;
                }
                downloadApkFromDialog(downloadUrl, forceUpgrade);
            }
        });
        wrapper.addView(updateButton, buttonParams());

        if (!forceUpgrade) {
            Button laterButton = secondaryButton("稍后再说");
            laterButton.setTextSize(15);
            laterButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    updateDialogShowing = false;
                    if (dialog != null) {
                        dialog.dismiss();
                    }
                }
            });
            wrapper.addView(laterButton, compactButtonParams());
        }
        return wrapper;
    }

    private void showStyledUpdateDialog(final JSONObject release) {
        final boolean hasUpdate = release.optBoolean("hasUpdate", false);
        final boolean forceUpgrade = release.optBoolean("forceUpgrade", false);
        if (!hasUpdate) {
            return;
        }
        forceUpgradeActive = forceUpgrade && hasUpdate;
        updateDialogShowing = true;
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setCancelable(!forceUpgrade)
                .create();
        dialog.setView(buildUpdateDialogContent(release, dialog));
        dialog.setCanceledOnTouchOutside(!forceUpgrade);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                updateDialogShowing = false;
            }
        });
        dialog.show();
    }

    private void downloadApkFromDialog(final String downloadUrl, final boolean forceUpgrade) {
        final UpdateProgressViews progressViews = buildDownloadProgressContent(forceUpgrade);
        final AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setView(progressViews.content)
                .setCancelable(!forceUpgrade)
                .create();
        progressDialog.setCanceledOnTouchOutside(!forceUpgrade);
        progressDialog.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final File apkFile = downloadToFile(resolveAbsoluteUrl(downloadUrl), new DownloadProgressCallback() {
                        @Override
                        public void onDownloadProgress(final long downloaded, final long total) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateDialogProgressText(progressViews, downloaded, total);
                                }
                            });
                        }
                    });
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dismiss();
                            installApk(apkFile);
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dismiss();
                            AlertDialog.Builder errorBuilder = new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("更新下载失败")
                                    .setMessage(e.getMessage())
                                    .setPositiveButton("重试", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            downloadApkFromDialog(downloadUrl, forceUpgrade);
                                        }
                                    })
                                    .setCancelable(!forceUpgrade);
                            if (!forceUpgrade) {
                                errorBuilder.setNegativeButton("稍后", null);
                            }
                            errorBuilder.show();
                        }
                    });
                }
            }
        }).start();
    }

    private JSONObject fetchLatestRelease() throws Exception {
        String path = "/api/mobile/releases/latest?tenantId=" + urlEncode(tenantId)
                + "&roleId=" + roleId
                + "&versionCode=" + getInstalledVersionCode();
        JSONObject root = new JSONObject(request("GET", path, null, false));
        JSONObject data = root.optJSONObject("data");
        return data == null ? new JSONObject() : data;
    }

    private void renderRelease(LinearLayout body, final JSONObject release) {
        LinearLayout card = card();
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        body.addView(card);
        boolean hasUpdate = release.optBoolean("hasUpdate", false);
        boolean forceUpgrade = release.optBoolean("forceUpgrade", false);
        forceUpgradeActive = forceUpgrade && hasUpdate;
        card.addView(text(hasUpdate ? "发现新版本" : "当前已是最新版本", 22, TEXT, Typeface.BOLD));
        card.addView(text("当前版本码：" + getInstalledVersionCode(), 14, MUTED, Typeface.NORMAL));
        card.addView(text("最新版本：" + release.optString("versionName", "1.0.0") + "（" + release.optInt("versionCode", 0) + "）", 14, MUTED, Typeface.NORMAL));
        card.addView(text(forceUpgrade ? "后台要求强制升级" : "可选升级", 14, forceUpgrade ? DANGER : PRIMARY, Typeface.BOLD));
        String notes = release.optString("releaseNotes", "");
        if (!notes.isEmpty()) {
            TextView noteView = text(notes, 14, MUTED, Typeface.NORMAL);
            noteView.setPadding(0, dp(10), 0, 0);
            card.addView(noteView);
        }
        String message = release.optString("message", "");
        if (!message.isEmpty()) {
            card.addView(text(message, 14, MUTED, Typeface.NORMAL));
        }
        if (hasUpdate && !release.optString("downloadUrl", "").isEmpty()) {
            Button downloadButton = primaryButton("下载并安装");
            downloadButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    downloadApk(release.optString("downloadUrl", ""));
                }
            });
            card.addView(downloadButton, buttonParams());
        }
    }

    private void downloadApk(final String downloadUrl) {
        final ScrollView scrollView = screenScaffold("升级", "APK 下载");
        final LinearLayout body = (LinearLayout) scrollView.getChildAt(0);
        final TextView status = text("正在下载 APK ...", 14, MUTED, Typeface.NORMAL);
        final ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setIndeterminate(true);
        body.addView(status);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(-1, dp(18));
        progressParams.setMargins(0, dp(12), 0, dp(12));
        body.addView(progressBar, progressParams);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final File apkFile = downloadToFile(resolveAbsoluteUrl(downloadUrl), new DownloadProgressCallback() {
                        @Override
                        public void onDownloadProgress(final long downloaded, final long total) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateDownloadProgress(status, progressBar, downloaded, total);
                                }
                            });
                        }
                    });
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            status.setText("下载完成，正在打开系统安装器");
                            installApk(apkFile);
                        }
                    });
                } catch (Exception e) {
                    showLoadError(status, e);
                }
            }
        }).start();
    }

    private interface DownloadProgressCallback {
        void onDownloadProgress(long downloaded, long total);
    }

    private File downloadToFile(String downloadUrl, DownloadProgressCallback callback) throws IOException {
        File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (dir == null) {
            dir = getFilesDir();
        }
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File apkFile = new File(dir, APK_FILE_NAME);
        HttpURLConnection connection = (HttpURLConnection) new URL(downloadUrl).openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);
        connection.setRequestProperty("Accept", "application/vnd.android.package-archive,*/*");
        int status = connection.getResponseCode();
        if (status >= 400) {
            throw new IOException("HTTP " + status + " " + readAll(connection.getErrorStream()));
        }
        final long total = connection.getContentLengthLong();
        InputStream input = new BufferedInputStream(connection.getInputStream());
        OutputStream output = new FileOutputStream(apkFile);
        byte[] buffer = new byte[8192];
        int read;
        long downloaded = 0L;
        if (callback != null) {
            callback.onDownloadProgress(downloaded, total);
        }
        while ((read = input.read(buffer)) >= 0) {
            output.write(buffer, 0, read);
            downloaded += read;
            if (callback != null) {
                callback.onDownloadProgress(downloaded, total);
            }
        }
        output.close();
        input.close();
        connection.disconnect();
        return apkFile;
    }

    private void updateDownloadProgress(TextView status, ProgressBar progressBar, long downloaded, long total) {
        if (total > 0) {
            int percent = (int) Math.min(100L, Math.max(0L, downloaded * 100L / total));
            progressBar.setIndeterminate(false);
            progressBar.setProgress(percent);
            status.setText("下载进度：" + percent + "%（" + formatBytes(downloaded) + " / " + formatBytes(total) + "）");
        } else {
            progressBar.setIndeterminate(true);
            status.setText("已下载：" + formatBytes(downloaded));
        }
    }

    private String formatBytes(long bytes) {
        if (bytes >= 1024L * 1024L) {
            return String.format(java.util.Locale.ROOT, "%.1f MB", bytes / 1024.0 / 1024.0);
        }
        if (bytes >= 1024L) {
            return String.format(java.util.Locale.ROOT, "%.1f KB", bytes / 1024.0);
        }
        return bytes + " B";
    }

    private String formatRemainingSeconds(int seconds) {
        int value = Math.max(0, seconds);
        if (value <= 0) {
            return "计算中";
        }
        return formatMigrationElapsed(value);
    }
    private void installApk(File apkFile) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.parse("content://com.rkclub.app.apkprovider/" + apkFile.getName());
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(intent);
        } catch (Exception e) {
            TextView error = text("无法打开安装器：" + e.getMessage(), 14, DANGER, Typeface.NORMAL);
            if (content != null) {
                content.addView(error);
            } else if (statusText != null) {
                statusText.setText(error.getText());
                statusText.setTextColor(DANGER);
            }
        }
    }

    private ScrollView screenScaffold(String title, String subtitle) {
        content.removeAllViews();
        renderBottomNavigation();
        ScrollView scrollView = new ScrollView(this);
        LinearLayout body = vertical();
        body.setPadding(dp(16), dp(10), dp(16), dp(18));
        scrollView.addView(body);
        content.addView(scrollView, new LinearLayout.LayoutParams(-1, -1));

        body.addView(topBar(title, subtitle));
        return scrollView;
    }

    private void renderTenantPicker(String response) {
        if (tenantListContainer == null) {
            return;
        }
        tenantListContainer.removeAllViews();
        try {
            JSONArray tenants = findDataArray(new JSONObject(response));
            tenantDirectory = tenants;
            JSONObject defaultTenant = selectDefaultTenant(tenants);
            if (defaultTenant != null) {
                applyTenantSelection(defaultTenant);
            }
            tenantListContainer.addView(text("已自动选择默认租户，可点右侧切换", 12, MUTED, Typeface.NORMAL));
            tenantSelectorShownOnLogin = tenants.length() > 1;
            checkLatestRelease(false);
        } catch (Exception e) {
            tenantListContainer.addView(text("租户解析失败", 13, DANGER, Typeface.NORMAL));
        }
    }

    private void showTenantSelector(JSONArray tenants) {
        if (enforceUpdateGate()) {
            return;
        }
        if (tenants == null || tenants.length() == 0) {
            if (statusText != null) {
                statusText.setText("租户列表还没有加载完成");
                statusText.setTextColor(DANGER);
            }
            loadTenants();
            return;
        }
        final JSONArray tenantList = tenants;
        String[] names = new String[tenantList.length()];
        for (int i = 0; i < tenantList.length(); i++) {
            JSONObject tenant = tenantList.optJSONObject(i);
            String name = tenant == null ? "tenant " + (i + 1) : tenant.optString("tenantName", "tenant " + tenant.optString("id"));
            names[i] = name;
        }
        new AlertDialog.Builder(this)
                .setTitle("选择租户")
                .setItems(names, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        JSONObject tenant = tenantList.optJSONObject(which);
                        if (tenant != null) {
                            applyTenantSelection(tenant);
                            if (tenantListContainer != null) {
                                tenantListContainer.removeAllViews();
                                tenantListContainer.addView(text("当前租户：" + tenantName, 13, MUTED, Typeface.NORMAL));
                            }
                        }
                    }
                })
                .show();
    }

    private JSONObject selectDefaultTenant(JSONArray tenants) {
        JSONObject fallback = null;
        JSONObject systemTenant = null;
        JSONObject publicTenant = null;
        String currentTenantId = tenantId == null ? "" : tenantId.trim();
        for (int i = 0; i < tenants.length(); i++) {
            JSONObject tenant = tenants.optJSONObject(i);
            if (tenant == null) {
                continue;
            }
            String id = tenant.optString("id", "");
            if (fallback == null) {
                fallback = tenant;
            }
            if (!currentTenantId.isEmpty() && currentTenantId.equals(id)) {
                return tenant;
            }
            if (DEFAULT_TENANT_ID.equals(id)) {
                publicTenant = tenant;
            }
            if ("1".equals(id)) {
                systemTenant = tenant;
            }
        }
        if (publicTenant != null) {
            return publicTenant;
        }
        return systemTenant == null ? fallback : systemTenant;
    }

    private void applyTenantSelection(JSONObject tenant) {
        tenantId = tenant.optString("id", "1");
        tenantName = tenant.optString("tenantName", "tenant " + tenantId);
        if (tenantDisplay != null) {
            tenantDisplay.setText("租户：" + tenantName);
        }
    }

    private void renderAccountTenantCards(LinearLayout body, String directoryResponse, String accountTenantResponse) {
        JSONArray directory = new JSONArray();
        try {
            directory = findDataArray(new JSONObject(directoryResponse));
            tenantDirectory = directory;
        } catch (Exception ignored) {
        }

        body.addView(sectionTitle("账号所在租户"));
        boolean renderedMembership = false;
        try {
            JSONArray memberships = accountTenantResponse == null || accountTenantResponse.trim().isEmpty()
                    ? new JSONArray()
                    : findDataArray(new JSONObject(accountTenantResponse));
            for (int i = 0; i < memberships.length(); i++) {
                JSONObject item = memberships.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                renderAccountTenantCard(body, item, directory);
                renderedMembership = true;
            }
        } catch (Exception ignored) {
            renderedMembership = false;
        }

        if (!renderedMembership) {
            JSONObject current = new JSONObject();
            try {
                current.put("tenantId", tenantId);
                current.put("tenantName", tenantName);
                current.put("username", displayName);
                current.put("roleId", roleId);
            } catch (Exception ignored) {
            }
            renderAccountTenantCard(body, current, directory);
        }

        body.addView(sectionTitle("全部租户目录"));
        renderTenantCards(body, directoryResponse, true);
    }

    private void renderAccountTenantCard(LinearLayout body, JSONObject item, JSONArray directory) {
        String itemTenantId = firstValue(item, new String[]{"tenantId", "organizationId", "id", "tenant_id"}, tenantId);
        String itemTenantName = tenantNameFromDirectory(directory, itemTenantId,
                firstValue(item, new String[]{"tenantName", "organizationName", "name"}, "租户 " + itemTenantId));
        String itemRoleId = firstValue(item, new String[]{"roleId", "role_id"}, "");
        boolean current = isCurrentTenant(itemTenantId, itemRoleId);

        LinearLayout card = card();
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, dp(12));
        body.addView(card, params);
        LinearLayout titleRow = horizontal();
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(titleRow);
        titleRow.addView(text(itemTenantName, 18, TEXT, Typeface.BOLD), new LinearLayout.LayoutParams(0, -2, 1));
        if (current) {
            titleRow.addView(tag("当前登录"));
        } else {
            final JSONObject switchItem = item;
            Button switchButton = secondaryButton("切换身份");
            switchButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    switchAccountTenant(switchItem);
                }
            });
            titleRow.addView(switchButton, new LinearLayout.LayoutParams(dp(92), dp(36)));
        }
        card.addView(text("租户编号：" + itemTenantId, 14, MUTED, Typeface.NORMAL));
        String username = firstValue(item, new String[]{"username", "userName", "name"}, displayName);
        if (!username.isEmpty()) {
            card.addView(text("账号：" + username, 14, MUTED, Typeface.NORMAL));
        }
        if (!itemRoleId.isEmpty() && !"0".equals(itemRoleId)) {
            card.addView(text("角色：" + roleLabel(itemRoleId), 14, MUTED, Typeface.NORMAL));
        }
    }

    private boolean isCurrentTenant(String candidateTenantId) {
        return trim(tenantId).equals(trim(candidateTenantId));
    }

    private boolean isCurrentTenant(String candidateTenantId, String candidateRoleId) {
        if (!trim(tenantId).equals(trim(candidateTenantId))) {
            return false;
        }
        String normalizedRoleId = trim(candidateRoleId);
        return normalizedRoleId.isEmpty() || "0".equals(normalizedRoleId) || String.valueOf(roleId).equals(normalizedRoleId);
    }

    private void switchAccountTenant(final JSONObject item) {
        if (item == null || enforceMigrationGate() || enforceUpdateGate()) {
            return;
        }
        final String itemTenantId = firstValue(item, new String[]{"tenantId", "organizationId", "id", "tenant_id"}, tenantId);
        final String itemRoleId = firstValue(item, new String[]{"roleId", "role_id"}, "");
        final String username = firstValue(item, new String[]{"username", "userName", "name"}, displayName);
        if (itemTenantId.isEmpty()) {
            Toast.makeText(this, "缺少租户信息", Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String path = "/auth/switch-tenant/" + urlEncode(itemTenantId)
                            + (itemRoleId.isEmpty() || "0".equals(itemRoleId) ? "" : "?roleId=" + urlEncode(itemRoleId));
                    String response = request("POST", path, null, true);
                    parseLogin(response, username);
                    syncProfileIdentity();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "身份已切换", Toast.LENGTH_SHORT).show();
                            showShell();
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "身份切换失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    private String tenantNameFromDirectory(JSONArray directory, String targetTenantId, String fallback) {
        if (directory == null) {
            return fallback;
        }
        String target = trim(targetTenantId);
        for (int i = 0; i < directory.length(); i++) {
            JSONObject tenant = directory.optJSONObject(i);
            if (tenant == null) {
                continue;
            }
            String id = firstValue(tenant, new String[]{"id", "tenantId", "organizationId"}, "");
            if (target.equals(trim(id))) {
                return firstValue(tenant, new String[]{"tenantName", "organizationName", "name"}, fallback);
            }
        }
        return fallback;
    }

    private void renderTenantCards(LinearLayout body, String response, boolean compact) {
        try {
            JSONArray tenants = findDataArray(new JSONObject(response));
            if (tenants.length() == 0) {
                body.addView(emptyState("暂无租户"));
                return;
            }
            for (int i = 0; i < tenants.length(); i++) {
                JSONObject item = tenants.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                LinearLayout card = card();
                card.setPadding(dp(16), dp(14), dp(16), dp(14));
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
                params.setMargins(0, 0, 0, dp(12));
                body.addView(card, params);
                card.addView(text(item.optString("tenantName", "tenant " + item.optString("id")), compact ? 16 : 18, TEXT, Typeface.BOLD));
                card.addView(text("租户编号：" + item.optString("id", "-"), 14, MUTED, Typeface.NORMAL));
                card.addView(text("状态：" + item.optString("status", "-"), 14, MUTED, Typeface.NORMAL));
                String description = item.optString("description", "");
                if (!description.isEmpty()) {
                    card.addView(text(description, 14, MUTED, Typeface.NORMAL));
                }
            }
        } catch (Exception e) {
            body.addView(emptyState("租户解析失败"));
        }
    }

    private void renderNewsCenter(LinearLayout body, TextView loading, String response) {
        body.removeView(loading);
        try {
            JSONArray items = findDataArray(new JSONObject(response));
            if (items.length() == 0) {
                body.addView(emptyState("暂无新闻"));
                return;
            }
            body.addView(sectionTitle("最新新闻"));
            int limit = Math.min(items.length(), 20);
            for (int i = 0; i < limit; i++) {
                final JSONObject item = items.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                final String itemId = firstValue(item, new String[]{"id", "newsId"}, "");
                LinearLayout card = contentCard(
                        firstValue(item, new String[]{"title", "name"}, "新闻动态"),
                        buildJoinedText(new String[]{
                                firstValue(item, new String[]{"category", "categoryName", "type"}, "新闻"),
                                firstValue(item, new String[]{"author", "publisher", "createBy"}, "")
                        }),
                        firstValue(item, new String[]{"summary", "description", "content"}, ""),
                        firstValue(item, new String[]{"publishTime", "createTime", "date"}, ""),
                        "新闻",
                        null);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
                params.setMargins(0, 0, 0, dp(12));
                body.addView(card, params);
                if (!itemId.isEmpty()) {
                    card.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            navigateChild(new ScreenAction() {
                                @Override
                                public void open() {
                                    showNewsDetail(itemId);
                                }
                            });
                        }
                    });
                }
            }
        } catch (Exception e) {
            body.addView(emptyState("新闻解析失败"));
        }
    }

    private void renderActivityHighlights(LinearLayout body, TextView loading, String response, MobileStatusFilter filter) {
        body.removeView(loading);
        try {
            JSONArray items = filterItemsByStatus(filterRowsByCurrentTenant(findDataArray(new JSONObject(response))), filter, true);
            if (items.length() == 0) {
                body.addView(emptyState(filter == MobileStatusFilter.ALL ? "暂无热门活动" : "暂无对应状态活动"));
                return;
            }
            body.addView(sectionTitle(filter == MobileStatusFilter.ALL ? "热门活动" : filter.label + "活动"));
            int limit = Math.min(items.length(), 20);
            for (int i = 0; i < limit; i++) {
                final JSONObject item = items.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                final String itemId = firstValue(item, new String[]{"id", "activityId"}, "");
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
                params.setMargins(0, 0, 0, dp(12));
                String status = statusLabel(classifyActivityStatus(item), firstValue(item, new String[]{"activityStatusName", "statusName", "activityStatus", "status"}, ""));
                String count = firstValue(item, new String[]{"registerCount", "participantCount", "applyCount"}, "0");
                String range = firstValue(item, new String[]{"startTime", "activityStart", "beginTime", "registrationStartTime"}, "");
                String end = firstValue(item, new String[]{"endTime", "activityEnd", "registrationEndTime"}, "");
                if (!end.isEmpty()) {
                    range = range + " - " + end;
                }
                LinearLayout card = contentCard(
                        firstValue(item, new String[]{"title", "activityName", "name"}, "活动"),
                        "状态：" + status + " / 报名：" + count,
                        firstValue(item, new String[]{"description", "summary", "content"}, ""),
                        buildJoinedText(new String[]{
                                range,
                                firstValue(item, new String[]{"location", "address", "venue"}, "")
                        }),
                        "活动",
                        null);
                body.addView(card, params);
                if (!itemId.isEmpty()) {
                    card.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            navigateChild(new ScreenAction() {
                                @Override
                                public void open() {
                                    showActivityDetail(itemId);
                                }
                            });
                        }
                    });
                }
            }
        } catch (Exception e) {
            body.addView(emptyState("活动解析失败"));
        }
    }

    private void renderCompetitionCenter(LinearLayout body, TextView loading, String response, MobileStatusFilter filter) {
        body.removeView(loading);
        try {
            JSONArray items = filterItemsByStatus(filterRowsByCurrentTenant(findDataArray(new JSONObject(response))), filter, false);
            if (items.length() == 0) {
                body.addView(emptyState(filter == MobileStatusFilter.ALL ? "暂无比赛" : "暂无对应状态比赛"));
                return;
            }
            body.addView(sectionTitle(filter == MobileStatusFilter.ALL ? "已发布比赛" : filter.label + "比赛"));
            int limit = Math.min(items.length(), 20);
            for (int i = 0; i < limit; i++) {
                final JSONObject item = items.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                final String itemId = firstValue(item, new String[]{"id", "competitionId"}, "");
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
                params.setMargins(0, 0, 0, dp(12));
                String type = firstValue(item, new String[]{"competitionType", "type", "level"}, "赛事");
                String status = statusLabel(classifyCompetitionStatus(item), firstValue(item, new String[]{"statusName", "status"}, ""));
                String registerRange = firstValue(item, new String[]{"registrationStart", "registerStart", "signupStart"}, "");
                String registerEnd = firstValue(item, new String[]{"registrationEnd", "registerEnd", "signupEnd"}, "");
                if (!registerEnd.isEmpty()) {
                    registerRange = registerRange + " - " + registerEnd;
                }
                String competitionRange = firstValue(item, new String[]{"competitionStart", "startTime"}, "");
                String competitionEnd = firstValue(item, new String[]{"competitionEnd", "endTime"}, "");
                if (!competitionEnd.isEmpty()) {
                    competitionRange = competitionRange + " - " + competitionEnd;
                }
                String location = firstValue(item, new String[]{"location", "address", "venue"}, "");
                String count = firstValue(item, new String[]{"registrationCount", "registerCount", "participantCount"}, "");
                LinearLayout card = contentCard(
                        firstValue(item, new String[]{"title", "competitionName", "name"}, "比赛"),
                        type + " / 状态：" + status,
                        firstValue(item, new String[]{"description", "summary", "content"}, ""),
                        buildJoinedText(new String[]{
                                registerRange.isEmpty() ? "" : "报名：" + registerRange,
                                competitionRange.isEmpty() ? "" : "比赛：" + competitionRange,
                                location.isEmpty() ? "" : "地点：" + location,
                                count.isEmpty() ? "" : "报名人数：" + count
                        }),
                        "比赛",
                        null);
                body.addView(card, params);
                if (!itemId.isEmpty()) {
                    card.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            navigateChild(new ScreenAction() {
                                @Override
                                public void open() {
                                    showCompetitionDetail(itemId);
                                }
                            });
                        }
                    });
                }
            }
        } catch (Exception e) {
            body.addView(emptyState("比赛解析失败"));
        }
    }

    private void renderContentCalendar(LinearLayout body, TextView loading, String activityResponse, String competitionResponse) {
        body.removeView(loading);
        try {
            JSONArray activities = filterRowsByCurrentTenant(findDataArray(new JSONObject(activityResponse)));
            JSONArray competitions = filterRowsByCurrentTenant(findDataArray(new JSONObject(competitionResponse)));
            ArrayList<CalendarEntry> entries = new ArrayList<>();
            addActivityCalendarEntries(entries, activities);
            addCompetitionCalendarEntries(entries, competitions);
            renderCalendarAgenda(body, entries);
        } catch (Exception e) {
            body.addView(emptyState("日历解析失败"));
        }
    }

    private void renderCalendarAgenda(LinearLayout body, ArrayList<CalendarEntry> entries) {
        if (entries.size() == 0) {
            body.addView(emptyState("暂无活动或比赛日程"));
            return;
        }
        Collections.sort(entries, new Comparator<CalendarEntry>() {
            @Override
            public int compare(CalendarEntry left, CalendarEntry right) {
                return left.date.compareTo(right.date);
            }
        });

        String currentDate = "";
        for (int i = 0; i < entries.size(); i++) {
            CalendarEntry entry = entries.get(i);
            if (!entry.date.equals(currentDate)) {
                currentDate = entry.date;
                body.addView(sectionTitle(currentDate));
            }
            LinearLayout card = contentCard(entry.title, entry.meta, entry.summary, entry.footer, entry.type, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if ("活动".equals(entry.type)) {
                        navigateChild(activityDetailScreenAction(entry.id));
                    } else if ("比赛".equals(entry.type)) {
                        navigateChild(competitionDetailScreenAction(entry.id));
                    }
                }
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
            params.setMargins(0, 0, 0, dp(10));
            body.addView(card, params);
        }
    }

    private JSONObject renderDetail(LinearLayout body, TextView loading, String response, String fallbackTitle, int heroRes,
                                    String[] titleKeys, String[] metaKeys, String[] contentKeys, String[] timeKeys) {
        body.removeView(loading);
        try {
            JSONObject item = findDataObject(new JSONObject(response));
            ImageView hero = image(heroRes, fallbackTitle + "详情视觉图");
            LinearLayout.LayoutParams heroParams = new LinearLayout.LayoutParams(-1, dp(148));
            heroParams.setMargins(0, 0, 0, dp(14));
            body.addView(hero, heroParams);

            LinearLayout detail = card();
            detail.setPadding(dp(16), dp(16), dp(16), dp(16));
            body.addView(detail);
            detail.addView(tag(fallbackTitle));
            TextView title = text(firstValue(item, titleKeys, fallbackTitle + "详情"), 23, TEXT, Typeface.BOLD);
            title.setPadding(0, dp(10), 0, dp(8));
            detail.addView(title);

            String meta = collectValues(item, metaKeys, " / ");
            String times = collectValues(item, timeKeys, " - ");
            if (!meta.isEmpty()) {
                detail.addView(text(meta, 14, PRIMARY, Typeface.BOLD));
            }
            if (!times.isEmpty()) {
                detail.addView(text(times, 14, MUTED, Typeface.NORMAL));
            }
            if ("活动".equals(fallbackTitle)) {
                addMetricRow(detail, "报名时间", times.isEmpty() ? "请查看活动安排" : times);
                addMetricRow(detail, "活动人数", firstValue(item, new String[]{"capacity", "maxParticipants", "participantLimit", "registerCount"}, "128 / 200"));
                addMetricRow(detail, "活动标签", collectValues(item, new String[]{"type", "category", "tag"}, " · ").isEmpty() ? "健身 · 才艺展示 · 校园文化" : collectValues(item, new String[]{"type", "category", "tag"}, " · "));
            } else if ("比赛".equals(fallbackTitle)) {
                addMetricRow(detail, "报名时间", times.isEmpty() ? "请查看比赛规则" : times);
                addMetricRow(detail, "参赛队伍", firstValue(item, new String[]{"teamCount", "teamLimit", "registrationCount"}, "32 / 40"));
                addMetricRow(detail, "比赛级别", collectValues(item, new String[]{"level", "competitionType", "type"}, " · ").isEmpty() ? "校级赛事" : collectValues(item, new String[]{"level", "competitionType", "type"}, " · "));
            }

            String contentText = stripHtml(firstValue(item, contentKeys, "暂无详细内容"));
            TextView contentView = text(contentText, 15, TEXT, Typeface.NORMAL);
            contentView.setPadding(0, dp(14), 0, 0);
            contentView.setLineSpacing(dp(4), 1.0f);
            detail.addView(contentView);
            return item;
        } catch (Exception e) {
            body.addView(emptyState(fallbackTitle + "详情解析失败"));
            return null;
        }
    }

    private void renderCommentsSection(final String targetType, final String targetId, final LinearLayout body, JSONArray comments) {
        final LinearLayout section = card();
        section.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, dp(14), 0, dp(14));
        body.addView(section, params);
        section.addView(text("评论", 18, TEXT, Typeface.BOLD));

        renderCommentList(section, comments);

        final EditText input = multiLineInput("写下你的看法", "");
        section.addView(input);
        final Button submit = primaryButton("发布评论");
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String contentText = input.getText() == null ? "" : input.getText().toString().trim();
                if (contentText.isEmpty()) {
                    Toast.makeText(MainActivity.this, "请输入评论内容", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (token.isEmpty()) {
                    Toast.makeText(MainActivity.this, "请先登录后再评论", Toast.LENGTH_SHORT).show();
                    return;
                }
                String commentKey = targetType + ":" + targetId;
                if (!markCommentSubmitting(commentKey)) {
                    Toast.makeText(MainActivity.this, "评论正在发布，请勿重复点击", Toast.LENGTH_SHORT).show();
                    return;
                }
                submit.setEnabled(false);
                submit.setText("发布中...");
                postComment(targetType, targetId, contentText, section, input, submit);
            }
        });
        section.addView(submit, compactButtonParams());
    }

    private void loadComments(final String targetType, final String targetId, final LinearLayout body) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String commentPath = "/api/comments?targetType=" + urlEncode(targetType) + "&targetId=" + urlEncode(targetId);
                    final String result = requestCommentList(commentPath);
                    final JSONArray comments = findDataArray(new JSONObject(result));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            renderCommentsSection(targetType, targetId, body, comments);
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            body.addView(emptyState("评论加载失败"));
                        }
                    });
                }
            }
        }).start();
    }

    private void showMyCredits() {
        activeTab = NavTab.PROFILE;
        final ScrollView scrollView = screenScaffold("我的学分", "总学分与学分类别");
        final LinearLayout body = (LinearLayout) scrollView.getChildAt(0);
        final TextView loading = text("正在加载 /api/credit/my/summary ...", 14, MUTED, Typeface.NORMAL);
        body.addView(loading);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String creditResult = request("GET", "/api/credit/my/summary", null, true);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            renderMyCredits(body, loading, creditResult);
                        }
                    });
                } catch (Exception e) {
                    showLoadError(loading, e);
                }
            }
        }).start();
    }

    private void showMyClubs() {
        activeTab = NavTab.PROFILE;
        final ScrollView scrollView = screenScaffold("我的社团", "我加入的社团");
        final LinearLayout body = (LinearLayout) scrollView.getChildAt(0);
        final TextView refreshStatus = text("正在更新", 12, MUTED, Typeface.NORMAL);
        refreshStatus.setPadding(0, 0, 0, dp(8));
        body.addView(refreshStatus);
        final LinearLayout rows = vertical();
        body.addView(rows);
        final String cachedMembers = loadCachedProfileSection("clubs:members", EMPTY_ARRAY_RESPONSE);
        final String cachedAlumni = loadCachedProfileSection("clubs:alumni", EMPTY_ARRAY_RESPONSE);
        renderMyClubs(rows, null, cachedMembers, cachedAlumni);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final String membersResult = request("GET", "/api/members/list?tenantId=" + urlEncode(tenantId), null, true);
                    final String alumniResult = request("GET", "/api/alumni/list?tenantId=" + urlEncode(tenantId), null, true);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            saveProfileSectionCache("clubs:members", membersResult);
                            saveProfileSectionCache("clubs:alumni", alumniResult);
                            rows.removeAllViews();
                            renderMyClubs(rows, null, membersResult, alumniResult);
                            refreshStatus.setText("已更新");
                        }
                    });
                } catch (Exception e) {
                    updateRefreshStatus(refreshStatus, "更新失败，已显示本地缓存");
                }
            }
        }).start();
    }

    private UpdateProgressViews buildDownloadProgressContent(boolean forceUpgrade) {
        UpdateProgressViews views = new UpdateProgressViews();
        LinearLayout progressContent = vertical();
        progressContent.setPadding(dp(22), dp(22), dp(22), dp(18));
        progressContent.setBackground(roundRect(SURFACE, dp(18)));
        TextView badge = text(forceUpgrade ? "强制更新" : "可选更新", 12, forceUpgrade ? DANGER : PRIMARY, Typeface.BOLD);
        badge.setPadding(dp(10), dp(4), dp(10), dp(4));
        badge.setBackground(roundStroke(forceUpgrade ? Color.rgb(254, 242, 242) : Color.rgb(236, 253, 245), forceUpgrade ? Color.rgb(252, 165, 165) : Color.rgb(167, 243, 208), dp(14)));
        progressContent.addView(badge);
        TextView title = text("正在下载更新", 21, TEXT, Typeface.BOLD);
        title.setPadding(0, dp(14), 0, dp(4));
        progressContent.addView(title);
        TextView desc = text("请保持网络连接，下载完成后会打开系统安装器。", 14, MUTED, Typeface.NORMAL);
        desc.setPadding(0, 0, 0, dp(14));
        progressContent.addView(desc);
        views.progressText = text("准备下载 APK ...", 15, TEXT, Typeface.BOLD);
        progressContent.addView(views.progressText);
        views.progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        views.progressBar.setMax(100);
        views.progressBar.setIndeterminate(true);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(-1, dp(18));
        progressParams.setMargins(0, dp(12), 0, dp(8));
        progressContent.addView(views.progressBar, progressParams);
        views.sizeText = text("等待服务器响应", 13, MUTED, Typeface.NORMAL);
        progressContent.addView(views.sizeText);
        views.content = progressContent;
        return views;
    }

    private void updateDialogProgressText(UpdateProgressViews views, long downloaded, long total) {
        if (views == null || views.progressText == null || views.progressBar == null || views.sizeText == null) {
            return;
        }
        if (total > 0) {
            int percent = (int) Math.min(100L, Math.max(0L, downloaded * 100L / total));
            views.progressBar.setIndeterminate(false);
            views.progressBar.setProgress(percent);
            views.progressText.setText("下载进度 " + percent + "%");
            views.sizeText.setText(formatDownloadSize(downloaded) + " / " + formatDownloadSize(total));
        } else {
            views.progressBar.setIndeterminate(true);
            views.progressText.setText("正在下载更新包");
            views.sizeText.setText("已下载 " + formatDownloadSize(downloaded));
        }
    }

    private String formatDownloadSize(long bytes) {
        return formatBytes(bytes);
    }

    private String requestCommentList(String commentPath) throws IOException {
        try {
            return request("GET", commentPath, null, false);
        } catch (IOException e) {
            if (isHttpUnauthorized(e) && !token.isEmpty()) {
                // comment public request returned 401, retrying with current mobile token
                return request("GET", commentPath, null, true);
            }
            throw e;
        }
    }

    private boolean isHttpUnauthorized(Exception e) {
        String message = e == null ? "" : e.getMessage();
        return message != null && message.startsWith("HTTP 401");
    }

    private void renderCommentList(LinearLayout section, JSONArray comments) {
        if (comments == null || comments.length() == 0) {
            TextView empty = text("暂无评论", 14, MUTED, Typeface.NORMAL);
            empty.setPadding(0, dp(12), 0, dp(6));
            section.addView(empty);
            return;
        }
        int limit = Math.min(comments.length(), 20);
        for (int i = 0; i < limit; i++) {
            final JSONObject item = comments.optJSONObject(i);
            if (item == null) {
                continue;
            }
            final String commenterId = extractUserId(item);
            LinearLayout row = horizontal();
            row.setGravity(Gravity.TOP);
            row.setPadding(0, dp(12), 0, dp(8));
            row.addView(avatarImageView(firstValue(item, new String[]{"userName", "username", "name"}, "用户"),
                    firstValue(item, new String[]{"userAvatar", "avatar", "avatarUrl"}, ""), dp(34)),
                    new LinearLayout.LayoutParams(dp(34), dp(34)));
            LinearLayout copy = vertical();
            copy.setPadding(dp(10), 0, 0, 0);
            row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
            copy.addView(text(firstValue(item, new String[]{"userName", "username", "name"}, "用户"), 14, TEXT, Typeface.BOLD));
            copy.addView(text(firstValue(item, new String[]{"content", "comment"}, ""), 14, MUTED, Typeface.NORMAL));
            String time = firstValue(item, new String[]{"createTime", "createdAt", "time"}, "");
            if (!time.isEmpty()) {
                copy.addView(text(time, 12, MUTED, Typeface.NORMAL));
            }
            if (!commenterId.isEmpty()) {
                row.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        navigateChild(userProfileScreenAction(commenterId));
                    }
                });
            }
            section.addView(row);
        }
    }

    private boolean markCommentSubmitting(String commentKey) {
        synchronized (pendingCommentSubmissions) {
            return pendingCommentSubmissions.add(commentKey);
        }
    }

    private void clearCommentSubmitting(String commentKey) {
        synchronized (pendingCommentSubmissions) {
            pendingCommentSubmissions.remove(commentKey);
        }
    }

    private void finishCommentSubmission(String commentKey, Button submit) {
        clearCommentSubmitting(commentKey);
        if (submit != null) {
            submit.setEnabled(true);
            submit.setText("发布评论");
        }
    }

    private void postComment(final String targetType, final String targetId, String contentText, final LinearLayout section, final EditText input, final Button submit) {
        final String commentKey = targetType + ":" + targetId;
        final String payload = "{\"targetType\":\"" + escapeJson(targetType) + "\",\"targetId\":" + parseLong(targetId)
                + ",\"content\":\"" + escapeJson(contentText) + "\"}";
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String response = request("POST", "/api/comments", payload, true);
                    ensureApiSuccess(response, "评论发布失败");
                    final JSONObject created = findDataObject(new JSONObject(response));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            finishCommentSubmission(commentKey, submit);
                            input.setText("");
                            JSONArray single = new JSONArray();
                            single.put(created);
                            renderCommentList(section, single);
                            Toast.makeText(MainActivity.this, "评论已发布", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    final String message = e.getMessage();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            finishCommentSubmission(commentKey, submit);
                            Toast.makeText(MainActivity.this, "评论发布失败：" + message, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void addRegistrationButton(LinearLayout body, String label, View.OnClickListener listener) {
        Button button = primaryButton(label);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(48));
        params.setMargins(0, dp(12), 0, dp(12));
        body.addView(button, params);
        button.setOnClickListener(listener);
    }

    private void registerActivity(final String id) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String response = request("POST", "/api/activity/" + urlEncode(id) + "/register", null, true);
                    ensureApiSuccess(response, "活动报名失败");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "活动报名已提交", Toast.LENGTH_SHORT).show();
                            openChildScreen(myServicesScreenAction());
                        }
                    });
                } catch (Exception e) {
                    final String message = e.getMessage();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "活动报名失败：" + message, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void registerCompetition(final String id) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String response = request("POST", "/api/competition/" + urlEncode(id) + "/register", "{}", true);
                    ensureApiSuccess(response, "比赛报名失败");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "比赛报名已提交", Toast.LENGTH_SHORT).show();
                            openChildScreen(myServicesScreenAction());
                        }
                    });
                } catch (Exception e) {
                    final String message = e.getMessage();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "比赛报名失败：" + message, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void renderAdminWorkbench(LinearLayout body, TextView loading, String monitoringResponse, String trafficResponse, String onlineResponse, String notificationResponse) {
        body.removeView(loading);
        LinearLayout summary = card();
        summary.setPadding(dp(16), dp(14), dp(16), dp(14));
        body.addView(summary);
        summary.addView(text("后台运行摘要", 19, TEXT, Typeface.BOLD));

        try {
            JSONObject monitoring = findDataObject(new JSONObject(monitoringResponse));
            addMetricRow(summary, "服务状态", firstValue(monitoring, new String[]{"status", "health", "overallStatus"}, "已连接"));
            addMetricRow(summary, "服务数量", firstValue(monitoring, new String[]{"serviceCount", "applicationCount", "podCount"}, "-"));
        } catch (Exception e) {
            addMetricRow(summary, "服务状态", "暂不可用");
        }

        try {
            JSONObject traffic = findDataObject(new JSONObject(trafficResponse));
            addMetricRow(summary, "访问国家", firstValue(traffic, new String[]{"countryCount", "regionCount"}, "0"));
            addMetricRow(summary, "移动端在线", firstValue(traffic, new String[]{"onlineMobileCount", "mobileOnlineCount"}, "0"));
            addMetricRow(summary, "电脑端在线", firstValue(traffic, new String[]{"onlinePcCount", "pcOnlineCount"}, "0"));
        } catch (Exception e) {
            addMetricRow(summary, "流量中心", "暂不可用");
        }

        try {
            JSONObject notification = findDataObject(new JSONObject(notificationResponse));
            addMetricRow(summary, "通知总数", firstValue(notification, new String[]{"total", "totalCount", "notificationCount"}, "0"));
            addMetricRow(summary, "升级通知", firstValue(notification, new String[]{"upgradeCount", "mobileUpgradeCount"}, "0"));
        } catch (Exception e) {
            addMetricRow(summary, "通知中心", "暂不可用");
        }

        body.addView(sectionTitle("在线用户"));
        try {
            JSONArray users = findNamedArray(new JSONObject(onlineResponse), "onlineUsers");
            if (users.length() == 0) {
                body.addView(emptyState("暂无在线用户"));
                return;
            }
            int limit = Math.min(users.length(), 12);
            for (int i = 0; i < limit; i++) {
                JSONObject item = users.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                LinearLayout card = card();
                card.setPadding(dp(16), dp(14), dp(16), dp(14));
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
                params.setMargins(0, 0, 0, dp(12));
                body.addView(card, params);
                card.addView(text(firstValue(item, new String[]{"displayName", "username", "userName", "name"}, "在线用户"), 17, TEXT, Typeface.BOLD));
                card.addView(text(firstValue(item, new String[]{"deviceType", "clientType", "terminal"}, "未知终端") + " / " + firstValue(item, new String[]{"ip", "ipAddress", "clientIp"}, "-"), 13, PRIMARY, Typeface.BOLD));
                String place = firstValue(item, new String[]{"location", "address", "country", "province"}, "");
                if (!place.isEmpty()) {
                    card.addView(text(place, 14, MUTED, Typeface.NORMAL));
                }
            }
        } catch (Exception e) {
            body.addView(emptyState("在线用户解析失败"));
        }
    }

    private void renderUserProfile(LinearLayout body, TextView loading, String response, String userId) {
        body.removeView(loading);
        try {
            JSONObject root = new JSONObject(response);
            JSONArray rows = findDataArray(root);
            JSONObject user = selectCurrentTenantUserProfile(rows);
            if (user == null) {
                user = findDataObject(root);
            }
            if (user == null || user.has("error")) {
                body.addView(emptyState("个人信息暂不可用"));
                body.addView(text("用户 ID：" + userId, 13, MUTED, Typeface.NORMAL));
                return;
            }

            LinearLayout card = card();
            card.setPadding(dp(16), dp(16), dp(16), dp(16));
            body.addView(card);
            LinearLayout row = horizontal();
            row.setGravity(Gravity.CENTER_VERTICAL);
            card.addView(row);
            String name = firstValue(user, new String[]{"realName", "displayName", "nickname", "username", "userName", "name"}, "用户 " + userId);
            row.addView(avatarImageView(name, extractAvatarUrl(user), dp(56)), new LinearLayout.LayoutParams(dp(56), dp(56)));
            LinearLayout copy = vertical();
            copy.setPadding(dp(14), 0, 0, 0);
            row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
            copy.addView(text(name, 22, TEXT, Typeface.BOLD));
            copy.addView(text(firstValue(user, new String[]{"roleName", "role", "userType"}, "成员资料"), 13, PRIMARY, Typeface.BOLD));
            copy.addView(text(firstValue(user, new String[]{"tenantName", "organizationName", "orgName"}, tenantName), 13, MUTED, Typeface.NORMAL));

            body.addView(sectionTitle("联系方式"));
            LinearLayout info = card();
            info.setPadding(dp(16), dp(14), dp(16), dp(14));
            body.addView(info);
            addMetricRow(info, "用户 ID", userId);
            addMetricRow(info, "邮箱", firstValue(user, new String[]{"email", "mail"}, "-"));
            addMetricRow(info, "手机", firstValue(user, new String[]{"phone", "mobile", "phoneNumber"}, "-"));
            addMetricRow(info, "部门", firstValue(user, new String[]{"department", "deptName", "major"}, "-"));
        } catch (Exception e) {
            body.addView(emptyState("个人信息暂不可用"));
            body.addView(text("用户 ID：" + userId, 13, MUTED, Typeface.NORMAL));
        }
    }

    private void renderNewsCards(LinearLayout body, TextView loading, String response) {
        body.removeView(loading);
        try {
            JSONArray items = findDataArray(new JSONObject(response));
            if (items.length() == 0) {
                body.addView(emptyState("暂无内容"));
                return;
            }
            int limit = Math.min(items.length(), 12);
            for (int i = 0; i < limit; i++) {
                final JSONObject item = items.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                LinearLayout card = card();
                card.setPadding(dp(16), dp(14), dp(16), dp(14));
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
                params.setMargins(0, 0, 0, dp(12));
                body.addView(card, params);
                card.addView(text(item.optString("title", "内容"), 18, TEXT, Typeface.BOLD));
                String summary = item.optString("summary", item.optString("content", ""));
                if (!summary.isEmpty()) {
                    card.addView(text(summary.length() > 80 ? summary.substring(0, 80) + "..." : summary, 14, MUTED, Typeface.NORMAL));
                }
                card.addView(text(item.optString("createTime", item.optString("publishTime", "")), 13, PRIMARY, Typeface.BOLD));
            }
        } catch (Exception e) {
            body.addView(emptyState("内容解析失败"));
        }
    }

    private void renderActivityCenter(LinearLayout body, TextView loading, String response) {
        body.removeView(loading);
        try {
            JSONArray items = filterRowsByCurrentTenant(findDataArray(new JSONObject(response)));
            if (items.length() == 0) {
                body.addView(emptyState("暂无活动"));
                return;
            }
            body.addView(sectionTitle("活动列表"));
            int limit = Math.min(items.length(), 20);
            for (int i = 0; i < limit; i++) {
                final JSONObject item = items.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                final String itemId = firstValue(item, new String[]{"id", "activityId"}, "");
                LinearLayout card = card();
                card.setPadding(dp(16), dp(14), dp(16), dp(14));
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
                params.setMargins(0, 0, 0, dp(12));
                body.addView(card, params);
                card.addView(text(item.optString("title", item.optString("name", "活动")), 18, TEXT, Typeface.BOLD));
                card.addView(text("状态：" + item.optString("status", "-"), 14, MUTED, Typeface.NORMAL));
                String range = item.optString("startTime", "") + (item.optString("endTime", "").isEmpty() ? "" : " - " + item.optString("endTime"));
                if (!range.trim().isEmpty()) {
                    card.addView(text(range, 13, PRIMARY, Typeface.BOLD));
                }
                String location = item.optString("location", item.optString("address", ""));
                if (!location.isEmpty()) {
                    card.addView(text("地点：" + location, 14, MUTED, Typeface.NORMAL));
                }
                if (!itemId.isEmpty()) {
                    card.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            navigateChild(activityDetailScreenAction(itemId));
                        }
                    });
                }
            }
        } catch (Exception e) {
            body.addView(emptyState("活动解析失败"));
        }
    }

    private void renderClubDirectory(LinearLayout body, TextView loading, String membersResponse, String alumniResponse) {
        body.removeView(loading);
        try {
            JSONArray members = findDataArray(new JSONObject(membersResponse));
            JSONArray alumni = findDataArray(new JSONObject(alumniResponse));
            body.addView(sectionTitle("成员台账"));
            renderPeopleCards(body, members, "studentId", "department", "position", 12);
            body.addView(sectionTitle("校友名录"));
            renderPeopleCards(body, alumni, "generationYear", "workUnit", "position", 12);
        } catch (Exception e) {
            body.addView(emptyState("目录解析失败"));
        }
    }

    private void renderPeopleCards(LinearLayout body, JSONArray rows, String metaKey, String secondKey, String thirdKey, int max) {
        rows = filterRowsByCurrentTenant(rows);
        if (rows == null || rows.length() == 0) {
            body.addView(emptyState("暂无数据"));
            return;
        }
        int limit = Math.min(rows.length(), max);
        for (int i = 0; i < limit; i++) {
            JSONObject item = rows.optJSONObject(i);
            if (item == null) {
                continue;
            }
            LinearLayout card = card();
            card.setPadding(dp(16), dp(14), dp(16), dp(14));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
            params.setMargins(0, 0, 0, dp(12));
            body.addView(card, params);
            card.addView(text(item.optString("name", "未命名"), 18, TEXT, Typeface.BOLD));
            String meta = item.optString(metaKey, "");
            if (!meta.isEmpty()) {
                card.addView(text(meta, 13, PRIMARY, Typeface.BOLD));
            }
            String second = item.optString(secondKey, "");
            String third = item.optString(thirdKey, "");
            if (!second.isEmpty() || !third.isEmpty()) {
                card.addView(text(second + (third.isEmpty() ? "" : " / " + third), 14, MUTED, Typeface.NORMAL));
            }
        }
    }

    private void renderMyServices(LinearLayout body, TextView loading, String activityResponse, String registrationResponse, String creditResponse) {
        if (loading != null) {
            body.removeView(loading);
        }
        LinearLayout creditCard = card();
        creditCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        body.addView(creditCard);
        creditCard.addView(text("我的积分", 18, TEXT, Typeface.BOLD));
        try {
            JSONObject creditRoot = new JSONObject(creditResponse);
            JSONObject data = creditRoot.optJSONObject("data");
            if (data == null) {
                data = creditRoot;
            }
            creditCard.addView(text("当前积分：" + data.optString("totalCredit", data.optString("score", "0")), 15, PRIMARY, Typeface.BOLD));
            creditCard.addView(text("信用等级：" + data.optString("level", data.optString("grade", "-")), 14, MUTED, Typeface.NORMAL));
        } catch (Exception e) {
            creditCard.addView(text("积分信息暂不可用", 14, MUTED, Typeface.NORMAL));
        }

        body.addView(sectionTitle("我的活动"));
        try {
            JSONArray activities = findDataArray(new JSONObject(activityResponse));
            if (activities.length() == 0) {
                JSONArray registrations = registrationResponse == null || registrationResponse.trim().isEmpty()
                        ? new JSONArray()
                        : findDataArray(new JSONObject(registrationResponse));
                if (registrations.length() == 0) {
                    body.addView(emptyState("暂无报名或参与的活动"));
                    return;
                }
                renderActivityRegistrationCards(body, registrations, 10);
                return;
            }
            int limit = Math.min(activities.length(), 10);
            for (int i = 0; i < limit; i++) {
                JSONObject item = activities.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                LinearLayout card = card();
                card.setPadding(dp(16), dp(14), dp(16), dp(14));
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
                params.setMargins(0, 0, 0, dp(12));
                body.addView(card, params);
                card.addView(text(firstValue(item, new String[]{"activityName", "title", "name"}, "活动"), 18, TEXT, Typeface.BOLD));
                card.addView(text("状态：" + firstValue(item, new String[]{"registrationStatus", "status"}, "-"), 14, MUTED, Typeface.NORMAL));
                card.addView(text(firstValue(item, new String[]{"registrationTime", "startTime", "createTime"}, ""), 13, PRIMARY, Typeface.BOLD));
            }
        } catch (Exception e) {
            body.addView(emptyState("活动解析失败"));
        }
    }

    private void renderActivityRegistrationCards(LinearLayout body, JSONArray registrations, int max) {
        int limit = Math.min(registrations.length(), max);
        for (int i = 0; i < limit; i++) {
            JSONObject item = registrations.optJSONObject(i);
            if (item == null) {
                continue;
            }
            LinearLayout card = card();
            card.setPadding(dp(16), dp(14), dp(16), dp(14));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
            params.setMargins(0, 0, 0, dp(12));
            body.addView(card, params);
            String activityId = firstValue(item, new String[]{"activityId", "id"}, "-");
            String title = firstValue(item, new String[]{"activityName", "title", "name"}, "活动 " + activityId);
            card.addView(text(title, 18, TEXT, Typeface.BOLD));
            card.addView(text("报名状态：" + firstValue(item, new String[]{"registrationStatus", "status"}, "-"), 14, MUTED, Typeface.NORMAL));
            card.addView(text("报名时间：" + firstValue(item, new String[]{"registrationTime", "createTime"}, "-"), 13, PRIMARY, Typeface.BOLD));
        }
    }

    private void renderMyCompetitions(LinearLayout body, TextView loading, String competitionResponse, String registrationResponse) {
        if (loading != null) {
            body.removeView(loading);
        }
        body.addView(sectionTitle("我的比赛"));
        try {
            JSONArray competitions = filterRowsByCurrentTenant(findDataArray(new JSONObject(competitionResponse)));
            if (competitions.length() > 0) {
                renderMyCompetitionRows(body, competitions, 10);
                return;
            }
            JSONArray registrations = registrationResponse == null || registrationResponse.trim().isEmpty()
                    ? new JSONArray()
                    : filterRowsByCurrentTenant(findDataArray(new JSONObject(registrationResponse)));
            if (registrations.length() > 0) {
                renderCompetitionRegistrationCards(body, registrations, 10);
                return;
            }
            body.addView(emptyState("暂无报名或参与的比赛"));
        } catch (Exception e) {
            body.addView(emptyState("比赛记录暂不可用"));
        }
    }

    private void renderMyCompetitionRows(LinearLayout body, JSONArray competitions, int max) {
        int limit = Math.min(competitions.length(), max);
        for (int i = 0; i < limit; i++) {
            final JSONObject item = competitions.optJSONObject(i);
            if (item == null) {
                continue;
            }
            final String competitionId = firstValue(item, new String[]{"id", "competitionId"}, "");
            LinearLayout card = card();
            card.setPadding(dp(16), dp(14), dp(16), dp(14));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
            params.setMargins(0, 0, 0, dp(12));
            body.addView(card, params);
            card.addView(text(firstValue(item, new String[]{"title", "competitionName", "name"}, "比赛"), 18, TEXT, Typeface.BOLD));
            card.addView(text("状态：" + firstValue(item, new String[]{"statusName", "status"}, "-"), 14, MUTED, Typeface.NORMAL));
            card.addView(text(firstValue(item, new String[]{"registrationTime", "competitionStart", "createTime"}, ""), 13, PRIMARY, Typeface.BOLD));
            if (!competitionId.isEmpty()) {
                card.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        navigateChild(competitionDetailScreenAction(competitionId));
                    }
                });
            }
        }
    }

    private void renderCompetitionRegistrationCards(LinearLayout body, JSONArray registrations, int max) {
        int limit = Math.min(registrations.length(), max);
        for (int i = 0; i < limit; i++) {
            final JSONObject item = registrations.optJSONObject(i);
            if (item == null) {
                continue;
            }
            final String competitionId = firstValue(item, new String[]{"competitionId", "id"}, "");
            LinearLayout card = card();
            card.setPadding(dp(16), dp(14), dp(16), dp(14));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
            params.setMargins(0, 0, 0, dp(12));
            body.addView(card, params);
            String title = firstValue(item, new String[]{"competitionTitle", "title", "competitionName", "name"}, competitionId.isEmpty() ? "比赛" : "比赛 " + competitionId);
            card.addView(text(title, 18, TEXT, Typeface.BOLD));
            card.addView(text("报名状态：" + firstValue(item, new String[]{"registrationStatus", "status"}, "-"), 14, MUTED, Typeface.NORMAL));
            card.addView(text("报名时间：" + firstValue(item, new String[]{"registrationTime", "createTime"}, "-"), 13, PRIMARY, Typeface.BOLD));
            if (!competitionId.isEmpty()) {
                card.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        navigateChild(competitionDetailScreenAction(competitionId));
                    }
                });
            }
        }
    }

    private void renderMyCredits(LinearLayout body, TextView loading, String creditResponse) {
        body.removeView(loading);
        LinearLayout summary = card();
        summary.setPadding(dp(18), dp(18), dp(18), dp(18));
        summary.setBackground(roundStroke(PRIMARY, PRIMARY_DARK, dp(10)));
        LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(-1, -2);
        summaryParams.setMargins(0, 0, 0, dp(16));
        body.addView(summary, summaryParams);
        summary.addView(text("总学分", 15, Color.WHITE, Typeface.BOLD));
        try {
            JSONObject creditRoot = new JSONObject(creditResponse);
            JSONObject data = creditRoot.optJSONObject("data");
            if (data == null) {
                data = creditRoot;
            }
            String total = data.optString("totalCredit", data.optString("score", "32.5"));
            TextView totalView = text(total + " /50", 36, Color.WHITE, Typeface.BOLD);
            totalView.setPadding(0, dp(8), 0, dp(4));
            summary.addView(totalView);
            summary.addView(text(data.optString("level", data.optString("grade", "已坚持 12 天")), 13, Color.rgb(218, 235, 255), Typeface.BOLD));
        } catch (Exception e) {
            TextView totalView = text("32.5 /50", 36, Color.WHITE, Typeface.BOLD);
            totalView.setPadding(0, dp(8), 0, dp(4));
            summary.addView(totalView);
            summary.addView(text("学分信息暂不可用，展示默认分类", 13, Color.rgb(218, 235, 255), Typeface.BOLD));
        }

        body.addView(sectionTitle("学分类别"));
        body.addView(creditRow("思想政治类", "8.5", "/10"));
        body.addView(creditRow("学术科技类", "12.0", "/20"));
        body.addView(creditRow("文化艺术类", "6.0", "/10"));
        body.addView(creditRow("社会实践类", "6.0", "/10"));
        TextView rule = text("学分规则说明 〉", 14, PRIMARY, Typeface.BOLD);
        rule.setPadding(0, dp(16), 0, 0);
        body.addView(rule);
    }

    private LinearLayout creditRow(String label, String value, String total) {
        LinearLayout row = card();
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, dp(10));
        row.setLayoutParams(params);
        row.addView(text(label, 15, TEXT, Typeface.BOLD), new LinearLayout.LayoutParams(0, -2, 1));
        TextView score = text(value, 19, PRIMARY, Typeface.BOLD);
        score.setGravity(Gravity.RIGHT);
        row.addView(score, new LinearLayout.LayoutParams(dp(62), -2));
        row.addView(text(total, 13, MUTED, Typeface.NORMAL));
        return row;
    }

    private void renderMyClubs(LinearLayout body, TextView loading, String membersResponse, String alumniResponse) {
        if (loading != null) {
            body.removeView(loading);
        }
        body.addView(sectionTitle("我加入的社团"));
        try {
            JSONArray members = filterRowsByCurrentTenant(findDataArray(new JSONObject(membersResponse)));
            JSONArray alumni = filterRowsByCurrentTenant(findDataArray(new JSONObject(alumniResponse)));
            int rendered = renderMyClubRows(body, members, "成员", 10);
            rendered += renderMyClubRows(body, alumni, "校友", Math.max(0, 10 - rendered));
            if (rendered == 0) {
                body.addView(emptyState("暂无当前租户社团数据"));
            }
        } catch (Exception ignored) {
            body.addView(emptyState("社团数据暂不可用"));
        }

        TextView more = text("发现更多社团", 16, PRIMARY, Typeface.BOLD);
        more.setGravity(Gravity.CENTER);
        more.setPadding(0, dp(18), 0, dp(18));
        more.setBackground(roundStroke(Color.rgb(235, 243, 255), Color.rgb(199, 221, 255), dp(10)));
        more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateChild(clubDirectoryScreenAction());
            }
        });
        body.addView(more);
    }

    private int renderMyClubRows(LinearLayout body, JSONArray rows, String sourceLabel, int max) {
        if (rows == null || max <= 0) {
            return 0;
        }
        int rendered = 0;
        int limit = Math.min(rows.length(), max);
        for (int i = 0; i < limit; i++) {
            JSONObject item = rows.optJSONObject(i);
            if (item == null) {
                continue;
            }
            String title = firstValue(item, new String[]{"clubName", "tenantName", "department", "organizationName"}, tenantName);
            if (title.isEmpty()) {
                title = tenantName.isEmpty() ? "当前社团" : tenantName;
            }
            String role = firstValue(item, new String[]{"position", "roleName", "memberRole", "memberStatus", "status"}, sourceLabel);
            String joinDate = firstValue(item, new String[]{"joinDate", "joinTime", "createTime", "graduationDate"}, "");
            String name = firstValue(item, new String[]{"name", "realName", "nickname", "studentId"}, "");
            LinearLayout card = contentCard(
                    title,
                    role,
                    name.isEmpty() ? sourceLabel : name,
                    joinDate.isEmpty() ? "当前租户：" + tenantName : "加入时间：" + joinDate,
                    sourceLabel,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            navigateChild(clubDirectoryScreenAction());
                        }
                    });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
            params.setMargins(0, 0, 0, dp(12));
            body.addView(card, params);
            rendered++;
        }
        return rendered;
    }

    private LinearLayout myClubCard(String title, String meta, String role, int imageRes) {
        return feedCard(title, role, meta, "进入社团", imageRes, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateChild(clubDirectoryScreenAction());
            }
        });
    }

    private void renderNotifications(LinearLayout body, TextView loading, String response) {
        if (loading != null) {
            body.removeView(loading);
        }
        try {
            JSONArray items = findNotificationArray(new JSONObject(response));
            if (items == null || items.length() == 0) {
                body.addView(emptyState("暂无通知"));
                return;
            }
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                LinearLayout card = card();
                card.setPadding(dp(16), dp(14), dp(16), dp(14));
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
                params.setMargins(0, 0, 0, dp(12));
                body.addView(card, params);
                card.addView(text(item.optString("title", "通知"), 18, TEXT, Typeface.BOLD));
                card.addView(text(item.optString("content", item.optString("message", "")), 14, MUTED, Typeface.NORMAL));
                JSONObject metadata = notificationMetadata(item);
                String type = item.optString("type", "");
                String version = notificationValue(item, metadata, "upgradeVersion");
                String releaseNotes = notificationValue(item, metadata, "releaseNotes");
                String downloadUrl = notificationValue(item, metadata, "downloadUrl");
                boolean forceUpgrade = notificationBoolean(item, metadata, "forceUpgrade");
                if (!type.isEmpty() || !version.isEmpty()) {
                    card.addView(text("类型：" + type + (version.isEmpty() ? "" : " / 版本：" + version), 13, PRIMARY, Typeface.BOLD));
                }
                if (!releaseNotes.isEmpty()) {
                    TextView notes = text(releaseNotes, 13, MUTED, Typeface.NORMAL);
                    notes.setPadding(0, dp(8), 0, 0);
                    card.addView(notes);
                }
                if (!downloadUrl.isEmpty()) {
                    final String finalDownloadUrl = downloadUrl;
                    final boolean finalForceUpgrade = forceUpgrade;
                    Button downloadButton = primaryButton("下载并安装");
                    downloadButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            downloadApkFromDialog(finalDownloadUrl, finalForceUpgrade);
                        }
                    });
                    card.addView(downloadButton, compactButtonParams());
                }
            }
        } catch (Exception e) {
            body.addView(emptyState("通知解析失败"));
            TextView raw = text(response, 12, MUTED, Typeface.NORMAL);
            raw.setPadding(0, dp(8), 0, 0);
            body.addView(raw);
        }
    }

    private JSONObject notificationMetadata(JSONObject item) {
        JSONObject metadata = item == null ? null : item.optJSONObject("metadata");
        return metadata == null ? new JSONObject() : metadata;
    }

    private String notificationValue(JSONObject item, JSONObject metadata, String key) {
        String value = item == null ? "" : item.optString(key, "");
        if (value != null && !value.trim().isEmpty()) {
            return value.trim();
        }
        String metadataValue = metadata == null ? "" : metadata.optString(key, "");
        return metadataValue == null ? "" : metadataValue.trim();
    }

    private boolean notificationBoolean(JSONObject item, JSONObject metadata, String key) {
        if (item != null && item.has(key)) {
            return item.optBoolean(key, false);
        }
        return metadata != null && metadata.optBoolean(key, false);
    }

    private void parseLogin(String response, String username) throws Exception {
        JSONObject root = new JSONObject(response);
        if (root.has("code") && root.optInt("code", 200) != 200) {
            throw new IOException(root.optString("msg", root.optString("message", "登录失败")));
        }
        JSONObject data = root.optJSONObject("data");
        if (data == null) {
            data = root;
        }
        token = extractLoginToken(data);
        if (token.isEmpty()) {
            throw new IOException("登录响应缺少令牌");
        }
        refreshToken = data.optString("refreshToken", data.optString("refresh_token", "")).trim();
        displayName = data.optString("username", data.optString("name", username));
        avatarUrl = extractAvatarUrl(data);
        tenantId = data.optString("organizationId", data.optString("tenantId", data.optString("tenant_id", tenantId)));
        tenantName = data.optString("tenantName", tenantName);
        JSONArray roles = data.optJSONArray("roles");
        roleId = 0L;
        if (roles != null && roles.length() > 0) {
            JSONObject role = roles.optJSONObject(0);
            if (role != null) {
                roleId = parseLong(role.optString("roleId", role.optString("id", "0")));
            }
        }
        if (roleId <= 0) {
            roleId = parseLong(data.optString("roleId", data.optString("role_id", "0")));
        }
        persistAuthSession();
    }

    private boolean restoreAuthSession() {
        SharedPreferences preferences = getSharedPreferences(AUTH_PREFS, MODE_PRIVATE);
        token = trim(preferences.getString(KEY_TOKEN, ""));
        refreshToken = trim(preferences.getString(KEY_REFRESH_TOKEN, ""));
        tenantId = trim(preferences.getString(KEY_TENANT_ID, DEFAULT_TENANT_ID));
        tenantName = trim(preferences.getString(KEY_TENANT_NAME, "系统管理"));
        displayName = trim(preferences.getString(KEY_DISPLAY_NAME, "未登录"));
        avatarUrl = trim(preferences.getString(KEY_AVATAR_URL, ""));
        roleId = preferences.getLong(KEY_ROLE_ID, 0L);
        if (token.isEmpty() || tenantId.isEmpty()) {
            clearAuthSession();
            return false;
        }
        return true;
    }

    private void persistAuthSession() {
        if (token == null || token.trim().isEmpty()) {
            return;
        }
        getSharedPreferences(AUTH_PREFS, MODE_PRIVATE)
                .edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_REFRESH_TOKEN, refreshToken == null ? "" : refreshToken)
                .putString(KEY_TENANT_ID, tenantId == null || tenantId.trim().isEmpty() ? DEFAULT_TENANT_ID : tenantId.trim())
                .putString(KEY_TENANT_NAME, tenantName == null || tenantName.trim().isEmpty() ? "系统管理" : tenantName.trim())
                .putString(KEY_DISPLAY_NAME, displayName == null || displayName.trim().isEmpty() ? "未登录" : displayName.trim())
                .putString(KEY_AVATAR_URL, avatarUrl == null ? "" : avatarUrl.trim())
                .putLong(KEY_ROLE_ID, roleId)
                .apply();
    }

    private void clearAuthSession() {
        getSharedPreferences(AUTH_PREFS, MODE_PRIVATE).edit().clear().apply();
        token = "";
        refreshToken = "";
        displayName = "未登录";
        avatarUrl = "";
        tenantId = DEFAULT_TENANT_ID;
        tenantName = "系统管理";
        roleId = 0L;
    }

    private String extractLoginToken(JSONObject data) {
        String[] tokenKeys = new String[]{"token", "accessToken", "access_token", "jwt", "jwtToken"};
        for (int i = 0; i < tokenKeys.length; i++) {
            String value = data.optString(tokenKeys[i], "");
            if (value != null && !value.trim().isEmpty()) {
                return value.trim().replaceFirst("(?i)^Bearer\\s+", "");
            }
        }
        return "";
    }

    private String extractAvatarUrl(JSONObject data) {
        String[] avatarKeys = new String[]{
                "avatarUrl", "avatar", "userAvatar", "icon", "photo", "headImage", "headImg", "profileImage", "profilePhoto"
        };
        String value = firstValue(data, avatarKeys, "");
        if (!value.isEmpty()) {
            return value;
        }
        String[] nestedKeys = new String[]{"userInfo", "user", "profile", "account", "member"};
        for (int i = 0; i < nestedKeys.length; i++) {
            JSONObject nested = data.optJSONObject(nestedKeys[i]);
            value = firstValue(nested, avatarKeys, "");
            if (!value.isEmpty()) {
                return value;
            }
        }
        return "";
    }

    private boolean syncProfileIdentity() {
        if (token.isEmpty()) {
            return false;
        }
        String previousName = trim(displayName);
        String previousAvatar = trim(avatarUrl);
        String previousTenantId = trim(tenantId);
        String previousTenantName = trim(tenantName);
        try {
            JSONObject root = new JSONObject(request("GET", "/users/me", null, true));
            JSONObject data = findDataObject(root);
            String syncedName = firstValue(data, new String[]{"displayName", "realName", "nickname", "username", "userName", "name"}, "");
            if (!syncedName.isEmpty()) {
                displayName = syncedName;
            }
            String syncedAvatar = extractAvatarUrl(data);
            if (!syncedAvatar.isEmpty()) {
                avatarUrl = syncedAvatar;
            }
            String syncedTenantId = firstValue(data, new String[]{"tenantId", "organizationId", "tenant_id", "orgId"}, "");
            if (!syncedTenantId.isEmpty()) {
                tenantId = syncedTenantId;
            }
            String syncedTenantName = firstValue(data, new String[]{"tenantName", "organizationName", "orgName"}, "");
            if (!syncedTenantName.isEmpty()) {
                tenantName = syncedTenantName;
            }
            boolean changed = !trim(displayName).equals(previousName)
                    || !trim(avatarUrl).equals(previousAvatar)
                    || !trim(tenantId).equals(previousTenantId)
                    || !trim(tenantName).equals(previousTenantName);
            if (changed) {
                persistAuthSession();
            }
            return !trim(displayName).equals(previousName)
                    || !trim(avatarUrl).equals(previousAvatar)
                    || !trim(tenantId).equals(previousTenantId)
                    || !trim(tenantName).equals(previousTenantName);
        } catch (Exception ignored) {
            // Keep login/session restore usable; profile cards keep their local fallback values.
            return false;
        }
    }

    private void syncProfileIdentityAsync() {
        if (token.isEmpty()) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean changed = syncProfileIdentity();
                if (!changed) {
                    return;
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        refreshAvatarScreen();
                    }
                });
            }
        }).start();
    }

    private void refreshAvatarScreen() {
        if (content == null) {
            return;
        }
        if (activeTab == NavTab.PROFILE) {
            showProfile();
        } else if (activeTab == NavTab.HOME) {
            showHome();
        }
    }

    private JSONArray findDataArray(JSONObject root) {
        Object data = root.opt("data");
        if (data instanceof JSONArray) {
            return (JSONArray) data;
        }
        if (data instanceof JSONObject) {
            JSONObject object = (JSONObject) data;
            JSONArray records = object.optJSONArray("records");
            if (records != null) {
                return records;
            }
            JSONArray list = object.optJSONArray("list");
            if (list != null) {
                return list;
            }
        }
        JSONArray fallback = root.optJSONArray("items");
        return fallback == null ? new JSONArray() : fallback;
    }

    private JSONObject findDataObject(JSONObject root) {
        JSONObject data = root.optJSONObject("data");
        return data == null ? root : data;
    }

    private void ensureApiSuccess(String response, String fallbackMessage) throws IOException {
        if (response == null || response.trim().isEmpty()) {
            return;
        }
        try {
            JSONObject root = new JSONObject(response);
            if (root.has("code") && root.optInt("code", 200) != 200) {
                throw new IOException(firstValue(root, new String[]{"msg", "message", "error"}, fallbackMessage));
            }
            if (root.has("success") && !root.optBoolean("success", true)) {
                throw new IOException(firstValue(root, new String[]{"msg", "message", "error"}, fallbackMessage));
            }
            JSONObject data = root.optJSONObject("data");
            if (data != null && data.has("success") && !data.optBoolean("success", true)) {
                throw new IOException(firstValue(data, new String[]{"msg", "message", "error"}, fallbackMessage));
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception ignored) {
        }
    }

    private JSONArray findNamedArray(JSONObject root, String key) {
        JSONObject data = root.optJSONObject("data");
        if (data != null) {
            JSONArray nested = data.optJSONArray(key);
            if (nested != null) {
                return nested;
            }
        }
        JSONArray direct = root.optJSONArray(key);
        return direct == null ? findDataArray(root) : direct;
    }

    private JSONArray findNotificationArray(JSONObject root) {
        JSONArray data = findDataArray(root);
        if (data.length() > 0) {
            return data;
        }
        return root.optJSONArray("notifications");
    }

    private int countDataRows(String response) {
        try {
            return findDataArray(new JSONObject(response)).length();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private int countCurrentTenantRows(String response) {
        try {
            return filterRowsByCurrentTenant(findDataArray(new JSONObject(response))).length();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private int countUnreadNotifications(String response) {
        try {
            JSONArray items = findNotificationArray(new JSONObject(response));
            if (items == null) {
                return 0;
            }
            int unread = 0;
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                boolean read = item.optBoolean("read", item.optBoolean("isRead", false));
                String status = firstValue(item, new String[]{"readStatus", "status"}, "");
                if (!status.isEmpty()) {
                    read = status.contains("已读") || "read".equalsIgnoreCase(status) || "1".equals(status);
                }
                if (!read) {
                    unread++;
                }
            }
            return unread;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private JSONArray filterItemsByStatus(JSONArray rows, MobileStatusFilter filter, boolean activity) {
        if (rows == null || filter == null || filter == MobileStatusFilter.ALL) {
            return rows == null ? new JSONArray() : rows;
        }
        JSONArray filtered = new JSONArray();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject item = rows.optJSONObject(i);
            if (item == null) {
                continue;
            }
            MobileStatusFilter itemStatus = activity ? classifyActivityStatus(item) : classifyCompetitionStatus(item);
            if (itemStatus == filter) {
                filtered.put(item);
            }
        }
        return filtered;
    }

    private String activityListPathForFilter(MobileStatusFilter filter) {
        if (filter == MobileStatusFilter.UPCOMING) {
            return "/api/activity/status/1";
        }
        if (filter == MobileStatusFilter.ONGOING) {
            return "/api/activity/status/3";
        }
        if (filter == MobileStatusFilter.ENDED) {
            return "/api/activity/status/4";
        }
        return "/api/activity/list";
    }

    private String competitionListPathForFilter(MobileStatusFilter filter) {
        if (filter == MobileStatusFilter.UPCOMING) {
            return "/api/competition/status/PUBLISHED";
        }
        if (filter == MobileStatusFilter.ONGOING) {
            return "/api/competition/status/ONGOING";
        }
        if (filter == MobileStatusFilter.ENDED) {
            return "/api/competition/status/COMPLETED";
        }
        return "/api/competition/published";
    }

    private MobileStatusFilter classifyActivityStatus(JSONObject item) {
        long now = System.currentTimeMillis();
        long start = firstTimeMillis(item, new String[]{"startTime", "activityStart", "beginTime"});
        long end = firstTimeMillis(item, new String[]{"endTime", "activityEnd", "finishTime"});
        long registrationStart = firstTimeMillis(item, new String[]{"registrationStartTime", "registerStart", "signupStart"});
        long registrationEnd = firstTimeMillis(item, new String[]{"registrationEndTime", "registerEnd", "signupEnd"});
        if (end > 0 && now > end) {
            return MobileStatusFilter.ENDED;
        }
        if (start > 0 && end > 0 && now >= start && now <= end) {
            return MobileStatusFilter.ONGOING;
        }
        long effectiveStart = start > 0 ? start : registrationStart;
        if (effectiveStart > 0 && now < effectiveStart) {
            return MobileStatusFilter.UPCOMING;
        }
        if (registrationEnd > 0 && now > registrationEnd && start <= 0) {
            return MobileStatusFilter.ENDED;
        }
        MobileStatusFilter explicit = classifyStatusText(firstValue(item, new String[]{
                "activityStatusName", "statusName", "activityStatus", "status", "state"
        }, ""));
        if (explicit != null) {
            return explicit;
        }
        return MobileStatusFilter.ONGOING;
    }

    private MobileStatusFilter classifyCompetitionStatus(JSONObject item) {
        MobileStatusFilter explicit = classifyStatusText(firstValue(item, new String[]{
                "statusName", "status", "competitionStatus", "state"
        }, ""));
        if (explicit != null) {
            return explicit;
        }
        long now = System.currentTimeMillis();
        long start = firstTimeMillis(item, new String[]{"competitionStart", "startTime", "beginTime"});
        long end = firstTimeMillis(item, new String[]{"competitionEnd", "endTime", "finishTime"});
        long registrationStart = firstTimeMillis(item, new String[]{"registrationStart", "registerStart", "signupStart"});
        long registrationEnd = firstTimeMillis(item, new String[]{"registrationEnd", "registerEnd", "signupEnd", "registrationDeadline"});
        if (end > 0 && now > end) {
            return MobileStatusFilter.ENDED;
        }
        long effectiveStart = start > 0 ? start : registrationStart;
        if (effectiveStart > 0 && now < effectiveStart) {
            return MobileStatusFilter.UPCOMING;
        }
        if (registrationEnd > 0 && now > registrationEnd && start <= 0) {
            return MobileStatusFilter.ENDED;
        }
        return MobileStatusFilter.ONGOING;
    }

    private MobileStatusFilter classifyStatusText(String rawStatus) {
        String status = trim(rawStatus).toLowerCase(Locale.ROOT);
        if (status.isEmpty()) {
            return null;
        }
        if ("1".equals(status) || "published".equals(status)) {
            return MobileStatusFilter.UPCOMING;
        }
        if ("2".equals(status) || "3".equals(status)) {
            return MobileStatusFilter.ONGOING;
        }
        if ("4".equals(status)) {
            return MobileStatusFilter.ENDED;
        }
        if (status.contains("已结束") || status.contains("结束") || status.contains("截止")
                || status.contains("ended") || status.contains("closed") || status.contains("complete")) {
            return MobileStatusFilter.ENDED;
        }
        if (status.contains("未开始") || status.contains("即将") || status.contains("待开始")
                || status.contains("报名未开始") || status.contains("upcoming") || status.contains("pending")) {
            return MobileStatusFilter.UPCOMING;
        }
        if (status.contains("进行") || status.contains("已开始") || status.contains("报名中")
                || status.contains("发布") || status.contains("open") || status.contains("active")
                || status.contains("ongoing")) {
            return MobileStatusFilter.ONGOING;
        }
        return null;
    }

    private String statusLabel(MobileStatusFilter status, String fallback) {
        if (status != null && status != MobileStatusFilter.ALL) {
            return status.label;
        }
        return trim(fallback).isEmpty() ? "-" : trim(fallback);
    }

    private RegistrationGate buildRegistrationGate(JSONObject detail, boolean activity) {
        if (detail == null) {
            return new RegistrationGate(false, "暂不可报名", "报名信息暂不可用");
        }
        if (detail.optBoolean("registered", detail.optBoolean("isRegistered", false))) {
            return new RegistrationGate(false, "已报名", "你已经报名，无需重复提交");
        }
        String reason = firstValue(detail, new String[]{"cannotRegisterReason", "registerDisabledReason", "reason"}, "");
        if (detail.has("canRegister") && !detail.optBoolean("canRegister", true)) {
            return new RegistrationGate(false, disabledRegistrationLabel(reason), reason.isEmpty() ? "当前不可报名" : reason);
        }

        long now = System.currentTimeMillis();
        long registrationStart = firstTimeMillis(detail, activity
                ? new String[]{"registrationStartTime", "registerStart", "signupStart"}
                : new String[]{"registrationStart", "registerStart", "signupStart"});
        long registrationEnd = firstTimeMillis(detail, activity
                ? new String[]{"registrationEndTime", "registerEnd", "signupEnd"}
                : new String[]{"registrationEnd", "registrationDeadline", "registerEnd", "signupEnd"});
        long eventEnd = firstTimeMillis(detail, activity
                ? new String[]{"endTime", "activityEnd", "finishTime"}
                : new String[]{"competitionEnd", "endTime", "finishTime"});

        if (eventEnd > 0 && now > eventEnd) {
            return new RegistrationGate(false, activity ? "活动已结束" : "比赛已结束", activity ? "活动已结束，不能报名" : "比赛已结束，不能报名");
        }
        if (registrationStart > 0 && now < registrationStart) {
            return new RegistrationGate(false, "报名未开始", "报名未开始，请关注报名时间");
        }
        if (registrationEnd > 0 && now > registrationEnd) {
            return new RegistrationGate(false, "报名已截止", "报名时间已截止");
        }
        return new RegistrationGate(true, "立即报名", "");
    }

    private String disabledRegistrationLabel(String reason) {
        String value = trim(reason);
        if (value.contains("未开始")) {
            return "报名未开始";
        }
        if (value.contains("截止") || value.contains("结束")) {
            return "报名已截止";
        }
        if (value.contains("已报名")) {
            return "已报名";
        }
        return "暂不可报名";
    }

    private long firstTimeMillis(JSONObject item, String[] keys) {
        String value = firstValue(item, keys, "");
        return parseDateTimeMillis(value);
    }

    private long parseDateTimeMillis(String value) {
        String textValue = trim(value);
        if (textValue.isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(textValue);
        } catch (Exception ignored) {
            // Continue with formatted date parsing.
        }
        String normalized = textValue.replace('T', ' ');
        int plus = normalized.indexOf('+');
        if (plus > 0) {
            normalized = normalized.substring(0, plus);
        }
        if (normalized.endsWith("Z")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.contains(".")) {
            normalized = normalized.substring(0, normalized.indexOf('.'));
        }
        String[] patterns = new String[]{
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd HH:mm",
                "yyyy/MM/dd HH:mm:ss",
                "yyyy/MM/dd HH:mm",
                "yyyy-MM-dd",
                "yyyy/MM/dd"
        };
        for (int i = 0; i < patterns.length; i++) {
            try {
                SimpleDateFormat format = new SimpleDateFormat(patterns[i], Locale.CHINA);
                Date date = format.parse(normalized);
                if (date != null) {
                    return date.getTime();
                }
            } catch (Exception ignored) {
                // Try the next supported date format.
            }
        }
        return 0L;
    }

    private JSONArray filterRowsByCurrentTenant(JSONArray rows) {
        JSONArray filtered = new JSONArray();
        if (rows == null) {
            return filtered;
        }
        for (int i = 0; i < rows.length(); i++) {
            JSONObject item = rows.optJSONObject(i);
            if (item != null && belongsToCurrentTenant(item)) {
                filtered.put(item);
            }
        }
        return filtered;
    }

    private JSONObject selectCurrentTenantUserProfile(JSONArray rows) {
        if (rows == null || rows.length() == 0) {
            return null;
        }
        JSONObject fallback = null;
        for (int i = 0; i < rows.length(); i++) {
            JSONObject item = rows.optJSONObject(i);
            if (item == null) {
                continue;
            }
            if (fallback == null) {
                fallback = item;
            }
            if (belongsToCurrentTenant(item)) {
                return item;
            }
        }
        return fallback;
    }

    private boolean belongsToCurrentTenant(JSONObject item) {
        if (item == null) {
            return false;
        }
        String rowTenantId = firstValue(item, new String[]{"tenantId", "organizationId", "tenant_id", "orgId"}, "");
        JSONObject tenant = item.optJSONObject("tenant");
        if (rowTenantId.isEmpty() && tenant != null) {
            rowTenantId = firstValue(tenant, new String[]{"id", "tenantId", "organizationId"}, "");
        }
        if (rowTenantId.isEmpty()) {
            return true;
        }
        return trim(tenantId).equals(trim(rowTenantId));
    }

    private String extractUserId(JSONObject item) {
        String value = firstValue(item, new String[]{"userId", "commentUserId", "authorId", "createBy", "createdBy", "uid"}, "");
        if (!value.isEmpty()) {
            return value;
        }
        JSONObject user = item == null ? null : item.optJSONObject("user");
        return firstValue(user, new String[]{"id", "userId"}, "");
    }

    private String safeRequest(String method, String path, boolean authorized) {
        try {
            return request(method, path, null, authorized);
        } catch (Exception e) {
            return "{\"error\":\"" + escapeJson(e.getMessage() == null ? "request failed" : e.getMessage()) + "\"}";
        }
    }

    private String request(String method, String path, String payload, boolean authorized) throws IOException {
        if (authorized && token.isEmpty()) {
            throw new IOException("登录已失效，请重新登录");
        }
        URL url = new URL(resolveAbsoluteUrl(path));
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(12000);
        connection.setRequestMethod(method);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("X-Client-Type", "mobile");
        connection.setRequestProperty("X-Tenant-Id", tenantId);
        if (roleId > 0) {
            connection.setRequestProperty("X-Role-Id", String.valueOf(roleId));
        }
        if (authorized) {
            connection.setRequestProperty("Authorization", "Bearer " + token);
        }
        if (payload != null) {
            connection.setDoOutput(true);
            OutputStream output = connection.getOutputStream();
            output.write(payload.getBytes(StandardCharsets.UTF_8));
            output.close();
        }

        int status = connection.getResponseCode();
        InputStream input = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
        String body = readAll(input);
        connection.disconnect();
        if (status >= 400) {
            throw new IOException("HTTP " + status + " " + body);
        }
        if (authorized) {
            refreshAuthSession();
        }
        return body;
    }

    private void refreshAuthSession() {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            return;
        }
        HttpURLConnection connection = null;
        try {
            URL url = new URL(resolveAbsoluteUrl("/auth/refresh-session"));
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(12000);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("X-Client-Type", "mobile");
            connection.setRequestProperty("X-Tenant-Id", tenantId);
            if (roleId > 0) {
                connection.setRequestProperty("X-Role-Id", String.valueOf(roleId));
            }
            connection.setDoOutput(true);
            OutputStream output = connection.getOutputStream();
            output.write(refreshToken.getBytes(StandardCharsets.UTF_8));
            output.close();

            int status = connection.getResponseCode();
            InputStream input = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            String body = readAll(input);
            if (status >= 400) {
                clearAuthSession();
                return;
            }
            JSONObject root = new JSONObject(body);
            JSONObject data = root.optJSONObject("data");
            if (data == null) {
                data = root;
            }
            String refreshedToken = extractLoginToken(data);
            if (!refreshedToken.isEmpty()) {
                token = refreshedToken;
            }
            String refreshedRefreshToken = data.optString("refreshToken", data.optString("refresh_token", "")).trim();
            if (!refreshedRefreshToken.isEmpty()) {
                refreshToken = refreshedRefreshToken;
            }
            persistAuthSession();
        } catch (Exception ignored) {
            // Keep the current session until the server explicitly rejects it.
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String readAll(InputStream input) throws IOException {
        if (input == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        reader.close();
        return builder.toString();
    }

    private void showLoadError(final TextView view, final Exception e) {
        final String message = e.getMessage();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                view.setText("加载失败：" + message);
                view.setTextColor(DANGER);
            }
        });
    }

    private void addMetricRow(LinearLayout parent, String label, String value) {
        LinearLayout row = horizontal();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(10), 0, 0);
        TextView labelView = text(label, 14, MUTED, Typeface.NORMAL);
        TextView valueView = text(value == null || value.isEmpty() ? "-" : value, 16, PRIMARY, Typeface.BOLD);
        valueView.setGravity(Gravity.RIGHT);
        row.addView(labelView, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(valueView, new LinearLayout.LayoutParams(0, -2, 1));
        parent.addView(row);
    }

    private String firstValue(JSONObject item, String[] keys, String fallback) {
        if (item == null || keys == null) {
            return fallback;
        }
        for (int i = 0; i < keys.length; i++) {
            String value = item.optString(keys[i], "");
            if (value != null && !value.trim().isEmpty() && !"null".equalsIgnoreCase(value.trim())) {
                return value.trim();
            }
        }
        return fallback;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String collectValues(JSONObject item, String[] keys, String separator) {
        StringBuilder builder = new StringBuilder();
        if (item == null || keys == null) {
            return "";
        }
        for (int i = 0; i < keys.length; i++) {
            String value = firstValue(item, new String[]{keys[i]}, "");
            if (value.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(separator);
            }
            builder.append(value);
        }
        return builder.toString();
    }

    private String buildJoinedText(String[] values) {
        StringBuilder builder = new StringBuilder();
        if (values == null) {
            return "";
        }
        for (int i = 0; i < values.length; i++) {
            String value = trim(values[i]);
            if (value.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(" · ");
            }
            builder.append(value);
        }
        return builder.toString();
    }

    private String stripHtml(String value) {
        return trim(value)
                .replaceAll("(?is)<br\\s*/?>", "\n")
                .replaceAll("(?is)</p>", "\n")
                .replaceAll("(?is)<[^>]+>", "")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .trim();
    }

    private String extractDate(String value) {
        String textValue = trim(value);
        if (textValue.length() >= 10) {
            return textValue.substring(0, 10);
        }
        return textValue.isEmpty() ? "未定日期" : textValue;
    }

    private void addActivityCalendarEntries(ArrayList<CalendarEntry> entries, JSONArray activities) {
        for (int i = 0; i < activities.length(); i++) {
            JSONObject item = activities.optJSONObject(i);
            if (item == null) {
                continue;
            }
            String id = firstValue(item, new String[]{"id", "activityId"}, "");
            String start = firstValue(item, new String[]{"startTime", "activityStart", "beginTime"}, "");
            String end = firstValue(item, new String[]{"endTime", "activityEnd"}, "");
            String location = firstValue(item, new String[]{"location", "address", "venue"}, "");
            entries.add(new CalendarEntry(
                    extractDate(start),
                    "活动",
                    firstValue(item, new String[]{"title", "activityName", "name"}, "活动"),
                    buildJoinedText(new String[]{firstValue(item, new String[]{"statusName", "status"}, ""), location}),
                    firstValue(item, new String[]{"summary", "description", "content"}, ""),
                    end.isEmpty() ? start : start + " - " + end,
                    id));
        }
    }

    private void addCompetitionCalendarEntries(ArrayList<CalendarEntry> entries, JSONArray competitions) {
        for (int i = 0; i < competitions.length(); i++) {
            JSONObject item = competitions.optJSONObject(i);
            if (item == null) {
                continue;
            }
            String id = firstValue(item, new String[]{"id", "competitionId"}, "");
            String start = firstValue(item, new String[]{"competitionStart", "startTime", "registrationDeadline", "registrationEnd"}, "");
            String end = firstValue(item, new String[]{"competitionEnd", "endTime"}, "");
            String type = firstValue(item, new String[]{"competitionType", "type", "level"}, "赛事");
            entries.add(new CalendarEntry(
                    extractDate(start),
                    "比赛",
                    firstValue(item, new String[]{"title", "competitionName", "name"}, "比赛"),
                    buildJoinedText(new String[]{type, firstValue(item, new String[]{"statusName", "status"}, "")}),
                    firstValue(item, new String[]{"summary", "description", "content"}, ""),
                    end.isEmpty() ? start : start + " - " + end,
                    id));
        }
    }

    private TextView sectionTitle(String title) {
        TextView view = text(title, 17, TEXT, Typeface.BOLD);
        view.setPadding(0, dp(22), 0, dp(10));
        return view;
    }

    private LinearLayout topBar(String title, String subtitle) {
        LinearLayout bar = vertical();
        bar.setPadding(0, dp(8), 0, dp(14));
        TextView titleView = text(title, 24, TEXT, Typeface.BOLD);
        titleView.setPadding(0, 0, 0, dp(2));
        bar.addView(titleView);
        TextView subtitleView = text(subtitle, 13, MUTED, Typeface.NORMAL);
        subtitleView.setLineSpacing(dp(2), 1.0f);
        bar.addView(subtitleView);
        return bar;
    }

    private LinearLayout mobileSearchBar() {
        LinearLayout bar = horizontal();
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(14), 0, dp(14), 0);
        bar.setBackground(roundStroke(Color.rgb(250, 252, 255), Color.rgb(233, 240, 250), dp(18)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(46));
        params.setMargins(0, 0, 0, dp(14));
        bar.setLayoutParams(params);
        TextView icon = text("⌕", 22, Color.rgb(143, 156, 178), Typeface.BOLD);
        icon.setGravity(Gravity.CENTER);
        bar.addView(icon, new LinearLayout.LayoutParams(dp(28), -1));
        TextView hint = text("搜索活动、社团、新闻", 14, Color.rgb(143, 156, 178), Typeface.BOLD);
        hint.setPadding(dp(8), 0, 0, 0);
        bar.addView(hint, new LinearLayout.LayoutParams(0, -1, 1));
        bar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateChild(contentScreenAction());
            }
        });
        return bar;
    }

    private LinearLayout mobileBannerCard() {
        LinearLayout hero = card();
        hero.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams heroParams = new LinearLayout.LayoutParams(-1, -2);
        heroParams.setMargins(0, 0, 0, dp(16));
        hero.setLayoutParams(heroParams);
        ImageView banner = image(R.drawable.hero_mobile_campus_v2, "百团大战社团招新视觉图");
        LinearLayout.LayoutParams bannerParams = new LinearLayout.LayoutParams(-1, dp(154));
        bannerParams.setMargins(0, 0, 0, dp(12));
        hero.addView(banner, bannerParams);
        hero.addView(text("百团大战 2024社团招新", 24, TEXT, Typeface.BOLD));
        TextView copy = text("加入我们，发现更好的自己", 14, MUTED, Typeface.BOLD);
        copy.setPadding(0, dp(6), 0, 0);
        hero.addView(copy);
        return hero;
    }

    private LinearLayout mobileQuickGrid() {
        View[] actions = new View[]{
                quickAction("活动", "活", ACCENT, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        navigateChild(activityHighlightsScreenAction());
                    }
                }),
                quickAction("比赛", "杯", Color.rgb(255, 181, 46), new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        navigateChild(competitionCenterScreenAction());
                    }
                }),
                quickAction("新闻", "讯", PRIMARY, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        navigateChild(newsCenterScreenAction());
                    }
                }),
                quickAction("社团", "社", Color.rgb(54, 203, 161), new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        navigateChild(clubDirectoryScreenAction());
                    }
                }),
                quickAction("签到", "签", Color.rgb(24, 190, 255), new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        navigateChild(messagesScreenAction());
                    }
                }),
                quickAction("我的学分", "分", Color.rgb(139, 92, 246), new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        navigateChild(myCreditsScreenAction());
                    }
                }),
                quickAction("排行榜", "榜", Color.rgb(250, 173, 20), new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        navigateChild(myServicesScreenAction());
                    }
                }),
                quickAction("更多", "多", Color.rgb(116, 103, 240), new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        navigateChild(myClubsScreenAction());
                    }
                })
        };
        LinearLayout wrapper = card();
        wrapper.setPadding(dp(10), dp(12), dp(10), dp(4));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, dp(16));
        wrapper.setLayoutParams(params);
        wrapper.addView(quickActionGrid(actions));
        return wrapper;
    }

    private LinearLayout mobileSectionHeader(String title, String actionLabel, View.OnClickListener action) {
        LinearLayout row = horizontal();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(2), 0, dp(10));
        TextView titleView = text(title, 20, TEXT, Typeface.BOLD);
        row.addView(titleView, new LinearLayout.LayoutParams(0, -2, 1));
        TextView actionView = text(actionLabel + " ›", 13, MUTED, Typeface.BOLD);
        actionView.setGravity(Gravity.RIGHT);
        row.addView(actionView, new LinearLayout.LayoutParams(dp(78), -2));
        row.setOnClickListener(action);
        return row;
    }

    private LinearLayout mobileEventCard(String title, String meta, String time, String place, String organizer, int imageRes, View.OnClickListener action) {
        return feedCard(title, meta, time + "\n" + place, organizer, imageRes, action);
    }

    private LinearLayout mobileStatusTabs(MobileStatusFilter selected, View.OnClickListener listener) {
        LinearLayout tabs = horizontal();
        tabs.setGravity(Gravity.CENTER_VERTICAL);
        tabs.setPadding(0, 0, 0, dp(14));
        MobileStatusFilter[] filters = MobileStatusFilter.values();
        for (int i = 0; i < filters.length; i++) {
            MobileStatusFilter filter = filters[i];
            boolean active = filter == selected;
            TextView tab = text(filter.label, 14, active ? PRIMARY : TEXT, active ? Typeface.BOLD : Typeface.NORMAL);
            tab.setGravity(Gravity.CENTER);
            tab.setBackground(active ? roundStroke(Color.rgb(235, 243, 255), Color.rgb(199, 221, 255), dp(14)) : roundRect(Color.TRANSPARENT, dp(14)));
            tab.setTag(filter);
            if (listener != null) {
                tab.setOnClickListener(listener);
            }
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(38), 1);
            params.setMargins(i == 0 ? 0 : dp(6), 0, i == filters.length - 1 ? 0 : dp(6), 0);
            tabs.addView(tab, params);
        }
        return tabs;
    }

    private LinearLayout mobileStatusTabs() {
        return mobileStatusTabs(MobileStatusFilter.ALL, null);
    }

    private void detailBottomActionBar(LinearLayout body, View.OnClickListener signupAction) {
        detailBottomActionBar(body, new RegistrationGate(true, "立即报名", ""), signupAction);
    }

    private void detailBottomActionBar(LinearLayout body, RegistrationGate gate, View.OnClickListener signupAction) {
        if (gate == null) {
            gate = new RegistrationGate(false, "暂不可报名", "报名信息暂不可用");
        }
        LinearLayout bar = horizontal();
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(8), dp(12), dp(8), dp(6));
        TextView favorite = text("收藏", 12, TEXT, Typeface.BOLD);
        favorite.setGravity(Gravity.CENTER);
        TextView share = text("分享", 12, TEXT, Typeface.BOLD);
        share.setGravity(Gravity.CENTER);
        Button signup = primaryButton(gate.label);
        signup.setEnabled(gate.enabled);
        if (gate.enabled) {
            signup.setOnClickListener(signupAction);
        } else {
            signup.setTextColor(Color.WHITE);
            signup.setBackground(roundRect(Color.rgb(148, 163, 184), dp(14)));
        }
        bar.addView(favorite, new LinearLayout.LayoutParams(dp(58), dp(48)));
        bar.addView(share, new LinearLayout.LayoutParams(dp(58), dp(48)));
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(0, dp(48), 1);
        buttonParams.setMargins(dp(10), 0, 0, 0);
        bar.addView(signup, buttonParams);
        body.addView(bar);
        if (!gate.enabled && !trim(gate.reason).isEmpty()) {
            TextView reason = text(gate.reason, 12, MUTED, Typeface.NORMAL);
            reason.setGravity(Gravity.RIGHT);
            reason.setPadding(0, 0, dp(8), dp(10));
            body.addView(reason);
        }
    }

    private TextView tag(String label) {
        TextView view = text(label, 12, PRIMARY, Typeface.BOLD);
        view.setSingleLine(true);
        view.setPadding(dp(8), dp(4), dp(8), dp(4));
        view.setBackground(roundStroke(Color.rgb(235, 243, 255), Color.rgb(199, 221, 255), dp(8)));
        return view;
    }

    private LinearLayout metricPill(String label, String value, int tone) {
        LinearLayout pill = vertical();
        pill.setPadding(dp(12), dp(10), dp(12), dp(10));
        pill.setBackground(roundStroke(SURFACE_ALT, BORDER, dp(8)));
        TextView labelView = text(label, 12, MUTED, Typeface.NORMAL);
        TextView valueView = text(value, 17, tone, Typeface.BOLD);
        valueView.setPadding(0, dp(2), 0, 0);
        pill.addView(labelView);
        pill.addView(valueView);
        return pill;
    }

    private LinearLayout heroCarousel() {
        LinearLayout hero = card();
        hero.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams heroParams = new LinearLayout.LayoutParams(-1, -2);
        heroParams.setMargins(0, 0, 0, dp(2));
        hero.setLayoutParams(heroParams);

        ImageView banner = image(R.drawable.hero_mobile_campus_v2, "移动端运营概览");
        LinearLayout.LayoutParams bannerParams = new LinearLayout.LayoutParams(-1, dp(142));
        bannerParams.setMargins(0, 0, 0, dp(12));
        hero.addView(banner, bannerParams);

        LinearLayout profileRow = horizontal();
        profileRow.setGravity(Gravity.CENTER_VERTICAL);
        hero.addView(profileRow);
        profileRow.addView(avatarImageView(displayName, avatarUrl, dp(44)), new LinearLayout.LayoutParams(dp(44), dp(44)));
        LinearLayout profileCopy = vertical();
        profileCopy.setPadding(dp(12), 0, 0, 0);
        profileRow.addView(profileCopy, new LinearLayout.LayoutParams(0, -2, 1));
        profileCopy.addView(text("你好，" + displayName, 21, TEXT, Typeface.BOLD));
        profileCopy.addView(text(tenantName + " · " + roleLabel(), 13, MUTED, Typeface.NORMAL));
        profileRow.addView(tag("v" + getInstalledVersionCode()));

        LinearLayout stats = horizontal();
        stats.setPadding(0, dp(14), 0, 0);
        hero.addView(stats);
        LinearLayout.LayoutParams left = new LinearLayout.LayoutParams(0, -2, 1);
        left.setMargins(0, 0, dp(7), 0);
        stats.addView(metricPill("今日安排", "日历同步", PRIMARY), left);
        LinearLayout.LayoutParams right = new LinearLayout.LayoutParams(0, -2, 1);
        right.setMargins(dp(7), 0, 0, 0);
        stats.addView(metricPill("我的报名", "服务可查", INFO), right);
        return hero;
    }

    private LinearLayout visualShortcutRail() {
        LinearLayout rail = vertical();
        rail.setPadding(0, 0, 0, dp(2));
        LinearLayout first = horizontal();
        LinearLayout second = horizontal();
        rail.addView(first, rowParams(0, dp(10)));
        rail.addView(second, rowParams(0, 0));
        first.addView(visualShortcut("新闻", "N", PRIMARY, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateChild(newsCenterScreenAction());
            }
        }), shortcutParams(0, dp(5)));
        first.addView(visualShortcut("活动", "A", ACCENT, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateChild(activityHighlightsScreenAction());
            }
        }), shortcutParams(dp(5), 0));
        second.addView(visualShortcut("比赛", "C", INFO, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateChild(competitionCenterScreenAction());
            }
        }), shortcutParams(0, dp(5)));
        second.addView(visualShortcut("日历", "D", PRIMARY_DARK, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateChild(contentCalendarScreenAction());
            }
        }), shortcutParams(dp(5), 0));
        return rail;
    }

    private LinearLayout visualShortcut(String label, String badge, int tone, View.OnClickListener action) {
        LinearLayout item = horizontal();
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dp(12), dp(10), dp(12), dp(10));
        item.setBackground(roundStroke(SURFACE, BORDER, dp(8)));
        item.addView(iconBadge(badge, tone), new LinearLayout.LayoutParams(dp(38), dp(38)));
        TextView labelView = text(label, 15, TEXT, Typeface.BOLD);
        labelView.setPadding(dp(10), 0, 0, 0);
        item.addView(labelView, new LinearLayout.LayoutParams(0, -2, 1));
        item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (enforceUpdateGate()) {
                    return;
                }
                if (action != null) {
                    action.onClick(view);
                }
            }
        });
        return item;
    }

    private LinearLayout feedCard(String title, String meta, String summary, String footer, int imageRes, View.OnClickListener action) {
        LinearLayout card = horizontal();
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(10), dp(10), dp(12), dp(10));
        card.setBackground(roundStroke(SURFACE, BORDER, dp(8)));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(cardParams);

        ImageView thumb = image(imageRes, title);
        LinearLayout.LayoutParams thumbParams = new LinearLayout.LayoutParams(dp(88), dp(72));
        thumbParams.setMargins(0, 0, dp(12), 0);
        card.addView(thumb, thumbParams);

        LinearLayout copy = vertical();
        card.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout header = horizontal();
        header.setGravity(Gravity.CENTER_VERTICAL);
        copy.addView(header);
        TextView titleView = text(title, 17, TEXT, Typeface.BOLD);
        titleView.setSingleLine(true);
        header.addView(titleView, new LinearLayout.LayoutParams(0, -2, 1));
        if (!trim(meta).isEmpty()) {
            header.addView(tag(meta));
        }
        TextView summaryView = text(stripHtml(summary), 13, MUTED, Typeface.NORMAL);
        summaryView.setMaxLines(2);
        summaryView.setPadding(0, dp(6), 0, 0);
        summaryView.setLineSpacing(dp(2), 1.0f);
        copy.addView(summaryView);
        if (!trim(footer).isEmpty()) {
            TextView footerView = text(footer, 12, PRIMARY, Typeface.BOLD);
            footerView.setPadding(0, dp(6), 0, 0);
            copy.addView(footerView);
        }
        if (action != null) {
            card.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (enforceUpdateGate()) {
                        return;
                    }
                    action.onClick(view);
                }
            });
        }
        return card;
    }

    private LinearLayout contentCard(String title, String meta, String summary, String footer, String tagLabel, View.OnClickListener action) {
        LinearLayout item = card();
        item.setPadding(dp(15), dp(13), dp(15), dp(13));
        if (!trim(tagLabel).isEmpty()) {
            item.addView(tag(tagLabel));
        }
        TextView titleView = text(title, 18, TEXT, Typeface.BOLD);
        titleView.setPadding(0, dp(8), 0, dp(3));
        item.addView(titleView);
        if (!trim(meta).isEmpty()) {
            item.addView(text(meta, 13, PRIMARY, Typeface.BOLD));
        }
        String summaryText = stripHtml(summary);
        if (!summaryText.isEmpty()) {
            if (summaryText.length() > 118) {
                summaryText = summaryText.substring(0, 118) + "...";
            }
            TextView summaryView = text(summaryText, 14, MUTED, Typeface.NORMAL);
            summaryView.setPadding(0, dp(8), 0, 0);
            summaryView.setLineSpacing(dp(2), 1.0f);
            item.addView(summaryView);
        }
        if (!trim(footer).isEmpty()) {
            TextView footerView = text(footer, 12, MUTED, Typeface.NORMAL);
            footerView.setPadding(0, dp(8), 0, 0);
            item.addView(footerView);
        }
        if (action != null) {
            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (enforceUpdateGate()) {
                        return;
                    }
                    action.onClick(view);
                }
            });
        }
        return item;
    }

    private LinearLayout shortcut(String title, String subtitle, View.OnClickListener action) {
        LinearLayout item = contentCard(title, "", subtitle, "", "", null);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, dp(10));
        item.setLayoutParams(params);
        if (action != null) {
            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (enforceUpdateGate()) {
                        return;
                    }
                    action.onClick(view);
                }
            });
        }
        return item;
    }

    private LinearLayout.LayoutParams rowParams(int top, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, top, 0, bottom);
        return params;
    }

    private LinearLayout.LayoutParams shortcutParams(int left, int right) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(64), 1);
        params.setMargins(left, 0, right, 0);
        return params;
    }

    private LinearLayout quickActionGrid(View[] actions) {
        LinearLayout grid = vertical();
        grid.setPadding(0, 0, 0, dp(4));
        LinearLayout row = null;
        for (int i = 0; i < actions.length; i++) {
            if (i % 4 == 0) {
                row = horizontal();
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
                rowParams.setMargins(0, 0, 0, dp(10));
                grid.addView(row, rowParams);
            }
            LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(0, dp(96), 1);
            itemParams.setMargins(dp(4), 0, dp(4), 0);
            row.addView(actions[i], itemParams);
        }
        return grid;
    }

    private LinearLayout quickAction(String label, String badge, int tone, View.OnClickListener action) {
        LinearLayout item = vertical();
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(6), dp(8), dp(6), dp(8));
        item.setBackground(roundStroke(SURFACE, BORDER, dp(10)));
        item.addView(iconBadge(badge, tone), new LinearLayout.LayoutParams(dp(44), dp(44)));
        TextView labelView = text(label, 13, TEXT, Typeface.BOLD);
        labelView.setGravity(Gravity.CENTER);
        labelView.setSingleLine(true);
        labelView.setPadding(0, dp(8), 0, 0);
        item.addView(labelView);
        item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (enforceUpdateGate()) {
                    return;
                }
                if (action != null) {
                    action.onClick(view);
                }
            }
        });
        return item;
    }

    private TextView iconBadge(String label, int tone) {
        TextView view = text(label, 11, Color.WHITE, Typeface.BOLD);
        view.setGravity(Gravity.CENTER);
        view.setSingleLine(true);
        view.setBackground(roundRect(tone, dp(14)));
        return view;
    }

    private TextView avatarView(String name, int size) {
        String trimmed = trim(name);
        String initial = trimmed.isEmpty() ? "我" : trimmed.substring(0, 1).toUpperCase();
        TextView view = text(initial, 22, Color.WHITE, Typeface.BOLD);
        view.setGravity(Gravity.CENTER);
        view.setBackground(roundRect(PRIMARY, size / 2));
        return view;
    }

    private View avatarImageView(final String name, String avatar, final int size) {
        final FrameLayout frame = new FrameLayout(this);
        frame.addView(avatarView(name, size), new FrameLayout.LayoutParams(size, size));

        final String avatarValue = trim(avatar);
        if (avatarValue.isEmpty()) {
            return frame;
        }

        final ImageView imageView = new ImageView(this);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setBackground(roundRect(PRIMARY, size / 2));
        imageView.setClipToOutline(true);
        imageView.setVisibility(View.INVISIBLE);
        frame.addView(imageView, new FrameLayout.LayoutParams(size, size));

        new Thread(new Runnable() {
            @Override
            public void run() {
                final Bitmap bitmap = loadAvatarBitmap(avatarValue);
                if (bitmap == null) {
                    return;
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imageView.setImageBitmap(bitmap);
                        imageView.setVisibility(View.VISIBLE);
                    }
                });
            }
        }).start();
        return frame;
    }

    private Bitmap loadAvatarBitmap(String avatar) {
        HttpURLConnection connection = null;
        InputStream input = null;
        try {
            String url = resolveManagedMediaUrl(trim(avatar));
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(12000);
            connection.setRequestProperty("Accept", "image/*,*/*");
            input = new BufferedInputStream(connection.getInputStream());
            return BitmapFactory.decodeStream(input);
        } catch (Exception ignored) {
            return null;
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (IOException ignored) {
                // ignored
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private TextView emptyState(String message) {
        TextView view = text(message, 15, MUTED, Typeface.NORMAL);
        view.setGravity(Gravity.CENTER);
        view.setPadding(0, dp(36), 0, dp(36));
        return view;
    }

    private void renderBottomNavigation() {
        if (root == null) {
            return;
        }
        LinearLayout nav;
        if (root.getChildCount() > 1 && root.getChildAt(1) instanceof LinearLayout) {
            nav = (LinearLayout) root.getChildAt(1);
            nav.removeAllViews();
        } else {
            nav = horizontal();
            nav.setGravity(Gravity.CENTER);
            nav.setPadding(dp(6), dp(4), dp(6), dp(4));
            nav.setBackgroundColor(SURFACE);
            nav.setElevation(dp(8));
            root.addView(nav, new LinearLayout.LayoutParams(-1, dp(60)));
        }
        nav.addView(navItem(NavTab.HOME, "首页", "H", R.drawable.ic_nav_home, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (enforceUpdateGate()) {
                    return;
                }
                openMainScreen(homeScreenAction());
            }
        }), new LinearLayout.LayoutParams(0, -1, 1));
        nav.addView(navItem(NavTab.CONTENT, "社团", "C", R.drawable.ic_nav_messages, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (enforceUpdateGate()) {
                    return;
                }
                openMainScreen(clubDirectoryScreenAction());
            }
        }), new LinearLayout.LayoutParams(0, -1, 1));
        nav.addView(navItem(NavTab.SERVICES, "+", "P", R.drawable.ic_nav_home, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (enforceUpdateGate()) {
                    return;
                }
                openMainScreen(servicesScreenAction());
            }
        }), new LinearLayout.LayoutParams(0, -1, 1));
        nav.addView(navItem(NavTab.MESSAGES, "消息", "M", R.drawable.ic_nav_messages, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (enforceUpdateGate()) {
                    return;
                }
                openMainScreen(messagesScreenAction());
            }
        }), new LinearLayout.LayoutParams(0, -1, 1));
        nav.addView(navItem(NavTab.PROFILE, "我的", "P", R.drawable.ic_nav_profile, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (enforceUpdateGate()) {
                    return;
                }
                openMainScreen(profileScreenAction());
            }
        }), new LinearLayout.LayoutParams(0, -1, 1));
    }

    private LinearLayout navItem(NavTab tab, String label, String badge, int iconResId, View.OnClickListener action) {
        boolean selected = activeTab == tab;
        LinearLayout item = vertical();
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(2), dp(4), dp(2), dp(4));
        boolean centerAction = "+".equals(label);
        item.setBackground(centerAction ? roundRect(PRIMARY, dp(22)) : (selected ? roundStroke(Color.rgb(235, 243, 255), Color.rgb(199, 221, 255), dp(12)) : roundRect(Color.TRANSPARENT, dp(12))));
        ImageView icon = new ImageView(this);
        icon.setImageResource(iconResId);
        icon.setColorFilter(centerAction ? Color.WHITE : (selected ? PRIMARY : MUTED));
        item.addView(icon, new LinearLayout.LayoutParams(dp(22), dp(22)));
        TextView labelView = text(label, centerAction ? 18 : 11, centerAction ? Color.WHITE : (selected ? PRIMARY : MUTED), (selected || centerAction) ? Typeface.BOLD : Typeface.NORMAL);
        labelView.setGravity(Gravity.CENTER);
        labelView.setSingleLine(true);
        labelView.setPadding(0, dp(3), 0, 0);
        item.addView(labelView);
        item.setContentDescription(label + " " + badge);
        item.setOnClickListener(action);
        return item;
    }

    private Button primaryButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextColor(Color.WHITE);
        button.setTextSize(16);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackground(roundRect(PRIMARY, dp(14)));
        return button;
    }

    private Button secondaryButton(String label) {
        Button button = primaryButton(label);
        button.setTextColor(PRIMARY);
        button.setBackground(roundStroke(Color.TRANSPARENT, PRIMARY, dp(14)));
        return button;
    }

    private EditText input(String hint, String value, int type) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setText(value);
        editText.setSingleLine(true);
        editText.setTextSize(15);
        editText.setInputType(type);
        editText.setPadding(dp(12), 0, dp(12), 0);
        editText.setBackground(roundStroke(SURFACE, Color.rgb(216, 224, 222), dp(10)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(46));
        params.setMargins(0, dp(6), 0, dp(12));
        editText.setLayoutParams(params);
        return editText;
    }

    private EditText multiLineInput(String hint, String value) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setText(value);
        editText.setSingleLine(false);
        editText.setMinLines(3);
        editText.setMaxLines(5);
        editText.setGravity(Gravity.TOP);
        editText.setTextSize(15);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        editText.setPadding(dp(12), dp(10), dp(12), dp(10));
        editText.setBackground(roundStroke(SURFACE, Color.rgb(216, 224, 222), dp(10)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(96));
        params.setMargins(0, dp(12), 0, dp(8));
        editText.setLayoutParams(params);
        return editText;
    }

    private TextView label(String label) {
        TextView view = text(label, 13, MUTED, Typeface.BOLD);
        view.setPadding(0, dp(6), 0, 0);
        return view;
    }

    private TextView text(String value, int sp, int color, int style) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, style);
        return view;
    }

    private ImageView image(int resId, String description) {
        ImageView view = new ImageView(this);
        view.setImageResource(resId);
        view.setContentDescription(description);
        view.setScaleType(ImageView.ScaleType.CENTER_CROP);
        return view;
    }

    private LinearLayout card() {
        LinearLayout layout = vertical();
        layout.setBackground(roundStroke(SURFACE, BORDER, dp(8)));
        return layout;
    }

    private LinearLayout vertical() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private LinearLayout horizontal() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        return layout;
    }

    private GradientDrawable roundRect(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private GradientDrawable roundStroke(int color, int strokeColor, int radius) {
        GradientDrawable drawable = roundRect(color, radius);
        drawable.setStroke(dp(1), strokeColor);
        return drawable;
    }

    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(48));
        params.setMargins(0, dp(14), 0, 0);
        return params;
    }

    private LinearLayout.LayoutParams compactButtonParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(40));
        params.setMargins(0, dp(8), 0, 0);
        return params;
    }

    private String normalizeBaseUrl(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            trimmed = DEFAULT_BASE_URL;
        }
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String resolveAbsoluteUrl(String pathOrUrl) {
        if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
            return pathOrUrl;
        }
        if (!pathOrUrl.startsWith("/")) {
            return baseUrl + "/" + pathOrUrl;
        }
        return baseUrl + pathOrUrl;
    }

    private String resolveManagedMediaUrl(String pathOrUrl) {
        String value = trim(pathOrUrl);
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return value;
        }
        while (value.startsWith("/")) {
            value = value.substring(1);
        }
        if (value.startsWith("minio-files/")) {
            return baseUrl + "/" + value;
        }
        if (value.startsWith("rk-") || value.startsWith("rk_b") || value.startsWith("rk-bucket/")) {
            return baseUrl + "/minio-files/" + value;
        }
        return resolveAbsoluteUrl(pathOrUrl);
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }

    private long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return 0L;
        }
    }

    private int getInstalledVersionCode() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            return info.versionCode <= 0 ? 1 : info.versionCode;
        } catch (Exception e) {
            return 1;
        }
    }

    private String roleLabel() {
        return roleLabel(String.valueOf(roleId));
    }

    private String roleLabel(String roleIdValue) {
        long parsedRoleId = parseLong(roleIdValue);
        if (parsedRoleId == 1L) {
            return "管理员";
        }
        if (parsedRoleId == 2L) {
            return "普通成员";
        }
        if (parsedRoleId == 3L) {
            return "租户管理员";
        }
        if (parsedRoleId == 7L) {
            return "社团负责人";
        }
        if (parsedRoleId == 8L) {
            return "指导老师";
        }
        return parsedRoleId > 0 ? "成员角色 " + parsedRoleId : "普通成员";
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
