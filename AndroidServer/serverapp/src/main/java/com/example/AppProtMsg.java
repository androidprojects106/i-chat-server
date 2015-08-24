package com.example;

/**
 * Created by LPC-Home1 on 3/29/2015.
 */

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import static java.lang.System.arraycopy;
import java.lang.String;

/*
 Message Syntax

 |m         - Communications method index (short, or 2 bytes list, with |b delimiter)
 |a         - Local client IP address (String, or bytes)
 |t         - Message type (1-2 bytes, or String)
 |n         - Sequence number (of the message) from source
 |s         - Source (client) ID (String, or bytes)
 |d         - Destination ID ((String, or bytes)
 |u         - Update for the server (Status number list, with |b delimiter)
 |w         - Text message (String, or bytes)
  b          - Separator (note there is no "|") for number types, not in strings

 Message types (used with "|t")
 "A"         - MSG_ACK
 "N"         - MSG_NAK
 "P"         - MSG_PROBEREQUEST
 "CM"        - MSG_CMLIST
 "PT"        - MSG_TRYPROBE
 "PR"        - MSG_TRYRESPONSE;
 "CR"        - MSG_PTTCALLREQUEST
 "CA"        - MSG_PTTCALLANNOUNCE
 "FR"        - MSG_PTTFLOORREQUEST
 "FG"        - MSG_PTTFLOORGRANT
 "FL"        - MSG_PTTFLOORRELEASE
 "TM"        - MSG_TEXTMSG
 "VS"        - MSG_VOICEMSGSTART
 "VE"        - MSG_VOICEMSGEND
 */

/**
 * Created by LPC-Home1 on 3/27/2015.
 */
public class AppProtMsg {
    public static final int MAX_STATUS_VECTOR = 8;

    public static final byte MSG_KEEPALIVE = (byte)0xff;
    public static final byte MSG_ERROR = 0;
    public static final byte MSG_REGISTER = 1;
    public static final byte MSG_DEREGISTER =99;
    public static final byte MSG_ACK = 2;
    public static final byte MSG_NAK = 3;
    public static final byte MSG_CMLIST = 4;
    public static final byte MSG_TRYPROBE = 10;                 // Try Probe from this user
    public static final byte MSG_TRYRESPONSE = 11;              // Try Request from the peer user
    public static final byte MSG_PROBEREQUEST = 12;             // Try Request from the peer user
    public static final byte MSG_PROBERESPWITHCM = 13;
    public static final byte MSG_PROBEMATCH =14;
    public static final byte MSG_TEXTMSG = 21;
    public static final byte MSG_TEXTMSGRECEIVE = 22;
    public static final byte MSG_TEXTMSGCONFIRM = 23;
    public static final byte MSG_VOICEMSGACTIVETRANSMITREQUEST = 50;
    public static final byte MSG_VOICEMSGSILENTTRANSMITREQUEST = 52;
    public static final byte MSG_VOICEMSGTRANSMITEND = 51;
    public static final byte MSG_VOICEMSGTRANSMITCONFIRM = 61;

    public static final byte MSG_VOICEMSGACTIVEARRIVE = 71;
    public static final byte MSG_VOICEMSGSILENTARRIVE = 77;
    public static final byte MSG_VOICEMSGACTIVERECEIVEANNOUNCE = 72;
    public static final byte MSG_VOICEMSGSILENTRECEIVEANNOUNCE = 78;
    public static final byte MSG_VOICEMSGRECEIVEREADY = 73;
    public static final byte MSG_VOICEMSGRECEIVEFINISH = 74;
    public static final byte MSG_VOICEMSGRECEIVESUCCESS =75;
    public static final byte MSG_VOICEMSGRECEIVEFAIL =76;

    public static final byte MSG_PTTCALLREQUEST = 30;
    public static final byte MSG_PTTCALLANNOUNCE = 31;
    public static final byte MSG_PTTCALLACCEPT = 32;
    public static final byte MSG_PTTCALLREJECT = 33;
    public static final byte MSG_PTTFLOORREQUEST = 34;
    public static final byte MSG_PTTFLOORGRANT = 35;
    public static final byte MSG_PTTFLOORRELEASE = 36;
    public static final byte MSG_PTTFLOORFREE = 37;
    public static final byte MSG_PTTCALLEND = 38;
    public static final byte MSG_PTTFLOORTAKE = 39;
    public static final byte MSG_PTTFLOORREJECT = 40;

