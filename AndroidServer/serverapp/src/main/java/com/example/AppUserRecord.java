package com.example;

import java.util.Random;

import static java.lang.System.arraycopy;

/**
 * Created by LPC-Home1 on 4/1/2015.
 */

public class AppUserRecord {

    public static final int SM_STATE_FINISHED = 0;
    public static final int SM_STATE_INITIALIZING = 100;
    public static final int SM_STATE_READY = 102;
    public static final int SM_STATE_REGISTRATION = 700;
    public static final int SM_STATE_REGISTERING = 701;
    public static final int SM_STATE_REGISTERABANDONED = 709;
    public static final int SM_STATE_REGISTERSUCCESS = 702;
    public static final int SM_STATE_REGISTERFAIL =703;
    public static final int SM_STATE_DEREGISTER = 704;
    public static final int SM_STATE_DEREGISTERABANDONED = 708;
    public static final int SM_STATE_DEREGISTERFAIL = 705;
    public static final int SM_STATE_DEREGISTERSUCCESS = 706;
    public static final int SM_STATE_DEREGISTERING = 707;

    public static final int SM_STATE_PROBEREQUESTTRYING = 103;
    public static final int SM_STATE_PROBEREQUESTRESPONSE = 104;
    public static final int SM_STATE_PROBEREQUEST_ABANDONED = 124;
    public static final int SM_STATE_PROBEREQUESTSUCCESS = 105;
    public static final int SM_STATE_PROBEMATCHING = 106;
    public static final int SM_STATE_PROBECONFIRMING = 107;
    public static final int SM_STATE_PROBEREQUESTFAIL =108;
    public static final int SM_STATE_PROBECONFIRMED = 109;
    public static final int SM_STATE_PROBETOTARGET = 110;
    public static final int SM_STATE_PROBERECEIVEREQUEST = 120;
    public static final int SM_STATE_PROBERECEIVEREQFAIL = 121;
    public static final int SM_STATE_PROBERECEIVEREQRESP_RECEIVED = 122;
    public static final int SM_STATE_PROBERECEIVEREQABANDONED =125;
    public static final int SM_STATE_PROBERECEIVEREQSUCESS = 123;


    public static final int SM_STATE_CMACTION = 200;
    public static final int SM_STATE_PROBEREQRECEIVED = 111;      // client only

    public static final int SM_STATE_PTTCALLREQUEST_RECEIVED = 105;
    public static final int SM_STATE_PTTTRANSMIT_START =600;
    public static final int SM_STATE_PTTTRANSMIT_FAIL =601;
    public static final int SM_STATE_PTTTRANSMIT_ABANDONED =602;
    public static final int SM_STATE_PTTTRANSMIT_ANNOUNCE =603;
    public static final int SM_STATE_PTTTRANSMIT_CONFIRM =604;
    public static final int SM_STATE_PTTTRANSMIT_RESPONSE =605;
    public static final int SM_STATE_PTTTRANSMIT_ACCEPTED =606;
    public static final int SM_STATE_PTTTRANSMIT_STARTED =607;
    public static final int SM_STATE_PTTTRANSMIT_PROGRESS =608;
    public static final int SM_STATE_PTTTRANSMIT_STOPPING =609;
    public static final int SM_STATE_PTTTRANSMIT_STOP =610;
    public static final int SM_STATE_PTTTRANSMIT_RELEASE =611;
    public static final int SM_STATE_PTTTRANSMIT_SUCCESS =612;
    public static final int SM_STATE_PTTTRANSMIT_ACCEPT =613;
    public static final int SM_STATE_PTTTRANSMIT_REJECT =614;

    public static final int SM_STATE_PTTCALLANNOUNCE_RECEIVED =620;
    public static final int SM_STATE_PTTRECEIVE_FAIL =621;
    public static final int SM_STATE_PTTRECEIVE_CONFIRM =622;
    public static final int SM_STATE_PTTRECEIVE_ACCEPT =623;
    public static final int SM_STATE_PTTRECEIVE_REJECT =624;
    public static final int SM_STATE_PTTRECEIVE_ABANDONED =625;
    public static final int SM_STATE_PTTRECEIVE_CONFIRMED =626;
    public static final int SM_STATE_PTTRECEIVE_STARTED =627;
    public static final int SM_STATE_PTTRECEIVE_STOPPING =628;
    public static final int SM_STATE_PTTRECEIVE_STOP =629;
    public static final int SM_STATE_PTTRECEIVE_SUCCESS =630;
    public static final int SM_STATE_PTTRECEIVE_PROGRESS =631;

