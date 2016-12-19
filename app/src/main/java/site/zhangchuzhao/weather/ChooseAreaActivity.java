package site.zhangchuzhao.weather;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import site.zhangchuzhao.db.CoolWeatherDB;
import site.zhangchuzhao.model.City;
import site.zhangchuzhao.model.County;
import site.zhangchuzhao.model.Province;
import site.zhangchuzhao.util.HttpCallbackListener;
import site.zhangchuzhao.util.HttpUtil;
import site.zhangchuzhao.util.Utility;

public class ChooseAreaActivity extends Activity {
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY =2;
    /**
     * 当前选中的级别
     */
    private int currentLevel;
    /**
     * 选中的省份
     */
    private Province selectedProvince;
    /**
     * 选中的城市
     */
    private City selectedCity;
    /**
     * 选中的县
     */
    private County selectedCounty;

    private CoolWeatherDB coolWeatherDB;
    private ProgressDialog progressDialog;
    private TextView titleText;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> dataList = new ArrayList<>();
    /**
     * 省列表
     */
    private List<Province> provinceList;
    /**
     * 市列表
     */
    private List<City> cityList;
    /**
     * 县列表
     */
    private List<County> countyList;
    /**
     * 是否从WeatherActivity跳转过来
     */
    private boolean isFromWeatherActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /**
         * 启动应用时, 先检测是否已现在城市, 如果已选择且不是从不是从WeatherActivity跳转过来重新选择，则直接跳转到天气显示界面(即:使用本地数据即可)
         */
        isFromWeatherActivity = getIntent().getBooleanExtra("from_weather_activity", false);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isSelectedCity = preferences.getBoolean("city_selected", false);
        Log.d("weather", "isFromWeatherActivity: " + isFromWeatherActivity + " isSelectedCity: " + isSelectedCity);
        if (!isFromWeatherActivity && isSelectedCity){
            Intent intent = new Intent(this, WeatherActivity.class);
            startActivity(intent);
            finish();
        }

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_choose_area);
        //初始化数据库
        coolWeatherDB = CoolWeatherDB.getInstance(this);
        //初始化界面元素
        titleText = (TextView)findViewById(R.id.title_text);
        listView = (ListView)findViewById(R.id.list_view);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,dataList);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (currentLevel == LEVEL_PROVINCE){
                    selectedProvince = provinceList.get(i);
                    queryCities();
                }else if (currentLevel == LEVEL_CITY){
                    selectedCity = cityList.get(i);
                    queryCounties();
                }else if (currentLevel == LEVEL_COUNTY){
                    Intent intent = new Intent(ChooseAreaActivity.this, WeatherActivity.class);
                    selectedCounty = countyList.get(i);
                    intent.putExtra("county_code", selectedCounty.getCountyCode());
                    startActivity(intent);
                    finish();
                }
            }
        });
        queryProvinces();//默认首次打开应用,显示省份列表
    }

    /**
     * 查询全国所有的省份, 优先从数据库查询,如果没有再到服务器上查询
     */
    private void queryProvinces() {
        provinceList = coolWeatherDB.loadProvinces();
        if (provinceList.size() > 0){
            dataList.clear();
            for (Province province : provinceList) {
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText("中国");
            currentLevel = LEVEL_PROVINCE;
        }else {
            queryFromServer(null, "province");
        }
    }

    /**
     * 查询选中省内所有的县, 优先从数据库查询,如果没有再到服务器上查询
     */
    private void queryCities() {
        cityList = coolWeatherDB.loadCities(selectedProvince.getId());
        if (cityList.size() > 0){
            dataList.clear();
            for (City city : cityList) {
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText(selectedProvince.getProvinceName());
            currentLevel = LEVEL_CITY;
        }else {
            queryFromServer(selectedProvince.getProvinceCode(), "city");
        }
    }

    /**
     * 查询选中市内所有的县, 优先从数据库查询,如果没有再到服务器上查询
     */
    private void queryCounties() {
        countyList = coolWeatherDB.loadCounties(selectedCity.getId());
        if (countyList.size() > 0){
            dataList.clear();
            for (County county : countyList) {
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText(selectedCity.getCityName());
            currentLevel = LEVEL_COUNTY;
        }else {
            queryFromServer(selectedCity.getCityCode(), "county");
        }
    }

    /**
     *
     * @param code
     * @param type
     */
    private void queryFromServer(final String code, final String type) {
        String address;
        if (TextUtils.isEmpty(code)){
            address = "http://www.weather.com.cn/data/list3/city.xml";
        }else {
            address = "http://www.weather.com.cn/data/list3/city" + code + ".xml";
        }
        //开始请求网络,显示进度缓冲对话框
        showProgressDialog();
        HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
            @Override
            public void onFinish(final String response) {
                boolean result = false;
                if ("province".equals(type)){
                    //Log.d("weather", "type == \"province\"" + (type == "province"));
                    result = Utility.handleProvincesResponse(coolWeatherDB, response);
                }else if ("city".equals(type)){
                    //Log.d("weather", "type == \"city\"" + (type == "city"));
                    result = Utility.handleCitiesResponse(coolWeatherDB, response, selectedProvince.getId());
                }else if ("county".equals(type)){
                    //Log.d("weather", "type == \"county\"" + (type == "county"));
                    result = Utility.handleCountiesResponse(coolWeatherDB, response, selectedCity.getId());
                }

                if (result){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if ("province".equals(type)){
                                queryProvinces();
                            }else if ("city".equals(type)){
                                queryCities();
                            }else if ("county".equals(type)){
                                queryCounties();
                            }
                        }
                    });
                }else {
                    Toast.makeText(ChooseAreaActivity.this, "暂无该地区天气数据", Toast.LENGTH_LONG).show();
                }
                //网络完成,退出进度缓冲对话框
                closeProgressDialog();
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(ChooseAreaActivity.this, "天气数据请求失败,稍候再试", Toast.LENGTH_LONG).show();
                    }
                });
                e.printStackTrace();
            }
        });
    }

    /**
     * 显示进度对话框
     */
    private void showProgressDialog() {
        if (progressDialog == null){
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    /**
     * 关闭进度对话框
     */
    private void closeProgressDialog(){
        if (progressDialog != null){
            progressDialog.dismiss();
        }
    }

    /**
     * 捕获Back按键,根据当前的级别判断,应该返回市列表\省列表,还是直接退出
     */
    @Override
    public void onBackPressed(){
        if (currentLevel == LEVEL_COUNTY){
            queryCities();
        }else if (currentLevel == LEVEL_CITY){
            queryProvinces();
        }else {
            if (isFromWeatherActivity){
                Intent intent = new Intent(this, WeatherActivity.class);
                startActivity(intent);
            }
            finish();
        }
    }

}
