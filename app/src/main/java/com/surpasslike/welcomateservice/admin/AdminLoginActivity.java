package com.surpasslike.welcomateservice.admin;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.surpasslike.welcomateservice.databinding.ActivityAdminLoginBinding;

public class AdminLoginActivity extends AppCompatActivity {
    private final String TAG = "AdminLoginActivity";
    private ActivityAdminLoginBinding binding;
    private AdminViewModel adminViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminLoginBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        adminViewModel = new ViewModelProvider(this).get(AdminViewModel.class);

        binding.btnLogin.setOnClickListener(v -> {
            String account = binding.etUsername.getText().toString();
            String password = binding.etPassword.getText().toString();

            // 调用AdminViewModel中的登录方法验证管理员登录
            boolean loginSuccess = adminViewModel.loginAdmin(account, password);
            if (loginSuccess) {
                // 登录成功，跳转到管理员主界面
                showToast("Login successful!");
                startActivity(new Intent(AdminLoginActivity.this, AdminDashboardActivity.class));
                finish(); // 结束当前登录界面，避免返回登录界面
            } else {
                // 登录失败，显示提示信息
                showToast("Login failed! Please check your account and password.");
            }
        });

        binding.tvRegister.setOnClickListener(view1 -> {
            Log.d(TAG, "onClick: tvRegister clicked");
            startActivity(new Intent(AdminLoginActivity.this, AdminRegisterActivity.class));
        });

    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
