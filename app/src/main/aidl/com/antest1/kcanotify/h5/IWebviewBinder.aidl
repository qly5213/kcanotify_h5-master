// IWebviewBinder.aidl
package com.antest1.kcanotify.h5;

// Declare any non-default types here with import statements

interface IWebviewBinder {
        /**
         * 处理JS调用Java的方法
         */
        void handleJsFunc(String path, String params, String respData, boolean end);
}
