package com.test.li182.my_game;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

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

public class UnionActivity extends AppCompatActivity {
    private final int MSG_CONNECT = 100;
    private final int MSG_TRANSFORM = 101;
    private final int MSG_START = 103;
    private final int MSG_REFRESH = 104;

    Button button1;
    Button button2;
    boolean isHouseOwner;

    public RecyclerView mRecyclerView;
    public RecyclerView.Adapter mAdapter;
    public RecyclerView.LayoutManager mLayoutManager;

    long sum;
    private String data;
    private static final int SPEED_SHRESHOLD = 6000;  // 速度阈值，当摇晃速度达到这值后产生作用
    private static final int UPTATE_INTERVAL_TIME = 50;  // 两次检测的时间间隔
    private float lastX;
    private float lastY;
    private float lastZ;
    private long lastUpdateTime;  // 上次检测时间

    boolean connecting;
    boolean transforming;

    private String IP;
    private final int PORT = 6666;
    Socket socket;
    ServerSocket serverSocket;

    List<Player> players;
    Set<String> clientAddress;

    private Vibrator vibrator;



    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
//            if(msg.what== MSG_CONNECT){
//                String message = (String) msg.obj;
//                String usrname = message.substring(1);
//                if (message.charAt(0)=='C'){
//                    players.add(new Player(usrname));
//                }
//                if (message.charAt(0)=='P'){
//                    Iterator<Player> iterator = players.iterator();
//                    while (iterator.hasNext()){
//                        Player p = iterator.next();
//                        if (p.getName().equals(usrname)){
//                            p.setReady(true);
//                        }
//                    }
//                }
//                mAdapter.notifyDataSetChanged();
//
//            }
            if (msg.what== MSG_TRANSFORM){
                String message = (String) msg.obj;
                String string = message.substring(1);
                if (message.charAt(0)=='T'){
                    String[] str = string.split(":");
                    Iterator<Player> iterator = players.iterator();
                    while (iterator.hasNext()){
                        Player p = iterator.next();
                        if (p.getName().equals(str[0])){
                            p.setScore(Integer.parseInt(str[1]));
                        }
                    }
                }
                if (message.charAt(0)=='E'){
                    Iterator<Player> iterator = players.iterator();
                    while (iterator.hasNext()){
                        Player p = iterator.next();
                        if (p.getName().equals(string)){
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
                        transforming = false;
                        closeSocket(transforming);
                        toast(UnionActivity.this,"游戏结束");
                    }
                }
                mAdapter.notifyDataSetChanged();
            }
            if (msg.what==MSG_START){
                serverTransform();
            }
            if (msg.what==MSG_REFRESH){
                freshData();
            }
            if (msg.what==105){
                String message = (String) msg.obj;
                toast(UnionActivity.this,message);
            }


        }
    };



    private void freshData() {
        button2.setText("加入房间");
        isHouseOwner = false;
        connecting = false;
        transforming = false;
        clientAddress.clear();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_union);
        //initData
        button1 = findViewById(R.id.button_ustart);
        button2 = findViewById(R.id.button_uadd);

        UnionActivity.SensorListener sensorListener = new UnionActivity.SensorListener();
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER );
        sensorManager.registerListener(sensorListener,accelerometer,SensorManager.SENSOR_DELAY_UI);

        mLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        List<Player> list = new ArrayList<>();
        clientAddress = new HashSet<>();
        players = Collections.synchronizedList(list);
        mAdapter = new MyAdapter(players);
        mRecyclerView = findViewById(R.id.recycleview);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        //bindListener
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                players.clear();
                mAdapter = new MyAdapter(players);
                mRecyclerView.setAdapter(mAdapter);
                if (!connecting){
                    receiveConnect();
                    connecting = true;
                    isHouseOwner = true;
                    button2.setText("开始");
                }
            }
        });

        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (button2.getText().toString().equals("加入房间")){
                    if (!isHouseOwner){
                        connect("Cinory");
                        button2.setText("准备");
                    }
                }
                else if (button2.getText().toString().equals("准备")){
                    if (!isHouseOwner){
                        connect("Pinory");
                        clintTransform();
                    }
                }
                else if (button2.getText().toString().equals("开始")){
                    connecting = false;
                    transforming = true;
                    closeSocket(transforming);
                }

            }
        });
    }

    private void clintTransform() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    serverSocket = new ServerSocket(PORT);
                    socket = serverSocket.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String data = in.readLine();


                    if (data.equals("start")){
                        sum=0;
                        for (int i = 0;i<10;i++){

                            data = "Tinory:"+sum;
                            socket = new Socket(IP, PORT);
                            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                            out.println(data);
                            out.close();
                            Thread.sleep(1000);
                        }

                        socket = new Socket(IP, PORT);
                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        out.println("Einory");
                        out.close();

                        transforming = false;
                        closeSocket(transforming);
                        Thread.sleep(1000);

                        vibrator.vibrate(300);
                        Thread.sleep(400);
                        vibrator.vibrate(300);
                    }


                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        new Thread(runnable).start();
    }

    private void serverTransform() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    for(String address : clientAddress){
                        Message msg = new Message();


                        socket = new Socket(address, PORT);
                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        out.println("start");
                        out.close();
                    }


                    serverSocket = new ServerSocket(PORT);
                    while (transforming){
                            socket = serverSocket.accept();
                            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                            String data = in.readLine();
                            in.close();
                            if (data!=null){
                                Message message = new Message();
                                message.what = MSG_TRANSFORM;
                                message.obj = data;
                                handler.sendMessage(message);
                            }

                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        new Thread(runnable).start();
    }


    private void receiveConnect() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                while (connecting){
                    try {
                        serverSocket = new ServerSocket(PORT);
                        socket = serverSocket.accept();
                        String string = socket.getInetAddress().getHostAddress();
                        if (!string.equals(getIP())){
                            clientAddress.add(string);

                        }

                        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        String data = in.readLine();

                        if (data!=null){
                            Message msg = new Message();
                            msg.what= MSG_CONNECT;
                            msg.obj = data;
                            handler.sendMessage(msg);
                        }


                        in.close();
                        socket.close();
                        serverSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        new Thread(runnable).start();
    }

    private void connect(final String message) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket(IP, PORT);
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.println(message);
                    out.close();
                    socket.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        };
       new Thread(runnable).start();
    }


    class SensorListener implements SensorEventListener{
        @Override
        public void onSensorChanged(SensorEvent event) {
            // 现在检测时间
            long currentUpdateTime = System.currentTimeMillis();
            // 两次检测的时间间隔
            long timeInterval = currentUpdateTime - lastUpdateTime;
            // 判断是否达到了检测时间间隔
            if (timeInterval < UPTATE_INTERVAL_TIME)
                return;
            // 现在的时间变成last时间
            lastUpdateTime = currentUpdateTime;

            // 获得x,y,z坐标
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            // 获得x,y,z的变化值
            float deltaX = x - lastX;
            float deltaY = y - lastY;
            float deltaZ = z - lastZ;

            double speed = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ
                    * deltaZ)/ timeInterval * 10000;

            if ((x*lastX)<0&&speed>SPEED_SHRESHOLD){
                //vibrator.vibrate(100);
                sum = sum + 1;
            }

            // 将现在的坐标变成last坐标
            lastX = x;
            lastY = y;
            lastZ = z;
            //sqrt 返回最近的双近似的平方根


//            if (speed >= SPEED_SHRESHOLD) {
//              vibrator.vibrate(1000);
//            }

        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu1:
                final EditText editText = new EditText(UnionActivity.this);
                editText.setText(getIP());
                new AlertDialog.Builder(UnionActivity.this).setTitle("输入对方ip：")
                        .setView(editText)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                IP = editText.getText().toString();
                            }
                        })
                        .create().show();
                return true;

            case R.id.menu3:
                String ip = getIP();
                toast(UnionActivity.this,ip);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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

    public void closeSocket(final boolean transforming){
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
                if (transforming){
                    Message message = new Message();
                    message.what = MSG_START;
                    handler.sendMessage(message);
                }else {
                    Message message = new Message();
                    message.what = MSG_REFRESH;
                    handler.sendMessage(message);
                }

            }
        };
        new Thread(runnable).start();


    }
}
