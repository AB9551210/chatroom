
package com.example.test.activites;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import com.example.test.databinding.ActivitySignUpBinding;
import com.example.test.utilities.Constants;
import com.example.test.utilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;

public class SignUpActivity extends AppCompatActivity {

    private ActivitySignUpBinding binding;
    private PreferenceManager preferenceManager;
    private String encodeImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        setListeners();
    }

    /**
     * 登录按钮监听
     * 注册按钮监听
     */
    private void setListeners(){
        //登录按钮
        binding.textSigIn.setOnClickListener(v->onBackPressed());
        //注册按钮
        binding.buttonSignUp.setOnClickListener(v->{
            if(isValidSignUpDeatails()){
                signUp();
            }
        });
        binding.layoutImage.setOnClickListener(v->{
            // 获取访问图片URI的权限
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            //Intent将被准许执行read操作的权限
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });
    }

    /**
     * 土司
     * @param message 消息
     */
    private void showToast(String message){
        Toast.makeText(getApplicationContext(),message,Toast.LENGTH_LONG).show();
    }

    /**
     * 注册事件
     */
    private void signUp(){
        //启动加载动画
        loading(true);
        //输入框邮箱获取
        String email = binding.inputEmail.getText().toString();
        //FirebaseFirestore实例获取
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_EMAIL,email).get().addOnCompleteListener(task -> {
                    if(task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size()>0){
                        showToast("邮箱已被注册");
                        loading(false);
                    }
                    else{
                        //HashMap填充
                        HashMap<String,Object> user = new HashMap<>();
                        user.put(Constants.KEY_NAME,binding.inputName.getText().toString());
                        user.put(Constants.KEY_EMAIL,binding.inputEmail.getText().toString());
                        user.put(Constants.KEY_PASSWORD,binding.inputPassword.getText().toString());
                        user.put(Constants.KEY_IMAGE,encodeImage);
                        //向Users集合添加数据
                        database.collection(Constants.KEY_COLLECTION_USERS)
                                .add(user)
                                //注册成功监听
                                .addOnSuccessListener(documentReference -> {
                                    //停止加载动画
                                    loading(false);
                                    preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN,true);
                                    //本地保存集合中的id、用户名、头像
                                    preferenceManager.putString(Constants.KEY_USER_ID,documentReference.getId());
                                    preferenceManager.putString(Constants.KEY_NAME,binding.inputName.getText().toString());
                                    preferenceManager.putString(Constants.KEY_IMAGE,encodeImage);
                                    //开启MainActivity
                                    Intent intent = new Intent(getApplicationContext(),MainActivity.class);
                                    //开始新的activity同时候移除之前全部的activity
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                })
                                //注册失败监听
                                .addOnFailureListener(exception->{
                                    //停止加载动画
                                    loading(false);
                                    //土司
                                    showToast(exception.getMessage());
                                });
                    }
                });
    }

    /**
     * 头像编码
     * @param bitmap
     * @return 编码的字节流
     */
    private String encodeImage(Bitmap bitmap){
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        //对bitmap进行缩放，缩小时filter无影响
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap,previewWidth,previewHeight,false);
        //字节输出流
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        //图片压缩（JPEG格式，压缩率50%）
        previewBitmap.compress(Bitmap.CompressFormat.JPEG,50,byteArrayOutputStream);
        //获取字节数组
        byte[] bytes = byteArrayOutputStream.toByteArray();
        //编码
        return Base64.encodeToString(bytes,Base64.DEFAULT);
    }

    /**
     * ActivityResultLauncher: 启动器，调用ActivityResultLauncher的launch方法来启动页面跳转，作用相当于原来的startActivity()
     */
    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            //ActivityResultContract: 协议，它定义了如何传递数据和如何处理返回的数据
            new ActivityResultContracts.StartActivityForResult(),
            //通过使用谷歌常用的ActivityResultContract协议，然后返回结果
            result -> {
                if(result.getResultCode()==RESULT_OK){
                    if(result.getData()!=null){
                        Uri imageUri = result.getData().getData();
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            binding.imageProfile.setImageBitmap(bitmap);
                            binding.textAddImage.setVisibility(View.GONE);
                            encodeImage = encodeImage(bitmap);
                        }catch (FileNotFoundException e){
                            e.printStackTrace();
                        }
                    }
                }
            }
    );

    /**
     * 注册表单内容检查
     * @return 判断结果
     */
    private Boolean isValidSignUpDeatails(){
        if(encodeImage == null){
            showToast("请选择头像");
            return false;
        }
        else if(binding.inputName.getText().toString().trim().isEmpty()){
            showToast("请填写昵称");
            return false;
        }
        else if(binding.inputEmail.getText().toString().trim().isEmpty()){
            showToast("请填写邮箱");
            return false;
        }
        else if(!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.getText().toString()).matches()){
            showToast("请填写有效邮箱");
            return false;
        }
        else if(binding.inputPassword.getText().toString().trim().isEmpty()){
            showToast("请填写密码");
            return false;
        }
        else if(binding.inputConfirmPassword.getText().toString().trim().isEmpty()){
            showToast("请填写确认密码");
            return false;
        }
        else if(!binding.inputPassword.getText().toString().equals(binding.inputConfirmPassword.getText().toString())){
            showToast("密码与确认密码不一致");
            return false;
        }
        else{
            return true;
        }
    }

    /**
     * 加载动画
     * @param isLoading 是否加载
     */
    private void loading(Boolean isLoading){
        if(isLoading){
            binding.buttonSignUp.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        }
        else{
            binding.progressBar.setVisibility(View.INVISIBLE);
            binding.buttonSignUp.setVisibility(View.VISIBLE);
        }
    }
}