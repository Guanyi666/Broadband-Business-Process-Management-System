package com.bbpms.file.dto;
import lombok.Data;
@Data
public class FileUploadResp {
    private Long id;
    private String objectKey;
    private String url;
    private String thumbnailUrl;
    private Long size;
    private String contentType;
}