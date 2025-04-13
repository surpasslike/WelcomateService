package com.surpasslike.welcomateservice;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.IBinder;
import android.util.Log;

import com.surpasslike.welcomateservice.admin.AdminService;

public class UserAIDLImpl extends IAdminService.Stub {
    private final String TAG = "UserAIDLImpl";
    private final DatabaseHelper dbHelper;

    public UserAIDLImpl() {
        this.dbHelper = new DatabaseHelper(MyApplication.getContext());
    }

    // 验证用户登录
    @Override
    public String loginAdmin(String account, String password) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selection = DatabaseHelper.COLUMN_ACCOUNT + " = ? AND " + DatabaseHelper.COLUMN_PASSWORD + " = ?";
        String[] selectionArgs = {account, password};

        Cursor cursor = db.query(DatabaseHelper.TABLE_USERS, null, selection, selectionArgs, null, null, null);

        if (cursor.moveToFirst()) {
            // 登录成功返回用户名
            @SuppressLint("Range") String username = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_USERNAME));
            cursor.close();
            return username;
        } else {
            // 登录失败返回空
            cursor.close();
            return null;
        }
    }

    @Override
    public boolean registerUser(String username, String account, String password) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_USERNAME, username);
        values.put(DatabaseHelper.COLUMN_ACCOUNT, account);
        values.put(DatabaseHelper.COLUMN_PASSWORD, password);

        long rowId = db.insert(DatabaseHelper.TABLE_USERS, null, values);
        return rowId != -1; // 注册成功返回true，注册失败返回false
    }

    @Override
    // 删除指定用户的方法
    public void deleteUser(String username) {
        Log.d(TAG, "deleteUser username = " + username);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String selection = DatabaseHelper.COLUMN_USERNAME + " = ?";
        String[] selectionArgs = {username};

        // 删除指定用户名的用户
        db.delete(DatabaseHelper.TABLE_USERS, selection, selectionArgs);
    }

    // 更新指定用户的密码
    public void updateUserPassword(String username, String newPassword) {
        Log.d(TAG, "updateUserPassword username = " + username);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_PASSWORD, newPassword);
        String selection = DatabaseHelper.COLUMN_USERNAME + " = ?";
        String[] selectionArgs = {username};
        db.update(DatabaseHelper.TABLE_USERS, values, selection, selectionArgs);
    }

    // 修改指定用户的密码
    private AdminService adminService;

    public void changeUserPassword(String username, String newPassword) {
        adminService.updateUserPassword(username, newPassword);
    }

    public IBinder onBind(Intent intent) {
        Log.d(TAG, "UserAIDLImpl onBind");
        return new UserAIDLImpl();
    }
}
