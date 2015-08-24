package com.example;

/**
 * Created by LPC-Home1 on 3/29/2015.
 */
public class CmIdx {
    private int Idx;
    private String CmString;

    public CmIdx(int i, String cmstr) {
        Idx =i;
        CmString =cmstr;
    }

    public int getIdx() {return Idx;}
    public String getCmString() {return CmString;}
}