package com.feseis.fish.ui.main;

import com.feseis.fish.R;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.feseis.fish.ui.home.HomeFragment;
import com.feseis.fish.ui.orders.OrdersFragment;
import com.feseis.fish.ui.profile.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * 登录后的主容器：底部导航 + 三个页面。
 * 订单页暂为占位；我的页含「成为员工」入口。
 */
public class HomeActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        this.setContentView(R.layout.activity_home);
        ViewCompat.setOnApplyWindowInsetsListener(this.findViewById(R.id.home_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        this.bottomNav = this.findViewById(R.id.bottom_nav);
        this.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                this.show(new HomeFragment());
                return true;
            }
            if (id == R.id.nav_orders) {
                this.show(new OrdersFragment());
                return true;
            }
            if (id == R.id.nav_profile) {
                this.show(new ProfileFragment());
                return true;
            }
            return false;
        });

        if (savedInstanceState == null) {
            this.bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }

    private void show(Fragment fragment) {
        this.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}
