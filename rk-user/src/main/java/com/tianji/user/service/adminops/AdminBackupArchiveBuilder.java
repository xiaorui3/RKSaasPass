package com.tianji.user.service.adminops;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

@Component
@RequiredArgsConstructor
public class AdminBackupArchiveBuilder {

    private static final DateTimeFormatter HEADER_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;

    public Map<String, List<String>> resolveTableSelections(List<String> databases, Map<String, List<String>> requestedTables) {
        if (databases == null || databases.isEmpty()) {
            return Map.of();
        }
        String placeholders = databases.stream().map(item -> "?").collect(Collectors.joining(","));
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT table_schema AS databaseName, table_name AS tableName " +
                        "FROM information_schema.tables " +
                        "WHERE table_schema IN (" + placeholders + ") AND table_type = 'BASE TABLE' " +
                        "ORDER BY table_schema, table_name",
                databases.toArray()
        );

        Map<String, List<String>> availableTables = new LinkedHashMap<>();
        for (String database : databases) {
            availableTables.put(database, new ArrayList<>());
        }
        for (Map<String, Object> row : rows) {
            String databaseName = String.valueOf(row.get("databaseName"));
            List<String> tableNames = availableTables.get(databaseName);
            if (tableNames != null) {
                tableNames.add(String.valueOf(row.get("tableName")));
            }
        }

        Map<String, List<String>> result = new LinkedHashMap<>();
        for (String database : databases) {
            List<String> available = availableTables.getOrDefault(database, List.of());
            List<String> requested = requestedTables == null ? null : requestedTables.get(database);
            if (requested == null || requested.isEmpty()) {
                result.put(database, new ArrayList<>(available));
                continue;
            }
            List<String> selected = requested.stream()
                    .filter(StringUtils::hasText)
                    .distinct()
                    .filter(available::contains)
                    .collect(Collectors.toList());
            if (selected.isEmpty()) {
                throw new IllegalArgumentException("database " + database + " has no matching tables to back up");
            }
            result.put(database, selected);
        }
        return result;
    }

    public byte[] buildArchive(List<String> databases, Map<String, List<String>> requestedTables) {
        Map<String, List<String>> tableSelections = resolveTableSelections(databases, requestedTables);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             GZIPOutputStream gzipOutputStream = new GZIPOutputStream(output);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(gzipOutputStream, StandardCharsets.UTF_8))) {
            writer.write("-- RK-Web backup generated at " + LocalDateTime.now().format(HEADER_TIME_FORMATTER));
            writer.newLine();
            writer.write("SET NAMES utf8mb4;");
            writer.newLine();
            writer.write("SET FOREIGN_KEY_CHECKS=0;");
            writer.newLine();
            writer.newLine();

            for (Map.Entry<String, List<String>> entry : tableSelections.entrySet()) {
                String database = entry.getKey();
                writer.write("CREATE DATABASE IF NOT EXISTS " + quoteIdentifier(database) + ";");
                writer.newLine();
                writer.write("USE " + quoteIdentifier(database) + ";");
                writer.newLine();
                writer.newLine();
                for (String tableName : entry.getValue()) {
                    writeTableDump(writer, database, tableName);
                }
            }

            writer.write("SET FOREIGN_KEY_CHECKS=1;");
            writer.newLine();
            writer.flush();
            gzipOutputStream.finish();
            return output.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("build backup archive failed", e);
        }
    }

    private void writeTableDump(BufferedWriter writer, String database, String tableName) throws Exception {
        String fullTableName = quoteIdentifier(database) + "." + quoteIdentifier(tableName);
        Map<String, Object> createTableRow = jdbcTemplate.queryForMap("SHOW CREATE TABLE " + fullTableName);
        writer.write("DROP TABLE IF EXISTS " + quoteIdentifier(tableName) + ";");
        writer.newLine();
        writer.write(extractCreateStatement(createTableRow) + ";");
        writer.newLine();

        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM " + fullTableName);
        if (!rows.isEmpty()) {
            List<String> columns = new ArrayList<>(rows.get(0).keySet());
            for (Map<String, Object> row : rows) {
                writer.write("INSERT INTO " + quoteIdentifier(tableName) + " (" +
                        columns.stream().map(this::quoteIdentifier).collect(Collectors.joining(", ")) +
                        ") VALUES (" +
                        columns.stream().map(column -> renderValue(row.get(column))).collect(Collectors.joining(", ")) +
                        ");");
                writer.newLine();
            }
        }
        writer.newLine();
    }

    private String extractCreateStatement(Map<String, Object> row) {
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getKey() != null && entry.getKey().toLowerCase(Locale.ROOT).startsWith("create ")) {
                return String.valueOf(entry.getValue());
            }
        }
        throw new IllegalStateException("missing SHOW CREATE TABLE output");
    }

    private String renderValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Number) {
            return String.valueOf(value);
        }
        if (value instanceof Boolean) {
            return Boolean.TRUE.equals(value) ? "1" : "0";
        }
        if (value instanceof byte[]) {
            return "X'" + toHex((byte[]) value) + "'";
        }
        if (value instanceof Timestamp || value instanceof Date || value instanceof Time) {
            return "'" + escapeSql(String.valueOf(value)) + "'";
        }
        return "'" + escapeSql(String.valueOf(value)) + "'";
    }

    private String quoteIdentifier(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }

    private String escapeSql(String value) {
        return value.replace("\\", "\\\\").replace("'", "''");
    }

    private String toHex(byte[] value) {
        StringBuilder builder = new StringBuilder(value.length * 2);
        for (byte item : value) {
            builder.append(String.format("%02X", item));
        }
        return builder.toString();
    }
}
