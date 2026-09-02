package com.bbpms.user.dto;

import lombok.Data;

@Data
public class PasswordChangeReq {

    private String oldPassword;

    private String newPassword;
}