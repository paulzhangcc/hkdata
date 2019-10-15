package com.meimeitech.hkdata.sdk;

import com.alibaba.fastjson.JSON;
import com.meimeitech.hkdata.sdk.condition.LinuxCondition;
import com.meimeitech.hkdata.sdk.condition.WindowsCondition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;

import java.io.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author paul
 * @description
 * @date 2019/6/13
 */

@Configuration
@Slf4j
public class SpringMonitorClockConfiguration {
    @Bean
    @Conditional(WindowsCondition.class)
    @Order
    public WindowSpringMonitorBeanDefinitionRegistryPostProcessor windowSpringMonitorBeanDefinitionRegistryPostProcessor(Environment environment) throws IOException {
        String hcConfigFilePath = environment.getProperty("hcConfigFilePath");
        InputStream resourceAsStream = null;
        if (hcConfigFilePath != null) {
            File file = new File(hcConfigFilePath);
            if (!file.exists()) {
                throw new FileNotFoundException("hcConfigFilePath配置文件不存在:" + hcConfigFilePath);
            }
            resourceAsStream = new FileInputStream(file);
        } else {
            log.warn("-DhcConfigFilePath文件不存在,取默认的hc.json");
            resourceAsStream = this.getClass().getClassLoader().getResourceAsStream("hc.json");
        }

        WindowSpringMonitorBeanDefinitionRegistryPostProcessor beanDefinitionRegistryPostProcessor = new WindowSpringMonitorBeanDefinitionRegistryPostProcessor();
        beanDefinitionRegistryPostProcessor.hcClinetProperties = JSON.parseObject(resourceAsStream, HcClinetProperties.class);
        String lib = beanDefinitionRegistryPostProcessor.hcClinetProperties.getLib();
        String userDir = System.getProperty("user.dir") + File.separator + "win64" + File.separator;
        if (lib == null && new File(userDir).isDirectory()) {
            beanDefinitionRegistryPostProcessor.hcClinetProperties.setLib(userDir);
            log.info("配置文件中lib为空,取运行目录的sdk:"+beanDefinitionRegistryPostProcessor.hcClinetProperties.getLib());
        }
        return beanDefinitionRegistryPostProcessor;
    }

    public class WindowSpringMonitorBeanDefinitionRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor {
        private final AtomicInteger atomicInteger = new AtomicInteger(0);
        public HcClinetProperties hcClinetProperties;

        @Override
        public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry) throws BeansException {
            hcClinetProperties.getConfig().forEach(config -> {
                GenericBeanDefinition genericBeanDefinition = new GenericBeanDefinition();
                String atomicIntegergenericBeanDefinition = MonitorClockFactoryBean.class.getSimpleName() + "_" + atomicInteger.getAndIncrement();
                genericBeanDefinition.setBeanClass(MonitorClockFactoryBean.class);
                genericBeanDefinition.getPropertyValues().add("ip", config.getIp());
                genericBeanDefinition.getPropertyValues().add("port", config.getPort());
                genericBeanDefinition.getPropertyValues().add("username", config.getUsername());
                genericBeanDefinition.getPropertyValues().add("password", config.getPassword());
                genericBeanDefinition.getPropertyValues().add("lib", hcClinetProperties.getLib());
                genericBeanDefinition.getPropertyValues().add("windows", true);
                genericBeanDefinition.setLazyInit(false);
                beanDefinitionRegistry.registerBeanDefinition(atomicIntegergenericBeanDefinition, genericBeanDefinition);

            });
        }

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {
        }
    }

    @Bean
    @Conditional(LinuxCondition.class)
    @Order
    public LinuxSpringMonitorBeanDefinitionRegistryPostProcessor linuxSpringMonitorBeanDefinitionRegistryPostProcessor(Environment environment) throws IOException {
        String hcConfigFilePath = environment.getProperty("hcConfigFilePath");
        InputStream resourceAsStream = null;
        if (hcConfigFilePath != null) {
            File file = new File(hcConfigFilePath);
            if (!file.exists()) {
                throw new FileNotFoundException("hcConfigFilePath配置文件不存在:" + hcConfigFilePath);
            }
            resourceAsStream = new FileInputStream(file);
        } else {
            log.warn("-DhcConfigFilePath文件不存在,取默认的hc.json");
            resourceAsStream = this.getClass().getClassLoader().getResourceAsStream("hc.json");
        }


        LinuxSpringMonitorBeanDefinitionRegistryPostProcessor beanDefinitionRegistryPostProcessor = new LinuxSpringMonitorBeanDefinitionRegistryPostProcessor();
        beanDefinitionRegistryPostProcessor.hcClinetProperties = JSON.parseObject(resourceAsStream, HcClinetProperties.class);
        String lib = beanDefinitionRegistryPostProcessor.hcClinetProperties.getLib();
        String userDir = System.getProperty("user.dir") + File.separator + "linux64" + File.separator;
        if (lib == null && new File(userDir).isDirectory()) {
            beanDefinitionRegistryPostProcessor.hcClinetProperties.setLib(userDir);
            log.info("配置文件中lib为空,取运行目录的sdk:"+beanDefinitionRegistryPostProcessor.hcClinetProperties.getLib());
        }
        return beanDefinitionRegistryPostProcessor;
    }

    public class LinuxSpringMonitorBeanDefinitionRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor {

        private final AtomicInteger atomicInteger = new AtomicInteger(0);
        public HcClinetProperties hcClinetProperties;

        @Override
        public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry) throws BeansException {
            hcClinetProperties.getConfig().forEach(config -> {
                GenericBeanDefinition genericBeanDefinition = new GenericBeanDefinition();
                String atomicIntegergenericBeanDefinition = MonitorClockFactoryBean.class.getSimpleName() + "_" + atomicInteger.getAndIncrement();
                genericBeanDefinition.setBeanClass(MonitorClockFactoryBean.class);
                genericBeanDefinition.getPropertyValues().add("ip", config.getIp());
                genericBeanDefinition.getPropertyValues().add("port", config.getPort());
                genericBeanDefinition.getPropertyValues().add("username", config.getUsername());
                genericBeanDefinition.getPropertyValues().add("password", config.getPassword());
                genericBeanDefinition.getPropertyValues().add("lib", hcClinetProperties.getLib());
                genericBeanDefinition.getPropertyValues().add("windows", false);
                genericBeanDefinition.setLazyInit(false);
                beanDefinitionRegistry.registerBeanDefinition(atomicIntegergenericBeanDefinition, genericBeanDefinition);

            });
        }

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {
        }
    }
}
