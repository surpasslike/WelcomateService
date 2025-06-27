package com.surpasslike.welcomateservice;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;

import com.surpasslike.welcomateservice.admin.AdminService;
import com.surpasslike.welcomateservice.config.SyncConfig;

import java.util.List;

/**
 * AIDL服务实现类 - 服务端核心业务逻辑
 * 
 * 功能职责：
 * 1. 实现IAdminService接口，提供跨进程的用户管理服务
 * 2. 管理服务端数据库B的所有CRUD操作
 * 3. 处理客户端的同步请求和数据变更通知
 * 4. 启动时主动连接客户端，拉取本地数据进行初始同步
 * 5. 广播数据库变化事件，通知AdminDashboard实时刷新
 * 
 * 同步机制：
 * - 启动同步：服务启动2秒后主动连接客户端，拉取所有本地数据
 * - 实时通知：接收客户端的数据变更通知，立即更新服务端数据库
 * - 广播刷新：数据变化后广播事件，让AdminDashboard及时刷新界面
 * 
 * AIDL方法：
 * - loginAdmin: 用户登录验证
 * - registerUser: 用户注册
 * - updateUserPassword: 修改密码
 * - deleteUser: 删除用户
 * - notify*: 接收客户端变更通知
 */
public class UserAIDLImpl extends IAdminService.Stub {
    private final String TAG = "UserAIDLImpl";
    private final DatabaseHelper dbHelper;
    private Handler mainHandler;

    /**
     * 构造函数 - 初始化AIDL服务
     * 创建时自动启动主动客户端同步机制
     */
    public UserAIDLImpl() {
        this.dbHelper = new DatabaseHelper(MyApplication.getContext());
        this.mainHandler = new Handler(Looper.getMainLooper());
        // 服务端B已准备就绪，主动尝试从客户端A获取数据
        Log.d(TAG, "UserAIDLImpl created, starting proactive client sync");
        startProactiveClientSync();
    }

    /**
     * AIDL接口实现 - 用户登录验证
     * 
     * @param account 用户账号
     * @param password 用户密码
     * @return 登录成功返回用户名，失败返回null
     */
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

    /**
     * AIDL接口实现 - 用户注册
     * 在服务端数据库B中创建新用户记录
     * 
     * @param username 用户名
     * @param account 用户账号
     * @param password 用户密码
     * @return 注册成功返回true，失败返回false（通常是因为用户已存在）
     */
    @Override
    public boolean registerUser(String username, String account, String password) {
        Log.d(TAG, "registerUser called - username: " + username + ", account: " + account);
        
        // 检查用户是否已存在
        if (checkUserExists(account)) {
            Log.d(TAG, "User already exists on server: " + account);
            return false;
        }
        
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_USERNAME, username);
        values.put(DatabaseHelper.COLUMN_ACCOUNT, account);
        values.put(DatabaseHelper.COLUMN_PASSWORD, password);

