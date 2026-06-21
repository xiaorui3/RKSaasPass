package com.tianji.media.controller;

import com.tianji.common.autoconfigure.media.MediaPathHelper;
import com.tianji.common.exceptions.CommonException;
import com.tianji.file.domain.po.FileInfo;
import com.tianji.media.domain.dto.FileDTO;
import com.tianji.media.storage.IFileStorage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping({"/files", "/api/files"})
@Api(tags = "统一文件上传接口")
@RequiredArgsConstructor
public class FileController {

    private final @Qualifier("rkFileLedgerService") com.tianji.file.service.IFileService ledgerFileService;
    private final IFileStorage fileStorage;
    private final MediaPathHelper mediaPathHelper;

    @ApiOperation("统一上传文件")
    @PostMapping({"", "/upload"})
    public FileDTO uploadFile(
            @ApiParam(value = "文件数据") @RequestParam("file") MultipartFile file,
            @ApiParam(value = "微服务名") @RequestParam("service") String service,
            @ApiParam(value = "业务类型") @RequestParam("bizType") String bizType) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is required");
        }
        if (!StringUtils.hasText(service)) {
            throw new IllegalArgumentException("service is required");
        }
        if (!StringUtils.hasText(bizType)) {
            throw new IllegalArgumentException("bizType is required");
        }

        String bucketName = service.trim();
        String objectName = buildObjectName(bizType.trim(), file.getOriginalFilename());
        String relativePath = bucketName + "/" + objectName;

        try {
            ledgerFileService.assertUploadAllowed(file.getSize());
            fileStorage.uploadFile(bucketName, objectName, file.getInputStream(), file.getSize(), file.getContentType());
            Long fileId = null;
            try {
                FileInfo fileInfo = ledgerFileService.saveUploadRecord(
                        file.getOriginalFilename(),
                        bucketName,
                        objectName,
                        file.getContentType(),
                        file.getSize()
                );
                if (fileInfo != null) {
                    fileId = fileInfo.getId();
                }
            } catch (CommonException e) {
                try {
                    fileStorage.deleteFile(bucketName, objectName);
                } catch (Exception cleanupError) {
                    log.warn("Cleanup uploaded file after ledger rejection failed: {}/{}", bucketName, objectName, cleanupError);
                }
                throw e;
            } catch (Exception e) {
                log.warn("Write rk_file ledger failed, upload still succeeded: {}", e.getMessage());
            }
            String publicUrl = mediaPathHelper.toPublicUrl(relativePath);
            return FileDTO.of(fileId, file.getOriginalFilename(), relativePath, publicUrl);
        } catch (IOException e) {
            throw new IllegalStateException("read upload file failed", e);
        }
    }

    @ApiOperation("获取文件信息")
    @GetMapping("/{id}")
    public FileDTO getFileInfo(@ApiParam(value = "文件id", example = "1") @PathVariable("id") Long id) {
        FileInfo fileInfo = ledgerFileService.getById(id);
        if (fileInfo == null) {
            return null;
        }
        String relativePath = fileInfo.getFilePath() != null ? fileInfo.getFilePath() : fileInfo.getFileUrl();
        String publicUrl = mediaPathHelper.toPublicUrl(relativePath);
        return FileDTO.of(fileInfo.getId(), fileInfo.getFileName(), relativePath, publicUrl);
    }

    @ApiOperation("删除文件")
    @DeleteMapping("/{id}")
    public void deleteFileById(@ApiParam(value = "文件id", example = "1") @PathVariable("id") Long id) {
        ledgerFileService.deleteFile(id);
    }

    @ApiOperation("按路径删除文件")
    @DeleteMapping
    public boolean deleteFileByUrl(@RequestParam("url") String url) {
        return ledgerFileService.deleteFileByUrl(url);
    }

    @ApiOperation("检查文件是否存在")
    @GetMapping("/check")
    public boolean checkFileExists(@RequestParam("url") String url) {
        return ledgerFileService.checkFileExists(url);
    }

    private String buildObjectName(String bizType, String originalFilename) {
        String extension = "";
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        LocalDate today = LocalDate.now();
        return String.format(
                "%s/%04d/%02d/%02d/%s%s",
                bizType,
                today.getYear(),
                today.getMonthValue(),
                today.getDayOfMonth(),
                UUID.randomUUID().toString().replace("-", ""),
                extension
        );
    }
}
