package com.feseis.fish;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.feseis.fish.logistics.api.LogisticsApi;
import com.feseis.fish.logistics.api.LogisticsApiProvider;
import com.feseis.fish.logistics.model.Poi;
import com.feseis.fish.logistics.model.RegeoResult;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

/**
 * 选择详细地址：定位一次，调后端 regeo 拿附近 POI，选中后回传。
 */
public class PoiPickerActivity extends AppCompatActivity implements AMapLocationListener {

    public static final String EXTRA_POI = "extra_poi";

    private LogisticsApi api;
    private AMapLocationClient locationClient;
    private TextView statusView;
    private MaterialButton retryButton;
    private CircularProgressIndicator progress;
    private TextInputEditText keywordInput;
    private PoiAdapter adapter;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean granted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION))
                        || Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                if (granted) {
                    this.startLocate();
                } else {
                    this.showError(R.string.address_picker_permission);
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        this.setContentView(R.layout.activity_poi_picker);
        ViewCompat.setOnApplyWindowInsetsListener(this.findViewById(R.id.poi_picker_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int pad = Math.round(20 * this.getResources().getDisplayMetrics().density);
            v.setPadding(systemBars.left + pad, systemBars.top + pad,
                    systemBars.right + pad, systemBars.bottom + pad);
            return insets;
        });

        this.api = LogisticsApiProvider.get(this);
        this.statusView = this.findViewById(R.id.tv_picker_status);
        this.retryButton = this.findViewById(R.id.btn_picker_retry);
        this.progress = this.findViewById(R.id.progress_poi);
        this.keywordInput = this.findViewById(R.id.et_poi_keyword);

        TextView back = this.findViewById(R.id.tv_poi_back);
        back.setOnClickListener(v -> this.finish());

        RecyclerView recyclerView = this.findViewById(R.id.rv_poi);
        this.adapter = new PoiAdapter(this::pick);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(this.adapter);

        this.retryButton.setOnClickListener(v -> {
            this.keywordInput.setText("");
            this.startLocate();
        });
        MaterialButton searchButton = this.findViewById(R.id.btn_poi_search);
        searchButton.setOnClickListener(v -> this.search());
        this.keywordInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                this.search();
                return true;
            }
            return false;
        });
        this.startLocate();
    }

    private void startLocate() {
        if (!this.hasLocationPermission()) {
            this.permissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION});
            return;
        }
        this.statusView.setText(R.string.address_picker_locating);
        this.retryButton.setVisibility(View.GONE);
        this.retryButton.setText(R.string.address_picker_retry);
        this.progress.setVisibility(View.VISIBLE);
        try {
            if (this.locationClient == null) {
                this.locationClient = new AMapLocationClient(this.getApplicationContext());
                AMapLocationClientOption option = new AMapLocationClientOption();
                option.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
                option.setOnceLocation(true);
                option.setOnceLocationLatest(true);
                this.locationClient.setLocationOption(option);
                this.locationClient.setLocationListener(this);
            }
            this.locationClient.startLocation();
        } catch (Exception e) {
            this.showError(R.string.address_picker_failed);
        }
    }

    @Override
    public void onLocationChanged(AMapLocation location) {
        if (location == null || location.getErrorCode() != 0) {
            this.showError(R.string.address_picker_failed);
            return;
        }
        this.api.regeo(location.getLongitude(), location.getLatitude(), result -> {
            this.progress.setVisibility(View.GONE);
            if (!result.isSuccess()) {
                this.showError(R.string.address_picker_failed);
                return;
            }
            RegeoResult regeo = result.getData();
            if (regeo == null || regeo.getPois().isEmpty()) {
                this.showError(R.string.address_picker_empty);
                return;
            }
            String formatted = regeo.getFormattedAddress();
            this.statusView.setText(formatted == null || formatted.isEmpty()
                    ? this.getString(R.string.address_picker_nearby) : formatted);
            this.adapter.submit(regeo.getPois());
            this.retryButton.setVisibility(View.VISIBLE);
            this.retryButton.setText(R.string.address_picker_retry);
        });
    }

    /** 关键字搜索：不限定城市，跨城地址也能查到。 */
    private void search() {
        String keyword = this.keywordInput.getText() == null
                ? "" : this.keywordInput.getText().toString().trim();
        if (keyword.isEmpty()) {
            Toast.makeText(this, R.string.address_picker_keyword_required, Toast.LENGTH_SHORT).show();
            return;
        }
        this.statusView.setText(R.string.address_picker_searching);
        this.retryButton.setVisibility(View.GONE);
        this.progress.setVisibility(View.VISIBLE);
        this.api.searchAddressPois(keyword, "", result -> {
            this.progress.setVisibility(View.GONE);
            if (!result.isSuccess()) {
                this.showError(R.string.address_picker_failed);
                return;
            }
            List<Poi> pois = result.getData();
            this.adapter.submit(pois);
            if (pois.isEmpty()) {
                this.statusView.setText(R.string.address_picker_search_empty);
            } else {
                this.statusView.setText(
                        this.getString(R.string.address_picker_search_result, keyword));
            }
            this.retryButton.setVisibility(View.VISIBLE);
            this.retryButton.setText(R.string.address_picker_back_nearby);
        });
    }

    private void pick(Poi poi) {
        Intent data = new Intent();
        data.putExtra(EXTRA_POI, poi);
        this.setResult(RESULT_OK, data);
        this.finish();
    }

    private void showError(int messageRes) {
        this.progress.setVisibility(View.GONE);
        this.statusView.setText(messageRes);
        this.retryButton.setVisibility(View.VISIBLE);
        this.retryButton.setText(R.string.address_picker_retry);
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onDestroy() {
        if (this.locationClient != null) {
            this.locationClient.stopLocation();
            this.locationClient.onDestroy();
            this.locationClient = null;
        }
        super.onDestroy();
    }
}
