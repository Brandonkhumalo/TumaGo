package com.techmania.tumago_driver.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

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
import com.techmania.tumago_driver.helpers.ApiClient;
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
    String totalMoney;
    int totalTrips;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finances);

        Toolbar toolbar = findViewById(R.id.toolbarHome);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        getFinances();

        pieChart = findViewById(R.id.pieChart);
        setupPieChart();

        todayTotal = findViewById(R.id.tdyTotal);
        todayCharges = findViewById(R.id.tdyCharges);
        todayProfit = findViewById(R.id.tdyProfit);
        weekProfit = findViewById(R.id.weekProfit);
        weekTotal = findViewById(R.id.weekTotal);
        monthTotal = findViewById(R.id.monthTotal);
        monthProfit = findViewById(R.id.monthProfit);
    }

    private void setupPieChart() {
        ArrayList<PieEntry> entries = new ArrayList<>();

        // Sample data
        entries.add(new PieEntry(700f, "Profit"));
        entries.add(new PieEntry(400f, "Charges"));

        PieDataSet dataSet = new PieDataSet(entries, "");

        dataSet.setColors(new int[]{
                Color.parseColor("#0e74bc"),  // blue
                Color.parseColor("#D50303"),  // red
        });

        // 🔹 Show labels outside the slices
        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE); // values
        dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE); // labels

        dataSet.setValueLinePart1Length(0.6f);   // length before break
        dataSet.setValueLinePart2Length(0.4f);   // length after break
        dataSet.setValueLineColor(Color.DKGRAY); // connector line color
        dataSet.setValueLineWidth(2f);

        PieData data = new PieData(dataSet);
        data.setDrawValues(true); // Show values (optional)
        data.setValueTextColor(Color.DKGRAY);
        data.setValueTextSize(14f);

        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getPieLabel(float value, PieEntry pieEntry) {
                return pieEntry.getLabel(); // Show "Profit", "Charges"
            }
        });

        pieChart.setData(data);
        pieChart.setUsePercentValues(true);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleRadius(70f);
        pieChart.setTransparentCircleRadius(75f);

        // 🔹 Center text with two lines
        String centerText = "$" + totalMoney + "\n" + totalTrips;
        pieChart.setCenterText(centerText);
        pieChart.setCenterTextSize(18f);
        pieChart.setCenterTextColor(Color.DKGRAY);

        pieChart.getDescription().setEnabled(false);
        pieChart.getLegend().setEnabled(false);

        pieChart.invalidate(); // Refresh
    }

    public void getFinances(){
        String accessToken = Token.getAccessToken(this);
        if (accessToken != null & !accessToken.isEmpty()){
            String authHeader = "Bearer " + accessToken;

            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            Call<FinanceInfo> call = apiService.getFinances(authHeader);

            call.enqueue(new Callback<FinanceInfo>() {
                @Override
                public void onResponse(Call<FinanceInfo> call, Response<FinanceInfo> response) {
                    if (response.isSuccessful() && response.body() != null){
                        FinanceInfo data = response.body();

                        todayTotal.setText("$" + data.getToday().getEarnings());
                        todayCharges.setText("$" + data.getToday().getCharges());
                        todayProfit.setText("$" + data.getToday().getProfit());

                        weekTotal.setText("$" + data.getWeek().getEarnings());
                        weekProfit.setText("$" + data.getWeek().getProfit());

                        monthTotal.setText("$" + data.getMonth().getEarnings());
                        monthProfit.setText("$" + data.getMonth().getProfit());

                        totalMoney = data.getAllTime().getEarnings();
                        totalTrips = data.getAllTime().getTotalTrips();
                    } else {
                        Log.d("Failed", response.message());
                    }
                }

                @Override
                public void onFailure(Call<FinanceInfo> call, Throwable t) {
                    Log.d("Failed", t.getMessage());
                }
            });

        } else {
            Intent intent = new Intent(Finances.this, Login.class);
            startActivity(intent);
            finish();
        }
    }
}
















