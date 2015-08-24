package com.example;

/**
 * Created by LPC-Home1 on 6/24/2015.
 */
public class UtilsHelpers {

    private UtilsHelpers() { }

    public static short incAppSeqNum(short appSeqNum) {
        if (appSeqNum<0x7FFF)
            appSeqNum++;                    // sequence numbers are expected to wrap around
        else
            appSeqNum=0;
        return appSeqNum;
    }

}
