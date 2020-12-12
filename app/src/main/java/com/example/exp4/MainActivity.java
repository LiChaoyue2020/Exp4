package com.example.exp4;


import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.IBinder;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSON;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Vector;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    ListView lv;
    EditText et_search;
    ImageButton btn_search, btn_like;
    List<String> url_list = new Vector<>();

    ArrayList<String> song_list = new ArrayList<>();
    ArrayList<String> artist_list = new ArrayList<>();
    ArrayList<Integer> id_list = new ArrayList<>();

    private String url1, song1, artist1;
    private DownloadService.DownloadBinder downloadBinder;//服务与活动间的通信
    private final ServiceConnection connection = new ServiceConnection() {//ServiceConnection匿名类，
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            downloadBinder = (DownloadService.DownloadBinder) service;//获取downloadBinder实例，用于在活动中调用服务提供的各种方法
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        androidx.appcompat.app.ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        btn_like = findViewById(R.id.btn_like);
        btn_like.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LikeActivity.class);
            startActivity(intent);
        });
        btn_search = findViewById(R.id.btn_search);
        btn_search.setOnClickListener(v -> {
            String search = et_search.getText().toString();
            sendRequestWithOkHttp_search(search);
        });
        et_search = findViewById(R.id.et_search);
        et_search.setSingleLine(true);
        et_search.setText("陈奕迅");
        Intent intent1 = new Intent(MainActivity.this, DownloadService.class);
        startService(intent1);//启动服务
        Log.d("Main", "已启动服务");
        bindService(intent1, connection, BIND_AUTO_CREATE);//绑定服务
        Log.d("Main", "已绑定服务");
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
        sendRequestWithOkHttp_search("陈奕迅");
    }

    private void sendRequestWithOkHttp_search(String keywords) {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url("https://autumnfish.cn/search?keywords=" + keywords)
                        .build();
                Response response = client.newCall(request).execute();
                String responseData = Objects.requireNonNull(response.body()).string();
                Log.d("data is", responseData);
                SearchResult sr = JSON.parseObject(responseData, SearchResult.class);
                SearchResult.Result result = sr.getResult();
                ArrayList<SearchResult.Result.Song> songs = result.getSongs();
                url_list.clear();
                id_list.clear();
                song_list.clear();
                artist_list.clear();
                for (SearchResult.Result.Song song : songs) {
                    id_list.add(song.getId());
                    song_list.add(song.getName());
                    artist_list.add(song.getArtists().get(0).getName());
                }
                showResponse();
                //   sendRequestWithOkHttp_url2();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

    }

    /*通过ID获得URL*/
    private String getUrlById(int id) throws InterruptedException {
        UrlThread urlThread = new UrlThread(id);
        Thread thread = new Thread(urlThread);
        thread.start();
        thread.join();
        return urlThread.getUrl();
    }


    //显示在界面上
    private void showResponse() {
        runOnUiThread(() -> {
            lv = findViewById(R.id.lv);
//                ArrayAdapter<String> adapter = new ArrayAdapter<String>(
//                        MainActivity.this, android.R.layout.simple_list_item_2, song_list);
            List<Map<String, String>> listItems = new ArrayList<>();
            for (int i = 0; i < song_list.size(); i++) {
                //实例化Map对象
                Map<String, String> map = new HashMap<>();
                map.put("songName", song_list.get(i));
                map.put("artist", artist_list.get(i));
                //将map对象添加到List集合
                listItems.add(map);
            }
            SimpleAdapter adapter = new SimpleAdapter(MainActivity.this, listItems,
                    android.R.layout.simple_list_item_2, new String[]{"songName", "artist"},
                    new int[]{android.R.id.text1, android.R.id.text2});
            lv.setAdapter(adapter);

            //点击item事件
            lv.setOnItemClickListener((parent, view, position, id) -> {
                int songId = id_list.get(position);
                try {
                    url1 = getUrlById(songId);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
                if (url1 == null || url1.equals(""))
                    Toast.makeText(MainActivity.this, "URL获取失败", Toast.LENGTH_SHORT).show();
                String name = songId + ".mp3";
                song1 = song_list.get(position);//得到song
                artist1 = artist_list.get(position);
                Log.d("MainActivity:", "url is " + url1 + "\nname is " + name);

                //如果歌曲不存在，则先下载，如果存在，则直接跳转
                if (downloadBinder == null) {
                    Log.d("Main", "未绑定");
                    return;
                }
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), name);
//                        File file = new File(getExternalFilesDir(null), name);
                Log.d("Main", file.getAbsolutePath());
                if (!file.exists()) {
                    downloadBinder.startDownload(url1, name);//若音乐文件不存在，则进行下载
                }

                Intent intent = new Intent(MainActivity.this, PlayActivity.class);
                intent.putExtra("thisUrl", url1);
                intent.putExtra("thisSongName", song1);
                intent.putExtra("thisArtist", artist1);
                intent.putExtra("songId", songId);
                intent.putExtra("position", position);
                intent.putExtra("url_list", (Serializable) url_list);
                intent.putExtra("as_list", song_list);
                intent.putExtra("id_list", id_list);

                startActivity(intent);

            });
        });
    }
}

