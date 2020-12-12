package com.example.exp4;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LikeActivity extends AppCompatActivity {
    ListView lv_like;
    ImageButton iv_back2;
    MyDBHelper dbHelper;
    private final ArrayList<Integer> _ids = new ArrayList<>();
    private final ArrayList<Integer> songIds = new ArrayList<>();
    private final ArrayList<String> songNames = new ArrayList<>();
    private final ArrayList<String> artists = new ArrayList<>();
    private String songName, artist;
    private int songId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_like);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        iv_back2 = findViewById(R.id.ib_back2);
        iv_back2.setOnClickListener(v -> LikeActivity.this.finish());
        dbHelper = new MyDBHelper(this, "LIKE.db", null, 1);

        InitData();//装填数据

        lv_like = findViewById(R.id.lv_like);
        List<Map<String, String>> listItems = new ArrayList<>();
        for (int i = 0; i < songNames.size(); i++) {
            //实例化Map对象
            Map<String, String> map = new HashMap<>();
            map.put("songName", songNames.get(i));
            map.put("artist", artists.get(i));
            //将map对象添加到List集合
            listItems.add(map);
        }
        SimpleAdapter adapter = new SimpleAdapter(LikeActivity.this, listItems,
                android.R.layout.simple_list_item_2, new String[]{"songName", "artist"},
                new int[]{android.R.id.text1, android.R.id.text2});
        lv_like.setAdapter(adapter);

        //点击item事件
        lv_like.setOnItemClickListener((parent, view, position, id) -> {

            songId = songIds.get(position);
            songName = songNames.get(position);//得到song
            artist = artists.get(position);

            Intent intent = new Intent(LikeActivity.this, PlayActivity.class);
            intent.putExtra("thisSongName", songName);
            intent.putExtra("thisArtist", artist);
            intent.putExtra("songId", songId);
            intent.putExtra("position", position);
            intent.putExtra("as_list", songNames);
            intent.putExtra("id_list", songIds);

            startActivity(intent);

        });
    }

    /*装填数据方法，先将列表内容清空，再用cursor游标循环获取数据并add到列表中*/
    public void InitData() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("MyLike", null, null, null,
                null, null, null);
        _ids.clear();
        songIds.clear();
        songNames.clear();
        artists.clear();

        while (cursor.moveToNext()) {

            int _id = cursor.getInt(cursor.getColumnIndex("_id"));
            int songId = cursor.getInt(cursor.getColumnIndex("song_id"));
            songName = cursor.getString(cursor.getColumnIndex("song_name"));
            artist = cursor.getString(cursor.getColumnIndex("artist"));

            _ids.add(_id);
            songIds.add(songId);
            songNames.add(songName);
            artists.add(artist);
        }

        cursor.close();
    }

}
