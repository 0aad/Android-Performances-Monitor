package com.droidlogic.performancemonitor;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpProgressMonitor;

import java.io.File;

public class SftpUploader {

    public interface UploadCallbacks {
        void onProgressUpdate(int percentage);
        void onError(String errorMessage);
        void onFinish();
    }

    public static void uploadFile(String localFile, String remoteFile, String remoteHost, String username, String password, int port, UploadCallbacks callbacks) {
        Session session = null;
        ChannelSftp channelSftp = null;

        try {
            JSch jsch = new JSch();
            session = jsch.getSession(username, remoteHost, port);
            session.setPassword(password);

            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            session.connect();

            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            File local = new File(localFile);
            long fileSize = local.length();

            channelSftp.put(localFile, remoteFile, new SftpProgressMonitor() {
                long totalTransferred = 0;

                @Override
                public void init(int op, String src, String dest, long max) {
                    // 初始化
                }

                @Override
                public boolean count(long count) {
                    totalTransferred += count;
                    int progress = (int) ((totalTransferred * 100) / fileSize);
                    if (callbacks != null) {
                        callbacks.onProgressUpdate(progress);
                    }
                    return true;
                }

                @Override
                public void end() {
                    // 上传完成
                    if (callbacks != null) {
                        callbacks.onFinish();
                    }
                }
            });

            System.out.println("File uploaded successfully - " + localFile + " to " + remoteFile);

        } catch (Exception ex) {
            if (callbacks != null) {
                callbacks.onError(ex.getMessage());
            }
            ex.printStackTrace();
        } finally {
            if (channelSftp != null) {
                channelSftp.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
    }
}

