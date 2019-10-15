/*
 * AlarmJavaDemoView.java
 */

package com.meimeitech.hkdata.sdk.window;

import com.alibaba.fastjson.JSON;
import com.meimeitech.hkdata.handle.DataHandler;
import com.meimeitech.hkdata.model.CardRecord;
import com.meimeitech.hkdata.sdk.MonitorClockHandler;
import com.meimeitech.hkdata.util.SpringContextUtil;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * The application's main frame.
 */
public class MonitorClockHandlerWindows implements ApplicationListener<ContextRefreshedEvent>, DisposableBean, MonitorClockHandler {
    public static Logger LOGGER = LoggerFactory.getLogger(MonitorClockHandlerWindows.class);

    HCNetSDK hCNetSDK = null;
    HCNetSDK.NET_DVR_USER_LOGIN_INFO m_strLoginInfo = new HCNetSDK.NET_DVR_USER_LOGIN_INFO();//设备登录信息
    HCNetSDK.NET_DVR_DEVICEINFO_V40 m_strDeviceInfo = new HCNetSDK.NET_DVR_DEVICEINFO_V40();//设备信息
    String m_sDeviceIP;//已登录设备的IP地址
    String m_sDevicePort;//已登录设备的PORT
    String m_sUsername;//设备用户名
    String m_sPassword;//设备密码

    int lUserID;//用户句柄
    int lAlarmHandle;//报警布防句柄
    int lListenHandle;//报警监听句柄

    private String lib;

    FMSGCallBack fMSFCallBack;//报警回调函数实现
    FMSGCallBack_V31 fMSFCallBack_V31;//报警回调函数实现


    public String getInfo() {
        return "ip=" + m_sDeviceIP + ",port=" + m_sDevicePort + ":";
    }


    public MonitorClockHandlerWindows(String m_sDeviceIP, String m_sDevicePort, String m_sUsername, String m_sPassword, String lib) {
        this.m_sDeviceIP = m_sDeviceIP;
        this.m_sDevicePort = m_sDevicePort;
        this.m_sUsername = m_sUsername;
        this.m_sPassword = m_sPassword;
        this.lib = lib;
        this.lUserID = -1;
        this.lAlarmHandle = -1;
        this.lListenHandle = -1;
        this.fMSFCallBack = null;
        this.fMSFCallBack_V31 = null;
        this.hCNetSDK = (HCNetSDK) Native.loadLibrary(lib+"HCNetSDK", HCNetSDK.class);
        boolean initSuc = hCNetSDK.NET_DVR_Init();
        if (initSuc != true) {
            LOGGER.error(getInfo() + "初始化失败");
        }

        HCNetSDK.NET_DVR_LOCAL_GENERAL_CFG struGeneralCfg = new HCNetSDK.NET_DVR_LOCAL_GENERAL_CFG();
        struGeneralCfg.byAlarmJsonPictureSeparate = 1; //控制JSON透传报警数据和图片是否分离，0-不分离，1-分离（分离后走COMM_ISAPI_ALARM回调返回）
        struGeneralCfg.write();

        if (!hCNetSDK.NET_DVR_SetSDKLocalCfg(17, struGeneralCfg.getPointer())) {
            LOGGER.info(getInfo() + "NET_DVR_SetSDKLocalCfg失败");
        }
    }

    @Override
    public void start() {
        if (init()) {
            setupAlarmChan();
        }
    }

    @Override
    public void stop() {
        logout();
    }

    @Override
    public void destroy() throws Exception {
        stop();
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        start();
    }

