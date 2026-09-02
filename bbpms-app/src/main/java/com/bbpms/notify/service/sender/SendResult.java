package com.bbpms.notify.service.sender;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @NoArgsConstructor @AllArgsConstructor
public class SendResult {
    private boolean success;
    private String messageId;
    private String errorMsg;
}