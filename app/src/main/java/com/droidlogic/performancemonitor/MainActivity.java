package com.droidlogic.performancemonitor;


import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.os.Environment;
import java.io.File;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;


public class MainActivity extends Activity implements SftpUploader.UploadCallbacks {

    private static final int REQUEST_CODE = 1;
    private static final int REQUEST_CODE_STORAGE_PERMISSION =101;
    private Button executeButton;
    private Button uploadButton;
    private static final String TAG = "ShizukuDebug";
    private ProgressDialog progressDialog;// 5分钟
    private Handler handler;
    private Runnable checkForFileTask;
    private static final String localPath = "/storage/emulated/0/Recordings/";
    private Button toggleInfoButton;
    private boolean isInfoWindowVisible = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        executeButton = findViewById(R.id.button_run_command);
        uploadButton = findViewById(R.id.button_upload);
        toggleInfoButton = findViewById(R.id.button_toggle_info);

        executeButton.setText("Start");

        executeButton.setOnClickListener(v -> {
            if (Utils.isServiceRunning(MainActivity.this, FloatingService.class)) {
                startFloatingService();
            }
        });

        uploadButton.setOnClickListener(v -> startUploadService());

        toggleInfoButton.setOnClickListener(v -> toggleInfoWindow());

        // 请求存储权限
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_STORAGE_PERMISSION);
        }

        // 设置定时任务
        handler = new Handler();
        checkForFileTask = new Runnable() {
            @Override
            public void run() {
                checkAndUploadFile();
                handler.postDelayed(this, 1 * 60 * 1000); // 每分钟检查一次
            }
        };

        if (!Utils.isServiceRunning(MainActivity.this, FloatingService.class)) {
            startFloatingService();
        }
    }

    private void checkAndUploadFile() {
        if (localPath != null) {
            File directory = new File(localPath);
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().endsWith(".ts")) {
                        // 找到目标文件，开始上传
                        initializeUploaderAndUpload(file.getAbsolutePath());
                        break;
                    }
                }
            }
        }
    }

    private void initializeUploaderAndUpload(String videoPath) {
        if (videoPath == null) {
            Log.e(TAG, "Video path is null");
            return;
        }

        File file = new File(videoPath);
        if (!file.exists()) {
            Log.e(TAG, "File does not exist: " + videoPath);
            return;
        }

        progressDialog = new ProgressDialog(this);
        progressDialog.setMax(100);
        progressDialog.setMessage("Uploading...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);
        progressDialog.show();

        new UploadTask(videoPath, this).execute();
    }

    @Override
    public void onProgressUpdate(final int percentage) {
        runOnUiThread(() -> progressDialog.setProgress(percentage));
    }

    @Override
    public void onError(final String errorMessage) {
        runOnUiThread(() -> {
            progressDialog.dismiss();
            Toast.makeText(MainActivity.this, "Upload error: " + errorMessage, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onFinish() {
        runOnUiThread(() -> {
            progressDialog.dismiss();
            Toast.makeText(MainActivity.this, "Upload complete", Toast.LENGTH_SHORT).show();
        });
    }

    // AsyncTask 用于在后台线程中进行分片上传
    private static class UploadTask extends AsyncTask<Void, Integer, String> {

        private final String videoPath;
        private final SftpUploader.UploadCallbacks callbacks;

        UploadTask(String videoPath, SftpUploader.UploadCallbacks callbacks) {
            this.videoPath = videoPath;
            this.callbacks = callbacks;
        }

        @Override
        protected String doInBackground(Void... voids) {
            String remoteHost = "10.18.19.252"; // Replace with your server host
            String username = "amlogic"; // Replace with your username
            String password = "se!23456"; // Replace with your password
            int port = 22; // Replace with your server port

            // Use the original file name for the remote file
            File localFile = new File(videoPath);
            String remoteFile = "/var/www/html/video_analyze/" + localFile.getName(); // Use the original file name

            SftpUploader.uploadFile(videoPath, remoteFile, remoteHost, username, password, port, new SftpUploader.UploadCallbacks() {
                @Override
                public void onProgressUpdate(int percentage) {
                    publishProgress(percentage);
                }

                @Override
                public void onError(String errorMessage) {
                    callbacks.onError(errorMessage);
                }

                @Override
                public void onFinish() {
                    // 上传成功后删除本地文件
                    File file = new File(videoPath);
                    if (file.exists()) {
                        if (file.delete()) {
                            Log.d("UploadTask", "File deleted successfully: " + videoPath);
                        } else {
                            Log.e("UploadTask", "Failed to delete file: " + videoPath);
                        }
                    }
                    callbacks.onFinish();
                }
            });

            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            callbacks.onProgressUpdate(values[0]);
        }
    }

    private void startFloatingService() {
        if (Settings.canDrawOverlays(this)) {
            if (!Utils.isServiceRunning(MainActivity.this, FloatingService.class)) {
                startService(new Intent(MainActivity.this, FloatingService.class));
                Toast.makeText(this, "Floating service started", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Floating service is already running", Toast.LENGTH_SHORT).show();
            }
        } else {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            startActivityForResult(intent, REQUEST_CODE);
        }
    }

    private void toggleInfoWindow() {
        Intent intent = new Intent(this, FloatingService.class);
        intent.setAction(FloatingService.ACTION_TOGGLE_INFO);
        startService(intent);
    }

    private void startUploadService() {
        handler.post(checkForFileTask);
        Toast.makeText(this, "视频上传服务已启动", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            if (Settings.canDrawOverlays(this)) {
                startService(new Intent(MainActivity.this, FloatingService.class));
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
                Toast.makeText(this, "存储权限已授予", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "存储权限被拒绝", Toast.LENGTH_SHORT).show();
            }
        }
    }
}