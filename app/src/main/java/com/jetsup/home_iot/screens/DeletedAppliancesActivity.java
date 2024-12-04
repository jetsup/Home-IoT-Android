package com.jetsup.home_iot.screens;

import static com.jetsup.home_iot.utils.Constants.HOME_LOG_TAG;

import android.os.Bundle;
import android.util.Log;
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

import com.jetsup.home_iot.MainActivity;
import com.jetsup.home_iot.R;
import com.jetsup.home_iot.adapters.DeletedAppliancesRecyclerAdapter;
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

public class DeletedAppliancesActivity extends AppCompatActivity {
    RecyclerView recyclerView;
    List<Appliance> deletedAppliances = new ArrayList<>();
    DeletedAppliancesRecyclerAdapter adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_deleted_appliances);

        // Initialize the toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle("Deleted Appliances");

        // Enable the back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.deleted_appliances_main_layout),
                (v, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                    return insets;
                });

        recyclerView = findViewById(R.id.deleted_appliances_recycler_view);

        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL,
                false));

        adapter = new DeletedAppliancesRecyclerAdapter(this, deletedAppliances);
        recyclerView.setAdapter(adapter);
    }

    public void getDeletedAppliances() {
        MainActivity.request = new Request.Builder()
                .url(MainActivity.serverIPAddress + Constants.API_ENDPOINT_DEVICES_DELETED)
                .build();

        try (Response response = MainActivity.client.newCall(MainActivity.request).execute()) {
            if (!response.isSuccessful()) {
                Log.e(HOME_LOG_TAG, "Unexpected code " + response);
            } else {
                /*
                {
                    "appliances":[
                                     {"name":"LED 1","is_digital":true,"pin":12,"value":1, "category": "Bulb"},
                                     {"name":"Motor 1","is_digital":false,"pin":14,"value":0, "category": "Fan"}
                                 ]
                 }
                 */
                if (response.body() != null) {
                    List<Appliance> returnedAppliances = new ArrayList<>();

                    String body = response.body().string();
                    Log.i(HOME_LOG_TAG, "Response: " + body);
                    try {
                        JSONObject json = new JSONObject(body);
                        JSONArray appliancesJSON = json.getJSONArray("appliances");

                        // parse the returned array
                        for (int i = 0; i < appliancesJSON.length(); i++) {
                            String name;
                            String category;
                            boolean isDigital;
                            int pin;
                            int value;
                            try {
                                JSONObject applianceJSON = appliancesJSON.getJSONObject(i);

                                name = applianceJSON.getString(Constants.JSON_APPLIANCE_NAME);
                                isDigital = applianceJSON.getBoolean(Constants.JSON_APPLIANCE_IS_DIGITAL);
                                pin = applianceJSON.getInt(Constants.JSON_APPLIANCE_PIN);
                                value = applianceJSON.getInt(Constants.JSON_APPLIANCE_VALUE);
                                category = applianceJSON.getString(Constants.JSON_APPLIANCE_CATEGORY);
                            } catch (JSONException e) {
                                e.printStackTrace();
                                Log.e(HOME_LOG_TAG, "JSON Error: " + e.getMessage());
                                continue;
                            }

                            returnedAppliances.add(new Appliance(name, isDigital, pin, value, category));
                            Log.i(HOME_LOG_TAG, "Appliance: " + name + " " + isDigital + " " + pin + " " + value);
                        }

                        runOnUiThread(() -> {
                            adapter.setAppliances(returnedAppliances);
                            adapter.notifyDataSetChanged();
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.e(HOME_LOG_TAG, "Devices Deleted JSON Error: " + e.getMessage());
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

        new Thread(this::getDeletedAppliances).start();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}