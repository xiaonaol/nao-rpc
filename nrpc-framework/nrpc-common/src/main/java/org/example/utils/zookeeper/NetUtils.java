package org.example.utils.zookeeper;

import lombok.extern.slf4j.Slf4j;
import org.example.exceptions.NetworkException;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * @author xiaonaol
 * @date 2024/10/28
 **/
@Slf4j
public class NetUtils {
    public static String getIp() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress inetAddress = addresses.nextElement();
                    // 检查是否是IPv4地址并且不是回环地址
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof java.net.Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
            throw new NetworkException();
        } catch (SocketException e) {
            log.error("获取局域网ip时发生异常：", e);
            throw new NetworkException(e);
        }
    }
}
