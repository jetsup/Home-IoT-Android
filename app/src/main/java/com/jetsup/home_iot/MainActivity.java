package com.jetsup.home_iot;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ekn.gruzer.gaugelibrary.HalfGauge;
import com.ekn.gruzer.gaugelibrary.Range;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
import java.util.Objects;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private static final long DATA_LATENCY_BEFORE_CHECKING_CONNECTION = 1000;
    static OkHttpClient client = new OkHttpClient();
    Request request;

    TextView tvTime;
    TextView tvDate;
    HalfGauge gaugeTemperature;
    HalfGauge gaugeHumidity;
    AppCompatButton btnConnectToServer;
    TextInputEditText etServerAddress;
    String serverIPAddress;
    private volatile boolean serverReachable = false;
    private volatile boolean shouldQuery = false;
    private String ipHostname;
    private long lastDataReceiveTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
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

        btnConnectToServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: check if the device is connected to WiFi network
                if (btnConnectToServer.getText().toString().equalsIgnoreCase("connect")) {
                    shouldQuery = true;
                    if (!MainActivity.this.isWiFiNetworkConnected()) {
                        Toast.makeText(MainActivity.this, "Please connect to WiFi network", Toast.LENGTH_LONG).show();
                        MainActivity.this.showWifiSettingsDialog();
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
                            if (!MainActivity.this.pingServer(ipHostname)) {
                                MainActivity.this.runOnUiThread(() -> {
                                    Toast.makeText(MainActivity.this, "The server 'http://" + ipHostname + "' did not respond!",
                                            Toast.LENGTH_SHORT).show();
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
                    btnConnectToServer.setText("Connect");
                    etServerAddress.setEnabled(true);
                }
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
                if (serverReachable && shouldQuery && lastDataReceiveTime < System.currentTimeMillis() - DATA_LATENCY_BEFORE_CHECKING_CONNECTION) {
                    try (Response response = client.newCall(request).execute()) {
                        if (!response.isSuccessful()) {
                            Log.e("MyTag", "Unexpected code " + response);
                        }

                        if (response.body() == null) {
                            Log.d("MyTag", "Response body is null");
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
                        pingServer(ipHostname);

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
                        pingServer(ipHostname);
                    }
                }
            }
        }).start();
    }

    private boolean pingServer(String ipHostname) {
        // send a get request to http://ipHostname/api/v1/ping/ using okHttp if the status is 200, then return true
        String pingURL = "http://" + ipHostname + "/api/v1/ping/";
        final Request req = new Request.Builder()
                .url(pingURL)
                .build();

        try (Response response = client.newCall(req).execute()) {
            Log.i("MyTag", "Response: " + response);

            if (response.isSuccessful()) {
                runOnUiThread(() -> {
                    etServerAddress.setError(null);
                    etServerAddress.setEnabled(false);

                    btnConnectToServer.setText("Disconnect");
                });
                serverReachable = true;
                lastDataReceiveTime = System.currentTimeMillis();

                request = new Request.Builder()
                        .url(serverIPAddress + "stats/")
                        .build();
            } else {
                runOnUiThread(() -> {
                    etServerAddress.setError("Server is not reachable");
                    etServerAddress.setEnabled(true);

                    btnConnectToServer.setText("Connect");
                });
                serverReachable = false;
                Log.e("MyTag", "Server is not reachable");
                Toast.makeText(this, "Server is not reachable", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> {
                etServerAddress.setError("Server is not reachable");
                etServerAddress.setEnabled(true);

                btnConnectToServer.setText("Connect");
            });
            serverReachable = false;
            Log.e("MyTag", "Error: " + e.getMessage());
        }

        return serverReachable;
    }

    private boolean isWiFiNetworkConnected() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            Network network = connectivityManager.getActiveNetwork();
            if (network != null) {
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
                if (capabilities != null) {
                    return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
                }
            }
        }
        return false;
    }

    private void showWifiSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Wi-Fi Required")
                .setMessage("Wi-Fi is not connected. Please enable Wi-Fi in settings.")
                .setPositiveButton("Go to Settings", (dialog, which) -> openWifiSettings())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
    }

    private void openWifiSettings() {
        Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
        this.startActivity(intent);
    }
}