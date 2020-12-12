package com.example.exp4;

import com.alibaba.fastjson.JSON;

import java.util.ArrayList;
import java.util.Objects;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/*用于获取下载URL的线程类*/
class UrlThread implements Runnable {
    private final int songId;
    private String url;

    public UrlThread(int songId) {
        this.songId = songId;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public void run() {
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url("https://autumnfish.cn/song/url?id=" + songId)
                    .build();
            Response response = client.newCall(request).execute();
            String responseData = Objects.requireNonNull(response.body()).string();
            SongURL songURL = JSON.parseObject(responseData, SongURL.class);
            ArrayList<SongURL.Data> dataArrayList = songURL.getData();
            url = dataArrayList.get(0).getUrl();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}