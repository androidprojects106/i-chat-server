package com.example;

/**
 * Created by LPC-Home1 on 6/22/2015.
 */
public class Constants {
    public final static boolean DEBUGGING =true;

    public final static int INIT =0;
    public final static int NAK_RECEIVED =1;
    public final static int NAK_RECEIVED1 =11;
    public final static int NAK_RECEIVED2 =12;
    public final static int NAK_RECEIVED3 =13;

    public final static int NAK_SENT=2;
    public final static int NAK_SENT1=21;
    public final static int NAK_SENT2=22;
    public final static int NAK_SENT3=23;

    public final static int ILLEGAL_MSG =50;
    public final static int ILLEGAL_MSG1 =51;
    public final static int ILLEGAL_MSG2 =52;
    public final static int ILLEGAL_MSG3 =53;
    public final static int TIMEDOUT_MSG =100;
    public final static int TIMEDOUT_MSG1 =101;
    public final static int TIMEDOUT_MSG2 =102;
    public final static int TIMEDOUT_MSG3 =103;

    public final static int EMPTYLIST_RESULT=1000;
    public final static int EMPTYLIST_RESULT1=1001;
    public final static int EMPTYLIST_RESULT2=1002;

    public final static int NOTCREATED_USER=2000;
    public final static int NOTFOUND_USER=2001;
    public final static int NOTFOUND_USER1=2002;
    public final static int NOTFOUND_USER2=2003;
    public final static int NOTFOUND_USER3=2004;
    public final static int NOTFOUND_USER4=2005;
    public final static int NOTFOUND_USER5=2006;
    public final static int NOTFOUND_USER6=2007;
    public final static int NOTFOUND_DATA=2015;
    public final static int NOTFOUND_UDP=2016;
    public final static int NOTFOUND_TCP=2017;
    public final static int AUDIOALREADYRUNNING =2018;
    public final static int AUDIONOTRUNNING =2019;

    public final static int APP_FINISH_ABNORMAL =90000;
    public final static int APP_FINISH_NORMAL =90001;
    public final static int APP_REGISTRATION_FAILED =90002;
    public final static int APP_DEREGISTRATION_SUCCESS =90003;
    public final static int APP_DEREGISTRATION_FAILED =90004;
    public final static int APP_KEEPALIVE_TIMEOUT =90005;

    public Constants(){ }
}
