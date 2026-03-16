package com.techmania.tumago_driver.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.techmania.tumago_driver.Interface.ApiService;
import com.techmania.tumago_driver.R;
import com.techmania.tumago_driver.auth.Login;
import com.techmania.tumago_driver.helpers.AnimHelper;
import com.techmania.tumago_driver.helpers.ApiClient;
import com.techmania.tumago_driver.helpers.UiHelper;
import com.techmania.tumago_driver.helpers.Token;
import com.techmania.tumago_driver.models.FinanceInfo;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Finances extends AppCompatActivity {
    PieChart pieChart;
    TextView todayTotal, todayCharges, todayProfit;
    TextView weekTotal, weekProfit;
    TextView monthTotal, monthProfit;
    TextView allTimeEarnings, allTimeTrips;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finances);

        Toolbar toolbar = findViewById(R.id.toolbarHome);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        pieChart = findViewById(R.id.pieChart);
        todayTotal = findViewById(R.id.tdyTotal);
        todayCharges = findViewById(R.id.tdyCharges);
        todayProfit = findViewById(R.id.tdyProfit);
        weekProfit = findViewById(R.id.weekProfit);
        weekTotal = findViewById(R.id.weekTotal);
        monthTotal = findViewById(R.id.monthTotal);
        monthProfit = findViewById(R.id.monthProfit);
        allTimeEarnings = findViewById(R.id.allTimeEarnings);
        allTimeTrips = findViewById(R.id.allTimeTrips);
        progressBar = findViewById(R.id.progressBar);

        getFinances();
    }

    // Builds the pie chart using real profit and charges values from the API
    private void setupPieChart(float profit, float charges) {
        ArrayList<PieEntry> entries = new ArrayList<>();

        // Only add entries with positive values so the chart doesn't look empty
        if (profit > 0) entries.add(new PieEntry(profit, "Profit"));
        if (charges > 0) entries.add(new PieEntry(charges, "Charges"));

        // If both are zero, show a placeholder so the chart isn't blank
        if (entries.isEmpty()) {
            entries.add(new PieEntry(1f, "No data"));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");

        if (profit > 0 || charges > 0) {
            ArrayList<Integer> colors = new ArrayList<>();
            if (profit > 0) colors.add(Color.parseColor("#0e74bc"));
            if (charges > 0) colors.add(Color.parseColor("#EF4444"));
            dataSet.setColors(colors);
        } else {
            dataSet.setColors(new int[]{Color.parseColor("#E0E0E0")});
        }

        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(6f);

        // Labels outside the slices for a cleaner look
        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setValueLinePart1Length(0.4f);
        dataSet.setValueLinePart2Length(0.3f);
        dataSet.setValueLineColor(Color.parseColor("#9BA3B2"));
        dataSet.setValueLineWidth(1.5f);

        PieData data = new PieData(dataSet);
        data.setValueTextColor(Color.parseColor("#3a3a3c"));
        data.setValueTextSize(13f);

        // Show dollar amounts as labels, e.g. "$12.50"
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getPieLabel(float value, PieEntry pieEntry) {
                if (pieEntry.getLabel().equals("No data")) return "";
                return "$" + String.format("%.2f", value);
            }
        });

        pieChart.setData(data);
        pieChart.setUsePercentValues(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleRadius(65f);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setTransparentCircleRadius(70f);
        pieChart.setTransparentCircleColor(Color.WHITE);
        pieChart.setTransparentCircleAlpha(110);

        pieChart.setDrawEntryLabels(false);
        pieChart.getDescription().setEnabled(false);
        pieChart.getLegend().setEnabled(false);
        pieChart.setRotationEnabled(false);

        // Smooth entry animation
        pieChart.animateY(800);
        pieChart.invalidate();
    }

    public void getFinances() {
        String accessToken = Token.getAccessToken(this);
        if (accessToken != null && !accessToken.isEmpty()) {
            String authHeader = "Bearer " + accessToken;

            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            Call<FinanceInfo> call = apiService.getFinances(authHeader);

            UiHelper.showLoading(progressBar);

            call.enqueue(new Callback<FinanceInfo>() {
                @Override
                public void onResponse(Call<FinanceInfo> call, Response<FinanceInfo> response) {
                    UiHelper.hideLoading(progressBar);
                    if (response.isSuccessful() && response.body() != null) {
                        FinanceInfo data = response.body();

                        AnimHelper.beginTransition((ViewGroup) findViewById(R.id.main));
                        todayTotal.setText("$" + data.getToday().getEarnings());
                        todayCharges.setText("$" + data.getToday().getCharges());
                        todayProfit.setText("$" + data.getToday().getProfit());

                        weekTotal.setText("$" + data.getWeek().getEarnings());
                        weekProfit.setText("$" + data.getWeek().getProfit());

                        monthTotal.setText("$" + data.getMonth().getEarnings());
                        monthProfit.setText("$" + data.getMonth().getProfit());

                        // All-time summary displayed above the chart
                        String earnings = data.getAllTime().getEarnings();
                        int trips = data.getAllTime().getTotalTrips();
                        allTimeEarnings.setText("$" + (earnings != null ? earnings : "0.00"));
                        allTimeTrips.setText(trips + (trips == 1 ? " delivery" : " deliveries"));

                        // Build the pie chart with real month data
                        float monthProfit = parseFloat(data.getMonth().getProfit());
                        float monthCharges = parseFloat(data.getMonth().getCharges());
                        setupPieChart(monthProfit, monthCharges);
                    } else {
                        Log.d("Finances", "Failed: " + response.message());
                        setupPieChart(0, 0);
                    }
                }

                @Override
                public void onFailure(Call<FinanceInfo> call, Throwable t) {
                    Log.d("Finances", "Error: " + t.getMessage());
                    UiHelper.hideLoading(progressBar);
                    UiHelper.showRetry(findViewById(android.R.id.content), "Failed to load finances", () -> getFinances());
                    setupPieChart(0, 0);
                }
            });

        } else {
            Intent intent = new Intent(Finances.this, Login.class);
            startActivity(intent);
            finish();
        }
    }

    // Safely parses a money string like "12.50" to float, returns 0 on failure
    private float parseFloat(String value) {
        if (value == null || value.isEmpty()) return 0f;
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return 0f;
        }
    }
}
















