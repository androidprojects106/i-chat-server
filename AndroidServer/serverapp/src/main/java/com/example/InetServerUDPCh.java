package com.example;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import sun.rmi.runtime.Log;

/**
 * Created by LPC-Home1 on 3/29/2015.
 */
class InetServerUDPCh extends DatagramSocket {

    private String _serverAddress;
    private int _serverUdpPort;
    private DatagramPacket packetIn = null, packetOut = null;

    public InetServerUDPCh(String _address, int _port) throws IOException {
        super(_port);

        _serverAddress = _address;
        _serverUdpPort = _port;
    }

    public void close() {
        packetIn = null;
        packetOut = null;

        super.close();
    }

    public boolean sendDataToClient(final byte[] dataToSend, int dataLength,
                                    String addrClient, int portClient)
    {
        InetAddress _inetAddress = null;
        try {
            _inetAddress = InetAddress.getByName(addrClient);
            if (_inetAddress != null) {
                packetOut = new DatagramPacket(dataToSend, dataLength, _inetAddress, portClient);
                try {
                    /* For debugging only *
                    byte[] bytes = new byte[AppProtData.APPPROTOCOLDATA_OVERHEADSIZE];
                    System.arraycopy(dataToSend, 0, bytes, 0, AppProtData.APPPROTOCOLDATA_OVERHEADSIZE);
                    int sessionId = (bytes[0] & 0xFF) | (bytes[1] & 0xFF) << 8 | (bytes[2] & 0xFF) << 16 | bytes[3] << 24;
                    short seqNum = (short)((short)bytes[4] | ((short)bytes[5])<<8);
                    if (Constants.DEBUGGING)
                        LogRecorder.writeStringToFile("sessionId: "+sessionId +
                            " SeqNum: " +seqNum, "testlog.bin", "=> ");
                    * For debugging only */
                    send(packetOut);
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
                return true;
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return false;
    }

    public DatagramPacket readDataFromClient (byte[] byteBuf) {
        packetIn = new DatagramPacket(byteBuf, byteBuf.length);
        try {
            receive(packetIn);
            /* For debugging only *
            byte[] bytes = new byte[AppProtData.APPPROTOCOLDATA_OVERHEADSIZE];
            System.arraycopy(packetIn.getData(), 0, bytes, 0, AppProtData.APPPROTOCOLDATA_OVERHEADSIZE);
            int sessionId = (bytes[0] & 0xFF) | (bytes[1] & 0xFF) << 8 | (bytes[2] & 0xFF) << 16 | bytes[3] << 24;
            short seqNum = (short)((short)bytes[4] | ((short)bytes[5])<<8);
            if (Constants.DEBUGGING)
                    LogRecorder.writeStringToFile("sessionId: "+sessionId +
                        " SeqNum: " +seqNum, "testlog.bin", "<= ");
            * For debugging only */
            return packetIn;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}