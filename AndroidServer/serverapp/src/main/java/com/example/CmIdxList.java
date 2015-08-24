package com.example;

import java.util.LinkedList;

/**
 * Created by LPC-Home1 on 3/29/2015.
 */
public class CmIdxList {

    public static final int CM_TYPE_NONE = 0;
    public static final int CM_TYPE_PHONE = 1;
    public static final int CM_TYPE_VOIP = 2;
    public static final int CM_TYPE_PTT = 3;
    public static final int CM_TYPE_TEXTMSG = 4;
    public static final int CM_TYPE_VOICEMSG_ACTIVE = 5;
    public static final int CM_TYPE_VOICEMSG_SILENT = 6;
    public static final int CM_TYPE_EMAIL = 7;

    public final static LinkedList<CmIdx> cmIdxLinkedList;


    // Indexed IDs for Comm Methods supported by the application
    static {
        CmIdx cmIdx;

        cmIdxLinkedList = new LinkedList<CmIdx>();

        cmIdx = new CmIdx(CM_TYPE_PHONE,"Phone");                           cmIdxLinkedList.add(cmIdx);
        cmIdx = new CmIdx(CM_TYPE_VOIP,"VoIP");                             cmIdxLinkedList.add(cmIdx);
        cmIdx = new CmIdx(CM_TYPE_PTT,"Push-to-Talk");                      cmIdxLinkedList.add(cmIdx);
        cmIdx = new CmIdx(CM_TYPE_TEXTMSG,"Text Message");                  cmIdxLinkedList.add(cmIdx);
        cmIdx = new CmIdx(CM_TYPE_VOICEMSG_ACTIVE,"Voice Message");         cmIdxLinkedList.add(cmIdx);
        cmIdx = new CmIdx(CM_TYPE_VOICEMSG_SILENT,"Voice Message Muted");   cmIdxLinkedList.add(cmIdx);
        cmIdx = new CmIdx(CM_TYPE_EMAIL,"Email");                           cmIdxLinkedList.add(cmIdx);
    }

    CmIdxList() {}              // blank constructor

    public static String getCmIdxString(int idx) {

        for (CmIdx cmidx: cmIdxLinkedList) {
            if (cmidx.getIdx() == idx)
                return cmidx.getCmString();
        }
        return null;
    }

}

