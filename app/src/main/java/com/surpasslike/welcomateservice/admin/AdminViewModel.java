package com.surpasslike.welcomateservice.admin;

import android.annotation.SuppressLint;

import androidx.lifecycle.ViewModel;

import com.surpasslike.welcomateservice.DatabaseHelper;
import com.surpasslike.welcomateservice.MyApplication;
import com.surpasslike.welcomateservice.User;

import java.util.List;

public class AdminViewModel extends ViewModel {
    @SuppressLint("StaticFieldLeak")
    private final AdminService adminService;

    public AdminViewModel() {
        // 在构造方法中初始化 AdminService
        DatabaseHelper dbHelper = new DatabaseHelper(MyApplication.getContext());
        adminService = new AdminService(dbHelper);
    }

    // 管理员登录验证方法
    public boolean loginAdmin(String account, String password) {
        return adminService.loginAdmin(account, password);
    }

    // 添加新用户的方法
    public long addUser(String username, String account, String password) {
        return adminService.addUser(username, account, password);
    }

    // 获取所有用户的方法
    public List<User> getAllUsers() {
        return adminService.getAllUsers();
    }

    // 删除用户的方法
    public void deleteUser(String username) {
        adminService.deleteUser(username);
    }

    // 修改指定用户的密码
    public void changeUserPassword(String username, String newPassword) {
        adminService.updateUserPassword(username, newPassword);
    }
}
