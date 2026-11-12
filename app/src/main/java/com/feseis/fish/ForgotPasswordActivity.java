package com.feseis.fish;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.feseis.fish.auth.api.AuthApi;
import com.feseis.fish.auth.api.AuthApiProvider;
import com.feseis.fish.auth.model.AuthError;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.regex.Pattern;

/**
 * 忘记密码：手机号 + 短信验证码重置密码。
 */
public class ForgotPasswordActivity extends AppCompatActivity {

    public static final String EXTRA_PHONE = "extra_phone";

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern CODE_PATTERN = Pattern.compile("^\\d{6}$");
    private static final int COUNTDOWN_SECONDS = 60;
    private static final int PASSWORD_MAX_LENGTH = 64;

    private AuthApi authApi;
    private TextInputLayout phoneLayout;
    private TextInputLayout codeLayout;
    private TextInputLayout passwordLayout;
    private TextInputLayout confirmLayout;
    private TextInputEditText phoneInput;
    private TextInputEditText codeInput;
    private TextInputEditText passwordInput;
    private TextInputEditText confirmInput;
    private MaterialButton sendCodeButton;
    private MaterialButton submitButton;
    private CircularProgressIndicator progress;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private int remainingSeconds;
    private boolean countdownRunning;

    private final Runnable countdownRunnable = new Runnable() {
        @Override
        public void run() {
            ForgotPasswordActivity.this.remainingSeconds--;
            if (ForgotPasswordActivity.this.remainingSeconds <= 0) {
                ForgotPasswordActivity.this.countdownRunning = false;
                ForgotPasswordActivity.this.sendCodeButton.setEnabled(true);
                ForgotPasswordActivity.this.sendCodeButton.setText(R.string.login_send_code);
                return;
            }
            ForgotPasswordActivity.this.sendCodeButton.setText(
                    ForgotPasswordActivity.this.getString(
                            R.string.login_send_code_countdown,
                            ForgotPasswordActivity.this.remainingSeconds));
            ForgotPasswordActivity.this.handler.postDelayed(this, 1000L);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        this.setContentView(R.layout.activity_forgot_password);
        ViewCompat.setOnApplyWindowInsetsListener(this.findViewById(R.id.forgot_password_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int pad = Math.round(20 * this.getResources().getDisplayMetrics().density);
            v.setPadding(systemBars.left + pad, systemBars.top + pad,
                    systemBars.right + pad, systemBars.bottom + pad);
            return insets;
        });

        this.authApi = AuthApiProvider.get();
        this.bindViews();

        String phone = this.getIntent().getStringExtra(EXTRA_PHONE);
        if (phone != null) {
            this.phoneInput.setText(phone);
        }

        this.findViewById(R.id.tv_forgot_back).setOnClickListener(v -> this.finish());
        this.sendCodeButton.setOnClickListener(v -> this.sendCode());
        this.submitButton.setOnClickListener(v -> this.submit());
    }

    private void bindViews() {
        this.phoneLayout = this.findViewById(R.id.til_forgot_phone);
        this.codeLayout = this.findViewById(R.id.til_forgot_code);
        this.passwordLayout = this.findViewById(R.id.til_forgot_password);
        this.confirmLayout = this.findViewById(R.id.til_forgot_confirm);
        this.phoneInput = this.findViewById(R.id.et_forgot_phone);
        this.codeInput = this.findViewById(R.id.et_forgot_code);
        this.passwordInput = this.findViewById(R.id.et_forgot_password);
        this.confirmInput = this.findViewById(R.id.et_forgot_confirm);
        this.sendCodeButton = this.findViewById(R.id.btn_forgot_send_code);
        this.submitButton = this.findViewById(R.id.btn_forgot_submit);
        this.progress = this.findViewById(R.id.progress_forgot);
    }

    private void sendCode() {
        if (this.countdownRunning) {
            return;
        }
        String phone = this.text(this.phoneInput);
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            this.phoneLayout.setError(this.getString(R.string.login_phone_invalid));
            return;
        }
        this.phoneLayout.setError(null);
        this.sendCodeButton.setEnabled(false);
        this.authApi.getSmsCode(phone, result -> {
            if (!result.isSuccess()) {
                this.sendCodeButton.setEnabled(true);
                this.showError(result.getError());
                return;
            }
            Toast.makeText(this, R.string.login_code_sent, Toast.LENGTH_SHORT).show();
            this.startCountdown();
        });
    }

    private void startCountdown() {
        this.countdownRunning = true;
        this.remainingSeconds = COUNTDOWN_SECONDS;
        this.sendCodeButton.setEnabled(false);
        this.sendCodeButton.setText(this.getString(
                R.string.login_send_code_countdown, this.remainingSeconds));
        this.handler.postDelayed(this.countdownRunnable, 1000L);
    }

    private void submit() {
        String phone = this.text(this.phoneInput);
        String code = this.text(this.codeInput);
        String password = this.text(this.passwordInput);
        String confirm = this.text(this.confirmInput);

        this.phoneLayout.setError(null);
        this.codeLayout.setError(null);
        this.passwordLayout.setError(null);
        this.confirmLayout.setError(null);

        if (!PHONE_PATTERN.matcher(phone).matches()) {
            this.phoneLayout.setError(this.getString(R.string.login_phone_invalid));
            return;
        }
        if (!CODE_PATTERN.matcher(code).matches()) {
            this.codeLayout.setError(this.getString(R.string.login_code_invalid));
            return;
        }
        if (password.isEmpty()) {
            this.passwordLayout.setError(this.getString(R.string.login_password_empty));
            return;
        }
        if (password.length() > PASSWORD_MAX_LENGTH) {
            this.passwordLayout.setError(this.getString(R.string.forgot_password_password_invalid));
            return;
        }
        if (!password.equals(confirm)) {
            this.confirmLayout.setError(this.getString(R.string.forgot_password_confirm_mismatch));
            return;
        }

        this.setSubmitting(true);
        this.authApi.resetPassword(phone, code, password, result -> {
            this.setSubmitting(false);
            if (!result.isSuccess()) {
                this.showError(result.getError());
                return;
            }
            Toast.makeText(this, R.string.forgot_password_success, Toast.LENGTH_LONG).show();
            this.finish();
        });
    }

    private void setSubmitting(boolean submitting) {
        this.submitButton.setEnabled(!submitting);
        this.submitButton.setText(submitting
                ? R.string.forgot_password_submitting : R.string.forgot_password_submit);
        this.progress.setVisibility(submitting ? View.VISIBLE : View.GONE);
    }

    private void showError(AuthError error) {
        String message = error == null ? null : error.getMessage();
        if (message == null || message.isEmpty()) {
            message = this.getString(R.string.forgot_password_failed);
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private String text(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    @Override
    protected void onDestroy() {
        this.handler.removeCallbacks(this.countdownRunnable);
        super.onDestroy();
    }
}
