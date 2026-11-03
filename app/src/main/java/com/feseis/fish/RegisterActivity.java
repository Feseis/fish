package com.feseis.fish;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.feseis.fish.auth.api.AuthApi;
import com.feseis.fish.auth.api.AuthApiProvider;
import com.feseis.fish.auth.api.MockAuthApi;
import com.feseis.fish.auth.model.ApiCallback;
import com.feseis.fish.auth.model.AuthError;
import com.feseis.fish.auth.model.LoginResult;
import com.feseis.fish.auth.store.SessionStore;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.regex.Pattern;

/**
 * 注册页：手机号 + 验证码 + 密码 + 确认密码，注册成功即自动登录。
 */
public class RegisterActivity extends AppCompatActivity {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern CODE_PATTERN = Pattern.compile("^\\d{6}$");
    private static final int COUNTDOWN_SECONDS = 60;
    private static final int PASSWORD_MIN_LENGTH = 8;
    private static final int PASSWORD_MAX_LENGTH = 64;

    private TextInputLayout phoneLayout;
    private TextInputLayout codeLayout;
    private TextInputLayout passwordLayout;
    private TextInputLayout confirmLayout;
    private TextInputEditText phoneInput;
    private TextInputEditText codeInput;
    private TextInputEditText passwordInput;
    private TextInputEditText confirmInput;
    private View rootView;
    private MaterialButton sendCodeButton;
    private MaterialButton registerButton;
    private CircularProgressIndicator registerProgress;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable countdownRunnable = new Runnable() {
        @Override
        public void run() {
            RegisterActivity.this.remainingSeconds--;
            if (RegisterActivity.this.remainingSeconds <= 0) {
                RegisterActivity.this.stopCountdown();
                return;
            }
            RegisterActivity.this.updateSendCodeText();
            RegisterActivity.this.handler.postDelayed(this, 1000L);
        }
    };