    public void alarmDataHandle(int lCommand, HCNetSDK.NET_DVR_ALARMER pAlarmer, Pointer pAlarmInfo, int dwBufLen, Pointer pUser) {
        String[] newRow = new String[3];
        //报警时间
        Date today = new Date();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String[] sIP = new String[2];

        String sAlarmType = new String("commandID=") + lCommand;
        switch (lCommand) {
            case HCNetSDK.COMM_ALARM_V40:
                HCNetSDK.NET_DVR_ALARMINFO_V40 struAlarmInfoV40 = new HCNetSDK.NET_DVR_ALARMINFO_V40();
                struAlarmInfoV40.write();
                Pointer pInfoV40 = struAlarmInfoV40.getPointer();
                pInfoV40.write(0, pAlarmInfo.getByteArray(0, struAlarmInfoV40.size()), 0, struAlarmInfoV40.size());
                struAlarmInfoV40.read();

                switch (struAlarmInfoV40.struAlarmFixedHeader.dwAlarmType) {
                    case 0:
                        struAlarmInfoV40.struAlarmFixedHeader.ustruAlarm.setType(HCNetSDK.struIOAlarm.class);
                        struAlarmInfoV40.read();
                        sAlarmType = sAlarmType + new String("：信号量报警") + "，" + "报警输入口：" + struAlarmInfoV40.struAlarmFixedHeader.ustruAlarm.struioAlarm.dwAlarmInputNo;
                        break;
                    case 1:
                        sAlarmType = sAlarmType + new String("：硬盘满");
                        break;
                    case 2:
                        sAlarmType = sAlarmType + new String("：信号丢失");
                        break;
                    case 3:
                        struAlarmInfoV40.struAlarmFixedHeader.ustruAlarm.setType(HCNetSDK.struAlarmChannel.class);
                        struAlarmInfoV40.read();
                        int iChanNum = struAlarmInfoV40.struAlarmFixedHeader.ustruAlarm.sstrualarmChannel.dwAlarmChanNum;
                        sAlarmType = sAlarmType + new String("：移动侦测") + "，" + "报警通道个数：" + iChanNum + "，" + "报警通道号：";

                        for (int i = 0; i < iChanNum; i++) {
                            byte[] byChannel = struAlarmInfoV40.pAlarmData.getByteArray(i * 4, 4);

                            int iChanneNo = 0;
                            for (int j = 0; j < 4; j++) {
                                int ioffset = j * 8;
                                int iByte = byChannel[j] & 0xff;
                                iChanneNo = iChanneNo + (iByte << ioffset);
                            }

                            sAlarmType = sAlarmType + "+ch[" + iChanneNo + "]";
                        }

                        break;
                    case 4:
                        sAlarmType = sAlarmType + new String("：硬盘未格式化");
                        break;
                    case 5:
                        sAlarmType = sAlarmType + new String("：读写硬盘出错");
                        break;
                    case 6:
                        sAlarmType = sAlarmType + new String("：遮挡报警");
                        break;
                    case 7:
                        sAlarmType = sAlarmType + new String("：制式不匹配");
                        break;
                    case 8:
                        sAlarmType = sAlarmType + new String("：非法访问");
                        break;
                }

                newRow[0] = dateFormat.format(today);
                //报警类型
                newRow[1] = sAlarmType;
                //报警设备IP地址
                sIP = new String(pAlarmer.sDeviceIP).split("\0", 2);
                newRow[2] = sIP[0];
                LOGGER.info(getInfo() + ":commandID={} " + ",时间:{},报警类型:{},报警IP:{}", lCommand, newRow[0], newRow[1], sIP);
                break;
            //门禁主机报警信息
            case HCNetSDK.COMM_ALARM_ACS:
                HCNetSDK.NET_DVR_ACS_ALARM_INFO strACSInfo = new HCNetSDK.NET_DVR_ACS_ALARM_INFO();
                strACSInfo.write();
                Pointer pACSInfo = strACSInfo.getPointer();
                pACSInfo.write(0, pAlarmInfo.getByteArray(0, strACSInfo.size()), 0, strACSInfo.size());
                strACSInfo.read();

                sAlarmType = sAlarmType + "：门禁主机报警信息，卡号：" + new String(strACSInfo.struAcsEventInfo.byCardNo).trim() + "，卡类型：" +
                        strACSInfo.struAcsEventInfo.byCardType + "，报警主类型：" + strACSInfo.dwMajor + "，报警次类型：" + strACSInfo.dwMinor + ",时间：" + strACSInfo.struTime.toStringTime();
                newRow[0] = dateFormat.format(today);
                //报警类型
                newRow[1] = sAlarmType;
                //报警设备IP地址
                sIP = new String(pAlarmer.sDeviceIP).split("\0", 2);
                newRow[2] = sIP[0];
                CardRecord cardRecord = CardRecord.builder().byCardNo(new String(strACSInfo.struAcsEventInfo.byCardNo).trim())
                        .byCardType(strACSInfo.struAcsEventInfo.byCardType)
                        .sourceIp(new String(pAlarmer.sDeviceIP).split("\0", 2)[0])
                        .dwMajor(strACSInfo.dwMajor)
                        .dwMinor(strACSInfo.dwMinor)
                        .cardTime(strACSInfo.struTime.getDate())
                        .createTime(new Date()).build();
                if (cardRecord.getByCardType() != 0) {
                    SpringContextUtil.getBean(DataHandler.class).put(cardRecord);
                    LOGGER.info(getInfo() + ":commandID={} " + "打卡数据:{}", lCommand, JSON.toJSONString(cardRecord));
                }
                break;
            default:
                newRow[0] = dateFormat.format(today);
                //报警类型
                newRow[1] = sAlarmType;
                //报警设备IP地址
                sIP = new String(pAlarmer.sDeviceIP).split("\0", 2);
                newRow[2] = sIP[0];
                LOGGER.info(getInfo() + ":commandID={} " + "时间:{},报警类型:{},报警IP:{}", lCommand, newRow[0], newRow[1], sIP);
                break;
        }
    }

    public class FMSGCallBack_V31 implements HCNetSDK.FMSGCallBack_V31 {
        //报警信息回调函数
        @Override
        public boolean invoke(int lCommand, HCNetSDK.NET_DVR_ALARMER pAlarmer, Pointer pAlarmInfo, int dwBufLen, Pointer pUser) {
            alarmDataHandle(lCommand, pAlarmer, pAlarmInfo, dwBufLen, pUser);
            return true;
        }
    }

