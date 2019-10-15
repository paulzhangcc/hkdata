package com.meimeitech.hkdata.model;

public enum RetCode {

    SUCCESS("0", "success"),
    ACCESSDENY("-1", "访问拒绝"),
    VALIDATEERROR("-2", "验证错误"),
    INTERNALEXCEP("-9", "内部异常"),
    TIMEOUT("-100", "超时"),
    NOLOGIN("-101", "请登录"),
    LOGINED("-102", "您已在别处登录此处已下线"),
    USERORPWDERR("-103", "用户名或密码错误"),
    COMMITDUP("-107", "重复提交"),
    SHOWCAPTCHA("-111", "显示图形验证码"),
    CAPTCHAERR("-112","图形验证码输入有误"),
    DENYFORMORE("-113", "请求次数超限，操作被拒绝"),
    NOPERMISSION("-114", "权限不足"),
    NOUSERINVESTSURVER("-112", "未查到用户风险评估信息"),
    USERLOCKED("-205", "账户被锁定"),
    SENDSMSFAIL("-211", "1分钟内已发送过验证码"),
    NOSMSTEMPLATE("-212", "没找到短信模板"),
    VERIFYSMSVERIFYCODEFAIL("-213", "请输入正确验证码"),
    EMPTYVVERIFYCODEFAIL("-214", "验证码为空"),
    NEEDUPDATE("-215", "数据已经存在,请进行修改"),
    NOTSUPPORT("-216", "不支持的操作类型"),
    UPDATEERROR("-217", "更新失败");

    private String code;

    private String message;

    private RetCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

}
