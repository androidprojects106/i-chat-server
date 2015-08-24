package com.example;

/**
 * Created by LPC-Home1 on 3/29/2015.
 */

import java.util.LinkedList;
import java.util.concurrent.Semaphore;
import static java.lang.Thread.sleep;
import java.lang.String;

public class AppProtMsgQueue {

    public final static int MAX_DUPLICATERECORDSIZE = 16;

    // track the last 16 non-duplicate messages from the server
    public static final int MAX_TIMEPOLLING = 100;
            // interval to listen to pages/messages from the server
    public final static int MAX_WAITFORTCPCHANNEL = 2000;

    private InetTcpSocketConn serverCtrlTcpChannel;
    private volatile boolean listening;
    private Semaphore semaphoreActiveClientQueue;
    private LinkedList<AppProtMsg> appMsgActiveClientReceiveQue;
    private LinkedList<AppProtMsg> appMsgDuplicateClientReceiveQue;
    private LinkedList<AppProtMsg> appMsgPendingClientReceiveQue;
            // Used as a Mutex to protect the ActiveClientReceiveQue
            // Accessed by both readMsgFromServQue and inputFromCtrlChannel
    private Semaphore semaphoreActiveServerQueue;
    private LinkedList<AppProtMsg> appMsgActiveServerReceiveQue;
    private LinkedList<AppProtMsg> appMsgDuplicateServerReceiveQue;
    private LinkedList<AppProtMsg> appMsgPendingServerReceiveQue;

    public AppProtMsgQueue(final InetTcpSocketConn socketConnection) {
        listening = true;
        serverCtrlTcpChannel =socketConnection;
        semaphoreActiveClientQueue = new Semaphore(1, true);     // Mutex only (1 resource) and "fair" =true
        appMsgActiveClientReceiveQue = new LinkedList<AppProtMsg>();
        appMsgDuplicateClientReceiveQue = new LinkedList<AppProtMsg>();
        appMsgPendingClientReceiveQue = new LinkedList<AppProtMsg>();
        semaphoreActiveServerQueue = new Semaphore(1, true);     // Mutex only (1 resource) and "fair" =true
        appMsgActiveServerReceiveQue = new LinkedList<AppProtMsg>();
        appMsgDuplicateServerReceiveQue = new LinkedList<AppProtMsg>();
        appMsgPendingServerReceiveQue = new LinkedList<AppProtMsg>();
    }

    /*
     Utilities
     */
    public Semaphore getSemaphoreActiveClientQueue() {return semaphoreActiveClientQueue;}
    public LinkedList<AppProtMsg> getAppMsgActiveClientReceiveQue() {return appMsgActiveClientReceiveQue;}
    public LinkedList<AppProtMsg> getAppMsgDuplicateClientReceiveQue() {return appMsgDuplicateClientReceiveQue;}
    public LinkedList<AppProtMsg> getAppMsgPendingClientReceiveQue() {return appMsgPendingClientReceiveQue;}
    public Semaphore getSemaphoreActiveServerQueue() {return semaphoreActiveServerQueue;}
    public LinkedList<AppProtMsg> getAppMsgActiveServerReceiveQue() {return appMsgActiveServerReceiveQue;}
    public LinkedList<AppProtMsg> getAppMsgDuplicateServerReceiveQue() {return appMsgDuplicateServerReceiveQue;}
    public LinkedList<AppProtMsg> getAppMsgPendingServertReceiveQue() {return appMsgPendingServerReceiveQue;}
    public InetTcpSocketConn getCtrlChannel() {
        return (serverCtrlTcpChannel);
    }

    /*
     Start listen to the server for incoming control msgs
    */
    public void executeListening() {
        listening =true;

        new Thread (new readingCtrlChannel())
                .start();
        // this thread is blocked when there is no incoming message
    }


