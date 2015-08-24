package com.example;

import static java.lang.Thread.sleep;

/**
 * Created by LPC-Home1 on 7/10/2015.
 */

public class ProcessorPttChatCallP {

    AppProtMsgQueue mAppMsgQue;
    AppProtDataQueues mAppDataMap;
    AppUserState mAppUserHashMap;

    String mUserIdLocal =null, mUserIdRemote =null;
    AppUserRecord mUserRecordLocal =null, mUserRecordRemote =null;
    int mSessionIdLocal =0, mSessionIdRemote =0;

    public ProcessorPttChatCallP(AppProtMsgQueue appMsgQue,
                             AppProtDataQueues appDataMap, AppUserState appUserHashMap) {
        this.mAppMsgQue =appMsgQue;
        this.mAppDataMap = appDataMap;
        this.mAppUserHashMap =appUserHashMap;
    }

    public void doHandlePttCall(String serverId, AppProtMsg receivedMsg, int stateCall) {

        AppProtMsg appMsg = receivedMsg;
        TimerControl timerHoldoff =new TimerControl();
        boolean continuing = true;
        int state =stateCall;

        switch (stateCall) {
            case AppUserRecord.SM_STATE_PTTCALLREQUEST_RECEIVED: {
                mUserIdLocal = appMsg.getMsgSrc();
                mUserIdRemote = appMsg.getMsgDst();
                mUserRecordLocal = mAppUserHashMap.getUserRegistration(mUserIdLocal);
                mUserRecordRemote = mAppUserHashMap.getUserRegistration(mUserIdRemote);
                mSessionIdLocal =appMsg.getSessionId();
                if (!mAppDataMap.isValidSession(mSessionIdLocal))
                    mSessionIdLocal = mAppDataMap.createMediaSession();
                mSessionIdRemote =0;
                break;
            }
            case AppUserRecord.SM_STATE_PTTCALLANNOUNCE_RECEIVED: {
                mUserIdLocal = appMsg.getMsgDst();
                mUserIdRemote = appMsg.getMsgSrc();
                mUserRecordLocal = mAppUserHashMap.getUserRegistration(mUserIdLocal);
                mUserRecordRemote = mAppUserHashMap.getUserRegistration(mUserIdRemote);
                mSessionIdLocal =mAppDataMap.createMediaSession();
                mSessionIdRemote =appMsg.getSessionId();
                break;
            }
        }

        timerHoldoff.setTimer(TimerControl.TIMER_PTTHOLDOFF);
        while (continuing && mAppMsgQue.isConnected()) {
            switch (state) {
                case AppUserRecord.SM_STATE_PTTCALLSUCCESS: {
                    continuing = false;
                    break;
                }
                case AppUserRecord.SM_STATE_PTTCALLREQUEST_RECEIVED: {
                    handlePttCallRequest(appMsg);
                    timerHoldoff.setTimer(TimerControl.TIMER_PTTHOLDOFF);
                    state = AppUserRecord.SM_STATE_PTTHOLDOFF;
                    break;
                }
                case AppUserRecord.SM_STATE_PTTCALLANNOUNCE_RECEIVED: {
                    handlePttCallReceive(appMsg);
                    timerHoldoff.setTimer(TimerControl.TIMER_PTTHOLDOFF);
                    state = AppUserRecord.SM_STATE_PTTHOLDOFF;
                    break;
                }
                case AppUserRecord.SM_STATE_PTTFLOORREQUEST: {
                    handlePttFloorRequest(appMsg);
                    timerHoldoff.setTimer(TimerControl.TIMER_PTTHOLDOFF);
                    state = AppUserRecord.SM_STATE_PTTHOLDOFF;
                    break;
                }
                case AppUserRecord.SM_STATE_PTTFLOORANNOUNCE: {
                    handlePttFloorReceive(appMsg);
                    timerHoldoff.setTimer(TimerControl.TIMER_PTTHOLDOFF);
                    state = AppUserRecord.SM_STATE_PTTHOLDOFF;
                    break;
                }
                case AppUserRecord.SM_STATE_PTTYOUSAYREQUEST: {
                    handlePttYouSayRequest(appMsg);
                    timerHoldoff.setTimer(TimerControl.TIMER_PTTHOLDOFF);
                    state = AppUserRecord.SM_STATE_PTTHOLDOFF;
                    break;
                }
                case AppUserRecord.SM_STATE_PTTYOUSAYANNOUNCE: {
                    handlePttYouSayReceive(appMsg);
                    timerHoldoff.setTimer(TimerControl.TIMER_PTTHOLDOFF);
                    state = AppUserRecord.SM_STATE_PTTHOLDOFF;
                    break;
                }
                case AppUserRecord.SM_STATE_PTTHOLDOFF: {
                    if (null != (appMsg = mAppMsgQue.readMsgFromServerPendedQue())
                            || null != (appMsg = mAppMsgQue.readMsgFromServerReceiveQue())) {
                        if (appMsg.equalTo(appMsg.getSeqNum(), AppProtMsg.MSG_PTTFLOORTAKE, mSessionIdRemote)) {
                            state = AppUserRecord.SM_STATE_PTTFLOORANNOUNCE;
                        }
                        else if (appMsg.equalTo(appMsg.getSeqNum(), AppProtMsg.MSG_PTTYOUSAYANNOUNCE, mSessionIdRemote)) {
                            state = AppUserRecord.SM_STATE_PTTYOUSAYANNOUNCE;
                        }
                        else if (appMsg.equalTo(appMsg.getSeqNum(), AppProtMsg.MSG_PTTCALLEND)) { //, mSessionIdRemote)) {
                            handlePttEndToClient(appMsg);
                            state = AppUserRecord.SM_STATE_PTTCALLSUCCESS;
                        }
                        else if (!mAppMsgQue.pendMsgIncomingFromServer(appMsg)) {
                            state = AppUserRecord.SM_STATE_PTTHOLDOFF;
                        }
                        break;
                    }
                    else if (null != (appMsg = mAppMsgQue.readMsgFromClientPendedQue())) {
                        if (appMsg.equalTo(appMsg.getSeqNum(), AppProtMsg.MSG_PTTFLOORREQUEST, mSessionIdLocal)) {
                            state = AppUserRecord.SM_STATE_PTTFLOORREQUEST;
                        }
                        else if (appMsg.equalTo(appMsg.getSeqNum(), AppProtMsg.MSG_PTTYOUSAY, mSessionIdLocal)) {
                            state = AppUserRecord.SM_STATE_PTTYOUSAYREQUEST;
                        }
                        else if (appMsg.equalTo(appMsg.getSeqNum(), AppProtMsg.MSG_PTTCALLEND, mSessionIdLocal)) {
                            handlePttEndToServer(appMsg);
                            state = AppUserRecord.SM_STATE_PTTCALLSUCCESS;
                        }
                        else if (!mAppMsgQue.pendMsgIncomingFromServer(appMsg)) {
                            state = AppUserRecord.SM_STATE_PTTHOLDOFF;
                        }
                        break;
                    }
                    else if (null != (appMsg = mAppMsgQue.readMsgFromClientReceiveQue())) {
                        if (appMsg.equalTo(appMsg.getSeqNum(), AppProtMsg.MSG_KEEPALIVE)) {
                            if (null != mUserRecordLocal) {
                                mUserRecordLocal.updateKeepAliveReceived();
                                if (mUserRecordLocal.isSendKeepAlive()
                                        && appMsg.equalTo(appMsg.getSeqNum(), AppProtMsg.MSG_KEEPALIVE)) {
                                    short seqNum = mUserRecordLocal.getSeqNum();
                                    mAppMsgQue.sendMsgToClient(seqNum, AppProtMsg.MSG_KEEPALIVE,
                                            serverId, mUserIdLocal);
                                    mUserRecordLocal.updateKeepAliveSent();
                                }
                            }
                        }
                        else if (appMsg.equalTo(appMsg.getSeqNum(), AppProtMsg.MSG_PTTFLOORREQUEST, mSessionIdLocal)) {
                            state = AppUserRecord.SM_STATE_PTTFLOORREQUEST;
                        }
                        else if (appMsg.equalTo(appMsg.getSeqNum(), AppProtMsg.MSG_PTTYOUSAY, mSessionIdLocal)) {
                            state = AppUserRecord.SM_STATE_PTTYOUSAYREQUEST;
                        }
                        else if (appMsg.equalTo(appMsg.getSeqNum(), AppProtMsg.MSG_PTTCALLEND, mSessionIdLocal)) {
                            handlePttEndToServer(appMsg);
                            state = AppUserRecord.SM_STATE_PTTCALLSUCCESS;
                        }
                        else if (!mAppMsgQue.pendMsgIncomingFromServer(appMsg)) {
                            state = AppUserRecord.SM_STATE_PTTHOLDOFF;
                        }
                        break;
                    }
                    else if (timerHoldoff.isTimeout()) {
                        continuing =false;
                        break;
                    }
                    break; // continue looping
                }
                default:
                    break;
            }
        }
    }

