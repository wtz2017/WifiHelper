package com.wtz.wifihelper.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;

import java.util.List;

/**
 * Created by WTZ on 2018/3/18.
 */

public class ReceiverUtil {

    public static void registerReceiver(Context context, BroadcastReceiver receiver, List<String> actions) {
        IntentFilter mFilter = new IntentFilter();
        for (String action : actions) {
            mFilter.addAction(action);
        }
        context.registerReceiver(receiver, mFilter);
    }

    public static void unregisterReceiverSafely(Context context, BroadcastReceiver receiver) {
        try {
            context.unregisterReceiver(receiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
