package com.feseis.fish.ui.address;

import com.feseis.fish.R;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.feseis.fish.core.model.AuthError;
import com.feseis.fish.auth.model.LoginResult;
import com.feseis.fish.auth.store.SessionStore;
import com.feseis.fish.logistics.api.LogisticsApi;
import com.feseis.fish.logistics.api.LogisticsApiProvider;
import com.feseis.fish.logistics.model.UserAddress;
import com.feseis.fish.logistics.store.DefaultAddressStore;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.List;

/**
 * 地址簿：默认地址置顶，支持新增、编辑、删除与设为默认。
 * 从首页进入时为选择模式：点条目直接返回地址。
 */
public class AddressBookActivity extends AppCompatActivity implements AddressAdapter.Listener {

    private static final int MENU_SET_DEFAULT = 1;
    private static final int MENU_DELETE = 2;

    public static final String EXTRA_ADDRESS = "extra_picked_address";
    public static final String EXTRA_PICK_TARGET = "extra_pick_target";
    public static final int TARGET_NONE = 0;
    public static final int TARGET_SENDER = 1;
    public static final int TARGET_RECEIVER = 2;

    private LogisticsApi api;
    private AddressAdapter adapter;
    private RecyclerView recyclerView;
    private View emptyView;
    private CircularProgressIndicator progress;

    private String accountId = "";
    private int pickTarget = TARGET_NONE;

    public static Intent newPickIntent(Context context, int target) {
        Intent intent = new Intent(context, AddressBookActivity.class);
        intent.putExtra(EXTRA_PICK_TARGET, target);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        this.setContentView(R.layout.activity_address_book);
        ViewCompat.setOnApplyWindowInsetsListener(this.findViewById(R.id.address_book_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int pad = Math.round(20 * this.getResources().getDisplayMetrics().density);
            v.setPadding(systemBars.left + pad, systemBars.top + pad,
                    systemBars.right + pad, systemBars.bottom + pad);
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
        this.pickTarget = this.getIntent().getIntExtra(EXTRA_PICK_TARGET, TARGET_NONE);

        TextView title = this.findViewById(R.id.tv_address_book_title);
        if (this.pickTarget == TARGET_SENDER) {
            title.setText(R.string.address_pick_sender_title);
        } else if (this.pickTarget == TARGET_RECEIVER) {
            title.setText(R.string.address_pick_receiver_title);
        }

        TextView back = this.findViewById(R.id.tv_address_book_back);
        back.setOnClickListener(v -> this.finish());

        this.recyclerView = this.findViewById(R.id.rv_address);
        this.emptyView = this.findViewById(R.id.layout_address_empty);
        this.progress = this.findViewById(R.id.progress_address_book);

        this.adapter = new AddressAdapter(this, this.pickTarget != TARGET_NONE);
        this.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        this.recyclerView.setAdapter(this.adapter);

        MaterialButton addButton = this.findViewById(R.id.btn_add_address);
        addButton.setOnClickListener(v ->
                this.startActivity(AddressEditActivity.newIntent(this, null)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!this.accountId.isEmpty()) {
            this.reload();
        }
    }

    private void reload() {
        this.progress.setVisibility(View.VISIBLE);
        String defaultId = DefaultAddressStore.get(this, this.accountId);
        this.api.listAddresses(result -> {
            this.progress.setVisibility(View.GONE);
            if (!result.isSuccess()) {
                Toast.makeText(this, this.describe(result.getError()), Toast.LENGTH_SHORT).show();
                return;
            }
            List<UserAddress> addresses = result.getData();
            this.adapter.submit(addresses, defaultId);
            this.emptyView.setVisibility(addresses.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public void onEdit(UserAddress address) {
        if (this.pickTarget != TARGET_NONE) {
            Intent data = new Intent();
            data.putExtra(EXTRA_ADDRESS, address);
            data.putExtra(EXTRA_PICK_TARGET, this.pickTarget);
            this.setResult(RESULT_OK, data);
            this.finish();
            return;
        }
        this.startActivity(AddressEditActivity.newIntent(this, address));
    }

    @Override
    public void onMore(View anchor, UserAddress address) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(0, MENU_SET_DEFAULT, 0, R.string.address_book_set_default);
        menu.getMenu().add(0, MENU_DELETE, 1, R.string.address_book_delete);
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == MENU_SET_DEFAULT) {
                DefaultAddressStore.set(this, this.accountId, address.getUserAddressId());
                this.reload();
                return true;
            }
            if (item.getItemId() == MENU_DELETE) {
                this.confirmDelete(address);
                return true;
            }
            return false;
        });
        menu.show();
    }

    private void confirmDelete(UserAddress address) {
        new AlertDialog.Builder(this)
                .setMessage(R.string.address_delete_confirm)
                .setNegativeButton(R.string.address_cancel, null)
                .setPositiveButton(R.string.address_book_delete, (dialog, which) ->
                        this.api.deleteAddress(address.getUserAddressId(), result -> {
                            if (!result.isSuccess()) {
                                Toast.makeText(this, this.describe(result.getError()),
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }
                            DefaultAddressStore.clearIfMatches(this, this.accountId,
                                    address.getUserAddressId());
                            this.reload();
                        }))
                .show();
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
}
