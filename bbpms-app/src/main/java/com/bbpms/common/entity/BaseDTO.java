package com.bbpms.common.entity;
import lombok.Data;
import java.io.Serializable;

@Data
public class BaseDTO implements Serializable {
    private Long pageNum = 1L;
    private Long pageSize = 20L;
    private String orderBy;
    private String keyword;
}
