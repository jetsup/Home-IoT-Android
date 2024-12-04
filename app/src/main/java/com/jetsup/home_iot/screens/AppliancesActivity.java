package com.jetsup.home_iot.screens;

import static com.jetsup.home_iot.utils.Constants.HOME_LOG_TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.jetsup.home_iot.MainActivity;
import com.jetsup.home_iot.R;
import com.jetsup.home_iot.adapters.AppliancesRecyclerAdapter;
import com.jetsup.home_iot.models.Appliance;
import com.jetsup.home_iot.utils.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import okhttp3.Request;
import okhttp3.Response;

public class AppliancesActivity extends AppCompatActivity {
    RecyclerView recyclerView;
    List<Appliance> appliances = new ArrayList<>();
    AppliancesRecyclerAdapter adapter;
    FloatingActionButton fabAddAppliance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_appliances);

        // Initialize the toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Appliances");

        // Enable the back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.appliances_main_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        recyclerView = findViewById(R.id.appliances_recycler_view);
        fabAddAppliance = findViewById(R.id.fabAddAppliance);

        fabAddAppliance.setOnClickListener(v -> {
            startActivity(new Intent(AppliancesActivity.this, ApplianceAddActivity.class));
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        adapter = new AppliancesRecyclerAdapter(this, appliances);
        recyclerView.setAdapter(adapter);
    }

    private void getAppliances() {
        MainActivity.request = new Request.Builder()
                .url(MainActivity.serverIPAddress + Constants.API_ENDPOINT_DEVICES)
                .build();

        try (Response response = MainActivity.client.newCall(MainActivity.request).execute()) {
            if (!response.isSuccessful()) {
                Log.e(HOME_LOG_TAG, "Unexpected code " + response);
            } else {
                //{"appliances":[{"name":"LED 1","is_digital":true,"pin":12,"value":1},{"name":"Motor 1","is_digital":false,"pin":14,"value":0}]}
                if (response.body() != null) {
                    List<Appliance> returnedAppliances = new ArrayList<>();

                    String body = response.body().string();
                    Log.i(HOME_LOG_TAG, "Response: " + body);
                    try {
                        JSONObject json = new JSONObject(body);
                        JSONArray appliancesJSON = json.getJSONArray("appliances");

                        // parse the appliances array
                        for (int i = 0; i < appliancesJSON.length(); i++) {
                            JSONObject applianceJSON = appliancesJSON.getJSONObject(i);

                            String name = applianceJSON.getString(Constants.JSON_APPLIANCE_NAME);
                            boolean isDigital = applianceJSON.getBoolean(Constants.JSON_APPLIANCE_IS_DIGITAL);
                            int pin = applianceJSON.getInt(Constants.JSON_APPLIANCE_PIN);
                            int value = applianceJSON.getInt(Constants.JSON_APPLIANCE_VALUE);
                            String category = applianceJSON.getString(Constants.JSON_APPLIANCE_CATEGORY);

                            returnedAppliances.add(new Appliance(name, isDigital, pin, value, category));
                            Log.i(HOME_LOG_TAG, "Appliance: " + name + " " + isDigital + " " + pin + " " + value);
                        }

                        runOnUiThread(() -> {
                            adapter.setAppliances(returnedAppliances);
                            adapter.notifyDataSetChanged();
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.e(HOME_LOG_TAG, "Devices JSON Error: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(HOME_LOG_TAG, "Error: " + e.getMessage());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        new Thread(this::getAppliances).start();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.appliance_deleted) {
            startActivity(new Intent(AppliancesActivity.this, DeletedAppliancesActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.appliances_options_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }
}