package com.jetsup.home_iot.utils;

import static com.jetsup.home_iot.MainActivity.client;
import static com.jetsup.home_iot.MainActivity.lastDataReceiveTime;
import static com.jetsup.home_iot.MainActivity.request;
import static com.jetsup.home_iot.MainActivity.serverIPAddress;
import static com.jetsup.home_iot.MainActivity.serverReachable;
import static com.jetsup.home_iot.utils.Constants.HOME_LOG_TAG;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.jetsup.home_iot.R;

import okhttp3.Request;
import okhttp3.Response;

public class HomeUtils {
    // Networking
    public static boolean isWiFiNetworkConnected(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

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

    public static void showWifiSettingsDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Wi-Fi Required")
                .setMessage("Wi-Fi is not connected. Please enable Wi-Fi in settings.")
                .setPositiveButton("Go to Settings", (dialog, which) -> openWifiSettings(context))
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
    }

    private static void openWifiSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
        context.startActivity(intent);
    }

    // Server
    public static boolean pingServer(Activity activity, String ipHostname) {
        // send a get request to http://ipHostname/api/v1/ping/ using okHttp if the status is 200, then return true
        String pingURL = "http://" + ipHostname + Constants.API_ENDPOINT_ROOT + Constants.API_ENDPOINT_PING;
        final Request req = new Request.Builder()
                .url(pingURL)
                .build();

        try (Response response = client.newCall(req).execute()) {
            Log.i(HOME_LOG_TAG, "Response: " + response);

            if (response.isSuccessful()) {
                serverReachable = true;
                lastDataReceiveTime = System.currentTimeMillis();

                // TODO: later add check for https
                if (serverIPAddress == null || !serverIPAddress.startsWith("http://" + ipHostname) ||
                        !serverIPAddress.endsWith(Constants.API_ENDPOINT_ROOT)) {
                    serverIPAddress = "http://" + ipHostname + Constants.API_ENDPOINT_ROOT;
                }

                request = new Request.Builder()
                        .url(serverIPAddress + Constants.API_ENDPOINT_STATS)
                        .build();
            } else {

                serverReachable = false;
                Log.e(HOME_LOG_TAG, "Server is not reachable");
                Toast.makeText(activity, "Server is not reachable", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            serverReachable = false;
            Log.e(HOME_LOG_TAG, "Ping Error: " + e.getMessage());
        }

        return serverReachable;
    }

    // Utility
    public static int getDrawableByCategory(String category) {
        /*"Bulb", "Snake Lights", "Fan", "TV", "Refrigerator",
            "Washing Machine", "Heater", "Pressure Mattress", "Curtain"*/
        switch (category) {
            case "bulb":
                return R.drawable.baseline_lightbulb_outline_150;
            case "snake lights":
                return R.drawable.baseline_straighten_150;
            case "fan":
                return R.drawable.baseline_mode_fan_off_150;
            case "tv":
                return R.drawable.baseline_live_tv_150;
            case "refrigerator":
                return R.drawable.baseline_kitchen_150;
            case "washing machine":
                return R.drawable.baseline_local_laundry_service_150;
            case "heater":
                return R.drawable.baseline_fireplace_150;
            case "pressure mattress":
                return R.drawable.baseline_bed_150;
            case "curtain":
                return R.drawable.baseline_curtains_150;
            default:
                return R.drawable.baseline_image_150;
        }
    }

}
