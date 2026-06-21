package com.tianji.user.service.impl;

import com.tianji.user.domain.dto.adminops.AdminReleaseDeploymentContext;
import com.tianji.user.domain.vo.adminops.AdminReleaseDeploymentResult;
import com.tianji.user.service.IAdminReleaseDeploymentExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Service
public class AdminReleaseK3sDeploymentExecutor implements IAdminReleaseDeploymentExecutor {

    private static final String K8S_STRATEGIC_MERGE_PATCH_CONTENT_TYPE = "application/strategic-merge-patch+json";
    private static final String KUBECTL_SET_IMAGE_MARKER = "kubectl set image";
    private static final String ROLLOUT_STATUS_MARKER = "rollout status";

    @Override
    public boolean supports(String runtimeMode) {
        return "k3s".equalsIgnoreCase(runtimeMode);
    }

    @Override
    public AdminReleaseDeploymentResult deploy(AdminReleaseDeploymentContext context) {
        String resource = context.getWorkloadResourceType() + "/" + context.getWorkloadName();
        int timeoutSeconds = Math.max(30, context.getRolloutTimeoutSeconds() == null ? 300 : context.getRolloutTimeoutSeconds());
        List<String> setImageCommand = List.of(
                "kubectl",
                "set",
                "image",
                resource,
                context.getContainerName() + "=" + context.getTargetImage(),
                "-n",
                context.getNamespace()
        );
        List<String> rolloutCommand = List.of(
                "kubectl",
                "rollout",
                "status",
                resource,
                "-n",
                context.getNamespace(),
                "--timeout=" + timeoutSeconds + "s"
        );
        StringBuilder logs = new StringBuilder();
        logs.append("release deploy runtime=k3s").append('\n');
        logs.append("release deploy content-type marker=").append(K8S_STRATEGIC_MERGE_PATCH_CONTENT_TYPE).append('\n');
        logs.append("release deploy command marker=").append(KUBECTL_SET_IMAGE_MARKER).append('\n');
        logs.append("release deploy rollout marker=").append(ROLLOUT_STATUS_MARKER).append('\n');
        logs.append(commandText(setImageCommand)).append('\n');
        logs.append(commandText(rolloutCommand)).append('\n');

        if (Boolean.TRUE.equals(context.getDryRun())) {
            logs.append("dry-run only, command not executed").append('\n');
            return AdminReleaseDeploymentResult.success("k3s-rollout", 100, logs.toString());
        }

        try {
            logs.append(runCommand(setImageCommand, timeoutSeconds + 60)).append('\n');
            logs.append(runCommand(rolloutCommand, timeoutSeconds + 120)).append('\n');
            return AdminReleaseDeploymentResult.success("k3s-rollout", 100, logs.toString());
        } catch (Exception e) {
            logs.append("deploy failed: ").append(e.getMessage()).append('\n');
            return AdminReleaseDeploymentResult.failed("k3s-rollout", 80, logs.toString());
        }
    }

    private String runCommand(List<String> command, int timeoutSeconds) {
        ProcessBuilder builder = new ProcessBuilder(command);
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

    private String commandText(List<String> command) {
        return String.join(" ", command);
    }

    private String firstText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}
