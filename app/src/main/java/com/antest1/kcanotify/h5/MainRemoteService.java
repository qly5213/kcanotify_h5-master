package com.antest1.kcanotify.h5;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import javax.annotation.Nullable;

public class MainRemoteService extends Service {

    private String currRespData;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return serviceBinder;
    }

    private IWebviewBinder.Stub serviceBinder = new IWebviewBinder.Stub() {

        @Override
        public void handleJsFunc(String path, String params, String respData, boolean end) throws RemoteException { //方法执行在子线程中
            currRespData = currRespData + respData;
            if(!end){
                return;
            }
            String result = currRespData;
            KcaVpnData.renderToHander(path, params, result);
            currRespData = "";
        }

    };
}
