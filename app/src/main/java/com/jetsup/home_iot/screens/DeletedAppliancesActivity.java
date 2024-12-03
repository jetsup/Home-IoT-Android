package com.jetsup.home_iot.screens;

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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.deleted_appliances_main_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        recyclerView = findViewById(R.id.deleted_appliances_recycler_view);

        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        adapter = new DeletedAppliancesRecyclerAdapter(this, deletedAppliances);
        recyclerView.setAdapter(adapter);
    }

    public void getDeletedAppliances() {
        MainActivity.request = new Request.Builder()
                .url(MainActivity.serverIPAddress + "devices/deleted/")
                .build();

        try (Response response = MainActivity.client.newCall(MainActivity.request).execute()) {
            if (!response.isSuccessful()) {
                Log.e("MyTag", "Unexpected code " + response);
            } else {
                //{"appliances":[{"name":"LED 1","is_digital":true,"pin":12,"value":1},{"name":"Motor 1","is_digital":false,"pin":14,"value":0}]}
                if (response.body() != null) {
                    List<Appliance> returnedAppliances = new ArrayList<>();

                    String body = response.body().string();
                    Log.i("MyTag", "Response: " + body);
                    try {
                        JSONObject json = new JSONObject(body);
                        JSONArray appliancesJSON = json.getJSONArray("appliances");

                        // parse the returned array
                        for (int i = 0; i < appliancesJSON.length(); i++) {
                            String name;
                            boolean isDigital;
                            int pin;
                            int value;
                            try {
                                JSONObject applianceJSON = appliancesJSON.getJSONObject(i);

                                name = applianceJSON.getString("name");
                                isDigital = applianceJSON.getBoolean("is_digital");
                                pin = applianceJSON.getInt("pin");
                                value = applianceJSON.getInt("value");
                            } catch (JSONException e) {
                                e.printStackTrace();
                                Log.e("MyTag", "JSON Error: " + e.getMessage());
                                continue;
                            }

                            returnedAppliances.add(new Appliance(name, isDigital, pin, value));
                            Log.i("MyTag", "Appliance: " + name + " " + isDigital + " " + pin + " " + value);
                        }

                        runOnUiThread(() -> {
                            adapter.setAppliances(returnedAppliances);
                            adapter.notifyDataSetChanged();
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.e("MyTag", "JSON Error: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("MyTag", "Error: " + e.getMessage());
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