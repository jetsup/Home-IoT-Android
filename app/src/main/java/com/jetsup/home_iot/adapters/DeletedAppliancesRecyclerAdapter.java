package com.jetsup.home_iot.adapters;

import static com.jetsup.home_iot.utils.Constants.HOME_LOG_TAG;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.jetsup.home_iot.MainActivity;
import com.jetsup.home_iot.R;
import com.jetsup.home_iot.models.Appliance;
import com.jetsup.home_iot.screens.DeletedAppliancesActivity;
import com.jetsup.home_iot.utils.Constants;
import com.jetsup.home_iot.utils.HomeUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DeletedAppliancesRecyclerAdapter extends
        RecyclerView.Adapter<DeletedAppliancesRecyclerAdapter.ApplianceViewHolder> {
    Context context;
    List<Appliance> appliances;

    public DeletedAppliancesRecyclerAdapter(Context context, List<Appliance> appliances) {
        this.context = context;
        this.appliances = appliances;
    }

    public void setAppliances(List<Appliance> appliances) {
        this.appliances = appliances;
    }

    @NonNull
    @Override
    public DeletedAppliancesRecyclerAdapter.ApplianceViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                                                   int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_deleted_appliance_card,
                parent, false);
        return new ApplianceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeletedAppliancesRecyclerAdapter.ApplianceViewHolder holder,
                                 int position) {
        String applianceNameHolder = appliances.get(position).getApplianceName() +
                " (" + appliances.get(position).getPin() + ")";
        holder.applianceName.setText(applianceNameHolder);
        holder.applianceImage.setImageResource(HomeUtils.getDrawableByCategory(appliances.get(position).getCategory()));

        holder.btnDeleteForever.setOnClickListener(v -> {
            // update to the server also
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Delete " + appliances.get(position).getApplianceName() +
                            " [" + appliances.get(position).getPin() + "]?")
                    .setMessage("Are you sure you want to delete this appliance?\n\nThis process can not be undone!")
                    .setPositiveButton("Delete", (dialog, which) -> deleteAppliancePermanently(position))
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .setCancelable(true)
                    .show();
        });

        holder.btnRestore.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Delete " + appliances.get(position).getApplianceName() +
                            " [" + appliances.get(position).getPin() + "]?")
                    .setMessage("Are you sure you want to restore this appliance?")
                    .setPositiveButton("Restore", (dialog, which) -> restoreAppliance(position))
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .setCancelable(true)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return appliances.size();
    }

    private void deleteAppliancePermanently(int position) {
        JSONObject json = new JSONObject();
        try {
            json.put(Constants.JSON_APPLIANCE_PIN, appliances.get(position).getPin());
            json.put(Constants.JSON_APPLIANCE_DELETE_PERMANENT, 1);

            new Thread(() -> {
                RequestBody reqBody = RequestBody.create(json.toString(), MediaType.get("application/json; charset=utf-8"));
                MainActivity.request = new Request.Builder()
                        .url(MainActivity.serverIPAddress + Constants.API_ENDPOINT_DEVICE_DELETE)
                        .post(reqBody)
                        .build();
                try (Response response = MainActivity.client.newCall(MainActivity.request).execute()) {
                    if (!response.isSuccessful()) {
                        Log.e(HOME_LOG_TAG, "Unexpected code " + response);
                    } else {
                        if (response.body() == null) {
                            Log.e(HOME_LOG_TAG, "Response body is null");
                            return;
                        }
                        Log.i(HOME_LOG_TAG, "Response: " + response.body().string());
                        // reload the deleted appliances
                        new Thread(() -> ((DeletedAppliancesActivity) context).getDeletedAppliances()).start();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(HOME_LOG_TAG, "Error: " + e.getMessage());
                }
            }).start();
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(HOME_LOG_TAG, "JSON Error: " + e.getMessage());
        }
    }

    private void restoreAppliance(int position) {
        JSONObject json = new JSONObject();
        try {
            json.put(Constants.JSON_APPLIANCE_PIN, appliances.get(position).getPin());
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(HOME_LOG_TAG, "Device Restore JSON Error: " + e.getMessage());
            return;
        }

        new Thread(() -> {
            RequestBody reqBody = RequestBody.create(json.toString(), MediaType.get("application/json; charset=utf-8"));
            MainActivity.request = new Request.Builder()
                    .url(MainActivity.serverIPAddress + Constants.API_ENDPOINT_DEVICE_RESTORE)
                    .post(reqBody)
                    .build();

            try (Response response = MainActivity.client.newCall(MainActivity.request).execute()) {
                if (!response.isSuccessful()) {
                    Log.e(HOME_LOG_TAG, "Unexpected code " + response);
                } else {
                    if (response.body() == null) {
                        Log.e(HOME_LOG_TAG, "Response body is null");
                        return;
                    }

                    Log.i(HOME_LOG_TAG, "Response: " + response.body().string());
                    // reload the deleted appliances
                    new Thread(() -> ((DeletedAppliancesActivity) context).getDeletedAppliances()).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(HOME_LOG_TAG, "Error: " + e.getMessage());
            }
        }).start();
    }

    public static class ApplianceViewHolder extends RecyclerView.ViewHolder {
        CardView applianceCard;
        ImageView applianceImage;
        TextView applianceName;
        AppCompatButton btnDeleteForever;
        AppCompatButton btnRestore;

        public ApplianceViewHolder(@NonNull View itemView) {
            super(itemView);

            applianceCard = itemView.findViewById(R.id.deletedApplianceCard);
            applianceImage = itemView.findViewById(R.id.applianceImage);
            applianceName = itemView.findViewById(R.id.applianceName);
            btnDeleteForever = itemView.findViewById(R.id.btnDeleteApplianceForever);
            btnRestore = itemView.findViewById(R.id.btnRestoreAppliance);
        }
    }
}
