package com.tianji.user.service.impl;

import com.tianji.user.domain.dto.adminops.AdminReleaseDeploymentContext;
import com.tianji.user.domain.vo.adminops.AdminReleaseDeploymentResult;
import com.tianji.user.service.IAdminReleaseDeploymentExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
public class AdminReleaseComposeDeploymentExecutor implements IAdminReleaseDeploymentExecutor {

    private static final Pattern COMPOSE_SERVICE_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9_.-]{0,127}$");
    private static final String DOCKER_COMPOSE_MARKER = "docker compose";

    @Value("${rk.ops.release.compose-workdir:}")
    private String composeWorkdir;

    @Override
    public boolean supports(String runtimeMode) {
        return "docker-compose".equalsIgnoreCase(runtimeMode);
    }

    @Override
    public AdminReleaseDeploymentResult deploy(AdminReleaseDeploymentContext context) {
        String composeProjectName = firstText(context.getComposeProjectName(), "rk-web");
        String composeServiceName = firstText(context.getComposeServiceName(), context.getServiceCode());
        String composeFile = firstText(context.getComposeFile(), "docker-compose.yaml");
        if (!COMPOSE_SERVICE_PATTERN.matcher(composeServiceName).matches()) {
            return AdminReleaseDeploymentResult.failed("compose-rollout", 0, "compose service name is invalid\n");
        }

        Path workDir = resolveWorkDir(composeFile);
        Path overrideFile = workDir.resolve(".rk-release-" + composeServiceName + ".override.yaml");
        String composeFileArg = resolveComposeFileArg(composeFile, workDir);
        String overrideFileArg = overrideFile.toString();
        List<String> pullCommand = composeCommand(composeProjectName, composeFileArg, overrideFileArg, "pull", composeServiceName);
        List<String> upCommand = composeCommand(composeProjectName, composeFileArg, overrideFileArg, "up", "-d", "--no-deps", composeServiceName);

        StringBuilder logs = new StringBuilder();
        logs.append("release deploy runtime=docker-compose").append('\n');
        logs.append("release deploy command marker=").append(DOCKER_COMPOSE_MARKER).append('\n');
        logs.append("composeProjectName=").append(composeProjectName).append('\n');
        logs.append("composeServiceName=").append(composeServiceName).append('\n');
        logs.append("override image ").append(composeServiceName).append('=').append(context.getTargetImage()).append('\n');
        logs.append(commandText(pullCommand)).append('\n');
        logs.append(commandText(upCommand)).append('\n');

        if (Boolean.TRUE.equals(context.getDryRun())) {
            logs.append("dry-run only, command not executed").append('\n');
            return AdminReleaseDeploymentResult.success("compose-rollout", 100, logs.toString());
        }

        try {
            Files.createDirectories(workDir);
            Files.writeString(overrideFile, buildOverrideYaml(composeServiceName, context.getTargetImage()), StandardCharsets.UTF_8);
            int timeoutSeconds = Math.max(60, context.getRolloutTimeoutSeconds() == null ? 420 : context.getRolloutTimeoutSeconds() + 120);
            logs.append(runCommand(pullCommand, workDir, timeoutSeconds)).append('\n');
            logs.append(runCommand(upCommand, workDir, timeoutSeconds)).append('\n');
            return AdminReleaseDeploymentResult.success("compose-rollout", 100, logs.toString());
        } catch (Exception e) {
            logs.append("deploy failed: ").append(e.getMessage()).append('\n');
            return AdminReleaseDeploymentResult.failed("compose-rollout", 80, logs.toString());
        }
    }

    private List<String> composeCommand(String projectName, String composeFile, String overrideFile, String action, String... args) {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("compose");
        command.add("-p");
        command.add(projectName);
        command.add("-f");
        command.add(composeFile);
        command.add("-f");
        command.add(overrideFile);
        command.add(action);
        command.addAll(List.of(args));
        return command;
    }

    private String buildOverrideYaml(String serviceName, String targetImage) {
        return "services:\n"
                + "  " + serviceName + ":\n"
                + "    image: " + targetImage + "\n";
    }

    private String runCommand(List<String> command, Path workDir, int timeoutSeconds) {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workDir.toFile());
        builder.redirectErrorStream(true);
        try {
            Process process = builder.start();
            if (!process.waitFor(Math.max(5, timeoutSeconds), TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("command timed out");
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (InputStream inputStream = process.getInputStream()) {
                inputStream.transferTo(output);
            }
            String text = output.toString(StandardCharsets.UTF_8);
            if (process.exitValue() != 0) {
                throw new IllegalStateException(firstText(text, "exit code " + process.exitValue()));
            }
            return text.trim();
        } catch (Exception e) {
            throw new IllegalStateException("command failed: " + e.getMessage(), e);
        }
    }

    private Path resolveWorkDir(String composeFile) {
        if (StringUtils.hasText(composeWorkdir)) {
            return Path.of(composeWorkdir.trim()).toAbsolutePath().normalize();
        }
        Path composePath = Path.of(composeFile);
        if (composePath.isAbsolute() && composePath.getParent() != null) {
            return composePath.getParent().toAbsolutePath().normalize();
        }
        return Path.of(".").toAbsolutePath().normalize();
    }

    private String resolveComposeFileArg(String composeFile, Path workDir) {
        Path composePath = Path.of(composeFile);
        if (composePath.isAbsolute()) {
            return composePath.toString();
        }
        return workDir.resolve(composeFile).normalize().toString();
    }

    private String commandText(List<String> command) {
        return String.join(" ", command);
    }

    private String firstText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}
