package com.jetsup.home_iot.screens;

import static com.jetsup.home_iot.utils.Constants.HOME_LOG_TAG;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.jetsup.home_iot.MainActivity;
import com.jetsup.home_iot.R;
import com.jetsup.home_iot.utils.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Objects;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SettingsActivity extends AppCompatActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private static final String TITLE_TAG = "settingsActivityTitle";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new HeaderFragment())
                    .commit();
        } else {
            setTitle(savedInstanceState.getCharSequence(TITLE_TAG));
        }
        getSupportFragmentManager().addOnBackStackChangedListener(
                new FragmentManager.OnBackStackChangedListener() {
                    @Override
                    public void onBackStackChanged() {
                        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                            setTitle(R.string.title_activity_settings);
                        }
                    }
                });
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save current activity title so we can set it again after a configuration change
        outState.putCharSequence(TITLE_TAG, getTitle());
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (getSupportFragmentManager().popBackStackImmediate()) {
            return true;
        }
        return super.onSupportNavigateUp();
    }

    @Override
    public boolean onPreferenceStartFragment(@NonNull PreferenceFragmentCompat caller, Preference pref) {
        // Instantiate the new Fragment
        final Bundle args = pref.getExtras();
        final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
                getClassLoader(),
                Objects.requireNonNull(pref.getFragment()));
        fragment.setArguments(args);
        fragment.setTargetFragment(caller, 0);
        // Replace the existing Fragment with the new Fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings, fragment)
                .addToBackStack(null)
                .commit();
        setTitle(pref.getTitle());
        return true;
    }

    public static class HeaderFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.header_preferences, rootKey);
        }
    }

    public static class SystemSettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.system_settings_preferences, rootKey);

            // Find the EditTextPreference by key
            EditTextPreference ipPreference = findPreference(getString(R.string.pref_key_server_ip_address));
            if (ipPreference != null) {
                ipPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                        String ipAddress = newValue.toString();

                        if (isValidIpAddress(ipAddress)) {
                            return true; // Valid IP, accept the change
                        } else {
                            // Show an error message
                            Toast.makeText(getContext(), "Invalid IP Address. Please try again.",
                                    Toast.LENGTH_LONG).show();
                            return false; // Reject the change
                        }
                    }
                });
            }

            // Find the reset preference by its key
            Preference resetPreference = findPreference(getString(R.string.pref_key_reset));
            if (resetPreference != null) {
                resetPreference.setOnPreferenceClickListener(preference -> {
                    showConfirmationDialog();
                    return true;
                });
            }
        }

        private boolean isValidIpAddress(String ipAddress) {
            if (TextUtils.isEmpty(ipAddress)) {
                return false;
            }
            // Android's built-in regex for IPv4 validation
            return Patterns.IP_ADDRESS.matcher(ipAddress).matches();
        }

        private void showConfirmationDialog() {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Confirm Reset")
                    .setMessage("Are you sure you want to reset the system? \n\n" +
                            "This action will delete all your devices and turn the whole system down.\n\n" +
                            "This action cannot be undone.")
                    .setPositiveButton("OK", (dialog, which) -> {
                        // Execute POST request on confirmation
                        if (MainActivity.serverReachable) {
                            performSystemReset();
                        } else {
                            Toast.makeText(requireContext(),
                                    "Server is not reachable. Please check the IP address.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setCancelable(true)
                    .show();
        }

        private void performSystemReset() {
            JSONObject json = new JSONObject();
            try {
                json.put(Constants.JSON_APPLIANCE_RESET, true);
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(HOME_LOG_TAG, "Devices Reset JSON Error: " + e.getMessage());
                return;
            }

            new Thread(() -> {
                try {
                    RequestBody reqBody = RequestBody.create(json.toString(), MediaType.parse("application/json"));
                    MainActivity.request = new Request.Builder()
                            .post(reqBody)
                            .url(MainActivity.serverIPAddress + Constants.API_ENDPOINT_DEVICES_RESET)
                            .build();

                    try (Response response = MainActivity.client.newCall(MainActivity.request).execute()) {
                        if (!response.isSuccessful()) {
                            Log.e(HOME_LOG_TAG, "Unexpected code " + response);
                        }

                        if (response.body() == null) {
                            Log.e(HOME_LOG_TAG, "Response body is null");
                            return;
                        }

                        Log.e(HOME_LOG_TAG, "Response: " + response.body().string());

                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(), "System reset successfully.",
                                        Toast.LENGTH_SHORT).show()
                        );
                    } catch (IOException e) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(), "Reset failed. Please try again.",
                                        Toast.LENGTH_SHORT).show()
                        );

                        e.printStackTrace();
                        Log.e(HOME_LOG_TAG, "System Reset Error: " + e.getMessage());
                    }
                } catch (Exception e) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "Sys Reset Error: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show()
                    );
                }
            }).start();
        }
    }

    public static class SyncFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.sync_preferences, rootKey);
        }
    }
}