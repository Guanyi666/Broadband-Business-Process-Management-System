package com.bbpms.resource.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** ONU VO（含绑定房间/PON 名）。 */
@Data
public class OnuVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long roomId;
    private String roomNo;
    private Long ponId;
    private String ponName;
    private String sn;
    private String model;
    private String status;
}