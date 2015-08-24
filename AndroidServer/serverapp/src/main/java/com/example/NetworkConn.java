package com.example;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Created by LPC-Home1 on 4/3/2015.
 */
public final class NetworkConn {
    public static final int NETWORK_ANY =0;                 // constants for network Ids
    public static final int NETWORK_WIFI =1;
    public static final int NETWORK_MOBILE =2;

    public static final int ADDRESS_IPv4=0;
    public static final int ADDRESS_IPv6=1;

    private NetworkConn() {}

    // For now we are working with IPv4 addresses only
    public static String getLocalIpAddress(int useIPver) {
        String address= null;
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        address = inetAddress.getHostAddress().toUpperCase();
                        if (inetAddress instanceof Inet4Address &&
                                address.length() < 18) {
                            if (useIPver == ADDRESS_IPv4)
                                return inetAddress.getHostAddress();
                        }
                        else if (useIPver==ADDRESS_IPv6) {      // use IPv6
                            int delim = address.indexOf('%'); // drop IPv6 port suffix
                            return delim<0 ? address : address.substring(0, delim);
                        }
                    }
                }
            }
        } catch (SocketException ex) {
        }
        return null;
    }


}
