package com.tianji.user.service.impl;

import com.tianji.user.domain.vo.adminops.AdminReleaseRegistryPublishResult;
import com.tianji.user.domain.vo.adminops.AdminReleaseRegistryPublishResult.PublishProgress;
import com.tianji.user.service.IAdminReleaseRegistryPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

@Slf4j
@Service
public class AdminReleaseRegistryPublisher implements IAdminReleaseRegistryPublisher {

    private static final String DEFAULT_REGISTRY_SERVER = "registry.example.com";
    private static final Pattern SENSITIVE_LINE_PATTERN = Pattern.compile(
            "(?i)(password|passwd|token|secret|authorization|\\.dockerconfigjson)"
    );

    @Value("${rk.ops.release.registry-server:${rk.ops.deploy-package.registry-server:registry.example.com}}")
    private String configuredRegistryServer;

    @Value("${rk.ops.release.registry-username:${rk.ops.deploy-package.registry-username:${RK_REGISTRY_USER:}}}")
    private String registryUsername;

    @Value("${rk.ops.release.registry-password:${rk.ops.deploy-package.registry-password:${RK_REGISTRY_PASSWORD:}}}")
    private String registryPassword;

    @Value("${rk.ops.release.registry-timeout-seconds:1800}")
    private Integer registryTimeoutSeconds;

    @Override
    public AdminReleaseRegistryPublishResult publishCurrentImage(String serviceCode,
                                                                 String sourceImage,
                                                                 String targetImage,
                                                                 boolean dryRun) {
        StringBuilder logs = new StringBuilder();
        PublishProgress progress = new PublishProgress("registry-push", 0);
        String registryServer = resolveRegistryServer(targetImage);
        logs.append("__RK_RELEASE_REGISTRY_PUSH__ START service=").append(serviceCode).append('\n');
        logs.append("target registry server: ").append(registryServer).append('\n');

        if (dryRun) {
            progress.setProgress(100);
            logs.append("dry-run registry push: docker tag ")
                    .append(sourceImage)
                    .append(' ')
                    .append(targetImage)
                    .append(" && docker push ")
                    .append(targetImage)
                    .append('\n');
            logs.append("__RK_RELEASE_REGISTRY_PUSH__ COMPLETE 1 1").append('\n');
            return AdminReleaseRegistryPublishResult.success(progress.getCurrentStep(), progress.getProgress(), logs.toString());
        }

        try {
            boolean loggedIn = false;
            if (hasRegistryCredentials()) {
                progress.setProgress(20);
                logs.append("docker login ")
                        .append(registryServer)
                        .append(" --username ****** --password-stdin")
                        .append('\n');
                CommandResult login = runCommand(
                        List.of("docker", "login", registryServer, "--username", registryUsername.trim(), "--password-stdin"),
                        registryPassword,
                        timeoutSeconds()
                );
                appendCommandOutput(logs, login.output);
                if (login.exitCode != 0) {
                    return failed(progress, logs, "docker login failed");
                }
                loggedIn = true;
            } else {
                logs.append("registry credentials source: none, skip docker login").append('\n');
            }

            progress.setProgress(45);
            logs.append("docker tag ").append(sourceImage).append(' ').append(targetImage).append('\n');
            CommandResult tag = runCommand(List.of("docker", "tag", sourceImage, targetImage), null, timeoutSeconds());
            appendCommandOutput(logs, tag.output);
            if (tag.exitCode != 0) {
                return failed(progress, logs, "docker tag failed");
            }

            progress.setProgress(75);
            logs.append("docker push ").append(targetImage).append('\n');
            CommandResult push = runCommand(List.of("docker", "push", targetImage), null, timeoutSeconds());
            appendCommandOutput(logs, push.output);
            if (push.exitCode != 0) {
                return failed(progress, logs, "docker push failed");
            }
            logs.append("__RK_RELEASE_REGISTRY_PUSH__ DONE 1 1 ")
                    .append(sourceImage)
                    .append(' ')
                    .append(targetImage)
                    .append('\n');

            if (loggedIn) {
                CommandResult logout = runCommand(List.of("docker", "logout", registryServer), null, 60);
                if (logout.exitCode != 0) {
                    logs.append("docker logout warning: ").append(firstMeaningfulLine(logout.output)).append('\n');
                }
            }

            progress.setProgress(100);
            logs.append("__RK_RELEASE_REGISTRY_PUSH__ COMPLETE 1 1").append('\n');
            return AdminReleaseRegistryPublishResult.success(progress.getCurrentStep(), progress.getProgress(), redactSensitive(logs.toString()));
        } catch (Exception e) {
            log.warn("release registry image publish failed, serviceCode={}, targetImage={}", serviceCode, targetImage, e);
            return failed(progress, logs, "registry push exception: " + e.getMessage());
        }
    }

