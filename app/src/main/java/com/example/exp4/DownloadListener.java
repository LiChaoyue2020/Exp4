package com.example.exp4;

public interface DownloadListener {//用于对下载过程中的各种状态进行监听和回调

    void onProgress(int Progress);

    void onSuccess();

    void onFailed();

    void onPaused();

    void onCanceled();
}
