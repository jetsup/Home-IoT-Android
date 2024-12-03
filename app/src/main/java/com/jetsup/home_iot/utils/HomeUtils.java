package com.jetsup.home_iot.utils;

import static com.jetsup.home_iot.MainActivity.client;
import static com.jetsup.home_iot.MainActivity.lastDataReceiveTime;
import static com.jetsup.home_iot.MainActivity.request;
import static com.jetsup.home_iot.MainActivity.serverIPAddress;
import static com.jetsup.home_iot.MainActivity.serverReachable;

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

import okhttp3.Request;
import okhttp3.Response;

public class HomeUtils {
    public static int[] ESP32_ALLOWED_IO_PINS = {2, 4, 5, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33};
    public static int[] ESP32_ALLOWED_I_PINS = {34, 35, 36, 39};
    public static int[] ESP32_ALLOWED_O_PINS = {12};

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
        String pingURL = "http://" + ipHostname + "/api/v1/ping/";
        final Request req = new Request.Builder()
                .url(pingURL)
                .build();

        try (Response response = client.newCall(req).execute()) {
            Log.i("MyTag", "Response: " + response);

            if (response.isSuccessful()) {
                serverReachable = true;
                lastDataReceiveTime = System.currentTimeMillis();

                request = new Request.Builder()
                        .url(serverIPAddress + "stats/")
                        .build();
            } else {

                serverReachable = false;
                Log.e("MyTag", "Server is not reachable");
                Toast.makeText(activity, "Server is not reachable", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            serverReachable = false;
            Log.e("MyTag", "Error: " + e.getMessage());
        }

        return serverReachable;
    }

}
