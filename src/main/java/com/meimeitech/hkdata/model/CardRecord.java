package com.meimeitech.hkdata.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @author paul
 * @description
 * @date 2019/6/5
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CardRecord {
    private String byCardNo;
    private int byCardType;
    private int dwMajor;
    private int dwMinor;
    private String sourceIp;
    private Date cardTime;
    private Date createTime;
}

