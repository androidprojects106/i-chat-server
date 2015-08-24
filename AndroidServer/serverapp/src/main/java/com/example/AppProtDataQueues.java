package com.example;

/**
 * Created by LPC-Home1 on 3/29/2015.
 */

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import static java.lang.Thread.sleep;

class AppProtDataQueues {
    public static final int MAX_UDPTRANSMITPDU_SIZE = 1400;        // (1472-60) octets
    public static final int MAX_UDPRECEIVEPDU_SIZE = 1500;        // (1472-60) octets
    public final static int MAX_DUPLICATERECORDDIZE = 16;


    String serverAddr =null;
    int serverUdpPort =8081;
    private volatile boolean listening;
    InetServerUDPCh dataChannel =null;
    private static Semaphore semaphore;
    // used as a Mutex to protect the ActiveReceiveQue
    // accessed by both readMsgFromReceiveQue and inputFromCtrlChannel

    private static ConcurrentHashMap<String, UdpAddrPort> appClientMap;
    private static ConcurrentHashMap<String, LinkedList<AppProtData>> appDataQueMap;
    // public static LinkedList<AppProtData> appDataReceiveQue;
    public static LinkedList<AppProtData> appDataDuplicateReceiveQue;

    public AppProtDataQueues(String ipAddr, int udpPort) {
        serverAddr =ipAddr;
        serverUdpPort =udpPort;
        semaphore = new Semaphore(1, true);     // Mutex only (1 resource) and "fair" =true
        appClientMap= new ConcurrentHashMap<String, UdpAddrPort>();
        appDataQueMap = new ConcurrentHashMap<String, LinkedList<AppProtData>>();
        // appDataReceiveQue = new LinkedList<AppProtData>();
        appDataDuplicateReceiveQue = new LinkedList<AppProtData>();

        try {
            dataChannel = new InetServerUDPCh(serverAddr, serverUdpPort);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    listening = true;

                    while (listening) {
                        // Wait for until TCP Socket is established
                        if (null == dataChannel) {
                            try { sleep(500); }
                            catch (InterruptedException e) { e.printStackTrace(); }
                        }
                        else {
                            boolean workDone = inputFromDataChannel(dataChannel);
                            if (workDone) {      // Nothing to do in msg queue for now
                                try { sleep(200); }
                                catch (InterruptedException e) { e.printStackTrace(); }
                            }
                        }
                    }
                }
            }).start();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ConcurrentHashMap<String, LinkedList<AppProtData>> getAppDataQueMap() {
        return appDataQueMap;
    }


    public boolean checkUdpTriggerFromClient(int sessionId) {
        AppProtData appData = readDataFromServQue(sessionId);
        boolean result = false;

        if (null != appData) {
            byte[] bData = AppProtData.UDP_TRIGGERSIGNATURE;
            // signature 7 bytes to trigger the UDP datagram socket from the client
            switch (appData.getDataType()) {
                case AppProtData.UDPTYPE_PTTCHATFINISH:
                case AppProtData.UDPTYPE_PTTFLOORPROTECTED:
                case AppProtData.UDPTYPE_PTTFLOORAVAILABLE:
                case AppProtData.UDPTYPE_FLOORCATCH:
                case AppProtData.UDPTYPE_FLOORRELEASE:
                case AppProtData.UDPTYPE_TRIGGER: {
                    result = true;
                    break;
                }
                case AppProtData.UDPTYPE_AUDIOMSG_PCM16:
                case AppProtData.UDPTYPE_AUDIOVOICE_PCM16:
                case AppProtData.UDPTYPE_VIDEOMSG_H264: {
                    return false;
                }
            }
            return (result && appData.getDataSize()==bData.length)
                    && Arrays.equals(appData.getData(), bData);
        }
        else
            return false;
    }

    public LinkedList<AppProtData> getAppDataQue(int sessionId) {
        String key = Integer.toString(sessionId);

        return appDataQueMap.get(key);
    }

    public LinkedList<AppProtData> hashDataQueMap(int sessionId, AppProtData appData) {
        String key = Integer.toString(sessionId);
        LinkedList<AppProtData> dataQue= appDataQueMap.get(key);

        if (dataQue!=null) {
            dataQue.addLast(appData);
        }
        else {
            dataQue = new LinkedList<AppProtData>();
            dataQue.add(appData);
            appDataQueMap.put(key, dataQue);
        }

        return dataQue;
    }

    /*
     Generate a new session id that can uniquely identify the UDP data session
     so that the InetServerUDPCh can associate with this InetTcpServer control
     Socket/Channel; for now a simple prototype is in place. Future work to check
     for uniqueness with existing session Ids
     */
    public int createMediaSession() {
        Random rn = new Random();
        int result = rn.nextInt((int) (System.currentTimeMillis() % 0x7fffffff)) + 1;

        while (!isValidSession(result)) {
            result = rn.nextInt((int) (System.currentTimeMillis() % 0x7fffffff)) + 1;
        }
        return result;
    }

    public boolean isValidSession(int sessionId) {
        boolean isValid = getAppDataQue(sessionId) == null;

        return sessionId != 0 && isValid;
    }

    public UdpAddrPort getClientMap(int sessionId) {
        return appClientMap.get(Integer.toString(sessionId));
    }

    public UdpAddrPort hashClientMap(int sessionId, String addr, int port) {
        String key = Integer.toString(sessionId);

        return appClientMap.putIfAbsent(key,new UdpAddrPort(addr, port));
    }

    /*
     Add an AppData (LiteRTP/UDP) to the appDataSendQue at the tail-end
     executed by the Call Processing thread
    */
    public void sendDataToClient(int sessionId, AppProtData data) {
        byte[] bytesToSend =data.composeUdpPacket();
        UdpAddrPort addrport =getClientMap(sessionId);

        if (addrport !=null) {
            dataChannel.sendDataToClient(bytesToSend, bytesToSend.length,
                    addrport.getUdpAddr(), addrport.getUdpPort());
            if (Constants.DEBUGGING) {
                LogRecorder.writeStringToFile("sessionId: " + data.getSessionId()
                        + " SeqNum: " + Short.toString(data.getSeqNum())
                        + " Raw Len: " + bytesToSend.length, "testlog.bin", "=> ");
            }
        }
    }

    /*
      Input data packet for socket communications. Executed by
      the CtrlChannel socket communications thread
      return true if any msg queue work is done
     */

    public boolean inputFromDataChannel(InetServerUDPCh dataChannel)
    {
        // input from socket and place to active receive (queue) tail
        byte[] bytesIn = new byte[MAX_UDPRECEIVEPDU_SIZE];
        DatagramPacket packetIn =dataChannel.readDataFromClient(bytesIn);
        if (packetIn ==null) {
            return true;     // work done (no more packets - at least for now)
        }
        else {
            AppProtData appData =new AppProtData(packetIn.getData(), packetIn.getLength());
            if (Constants.DEBUGGING) {
                LogRecorder.writeStringToFile("sessionId: " + appData.getSessionId()
                        + " SeqNum: " + Short.toString(appData.getSeqNum())
                        + " Raw Len: " + packetIn.getLength(), "testlog.bin", "<= ");
            }
            if (appData.isValid() && !appDataDuplicateReceiveQue.contains(appData)) {
                int sessionId =appData.getSessionId();
                UdpAddrPort addrport =getClientMap(sessionId);

                if (addrport==null) {
                            // client (Addr and Port) not yet in map
                    InetAddress inetAddress =packetIn.getAddress();
                    int portClient =packetIn.getPort();
                    if (inetAddress!= null) {
                        String addrClient = inetAddress.getHostAddress();
                        if (!(inetAddress instanceof Inet4Address &&
                                addrClient.length() < 18)) {
                            // IPv6 address
                            addrClient = addrClient.toUpperCase();
                            int delim = addrClient.indexOf('%'); // drop IPv6 port suffix
                            addrClient = delim < 0 ? addrClient : addrClient.substring(0, delim);
                        }       // else it is IPv4 address
                        hashClientMap(sessionId, addrClient, portClient);
                    }
                }
                try {
                    semaphore.acquire();
                    LinkedList<AppProtData> appDataReceiveQue = hashDataQueMap(sessionId, appData);
                    semaphore.release();
                    appDataDuplicateReceiveQue.addLast(appData);
                    if (appDataDuplicateReceiveQue.size()>MAX_DUPLICATERECORDDIZE)
                        appDataDuplicateReceiveQue.remove();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return false;    // work not done yet - more packets in udp socket
        }
    }

    /*
     Check there is AppData (LiteRTP/UDP) in the appDataReceiveQue at the head
     of the queue (but do not extract it)
     executed by the Call Processing thread
    */
    public AppProtData checkDataFromServQue(int sessionId) {
        return checkDataFromServQue(sessionId, AppProtData.UDPTYPE_AUDIOMSG_PCM16);
    }

    public AppProtData checkDataFromServQue(int sessionId, final byte dataType) {
        LinkedList<AppProtData> appDataReceiveQue = getAppDataQue(sessionId);
        AppProtData result = null;

        if (null != appDataReceiveQue) {
            try {
                semaphore.acquire();
                AppProtData appData = appDataReceiveQue.getFirst();
                if (null != appData && dataType == appData.getDataType())
                    result = appData;
                semaphore.release();
                return result;
            } catch (InterruptedException e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    /*
     Extract an AppData (LiteRTP/UDP) from the appDataReceiveQue at the head
     of the queue
     executed by the Call Processing thread
    */
    public AppProtData readDataFromServQue(int sessionId) {
        AppProtData appData = null;

        try {
            semaphore.acquire();
            LinkedList<AppProtData> appDataReceiveQue =getAppDataQue(sessionId);
            if (appDataReceiveQue!= null && !appDataReceiveQue.isEmpty())
                appData = appDataReceiveQue.removeFirst();
            semaphore.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return appData;
    }

    /*
     Run init() in order to send to and listen to the server for the data
     (or media: voice, video, text) in both directions
     */
    private class udpListener implements Runnable {
        @Override
        public void run() {
            // TCP Socket opened in separate thread

            try {
                dataChannel = new InetServerUDPCh(serverAddr,serverUdpPort);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    protected class UdpAddrPort {
        String udpAddr;
        int udpPort;

        protected UdpAddrPort(String addr, int port) {
            udpAddr = addr;
            udpPort =port;
        }

        protected String getUdpAddr() {return udpAddr; }
        protected void setUdpAddr(String addr) {udpAddr = addr; }
        protected int getUdpPort() {return udpPort; }
        protected void setUdpPort(int port) {udpPort = port; }
    }



    public boolean sendVoiceMediaDataToClientbyMem(int mediaSessionIn, int mediaSessionOut) {
        AppProtData appDataOut, appDataIn = readDataFromServQue(mediaSessionIn);

        boolean result=false;
        while (null != appDataIn) {
            result =true;
            appDataOut = new AppProtData(appDataIn.getSeqNum(), appDataIn.getDataType(),
                    appDataIn.getData(), appDataIn.getDataSize(), mediaSessionOut);
            sendDataToClient(mediaSessionOut, appDataOut);

            appDataIn = readDataFromServQue(mediaSessionIn);
        }
        return result;
    }

    public void sendVoiceMediaDataToClientbyRecord(String userId,
                                                   int sessionIdIn, int sessionIdOut) {
        BufferedInputStream bufInStream = null;
        FileInputStream fis = null;
        String fileNameSessionId = LogRecorder.fileMediaFormat(userId, sessionIdIn);
        try {
            AppProtData appData =null;
            short dataSeqNum = 0;
            byte dataType = AppProtData.UDPTYPE_AUDIOMSG_PCM16;

            fis = new FileInputStream(fileNameSessionId);
            bufInStream = new BufferedInputStream(fis,8*1024);

            byte[] bytes = new byte[AppProtDataQueues.MAX_UDPTRANSMITPDU_SIZE];
            int bytesRead = bufInStream.read(bytes, 0, bytes.length);
            while (bytesRead >0) {       // !=-1 (end of file)
                appData = new AppProtData(dataSeqNum, dataType, bytes, bytesRead, sessionIdOut);
                sendDataToClient(sessionIdOut, appData);
                dataSeqNum = UtilsHelpers.incAppSeqNum(dataSeqNum);

                bytesRead = bufInStream.read(bytes, 0, bytes.length);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (fis!=null) {
                    fis.close();
                    fis =null;
                }
                if (bufInStream!=null) {
                    bufInStream.close();
                    bufInStream =null;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
