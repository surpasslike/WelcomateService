package com.surpasslike.welcomateservice.ui.admin;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Base64;
import android.util.Log;

import androidx.lifecycle.ViewModel;

import com.surpasslike.welcomateservice.MyApplication;
import com.surpasslike.welcomateservice.data.db.DatabaseHelper;
import com.surpasslike.welcomateservice.data.model.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin UI 的 ViewModel
 * 负责为 UI 提供数据，并处理用户的交互逻辑
 * 注意：在更大型的应用中，ViewModel 通常会通过一个 Repository 层来与数据源交互，
 * 而不是直接操作 DatabaseHelper
 */
public class AdminViewModel extends ViewModel {
    private static final String TAG = "AdminViewModel";
    private final DatabaseHelper dbHelper;

    /**
     * 构造函数
     * 初始化数据库帮助类
     */
    public AdminViewModel() {
        this.dbHelper = new DatabaseHelper(MyApplication.getContext());
    }

    /**
     * 对给定的密码字符串进行 SHA-256 哈希处理
     *
     * @param password 要哈希的原始密码
     * @return 哈希后并经过 Base64 编码的字符串如果算法不可用，则返回 null
     */
    private String hashPassword(String password) {
        if (password == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(hash, Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "SHA-256 algorithm not found", e);
            return null;
        }
    }

    /**
     * 验证管理员登录
     *
     * @param account  用户输入的账户
     * @param password 用户输入的原始密码
     * @return 如果登录成功，返回 true；否则返回 false
     */
    public boolean loginAdmin(String account, String password) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selection = DatabaseHelper.COLUMN_ACCOUNT + " = ?";
        String[] selectionArgs = {account};

        try (Cursor cursor = db.query(DatabaseHelper.TABLE_USERS, null, selection, selectionArgs, null, null, null)) {
            if (cursor.moveToFirst()) {
                @SuppressLint("Range") String storedPasswordHash = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_PASSWORD));
                String inputPasswordHash = hashPassword(password);
                return inputPasswordHash != null && inputPasswordHash.equals(storedPasswordHash);
            }
        }
        return false;
    }

    /**
     * 添加一个新用户
     *
     * @param username 用户名
     * @param account  账户
     * @param password 原始密码，将被哈希后存储
     * @return 新插入行的行 ID，如果发生错误则为 -1
     */
    public long addUser(String username, String account, String password) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_USERNAME, username);
        values.put(DatabaseHelper.COLUMN_ACCOUNT, account);

        String hashedPassword = hashPassword(password);
        if (hashedPassword == null) {
            return -1; // 哈希失败
        }
        values.put(DatabaseHelper.COLUMN_PASSWORD, hashedPassword);

        return db.insert(DatabaseHelper.TABLE_USERS, null, values);
    }

    /**
     * 获取所有用户的列表
     *
     * @return 包含所有用户的 List
     */
    public List<User> getAllUsers() {
        List<User> userList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(DatabaseHelper.TABLE_USERS, null, null, null, null, null, null)) {
            if (cursor.moveToFirst()) {
                do {
                    @SuppressLint("Range") String username = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_USERNAME));
                    @SuppressLint("Range") String account = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_ACCOUNT));
                    // 为了安全，不将密码哈希传递给 User 对象
                    userList.add(new User(username, account, null));
                } while (cursor.moveToNext());
            }
        }
        return userList;
    }

    /**
     * 根据用户名删除一个用户
     *
     * @param username 要删除的用户的用户名
     */
    public void deleteUser(String username) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String selection = DatabaseHelper.COLUMN_USERNAME + " = ?";
        String[] selectionArgs = {username};
        db.delete(DatabaseHelper.TABLE_USERS, selection, selectionArgs);
    }

    /**
     * 更新指定用户的密码
     *
     * @param username    要更新密码的用户的用户名
     * @param newPassword 新的原始密码
     */
    public void changeUserPassword(String username, String newPassword) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        String hashedPassword = hashPassword(newPassword);
        if (hashedPassword == null) {
            Log.e(TAG, "Password hashing failed, update aborted.");
            return;
        }
        values.put(DatabaseHelper.COLUMN_PASSWORD, hashedPassword);

        String selection = DatabaseHelper.COLUMN_USERNAME + " = ?";
        String[] selectionArgs = {username};
        db.update(DatabaseHelper.TABLE_USERS, values, selection, selectionArgs);
    }
}