    private int handlePttEndToClient(AppProtMsg appMsg) {
        short seqNumLocal =mUserRecordLocal.getSeqNum();

        mAppMsgQue.sendMsgToClient(seqNumLocal, AppProtMsg.MSG_PTTCALLEND,
                mUserIdRemote, mUserIdLocal, mSessionIdLocal);

        return 1;
    }

    private int handlePttEndToServer(AppProtMsg appMsg) {
        short seqNumRemote =mUserRecordRemote.getSeqNum();

        mAppMsgQue.sendMsgToServerTargetUsers(mAppUserHashMap, mUserIdLocal,
                mUserIdRemote, seqNumRemote, AppProtMsg.MSG_PTTCALLEND,
                mSessionIdLocal);

        return 1;
    }

    private int handlePttCallRequest(AppProtMsg receivedMsg) {
        final String LOGFILE = "messagelog.text";
        final String LOGTAG = "Msg: ";
        int reasonCode = Constants.INIT;

        AppProtMsg appClientMsg = receivedMsg;
        short seqNumLocal = appClientMsg.getSeqNum();
        AppProtMsg appServerMsg=null;
        short seqNumRemote =0;
        TimerControl timerControl = new TimerControl();
        boolean continuing = true;
        int substate = AppUserRecord.SM_STATE_PTTTRANSMIT_START;
        int stateResult = AppUserRecord.SM_STATE_PTTTRANSMIT_FAIL;

        while (continuing) {
            switch (substate) {
                case AppUserRecord.SM_STATE_PTTTRANSMIT_START: {
                    if (mUserIdLocal == null || mUserIdRemote == null) {
                        substate = AppUserRecord.SM_STATE_PTTTRANSMIT_ABANDONED;
                        reasonCode = Constants.NOTFOUND_USER1;
                    }
                    else {
                        substate = AppUserRecord.SM_STATE_PTTTRANSMIT_ANNOUNCE;
                    }
                    break;
                }
                case AppUserRecord.SM_STATE_PTTTRANSMIT_ANNOUNCE: {
                    seqNumRemote =mUserRecordRemote.getSeqNum();
                    if (mAppMsgQue.sendMsgToServerTargetUsers(mAppUserHashMap, mUserIdLocal,
                            mUserIdRemote, seqNumRemote, AppProtMsg.MSG_PTTCALLANNOUNCE,
                            mSessionIdLocal)) {
                        timerControl.setTimer(TimerControl.TIMER_CLIENTRESPONSE);
                        substate = AppUserRecord.SM_STATE_PTTTRANSMIT_CONFIRM;
                    }
                    else {
                        substate = AppUserRecord.SM_STATE_PTTTRANSMIT_ABANDONED;
                        reasonCode = Constants.NOTFOUND_USER2;
                    }
                    break;
                }
                case AppUserRecord.SM_STATE_PTTTRANSMIT_CONFIRM: {
                    if (null != (appServerMsg = mAppMsgQue.readMsgFromServerReceiveQue())) {
                        if (appServerMsg.equalTo(seqNumRemote, AppProtMsg.MSG_PTTCALLACCEPT)) {
                            mSessionIdRemote =appServerMsg.getSessionId();
                            substate = AppUserRecord.SM_STATE_PTTTRANSMIT_ACCEPT;
                        }
                        else if (appServerMsg.equalTo(seqNumRemote, AppProtMsg.MSG_PTTCALLREJECT)) {
                            mSessionIdRemote =appServerMsg.getSessionId();
                            substate = AppUserRecord.SM_STATE_PTTTRANSMIT_REJECT;
                            reasonCode = Constants.NAK_RECEIVED1;
                            seqNumLocal =UtilsHelpers.incAppSeqNum(seqNumLocal);
                        }
                        else if (appServerMsg.equalTo(seqNumRemote, AppProtMsg.MSG_NAK)) {
                            substate = AppUserRecord.SM_STATE_PTTTRANSMIT_ABANDONED;
                            reasonCode = Constants.NAK_RECEIVED1;
                            seqNumLocal =UtilsHelpers.incAppSeqNum(seqNumLocal);
                        }
                        else if (!mAppMsgQue.pendMsgIncomingFromServer(appServerMsg)) {
                            substate = AppUserRecord.SM_STATE_PTTTRANSMIT_ABANDONED;
                            reasonCode = Constants.ILLEGAL_MSG1;
                            seqNumLocal =UtilsHelpers.incAppSeqNum(seqNumLocal);
                        }
                    } else if (timerControl.isTimeout()) {
                        substate = AppUserRecord.SM_STATE_PTTTRANSMIT_ABANDONED;
                        reasonCode = Constants.TIMEDOUT_MSG1;
                        seqNumLocal =UtilsHelpers.incAppSeqNum(seqNumLocal);
                    }
                    break;
                }
                case AppUserRecord.SM_STATE_PTTTRANSMIT_ACCEPT: {
                    if (mAppMsgQue.sendMsgToClient(seqNumLocal, AppProtMsg.MSG_PTTCALLACCEPT,
                            mUserIdRemote, mUserIdLocal, mSessionIdLocal)) {
                        mUserRecordLocal.updateKeepAliveSent();
                        timerControl.setTimer(TimerControl.TIMER_CLIENTRESPONSE);
                        substate = AppUserRecord.SM_STATE_PTTTRANSMIT_ACCEPTED;
                    }
                    else {
                        substate = AppUserRecord.SM_STATE_PTTTRANSMIT_FAIL;
                        reasonCode = Constants.NOTFOUND_USER3;
                        seqNumLocal =UtilsHelpers.incAppSeqNum(seqNumLocal);
                    }
                    break;
                }
                case AppUserRecord.SM_STATE_PTTTRANSMIT_REJECT: {
                    if (mAppMsgQue.sendMsgToClient(seqNumLocal, AppProtMsg.MSG_PTTCALLREJECT,
                            mUserIdRemote, mUserIdLocal, mSessionIdLocal)) {
                        mUserRecordLocal.updateKeepAliveSent();
                        substate = AppUserRecord.SM_STATE_PTTTRANSMIT_SUCCESS;
                    }
                    else {
                        substate = AppUserRecord.SM_STATE_PTTTRANSMIT_FAIL;
                        reasonCode = Constants.NOTFOUND_USER4;
                        seqNumLocal =UtilsHelpers.incAppSeqNum(seqNumLocal);
                    }
                    break;
                }
                case AppUserRecord.SM_STATE_PTTTRANSMIT_ACCEPTED: {
                    if ((appClientMsg = mAppMsgQue.readMsgFromClientReceiveQue()) != null) {
                        mUserRecordLocal.updateKeepAliveReceived();
                        if (appClientMsg.equalTo(seqNumLocal, AppProtMsg.MSG_ACK, mSessionIdLocal)) {
                            timerControl.setTimer(TimerControl.TIMER_PTTMAXSEGMENT);
                            substate = AppUserRecord.SM_STATE_PTTTRANSMIT_STOPPING;
                            seqNumLocal =UtilsHelpers.incAppSeqNum(seqNumLocal);
                        }
                        else if (appClientMsg.equalTo(seqNumLocal, AppProtMsg.MSG_NAK)) {
                            reasonCode = Constants.NAK_RECEIVED2;
                            substate = AppUserRecord.SM_STATE_PTTTRANSMIT_FAIL;
                            seqNumLocal =UtilsHelpers.incAppSeqNum(seqNumLocal);
                        }
                        else if (!mAppMsgQue.pendMsgIncomingFromClient(appClientMsg)) {
                            // msg from the server not handled in current SM state
                            substate = AppUserRecord.SM_STATE_PTTTRANSMIT_ABANDONED;
                            reasonCode = Constants.ILLEGAL_MSG2;
                            seqNumLocal =UtilsHelpers.incAppSeqNum(seqNumLocal);
                        }
                    }
                    else if (timerControl.isTimeout()) {
                        substate = AppUserRecord.SM_STATE_PTTTRANSMIT_ABANDONED;
                        reasonCode = Constants.TIMEDOUT_MSG2;
                        seqNumLocal =UtilsHelpers.incAppSeqNum(seqNumLocal);
                    }
                    break;      // continue to wait with timer
                }
                case AppUserRecord.SM_STATE_PTTTRANSMIT_STOPPING: {
                    if (null != (appClientMsg = mAppMsgQue.readMsgFromClientReceiveQue())) {
                        mUserRecordLocal.updateKeepAliveReceived();
                        if (appClientMsg.equalTo(seqNumLocal, AppProtMsg.MSG_PTTFLOORRELEASE, mSessionIdLocal)) {
                            substate = AppUserRecord.SM_STATE_PTTTRANSMIT_STOP;
                        }
                        else if (appClientMsg.equalTo(seqNumLocal, AppProtMsg.MSG_NAK)) {
                            reasonCode = Constants.NAK_RECEIVED3;
                            substate = AppUserRecord.SM_STATE_PTTTRANSMIT_FAIL;
                        }
                        else if (!mAppMsgQue.pendMsgIncomingFromClient(appClientMsg)) {
                            substate = AppUserRecord.SM_STATE_PTTTRANSMIT_ABANDONED;
                            // msg from the server not handled in current SM state
                            reasonCode = Constants.ILLEGAL_MSG3;
                        }
                    }
                    else if (timerControl.isTimeout()) {
                        substate = AppUserRecord.SM_STATE_PTTTRANSMIT_ABANDONED;
                        reasonCode = Constants.TIMEDOUT_MSG3;
                    }

                    break;
                }
                case AppUserRecord.SM_STATE_PTTTRANSMIT_STOP: {
                    if (mAppMsgQue.sendMsgToServerTargetUsers(mAppUserHashMap, mUserIdLocal, mUserIdRemote,
                            seqNumRemote, AppProtMsg.MSG_PTTFLOORFREE, mSessionIdLocal)) {
                        substate = AppUserRecord.SM_STATE_PTTTRANSMIT_SUCCESS;
                    } else {
                        substate = AppUserRecord.SM_STATE_PTTTRANSMIT_ABANDONED;
                        reasonCode = Constants.NOTFOUND_USER5;
                    }
                    break;
                }
                case AppUserRecord.SM_STATE_PTTTRANSMIT_ABANDONED: {
                    mAppMsgQue.sendMsgToClient(seqNumLocal, AppProtMsg.MSG_NAK, mUserIdRemote, mUserIdLocal);
                    if (null != mUserRecordLocal)
                        mUserRecordLocal.updateKeepAliveSent();
                    substate = AppUserRecord.SM_STATE_PTTTRANSMIT_FAIL;
                    break;
                }
                case AppUserRecord.SM_STATE_PTTTRANSMIT_SUCCESS: {
                    stateResult = AppUserRecord.SM_STATE_PTTTRANSMIT_SUCCESS;
                    mUserRecordLocal.setStateCurrent(stateResult);
                    continuing = false;
                    break;
                }
                case AppUserRecord.SM_STATE_PTTTRANSMIT_FAIL: {
                    stateResult = AppUserRecord.SM_STATE_PTTTRANSMIT_FAIL;
                    if (Constants.DEBUGGING)
                        LogRecorder.writeStringToFile("PTTTRANS: reason code#" + reasonCode, LOGFILE, LOGTAG);
                    if (null != mUserRecordLocal)
                        mUserRecordLocal.setStateCurrent(stateResult);
                    continuing = false;
                    break;
                }
            }
        }
        return stateResult;  // in actual implementation
    }

