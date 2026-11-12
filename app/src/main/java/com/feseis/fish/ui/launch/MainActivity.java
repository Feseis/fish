package com.feseis.fish.ui.launch;

import com.feseis.fish.BuildConfig;
import com.feseis.fish.R;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.feseis.fish.auth.api.AuthApiProvider;
import com.feseis.fish.auth.api.TokenRefresher;
import com.feseis.fish.auth.config.AuthEndpoint;
import com.feseis.fish.core.model.AuthError;
import com.feseis.fish.auth.model.HealthResult;
import com.feseis.fish.auth.store.PersistentSessionStore;
import com.feseis.fish.auth.store.SessionStore;
import com.feseis.fish.ui.auth.LoginActivity;
import com.feseis.fish.ui.main.HomeActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * 启动页：服务地址配置 + 登录入口。
 * 地址输入只收 IP，端口固定为 auth 服务的 8081。
 */
public class MainActivity extends AppCompatActivity {

    private MaterialButton goLoginButton;
    private MaterialButton logoutButton;
    private View devPanel;
    private TextInputLayout endpointLayout;
    private TextInputEditText endpointInput;
    private MaterialButton testEndpointButton;
    private TextView endpointStatus;
    private boolean restoring;

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
        this.setupDevPanel();
        this.refreshUi();
        this.tryRestoreSession();
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.refreshUi();
    }

    private void bindViews() {
        this.goLoginButton = this.findViewById(R.id.btn_go_login);
        this.logoutButton = this.findViewById(R.id.btn_logout);
        this.devPanel = this.findViewById(R.id.dev_panel);
        this.endpointLayout = this.findViewById(R.id.til_endpoint);
        this.endpointInput = this.findViewById(R.id.et_endpoint);
        this.testEndpointButton = this.findViewById(R.id.btn_test_endpoint);
        this.endpointStatus = this.findViewById(R.id.tv_endpoint_status);
    }

    private void refreshUi() {
        if (this.restoring) {
            this.goLoginButton.setVisibility(View.VISIBLE);
            this.goLoginButton.setEnabled(false);
            this.goLoginButton.setText(R.string.main_restoring);
            this.logoutButton.setVisibility(View.GONE);
            return;
        }
        boolean loggedIn = SessionStore.get().isLoggedIn();
        this.goLoginButton.setEnabled(true);
        this.goLoginButton.setText(R.string.launch_go_login);
        this.goLoginButton.setVisibility(loggedIn ? View.GONE : View.VISIBLE);
        this.logoutButton.setVisibility(loggedIn ? View.VISIBLE : View.GONE);
    }

    /** 冷启动：有持久化 refresh 时静默换新 access，成功后直接进首页。 */
    private void tryRestoreSession() {
        if (SessionStore.get().isLoggedIn() || !PersistentSessionStore.hasSession()) {
            return;
        }
        this.restoring = true;
        this.refreshUi();
        TokenRefresher.refreshAsync(success -> {
            if (this.isFinishing() || this.isDestroyed()) {
                return;
            }
            this.restoring = false;
            if (!success) {
                this.refreshUi();
                Toast.makeText(this, R.string.main_restore_failed, Toast.LENGTH_SHORT).show();
                return;
            }
            this.startActivity(new Intent(this, HomeActivity.class));
            this.finish();
        });
    }

    private void logout() {
        this.logoutButton.setEnabled(false);
        AuthApiProvider.get().logout(result -> {
            if (this.isFinishing() || this.isDestroyed()) {
                return;
            }
            this.logoutButton.setEnabled(true);
            SessionStore.get().clear();
            PersistentSessionStore.clear();
            this.refreshUi();
            Toast.makeText(this, R.string.main_logout_success, Toast.LENGTH_SHORT).show();
        });
    }

    private void setupDevPanel() {
        this.devPanel.setVisibility(BuildConfig.DEBUG ? View.VISIBLE : View.GONE);
        if (!BuildConfig.DEBUG) {
            return;
        }
        this.endpointInput.setText(AuthEndpoint.host(this));
        this.renderEndpointStatus();
        this.testEndpointButton.setOnClickListener(v -> this.testEndpoint());
    }

    private void renderEndpointStatus() {
        String current = AuthEndpoint.get(this);
        this.showEndpointStatus(current.isEmpty()
                        ? this.getString(R.string.dev_connect_unset)
                        : this.getString(R.string.dev_connect_current, current),
                R.color.fish_text_secondary);
    }

    private void testEndpoint() {
        String raw = this.endpointInput.getText() == null
                ? "" : this.endpointInput.getText().toString();
        String normalized = AuthEndpoint.normalize(raw);
        if (!AuthEndpoint.isValid(normalized)) {
            this.endpointLayout.setError(this.getString(R.string.dev_connect_invalid));
            return;
        }
        this.endpointLayout.setError(null);
        AuthEndpoint.save(this, raw);
        this.endpointInput.setText(AuthEndpoint.host(this));
        this.setEndpointTesting(true);
        AuthApiProvider.get().checkHealth(result -> {
            if (this.isFinishing() || this.isDestroyed()) {
                return;
            }
            this.setEndpointTesting(false);
            if (!result.isSuccess()) {
                AuthError error = result.getError();
                this.showEndpointStatus(this.getString(R.string.dev_connect_network_fail,
                                error == null ? "" : error.getMessage()),
                        R.color.fish_error);
                return;
            }
            HealthResult health = result.getData();
            if (health != null && health.isDatabase() && health.isRedis()) {
                this.showEndpointStatus(this.getString(R.string.dev_connect_ok, normalized),
                        R.color.fish_success);
            } else {
                this.showEndpointStatus(this.getString(R.string.dev_connect_degraded),
                        R.color.fish_warning);
            }
        });
    }

    private void setEndpointTesting(boolean testing) {
        this.testEndpointButton.setEnabled(!testing);
        this.testEndpointButton.setText(testing
                ? R.string.dev_connect_testing : R.string.dev_connect_action);
        this.endpointInput.setEnabled(!testing);
        if (testing) {
            this.showEndpointStatus(this.getString(R.string.dev_connect_testing),
                    R.color.fish_text_secondary);
        }
    }

    private void showEndpointStatus(String text, int colorRes) {
        this.endpointStatus.setText(text);
        this.endpointStatus.setTextColor(ContextCompat.getColor(this, colorRes));
    }
}
