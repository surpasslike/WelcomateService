package com.surpasslike.welcomateservice.admin;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.surpasslike.welcomateservice.IAdminService;
import com.surpasslike.welcomateservice.R;
import com.surpasslike.welcomateservice.User;
import com.surpasslike.welcomateservice.config.SyncConfig;
import com.surpasslike.welcomateservice.databinding.ActivityAdminDashboardBinding;

import java.util.List;

/**
 * 管理员仪表盘界面 - 服务端用户管理中心
 * 
 * 功能职责：
 * 1. 提供服务端用户管理的图形界面（增删改查）
 * 2. 显示服务端数据库B中的所有用户列表
 * 3. 支持管理员对用户进行密码修改、删除、添加操作
 * 4. 实时监听数据库变化广播，自动刷新用户列表
 * 5. 将服务端的操作同步推送到客户端A
 * 
 * 同步机制：
 * - 启动同步：页面加载时主动从客户端拉取数据
 * - 操作同步：管理员操作后立即推送到客户端
 * - 实时刷新：监听广播事件，自动刷新界面显示
 * 
 * 主要功能按钮：
 * - 修改密码：选择用户并修改其密码
 * - 添加用户：创建新的用户账号
 * - 删除用户：删除指定的用户账号
 * - 刷新列表：手动刷新用户列表显示
 */
public class AdminDashboardActivity extends AppCompatActivity {

    private static final String TAG = "AdminDashboardActivity";
    private ActivityAdminDashboardBinding binding;
    private AdminViewModel adminViewModel;
    private List<User> userList;
    private com.surpasslike.welcomateservice.admin.AdminUserAdapter adapter;
    
