package com.zoombit.domain;

public class MarketInfo {

    private String market;
    private String korean_name;
    private String english_name;

    public String getMarket() {
        return market;
    }

    public String getKorean_name() {
        return korean_name;
    }

    public String getEnglish_name() {
        return english_name;
    }

    @Override
    public String toString() {
        return "MarketInfo{" +
                "market='" + market + '\'' +
                ", korean_name='" + korean_name + '\'' +
                ", english_name='" + english_name + '\'' +
                '}';
    }

}
