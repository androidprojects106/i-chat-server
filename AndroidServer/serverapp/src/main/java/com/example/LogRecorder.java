package com.example;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by LPC-Home1 on 4/24/2015.
 */
public class LogRecorder {

    LogRecorder() {
    }              // blank constructor

    public static String fileMediaFormat(String userId,int sessionIdIn) {
        return userId.replace('@','_')+"_" +Integer.toString(sessionIdIn)
                +"_audio_record.bin";
    }

    public static void writeVoiceMsgDataToFile(byte[] bytes, String fileName) {
        try {
            BufferedOutputStream bufO=
                    new BufferedOutputStream(new FileOutputStream(fileName, true));
            bufO.write(bytes);
            bufO.flush();
            bufO.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeVoiceMsgDataToStream(byte[] bytes, BufferedOutputStream bufO) {
        try {
            bufO.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeStringToFile(String str, String fileName, String tag) {
        FileWriter fw = null;
        BufferedWriter bw = null;

        try {
            fw = new FileWriter(new File(fileName).getAbsoluteFile(), true);
            bw = new BufferedWriter(fw);

            long timeNow = System.currentTimeMillis();
            bw.write(timeNow + ": " + tag +  str);

            bw.newLine();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
