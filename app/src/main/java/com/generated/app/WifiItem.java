package com.generated.app;

import android.net.wifi.ScanResult;

public class WifiItem {
    private String ssid;
    private String bssid;
    private int level;
    private String capabilities;
    private boolean isProtected;
    private String securityType;
    private ScanResult scanResult;

    public WifiItem(ScanResult scanResult) {
        this.scanResult = scanResult;
        this.ssid = (scanResult.SSID == null || scanResult.SSID.isEmpty()) ? "[רשת מוסתרת]" : scanResult.SSID;
        this.bssid = scanResult.BSSID;
        this.level = scanResult.level;
        this.capabilities = scanResult.capabilities;

        parseCapabilities(scanResult.capabilities);
    }

    private void parseCapabilities(String caps) {
        if (caps == null) {
            this.isProtected = false;
            this.securityType = "פתוח";
            return;
        }

        if (caps.contains("WPA3")) {
            this.isProtected = true;
            this.securityType = "WPA3";
        } else if (caps.contains("WPA2")) {
            this.isProtected = true;
            this.securityType = "WPA2";
        } else if (caps.contains("WPA")) {
            this.isProtected = true;
            this.securityType = "WPA";
        } else if (caps.contains("WEP")) {
            this.isProtected = true;
            this.securityType = "WEP";
        } else {
            this.isProtected = false;
            this.securityType = "פתוח";
        }
    }

    public String getSsid() {
        return ssid;
    }

    public String getBssid() {
        return bssid;
    }

    public int getLevel() {
        return level;
    }

    public boolean isProtected() {
        return isProtected;
    }

    public String getSecurityType() {
        return securityType;
    }

    public ScanResult getScanResult() {
        return scanResult;
    }
}
