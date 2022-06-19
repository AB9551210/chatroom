package com.example.test.activites;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.example.test.databinding.ActivityBeginBinding;

public class BeginActivity extends AppCompatActivity {

    private ActivityBeginBinding binding;

    Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            int count = msg.arg1;
            if (count != 0) {
                binding.textView.setText(count + "秒后跳过广告");
            } else {
                binding.textView.setText("跳过广告");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBeginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.textView.setOnClickListener(v-> {
                String text = binding.textView.getText().toString();
                if (text.equals("跳过广告")){
                    //进入登录页面
                    Intent intent = new Intent(getApplicationContext(),SignInActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
        });

        //主线程不能做耗时曹组,否则界面会卡住
        new Thread(new Runnable() {
            @Override
            public void run() {
                //每隔1秒减1,当减到0时跳转到下一个页面(登录页面)
                int count = 3;
                while (count > 0) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    count--;

                    Message msg = Message.obtain();
                    msg.arg1 = count;
                    handler.sendMessage(msg);
                }
            }
        }).start();
    }
}