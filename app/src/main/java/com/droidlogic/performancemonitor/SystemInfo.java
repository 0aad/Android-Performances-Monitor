package com.droidlogic.performancemonitor;

import com.droidlogic.performancemonitor.NetworkUtils.NetworkInfo;

public class SystemInfo {
    String identifier = "";
    String memInfo = "";
    String cpuUsage = "";
    NetworkInfo networkInfo = new NetworkInfo(0, 0);
    String formattedDownloadSpeed = "0.00 KB/s";
    String formattedUploadSpeed = "0.00 KB/s";
    String thermalinfo = "";

    public void updateNetworkInfo(NetworkInfo networkInfo) {
        this.networkInfo = networkInfo;
    }

    public void thermalinfo(String info) {
    }
}



