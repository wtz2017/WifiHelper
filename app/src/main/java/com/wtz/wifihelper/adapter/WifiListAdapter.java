package com.wtz.wifihelper.adapter;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.wtz.wifihelper.R;

import java.util.List;

public class WifiListAdapter extends BaseAdapter {
    private final static String TAG = WifiListAdapter.class.getName();

    private Context mContext;

    private List<ScanResult> mList;

    public WifiListAdapter(Context context, List<ScanResult> list) {
        mContext = context;
        mList = list;
    }

    public void update(List<ScanResult> list) {
        mList = list;
        notifyDataSetChanged();
    }

    public List<ScanResult> getList() {
        return mList;
    }

    @Override
    public int getCount() {
        return (mList == null) ? 0 : mList.size();
    }

    @Override
    public Object getItem(int position) {
        return (mList == null) ? null : mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (null == mContext) {
            Log.d(TAG, "getView...null == mContext");
            return null;
        }

        if (null == mList || mList.isEmpty()) {
            Log.d(TAG, "getView...list isEmpty");
            return null;
        }

        ViewHolder itemLayout = null;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.wifi_item_layout, null);
            itemLayout = new ViewHolder();
            itemLayout.tvName = (TextView) convertView.findViewById(R.id.tv_name);
            itemLayout.tvEncrypt = (TextView) convertView.findViewById(R.id.tv_encrypt);
            itemLayout.ivIcon = (ImageView) convertView.findViewById(R.id.iv_icon);
            convertView.setTag(itemLayout);
        } else {
            itemLayout = (ViewHolder) convertView.getTag();
        }

        ScanResult item = null;
        if ((item = mList.get(position)) != null) {
            itemLayout.tvName.setText(item.SSID);
            itemLayout.tvEncrypt.setText(item.capabilities);

            int level = item.level;
            if (level <= 0 && level >= -50) {
                itemLayout.ivIcon.setImageResource(R.mipmap.ic_wifi_signal_4);
            } else if (level < -50 && level >= -70) {
                itemLayout.ivIcon.setImageResource(R.mipmap.ic_wifi_signal_3);
            } else if (level < -70 && level >= -80) {
                itemLayout.ivIcon.setImageResource(R.mipmap.ic_wifi_signal_2);
            } else if (level < -80 && level >= -100) {
                itemLayout.ivIcon.setImageResource(R.mipmap.ic_wifi_signal_1);
            } else {
                itemLayout.ivIcon.setImageResource(R.mipmap.ic_wifi_signal_1);
            }
        }

        return convertView;
    }

    class ViewHolder {
        TextView tvName;
        TextView tvEncrypt;
        ImageView ivIcon;
    }

}
