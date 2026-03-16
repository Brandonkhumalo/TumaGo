package com.techmania.tumago.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.techmania.tumago.Activities.UserProfile;
import com.techmania.tumago.R;
import com.techmania.tumago.helper.LogOutUser;
import com.techmania.tumago.helper.ThemeHelper;
import com.techmania.tumago.helper.Token;

public class SettingsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Profile link
        LinearLayout goToProfile = view.findViewById(R.id.goToProfile);
        goToProfile.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), UserProfile.class);
            startActivity(intent);
            requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        // Theme settings
        RadioGroup themeGroup = view.findViewById(R.id.themeRadioGroup);
        RadioButton radioSystem = view.findViewById(R.id.radioSystem);
        RadioButton radioLight = view.findViewById(R.id.radioLight);
        RadioButton radioDark = view.findViewById(R.id.radioDark);

        int savedMode = ThemeHelper.getSavedThemeMode(requireContext());
        switch (savedMode) {
            case ThemeHelper.MODE_LIGHT:
                radioLight.setChecked(true);
                break;
            case ThemeHelper.MODE_DARK:
                radioDark.setChecked(true);
                break;
            default:
                radioSystem.setChecked(true);
                break;
        }

        themeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int mode;
            if (checkedId == R.id.radioLight) {
                mode = ThemeHelper.MODE_LIGHT;
            } else if (checkedId == R.id.radioDark) {
                mode = ThemeHelper.MODE_DARK;
            } else {
                mode = ThemeHelper.MODE_SYSTEM;
            }
            ThemeHelper.saveAndApplyTheme(requireContext(), mode);
        });

        // Logout
        LinearLayout logout = view.findViewById(R.id.logOut);
        logout.setOnClickListener(v -> {
            String accessToken = Token.getAccessToken(requireContext());
            if (accessToken != null && !accessToken.isEmpty()) {
                LogOutUser.LogOut(requireContext(), accessToken);
            } else {
                Log.d("AccessToken", "No accessToken");
            }
        });
    }
}
