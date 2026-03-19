package com.techmania.tumago.auth;

import java.util.ArrayList;
import java.util.List;

public class CountryCodes {

    // Each entry: { displayName, dialCode, isoCode }
    // Sorted alphabetically. isoCode used as fallback label when emoji flags don't render.
    private static final String[][] COUNTRIES = {
        {"Angola",        "+244", "AO"},
        {"Botswana",      "+267", "BW"},
        {"DRC",           "+243", "CD"},
        {"Eswatini",      "+268", "SZ"},
        {"Lesotho",       "+266", "LS"},
        {"Malawi",        "+265", "MW"},
        {"Mauritius",     "+230", "MU"},
        {"Mozambique",    "+258", "MZ"},
        {"Namibia",       "+264", "NA"},
        {"Seychelles",    "+248", "SC"},
        {"South Africa",  "+27",  "ZA"},
        {"Tanzania",      "+255", "TZ"},
        {"Zambia",        "+260", "ZM"},
        {"Zimbabwe",      "+263", "ZW"},
    };

    // Returns a list of display names for the dropdown: "ZW  Zimbabwe (+263)"
    public static List<String> getDropdownNames() {
        List<String> names = new ArrayList<>();
        for (String[] c : COUNTRIES) {
            names.add(c[2] + "  " + c[0] + " (" + c[1] + ")");
        }
        return names;
    }

    // Returns the short label for the collapsed spinner: "ZW +263"
    public static List<String> getShortLabels() {
        List<String> labels = new ArrayList<>();
        for (String[] c : COUNTRIES) {
            labels.add(c[2] + " " + c[1]);
        }
        return labels;
    }

    // Returns the dial code for a given dropdown position
    public static String getCodeByPosition(int position) {
        if (position >= 0 && position < COUNTRIES.length) {
            return COUNTRIES[position][1];
        }
        return "+263"; // Default to Zimbabwe
    }

    // Returns the position index of Zimbabwe (default country)
    public static int getDefaultPosition() {
        for (int i = 0; i < COUNTRIES.length; i++) {
            if ("ZW".equals(COUNTRIES[i][2])) return i;
        }
        return 0;
    }
}
