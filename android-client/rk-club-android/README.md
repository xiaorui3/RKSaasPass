# RK Club Android

独立 Android 客户端，和 Web 前端分开维护。当前版本使用原生 Java Activity，不依赖 Gradle，可以直接用本机 Android SDK 命令行工具构建。

## 功能

- 登录 `/auth/login`
- 底部导航：首页、消息、我的
- 消息页读取 `/api/notifications`
- 个人中心展示当前账号、租户和登录态

## 构建

```powershell
powershell -ExecutionPolicy Bypass -File android-client\rk-club-android\build-apk.ps1
```

APK 输出：

```text
android-client/rk-club-android/dist/rk-club-debug.apk
```
