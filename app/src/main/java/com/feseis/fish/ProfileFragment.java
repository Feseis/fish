package com.feseis.fish;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.feseis.fish.auth.model.LoginResult;
import com.feseis.fish.auth.store.SessionStore;

/**
 * 我的：头像、姓名占位、脱敏手机号、三个入口。
 * 消息、扫一扫、设置暂不实现；成为员工进入入职页。
 */
public class ProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        TextView phoneView = view.findViewById(R.id.tv_profile_phone);
        LoginResult session = SessionStore.get().getSession();
        if (session != null) {
            phoneView.setText(maskPhone(session.getPhone()));
        }

        view.findViewById(R.id.entry_become_staff).setOnClickListener(v ->
                this.startActivity(new Intent(requireContext(), StaffApplyActivity.class)));
        view.findViewById(R.id.entry_address_book).setOnClickListener(v ->
                this.startActivity(new Intent(requireContext(), AddressBookActivity.class)));
        return view;
    }

    /** 11 位手机号隐藏中间 4 位，其余原样展示。 */
    private static String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone == null ? "" : phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
