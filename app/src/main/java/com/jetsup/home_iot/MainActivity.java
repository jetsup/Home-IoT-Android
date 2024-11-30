package com.jetsup.home_iot;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ekn.gruzer.gaugelibrary.HalfGauge;
import com.ekn.gruzer.gaugelibrary.Range;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

import java.util.Locale;
import java.util.Objects;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    TextView tvTime;
    TextView tvDate;

    HalfGauge gaugeTemperature;
    HalfGauge gaugeHumidity;

    AppCompatButton btnConnectToServer;
    TextInputEditText etServerAddress;
    String serverIPAddress;
    private Thread serverThread;

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

        btnConnectToServer.setOnClickListener(v -> {
            if (Objects.requireNonNull(etServerAddress.getText()).toString().isEmpty()) {
                etServerAddress.setError("Please enter server address");
                return;
            }

            //  Check if IP address is valid
            if (!etServerAddress.getText().toString().matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")) {
                etServerAddress.setError("Please enter a valid IP address");
                return;
            }

            serverIPAddress = etServerAddress.getText().toString().trim();
            if (serverThread.isAlive()) {
//                serverThread.interrupt();
            }
            // serverThread.start();
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

        serverThread = new Thread(new Runnable() {
            final OkHttpClient client = new OkHttpClient();
            final Request request = new Request.Builder()
                    // .url("http://" + serverIPAddress + "/api/v1/")
                    .url("http://192.168.181.140/api/v1/")
                    .build();

            @Override
            public void run() {
                double temperature, previousTemperature = 0;
                double humidity, previousHumidity = 0;
                String time;
                String date;

                while (true) {
                    try (Response response = client.newCall(request).execute()) {
                        if (!response.isSuccessful()) {
                            Log.e("M", "Unexpected code " + response);
                        }

                        if (response.body() == null) {
                            Log.d("M", "Response body is null");
                        }

                        String body = response.body().string();
                        Log.i("M", "Response: " + body);

                        // parse JSON
                        JSONObject json = new JSONObject(body);
                        temperature = json.getDouble("temperature");
                        humidity = json.getDouble("humidity");
                        time = json.getString("time");
                        date = json.getString("date");
                        // convert date to human readable format dd/MM/yyyy
                        date = date.substring(8) + "/" + date.substring(5, 7) + "/" + date.substring(0, 4);

                        previousTemperature = temperature != 0 ? temperature : previousTemperature;
                        previousHumidity = humidity != 0 ? humidity : previousHumidity;

                        Log.i("M", "Temperature: " + temperature);
                        Log.i("M", "Humidity: " + humidity);
                        Log.i("M", "Time: " + time);
                        Log.i("M", "Date: " + date);

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
                    }

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });

        serverThread.start();
    }
}