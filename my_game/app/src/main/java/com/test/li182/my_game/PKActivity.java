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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

import static com.test.li182.my_game.Utils.*;

public class PKActivity extends AppCompatActivity {

    private String aData;
    private String bData;
    private TextView tvA;
    private TextView tvB;
    private TextView etTest;

    private String IP;
    private final int PORT = 6666;
    private Socket socket;
    private ServerSocket serverSocket;

    private Vibrator vibrator;

    long sum;
    private static final int SPEED_SHRESHOLD = 6000;  // 速度阈值，当摇晃速度达到这值后产生作用
    private static final int UPTATE_INTERVAL_TIME = 50;  // 两次检测的时间间隔
    private float lastX;
    private float lastY;
    private float lastZ;
    private long lastUpdateTime;  // 上次检测时间

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(msg.what ==1){
                LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.MATCH_PARENT,Long.parseLong(aData));
                tvA.setLayoutParams(p1);
                tvA.setText(aData);
                LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.MATCH_PARENT,Long.parseLong(bData));
                tvB.setLayoutParams(p2);
                tvB.setText(bData);
                //etTest.setText(aData+" : "+bData);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pk);
        etTest = findViewById(R.id.et_test);
        Button btStart =  findViewById(R.id.button_start);
        Button btAdd = findViewById(R.id.button_add);
        tvA = findViewById(R.id.tv_a);
        tvB = findViewById(R.id.tv_b);


        SensorListener sensorListener = new SensorListener();
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER );
        sensorManager.registerListener(sensorListener,accelerometer,SensorManager.SENSOR_DELAY_UI);

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);


        btStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                receive();
            }
        });

        btAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                send();
            }
        });


    }

    private void send() {
        sum = 0;
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    serverSocket = new ServerSocket(PORT);
                    for (int i = 0;i<10;i++){
                        aData = ""+sum;
                        socket = new Socket(IP, PORT);
                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        out.println(aData);
                        out.close();


                        socket = serverSocket.accept();
                        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        bData = in.readLine();
                        in.close();
                        Thread.sleep(500);


                        Message msg = new Message();
                        msg.what=1;
                        handler.sendMessage(msg);
                    }
                    socket.close();
                    serverSocket.close();
                    vibrator.vibrate(300);
                    Thread.sleep(400);
                    vibrator.vibrate(300);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        new Thread(runnable).start();

    }

    private void receive() {
        sum = 0;
        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    serverSocket = new ServerSocket(PORT);
                    for (int i = 0;i<10;i++){
                        socket = serverSocket.accept();
                        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        bData = in.readLine();
                        in.close();
                        Thread.sleep(500);

                        aData = ""+sum;
                        socket = new Socket(IP, PORT);
                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        out.println(aData);
                        out.close();
                        Message msg = new Message();
                        msg.what=1;
                        handler.sendMessage(msg);
                    }
                    socket.close();
                    serverSocket.close();
                    vibrator.vibrate(300);
                    Thread.sleep(400);
                    vibrator.vibrate(300);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        new Thread(task).start();
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
                final EditText editText = new EditText(PKActivity.this);
                editText.setText(getIP());
                new AlertDialog.Builder(PKActivity.this).setTitle("输入对方ip：")
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
                etTest.setText(ip);
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
