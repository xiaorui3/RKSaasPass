package com.tianji.media.storage;

import com.tianji.media.storage.IMediaStorage;
import com.tianji.media.storage.IFileStorage;
import com.tianji.media.storage.MediaUploadResult;
import com.tianji.media.domain.po.Media;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

/**
 * MinIO媒体存储适配器
 * 通过适配模式解决接口不匹配问题
 * 
 * @author System
 * @since 2026-02-15
 */
@Service("mediaStorageAdapter") // 使用不同的bean名称
@RequiredArgsConstructor
public class MediaStorageAdapter implements IMediaStorage {
    
    private final IFileStorage fileStorage;

    @Override
    public String getUploadSignature() {
        // IFileStorage没有此方法，返回空字符串或实现相应逻辑
        return "";
    }

    @Override
    public String getPlaySignature(String fieldId, Long userId, Integer freeTrial) {
        // IFileStorage只有两个参数的方法，忽略额外参数，返回空字符串
        return "";
    }

    @Override
    public MediaUploadResult uploadFile(String filename, InputStream inputStream, long contentLength) {
        String requestId = fileStorage.uploadFile(filename, inputStream, contentLength);
        return MediaUploadResult.builder()
                .requestId(requestId)
                .filename(filename)
                .build();
    }

    @Override
    public void deleteFile(String fileId) {
        fileStorage.deleteFile(fileId);
    }

    @Override
    public void deleteFiles(List<String> fileIds) {
        fileStorage.deleteFiles(fileIds);
    }

    @Override
    public List<Media> queryMediaInfos(String... fileIds) {
        return Collections.emptyList();
    }
}
