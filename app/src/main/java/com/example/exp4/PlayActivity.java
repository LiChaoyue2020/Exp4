package com.example.exp4;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.os.Environment;
import android.os.IBinder;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class PlayActivity extends AppCompatActivity implements View.OnClickListener {
    TextView tv_song_title, tv_end, tv_begin, tv_song_name, tv_artist;
    ImageButton ib_previous, ib_play, ib_next, ib_back, ib_like, ib_download;
    SeekBar sb_progress;
    ImageView iv_disk;
    private String thisUrl;
    private String thisSongName;
    private String thisArtist;
    private String fileName;
    private int position;
    private int songId;
    ArrayList<Integer> id_list = new ArrayList<>();
    ArrayList<String> song_list = new ArrayList<>();
    private final MediaPlayer mediaPlayer = new MediaPlayer();//实例化MediaPlayer
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

    /*与进度条有关*/
    Timer timer;
    TimerTask timerTask;
    Boolean isChanged = false;
    private long currentPosition = 0;

    private MyDBHelper dbHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
        androidx.appcompat.app.ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        dbHelper = new MyDBHelper(this, "LIKE.db", null, 1);
        //绑定监听事件的id
        tv_song_title = findViewById(R.id.tv_song);
        tv_song_name = findViewById(R.id.song_name);
        tv_artist = findViewById(R.id.artist);
        tv_end = findViewById(R.id.tv_end);
        tv_begin = findViewById(R.id.tv_begin);
        ib_previous = findViewById(R.id.ib_previous);
        ib_play = findViewById(R.id.ib_play);
        ib_play.setBackgroundResource(R.drawable.play);
        ib_next = findViewById(R.id.ib_next);
        ib_back = findViewById(R.id.ib_back);
        ib_like = findViewById(R.id.ib_like);
        ib_like.setBackgroundResource(R.drawable.heart_empty);

        sb_progress = findViewById(R.id.sb_progress);
        ib_download = findViewById(R.id.ib_load);
        iv_disk = findViewById(R.id.iv_disk);

        //设置点击事件
        ib_previous.setOnClickListener(this);
        ib_play.setOnClickListener(this);
        ib_next.setOnClickListener(this);
        ib_back.setOnClickListener(this);
        ib_like.setOnClickListener(this);
        ib_download.setOnClickListener(this);
        //seekebar的监听事件
        sb_progress.setOnSeekBarChangeListener(new MySeekBar());

        //从Intent得到歌曲基本信息
        Intent intent = getIntent();
        thisUrl = intent.getStringExtra("thisUrl");//得到下载链接
        thisSongName = intent.getStringExtra("thisSongName");//设置播放界面的音乐名
        thisArtist = intent.getStringExtra("thisArtist");
        songId = intent.getIntExtra("songId", 0);
        fileName = songId + ".mp3";
        position = intent.getIntExtra("position", 0);
        song_list = intent.getStringArrayListExtra("as_list");
        id_list = intent.getIntegerArrayListExtra("id_list");

        //打印验证
        Log.e("PlayActivity", "url is " + thisUrl);
        Log.e("PlayActivity", "as is " + thisSongName);
        Log.e("PlayActivity", "name is " + fileName);
        InitData();

        Intent intent1 = new Intent(PlayActivity.this, DownloadService.class);
        startService(intent1);//启动服务
        bindService(intent1, connection, BIND_AUTO_CREATE);//绑定服务
        if (ContextCompat.checkSelfPermission(PlayActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(PlayActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        } else {
            initMediaPlayer();//初始化MediaPlayer
        }

        /*时间处理线程*/
        tv_end.setText(formatTime(mediaPlayer.getDuration()));//总时间
        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                if (isChanged) {
                    return;
                }
                currentPosition = mediaPlayer.getCurrentPosition();
                sb_progress.setProgress(mediaPlayer.getCurrentPosition());//设置进度
                showCurrentTime();
            }
        };
        timer.schedule(timerTask, 0, 10);
    }

    @Override
    public void onClick(View v) {
        int id = 0;
        if (downloadBinder == null) {
            return;
        }
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);//当前歌曲路径

        switch (v.getId()) {
            case R.id.ib_back:
                PlayActivity.this.finish();
                break;
            case R.id.ib_like:
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues values = new ContentValues();
                Cursor cursor = db.rawQuery("select * from MyLike where song_id=?", new String[]{songId + ""});////查询关注数据库中是否存在当前歌曲
                if (!cursor.moveToNext()) {//当前歌曲不存在关注列表中,则将歌曲信息加入到关注列表中，设置图标变为红色
                    values.put("song_id", songId);
                    values.put("song_name", thisSongName);
                    values.put("artist", thisArtist);
                    db.insert("MyLike", null, values);

                    ib_like.setBackgroundResource(R.drawable.heart_full);
                    Toast.makeText(PlayActivity.this, "收藏成功😀", Toast.LENGTH_SHORT).show();

                } else {///当前歌曲存在关注列表中,
                    db.delete("MyLike", "song_id=?", new String[]{songId + ""});
                    db.close();
                    ib_like.setBackgroundResource(R.drawable.heart_empty);
                    Toast.makeText(PlayActivity.this, "取消收藏😐", Toast.LENGTH_SHORT).show();
                }
                cursor.close();
                break;
            case R.id.ib_load:
                if (!file.exists()) {
                    downloadBinder.startDownload(thisUrl, fileName);
                } else {
                    Toast.makeText(PlayActivity.this, "该歌曲已经下载！", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.ib_play:
                if (file.exists()) {//文件存在，则可以播放
                    if (!mediaPlayer.isPlaying()) {
                        initMediaPlayer();//刷新界面
                        mediaPlayer.start();//开始播放
                        ib_play.setBackgroundResource(R.drawable.pause);
                        Toast.makeText(PlayActivity.this, "开始播放😄", Toast.LENGTH_SHORT).show();
                    } else {
                        mediaPlayer.pause();//暂停播放
                        ib_play.setBackgroundResource(R.drawable.play);

                        Toast.makeText(PlayActivity.this, "暂停播放😅", Toast.LENGTH_SHORT).show();
                    }
                } else {//文件不存在，则提示
                    Toast.makeText(PlayActivity.this, "该歌曲未下载😥", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.ib_previous:
                mediaPlayer.reset();//音乐重置
                ib_play.setBackgroundResource(R.drawable.play);
                //得到音乐文件名
                try {
                    if (position == 0) {
                        position = song_list.size() - 1;
                        thisSongName = song_list.get(position);
                        id = id_list.get(position);
                        thisUrl = getUrlById(id);
                        Log.d("PPP1", position + ":" + thisSongName + ":" + id + ":" + thisUrl);

                    } else {
                        position--;
                        thisSongName = song_list.get(position);
                        id = id_list.get(position);
                        thisUrl = getUrlById(id);
                        Log.d("PPP2", position + ":" + thisSongName + ":" + id + ":" + thisUrl);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
                fileName = id + ".mp3";
                InitData();//修改UI界面的信息
                initMediaPlayer();//初始化MediaPlayer
                break;
            case R.id.ib_next:
                mediaPlayer.reset();
                ib_play.setBackgroundResource(R.drawable.play);
                try {
                    if (position == song_list.size() - 1) {
                        position = 0;
                        thisSongName = song_list.get(position);
                        id = id_list.get(position);
                        thisUrl = getUrlById(id);
                        Log.d("PPP3", position + ":" + thisSongName + ":" + id + ":" + thisUrl);
                    } else {
                        position++;
                        thisSongName = song_list.get(position);
                        id = id_list.get(position);
                        thisUrl = getUrlById(id);
                        Log.d("PP4", position + ":" + thisSongName + ":" + id + ":" + thisUrl);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
                fileName = id + ".mp3";
                InitData();
                initMediaPlayer();//初始化MediaPlayer
                break;
        }
    }

    //设置UI界面的显示数据
    private void InitData() {
        runOnUiThread(() -> {
            String s = thisSongName + "-" + thisArtist;
            tv_song_title.setText(s);
            tv_song_name.setText(thisSongName);
            tv_artist.setText(thisArtist);
            if (dbHelper.isLike(songId))
                ib_like.setBackgroundResource(R.drawable.heart_full);
            else
                ib_like.setBackgroundResource(R.drawable.heart_empty);
            String beginTime = "00:00";
            tv_begin.setText(beginTime);
        });
    }

    //初始化音乐播放器
    private void initMediaPlayer() {
        try {
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
            mediaPlayer.setDataSource(file.getPath());//指定音频文件的路径
            mediaPlayer.prepare();//让mediaPlayer进入到准备状态
            sb_progress.setMax(mediaPlayer.getDuration());//设置进度条的最大值
            tv_end.setText(formatTime(mediaPlayer.getDuration()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*根据ID获得URL*/
    private String getUrlById(int id) throws InterruptedException {
        UrlThread urlThread = new UrlThread(id);
        Thread thread = new Thread(urlThread);
        thread.start();
        thread.join();
        return urlThread.getUrl();
    }

    //更新播放的时间
    private void showCurrentTime() {
        runOnUiThread(() -> tv_begin.setText(formatTime(currentPosition)));
    }

    //时间转换方法，将得到的音乐时间毫秒转换为时分秒格式
    private String formatTime(long length) {
        Date date = new Date(length);
        SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");
        return sdf.format(date);
    }

    /*进度条类*/
    class MySeekBar implements SeekBar.OnSeekBarChangeListener {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            isChanged = true;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            mediaPlayer.seekTo(seekBar.getProgress());
            isChanged = false;
        }
    }

    //权限申请
    @Override
    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions, @NotNull int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(PlayActivity.this, "拒绝权限将无法使用程序", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                initMediaPlayer();
            }
        }
    }

    //若活动销毁则对服务进行解绑
    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(connection);

        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.reset();
            mediaPlayer.release();
        }

    }
}

