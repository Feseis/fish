package com.feseis.fish.ui.address;

import com.feseis.fish.R;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.feseis.fish.logistics.model.Poi;

import java.util.ArrayList;
import java.util.List;

/**
 * 附近 POI 候选列表。
 */
public final class PoiAdapter extends RecyclerView.Adapter<PoiAdapter.Holder> {

    public interface Listener {
        void onPick(Poi poi);
    }

    private final List<Poi> items = new ArrayList<>();
    private final Listener listener;

    public PoiAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Poi> pois) {
        this.items.clear();
        this.items.addAll(pois);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_poi, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Poi poi = this.items.get(position);
        holder.name.setText(poi.getPoiName());
        String address = poi.getAddress();
        if (address == null || address.isEmpty()) {
            holder.address.setVisibility(View.GONE);
        } else {
            holder.address.setVisibility(View.VISIBLE);
            holder.address.setText(address);
        }
        holder.itemView.setOnClickListener(v -> this.listener.onPick(poi));
    }

    @Override
    public int getItemCount() {
        return this.items.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {

        private final TextView name;
        private final TextView address;

        Holder(@NonNull View itemView) {
            super(itemView);
            this.name = itemView.findViewById(R.id.tv_poi_name);
            this.address = itemView.findViewById(R.id.tv_poi_address);
        }
    }
}
