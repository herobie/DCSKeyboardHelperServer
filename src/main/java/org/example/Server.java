package org.example;

import com.google.gson.Gson;
import org.example.json.BaseJson;
import org.example.json.SocketCallback;
import org.example.util.Logcat;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private ServerSocket ss;
    private Socket socket;
    private InputStream inputStream;

    private InputStreamReader inputStreamReader;

    private BufferedReader bufferedReader;
    private Thread msgReceiver;
    private boolean isConnected = false;//与客户端连接情况
    private boolean isDestroy = false;//判断是否执行了destroy方法，用于鉴定是否要结束主线程
    //消息类型
    public static final int TYPE_KEEP_ALIVE = 0;//保活消息
    public static final int TYPE_ACTION = 1;//用户操作消息

    private SocketCallback socketCallback;

    private final char endIcon = '\n';//单次数据传输终止符

    public Server() throws IOException {
        init();
    }

    public void init() throws IOException {
        //创建对象
        ss = new ServerSocket(1688);
        Logcat.print("等待连接");
        //监听Client连接
        socket = ss.accept();
        if (socket != null){
            Logcat.print("设备连接成功");
            inputStream = socket.getInputStream();
            inputStreamReader = new InputStreamReader(inputStream);
            bufferedReader = new BufferedReader(inputStreamReader);
            isDestroy = false;
            isConnected = true;
            msgReceiver = new Thread(new MessageReceiver());//开启接收消息的线程
            msgReceiver.start();
        }
    }

    //开启信息处理线程
    public void startMessageHandler(){
        new Thread(new MessageHandler()).start();
    }

    //检查客户端是否仍在连接
    public boolean isClientAlive(){
        Logcat.print("正在检查客户端是否连接");
        if (!isConnected || isDestroy || socket.isClosed()){
            return false;
        }
        try {
            OutputStream os = socket.getOutputStream();
            os.write(new Gson().toJson(new BaseJson<>(TYPE_KEEP_ALIVE)).getBytes());
            os.flush();
            Logcat.print("客户端连接状态正常");
            return true;
        } catch (IOException e) {
            //如果消息无法发出，则可能是客户端断连了
            e.printStackTrace();
            if (e.getMessage().equals("你的主机中的软件中止了一个已建立的连接。")){
                Logcat.print("连接终止");
            }else if (e.getMessage().equals("Connection reset by peer")){
                Logcat.print("connection reset32");
            }
            Logcat.print("客户端连接出现问题");
            return false;
        }
    }

    public void disconnect(){
        Logcat.print("连接关闭");
        if (isConnected) isConnected = false;
    }

    public void destroy(){
        Logcat.print("执行Destroy");
        if (isDestroy){
            return;
        }
        //关闭连接和线程，如果socket等对象为null就直接return
        try {
            isDestroy = true;
            isConnected = false;
            msgReceiver.interrupt();
            socket.getOutputStream().flush();
            socket.getOutputStream().close();
            socket.close();
            ss.close();
            bufferedReader.close();
            inputStreamReader.close();
            inputStream.close();
            socket = null;
            ss = null;
        }catch (NullPointerException | IOException e){
            e.printStackTrace();
        }
    }

    /**
     * 客户端接收消息线程
     */
    private class MessageReceiver implements Runnable{
        @Override
        public void run() {
            while (isConnected && !isDestroy){
                StringBuilder builder = new StringBuilder();
                String response;
                try {
                    int b;
                    while (( b = bufferedReader.read()) != -1 && isConnected && !isDestroy){
                        builder.append((char) b);
                        if ((char) b == endIcon){//检测到数据传输终止符就跳出循环开始解析
                            break;
                        }
                    }
                    response = builder.toString();//拼接获取传输的数据,当客户端连接成功时似乎会发送来一条空的消息
                    Logcat.print("收到消息: " + response);
                    if (response.isEmpty()){//如果为空，则检查连接状态，如果断连则重启，否则直接跳过避免解析报错
                        boolean isAlive = isClientAlive();
                        if (!isAlive){
                            disconnect();
                            destroy();
                        }
                        Thread.sleep(75);
                        continue;
                    }
                    if (socketCallback == null){
                        continue;
                    }
                    socketCallback.onResponse(response);//接口执行接收方法
                } catch (IOException e) {
                    if (socketCallback == null){
                        continue;
                    }
                    socketCallback.onFailure(e);//接口处理失败方法
                    disconnect();//接收出现问题，关闭连接
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Logcat.print("接收线程执行完毕");
        }
    }

    /**
     * 信息处理线程，为保证操作有序进行，故使用synchronized关键字进行上锁
     */
    private class MessageHandler implements Runnable{

        @Override
        public void run() {
            synchronized (this){//上锁，保证短时间获取多个数据时程序能有序执行操作
                if (socketCallback == null){
                    return;
                }
                Logcat.print("正在处理消息");
                socketCallback.handleMessage();//接口执行处理消息方法
                Logcat.print("消息已处理");
            }
        }
    }

    public boolean isDestroy() {
        return isDestroy;
    }

    public void setSocketCallback(SocketCallback socketCallback) {
        this.socketCallback = socketCallback;
    }
}
