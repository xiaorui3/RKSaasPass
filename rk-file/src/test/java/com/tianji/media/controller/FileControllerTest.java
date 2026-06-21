package com.tianji.media.controller;

import com.tianji.common.autoconfigure.media.MediaPathHelper;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.file.domain.po.FileInfo;
import com.tianji.media.domain.dto.FileDTO;
import com.tianji.media.storage.IFileStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileControllerTest {

    @Mock
    private com.tianji.file.service.IFileService ledgerFileService;
    @Mock
    private IFileStorage fileStorage;
    @Mock
    private MediaPathHelper mediaPathHelper;

    @InjectMocks
    private FileController fileController;

    @Test
    void uploadFile_shouldRequireService() {
        MockMultipartFile file = new MockMultipartFile("file", "demo.png", "image/png", new byte[]{1, 2, 3});

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> fileController.uploadFile(file, " ", "avatar")
        );

        assertEquals("service is required", ex.getMessage());
        verify(fileStorage, never()).uploadFile(any(), any(), any(), any(Long.class), any());
    }

    @Test
    void uploadFile_shouldRequireBizType() {
        MockMultipartFile file = new MockMultipartFile("file", "demo.png", "image/png", new byte[]{1, 2, 3});

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> fileController.uploadFile(file, "rk-user", "")
        );

        assertEquals("bizType is required", ex.getMessage());
        verify(fileStorage, never()).uploadFile(any(), any(), any(), any(Long.class), any());
    }

    @Test
    void uploadFile_shouldReturnRelativePathAndPublicUrlWhenLedgerWriteSucceeds() {
        MockMultipartFile file = new MockMultipartFile("file", "demo.png", "image/png", new byte[]{1, 2, 3});
        when(fileStorage.uploadFile(eq("rk-user"), startsWith("avatar/"), any(), eq(3L), eq("image/png"))).thenReturn("req-1");
        FileInfo fileInfo = new FileInfo();
        fileInfo.setId(88L);
        when(ledgerFileService.saveUploadRecord(eq("demo.png"), eq("rk-user"), startsWith("avatar/"), eq("image/png"), eq(3L))).thenReturn(fileInfo);
        when(mediaPathHelper.toPublicUrl(startsWith("rk-user/avatar/"))).thenReturn("http://minio/rk-user/avatar/2026/04/23/demo.png");

        FileDTO dto = fileController.uploadFile(file, "rk-user", "avatar");

        assertNotNull(dto);
        assertEquals(88L, dto.getId());
        assertTrue(dto.getRelativePath().startsWith("rk-user/avatar/"));
        assertEquals(dto.getRelativePath(), dto.getPath());
        assertEquals("http://minio/rk-user/avatar/2026/04/23/demo.png", dto.getUrl());
        assertEquals("http://minio/rk-user/avatar/2026/04/23/demo.png", dto.getFileUrl());
        verify(ledgerFileService).assertUploadAllowed(3L);
        verify(ledgerFileService).saveUploadRecord(eq("demo.png"), eq("rk-user"), startsWith("avatar/"), eq("image/png"), eq(3L));
    }

    @Test
    void uploadFile_shouldStillSucceedWhenLedgerWriteFails() {
        MockMultipartFile file = new MockMultipartFile("file", "demo.txt", "text/plain", "hello".getBytes());
        when(fileStorage.uploadFile(eq("rk-content"), startsWith("news-attachment/"), any(), eq(5L), eq("text/plain"))).thenReturn("req-2");
        when(ledgerFileService.saveUploadRecord(eq("demo.txt"), eq("rk-content"), startsWith("news-attachment/"), eq("text/plain"), eq(5L)))
                .thenThrow(new RuntimeException("table missing"));
        when(mediaPathHelper.toPublicUrl(startsWith("rk-content/news-attachment/")))
                .thenReturn("http://minio/rk-content/news-attachment/2026/04/23/demo.txt");

        FileDTO dto = fileController.uploadFile(file, "rk-content", "news-attachment");

        assertNotNull(dto);
        assertEquals(null, dto.getId());
        assertTrue(dto.getRelativePath().startsWith("rk-content/news-attachment/"));
        assertEquals(dto.getRelativePath(), dto.getPath());
        assertEquals("http://minio/rk-content/news-attachment/2026/04/23/demo.txt", dto.getUrl());
    }

    @Test
    void uploadFile_shouldPropagateQuotaRejectionFromLedger() {
        MockMultipartFile file = new MockMultipartFile("file", "demo.png", "image/png", new byte[]{1, 2, 3});
        org.mockito.Mockito.doThrow(new BadRequestException("当前租户文件存储已达到上限：100MB"))
                .when(ledgerFileService).assertUploadAllowed(3L);

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> fileController.uploadFile(file, "rk-user", "avatar")
        );

        assertEquals("当前租户文件存储已达到上限：100MB", ex.getMessage());
        verify(fileStorage, never()).uploadFile(any(), any(), any(), any(Long.class), any());
    }

    @Test
    void uploadFile_shouldCleanupUploadedObjectWhenLedgerRejectsAfterUpload() {
        MockMultipartFile file = new MockMultipartFile("file", "demo.png", "image/png", new byte[]{1, 2, 3});
        when(fileStorage.uploadFile(eq("rk-user"), startsWith("avatar/"), any(), eq(3L), eq("image/png"))).thenReturn("req-4");
        when(ledgerFileService.saveUploadRecord(eq("demo.png"), eq("rk-user"), startsWith("avatar/"), eq("image/png"), eq(3L)))
                .thenThrow(new BadRequestException("当前租户文件存储已达到上限：100MB"));

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> fileController.uploadFile(file, "rk-user", "avatar")
        );

        assertEquals("当前租户文件存储已达到上限：100MB", ex.getMessage());
        verify(fileStorage).deleteFile(eq("rk-user"), startsWith("avatar/"));
    }
}