    private AuthApi authApi;
    private int remainingSeconds;
    private boolean countdownRunning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        this.setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(this.findViewById(R.id.register_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        this.authApi = AuthApiProvider.get();
        this.bindViews();
        this.setupInputs();
        this.setupSendCode();
        this.setupRegister();
        this.findViewById(R.id.tv_back_login).setOnClickListener(v -> this.finish());
    }

    @Override
    protected void onDestroy() {
        this.handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void bindViews() {
        this.phoneLayout = this.findViewById(R.id.til_register_phone);
        this.codeLayout = this.findViewById(R.id.til_register_code);
        this.passwordLayout = this.findViewById(R.id.til_register_password);
        this.confirmLayout = this.findViewById(R.id.til_register_confirm);
        this.phoneInput = this.findViewById(R.id.et_register_phone);
        this.codeInput = this.findViewById(R.id.et_register_code);
        this.passwordInput = this.findViewById(R.id.et_register_password);
        this.confirmInput = this.findViewById(R.id.et_register_confirm);
        this.rootView = this.findViewById(R.id.register_root);
        this.sendCodeButton = this.findViewById(R.id.btn_register_send_code);
        this.registerButton = this.findViewById(R.id.btn_register);
        this.registerProgress = this.findViewById(R.id.progress_register);
    }

    private void setupInputs() {
        this.clearErrorOnInput(this.phoneInput, this.phoneLayout);
        this.clearErrorOnInput(this.codeInput, this.codeLayout);
        this.clearErrorOnInput(this.passwordInput, this.passwordLayout);
        this.clearErrorOnInput(this.confirmInput, this.confirmLayout);
    }

    private void clearErrorOnInput(TextInputEditText input, TextInputLayout layout) {
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                layout.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setupSendCode() {
        this.sendCodeButton.setOnClickListener(v -> {
            if (this.countdownRunning) {
                return;
            }
            if (!this.isPhoneValid()) {
                this.phoneLayout.setError(this.getString(R.string.login_phone_invalid));
                return;
            }
            this.sendCodeButton.setEnabled(false);
            this.authApi.getSmsCode(this.phone(), result -> {
                if (this.isFinishing() || this.isDestroyed()) {
                    return;
                }
                if (result.isSuccess()) {
                    Toast.makeText(this,
                            this.getString(R.string.login_code_sent_format, MockAuthApi.MOCK_SMS_CODE),
                            Toast.LENGTH_SHORT).show();
                    this.startCountdown();
                } else {
                    this.sendCodeButton.setEnabled(true);
                    this.showAuthError(result.getError());
                }
            });
        });
    }

    private void setupRegister() {
        this.registerButton.setOnClickListener(v -> {
            if (!this.validateForSubmit()) {
                return;
            }
            this.setSubmitting(true);
            ApiCallback<LoginResult> callback = result -> {
                if (this.isFinishing() || this.isDestroyed()) {
                    return;
                }
                this.setSubmitting(false);
                if (result.isSuccess()) {
                    SessionStore.get().save(result.getData());
                    Toast.makeText(this, R.string.register_success, Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    this.startActivity(intent);
                    this.finish();
                } else {
                    this.showAuthError(result.getError());
                }
            };
            this.authApi.register(this.phone(), this.code(), this.text(this.passwordInput),
                    this.text(this.confirmInput), callback);
        });
    }

    private boolean validateForSubmit() {
        if (!this.isPhoneValid()) {
            this.phoneLayout.setError(this.getString(R.string.login_phone_invalid));
            return false;
        }
        if (!CODE_PATTERN.matcher(this.code()).matches()) {
            this.codeLayout.setError(this.getString(R.string.login_code_invalid));
            return false;
        }
        String password = this.text(this.passwordInput);
        if (password.length() < PASSWORD_MIN_LENGTH || password.length() > PASSWORD_MAX_LENGTH) {
            this.passwordLayout.setError(this.getString(R.string.register_password_invalid));
            return false;
        }
        if (!password.equals(this.text(this.confirmInput))) {
            this.confirmLayout.setError(this.getString(R.string.register_confirm_mismatch));
            return false;
        }
        return true;
    }

    private void setSubmitting(boolean submitting) {
        this.registerButton.setEnabled(!submitting);
        this.registerButton.setText(submitting ? R.string.register_submitting : R.string.register_action);
        this.registerProgress.setVisibility(submitting ? View.VISIBLE : View.GONE);
        this.sendCodeButton.setEnabled(!submitting && !this.countdownRunning);
        this.phoneInput.setEnabled(!submitting);
        this.codeInput.setEnabled(!submitting);
        this.passwordInput.setEnabled(!submitting);
        this.confirmInput.setEnabled(!submitting);
    }

    private void showAuthError(AuthError error) {
        if (error == null) {
            return;
        }
        String code = error.getCode();
        String message = error.getMessage() == null ? "" : error.getMessage();
        if (AuthError.PHONE_EXISTS.equals(code)) {
            this.phoneLayout.setError(message);
            return;
        }
        if (AuthError.SMS_WRONG.equals(code) || AuthError.SMS_EXPIRED.equals(code)) {
            this.codeLayout.setError(message);
            return;
        }
        if (AuthError.INVALID_PARAM.equals(code)) {
            this.confirmLayout.setError(message);
            return;
        }
        Snackbar.make(this.rootView, message, Snackbar.LENGTH_SHORT).show();
    }

    private void startCountdown() {
        this.remainingSeconds = COUNTDOWN_SECONDS;
        this.countdownRunning = true;
        this.sendCodeButton.setEnabled(false);
        this.updateSendCodeText();
        this.handler.postDelayed(this.countdownRunnable, 1000L);
    }

    private void stopCountdown() {
        this.countdownRunning = false;
        this.handler.removeCallbacks(this.countdownRunnable);
        this.sendCodeButton.setEnabled(true);
        this.sendCodeButton.setText(R.string.login_send_code);
    }

    private void updateSendCodeText() {
        this.sendCodeButton.setText(this.getString(R.string.login_send_code_countdown, this.remainingSeconds));
    }

    private String phone() {
        return this.text(this.phoneInput).trim();
    }

    private String code() {
        return this.text(this.codeInput).trim();
    }

    private String text(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }

    private boolean isPhoneValid() {
        return PHONE_PATTERN.matcher(this.phone()).matches();
    }
}
