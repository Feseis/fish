package com.feseis.fish.ui.order;

import com.feseis.fish.R;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.feseis.fish.core.model.AuthError;
import com.feseis.fish.logistics.api.LogisticsApi;
import com.feseis.fish.logistics.api.LogisticsApiProvider;
import com.feseis.fish.logistics.model.PriceQuote;
import com.feseis.fish.logistics.model.UserAddress;
import com.feseis.fish.logistics.store.ShipmentDraftStore;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 填写寄件信息：展示寄收地址摘要，填写物品信息并试算运费。
 * 下单与支付在下一轮接入。
 */
public class OrderCreateActivity extends AppCompatActivity {

    private static final Pattern ADCODE_PATTERN = Pattern.compile("^\\d{6}$");
    private static final Pattern WEIGHT_PATTERN = Pattern.compile("^\\d{1,7}(\\.\\d{1,3})?$");
    private static final Pattern VALUE_PATTERN = Pattern.compile("^\\d{1,8}(\\.\\d{1,2})?$");

    private LogisticsApi api;
    private UserAddress sender;
    private UserAddress receiver;

    private TextInputEditText weightInput;
    private TextInputLayout weightLayout;
    private TextInputEditText declaredInput;
    private TextInputLayout declaredLayout;
    private View quoteCard;
    private TextView freightView;
    private TextView insuredFeeView;
    private TextView totalView;
    private MaterialButton quoteButton;
    private CircularProgressIndicator progress;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        this.setContentView(R.layout.activity_order_create);
        ViewCompat.setOnApplyWindowInsetsListener(this.findViewById(R.id.order_create_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int pad = Math.round(20 * this.getResources().getDisplayMetrics().density);
            v.setPadding(systemBars.left + pad, systemBars.top + pad,
                    systemBars.right + pad, systemBars.bottom + pad);
            return insets;
        });

        this.api = LogisticsApiProvider.get(this);
        ShipmentDraftStore draft = ShipmentDraftStore.get();
        this.sender = draft.getSender();
        this.receiver = draft.getReceiver();

        this.bindViews();
        if (!this.isAddressUsable(this.sender) || !this.isAddressUsable(this.receiver)) {
            Toast.makeText(this, R.string.order_create_error_address, Toast.LENGTH_LONG).show();
            this.finish();
            return;
        }
        this.renderSummary();
    }

    private void bindViews() {
        this.findViewById(R.id.tv_order_create_back).setOnClickListener(v -> this.finish());

        this.weightInput = this.findViewById(R.id.et_order_weight);
        this.weightLayout = this.findViewById(R.id.til_order_weight);
        this.declaredInput = this.findViewById(R.id.et_order_declared);
        this.declaredLayout = this.findViewById(R.id.til_order_declared);
        this.quoteCard = this.findViewById(R.id.card_order_quote);
        this.freightView = this.findViewById(R.id.tv_order_freight);
        this.insuredFeeView = this.findViewById(R.id.tv_order_insured_fee);
        this.totalView = this.findViewById(R.id.tv_order_total);
        this.quoteButton = this.findViewById(R.id.btn_order_quote);
        this.progress = this.findViewById(R.id.progress_order_quote);

        this.quoteButton.setOnClickListener(v -> this.quote());
        this.hideQuoteOnEdit(this.weightInput);
        this.hideQuoteOnEdit(this.declaredInput);
    }

    private void renderSummary() {
        TextView senderAddress = this.findViewById(R.id.tv_order_sender_address);
        TextView senderContact = this.findViewById(R.id.tv_order_sender_contact);
        TextView receiverAddress = this.findViewById(R.id.tv_order_receiver_address);
        TextView receiverContact = this.findViewById(R.id.tv_order_receiver_contact);

        senderAddress.setText(this.sender.fullAddress());
        senderContact.setText(this.contactOf(this.sender));
        receiverAddress.setText(this.receiver.fullAddress());
        receiverContact.setText(this.contactOf(this.receiver));
    }

    private void quote() {
        this.weightLayout.setError(null);
        this.declaredLayout.setError(null);

        BigDecimal weight = this.readWeight();
        if (weight == null) {
            return;
        }
        BigDecimal declaredValue = this.readDeclaredValue();
        if (declaredValue == null) {
            return;
        }

        this.setLoading(true);
        this.api.quote(this.sender.getAdcode(), this.receiver.getAdcode(),
                weight.doubleValue(), declaredValue.doubleValue(), result -> {
                    this.setLoading(false);
                    if (result.isSuccess()) {
                        this.renderQuote(result.getData());
                        return;
                    }
                    Toast.makeText(this, this.describe(result.getError()), Toast.LENGTH_LONG).show();
                });
    }

    private BigDecimal readWeight() {
        String raw = text(this.weightInput);
        if (!WEIGHT_PATTERN.matcher(raw).matches() || new BigDecimal(raw).signum() <= 0) {
            this.weightLayout.setError(this.getString(R.string.order_create_error_weight));
            return null;
        }
        return new BigDecimal(raw);
    }

    private BigDecimal readDeclaredValue() {
        String raw = text(this.declaredInput);
        if (raw.isEmpty()) {
            return BigDecimal.ZERO;
        }
        if (!VALUE_PATTERN.matcher(raw).matches()) {
            this.declaredLayout.setError(this.getString(R.string.order_create_error_declared));
            return null;
        }
        return new BigDecimal(raw);
    }

    private void renderQuote(PriceQuote quote) {
        this.freightView.setText(money(quote.getFreightAmount()));
        this.insuredFeeView.setText(money(quote.getInsuredFee()));
        this.totalView.setText(money(quote.getTotalAmount()));
        this.quoteCard.setVisibility(View.VISIBLE);
    }

    private void setLoading(boolean loading) {
        this.quoteButton.setEnabled(!loading);
        this.quoteButton.setText(loading
                ? R.string.order_create_quoting : R.string.order_create_quote);
        this.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private String describe(AuthError error) {
        if (error == null) {
            return this.getString(R.string.order_create_error_generic, "");
        }
        switch (error.getCode()) {
            case "PRICE_RULE_MISSING":
                return this.getString(R.string.order_create_error_price_rule);
            case "INSURANCE_RULE_MISSING":
                return this.getString(R.string.order_create_error_insurance_rule);
            case "INSURANCE_LIMIT_EXCEEDED":
                return this.getString(R.string.order_create_error_insurance_limit);
            case AuthError.UNAUTHORIZED:
            case AuthError.TOKEN_EXPIRED:
                return this.getString(R.string.order_create_error_unauthorized);
            default:
                String message = error.getMessage() == null ? "" : error.getMessage();
                return this.getString(R.string.order_create_error_generic, message);
        }
    }

    private boolean isAddressUsable(UserAddress address) {
        return address != null
                && ADCODE_PATTERN.matcher(address.getAdcode()).matches()
                && !address.getProvince().isEmpty()
                && !address.getCity().isEmpty()
                && !address.getDistrict().isEmpty()
                && !address.getPoiId().isEmpty();
    }

    private void hideQuoteOnEdit(TextInputEditText input) {
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                OrderCreateActivity.this.quoteCard.setVisibility(View.GONE);
            }
        });
    }

    private String contactOf(UserAddress address) {
        return address.getContactName() + "  " + address.getContactPhone();
    }

    private static String text(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private static String money(double amount) {
        return String.format(Locale.CHINA, "¥%.2f", amount);
    }
}
