package com.techmania.tumago_driver.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.card.MaterialCardView;
import com.techmania.tumago_driver.Interface.ApiService;
import com.techmania.tumago_driver.R;
import com.techmania.tumago_driver.helpers.ApiClient2;
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
    private Uri selectedImageUri;
    private MaterialCardView selectButton, uploadButton, cancelButton;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_license);

        Toolbar toolbar = findViewById(R.id.toolbarHome);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        imageView = findViewById(R.id.imageView);
        selectButton = findViewById(R.id.selectButton);
        uploadButton = findViewById(R.id.uploadButton);
        cancelButton = findViewById(R.id.cancelButton);

        apiService = ApiClient2.getClient("http://192.168.8.147:8000/").create(ApiService.class);

        selectButton.setOnClickListener(v -> openImagePicker());

        uploadButton.setOnClickListener(v -> {
            if (selectedImageUri != null) {
                uploadImage(selectedImageUri);
            } else {
                Toast.makeText(this, "Select an image first", Toast.LENGTH_SHORT).show();
            }
        });

        cancelButton.setOnClickListener(v -> {
            startActivity(new Intent(UploadLicenseActivity.this, MainActivity.class));
            finish();
        });
    }

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();

                    if (uri != null && isImageFile(uri)) {
                        selectedImageUri = uri;
                        imageView.setImageURI(uri);
                    } else {
                        Toast.makeText(this, "Please select an image file only.", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");  // ensure only images are shown
        imagePickerLauncher.launch(intent);
    }

    private boolean isImageFile(Uri uri) {
        String type = getContentResolver().getType(uri);
        if (type == null || !type.startsWith("image/")) {
            return false;
        }

        String[] validExtensions = {"jpg", "jpeg", "png", "webp"};
        String path = uri.getPath();
        if (path != null) {
            String lowerPath = path.toLowerCase();
            for (String ext : validExtensions) {
                if (lowerPath.endsWith("." + ext)) {
                    return true;
                }
            }
        }

        return false;
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
            call.enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(UploadLicenseActivity.this, "Upload successful", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(UploadLicenseActivity.this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(UploadLicenseActivity.this, "Upload failed: " + response.message(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Toast.makeText(UploadLicenseActivity.this, "Upload error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to read file", Toast.LENGTH_SHORT).show();
        }
    }
}