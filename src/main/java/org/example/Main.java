package org.example;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.example.json.Action;
import org.example.json.BaseJson;
import org.example.json.SocketCallback;
import org.example.util.Logcat;

import java.awt.*;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException, AWTException {
        InetAddress address = InetAddress.getByName("LAPTOP-IPSM6TUU");//这里输入自己的电脑名
        System.out.println(address);
        Robot robot = new Robot();
        Server server = new Server();
        server.setSocketCallback(new SocketCallback() {
            private BaseJson<Action> json;
            @Override
            public void onResponse(String response) {
                Gson gson = new Gson();
                Type type = new TypeToken<BaseJson<Action>>(){}.getType();
                json = gson.fromJson(response, type);//将数据转化为gson对象
                if (json != null && json.getType() != Server.TYPE_KEEP_ALIVE){//检查如果该条消息不是保活消息，则开始处理消息
                    server.startMessageHandler();
                }
            }

            @Override
            public void onFailure(IOException e) {
                Logcat.print(e.getMessage());
            }

            @Override
            public void handleMessage() {
                if (json == null){
                    return;
                }
                List<Integer> codes = json.getData().getCodes();//获取keyEvent操作码
                for (int code : codes){
                    robot.keyPress(code);//顺序执行按键操作
                }
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                for (int code : codes){
                    robot.keyRelease(code);//顺序松开按键
                }
            }
        });
    }
}