package com.bbpms.file.service.storage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
@Slf4j
@Service
@ConditionalOnProperty(name = "bbpms.file.storage-type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {
    @Value("${bbpms.file.upload-dir:./uploads}")
    private String uploadDir;
    @Value("${bbpms.file.base-url:http://localhost:8080/files}")
    private String baseUrl;
    @Override
    public String upload(String objectKey, MultipartFile file) throws IOException {
        File dest = new File(uploadDir, objectKey);
        dest.getParentFile().mkdirs();
        file.transferTo(dest);
        return baseUrl + "/" + objectKey;
    }
    @Override
    public String getPresignedUrl(String objectKey, int expirySeconds) {
        return baseUrl + "/" + objectKey;
    }
    @Override
    public void delete(String objectKey) {
        File f = new File(uploadDir, objectKey);
        if (f.exists()) f.delete();
    }
    @Override
    public String getBucket() { return "local"; }
}