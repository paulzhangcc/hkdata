package com.meimeitech.hkdata.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;

/**
 * @author paul
 * @description
 * @date 2019/5/21
 */
public class InetUtils {
    private final static Logger logger = LoggerFactory.getLogger(InetUtils.class);

    public static String findFirstNonLoopbackAddress() {
        InetAddress result = null;
        try {
            int lowest = Integer.MAX_VALUE;
            for (Enumeration<NetworkInterface> nics = NetworkInterface
                    .getNetworkInterfaces(); nics.hasMoreElements(); ) {
                NetworkInterface ifc = nics.nextElement();
                if (ifc.isUp()) {
                    logger.trace("Testing interface: " + ifc.getDisplayName());
                    if (ifc.getIndex() < lowest || result == null) {
                        lowest = ifc.getIndex();
                    } else if (result != null) {
                        continue;
                    }
                }
            }
        } catch (IOException ex) {
            logger.error("Cannot get first non-loopback address", ex);
        }

        if (result != null) {
            return result.getHostAddress();
        }

        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            logger.warn("Unable to retrieve localhost");
        }
        return null;
    }
}

