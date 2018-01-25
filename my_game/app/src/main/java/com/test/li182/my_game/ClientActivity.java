package com.test.li182.my_game;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
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

import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

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
    private final int MSG_UPDATE_SCORE = 63;
    private int touID;
    private String name;

    private Button func_button;
    private Button conn_button;
    private TextView user_score;

    private String IP;
    private final int PORT = 8899;
    Socket socket;
    ServerSocket serverSocket;

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
    private int buttonState = 1;  //1:加入房间  2.准备 3.传输中
    private boolean prepare = false;

    @SuppressLint("HandlerLeak")
    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MSG_START_TRANS:
                    buttonState = 3;
                    func_button.setBackgroundResource(R.drawable.sending);
                    func_button.setClickable(false);
                    break;

                case MSG_UPDATE_SCORE:
                    long score = (long) msg.obj;
                    user_score.setText(""+score);
                    break;

                case MSG_END_TRANS:
                    shake();
                    break;

                case MSG_RESULT:
                    String info = (String) msg.obj;
                    buttonState = 1;
                    func_button.setBackgroundResource(R.drawable.join_game);
                    func_button.setClickable(true);
                    if (info.substring(1).equals("win")){
                        alertText(ClientActivity.this,"恭喜","大吉大利，今晚吃鸡！");
                    }
                    else {
                        alertText(ClientActivity.this,"很遗憾","下次努力！");
                    }

                    prepare = false;
                    receving =false;
                    closeSocket();

                    break;


            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);//隐藏标题栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);//隐藏状态栏
        setContentView(R.layout.activity_client);

        func_button = findViewById(R.id.button_client);
        conn_button = findViewById(R.id.button_conn);

        SharedPreferences preferences = getSharedPreferences("Userinfo",MODE_PRIVATE);
        name = preferences.getString("Name","NULL");
        touID = preferences.getInt("ID",0);
        initTou();

        ClientActivity.SensorListener sensorListener = new ClientActivity.SensorListener();
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER );
        sensorManager.registerListener(sensorListener,accelerometer,SensorManager.SENSOR_DELAY_UI);

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        func_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (buttonState == 1){
                    buttonState = 2;
                    func_button.setBackgroundResource(R.drawable.ready);
                    sendmessage(IP, "A"+name+":"+touID);
                }
                else if (buttonState == 2){
                    if(!prepare){
                        sendmessage(IP, "P"+name);
                        receivingMsg();
                        prepare = true;
                    }

                }
            }
        });
        conn_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
            }
        });
    }

    private void initTou() {
        ImageView touxiang = findViewById(R.id.touxiang);
        TextView user_name = findViewById(R.id.user_name);
        user_score = findViewById(R.id.user_score);
        switch (touID)
        {
            case 0:touxiang.setBackgroundResource(R.drawable.tou0);break;
            case 1:touxiang.setBackgroundResource(R.drawable.tou1);break;
            case 2:touxiang.setBackgroundResource(R.drawable.tou2);break;
            case 3:touxiang.setBackgroundResource(R.drawable.tou3);break;
            case 4:touxiang.setBackgroundResource(R.drawable.tou4);break;
            case 5:touxiang.setBackgroundResource(R.drawable.tou5);break;
            case 6:touxiang.setBackgroundResource(R.drawable.tou6);break;
            case 7:touxiang.setBackgroundResource(R.drawable.tou7);break;
            case 8:touxiang.setBackgroundResource(R.drawable.tou8);break;
            case 9:touxiang.setBackgroundResource(R.drawable.tou9);break;
            case 10:touxiang.setBackgroundResource(R.drawable.tou10);break;
            case 11:touxiang.setBackgroundResource(R.drawable.tou11);break;
            case 12:touxiang.setBackgroundResource(R.drawable.tou12);break;
        }
        user_name.setText(name);
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
                                    long temp = sum;
                                    Message msg2 = new Message();
                                    msg2.what = MSG_UPDATE_SCORE;
                                    msg2.obj = temp;
                                    handler.sendMessage(msg2);
                                    sendmessage(IP, "T"+name+":" + temp);
                                    Thread.sleep(1000);
                                }
                                sendmessage(IP, "E"+name);
                                Message msg3 = new Message();
                                msg3.what = MSG_END_TRANS;
                                handler.sendMessage(msg3);
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