        long rowId = db.insert(DatabaseHelper.TABLE_USERS, null, values);
        boolean success = rowId != -1;
        Log.d(TAG, "Registration result: " + success + ", rowId: " + rowId);
        return success;
    }

    /**
     * AIDL接口实现 - 删除用户
     * 从服务端数据库B中删除指定用户，并广播变化事件
     * 
     * @param username 要删除的用户名
     * @throws RemoteException AIDL调用异常
     */
    @Override
    public void deleteUser(String username) throws RemoteException {
        Log.d(TAG, "deleteUser: " + username);
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            String selection = DatabaseHelper.COLUMN_USERNAME + " = ?";
            String[] selectionArgs = {username};

            int deletedRows = db.delete(DatabaseHelper.TABLE_USERS, selection, selectionArgs);
            Log.d(TAG, "User deletion result: " + deletedRows + " rows deleted for user: " + username);
            
            if (deletedRows > 0) {
                // 广播数据变化通知AdminDashboard
                broadcastDatabaseChange("user_deleted_via_aidl", username);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting user in AIDL", e);
            throw new RemoteException("Failed to delete user: " + e.getMessage());
        }
    }

    /**
     * AIDL接口实现 - 更新用户密码
     * 在服务端数据库B中修改指定用户的密码，并广播变化事件
     * 
     * @param username 用户名
     * @param newPassword 新密码
     * @throws RemoteException AIDL调用异常
     */
    @Override
    public void updateUserPassword(String username, String newPassword) throws RemoteException {
        Log.d(TAG, "updateUserPassword for user: " + username);
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COLUMN_PASSWORD, newPassword);
            String selection = DatabaseHelper.COLUMN_USERNAME + " = ?";
            String[] selectionArgs = {username};
            
            int updatedRows = db.update(DatabaseHelper.TABLE_USERS, values, selection, selectionArgs);
            Log.d(TAG, "Password update result: " + updatedRows + " rows updated for user: " + username);
            
            if (updatedRows > 0) {
                // 广播数据变化通知AdminDashboard
                broadcastDatabaseChange("password_updated_via_aidl", username);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating user password in AIDL", e);
            throw new RemoteException("Failed to update password: " + e.getMessage());
        }
    }


    @Override
    public List<String> getLocalUsers() throws RemoteException {
        // 服务端不实现这个方法，这是客户端专用的
        return null;
    }

    @Override
    public void clearLocalUsers() throws RemoteException {
        // 服务端不实现这个方法，这是客户端专用的
    }
    
    /**
     * 接收客户端用户注册通知
     * 当客户端在离线状态下注册用户后，会调用此方法通知服务端
     * 
     * @param username 注册的用户名
     * @param account 注册的账号
     * @param password 注册的密码
     * @throws RemoteException AIDL调用异常
     */
    @Override
    public void notifyUserRegistered(String username, String account, String password) throws RemoteException {
        Log.d(TAG, "Received notification: User registered - " + username);
        // 这里可以添加额外的服务端逻辑，比如日志记录、统计等
        // 实际的用户数据已经通过registerUser方法处理了
    }
    
    /**
     * 接收客户端用户删除通知
     * 当客户端删除用户后，通过此方法确保服务端数据库B也删除对应记录
     * 
     * @param username 被删除的用户名
     * @throws RemoteException AIDL调用异常
     */
    @Override
    public void notifyUserDeleted(String username) throws RemoteException {
        Log.d(TAG, "Received notification: User deleted from client - " + username);
        
        // 客户端删除用户后，确保服务端数据库也删除（双向同步确保）
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            String selection = DatabaseHelper.COLUMN_USERNAME + " = ?";
            String[] selectionArgs = {username};
            
            int deletedRows = db.delete(DatabaseHelper.TABLE_USERS, selection, selectionArgs);
            if (deletedRows > 0) {
                Log.d(TAG, "Server database updated: User deleted - " + username);
                
                // 通知AdminDashboard刷新（如果有回调机制的话）
                broadcastDatabaseChange("user_deleted", username);
            } else {
                Log.w(TAG, "User not found in server database: " + username);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating server database for user deletion", e);
        }
    }
    
    /**
     * 接收客户端密码更新通知
     * 当客户端修改密码后，通过此方法确保服务端数据库B也更新对应记录
     * 
     * @param username 用户名
     * @param newPassword 新密码
     * @throws RemoteException AIDL调用异常
     */
    @Override
    public void notifyPasswordUpdated(String username, String newPassword) throws RemoteException {
        Log.d(TAG, "Received notification: Password updated from client for user - " + username);
        
        // 客户端修改密码后，确保服务端数据库也更新（双向同步确保）
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COLUMN_PASSWORD, newPassword);
            
            String selection = DatabaseHelper.COLUMN_USERNAME + " = ?";
            String[] selectionArgs = {username};
            
            int updatedRows = db.update(DatabaseHelper.TABLE_USERS, values, selection, selectionArgs);
            if (updatedRows > 0) {
                Log.d(TAG, "Server database updated: Password changed for user - " + username);
                
                // 通知AdminDashboard刷新（如果有回调机制的话）
                broadcastDatabaseChange("password_updated", username);
            } else {
                Log.w(TAG, "User not found in server database for password update: " + username);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating server database for password change", e);
        }
    }
    
    @Override
    public boolean userExists(String account) throws RemoteException {
        return checkUserExists(account); // 调用私有方法
    }
    
    
    /**
     * 检查用户是否存在
     * 在服务端数据库B中查询指定账号是否已存在
     * 
     * @param account 用户账号
     * @return true表示用户存在，false表示不存在
     */
    private boolean checkUserExists(String account) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selection = DatabaseHelper.COLUMN_ACCOUNT + " = ?";
        String[] selectionArgs = {account};
        
        Cursor cursor = db.query(DatabaseHelper.TABLE_USERS, new String[]{DatabaseHelper.COLUMN_ID}, 
                selection, selectionArgs, null, null, null);
        
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }
    
    /**
     * 广播数据库变化，让AdminDashboard能够及时刷新
     */
    private void broadcastDatabaseChange(String operation, String username) {
        try {
            Intent broadcastIntent = new Intent(SyncConfig.DATABASE_CHANGE_ACTION);
            broadcastIntent.putExtra("operation", operation);
            broadcastIntent.putExtra("username", username);
            broadcastIntent.putExtra("timestamp", System.currentTimeMillis());
            
            MyApplication.getContext().sendBroadcast(broadcastIntent);
            Log.d(TAG, "Database change broadcast sent: " + operation + " for user: " + username);
        } catch (Exception e) {
            Log.e(TAG, "Error broadcasting database change", e);
        }
    }
    
    /**
     * 服务端启动后主动尝试从客户端获取数据（仅尝试一次）
     */
    private void startProactiveClientSync() {
        Log.d(TAG, "Starting proactive client sync - single attempt");
        
        // 使用配置中的延迟时间
        mainHandler.postDelayed(() -> tryConnectToClient(), SyncConfig.STARTUP_SYNC_DELAY);
    }
    
    /**
     * 尝试连接到客户端并获取数据（仅尝试一次，不重试）
     */
    private void tryConnectToClient() {
        Log.d(TAG, "Attempting to connect to client for startup sync");
        
        new Thread(() -> {
            try {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(SyncConfig.CLIENT_PACKAGE, SyncConfig.CLIENT_SERVICE));
                
                ServiceConnection clientConnection = new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName name, IBinder service) {
                        Log.d(TAG, "Startup sync: Connected to client service");
                        try {
                            IAdminService clientService = IAdminService.Stub.asInterface(service);
                            List<String> clientUsers = clientService.getLocalUsers();
                            Log.d(TAG, "Startup sync: Retrieved " + clientUsers.size() + " users from client");
                            
                            int syncedCount = 0;
                            for (String userData : clientUsers) {
                                String[] parts = userData.split("\\|");
                                if (parts.length == 3) {
                                    String username = parts[0];
                                    String account = parts[1];
                                    String password = parts[2];
                                    
                                    // 检查服务端是否已存在该用户
                                    if (!checkUserExists(account)) {
                                        // 服务端不存在，添加用户
                                        boolean success = registerUser(username, account, password);
                                        if (success) {
                                            syncedCount++;
                                            Log.d(TAG, "Startup sync: Added user from client: " + username);
                                        }
                                    } else {
                                        Log.d(TAG, "Startup sync: User already exists on server: " + username);
                                    }
                                }
                            }
                            
                            Log.d(TAG, "Startup sync completed: " + syncedCount + " users synced from client");
                            
                            // 广播同步完成事件
                            if (syncedCount > 0) {
                                broadcastDatabaseChange("startup_sync_completed", "synced_" + syncedCount + "_users");
                            }
                            
                            // 解绑服务
                            try {
                                MyApplication.getContext().unbindService(this);
                            } catch (Exception e) {
                                Log.e(TAG, "Error unbinding client service", e);
                            }
                            
                        } catch (RemoteException e) {
                            Log.e(TAG, "Error during startup sync", e);
                        }
                    }
                    
                    @Override
                    public void onServiceDisconnected(ComponentName name) {
                        Log.d(TAG, "Startup sync: Client service disconnected");
                    }
                };
                
                boolean bound = MyApplication.getContext().bindService(intent, clientConnection, Context.BIND_AUTO_CREATE);
                if (bound) {
                    Log.d(TAG, "Startup sync: Successfully bound to client service");
                } else {
                    Log.w(TAG, "Startup sync: Failed to bind to client service - client may not be running");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error in startup client sync", e);
            }
        }).start();
    }

    public IBinder onBind(Intent intent) {
        Log.d(TAG, "UserAIDLImpl onBind");
        return new UserAIDLImpl();
    }
}
