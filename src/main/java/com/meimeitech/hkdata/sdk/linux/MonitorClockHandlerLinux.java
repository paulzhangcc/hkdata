/*
 * AlarmJavaDemoView.java
 */

package com.meimeitech.hkdata.sdk.linux;

import com.meimeitech.hkdata.handle.DataHandler;
import com.meimeitech.hkdata.model.CardRecord;
import com.meimeitech.hkdata.sdk.MonitorClockHandler;
import com.meimeitech.hkdata.util.SpringContextUtil;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.Date;


/**
 * The application's main frame.
 */
public class MonitorClockHandlerLinux implements ApplicationListener<ContextRefreshedEvent>, DisposableBean, MonitorClockHandler {
    HCNetSDK hCNetSDK = null;
    HCNetSDK.NET_DVR_USER_LOGIN_INFO m_strLoginInfo = new HCNetSDK.NET_DVR_USER_LOGIN_INFO();//设备登录信息
    HCNetSDK.NET_DVR_DEVICEINFO_V40 m_strDeviceInfo = new HCNetSDK.NET_DVR_DEVICEINFO_V40();//设备信息
    String m_sDeviceIP;//已登录设备的IP地址
    String m_sDevicePort;//已登录设备的PORT
    String m_sUsername;//设备用户名
    String m_sPassword;//设备密码
    String lib;
    NativeLong lUserID;//用户句柄
    NativeLong lAlarmHandle;//报警布防句柄
    NativeLong lListenHandle;//报警监听句柄

    FMSGCallBack fMSFCallBack;//报警回调函数实现


    public String getInfo() {
        return "ip=" + m_sDeviceIP + ",port=" + m_sDevicePort + ":";
    }

    public MonitorClockHandlerLinux(String m_sDeviceIP, String m_sDevicePort, String m_sUsername, String m_sPassword, String lib) {
        this.m_sDeviceIP = m_sDeviceIP;
        this.m_sDevicePort = m_sDevicePort;
        this.m_sUsername = m_sUsername;
        this.m_sPassword = m_sPassword;
        this.lib = lib;
        this.lUserID = new NativeLong(-1);
        this.lAlarmHandle = new NativeLong(-1);
        this.lListenHandle = new NativeLong(-1);
        this.fMSFCallBack = null;
        this.hCNetSDK = (HCNetSDK) Native.loadLibrary(lib+"libhcnetsdk.so", HCNetSDK.class);

        String strPathCom = this.lib;
        HCNetSDK.NET_DVR_LOCAL_SDK_PATH struComPath = new HCNetSDK.NET_DVR_LOCAL_SDK_PATH();
        System.arraycopy(strPathCom.getBytes(), 0, struComPath.sPath, 0, strPathCom.length());
        struComPath.write();
        hCNetSDK.NET_DVR_SetSDKInitCfg(2, struComPath.getPointer());

        //设置libcrypto.so所在路径
        HCNetSDK.NET_DVR_LOCAL_SDK_PATH ptrByteArrayCrypto = new HCNetSDK.NET_DVR_LOCAL_SDK_PATH();
        String strPathCrypto = strPathCom + "/libcrypto.so";
        System.arraycopy(strPathCrypto.getBytes(), 0, struComPath.sPath, 0, strPathCrypto.length());
        ptrByteArrayCrypto.write();
        hCNetSDK.NET_DVR_SetSDKInitCfg(3, ptrByteArrayCrypto.getPointer());

        //设置libssl.so所在路径
        HCNetSDK.NET_DVR_LOCAL_SDK_PATH ptrByteArraySsl = new HCNetSDK.NET_DVR_LOCAL_SDK_PATH();
        String strPathSsl = strPathCom + "/libssl.so";
        System.arraycopy(strPathSsl.getBytes(), 0, struComPath.sPath, 0, strPathSsl.length());
        ptrByteArraySsl.write();
        hCNetSDK.NET_DVR_SetSDKInitCfg(4, ptrByteArraySsl.getPointer());

        boolean initSuc = hCNetSDK.NET_DVR_Init();
        if (initSuc != true) {
            System.out.println(getInfo() + "初始化失败");
        }

    }

    @Override
    public void destroy() throws Exception {
        stop();
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        start();
    }

    @Override
    public void stop() {
        logout();
    }

    @Override
    public void start() {
        if (init()) {
            setupAlarmChan();
        }

    }

    public void alarmDataHandle(NativeLong command, HCNetSDK.NET_DVR_ALARMER pAlarmer, HCNetSDK.RECV_ALARM palarmInfo) {
        switch (command.intValue()) {
            case HCNetSDK.COMM_ALARM_ACS:
                HCNetSDK.NET_DVR_ACS_ALARM_INFO strACSInfo = new HCNetSDK.NET_DVR_ACS_ALARM_INFO();
                strACSInfo.write();
                Pointer pACSInfo = strACSInfo.getPointer();
                pACSInfo.write(0, palarmInfo.RecvBuffer, 0, strACSInfo.size());
                strACSInfo.read();

                CardRecord cardRecord = CardRecord.builder().byCardNo(new String(strACSInfo.struAcsEventInfo.byCardNo).trim())
                        .byCardType(strACSInfo.struAcsEventInfo.byCardType)
                        .sourceIp(new String(pAlarmer.sDeviceIP).split("\0", 2)[0])
                        .dwMajor(strACSInfo.dwMajor)
                        .dwMinor(strACSInfo.dwMinor)
                        .cardTime(strACSInfo.struTime.getDate())
                        .createTime(new Date()).build();
                if (cardRecord.getByCardType() != 0) {
                    SpringContextUtil.getBean(DataHandler.class).put(cardRecord);
                    System.out.println(":commandID=" + command.intValue() + " " + "打卡数据:" + cardRecord);
                }
                break;
            default:
                break;
        }

    }


