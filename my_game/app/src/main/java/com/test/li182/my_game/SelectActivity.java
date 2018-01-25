package com.test.li182.my_game;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

public class SelectActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);//隐藏标题栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);//隐藏状态栏
        setContentView(R.layout.activity_select);
        Intent intent = new Intent(SelectActivity.this,BgmService.class);
        startService(intent);
        Button buttonPK = findViewById(R.id.button_pk);
        Button buttonUnion = findViewById(R.id.button_union);

        buttonPK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SelectActivity.this, PKActivity.class);
                startActivity(intent);
            }
        });

       buttonUnion.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               new AlertDialog.Builder(SelectActivity.this).setTitle("请选择模式")
                       .setPositiveButton("服务器", new DialogInterface.OnClickListener() {
                           @Override
                           public void onClick(DialogInterface dialogInterface, int i) {
                               Intent intent = new Intent(SelectActivity.this, ServerActivity.class);
                               startActivity(intent);
                           }
                       })
                       .setNegativeButton("玩家", new DialogInterface.OnClickListener() {
                           @Override
                           public void onClick(DialogInterface dialogInterface, int i) {
                               Intent intent = new Intent(SelectActivity.this, ClientActivity.class);
                               startActivity(intent);
                           }
                       })
                       .create().show();
           }
       });
    }
    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Intent intent = new Intent(SelectActivity.this,BgmService.class);
        stopService(intent);
        super.onDestroy();
    }
}
