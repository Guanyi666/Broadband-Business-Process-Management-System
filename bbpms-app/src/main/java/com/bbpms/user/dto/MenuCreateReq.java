package com.bbpms.user.dto;

import lombok.Data;

@Data
public class MenuCreateReq {

    private Long parentId;

    private String menuName;

    private String menuType;

    private String path;

    private String component;

    private String perms;

    private String icon;

    private Integer sortOrder;

    private Integer visible;

    private Integer status;
}