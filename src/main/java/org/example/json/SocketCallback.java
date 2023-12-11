package org.example.json;

import java.io.IOException;
//Server类接收信息回调
public interface SocketCallback {
    void onResponse(String response);

    void onFailure(IOException e);

    void handleMessage();
}
