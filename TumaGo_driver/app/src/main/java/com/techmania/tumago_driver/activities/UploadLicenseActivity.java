package com.techmania.tumago_driver.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.techmania.tumago_driver.BuildConfig;
import com.techmania.tumago_driver.Interface.ApiService;
import com.techmania.tumago_driver.R;
import com.techmania.tumago_driver.helpers.AnimHelper;
import com.techmania.tumago_driver.helpers.ApiClient2;
import com.techmania.tumago_driver.helpers.UiHelper;
import com.techmania.tumago_driver.helpers.FileUtil;
import com.techmania.tumago_driver.helpers.Token;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UploadLicenseActivity extends AppCompatActivity {

    private ImageView imageView;
    private LinearLayout uploadPlaceholder;
    private Uri selectedImageUri;
    private MaterialCardView selectButton, uploadButton, cancelButton;
    private ProgressBar progressBar;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_license);

        Toolbar toolbar = findViewById(R.id.toolbarHome);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        imageView = findViewById(R.id.imageView);
        uploadPlaceholder = findViewById(R.id.uploadPlaceholder);
        selectButton = findViewById(R.id.selectButton);
        uploadButton = findViewById(R.id.uploadButton);
        cancelButton = findViewById(R.id.cancelButton);
        progressBar = findViewById(R.id.progressBar);

        apiService = ApiClient2.getClient(BuildConfig.BASE_URL).create(ApiService.class);

        selectButton.setOnClickListener(v -> openImagePicker());

        uploadButton.setOnClickListener(v -> {
            if (selectedImageUri != null) {
                uploadImage(selectedImageUri);
            } else {
                Toast.makeText(this, "Select an image first", Toast.LENGTH_SHORT).show();
            }
        });

        cancelButton.setOnClickListener(v -> {
            Intent i = new Intent(UploadLicenseActivity.this, MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        });
    }

    // Requests the appropriate storage permission based on API level
    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    launchImagePicker();
                } else {
                    Toast.makeText(this, "Storage permission is required to select an image", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();

                    if (uri != null && isImageFile(uri)) {
                        selectedImageUri = uri;
                        imageView.setImageURI(uri);
                        AnimHelper.fadeIn(imageView);
                        AnimHelper.fadeOut(uploadPlaceholder);
                    } else {
                        Toast.makeText(this, "Please select an image file only.", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private void openImagePicker() {
        // API 33+ uses READ_MEDIA_IMAGES, older versions use READ_EXTERNAL_STORAGE
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            launchImagePicker();
        } else {
            permissionLauncher.launch(permission);
        }
    }

    private void launchImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private boolean isImageFile(Uri uri) {
        String type = getContentResolver().getType(uri);
        return type != null && type.startsWith("image/");
    }

    private void uploadImage(Uri imageUri) {
        try {
            File file = FileUtil.from(this, imageUri);

            String mimeType = getContentResolver().getType(imageUri);
            RequestBody requestFile = RequestBody.create(file, MediaType.parse(mimeType));

            MultipartBody.Part body = MultipartBody.Part.createFormData("license_picture", file.getName(), requestFile);

            String accessToken = Token.getAccessToken(this);
            String authHeader = "Bearer " + accessToken;

            Call<Void> call = apiService.uploadLicense(authHeader, body);

            UiHelper.showLoading(progressBar, uploadButton);

            call.enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    UiHelper.hideLoading(progressBar, uploadButton);
                    if (response.isSuccessful()) {
                        Toast.makeText(UploadLicenseActivity.this, "Upload successful", Toast.LENGTH_SHORT).show();
                        Intent i = new Intent(UploadLicenseActivity.this, MainActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(i);
                        finish();
                    } else {
                        Toast.makeText(UploadLicenseActivity.this, "Upload failed: " + response.message(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    UiHelper.hideLoading(progressBar, uploadButton);
                    UiHelper.showRetry(findViewById(android.R.id.content), "Upload failed", () -> uploadImage(imageUri));
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to read file", Toast.LENGTH_SHORT).show();
        }
    }
}