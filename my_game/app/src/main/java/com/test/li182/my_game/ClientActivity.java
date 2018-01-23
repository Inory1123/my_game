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
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import static com.test.li182.my_game.Utils.alertText;
import static com.test.li182.my_game.Utils.intToIp;
import static com.test.li182.my_game.Utils.toast;

public class ClientActivity extends AppCompatActivity {
    private final int MSG_START_TRANS = 60;
    private final int MSG_END_TRANS = 61;
    private final int MSG_RESULT = 62;

    private Button button;

    private String IP;
    private final int PORT = 8899;
    Socket socket;
    ServerSocket serverSocket;

    public RecyclerView mRecyclerView;
    public RecyclerView.Adapter mAdapter;
    public RecyclerView.LayoutManager mLayoutManager;
    private Vibrator vibrator;

    long sum;
    private String data;
    private static final int SPEED_SHRESHOLD = 6000;  // 速度阈值，当摇晃速度达到这值后产生作用
    private static final int UPTATE_INTERVAL_TIME = 50;  // 两次检测的时间间隔
    private float lastX;
    private float lastY;
    private float lastZ;
    private long lastUpdateTime;  // 上次检测时间

    private  boolean receving;

    @SuppressLint("HandlerLeak")
    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MSG_START_TRANS:
                    button.setText("传输中");
                    button.setClickable(false);
                    break;

                case MSG_END_TRANS:
                    shake();
                    break;

                case MSG_RESULT:
                    String info = (String) msg.obj;
                    button.setText("加入房间");
                    button.setClickable(true);
                    if (info.substring(1).equals("win")){
                        alertText(ClientActivity.this,"恭喜","大吉大利，今晚吃鸡");
                    }
                    else if (info.substring(1).equals("lose")){
                        alertText(ClientActivity.this,"恭喜","大吉大利，今晚吃鸡");
                    }

                    receving =false;
                    closeSocket();

                    break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        button = findViewById(R.id.button_client);

        ClientActivity.SensorListener sensorListener = new ClientActivity.SensorListener();
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER );
        sensorManager.registerListener(sensorListener,accelerometer,SensorManager.SENSOR_DELAY_UI);

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (button.getText().toString().equals("加入房间")){
                    button.setText("准备");
                    sendmessage(IP, "Atom");
                }
                else if (button.getText().toString().equals("准备")){
                    sendmessage(IP, "Ptom");

                    receivingMsg();
                    //startReceving();
                }
            }
        });
    }

    public void shake(){
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                vibrator.vibrate(300);
                try {
                    Thread.sleep(400);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                vibrator.vibrate(300);
            }
        };
        new Thread(runnable).start();

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
                    serverSocket = new ServerSocket(PORT);
                    socket = serverSocket.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String data = in.readLine();
                    in.close();
                    if (data != null) {
                        if (data.equals("start")) {
                            Message msg1 = new Message();
                            msg1.what = MSG_START_TRANS;
                            handler.sendMessage(msg1);
                            sum = 0;
                            for (int i = 0; i < 10; i++) {
                                sendmessage(IP, "Ttom:" + sum);
                                Thread.sleep(1000);
                            }
                            sendmessage(IP, "Etom");
                            Message msg2 = new Message();
                            msg2.what = MSG_END_TRANS;
                            handler.sendMessage(msg2);
                            socket.close();
                            serverSocket.close();
                        }
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

    private void receivingMsg(){
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                receving = true;
                try {
                    serverSocket = new ServerSocket(PORT);
                    while (true){
                    socket = serverSocket.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String data = in.readLine();
                    in.close();

                   if (data!=null){
                       if (data.equals("start")){
                           Message msg1 = new Message();
                           msg1.what = MSG_START_TRANS;
                           handler.sendMessage(msg1);
                           sum = 0;
                           for (int i = 0; i < 10; i++) {
                               sendmessage(IP, "Ttom:" + sum);
                               Thread.sleep(1000);
                           }
                           sendmessage(IP, "Etom");
                           Message msg2 = new Message();
                           msg2.what = MSG_END_TRANS;
                           handler.sendMessage(msg2);
                       }
                       if (data.matches("R.*")){
                           Message resultMsg = new Message();
                           resultMsg.obj = data;
                           resultMsg.what = MSG_RESULT;
                           handler.sendMessage(resultMsg);
                       }
                   }

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


    class SensorListener implements SensorEventListener {
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
                final EditText editText = new EditText(ClientActivity.this);
                editText.setText(getIP());
                new AlertDialog.Builder(ClientActivity.this).setTitle("输入对方ip：")
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
                toast(ClientActivity.this,ip);
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

}
