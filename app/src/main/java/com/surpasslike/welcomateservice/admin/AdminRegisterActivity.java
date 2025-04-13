package com.surpasslike.welcomateservice.admin;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.surpasslike.welcomateservice.databinding.ActivityAdminRegisterBinding;

public class AdminRegisterActivity extends AppCompatActivity {

    private ActivityAdminRegisterBinding binding;
    private AdminViewModel adminViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminRegisterBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        adminViewModel = new ViewModelProvider(this).get(AdminViewModel.class);

        binding.btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = binding.etUsername.getText().toString();
                String account = binding.etAccount.getText().toString();
                String password = binding.etPassword.getText().toString();

                // 调用ViewModel中的注册方法
                long rowId = adminViewModel.addUser(username, account, password);
                if (rowId != -1) {
                    showToast("Registration successful!");
                    // 注册成功后跳转到登录页面
                    finish();
                } else {
                    showToast("Registration failed! Please try again.");
                }
            }
        });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
