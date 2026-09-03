package com.bbpms.resource.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** PON 口 VO（含 OLT 名）。 */
@Data
public class PonVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long oltId;
    private String oltName;
    private String name;
    private Integer totalPorts;
    private Integer usedPorts;
}