package com.techmania.tumago.auth;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.techmania.tumago.Activities.MainActivity;
import com.techmania.tumago.Interface.ApiService;
import com.techmania.tumago.Model.UserProfileData;
import com.techmania.tumago.R;
import com.techmania.tumago.helper.ApiClient;
import com.techmania.tumago.helper.Token;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserInfo extends AppCompatActivity {
    EditText Name, Surname, IDnumber, Street, AddressLine, Province, City, PostalCode;
    Button Register;
    TextView fields;
    ImageView fileupload;
    TextView selectedFile;
    Uri fileUri;
    LinearLayout progressBar;

    ActivityResultLauncher<Intent> filePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);

        Name = findViewById(R.id.nameInput);
        Surname = findViewById(R.id.surnameInput);
        IDnumber = findViewById(R.id.userId);
        Street = findViewById(R.id.userStreet);
        AddressLine = findViewById(R.id.userAddress);
        Province = findViewById(R.id.userProvince);
        City = findViewById(R.id.userCity);
        PostalCode = findViewById(R.id.userPostalCode);
        Register = findViewById(R.id.infoRegister);
        fields = findViewById(R.id.fields);
        fileupload = findViewById(R.id.FileUpload);
        selectedFile = findViewById(R.id.textSelectedFile);
        progressBar = findViewById(R.id.progressBar);
        fields = findViewById(R.id.fields);

        Register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SendUserInfo();
            }
        });

        filePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                fileUri = result.getData().getData();
                selectedFile.setText(fileUri.getLastPathSegment());
            } else {
                Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show();
            }
        });

        fileupload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                filePickerLauncher.launch(intent);
            }
        });
    }

    public void SendUserInfo(){
        String name = Name.getText().toString();
        String surname = Surname.getText().toString();
        String identity_number = IDnumber.getText().toString();
        String streetAdress = Street.getText().toString();
        String addressLine = AddressLine.getText().toString();
        String province = Province.getText().toString();
        String city = City.getText().toString();
        String postalCode = PostalCode.getText().toString();
        Boolean verifiedEmail = getBoolean(this);

        progressBar.setVisibility(View.VISIBLE);

        if (!name.isEmpty() && !surname.isEmpty() && !streetAdress.isEmpty() &&
                !province.isEmpty() && !city.isEmpty() && !postalCode.isEmpty()) {

            UserProfileData userProfileData = new UserProfileData(name, surname, identity_number, streetAdress, addressLine, province,
                    city, postalCode, verifiedEmail);

            String accessToken = Token.getAccessToken(this);

            if (accessToken != null && !accessToken.isEmpty()) {
                String authHeader = "Bearer " + accessToken;

                ApiService apiService = ApiClient.getClient().create(ApiService.class);
                Call<ResponseBody> call = apiService.updateProfile(authHeader, userProfileData);

                call.enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            progressBar.setVisibility(View.GONE);
                            Intent i = new Intent(UserInfo.this, TermsAgreement.class);
                            startActivity(i);
                            finish();
                        } else {
                                Toast.makeText(UserInfo.this, "Registration failed", Toast.LENGTH_SHORT).show();
                                progressBar.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(UserInfo.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                Toast.makeText(UserInfo.this, "Token is null", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        } else {
            progressBar.setVisibility(View.GONE);
            fields.setVisibility(View.VISIBLE);
            new Handler().postDelayed(() -> fields.setVisibility(View.GONE), 5000);
        }
    }

    private File createTempFileFromUri(Uri uri) throws Exception {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        File file = File.createTempFile("upload", ".tmp", getCacheDir());
        try (OutputStream outputStream = new FileOutputStream(file)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        }
        return file;
    }

    public static Boolean getBoolean(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            SharedPreferences securePrefs = EncryptedSharedPreferences.create(
                    context,
                    "secure_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            return securePrefs.getBoolean("verifiedEmail", true);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}









