package com.surpasslike.welcomateservice.admin;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;

import com.surpasslike.welcomateservice.DatabaseHelper;
import com.surpasslike.welcomateservice.User;
import com.surpasslike.welcomateservice.UserAIDLImpl;

public class AdminService extends Service {
    private final String TAG = "AdminService";

    public AdminService() {
        // 这是默认的无参数构造函数
    }

    private DatabaseHelper dbHelper;

    public AdminService(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    // 验证用户登录
    public boolean loginAdmin(String account, String password) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selection = DatabaseHelper.COLUMN_ACCOUNT + " = ? AND " + DatabaseHelper.COLUMN_PASSWORD + " = ?";
        String[] selectionArgs = {account, password};

        Cursor cursor = db.query(DatabaseHelper.TABLE_USERS, null, selection, selectionArgs, null, null, null);

        boolean loginSuccess = cursor.moveToFirst();
        cursor.close();
        return loginSuccess;
    }

    // 获取所有用户信息的方法
    public List<User> getAllUsers() {
        List<User> userList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_USERS, null, null, null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                @SuppressLint("Range") String username = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_USERNAME));
                @SuppressLint("Range") String account = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_ACCOUNT));
                @SuppressLint("Range") String password = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_PASSWORD));
                User user = new User(username, account, password);
                userList.add(user);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return userList;
    }

    // 添加新用户
    public long addUser(String username, String account, String password) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_USERNAME, username);
        values.put(DatabaseHelper.COLUMN_ACCOUNT, account);
        values.put(DatabaseHelper.COLUMN_PASSWORD, password);

        // 插入新用户数据并返回插入的行ID
        return db.insert(DatabaseHelper.TABLE_USERS, null, values);
    }

    // 删除指定用户的方法
    public void deleteUser(String username) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String selection = DatabaseHelper.COLUMN_USERNAME + " = ?";
        String[] selectionArgs = {username};

        Log.d(TAG, "deleteUser username = " + username);
        // 删除指定用户名的用户
        db.delete(DatabaseHelper.TABLE_USERS, selection, selectionArgs);
    }

    // 更新指定用户的密码
    public void updateUserPassword(String username, String newPassword) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_PASSWORD, newPassword);

        String selection = DatabaseHelper.COLUMN_USERNAME + " = ?";
        String[] selectionArgs = {username};

        db.update(DatabaseHelper.TABLE_USERS, values, selection, selectionArgs);
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        System.out.println("服务被创建");
        Log.d(TAG, "AdminService onBind");
        return new UserAIDLImpl();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "AdminService onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