    private int handlePttCallReceive(AppProtMsg receivedMsg) {
                            /* for testing purposes the sessionId argument is to get any
                             * session already in the mAppDataMap
                             */
        final String LOGFILE ="messagelog.text";
        final String LOGTAG ="Msg: ";
        int reasonCode= Constants.INIT;

        AppProtMsg appServerMsg, appClientMsg = receivedMsg;
        short seqNumLocal = 0;
        short seqNumRemote = appClientMsg.getSeqNum();
        TimerControl timerControl = new TimerControl();
        TimerControl timerPttSeg1 = new TimerControl();
        TimerControl timerPttGap2 = new TimerControl();
        boolean continuing = true;
        int substate = AppUserRecord.SM_STATE_PTTCALLANNOUNCE_RECEIVED;
        int stateResult = AppUserRecord.SM_STATE_PTTRECEIVE_FAIL;

        while (continuing) {
            switch (substate) {
                case AppUserRecord.SM_STATE_PTTCALLANNOUNCE_RECEIVED: {
                    if (null == mUserRecordLocal) {
                        substate = AppUserRecord.SM_STATE_PTTRECEIVE_FAIL;
                        reasonCode= Constants.NOTFOUND_USER1;
                    }
                    else {
                        seqNumLocal = mUserRecordLocal.getSeqNum();
                        if (mAppMsgQue.sendMsgToClient(seqNumLocal, AppProtMsg.MSG_PTTCALLANNOUNCE,
                                mUserIdRemote, mUserIdLocal, mSessionIdLocal)) {
                            mUserRecordLocal.updateKeepAliveSent();
                            substate = AppUserRecord.SM_STATE_PTTRECEIVE_CONFIRM;
                            timerControl.setTimer(TimerControl.TIMER_CLIENTRESPONSE);
                        } else {
                            substate = AppUserRecord.SM_STATE_PTTRECEIVE_FAIL;
                            reasonCode = Constants.NOTFOUND_USER1;
                        }
                    }
                    break;
                }
                case AppUserRecord.SM_STATE_PTTRECEIVE_CONFIRM: {
                    if ((appClientMsg = mAppMsgQue.readMsgFromClientReceiveQue()) != null) {
                        mUserRecordLocal.updateKeepAliveReceived();
                        if (appClientMsg.equalTo(seqNumLocal, AppProtMsg.MSG_PTTCALLACCEPT,
                                mSessionIdLocal)) {
                            substate = AppUserRecord.SM_STATE_PTTRECEIVE_ACCEPT;
                        }
                        else if (appClientMsg.equalTo(seqNumLocal, AppProtMsg.MSG_PTTCALLREJECT,
                                mSessionIdLocal)) {
                            substate = AppUserRecord.SM_STATE_PTTRECEIVE_REJECT;
                        }
                        else if (appClientMsg.equalTo(seqNumLocal, AppProtMsg.MSG_NAK)) {
                            reasonCode= Constants.NAK_RECEIVED1;
                            substate = AppUserRecord.SM_STATE_PTTRECEIVE_FAIL;
                        }
                        else if (!mAppMsgQue.pendMsgIncomingFromClient(appClientMsg)) {
                            // msg from the server not handled in current SM state
                            substate = AppUserRecord.SM_STATE_PTTRECEIVE_FAIL;
                            reasonCode= Constants.ILLEGAL_MSG1;
                        }
                    }
                    else if (timerControl.isTimeout()) {
                        substate = AppUserRecord.SM_STATE_PTTRECEIVE_ABANDONED;
                        reasonCode= Constants.TIMEDOUT_MSG1;
                    }
                    break;      // continue to wait with timer
                }
                case AppUserRecord.SM_STATE_PTTRECEIVE_ACCEPT: {
                    if (mAppDataMap.checkUdpTriggerFromClient(mSessionIdLocal)) {
                        if (mAppMsgQue.sendMsgToClient(seqNumLocal, AppProtMsg.MSG_ACK, mUserIdRemote, mUserIdLocal)) {
                            mUserRecordLocal.updateKeepAliveSent();
                            substate = AppUserRecord.SM_STATE_PTTRECEIVE_CONFIRMED;
                        }
                        else {
                            substate = AppUserRecord.SM_STATE_PTTRECEIVE_ABANDONED;
                            reasonCode= Constants.NOTFOUND_USER2;
                        }
                    }
                    else if (timerControl.isTimeout()) {
                        substate = AppUserRecord.SM_STATE_PTTRECEIVE_ABANDONED;
                        reasonCode= Constants.NOTFOUND_UDP;
                    }
                    break;
                }
                case AppUserRecord.SM_STATE_PTTRECEIVE_CONFIRMED: {
                    if (mAppMsgQue.sendMsgToServerTargetUsers(mAppUserHashMap, mUserIdLocal, mUserIdRemote,
                            seqNumRemote, AppProtMsg.MSG_PTTCALLACCEPT, mSessionIdLocal)) {
                        timerControl.setTimer(TimerControl.TIMER_PTTAUDIOINITIAL);
                        substate = AppUserRecord.SM_STATE_PTTRECEIVE_STARTED;
                    } else {
                        reasonCode = Constants.NOTFOUND_USER3;
                        substate = AppUserRecord.SM_STATE_PTTRECEIVE_ABANDONED;
                    }
                    break;
                }
                case AppUserRecord.SM_STATE_PTTRECEIVE_STARTED: {
                    if (mAppDataMap.sendVoiceMediaDataToClientbyMem(mSessionIdRemote, mSessionIdLocal)) {
                        substate = AppUserRecord.SM_STATE_PTTRECEIVE_PROGRESS;
                        timerPttSeg1.setTimer(TimerControl.TIMER_PTTMAXSEGMENT);
                        timerPttGap2.setTimer(TimerControl.TIMER_PTTCHATGAP);
                    } // else just continue to read until client signals to stop
                    else if (timerControl.isTimeout()) {
                        timerControl.setTimer(TimerControl.TIMER_PTTCLIENTRELEASE);
                        substate = AppUserRecord.SM_STATE_PTTRECEIVE_STOPPING;
                    }
                    break;
                }
                case AppUserRecord.SM_STATE_PTTRECEIVE_PROGRESS: {
                    if (mAppDataMap.sendVoiceMediaDataToClientbyMem(mSessionIdRemote, mSessionIdLocal)) {
                        timerPttGap2.setTimer(TimerControl.TIMER_PTTCHATGAP);
                    } // else just continue to read until client signals to stop
                    else if (timerPttSeg1.isTimeout()) {
                        timerControl.setTimer(TimerControl.TIMER_PTTCLIENTRELEASE);
                        substate = AppUserRecord.SM_STATE_PTTRECEIVE_STOPPING;
                    }
                    else if (timerPttGap2.isTimeout()) {
                        timerControl.setTimer(TimerControl.TIMER_PTTCLIENTRELEASE);
                        substate = AppUserRecord.SM_STATE_PTTRECEIVE_STOPPING;
                    }
                    break;
                }
                case AppUserRecord.SM_STATE_PTTRECEIVE_ABANDONED: {
                    mAppMsgQue.sendMsgToClient(seqNumLocal, AppProtMsg.MSG_NAK, mUserIdRemote, mUserIdLocal);
                    mUserRecordLocal.updateKeepAliveSent();
                    mAppMsgQue.sendMsgToServerTargetUsers(mAppUserHashMap, mUserIdLocal, mUserIdRemote,
                            seqNumRemote, AppProtMsg.MSG_NAK);
                    substate = AppUserRecord.SM_STATE_PTTRECEIVE_FAIL;
                    break;
                }
                case AppUserRecord.SM_STATE_PTTRECEIVE_REJECT: {
                    if (mAppMsgQue.sendMsgToServerTargetUsers(mAppUserHashMap, mUserIdLocal, mUserIdRemote,
                            seqNumRemote, AppProtMsg.MSG_PTTCALLREJECT, mSessionIdLocal)) {
                        substate = AppUserRecord.SM_STATE_PTTRECEIVE_SUCCESS;
                    } else {
                        reasonCode = Constants.NOTFOUND_USER4;
                        substate = AppUserRecord.SM_STATE_PTTRECEIVE_FAIL;
                    }
                    break;
                }
                case AppUserRecord.SM_STATE_PTTRECEIVE_STOPPING: {
                    if (null != (appServerMsg = mAppMsgQue.readMsgFromServerReceiveQue())) {
                        if (appServerMsg.equalTo(seqNumLocal, AppProtMsg.MSG_PTTFLOORFREE, mSessionIdRemote)) {
                            substate = AppUserRecord.SM_STATE_PTTRECEIVE_STOP;
                        }
                        else if (!mAppMsgQue.pendMsgIncomingFromClient(appServerMsg)) {
                            // msg from the server not handled in current SM state
                            substate = AppUserRecord.SM_STATE_PTTRECEIVE_FAIL;
                            reasonCode= Constants.ILLEGAL_MSG2;
                        }
                    }
                    else if (timerControl.isTimeout()) {
                        substate = AppUserRecord.SM_STATE_PTTRECEIVE_FAIL;
                        reasonCode= Constants.TIMEDOUT_MSG2;
                    }
                    break;
                }
                case AppUserRecord.SM_STATE_PTTRECEIVE_STOP: {
                    if (mAppMsgQue.sendMsgToClient(seqNumLocal, AppProtMsg.MSG_PTTFLOORFREE,
                            mUserIdRemote, mUserIdLocal, mSessionIdLocal)) {
                        mUserRecordLocal.updateKeepAliveSent();
                        substate = AppUserRecord.SM_STATE_PTTRECEIVE_SUCCESS;
                    }
                    else {
                        substate = AppUserRecord.SM_STATE_PTTRECEIVE_FAIL;
                        reasonCode= Constants.NOTFOUND_USER5;
                    }
                    break;
                }
                case AppUserRecord.SM_STATE_PTTRECEIVE_SUCCESS: {
                    stateResult =AppUserRecord.SM_STATE_PTTRECEIVE_SUCCESS;
                    mUserRecordLocal.setStateCurrent(AppUserRecord.SM_STATE_PTTRECEIVE_SUCCESS);
                    continuing =false;
                    break;
                }
                case AppUserRecord.SM_STATE_PTTRECEIVE_FAIL: {
                    stateResult =AppUserRecord.SM_STATE_PTTRECEIVE_FAIL;
                    if (null != mUserRecordLocal)
                        mUserRecordLocal.setStateCurrent(AppUserRecord.SM_STATE_PTTRECEIVE_FAIL);
                    if (Constants.DEBUGGING)
                        LogRecorder.writeStringToFile("PTTREC: reason code#"+reasonCode, LOGFILE, LOGTAG);
                    continuing =false;
                    break;
                }
            }
        }
        if (null != mUserRecordLocal)
            mUserRecordLocal.incAppSeqNum();
        return stateResult;
    }

