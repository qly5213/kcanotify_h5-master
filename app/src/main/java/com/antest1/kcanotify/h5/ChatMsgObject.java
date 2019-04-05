package com.antest1.kcanotify.h5;


import java.text.SimpleDateFormat;
import java.util.Date;

public class ChatMsgObject {
    private String userName;
    private int msgType;
    private String msg;
    private String imageMsg;

    public static class MsgTypeCont{

        public static final int MSG_TEXT = 1;
        public static final int MSG_IMAGE= 2;
    }

    public String getImageMsg() {
        return imageMsg;
    }

    public void setImageMsg(String imageMsg) {
        this.imageMsg = imageMsg;
    }

    public ChatMsgObject(String userName, int msgType, String msg, String imageMsg) {
        this.userName = userName + "  " + new SimpleDateFormat("HH:mm:ss").format(new Date());
        this.msgType = msgType;
        this.msg = msg;
        this.imageMsg = imageMsg;
    }

    public ChatMsgObject(String userName, int msgType, String msg) {
        this.userName = userName + "  " + new SimpleDateFormat("HH:mm:ss").format(new Date());
        this.msgType = msgType;
        this.msg = msg;
    }

    public ChatMsgObject(String userName, String msg){
        this.userName = userName + "  " + new SimpleDateFormat("HH:mm:ss").format(new Date());
        this.msg = msg;
        msgType = MsgTypeCont.MSG_TEXT;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int getMsgType() {
        return msgType;
    }

    public void setMsgType(int msgType) {
        this.msgType = msgType;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