    public static final byte MSG_PTTYOUSAY = 80;
    public static final byte MSG_PTTYOUSAYCONFIRM = 81;
    public static final byte MSG_PTTYOUSAYREJECT = 82;
    public static final byte MSG_PTTYOUSAYANNOUNCE = 83;



    public static final String MSG_KEEPALIVE_STRING ="K";
    public static final String MSG_REGISTER_STRING = "R";
    public static final String MSG_DEREGISTER_STRING ="D";
    public static final String MSG_ACK_STRING = "A";
    public static final String MSG_NAK_STRING = "N";
    public static final String MSG_PROBEREQUEST_STRING = "P";        // Request from the server
    public static final String MSG_CMLIST_STRING = "CM";
    public static final String MSG_TRYPROBE_STRING = "PT";
    public static final String MSG_TRYRESPONSE_STRING = "PR";
    public static final String MSG_PROBERESPWITHCM_STRING= "PW";
    public static final String MSG_PROBEMATCH_STRING= "PM";
    public static final String MSG_TEXTMSG_STRING= "TM";
    public static final String MSG_TEXTMSGRECEIVE_STRING= "TR";
    public static final String MSG_TEXTMSGCONFIRM_STRING= "TC";
    public static final String MSG_VOICEMSGACTIVETRANSMITREQUEST_STRING = "VSA";
    public static final String MSG_VOICEMSGSILENTTRANSMITREQUEST_STRING = "VSS";
    public static final String MSG_VOICEMSGTRANSMITEND_STRING = "VE";
    public static final String MSG_VOICEMSGTRANSMITCONFIRM_STRING = "VC";
    public static final String MSG_VOICEMSGACTIVEARRIVE_STRING = "VAA";
    public static final String MSG_VOICEMSGSILENTARRIVE_STRING = "VAS";
    public static final String MSG_VOICEMSGACTIVERECEIVEANNOUNCE_STRING = "VMA";
    public static final String MSG_VOICEMSGSILENTRECEIVEANNOUNCE_STRING = "VMS";
    public static final String MSG_VOICEMSGRECEIVEREADY_STRING= "VR";
    public static final String MSG_VOICEMSGRECEIVEFINISH_STRING= "VF";
    public static final String MSG_VOICEMSGRECEIVESUCCESS_STRING ="VX";
    public static final String MSG_VOICEMSGRECEIVEFAIL_STRING ="VL";

    public static final String MSG_PTTCALLREQUEST_STRING = "CR";
    public static final String MSG_PTTCALLANNOUNCE_STRING = "CA";
    public static final String MSG_PTTCALLACCEPT_STRING = "CC";
    public static final String MSG_PTTCALLREJECT_STRING = "CJ";
    public static final String MSG_PTTFLOORREQUEST_STRING = "FR";
    public static final String MSG_PTTFLOORGRANT_STRING = "FG";
    public static final String MSG_PTTFLOORRELEASE_STRING = "FL";
    public static final String MSG_PTTFLOORFREE_STRING = "FE";
    public static final String MSG_PTTFLOORTAKE_STRING = "FI";
    public static final String MSG_PTTFLOORREJECT_STRING = "FJ";
    public static final String MSG_PTTCALLEND_STRING = "CE";


    public static final String MSG_PTTYOUSAY_STRING = "YS";
    public static final String MSG_PTTYOUSAYCONFIRM_STRING = "YC";
    public static final String MSG_PTTYOUSAYREJECT_STRING = "YR";
    public static final String MSG_PTTYOUSAYANNOUNCE_STRING = "YA";

    public static final String DELIMETER_STRING_MSGTYPE = "|t";
    public static final String DELIMETER_STRING_MSGSESSIONID = "|i";
    public static final String DELIMETER_STRING_MSGSEQNUM = "|n";
    public static final String DELIMETER_STRING_MSGSRC = "|s";
    public static final String DELIMETER_STRING_MSGDST = "|d";
    public static final String DELIMETER_STRING_MSGIPADDR = "|a";
    public static final String DELIMETER_STRING_MSGTEXT = "|x";
    public static final String DELIMETER_STRING_MSGCOMMETHOD = "|m";
    public static final String DELIMETER_STRING_MSGSTATUS = "|r";

    public static final char DELIMETER_CHAR_MSGTYPE = 't';
    public static final char DELIMETER_CHAR_MSGSEQNUM = 'n';
    public static final char DELIMETER_CHAR_MSGSESSIONID = 'i';
    public static final char DELIMETER_CHAR_MSGSRC = 's';
    public static final char DELIMETER_CHAR_MSGDST = 'd';
    public static final char DELIMETER_CHAR_MSGIPADDR = 'a';
    public static final char DELIMETER_CHAR_MSGTEXT = 'x';
    public static final char DELIMETER_CHAR_MSGCOMMETHOD = 'm';
    public static final char DELIMETER_CHAR_MSGSTATUS = 'r';

