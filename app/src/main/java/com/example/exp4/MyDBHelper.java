package com.example.exp4;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class MyDBHelper extends SQLiteOpenHelper {
    public static final String CREATE_LIKE = "create table MyLike("
            + "_id integer primary key autoincrement,"
            + "song_id integer,"
            + "song_name text ,"
            + "artist text)";

    public MyDBHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    public boolean isLike(int songId) {
        Cursor cursor = getReadableDatabase().rawQuery("select * from MyLike where song_id=?", new String[]{songId + ""});
        boolean result = cursor.getCount() > 0;
        cursor.close();
        return result;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_LIKE);///执行数据库建表语句
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVision) {
    }
}
