package com.example;

/**
 * Created by LPC-Home1 on 3/29/2015.
 */
class AppProtData {

    public static final byte UDPTYPE_AUDIOMSG_PCM16 = 0x01;
    public static final byte UDPTYPE_AUDIOVOICE_PCM16 = 0x02;
    public static final byte UDPTYPE_VIDEOMSG_H264 = 0x03;
    public static final byte UDPTYPE_PTTCHATFINISH = 0x13;
    public static final byte UDPTYPE_PTTFLOORPROTECTED = 0x14;
    public static final byte UDPTYPE_PTTFLOORAVAILABLE = 0x15;
    public static final byte UDPTYPE_FLOORCATCH = 0x16;
    public static final byte UDPTYPE_FLOORRELEASE = 0x17;
    public static final byte UDPTYPE_TRIGGER = 0x7F;


    public static final int APPPROTOCOLDATA_OVERHEADSIZE = 11;
    public static final byte[] UDP_TRIGGERSIGNATURE =
            new byte[]{0x01, 0x03, 0x01, 0x01, 0x7F, 0x7F, 0x7F};

    private int sessionId;
    private short seqNum;
    private byte dataType;
    private long timeStampSrc;
    private byte[] data;
    private int dataSize;
    private long timeStampDst;

        // packet payload size plus 7 byte for the type, 2 for the seqNum,
        // and 4 for timestamp at the source

    /*
     Constructor for creating a UDP packet template from received data block "dataBlock"
     notice the time stamp @Src is local and the time stamp @Dst to be filled
     */
    public AppProtData(short num, byte type,int appSessionId) {
        sessionId =appSessionId;
        seqNum = num;
        dataType = type;
        timeStampSrc =System.currentTimeMillis();
        data = null;
        dataSize =0;
        timeStampDst =0l;
    }

    /*
     Constructor for creating a UDP packet template from received data block "dataBlock"
     notice the time stamp @Src is local and the time stamp @Dst to be filled
     */
    public AppProtData(short num, byte type, byte[] dataBlock, int size, int appSessionId) {
        sessionId =appSessionId;
        seqNum = num;
        dataType = type;
        timeStampSrc = System.currentTimeMillis();
        data = dataBlock;
        dataSize =size;
        timeStampDst =0l;
    }

    /*
     Message parser constructor for the application protocol between the application client
     and the server
    */
    public AppProtData(byte[] bytes, int lengthUdpPacket) {
        int size =lengthUdpPacket - APPPROTOCOLDATA_OVERHEADSIZE;     // at least N bytes of overhead data

        if (size >0) {
            data = new byte[size];

            // sessionId
            sessionId = (bytes[0] & 0xFF) | (bytes[1] & 0xFF) << 8 | (bytes[2] & 0xFF) << 16 | bytes[3] << 24;
            // sequence number
            seqNum = (short)((bytes[4] & 0xFF) | ((bytes[5] & 0x007F))<<8);
            dataType = bytes[6];
            timeStampSrc = (bytes[7] & 0xFF) | (bytes[8] & 0xFF) << 8 | (bytes[9] & 0xFF) << 16 | bytes[10] << 24;
            System.arraycopy(bytes, APPPROTOCOLDATA_OVERHEADSIZE, data, 0, size);
            dataSize =size;
        }
        else {
            sessionId = 0;
            seqNum = 0;
            dataType = 0;
            timeStampSrc =0;
            data = null;
            dataSize =0;
        }
        timeStampDst = System.currentTimeMillis() & 0x7FFFFFFF;     // 63-bit timestamp used
    }

    public boolean isValid() {
        return data != null;
    }

    /*
     Message composer for the  application protocol from the application client
     to the server
     */
    public byte[] composeUdpPacket() {
        byte[] bytes = new byte[dataSize+
                APPPROTOCOLDATA_OVERHEADSIZE];            // 11 overhead bytes

        // sessionId
        bytes[0]=(byte)(sessionId & 0x000000FF);
        bytes[1]=(byte)((sessionId >> 8) & 0x000000FF);
        bytes[2]=(byte)((sessionId >> 16) & 0x000000FF);
        bytes[3]=(byte)((sessionId >> 24) & 0x000000FF);
        // sequence number
        bytes[4]=(byte)(seqNum & 0x00FF);
        bytes[5]=(byte)((seqNum >> 8) & 0x007F);
        bytes[6]=dataType;
        bytes[7] = (byte)(timeStampSrc & 0x00000000000000FF);
        bytes[8] = (byte)((timeStampSrc >> 8) & 0x00000000000000FF);
        bytes[9] = (byte)((timeStampSrc >> 16) & 0x00000000000000FF);
        bytes[10] = (byte)((timeStampSrc >> 24) & 0x000000000000007F);
        System.arraycopy(data,0, bytes, APPPROTOCOLDATA_OVERHEADSIZE, dataSize);

        return bytes;
    }

    /*
     Utilities
    */
    public int getSessionId () {return sessionId;}
    public void setSessionId (int si) {sessionId =si;}
    public short getSeqNum () {return seqNum;}
    public void setSeqNum (short sn) {seqNum =sn;}
    public byte getDataType () {return dataType;}
    public void setDataType (byte dt) {dataType =dt;}
    public long getTimeStampSrc () {return timeStampSrc;}
    public void setTimeStampSrc (long ts) {timeStampSrc =ts;}
    public byte[] getData () {return data;}
    public void setData (byte[] d) {data =d;}
    public int getDataSize () {return dataSize;}
    public void setDataSize (int size) {dataSize =size;}
    public long getTimeStampDst () {return timeStampDst;}
    public void setTimeStampDst (long ts) {timeStampDst =ts;}

}