    /*
      Input control for socket communications. Executed by
      the ctrlChannel socket communications thread
      return true if any msg queue work is done
     */
    class readingCtrlChannel implements Runnable {
        @Override
        public void run() {
            if (waitForTcpCtrlChannel()) {
                    // Wait until TCP Socket is established
                while (listening && isConnected()) {
                    // input from socket and place to active receive (queue) tail
                    String msgString =serverCtrlTcpChannel.readMsgStringFromClient();
                    if (msgString ==null) {
                        try { sleep(MAX_TIMEPOLLING); }
                        catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    else if (msgString.equals("*******END*")) {
                            if (serverCtrlTcpChannel!=null
                                    && serverCtrlTcpChannel.isConnected()
                                    && !serverCtrlTcpChannel.isClosed())
                                serverCtrlTcpChannel.sendMsgStringToClient("*******END*");
                            break;
                    }
                    else {
                        AppProtMsg appMsg = new AppProtMsg(msgString);
                        if (!appMsgDuplicateClientReceiveQue.contains(appMsg)) {
                            // the duplicate queue action is only executed by the
                            // control channel reading thread (i.e., a single
                            // thread, and not the call processing thread ) - there
                            // is no need for semaphore protection
                            try {
                                semaphoreActiveClientQueue.acquire();
                                appMsgActiveClientReceiveQue.addLast(appMsg);
                                semaphoreActiveClientQueue.release();
                                appMsgDuplicateClientReceiveQue.addLast(appMsg);
                                if (appMsgDuplicateClientReceiveQue.size() > MAX_DUPLICATERECORDSIZE)
                                    appMsgDuplicateClientReceiveQue.removeFirst();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
            if (serverCtrlTcpChannel!=null && serverCtrlTcpChannel.isConnected()
                    && !serverCtrlTcpChannel.isClosed()) {
                serverCtrlTcpChannel.close();
                serverCtrlTcpChannel =null;
            }
        }
    }

    public boolean waitForTcpCtrlChannel() {
        long timeStart =System.currentTimeMillis();
        long timePassed = System.currentTimeMillis()-timeStart;

        while (serverCtrlTcpChannel ==null && timePassed <=MAX_WAITFORTCPCHANNEL) {
            // Wait until UDP Socket is established
            try {
                sleep(200);
                timePassed = System.currentTimeMillis()-timeStart;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return (timePassed <=MAX_WAITFORTCPCHANNEL);
    }

    public boolean isConnected() {
        return (serverCtrlTcpChannel !=null
                && serverCtrlTcpChannel.isConnected()
                && !serverCtrlTcpChannel.isClosed());     // && serverCtrlTcpChannel.isBound()
    }

    public void clear() {
        appMsgActiveClientReceiveQue.clear();
        appMsgDuplicateClientReceiveQue.clear();
        appMsgPendingClientReceiveQue.clear();
        appMsgActiveServerReceiveQue.clear();
        appMsgDuplicateServerReceiveQue.clear();
        appMsgPendingServerReceiveQue.clear();
    }

    public void close() {
        listening =false;

        try { sleep(2000); } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (serverCtrlTcpChannel !=null
               && serverCtrlTcpChannel.isConnected()
               && !serverCtrlTcpChannel.isClosed()) {
            serverCtrlTcpChannel.close();
            serverCtrlTcpChannel =null;
        }
    }

    /*
     Add an AppProtocolMsg to the target user (who are in the same server HashMap) at the tail
     end, executed by the call processing thread
     */
    public boolean sendMsgToServerTargetUsers(AppUserState appUserHashMap,
                            String idSrc, String idDstTarget,short seqNum,byte msgType) {
        AppProtMsgQueue targetMsgQueue =appUserHashMap.getActiveUser(idDstTarget);
        if (targetMsgQueue ==null)
            return false;
        else {
            AppProtMsg appMsg = new AppProtMsg(seqNum,msgType,idSrc,idDstTarget);
            if (Constants.DEBUGGING) {
                String msgDebug = appMsg.composeProtocolMsg();
                LogRecorder.writeStringToFile(msgDebug, "testlog.bin", ">> ");
            }
            sendMsgtoServerTaget(targetMsgQueue, appMsg);
            return true;
        }
    }

    public boolean sendMsgToServerTargetUsers(AppUserState appUserHashMap,
                        String idSrc,String idDstTarget,short seqNum,byte msgType,
                        int[] cmList, String[] cmInfo) {
        AppProtMsgQueue targetMsgQueue =appUserHashMap.getActiveUser(idDstTarget);
        if (targetMsgQueue ==null)
            return false;
        else {
            AppProtMsg appMsg = new AppProtMsg(seqNum,msgType,idSrc,idDstTarget,cmList,cmInfo);
            if (Constants.DEBUGGING) {
                String msgDebug = appMsg.composeProtocolMsg();
                LogRecorder.writeStringToFile(msgDebug, "testlog.bin", ">> ");
            }
            sendMsgtoServerTaget(targetMsgQueue, appMsg);

            return true;
        }
    }

    public boolean sendMsgToServerTargetUsers(AppUserState appUserHashMap,
                    String idSrc,String idDstTarget,short seqNum,byte msgType,String msgString) {
        AppProtMsgQueue targetMsgQueue =appUserHashMap.getActiveUser(idDstTarget);
        if (targetMsgQueue ==null)
            return false;
        else {
            AppProtMsg appMsg = new AppProtMsg(seqNum,msgType,idSrc,idDstTarget,msgString);
            if (Constants.DEBUGGING) {
                String msgDebug = appMsg.composeProtocolMsg(msgString);
                LogRecorder.writeStringToFile(msgDebug, "testlog.bin", ">> ");
            }
            sendMsgtoServerTaget(targetMsgQueue, appMsg);

            return true;
        }
    }


    public boolean sendMsgToServerTargetUsers(AppUserState appUserHashMap,
                    String idSrc,String idDstTarget,short seqNum,byte msgType,int sessionId) {
        AppProtMsgQueue targetMsgQueue =appUserHashMap.getActiveUser(idDstTarget);
        if (targetMsgQueue ==null)
            return false;
        else {
            // AppProtMsg appMsg = new AppProtMsg(seqNum,msgType,srcId,targetId,sessionId);
            AppProtMsg appMsg = new AppProtMsg(seqNum,msgType,idSrc,idDstTarget,sessionId);
            if (Constants.DEBUGGING) {
                String msgDebug = appMsg.composeProtocolMsg(sessionId);
                LogRecorder.writeStringToFile(msgDebug, "testlog.bin", ">> ");
            }
            sendMsgtoServerTaget(targetMsgQueue, appMsg);

            return true;
        }
    }

    private void sendMsgtoServerTaget(AppProtMsgQueue targetMsgQueue, AppProtMsg appMsg) {
        if (!targetMsgQueue.getAppMsgDuplicateServerReceiveQue().contains(appMsg)) {
            // the duplicate queue action is only executed by the
            // control channel reading thread (i.e., a single
            // thread, and not the call processing thread ) - there
            // is no need for semaphore protection
            try {
                targetMsgQueue.getSemaphoreActiveServerQueue().acquire();
                targetMsgQueue.getAppMsgActiveServerReceiveQue().addLast(appMsg);
                targetMsgQueue.getSemaphoreActiveServerQueue().release();

                targetMsgQueue.getAppMsgDuplicateServerReceiveQue().addLast(appMsg);
                if (targetMsgQueue.getAppMsgDuplicateServerReceiveQue().size() > MAX_DUPLICATERECORDSIZE)
                    targetMsgQueue.getAppMsgDuplicateServerReceiveQue().removeFirst();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /*
    Extract an AppProtocolMsg from the appMsgActiveServerReceiveQue at the head
    of the queue
    executed by the Call Processing thread
    */
    public AppProtMsg readMsgFromServerReceiveQue() {
                // read from the current received queue only
        AppProtMsg appMsg = null;

        try {
            semaphoreActiveServerQueue.acquire();
            if (appMsgActiveServerReceiveQue !=null && !appMsgActiveServerReceiveQue.isEmpty()) {
                appMsg = appMsgActiveServerReceiveQue.removeFirst();
                if (Constants.DEBUGGING) {
                    String msgDebug = appMsg.composeProtocolMsg();
                    LogRecorder.writeStringToFile(msgDebug, "testlog.bin", "<< ");
                }
            }
            semaphoreActiveServerQueue.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return appMsg;
    }

    public AppProtMsg readMsgFromServerPendedQue() {
            // read from the pended queue only
        AppProtMsg appMsg = null;
        if (appMsgPendingServerReceiveQue !=null && !appMsgPendingServerReceiveQue.isEmpty()) {
            appMsg = appMsgPendingServerReceiveQue.removeFirst();
            if (Constants.DEBUGGING) {
                String msgDebug = appMsg.composeProtocolMsg();
                LogRecorder.writeStringToFile(msgDebug, "testlog.bin", "<< ");
            }
        }
        return appMsg;
    }

    /*
     Add an AppProtocolMsg message to the appMsgActiveSendQue at the tail-end
     executed by the Call Processing thread
    */
    public boolean sendMsgToClient(short SeqNum, byte MsgType, String idSource, String idDstUser) {
        AppProtMsg appMsg = new AppProtMsg(SeqNum,MsgType,idSource,idDstUser);
        String msgString = appMsg.composeProtocolMsg();
        // primitive messages are composed
        if (serverCtrlTcpChannel !=null && msgString!=null) {
            serverCtrlTcpChannel.sendMsgStringToClient(msgString);
            return true;
        }
        else
            return false;
    }

    public boolean sendMsgToClient(short SeqNum, byte MsgType,
                                   String idSource, String idDstUser, int sessionId) {
        AppProtMsg appMsg = new AppProtMsg(SeqNum,MsgType,idSource,idDstUser,sessionId);
        String msgString = appMsg.composeProtocolMsg();
        // piggybacked text message are composed
        if (serverCtrlTcpChannel !=null && msgString!=null) {
            serverCtrlTcpChannel.sendMsgStringToClient(msgString);
            return true;
        }
        else
            return false;
    }

    public boolean sendMsgToClient(short SeqNum, byte MsgType,
                                   String idSource, String idDstUser, String msgText) {
        AppProtMsg appMsg = new AppProtMsg(SeqNum,MsgType,idSource,idDstUser);
        String msgString = appMsg.composeProtocolMsg(msgText);
        // piggybacked text message are composed
        if (serverCtrlTcpChannel !=null && msgString!=null) {
            serverCtrlTcpChannel.sendMsgStringToClient(msgString);
            return true;
        }
        else
            return false;
    }

    public boolean sendMsgToClient(short SeqNum, byte MsgType,String idSource,String idDstUser,
                                   int[] cmList, String[] cmInfo) {
        AppProtMsg appMsg = new AppProtMsg(SeqNum,MsgType,idSource,idDstUser);
        String msgString = appMsg.composeProtocolMsg(cmList, cmInfo);
        // piggybacked text message are composed
        if (serverCtrlTcpChannel !=null && msgString!=null) {
            serverCtrlTcpChannel.sendMsgStringToClient(msgString);
            return true;
        }
        else
            return false;
    }

    /*
     Extract an AppProtocolMsg from the appMsgActiveClientReceiveQue at the head
     of the queue
     executed by the Call Processing thread
    */
    public AppProtMsg readMsgFromClientReceiveQue() {
                // receive from the current received queue only
        AppProtMsg appMsg = null;

        try {
            semaphoreActiveClientQueue.acquire();
            if (appMsgActiveClientReceiveQue !=null && !appMsgActiveClientReceiveQue.isEmpty())
                appMsg = appMsgActiveClientReceiveQue.removeFirst();
            semaphoreActiveClientQueue.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return appMsg;
    }

    public AppProtMsg readMsgFromClientPendedQue() {
                // receive from the pended queue only
        AppProtMsg appMsg = null;
        if (appMsgPendingClientReceiveQue !=null &&
                !appMsgPendingClientReceiveQue.isEmpty()) {
            appMsg = appMsgPendingClientReceiveQue.removeFirst();
        }
        return appMsg;
    }

    /*
     Add the message to the pending queue for further processing
     (in READY state) when the current processing is finished, this
     action is only executed by the call processing thread (i.e., a
     single thread, and not the control channel reading thread)
     */
    public boolean pendMsgIncomingFromClient(AppProtMsg appMsg) {
        if (appMsg.isPendable()) {
            appMsgPendingClientReceiveQue.addLast(appMsg);
            return true;
        }
        else  return false;
    }

    public boolean pendMsgIncomingFromServer(AppProtMsg appMsg) {
        if (appMsg.isPendable()) {
            appMsgPendingServerReceiveQue.addLast(appMsg);
            return true;
        }
        else  return false;
    }
}
