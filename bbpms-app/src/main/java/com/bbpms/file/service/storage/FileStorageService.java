package com.bbpms.file.service.storage;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
public interface FileStorageService {
    String upload(String objectKey, MultipartFile file) throws IOException;
    String getPresignedUrl(String objectKey, int expirySeconds);
    void delete(String objectKey);
    String getBucket();
}