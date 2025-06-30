package com.surpasslike.welcomateservice.aidl;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Base64;
import android.util.Log;

import com.surpasslike.welcomateservice.IAdminService;
import com.surpasslike.welcomateservice.MyApplication;
import com.surpasslike.welcomateservice.data.db.DatabaseHelper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * AIDL 接口 IAdminService 的具体实现
 * 此类处理所有客户端通过 AIDL 请求的业务逻辑，包括用户认证和管理
 * 注意：此类中的方法将在 Binder 线程池中执行
 */
public class AdminApiImpl extends IAdminService.Stub {
    private static final String TAG = "AdminApiImpl";
    private final DatabaseHelper dbHelper;

    /**
     * 构造函数，初始化数据库帮助类
     */
    public AdminApiImpl() {
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
     * @return 如果登录成功，返回用户名；否则返回 null
     */
    @Override
    public String loginAdmin(String account, String password) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selection = DatabaseHelper.COLUMN_ACCOUNT + " = ?";
        String[] selectionArgs = {account};

        Cursor cursor = db.query(DatabaseHelper.TABLE_USERS, null, selection, selectionArgs, null, null, null);

        if (cursor.moveToFirst()) {
            @SuppressLint("Range") String storedPasswordHash = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_PASSWORD));
            String inputPasswordHash = hashPassword(password);

            if (inputPasswordHash != null && inputPasswordHash.equals(storedPasswordHash)) {
                // 密码哈希匹配，登录成功
                @SuppressLint("Range") String username = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_USERNAME));
                cursor.close();
                return username;
            }
        }
        // 账户不���在或密码不匹配
        cursor.close();
        return null;
    }

    /**
     * 注册一个新用户
     *
     * @param username 用户名
     * @param account  账户
     * @param password 原始密码，将被哈希后存储
     * @return 如果注册成功，返回 true；否则返回 false
     */
    @Override
    public boolean registerUser(String username, String account, String password) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_USERNAME, username);
        values.put(DatabaseHelper.COLUMN_ACCOUNT, account);

        String hashedPassword = hashPassword(password);
        if (hashedPassword == null) {
            return false; // 哈希失败，无法注册
        }
        values.put(DatabaseHelper.COLUMN_PASSWORD, hashedPassword);

        long rowId = db.insert(DatabaseHelper.TABLE_USERS, null, values);
        return rowId != -1;
    }

    /**
     * 根据用户名删除一个用户
     *
     * @param username 要删除的用户的用户名
     */
    @Override
    public void deleteUser(String username) {
        Log.d(TAG, "Deleting user: " + username);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String selection = DatabaseHelper.COLUMN_USERNAME + " = ?";
        String[] selectionArgs = {username};
        db.delete(DatabaseHelper.TABLE_USERS, selection, selectionArgs);
    }

    /**
     * 更新指定用户的密码
     *
     * @param username    要更新密码的用户的用户名
     * @param newPassword 新的原始密码，将被哈希后存储
     */
    @Override
    public void updateUserPassword(String username, String newPassword) {
        Log.d(TAG, "Updating password for user: " + username);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        String hashedPassword = hashPassword(newPassword);
        if (hashedPassword == null) {
            Log.e(TAG, "Password hashing failed, update aborted.");
            return; // 哈希失败，取消更新
        }
        values.put(DatabaseHelper.COLUMN_PASSWORD, hashedPassword);

        String selection = DatabaseHelper.COLUMN_USERNAME + " = ?";
        String[] selectionArgs = {username};
        db.update(DatabaseHelper.TABLE_USERS, values, selection, selectionArgs);
    }
}