    public static final byte MSG_PROBEINCOMING = 60;
    public static final byte MSG_PROBERESPONSE = 61;

    private short seqNum;
    private byte msgType;
    private String msgSrc;
    private String msgDst;
    private String msgIpAddr;
    private short numStatus;
    private boolean statusList[];
    private short numCM;
    private int cmList[];
    private String cmInfo[];
    private String msgText;
    private int sessionId;

    public AppProtMsg(short num, byte type, String appServerIdSrc, String appUserIdDst) {
        seqNum = num;
        msgType = type;
        msgSrc = appServerIdSrc;
        msgDst = appUserIdDst;
        msgIpAddr = NetworkConn.getLocalIpAddress(NetworkConn.ADDRESS_IPv4);
        numStatus = 0;
        statusList = null;
        numCM = 0;
        cmList = null;
        cmInfo = null;
        msgText = null;
        sessionId = 0;
    }

    public AppProtMsg(short num, byte type, String idSrc, String idDst, int appSessionId) {
        seqNum = num;
        msgType = type;
        msgSrc = idSrc;
        msgDst = idDst;
        msgIpAddr = NetworkConn.getLocalIpAddress(NetworkConn.ADDRESS_IPv4);
        numStatus = 0;
        statusList = null;
        numCM = 0;
        cmList = null;
        cmInfo = null;
        msgText = null;
        sessionId = appSessionId;
    }

    public AppProtMsg(short num, byte type,  String idSrc, String idDst, int[] cmL, String[] cmI) {
        seqNum = num;
        msgType = type;
        msgSrc = idSrc;
        msgDst = idDst;
        msgIpAddr = null;
        numStatus = 0;
        statusList = null;
        numCM = (short)cmL.length;
        cmList = cmL;
        cmInfo = cmI;
        msgText = null;
        sessionId = 0;
    }

    public AppProtMsg(short num, byte type,  String idSrc, String idDst, String msgString) {
        seqNum = num;
        msgType = type;
        msgSrc = idSrc;
        msgDst = idDst;
        msgIpAddr = null;
        numStatus = 0;
        statusList = null;
        numCM = 0;
        cmList = null;
        cmInfo = null;
        msgText = msgString;
        sessionId = 0;
    }

    public AppProtMsg(String msg) {
        sessionId = 0;
        seqNum = 0;
        msgType = 0;
        msgSrc = null;
        msgDst = null;
        numStatus = 0;
        statusList = null;
        numCM = 0;
        cmList = null;
        cmInfo = null;
        msgText = null;

        parseProtocolMsg(msg);
    }

    public boolean equalTo(short SeqNum, byte MsgType) {
        return SeqNum == getSeqNum() && MsgType == getMsgType();
    }

    public boolean equalTo(short SeqNum, byte MsgType, int sessionId) {
        return SeqNum == getSeqNum() && MsgType == getMsgType() && sessionId ==getSessionId();
    }

    /*
     Determine whether this message is a protocol message that may be pended for processing
     when the call proc engine is currently busy, e.g., doing some other communications task
     */
    public boolean isPendable() {
        switch (getMsgType()) {
            case MSG_PROBEREQUEST:
            case MSG_PTTCALLREQUEST:
            case MSG_PTTFLOORREQUEST:
            case MSG_PTTFLOORTAKE:
            case MSG_PTTYOUSAY:
            case MSG_PTTYOUSAYANNOUNCE:
            case MSG_PTTCALLEND:
            case MSG_TEXTMSGRECEIVE:
            case MSG_VOICEMSGACTIVEARRIVE:
            case MSG_VOICEMSGSILENTARRIVE:
            case MSG_VOICEMSGACTIVERECEIVEANNOUNCE:
            case MSG_VOICEMSGSILENTRECEIVEANNOUNCE: {
                return true;
            }

            /*
            These cases do not need to be pended as they only arise
            in specific cases, e.g., while in a specific call
            that must be handled with the specific call flow in the
            state machine
            */
            case MSG_KEEPALIVE:
            case MSG_REGISTER:
            case MSG_DEREGISTER:
            case MSG_ACK:
            case MSG_NAK:
            case MSG_CMLIST:
            case MSG_TRYPROBE:
            case MSG_TRYRESPONSE:
            case MSG_PTTCALLACCEPT:
            case MSG_PTTCALLREJECT:
            case MSG_PTTCALLANNOUNCE:
            case MSG_PTTFLOORFREE:
            case MSG_PTTFLOORGRANT:
            case MSG_PTTFLOORRELEASE:
            case MSG_PTTFLOORREJECT:
            case MSG_TEXTMSG:
            case MSG_TEXTMSGCONFIRM:
            case MSG_VOICEMSGACTIVETRANSMITREQUEST:
            case MSG_VOICEMSGSILENTTRANSMITREQUEST:
            case MSG_VOICEMSGTRANSMITEND:
            default: {
                return false;
            }
        }
    }

