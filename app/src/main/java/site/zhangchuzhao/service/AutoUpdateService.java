package site.zhangchuzhao.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import site.zhangchuzhao.receiver.AutoUpdateReceiver;
import site.zhangchuzhao.util.HttpCallbackListener;
import site.zhangchuzhao.util.HttpUtil;
import site.zhangchuzhao.util.Utility;

/**
 * Created by Devin on 2016/12/1.
 */

public class AutoUpdateService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        //更新网络天气信息比较耗时，则在子线程中执行即可
        new Thread(new Runnable() {
            @Override
            public void run() {
                updateWeatehrInfo();
            }
        });
        /**
         * 设置一个定时任务
         */
        AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
        int eightHour = 8 * 60 * 60 * 1000;
        long triggerAtTime = SystemClock.elapsedRealtime() + eightHour;
        Intent intentTask = new Intent(this, AutoUpdateReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intentTask, 0);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pendingIntent);
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * 更新天气信息
     */
    private void updateWeatehrInfo() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherCode = preferences.getString("weather_code", "");
        String address =  "http://www.weather.com.cn/data/cityinfo/" + weatherCode + ".html";
        //根据当前已保存的天气代号更新天气信息
        HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
            @Override
            public void onFinish(String response) {
                Utility.handleWeatherResponse(AutoUpdateService.this, response);//解析返回的天气信息并保存
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
            }
        });
    }
}
