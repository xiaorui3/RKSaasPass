package com.tianji.file.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.TenantSelfServiceAdmissionPolicyDTO;
import com.tianji.common.autoconfigure.media.MediaPathHelper;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.CommonException;
import com.tianji.common.utils.TenantContext;
import com.tianji.common.utils.UserContext;
import com.tianji.file.domain.po.FileInfo;
import com.tianji.file.mapper.FileMapper;
import com.tianji.file.service.IFileService;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.SetBucketPolicyArgs;
import io.minio.StatObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service("rkFileLedgerService")
@RequiredArgsConstructor
public class FileServiceImpl extends ServiceImpl<FileMapper, FileInfo> implements IFileService {

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final long MAX_AVATAR_SIZE = 2 * 1024 * 1024;

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "bmp"
    );
    private static final Set<String> ALLOWED_IMAGE_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp"
    );
    private static final Set<String> ALLOWED_ATTACHMENT_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "zip", "rar", "mp4"
    );

    private final MinioClient minioClient;
    private final MediaPathHelper mediaPathHelper;
    private final UserClient userClient;

    @Value("${minio.bucket-name:rk-bucket}")
    private String defaultBucketName;

    @Override
    public FileInfo uploadFile(MultipartFile file) {
        validateFile(file, MAX_FILE_SIZE);
        return uploadManaged(file, defaultBucketName, "common");
    }

    @Override
    public void assertUploadAllowed(Long fileSize) {
        ensureStorageQuota(currentTenantId(), fileSize);
    }

    @Override
    public FileInfo saveUploadRecord(String originalFilename, String bucketName, String objectName, String contentType, Long fileSize) {
        Long tenantId = currentTenantId();
        ensureStorageQuota(tenantId, fileSize);
        String relativePath = toRelativePath(bucketName, objectName);
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileName(originalFilename);
        fileInfo.setOriginalName(originalFilename);
        fileInfo.setFilePath(relativePath);
        fileInfo.setFileUrl(relativePath);
        fileInfo.setFileSize(fileSize);
        fileInfo.setFileType(contentType);
        fileInfo.setExtension(getFileExtension(originalFilename));
        fileInfo.setMimeType(contentType);
        fileInfo.setBucketName(bucketName);
        fileInfo.setObjectName(objectName);
        fileInfo.setTenantId(tenantId);
        fileInfo.setUploadedBy(UserContext.getUser() == null ? null : String.valueOf(UserContext.getUser()));
        fileInfo.setDownloadCount(0);
        fileInfo.setCreateTime(LocalDateTime.now());
        fileInfo.setUpdateTime(LocalDateTime.now());
        fileInfo.setDeleteFlag(0);
        save(fileInfo);
        return fileInfo;
    }

    @Override
    public String uploadNewsImage(MultipartFile file) {
        validateImageFile(file);
        return mediaPathHelper.toPublicUrl(uploadManaged(file, defaultBucketName, "news-image").getFilePath());
    }

    @Override
    public String uploadNewsAttachment(MultipartFile file) {
        validateAttachmentFile(file);
        return mediaPathHelper.toPublicUrl(uploadManaged(file, defaultBucketName, "news-attachment").getFilePath());
    }

    @Override
    public String uploadWorksCover(MultipartFile file) {
        validateImageFile(file);
        return mediaPathHelper.toPublicUrl(uploadManaged(file, defaultBucketName, "works-cover").getFilePath());
    }

    @Override
    public String uploadAvatar(MultipartFile file) {
        validateAvatarFile(file);
        return mediaPathHelper.toPublicUrl(uploadManaged(file, defaultBucketName, "avatar").getFilePath());
    }

    @Override
    public boolean deleteFileByUrl(String url) {
        StoredObject storedObject = parseStoredObject(url);
        if (storedObject == null) {
            return false;
        }
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(storedObject.bucket())
                            .object(storedObject.objectName())
                            .build()
            );
            LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(FileInfo::getBucketName, storedObject.bucket())
                    .eq(FileInfo::getObjectName, storedObject.objectName());
            remove(wrapper);
            return true;
        } catch (Exception e) {
            log.error("Delete file failed for {}", url, e);
            return false;
        }
    }

    @Override
    public boolean checkFileExists(String url) {
        StoredObject storedObject = parseStoredObject(url);
        if (storedObject == null) {
            return false;
        }
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(storedObject.bucket())
                            .object(storedObject.objectName())
                            .build()
            );
            return true;
        } catch (Exception e) {
            log.warn("File does not exist: {}", url);
            return false;
        }
    }

    @Override
    public List<FileInfo> getFileList() {
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(FileInfo::getCreateTime);
        return list(wrapper);
    }

    @Override
    public boolean deleteFile(Long id) {
        FileInfo fileInfo = getById(id);
        if (fileInfo == null) {
            return false;
        }
        StoredObject storedObject = parseStoredObject(
                fileInfo.getFilePath() != null ? fileInfo.getFilePath() : fileInfo.getFileUrl()
        );
        if (storedObject == null) {
            removeById(id);
            return true;
        }
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(storedObject.bucket())
                            .object(storedObject.objectName())
                            .build()
            );
            removeById(id);
            return true;
        } catch (Exception e) {
            log.error("Delete file by id failed: {}", id, e);
            return false;
        }
    }

    private FileInfo uploadManaged(MultipartFile file, String bucketName, String bizType) {
        assertUploadAllowed(file.getSize());
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String objectName = buildObjectName(bizType, extension);
        try (InputStream inputStream = file.getInputStream()) {
            ensureBucketExists(bucketName);
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            return saveUploadRecord(originalFilename, bucketName, objectName, file.getContentType(), file.getSize());
        } catch (IOException e) {
            throw new IllegalStateException("read upload file failed", e);
        } catch (CommonException e) {
            removeUploadedObjectQuietly(bucketName, objectName);
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("upload file failed", e);
        }
    }

    private void removeUploadedObjectQuietly(String bucketName, String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
        } catch (Exception cleanupError) {
            log.warn("cleanup uploaded file after ledger rejection failed, bucket={}, object={}",
                    bucketName, objectName, cleanupError);
        }
    }

    private void ensureStorageQuota(Long tenantId, Long incomingBytes) {
        TenantSelfServiceAdmissionPolicyDTO policy = loadTenantSelfServicePolicy(tenantId);
        Integer maxStorageMb = policy == null ? null : policy.getMaxStorageMb();
        if (maxStorageMb == null || maxStorageMb <= 0) {
            return;
        }
        long incoming = incomingBytes == null ? 0L : Math.max(0L, incomingBytes);
        Long usedBytes;
        try {
            usedBytes = baseMapper.sumActiveFileSizeByTenant(tenantId == null ? 1L : tenantId);
        } catch (Exception e) {
            log.warn("sum tenant file storage failed, tenantId={}, reason={}", tenantId, e.getMessage());
            return;
        }
        if (usedBytes == null) {
            usedBytes = 0L;
        }
        long limitBytes = maxStorageMb.longValue() * 1024L * 1024L;
        if (usedBytes + incoming > limitBytes) {
            throw new BadRequestException("当前租户文件存储已达到上限：" + maxStorageMb + "MB");
        }
    }

    private TenantSelfServiceAdmissionPolicyDTO loadTenantSelfServicePolicy(Long tenantId) {
        try {
            return userClient.queryTenantSelfServiceAdmissionPolicy(tenantId == null ? 1L : tenantId);
        } catch (Exception e) {
            log.warn("load tenant self service policy failed, tenantId={}, reason={}", tenantId, e.getMessage());
            return null;
        }
    }

    private Long currentTenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }

    private void validateFile(MultipartFile file, long maxSize) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file must not be empty");
        }
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("file size exceeds limit");
        }
    }

    private void validateImageFile(MultipartFile file) {
        validateFile(file, MAX_IMAGE_SIZE);
        String extension = getFileExtension(file.getOriginalFilename());
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("unsupported image extension");
        }
        String contentType = normalize(file.getContentType());
        if (!ALLOWED_IMAGE_MIME_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("unsupported image content type");
        }
    }

    private void validateAvatarFile(MultipartFile file) {
        validateFile(file, MAX_AVATAR_SIZE);
        validateImageFile(file);
    }

    private void validateAttachmentFile(MultipartFile file) {
        validateFile(file, MAX_FILE_SIZE);
        String extension = getFileExtension(file.getOriginalFilename());
        if (!ALLOWED_ATTACHMENT_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("unsupported attachment extension");
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.isBlank() || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private String buildObjectName(String bizType, String extension) {
        LocalDateTime now = LocalDateTime.now();
        String suffix = extension.isBlank() ? "" : "." + extension;
        return String.format(
                "%s/%04d/%02d/%02d/%s%s",
                bizType,
                now.getYear(),
                now.getMonthValue(),
                now.getDayOfMonth(),
                UUID.randomUUID().toString().replace("-", ""),
                suffix
        );
    }

    private String toRelativePath(String bucketName, String objectName) {
        return bucketName + "/" + objectName;
    }

    private StoredObject parseStoredObject(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = mediaPathHelper.normalizeForStorage(value);
        if (normalized == null || normalized.isBlank()) {
            return null;
        }
        String candidate = normalized.startsWith("/") ? normalized.substring(1) : normalized;
        int firstSlash = candidate.indexOf('/');
        if (firstSlash < 0) {
            return null;
        }
        String bucket = candidate.substring(0, firstSlash);
        String objectName = candidate.substring(firstSlash + 1);
        if (bucket.isBlank() || objectName.isBlank()) {
            return null;
        }
        if (!bucket.startsWith("rk-")) {
            bucket = defaultBucketName;
            objectName = candidate;
        }
        return new StoredObject(bucket, objectName);
    }

    private void ensureBucketExists(String bucketName) throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (exists) {
            return;
        }
        minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        minioClient.setBucketPolicy(
                SetBucketPolicyArgs.builder()
                        .bucket(bucketName)
                        .config("{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":\"*\"},\"Action\":[\"s3:GetObject\"],\"Resource\":[\"arn:aws:s3:::" + bucketName + "/*\"]}]}")
                        .build()
        );
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static final class StoredObject {
        private final String bucket;
        private final String objectName;

        private StoredObject(String bucket, String objectName) {
            this.bucket = bucket;
            this.objectName = objectName;
        }

        private String bucket() {
            return bucket;
        }

        private String objectName() {
            return objectName;
        }
    }
}