    public static final int SM_STATE_PTTFLOORREQUEST =632;
    public static final int SM_STATE_PTTFLOORANNOUNCE =633;
    public static final int SM_STATE_PTTFLOORGRANTED =634;
    public static final int SM_STATE_PTTFLOOR_PREPARED =635;
    public static final int SM_STATE_PTTFLOORREQUEST_FAIL =636;
    public static final int SM_STATE_PTTFLOOR_COMPLETE =637;
    public static final int SM_STATE_PTTFLOORANNOUNCE_FAIL =638;
    public static final int SM_STATE_PTTFLOORRECEIVE_STARTED =639;
    public static final int SM_STATE_PTTFLOORRECEIVE_SUCCESS =640;
    public static final int SM_STATE_PTTFLOORTAKEN_ANNOUNCE =640;
    public static final int SM_STATE_PTTFLOOR_SUCCESS =641;
    public static final int SM_STATE_PTTFLOOR_CONFIRM =642;
    public static final int SM_STATE_PTTFLOORRECEIVE_PROGRESS =643;

    public static final int SM_STATE_PTTCALLFINISH =652;
    public static final int SM_STATE_PTTCALLSUCCESS =650;
    public static final int SM_STATE_PTTCALLFAIL =651;
    public static final int SM_STATE_PTTHOLDOFF =659;


    public static final int SM_STATE_PTTYOUSAYREQUEST =670;
    public static final int SM_STATE_PTTYOUSAYCONFIRM =671;
    public static final int SM_STATE_PTTYOUSAY_STARTED =672;
    public static final int SM_STATE_PTTYOUSAY_STOP =673;
    public static final int SM_STATE_PTTYOUSAYREQUEST_FAIL =674;
    public static final int SM_STATE_PTTYOUSAY_SUCCESS =675;
    public static final int SM_STATE_PTTYOUSAYREQUEST_COMPLETE =676;


    public static final int SM_STATE_PTTYOUSAYANNOUNCE =680;
    public static final int SM_STATE_PTTYOUSAYRECEIVE_PREPARE =681;
    public static final int SM_STATE_PTTYOUSAYRECEIVE_STOPPING =682;
    public static final int SM_STATE_PTTYOUSAYRECEIVE_STOP =683;
    public static final int SM_STATE_PTTYOUSAYRECEIVE_FAIL =684;
    public static final int SM_STATE_PTTYOUSAYRECEIVE_SUCCESS =685;

    public static final int SM_STATE_PTTYOUSAYREQUEST_ANNOUNCE =690;
    public static final int SM_STATE_PTTYOUSAYCONFIRMED =691;
    public static final int SM_STATE_PTTYOUSAYANNOUNCE_STARTED =693;
    public static final int SM_STATE_PTTYOUSAY_PROGRESS =694;
    public static final int SM_STATE_PTTYOUSAY_STOPPING =695;


    public static final int SM_STATE_PTTYOUSAYANNOUNCE_CONFIRM =697;
    public static final int SM_STATE_PTTYOUSAYANNOUNCE_SUCCESS =698;
    public static final int SM_STATE_PTTYOUSAYANNOUNCE_FAIL =699;



    public static final int SM_STATE_TEXTMSGRECEIVED = 241;
    public static final int SM_STATE_TEXTMSGRECEIVE_RESPONSE = 242;
    public static final int SM_STATE_TEXTMSGTRANSMIT_FORWARD =240;
    public static final int SM_STATE_TEXTMSGRECEIVE_CONFIRM = 243;
    public static final int SM_STATE_TEXTMSGRECEIVE_ABANDONED = 246;
    public static final int SM_STATE_TEXTMSGRECEIVE_SUCCESS = 244;
    public static final int SM_STATE_TEXTMSGRECEIVE_FAIL = 245;

