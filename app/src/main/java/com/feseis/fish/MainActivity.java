package com.feseis.fish;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
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

public class MainActivity extends AppCompatActivity {

    private TextView loginState;
    private View infoGroup;
    private TextView accountInfo;
    private TextView roleInfo;
    private TextView expireInfo;
    private MaterialButton goLoginButton;
    private MaterialButton logoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        this.bindViews();
        this.goLoginButton.setOnClickListener(v -> this.startActivity(new Intent(this, LoginActivity.class)));
        this.logoutButton.setOnClickListener(v -> this.logout());
        this.refreshUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.refreshUi();
    }

    private void bindViews() {
        this.loginState = this.findViewById(R.id.tv_login_state);
        this.infoGroup = this.findViewById(R.id.info_group);
        this.accountInfo = this.findViewById(R.id.tv_account_info);
        this.roleInfo = this.findViewById(R.id.tv_role_info);
        this.expireInfo = this.findViewById(R.id.tv_expire_info);
        this.goLoginButton = this.findViewById(R.id.btn_go_login);
        this.logoutButton = this.findViewById(R.id.btn_logout);
    }

    private void refreshUi() {
        LoginResult session = SessionStore.get().getSession();
        boolean loggedIn = session != null;
        this.loginState.setText(loggedIn ? R.string.main_logged_in : R.string.main_not_logged_in);
        this.infoGroup.setVisibility(loggedIn ? View.VISIBLE : View.GONE);
        this.logoutButton.setVisibility(loggedIn ? View.VISIBLE : View.GONE);
        this.goLoginButton.setVisibility(loggedIn ? View.GONE : View.VISIBLE);
        if (!loggedIn) {
            this.accountInfo.setText("");
            this.roleInfo.setText("");
            this.expireInfo.setText("");
            return;
        }
        this.accountInfo.setText(this.getString(R.string.main_account_format, session.getPhone()));
        this.roleInfo.setText(this.getString(R.string.main_roles_format,
                TextUtils.join(", ", session.getRoles())));
        this.expireInfo.setText(this.getString(R.string.main_expire_format,
                formatExpiry(session.getAccessExpiresAt())));
    }

    private void logout() {
        this.logoutButton.setEnabled(false);
        AuthApiProvider.get().logout(result -> {
            if (this.isFinishing() || this.isDestroyed()) {
                return;
            }
            this.logoutButton.setEnabled(true);
            SessionStore.get().clear();
            this.refreshUi();
            Toast.makeText(this, R.string.main_logout_success, Toast.LENGTH_SHORT).show();
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
