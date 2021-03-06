package com.daoshengwanwu.android.tourassistant.utils;


public class AppUtil {
    public static final class SharingServer {
        public static final String HOST = "123.206.34.83";//192.168.1.125
        public static final int PORT = 9688;
        public static final String COMMAND_SET_LOCATION = "set_location";
        public static final String REQUEST_MEMBER_LOCATION = "request_location";
        public static final String REQUEST_STOP = "over";
        public static final String COMMAND_SET_USERID = "userId";
        public static final String COMMAND_SET_GROUPID = "groupId";
        public static final String SEPARATOR_LOCATION_DIFFER_MEMBER = "#";
        public static final String SEPARATOR_LOCATION_LAT_LON = ",";
        public static final String SEPARATOR_LOCATION_ID_LOC = "->";
        public static final String SEPARATOR_COMMAND_CONTENT = ":";
        public static final String RECEIVED_MEMBER_LOCATIONS = "member_locations";
    }

    public static final class JFinalServer {
        public static final String HOST = "123.206.34.83";//192.168.1.125
        public static final int PORT = 9638;
        public static final String xyurl = "http://"+AppUtil.JFinalServer.HOST+":"+AppUtil.JFinalServer.PORT+ "/user/getInformation";
        public static final String xyurl2 = "http://"+AppUtil.JFinalServer.HOST+":"+AppUtil.JFinalServer.PORT+ "/team/getInformation";
        public static final String xyurl3 = "http://"+AppUtil.JFinalServer.HOST+":"+AppUtil.JFinalServer.PORT+ "/team/joinTeam";
        public static final String xyurl4 = "http://"+AppUtil.JFinalServer.HOST+":"+AppUtil.JFinalServer.PORT+ "/user/editHeadPic";
    }

    public static final class User {
        public static String USER_ID = "";
        public static String USER_NAME = "";
        public static String USER_IMG = "";
        public static String USER_GENDER = "";

    }

    public static final class Group {
        public static String GROUP_ID = "";
        public static String CHAT_TEAM_ID="";
        public static String GROUP_NAME = "";
        public static String GROUP_CAPTIAN = "";
    }
}
