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

/**
 * 管理员服务类 - 服务端数据库操作封装
 * 
 * 功能职责：
 * 1. 封装服务端数据库B的所有CRUD操作
 * 2. 提供用户管理的业务逻辑方法
 * 3. 为AdminViewModel和AdminDashboard提供数据访问层
 * 4. 作为Service组件，支持跨进程绑定和调用
 * 
 * 主要功能：
 * - 用户登录验证：loginAdmin()
 * - 获取所有用户：getAllUsers()
 * - 添加新用户：addUser()
 * - 删除用户：deleteUser()
 * - 修改密码：updateUserPassword()
 * 
 * 服务生命周期：
 * - onCreate(): 服务创建时初始化
 * - onBind(): 返回AIDL接口实例
 * - onStartCommand(): 处理启动命令
 * - onDestroy(): 服务销毁时清理资源
 * 
 * 数据库操作：
 * - 直接操作SQLite数据库
 * - 使用DatabaseHelper进行数据库连接管理
 * - 支持事务性操作，确保数据一致性
 */
public class AdminService extends Service {
    private final String TAG = "AdminService";

    /**
     * 默认构造函数
     * Android Service组件必须提供无参构造函数
     */
    public AdminService() {
        // 这是默认的无参数构造函数
    }

    private DatabaseHelper dbHelper;

    /**
     * 带参构造函数
     * 用于依赖注入DatabaseHelper实例
     * 
     * @param dbHelper 数据库助手实例
     */
    public AdminService(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    /**
     * 验证管理员登录
     * 根据账号和密码验证用户身份
     * 
     * @param account 用户账号
     * @param password 用户密码
     * @return true表示登录成功，false表示登录失败
     */
    public boolean loginAdmin(String account, String password) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selection = DatabaseHelper.COLUMN_ACCOUNT + " = ? AND " + DatabaseHelper.COLUMN_PASSWORD + " = ?";
        String[] selectionArgs = {account, password};

        Cursor cursor = db.query(DatabaseHelper.TABLE_USERS, null, selection, selectionArgs, null, null, null);

        boolean loginSuccess = cursor.moveToFirst();
        cursor.close();
        return loginSuccess;
    }

    /**
     * 获取所有用户信息
     * 从服务端数据库B中查询所有用户记录
     * 
     * @return 用户列表，包含所有用户的用户名、账号、密码信息
     */
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

    /**
     * 添加新用户
     * 在服务端数据库B中创建新用户记录
     * 
     * @param username 用户名
     * @param account 用户账号
     * @param password 用户密码
     * @return 插入的行ID，-1表示插入失败
     */
    public long addUser(String username, String account, String password) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_USERNAME, username);
        values.put(DatabaseHelper.COLUMN_ACCOUNT, account);
        values.put(DatabaseHelper.COLUMN_PASSWORD, password);

        // 插入新用户数据并返回插入的行ID
        return db.insert(DatabaseHelper.TABLE_USERS, null, values);
    }

    /**
     * 删除指定用户
     * 从服务端数据库B中删除指定用户名的用户记录
     * 
     * @param username 要删除的用户名
     */
    public void deleteUser(String username) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String selection = DatabaseHelper.COLUMN_USERNAME + " = ?";
        String[] selectionArgs = {username};

        Log.d(TAG, "deleteUser username = " + username);
        // 删除指定用户名的用户
        db.delete(DatabaseHelper.TABLE_USERS, selection, selectionArgs);
    }

    /**
     * 更新用户密码
     * 修改服务端数据库B中指定用户的密码
     * 
     * @param username 用户名
     * @param newPassword 新密码
     */
    public void updateUserPassword(String username, String newPassword) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_PASSWORD, newPassword);

        String selection = DatabaseHelper.COLUMN_USERNAME + " = ?";
        String[] selectionArgs = {username};

        db.update(DatabaseHelper.TABLE_USERS, values, selection, selectionArgs);
    }


    /**
     * 服务绑定回调
     * 当其他组件绑定到此服务时调用，返回AIDL接口实例
     * 
     * @param intent 绑定意图
     * @return UserAIDLImpl的IBinder实例，用于跨进程通信
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        System.out.println("服务被创建");
        Log.d(TAG, "AdminService onBind");
        return new UserAIDLImpl();
    }

    /**
     * 服务创建回调
     * 服务第一次创建时调用，用于初始化资源
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "AdminService onCreate");
    }

    /**
     * 服务启动回调
     * 每次通过startService启动服务时调用
     * 
     * @param intent 启动意图
     * @param flags 启动标志
     * @param startId 启动ID
     * @return 服务启动模式，使用默认模式
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * 服务销毁回调
     * 服务被销毁时调用，用于清理资源
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
