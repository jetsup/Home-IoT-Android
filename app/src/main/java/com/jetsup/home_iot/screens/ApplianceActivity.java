package com.jetsup.home_iot.screens;

import static com.jetsup.home_iot.utils.HomeUtils.ESP32_ALLOWED_IO_PINS;
import static com.jetsup.home_iot.utils.HomeUtils.ESP32_ALLOWED_O_PINS;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApplianceActivity extends AppCompatActivity {
    String applianceName;
    boolean isDigital;
    int pin, oldPin;
    int value;

    TextInputLayout textInputLayoutName;
    TextInputEditText textInputEditTextName;
    TextInputLayout textInputLayoutPin;
    TextInputEditText textInputEditTextPin;
    AppCompatSpinner spinnerType;
    AppCompatButton btnUpdate;
    AppCompatButton btnDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_appliance);

        applianceName = getIntent().getStringExtra("applianceName");
        isDigital = getIntent().getBooleanExtra("isDigital", true);
        pin = getIntent().getIntExtra("pin", 0);
        oldPin = pin;
        value = getIntent().getIntExtra("value", 0);

        // Initialize the toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle(applianceName);

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

        spinnerType = findViewById(R.id.spApplianceType);

        btnUpdate = findViewById(R.id.btnApplianceUpdate);
        btnDelete = findViewById(R.id.btnApplianceDelete);

        textInputEditTextName.setText(applianceName);
        textInputEditTextPin.setText(String.valueOf(pin));

        if (isDigital) {
            spinnerType.setSelection(0);
        } else {
            spinnerType.setSelection(1);
        }

        textInputEditTextPin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0) return;

                if (!s.toString().matches("^[1-9][0-9]*$")) {
                    String validInput = s.toString().replaceAll("[^1-9]*", ""); // Keep only valid numbers
                    textInputEditTextPin.setText(validInput);

                    textInputLayoutPin.setError("Only numbers allowed starting with 1-9!");
                    textInputEditTextPin.setSelection(validInput.length());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        btnUpdate.setOnClickListener(v -> {
            applianceName = Objects.requireNonNull(textInputEditTextName.getText()).toString();

            if (Objects.requireNonNull(textInputEditTextPin.getText()).toString().isEmpty()) {
                textInputLayoutPin.setError("Pin cannot be empty!");
                textInputEditTextPin.requestFocus();
                return;
            } else {
                textInputLayoutPin.setError(null);
            }

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

            pin = Integer.parseInt(Objects.requireNonNull(textInputEditTextPin.getText()).toString());
            isDigital = spinnerType.getSelectedItemPosition() == 0;

            if (applianceName.isEmpty()) {
                textInputLayoutName.setError("Appliance name cannot be empty!");
                textInputEditTextName.requestFocus();
                return;
            } else {
                textInputLayoutName.setError(null);
            }

            if (pin < 1 || pin > 32) {
                textInputLayoutPin.setError("Pin must be between 1 and 32!");
                textInputEditTextPin.requestFocus();
                return;
            } else {
                textInputLayoutPin.setError(null);
            }

            // wrap all this data in a JSONObject and send it to the server
            new Thread(() -> {
                JSONObject json = new JSONObject();
                try {
                    if (isDigital) {
                        value = (value >= 1) ? 1 : 0;
                    }

                    json.put("name", applianceName);
                    json.put("is_digital", isDigital);
                    json.put("pin", pin);
                    json.put("old_pin", oldPin);
                    json.put("value", value);

                    // send the JSONObject to the server
                    RequestBody reqBody = RequestBody.create(json.toString(), MediaType.parse("application/json"));
                    MainActivity.request = new Request.Builder().post(reqBody).url(MainActivity.serverIPAddress + "device/update/").build();
                    try (Response response = MainActivity.client.newCall(MainActivity.request).execute()) {
                        if (!response.isSuccessful()) {
                            Log.e("MyTag", "Unexpected code " + response);
                            return;
                        }

                        if (response.body() == null) {
                            Log.e("MyTag", "Response body is null");
                            return;
                        }

                        Log.e("MyTag", "Response: " + response.body().string());
                        runOnUiThread(this::finish);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e("MyTag", "Error: " + e.getMessage());
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.e("MyTag", "JSON Error: " + e.getMessage());
                }
            }).start();
        });

        btnDelete.setOnClickListener(v -> {
            // wrap all this data in a JSONObject and send it to the server
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Delete " + applianceName + " [" + pin + "]?")
                    .setMessage("Are you sure you want to move this appliance to trash?")
                    .setPositiveButton("Proceed", (dialog, which) -> proceedDelete())
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .setCancelable(true)
                    .show();
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

    private void proceedDelete() {
        new Thread(() -> {
            JSONObject json = new JSONObject();
            try {
                if (isDigital) {
                    value = (value >= 1) ? 1 : 0;
                }

                json.put("pin", oldPin);

                // send the JSONObject to the server
                RequestBody reqBody = RequestBody.create(json.toString(), MediaType.parse("application/json"));
                MainActivity.request = new Request.Builder().post(reqBody).url(MainActivity.serverIPAddress + "device/delete/").build();
                try (Response response = MainActivity.client.newCall(MainActivity.request).execute()) {
                    if (!response.isSuccessful()) {
                        Log.e("MyTag", "Unexpected code " + response);
                    }

                    if (response.body() == null) {
                        Log.e("MyTag", "Response body is null");
                        return;
                    }

                    Log.e("MyTag", "Response: " + response.body().string());
                    runOnUiThread(this::finish);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("MyTag", "Error: " + e.getMessage());
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e("MyTag", "JSON Error: " + e.getMessage());
            }
        }).start();
    }
}