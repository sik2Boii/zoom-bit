package com.zoombit.domain;

import java.io.Serializable;

public class User implements Serializable {

    private String email; // 알림을 받을 이메일 주소
    private String market; // 종목 구분 코드
    private double targetPrice; // 알림을 받을 목표 시세

    public User() {
    }

    public User(String email, String preferredCurrency, double targetPrice) {
        this.email = email;
        this.market = preferredCurrency;
        this.targetPrice = targetPrice;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMarket() {
        return market;
    }

    public void setMarket(String market) {
        this.market = market;
    }

    public double getTargetPrice() {
        return targetPrice;
    }

    public void setTargetPrice(double targetPrice) {
        this.targetPrice = targetPrice;
    }

    @Override
    public String toString() {
        return "User{" +
                "email='" + email + '\'' +
                ", preferredCurrency='" + market + '\'' +
                ", targetPrice=" + targetPrice +
                '}';
    }

}
