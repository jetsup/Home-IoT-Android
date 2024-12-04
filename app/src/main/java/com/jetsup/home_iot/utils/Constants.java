package com.jetsup.home_iot.utils;

public class Constants {
    // Logs
    public static final String HOME_LOG_TAG = "MyTag";
    public static final String JSON_APPLIANCE_DELETE_PERMANENT = "delete_permanent";
    public static final String API_ENDPOINT_DEVICE_RESTORE = "device/restore/";
    // Pins
    public static int[] ESP32_ALLOWED_IO_PINS = {2, 4, 5, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22,
            23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33};
    public static int[] ESP32_ALLOWED_I_PINS = {34, 35, 36, 39};
    public static int[] ESP32_ALLOWED_O_PINS = {12};
    // api endpoints
    public static String API_ENDPOINT_ROOT = "/api/v1/";
    public static String API_ENDPOINT_PING = "ping/";
    public static String API_ENDPOINT_STATS = "stats/";
    public static String API_ENDPOINT_DEVICES = "devices/";
    public static String API_ENDPOINT_DEVICES_DELETED = "devices/deleted/";
    public static String API_ENDPOINT_DEVICES_RESET = "devices/reset/";
    public static String API_ENDPOINT_DEVICE = "device/";
    public static String API_ENDPOINT_DEVICE_ADD = "device/add/";
    public static String API_ENDPOINT_DEVICE_VALUE = "device/value/";
    public static String API_ENDPOINT_DEVICE_UPDATE = "device/update/";
    public static String API_ENDPOINT_DEVICE_DELETE = "device/delete/";
    // device types
    public static String[] DEVICE_CATEGORIES = {"Bulb", "Snake Lights", "Fan", "TV", "Refrigerator",
            "Washing Machine", "Heater", "Pressure Mattress", "Curtain"};
    // json keys
    public static String JSON_APPLIANCE_NAME = "name";
    public static String JSON_APPLIANCE_PIN = "pin";
    public static String JSON_APPLIANCE_VALUE = "value";
    public static String JSON_APPLIANCE_CATEGORY = "category";
    public static String JSON_APPLIANCE_IS_DIGITAL = "is_digital";
    public static String JSON_APPLIANCE_RESET = "reset";
}
