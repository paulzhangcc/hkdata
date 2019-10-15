package com.meimeitech.hkdata.handle;

import com.alibaba.fastjson.JSON;
import com.meimeitech.hkdata.model.CardRecord;
import com.meimeitech.hkdata.model.DataRecordVO;
import com.meimeitech.hkdata.model.Response;
import com.meimeitech.hkdata.util.InetUtils;
import com.meimeitech.hkdata.util.SnowFlakeUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author paul
 * @description
 * @date 2019/6/17
 */
@Component
@Slf4j
public class DataHandler implements InitializingBean {

    @Value("${oa.server.url}")
    private String url;


    private Map<String, ExecutorService> runner = new HashMap<>();

    public static ConcurrentHashMap<String, DataRecordVO> concurrentHashMap = new ConcurrentHashMap();

    public void put(CardRecord cardRecord) {
        log.info("考勤原始数据:" + JSON.toJSONString(cardRecord));
        String index = cardRecord.getByCardNo().substring(cardRecord.getByCardNo().length() - 1, cardRecord.getByCardNo().length());

        DataRecordVO dataRecordVO = DataRecordVO.builder().clientReuqestNo(String.valueOf(SnowFlakeUtil.getFlowIdInstance().nextId()))
                .cardNo(cardRecord.getByCardNo())
                .cardTime(System.currentTimeMillis() + "")
                .clientIp(InetUtils.findFirstNonLoopbackAddress())
                .sourceIp(cardRecord.getSourceIp())
                .clientType("HC:DS-K5606").build();

        if (cardRecord.getDwMajor() == 5 && cardRecord.getDwMinor() == 75) {
            dataRecordVO.setType(1);
        } else if (cardRecord.getDwMajor() == 5 && cardRecord.getDwMinor() == 38) {
            dataRecordVO.setType(3);
        } else if (cardRecord.getDwMajor() == 5 && cardRecord.getDwMinor() == 1) {
            dataRecordVO.setType(2);
        } else {
            dataRecordVO.setType(0);
        }
        log.info("考勤上传远程数据上传:clientReuqestNo={}," + JSON.toJSONString(dataRecordVO), dataRecordVO.getClientReuqestNo());
        concurrentHashMap.put(dataRecordVO.getClientReuqestNo(), dataRecordVO);
        runner.get(index).submit(new Runnable() {
            @Override
            public void run() {
                try {
                    doPost(url, dataRecordVO);
                } catch (Exception e) {
                    log.error("发送数据到中控台报错:", e);
                }
            }
        });
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        for (int i = 0; i < 10; i++) {
            runner.put(String.valueOf(i), Executors.newSingleThreadExecutor());
        }
    }


    /**
     * post请求
     *
     * @param url
     * @return
     */
    public static void doPost(String url, DataRecordVO dataRecordVO) throws Exception {

        CloseableHttpClient httpclient = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(url);
        StringEntity s = new StringEntity(JSON.toJSONString(dataRecordVO));
        s.setContentEncoding("UTF-8");
        s.setContentType("application/json");
        post.setEntity(s);
        HttpResponse res = httpclient.execute(post);
        if (res.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            String reslult = EntityUtils.toString(res.getEntity(), "UTF-8");
            log.info("考勤上传远程数据返回数据:{}", reslult);
            Response response = JSON.parseObject(reslult, Response.class);
            String string = response.getResult().toString();
            for (String id : string.split(",")) {
                if (id != null && id.length() > 0) {
                    concurrentHashMap.remove(id);
                    log.info("考勤上传远程数据上传成功:clientReuqestNo={}", dataRecordVO.getClientReuqestNo());
                }

            }
        } else {
            int statusCode = res.getStatusLine().getStatusCode();
            String reslult = EntityUtils.toString(res.getEntity(), "UTF-8");
            log.info("考勤上传远程数据上传失败:clientReuqestNo={},code=[{}],message=[{}]", dataRecordVO.getClientReuqestNo(), statusCode, reslult);

        }
    }
}