    private int handlePttFloorRequest(AppProtMsg receivedMsg) {
        final String LOGFILE = "messagelog.text";
        final String LOGTAG = "Msg: ";
        int reasonCode = Constants.INIT;

        AppProtMsg appClientMsg = receivedMsg;
        short seqNumLocal = appClientMsg.getSeqNum();
        short seqNumRemote =0;
        TimerControl timerControl = new TimerControl();
        boolean continuing = true;
        int substate = AppUserRecord.SM_STATE_PTTFLOORREQUEST;
        int stateResult = AppUserRecord.SM_STATE_PTTFLOORREQUEST_FAIL;

        while (continuing) {
            switch (substate) {
                case AppUserRecord.SM_STATE_PTTFLOORREQUEST: {
                    if (mUserRecordLocal == null || mUserRecordRemote == null) {
                        substate = AppUserRecord.SM_STATE_PTTFLOORREQUEST_FAIL;
                        reasonCode = Constants.NOTFOUND_USER1;
                        seqNumLocal =UtilsHelpers.incAppSeqNum(seqNumLocal);
                    }
                    else {
                        substate = AppUserRecord.SM_STATE_PTTFLOORTAKEN_ANNOUNCE;
                    }
                    break;
                }
                case AppUserRecord.SM_STATE_PTTFLOORTAKEN_ANNOUNCE: {
                    seqNumRemote = mUserRecordRemote.getSeqNum();
                    if (mAppMsgQue.sendMsgToServerTargetUsers(mAppUserHashMap, mUserIdLocal, mUserIdRemote,
                            seqNumRemote, AppProtMsg.MSG_PTTFLOORTAKE, mSessionIdLocal)) {
                        substate = AppUserRecord.SM_STATE_PTTFLOORGRANTED;
                    }
                    else {
                        substate = AppUserRecord.SM_STATE_PTTFLOORREQUEST_FAIL;
                        reasonCode = Constants.NOTFOUND_USER2;
                        seqNumLocal =UtilsHelpers.incAppSeqNum(seqNumLocal);
                    }
                    break;
                }
                case AppUserRecord.SM_STATE_PTTFLOORGRANTED: {
                    if (mAppMsgQue.sendMsgToClient(seqNumLocal, AppProtMsg.MSG_PTTFLOORGRANT,
                            mUserIdRemote, mUserIdLocal, mSessionIdLocal)) {
                        mUserRecordLocal.updateKeepAliveSent();
                        timerControl.setTimer(TimerControl.TIMER_PTTMAXSEGMENT);
                        substate = AppUserRecord.SM_STATE_PTTTRANSMIT_STOPPING;
                        seqNumLocal =UtilsHelpers.incAppSeqNum(seqNumLocal);
                    }
                    else {
                        substate = AppUserRecord.SM_STATE_PTTFLOORREQUEST_FAIL;
                        reasonCode = Constants.NOTFOUND_USER3;
                        seqNumLocal =UtilsHelpers.incAppSeqNum(seqNumLocal);
                    }
                    break;
                }
                case AppUserRecord.SM_STATE_PTTTRANSMIT_STOPPING: {
                    if (null != (appClientMsg = mAppMsgQue.readMsgFromClientReceiveQue())) {
                        mUserRecordLocal.updateKeepAliveReceived();
                        if (appClientMsg.equalTo(seqNumLocal, AppProtMsg.MSG_PTTFLOORRELEASE, mSessionIdLocal)) {
                            substate = AppUserRecord.SM_STATE_PTTTRANSMIT_STOP;
                        }
                        else if (appClientMsg.equalTo(seqNumLocal, AppProtMsg.MSG_NAK)) {
                            reasonCode = Constants.NAK_RECEIVED3;
                            substate = AppUserRecord.SM_STATE_PTTFLOORREQUEST_FAIL;
                        }
                        else if (!mAppMsgQue.pendMsgIncomingFromClient(appClientMsg)) {
                            substate = AppUserRecord.SM_STATE_PTTFLOORREQUEST_FAIL;
                            // msg from the server not handled in current SM state
                            reasonCode = Constants.ILLEGAL_MSG3;
                        }
                    }
                    else if (timerControl.isTimeout()) {
                        substate = AppUserRecord.SM_STATE_PTTFLOORREQUEST_FAIL;
                        reasonCode = Constants.TIMEDOUT_MSG3;
                    }

                    break;
                }
                case AppUserRecord.SM_STATE_PTTTRANSMIT_STOP: {
                    if (mAppMsgQue.sendMsgToServerTargetUsers(mAppUserHashMap, mUserIdLocal, mUserIdRemote,
                            seqNumRemote, AppProtMsg.MSG_PTTFLOORFREE, mSessionIdLocal)) {
                        substate = AppUserRecord.SM_STATE_PTTFLOOR_SUCCESS;
                    } else {
                        substate = AppUserRecord.SM_STATE_PTTFLOORREQUEST_FAIL;
                        reasonCode = Constants.NOTFOUND_USER5;
                    }
                    break;
                }
                case AppUserRecord.SM_STATE_PTTFLOOR_SUCCESS: {
                    stateResult = AppUserRecord.SM_STATE_PTTFLOOR_SUCCESS;
                    mUserRecordLocal.setStateCurrent(stateResult);
                    continuing = false;
                    break;
                }
                case AppUserRecord.SM_STATE_PTTFLOORREQUEST_FAIL: {
                    stateResult = AppUserRecord.SM_STATE_PTTFLOORREQUEST_FAIL;
                    if (Constants.DEBUGGING)
                        LogRecorder.writeStringToFile("PTTFTRANS: reason code#" + reasonCode, LOGFILE, LOGTAG);
                    if (null != mUserRecordLocal)
                        mUserRecordLocal.setStateCurrent(stateResult);
                    continuing = false;
                    break;
                }
            }
        }
        return stateResult;  // in actual implementation
    }