    public class FMSGCallBack implements HCNetSDK.FMSGCallBack {
        @Override
        public void invoke(NativeLong lCommand, HCNetSDK.NET_DVR_ALARMER pAlarmer, HCNetSDK.RECV_ALARM pAlarmInfo,
                           int dwBufLen, Pointer pUser) {
            alarmDataHandle(lCommand, pAlarmer, pAlarmInfo);
        }
    }

    private boolean init() {//GEN-FIRST:event_jButtonLoginActionPerformed
        //注册之前先注销已注册的用户,预览情况下不可注销
        if (lUserID.intValue() > -1) {
            //先注销
            hCNetSDK.NET_DVR_Logout(lUserID);
            lUserID = new NativeLong(-1);
        }

        //注册
        m_strLoginInfo.sDeviceAddress = new byte[HCNetSDK.NET_DVR_DEV_ADDRESS_MAX_LEN];
        System.arraycopy(m_sDeviceIP.getBytes(), 0, m_strLoginInfo.sDeviceAddress, 0, m_sDeviceIP.length());

        m_strLoginInfo.sUserName = new byte[HCNetSDK.NET_DVR_LOGIN_USERNAME_MAX_LEN];
        System.arraycopy(m_sUsername.getBytes(), 0, m_strLoginInfo.sUserName, 0, m_sUsername.length());

        m_strLoginInfo.sPassword = new byte[HCNetSDK.NET_DVR_LOGIN_PASSWD_MAX_LEN];
        System.arraycopy(m_sPassword.getBytes(), 0, m_strLoginInfo.sPassword, 0, m_sPassword.length());

        m_strLoginInfo.wPort = (short) Integer.parseInt(m_sDevicePort);

        m_strLoginInfo.bUseAsynLogin = 0; //是否异步登录：0- 否，1- 是
        m_strLoginInfo.write();
        lUserID = hCNetSDK.NET_DVR_Login_V40(m_strLoginInfo.getPointer(), m_strDeviceInfo.getPointer());

        if (lUserID.intValue() == -1) {
            System.out.println(getInfo() + "注册失败，错误号:" + hCNetSDK.NET_DVR_GetLastError());
            return false;
        } else {
            System.out.println(getInfo() + "注册成功");
            return true;
        }
    }

    public void logout() {
        //报警撤防
        if (lAlarmHandle.intValue() > -1) {
            if (!hCNetSDK.NET_DVR_CloseAlarmChan_V30(lAlarmHandle)) {
                System.out.println(getInfo() + "撤防失败，错误号:" + hCNetSDK.NET_DVR_GetLastError());
            } else {
                lAlarmHandle = new NativeLong(-1);
                System.out.println(getInfo() + "撤防成功");
            }
        }

        //注销
        if (lUserID.intValue() > -1) {
            if (hCNetSDK.NET_DVR_Logout(lUserID)) {
                System.out.println(getInfo() + "注销成功");
                lUserID = new NativeLong(-1);
            } else {
                System.out.println(getInfo() + "注销失败，错误号:" + hCNetSDK.NET_DVR_GetLastError());
            }
        }
        hCNetSDK.NET_DVR_Cleanup();
    }

    public void setupAlarmChan() {
        if (lUserID.intValue() == -1) {
            System.out.println(getInfo() + "请先注册");
            return;
        }
        if (lAlarmHandle.intValue() < 0)//尚未布防,需要布防
        {
            if (fMSFCallBack == null) {
                fMSFCallBack = new FMSGCallBack();
                Pointer pUser = null;
                if (!hCNetSDK.NET_DVR_SetDVRMessageCallBack_V30(fMSFCallBack, pUser)) {
                    System.out.println(getInfo() + "设置回调函数失败，错误号:" + hCNetSDK.NET_DVR_GetLastError());
                    return;
                }
            }
            HCNetSDK.NET_DVR_SETUPALARM_PARAM m_strAlarmInfo = new HCNetSDK.NET_DVR_SETUPALARM_PARAM();
            m_strAlarmInfo.dwSize = m_strAlarmInfo.size();
            m_strAlarmInfo.byLevel = 1;//智能交通布防优先级：0- 一等级（高），1- 二等级（中），2- 三等级（低）
            m_strAlarmInfo.byAlarmInfoType = 1;//智能交通报警信息上传类型：0- 老报警信息（NET_DVR_PLATE_RESULT），1- 新报警信息(NET_ITS_PLATE_RESULT)
            m_strAlarmInfo.byDeployType = 1; //布防类型(仅针对门禁主机、人证设备)：0-客户端布防(会断网续传)，1-实时布防(只上传实时数据)
            m_strAlarmInfo.write();
            lAlarmHandle = hCNetSDK.NET_DVR_SetupAlarmChan_V41(lUserID, m_strAlarmInfo);
            if (lAlarmHandle.intValue() == -1) {
                System.out.println(getInfo() + "布防失败，错误号:" + hCNetSDK.NET_DVR_GetLastError());
            } else {
                System.out.println(getInfo() + "布防成功");
            }
        }
    }

    public void CloseAlarmChan() {
        //报警撤防
        if (lAlarmHandle.intValue() > -1) {
            if (hCNetSDK.NET_DVR_CloseAlarmChan_V30(lAlarmHandle)) {
                System.out.println(getInfo() + "撤防成功");
                lAlarmHandle = new NativeLong(-1);
            }
        }
    }
}


