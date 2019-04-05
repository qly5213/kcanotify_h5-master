package com.antest1.kcanotify.h5;


import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class ChatDialogUtils {
    private Dialog dialog;
    private View inflate;
    private OkHttpClient mOkHttpClient;
    private List<ChatMsgObject> msgList;
    private WebSocket mWebSocket;
    private Gson gson = new Gson();
    private ChatAdapter chatAdapter;
    private Context context;
    private Handler mWebSocketHandler;
    private boolean connected;
    private final EditText msgEditText;
    private final Button sendBtn;
    private boolean connecting = false;
    private String showName;
    private String imageSize;
    private ChatListener listener;
    private final ImageButton selectImageBtn;
    private int retryConnCnt;
    private final ListView chatMsgListView;

    public ChatDialogUtils(Context context, ChatListener listener, String imageSize) {
        msgList = new ArrayList<>();
        this.context = context;
        showName = "游客 - 未登录";
        this.imageSize = imageSize;
        this.listener = listener;
        mWebSocketHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                ChatMsgObject chatMsgObject = (ChatMsgObject)msg.obj;
                msgList.add(chatMsgObject);
                if(msgList.size() > 100){
                    msgList.remove(0);
                }
                chatMsgListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
                chatAdapter.notifyDataSetChanged();
                listener.onMessage(chatMsgObject);
                return false;
            }
        });
        inflate = LayoutInflater.from(context).inflate(R.layout.view_dialog_chat, null);
        dialog = new Dialog(context, R.style.DialogLeft);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setContentView(inflate);
        webSocketConnect();
        chatAdapter = new ChatAdapter(context, R.layout.view_chat_item, msgList);
        chatMsgListView = dialog.findViewById(R.id.lv_main_msg);
        chatMsgListView.setAdapter(chatAdapter);

        msgEditText = dialog.findViewById(R.id.msg_text_view);
        sendBtn = dialog.findViewById(R.id.send_btn);
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = msgEditText.getText().toString();
                if(msg != null && !msg.equals("") && mWebSocket != null){
                    ChatMsgObject chatMsgObject = new ChatMsgObject(showName, msg);
                    msgList.add(chatMsgObject);
                    if(msgList.size() > 100){
                        msgList.remove(0);
                    }
                    chatMsgListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
                    chatAdapter.notifyDataSetChanged();
                    mWebSocket.send(gson.toJson(chatMsgObject));
                    msgEditText.setText("");
                }
            }
        });
        selectImageBtn = dialog.findViewById(R.id.select_image_btn);
        selectImageBtn.setOnClickListener(v -> {
            listener.onSelectMsg();
        });


        Window window = dialog.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();
        wlp.gravity = Gravity.LEFT;
        wlp.width = 800;
        wlp.height = WindowManager.LayoutParams.MATCH_PARENT;
        window.setAttributes(wlp);
    }

    private class ClientWebSocketListener extends  WebSocketListener {
        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            mWebSocket=webSocket;
            connected = true;
            connecting = false;
            retryConnCnt = 0;
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            Log.i("KCV", text);
            ChatMsgObject chatMsgObject = gson.fromJson(text, ChatMsgObject.class);
            if(chatMsgObject != null) {
                Message message = Message.obtain();
                message.obj = chatMsgObject;
                mWebSocketHandler.sendMessage(message);
            }
        }

        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            if(null!=mWebSocket){
                mWebSocket.close(1000,"");
                mWebSocket=null;
            }
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            Log.e("KCV", reason);
            connected = false;
            connecting = false;
            if(retryConnCnt < 5) {
                webSocketConnect();
            } else {
                retryConnCnt++;
            }
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, @javax.annotation.Nullable Response response) {
            t.printStackTrace();
            connected = false;
            connecting = false;
            if(retryConnCnt < 5) {
                webSocketConnect();
            } else {
                retryConnCnt++;
            }
        }
    }

    private void webSocketConnect() {
        if(connecting){
            return;
        }
        connecting = true;
        mOkHttpClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        Request request = new Request.Builder()
                .url("ws://www.uyan.pw/websocket")
//                .url("ws://192.168.3.8:9090")
                .build();
        ClientWebSocketListener listener=new ClientWebSocketListener();
        mOkHttpClient.newWebSocket(request,listener);
    }

    public void showLeftChat(String showName) {
        retryConnCnt = 0;
        if(!connected){
            webSocketConnect();
            Toast.makeText(context, "未能连接服务器，请重开聊天窗口！", Toast.LENGTH_LONG).show();
            sendBtn.setBackgroundColor(Color.GRAY);
            sendBtn.setEnabled(false);
            selectImageBtn.setBackgroundColor(Color.GRAY);
            selectImageBtn.setEnabled(false);
        } else {
            sendBtn.setBackgroundColor(Color.parseColor("#52ade9"));
            sendBtn.setEnabled(true);
//            selectImageBtn.setBackgroundColor(Color.parseColor("#52ade9"));
            selectImageBtn.setEnabled(true);
        }
        if(showName != null) {
            this.showName = showName;
        }
        dialog.show();
    }

    public void sendImage(String path) {
        if(!connected){
            webSocketConnect();
            Toast.makeText(context, "未能连接服务器，请重开聊天窗口！", Toast.LENGTH_LONG).show();
            sendBtn.setBackgroundColor(Color.GRAY);
            sendBtn.setEnabled(false);
            selectImageBtn.setBackgroundColor(Color.GRAY);
            selectImageBtn.setEnabled(false);
        } else {
            sendBtn.setBackgroundColor(Color.parseColor("#52ade9"));
            sendBtn.setEnabled(true);
//            selectImageBtn.setBackgroundColor(Color.parseColor("#52ade9"));
            selectImageBtn.setEnabled(true);
        }
        File file = new File(path);
        if(file.length() > Integer.parseInt(imageSize) * 1024){
            Toast.makeText(context, "文件过大，请确保图片大小小于" + imageSize + "KB！", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(path);
            String imageBase64Str = PictureUtils.bitmapToString(bitmap);
            ChatMsgObject chatMsgObject = new ChatMsgObject(showName, ChatMsgObject.MsgTypeCont.MSG_IMAGE, "【图片】我发送了一张图片，请使用新版Kcanotify查看", imageBase64Str);
            msgList.add(chatMsgObject);
            if(msgList.size() > 100){
                msgList.remove(0);
            }
            chatMsgListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
            chatAdapter.notifyDataSetChanged();
            mWebSocket.send(gson.toJson(chatMsgObject));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //关闭dialog时调用
    public void close() {
        if (dialog != null) {
            dialog.hide();
        }
    }

    public boolean isShow(){
        return dialog.isShowing();
    }
}