    private int handlePttFloorReceive(AppProtMsg receivedMsg) {
        final String LOGFILE ="messagelog.text";
        final String LOGTAG ="Msg: ";
        int reasonCode= Constants.INIT;

        AppProtMsg appServerMsg, appClientMsg = receivedMsg;
        short seqNumLocal = 0;
        TimerControl timerControl = new TimerControl();
        TimerControl timerPttSeg1 = new TimerControl();
        TimerControl timerPttGap2 = new TimerControl();
        boolean continuing = true;
        int substate = AppUserRecord.SM_STATE_PTTFLOORANNOUNCE;
        int stateResult = AppUserRecord.SM_STATE_PTTFLOORANNOUNCE_FAIL;

        while (continuing) {
            switch (substate) {
                case AppUserRecord.SM_STATE_PTTFLOORANNOUNCE: {
                    if (null == mUserRecordLocal) {
                        substate = AppUserRecord.SM_STATE_PTTFLOORANNOUNCE_FAIL;
                        reasonCode= Constants.NOTFOUND_USER1;
                    }
                    else {
                        seqNumLocal = mUserRecordLocal.getSeqNum();
                        if (mAppMsgQue.sendMsgToClient(seqNumLocal, AppProtMsg.MSG_PTTFLOORTAKE,
                                mUserIdRemote, mUserIdLocal, mSessionIdLocal)) {
                            mUserRecordLocal.updateKeepAliveSent();
                            substate = AppUserRecord.SM_STATE_PTTFLOOR_CONFIRM;
                            timerControl.setTimer(TimerControl.TIMER_CLIENTRESPONSE);
                        } else {
                            substate = AppUserRecord.SM_STATE_PTTFLOORANNOUNCE_FAIL;
                            reasonCode = Constants.NOTFOUND_USER1;
                        }
                    }
                    break;
                }
                case AppUserRecord.SM_STATE_PTTFLOOR_CONFIRM: {
                    if ((appClientMsg = mAppMsgQue.readMsgFromClientReceiveQue()) != null) {
                        mUserRecordLocal.updateKeepAliveReceived();
                        if (appClientMsg.equalTo(seqNumLocal, AppProtMsg.MSG_ACK,
                                mSessionIdLocal)) {
                            substate = AppUserRecord.SM_STATE_PTTFLOORRECEIVE_STARTED;
                        }
                        else if (appClientMsg.equalTo(seqNumLocal, AppProtMsg.MSG_NAK)) {
                            reasonCode= Constants.NAK_RECEIVED1;
                            substate = AppUserRecord.SM_STATE_PTTFLOORANNOUNCE_FAIL;
                        }
                        else if (!mAppMsgQue.pendMsgIncomingFromClient(appClientMsg)) {
                            // msg from the server not handled in current SM state
                            substate = AppUserRecord.SM_STATE_PTTFLOORANNOUNCE_FAIL;
                            reasonCode= Constants.ILLEGAL_MSG1;
                        }
                    }
                    else if (timerControl.isTimeout()) {
                        substate = AppUserRecord.SM_STATE_PTTFLOORANNOUNCE_FAIL;
                        reasonCode= Constants.TIMEDOUT_MSG1;
                    }
                    break;      // continue to wait with timer
                }
                case AppUserRecord.SM_STATE_PTTFLOORRECEIVE_STARTED: {
                    if (mAppDataMap.sendVoiceMediaDataToClientbyMem(mSessionIdRemote, mSessionIdLocal)) {
                        substate = AppUserRecord.SM_STATE_PTTFLOORRECEIVE_PROGRESS;
                        timerPttSeg1.setTimer(TimerControl.TIMER_PTTMAXSEGMENT);
                        timerPttGap2.setTimer(TimerControl.TIMER_PTTCHATGAP);
                    } // else just continue to read until client signals to stop
                    else if (timerControl.isTimeout()) {
                        timerControl.setTimer(TimerControl.TIMER_PTTCLIENTRELEASE);
                        substate = AppUserRecord.SM_STATE_PTTRECEIVE_STOPPING;
                    }
                    break;
                }
                case AppUserRecord.SM_STATE_PTTFLOORRECEIVE_PROGRESS: {
                    if (mAppDataMap.sendVoiceMediaDataToClientbyMem(mSessionIdRemote, mSessionIdLocal)) {
                        timerPttGap2.setTimer(TimerControl.TIMER_PTTCHATGAP);
                    } // else just continue to read until client signals to stop
                    else if (timerPttSeg1.isTimeout()) {
                        timerControl.setTimer(TimerControl.TIMER_PTTCLIENTRELEASE);
                        substate = AppUserRecord.SM_STATE_PTTRECEIVE_STOPPING;
                    }
                    else if (timerPttGap2.isTimeout()) {
                        timerControl.setTimer(TimerControl.TIMER_PTTCLIENTRELEASE);
                        substate = AppUserRecord.SM_STATE_PTTRECEIVE_STOPPING;
                    }
                    break;
                }
                case AppUserRecord.SM_STATE_PTTRECEIVE_STOPPING: {
                    if (null != (appServerMsg = mAppMsgQue.readMsgFromServerReceiveQue())) {
                        if (appServerMsg.equalTo(seqNumLocal, AppProtMsg.MSG_PTTFLOORFREE, mSessionIdRemote)) {
                            substate = AppUserRecord.SM_STATE_PTTRECEIVE_STOP;
                        }
                        else if (!mAppMsgQue.pendMsgIncomingFromClient(appServerMsg)) {
                            // msg from the server not handled in current SM state
                            substate = AppUserRecord.SM_STATE_PTTFLOORANNOUNCE_FAIL;
                            reasonCode= Constants.ILLEGAL_MSG2;
                        }
                    }
                    else if (timerControl.isTimeout()) {
                        substate = AppUserRecord.SM_STATE_PTTFLOORANNOUNCE_FAIL;
                        reasonCode= Constants.TIMEDOUT_MSG2;
                    }
                    break;
                }
                case AppUserRecord.SM_STATE_PTTRECEIVE_STOP: {
                    if (mAppMsgQue.sendMsgToClient(seqNumLocal, AppProtMsg.MSG_PTTFLOORFREE,
                            mUserIdRemote, mUserIdLocal, mSessionIdLocal)) {
                        mUserRecordLocal.updateKeepAliveSent();
                        substate = AppUserRecord.SM_STATE_PTTFLOORRECEIVE_SUCCESS;
                    }
                    else {
                        substate = AppUserRecord.SM_STATE_PTTFLOORANNOUNCE_FAIL;
                        reasonCode= Constants.NOTFOUND_USER5;
                    }
                    break;
                }
                case AppUserRecord.SM_STATE_PTTFLOORRECEIVE_SUCCESS: {
                    stateResult =AppUserRecord.SM_STATE_PTTFLOORRECEIVE_SUCCESS;
                    mUserRecordLocal.setStateCurrent(AppUserRecord.SM_STATE_PTTFLOORRECEIVE_SUCCESS);
                    continuing =false;
                    break;
                }
                case AppUserRecord.SM_STATE_PTTFLOORANNOUNCE_FAIL: {
                    stateResult =AppUserRecord.SM_STATE_PTTFLOORANNOUNCE_FAIL;
                    if (null != mUserRecordLocal)
                        mUserRecordLocal.setStateCurrent(AppUserRecord.SM_STATE_PTTFLOORANNOUNCE_FAIL);
                    if (Constants.DEBUGGING)
                        LogRecorder.writeStringToFile("PTTFREC: reason code#"+reasonCode, LOGFILE, LOGTAG);
                    continuing =false;
                    break;
                }
            }
        }
        if (null != mUserRecordLocal)
            mUserRecordLocal.incAppSeqNum();
        return stateResult;
    }