    /*
     Utilities
    */
    public int getSessionId() {
        return sessionId;
    }

    public void setSessionId(int si) {
        sessionId = si;
    }

    public short getSeqNum() {
        return seqNum;
    }

    public void setSeqNum(short sn) {
        seqNum = sn;
    }

    public byte getMsgType() {
        return msgType;
    }

    public void setMsgType(byte mt) {
        msgType = mt;
    }

    public String getMsgSrc() {
        return msgSrc;
    }

    public void setMsgSrc(String src) {
        msgSrc = src;
    }

    public String getMsgDst() {
        return msgDst;
    }

    public void setMsgDst(String dst) {
        msgDst = dst;
    }

    public String getMsgIpAddr() {
        return msgIpAddr;
    }

    public void setMsgIpAddr(String ip) {
        msgIpAddr = ip;
    }

    public short getNumStatus() {
        return numStatus;
    }

    public void setNumStatus(short n) {
        numStatus = n;
    }

    public boolean[] getStatusList() {
        return statusList;
    }

    public void setStatusList(short n, boolean[] sl) {
        statusList = new boolean[n];
        arraycopy(sl, 0, statusList, 0, n);
    }

    public short getNumCM() {
        return numCM;
    }

    public void setNumCM(short m) {
        numCM = m;
    }

    public int[] getCmList() {
        return cmList;
    }

    public void setCMList(short m, int[] cl) {
        cmList = new int[m];
        arraycopy(cl, 0, cmList, 0, m);
    }
    public String[] getCmInfo() {return cmInfo;}
    public void setCmInfo(short m, String[] ci) {
        cmInfo = new String[m];
        arraycopy(ci, 0, cmInfo, 0, m);
    }

    public String getMsgText() {
        return msgText;
    }

    public void setMsgText(String text) {
        msgText = text;
    }

    public void release() {
        sessionId = 0;
        seqNum = 0;
        msgType = 0;
        msgSrc = null;
        msgDst = null;
        numStatus = 0;
        statusList = null;
        numCM = 0;
        cmList = null;
        cmInfo = null;
        msgText = null;
    }

    public void copy(AppProtMsg msg) {       // deep copy
        sessionId = msg.sessionId;
        seqNum = msg.seqNum;
        msgType = msg.msgType;
        msgSrc = msg.msgSrc;
        msgDst = msg.msgDst;
        numStatus = msg.numStatus;
        statusList = new boolean[numStatus];
        arraycopy(msg.statusList, 0, statusList, 0, numStatus);
        numCM = msg.numCM;
        cmList = new int[numCM];
        arraycopy(msg.cmList, 0, cmList, 0, numCM);
        cmInfo = new String[numCM];
        arraycopy(msg.cmInfo, 0, cmInfo, 0, numCM);
        msgText = msg.msgText;
    }

