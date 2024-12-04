package com.jetsup.home_iot.screens;

import static com.jetsup.home_iot.utils.Constants.ESP32_ALLOWED_IO_PINS;
import static com.jetsup.home_iot.utils.Constants.ESP32_ALLOWED_O_PINS;
import static com.jetsup.home_iot.utils.Constants.HOME_LOG_TAG;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ArrayAdapter;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.jetsup.home_iot.MainActivity;
import com.jetsup.home_iot.R;
import com.jetsup.home_iot.utils.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApplianceAddActivity extends AppCompatActivity {
    TextInputLayout textInputLayoutName;
    TextInputEditText textInputEditTextName;
    TextInputLayout textInputLayoutPin;
    TextInputEditText textInputEditTextPin;
    AppCompatSpinner spinnerCategory;
    AppCompatSpinner spinnerSignal;
    AppCompatButton btnAdd;

    String applianceName;
    boolean isDigital;
    int pin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_appliance_add);

        // Initialize the toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Add Appliance");

        // Enable the back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        textInputLayoutName = findViewById(R.id.tlApplianceName);
        textInputEditTextName = findViewById(R.id.etApplianceName);

        textInputLayoutPin = findViewById(R.id.tlAppliancePin);
        textInputEditTextPin = findViewById(R.id.etAppliancePin);

        spinnerCategory = findViewById(R.id.spApplianceCategory);
        spinnerSignal = findViewById(R.id.spApplianceSignal);

        // set the category spinner adapter to a String array of device categorie
        spinnerCategory.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, Constants.DEVICE_CATEGORIES));

        btnAdd = findViewById(R.id.btnApplianceAdd);

        textInputEditTextPin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0) return;

                if (!s.toString().matches("^[1-9][0-9]*$")) {
                    // Keep only valid numbers
                    String validInput = s.toString().replaceAll("[^1-9]*", "");
                    textInputEditTextPin.setText(validInput);

                    textInputLayoutPin.setError("Only numbers allowed starting with 1-9!");
                    textInputEditTextPin.setSelection(validInput.length());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        btnAdd.setOnClickListener(v -> {
            applianceName = Objects.requireNonNull(textInputEditTextName.getText()).toString();

            if (Objects.requireNonNull(textInputEditTextPin.getText()).toString().isEmpty()) {
                textInputLayoutPin.setError("Pin cannot be empty!");
                textInputEditTextPin.requestFocus();
                return;
            } else {
                textInputLayoutPin.setError(null);
            }
            pin = Integer.parseInt(Objects.requireNonNull(textInputEditTextPin.getText()).toString());
            isDigital = spinnerSignal.getSelectedItemPosition() == 0;

            if (applianceName.isEmpty()) {
                textInputLayoutName.setError("Appliance name cannot be empty!");
                textInputEditTextName.requestFocus();
                return;
            } else {
                textInputLayoutName.setError(null);
            }

            // TODO: move this inside TextWatcher
            int intPin = Integer.parseInt(Objects.requireNonNull(textInputEditTextPin.getText()).toString());
            if (!Arrays.stream(ESP32_ALLOWED_IO_PINS).anyMatch(i -> i == intPin) &&
                    !Arrays.stream(ESP32_ALLOWED_O_PINS).anyMatch(i -> i == intPin)
            ) {
                textInputLayoutPin.setError("Pin is not allowed!");
                textInputEditTextPin.requestFocus();
                return;
            } else {
                textInputLayoutPin.setError(null);
            }

            // wrap all this data in a JSONObject and send it to the server
            new Thread(() -> {
                JSONObject json = new JSONObject();
                try {
                    json.put(Constants.JSON_APPLIANCE_NAME, applianceName);
                    json.put(Constants.JSON_APPLIANCE_IS_DIGITAL, isDigital);
                    json.put(Constants.JSON_APPLIANCE_PIN, pin);
                    json.put(Constants.JSON_APPLIANCE_CATEGORY,
                            spinnerCategory.getSelectedItem().toString().toLowerCase());

                    // send the JSONObject to the server
                    RequestBody reqBody = RequestBody.create(json.toString(),
                            MediaType.parse("application/json"));
                    MainActivity.request = new Request.Builder()
                            .post(reqBody)
                            .url(MainActivity.serverIPAddress + Constants.API_ENDPOINT_DEVICE_ADD)
                            .build();
                    try (Response response = MainActivity.client.newCall(MainActivity.request).execute()) {
                        if (!response.isSuccessful()) {
                            Log.e(HOME_LOG_TAG, "Unexpected code " + response);
                            return;
                        }

                        if (response.body() == null) {
                            Log.e(HOME_LOG_TAG, "Response body is null");
                            return;
                        }

                        Log.e(HOME_LOG_TAG, "Response: " + response.body().string());
                        runOnUiThread(this::finish);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(HOME_LOG_TAG, "Error: " + e.getMessage());
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.e(HOME_LOG_TAG, "Device Add JSON Error: " + e.getMessage());
                }
            }).start();
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // process back button pressed from actionbar
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}