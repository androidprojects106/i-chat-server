package com.example;

import java.util.concurrent.ConcurrentHashMap;


/**
 * Created by LPC-Home1 on 3/29/2015.
 */

public class AppUserState {
    // public final static int MAX_USERS_SERVERSUPPORTED = 16;

    private static ConcurrentHashMap<String, AppProtMsgQueue> appProtMsgQueMap;
            // for now in the prototype the MsgQueue can be held in this list
            // (bad for scalability) for the eventual implementation it shall be
            // held in teh separate user HashMap for registered users
    private static ConcurrentHashMap<String, AppUserRecord> userHashMap;

    public AppUserState() {
        appProtMsgQueMap = new ConcurrentHashMap<String, AppProtMsgQueue>();
        userHashMap = new ConcurrentHashMap<String, AppUserRecord>();
    }

    public void addProtMsgQueMap(String userId, AppProtMsgQueue appProtMsgQueue) {
        appProtMsgQueMap.remove(userId);
        appProtMsgQueMap.put(userId, appProtMsgQueue);
    }

    public boolean deleteProtMsgQueMap(String userId) {
        boolean result = appProtMsgQueMap.containsKey(userId);

        if (result)
            appProtMsgQueMap.remove(userId);
        return result;
    }

    public AppProtMsgQueue getActiveUser(String userId) {
        return appProtMsgQueMap.get(userId);
    }

    public AppUserRecord addHashUserRegistration(String userId,
                                                 int sessionId, String userIpAddr, String serverId,
                                                 boolean[] statusList, int[] cmListCapable) {
        AppUserRecord userRecord;
        short statusSize= (statusList==null) ? 0: (short)statusList.length;
        short cmSize = (cmListCapable==null) ? 0: (short)cmListCapable.length;

        if (userHashMap.containsKey(userId)) {
            userRecord = userHashMap.get(userId);
        }
        else {
            userRecord = new AppUserRecord();
            userRecord.setAppUserId(userId);
            userHashMap.put(userId, userRecord);
        }

        if (statusSize>0) {
            userRecord.setNumStatus(statusSize);
            userRecord.setStatusList(statusSize,statusList);
        }
        if (cmSize>0) {
            userRecord.setNumCM(cmSize);
            userRecord.setCMList(cmSize, cmListCapable);
        }
        userRecord.setServerId(serverId);
        userRecord.setUserIpAddr(userIpAddr);
        userRecord.setUpdateLast(System.currentTimeMillis());

        return userRecord;
    }

    public boolean deleteUserHashMap(String userId) {
        boolean result = userHashMap.containsKey(userId);

        if (result)
            userHashMap.remove(userId);
        return result;
    }

    public int getActiveUserCount() {
        return appProtMsgQueMap.size();

        /* return appProtMsgQueList.size(); */
    }

    public AppUserRecord getUserRegistration(String userID) {
        return userHashMap.get(userID);
    }

    public void removeUserRegistration(String userID) {
        userHashMap.remove(userID);
    }
}

