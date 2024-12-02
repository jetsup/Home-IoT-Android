package com.jetsup.home_iot;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.ekn.gruzer.gaugelibrary.HalfGauge;
import com.ekn.gruzer.gaugelibrary.Range;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.jetsup.home_iot.screens.AboutActivity;
import com.jetsup.home_iot.screens.AppliancesActivity;
import com.jetsup.home_iot.screens.DeletedAppliancesActivity;
import com.jetsup.home_iot.screens.SettingsActivity;
import com.jetsup.home_iot.utils.HomeUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
import java.util.Objects;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private static final long DATA_LATENCY_BEFORE_CHECKING_CONNECTION = 1000;
    public static String ipHostname;
    public static volatile boolean serverReachable = false;
    public static volatile boolean shouldQuery = false;
    public static OkHttpClient client = new OkHttpClient();
    public static Request request;
    public static String serverIPAddress;
    public static long lastDataReceiveTime;
    private static volatile boolean mainThreadServerRun = false;
    TextView tvTime;
    TextView tvDate;
    HalfGauge gaugeTemperature;
    HalfGauge gaugeHumidity;
    AppCompatButton btnConnectToServer;
    TextInputEditText etServerAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.drawer_activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvTime = findViewById(R.id.tvTime);
        tvDate = findViewById(R.id.tvDate);

        gaugeTemperature = findViewById(R.id.gaugeTemperature);
        gaugeHumidity = findViewById(R.id.gaugeHumidity);
        btnConnectToServer = findViewById(R.id.btnConnectServer);
        etServerAddress = findViewById(R.id.etServerAddress);

        NavigationView navigationView = findViewById(R.id.main_activity_navigation_view);
        navigationView.setNavigationItemSelectedListener(item -> {
            /*if (item.getItemId() == R.id.nav_home) {
                Toast.makeText(this, "Home", Toast.LENGTH_SHORT).show();
            } else*/
            if (item.getItemId() == R.id.nav_appliances) {
                if (!serverReachable) {
                    Toast.makeText(this, "Server not reachable", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Appliances", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(MainActivity.this, AppliancesActivity.class));
                }
            } else if (item.getItemId() == R.id.nav_deleted_appliances) {
                if (!serverReachable) {
                    Toast.makeText(this, "Server not reachable", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Deleted Appliances", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(MainActivity.this, DeletedAppliancesActivity.class));
                }
            } else if (item.getItemId() == R.id.nav_settings) {
                Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            } else if (item.getItemId() == R.id.nav_about) {
                Toast.makeText(this, "About", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MainActivity.this, AboutActivity.class));
            }
            // close the drawer
            DrawerLayout drawer = findViewById(R.id.drawer_activity_main);
            drawer.closeDrawers();
            return true;
        });

        btnConnectToServer.setOnClickListener(v -> {
            // TODO: check if the device is connected to WiFi network
            if (btnConnectToServer.getText().toString().equalsIgnoreCase("connect")) {
                shouldQuery = true;
                if (!HomeUtils.isWiFiNetworkConnected(MainActivity.this)) {
                    Toast.makeText(MainActivity.this, "Please connect to WiFi network", Toast.LENGTH_LONG).show();
                    HomeUtils.showWifiSettingsDialog(MainActivity.this);
                    return;
                }

                if (Objects.requireNonNull(etServerAddress.getText()).toString().isEmpty()) {
                    etServerAddress.setError("Please enter server address");
                    return;
                }

                //  Check if IP address is valid
                ipHostname = etServerAddress.getText().toString().trim();
                // match ipaddress or hostname.local
                // FIXME: The hostname.local is not working
                if (!ipHostname.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}") &&
                        !ipHostname.matches("\\w+\\.local")) {
                    etServerAddress.setError("Please enter a valid IP address");
                    return;
                }
                new Thread(() -> {
                    while (!serverReachable) {
                        if (!HomeUtils.pingServer(MainActivity.this, ipHostname)) {
                            MainActivity.this.runOnUiThread(() -> {
                                Toast.makeText(MainActivity.this, "The server 'http://" + ipHostname + "' did not respond!",
                                        Toast.LENGTH_SHORT).show();
                                //
                                etServerAddress.setError("Server is not reachable");
                                etServerAddress.setEnabled(true);

                                btnConnectToServer.setText(R.string.connect);
                            });
                        } else {
                            runOnUiThread(() -> {
                                etServerAddress.setError(null);
                                etServerAddress.setEnabled(false);
                                mainThreadServerRun = true;

                                btnConnectToServer.setText(R.string.disconnect);
                            });
                        }

                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }).start();

                serverIPAddress = "http://" + ipHostname + "/api/v1/";
            } else { // disconnect
                shouldQuery = false;
                serverReachable = false;
                btnConnectToServer.setText(R.string.connect);
                etServerAddress.setEnabled(true);
            }
        });

        gaugeTemperature.enableAnimation(true);
        gaugeTemperature.setMaxValue(45);
        gaugeTemperature.setMinValue(15);
        gaugeTemperature.setEnableNeedleShadow(true);
        gaugeTemperature.setNeedleColor(0xffc4c4ff);
        gaugeTemperature.setValueColor(0xffcfc4ff);

        gaugeHumidity.enableAnimation(true);
        gaugeHumidity.setMaxValue(70);
        gaugeHumidity.setMinValue(20);
        gaugeHumidity.setEnableNeedleShadow(true);
        gaugeHumidity.setNeedleColor(0xfff4c4cf);
        gaugeHumidity.setValueColor(0xfff4c4cf);


        // has three ranges cold warm and hot
        Range rangeTemperatureCold = new Range();
        rangeTemperatureCold.setColor(0xff53a0d4);
        rangeTemperatureCold.setFrom(10);
        rangeTemperatureCold.setTo(22);
        gaugeTemperature.addRange(rangeTemperatureCold);
        Range rangeTemperatureWarm = new Range();
        rangeTemperatureWarm.setColor(0xFFb57c36);
        rangeTemperatureWarm.setFrom(22);
        rangeTemperatureWarm.setTo(30);
        gaugeTemperature.addRange(rangeTemperatureWarm);
        Range rangeTemperatureHot = new Range();
        rangeTemperatureHot.setColor(0xFFcc501b);
        rangeTemperatureHot.setFrom(30);
        rangeTemperatureHot.setTo(50);
        gaugeTemperature.addRange(rangeTemperatureHot);
        gaugeTemperature.setFormatter(value -> String.format(Locale.getDefault(), "%.1f°C", value));

        // has two ranges dry and humid colored with green and red
        Range rangeHumidityDry = new Range();
        rangeHumidityDry.setColor(0xFF00FF00);
        rangeHumidityDry.setFrom(gaugeHumidity.getMinValue());
        rangeHumidityDry.setTo(gaugeHumidity.getMinValue() + 20);
        gaugeHumidity.addRange(rangeHumidityDry);
        Range rangeHumidityHumid = new Range();
        rangeHumidityHumid.setColor(0xFFFF0000);
        rangeHumidityHumid.setFrom(gaugeHumidity.getMinValue() + 20);
        rangeHumidityHumid.setTo(gaugeHumidity.getMaxValue());
        gaugeHumidity.addRange(rangeHumidityHumid);
        gaugeHumidity.setFormatter(value -> String.format(Locale.getDefault(), "%.1f%%", value));

        new Thread(() -> {
            double temperature, previousTemperature = 0;
            double humidity, previousHumidity = 0;
            String time;
            String date;

            while (true) {
                if (mainThreadServerRun && serverReachable && shouldQuery &&
                        lastDataReceiveTime < System.currentTimeMillis() - DATA_LATENCY_BEFORE_CHECKING_CONNECTION) {
                    try (Response response = client.newCall(request).execute()) {
                        if (!response.isSuccessful()) {
                            Log.e("MyTag", "Unexpected code " + response);
                            continue;
                        }

                        if (response.body() == null) {
                            Log.d("MyTag", "Response body is null");
                            continue;
                        }

                        String body = response.body().string();
                        Log.i("MyTag", "Response: " + body);

                        // parse JSON
                        try {
                            JSONObject json = new JSONObject(body);
                            temperature = json.getDouble("temperature");
                            humidity = json.getDouble("humidity");
                            time = json.getString("time");
                            date = json.getString("date");
                        } catch (JSONException e) {
                            Log.e("MyTag", "JSON Error: " + e.getMessage());
                            continue;
                        }
                        // convert date to human readable format dd/MM/yyyy
                        date = date.substring(8) + "/" + date.substring(5, 7) + "/" + date.substring(0, 4);

                        previousTemperature = temperature != 0 ? temperature : previousTemperature;
                        previousHumidity = humidity != 0 ? humidity : previousHumidity;

                        // update UI
                        double finalTemperature = temperature != 0 ? temperature : previousTemperature;
                        double finalHumidity = humidity != 0 ? humidity : previousHumidity;
                        String finalTime = time;
                        String finalDate = date;
                        runOnUiThread(() -> {
                            tvTime.setText(finalTime);
                            tvDate.setText(finalDate);
                            gaugeTemperature.setValue(finalTemperature);
                            gaugeHumidity.setValue(finalHumidity);
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        HomeUtils.pingServer(MainActivity.this, ipHostname);

                        Log.e("MyTag", "Thread Error: " + e.getMessage());
                    }

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    if (ipHostname != null && shouldQuery && !serverReachable) {
                        Log.i("MyTag", "While Else: " + ipHostname);
                        HomeUtils.pingServer(MainActivity.this, ipHostname);
                    }
                }
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (serverReachable) {
            request = new Request.Builder()
                    .url(serverIPAddress + "stats/")
                    .build();

            mainThreadServerRun = true;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        mainThreadServerRun = false;
    }
}