package com.jetsup.home_iot.models;

public class Appliance {
    String applianceName;
    boolean isDigital;
    String category;
    int pin;

    int value;

    public Appliance(String applianceName, boolean isDigital, int pin, int value, String category) {
        this.applianceName = applianceName;
        this.isDigital = isDigital;
        this.pin = pin;
        this.value = value;
        this.category = category.toLowerCase();
    }

    public String getApplianceName() {
        return applianceName;
    }

    public void setApplianceName(String applianceName) {
        this.applianceName = applianceName;
    }

    public boolean isDigital() {
        return isDigital;
    }

    public void setDigital(boolean digital) {
        isDigital = digital;
    }

    public int getPin() {
        return pin;
    }

    public void setPin(int pin) {
        this.pin = pin;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