    public static final int SM_STATE_TEXTMSGSELECTED = 490;
    public static final int SM_STATE_TEXTMSGTRANSMIT = 491;
    public static final int SM_STATE_TEXTMSGTRANSMIT_RESPONSE = 492;
    public static final int SM_STATE_TEXTMSGTRANSMIT_CONFIRM = 494;
    public static final int SM_STATE_TEXTMSGTRANSMIT_ABANDONED = 496;
    public static final int SM_STATE_TEXTMSGTRANSMIT_FINISH = 492;
    public static final int SM_STATE_TEXTMSGTRANSMIT_FAIL =493;
    public static final int SM_STATE_TEXTMSGTRANSMIT_SUCCESS =495;

    public static final int SM_STATE_VOICEMSGSELECTED = 500;
    public static final int SM_STATE_VOICEMSGACTIVETRANSMIT_START = 501;
    public static final int SM_STATE_VOICEMSGSILENTTRANSMIT_START = 512;
    public static final int SM_STATE_VOICEMSGTRANSMIT_RESPONSE = 510;
    public static final int SM_STATE_VOICEMSGTRANSMIT_STARTED = 504;
    public static final int SM_STATE_VOICEMSGTRANSMIT_STOPPING = 502;
    public static final int SM_STATE_VOICEMSGTRANSMIT_STOPPED = 503;
    public static final int SM_STATE_VOICEMSGTRANSMIT_FAIL = 505;
    public static final int SM_STATE_VOICEMSGTRANSMIT_CONFIRM = 506;
    public static final int SM_STATE_VOICEMSGTRANSMIT_SUCCESS =509;
    public static final int SM_STATE_VOICEMSGTRANSMIT_ABANDONED =513;
    public static final int SM_STATE_VOICEMSGTRANSMIT_RECEIVING =507;
    public static final int SM_STATE_VOICEMSGTRANSMIT_RECEIVINGINITIAL =508;
    public static final int SM_STATE_VOICEMSGTRANSMIT_RECORDED =511;

    public static final int SM_STATE_VOICEMSGACTIVERECEIVE_ANNONCE =520;
    public static final int SM_STATE_VOICEMSGSILENTRECEIVE_ANNONCE =531;
    public static final int SM_STATE_VOICEMSGRECEIVED =521;
    public static final int SM_STATE_VOICEMSGRECEIVE_CONFIRM =523;
    public static final int SM_STATE_VOICEMSGRECEIVE_ABANDONED =532;
    public static final int SM_STATE_VOICEMSGRECEIVE_STARTED =522;
    public static final int SM_STATE_VOICEMSGRECEIVE_STOP =524;
    public static final int SM_STATE_VOICEMSGRECEIVE_STOPPING =525;
    public static final int SM_STATE_VOICEMSGRECEIVE_STOPPED =526;
    public static final int SM_STATE_VOICEMSGRECEIVE_SUCCESS =527;
    public static final int SM_STATE_VOICEMSGRECEIVE_FAIL = 529;

    public static final int SM_STATE_EMAILSELECTED = 110;


    public static final int SM_STATE_CMSELECT = 113;

    public static final int SM_STATE_WAITINGFORUI_TEXTMSGTRANSMIT = 990;
    public static final int SM_STATE_WAITINGFORUI_VOICEMSGTRANSMIT = 991;
    public static final int SM_STATE_WAITINGFORUI_PTTTRANSMIT = 993;
    public static final int SM_STATE_WAITINGFORUI_PTTFLOOR = 994;
    public static final int SM_STATE_WAITINGFORUI_PROBEMATCH =995;
    public static final int SM_STATE_WAITINGFORUI_PTTYOUSAY =996;
    public static final int SM_STATE_WAITINGFORUI_CMSELECTION = 999;


    private String appUserId;           // used to identify this user and device/app instance
    private String appServerId;
    short seqNum;
    private int stateCurrent;         // application user state machine

    private long lastKeepAliveReceived;
    private long lastKeepAliveSent;
    private long updateLast;
    private String userIpAddr;
    private String noTel;               // MSDN in free form (may be rejected by Phone app)
    private String url;                 // VoIP url used for VoIP, PTT, and EMail
    private short numStatus;
    private boolean statusList[];
    // boolean[] {false,false,false,false,false,false};
    // Roaming, WiFi, Busy, Drive, PttOnly, MsgOnly
    private short numCM;
    private int cmList[];
    private String nameFirst;
    private String nameLast;

