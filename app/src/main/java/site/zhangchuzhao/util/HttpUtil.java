package site.zhangchuzhao.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Devin on 2016/11/26.
 */

public class HttpUtil {
    public static void sendHttpRequest(final String address, final HttpCallbackListener listener){
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                try {
                    URL url = new URL(address);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("Accept-Encoding", "");
                    //connection.setRequestProperty("Accept-Encoding", "musixmatch");
                    connection.setConnectTimeout(8000);
                    connection.setReadTimeout(8000);
                    //Log.d("weather", connection.getResponseCode() + " : " + connection.getResponseMessage());
                    InputStream in = connection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null){
                        response.append(line);
                    }

                    //二进制流读取
                    //byte[] bytes = new byte[1024];
                    //for(int len; (len = in.read()) != -1;){
                    //    response.append(new String(bytes, 0, len));
                    //}

                    //字符流读取
                    //char[] data = new char[1000];
                    //int length;
                    //while ((length = reader.read(data)) != -1){
                    //    String line = String.valueOf(data, 0, length);
                    //    response.append(line);
                    //}

                    //缓冲区一次性读取
                    //char[] data = new char[1000];
                    //int length = reader.read(data);
                    //String line = String.valueOf(data, 0, length);
                    //response.append(line);

                    if (listener != null){
                        listener.onFinish(response.toString());
                    }
                } catch (Exception e) {
                    if (listener != null){
                        listener.onError(e);
                    }
                }finally {
                    if (connection != null){
                        connection.disconnect();
                    }
                }
            }
        }).start();
    }
}
