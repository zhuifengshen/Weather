package site.zhangchuzhao.weather;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import site.zhangchuzhao.service.AutoUpdateService;
import site.zhangchuzhao.util.HttpCallbackListener;
import site.zhangchuzhao.util.HttpUtil;
import site.zhangchuzhao.util.Utility;

public class WeatherActivity extends Activity implements View.OnClickListener{

    private LinearLayout weatherInfoLayout;
    /**
     * 用于显示城市名
     */
    private TextView cityNameText;
    /**
     * 用于显示发布时间
     */
    private TextView publicshText;
    /**
     * 用于显示天气描述信息
     */
    private TextView weatherDespText;
    /**
     * 用于显示最低气温
     */
    private TextView temp1Text;
    /**
     * 用于显示最高气温
     */
    private TextView temp2Text;
    /**
     * 用于显示当前时间
     */
    private TextView currentDateText;
    /**
     * 切换城市按钮
     */
    private TextView switchCity;
    /**
     * 更新天气按钮
     */
    private TextView refreshWeather;
    /**
     * 县级代码
     */
    private String countyCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_weather);
        /**
         * 初始化各控件
         */
        weatherInfoLayout = (LinearLayout)findViewById(R.id.weather_info_layout);
        cityNameText = (TextView)findViewById(R.id.city_name);
        publicshText = (TextView)findViewById(R.id.publish_text);
        weatherDespText = (TextView)findViewById(R.id.weather_desp);
        temp1Text = (TextView)findViewById(R.id.temp1);
        temp2Text = (TextView)findViewById(R.id.temp2);
        currentDateText = (TextView)findViewById(R.id.current_date);
        switchCity = (Button)findViewById(R.id.switch_city);
        refreshWeather = (Button)findViewById(R.id.refresh_weather);
        /**
         * 跳转到天气界面有两种情况:
         * 1.逐步选择地点, 然后跳转进来, 并去请求网上天气数据;
         * 2.已经选择地点, 直接跳转进来,天气数据直接使用上一次的数据
         */
        countyCode = getIntent().getStringExtra("county_code");
        if (!TextUtils.isEmpty(countyCode)){
            //有县级代号时就去查询天气
            publicshText.setText("同步中...");
            weatherInfoLayout.setVisibility(View.INVISIBLE);
            cityNameText.setVisibility(View.INVISIBLE);
            queryWeatherCode(countyCode);
        }else {
            //直接读取本地天气数据
            showWeather();
        }
        switchCity.setOnClickListener(this);
        refreshWeather.setOnClickListener(this);
    }

    /**
     * 响应切换城市和更新天气的点击事件
     * @param view
     */
    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.switch_city:
                Intent intent = new Intent(this, ChooseAreaActivity.class);
                intent.putExtra("from_weather_activity", true);
                startActivity(intent);
                finish();
                break;
            case R.id.refresh_weather:
                publicshText.setText("同步中...");
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                String weatherCode = preferences.getString("weather_code", "");
                if (!TextUtils.isEmpty(weatherCode)){
                    queryWeatherInfo(weatherCode);
                }else {
                    //首次使用时，如果网络不好导致显示失败，这时点击刷新按钮，则重新请求网络，而不是从本地获取数据
                    queryWeatherCode(countyCode);
                }
                break;
            default:

                break;

        }

    }

    /**
     * 查询县级代号所对应的天气代号
     * @param countyCode 县级代号
     */
    private void queryWeatherCode(String countyCode) {
        String address = "http://www.weather.com.cn/data/list3/city" + countyCode + ".xml";
        queryFromServer(address, "countyCode");
    }

    /**
     * 查询天气代号所对应的天气
     * @param weatherCode 天气代号
     */
    private void queryWeatherInfo(String weatherCode){
        String address = "http://www.weather.com.cn/data/cityinfo/" + weatherCode + ".html";
        queryFromServer(address, "weatherCode");
    }

    /**
     * 根据传入的地址和类型向服务器查询天气代号和天气信息
     * @param address 请求地址
     * @param type 请求类型
     */
    private void queryFromServer(String address, final String type) {
        Log.d("weather", "type: " + type + " address: " + address);
        HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
            @Override
            public void onFinish(String response) {
                Log.d("weather info: ", response);
                if ("countyCode".equals(type)){
                    if (!TextUtils.isEmpty(response)){
                        String[] array = response.split("\\|");
                        if (array != null && array.length == 2){
                            String weatherCode = array[1];
                            queryWeatherInfo(weatherCode);
                        }
                    }
                }else if ("weatherCode".equals(type)){
                    if (!TextUtils.isEmpty(response)){
                        Utility.handleWeatherResponse(WeatherActivity.this, response);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showWeather();
                            }
                        });
                    }
                }
            }
            @Override
            public void onError(Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        publicshText.setText("同步失败, 请稍后再试");
                    }
                });
                e.printStackTrace();
            }
        });
    }

    /**
     * 从SharedPreferences文件中读取存储的天气新,并显示在界面上
     */
    private void showWeather() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        cityNameText.setText(preferences.getString("city_name", ""));
        publicshText.setText("今天" + preferences.getString("publish_time", "") + "发布");
        weatherDespText.setText(preferences.getString("weather_desp", ""));
        temp1Text.setText(preferences.getString("temp1", ""));
        temp2Text.setText(preferences.getString("temp2", ""));
        currentDateText.setText(preferences.getString("current_date", ""));
        weatherInfoLayout.setVisibility(View.VISIBLE);
        cityNameText.setVisibility(View.VISIBLE);
        //启动定时更新天气
        Intent autoUpdateIntent = new Intent(this, AutoUpdateService.class);
        startService(autoUpdateIntent);
    }
}
