package com.meimeitech.hkdata.sdk;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * @author paul
 * @description
 * @date 2019/5/5
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

@ConfigurationProperties
public class HcClinetProperties {
    private List<Config> config = new ArrayList<>();
    private String lib;
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Config{
        String ip;
        Integer port;
        String username;
        String password;
    }
}
