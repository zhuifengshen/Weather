package site.zhangchuzhao.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import site.zhangchuzhao.service.AutoUpdateService;

/**
 * Created by Devin on 2016/12/1.
 */

public class AutoUpdateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent intentService = new Intent(context, AutoUpdateService.class);
        context.startService(intentService);
    }
}
