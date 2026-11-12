package com.feseis.fish;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.feseis.fish.logistics.model.UserAddress;

import java.util.ArrayList;
import java.util.List;

/**
 * 地址簿列表：默认地址置顶，点击编辑，右侧「⋯」打开更多操作。
 */
public final class AddressAdapter extends RecyclerView.Adapter<AddressAdapter.Holder> {

    public interface Listener {
        void onEdit(UserAddress address);

        void onMore(View anchor, UserAddress address);
    }

    private final List<UserAddress> items = new ArrayList<>();
    private final Listener listener;
    private final boolean pickMode;
    private String defaultId = "";

    public AddressAdapter(Listener listener, boolean pickMode) {
        this.listener = listener;
        this.pickMode = pickMode;
    }

    public void submit(List<UserAddress> addresses, String defaultId) {
        this.defaultId = defaultId == null ? "" : defaultId;
        this.items.clear();
        UserAddress defaultAddress = null;
        for (UserAddress address : addresses) {
            if (address.getUserAddressId().equals(this.defaultId)) {
                defaultAddress = address;
            } else {
                this.items.add(address);
            }
        }
        if (defaultAddress != null) {
            this.items.add(0, defaultAddress);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_address, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        UserAddress address = this.items.get(position);
        holder.name.setText(address.getContactName());
        holder.phone.setText(address.getContactPhone());
        holder.address.setText(address.fullAddress());

        String tag = address.getTag();
        if (tag == null || tag.isEmpty()) {
            holder.tag.setVisibility(View.GONE);
        } else {
            holder.tag.setVisibility(View.VISIBLE);
            holder.tag.setText(tag);
        }

        boolean isDefault = address.getUserAddressId().equals(this.defaultId);
        holder.defaultBadge.setVisibility(isDefault ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> this.listener.onEdit(address));
        holder.more.setVisibility(this.pickMode ? View.GONE : View.VISIBLE);
        holder.more.setOnClickListener(v -> this.listener.onMore(v, address));
    }

    @Override
    public int getItemCount() {
        return this.items.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {

        private final TextView name;
        private final TextView phone;
        private final TextView address;
        private final TextView tag;
        private final TextView defaultBadge;
        private final TextView more;

        Holder(@NonNull View itemView) {
            super(itemView);
            this.name = itemView.findViewById(R.id.tv_item_name);
            this.phone = itemView.findViewById(R.id.tv_item_phone);
            this.address = itemView.findViewById(R.id.tv_item_address);
            this.tag = itemView.findViewById(R.id.tv_item_tag);
            this.defaultBadge = itemView.findViewById(R.id.tv_item_default);
            this.more = itemView.findViewById(R.id.tv_item_more);
        }
    }
}