    private int handlePttYouSayRequest(AppProtMsg receivedMsg) {
        final String LOGFILE = "messagelog.text";
        final String LOGTAG = "Msg: ";
        int reasonCode = Constants.INIT;

        AppProtMsg appClientMsg = receivedMsg;
        short seqNumLocal = appClientMsg.getSeqNum();
        short seqNumRemote =0;
        TimerControl timerControl = new TimerControl();
        TimerControl timerPttSeg1 = new TimerControl();
        TimerControl timerPttGap2 = new TimerControl();
        boolean continuing = true;
        int substate = AppUserRecord.SM_STATE_PTTYOUSAYREQUEST;
        int stateResult = AppUserRecord.SM_STATE_PTTYOUSAYREQUEST_FAIL;

        while (continuing) {
            switch (substate) {
                case AppUserRecord.SM_STATE_PTTYOUSAYREQUEST: {
                    if (mUserRecordLocal == null || mUserRecordRemote == null) {
                        substate = AppUserRecord.SM_STATE_PTTYOUSAYREQUEST_FAIL;
                        reasonCode = Constants.NOTFOUND_USER1;
                        seqNumLocal =UtilsHelpers.incAppSeqNum(seqNumLocal);
                    }
                    else {
                        substate = AppUserRecord.SM_STATE_PTTYOUSAYREQUEST_ANNOUNCE;
                    }
                    break;
                }
                case AppUserRecord.SM_STATE_PTTYOUSAYREQUEST_ANNOUNCE: {
                    seqNumRemote = mUserRecordRemote.getSeqNum();
                    if (mAppMsgQue.sendMsgToServerTargetUsers(mAppUserHashMap, mUserIdLocal, mUserIdRemote,
                            seqNumRemote, AppProtMsg.MSG_PTTYOUSAYANNOUNCE, mSessionIdLocal)) {
                        substate = AppUserRecord.SM_STATE_PTTYOUSAYCONFIRM;
                    }
                    else {
                        substate = AppUserRecord.SM_STATE_PTTYOUSAYREQUEST_FAIL;
                        reasonCode = Constants.NOTFOUND_USER2;
                        seqNumLocal =UtilsHelpers.incAppSeqNum(seqNumLocal);
                    }
                    break;
                }
                case AppUserRecord.SM_STATE_PTTYOUSAYCONFIRM: {
                    if (mAppMsgQue.sendMsgToClient(seqNumLocal, AppProtMsg.MSG_PTTYOUSAYCONFIRM,
                            mUserIdRemote, mUserIdLocal, mSessionIdLocal)) {
                        mUserRecordLocal.updateKeepAliveSent();
                        timerControl.setTimer(TimerControl.TIMER_PTTAUDIOINITIAL);
                        substate = AppUserRecord.SM_STATE_PTTYOUSAY_STARTED;
                        seqNumLocal =UtilsHelpers.incAppSeqNum(seqNumLocal);
                    }
                    else {
                        substate = AppUserRecord.SM_STATE_PTTYOUSAYREQUEST_FAIL;
                        reasonCode = Constants.NOTFOUND_USER3;
                        seqNumLocal =UtilsHelpers.incAppSeqNum(seqNumLocal);
                    }
                    break;
                }
                case AppUserRecord.SM_STATE_PTTYOUSAY_STARTED: {
                    if (mAppDataMap.sendVoiceMediaDataToClientbyMem(mSessionIdRemote, mSessionIdLocal)) {
                        substate = AppUserRecord.SM_STATE_PTTYOUSAY_PROGRESS;
                        timerPttSeg1.setTimer(TimerControl.TIMER_PTTMAXSEGMENT);
                        timerPttGap2.setTimer(TimerControl.TIMER_PTTCHATGAP);
                    } // else just continue to read until client signals to stop
                    else if (timerControl.isTimeout()) {
                        timerControl.setTimer(TimerControl.TIMER_PTTCLIENTRELEASE);
                        substate = AppUserRecord.SM_STATE_PTTYOUSAY_STOPPING;
                    }
                    break;
                }
                case AppUserRecord.SM_STATE_PTTYOUSAY_PROGRESS: {
                    if (mAppDataMap.sendVoiceMediaDataToClientbyMem(mSessionIdRemote, mSessionIdLocal)) {
                        timerPttGap2.setTimer(TimerControl.TIMER_PTTCHATGAP);
                    } // else just continue to read until client signals to stop
                    else if (timerPttSeg1.isTimeout()) {
                        timerControl.setTimer(TimerControl.TIMER_PTTCLIENTRELEASE);
                        substate = AppUserRecord.SM_STATE_PTTYOUSAY_STOPPING;
                    }
                    else if (timerPttGap2.isTimeout()) {
                        timerControl.setTimer(TimerControl.TIMER_PTTCLIENTRELEASE);
                        substate = AppUserRecord.SM_STATE_PTTYOUSAY_STOPPING;
                    }
                    break;
                }
                case AppUserRecord.SM_STATE_PTTYOUSAY_STOPPING: {
                    if (null != (appClientMsg = mAppMsgQue.readMsgFromClientReceiveQue())) {
                        mUserRecordLocal.updateKeepAliveReceived();
                        if (appClientMsg.equalTo(seqNumLocal, AppProtMsg.MSG_PTTFLOORRELEASE, mSessionIdLocal)) {
                            substate = AppUserRecord.SM_STATE_PTTYOUSAY_STOP;
                        }
                        else if (appClientMsg.equalTo(seqNumLocal, AppProtMsg.MSG_NAK)) {
                            reasonCode = Constants.NAK_RECEIVED3;
                            substate = AppUserRecord.SM_STATE_PTTYOUSAYREQUEST_FAIL;
                        }
                        else if (!mAppMsgQue.pendMsgIncomingFromClient(appClientMsg)) {
                            substate = AppUserRecord.SM_STATE_PTTYOUSAYREQUEST_FAIL;
                            // msg from the server not handled in current SM state
                            reasonCode = Constants.ILLEGAL_MSG3;
                        }
                    }
                    else if (timerControl.isTimeout()) {
                        substate = AppUserRecord.SM_STATE_PTTYOUSAYREQUEST_FAIL;
                        reasonCode = Constants.TIMEDOUT_MSG3;
                    }

                    break;
                }
                case AppUserRecord.SM_STATE_PTTYOUSAY_STOP: {
                    if (mAppMsgQue.sendMsgToServerTargetUsers(mAppUserHashMap, mUserIdLocal, mUserIdRemote,
                            seqNumRemote, AppProtMsg.MSG_PTTFLOORFREE, mSessionIdLocal)) {
                        substate = AppUserRecord.SM_STATE_PTTYOUSAY_SUCCESS;
                    } else {
                        substate = AppUserRecord.SM_STATE_PTTYOUSAYREQUEST_FAIL;
                        reasonCode = Constants.NOTFOUND_USER5;
                    }
                    break;
                }
                case AppUserRecord.SM_STATE_PTTYOUSAY_SUCCESS: {
                    stateResult = AppUserRecord.SM_STATE_PTTYOUSAY_SUCCESS;
                    mUserRecordLocal.setStateCurrent(stateResult);
                    continuing = false;
                    break;
                }
                case AppUserRecord.SM_STATE_PTTYOUSAYREQUEST_FAIL: {
                    stateResult = AppUserRecord.SM_STATE_PTTYOUSAYREQUEST_FAIL;
                    if (Constants.DEBUGGING)
                        LogRecorder.writeStringToFile("PTTFTRANS: reason code#" + reasonCode, LOGFILE, LOGTAG);
                    if (null != mUserRecordLocal)
                        mUserRecordLocal.setStateCurrent(stateResult);
                    continuing = false;
                    break;
                }
            }
        }
        return stateResult;  // in actual implementation
    }