    /*
     Message parser for the application protocol between the application client
     and the server
     */
    private void parseProtocolMsg(String msg) {
        String segments[], msgString;

        segments = msg.split("\\|");
        msgType = getMsgTypeFromString(segments[1]);
        // The first segment of the message must be the message type
        seqNum = getMsgSeqNumFromString(segments[2]);
        // The second message segment must be the sequence number
        numCM = 0;
        if (cmList == null) {
            cmList = new int[]{0, 0, 0, 0, 0, 0, 0, 0};
            cmInfo = new String[]{null, null, null, null, null, null, null, null};
        }
        for (int i = 2; i < segments.length; i++) {
            msgString = segments[i];
            switch (msgString.charAt(0)) {
                case DELIMETER_CHAR_MSGSESSIONID: {
                    sessionId = getMsgSessionIdFromString(msgString);
                    break;
                }
                case DELIMETER_CHAR_MSGSRC: {          // Source ID or address
                    msgSrc = msgString.substring(1);
                    break;
                }
                case DELIMETER_CHAR_MSGDST: {         // Destination ID or address
                    msgDst = msgString.substring(1);
                    break;
                }
                case DELIMETER_CHAR_MSGCOMMETHOD: {         // (Communications) Method ID
                    int cm =parseMsgCM(msgString.charAt(1));
                    if (numCM< 8 && cm > 0 && cm < 9) {
                        cmList[numCM] = cm;
                        cmInfo[numCM] =msgString.substring(2);
                        numCM++;
                    }
                    break;
                }
                case DELIMETER_CHAR_MSGIPADDR: {
                    // Client IP address: is represents the case where the server extracts
                    // status update from client therefore it is not implemented on
                    // the client code
                    String ipAddress = msgString.substring(1);
                    InetAddress inetAddr = null;
                    try {
                        inetAddr = InetAddress.getByName(ipAddress);
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                    if (inetAddr instanceof Inet4Address && ipAddress.length() < 18)
                        msgIpAddr = ipAddress;
                    else if (inetAddr instanceof Inet6Address) {
                        int delim = ipAddress.indexOf('%'); // drop IPv6 port suffix
                        msgIpAddr = (delim < 0) ? ipAddress : ipAddress.substring(0, delim);
                    } else
                        msgIpAddr = null;
                    break;
                }
                case DELIMETER_CHAR_MSGSTATUS: {
                    // Update for the server: this represents the case where the server
                    // extracts status update from client therefore it is not implemented
                    // on the client code
                    boolean[] userStatus = fromStringAppUserState(msgString.substring(1));
                    short numStatus = (short) userStatus.length;
                    setNumStatus(numStatus);
                    setStatusList(numStatus, userStatus);
                    break;
                }
                case DELIMETER_CHAR_MSGTEXT: {
                    String text = msgString.substring(1);
                    if (!text.equals("") && !text.equals(" "))
                        msgText = text;
                    break;
                }
                default:
                    break;
            }
        }
    }

    private byte getMsgTypeFromString(String msgTypeString) {
        String msgString = msgTypeString;
        byte msgType = MSG_ERROR;

        if (msgString != null && msgString != ""
                && msgString.charAt(0) == DELIMETER_CHAR_MSGTYPE) {
            msgString = msgTypeString.substring(1);
            msgType = parseMsgType(msgString);
        }
        return msgType;
    }

    private int getMsgSessionIdFromString(String msgSessionIdString) {
        int len = msgSessionIdString.length();

        if (msgSessionIdString.charAt(0) == DELIMETER_CHAR_MSGSESSIONID) {
            for (int j = 1; j < len; j++) {
                if (!(msgSessionIdString.substring(j, j + 1).matches("[0-9]"))) ;
            }
        }
        // Extract the SeqNum from the reminder of the string
        return (Integer.parseInt(msgSessionIdString.substring(1)));
    }

    private short getMsgSeqNumFromString(String msgSeqNumString) {
        String msgString = msgSeqNumString;
        short seqNum = 0;

        if (msgString.charAt(0) == DELIMETER_CHAR_MSGSEQNUM) {
            for (int j = 1; j < msgString.length(); j++) {
                if (!(msgString.substring(j, j + 1).matches("[0-9]"))) ;
            }
        }
        msgString = msgString.substring(1);
        // Extract the SeqNum from the reminder of the string
        return (Short.parseShort(msgString));
    }

    public void getIpClientAddr(String ipAddress) {
        // Client IP address: is represents the case where the server extracts
        // status update from client therefore it is not implemented on
        // the client code
        InetAddress inetAddr = null;
        try {
            inetAddr = InetAddress.getByName(ipAddress);
        }
        catch (UnknownHostException e) {
            e.printStackTrace();
        }

        if(inetAddr instanceof Inet4Address
                && ipAddress.length()<18)
            msgIpAddr=ipAddress;
        else if(inetAddr instanceof Inet6Address) {
            int delim = ipAddress.indexOf('%'); // drop IPv6 port suffix
            msgIpAddr = (delim < 0) ? ipAddress : ipAddress.substring(0, delim);
        } else
            msgIpAddr=null;
    }

    int parseMsgCM(char c) {
        String str = ""+c;
        return Integer.parseInt(str);
    }

    private byte parseMsgType(String msgTypeString)
    {
        byte result= MSG_ERROR;

        if (MSG_KEEPALIVE_STRING.equals(msgTypeString)) result = MSG_KEEPALIVE;
        if (MSG_REGISTER_STRING.equals(msgTypeString)) result = MSG_REGISTER;
        if (MSG_DEREGISTER_STRING.equals(msgTypeString)) result = MSG_DEREGISTER;
        if (MSG_ACK_STRING.equals(msgTypeString)) result = MSG_ACK;
        if (MSG_NAK_STRING.equals(msgTypeString)) result = MSG_NAK;
        if (MSG_PROBEREQUEST_STRING.equals(msgTypeString)) result = MSG_PROBEREQUEST;
        // Request from the server
        if (MSG_CMLIST_STRING.equals(msgTypeString)) result = MSG_CMLIST;
        if (MSG_TRYPROBE_STRING.equals(msgTypeString)) result = MSG_TRYPROBE;
        if (MSG_TRYRESPONSE_STRING.equals(msgTypeString)) result = MSG_TRYRESPONSE;
        if (MSG_PROBERESPWITHCM_STRING.equals(msgTypeString)) result = MSG_PROBERESPWITHCM;
        if (MSG_PROBEMATCH_STRING.equals(msgTypeString)) result = MSG_PROBEMATCH;
        if (MSG_PTTCALLREQUEST_STRING.equals(msgTypeString)) result = MSG_PTTCALLREQUEST;
        if (MSG_PTTCALLANNOUNCE_STRING.equals(msgTypeString)) result = MSG_PTTCALLANNOUNCE;
        if (MSG_PTTCALLACCEPT_STRING.equals(msgTypeString)) result = MSG_PTTCALLACCEPT;
        if (MSG_PTTCALLREJECT_STRING.equals(msgTypeString)) result = MSG_PTTCALLREJECT;
        if (MSG_PTTFLOORREQUEST_STRING.equals(msgTypeString)) result = MSG_PTTFLOORREQUEST;
        if (MSG_PTTFLOORGRANT_STRING.equals(msgTypeString)) result = MSG_PTTFLOORGRANT;
        if (MSG_PTTFLOORRELEASE_STRING.equals(msgTypeString)) result = MSG_PTTFLOORRELEASE;
        if (MSG_PTTFLOORFREE_STRING.equals(msgTypeString)) result = MSG_PTTFLOORFREE;
        if (MSG_PTTFLOORTAKE_STRING.equals(msgTypeString)) result = MSG_PTTFLOORTAKE;
        if (MSG_PTTFLOORREJECT_STRING.equals(msgTypeString)) result = MSG_PTTFLOORREJECT;
        if (MSG_PTTCALLEND_STRING.equals(msgTypeString)) result = MSG_PTTCALLEND;
        if (MSG_PTTYOUSAY_STRING.equals(msgTypeString)) result = MSG_PTTYOUSAY;
        if (MSG_PTTYOUSAYCONFIRM_STRING.equals(msgTypeString)) result = MSG_PTTYOUSAYCONFIRM;
        if (MSG_PTTYOUSAYREJECT_STRING.equals(msgTypeString)) result = MSG_PTTYOUSAYREJECT;
        if (MSG_PTTYOUSAYANNOUNCE_STRING.equals(msgTypeString)) result = MSG_PTTYOUSAYANNOUNCE;
        if (MSG_TEXTMSG_STRING.equals(msgTypeString)) result = MSG_TEXTMSG;
        if (MSG_TEXTMSGRECEIVE_STRING.equals(msgTypeString)) result = MSG_TEXTMSGRECEIVE;
        if (MSG_TEXTMSGCONFIRM_STRING.equals(msgTypeString)) result = MSG_TEXTMSGCONFIRM;
        if (MSG_VOICEMSGACTIVETRANSMITREQUEST_STRING.equals(msgTypeString)) result = MSG_VOICEMSGACTIVETRANSMITREQUEST;
        if (MSG_VOICEMSGSILENTTRANSMITREQUEST_STRING.equals(msgTypeString)) result = MSG_VOICEMSGSILENTTRANSMITREQUEST;
        if (MSG_VOICEMSGTRANSMITEND_STRING.equals(msgTypeString)) result = MSG_VOICEMSGTRANSMITEND;
        if (MSG_VOICEMSGTRANSMITCONFIRM_STRING.equals(msgTypeString)) result = MSG_VOICEMSGTRANSMITCONFIRM;
        if (MSG_VOICEMSGACTIVEARRIVE_STRING.equals(msgTypeString)) result = MSG_VOICEMSGACTIVEARRIVE;
        if (MSG_VOICEMSGSILENTARRIVE_STRING.equals(msgTypeString)) result = MSG_VOICEMSGSILENTARRIVE;
        if (MSG_VOICEMSGACTIVERECEIVEANNOUNCE_STRING.equals(msgTypeString)) result = MSG_VOICEMSGACTIVERECEIVEANNOUNCE;
        if (MSG_VOICEMSGSILENTRECEIVEANNOUNCE_STRING.equals(msgTypeString)) result = MSG_VOICEMSGSILENTRECEIVEANNOUNCE;
        if (MSG_VOICEMSGRECEIVEREADY_STRING.equals(msgTypeString)) result = MSG_VOICEMSGRECEIVEREADY;
        if (MSG_VOICEMSGRECEIVEFINISH_STRING.equals(msgTypeString)) result = MSG_VOICEMSGRECEIVEFINISH;
        if (MSG_VOICEMSGRECEIVESUCCESS_STRING.equals(msgTypeString)) result = MSG_VOICEMSGRECEIVESUCCESS;
        if (MSG_VOICEMSGRECEIVEFAIL_STRING.equals(msgTypeString)) result = MSG_VOICEMSGRECEIVEFAIL;

        return result;
    }

    /*
     Message composer for the  application protocol from the application client
     to the server
     */
    public String composeProtocolMsg() {
        String msgString = null, str = getMsgTypeString(msgType);

        if (str != null) {
            msgString = DELIMETER_STRING_MSGTYPE +str
                            + DELIMETER_STRING_MSGSEQNUM + Short.toString(seqNum)
                            + DELIMETER_STRING_MSGSRC + msgSrc
                            + DELIMETER_STRING_MSGDST + msgDst;
            if (sessionId !=0)
                msgString = msgString + DELIMETER_STRING_MSGSESSIONID + Integer.toString(sessionId);
            msgString = msgString + DELIMETER_STRING_MSGIPADDR
                + NetworkConn.getLocalIpAddress(NetworkConn.ADDRESS_IPv4);
        }
        return msgString;
    }

    public String composeProtocolMsg(String msgText) {
        String msgString =composeProtocolMsg();

        if (msgString==null || (msgType != MSG_TEXTMSG && msgType != MSG_TEXTMSGRECEIVE))
            return msgString;
        else return msgString+DELIMETER_STRING_MSGTEXT+msgText;
        // include the message in the control (TCP) channel message
    }


    public String composeProtocolMsg(int sessionId) {
        String msgString =composeProtocolMsg();

        if (msgString!=null && sessionId!=0) {
            msgString += DELIMETER_STRING_MSGSESSIONID+Integer.toString(sessionId);
        }
        // include the message in the control (TCP) channel message
        return msgString;
    }


    public String composeProtocolMsg(int[] cmList, String[] cmInfo) {       // cmList must not be null
        String msgString =composeProtocolMsg();
        if (msgString!=null && cmList !=null) {
            String cmStr ="";
            for (int i=0; i< cmList.length; i++) {
                if (cmList[i] > 0 && cmList[i] < 9) {
                    cmStr = cmStr + DELIMETER_STRING_MSGCOMMETHOD + cmList[i]; // Integer.toString(cm);
                    if (cmInfo != null && cmInfo[i] != null)
                        cmStr += cmInfo[i];
                }
            }
            msgString +=cmStr;
        }
        return msgString;
        // include the comm methods in the control (TCP) channel message
    }


    private String getMsgTypeString(byte msgType)
    {
        String result;

        switch (msgType) {
            case MSG_KEEPALIVE: { result = MSG_KEEPALIVE_STRING; break; }
            case MSG_REGISTER: { result = MSG_REGISTER_STRING; break; }
            case MSG_DEREGISTER: { result = MSG_DEREGISTER_STRING; break; }
            case MSG_ACK: { result = MSG_ACK_STRING; break; }
            case MSG_NAK: { result = MSG_NAK_STRING; break; }
            case MSG_CMLIST: { result = MSG_CMLIST_STRING; break; }
            case MSG_PTTCALLREQUEST: { result = MSG_PTTCALLREQUEST_STRING; break; }
            case MSG_PTTCALLANNOUNCE: { result = MSG_PTTCALLANNOUNCE_STRING; break; }
            case MSG_PTTCALLACCEPT: { result = MSG_PTTCALLACCEPT_STRING; break; }
            case MSG_PTTCALLREJECT: { result = MSG_PTTCALLREJECT_STRING; break; }
            case MSG_PTTFLOORREQUEST: { result = MSG_PTTFLOORREQUEST_STRING;  break; }
            case MSG_PTTFLOORGRANT: { result = MSG_PTTFLOORGRANT_STRING; break; }
            case MSG_PTTFLOORRELEASE:{ result = MSG_PTTFLOORRELEASE_STRING; break; }
            case MSG_PTTFLOORFREE:{ result = MSG_PTTFLOORFREE_STRING; break; }
            case MSG_PTTFLOORTAKE:{ result = MSG_PTTFLOORTAKE_STRING; break; }
            case MSG_PTTFLOORREJECT:{ result = MSG_PTTFLOORREJECT_STRING; break; }
            case MSG_PTTCALLEND:{ result = MSG_PTTCALLEND_STRING; break; }
            case MSG_PTTYOUSAY:{ result = MSG_PTTYOUSAY_STRING; break; }
            case MSG_PTTYOUSAYCONFIRM:{ result = MSG_PTTYOUSAYCONFIRM_STRING; break; }
            case MSG_PTTYOUSAYREJECT:{ result = MSG_PTTYOUSAYREJECT_STRING; break; }
            case MSG_PTTYOUSAYANNOUNCE:{ result = MSG_PTTYOUSAYANNOUNCE_STRING; break; }
            case MSG_TRYPROBE: { result = MSG_TRYPROBE_STRING; break; }
            case MSG_TRYRESPONSE:  { result = MSG_TRYRESPONSE_STRING; break;}
            case MSG_PROBERESPWITHCM:  { result = MSG_PROBERESPWITHCM_STRING; break;}
            case MSG_PROBEMATCH:  { result = MSG_PROBEMATCH_STRING; break;}
            case MSG_PROBEREQUEST: { result = MSG_PROBEREQUEST_STRING; break; }
            case MSG_TEXTMSG:{ result = MSG_TEXTMSG_STRING; break; }
            case MSG_TEXTMSGRECEIVE:{ result = MSG_TEXTMSGRECEIVE_STRING; break; }
            case MSG_TEXTMSGCONFIRM:{ result = MSG_TEXTMSGCONFIRM_STRING; break; }
            case MSG_VOICEMSGACTIVETRANSMITREQUEST:{ result = MSG_VOICEMSGACTIVETRANSMITREQUEST_STRING; break; }
            case MSG_VOICEMSGSILENTTRANSMITREQUEST:{ result = MSG_VOICEMSGSILENTTRANSMITREQUEST_STRING; break; }
            case MSG_VOICEMSGTRANSMITEND:{ result = MSG_VOICEMSGTRANSMITEND_STRING; break; }
            case MSG_VOICEMSGTRANSMITCONFIRM:{ result = MSG_VOICEMSGTRANSMITCONFIRM_STRING; break; }
            case MSG_VOICEMSGACTIVEARRIVE:{ result = MSG_VOICEMSGACTIVEARRIVE_STRING; break; }
            case MSG_VOICEMSGSILENTARRIVE:{ result = MSG_VOICEMSGSILENTARRIVE_STRING; break; }
            case MSG_VOICEMSGACTIVERECEIVEANNOUNCE:{ result = MSG_VOICEMSGACTIVERECEIVEANNOUNCE_STRING; break; }
            case MSG_VOICEMSGSILENTRECEIVEANNOUNCE:{ result = MSG_VOICEMSGSILENTRECEIVEANNOUNCE_STRING; break; }
            case MSG_VOICEMSGRECEIVEREADY:{ result = MSG_VOICEMSGRECEIVEREADY_STRING; break; }
            case MSG_VOICEMSGRECEIVEFINISH:{ result = MSG_VOICEMSGRECEIVEFINISH_STRING; break; }
            case MSG_VOICEMSGRECEIVESUCCESS:{ result = MSG_VOICEMSGRECEIVESUCCESS_STRING; break; }
            case MSG_VOICEMSGRECEIVEFAIL:{ result = MSG_VOICEMSGRECEIVEFAIL_STRING; break; }

            default: { result =null; break; }
        }
        return result;
    }

    public String toStringAppUserState(boolean[] userState) {
        StringBuilder arToStr = new StringBuilder();
        if (userState.length > 0) {
            char cUserState = userState[0]? '1':'0';
            arToStr.append(cUserState);
            for (int i=1; i<userState.length; i++) {
                cUserState = userState[i]? '1':'0';
                arToStr.append(cUserState);
            }
        }
        return arToStr.toString();
    }

    public boolean[] fromStringAppUserState(String userStateString) {

        int len = userStateString.length();
        int size = (len < MAX_STATUS_VECTOR) ? len : MAX_STATUS_VECTOR;
        boolean[] userState = new boolean[size];

        for (int i =0; i< size; i++) {
            userState[i] = (userStateString.charAt(i)!='0');
        }
        return userState;
    }
}
