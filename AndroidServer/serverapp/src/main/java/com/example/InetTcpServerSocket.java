package com.example;

/**
 * Created by LPC-Home1 on 4/1/2015.
 */

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class InetTcpServerSocket extends ServerSocket {

    public final static int MAX_TCPBLOCKSIZE = 128;
    // maximum number of pending connections on the socket
    public static final int MAX_IPPDUSIZE = 1428;

    public InetTcpServerSocket(String addr, int port) throws IOException {
        super(port, MAX_TCPBLOCKSIZE, InetAddress.getByName(addr));
    }

    public InetTcpSocketConn initSocketAccept() {
        Socket _socket =null;
        String addrClient =null;
        int portClient =0;
        DataOutputStream outToClient =null;
        BufferedReader inFromClient =null;

        if (isBound()) {
            try {
                _socket = accept();
                addrClient = _socket.getInetAddress().getHostAddress();
                portClient = _socket.getPort();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                outToClient = new DataOutputStream(_socket.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
                outToClient = null;
            }
            try {
                inFromClient = new BufferedReader(new
                        InputStreamReader(_socket.getInputStream(), "UTF-8"), MAX_IPPDUSIZE);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                inFromClient = null;
            } catch (IOException e) {
                e.printStackTrace();
                inFromClient = null;
            }
        }
        if (_socket !=null
                && outToClient!=null
                && inFromClient!=null ) {
            InetTcpSocketConn connect = new InetTcpSocketConn(_socket, addrClient, portClient, outToClient, inFromClient);
            return connect;
        }
        else {
            if (_socket !=null) try {
                _socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
