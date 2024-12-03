package com.jetsup.home_iot.adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.slider.Slider;
import com.jetsup.home_iot.MainActivity;
import com.jetsup.home_iot.R;
import com.jetsup.home_iot.models.Appliance;
import com.jetsup.home_iot.screens.ApplianceActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AppliancesRecyclerAdapter extends RecyclerView.Adapter<AppliancesRecyclerAdapter.ApplianceViewHolder> {
    Context context;
    List<Appliance> appliances;

    public AppliancesRecyclerAdapter(Context context, List<Appliance> appliances) {
        this.context = context;
        this.appliances = appliances;
    }

    public void setAppliances(List<Appliance> appliances) {
        this.appliances = appliances;
    }

    @NonNull
    @Override
    public AppliancesRecyclerAdapter.ApplianceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_appliance_card, parent, false);
        return new ApplianceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppliancesRecyclerAdapter.ApplianceViewHolder holder, int position) {
        String applianceNameHolder = appliances.get(position).getApplianceName() + " (" + appliances.get(position).getPin() + ")";
        holder.applianceName.setText(applianceNameHolder);
        holder.applianceImage.setImageResource(R.drawable.baseline_image_150);

        if (appliances.get(position).isDigital()) {
            holder.applianceSwitch.setVisibility(View.VISIBLE);
            holder.applianceSlider.setVisibility(View.GONE);

            holder.applianceSwitch.setChecked(appliances.get(position).getValue() >= 1);
            holder.applianceSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                appliances.get(position).setValue(isChecked ? 1 : 0);
                // update to the server also
                JSONObject json = new JSONObject();
                try {
                    json.put("pin", appliances.get(position).getPin());
                    json.put("value", appliances.get(position).getValue());
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.e("MyTag", "JSON Error: " + e.getMessage());
                }

                new Thread(() -> {
                    RequestBody reqBody = RequestBody.create(json.toString(), MediaType.get("application/json; charset=utf-8"));
                    MainActivity.request = new Request.Builder()
                            .url(MainActivity.serverIPAddress + "device/value/")
                            .post(reqBody)
                            .build();
                    try (Response response = MainActivity.client.newCall(MainActivity.request).execute()) {
                        if (!response.isSuccessful()) {
                            Log.e("MyTag", "Unexpected code " + response);
                        } else {
                            if (response.body() == null) {
                                Log.e("MyTag", "Response body is null");
                                return;
                            }
                            Log.i("MyTag", "Response: " + response.body().string());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e("MyTag", "Error: " + e.getMessage());
                    }
                }).start();

            });
        } else {
            holder.applianceSwitch.setVisibility(View.GONE);
            holder.applianceSlider.setVisibility(View.VISIBLE);

            holder.applianceSlider.setValue(appliances.get(position).getValue());
            holder.applianceSlider.addOnChangeListener((slider, value, fromUser) -> {
                appliances.get(position).setValue((int) value);
                // update to the server also
                JSONObject json = new JSONObject();
                try {
                    json.put("pin", appliances.get(position).getPin());
                    json.put("value", appliances.get(position).getValue());
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.e("MyTag", "JSON Error: " + e.getMessage());
                }

                new Thread(() -> {
                    RequestBody reqBody = RequestBody.create(json.toString(), MediaType.get("application/json; charset=utf-8"));
                    MainActivity.request = new Request.Builder()
                            .url(MainActivity.serverIPAddress + "device/value/")
                            .post(reqBody)
                            .build();
                    try (Response response = MainActivity.client.newCall(MainActivity.request).execute()) {
                        if (!response.isSuccessful()) {
                            Log.e("MyTag", "Unexpected code " + response);
                        } else {
                            if (response.body() == null) {
                                Log.e("MyTag", "Response body is null");
                                return;
                            }
                            Log.i("MyTag", "Response: " + response.body().string());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e("MyTag", "Error: " + e.getMessage());
                    }
                }).start();
            });
        }

        holder.applianceImage.setOnClickListener(v -> {
            // start a new activity and pass the appliance object to it
            Intent intent = new Intent(context, ApplianceActivity.class);
            intent.putExtra("applianceName", appliances.get(position).getApplianceName());
            intent.putExtra("isDigital", appliances.get(position).isDigital());
            intent.putExtra("pin", appliances.get(position).getPin());
            intent.putExtra("value", appliances.get(position).getValue());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return appliances.size();
    }

    public static class ApplianceViewHolder extends RecyclerView.ViewHolder {
        CardView applianceCard;
        ImageView applianceImage;
        TextView applianceName;
        SwitchCompat applianceSwitch;
        Slider applianceSlider;

        public ApplianceViewHolder(@NonNull View itemView) {
            super(itemView);

            applianceCard = itemView.findViewById(R.id.applianceCard);
            applianceImage = itemView.findViewById(R.id.applianceImage);
            applianceName = itemView.findViewById(R.id.applianceName);
            applianceSwitch = itemView.findViewById(R.id.applianceSwitch);
            applianceSlider = itemView.findViewById(R.id.applianceSlider);
        }
    }
}
