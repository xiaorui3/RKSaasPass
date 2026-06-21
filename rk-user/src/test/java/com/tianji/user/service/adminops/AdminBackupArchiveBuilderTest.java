package com.tianji.user.service.adminops;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminBackupArchiveBuilderTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private AdminBackupArchiveBuilder builder;

    @Test
    void buildArchive_shouldIncludeSelectedTableDefinitionsAndRows() throws Exception {
        when(jdbcTemplate.queryForList(
                "SELECT table_schema AS databaseName, table_name AS tableName " +
                        "FROM information_schema.tables " +
                        "WHERE table_schema IN (?) AND table_type = 'BASE TABLE' " +
                        "ORDER BY table_schema, table_name",
                "rk_user"
        )).thenReturn(List.of(Map.of("databaseName", "rk_user", "tableName", "sys_user")));

        when(jdbcTemplate.queryForMap("SHOW CREATE TABLE `rk_user`.`sys_user`"))
                .thenReturn(Map.of(
                        "Table", "sys_user",
                        "Create Table", "CREATE TABLE `sys_user` (`id` bigint NOT NULL, `nick_name` varchar(255) DEFAULT NULL, PRIMARY KEY (`id`))"
                ));

        LinkedHashMap<String, Object> row = new LinkedHashMap<>();
        row.put("id", 1L);
        row.put("nick_name", "O'Hara");
        when(jdbcTemplate.queryForList("SELECT * FROM `rk_user`.`sys_user`"))
                .thenReturn(List.of(row));

        byte[] archive = builder.buildArchive(List.of("rk_user"), Map.of("rk_user", List.of("sys_user")));

        String sql = unzip(archive);
        assertTrue(sql.contains("CREATE DATABASE IF NOT EXISTS `rk_user`;"));
        assertTrue(sql.contains("USE `rk_user`;"));
        assertTrue(sql.contains("CREATE TABLE `sys_user`"));
        assertTrue(sql.contains("INSERT INTO `sys_user` (`id`, `nick_name`) VALUES (1, 'O''Hara');"));
    }

    private String unzip(byte[] archive) throws Exception {
        try (Reader reader = new InputStreamReader(
                new GZIPInputStream(new ByteArrayInputStream(archive)),
                StandardCharsets.UTF_8
        ); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            char[] buffer = new char[1024];
            int len;
            while ((len = reader.read(buffer)) >= 0) {
                output.write(new String(buffer, 0, len).getBytes(StandardCharsets.UTF_8));
            }
            return output.toString(StandardCharsets.UTF_8);
        }
    }
}
