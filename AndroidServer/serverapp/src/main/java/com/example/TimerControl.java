package com.example;

/**
 * Created by LPC-Home1 on 4/25/2015.
 */
public class TimerControl {
    public static final int TIMER_PTTMAXSEGMENT= (!Constants.DEBUGGING) ? 20000 :20000;    // DEBUG 300000
    // 30 sec maximum PTT segment/sentence length
    public static final int TIMER_PTTCHATGAP = 2000;
    // 1 second gap assumed (same debugging or not)
    public static final int TIMER_PTTAUDIOINITIAL =10000;
    // 10 second initial for the audio to appear (same debugging or not)
    public static final int TIMER_PTTCLIENTRELEASE =5000;
    // 5 second initial for the source/user to release (same debugging or not)
    public static final int TIMER_PTTJITTERWAIT =(!Constants.DEBUGGING) ? 2000 :2000;     // DEBUG 1000
    // 500 milliseconds wait for likely jitter and gap in PTT audio
    public static final int TIMER_PTTHOLDOFF =(!Constants.DEBUGGING) ? 20000 :20000;   // DEBUG 300000;
    public static final int TIMER_PTTBARCALL =(!Constants.DEBUGGING) ? 3000 :3000;   // DEBUG 300000;  // interval for call attempts

    public static final int TIMER_KEEPALIVE = 300000;           // 300 sec keepalive time (5 minutes keepAlive)  // 900 sec for testing
    public static final int TIMER_KEEPALIVE_FREQ = 100000;      // keepalive frequency/interval (1/3 minutes keepAlive)

    public static final int TIMER_SERVERRESPONSE = (!Constants.DEBUGGING) ? 5000:300000;
            // server response time (5 sec)  // (300 sec) for testing
    public static final int TIMER_CLIENTRESPONSE = (!Constants.DEBUGGING) ? 5000:300000;
            // client response time (5 sec)  // (300 sec) for testing
    public static final int TIMER_TARGETRESPONSE = (!Constants.DEBUGGING) ? 60000:300000;
            // user (if manual) response time (60 sec)          // (300 sec) for testing
    public static final int TIMER_PROBERESPONSE  =              // user (if manual) response time (60 sec)  // (300 sec) for testing
                                TIMER_TARGETRESPONSE+5000;      // seconds more than the response from the target client
    public static final int TIMER_VOICEMSGWAITINITIAL = (!Constants.DEBUGGING) ? 60000:300000;
            // 60 sec // (300 sec) for testing

    public static final int TIMER_VOICEMSGWAITTOSTOP = 2000;    // 2 sec for speech gaps at the most
    public static final int TIMER_VOICEMSGCLIENTTOCLEAR = 2000; // 2 sec for client signaling msg to clear
    public static final int MAX_WAITCONNECTIONTIME = 5000;      // 5 sec // (300 sec) for testing
    public static final int MAX_TIMEPOLLING = 400;              // polling for message time in ready state
    private boolean timerIsSet;
    private long timeout, prevTime;

    public TimerControl() {
        resetTimer();
    }

    public void setTimer(long t) {
        prevTime = System.currentTimeMillis();
        timeout = t;
        timerIsSet = true;
    }

    public boolean isTimeout() {

        return timerIsSet && (System.currentTimeMillis() - prevTime) >= timeout;
    }

    public void resetTimer () {
        timerIsSet = false;
        prevTime =0;
        timeout =0;
    }
}
