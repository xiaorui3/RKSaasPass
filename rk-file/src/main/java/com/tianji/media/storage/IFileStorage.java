package com.tianji.media.storage;

import java.io.InputStream;
import java.util.List;

public interface IFileStorage {

    String uploadFile(String key, InputStream inputStream, long contentLength);

    default String uploadFile(String bucket, String key, InputStream inputStream, long contentLength, String contentType) {
        return uploadFile(key, inputStream, contentLength);
    }

    InputStream downloadFile(String key);

    default InputStream downloadFile(String bucket, String key) {
        return downloadFile(key);
    }

    void deleteFile(String key);

    default void deleteFile(String bucket, String key) {
        deleteFile(key);
    }

    void deleteFiles(List<String> keys);
}
