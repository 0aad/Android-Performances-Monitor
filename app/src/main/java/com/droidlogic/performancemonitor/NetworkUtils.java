package com.droidlogic.performancemonitor;


import android.net.TrafficStats;

public class NetworkUtils {
    private static long previousRxBytes = 0;
    private static long previousTxBytes = 0;
    private static long previousTime = 0;

    public static NetworkInfo getNetworkInfo() {
        long currentRxBytes = TrafficStats.getTotalRxBytes();
        long currentTxBytes = TrafficStats.getTotalTxBytes();
        long currentTime = System.currentTimeMillis();

        if (previousTime == 0) {
            previousRxBytes = currentRxBytes;
            previousTxBytes = currentTxBytes;
            previousTime = currentTime;
            return new NetworkInfo(0, 0);
        }

        long rxBytes = currentRxBytes - previousRxBytes;
        long txBytes = currentTxBytes - previousTxBytes;
        long timeElapsed = currentTime - previousTime;

        previousRxBytes = currentRxBytes;
        previousTxBytes = currentTxBytes;
        previousTime = currentTime;

        double downloadSpeed = (rxBytes / (timeElapsed / 1000.0)) / 1024.0; // KB/s
        double uploadSpeed = (txBytes / (timeElapsed / 1000.0)) / 1024.0; // KB/s

        return new NetworkInfo(downloadSpeed, uploadSpeed);
    }

    public static class NetworkInfo {
        public double downloadSpeed;
        public double uploadSpeed;

        public NetworkInfo(double downloadSpeed, double uploadSpeed) {
            this.downloadSpeed = downloadSpeed;
            this.uploadSpeed = uploadSpeed;
        }

        public String getFormattedDownloadSpeed() {
            return String.format("%.2f KB/s", downloadSpeed);
        }

        public String getFormattedUploadSpeed() {
            return String.format("%.2f KB/s", uploadSpeed);
        }
    }
}

