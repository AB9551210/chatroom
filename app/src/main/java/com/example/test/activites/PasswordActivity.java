package com.example.test.activites;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.test.databinding.ActivityPasswordBinding;
import com.example.test.utilities.Constants;
import com.example.test.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class PasswordActivity extends BaseActivity {

    private ActivityPasswordBinding binding;
    private PreferenceManager preferenceManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferenceManager = new PreferenceManager(getApplicationContext());
        binding = ActivityPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListeners();
    }

    private void loading(Boolean isLoading){
        if(isLoading){
            binding.beginPassword.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        }
        else{
            binding.progressBar.setVisibility(View.INVISIBLE);
            binding.beginPassword.setVisibility(View.VISIBLE);
        }
    }


    private void changePassword(){
        //启动加载动画
        loading(true);
        //旧密码获取
        String password = binding.beginPassword.getText().toString();
        //新密码获取
        String newPassword = binding.afterPassword.getText().toString();
        //FirebaseFirestore实例获取
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        //通过数据id获取用户数据
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_USERS).document(preferenceManager.getString(Constants.KEY_USER_ID));
        //修改密码
        documentReference.get().addOnCompleteListener(task -> {
                    if(task.isSuccessful() && task.getResult() != null){
                        String userPassword = task.getResult().get(Constants.KEY_PASSWORD).toString();
                        if(userPassword.equals(password)){
                            showToast("修改密码成功");
                            //修改数据密码
                            documentReference.update(
                                    Constants.KEY_PASSWORD,newPassword
                            );
                            //停止加载动画
                            loading(false);
                            onBackPressed();
                        }
                        else{
                            showToast("原密码不一致修改失败");
                            //停止加载动画
                            loading(false);
                        }
                    }
                    else{
                        showToast("获取用户信息失败");
                        //停止加载动画
                        loading(false);
                    }
                });
    }

    private Boolean isValidPasswordDetails(){
        if(binding.beginPassword.getText().toString().trim().isEmpty()){
            showToast("请填写旧密码");
            return false;
        }
        else if(binding.afterPassword.getText().toString().trim().isEmpty()){
            showToast("请填写新密码");
            return false;
        }
        else if(!binding.afterPassword.getText().toString().equals(binding.afterPasswordRe.getText().toString())){
            showToast("确认输入的新密码不一致");
            return false;
        }
        else{
            return true;
        }
    }

    /**
     * 土司
     * @param message 消息
     */
    private void showToast(String message){
        Toast.makeText(getApplicationContext(),message,Toast.LENGTH_LONG).show();
    }

    private void setListeners() {
        binding.back.setOnClickListener(v->onBackPressed());
        binding.buttonChange.setOnClickListener(v->{
            if(isValidPasswordDetails()){
                changePassword();
            }
        });
    }
}