package com.meimeitech.hkdata.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 考勤记录表
 */


@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DataRecordVO {

    private String clientReuqestNo = null;

    private String clientIp = null;

    private String sourceIp = null;

    private String clientType = null;

    private Integer type = null;

    private String cardNo = null;

    private String cardTime = null;
}

