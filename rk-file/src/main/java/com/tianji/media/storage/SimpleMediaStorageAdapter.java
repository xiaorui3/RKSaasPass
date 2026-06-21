package com.tianji.media.storage;

import com.tianji.media.domain.po.Media;
import com.tianji.media.storage.MediaUploadResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

/**
 * 简单媒体存储适配器
 * 解决接口方法签名不匹配问题
 * 
 * @author System
 * @since 2026-02-15
 */
@Service("simpleMediaStorage") // 使用不同的bean名称
@RequiredArgsConstructor
public class SimpleMediaStorageAdapter implements IMediaStorage {
    
    private final IFileStorage fileStorage;

    @Override
    public String getUploadSignature() {
        // IFileStorage没有此方法，返回空字符串
        return "";
    }

    @Override
    public String getPlaySignature(String fieldId, Long userId, Integer freeTrial) {
        // IFileStorage没有此方法，返回空字符串
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