    public AppUserRecord()
    {
        appUserId = null;
        appServerId =null;
        seqNum =startFromRandom();
        stateCurrent =SM_STATE_READY;

        lastKeepAliveReceived = lastKeepAliveSent =System.currentTimeMillis();
        updateLast = 0;
        userIpAddr =null;
        noTel ="408-111-222";
        url ="app.example.com";
        numStatus =0;
        statusList =null;
        numCM =6;
        cmList = new int[] {0,0,0,0,0,0};
        nameFirst ="firstName";
        nameLast ="lastName";
    }

    private short startFromRandom() {
        Random rn = new Random();
        int result =rn.nextInt((int)(System.currentTimeMillis()%0x7fff))+1;
        return (short)result;
    }

    public short incAppSeqNum() {
        if (seqNum<0x7FFF)
            seqNum++;                    // sequence numbers are expected to wrap around
        else seqNum=0;
        return seqNum;
    }


    public boolean isClientAlive() {
        return (System.currentTimeMillis() - lastKeepAliveReceived
                < TimerControl.TIMER_KEEPALIVE);
    }

    public boolean isSendKeepAlive() {
        return (System.currentTimeMillis() - lastKeepAliveSent
                > TimerControl.TIMER_KEEPALIVE_FREQ);
    }

    public void updateKeepAliveReceived() {
        lastKeepAliveReceived =System.currentTimeMillis();
    }

    public void updateKeepAliveSent() {
        lastKeepAliveSent =System.currentTimeMillis();
    }

    /*
     Utilities
    */

    public String getAppUserId () { return appUserId; }
    public void setAppUserId (String id) {appUserId =id;}
    public String getServerId () { return appServerId; }
    public void setServerId (String id) {appServerId =id;}
    public short getSeqNum () { return seqNum; }
    public void setSeqNum (short n) {seqNum =n;}

    public int getStateCurrent () {return stateCurrent;}
    public void setStateCurrent (int s) {stateCurrent =s;}
    public long getUpdateLast () {return updateLast;}
    public void setUpdateLast (long u) {updateLast =u;}
    public String getUserIpAddr () { return userIpAddr; }
    public void setUserIpAddr (String ipd) {userIpAddr =ipd;}
    public String getNoTel () { return noTel; }
    public void setNoTel (String id) {noTel =id;}
    public String getUrl () { return url; }
    public void setUrl (String id) {url =id;}
    public short getNumStatus () {return numStatus;}
    public void setNumStatus (short n) {numStatus =n;}
    public boolean[] getStatusList () {return statusList;}
    public void setStatusList (short n, boolean[] sl) {
        statusList = new boolean[n];
        arraycopy(sl, 0, statusList, 0, n);
    }
    public short getNumCM () {return numCM;}
    public void setNumCM (short m) {numCM =m;}
    public int[] getCMList () {return cmList;}
    public void setCMList(short m, int[] cl) {
        cmList = new int[m];
        arraycopy(cl, 0, cmList, 0, m);
    }
    public String getNameFirst () { return nameFirst; }
    public void setNameFirst (String f) {nameFirst =f;}
    public String getNameLast () { return nameLast; }
    public void setNameLast (String l) {nameLast =l;}

    public void release()
    {
        appUserId = null;
        stateCurrent =0;

        updateLast = 0;
        noTel =null;
        url =null;
        numStatus =0;
        statusList =null;
        numCM =0;
        cmList =null;
        nameFirst =null;
        nameLast =null;
    }

    public void copy(AppUserRecord user)
    {       // deep copy
        appUserId = user.appUserId;
        stateCurrent = user.stateCurrent;

        updateLast = user.updateLast;
        noTel =user.noTel;
        url =user.url;
        numStatus =user.numStatus;
        statusList = new boolean[numStatus];
        arraycopy(user.statusList, 0, statusList, 0, numStatus);
        numCM =user.numCM;
        cmList = new int[numCM];
        arraycopy(user.cmList, 0, cmList, 0, numCM);
        nameFirst =user.nameFirst;
        nameLast =user.nameLast;

    }

}
