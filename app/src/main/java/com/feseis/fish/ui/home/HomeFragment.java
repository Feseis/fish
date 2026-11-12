package com.feseis.fish.ui.home;

import com.feseis.fish.R;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.TextureMapView;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MyLocationStyle;
import com.feseis.fish.logistics.model.UserAddress;
import com.feseis.fish.logistics.store.ShipmentDraftStore;
import com.feseis.fish.ui.address.AddressBookActivity;
import com.feseis.fish.ui.address.AddressEntryActivity;
import com.feseis.fish.ui.order.OrderCreateActivity;

/**
 * 首页：顶部地图 + 寄收地址选择。
 * 寄、收都选好后显示「填写寄件信息」入口；地址草稿由 ShipmentDraftStore 跨 tab 保留。
 */
public class HomeFragment extends Fragment implements LocationSource, AMapLocationListener {

    /** 默认视野：杭州，与演示网点城市一致。 */
    private static final LatLng DEFAULT_CENTER = new LatLng(30.2741, 120.1551);
    private static final float DEFAULT_ZOOM = 12f;
    private static final float LOCATED_ZOOM = 16f;

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean granted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION))
                        || Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                if (granted) {
                    this.enableMyLocation();
                } else {
                    Toast.makeText(requireContext(), R.string.home_location_denied, Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> addressPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) {
                    return;
                }
                Object extra = result.getData().getSerializableExtra(AddressBookActivity.EXTRA_ADDRESS);
                if (!(extra instanceof UserAddress)) {
                    return;
                }
                int target = result.getData().getIntExtra(AddressBookActivity.EXTRA_PICK_TARGET,
                        AddressBookActivity.TARGET_NONE);
                if (target == AddressBookActivity.TARGET_SENDER) {
                    this.draft.setSender((UserAddress) extra);
                } else if (target == AddressBookActivity.TARGET_RECEIVER) {
                    this.draft.setReceiver((UserAddress) extra);
                }
                this.renderAddresses();
            });

    private final ShipmentDraftStore draft = ShipmentDraftStore.get();

    private TextureMapView mapView;
    private AMap aMap;
    private AMapLocationClient locationClient;
    private OnLocationChangedListener locationChangedListener;
    private boolean centered;

    private TextView senderAddressView;
    private TextView senderContactView;
    private TextView receiverAddressView;
    private TextView receiverContactView;
    private View shipmentButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        this.mapView = root.findViewById(R.id.home_map);
        this.mapView.onCreate(savedInstanceState);
        // 地图手势优先：手指落在图上时地图接管拖动/缩放，页面只在图外滑动时滚动
        this.mapView.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                v.getParent().requestDisallowInterceptTouchEvent(true);
            }
            return false;
        });
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.aMap = this.mapView.getMap();
        this.aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_CENTER, DEFAULT_ZOOM));
        this.aMap.getUiSettings().setZoomControlsEnabled(false);
        this.aMap.setLocationSource(this);

        MyLocationStyle style = new MyLocationStyle();
        style.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATE);
        style.showMyLocation(true);
        this.aMap.setMyLocationStyle(style);

        if (this.hasLocationPermission()) {
            this.enableMyLocation();
        } else {
            this.locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION});
        }

        this.bindAddressViews(view);
    }

    private void bindAddressViews(View view) {
        this.senderAddressView = view.findViewById(R.id.tv_home_sender_address);
        this.senderContactView = view.findViewById(R.id.tv_home_sender_contact);
        this.receiverAddressView = view.findViewById(R.id.tv_home_receiver_address);
        this.receiverContactView = view.findViewById(R.id.tv_home_receiver_contact);
        this.shipmentButton = view.findViewById(R.id.btn_home_shipment);

        view.findViewById(R.id.row_home_sender).setOnClickListener(v ->
                this.startActivity(AddressEntryActivity.newIntent(
                        requireContext(), AddressBookActivity.TARGET_SENDER)));
        view.findViewById(R.id.row_home_receiver).setOnClickListener(v ->
                this.startActivity(AddressEntryActivity.newIntent(
                        requireContext(), AddressBookActivity.TARGET_RECEIVER)));
        view.findViewById(R.id.tv_home_sender_book).setOnClickListener(v ->
                this.addressPickerLauncher.launch(AddressBookActivity.newPickIntent(
                        requireContext(), AddressBookActivity.TARGET_SENDER)));
        view.findViewById(R.id.tv_home_receiver_book).setOnClickListener(v ->
                this.addressPickerLauncher.launch(AddressBookActivity.newPickIntent(
                        requireContext(), AddressBookActivity.TARGET_RECEIVER)));
        view.findViewById(R.id.btn_home_swap).setOnClickListener(v -> {
            this.draft.swap();
            this.renderAddresses();
        });
        this.shipmentButton.setOnClickListener(v ->
                this.startActivity(new Intent(requireContext(), OrderCreateActivity.class)));

        this.renderAddresses();
    }

    private void renderAddresses() {
        this.renderRow(this.senderAddressView, this.senderContactView,
                this.draft.getSender(), R.string.home_pickup_hint);
        this.renderRow(this.receiverAddressView, this.receiverContactView,
                this.draft.getReceiver(), R.string.home_delivery_hint);
        this.shipmentButton.setVisibility(this.draft.isComplete() ? View.VISIBLE : View.GONE);
    }

    private void renderRow(TextView addressView, TextView contactView,
                           UserAddress address, int emptyHint) {
        if (address == null) {
            addressView.setText(emptyHint);
            contactView.setVisibility(View.GONE);
            return;
        }
        addressView.setText(address.fullAddress());
        contactView.setText(address.getContactName() + "  " + address.getContactPhone());
        contactView.setVisibility(View.VISIBLE);
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void enableMyLocation() {
        if (this.aMap != null) {
            this.aMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public void activate(OnLocationChangedListener listener) {
        this.locationChangedListener = listener;
        this.startLocation();
    }

    @Override
    public void deactivate() {
        this.locationChangedListener = null;
        if (this.locationClient != null) {
            this.locationClient.stopLocation();
        }
    }

    private void startLocation() {
        if (!this.hasLocationPermission()) {
            return;
        }
        try {
            if (this.locationClient == null) {
                this.locationClient = new AMapLocationClient(requireContext().getApplicationContext());
                AMapLocationClientOption option = new AMapLocationClientOption();
                option.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
                option.setOnceLocation(true);
                option.setOnceLocationLatest(true);
                this.locationClient.setLocationOption(option);
                this.locationClient.setLocationListener(this);
            }
            this.locationClient.startLocation();
        } catch (Exception e) {
            Toast.makeText(requireContext(), R.string.home_location_failed, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onLocationChanged(AMapLocation location) {
        if (location == null || location.getErrorCode() != 0) {
            return;
        }
        if (this.locationChangedListener != null) {
            this.locationChangedListener.onLocationChanged(location);
        }
        if (!this.centered && this.aMap != null) {
            this.centered = true;
            this.aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(location.getLatitude(), location.getLongitude()), LOCATED_ZOOM));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mapView.onResume();
        this.renderAddresses();
    }

    @Override
    public void onPause() {
        this.mapView.onPause();
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        this.mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        if (this.locationClient != null) {
            this.locationClient.stopLocation();
            this.locationClient.onDestroy();
            this.locationClient = null;
        }
        this.locationChangedListener = null;
        this.aMap = null;
        this.mapView.onDestroy();
        this.mapView = null;
        super.onDestroyView();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (this.mapView != null) {
            this.mapView.onLowMemory();
        }
    }
}
