package com.test.li182.my_game;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
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
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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

    private static int MSG_TRANS_DATA = 31;
    private static int MSG_CONNECT = 32;

    String aData;

    String bData;
    long sumA,sumB;
    TextView tvA;
    TextView tvB;

    private final int PORT = 6666;
    private String IP;
    Socket socket;
    ServerSocket serverSocket;

    private int atouID;
    private String aname;
    private int btouID;
    private String bname;

    TextView tv_aname;
    TextView tv_bname;
    ImageView atou;
    ImageView btou;

    long sum;
    private static final int SPEED_SHRESHOLD = 6000;  // 速度阈值，当摇晃速度达到这值后产生作用
    private static final int UPTATE_INTERVAL_TIME = 50;  // 两次检测的时间间隔
    private float lastX;
    private float lastY;
    private float lastZ;
    private long lastUpdateTime;  // 上次检测时间

    private Vibrator vibrator;


    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == MSG_TRANS_DATA){
                LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.MATCH_PARENT,Long.parseLong(aData));
                tvA.setLayoutParams(p1);
                tvA.setText(aData);
                LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.MATCH_PARENT,Long.parseLong(bData));
                tvB.setLayoutParams(p2);
                tvB.setText(bData);
            }
            if (msg.what == MSG_CONNECT){
                String info = (String) msg.obj;
                String[] result = info.split(":");
                initTou(tv_bname,result[0],btou,Integer.parseInt(result[1]));
            }
        }
    };
    private ServiceConnection conn =new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);//隐藏标题栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);//隐藏状态栏
        setContentView(R.layout.activity_pk);
        Intent intent = new Intent(PKActivity.this,BgmService.class);
        bindService(intent,conn,Context.BIND_AUTO_CREATE);

        //初始化页面
        Button btStart = findViewById(R.id.button_start);
        Button btAdd = findViewById(R.id.button_add);
        Button bt_set_ip = findViewById(R.id.button_set_ip);
        Button bt_get_ip = (Button) findViewById(R.id.button_get_ip);
         tv_aname = findViewById(R.id.tv_a_name);
         tv_bname = findViewById(R.id.tv_b_name);
         atou = findViewById(R.id.a_tou);
         btou = findViewById(R.id.b_tou);
        tvA = findViewById(R.id.tv_a);
        tvB = findViewById(R.id.tv_b);

        SharedPreferences preferences = getSharedPreferences("Userinfo",MODE_PRIVATE);
        aname = preferences.getString("Name","NULL");
        atouID = preferences.getInt("ID",0);
        initTou(tv_aname,aname,atou,atouID);


        SensorListener sensorListener = new SensorListener();
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER );
        sensorManager.registerListener(sensorListener,accelerometer,SensorManager.SENSOR_DELAY_UI);

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        btStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sumA = 0L;
                sumB = 0L;
                receive();
            }
        });

        btAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sumA = 0L;
                sumB = 0L;
                send();
            }
        });
        bt_set_ip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
            }
        });
        bt_get_ip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String ip = getIP();
                toast(PKActivity.this,ip);
            }
        });


    }
    private void initTou(TextView tv_name,String name,ImageView tou,int touID) {
        tv_name.setText(name);
        switch (touID)
        {
            case 0:tou.setBackgroundResource(R.drawable.tou0);break;
            case 1:tou.setBackgroundResource(R.drawable.tou1);break;
            case 2:tou.setBackgroundResource(R.drawable.tou2);break;
            case 3:tou.setBackgroundResource(R.drawable.tou3);break;
            case 4:tou.setBackgroundResource(R.drawable.tou4);break;
            case 5:tou.setBackgroundResource(R.drawable.tou5);break;
            case 6:tou.setBackgroundResource(R.drawable.tou6);break;
            case 7:tou.setBackgroundResource(R.drawable.tou7);break;
            case 8:tou.setBackgroundResource(R.drawable.tou8);break;
            case 9:tou.setBackgroundResource(R.drawable.tou9);break;
            case 10:tou.setBackgroundResource(R.drawable.tou10);break;
            case 11:tou.setBackgroundResource(R.drawable.tou11);break;
            case 12:tou.setBackgroundResource(R.drawable.tou12);break;
        }
    }

    private void send() {
        sum = 0;
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    serverSocket = new ServerSocket(PORT);

                    socket = new Socket(IP, PORT);
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.println(aname+":"+atouID);
                    out.close();


                    socket = serverSocket.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String tempData = in.readLine();
                    in.close();

                    Thread.sleep(500);

                    Message message = new Message();
                    message.what = MSG_CONNECT;
                    message.obj = tempData;
                    handler.sendMessage(message);

                    for (int i = 0;i<10;i++){
                        aData = ""+sum;
                        socket = new Socket(IP, PORT);
                        out = new PrintWriter(socket.getOutputStream(), true);
                        out.println(aData);
                        out.close();


                        socket = serverSocket.accept();
                        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        bData = in.readLine();
                        in.close();
                        Thread.sleep(500);


                        Message msg = new Message();
                        msg.what=MSG_TRANS_DATA;
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
                    socket = serverSocket.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String tempData = in.readLine();
                    in.close();
                    Thread.sleep(500);

                    socket = new Socket(IP, PORT);
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.println(aname+":"+atouID);
                    out.close();

                    Message message = new Message();
                    message.what = MSG_CONNECT;
                    message.obj = tempData;
                    handler.sendMessage(message);

                    for (int i = 0;i<10;i++){
                        socket = serverSocket.accept();
                        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        bData = in.readLine();
                        in.close();
                        Thread.sleep(500);

                        aData = ""+sum;
                        socket = new Socket(IP, PORT);
                        out = new PrintWriter(socket.getOutputStream(), true);
                        out.println(aData);
                        out.close();
                        Message msg = new Message();
                        msg.what=MSG_TRANS_DATA;
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

    @Override
    protected void onStop() {
        unbindService(conn);
        super.onStop();
    }
}
