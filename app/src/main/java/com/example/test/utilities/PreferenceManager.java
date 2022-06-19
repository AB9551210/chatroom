package com.example.test.utilities;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceManager {
    //SharedPreferences：将少量简单类型数据保存在本地，如：用户设置。
    private final SharedPreferences sharedPreferences;

    public PreferenceManager(Context context){
        /**
         * name：命名
         * mode：模式，包括
         * MODE_PRIVATE（只能被自己的应用程序访问）
         * MODE_WORLD_READABLE（除了自己访问外还可以被其它应该程序读取）
         * MODE_WORLD_WRITEABLE（除了自己访问外还可以被其它应该程序读取和写入）
         * */
        sharedPreferences = context.getSharedPreferences(Constants.KEY_PREFERENCE_NAME,Context.MODE_PRIVATE);
    }

    public void putBoolean(String key,Boolean value){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key,value);
        editor.apply();
    }

    public Boolean getBoolean(String key){
        return sharedPreferences.getBoolean(key,false);
    }

    public void putString(String key,String value){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key,value);
        editor.apply();
    }

    public String getString(String key){
        return sharedPreferences.getString(key,null);
    }

    public void clear(){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }
}
