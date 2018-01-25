package com.test.li182.my_game;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static com.test.li182.my_game.Utils.intToIp;
import static com.test.li182.my_game.Utils.toast;

public class ServerActivity extends AppCompatActivity {
    private final int MSG_ADD = 100;
    private final int MSG_PREPARE = 101;
    private final int MSG_TRANSFORM = 102;
    private final int MSG_END = 103;
    private final int MSG_TEST = 104;
    private Button func_button;
    private Button getip_button;

    private boolean receving;
    private int buttonState = 1;  //1:创建房间  2.等待玩家进入 3.开始 4.传输中

    private final int PORT = 8899;
    Socket socket;
    ServerSocket serverSocket;

    List<Player> players;
    Set<String> clientAddress;

    public RecyclerView mRecyclerView;
    public RecyclerView.Adapter mAdapter;
    public RecyclerView.LayoutManager mLayoutManager;


    @SuppressLint("HandlerLeak")
    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            String info = (String) msg.obj;
            Iterator<Player> iterator;
            switch (msg.what){
                case MSG_ADD:
                    String[] playerMsg = info.substring(1).split(":");
                    players.add(new Player(playerMsg[0],Integer.parseInt(playerMsg[1]),playerMsg[2]));
                    mAdapter.notifyDataSetChanged();
                    break;
                case MSG_PREPARE:
                    iterator = players.iterator();
                    while (iterator.hasNext()){
                        Player p = iterator.next();
                        if (p.getName().equals(info.substring(1))){
                            p.setReady(true);
                            break;
                        }
                    }
                    mAdapter.notifyDataSetChanged();

                    boolean isReady = true;
                    iterator = players.iterator();
                    while (iterator.hasNext()){
                        Player p = iterator.next();
                        if (!p.isReady()){
                            isReady = false;
                            break;
                        }
                    }
                    if (isReady){
                        buttonState = 3;
                        func_button.setBackgroundResource(R.drawable.start);
                        func_button.setClickable(true);
                    }
                    break;
                case MSG_TRANSFORM:
                    String[] str = info.substring(1).split(":");
                    iterator = players.iterator();
                    while (iterator.hasNext()){
                        Player p = iterator.next();
                        if (p.getName().equals(str[0])){
                            p.setScore(Integer.parseInt(str[1]));
                            break;
                        }
                    }
                    mAdapter.notifyDataSetChanged();
                    break;
                case MSG_END:
                    iterator = players.iterator();
                    while (iterator.hasNext()){
                        Player p = iterator.next();
                        if (p.getName().equals(info.substring(1))){
                            p.setOk(true);
                        }
                    }
                    boolean isEnd = true;
                    iterator = players.iterator();
                    while (iterator.hasNext()){
                        Player p = iterator.next();
                        if (p.isOk()==false){
                            isEnd = false;
                        }
                    }

                    if (isEnd){
                        returnMessage();
                        receving = false;
                        closeSocket();
                        buttonState = 1;
                        func_button.setBackgroundResource(R.drawable.creat_room);
                        func_button.setClickable(true);
                        clientAddress.clear();

                    }
                    break;
                case MSG_TEST:
                    toast(ServerActivity.this,info);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);//隐藏标题栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);//隐藏状态栏
        setContentView(R.layout.activity_server);

        func_button = findViewById(R.id.button_server);
        getip_button = findViewById(R.id.button_getip);


        mLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        List<Player> list = new ArrayList<>();
        clientAddress = new HashSet<>();
        players = Collections.synchronizedList(list);

        initView();

        func_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (buttonState == 1){
                    initView();
                    buttonState = 2;
                    func_button.setBackgroundResource(R.drawable.waiting);
                    func_button.setClickable(false);
                    startReceving();
                }
                else if (buttonState == 3){
                    buttonState = 4;
                    func_button.setBackgroundResource(R.drawable.sending);
                    func_button.setClickable(false);
                    for(String addr : clientAddress){
                        sendmessage(addr,"start");
                    }
                }
            }
        });
        getip_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String ip = getIP();
                toast(ServerActivity.this,ip);
            }
        });
    }

    private void initView() {
        players.clear();
        mAdapter = new MyAdapter(players);
        mRecyclerView = findViewById(R.id.rc_server);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);
    }

    private void returnMessage() {
        Collections.sort(players);
        Iterator<Player> iterator = players.iterator();
        Player temp = iterator.next();
        sendmessage(temp.address,"Rwin");
        while (iterator.hasNext()){
            temp = iterator.next();
            sendmessage(temp.address,"Rlose");
        }
    }

    private void sendmessage(final String addr, final String info) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    Socket sendSocket = new Socket(addr, PORT);
                    PrintWriter out = new PrintWriter(sendSocket.getOutputStream(), true);
                    out.println(info);
                    out.close();
                    sendSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        new Thread(runnable).start();
    }

    private void startReceving() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    receving = true;
                    serverSocket = new ServerSocket(PORT);
                    while (receving){
                        socket = serverSocket.accept();
                        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        String data = in.readLine();
                        Message message = new Message();
                        in.close();
                        if (data != null) {

                            if (data.matches("A.*")) {
                                String string = socket.getInetAddress().getHostAddress();
                                clientAddress.add(string);
                                message.what = MSG_ADD;
                                message.obj = data+":"+string;
                                handler.sendMessage(message);
                            }
                            if (data.matches("P.*")) {
                                message.what = MSG_PREPARE;
                                message.obj = data;
                                handler.sendMessage(message);
                            }
                            if (data.matches("T.*")) {
                                message.what = MSG_TRANSFORM;
                                message.obj = data;
                                handler.sendMessage(message);
                            }
                            if (data.matches("E.*")) {
                                message.what = MSG_END;
                                message.obj = data;
                                handler.sendMessage(message);
                            }

                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }


            }
        };
        new Thread(runnable).start();
    }

    public void closeSocket(){
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Socket tempSocket=null;
                if(serverSocket!=null&&!serverSocket.isClosed())
                    try {
                        //开启一个无用的Socket，这样就能让ServerSocket从accept状态跳出
                        tempSocket = new Socket(getIP(),PORT);
                        serverSocket.close();
                    } catch (UnknownHostException e1) {
                        e1.printStackTrace();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                if(tempSocket!=null&&!tempSocket.isClosed()){
                    try {
                        tempSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if(socket!=null&&!socket.isClosed()){
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }


            }
        };
        new Thread(runnable).start();


    }
    @NonNull
    private String getIP() {
        //获取wifi服务
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        //判断wifi是否开启
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        return intToIp(ipAddress);
    }
}