    public class FMSGCallBack implements HCNetSDK.FMSGCallBack {
        //报警信息回调函数
        @Override
        public void invoke(int lCommand, HCNetSDK.NET_DVR_ALARMER pAlarmer, Pointer pAlarmInfo, int dwBufLen, Pointer pUser) {
            alarmDataHandle(lCommand, pAlarmer, pAlarmInfo, dwBufLen, pUser);
        }
    }


    private boolean init() {//GEN-FIRST:event_jButtonLoginActionPerformed

        //注册之前先注销已注册的用户,预览情况下不可注销
        if (lUserID > -1) {
            //先注销
            hCNetSDK.NET_DVR_Logout(lUserID);
            lUserID = -1;
        }

        //注册
        m_strLoginInfo.sDeviceAddress = new byte[HCNetSDK.NET_DVR_DEV_ADDRESS_MAX_LEN];
        System.arraycopy(m_sDeviceIP.getBytes(), 0, m_strLoginInfo.sDeviceAddress, 0, m_sDeviceIP.length());

        m_strLoginInfo.sUserName = new byte[HCNetSDK.NET_DVR_LOGIN_USERNAME_MAX_LEN];
        System.arraycopy(m_sUsername.getBytes(), 0, m_strLoginInfo.sUserName, 0, m_sUsername.length());

        m_strLoginInfo.sPassword = new byte[HCNetSDK.NET_DVR_LOGIN_PASSWD_MAX_LEN];
        System.arraycopy(m_sPassword.getBytes(), 0, m_strLoginInfo.sPassword, 0, m_sPassword.length());

        m_strLoginInfo.wPort = (short) Integer.parseInt(m_sDevicePort);

        m_strLoginInfo.bUseAsynLogin = false; //是否异步登录：0- 否，1- 是
        m_strLoginInfo.write();
        lUserID = hCNetSDK.NET_DVR_Login_V40(m_strLoginInfo, m_strDeviceInfo);

        if (lUserID == -1) {
            LOGGER.info(getInfo() + "注册失败，错误号:" + hCNetSDK.NET_DVR_GetLastError());
            return false;
        } else {
            LOGGER.info(getInfo() + "注册成功");
            return true;
        }
    }

    public void logout() {
        //报警撤防
        if (lAlarmHandle > -1) {
            if (!hCNetSDK.NET_DVR_CloseAlarmChan_V30(lAlarmHandle)) {
                LOGGER.info(getInfo() + "撤防失败，错误号:" + hCNetSDK.NET_DVR_GetLastError());
            } else {
                lAlarmHandle = -1;
                LOGGER.info(getInfo() + "撤防成功");
            }
        }

        //注销
        if (lUserID > -1) {
            if (hCNetSDK.NET_DVR_Logout(lUserID)) {
                LOGGER.info(getInfo() + "注销成功");
                lUserID = -1;
            } else {
                LOGGER.info(getInfo() + "注销失败，错误号:" + hCNetSDK.NET_DVR_GetLastError());
            }
        }
        hCNetSDK.NET_DVR_Cleanup();
    }

    public void setupAlarmChan() {
        if (lUserID == -1) {
            LOGGER.info(getInfo() + "请先注册");
            return;
        }
        if (lAlarmHandle < 0)//尚未布防,需要布防
        {
            if (fMSFCallBack_V31 == null) {
                fMSFCallBack_V31 = new FMSGCallBack_V31();
                Pointer pUser = null;
                if (!hCNetSDK.NET_DVR_SetDVRMessageCallBack_V31(fMSFCallBack_V31, pUser)) {
                    LOGGER.info(getInfo() + "设置回调函数失败，错误号:" + hCNetSDK.NET_DVR_GetLastError());
                }
            }
            HCNetSDK.NET_DVR_SETUPALARM_PARAM m_strAlarmInfo = new HCNetSDK.NET_DVR_SETUPALARM_PARAM();
            m_strAlarmInfo.dwSize = m_strAlarmInfo.size();
            m_strAlarmInfo.byLevel = 1;//智能交通布防优先级：0- 一等级（高），1- 二等级（中），2- 三等级（低）
            m_strAlarmInfo.byAlarmInfoType = 1;//智能交通报警信息上传类型：0- 老报警信息（NET_DVR_PLATE_RESULT），1- 新报警信息(NET_ITS_PLATE_RESULT)
            m_strAlarmInfo.byDeployType = 1; //布防类型(仅针对门禁主机、人证设备)：0-客户端布防(会断网续传)，1-实时布防(只上传实时数据)
            m_strAlarmInfo.write();
            lAlarmHandle = hCNetSDK.NET_DVR_SetupAlarmChan_V41(lUserID, m_strAlarmInfo);
            if (lAlarmHandle == -1) {
                LOGGER.info(getInfo() + "布防失败，错误号:" + hCNetSDK.NET_DVR_GetLastError());
            } else {
                LOGGER.info(getInfo() + "布防成功");
            }
        }
    }

}


