package com.generated.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {

    private static final int PERMISSION_REQUEST_CODE = 1001;

    private WifiManager wifiManager;
    private WifiNetworkAdapter adapter;
    private List<WifiItem> wifiListItems = new ArrayList<>();

    private TextView tvConnectedStatus;
    private TextView tvWifiState;
    private TextView tvNetworkCount;
    private TextView tvEmptyMessage;
    private ProgressBar pbScanning;
    private LinearLayout layoutEmpty;
    private Button btnEnableWifi;
    private Button btnScanNetworks;
    private Button btnGrantPermission;
    private NonScrollListView listWifi;

    private BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
                pbScanning.setVisibility(View.GONE);
                updateWifiList();
            } else if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
                updateWifiStatusUI();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        initViews();
        setupListeners();

        // Turn on Wi-Fi automatically if disabled
        enableWifiAutomaticallySilently();

        if (checkAndRequestPermissions()) {
            startWifiScan();
        }
    }

    private void initViews() {
        tvConnectedStatus = findViewById(R.id.tv_connected_status);
        tvWifiState = findViewById(R.id.tv_wifi_state);
        tvNetworkCount = findViewById(R.id.tv_network_count);
        tvEmptyMessage = findViewById(R.id.tv_empty_message);
        pbScanning = findViewById(R.id.pb_scanning);
        layoutEmpty = findViewById(R.id.layout_empty);
        btnEnableWifi = findViewById(R.id.btn_enable_wifi);
        btnScanNetworks = findViewById(R.id.btn_scan_networks);
        btnGrantPermission = findViewById(R.id.btn_grant_permission);
        listWifi = findViewById(R.id.list_wifi);

        adapter = new WifiNetworkAdapter(this, wifiListItems);
        listWifi.setAdapter(adapter);

        updateWifiStatusUI();
        updateConnectedWifiInfo();
    }

    private void setupListeners() {
        btnEnableWifi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enableWifiAutomaticallyWithFeedback();
            }
        });

        btnScanNetworks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkAndRequestPermissions()) {
                    startWifiScan();
                }
            }
        });

        btnGrantPermission.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAndRequestPermissions();
            }
        });

        listWifi.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                WifiItem selectedItem = adapter.getItem(position);
                if (selectedItem != null) {
                    showConnectDialog(selectedItem);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        registerReceiver(wifiReceiver, filter);

        updateWifiStatusUI();
        updateConnectedWifiInfo();
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(wifiReceiver);
        } catch (Exception ignored) {
        }
    }

    private boolean checkAndRequestPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission("android.permission.NEARBY_WIFI_DEVICES") != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add("android.permission.NEARBY_WIFI_DEVICES");
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            requestPermissions(permissionsNeeded.toArray(new String[0]), PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                layoutEmpty.setVisibility(View.GONE);
                btnGrantPermission.setVisibility(View.GONE);
                startWifiScan();
            } else {
                tvEmptyMessage.setText(R.string.permission_required);
                btnGrantPermission.setVisibility(View.VISIBLE);
                layoutEmpty.setVisibility(View.VISIBLE);
            }
        }
    }

    private void enableWifiAutomaticallySilently() {
        if (!wifiManager.isWifiEnabled()) {
            try {
                wifiManager.setWifiEnabled(true);
            } catch (Exception ignored) {
            }
        }
    }

    private void enableWifiAutomaticallyWithFeedback() {
        if (!wifiManager.isWifiEnabled()) {
            try {
                boolean success = wifiManager.setWifiEnabled(true);
                if (!success) {
                    Toast.makeText(this, "פותח הגדרות Wi-Fi להפעלה...", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                } else {
                    Toast.makeText(this, "רכיב ה-Wi-Fi הופעל בהצלחה", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            }
        } else {
            Toast.makeText(this, "רכיב ה-Wi-Fi כבר פעיל", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateWifiStatusUI() {
        if (wifiManager != null && wifiManager.isWifiEnabled()) {
            tvWifiState.setText(R.string.wifi_on);
            tvWifiState.setTextColor(Color.parseColor("#10B981"));
        } else {
            tvWifiState.setText(R.string.wifi_off);
            tvWifiState.setTextColor(Color.parseColor("#EF4444"));
        }
    }

    private void startWifiScan() {
        if (!wifiManager.isWifiEnabled()) {
            enableWifiAutomaticallySilently();
        }

        pbScanning.setVisibility(View.VISIBLE);
        boolean started = wifiManager.startScan();
        if (!started) {
            pbScanning.setVisibility(View.GONE);
            updateWifiList();
        }
    }

    private void updateWifiList() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        List<ScanResult> scanResults = wifiManager.getScanResults();
        List<WifiItem> processedList = processScanResults(scanResults);

        wifiListItems.clear();
        wifiListItems.addAll(processedList);
        adapter.notifyDataSetChanged();

        tvNetworkCount.setText("(" + wifiListItems.size() + ")");

        if (wifiListItems.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            tvEmptyMessage.setText(R.string.no_networks);
            btnGrantPermission.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
        }

        updateConnectedWifiInfo();
    }

    private List<WifiItem> processScanResults(List<ScanResult> results) {
        Map<String, ScanResult> bestResultsMap = new HashMap<>();

        if (results != null) {
            for (ScanResult result : results) {
                if (result.SSID == null || result.SSID.trim().isEmpty()) {
                    continue;
                }
                String ssid = result.SSID;
                if (!bestResultsMap.containsKey(ssid) || bestResultsMap.get(ssid).level < result.level) {
                    bestResultsMap.put(ssid, result);
                }
            }
        }

        List<WifiItem> items = new ArrayList<>();
        for (ScanResult result : bestResultsMap.values()) {
            items.add(new WifiItem(result));
        }

        Collections.sort(items, new Comparator<WifiItem>() {
            @Override
            public int compare(WifiItem o1, WifiItem o2) {
                return Integer.compare(o2.getLevel(), o1.getLevel());
            }
        });

        return items;
    }

    private void updateConnectedWifiInfo() {
        WifiInfo info = wifiManager.getConnectionInfo();
        if (info != null && info.getNetworkId() != -1) {
            String ssid = info.getSSID();
            if (ssid != null && ssid.startsWith("\"") && ssid.endsWith("\"")) {
                ssid = ssid.substring(1, ssid.length() - 1);
            }
            if (ssid != null && !ssid.equals("<unknown ssid>")) {
                tvConnectedStatus.setText(getString(R.string.connected_to, ssid));
                return;
            }
        }
        tvConnectedStatus.setText("לא מחובר לרשת");
    }

    private void showConnectDialog(final WifiItem item) {
        if (!item.isProtected()) {
            connectToWifi(item, "");
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_connect, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvSsid = dialogView.findViewById(R.id.dialog_ssid);
        final EditText etPassword = dialogView.findViewById(R.id.et_password);
        Button btnCancel = dialogView.findViewById(R.id.btn_dialog_cancel);
        Button btnConnect = dialogView.findViewById(R.id.btn_dialog_connect);

        tvSsid.setText(item.getSsid());

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String password = etPassword.getText().toString().trim();
                if (password.length() < 8 && item.getSecurityType().contains("WPA")) {
                    etPassword.setError("סיסמה חייבת להכיל לפחות 8 תווים");
                    return;
                }
                dialog.dismiss();
                connectToWifi(item, password);
            }
        });

        dialog.show();
    }

    private void connectToWifi(WifiItem item, String password) {
        Toast.makeText(this, String.format(getString(R.string.connecting), item.getSsid()), Toast.LENGTH_SHORT).show();

        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            connectWifiQPlus(item, password);
        } else {
            connectWifiLegacy(item, password);
        }
    }

    private void connectWifiLegacy(WifiItem item, String password) {
        try {
            WifiConfiguration wifiConfig = new WifiConfiguration();
            wifiConfig.SSID = String.format("\"%s\"", item.getSsid());

            if (item.isProtected()) {
                if (item.getSecurityType().equalsIgnoreCase("WEP")) {
                    wifiConfig.wepKeys[0] = String.format("\"%s\"", password);
                    wifiConfig.wepTxKeyIndex = 0;
                    wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                } else {
                    wifiConfig.preSharedKey = String.format("\"%s\"", password);
                }
            } else {
                wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            }

            int netId = wifiManager.addNetwork(wifiConfig);
            if (netId == -1) {
                List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
                if (list != null) {
                    for (WifiConfiguration i : list) {
                        if (i.SSID != null && i.SSID.equals("\"" + item.getSsid() + "\"")) {
                            netId = i.networkId;
                            break;
                        }
                    }
                }
            }

            if (netId != -1) {
                wifiManager.disconnect();
                boolean enabled = wifiManager.enableNetwork(netId, true);
                wifiManager.reconnect();
                if (enabled) {
                    Toast.makeText(this, String.format(getString(R.string.connect_success), item.getSsid()), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, String.format(getString(R.string.connect_failed), item.getSsid()), Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, String.format(getString(R.string.connect_failed), item.getSsid()), Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "שגיאה בחיבור: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void connectWifiQPlus(final WifiItem item, final String password) {
        try {
            WifiNetworkSpecifier.Builder builder = new WifiNetworkSpecifier.Builder();
            builder.setSsid(item.getSsid());

            if (item.isProtected()) {
                builder.setWpa2Passphrase(password);
            }

            WifiNetworkSpecifier specifier = builder.build();

            NetworkRequest request = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .setNetworkSpecifier(specifier)
                    .build();

            final ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                connectivityManager.requestNetwork(request, new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(Network network) {
                        super.onAvailable(network);
                        connectivityManager.bindProcessToNetwork(network);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, String.format(getString(R.string.connect_success), item.getSsid()), Toast.LENGTH_LONG).show();
                                updateConnectedWifiInfo();
                            }
                        });
                    }

                    @Override
                    public void onUnavailable() {
                        super.onUnavailable();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                connectWifiLegacy(item, password);
                            }
                        });
                    }
                });
            }
        } catch (Exception e) {
            connectWifiLegacy(item, password);
        }
    }
}
