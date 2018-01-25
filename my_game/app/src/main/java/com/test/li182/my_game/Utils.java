package com.test.li182.my_game;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.Queue;

/**
 * Created by li182 on 2018/1/15.
 */

public class Utils {

    public static void addToQue(Queue<Integer> que, int data, int capacity) {
        if(que.size()<capacity){
            que.add(data);
        }else {
            que.poll();
            que.add(data);
        }
    }

    public static Integer queMean(Queue<Integer> que) {
        float mean = 0f;
        for (Iterator it = que.iterator();it.hasNext();){
            mean = mean+(Integer)it.next();
        }
        return (int)mean/que.size();
    }

    public static void toast(Context context,String str){
        Toast.makeText(context,str,Toast.LENGTH_SHORT).show();
    }

    public static String intToIp(int i) {

        return (i & 0xFF ) + "." +
                ((i >> 8 ) & 0xFF) + "." +
                ((i >> 16 ) & 0xFF) + "." +
                ( i >> 24 & 0xFF) ;
    }

    public static void alertText(Context context,String title,String message){
        new AlertDialog.Builder(context).setTitle(title)
                .setMessage(message)
                .setPositiveButton("确定",null)
                .create().show();
    }



}
