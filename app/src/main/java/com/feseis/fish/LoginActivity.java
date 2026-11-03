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
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.regex.Pattern;

public class LoginActivity extends AppCompatActivity {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern CODE_PATTERN = Pattern.compile("^\\d{6}$");
    private static final int COUNTDOWN_SECONDS = 60;
    private static final int PASSWORD_MAX_LENGTH = 64;

    private TextInputLayout phoneLayout;
    private TextInputLayout passwordLayout;
    private TextInputLayout codeLayout;
    private TextInputEditText phoneInput;
    private TextInputEditText passwordInput;
    private TextInputEditText codeInput;
    private MaterialButtonToggleGroup modeGroup;
    private View codeSection;
    private View rootView;
    private MaterialButton sendCodeButton;
    private MaterialButton loginButton;
    private CircularProgressIndicator loginProgress;
    private View registerEntry;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable countdownRunnable = new Runnable() {
        @Override
        public void run() {
            LoginActivity.this.remainingSeconds--;
            if (LoginActivity.this.remainingSeconds <= 0) {
                LoginActivity.this.stopCountdown();
                return;
            }
            LoginActivity.this.updateSendCodeText();
            LoginActivity.this.handler.postDelayed(this, 1000L);
        }
    };

    private AuthApi authApi;
    private int remainingSeconds;
    private boolean countdownRunning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        this.setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(this.findViewById(R.id.login_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        this.authApi = AuthApiProvider.get();
        this.bindViews();
        this.setupModeSwitch();
        this.setupInputs();
        this.setupSendCode();
        this.setupLogin();
        this.registerEntry.setOnClickListener(v ->
                this.startActivity(new Intent(this, RegisterActivity.class)));
    }

    @Override
    protected void onDestroy() {
        this.handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void bindViews() {
        this.phoneLayout = this.findViewById(R.id.til_phone);
        this.passwordLayout = this.findViewById(R.id.til_password);
        this.codeLayout = this.findViewById(R.id.til_code);
        this.phoneInput = this.findViewById(R.id.et_phone);
        this.passwordInput = this.findViewById(R.id.et_password);
        this.codeInput = this.findViewById(R.id.et_code);
        this.modeGroup = this.findViewById(R.id.mode_group);
        this.codeSection = this.findViewById(R.id.code_section);
        this.rootView = this.findViewById(R.id.login_root);
        this.sendCodeButton = this.findViewById(R.id.btn_send_code);
        this.loginButton = this.findViewById(R.id.btn_login);
        this.loginProgress = this.findViewById(R.id.progress_login);
        this.registerEntry = this.findViewById(R.id.tv_register);
    }

    private void setupModeSwitch() {
        this.modeGroup.check(R.id.tab_password);
        this.modeGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            boolean passwordMode = checkedId == R.id.tab_password;
            this.passwordLayout.setVisibility(passwordMode ? View.VISIBLE : View.GONE);
            this.codeSection.setVisibility(passwordMode ? View.GONE : View.VISIBLE);
            this.clearErrors();
        });
    }

    private void setupInputs() {
        this.clearErrorOnInput(this.phoneInput, this.phoneLayout);
        this.clearErrorOnInput(this.passwordInput, this.passwordLayout);
        this.clearErrorOnInput(this.codeInput, this.codeLayout);
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

    private void clearErrors() {
        this.phoneLayout.setError(null);
        this.passwordLayout.setError(null);
        this.codeLayout.setError(null);
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

    private void setupLogin() {
        this.loginButton.setOnClickListener(v -> {
            if (!this.validateForSubmit()) {
                return;
            }
            boolean smsMode = this.modeGroup.getCheckedButtonId() == R.id.tab_code;
            String phone = this.phone();
            String password = this.passwordInput.getText() == null
                    ? "" : this.passwordInput.getText().toString();
            String code = this.codeInput.getText() == null
                    ? "" : this.codeInput.getText().toString().trim();
            this.setSubmitting(true);
            ApiCallback<LoginResult> callback = result -> {
                if (this.isFinishing() || this.isDestroyed()) {
                    return;
                }
                this.setSubmitting(false);
                if (result.isSuccess()) {
                    SessionStore.get().save(result.getData());
                    Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    this.startActivity(intent);
                    this.finish();
                } else {
                    this.showAuthError(result.getError());
                }
            };
            if (smsMode) {
                this.authApi.loginBySms(phone, code, callback);
            } else {
                this.authApi.loginByPassword(phone, password, callback);
            }
        });
    }

    private boolean validateForSubmit() {
        if (!this.isPhoneValid()) {
            this.phoneLayout.setError(this.getString(R.string.login_phone_invalid));
            return false;
        }
        if (this.modeGroup.getCheckedButtonId() == R.id.tab_code) {
            String code = this.codeInput.getText() == null
                    ? "" : this.codeInput.getText().toString().trim();
            if (!CODE_PATTERN.matcher(code).matches()) {
                this.codeLayout.setError(this.getString(R.string.login_code_invalid));
                return false;
            }
            return true;
        }
        String password = this.passwordInput.getText() == null
                ? "" : this.passwordInput.getText().toString();
        if (password.isEmpty()) {
            this.passwordLayout.setError(this.getString(R.string.login_password_empty));
            return false;
        }
        if (password.length() > PASSWORD_MAX_LENGTH) {
            this.passwordLayout.setError(this.getString(R.string.login_password_too_long));
            return false;
        }
        return true;
    }

    private void setSubmitting(boolean submitting) {
        this.loginButton.setEnabled(!submitting);
        this.loginButton.setText(submitting ? R.string.login_submitting : R.string.login_action);
        this.loginProgress.setVisibility(submitting ? View.VISIBLE : View.GONE);
        this.sendCodeButton.setEnabled(!submitting && !this.countdownRunning);
        this.phoneInput.setEnabled(!submitting);
        this.passwordInput.setEnabled(!submitting);
        this.codeInput.setEnabled(!submitting);
        for (int i = 0; i < this.modeGroup.getChildCount(); i++) {
            this.modeGroup.getChildAt(i).setEnabled(!submitting);
        }
    }

    private void showAuthError(AuthError error) {
        if (error == null) {
            return;
        }
        String code = error.getCode();
        String message = error.getMessage() == null ? "" : error.getMessage();
        if (AuthError.PWD_WRONG.equals(code)) {
            this.passwordLayout.setError(message);
            return;
        }
        if (AuthError.SMS_WRONG.equals(code) || AuthError.SMS_EXPIRED.equals(code)) {
            this.codeLayout.setError(message);
            return;
        }
        if (AuthError.NO_ACCOUNT.equals(code)) {
            this.phoneLayout.setError(message);
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
        return this.phoneInput.getText() == null ? "" : this.phoneInput.getText().toString().trim();
    }

    private boolean isPhoneValid() {
        return PHONE_PATTERN.matcher(this.phone()).matches();
    }
}
