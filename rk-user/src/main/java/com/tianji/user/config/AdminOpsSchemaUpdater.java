package com.tianji.user.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminOpsSchemaUpdater {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureAdminOpsSchema() {
        ensureTable("admin_backup_record",
                "CREATE TABLE admin_backup_record (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT," +
                        "backup_name VARCHAR(255) NOT NULL," +
                        "database_scope VARCHAR(64) NOT NULL," +
                        "database_list TEXT," +
                        "file_name VARCHAR(255) DEFAULT NULL," +
                        "remote_path VARCHAR(512) DEFAULT NULL," +
                        "file_size_bytes BIGINT DEFAULT 0," +
                        "status VARCHAR(32) NOT NULL," +
                        "backup_type VARCHAR(32) NOT NULL," +
                        "note_text VARCHAR(255) DEFAULT NULL," +
                        "summary_json LONGTEXT," +
                        "created_by BIGINT DEFAULT NULL," +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "is_deleted TINYINT(1) NOT NULL DEFAULT 0," +
                        "PRIMARY KEY (id)," +
                        "KEY idx_admin_backup_record_status (status)," +
                        "KEY idx_admin_backup_record_create_time (create_time)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='admin backup record table'");

        ensureTable("admin_backup_schedule",
                "CREATE TABLE admin_backup_schedule (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT," +
                        "enabled TINYINT(1) NOT NULL DEFAULT 0," +
                        "frequency VARCHAR(32) NOT NULL DEFAULT 'daily'," +
                        "backup_time VARCHAR(16) NOT NULL DEFAULT '02:00'," +
                        "retention_days INT NOT NULL DEFAULT 30," +
                        "database_list TEXT," +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "PRIMARY KEY (id)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='admin backup schedule table'");

        ensureTable("ops_deploy_package_record",
                "CREATE TABLE ops_deploy_package_record (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT," +
                        "package_name VARCHAR(255) NOT NULL," +
                        "status VARCHAR(32) NOT NULL DEFAULT 'queued'," +
                        "progress INT NOT NULL DEFAULT 0," +
                        "package_mode VARCHAR(32) NOT NULL DEFAULT 'offline-full'," +
                        "delivery_mode VARCHAR(32) NOT NULL DEFAULT 'direct-download'," +
                        "image_artifact_mode VARCHAR(32) NOT NULL DEFAULT 'both'," +
                        "deploy_mode VARCHAR(32) NOT NULL DEFAULT 'package'," +
                        "remote_deploy TINYINT(1) NOT NULL DEFAULT 0," +
                        "remote_cluster_name VARCHAR(128) DEFAULT NULL," +
                        "deploy_started_at DATETIME DEFAULT NULL," +
                        "deploy_finished_at DATETIME DEFAULT NULL," +
                        "current_step VARCHAR(128) DEFAULT NULL," +
                        "current_image VARCHAR(255) DEFAULT NULL," +
                        "uploaded_images INT NOT NULL DEFAULT 0," +
                        "total_images INT NOT NULL DEFAULT 0," +
                        "upload_percent INT NOT NULL DEFAULT 0," +
                        "registry_pull_package TINYINT(1) NOT NULL DEFAULT 0," +
                        "streaming_migration TINYINT(1) NOT NULL DEFAULT 0," +
                        "global_migration_lock TINYINT(1) NOT NULL DEFAULT 0," +
                        "estimated_remaining_seconds INT DEFAULT 0," +
                        "migration_started_at DATETIME DEFAULT NULL," +
                        "migration_updated_at DATETIME DEFAULT NULL," +
                        "file_name VARCHAR(255) DEFAULT NULL," +
                        "remote_path VARCHAR(512) DEFAULT NULL," +
                        "file_size_bytes BIGINT DEFAULT 0," +
                        "delete_after_download TINYINT(1) NOT NULL DEFAULT 1," +
                        "downloaded TINYINT(1) NOT NULL DEFAULT 0," +
                        "downloaded_at DATETIME DEFAULT NULL," +
                        "kubeconfig_ciphertext LONGTEXT," +
                        "kubeconfig_redacted LONGTEXT," +
                        "kubeconfig_fingerprint VARCHAR(128) DEFAULT NULL," +
                        "last_diagnosis_json LONGTEXT," +
                        "last_repair_json LONGTEXT," +
                        "note_text VARCHAR(255) DEFAULT NULL," +
                        "request_json LONGTEXT," +
                        "logs LONGTEXT," +
                        "created_by BIGINT DEFAULT NULL," +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "is_deleted TINYINT(1) NOT NULL DEFAULT 0," +
                        "PRIMARY KEY (id)," +
                        "KEY idx_ops_deploy_package_status (status, create_time)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ops deploy package record table'");

        ensureTable("ops_build_record",
                "CREATE TABLE ops_build_record (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT," +
                        "job_name VARCHAR(128) NOT NULL," +
                        "build_number INT DEFAULT NULL," +
                        "build_url VARCHAR(512) DEFAULT NULL," +
                        "queue_id VARCHAR(64) DEFAULT NULL," +
                        "git_source VARCHAR(32) DEFAULT NULL," +
                        "git_repo VARCHAR(512) DEFAULT NULL," +
                        "branch_name VARCHAR(128) DEFAULT NULL," +
                        "commit_id VARCHAR(64) DEFAULT NULL," +
                        "image_mode VARCHAR(32) DEFAULT NULL," +
                        "services_text VARCHAR(512) DEFAULT NULL," +
                        "status VARCHAR(32) NOT NULL," +
                        "stage VARCHAR(64) DEFAULT NULL," +
                        "result VARCHAR(64) DEFAULT NULL," +
                        "failure_reason TEXT," +
                        "trigger_user_id BIGINT DEFAULT NULL," +
                        "trigger_user_name VARCHAR(128) DEFAULT NULL," +
                        "trigger_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "start_time DATETIME DEFAULT NULL," +
                        "finish_time DATETIME DEFAULT NULL," +
                        "duration_ms BIGINT DEFAULT 0," +
                        "log_tail LONGTEXT," +
                        "preflight_summary TEXT," +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "is_deleted TINYINT(1) NOT NULL DEFAULT 0," +
                        "PRIMARY KEY (id)," +
                        "KEY idx_ops_build_record_queue (queue_id)," +
                        "KEY idx_ops_build_record_job_build (job_name, build_number)," +
                        "KEY idx_ops_build_record_time (trigger_time)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ops build record table'");

        ensureTable("ops_build_service",
                "CREATE TABLE ops_build_service (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT," +
                        "record_id BIGINT NOT NULL," +
                        "service_code VARCHAR(128) NOT NULL," +
                        "status VARCHAR(32) DEFAULT NULL," +
                        "image_name VARCHAR(255) DEFAULT NULL," +
                        "is_deleted TINYINT(1) NOT NULL DEFAULT 0," +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "PRIMARY KEY (id)," +
                        "KEY idx_ops_build_service_record (record_id)," +
                        "KEY idx_ops_build_service_code (service_code)," +
                        "KEY idx_ops_build_service_deleted (service_code, is_deleted, create_time)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ops build service table'");

        ensureTable("ops_build_log",
                "CREATE TABLE ops_build_log (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT," +
                        "record_id BIGINT NOT NULL," +
                        "service_code VARCHAR(128) DEFAULT NULL," +
                        "build_number INT DEFAULT NULL," +
                        "log_type VARCHAR(32) DEFAULT 'CONSOLE'," +
                        "content LONGTEXT," +
                        "line_from INT DEFAULT 0," +
                        "line_to INT DEFAULT 0," +
                        "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "PRIMARY KEY (id)," +
                        "KEY idx_ops_build_log_record (record_id)," +
                        "KEY idx_ops_build_log_service (service_code)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ops build log table'");

        ensureTable("ops_service_registry",
                "CREATE TABLE ops_service_registry (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT," +
                        "service_code VARCHAR(128) NOT NULL," +
                        "display_name VARCHAR(128) NOT NULL," +
                        "service_type VARCHAR(32) NOT NULL DEFAULT 'backend'," +
                        "git_source VARCHAR(32) NOT NULL DEFAULT 'local'," +
                        "git_repo VARCHAR(512) DEFAULT NULL," +
                        "branch_name VARCHAR(128) DEFAULT 'cloud-master-new'," +
                        "module_path VARCHAR(255) DEFAULT NULL," +
                        "build_mode VARCHAR(64) DEFAULT 'maven-docker'," +
                        "image_name VARCHAR(255) DEFAULT NULL," +
                        "namespace_name VARCHAR(128) DEFAULT 'shetuanguanlixitong'," +
                        "workload_type VARCHAR(32) DEFAULT 'StatefulSet'," +
                        "workload_name VARCHAR(128) DEFAULT NULL," +
                        "container_name VARCHAR(128) DEFAULT NULL," +
                        "health_check_path VARCHAR(255) DEFAULT NULL," +
                        "resource_limits VARCHAR(255) DEFAULT NULL," +
                        "enabled TINYINT(1) NOT NULL DEFAULT 1," +
                        "sort_order INT NOT NULL DEFAULT 100," +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "is_deleted TINYINT(1) NOT NULL DEFAULT 0," +
                        "PRIMARY KEY (id)," +
                        "UNIQUE KEY uk_ops_service_registry_code (service_code)," +
                        "KEY idx_ops_service_registry_enabled (enabled, sort_order)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ops service registry table'");

        ensureTable("ops_operation_audit",
                "CREATE TABLE ops_operation_audit (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT," +
                        "operation_type VARCHAR(64) NOT NULL," +
                        "target_namespace VARCHAR(128) DEFAULT NULL," +
                        "target_kind VARCHAR(64) DEFAULT NULL," +
                        "target_name VARCHAR(255) DEFAULT NULL," +
                        "action VARCHAR(64) DEFAULT NULL," +
                        "status VARCHAR(32) DEFAULT NULL," +
                        "reason VARCHAR(512) DEFAULT NULL," +
                        "request_payload LONGTEXT," +
                        "result_output LONGTEXT," +
                        "operator_user_id BIGINT DEFAULT NULL," +
                        "operator_user_name VARCHAR(128) DEFAULT NULL," +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "PRIMARY KEY (id)," +
                        "KEY idx_ops_operation_audit_type_time (operation_type, create_time)," +
                        "KEY idx_ops_operation_audit_target (target_namespace, target_kind, target_name)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ops operation audit table'");

        ensureTable("ops_release_version",
                "CREATE TABLE ops_release_version (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT," +
                        "service_code VARCHAR(128) NOT NULL," +
                        "version_tag VARCHAR(128) NOT NULL," +
                        "source_image VARCHAR(512) DEFAULT NULL," +
                        "registry_image VARCHAR(512) NOT NULL," +
                        "source_type VARCHAR(64) DEFAULT NULL," +
                        "git_source VARCHAR(32) DEFAULT NULL," +
                        "branch_name VARCHAR(128) DEFAULT NULL," +
                        "commit_id VARCHAR(64) DEFAULT NULL," +
                        "jenkins_record_id BIGINT DEFAULT NULL," +
                        "build_number INT DEFAULT NULL," +
                        "metadata_json LONGTEXT," +
                        "status VARCHAR(32) NOT NULL DEFAULT 'success'," +
                        "created_by BIGINT DEFAULT NULL," +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "is_deleted TINYINT(1) NOT NULL DEFAULT 0," +
                        "PRIMARY KEY (id)," +
                        "UNIQUE KEY uk_ops_release_version_service_tag (service_code, version_tag, is_deleted)," +
                        "KEY idx_ops_release_version_service_time (service_code, create_time)," +
                        "KEY idx_ops_release_version_status (status, create_time)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ops release version table'");

        ensureTable("ops_release_task",
                "CREATE TABLE ops_release_task (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT," +
                        "task_type VARCHAR(64) NOT NULL," +
                        "service_code VARCHAR(128) NOT NULL," +
                        "target_version_id BIGINT DEFAULT NULL," +
                        "runtime_mode VARCHAR(32) DEFAULT NULL," +
                        "status VARCHAR(32) NOT NULL DEFAULT 'queued'," +
                        "current_step VARCHAR(128) DEFAULT NULL," +
                        "progress INT NOT NULL DEFAULT 0," +
                        "logs LONGTEXT," +
                        "created_by BIGINT DEFAULT NULL," +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "is_deleted TINYINT(1) NOT NULL DEFAULT 0," +
                        "PRIMARY KEY (id)," +
                        "KEY idx_ops_release_task_service_time (service_code, create_time)," +
                        "KEY idx_ops_release_task_status (status, create_time)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ops release task table'");

        ensureTable("ops_release_deployment",
                "CREATE TABLE ops_release_deployment (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT," +
                        "service_code VARCHAR(128) NOT NULL," +
                        "version_id BIGINT DEFAULT NULL," +
                        "runtime_mode VARCHAR(32) DEFAULT NULL," +
                        "namespace_name VARCHAR(128) DEFAULT NULL," +
                        "workload_type VARCHAR(32) DEFAULT NULL," +
                        "workload_name VARCHAR(128) DEFAULT NULL," +
                        "container_name VARCHAR(128) DEFAULT NULL," +
                        "previous_image VARCHAR(512) DEFAULT NULL," +
                        "target_image VARCHAR(512) NOT NULL," +
                        "status VARCHAR(32) NOT NULL DEFAULT 'success'," +
                        "reason VARCHAR(512) DEFAULT NULL," +
                        "created_by BIGINT DEFAULT NULL," +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "is_deleted TINYINT(1) NOT NULL DEFAULT 0," +
                        "PRIMARY KEY (id)," +
                        "KEY idx_ops_release_deployment_service_time (service_code, create_time)," +
                        "KEY idx_ops_release_deployment_workload (namespace_name, workload_type, workload_name)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ops release deployment table'");

        ensureTable("ops_release_update_package",
                "CREATE TABLE ops_release_update_package (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT," +
                        "update_version VARCHAR(128) NOT NULL," +
                        "channel VARCHAR(32) NOT NULL DEFAULT 'stable'," +
                        "runtime_mode VARCHAR(32) NOT NULL DEFAULT 'k3s'," +
                        "manifest_json LONGTEXT NOT NULL," +
                        "status VARCHAR(32) NOT NULL DEFAULT 'draft'," +
                        "created_by BIGINT DEFAULT NULL," +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "is_deleted TINYINT(1) NOT NULL DEFAULT 0," +
                        "PRIMARY KEY (id)," +
                        "KEY idx_ops_release_update_package_channel_time (channel, create_time)," +
                        "KEY idx_ops_release_update_package_version (update_version)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ops release update package table'");

        ensureTable("mobile_release_build_record",
                "CREATE TABLE mobile_release_build_record (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT," +
                        "status VARCHAR(32) NOT NULL DEFAULT 'queued'," +
                        "version_name VARCHAR(64) DEFAULT NULL," +
                        "version_code INT DEFAULT 0," +
                        "min_supported_version_code INT DEFAULT 0," +
                        "force_upgrade TINYINT(1) NOT NULL DEFAULT 0," +
                        "download_url VARCHAR(1024) DEFAULT NULL," +
                        "apk_path VARCHAR(512) DEFAULT NULL," +
                        "file_size_bytes BIGINT DEFAULT 0," +
                        "release_notes LONGTEXT," +
                        "release_source VARCHAR(64) DEFAULT 'auto-build'," +
                        "git_commit VARCHAR(64) DEFAULT NULL," +
                        "git_range VARCHAR(128) DEFAULT NULL," +
                        "build_log LONGTEXT," +
                        "started_at DATETIME DEFAULT NULL," +
                        "finished_at DATETIME DEFAULT NULL," +
                        "duration_ms BIGINT DEFAULT 0," +
                        "created_by BIGINT DEFAULT NULL," +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "is_deleted TINYINT(1) NOT NULL DEFAULT 0," +
                        "PRIMARY KEY (id)," +
                        "KEY idx_mobile_release_build_status (status, create_time)," +
                        "KEY idx_mobile_release_build_commit (git_commit)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='mobile apk build record table'");

        ensureTable("mobile_release_build_log",
                "CREATE TABLE mobile_release_build_log (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT," +
                        "build_id BIGINT NOT NULL," +
                        "line_no INT NOT NULL DEFAULT 0," +
                        "log_type VARCHAR(32) DEFAULT 'CONSOLE'," +
                        "content LONGTEXT," +
                        "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "PRIMARY KEY (id)," +
                        "KEY idx_mobile_release_build_log_build (build_id, line_no)," +
                        "KEY idx_mobile_release_build_log_time (created_at)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='mobile apk build log table'");

        ensureOpsBuildRecordColumns();
        ensureDeployPackageColumns();
        ensureOpsServiceRegistryColumns();
        ensureOpsOperationAuditColumns();
        ensureReleaseCenterColumns();
        ensureMobileReleaseBuildColumns();
        ensureDefaultServiceRegistry();
        closeStaleRunningDeployPackages();
    }

    private void ensureTable(String tableName, String ddl) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                Integer.class,
                tableName
        );
        if (count != null && count > 0) {
            return;
        }
        log.info("{} table missing, creating automatically", tableName);
        jdbcTemplate.execute(ddl);
    }

    private void ensureColumn(String tableName, String columnName, String ddl) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                tableName,
                columnName
        );
        if (count != null && count > 0) {
            return;
        }
        log.info("{}.{} column missing, adding automatically", tableName, columnName);
        jdbcTemplate.execute(ddl);
    }

    private void ensureIndex(String tableName, String indexName, String ddl) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?",
                Integer.class,
                tableName,
                indexName
        );
        if (count != null && count > 0) {
            return;
        }
        log.info("{}.{} index missing, creating automatically", tableName, indexName);
        jdbcTemplate.execute(ddl);
    }

    private void ensureOpsBuildRecordColumns() {
        ensureColumn("ops_build_record", "build_url",
                "ALTER TABLE ops_build_record ADD COLUMN build_url VARCHAR(512) DEFAULT NULL AFTER build_number");
        ensureColumn("ops_build_record", "commit_id",
                "ALTER TABLE ops_build_record ADD COLUMN commit_id VARCHAR(64) DEFAULT NULL AFTER branch_name");
        ensureColumn("ops_build_record", "log_tail",
                "ALTER TABLE ops_build_record ADD COLUMN log_tail LONGTEXT AFTER duration_ms");
        ensureColumn("ops_build_record", "preflight_summary",
                "ALTER TABLE ops_build_record ADD COLUMN preflight_summary TEXT AFTER log_tail");
        ensureColumn("ops_build_service", "is_deleted",
                "ALTER TABLE ops_build_service ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0 AFTER image_name");
        ensureIndex("ops_build_service", "idx_ops_build_service_deleted",
                "CREATE INDEX idx_ops_build_service_deleted ON ops_build_service (service_code, is_deleted, create_time)");
    }

    private void ensureDeployPackageColumns() {
        ensureColumn("ops_deploy_package_record", "package_mode",
                "ALTER TABLE ops_deploy_package_record ADD COLUMN package_mode VARCHAR(32) NOT NULL DEFAULT 'offline-full' AFTER progress");
        ensureColumn("ops_deploy_package_record", "delivery_mode",
                "ALTER TABLE ops_deploy_package_record ADD COLUMN delivery_mode VARCHAR(32) NOT NULL DEFAULT 'direct-download' AFTER package_mode");
        ensureColumn("ops_deploy_package_record", "image_artifact_mode",
                "ALTER TABLE ops_deploy_package_record ADD COLUMN image_artifact_mode VARCHAR(32) NOT NULL DEFAULT 'both' AFTER delivery_mode");
        ensureColumn("ops_deploy_package_record", "deploy_mode",
                "ALTER TABLE ops_deploy_package_record ADD COLUMN deploy_mode VARCHAR(32) NOT NULL DEFAULT 'package' AFTER image_artifact_mode");
        ensureColumn("ops_deploy_package_record", "remote_deploy",
                "ALTER TABLE ops_deploy_package_record ADD COLUMN remote_deploy TINYINT(1) NOT NULL DEFAULT 0 AFTER deploy_mode");
        ensureColumn("ops_deploy_package_record", "remote_cluster_name",
                "ALTER TABLE ops_deploy_package_record ADD COLUMN remote_cluster_name VARCHAR(128) DEFAULT NULL AFTER remote_deploy");
        ensureColumn("ops_deploy_package_record", "deploy_started_at",
                "ALTER TABLE ops_deploy_package_record ADD COLUMN deploy_started_at DATETIME DEFAULT NULL AFTER remote_cluster_name");
        ensureColumn("ops_deploy_package_record", "deploy_finished_at",
                "ALTER TABLE ops_deploy_package_record ADD COLUMN deploy_finished_at DATETIME DEFAULT NULL AFTER deploy_started_at");
        ensureColumn("ops_deploy_package_record", "current_step",
                "ALTER TABLE ops_deploy_package_record ADD COLUMN current_step VARCHAR(128) DEFAULT NULL AFTER image_artifact_mode");
        ensureColumn("ops_deploy_package_record", "current_image",
                "ALTER TABLE ops_deploy_package_record ADD COLUMN current_image VARCHAR(255) DEFAULT NULL AFTER current_step");
        ensureColumn("ops_deploy_package_record", "uploaded_images",
                "ALTER TABLE ops_deploy_package_record ADD COLUMN uploaded_images INT NOT NULL DEFAULT 0 AFTER current_image");
        ensureColumn("ops_deploy_package_record", "total_images",
                "ALTER TABLE ops_deploy_package_record ADD COLUMN total_images INT NOT NULL DEFAULT 0 AFTER uploaded_images");
        ensureColumn("ops_deploy_package_record", "upload_percent",
                "ALTER TABLE ops_deploy_package_record ADD COLUMN upload_percent INT NOT NULL DEFAULT 0 AFTER total_images");
        ensureColumn("ops_deploy_package_record", "registry_pull_package",
                "ALTER TABLE ops_deploy_package_record ADD COLUMN registry_pull_package TINYINT(1) NOT NULL DEFAULT 0 AFTER upload_percent");
        ensureColumn("ops_deploy_package_record", "streaming_migration",
                "ALTER TABLE ops_deploy_package_record ADD COLUMN streaming_migration TINYINT(1) NOT NULL DEFAULT 0 AFTER registry_pull_package");
        ensureColumn("ops_deploy_package_record", "global_migration_lock",
                "ALTER TABLE ops_deploy_package_record ADD COLUMN global_migration_lock TINYINT(1) NOT NULL DEFAULT 0 AFTER streaming_migration");
        ensureColumn("ops_deploy_package_record", "estimated_remaining_seconds",
                "ALTER TABLE ops_deploy_package_record ADD COLUMN estimated_remaining_seconds INT DEFAULT 0 AFTER global_migration_lock");
        ensureColumn("ops_deploy_package_record", "migration_started_at",
                "ALTER TABLE ops_deploy_package_record ADD COLUMN migration_started_at DATETIME DEFAULT NULL AFTER estimated_remaining_seconds");
        ensureColumn("ops_deploy_package_record", "migration_updated_at",
                "ALTER TABLE ops_deploy_package_record ADD COLUMN migration_updated_at DATETIME DEFAULT NULL AFTER migration_started_at");
        ensureColumn("ops_deploy_package_record", "delete_after_download",
                "ALTER TABLE ops_deploy_package_record ADD COLUMN delete_after_download TINYINT(1) NOT NULL DEFAULT 1 AFTER file_size_bytes");
        ensureColumn("ops_deploy_package_record", "downloaded",
                "ALTER TABLE ops_deploy_package_record ADD COLUMN downloaded TINYINT(1) NOT NULL DEFAULT 0 AFTER delete_after_download");
        ensureColumn("ops_deploy_package_record", "downloaded_at",
                "ALTER TABLE ops_deploy_package_record ADD COLUMN downloaded_at DATETIME DEFAULT NULL AFTER downloaded");
        ensureColumn("ops_deploy_package_record", "kubeconfig_ciphertext",
                "ALTER TABLE ops_deploy_package_record ADD COLUMN kubeconfig_ciphertext LONGTEXT AFTER downloaded_at");
        ensureColumn("ops_deploy_package_record", "kubeconfig_redacted",
                "ALTER TABLE ops_deploy_package_record ADD COLUMN kubeconfig_redacted LONGTEXT AFTER kubeconfig_ciphertext");
        ensureColumn("ops_deploy_package_record", "kubeconfig_fingerprint",
                "ALTER TABLE ops_deploy_package_record ADD COLUMN kubeconfig_fingerprint VARCHAR(128) DEFAULT NULL AFTER kubeconfig_redacted");
        ensureColumn("ops_deploy_package_record", "last_diagnosis_json",
                "ALTER TABLE ops_deploy_package_record ADD COLUMN last_diagnosis_json LONGTEXT AFTER kubeconfig_fingerprint");
        ensureColumn("ops_deploy_package_record", "last_repair_json",
                "ALTER TABLE ops_deploy_package_record ADD COLUMN last_repair_json LONGTEXT AFTER last_diagnosis_json");
    }

    private void closeStaleRunningDeployPackages() {
        String interruptedLine = "\ninterrupted by service restart; stale RUNNING record closed automatically\n";
        int updated = jdbcTemplate.update(
                "UPDATE ops_deploy_package_record SET status = 'FAILED', progress = 100, current_step = 'interrupted', " +
                        "logs = CONCAT(COALESCE(logs,''), ?), update_time = NOW() " +
                        "WHERE is_deleted = 0 AND status = 'RUNNING'",
                interruptedLine
        );
        if (updated > 0) {
            log.warn("closed {} stale RUNNING deploy package record(s) after service startup", updated);
        }
    }

    private void ensureOpsServiceRegistryColumns() {
        ensureColumn("ops_service_registry", "workload_type",
                "ALTER TABLE ops_service_registry ADD COLUMN workload_type VARCHAR(32) DEFAULT 'StatefulSet' AFTER namespace_name");
        ensureColumn("ops_service_registry", "resource_limits",
                "ALTER TABLE ops_service_registry ADD COLUMN resource_limits VARCHAR(255) DEFAULT NULL AFTER health_check_path");
        ensureColumn("ops_service_registry", "sort_order",
                "ALTER TABLE ops_service_registry ADD COLUMN sort_order INT NOT NULL DEFAULT 100 AFTER enabled");
    }

    private void ensureOpsOperationAuditColumns() {
        ensureColumn("ops_operation_audit", "reason",
                "ALTER TABLE ops_operation_audit ADD COLUMN reason VARCHAR(512) DEFAULT NULL AFTER status");
        ensureColumn("ops_operation_audit", "request_payload",
                "ALTER TABLE ops_operation_audit ADD COLUMN request_payload LONGTEXT AFTER reason");
        ensureColumn("ops_operation_audit", "result_output",
                "ALTER TABLE ops_operation_audit ADD COLUMN result_output LONGTEXT AFTER request_payload");
    }

    private void ensureReleaseCenterColumns() {
        ensureColumn("ops_release_version", "version_tag",
                "ALTER TABLE ops_release_version ADD COLUMN version_tag VARCHAR(128) NOT NULL DEFAULT '' AFTER service_code");
        ensureColumn("ops_release_version", "registry_image",
                "ALTER TABLE ops_release_version ADD COLUMN registry_image VARCHAR(512) NOT NULL DEFAULT '' AFTER source_image");
        ensureColumn("ops_release_version", "metadata_json",
                "ALTER TABLE ops_release_version ADD COLUMN metadata_json LONGTEXT AFTER build_number");
        ensureColumn("ops_release_task", "target_version_id",
                "ALTER TABLE ops_release_task ADD COLUMN target_version_id BIGINT DEFAULT NULL AFTER service_code");
        ensureColumn("ops_release_task", "runtime_mode",
                "ALTER TABLE ops_release_task ADD COLUMN runtime_mode VARCHAR(32) DEFAULT NULL AFTER target_version_id");
        ensureColumn("ops_release_task", "logs",
                "ALTER TABLE ops_release_task ADD COLUMN logs LONGTEXT AFTER progress");
        ensureColumn("ops_release_deployment", "runtime_mode",
                "ALTER TABLE ops_release_deployment ADD COLUMN runtime_mode VARCHAR(32) DEFAULT NULL AFTER version_id");
        ensureColumn("ops_release_deployment", "previous_image",
                "ALTER TABLE ops_release_deployment ADD COLUMN previous_image VARCHAR(512) DEFAULT NULL AFTER container_name");
        ensureColumn("ops_release_deployment", "target_image",
                "ALTER TABLE ops_release_deployment ADD COLUMN target_image VARCHAR(512) NOT NULL DEFAULT '' AFTER previous_image");
        ensureColumn("ops_release_update_package", "channel",
                "ALTER TABLE ops_release_update_package ADD COLUMN channel VARCHAR(32) NOT NULL DEFAULT 'stable' AFTER update_version");
        ensureColumn("ops_release_update_package", "runtime_mode",
                "ALTER TABLE ops_release_update_package ADD COLUMN runtime_mode VARCHAR(32) NOT NULL DEFAULT 'k3s' AFTER channel");
        ensureColumn("ops_release_update_package", "manifest_json",
                "ALTER TABLE ops_release_update_package ADD COLUMN manifest_json LONGTEXT NOT NULL AFTER runtime_mode");
        ensureColumn("ops_release_update_package", "status",
                "ALTER TABLE ops_release_update_package ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'draft' AFTER manifest_json");
        ensureIndex("ops_release_update_package", "idx_ops_release_update_package_channel_time",
                "CREATE INDEX idx_ops_release_update_package_channel_time ON ops_release_update_package (channel, create_time)");
        ensureIndex("ops_release_update_package", "idx_ops_release_update_package_version",
                "CREATE INDEX idx_ops_release_update_package_version ON ops_release_update_package (update_version)");
        ensureIndex("ops_release_version", "idx_ops_release_version_service_time",
                "CREATE INDEX idx_ops_release_version_service_time ON ops_release_version (service_code, create_time)");
        ensureIndex("ops_release_task", "idx_ops_release_task_service_time",
                "CREATE INDEX idx_ops_release_task_service_time ON ops_release_task (service_code, create_time)");
        ensureIndex("ops_release_deployment", "idx_ops_release_deployment_service_time",
                "CREATE INDEX idx_ops_release_deployment_service_time ON ops_release_deployment (service_code, create_time)");
    }

    private void ensureMobileReleaseBuildColumns() {
        ensureColumn("mobile_release_build_record", "download_url",
                "ALTER TABLE mobile_release_build_record ADD COLUMN download_url VARCHAR(1024) DEFAULT NULL AFTER force_upgrade");
        ensureColumn("mobile_release_build_record", "git_commit",
                "ALTER TABLE mobile_release_build_record ADD COLUMN git_commit VARCHAR(64) DEFAULT NULL AFTER release_source");
        ensureColumn("mobile_release_build_record", "git_range",
                "ALTER TABLE mobile_release_build_record ADD COLUMN git_range VARCHAR(128) DEFAULT NULL AFTER git_commit");
        ensureColumn("mobile_release_build_record", "build_log",
                "ALTER TABLE mobile_release_build_record ADD COLUMN build_log LONGTEXT AFTER git_range");
        ensureIndex("mobile_release_build_log", "idx_mobile_release_build_log_build",
                "CREATE INDEX idx_mobile_release_build_log_build ON mobile_release_build_log (build_id, line_no)");
    }

    private void ensureDefaultServiceRegistry() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ops_service_registry WHERE is_deleted = 0",
                Integer.class
        );
        if (count != null && count > 0) {
            return;
        }
        insertDefaultService("rk-auth", "璁よ瘉鏈嶅姟", "backend", "rk-auth/rk-auth-service", "rk-server-rk-auth", "rk-auth", 10);
        insertDefaultService("rk-user", "鐢ㄦ埛鏈嶅姟", "backend", "rk-user", "rk-server-rk-user", "rk-user", 20);
        insertDefaultService("rk-file", "鏂囦欢鏈嶅姟", "backend", "rk-file", "rk-server-rk-file", "rk-file", 30);
        insertDefaultService("rk-message", "娑堟伅鏈嶅姟", "backend", "rk-message/rk-message-service", "rk-server-rk-message", "rk-message", 40);
        insertDefaultService("rk-content", "鍐呭鏈嶅姟", "backend", "rk-content", "rk-server-rk-content", "rk-content", 50);
        insertDefaultService("rk-activity", "娲诲姩鏈嶅姟", "backend", "rk-activity", "rk-server-rk-activity", "rk-activity", 60);
        insertDefaultService("rk-gateway", "缃戝叧鏈嶅姟", "backend", "rk-gateway", "rk-server-rk-gateway", "rk-gateway", 70);
        insertDefaultService("frontend", "鍓嶇绔欑偣", "frontend", "newpro/rk", "rk-server-rk-web-frontend", "frontend", 80);
    }

    private void insertDefaultService(String code, String name, String type, String modulePath,
                                      String workloadName, String containerName, int sortOrder) {
        jdbcTemplate.update(
                "INSERT INTO ops_service_registry " +
                        "(service_code, display_name, service_type, git_source, git_repo, branch_name, module_path, build_mode, image_name, namespace_name, workload_type, workload_name, container_name, health_check_path, resource_limits, enabled, sort_order) " +
                        "VALUES (?, ?, ?, 'local', 'https://github.com/example/rk-web.git', 'cloud-master-new', ?, ?, ?, 'shetuanguanlixitong', 'StatefulSet', ?, ?, ?, ?, 1, ?)",
                code,
                name,
                type,
                modulePath,
                "frontend".equals(code) ? "npm-docker" : "maven-docker",
                "rk-web/" + code,
                workloadName,
                containerName,
                "frontend".equals(code) ? "/" : "/actuator/health",
                "512Mi",
                sortOrder
        );
    }
}
