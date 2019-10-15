package com.meimeitech.hkdata.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 *
 * @author 北京鹏润美美科技有限公司
 * @since 2017年6月30日
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Response<T> {

    /**
     * 返回代码
     */
    private String code;

    /**
     * 返回消息
     */
    private String message;

    /**
     * 结果对象
     */
    private T result;

    /**
     * 时间戳
     */
    private long timestamp = System.currentTimeMillis();

    public Response() {}

    public static <T> Response<T> success(T body) {
        Response response = new Response();
        response.setCode(RetCode.SUCCESS.getCode());
        response.setResult(body);
        return response;
    }

    public static Response error(String message) {
        Response response = new Response();
        response.setCode(RetCode.INTERNALEXCEP.getCode());
        response.setMessage(message);
        return response;
    }

    public static Response exception(Throwable e) {
        Response response = new Response();
        response.setCode(RetCode.INTERNALEXCEP.getCode());
        response.setMessage(e.getMessage());
        return response;
    }

    public static <T> Response<T> response(String code, String message, T body) {
        Response response = new Response();
        response.setCode(code);
        response.setMessage(message);
        response.setResult(body);
        return response;
    }

    public static <T> Response<T> response(RetCode retCode) {
        Response response = new Response();
        response.setCode(retCode.getCode());
        response.setMessage(retCode.getMessage());
        return response;
    }

    public static <T> Response<T> response(String code, String message) {
        Response response = new Response();
        response.setCode(code);
        response.setMessage(message);
        return response;
    }


    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
