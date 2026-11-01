package com.generated.app;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class WifiNetworkAdapter extends ArrayAdapter<WifiItem> {

    public WifiNetworkAdapter(Context context, List<WifiItem> networks) {
        super(context, 0, networks);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        WifiItem item = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_wifi, parent, false);
        }

        TextView tvSsid = convertView.findViewById(R.id.tv_ssid);
        TextView tvSecurity = convertView.findViewById(R.id.tv_security);
        TextView tvSignalLevel = convertView.findViewById(R.id.tv_signal_level);
        ImageView ivLockStatus = convertView.findViewById(R.id.iv_lock_status);

        if (item != null) {
            tvSsid.setText(item.getSsid());
            tvSecurity.setText(item.getSecurityType());

            int signalLevel = WifiManager.calculateSignalLevel(item.getLevel(), 5);
            String signalText;
            if (signalLevel >= 4) {
                signalText = getContext().getString(R.string.signal_strong);
            } else if (signalLevel >= 2) {
                signalText = getContext().getString(R.string.signal_medium);
            } else {
                signalText = getContext().getString(R.string.signal_weak);
            }
            tvSignalLevel.setText(signalText);

            if (item.isProtected()) {
                ivLockStatus.setImageResource(R.drawable.ic_lock);
            } else {
                ivLockStatus.setImageResource(R.drawable.ic_unlock);
            }
        }

        return convertView;
    }
}
