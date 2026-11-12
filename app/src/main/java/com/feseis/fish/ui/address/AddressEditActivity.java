package com.feseis.fish.ui.address;

import com.feseis.fish.R;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.feseis.fish.core.api.ApiResult;
import com.feseis.fish.core.model.AuthError;
import com.feseis.fish.auth.model.LoginResult;
import com.feseis.fish.auth.store.SessionStore;
import com.feseis.fish.logistics.api.LogisticsApi;
import com.feseis.fish.logistics.api.LogisticsApiProvider;
import com.feseis.fish.logistics.model.AddressDraft;
import com.feseis.fish.logistics.model.Poi;
import com.feseis.fish.logistics.model.UserAddress;
import com.feseis.fish.logistics.store.DefaultAddressStore;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import java.util.regex.Pattern;

/**
 * 新增 / 编辑地址。地理部分必须通过 POI 选择页获取，对应 D-022 的录入链路。
 */
public class AddressEditActivity extends AppCompatActivity {

    private static final String EXTRA_ADDRESS = "extra_address";
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    private LogisticsApi api;
    private String accountId = "";
    private UserAddress editing;
    private Poi poi;
    private String tag = "";

    private TextView pickView;
    private TextInputEditText detailInput;
    private TextInputEditText nameInput;
    private TextInputEditText phoneInput;
    private MaterialSwitch defaultSwitch;
    private MaterialButton saveButton;
    private CircularProgressIndicator progress;

