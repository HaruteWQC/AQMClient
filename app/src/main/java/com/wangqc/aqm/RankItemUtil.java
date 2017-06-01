package com.wangqc.aqm;

/**
 * Created by wang9 on 2017/5/3.
 */

public class RankItemUtil {
    private String cityName;
    private String aqi;
    private String airLevel;

    public RankItemUtil(String cityName, String aqi, String airLevel){
        this.cityName = cityName;
        this.aqi = aqi;
        this.airLevel = airLevel;
    }

    public String getCityName(){
        return cityName;
    }

    public String getAqi(){
        return aqi;
    }

    public String getAirLevel(){
        return airLevel;
    }
}
