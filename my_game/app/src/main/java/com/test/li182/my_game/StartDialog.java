package com.test.li182.my_game;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

/**
 * Created by apple on 2018/1/21.
 */

public class StartDialog extends Dialog {
    public StartDialog(Context context) {
        super(context, R.style.Theme_AppCompat);
    }
    TextView tv_time;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);//隐藏标题栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);//隐藏状态栏
        setContentView(R.layout.dialog_start);
        // 设置是否可以关闭当前控件
        setCancelable(false);
        // 找到tv_time控件
        tv_time = (TextView) findViewById(R.id.tv_time);
        new DownTimer().start();

    }

    // 继承CountDownTimer类
    class DownTimer extends CountDownTimer {

        public DownTimer() {
            // 设置时间5秒
            super(7000, 1000);
        }
        // 重写CountDownTimer的两个方法
        @Override
        public void onTick(long millisUntilFinished) {
            tv_time.setText(millisUntilFinished / 1000 + "s");
        }

        @Override
        public void onFinish() {
            StartDialog.this.dismiss();

        }

    }
}
