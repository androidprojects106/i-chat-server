package com.example;



import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Thread.sleep;

public class ServerAppClass {

    public static final int APP_SERVERTCPPORT = 8081;
    private static AppUserState appUserHashMap;
    private static AppProtDataQueues appDataQueues;

    public static void main(String[] args) {
        /*
         Initializing of the app states and connection to server
         UserHashMap is a ConcurrentHashMap so that it is safe to
         have multiple thread accessing it (for differrent user's
         control channels)
        */
        final ServerAppClass serverApp = new ServerAppClass();
        final int serverPort = APP_SERVERTCPPORT;
        final String serverIpAddress = NetworkConn.getLocalIpAddress(NetworkConn.ADDRESS_IPv4);
        /*
         Wait for incoming user requests
         */
        final AtomicBoolean isRunning = new AtomicBoolean(true);

        try {
            /*
             Start the server control channel listening thread
             wait for one or more tcp sockets to be accepted and start communications
             with the client application
            */
            final InetTcpServerSocket ctrlServerSocket =
                    new InetTcpServerSocket(serverIpAddress, serverPort);
            /*
             Start the server udp channel listening thread
             wait for one or more client applications to send udp packets to the server
             that sorts them into the receive queues based on the session Id to uniquely
             identify the communications party
            */
            appUserHashMap = new AppUserState();
            appDataQueues = new AppProtDataQueues(serverIpAddress, serverPort);
            Thread tcpServerThread = new Thread(new Runnable() {
                @Override
                public void run() {

                    while (isRunning.get()) {
                        /*
                         Start the TCP socket for the control channel: send and listen to the
                         clients for the control msgs in both directions
                        */
                        final InetTcpSocketConn socketConnection = ctrlServerSocket.initSocketAccept();
                        // caught one user control connection
                        if (socketConnection != null) {
                            AppProtMsgQueue appProtMsgQueue = new AppProtMsgQueue(socketConnection);
                            /* Launch thread to handle this user's communications */
                            Thread callProc =new Thread(serverApp.new CallProcUserThread(appProtMsgQueue));
                            callProc.start();
                        }
                    }
                }

            });
            tcpServerThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getServerId() {
        return "i-EchoAppServer";
    }

    private class CallProcUserThread implements Runnable {
        private String mUserId;
        AppProtMsgQueue mAppMsgQue;

        public CallProcUserThread (AppProtMsgQueue appProtMsgQueue) {
            mUserId =null;                          // not assigned until user registers
            mAppMsgQue = appProtMsgQueue;
        }

        @Override
        public void run() {
            mAppMsgQue.executeListening();
            // start separate thread to listen to incoming messages
            // from this user
            doAppCallP(mAppMsgQue, appDataQueues, appUserHashMap);
            if (mAppMsgQue !=null) {
                mAppMsgQue.clear();
                mAppMsgQue.close();
            }
        }

        private void doAppCallP(AppProtMsgQueue appMsgQue,AppProtDataQueues appDataMap,
                                AppUserState appUserHashMa) {
            AppProtMsg appClientMsg = null;
            AppUserRecord userRecord = null;
            boolean continuing = true, isExitAbnormal =false;
            int stateResult, state =AppUserRecord.SM_STATE_READY;

            int sessionId =0;       // this session Id used for testing only
            while (continuing && appMsgQue.isConnected()) {
                switch (state) {
                    case AppUserRecord.SM_STATE_REGISTRATION: {
                        userRecord =handleClientRegistration(appClientMsg, appMsgQue, appUserHashMap);
                        if (userRecord !=null) {
                            mUserId =userRecord.getAppUserId();
                            state = AppUserRecord.SM_STATE_READY;
                            userRecord.setStateCurrent(AppUserRecord.SM_STATE_REGISTERSUCCESS);
                        }
                        else continuing =false;
                        break;
                    }
                    case AppUserRecord.SM_STATE_DEREGISTER: {
                        stateResult =handleClientDeregister(appClientMsg, appMsgQue, appUserHashMap);
                        if (userRecord !=null)
                            userRecord.setStateCurrent(stateResult);
                        continuing =false;
                        break;
                    }
                    case AppUserRecord.SM_STATE_READY: {
                        if ((appClientMsg =appMsgQue.readMsgFromServerPendedQue()) != null)
                            state =handleMsgFromClient(appClientMsg);
                        else if ((appClientMsg =appMsgQue.readMsgFromServerReceiveQue()) != null)
                            state =handleMsgFromClient(appClientMsg);
                        else if ((appClientMsg =appMsgQue.readMsgFromClientPendedQue()) != null)
                            state =handleMsgFromClient(appClientMsg);
                        else if ((appClientMsg =appMsgQue.readMsgFromClientReceiveQue()) != null) {
                            if (userRecord !=null) {
                                userRecord.updateKeepAliveReceived();
                                if (userRecord.isSendKeepAlive()
                                        && appClientMsg.equalTo(appClientMsg.getSeqNum(),
                                            AppProtMsg.MSG_KEEPALIVE)) {
                                    short seqNum = userRecord.getSeqNum();
                                    String userIdDst = userRecord.getAppUserId();
                                    appMsgQue.sendMsgToClient(seqNum, AppProtMsg.MSG_KEEPALIVE,
                                            getServerId(), userIdDst);
                                    userRecord.updateKeepAliveSent();
                                }
                            }
                            state =handleMsgFromClient(appClientMsg);
                        }
                        else {
                            // nothing read from the user
                            try { sleep(TimerControl.MAX_TIMEPOLLING); }
                            catch (InterruptedException e) { e.printStackTrace(); }
                        }
                        break; // continue looping
                    }
                    case AppUserRecord.SM_STATE_VOICEMSGACTIVETRANSMIT_START: {
                        sessionId =handleVoiceMsgActiveTransmit(appClientMsg, appMsgQue, appDataMap, appUserHashMap);
                        // state = AppUserRecord.SM_STATE_READY;    // in actual implementation
                        state = AppUserRecord.SM_STATE_READY;
                        // for testing now
                        break;
                    }
                    case AppUserRecord.SM_STATE_VOICEMSGSILENTTRANSMIT_START: {
                        sessionId =handleVoiceMsgSilentTransmit(appClientMsg, appMsgQue, appDataMap, appUserHashMap);
                        // state = AppUserRecord.SM_STATE_READY;    // in actual implementation
                        state = AppUserRecord.SM_STATE_READY;
                        // for testing now
                        break;
                    }
                    case AppUserRecord.SM_STATE_VOICEMSGACTIVERECEIVE_ANNONCE: {
                        handleVoiceMsgActiveReceive(appClientMsg, appMsgQue, appDataMap, appUserHashMap);
                        state = AppUserRecord.SM_STATE_READY;
                        break;
                    }
                    case AppUserRecord.SM_STATE_VOICEMSGSILENTRECEIVE_ANNONCE: {
                        handleVoiceMsgSilentReceive(appClientMsg, appMsgQue, appDataMap, appUserHashMap);
                        state = AppUserRecord.SM_STATE_READY;
                        break;
                    }
                    case AppUserRecord.SM_STATE_PROBEREQUESTTRYING: {
                        handleProbeTrying(appClientMsg, appMsgQue, appUserHashMap);
                        state = AppUserRecord.SM_STATE_READY;
                        break;
                    }
                    case AppUserRecord.SM_STATE_PROBERECEIVEREQUEST: {
                        handleProbeRequest(appClientMsg, appMsgQue, appUserHashMap);
                        state = AppUserRecord.SM_STATE_READY;
                        break;
                    }
                    case AppUserRecord.SM_STATE_TEXTMSGTRANSMIT: {
                        handleTextMsgTransmit(appClientMsg, appMsgQue, appUserHashMap);
                        state = AppUserRecord.SM_STATE_READY;
                        break;
                    }
                    case AppUserRecord.SM_STATE_TEXTMSGRECEIVED: {
                        handleTextMsgReceive(appClientMsg, appMsgQue, appUserHashMap);
                        state = AppUserRecord.SM_STATE_READY;
                        break;
                    }
                    case AppUserRecord.SM_STATE_PTTCALLREQUEST_RECEIVED: {
                        ProcessorPttChatCallP pttChatCall =
                                new ProcessorPttChatCallP(appMsgQue, appDataMap, appUserHashMap);
                        pttChatCall.doHandlePttCall(getServerId(), appClientMsg,
                                AppUserRecord.SM_STATE_PTTCALLREQUEST_RECEIVED);
                        state = AppUserRecord.SM_STATE_READY;
                        break;
                    }
                    case AppUserRecord.SM_STATE_PTTCALLANNOUNCE_RECEIVED: {
                        ProcessorPttChatCallP pttChatCall =
                                new ProcessorPttChatCallP(appMsgQue, appDataMap, appUserHashMap);
                        pttChatCall.doHandlePttCall(getServerId(), appClientMsg,
                                AppUserRecord.SM_STATE_PTTCALLANNOUNCE_RECEIVED);
                        state = AppUserRecord.SM_STATE_READY;
                        break;
                    }
                    default:
                        break;
                }
                if (userRecord !=null && !userRecord.isClientAlive()) {
                    isExitAbnormal = true;
                    continuing =false;
                }
            }
            if (isExitAbnormal)
                appUserHashMap.deleteProtMsgQueMap(mUserId);
        }

        private int handleMsgFromClient(AppProtMsg appClientMsg) {
            switch (appClientMsg.getMsgType()) {
                case AppProtMsg.MSG_REGISTER:
                    return AppUserRecord.SM_STATE_REGISTRATION;
                case AppProtMsg.MSG_DEREGISTER:
                    return AppUserRecord.SM_STATE_DEREGISTER;
                case AppProtMsg.MSG_TRYPROBE:
                    return AppUserRecord.SM_STATE_PROBEREQUESTTRYING;
                case AppProtMsg.MSG_PROBEREQUEST:
                    return AppUserRecord.SM_STATE_PROBERECEIVEREQUEST;
                case AppProtMsg.MSG_TEXTMSG:
                    return AppUserRecord.SM_STATE_TEXTMSGTRANSMIT;
                case AppProtMsg.MSG_TEXTMSGRECEIVE:
                    return AppUserRecord.SM_STATE_TEXTMSGRECEIVED;
                case AppProtMsg.MSG_VOICEMSGACTIVETRANSMITREQUEST:
                    return AppUserRecord.SM_STATE_VOICEMSGACTIVETRANSMIT_START;
                case AppProtMsg.MSG_VOICEMSGSILENTTRANSMITREQUEST:
                    return AppUserRecord.SM_STATE_VOICEMSGSILENTTRANSMIT_START;
                case AppProtMsg.MSG_VOICEMSGACTIVEARRIVE:
                    return AppUserRecord.SM_STATE_VOICEMSGACTIVERECEIVE_ANNONCE;
                case AppProtMsg.MSG_VOICEMSGSILENTARRIVE:
                    return AppUserRecord.SM_STATE_VOICEMSGSILENTRECEIVE_ANNONCE;
                case AppProtMsg.MSG_PTTCALLREQUEST:
                    return AppUserRecord.SM_STATE_PTTCALLREQUEST_RECEIVED;
                case AppProtMsg.MSG_PTTCALLANNOUNCE:
                    return AppUserRecord.SM_STATE_PTTCALLANNOUNCE_RECEIVED;

/*                case AppProtMsg.MSG_PTTFLOORREQUEST:
                    return AppUserRecord.SM_STATE_PTTFLOORREQUEST;
                case AppProtMsg.MSG_PTTFLOORTAKE:
                    return AppUserRecord.SM_STATE_PTTFLOORANNOUNCE;*/

                case AppProtMsg.MSG_KEEPALIVE:
                case AppProtMsg.MSG_ERROR:
                default:
                    return AppUserRecord.SM_STATE_READY;      // ignored
            }
        }


        private int handleClientDeregister(AppProtMsg appClientMsg,
                    AppProtMsgQueue appMsgQue, AppUserState appUserHashMap) {
            final String LOGFILE = "messagelog.text";
            final String LOGTAG = "Msg: ";
            int reasonCode = Constants.INIT;

            short seqNum =appClientMsg.getSeqNum();
            String userIdDst = appClientMsg.getMsgSrc();
            boolean continuing =true;
            int substate = AppUserRecord.SM_STATE_DEREGISTER;
            int stateResult = AppUserRecord.SM_STATE_DEREGISTERFAIL;

            AppUserRecord userRecord=appUserHashMap.getUserRegistration(userIdDst);
            while (continuing) {
                switch (substate) {
                    case AppUserRecord.SM_STATE_DEREGISTER: {
                        if (userRecord == null) {
                            // user is not registered
                            substate = AppUserRecord.SM_STATE_DEREGISTERABANDONED;
                            reasonCode= Constants.NOTFOUND_USER;
                        }
                        else
                            substate =AppUserRecord.SM_STATE_DEREGISTERING;
                        break;
                    }
                    case AppUserRecord.SM_STATE_DEREGISTERING: {
                        appUserHashMap.deleteProtMsgQueMap(userIdDst);
                        if (appMsgQue.sendMsgToClient(seqNum,
                                AppProtMsg.MSG_ACK,getServerId(),userIdDst)) {
                            userRecord.updateKeepAliveSent();
                            substate = AppUserRecord.SM_STATE_DEREGISTERSUCCESS;
                        }
                        else {
                            substate = AppUserRecord.SM_STATE_DEREGISTERFAIL;
                            reasonCode= Constants.NOTFOUND_TCP;
                        }
                        break;
                    }
                    case AppUserRecord.SM_STATE_DEREGISTERABANDONED: {
                        appMsgQue.sendMsgToClient(seqNum, AppProtMsg.MSG_NAK,getServerId(),userIdDst);
                        substate = AppUserRecord.SM_STATE_DEREGISTERFAIL;
                        break;
                    }
                    case AppUserRecord.SM_STATE_DEREGISTERSUCCESS: {
                        continuing = false;
                        stateResult =AppUserRecord.SM_STATE_DEREGISTERSUCCESS;
                        break;
                    }
                    case AppUserRecord.SM_STATE_DEREGISTERFAIL: {
                        if (Constants.DEBUGGING)
                            LogRecorder.writeStringToFile("DEREG: reason code#"+reasonCode, LOGFILE, LOGTAG);
                        stateResult =AppUserRecord.SM_STATE_DEREGISTERFAIL;
                        continuing = false;
                        break;
                    }
                }
            }
            return stateResult;
        }

        private AppUserRecord handleClientRegistration(AppProtMsg appClientMsg,
                    AppProtMsgQueue appMsgQue, AppUserState appUserHashMap) {

            final String LOGFILE ="messagelog.text";
            final String LOGTAG ="Msg: ";
            int reasonCode= Constants.INIT;

            int sessionId = appClientMsg.getSessionId();
            String msgIpAddr = appClientMsg.getMsgIpAddr();
            String serverId =getServerId();
            boolean[] statusList = appClientMsg.getStatusList();
            int[] cmList =appClientMsg.getCmList();
            short seqNum =appClientMsg.getSeqNum();
            AppUserRecord userRecord =null;
            int substate =AppUserRecord.SM_STATE_REGISTRATION;
            String userKeyDst =appClientMsg.getMsgSrc();

            boolean continuing =true;
            while (continuing) {
                switch (substate) {
                    case AppUserRecord.SM_STATE_REGISTRATION: {
                        // handle this user's registration request here
                        userRecord = appUserHashMap.addHashUserRegistration(userKeyDst,
                                sessionId, msgIpAddr, serverId, statusList, cmList);
                        appUserHashMap.addProtMsgQueMap(userKeyDst, appMsgQue);
                        if (userRecord != null) {
                            userRecord.updateKeepAliveReceived();
                            substate = AppUserRecord.SM_STATE_REGISTERING;
                        }
                        else {     // failed registration
                            substate = AppUserRecord.SM_STATE_REGISTERABANDONED;
                            reasonCode= Constants.NOTCREATED_USER;
                        }
                        break;
                    }
                    case AppUserRecord.SM_STATE_REGISTERING: {
                        if (appMsgQue.sendMsgToClient(seqNum,
                                AppProtMsg.MSG_ACK,getServerId(),userKeyDst)) {
                            userRecord.updateKeepAliveSent();
                            substate = AppUserRecord.SM_STATE_REGISTERSUCCESS;
                        }
                        else {
                            substate = AppUserRecord.SM_STATE_REGISTERFAIL;
                            reasonCode = Constants.NOTFOUND_TCP;
                        }
                        break;
                    }
                    case AppUserRecord.SM_STATE_REGISTERABANDONED: {
                        appMsgQue.sendMsgToClient(seqNum, AppProtMsg.MSG_NAK, getServerId(), userKeyDst);
                        substate = AppUserRecord.SM_STATE_REGISTERFAIL;
                        break;
                    }
                    case AppUserRecord.SM_STATE_REGISTERSUCCESS: {
                        continuing = false;
                        break;
                    }
                    case AppUserRecord.SM_STATE_REGISTERFAIL: {
                        if (Constants.DEBUGGING)
                            LogRecorder.writeStringToFile("REG: reason code#"+reasonCode, LOGFILE, LOGTAG);
                        userRecord = null;
                        continuing = false;
                        break;
                    }
                }
            }
            return userRecord;
        }

        private int handleProbeTrying(AppProtMsg appClientMsg, AppProtMsgQueue appMsgQue,
                    AppUserState appUserHashMap) {
            final String LOGFILE ="messagelog.text";
            final String LOGTAG ="Msg: ";
            int reasonCode= Constants.INIT;

            int[] cmList =null;
            String[] cmInfo =null;
            AppProtMsg appServerMsg =null;
            short seqNumUser = appClientMsg.getSeqNum();
            String userIdSrc = appClientMsg.getMsgSrc();
            String targetId = appClientMsg.getMsgDst();
            TimerControl timerControl =new TimerControl();
            boolean continuing =true;
            int substate = AppUserRecord.SM_STATE_PROBEREQUESTTRYING;
            int stateResult = AppUserRecord.SM_STATE_PROBEREQUESTFAIL;

            AppUserRecord userRecord=appUserHashMap.getUserRegistration(userIdSrc);
            AppUserRecord targetRecord=appUserHashMap.getUserRegistration(targetId);
            short seqNumTarget =0;

            appMsgQue =appUserHashMap.getActiveUser(userIdSrc);
            while (continuing) {
                switch(substate) {
                    case AppUserRecord.SM_STATE_PROBEREQUESTTRYING: {
                        if (userRecord ==null || targetRecord == null) {
                            // target user is not registered
                            substate = AppUserRecord.SM_STATE_PROBEREQUEST_ABANDONED;
                            reasonCode= Constants.NOTFOUND_USER;
                        }
                        else {
                            seqNumTarget = userRecord.getSeqNum();
                            substate =AppUserRecord.SM_STATE_PROBETOTARGET;
                        }
                        break;
                    }
                    case AppUserRecord.SM_STATE_PROBETOTARGET: {
                        cmList =appClientMsg.getCmList();
                        cmInfo =appClientMsg.getCmInfo();
                        if (appMsgQue.sendMsgToServerTargetUsers(appUserHashMap,userIdSrc,targetId,
                                seqNumTarget,AppProtMsg.MSG_PROBEREQUEST, cmList, cmInfo)) {
                            substate = AppUserRecord.SM_STATE_PROBEMATCHING;
                            timerControl.setTimer(TimerControl.TIMER_TARGETRESPONSE);
                        }
                        else {
                            substate = AppUserRecord.SM_STATE_PROBEREQUEST_ABANDONED;
                            reasonCode= Constants.NOTFOUND_USER;
                        }
                        break;
                    }
                    case AppUserRecord.SM_STATE_PROBEMATCHING: {
                        if ((appServerMsg= appMsgQue.readMsgFromServerReceiveQue()) !=null) {
                            if (appServerMsg.equalTo(seqNumTarget, AppProtMsg.MSG_PROBEMATCH)) {
                                substate = AppUserRecord.SM_STATE_PROBEREQUESTRESPONSE;
                            } else if (appServerMsg.equalTo(seqNumTarget, AppProtMsg.MSG_NAK)) {
                                substate = AppUserRecord.SM_STATE_PROBEREQUEST_ABANDONED;
                                reasonCode= Constants.NAK_RECEIVED1;
                            }
                            else if (!appMsgQue.pendMsgIncomingFromServer(appServerMsg)) {
                                // msg from the server not handled in current SM state
                                substate = AppUserRecord.SM_STATE_PROBEREQUEST_ABANDONED;
                                reasonCode = Constants.ILLEGAL_MSG1;
                            }
                        }
                        else if (timerControl.isTimeout()) {
                            substate = AppUserRecord.SM_STATE_PROBEREQUEST_ABANDONED;
                            reasonCode= Constants.TIMEDOUT_MSG1;
                        }
                        break;
                    }
                    case AppUserRecord.SM_STATE_PROBEREQUESTRESPONSE: {
                        cmList = appServerMsg.getCmList();
                        cmInfo = appServerMsg.getCmInfo();
                        if (appMsgQue.sendMsgToClient(seqNumUser, AppProtMsg.MSG_TRYRESPONSE,
                                targetId, userIdSrc, cmList, cmInfo)) {
                            // targetId user is the source and userIdSrc is the destination
                            userRecord.updateKeepAliveSent();
                            timerControl.setTimer(TimerControl.TIMER_CLIENTRESPONSE);
                            substate = AppUserRecord.SM_STATE_PROBECONFIRMING;
                        }
                        else {
                            substate = AppUserRecord.SM_STATE_PROBEREQUESTFAIL;
                            reasonCode = Constants.NOTFOUND_TCP;
                        }
                        break;
                    }
                    case AppUserRecord.SM_STATE_PROBEREQUEST_ABANDONED: {
                        if (appMsgQue.sendMsgToClient(seqNumUser,
                                AppProtMsg.MSG_NAK, targetId, userIdSrc))
                                // targetId user is the source and userIdSrc is the destination
                            userRecord.updateKeepAliveSent();
                        substate = AppUserRecord.SM_STATE_PROBEREQUESTFAIL;
                        break;
                    }
                    case AppUserRecord.SM_STATE_PROBECONFIRMING: {
                        if ((appClientMsg = appMsgQue.readMsgFromClientReceiveQue()) != null) {
                            userRecord.updateKeepAliveReceived();
                            if (appClientMsg.equalTo(seqNumUser, AppProtMsg.MSG_ACK)) {
                                substate = AppUserRecord.SM_STATE_PROBECONFIRMED;
                            }
                            else if (!appMsgQue.pendMsgIncomingFromClient(appClientMsg)) {
                                // msg from the server not handled in current SM state
                                substate = AppUserRecord.SM_STATE_PROBEREQUESTFAIL;
                                reasonCode= Constants.ILLEGAL_MSG2;
                            }
                        }
                        else if (timerControl.isTimeout()) {
                            substate = AppUserRecord.SM_STATE_PROBEREQUESTFAIL;
                            reasonCode= Constants.TIMEDOUT_MSG2;
                        }
                        break;      // continue to wait with timer
                    }
                    case AppUserRecord.SM_STATE_PROBECONFIRMED: {
                        stateResult = AppUserRecord.SM_STATE_PROBECONFIRMED;
                        userRecord.setStateCurrent(AppUserRecord.SM_STATE_PROBEREQUESTSUCCESS);
                        continuing = false;
                        break;                  // continue to wait with timer
                    }
                    case AppUserRecord.SM_STATE_PROBEREQUESTFAIL: {
                        stateResult = AppUserRecord.SM_STATE_PROBEREQUESTFAIL;
                        if (Constants.DEBUGGING)
                            LogRecorder.writeStringToFile("PROBE: reason code#"+reasonCode, LOGFILE, LOGTAG);
                        userRecord.setStateCurrent(AppUserRecord.SM_STATE_PROBEREQUESTFAIL);
                        continuing = false;
                        break;
                    }
                }
            }

            return stateResult;
        }

        private int handleProbeRequest(AppProtMsg receivedMsg, AppProtMsgQueue appMsgQue,
                                       AppUserState appUserHashMap) {
            final String LOGFILE ="messagelog.text";
            final String LOGTAG ="Msg: ";
            int reasonCode= Constants.INIT;

            AppProtMsg appClientMsg =receivedMsg;
            int[] cmList;
            String[] cmInfo;
            String userIdDst = appClientMsg.getMsgDst();
            String targetId = appClientMsg.getMsgSrc();
                    // note the switchover of the src and dst roles
            short seqNumTarget = appClientMsg.getSeqNum();
            TimerControl timerControl=new TimerControl();
            boolean continuing =true;
            int substate = AppUserRecord.SM_STATE_PROBERECEIVEREQUEST;
            int stateResult = AppUserRecord.SM_STATE_PROBERECEIVEREQFAIL;

            AppUserRecord userRecord=appUserHashMap.getUserRegistration(userIdDst);
            short seqNumUser = 0;
            AppUserRecord targetRecord=appUserHashMap.getUserRegistration(targetId);

            appMsgQue =appUserHashMap.getActiveUser(userIdDst);
            while (continuing) {
                switch(substate) {
                    case AppUserRecord.SM_STATE_PROBERECEIVEREQUEST: {
                        if (userRecord ==null || targetRecord == null) {
                            reasonCode = Constants.NOTFOUND_USER;
                            substate = AppUserRecord.SM_STATE_PROBERECEIVEREQABANDONED;
                            // This should not happen as the message is received from the "targetId"
                        }
                        else {
                            seqNumUser = userRecord.getSeqNum();
                            cmList =appClientMsg.getCmList();
                            cmInfo =appClientMsg.getCmInfo();
                            if (appMsgQue.sendMsgToClient(seqNumUser, AppProtMsg.MSG_PROBEREQUEST,
                                    targetId, userIdDst, cmList, cmInfo)) {
                                userRecord.updateKeepAliveSent();
                                timerControl.setTimer(TimerControl.TIMER_PROBERESPONSE);
                                substate = AppUserRecord.SM_STATE_PROBETOTARGET;
                            }
                            else {
                                substate =AppUserRecord.SM_STATE_PROBERECEIVEREQABANDONED;
                                reasonCode = Constants.NOTFOUND_TCP;
                            }
                        }
                        break;
                    }
                    case AppUserRecord.SM_STATE_PROBETOTARGET: {
                        if ((appClientMsg = appMsgQue.readMsgFromClientReceiveQue()) != null) {
                            userRecord.updateKeepAliveReceived();
                            if (appClientMsg.equalTo(seqNumUser, AppProtMsg.MSG_PROBERESPWITHCM)) {
                                substate = AppUserRecord.SM_STATE_PROBERECEIVEREQRESP_RECEIVED;
                            } else if (appClientMsg.equalTo(seqNumUser, AppProtMsg.MSG_NAK)) {
                                reasonCode= Constants.NAK_RECEIVED;
                                substate = AppUserRecord.SM_STATE_PROBERECEIVEREQABANDONED;
                            }
                            else if (!appMsgQue.pendMsgIncomingFromClient(appClientMsg)) {
                                reasonCode= Constants.ILLEGAL_MSG;
                                // msg from the server not handled in current SM state
                                substate = AppUserRecord.SM_STATE_PROBERECEIVEREQABANDONED;
                            }
                        }
                        else if (timerControl.isTimeout()) {
                            reasonCode= Constants.TIMEDOUT_MSG;
                            substate = AppUserRecord.SM_STATE_PROBERECEIVEREQABANDONED;
                        }
                        break;      // continue to wait with timer
                    }
                    case AppUserRecord.SM_STATE_PROBERECEIVEREQRESP_RECEIVED: {
                        cmList =appClientMsg.getCmList();
                        cmInfo =appClientMsg.getCmInfo();
                        if (appMsgQue.sendMsgToServerTargetUsers(appUserHashMap,userIdDst,targetId,
                                seqNumTarget,AppProtMsg.MSG_PROBEMATCH, cmList,cmInfo)) {
                            if (appMsgQue.sendMsgToClient(seqNumUser,AppProtMsg.MSG_ACK,targetId,userIdDst))
                                userRecord.updateKeepAliveSent();
                            substate = AppUserRecord.SM_STATE_PROBERECEIVEREQSUCESS;
                        }
                        else if (appMsgQue.sendMsgToClient(seqNumUser,AppProtMsg.MSG_NAK,targetId,userIdDst)) {
                            userRecord.updateKeepAliveSent();
                            substate = AppUserRecord.SM_STATE_PROBERECEIVEREQFAIL;
                            reasonCode= Constants.NOTFOUND_USER;
                        }
                        else {
                            reasonCode= Constants.NOTFOUND_TCP;
                            substate = AppUserRecord.SM_STATE_PROBERECEIVEREQFAIL;
                        }
                        break;
                    }
                    case AppUserRecord.SM_STATE_PROBERECEIVEREQABANDONED: {
                        appMsgQue.sendMsgToServerTargetUsers(appUserHashMap, userIdDst, targetId,
                                seqNumTarget, AppProtMsg.MSG_NAK);
                        substate = AppUserRecord.SM_STATE_PROBERECEIVEREQFAIL;
                        break;
                    }
                    case AppUserRecord.SM_STATE_PROBERECEIVEREQSUCESS: {
                        stateResult = AppUserRecord.SM_STATE_PROBERECEIVEREQSUCESS;
                        userRecord.setStateCurrent(AppUserRecord.SM_STATE_PROBERECEIVEREQSUCESS);
                        continuing =false;
                        break;
                    }
                    case AppUserRecord.SM_STATE_PROBERECEIVEREQFAIL: {
                        stateResult = AppUserRecord.SM_STATE_PROBERECEIVEREQFAIL;
                        userRecord.setStateCurrent(AppUserRecord.SM_STATE_PROBERECEIVEREQFAIL);
                        if (Constants.DEBUGGING)
                            LogRecorder.writeStringToFile("PROBEREC: reason code#"+reasonCode, LOGFILE, LOGTAG);
                        continuing =false;
                        break;
                    }
                }
            }
            userRecord.incAppSeqNum();

            return stateResult;
        }

        private int handleTextMsgTransmit(AppProtMsg appClientMsg, AppProtMsgQueue appMsgQue,
                                          AppUserState appUserHashMap) {
            final String LOGFILE ="messagelog.text";
            final String LOGTAG ="Msg: ";
            int reasonCode= Constants.INIT;

            short seqNum =appClientMsg.getSeqNum();
            String userIdSrc =appClientMsg.getMsgSrc();
            String targetId =appClientMsg.getMsgDst();
            TimerControl timerControl =new TimerControl();
            boolean continuing =true;
            int substate = AppUserRecord.SM_STATE_TEXTMSGTRANSMIT;
            int stateResult = AppUserRecord.SM_STATE_TEXTMSGTRANSMIT_FAIL;

            AppUserRecord userRecord =appUserHashMap.getUserRegistration(userIdSrc);
            short seqNumTarget = 0;
            AppUserRecord targetRecord=appUserHashMap.getUserRegistration(targetId);
            while (continuing) {
                switch (substate) {
                    case AppUserRecord.SM_STATE_TEXTMSGTRANSMIT: {
                        if (userRecord == null || targetRecord == null) {
                            substate = AppUserRecord.SM_STATE_TEXTMSGTRANSMIT_ABANDONED;
                            reasonCode= Constants.NOTFOUND_USER1;
                        }
                        else {
                            seqNumTarget = userRecord.getSeqNum();
                            substate = AppUserRecord.SM_STATE_TEXTMSGTRANSMIT_FORWARD;
                        }
                        break;
                    }
                    case AppUserRecord.SM_STATE_TEXTMSGTRANSMIT_FORWARD: {
                        String msgString = appClientMsg.getMsgText();
                        if (Constants.DEBUGGING)
                            LogRecorder.writeStringToFile(msgString, LOGFILE, LOGTAG);
                        if (appMsgQue.sendMsgToServerTargetUsers(appUserHashMap,userIdSrc,targetId,
                                seqNumTarget,AppProtMsg.MSG_TEXTMSGRECEIVE,msgString)) {
                            substate = AppUserRecord.SM_STATE_TEXTMSGTRANSMIT_RESPONSE;
                            timerControl.setTimer(TimerControl.TIMER_CLIENTRESPONSE);
                        }
                        else {
                            substate = AppUserRecord.SM_STATE_TEXTMSGTRANSMIT_ABANDONED;
                            reasonCode= Constants.NOTFOUND_USER2;
                        }
                        break;
                    }
                    case AppUserRecord.SM_STATE_TEXTMSGTRANSMIT_RESPONSE: {
                        AppProtMsg appServerMsg= appMsgQue.readMsgFromServerReceiveQue();
                        if (appServerMsg !=null) {
                            if (appServerMsg.equalTo(seqNumTarget, AppProtMsg.MSG_TEXTMSGCONFIRM)) {
                                substate = AppUserRecord.SM_STATE_TEXTMSGTRANSMIT_CONFIRM;
                            } else if (appServerMsg.equalTo(seqNumTarget, AppProtMsg.MSG_NAK)) {
                                reasonCode= Constants.NAK_RECEIVED;
                                substate = AppUserRecord.SM_STATE_TEXTMSGTRANSMIT_ABANDONED;
                            }
                            else if (!appMsgQue.pendMsgIncomingFromServer(appServerMsg)) {
                                // msg from the server not handled in current SM state
                                substate = AppUserRecord.SM_STATE_TEXTMSGTRANSMIT_ABANDONED;
                                reasonCode= Constants.ILLEGAL_MSG;
                            }
                        }
                        else if (timerControl.isTimeout()) {
                            substate = AppUserRecord.SM_STATE_TEXTMSGTRANSMIT_ABANDONED;
                            reasonCode= Constants.TIMEDOUT_MSG;
                        }
                        break;
                    }
                    case AppUserRecord.SM_STATE_TEXTMSGTRANSMIT_CONFIRM: {
                        if (appMsgQue.sendMsgToClient(seqNum, AppProtMsg.MSG_ACK, targetId, userIdSrc)) {
                            // note targetId user is teh src and userIdSrc is the dest
                            userRecord.updateKeepAliveSent();
                            substate = AppUserRecord.SM_STATE_TEXTMSGTRANSMIT_SUCCESS;
                        }
                        else {
                            reasonCode= Constants.NOTFOUND_USER;
                            substate = AppUserRecord.SM_STATE_TEXTMSGTRANSMIT_FAIL;
                        }
                        break;
                    }
                    case AppUserRecord.SM_STATE_TEXTMSGTRANSMIT_ABANDONED: {
                        appMsgQue.sendMsgToClient(seqNum, AppProtMsg.MSG_NAK, targetId, userIdSrc);
                                // note targetId user is teh src and userIdSrc is the dest
                        userRecord.updateKeepAliveSent();
                        substate =AppUserRecord.SM_STATE_TEXTMSGTRANSMIT_FAIL;
                        break;
                    }
                    case AppUserRecord.SM_STATE_TEXTMSGTRANSMIT_SUCCESS: {
                        stateResult = AppUserRecord.SM_STATE_TEXTMSGTRANSMIT_SUCCESS;
                        userRecord.setStateCurrent(AppUserRecord.SM_STATE_TEXTMSGTRANSMIT_SUCCESS);
                        continuing =false;
                        break;
                    }
                    case AppUserRecord.SM_STATE_TEXTMSGTRANSMIT_FAIL: {
                        stateResult =AppUserRecord.SM_STATE_TEXTMSGTRANSMIT_FAIL;
                        userRecord.setStateCurrent(AppUserRecord.SM_STATE_TEXTMSGTRANSMIT_FAIL);
                        if (Constants.DEBUGGING)
                            LogRecorder.writeStringToFile("TTRANS: reason code#"+reasonCode, LOGFILE, LOGTAG);
                        continuing =false;
                        break;
                    }
                }
            }

            return stateResult;
        }

        private int handleTextMsgReceive(AppProtMsg receivedMsg, AppProtMsgQueue appMsgQue,
                                         AppUserState appUserHashMap) {
            final String LOGFILE ="messagelog.text";
            final String LOGTAG ="Msg: ";
            int reasonCode= Constants.INIT;

            AppProtMsg appClientMsg =receivedMsg;

            String msgString =appClientMsg.getMsgText();
            String userIdDst = appClientMsg.getMsgDst();
            String targetId = appClientMsg.getMsgSrc();
            short seqNumTarget = appClientMsg.getSeqNum();
            TimerControl timerControl =new TimerControl();
            boolean continuing =true;
            int substate = AppUserRecord.SM_STATE_TEXTMSGRECEIVED;
            int stateResult = AppUserRecord.SM_STATE_TEXTMSGRECEIVE_FAIL;

            AppUserRecord userRecord=appUserHashMap.getUserRegistration(userIdDst);
            short seqNumUser = 0;
            AppUserRecord targetRecord=appUserHashMap.getUserRegistration(targetId);
            while (continuing) {
                switch(substate) {
                    case AppUserRecord.SM_STATE_TEXTMSGRECEIVED: {
                        if (userRecord==null || targetRecord == null) {
                            // so received a message from an non-existent source // not possible
                            substate = AppUserRecord.SM_STATE_TEXTMSGRECEIVE_FAIL;
                            reasonCode= Constants.NOTFOUND_USER;
                        }
                        else {
                            seqNumUser = userRecord.getSeqNum();
                            if (appMsgQue.sendMsgToClient(seqNumUser, AppProtMsg.MSG_TEXTMSGRECEIVE,
                                    targetId, userIdDst, msgString)) {
                                userRecord.updateKeepAliveSent();
                                timerControl.setTimer(TimerControl.TIMER_CLIENTRESPONSE);
                                substate = AppUserRecord.SM_STATE_TEXTMSGRECEIVE_RESPONSE;
                            } else {
                                substate = AppUserRecord.SM_STATE_TEXTMSGRECEIVE_ABANDONED;
                                reasonCode = Constants.NOTFOUND_USER;
                            }
                        }
                        break;
                    }
                    case AppUserRecord.SM_STATE_TEXTMSGRECEIVE_RESPONSE: {
                        if ((appClientMsg = appMsgQue.readMsgFromClientReceiveQue()) != null) {
                            userRecord.updateKeepAliveReceived();
                            if (appClientMsg.equalTo(seqNumUser, AppProtMsg.MSG_TEXTMSGCONFIRM)) {
                                substate = AppUserRecord.SM_STATE_TEXTMSGRECEIVE_CONFIRM;
                            } else if (appClientMsg.equalTo(seqNumUser, AppProtMsg.MSG_NAK)) {
                                substate = AppUserRecord.SM_STATE_TEXTMSGRECEIVE_ABANDONED;
                                reasonCode= Constants.NAK_RECEIVED;
                            }
                            else if (!appMsgQue.pendMsgIncomingFromClient(appClientMsg)) {
                                // msg from the server not handled in current SM state
                                substate = AppUserRecord.SM_STATE_TEXTMSGRECEIVE_ABANDONED;
                                reasonCode= Constants.ILLEGAL_MSG;
                            }
                        } else if (timerControl.isTimeout()) {
                            substate = AppUserRecord.SM_STATE_TEXTMSGRECEIVE_ABANDONED;
                            reasonCode= Constants.TIMEDOUT_MSG;
                        }
                        break;      // continue to wait with timer
                    }
                    case AppUserRecord.SM_STATE_TEXTMSGRECEIVE_CONFIRM: {
                        if (appMsgQue.sendMsgToServerTargetUsers(appUserHashMap,userIdDst,targetId,
                                seqNumTarget,AppProtMsg.MSG_TEXTMSGCONFIRM)) {
                            if (appMsgQue.sendMsgToClient(seqNumUser, AppProtMsg.MSG_ACK,
                                    targetId, userIdDst))
                                userRecord.updateKeepAliveSent();
                            substate = AppUserRecord.SM_STATE_TEXTMSGRECEIVE_SUCCESS;
                        }
                        else if (appMsgQue.sendMsgToClient(seqNumUser, AppProtMsg.MSG_NAK,
                                    targetId, userIdDst)) {
                            substate = AppUserRecord.SM_STATE_TEXTMSGRECEIVE_FAIL;
                            userRecord.updateKeepAliveSent();
                            reasonCode= Constants.NOTFOUND_USER;
                        }
                        else {
                            substate = AppUserRecord.SM_STATE_TEXTMSGRECEIVE_FAIL;
                            reasonCode= Constants.NOTFOUND_USER;
                        }
                        userRecord.updateKeepAliveSent();
                        break;
                    }
                    case AppUserRecord.SM_STATE_TEXTMSGRECEIVE_ABANDONED: {
                        appMsgQue.sendMsgToServerTargetUsers(appUserHashMap,userIdDst,targetId,
                                seqNumTarget,AppProtMsg.MSG_NAK);
                        substate = AppUserRecord.SM_STATE_TEXTMSGRECEIVE_FAIL;
                        break;
                    }
                    case AppUserRecord.SM_STATE_TEXTMSGRECEIVE_SUCCESS: {
                        stateResult = AppUserRecord.SM_STATE_TEXTMSGRECEIVE_SUCCESS;
                        userRecord.setStateCurrent(AppUserRecord.SM_STATE_TEXTMSGRECEIVE_SUCCESS);
                        continuing =false;
                        break;
                    }
                    case AppUserRecord.SM_STATE_TEXTMSGRECEIVE_FAIL: {
                        stateResult = AppUserRecord.SM_STATE_TEXTMSGRECEIVE_FAIL;
                        userRecord.setStateCurrent(AppUserRecord.SM_STATE_TEXTMSGRECEIVE_FAIL);
                        if (Constants.DEBUGGING)
                            LogRecorder.writeStringToFile("TREC: reason code#"+reasonCode, LOGFILE, LOGTAG);
                        continuing =false;
                        break;
                    }
                }
            }
            userRecord.incAppSeqNum();

            return stateResult;
        }

        private int handleVoiceMsgActiveTransmit(AppProtMsg receivedMsg, AppProtMsgQueue appMsgQue,
                                               AppProtDataQueues appDataMap, AppUserState appUserHashMap) {
            return handleVoiceMsgTransmitBase(receivedMsg, appMsgQue, appDataMap, appUserHashMap,
                    CmIdxList.CM_TYPE_VOICEMSG_ACTIVE);
        }

        private int handleVoiceMsgSilentTransmit(AppProtMsg receivedMsg, AppProtMsgQueue appMsgQue,
                                                 AppProtDataQueues appDataMap, AppUserState appUserHashMap) {
            return handleVoiceMsgTransmitBase(receivedMsg, appMsgQue, appDataMap, appUserHashMap,
                    CmIdxList.CM_TYPE_VOICEMSG_SILENT);
        }

        private int handleVoiceMsgTransmitBase(AppProtMsg receivedMsg, AppProtMsgQueue appMsgQue,
                                           AppProtDataQueues appDataMap, AppUserState appUserHashMap,
                                           int flag) {
            final String LOGFILE ="messagelog.text";
            final String LOGTAG ="Msg: ";
            int reasonCode= Constants.INIT;

            AppProtMsg appClientMsg =receivedMsg;
            short seqNum = appClientMsg.getSeqNum();
            String userIdSrc = appClientMsg.getMsgSrc();
            String targetId = appClientMsg.getMsgDst();
            int sessionId = appClientMsg.getSessionId();
            TimerControl timerControl =new TimerControl();
            boolean continuing =true;
            int substate = AppUserRecord.SM_STATE_VOICEMSGACTIVETRANSMIT_START;
            int stateResult = AppUserRecord.SM_STATE_VOICEMSGTRANSMIT_FAIL;

            String fileNameSessionId =null;
            BufferedOutputStream bufO =null;
            AppUserRecord userRecord =appUserHashMap.getUserRegistration(userIdSrc);
            AppUserRecord targetRecord=appUserHashMap.getUserRegistration(targetId);
            short seqNumTarget =0;
            while(continuing) {
                switch(substate) {
                    case AppUserRecord.SM_STATE_VOICEMSGACTIVETRANSMIT_START: {
                        if (userRecord ==null || targetRecord == null) {
                            substate = AppUserRecord.SM_STATE_VOICEMSGTRANSMIT_ABANDONED;
                            reasonCode= Constants.NOTFOUND_USER1;
                        }
                        else {
                            seqNumTarget = targetRecord.getSeqNum();
                            substate = AppUserRecord.SM_STATE_VOICEMSGTRANSMIT_RESPONSE;
                        }
                        break;
                    }
                    case AppUserRecord.SM_STATE_VOICEMSGTRANSMIT_RESPONSE: {
                        if (!appDataQueues.isValidSession(sessionId))
                            sessionId = appDataMap.createMediaSession();
                        if (appMsgQue.sendMsgToClient(seqNum,
                                AppProtMsg.MSG_VOICEMSGTRANSMITCONFIRM,
                                targetId, userIdSrc, sessionId)) {
                            userRecord.updateKeepAliveSent();
                            timerControl.setTimer(TimerControl.TIMER_CLIENTRESPONSE);
                            substate = AppUserRecord.SM_STATE_VOICEMSGTRANSMIT_CONFIRM;
                        }
                        else {
                            substate = AppUserRecord.SM_STATE_VOICEMSGTRANSMIT_FAIL;
                            reasonCode= Constants.NOTFOUND_USER2;
                        }
                        break;
                    }
                    case AppUserRecord.SM_STATE_VOICEMSGTRANSMIT_CONFIRM: {
                        if ((appClientMsg = appMsgQue.readMsgFromClientReceiveQue()) != null) {
                            userRecord.updateKeepAliveReceived();
                            if (appClientMsg.equalTo(seqNum, AppProtMsg.MSG_ACK, sessionId)) {
                                substate = AppUserRecord.SM_STATE_VOICEMSGTRANSMIT_STARTED;
                                seqNum =UtilsHelpers.incAppSeqNum(seqNum);
                            }
                            else if (appClientMsg.equalTo(seqNum, AppProtMsg.MSG_NAK)) {
                                reasonCode= Constants.NAK_RECEIVED1;
                                substate = AppUserRecord.SM_STATE_VOICEMSGTRANSMIT_ABANDONED;
                                seqNum =UtilsHelpers.incAppSeqNum(seqNum);
                            }
                            else if (!appMsgQue.pendMsgIncomingFromClient(appClientMsg)) {
                                // msg from the server not handled in current SM state
                                substate = AppUserRecord.SM_STATE_VOICEMSGTRANSMIT_ABANDONED;
                                reasonCode= Constants.ILLEGAL_MSG1;
                                seqNum =UtilsHelpers.incAppSeqNum(seqNum);
                            }
                        }
                        else if (timerControl.isTimeout()) {
                            substate = AppUserRecord.SM_STATE_VOICEMSGTRANSMIT_ABANDONED;
                            reasonCode= Constants.TIMEDOUT_MSG1;
                            seqNum =UtilsHelpers.incAppSeqNum(seqNum);
                        }
                        // note the voice start and end are two separate transaction
                        // with incremented seqNum
                        break;      // continue to wait with timer
                    }
                    case AppUserRecord.SM_STATE_VOICEMSGTRANSMIT_STARTED: {
                        fileNameSessionId = LogRecorder.fileMediaFormat(userIdSrc, sessionId);
                        try {
                            bufO= new BufferedOutputStream(new FileOutputStream(fileNameSessionId, true));
                            timerControl.setTimer(TimerControl.TIMER_VOICEMSGWAITINITIAL);
                            substate =AppUserRecord.SM_STATE_VOICEMSGTRANSMIT_RECEIVINGINITIAL;
                        } catch (IOException e) {
                            substate = AppUserRecord.SM_STATE_VOICEMSGTRANSMIT_FAIL;
                        }
                        break;
                    }
                    case AppUserRecord.SM_STATE_VOICEMSGTRANSMIT_RECEIVINGINITIAL: {
                        /* in actual implementation */
                        AppProtData appData =appDataMap.readDataFromServQue(sessionId);
                        if (appData != null) {
                            LogRecorder.writeVoiceMsgDataToStream(appData.getData(),bufO);
                            // implementing saving and transfer of the client media
                            // to target receiver - note no SM state change here
                            timerControl.setTimer(TimerControl.TIMER_VOICEMSGWAITTOSTOP);
                            substate =AppUserRecord.SM_STATE_VOICEMSGTRANSMIT_RECEIVING;
                        }       // else just continue to read until client signals to stop
                        else if (timerControl.isTimeout()) {
                            substate = AppUserRecord.SM_STATE_VOICEMSGTRANSMIT_ABANDONED;
                            reasonCode= Constants.NOTFOUND_DATA;
                        }
                        break;
                    }
                    case AppUserRecord.SM_STATE_VOICEMSGTRANSMIT_RECEIVING: {
                        /* in actual implementation */
                        AppProtData appData = appDataMap.readDataFromServQue(sessionId);
                        if (appData != null) {
                            LogRecorder.writeVoiceMsgDataToStream(appData.getData(), bufO);
                            timerControl.setTimer(TimerControl.TIMER_VOICEMSGWAITTOSTOP);
                        }       // else just continue to read until client signals to stop
                        else if (!timerControl.isTimeout()) {
                            // do nothing, wait for the gap to clear
                            try { sleep(400); } catch (InterruptedException e) { e.printStackTrace(); }
                        }
                        else {  // audio stopped by the timer wait-to-stop
                            substate = AppUserRecord.SM_STATE_VOICEMSGTRANSMIT_STOPPING;
                            timerControl.setTimer(TimerControl.TIMER_VOICEMSGCLIENTTOCLEAR);
                        }
                        break;
                    }
                    case AppUserRecord.SM_STATE_VOICEMSGTRANSMIT_STOPPING: {
                        if ((appClientMsg = appMsgQue.readMsgFromClientReceiveQue()) != null) {
                            userRecord.updateKeepAliveReceived();
                            if (appClientMsg.equalTo(seqNum, AppProtMsg.MSG_VOICEMSGTRANSMITEND))
                                substate = AppUserRecord.SM_STATE_VOICEMSGTRANSMIT_STOPPED;
                            else if (!appMsgQue.pendMsgIncomingFromClient(appClientMsg)) {
                                substate = AppUserRecord.SM_STATE_VOICEMSGTRANSMIT_ABANDONED;
                                // msg from the server not handled in current SM state
                                reasonCode= Constants.ILLEGAL_MSG2;
                            }
                        }
                        else if (timerControl.isTimeout()) {
                            substate = AppUserRecord.SM_STATE_VOICEMSGTRANSMIT_ABANDONED;
                            reasonCode= Constants.TIMEDOUT_MSG2;
                        }
                        break;
                    }
                    case AppUserRecord.SM_STATE_VOICEMSGTRANSMIT_STOPPED: {
                        AppProtData appData = appDataMap.readDataFromServQue(sessionId);
                        try {
                            while (appData != null) {
                                LogRecorder.writeVoiceMsgDataToStream(appData.getData(), bufO);
                                appData = appDataMap.readDataFromServQue(sessionId);
                            }
                            bufO.flush();
                            bufO.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if (appMsgQue.sendMsgToClient(seqNum, AppProtMsg.MSG_ACK, targetId, userIdSrc)) {
                            userRecord.updateKeepAliveSent();
                            substate = AppUserRecord.SM_STATE_VOICEMSGTRANSMIT_RECORDED;
                        }
                        else {
                            substate = AppUserRecord.SM_STATE_VOICEMSGTRANSMIT_FAIL;
                            reasonCode= Constants.NOTFOUND_USER3;
                        }
                        break;
                    }
                    case AppUserRecord.SM_STATE_VOICEMSGTRANSMIT_RECORDED: {
                        byte msgType;
                        switch (flag) {
                            case CmIdxList.CM_TYPE_VOICEMSG_ACTIVE: {
                                msgType =AppProtMsg.MSG_VOICEMSGACTIVEARRIVE;
                                break;
                            }
                            case CmIdxList.CM_TYPE_VOICEMSG_SILENT:
                            default: {
                                msgType =AppProtMsg.MSG_VOICEMSGSILENTARRIVE;
                                break;
                            }
                        }
                        if (appMsgQue.sendMsgToServerTargetUsers(appUserHashMap, userIdSrc,
                                targetId, seqNumTarget, msgType, sessionId)) {
                            substate = AppUserRecord.SM_STATE_VOICEMSGTRANSMIT_SUCCESS;
                        }
                        else {
                            substate =AppUserRecord.SM_STATE_VOICEMSGTRANSMIT_ABANDONED;
                            reasonCode= Constants.NOTFOUND_USER2;
                        }
                        break;
                    }
                    case AppUserRecord.SM_STATE_VOICEMSGTRANSMIT_ABANDONED: {
                        appMsgQue.sendMsgToClient(seqNum, AppProtMsg.MSG_NAK, targetId, userIdSrc);
                        if (userRecord!= null)
                            userRecord.updateKeepAliveSent();
                        substate =AppUserRecord.SM_STATE_VOICEMSGTRANSMIT_FAIL;
                        break;
                    }
                    case AppUserRecord.SM_STATE_VOICEMSGTRANSMIT_SUCCESS: {
                        stateResult = AppUserRecord.SM_STATE_VOICEMSGTRANSMIT_SUCCESS;
                        userRecord.setStateCurrent(stateResult);
                        continuing =false;
                        break;
                    }
                    case AppUserRecord.SM_STATE_VOICEMSGTRANSMIT_FAIL: {
                        stateResult = AppUserRecord.SM_STATE_VOICEMSGTRANSMIT_FAIL;
                        if (Constants.DEBUGGING)
                            LogRecorder.writeStringToFile("VTRANSC: reason code#"+reasonCode, LOGFILE, LOGTAG);
                        if (userRecord!= null)
                            userRecord.setStateCurrent(stateResult);
                        continuing =false;
                        break;
                    }
                }
            }

            // return stateResult;  // in actual implementation
            return sessionId;       // for testing only
        }

        private int handleVoiceMsgActiveReceive(AppProtMsg receivedMsg, AppProtMsgQueue appMsgQue,
                                              AppProtDataQueues appDataMap, AppUserState appUserHashMap) {
            int flag =CmIdxList.CM_TYPE_VOICEMSG_ACTIVE;
            return handleVoiceMsgReceiveBase(flag, receivedMsg, appMsgQue, appDataMap, appUserHashMap);
        }

        private int handleVoiceMsgSilentReceive(AppProtMsg receivedMsg, AppProtMsgQueue appMsgQue,
                                                AppProtDataQueues appDataMap, AppUserState appUserHashMap) {
            int flag =CmIdxList.CM_TYPE_VOICEMSG_SILENT;
            return handleVoiceMsgReceiveBase(flag, receivedMsg, appMsgQue, appDataMap, appUserHashMap);
        }

        private int handleVoiceMsgReceiveBase(int flag, AppProtMsg receivedMsg, AppProtMsgQueue appMsgQue,
                                          AppProtDataQueues appDataMap, AppUserState appUserHashMap) {
                        /* for testing purposes the sessionId argument is to get any
                         * session already in the mAppDataMap
                         */
            final String LOGFILE ="messagelog.text";
            final String LOGTAG ="Msg: ";
            int reasonCode= Constants.INIT;

            AppProtMsg appClientMsg = receivedMsg;
            String userIdDst= appClientMsg.getMsgDst();
            String targetId= appClientMsg.getMsgSrc();
            int sessionIdIn =appClientMsg.getSessionId();

            TimerControl timerControl = new TimerControl();
            boolean continuing = true;
            int substate = AppUserRecord.SM_STATE_VOICEMSGACTIVERECEIVE_ANNONCE;
            int stateResult = AppUserRecord.SM_STATE_VOICEMSGRECEIVE_FAIL;

            AppUserRecord userRecord = appUserHashMap.getUserRegistration(userIdDst);
            short seqNum = userRecord.getSeqNum();
            int sessionIdOut=0;
            while (continuing) {
                switch (substate) {
                    case AppUserRecord.SM_STATE_VOICEMSGACTIVERECEIVE_ANNONCE: {
                        // try { sleep(200); } catch (InterruptedException e) { e.printStackTrace(); }
                        /* in actual implementation - get a session Id and
                         * associate it with the recorded voice data
                         */
                        byte msgType;
                        sessionIdOut = appDataMap.createMediaSession();
                        if (flag ==CmIdxList.CM_TYPE_VOICEMSG_ACTIVE)
                            msgType =AppProtMsg.MSG_VOICEMSGACTIVERECEIVEANNOUNCE;
                        else
                            msgType =AppProtMsg.MSG_VOICEMSGSILENTRECEIVEANNOUNCE;
                        if (appMsgQue.sendMsgToClient(seqNum, msgType, targetId, userIdDst, sessionIdOut)) {
                            userRecord.updateKeepAliveSent();
                            substate = AppUserRecord.SM_STATE_VOICEMSGRECEIVED;
                            timerControl.setTimer(TimerControl.TIMER_CLIENTRESPONSE);
                        }
                        else {
                            substate = AppUserRecord.SM_STATE_VOICEMSGRECEIVE_FAIL;
                            reasonCode= Constants.NOTFOUND_USER1;
                        }
                        break;
                    }
                    case AppUserRecord.SM_STATE_VOICEMSGRECEIVED: {
                        if ((appClientMsg = appMsgQue.readMsgFromClientReceiveQue()) != null) {
                            userRecord.updateKeepAliveReceived();
                            if (appClientMsg.equalTo(seqNum, AppProtMsg.MSG_VOICEMSGRECEIVEREADY,
                                    sessionIdOut)) {
                                substate = AppUserRecord.SM_STATE_VOICEMSGRECEIVE_CONFIRM;
                            }
                            else if (appClientMsg.equalTo(seqNum, AppProtMsg.MSG_VOICEMSGRECEIVEFAIL)) {
                                substate = AppUserRecord.SM_STATE_VOICEMSGRECEIVE_FAIL;
                                reasonCode= Constants.NAK_RECEIVED1;
                            }
                            else if (!appMsgQue.pendMsgIncomingFromClient(appClientMsg)) {
                                // msg from the server not handled in current SM state
                                substate = AppUserRecord.SM_STATE_VOICEMSGRECEIVE_FAIL;
                                reasonCode= Constants.ILLEGAL_MSG1;
                            }
                        }
                        else if (timerControl.isTimeout()) {
                            substate = AppUserRecord.SM_STATE_VOICEMSGRECEIVE_ABANDONED;
                            reasonCode= Constants.TIMEDOUT_MSG1;
                        }
                        break;      // continue to wait with timer
                    }
                    case AppUserRecord.SM_STATE_VOICEMSGRECEIVE_CONFIRM: {
                        if (appDataMap.checkUdpTriggerFromClient(sessionIdOut)) {
                            if (appMsgQue.sendMsgToClient(seqNum, AppProtMsg.MSG_ACK, targetId, userIdDst)) {
                                userRecord.updateKeepAliveSent();
                                substate = AppUserRecord.SM_STATE_VOICEMSGRECEIVE_STARTED;
                            }
                            else {
                                substate = AppUserRecord.SM_STATE_VOICEMSGRECEIVE_FAIL;
                                reasonCode= Constants.NOTFOUND_USER2;
                            }
                        }
                        else if (timerControl.isTimeout()) {
                            substate = AppUserRecord.SM_STATE_VOICEMSGRECEIVE_ABANDONED;
                            reasonCode= Constants.NOTFOUND_UDP;
                        }
                        break;
                    }
                    case AppUserRecord.SM_STATE_VOICEMSGRECEIVE_ABANDONED: {
                        appMsgQue.sendMsgToClient(seqNum, AppProtMsg.MSG_NAK, targetId, userIdDst);
                        userRecord.updateKeepAliveSent();
                        substate = AppUserRecord.SM_STATE_VOICEMSGRECEIVE_FAIL;
                        break;
                    }
                    case AppUserRecord.SM_STATE_VOICEMSGRECEIVE_STARTED: {
                        appDataMap.sendVoiceMediaDataToClientbyRecord(targetId, sessionIdIn, sessionIdOut);
                        substate = AppUserRecord.SM_STATE_VOICEMSGRECEIVE_STOP;
                        break;
                    }
                    case AppUserRecord.SM_STATE_VOICEMSGRECEIVE_STOP: {
                        try { sleep(2000); } // allow socket to drain
                        catch (InterruptedException e) { e.printStackTrace(); }
                        if (appMsgQue.sendMsgToClient(seqNum, AppProtMsg.MSG_VOICEMSGRECEIVEFINISH,
                                targetId, userIdDst, sessionIdOut)) {
                            userRecord.updateKeepAliveSent();
                            substate = AppUserRecord.SM_STATE_VOICEMSGRECEIVE_SUCCESS;
                        }
                        else {
                            substate = AppUserRecord.SM_STATE_VOICEMSGRECEIVE_FAIL;
                            reasonCode= Constants.NOTFOUND_USER3;
                        }
                        break;
                    }
                    case AppUserRecord.SM_STATE_VOICEMSGRECEIVE_SUCCESS: {
                        stateResult =AppUserRecord.SM_STATE_VOICEMSGRECEIVE_SUCCESS;
                        userRecord.setStateCurrent(AppUserRecord.SM_STATE_VOICEMSGRECEIVE_SUCCESS);
                        continuing =false;
                        break;
                    }
                    case AppUserRecord.SM_STATE_VOICEMSGRECEIVE_FAIL: {
                        stateResult =AppUserRecord.SM_STATE_VOICEMSGRECEIVE_FAIL;
                        userRecord.setStateCurrent(AppUserRecord.SM_STATE_VOICEMSGRECEIVE_FAIL);
                        if (Constants.DEBUGGING)
                            LogRecorder.writeStringToFile("VREC: reason code#"+reasonCode, LOGFILE, LOGTAG);
                        continuing =false;
                        break;
                    }
                }
            }
            userRecord.incAppSeqNum();
            return stateResult;
        }

    }
}
