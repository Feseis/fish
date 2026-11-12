package com.feseis.fish;

import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * 填写寄件信息占位页。物品、重量、保价与支付在下一轮实现。
 */
public class OrderCreateActivity extends AppCompatActivity {

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

        TextView back = this.findViewById(R.id.tv_order_create_back);
        back.setOnClickListener(v -> this.finish());
    }
}
