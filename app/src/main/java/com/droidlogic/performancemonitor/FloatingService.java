package com.droidlogic.performancemonitor;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class FloatingService extends Service {

    private static final String TAG = "FloatingService";
    private WindowManager windowManager;
    private View floatingView;
    private View identifierView;
    private TextView resultTextView;
    private Handler handler;
    private Runnable pollingRunnable;
    private SystemInfo systemInfo = new SystemInfo();
    private String previousFormattedResult = "";
    private TextView identifierTextView;
    private TextView serialTextView;
    private ConcurrentHashMap<Integer, String> thermalZoneData = new ConcurrentHashMap<>();
    public static final String ACTION_TOGGLE_INFO = "com.droidlogic.performancemonitor.TOGGLE_INFO";
    private boolean isInfoWindowVisible = true;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_layout, null);
        resultTextView = floatingView.findViewById(R.id.resultTextView);
        resultTextView.setWidth(WindowManager.LayoutParams.MATCH_PARENT);
        resultTextView.setTextSize(10);

        WindowManager.LayoutParams floatingParams;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            floatingParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL| WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    android.graphics.PixelFormat.TRANSLUCENT);
        } else {
            floatingParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL| WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    android.graphics.PixelFormat.TRANSLUCENT);
        }

        windowManager.addView(floatingView, floatingParams);

        identifierView = LayoutInflater.from(this).inflate(R.layout.identifier_layout, null);
        identifierTextView = identifierView.findViewById(R.id.identifierTextView);
        serialTextView = identifierView.findViewById(R.id.serialTextView);
        identifierTextView.setTextSize(24);
        serialTextView.setTextSize(18);

        WindowManager.LayoutParams identifierParams;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            identifierParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL| WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    android.graphics.PixelFormat.TRANSLUCENT);
        } else {
            identifierParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL| WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    android.graphics.PixelFormat.TRANSLUCENT);
        }

        identifierParams.gravity = Gravity.TOP | Gravity.LEFT;
        identifierParams.x = 0;
        identifierParams.y = 0;

        windowManager.addView(identifierView, identifierParams);
        Log.d("FloatingService", "Identifier view added with params: " + identifierParams.toString());

        startPolling();
        startDisplayRefresh();
        startIdentifierRefresh();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null) {
            windowManager.removeView(floatingView);
        }
        handler.removeCallbacks(pollingRunnable);

        Intent broadcastIntent = new Intent(this, RestartServiceReceiver.class);
        sendBroadcast(broadcastIntent);
        super.onDestroy();
    }

    private void startPolling() {
        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final String cpuUsage = getCpuUsage();
                        final String cpuUsage2 = readProcStat();
                        final NetworkUtils.NetworkInfo networkInfo = NetworkUtils.getNetworkInfo();
                        getThermalInfo(thermalinfo -> {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                updateCpuUsage(cpuUsage);
                                updateNetworkInfo(networkInfo);
                                updateThermalInfo(thermalinfo);
                            }
                        });
                        });
                    }
                }).start();
                handler.postDelayed(this, 1000); // 1 second delay
            }
        };
        handler.post(pollingRunnable);

        Runnable memInfoPollingRunnable = new Runnable() {
            @Override
            public void run() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final String memInfo = readMemInfo();
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                updateMemInfo(memInfo);
                            }
                        });
                    }
                }).start();
                    handler.postDelayed(this, 1000);
            }
        };
        handler.post(memInfoPollingRunnable);

        Runnable identifierPollingRunnable = new Runnable() {
            @Override
            public void run() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final String identifier = executeShellCommand("getprop persist.adb.bind_code");
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                updateIdentifier(identifier);
                            }
                        });
                    }
                }).start();
                handler.postDelayed(this, 60000); // 1 minute delay
            }
        };
        handler.post(identifierPollingRunnable);
    }

    private String executeShellCommand(String command) {
        StringBuilder output = new StringBuilder();
        try {
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            reader.close();
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
            output.append("Error: ").append(e.getMessage());
        }
        return output.toString();
    }

    private void executeShellCommand(String command, Callback callback) {
        new Thread(() -> {
            StringBuilder output = new StringBuilder();
            try {
                Process process = Runtime.getRuntime().exec(command);
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                reader.close();
                process.waitFor();
            } catch (Exception e) {
                output.append("Error: ").append(e.getMessage());
            }
            callback.onResult(output.toString().trim());
        }).start();
    }

    private void executeShellCommand(String command, Callback callback, Callback errorCallback) {
        new Thread(() -> {
            StringBuilder output = new StringBuilder();
            try {
                Process process = Runtime.getRuntime().exec(command);
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                reader.close();
                process.waitFor();
                callback.onResult(output.toString().trim());
            } catch (Exception e) {
                errorCallback.onResult("Error: " + e.getMessage());
            }
        }).start();
    }

    public interface Callback {
        void onResult(String result);
    }

    private String readMemInfo() {
        StringBuilder memInfo = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/meminfo"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("MemTotal:") ||
                        line.startsWith("MemFree:") ||
                        line.startsWith("MemAvailable:") ||
                        line.startsWith("Buffers:") ||
                        line.startsWith("Cached:") ||
                        line.startsWith("SwapCached:") ||
                        line.startsWith("SwapTotal:") ||
                        line.startsWith("SwapFree:")) {
                    memInfo.append(line).append("\n");
                }
            }
        } catch (IOException e) {
            return "Error reading /proc/meminfo: " + e.getMessage();
        }
        return memInfo.toString();
    }

    private String getCpuUsage() {
        StringBuilder cpuInfo = new StringBuilder();
        try {
            String output = executeShellCommand("top -n 1");
            BufferedReader reader = new BufferedReader(new InputStreamReader(new java.io.ByteArrayInputStream(output.getBytes())));
            String line;
            boolean startReading = false;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                if (line.contains("Tasks:")) {
                    int index = line.indexOf("Tasks:");
                    line = line.substring(index);
                    startReading = true;
                }
                if (startReading && lineNumber < 4) {
                    cpuInfo.append(line).append("\n");
                    lineNumber++;
                }
            }
            reader.close();
        } catch (Exception e) {
            return "Error reading CPU usage: " + e.getMessage();
        }
        return cpuInfo.toString();
    }

    private String readProcStat() {
        StringBuilder cpuInfo = new StringBuilder();
        try {
            String result = executeShellCommand("cat /proc/cpuinfo");
            BufferedReader reader = new BufferedReader(new InputStreamReader(new java.io.ByteArrayInputStream(result.getBytes())));
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null && lineNumber < 4) {
                cpuInfo.append(line).append("\n");
                lineNumber++;
            }
            reader.close();
        } catch (Exception e) {
            return "Error reading CPU usage: " + e.getMessage();
        }
        return cpuInfo.toString();
    }

    private void getThermalInfo(Callback callback) {
        StringBuilder thermalInfo = new StringBuilder();
        readThermalZone(0, thermalInfo, callback);
    }

    private void readThermalZone(int zone, StringBuilder thermalInfo, Callback callback) {
        String typeCommand = "cat /sys/class/thermal/thermal_zone" + zone + "/type";
        String tempCommand = "cat /sys/class/thermal/thermal_zone" + zone + "/temp";

        executeShellCommand(typeCommand, typeOutput -> {
            if (typeOutput != null && !typeOutput.isEmpty()) {
                executeShellCommand(tempCommand, tempOutput -> {
                    try {
                        if (tempOutput != null && !tempOutput.isEmpty()) {
                            float temperature = Float.parseFloat(tempOutput.trim()) / 1000.0f; // 假设温度以毫度为单位
                            String result = "Type: " + typeOutput.trim() + "    Temperature: " + temperature + "°C";
                            thermalInfo.append(result).append("\n");
                        } else {
                            thermalInfo.append("Temperature output is null or empty\n");
                        }
                    } catch (NumberFormatException e) {
                        thermalInfo.append("Error parsing temperature: ").append(tempOutput).append("\n");
                    }
                    // 继续读取下一个 thermal zone
                    readThermalZone(zone + 1, thermalInfo, callback);
                });
            } else {
                // 当 typeOutput 为空时，表示没有更多的 thermal zone，调用 callback
                callback.onResult(thermalInfo.toString().trim());
            }
        }, error -> {
            // 如果 typeCommand 失败，表示没有更多的 thermal zone，调用 callback
            callback.onResult(thermalInfo.toString().trim());
        });
    }

    private String getSerialNumber() {
        String cpuInfo = executeShellCommand("cat /proc/cpuinfo");
        String cpuChipIdInfo = executeShellCommand("cat /proc/cpu_chipid");

        for (String line : cpuInfo.split("\n")) {
            if (line.startsWith("Serial")) {
                return line.split(":")[1].trim();
            }
        }

        // 如果在 /proc/cpuinfo 中找不到序列号，尝试在 /proc/cpu_chipid 中查找
        for (String line : cpuChipIdInfo.split("\n")) {
            if (line.startsWith("Serial")) {
                return line.split(":")[1].trim();
            }
        }

        return "Unknown Serial";
    }

    private void updateIdentifier(String identifier) {
        identifierTextView.setText(identifier);
    }

    private void updateMemInfo(String memInfo) {
        synchronized (systemInfo) {
            systemInfo.memInfo = memInfo;
        }
    }

    private void updateCpuUsage(String cpuUsage) {
        synchronized (systemInfo) {
            systemInfo.cpuUsage = cpuUsage;
        }
    }

    private void updateNetworkInfo(NetworkUtils.NetworkInfo networkInfo) {
        synchronized (systemInfo) {
            systemInfo.updateNetworkInfo(networkInfo);
        }
    }

    private void updateThermalInfo(String info) {
        synchronized (systemInfo) {
            systemInfo.thermalinfo = info;
        }
    }

    private void updateIdentifier(String identifier, String serial) {
        identifierTextView.setText("Identifier: " + identifier);
        serialTextView.setText("Serial: " + serial);
    }

    private void refreshDisplay() {
        synchronized (systemInfo) {
            String formattedResult = formatAsTable(
                    systemInfo.memInfo,
                    systemInfo.cpuUsage,
                    String.format("%.2f KB/s", systemInfo.networkInfo.downloadSpeed),
                    String.format("%.2f KB/s", systemInfo.networkInfo.uploadSpeed),
                    systemInfo.thermalinfo
            );
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                resultTextView.setText(Html.fromHtml("<pre>" + formattedResult + "</pre>", Html.FROM_HTML_MODE_LEGACY));
            } else {
                resultTextView.setText(Html.fromHtml("<pre>" + formattedResult + "</pre>"));
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_TOGGLE_INFO.equals(intent.getAction())) {
            toggleInfoWindow();
        } else {
            startPolling();
            startDisplayRefresh();
            startIdentifierRefresh();
        }
        return START_STICKY;
    }

    private void toggleInfoWindow() {
        if (isInfoWindowVisible) {
            floatingView.setVisibility(View.GONE);
        } else {
            floatingView.setVisibility(View.VISIBLE);
        }
        isInfoWindowVisible = !isInfoWindowVisible;
    }

    private void startDisplayRefresh() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                refreshDisplay();
                handler.postDelayed(this, 1000); // 每秒刷新一次显示内容
            }
        }, 1000);
    }

    private void startIdentifierRefresh() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final String identifier = executeShellCommand("getprop persist.adb.bind_code");
                        final String serial = getSerialNumber();
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                updateIdentifier(identifier, serial);
                            }
                        });
                    }
                }).start();
                handler.postDelayed(this, 60000); // 1 minute delay
            }
        }, 60000); // Initial delay of 1 minute
    }

    private String formatAsTable(String memInfo, String cpuUsage, String downloadSpeed, String uploadSpeed, String thermalinfo) {
        StringBuilder formattedResult = new StringBuilder();
        formattedResult.append("Memory Detail:<br>").append(memInfo.replaceAll(" ", "&nbsp;")).append("<br><br>");
        formattedResult.append("CPU Usage:<br>").append(cpuUsage.replaceAll(" ", "&nbsp;")).append("<br><br>");
        formattedResult.append("Network Info:<br>")
                .append("Download Speed: ").append(downloadSpeed).append("<br>")
                .append("Upload Speed: ").append(uploadSpeed).append("<br><br>");
        formattedResult.append("Temperature: ").append(thermalinfo).append("<br><br>");
        return formattedResult.toString();
    }
}
