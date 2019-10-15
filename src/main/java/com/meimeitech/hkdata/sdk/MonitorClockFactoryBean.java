/**
 * Copyright 2010-2018 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.meimeitech.hkdata.sdk;

import com.meimeitech.hkdata.sdk.linux.MonitorClockHandlerLinux;
import com.meimeitech.hkdata.sdk.window.MonitorClockHandlerWindows;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.FactoryBean;

@Getter
@Setter
public class MonitorClockFactoryBean implements FactoryBean<MonitorClockHandler> {

    private String ip;
    private String port;
    private String username;
    private String password;
    private String lib;
    private boolean windows;

    @Override
    public MonitorClockHandler getObject() throws Exception {
        if (windows){
            return new MonitorClockHandlerWindows(ip,
                    port,
                    username,
                    password,
                    lib
            );
        }
        return new MonitorClockHandlerLinux(ip,
                port,
                username,
                password,
                lib
        );
    }

    @Override
    public Class<MonitorClockHandler> getObjectType() {
        return MonitorClockHandler.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
