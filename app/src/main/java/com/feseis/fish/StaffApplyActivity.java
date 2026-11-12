package com.feseis.fish;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.feseis.fish.auth.model.AuthError;
import com.feseis.fish.auth.model.LoginResult;
import com.feseis.fish.auth.store.SessionStore;
import com.feseis.fish.logistics.api.LogisticsApiProvider;
import com.feseis.fish.logistics.model.StaffApplyResult;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.regex.Pattern;

/**
 * 成为员工：输入培训凭证码，提交入职申请。
 */
public class StaffApplyActivity extends AppCompatActivity {

    private static final Pattern VOUCHER_PATTERN = Pattern.compile("^[A-Za-z0-9]{12}$");

    private TextInputLayout voucherLayout;
    private TextInputEditText voucherInput;
    private TextInputEditText remarkInput;
    private MaterialButton submitButton;
    private CircularProgressIndicator progress;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        this.setContentView(R.layout.activity_staff_apply);
        ViewCompat.setOnApplyWindowInsetsListener(this.findViewById(R.id.staff_apply_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        this.voucherLayout = this.findViewById(R.id.til_staff_voucher);
        this.voucherInput = this.findViewById(R.id.et_staff_voucher);
        this.remarkInput = this.findViewById(R.id.et_staff_remark);
        this.submitButton = this.findViewById(R.id.btn_staff_submit);
        this.progress = this.findViewById(R.id.progress_staff);

        TextView accountView = this.findViewById(R.id.tv_staff_account);
        LoginResult session = SessionStore.get().getSession();
        if (session != null) {
            accountView.setText(this.getString(R.string.staff_apply_account_format,
                    maskPhone(session.getPhone())));
        }

        this.findViewById(R.id.tv_staff_back).setOnClickListener(v -> this.finish());
        this.submitButton.setOnClickListener(v -> this.submit());
        this.voucherInput.addTextChangedListener(new SimpleWatcher(() -> this.voucherLayout.setError(null)));
    }

    private void submit() {
        String voucherCode = this.text(this.voucherInput).trim().toUpperCase();
        if (!VOUCHER_PATTERN.matcher(voucherCode).matches()) {
            this.voucherLayout.setError(this.getString(R.string.staff_apply_voucher_invalid));
            return;
        }

        this.setSubmitting(true);
        String remark = this.text(this.remarkInput).trim();
        LogisticsApiProvider.get(this).applyStaff(voucherCode, remark, result -> {
            if (this.isFinishing() || this.isDestroyed()) {
                return;
            }
            this.setSubmitting(false);
            if (result.isSuccess()) {
                StaffApplyResult data = result.getData();
                Toast.makeText(this,
                        this.getString(R.string.staff_apply_success_format,
                                data == null ? "" : data.getStaffNo()),
                        Toast.LENGTH_LONG).show();
                this.finish();
                return;
            }
            AuthError error = result.getError();
            String message = error == null || error.getMessage() == null ? "" : error.getMessage();
            if (error != null && "VOUCHER_NOT_FOUND".equals(error.getCode())) {
                this.voucherLayout.setError(message);
                return;
            }
            if (error != null && ("VOUCHER_EXPIRED".equals(error.getCode())
                    || "VOUCHER_ACCOUNT_MISMATCH".equals(error.getCode()))) {
                this.voucherLayout.setError(message);
                return;
            }
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
    }

    private void setSubmitting(boolean submitting) {
        this.submitButton.setEnabled(!submitting);
        this.submitButton.setText(submitting
                ? R.string.staff_apply_submitting : R.string.staff_apply_submit);
        this.progress.setVisibility(submitting ? View.VISIBLE : View.GONE);
        this.voucherInput.setEnabled(!submitting);
        this.remarkInput.setEnabled(!submitting);
    }

    private String text(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone == null ? "" : phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    /** 只做清错误提示的简化 TextWatcher。 */
    private static final class SimpleWatcher implements android.text.TextWatcher {
        private final Runnable action;

        private SimpleWatcher(Runnable action) {
            this.action = action;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(android.text.Editable s) {
            this.action.run();
        }
    }
}