    // 数据库变化监听器
    private BroadcastReceiver databaseChangeReceiver;
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminDashboardBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        adminViewModel = new ViewModelProvider(this).get(AdminViewModel.class);
        // 初始化 RecyclerView 和用户列表
        RecyclerView recyclerView = binding.recyclerView;
        userList = adminViewModel.getAllUsers();
        adapter = new com.surpasslike.welcomateservice.admin.AdminUserAdapter(userList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        binding.btnChangePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //实现修改指定用户密码功能
                showUserListDialog();
            }
        });

        binding.btnAddUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 实现增加用户的功能
                showAddUserDialog();

            }
        });

        binding.btnDeleteUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 实现删除用户的功能
                showDeleteUserDialog();
            }
        });
        
        binding.btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 刷新用户列表
                refreshUserList();
            }
        });
        
        // 页面加载时尝试从客户端同步数据
        triggerInitialSync();
        
        // 注册数据库变化监听器
        setupDatabaseChangeListener();
    }

    // 显示Toast提示信息的方法
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    //显示用户列表对话框
    private void showUserListDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select User to Change Password");

        //将其替换为数据库中的实际用户列表
        List<User> userList = adminViewModel.getAllUsers();

        // 将用户列表转换为要在对话框中显示的用户名数组
        String[] usernames = new String[userList.size()];
        for (int i = 0; i < userList.size(); i++) {
            usernames[i] = userList.get(i).getUsername();
        }

        // 在对话框中显示用户名列表
        builder.setItems(usernames, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 获取所选用户名
                String selectedUsername = userList.get(which).getUsername();

                // 调用方法以更改所选用户的密码
                showChangePasswordDialog(selectedUsername);
            }
        });

        //显示对话框
        builder.create().show();
    }

    //显示更改密码对话框
    private void showChangePasswordDialog(String username) {
        // 创建对话框以更改所选用户的密码
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Password for " + username);

        View view = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        final EditText etNewPassword = view.findViewById(R.id.etNewPassword);
        builder.setView(view);

        builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 获取新密码
                String newPassword = etNewPassword.getText().toString();

                // 调用方法以更改所选用户的密码
                adminViewModel.changeUserPassword(username, newPassword);
                userList = adminViewModel.getAllUsers(); // 添加用户后刷新用户列表
                adapter.setUserList(userList); // 使用新用户列表更新适配器
                showToast("Password changed for " + username);
                
                // 服务端修改后，立即推送到客户端A
                pushOperationToClient("updatePassword", username, null, newPassword);
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 取消密码更改操作
            }
        });
        //显示对话框
        builder.create().show();
    }

    // 增加用户的功能
    private void showAddUserDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New User");

        View view = getLayoutInflater().inflate(R.layout.dialog_add_user, null);
        final EditText etNewUsername = view.findViewById(R.id.etNewUsername);
        final EditText etNewAccount = view.findViewById(R.id.etNewAccount);
        final EditText etNewPassword = view.findViewById(R.id.etNewPassword);
        builder.setView(view);

        builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 从输入字段中获取新用户的详细信息
                String newUsername = etNewUsername.getText().toString();
                String newAccount = etNewAccount.getText().toString();
                String newPassword = etNewPassword.getText().toString();

                // 调用方法以添加新用户
                long rowId = adminViewModel.addUser(newUsername, newAccount, newPassword);
                if (rowId != -1) {
                    showToast("User added");
                    userList = adminViewModel.getAllUsers(); // 添加用户后刷新用户列表
                    adapter.setUserList(userList); // 使用新用户列表更新适配器
                    
                    // 服务端添加用户后，立即推送到客户端A
                    pushOperationToClient("addUser", newUsername, newAccount, newPassword);
                } else {
                    showToast("Failed to add user");
                }
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 取消用户添加操作
            }
        });

        builder.create().show();
    }

    private void showDeleteUserDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete User");

        View view = getLayoutInflater().inflate(R.layout.dialog_delete_user, null);
        final EditText etUsernameToDelete = view.findViewById(R.id.etUsernameToDelete);
        builder.setView(view);

        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 从输入字段中获取要删除的用户名
                String usernameToDelete = etUsernameToDelete.getText().toString();

                // 调用方法删除用户
                adminViewModel.deleteUser(usernameToDelete);
                showToast("User deleted");
                userList = adminViewModel.getAllUsers(); // 删除用户后刷新用户列表
                adapter.setUserList(userList); // 使用更新的用户列表更新适配器
                
                // 服务端删除用户后，立即推送到客户端A
                pushOperationToClient("deleteUser", usernameToDelete, null, null);
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 取消用户删除操作
            }
        });

        builder.create().show();
    }
    
    /**
     * 页面启动时触发初始同步
     */
    private void triggerInitialSync() {
        Log.d(TAG, "Triggering initial sync from client...");
        
        new Thread(() -> {
            try {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(SyncConfig.CLIENT_PACKAGE, SyncConfig.CLIENT_SERVICE));
                
                ServiceConnection connection = new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName name, IBinder service) {
                        Log.d(TAG, "Connected to client for initial sync");
                        try {
                            IAdminService clientService = IAdminService.Stub.asInterface(service);
                            List<String> clientUsers = clientService.getLocalUsers();
                            Log.d(TAG, "Retrieved " + clientUsers.size() + " users from client during initial sync");
                            
                            int syncedCount = 0;
                            for (String userData : clientUsers) {
                                String[] parts = userData.split("\\|");
                                if (parts.length == 3) {
                                    String username = parts[0];
                                    String account = parts[1];
                                    String password = parts[2];
                                    
                                    // 检查用户是否已存在
                                    boolean userExists = false;
                                    for (User existingUser : userList) {
                                        if (existingUser.getAccount().equals(account)) {
                                            userExists = true;
                                            break;
                                        }
                                    }
                                    
                                    if (!userExists) {
                                        long result = adminViewModel.addUser(username, account, password);
                                        if (result != -1) {
                                            syncedCount++;
                                            Log.d(TAG, "Initially synced user: " + username);
                                        }
                                    }
                                }
                            }
                            
                            final int finalSyncedCount = syncedCount;
                            runOnUiThread(() -> {
                                if (finalSyncedCount > 0) {
                                    showToast("初始同步完成，获取到 " + finalSyncedCount + " 个用户");
                                    userList = adminViewModel.getAllUsers();
                                    adapter.setUserList(userList);
                                } else {
                                    Log.d(TAG, "No new users to sync during initial sync");
                                }
                            });
                            
                            // 在onServiceConnected内部解绑服务
                            try {
                                unbindService(this);
                            } catch (Exception e) {
                                Log.e(TAG, "Error unbinding service", e);
                            }
                            
                        } catch (RemoteException e) {
                            Log.e(TAG, "Error during initial sync", e);
                        }
                    }
                    
                    @Override
                    public void onServiceDisconnected(ComponentName name) {
                        Log.d(TAG, "Client service disconnected after initial sync");
                    }
                };
                
                boolean bound = bindService(intent, connection, Context.BIND_AUTO_CREATE);
                if (!bound) {
                    Log.w(TAG, "Failed to bind to client for initial sync - client may not be running");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error in initial sync", e);
            }
        }).start();
    }
    
    /**
     * 刷新用户列表显示
     */
    private void refreshUserList() {
        Log.d(TAG, "Refreshing user list...");
        userList = adminViewModel.getAllUsers();
        adapter.setUserList(userList);
        showToast("用户列表已刷新");
        
        // 同时尝试从客户端同步新数据
        triggerInitialSync();
    }
    
    /**
     * 统一的推送操作到客户端方法
     */
    private void pushOperationToClient(String operation, String username, String account, String password) {
        Log.d(TAG, "Pushing " + operation + " to client - user: " + username);
        
        new Thread(() -> {
            try {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(SyncConfig.CLIENT_PACKAGE, SyncConfig.CLIENT_SERVICE));
                
                ServiceConnection connection = new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName name, IBinder service) {
                        Log.d(TAG, "Connected to client for " + operation + " push");
                        try {
                            IAdminService clientService = IAdminService.Stub.asInterface(service);
                            
                            boolean success = true;
                            String successMessage = "";
                            String failMessage = "";
                            
                            // 根据操作类型执行相应的客户端操作
                            switch (operation) {
                                case "updatePassword":
                                    clientService.updateUserPassword(username, password);
                                    successMessage = "密码已同步到客户端";
                                    failMessage = "密码同步到客户端失败";
                                    break;
                                    
                                case "addUser":
                                    success = clientService.registerUser(username, account, password);
                                    successMessage = success ? "新用户已同步到客户端" : "用户已存在于客户端";
                                    failMessage = "用户同步到客户端失败";
                                    break;
                                    
                                case "deleteUser":
                                    clientService.deleteUser(username);
                                    successMessage = "用户删除已同步到客户端";
                                    failMessage = "用户删除同步到客户端失败";
                                    break;
                                    
                                default:
                                    Log.w(TAG, "Unknown operation: " + operation);
                                    success = false;
                                    failMessage = "未知操作";
                            }
                            
                            if (success) {
                                Log.d(TAG, operation + " pushed to client successfully");
                                final String msg = successMessage;
                                runOnUiThread(() -> showToast(msg));
                            } else {
                                Log.w(TAG, operation + " push failed");
                                final String msg = failMessage;
                                runOnUiThread(() -> showToast(msg));
                            }
                            
                            unbindService(this);
                        } catch (Exception e) {
                            Log.e(TAG, "Error pushing " + operation + " to client", e);
                            runOnUiThread(() -> showToast(operation + "同步到客户端失败"));
                        }
                    }
                    
                    @Override
                    public void onServiceDisconnected(ComponentName name) {
                        Log.d(TAG, "Client service disconnected after " + operation + " push");
                    }
                };
                
                boolean bound = bindService(intent, connection, Context.BIND_AUTO_CREATE);
                if (!bound) {
                    Log.w(TAG, "Failed to bind to client for " + operation + " push");
                    runOnUiThread(() -> showToast("客户端不可用，操作未同步"));
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error in pushOperationToClient for " + operation, e);
            }
        }).start();
    }
    
    /**
     * 设置数据库变化监听器
     */
    private void setupDatabaseChangeListener() {
        databaseChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (SyncConfig.DATABASE_CHANGE_ACTION.equals(intent.getAction())) {
                    String operation = intent.getStringExtra("operation");
                    String username = intent.getStringExtra("username");
                    
                    Log.d(TAG, "Received database change broadcast: " + operation + " for user: " + username);
                    
                    // 在主线程中刷新用户列表
                    runOnUiThread(() -> {
                        userList = adminViewModel.getAllUsers();
                        adapter.setUserList(userList);
                        
                        // 显示相应的提示信息
                        switch (operation) {
                            case "password_updated":
                                showToast("客户端修改了用户密码: " + username);
                                break;
                            case "user_deleted":
                                showToast("客户端删除了用户: " + username);
                                break;
                            default:
                                showToast("客户端数据变化: " + operation);
                                break;
                        }
                    });
                }
            }
        };
        
        // 注册广播接收器
        IntentFilter filter = new IntentFilter(SyncConfig.DATABASE_CHANGE_ACTION);
        
        // Android 14+ 需要指定 RECEIVER_NOT_EXPORTED，因为这是应用内部广播
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(databaseChangeReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(databaseChangeReceiver, filter);
        }
        Log.d(TAG, "Database change listener registered");
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // 注销广播接收器
        if (databaseChangeReceiver != null) {
            try {
                unregisterReceiver(databaseChangeReceiver);
                Log.d(TAG, "Database change listener unregistered");
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering database change listener", e);
            }
        }
    }
}
