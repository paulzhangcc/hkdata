package com.meimeitech.hkdata;

import com.meimeitech.hkdata.sdk.MonitorClockHandler;
import com.meimeitech.hkdata.util.SnowFlakeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@SpringBootApplication
@RestController
public class HkdataApplication {

	@Autowired
	List<MonitorClockHandler> monitorClockHandlers;

	public static void main(String[] args) {
		SnowFlakeUtil.getFlowIdInstance();
		SpringApplication.run(HkdataApplication.class, args);
	}
}
