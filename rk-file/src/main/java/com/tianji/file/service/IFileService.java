package com.tianji.file.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.file.domain.po.FileInfo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IFileService extends IService<FileInfo> {

    FileInfo uploadFile(MultipartFile file);

    void assertUploadAllowed(Long fileSize);

    FileInfo saveUploadRecord(String originalFilename, String bucketName, String objectName, String contentType, Long fileSize);

    String uploadNewsImage(MultipartFile file);

    String uploadNewsAttachment(MultipartFile file);

    String uploadWorksCover(MultipartFile file);

    String uploadAvatar(MultipartFile file);

    boolean deleteFileByUrl(String url);

    boolean checkFileExists(String url);

    List<FileInfo> getFileList();

    boolean deleteFile(Long id);
}
