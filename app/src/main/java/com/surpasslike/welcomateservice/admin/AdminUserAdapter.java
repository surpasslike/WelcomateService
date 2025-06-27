package com.surpasslike.welcomateservice.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.surpasslike.welcomateservice.R;
import com.surpasslike.welcomateservice.User;

import java.util.List;

/**
 * 管理员用户列表适配器 - RecyclerView数据适配器
 * 
 * 功能职责：
 * 1. 将服务端数据库B中的用户数据展示在AdminDashboard的RecyclerView中
 * 2. 处理用户列表的显示和更新
 * 3. 管理每个用户项的视图绑定
 * 4. 支持动态刷新用户列表数据
 * 
 * 显示内容：
 * - 用户名（username）
 * - 用户账号（account）
 * - 用户密码（password）
 * 
 * 使用模式：
 * - ViewHolder模式：优化RecyclerView性能
 * - 数据绑定：将User对象绑定到UI组件
 * - 动态更新：支持通过setUserList方法刷新数据
 */
public class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.UserViewHolder> {

    private List<User> userList;

    /**
     * 构造函数 - 初始化适配器
     * 
     * @param userList 用户数据列表
     */
    public AdminUserAdapter(List<User> userList) {
        this.userList = userList;
    }

    /**
     * 创建ViewHolder
     * 当RecyclerView需要创建新的ViewHolder时调用
     * 
     * @param parent 父ViewGroup
     * @param viewType 视图类型
     * @return 新创建的UserViewHolder实例
     */
    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    /**
     * 绑定数据到ViewHolder
     * 将指定位置的用户数据绑定到对应的ViewHolder视图上
     * 
     * @param holder ViewHolder实例
     * @param position 数据在列表中的位置
     */
    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.tvUsername.setText(user.getUsername());
        holder.tvAccount.setText(user.getAccount());
        holder.tvPassword.setText(user.getPassword());
    }

    /**
     * 获取数据项总数
     * 
     * @return 用户列表的大小
     */
    @Override
    public int getItemCount() {
        return userList.size();
    }

    /**
     * 更新用户列表数据
     * 当数据发生变化时，调用此方法刷新RecyclerView显示
     * 
     * @param userList 新的用户数据列表
     */
    public void setUserList(List<User> userList) {
        this.userList = userList;
        notifyDataSetChanged();
    }

    /**
     * ViewHolder内部类 - 用户列表项视图持有者
     * 缓存每个列表项的视图组件，避免重复findViewById调用，提高性能
     */
    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsername, tvAccount, tvPassword;

        /**
         * ViewHolder构造函数
         * 初始化并缓存列表项中的所有视图组件
         * 
         * @param itemView 列表项的根视图
         */
        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvAccount = itemView.findViewById(R.id.tvAccount);
            tvPassword = itemView.findViewById(R.id.tvPassword);
        }
    }
}
