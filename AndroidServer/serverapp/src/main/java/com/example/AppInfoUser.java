package com.example;

/**
 * Created by LPC-Home1 on 3/30/2015.
 */
public class AppInfoUser {

    private String appUserID;           // used to identify this user and device and the app instance
    private String nameFirst;
    private String nameLast;
    private String noTel;               // MSDN in free form (may be rejected by Phone app)
    private String url;                 // VoIP url used for VoIP, PTT, and EMail
    private long activeLast =0;         // time of last activity for this user

    public AppInfoUser() {
        appUserID = "appAndroidUser1";
        nameFirst = "user1";
        nameLast = "Android";
        noTel = "++1-408-390-1770";
        url = "user1Android@android.com";
    }
    /*
        Utilities
     */

    public String getAppUserID() { return appUserID; }
    public void setAppUserID(String string) {appUserID =string; }
    public String getNameFirst() { return nameFirst; }
    public void setnameFirst(String string) {nameFirst =string; }
    public String getNameLast() { return nameLast; }
    public void setNameLast(String string) {nameLast =string; }
    public String getNoTel() { return noTel; }
    public void setNoTel(String string) {noTel =string; }
    public String getUrl() { return url; }
    public void setUrl(String string) {url =string; }

    public String getUserDisplayInfo() {
        String displayInfo=""
                +"First Name:                   "+nameFirst+" \n"
                +"Last Name:                    "+nameLast+" \n"
                +"Phone:                        "+noTel+" \n"
                +"URL:                              "+url+" \n"
                +"(VoIP/PTT/Mail)";

        return displayInfo;
    }
}
