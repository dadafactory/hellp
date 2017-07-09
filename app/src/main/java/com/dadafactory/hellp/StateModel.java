package com.dadafactory.hellp;

import java.util.Calendar;

/**
 * Created by aleckim on 2017. 7. 9..
 */

public class StateModel {
    private String petName;
    private long startDate;
    private long foodDate;
    private boolean foodWater;
    private int petState; //1,2,3;

    public void setPetName(String petName) {
        this.petName = petName;
    }

    public String getPetName() {
        return petName;
    }

    public void setFoodDate(long foodDate) {
        this.foodDate = foodDate;
    }

    public long getFoodDate() {
        return foodDate;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public long getStartDate() {
        return startDate;
    }

    public void setFoodWater(boolean foodWater) {
        this.foodWater = foodWater;
    }

    public boolean isFoodWater() {
        return foodWater;
    }

    public void setPetState(int petState) {
        this.petState = petState;
    }

    public int getPetState() {
        return petState;
    }
}
