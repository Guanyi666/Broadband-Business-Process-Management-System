package com.bbpms.resource.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** OLT VO（含区域名）。 */
@Data
public class OltVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private Long regionId;
    private String regionName;
    private String ip;
    private String vendor;
    private String model;
    private Integer status;
}