    private final ActivityResultLauncher<Intent> pickLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                    return;
                }
                Object extra = result.getData().getSerializableExtra(PoiPickerActivity.EXTRA_POI);
                if (extra instanceof Poi) {
                    this.poi = (Poi) extra;
                    this.renderPoi();
                }
            });

    public static Intent newIntent(Context context, UserAddress address) {
        Intent intent = new Intent(context, AddressEditActivity.class);
        if (address != null) {
            intent.putExtra(EXTRA_ADDRESS, address);
        }
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        this.setContentView(R.layout.activity_address_edit);
        ViewCompat.setOnApplyWindowInsetsListener(this.findViewById(R.id.address_edit_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        this.api = LogisticsApiProvider.get(this);
        LoginResult session = SessionStore.get().getSession();
        if (session == null) {
            Toast.makeText(this, R.string.address_error_unauthorized, Toast.LENGTH_SHORT).show();
            this.finish();
            return;
        }
        this.accountId = session.getAccountId();
        this.editing = (UserAddress) this.getIntent().getSerializableExtra(EXTRA_ADDRESS);

        TextView title = this.findViewById(R.id.tv_edit_title);
        title.setText(this.editing == null
                ? R.string.address_edit_title_new : R.string.address_edit_title_edit);

        TextView back = this.findViewById(R.id.tv_edit_back);
        back.setOnClickListener(v -> this.finish());
        TextView clear = this.findViewById(R.id.tv_edit_clear);
        clear.setOnClickListener(v -> this.clearForm());

        this.pickView = this.findViewById(R.id.tv_pick_address);
        this.detailInput = this.findViewById(R.id.et_edit_detail);
        this.nameInput = this.findViewById(R.id.et_edit_name);
        this.phoneInput = this.findViewById(R.id.et_edit_phone);
        this.defaultSwitch = this.findViewById(R.id.switch_edit_default);
        this.saveButton = this.findViewById(R.id.btn_save_address);
        this.progress = this.findViewById(R.id.progress_edit_address);

        this.findViewById(R.id.row_pick_address).setOnClickListener(v ->
                this.pickLauncher.launch(new Intent(this, PoiPickerActivity.class)));
        this.saveButton.setOnClickListener(v -> this.save());

        if (this.editing != null) {
            this.tag = this.editing.getTag() == null ? "" : this.editing.getTag();
            this.poi = new Poi(this.editing.getPoiId(), this.editing.getPoiName(),
                    this.editing.getAdcode(), this.editing.getProvince(), this.editing.getCity(),
                    this.editing.getDistrict(), "", this.editing.getLng(), this.editing.getLat());
            this.detailInput.setText(this.editing.getDetailAddr());
            this.nameInput.setText(this.editing.getContactName());
            this.phoneInput.setText(this.editing.getContactPhone());
            this.defaultSwitch.setChecked(this.editing.getUserAddressId()
                    .equals(DefaultAddressStore.get(this, this.accountId)));
        }
        this.renderPoi();
    }

    private void renderPoi() {
        if (this.poi == null) {
            this.pickView.setText(R.string.address_edit_pick_hint);
            this.pickView.setTextColor(ContextCompat.getColor(this, R.color.fish_text_secondary));
        } else {
            this.pickView.setText(this.poi.summary());
            this.pickView.setTextColor(ContextCompat.getColor(this, R.color.fish_text_primary));
        }
    }

    private void clearForm() {
        this.poi = null;
        this.tag = "";
        this.detailInput.setText("");
        this.nameInput.setText("");
        this.phoneInput.setText("");
        this.defaultSwitch.setChecked(false);
        this.renderPoi();
    }

    private void save() {
        String detail = text(this.detailInput);
        String name = text(this.nameInput);
        String phone = text(this.phoneInput);

        if (this.poi == null) {
            Toast.makeText(this, R.string.address_error_pick_required, Toast.LENGTH_SHORT).show();
            return;
        }
        if (detail.isEmpty()) {
            Toast.makeText(this, R.string.address_error_detail_required, Toast.LENGTH_SHORT).show();
            return;
        }
        if (name.isEmpty()) {
            Toast.makeText(this, R.string.address_error_name_required, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            Toast.makeText(this, R.string.address_error_phone_invalid, Toast.LENGTH_SHORT).show();
            return;
        }

        AddressDraft draft = new AddressDraft(this.poi.getPoiId(), this.poi.getPoiName(),
                this.poi.getAdcode(), this.poi.getProvince(), this.poi.getCity(),
                this.poi.getDistrict(), this.poi.getLng(), this.poi.getLat(),
                name, phone, detail, this.tag);

        this.setSaving(true);
        if (this.editing == null) {
            this.api.createAddress(draft, this::onSaved);
        } else {
            this.api.updateAddress(this.editing.getUserAddressId(), draft, this::onSaved);
        }
    }

    private void onSaved(ApiResult<UserAddress> result) {
        this.setSaving(false);
        if (!result.isSuccess()) {
            Toast.makeText(this, this.describe(result.getError()), Toast.LENGTH_SHORT).show();
            return;
        }
        UserAddress saved = result.getData();
        if (this.defaultSwitch.isChecked()) {
            DefaultAddressStore.set(this, this.accountId, saved.getUserAddressId());
        } else {
            DefaultAddressStore.clearIfMatches(this, this.accountId, saved.getUserAddressId());
        }
        this.setResult(RESULT_OK);
        this.finish();
    }

    private void setSaving(boolean saving) {
        this.saveButton.setEnabled(!saving);
        this.saveButton.setText(saving ? R.string.address_edit_saving : R.string.address_edit_save);
        this.progress.setVisibility(saving ? View.VISIBLE : View.GONE);
    }

    private String describe(AuthError error) {
        if (error == null) {
            return this.getString(R.string.address_error_generic, "");
        }
        if ("ADDRESS_DUPLICATED".equals(error.getCode())) {
            return this.getString(R.string.address_error_duplicated);
        }
        if (AuthError.UNAUTHORIZED.equals(error.getCode())
                || AuthError.TOKEN_EXPIRED.equals(error.getCode())) {
            return this.getString(R.string.address_error_unauthorized);
        }
        return this.getString(R.string.address_error_generic, error.getMessage());
    }

    private static String text(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }
}
