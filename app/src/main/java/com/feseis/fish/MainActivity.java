package com.feseis.fish;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.feseis.fish.auth.config.AuthEndpoint;
import com.feseis.fish.auth.model.AuthError;
import com.feseis.fish.auth.model.HealthResult;
import com.feseis.fish.auth.model.LoginResult;
import com.feseis.fish.auth.store.SessionStore;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class MainActivity extends AppCompatActivity {

    private TextView loginState;
    private View infoGroup;
    private TextView accountInfo;
    private TextView roleInfo;
    private TextView expireInfo;
    private MaterialButton goLoginButton;
    private MaterialButton logoutButton;
    private View devPanel;
    private TextInputLayout endpointLayout;
    private TextInputEditText endpointInput;
    private MaterialButton testEndpointButton;
    private TextView endpointStatus;

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
        this.devPanel = this.findViewById(R.id.dev_panel);
        this.endpointLayout = this.findViewById(R.id.til_endpoint);
        this.endpointInput = this.findViewById(R.id.et_endpoint);
        this.testEndpointButton = this.findViewById(R.id.btn_test_endpoint);
        this.endpointStatus = this.findViewById(R.id.tv_endpoint_status);
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

    private void setupDevPanel() {
        this.devPanel.setVisibility(BuildConfig.DEBUG ? View.VISIBLE : View.GONE);
        if (!BuildConfig.DEBUG) {
            return;
        }
        this.endpointInput.setText(AuthEndpoint.get(this));
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
        String normalized = AuthEndpoint.normalize(this.endpointInput.getText() == null
                ? "" : this.endpointInput.getText().toString());
        if (!AuthEndpoint.isValid(normalized)) {
            this.endpointLayout.setError(this.getString(R.string.dev_connect_invalid));
            return;
        }
        this.endpointLayout.setError(null);
        AuthEndpoint.save(this, normalized);
        this.endpointInput.setText(normalized);
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

    private static String formatExpiry(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "-";
        }
        String value = raw.length() > 19 ? raw.substring(0, 19) : raw;
        return value.replace('T', ' ');
    }
}
