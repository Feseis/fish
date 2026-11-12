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

import com.feseis.fish.core.model.AuthError;
import com.feseis.fish.logistics.api.LogisticsApi;
import com.feseis.fish.logistics.api.LogisticsApiProvider;
import com.feseis.fish.logistics.model.AddressDraft;
import com.feseis.fish.logistics.model.Poi;
import com.feseis.fish.logistics.model.UserAddress;
import com.feseis.fish.logistics.store.ShipmentDraftStore;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import java.util.regex.Pattern;

/**
 * 一次性地址填写页：手动选点或从地址簿回填，默认不写地址簿。
 */
public class AddressEntryActivity extends AppCompatActivity {

    private static final String EXTRA_TARGET = "extra_entry_target";
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    private LogisticsApi api;
    private int target = AddressBookActivity.TARGET_SENDER;
    private Poi poi;

    private TextView addressView;
    private TextInputEditText detailInput;
    private TextInputEditText nameInput;
    private TextInputEditText phoneInput;
    private MaterialSwitch saveSwitch;
    private MaterialButton confirmButton;
    private CircularProgressIndicator progress;

    private final ActivityResultLauncher<Intent> poiLauncher =
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

    private final ActivityResultLauncher<Intent> bookLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                    return;
                }
                Object extra = result.getData().getSerializableExtra(AddressBookActivity.EXTRA_ADDRESS);
                if (extra instanceof UserAddress) {
                    this.fillFromAddress((UserAddress) extra);
                }
            });

    public static Intent newIntent(Context context, int target) {
        Intent intent = new Intent(context, AddressEntryActivity.class);
        intent.putExtra(EXTRA_TARGET, target);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        this.setContentView(R.layout.activity_address_entry);
        ViewCompat.setOnApplyWindowInsetsListener(this.findViewById(R.id.address_entry_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int pad = Math.round(20 * this.getResources().getDisplayMetrics().density);
            v.setPadding(systemBars.left + pad, systemBars.top + pad,
                    systemBars.right + pad, systemBars.bottom + pad);
            return insets;
        });

        this.api = LogisticsApiProvider.get(this);
        this.target = this.getIntent().getIntExtra(EXTRA_TARGET, AddressBookActivity.TARGET_SENDER);

        TextView title = this.findViewById(R.id.tv_entry_title);
        title.setText(this.target == AddressBookActivity.TARGET_SENDER
                ? R.string.address_entry_title_sender : R.string.address_entry_title_receiver);

        TextView back = this.findViewById(R.id.tv_entry_back);
        back.setOnClickListener(v -> this.finish());

        this.addressView = this.findViewById(R.id.tv_entry_address);
        this.detailInput = this.findViewById(R.id.et_entry_detail);
        this.nameInput = this.findViewById(R.id.et_entry_name);
        this.phoneInput = this.findViewById(R.id.et_entry_phone);
        this.saveSwitch = this.findViewById(R.id.switch_entry_save);
        this.confirmButton = this.findViewById(R.id.btn_entry_confirm);
        this.progress = this.findViewById(R.id.progress_entry);

        this.findViewById(R.id.row_entry_address).setOnClickListener(v ->
                this.poiLauncher.launch(new Intent(this, PoiPickerActivity.class)));
        this.findViewById(R.id.tv_entry_book).setOnClickListener(v ->
                this.bookLauncher.launch(AddressBookActivity.newPickIntent(this, this.target)));
        this.confirmButton.setOnClickListener(v -> this.confirm());

        UserAddress existing = this.currentDraft();
        if (existing != null) {
            this.fillFromAddress(existing);
        }
        this.renderPoi();
    }

    private UserAddress currentDraft() {
        ShipmentDraftStore draft = ShipmentDraftStore.get();
        return this.target == AddressBookActivity.TARGET_SENDER ? draft.getSender() : draft.getReceiver();
    }

    private void fillFromAddress(UserAddress address) {
        this.poi = new Poi(address.getPoiId(), address.getPoiName(), address.getAdcode(),
                address.getProvince(), address.getCity(), address.getDistrict(), "",
                address.getLng(), address.getLat());
        this.detailInput.setText(address.getDetailAddr());
        this.nameInput.setText(address.getContactName());
        this.phoneInput.setText(address.getContactPhone());
        this.renderPoi();
    }

    private void renderPoi() {
        if (this.poi == null) {
            this.addressView.setText(R.string.address_entry_address_hint);
            this.addressView.setTextColor(ContextCompat.getColor(this, R.color.fish_text_secondary));
        } else {
            this.addressView.setText(this.poi.summary());
            this.addressView.setTextColor(ContextCompat.getColor(this, R.color.fish_text_primary));
        }
    }

    private void confirm() {
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

        if (!this.saveSwitch.isChecked()) {
            this.apply(null, detail, name, phone);
            return;
        }

        AddressDraft draft = new AddressDraft(this.poi.getPoiId(), this.poi.getPoiName(),
                this.poi.getAdcode(), this.poi.getProvince(), this.poi.getCity(),
                this.poi.getDistrict(), this.poi.getLng(), this.poi.getLat(),
                name, phone, detail, "");
        this.setSaving(true);
        this.api.createAddress(draft, result -> {
            this.setSaving(false);
            if (result.isSuccess()) {
                Toast.makeText(this, R.string.address_entry_saved_to_book, Toast.LENGTH_SHORT).show();
                this.apply(result.getData(), detail, name, phone);
                return;
            }
            AuthError error = result.getError();
            if (error != null && "ADDRESS_DUPLICATED".equals(error.getCode())) {
                this.apply(null, detail, name, phone);
                return;
            }
            Toast.makeText(this, this.describe(error), Toast.LENGTH_SHORT).show();
        });
    }

    /** saved 非空表示刚写入地址簿，带真实 ID；否则为仅本次使用的本地地址。 */
    private void apply(UserAddress saved, String detail, String name, String phone) {
        UserAddress address = saved != null ? saved : new UserAddress(
                "",
                this.poi.getPoiId(),
                this.poi.getPoiName(),
                this.poi.getAdcode(),
                this.poi.getProvince(),
                this.poi.getCity(),
                this.poi.getDistrict(),
                this.poi.getLng(),
                this.poi.getLat(),
                name,
                phone,
                detail,
                "",
                "");
        ShipmentDraftStore draft = ShipmentDraftStore.get();
        if (this.target == AddressBookActivity.TARGET_SENDER) {
            draft.setSender(address);
        } else {
            draft.setReceiver(address);
        }
        this.setResult(RESULT_OK);
        this.finish();
    }

    private void setSaving(boolean saving) {
        this.confirmButton.setEnabled(!saving);
        this.confirmButton.setText(saving
                ? R.string.address_entry_confirming : R.string.address_entry_confirm);
        this.progress.setVisibility(saving ? View.VISIBLE : View.GONE);
    }

    private String describe(AuthError error) {
        if (error == null) {
            return this.getString(R.string.address_error_generic, "");
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