    private int handlePttYouSayReceive(AppProtMsg receivedMsg) {
        final String LOGFILE ="messagelog.text";
        final String LOGTAG ="Msg: ";
        int reasonCode= Constants.INIT;

        AppProtMsg appServerMsg, appClientMsg = receivedMsg;
        short seqNumLocal = 0;
        TimerControl timerControl = new TimerControl();
        boolean continuing = true;
        int substate = AppUserRecord.SM_STATE_PTTYOUSAYANNOUNCE;
        int stateResult = AppUserRecord.SM_STATE_PTTYOUSAYANNOUNCE_FAIL;

        while (continuing) {
            switch (substate) {
                case AppUserRecord.SM_STATE_PTTYOUSAYANNOUNCE: {
                    if (null == mUserRecordLocal) {
                        substate = AppUserRecord.SM_STATE_PTTYOUSAYANNOUNCE_FAIL;
                        reasonCode= Constants.NOTFOUND_USER1;
                    }
                    else {
                        seqNumLocal = mUserRecordLocal.getSeqNum();
                        if (mAppMsgQue.sendMsgToClient(seqNumLocal, AppProtMsg.MSG_PTTYOUSAYANNOUNCE,
                                mUserIdRemote, mUserIdLocal, mSessionIdLocal)) {
                            mUserRecordLocal.updateKeepAliveSent();
                            substate = AppUserRecord.SM_STATE_PTTYOUSAYANNOUNCE_CONFIRM;
                            timerControl.setTimer(TimerControl.TIMER_CLIENTRESPONSE);
                        } else {
                            substate = AppUserRecord.SM_STATE_PTTYOUSAYANNOUNCE_FAIL;
                            reasonCode = Constants.NOTFOUND_USER1;
                        }
                    }
                    break;
                }
                case AppUserRecord.SM_STATE_PTTYOUSAYANNOUNCE_CONFIRM: {
                    if ((appClientMsg = mAppMsgQue.readMsgFromClientReceiveQue()) != null) {
                        mUserRecordLocal.updateKeepAliveReceived();
                        if (appClientMsg.equalTo(seqNumLocal, AppProtMsg.MSG_ACK,
                                mSessionIdLocal)) {
                            substate = AppUserRecord.SM_STATE_PTTYOUSAY_STOPPING;
                            timerControl.setTimer(TimerControl.TIMER_PTTMAXSEGMENT);
                        }
                        else if (appClientMsg.equalTo(seqNumLocal, AppProtMsg.MSG_NAK)) {
                            reasonCode= Constants.NAK_RECEIVED1;
                            substate = AppUserRecord.SM_STATE_PTTYOUSAYANNOUNCE_FAIL;
                        }
                        else if (!mAppMsgQue.pendMsgIncomingFromClient(appClientMsg)) {
                            // msg from the server not handled in current SM state
                            substate = AppUserRecord.SM_STATE_PTTYOUSAYANNOUNCE_FAIL;
                            reasonCode= Constants.ILLEGAL_MSG1;
                        }
                    }
                    else if (timerControl.isTimeout()) {
                        substate = AppUserRecord.SM_STATE_PTTYOUSAYANNOUNCE_FAIL;
                        reasonCode= Constants.TIMEDOUT_MSG1;
                    }
                    break;      // continue to wait with timer
                }
                case AppUserRecord.SM_STATE_PTTYOUSAY_STOPPING: {
                    if (null != (appServerMsg = mAppMsgQue.readMsgFromServerReceiveQue())) {
                        if (appServerMsg.equalTo(seqNumLocal, AppProtMsg.MSG_PTTFLOORFREE, mSessionIdRemote)) {
                            substate = AppUserRecord.SM_STATE_PTTYOUSAY_STOP;
                        }
                        else if (!mAppMsgQue.pendMsgIncomingFromClient(appServerMsg)) {
                            // msg from the server not handled in current SM state
                            substate = AppUserRecord.SM_STATE_PTTYOUSAYANNOUNCE_FAIL;
                            reasonCode= Constants.ILLEGAL_MSG2;
                        }
                    }
                    else if (timerControl.isTimeout()) {
                        substate = AppUserRecord.SM_STATE_PTTYOUSAYANNOUNCE_FAIL;
                        reasonCode= Constants.TIMEDOUT_MSG2;
                    }
                    break;
                }
                case AppUserRecord.SM_STATE_PTTYOUSAY_STOP: {
                    if (mAppMsgQue.sendMsgToClient(seqNumLocal, AppProtMsg.MSG_PTTFLOORFREE,
                            mUserIdRemote, mUserIdLocal, mSessionIdLocal)) {
                        mUserRecordLocal.updateKeepAliveSent();
                        substate = AppUserRecord.SM_STATE_PTTYOUSAYANNOUNCE_SUCCESS;
                    }
                    else {
                        substate = AppUserRecord.SM_STATE_PTTYOUSAYANNOUNCE_FAIL;
                        reasonCode= Constants.NOTFOUND_USER5;
                    }
                    break;
                }
                case AppUserRecord.SM_STATE_PTTYOUSAYANNOUNCE_SUCCESS: {
                    stateResult =AppUserRecord.SM_STATE_PTTYOUSAYANNOUNCE_SUCCESS;
                    mUserRecordLocal.setStateCurrent(AppUserRecord.SM_STATE_PTTYOUSAYANNOUNCE_SUCCESS);
                    continuing =false;
                    break;
                }
                case AppUserRecord.SM_STATE_PTTYOUSAYANNOUNCE_FAIL: {
                    stateResult =AppUserRecord.SM_STATE_PTTYOUSAYANNOUNCE_FAIL;
                    if (null != mUserRecordLocal)
                        mUserRecordLocal.setStateCurrent(AppUserRecord.SM_STATE_PTTYOUSAYANNOUNCE_FAIL);
                    if (Constants.DEBUGGING)
                        LogRecorder.writeStringToFile("PTTFREC: reason code#"+reasonCode, LOGFILE, LOGTAG);
                    continuing =false;
                    break;
                }
            }
        }
        if (null != mUserRecordLocal)
            mUserRecordLocal.incAppSeqNum();
        return stateResult;
    }
}
