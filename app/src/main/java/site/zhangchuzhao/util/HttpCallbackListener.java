package site.zhangchuzhao.util;

/**
 * Created by Devin on 2016/11/26.
 */

public interface HttpCallbackListener {
    void onFinish(String response);
    void onError(Exception e);
}
