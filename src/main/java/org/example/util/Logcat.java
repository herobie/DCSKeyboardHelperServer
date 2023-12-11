package org.example.util;

public class Logcat {
    //打印日志工具类
    public static void print(String message){
        System.out.println(DateUtil.getCurrentTime() + "  " + message);
    }
}
