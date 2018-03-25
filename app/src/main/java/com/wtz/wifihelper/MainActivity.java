package com.wtz.wifihelper;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.wang.avi.AVLoadingIndicatorView;
import com.wtz.wifihelper.adapter.WifiListAdapter;
import com.wtz.wifihelper.utils.ReceiverUtil;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private final static String TAG = "MainActivity";

    private WifiManager mWifiManager;
    private WifiListAdapter mWifiListAdapter;

    private final static int WIFICIPHER_NOPASS = 0;
    private final static int WIFICIPHER_WPA = 1;
    private final static int WIFICIPHER_WEP = 2;

    private TextView tvConnectState;

    private String[] permissions = new String[]{
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    private List<String> mPermissionList = new ArrayList<>();
    private final static int REQUEST_PERMISSIONS_CODE = 1;

    private AVLoadingIndicatorView mLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= 23) {
            judgePermission();
        }

        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mWifiListAdapter = new WifiListAdapter(this, null);
        registerReceiver();

        Button buttonScan = (Button) findViewById(R.id.button_scan);
        buttonScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "buttonScan.onClick");
                searchWifi();
            }
        });

        tvConnectState = findViewById(R.id.tv_connect_state);

        ListView lvWifiList = findViewById(R.id.lv_wifi_list);
        lvWifiList.setAdapter(mWifiListAdapter);
        lvWifiList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "lvWifiList.onItemClick...position=" + position);
                connectWifiItem(position);
            }
        });

        mLoading = (AVLoadingIndicatorView) findViewById(R.id.loading);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        ReceiverUtil.unregisterReceiverSafely(this, mReceiver);
        super.onDestroy();
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void judgePermission() {
        mPermissionList.clear();
        for (int i = 0; i < permissions.length; i++) {
            if (checkSelfPermission(permissions[i]) != PackageManager.PERMISSION_GRANTED) {
                mPermissionList.add(permissions[i]);
            }
        }
        if (mPermissionList.isEmpty()) {//未授予的权限为空，表示都授予了
            // TODO: do something
        } else {//请求权限方法
            String[] permissions = mPermissionList.toArray(new String[mPermissionList.size()]);//将List转为数组
            this.requestPermissions(permissions, REQUEST_PERMISSIONS_CODE);
        }
    }

    private void registerReceiver() {
        IntentFilter mFilter = new IntentFilter();
        mFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        mFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(mReceiver, mFilter);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "mReceiver: " + action);
            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                onWifiScanResult();
            } else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                onWifiStateChange(context, intent);
            }
        }
    };

    /**
     * 搜索wifi热点
     */
    private void searchWifi() {
        if (mWifiManager == null) {
            Log.d(TAG, "mWifiManager is null");
            return;
        }
        if (!mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(true);
        }
        mLoading.show();
        mWifiManager.startScan();
    }

    private void onWifiScanResult() {
        mLoading.hide();
        List<ScanResult> scanResults = mWifiManager.getScanResults();
        Log.d(TAG, "scanResults = " + scanResults);
        mWifiListAdapter.update(scanResults);
        if (scanResults == null || scanResults.size() == 0) {
            Toast.makeText(MainActivity.this, "scanResults is null", Toast.LENGTH_LONG).show();
        }
    }

    private void connectWifiItem(int position) {
        final ScanResult scanResult = (ScanResult) mWifiListAdapter.getItem(position);

        Log.d(TAG, "capabilities=" + scanResult.capabilities);
        int type = getEncryptType(scanResult.capabilities);

        WifiConfiguration config = findExistWifiConfig(scanResult.SSID);
        Log.d(TAG, scanResult.SSID + " exsits? " + (config != null));

        if (config != null) {
            //过去连接成功有记录
            connectWifi(config);
            return;
        }

        if (type == WIFICIPHER_NOPASS) {
            // 无需密码
            connectWifi(createWifiInfo(scanResult.SSID, "", type));
            return;
        }

        //需要密码
        requestUserPassword(scanResult, type);
    }

    private int getEncryptType(String capabilities) {
        int type = WIFICIPHER_WPA;
        if (!TextUtils.isEmpty(capabilities)) {
            if (capabilities.contains("WPA") || capabilities.contains("wpa")) {
                type = WIFICIPHER_WPA;
            } else if (capabilities.contains("WEP") || capabilities.contains("wep")) {
                type = WIFICIPHER_WEP;
            } else {
                type = WIFICIPHER_NOPASS;
            }
        }
        return type;
    }

    private WifiConfiguration findExistWifiConfig(String SSID) {
        List<WifiConfiguration> existingConfigs = mWifiManager.getConfiguredNetworks();
        for (WifiConfiguration existingConfig : existingConfigs) {
            if (existingConfig.SSID.equals("\"" + SSID + "\"")) {
                Log.d(TAG, "existingConfig.SSID = " + existingConfig.SSID);
                return existingConfig;
            }
        }
        return null;
    }

    private void requestUserPassword(final ScanResult scanResult, int type) {
        final EditText editText = new EditText(MainActivity.this);
        final int finalType = type;
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("请输入Wifi密码")
                .setIcon(android.R.drawable.ic_dialog_info)
                .setView(editText)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String pwd = editText.getText().toString();
                        if (TextUtils.isEmpty(pwd)) {
                            Toast.makeText(MainActivity.this, "password is empty!", Toast.LENGTH_LONG).show();
                            return;
                        }
                        connectWifi(createWifiInfo(scanResult.SSID, pwd, finalType));
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 创建WifiConfiguration对象
     * 分为三种情况：1没有密码;2用wep加密;3用wpa加密
     *
     * @param SSID
     * @param Password
     * @param Type
     * @return
     */
    public WifiConfiguration createWifiInfo(String SSID, String Password, int Type) {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + SSID + "\"";

        WifiConfiguration tempConfig = this.findExistWifiConfig(SSID);
        if (tempConfig != null) {
            mWifiManager.removeNetwork(tempConfig.networkId);
        }

        if (Type == WIFICIPHER_NOPASS) {
            config.wepKeys[0] = "\"" + "\"";
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        }
        if (Type == WIFICIPHER_WEP) {
            config.hiddenSSID = true;
            config.wepKeys[0] = "\"" + Password + "\"";
            config.allowedAuthAlgorithms
                    .set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            config.allowedGroupCiphers
                    .set(WifiConfiguration.GroupCipher.WEP104);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        }
        if (Type == WIFICIPHER_WPA) {
            config.preSharedKey = "\"" + Password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms
                    .set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers
                    .set(WifiConfiguration.PairwiseCipher.TKIP);
            // config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers
                    .set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;
        }
        return config;
    }

    private void connectWifi(WifiConfiguration config) {
        mWifiManager.disconnect();

        // netId is -1 on failure
        int netId = mWifiManager.addNetwork(config);
        Log.d(TAG, "addNetwork return netId = " + netId);
        Toast.makeText(this, "netId = " + netId, Toast.LENGTH_LONG).show();
        if (netId == -1) {
            // 移除过去连接成功但现在密码已经更换的wifi记录
            mWifiManager.removeNetwork(config.networkId);
            return;
        }

        mWifiManager.enableNetwork(netId, true);
    }

    private void onWifiStateChange(Context context, Intent intent) {
        NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
        if (info.getState().equals(NetworkInfo.State.DISCONNECTED)) {
            tvConnectState.setText("连接已断开");
        } else if (info.getState().equals(NetworkInfo.State.CONNECTED)) {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            final WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            tvConnectState.setText("已连接到网络:" + wifiInfo.getSSID());
        } else {
            NetworkInfo.DetailedState state = info.getDetailedState();
            if (state == state.CONNECTING) {
                tvConnectState.setText("连接中...");
            } else if (state == state.AUTHENTICATING) {
                tvConnectState.setText("正在验证身份信息...");
            } else if (state == state.OBTAINING_IPADDR) {
                tvConnectState.setText("正在获取IP地址...");
            } else if (state == state.FAILED) {
                tvConnectState.setText("连接失败");
            }
        }
    }

}
