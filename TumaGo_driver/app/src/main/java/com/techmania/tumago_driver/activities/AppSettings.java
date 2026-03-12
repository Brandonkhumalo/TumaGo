package com.techmania.tumago_driver.activities;

import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.techmania.tumago_driver.R;
import com.techmania.tumago_driver.helpers.ThemeHelper;

public class AppSettings extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        RadioGroup themeGroup = findViewById(R.id.themeRadioGroup);
        RadioButton radioSystem = findViewById(R.id.radioSystem);
        RadioButton radioLight = findViewById(R.id.radioLight);
        RadioButton radioDark = findViewById(R.id.radioDark);

        int savedMode = ThemeHelper.getSavedThemeMode(this);
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
            ThemeHelper.saveAndApplyTheme(AppSettings.this, mode);
        });
    }
}
