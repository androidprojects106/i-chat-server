package com.example;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

/**
 * Created by LPC-Home1 on 4/1/2015.
 */
public class InetTcpSocketConn extends Socket {
    Socket _socket;
    String _addrClient;
    int _portClient;
    private DataOutputStream outToClient;       // Network output Stream
    private BufferedReader inFromClient;        // Network Input Stream

    public InetTcpSocketConn(Socket socket, String addr, int port, DataOutputStream dos, BufferedReader ifc) {
        _socket = socket;
        _addrClient = addr;
        _portClient = port;
        outToClient= dos;
        inFromClient =ifc;
    }

    public boolean isBound() { return _socket.isBound(); }
    public boolean isClosed() { return _socket.isClosed(); }
    public boolean isConnected() { return _socket.isConnected(); }

    public void close() {
        try {
            inFromClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            outToClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (_socket !=null && !_socket.isClosed())
            close();
    }

    public boolean sendMsgStringToClient(final String msgToSend) {
        if (outToClient==null)
            return false;
        try {
            outToClient.writeBytes(msgToSend + "\n");
            if (Constants.DEBUGGING)
                LogRecorder.writeStringToFile(msgToSend, "testlog.bin", "=> ");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public String readMsgStringFromClient() {
        InputStream clientInputStream = null;
        try {
            // clientInputStream = _socket.getInputStream();
            String packetString = null;
            packetString = inFromClient.readLine();
            if (packetString != null && packetString.compareTo("") !=0) {
                if (Constants.DEBUGGING)
                    LogRecorder.writeStringToFile(packetString, "testlog.bin", "<= ");
                return packetString;
            }
        } catch (IOException e) {
            if (Constants.DEBUGGING) {
                e.printStackTrace();
            }
            return null;
        }
        return null;
    }

    public Socket getSocket() {return _socket; }
    public void setSocket(Socket s) {_socket =s; }
    public String getAddrClient() {return _addrClient; }
    public void setAddrClient(String addr) {_addrClient =addr; }
    public int getPortClient() {return _portClient; }
    public void setPortClient(int port) {_portClient =port; }
    public DataOutputStream getDataOutputStream() {return outToClient; }
    public void setDataOutputStream(DataOutputStream dos) {outToClient =dos; }
    public BufferedReader getBufferedReader() {return inFromClient; }
    public void setBufferedReader(BufferedReader ifc) {inFromClient =ifc; }

}
