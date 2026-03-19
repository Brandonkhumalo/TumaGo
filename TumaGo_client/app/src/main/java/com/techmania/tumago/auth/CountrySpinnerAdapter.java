package com.techmania.tumago.auth;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.techmania.tumago.R;

import java.util.List;

/**
 * Shows a short label (e.g. "ZW +263") in the collapsed spinner
 * and a full label (e.g. "ZW  Zimbabwe (+263)") in the dropdown.
 */
public class CountrySpinnerAdapter extends BaseAdapter {
    private final Context context;
    private final List<String> shortLabels;
    private final List<String> dropdownLabels;

    public CountrySpinnerAdapter(Context context, List<String> shortLabels, List<String> dropdownLabels) {
        this.context = context;
        this.shortLabels = shortLabels;
        this.dropdownLabels = dropdownLabels;
    }

    @Override
    public int getCount() {
        return shortLabels.size();
    }

    @Override
    public Object getItem(int position) {
        return dropdownLabels.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    // Collapsed view — shows "ZW +263"
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                .inflate(R.layout.spinner_country_selected, parent, false);
        }
        TextView tv = convertView.findViewById(android.R.id.text1);
        tv.setText(shortLabels.get(position));
        return convertView;
    }

    // Dropdown view — shows "ZW  Zimbabwe (+263)"
    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                .inflate(R.layout.spinner_country_dropdown, parent, false);
        }
        TextView tv = convertView.findViewById(android.R.id.text1);
        tv.setText(dropdownLabels.get(position));
        return convertView;
    }
}