    private AdminReleaseRegistryPublishResult failed(PublishProgress progress, StringBuilder logs, String message) {
        logs.append("__RK_RELEASE_REGISTRY_PUSH__ FAIL ")
                .append(progress.getProgress() == null ? 0 : progress.getProgress())
                .append(" 100 ")
                .append(message)
                .append('\n');
        return AdminReleaseRegistryPublishResult.failed(progress.getCurrentStep(), progress.getProgress(), redactSensitive(logs.toString()));
    }

    private CommandResult runCommand(List<String> command, String stdin, int timeoutSeconds) {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        try {
            Process process = builder.start();
            StringBuilder output = new StringBuilder();
            AtomicReference<Exception> readerError = new AtomicReference<>();
            Thread readerThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append('\n');
                    }
                } catch (Exception e) {
                    readerError.set(e);
                }
            }, "rk-release-registry-push-log");
            readerThread.setDaemon(true);
            readerThread.start();

            writeStdin(process, stdin);
            if (!process.waitFor(Math.max(5, timeoutSeconds), TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return new CommandResult(124, "command timed out after " + timeoutSeconds + "s");
            }
            readerThread.join(5000L);
            if (readerError.get() != null) {
                output.append("read command output failed: ").append(readerError.get().getMessage()).append('\n');
            }
            return new CommandResult(process.exitValue(), output.toString());
        } catch (Exception e) {
            return new CommandResult(1, "command failed: " + e.getMessage());
        }
    }

    private void writeStdin(Process process, String stdin) {
        try (OutputStream outputStream = process.getOutputStream()) {
            if (stdin != null) {
                outputStream.write(stdin.getBytes(StandardCharsets.UTF_8));
                outputStream.write('\n');
                outputStream.flush();
            }
        } catch (Exception e) {
            throw new IllegalStateException("write command stdin failed: " + e.getMessage(), e);
        }
    }

    private void appendCommandOutput(StringBuilder logs, String output) {
        String safe = redactSensitive(output);
        if (StringUtils.hasText(safe)) {
            logs.append(safe.trim()).append('\n');
        }
    }

    private boolean hasRegistryCredentials() {
        return StringUtils.hasText(registryUsername) && StringUtils.hasText(registryPassword);
    }

    private int timeoutSeconds() {
        return Math.max(30, registryTimeoutSeconds == null ? 1800 : registryTimeoutSeconds);
    }

    private String resolveRegistryServer(String targetImage) {
        String imageServer = firstImageSegment(targetImage);
        if (StringUtils.hasText(imageServer)) {
            return normalizeRegistryServerKey(imageServer);
        }
        return normalizeRegistryServerKey(StringUtils.hasText(configuredRegistryServer) ? configuredRegistryServer : DEFAULT_REGISTRY_SERVER);
    }

    private String firstImageSegment(String image) {
        if (!StringUtils.hasText(image)) {
            return null;
        }
        String value = image.trim();
        int slash = value.indexOf('/');
        return slash > 0 ? value.substring(0, slash) : null;
    }

    String normalizeRegistryServerKey(String value) {
        String result = value == null ? "" : value.trim();
        if (result.toLowerCase(Locale.ROOT).startsWith("https://")) {
            result = result.substring("https://".length());
        } else if (result.toLowerCase(Locale.ROOT).startsWith("http://")) {
            result = result.substring("http://".length());
        }
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return StringUtils.hasText(result) ? result : DEFAULT_REGISTRY_SERVER;
    }

    String redactSensitive(String text) {
        if (text == null) {
            return "";
        }
        String safe = text;
        safe = safe.replaceAll("(?i)(--password(?:=|\\s+))\\S+", "$1******");
        safe = safe.replaceAll("(?i)(--username(?:=|\\s+))\\S+", "$1******");
        safe = safe.replaceAll("(?i)((?:password|passwd|token|secret|authorization)\\s*[=:]\\s*)\\S+", "$1******");
        safe = safe.replaceAll("(?i)((?:REGISTRY_PASSWORD|REGISTRY_USERNAME)\\s*[=:]\\s*)\\S+", "$1******");
        String password = StringUtils.hasText(registryPassword) ? registryPassword.trim() : null;
        if (StringUtils.hasText(password)) {
            safe = safe.replace(password, "******");
        }
        String username = StringUtils.hasText(registryUsername) ? registryUsername.trim() : null;
        if (StringUtils.hasText(username)) {
            safe = safe.replace(username, "******");
        }
        if (SENSITIVE_LINE_PATTERN.matcher(safe).find()) {
            safe = safe.replaceAll("(?im)(\\.dockerconfigjson\\s*[=:]\\s*).+$", "$1******");
        }
        return safe;
    }

    private String firstMeaningfulLine(String output) {
        if (!StringUtils.hasText(output)) {
            return "";
        }
        try (InputStream inputStream = new java.io.ByteArrayInputStream(output.getBytes(StandardCharsets.UTF_8));
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (StringUtils.hasText(line)) {
                    return redactSensitive(line.trim());
                }
            }
            return "";
        } catch (Exception e) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            return buffer.toString(StandardCharsets.UTF_8);
        }
    }

    private static final class CommandResult {
        private final int exitCode;
        private final String output;

        private CommandResult(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output == null ? "" : output;
        }
    }
}
