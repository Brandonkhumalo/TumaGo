package com.techmania.tumago.auth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CountryCodes {
    private static final HashMap<String, String> sadcCountries = new HashMap<>();

    static {
        sadcCountries.put("🇿🇦 South Africa", "+27");
        sadcCountries.put("🇧🇼 Botswana", "+267");
        sadcCountries.put("🇲🇿 Mozambique", "+258");
        sadcCountries.put("🇳🇦 Namibia", "+264");
        sadcCountries.put("🇿🇲 Zambia", "+260");
        sadcCountries.put("🇿🇼 Zimbabwe", "+263");
        sadcCountries.put("🇱🇸 Lesotho", "+266");
        sadcCountries.put("🇲🇼 Malawi", "+265");
        sadcCountries.put("🇲🇺 Mauritius", "+230");
        sadcCountries.put("🇸🇿 Eswatini", "+268");
        sadcCountries.put("🇦🇴 Angola", "+244");
        sadcCountries.put("🇨🇩 DRC", "+243");
        sadcCountries.put("🇸🇨 Seychelles", "+248");
        sadcCountries.put("🇹🇿 Tanzania", "+255");
    }

    public static HashMap<String, String> getCountryCodes() {
        return sadcCountries;
    }

    public static List<String> getCountryNames() {
        return new ArrayList<>(sadcCountries.keySet());
    }

    public static String getCode(String countryName) {
        return sadcCountries.getOrDefault(countryName, "+263"); // Default to ZW
    }
}
