package com.feseis.fish;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.feseis.fish.auth.api.AuthApiProvider;
import com.feseis.fish.auth.model.LoginResult;
import com.feseis.fish.auth.store.SessionStore;
import com.google.android.material.button.MaterialButton;

/**
 * 会话信息页：展示登录响应里拿到的全部字段。
 */
public class SessionActivity extends AppCompatActivity {

    private TextView accountView;
    private TextView phoneView;
    private TextView rolesView;
    private TextView tokenView;
    private TextView expireView;
    private MaterialButton logoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        this.setContentView(R.layout.activity_session);
        ViewCompat.setOnApplyWindowInsetsListener(this.findViewById(R.id.session_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        this.bindViews();
        this.render();
        this.findViewById(R.id.btn_back_home).setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            this.startActivity(intent);
            this.finish();
        });
        this.logoutButton.setOnClickListener(v -> this.logout());
    }

    private void bindViews() {
        this.accountView = this.findViewById(R.id.tv_session_account);
        this.phoneView = this.findViewById(R.id.tv_session_phone);
        this.rolesView = this.findViewById(R.id.tv_session_roles);
        this.tokenView = this.findViewById(R.id.tv_session_token);
        this.expireView = this.findViewById(R.id.tv_session_expire);
        this.logoutButton = this.findViewById(R.id.btn_session_logout);
    }

    private void render() {
        LoginResult session = SessionStore.get().getSession();
        if (session == null) {
            this.accountView.setText(R.string.session_empty);
            this.phoneView.setText(R.string.session_empty);
            this.rolesView.setText(R.string.session_empty);
            this.tokenView.setText(R.string.session_empty);
            this.expireView.setText(R.string.session_empty);
            return;
        }
        this.accountView.setText(session.getAccountId());
        this.phoneView.setText(session.getPhone());
        this.rolesView.setText(TextUtils.join(", ", session.getRoles()));
        this.tokenView.setText(session.getAccessToken());
        this.expireView.setText(formatExpiry(session.getAccessExpiresAt()));
    }

    private void logout() {
        this.logoutButton.setEnabled(false);
        AuthApiProvider.get().logout(result -> {
            if (this.isFinishing() || this.isDestroyed()) {
                return;
            }
            this.logoutButton.setEnabled(true);
            SessionStore.get().clear();
            Toast.makeText(this, R.string.main_logout_success, Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            this.startActivity(intent);
            this.finish();
        });
    }

    private static String formatExpiry(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "-";
        }
        String value = raw.length() > 19 ? raw.substring(0, 19) : raw;
        return value.replace('T', ' ');
    }
}
