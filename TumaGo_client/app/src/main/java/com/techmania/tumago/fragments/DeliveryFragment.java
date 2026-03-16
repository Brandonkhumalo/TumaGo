package com.techmania.tumago.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.techmania.tumago.BuildConfig;
import com.techmania.tumago.Interface.ApiService;
import com.techmania.tumago.Model.Expense;
import com.techmania.tumago.Model.TransportModel;
import com.techmania.tumago.R;
import com.techmania.tumago.adapter.TransportAdapter;
import com.techmania.tumago.helper.ApiClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.app.Activity.RESULT_OK;

public class DeliveryFragment extends Fragment implements OnMapReadyCallback {

    private static final String API_KEY = BuildConfig.MAPS_API_KEY;

    private EditText originInput, destInput;
    private TextView distanceText;
    private GoogleMap mMap;
    private LatLng originLatLng, destLatLng;
    private CardView scooter;
    private RecyclerView mainRecycler;
    private TransportAdapter transportAdapter;
    private ArrayList<TransportModel> arrayList;

    double Distance, ScooterPrice, VanPrice, TruckPrice;

    // Launchers for Places Autocomplete
    private ActivityResultLauncher<Intent> originLauncher;
    private ActivityResultLauncher<Intent> destLauncher;

    // Location permission launcher
    private ActivityResultLauncher<String> locationPermissionLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        originLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Place place = Autocomplete.getPlaceFromIntent(result.getData());
                        handlePlaceResult(place, true);
                    } else if (result.getData() != null) {
                        Status status = Autocomplete.getStatusFromIntent(result.getData());
                        Toast.makeText(requireContext(), "Location search failed. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });

        destLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Place place = Autocomplete.getPlaceFromIntent(result.getData());
                        handlePlaceResult(place, false);
                    } else if (result.getData() != null) {
                        Status status = Autocomplete.getStatusFromIntent(result.getData());
                        Toast.makeText(requireContext(), "Location search failed. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });

        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), granted -> {
                    if (granted) {
                        enableLocationFeatures();
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_delivery, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        scooter = view.findViewById(R.id.scooter);
        originInput = view.findViewById(R.id.input_origin);
        destInput = view.findViewById(R.id.input_dest);
        distanceText = view.findViewById(R.id.distance_text);
        mainRecycler = view.findViewById(R.id.mainRecycler);

        mainRecycler.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));

        originInput.setFocusable(false);
        destInput.setFocusable(false);

        originInput.setOnClickListener(v -> launchAutocomplete(originLauncher));
        destInput.setOnClickListener(v -> launchAutocomplete(destLauncher));

        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), API_KEY);
        }

        // Set up map inside fragment using getChildFragmentManager
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map_container);
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.map_container, mapFragment)
                    .commit();
        }
        mapFragment.getMapAsync(this);

        requestLocationPermission();
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            enableLocationFeatures();
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void enableLocationFeatures() {
        if (mMap != null && ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);

            FusedLocationProviderClient fusedLocationClient =
                    LocationServices.getFusedLocationProviderClient(requireActivity());
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(requireActivity(), location -> {
                        if (location != null && mMap != null) {
                            LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f));
                        }
                    });
        }
    }

    private void launchAutocomplete(ActivityResultLauncher<Intent> launcher) {
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG);
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                .build(requireContext());
        launcher.launch(intent);
    }

    private void handlePlaceResult(Place place, boolean isOrigin) {
        LatLng latLng = place.getLatLng();
        if (latLng == null || mMap == null) {
            Toast.makeText(requireContext(), "Map not ready or invalid location", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isOrigin) {
            originInput.setText(place.getName());
            originLatLng = latLng;
        } else {
            destInput.setText(place.getName());
            destLatLng = latLng;
        }

        if (originLatLng != null && destLatLng != null) {
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(originLatLng).title("Origin")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            mMap.addMarker(new MarkerOptions().position(destLatLng).title("Destination")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

            drawRouteAndDistance(originLatLng, destLatLng);
            saveLatLngToSharedPreferences();

            scooter.setVisibility(View.VISIBLE);
            Animation slideUp = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up);
            scooter.startAnimation(slideUp);
        }

        originInput.postDelayed(() -> {
            if (mMap != null) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10f));
            }
        }, 300);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        enableLocationFeatures();
    }

    private void drawRouteAndDistance(LatLng origin, LatLng destination) {
        if (mMap == null) return;

        String url = "https://maps.googleapis.com/maps/api/directions/json?origin=" +
                origin.latitude + "," + origin.longitude +
                "&destination=" + destination.latitude + "," + destination.longitude +
                "&key=" + API_KEY;

        new Thread(() -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder responseBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBuilder.append(line);
                }

                JSONObject response = new JSONObject(responseBuilder.toString());
                JSONArray routes = response.getJSONArray("routes");
                if (routes.length() > 0) {
                    JSONObject route = routes.getJSONObject(0);
                    JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                    String encodedPoints = overviewPolyline.getString("points");

                    String distance = route.getJSONArray("legs")
                            .getJSONObject(0)
                            .getJSONObject("distance")
                            .getString("text");

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (mMap == null || !isAdded()) return;

                            List<LatLng> points = com.google.maps.android.PolyUtil.decode(encodedPoints);
                            mMap.addPolyline(new PolylineOptions()
                                    .addAll(points)
                                    .width(10f)
                                    .color(getResources().getColor(R.color.dark_blue)));

                            distanceText.setText("Distance: " + distance);
                            String cleanedDistance = distance.replaceAll("[^\\d.]", "");
                            double distanceKm = Double.parseDouble(cleanedDistance);
                            Distance = distanceKm;

                            GetTripExpense(distanceKm);
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "Error drawing route", Toast.LENGTH_SHORT).show());
                }
            }
        }).start();
    }

    public void GetTripExpense(double distance2) {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<Expense> call = apiService.getExpense(distance2);

        call.enqueue(new Callback<Expense>() {
            @Override
            public void onResponse(Call<Expense> call, Response<Expense> response) {
                if (response.isSuccessful() && response.body() != null && isAdded()) {
                    Expense fare = response.body();
                    ScooterPrice = fare.getScooter();
                    VanPrice = fare.getVan();
                    TruckPrice = fare.getTruck();

                    arrayList = new ArrayList<>();
                    arrayList.add(new TransportModel("scooter", "Scooter", ScooterPrice));
                    arrayList.add(new TransportModel("vanm", "Van", VanPrice));
                    arrayList.add(new TransportModel("truckm", "Truck", TruckPrice));

                    transportAdapter = new TransportAdapter(arrayList, requireContext());
                    mainRecycler.setAdapter(transportAdapter);
                }
            }

            @Override
            public void onFailure(Call<Expense> call, Throwable t) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Failed to load pricing. Please try again.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void saveLatLngToSharedPreferences() {
        if (!isAdded()) return;
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("MyPrefs",
                android.content.Context.MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();

        editor.putLong("originLat", Double.doubleToRawLongBits(originLatLng.latitude));
        editor.putLong("originLng", Double.doubleToRawLongBits(originLatLng.longitude));
        editor.putLong("destLat", Double.doubleToRawLongBits(destLatLng.latitude));
        editor.putLong("destLng", Double.doubleToRawLongBits(destLatLng.longitude));

        editor.apply();
    }
}
