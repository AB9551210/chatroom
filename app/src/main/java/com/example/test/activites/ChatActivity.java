package com.example.test.activites;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.test.adapters.ChatAdapter;
import com.example.test.databinding.ActivityChatBinding;
import com.example.test.models.ChatMessage;
import com.example.test.models.User;
import com.example.test.network.ApiClient;
import com.example.test.network.ApiService;
import com.example.test.utilities.Constants;
import com.example.test.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends BaseActivity {

    private ActivityChatBinding binding;
    private User receiverUser;
    private List<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;
    //双方在conversion集合的id
    private String conversionId = null;
    private Boolean isReceiverAvailable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListeners();
        loadReceiverDetails();
        init();
        listenMessages();
    }

    @Override
    protected void onResume() {
        super.onResume();
        listenAvailabilityOfReceiver();
    }

    /**
     * 数据初始化
     */
    private void init(){
        //创建本地数据管理对象
        preferenceManager = new PreferenceManager(getApplicationContext());
        //创建消息列表
        chatMessages = new ArrayList<>();
        //创建聊天Adapter
        chatAdapter = new ChatAdapter(
                chatMessages,
                getBitmapFromEncodeString(receiverUser.image),
                preferenceManager.getString(Constants.KEY_USER_ID)
        );
        //消息视图设置Adapter
        binding.chatRecyclerView.setAdapter(chatAdapter);
        //获取FirebaseFirestore对象
        database = FirebaseFirestore.getInstance();
    }

    /**
     * 消息发送
     */
    private void sendMessage(){
        HashMap<String,Object> message = new HashMap<>();
        //发送者ID
        message.put(Constants.KEY_SENDER_ID,preferenceManager.getString(Constants.KEY_USER_ID));
        //接收者ID
        message.put(Constants.KEY_RECEIVER_ID,receiverUser.id);
        //消息文本
        message.put(Constants.KEY_MESSAGE,binding.inputMessage.getText().toString());
        //时间
        message.put(Constants.KEY_TIMESTAMP,new Date());
        //向chat集合添加数据
        database.collection(Constants.KEY_COLLECTION_CHAT).add(message);

        if(conversionId!=null){
            //更新conversions集合
            updateConversion(binding.inputMessage.getText().toString());
        }
        else{
            HashMap<String,Object> conversion = new HashMap<>();
            conversion.put(Constants.KEY_SENDER_ID,preferenceManager.getString(Constants.KEY_USER_ID));
            conversion.put(Constants.KEY_SENDER_NAME,preferenceManager.getString(Constants.KEY_NAME));
            conversion.put(Constants.KEY_SENDER_IMAGE,preferenceManager.getString(Constants.KEY_IMAGE));
            conversion.put(Constants.KEY_RECEIVER_ID,receiverUser.id);
            conversion.put(Constants.KEY_RECEIVER_NAME,receiverUser.name);
            conversion.put(Constants.KEY_RECEIVER_IMAGE,receiverUser.image);
            conversion.put(Constants.KEY_LAST_MESSAGE,binding.inputMessage.getText().toString());
            conversion.put(Constants.KEY_TIMESTAMP,new Date());
            //向conversions集合添加数据
            addConversion(conversion);
        }

        //发送离线消息时并进行通知（废弃）
        if(!isReceiverAvailable){
            try {
                JSONArray tokens = new JSONArray();
                tokens.put(receiverUser.token);

                JSONObject data = new JSONObject();
                data.put(Constants.KEY_USER_ID,preferenceManager.getString(Constants.KEY_USER_ID));
                data.put(Constants.KEY_NAME,preferenceManager.getString(Constants.KEY_NAME));
                data.put(Constants.KEY_FCM_TOKEN,preferenceManager.getString(Constants.KEY_FCM_TOKEN));
                data.put(Constants.KEY_MESSAGE,binding.inputMessage.getText().toString());

                JSONObject body = new JSONObject();
                body.put(Constants.REMOTE_MSG_DATA,data);
                body.put(Constants.REMOTE_MSG_REGISTRATION_IDS,tokens);

                sendNotification(body.toString());
            }catch (Exception exception){
                showToast(exception.getMessage());
            }
        }

        //重置输入框
        binding.inputMessage.setText(null);
    }

    /**
     * 起始获取聊天数据
     */
    private void listenAvailabilityOfReceiver() {
        database.collection(Constants.KEY_COLLECTION_USERS).document(
                receiverUser.id
        ).addSnapshotListener(ChatActivity.this,(value, error) -> {
            if(error != null){
                return;
            }
            if(value != null){
                if(value.getLong(Constants.KEY_AVAILABILITY) != null){
                    //获取availability值
                    int availability = Objects.requireNonNull(
                            value.getLong(Constants.KEY_AVAILABILITY)
                    ).intValue();
                    //availability判断是否在线
                    isReceiverAvailable = availability == 1;
                }
                //获取token
                receiverUser.token = value.getString(Constants.KEY_FCM_TOKEN);
                if(receiverUser.image == null){
                    //获取头像字节流
                    receiverUser.image = value.getString(Constants.KEY_IMAGE);
                    //设置头像位图
                    chatAdapter.setReceiverProfileImage(getBitmapFromEncodeString(receiverUser.image));
                    //绑定数据后及时更新
                    chatAdapter.notifyItemRangeChanged(0,chatMessages.size());
                }
            }
            //在线显示
            if(isReceiverAvailable){
                binding.textAvailability.setVisibility(View.VISIBLE);
            }
            //不在线显示
            else{
                binding.textAvailability.setVisibility(View.GONE);
            }
        });
    }

    /**
     * 发送土司
     * @param message 消息体
     */
    private void showToast(String message){
        Toast.makeText(getApplicationContext(),message,Toast.LENGTH_SHORT).show();
    }

    /**
     * 功能废弃（令牌无效）
     * @param messageBody 消息体
     */
    private void sendNotification(String messageBody){
        ApiClient.getClient().create(ApiService.class).sendMessage(
                Constants.getRemoteMsgHeaders(),
                messageBody
        ).enqueue(new Callback<String>() {
            @Override
            public void onResponse(@Nullable Call<String> call, @Nullable Response<String> response) {
                if(response.isSuccessful()){
                    try {
                        if(response.body() != null){
                            JSONObject responseJson = new JSONObject(response.body());
                            JSONArray results = responseJson.getJSONArray("results");
                            if(responseJson.getInt("failure") == 1){
                                JSONObject error = (JSONObject) results.get(0);
                                showToast(error.getString("error"));
                                return;
                            }
                        }
                    }catch (JSONException e){
                        e.printStackTrace();
                    }
                    showToast("离线讯息发送成功");
                }
                else{
                    showToast("错误: "+response.code());
                }
            }

            @Override
            public void onFailure(@Nullable Call<String> call,@Nullable Throwable t) {
                showToast(t.getMessage());
            }
        });
    }

    /**
     * 消息快照监听器（EventListener<QuerySnapshot>接口）
     */
    private final EventListener<QuerySnapshot> eventListener = ((value, error) -> {
        //错误null判断
        if(error != null){
            return;
        }
        //内容null判断
        if(value != null){
            //获取截至目前的消息列表长度
            int count = chatMessages.size();
            //遍历消息快照的列表
            for(DocumentChange documentChange : value.getDocumentChanges()){
                //判断每一条快照是否为新添加的消息query
                if(documentChange.getType() == DocumentChange.Type.ADDED){
                    //创建消息对象
                    ChatMessage chatMessage = new ChatMessage();
                    //消息对象赋值
                    chatMessage.senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    chatMessage.receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    chatMessage.message = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                    //格式化的Time
                    chatMessage.dateTime = getReadableDateTime(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                    //未格式化的Date对象
                    chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    //把每个消息对象添加至消息列表
                    chatMessages.add(chatMessage);
                }
            }
            //消息列表时间升序排序
            Collections.sort(chatMessages,(obj1,obj2)->obj1.dateObject.compareTo(obj2.dateObject));
            if(count == 0){
                chatAdapter.notifyDataSetChanged();
            }
            else{
                //局部刷新功能（增）
                chatAdapter.notifyItemRangeInserted(chatMessages.size(),chatMessages.size());
                //RecyclerView的smoothScrollToPosition()方法以0为起始位置
                binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size()-1);
            }
            //设置RecyclerView聊天视图可见
            binding.chatRecyclerView.setVisibility(View.VISIBLE);
        }
        //设置加载进度条滚动
        binding.progressBar.setVisibility(View.GONE);
        if(conversionId == null){
            checkForConversion();
        }
    });

    /**
     *获取消息快照并设置消息快照监听器
     */
    private void listenMessages(){
        //获取Chat集合（获取本用户发送的消息）
        database.collection(Constants.KEY_COLLECTION_CHAT)
                //匹配Chat集合中 senderId 等于 KEY_USER_ID
                .whereEqualTo(Constants.KEY_SENDER_ID,preferenceManager.getString(Constants.KEY_USER_ID))
                //匹配Chat集合中 receiverId 等于 receiverUser.id
                .whereEqualTo(Constants.KEY_RECEIVER_ID,receiverUser.id)
                //设置快照监听器
                .addSnapshotListener(eventListener);

        //获取Chat集合（获取其它用户发送的消息）
        database.collection(Constants.KEY_COLLECTION_CHAT)
                //匹配Chat集合中 senderId 等于 receiverUser.id
                .whereEqualTo(Constants.KEY_SENDER_ID, receiverUser.id)
                //匹配Chat集合中 receiverId 等于 KEY_USER_ID
                .whereEqualTo(Constants.KEY_RECEIVER_ID,preferenceManager.getString(Constants.KEY_USER_ID))
                //设置快照监听器
                .addSnapshotListener(eventListener);
    }

    /**
     *获取位图
     * @param encodeImage 图片字节数据
     * @return 位图
     */
    private Bitmap getBitmapFromEncodeString(String encodeImage){
        if(encodeImage != null){
            //将资源图片解码
            byte[] bytes = Base64.decode(encodeImage,Base64.DEFAULT);
            //将资源图片转换成Bitmap对象
            return BitmapFactory.decodeByteArray(bytes,0,bytes.length);
        }else{
            return null;
        }
    }

    /**
     * 获取接收消息方的User对象
     */
    private void loadReceiverDetails(){
        //接受其它Activity传输的User对象
        receiverUser = (User) getIntent().getSerializableExtra(Constants.KEY_USER);
        //数据绑定至文本
        binding.textName.setText(receiverUser.name);
    }

    /**
     * 聊天窗口按钮监听
     */
    private void setListeners(){
        //返回按钮监听
        binding.imageBack.setOnClickListener(v->onBackPressed());
        //发送按钮监听
        binding.layoutSend.setOnClickListener(v->sendMessage());
    }

    /**
     * Date类型的时间格式化为String类型
     * @param date Date类型的时间
     * @return String类型的时间
     */
    private String getReadableDateTime(Date date){
        return new SimpleDateFormat("MMMM dd,yyyy - hh:mm a", Locale.getDefault()).format(date);
    }

    private void addConversion(HashMap<String,Object> conversion){
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .add(conversion)
                .addOnSuccessListener(documentReference -> conversionId = documentReference.getId());
    }

    /**
     * conversions集合添加数据
     * @param message 消息文本
     */
    private void updateConversion(String message){
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).document(conversionId);
        documentReference.update(
                Constants.KEY_LAST_MESSAGE,message,
                Constants.KEY_TIMESTAMP,new Date()
        );
    }

    /**
     * 双向监测(对方是发送者/己方是发送者)
     */
    private void checkForConversion(){
        if(chatMessages.size()!=0){
            checkForConversionRemotely(
                    preferenceManager.getString(Constants.KEY_USER_ID),
                    receiverUser.id
            );
            checkForConversionRemotely(
                    receiverUser.id,
                    preferenceManager.getString(Constants.KEY_USER_ID)
            );
        }
    }

    /**
     * @param senderId 发送者id
     * @param receiverId 接收者id
     */
    private void checkForConversionRemotely(String senderId,String receiverId){
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID,senderId)
                .whereEqualTo(Constants.KEY_RECEIVER_ID,receiverId)
                //封装成消息任务（能够监测到Query是否获取成功）
                .get()
                .addOnCompleteListener(conversionOnCompleteListener);
    }

    /**
     * OnCompleteListener能监听成功和失败的监听者
     */
    private final OnCompleteListener<QuerySnapshot> conversionOnCompleteListener = task -> {
        //task.getResult().getDocuments()返回的是集合查询结果的快照（这里只处理success情况）
        if(task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size()>0){
            //因为conversions集合只存在发送者与接收者的一条内容，所以只取0
            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
            //获取该内容在conversions中的内容id
            conversionId = documentSnapshot.getId();
        }
        //失败情况省略
    };
}