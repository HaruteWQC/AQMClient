package com.wangqc.aqm;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by wang9 on 2017/5/3.
 */

public class JsonUtil {
    public JSONObject getJsonFromUrl(String urlStr) {
        JSONObject jsonObject = null;
        URL url = null;
        HttpURLConnection conn = null;
        try {
            url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            if (conn.getResponseCode() == 200) {
                InputStream inputStream = conn.getInputStream();
                BufferedReader streamReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                StringBuilder responseStrBuilder = new StringBuilder();
                String inputStr;
                while ((inputStr = streamReader.readLine()) != null) {
                    responseStrBuilder.append(inputStr);
                }
                jsonObject = new JSONObject(responseStrBuilder.toString());
            }
        } catch (Exception e) {
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
            return jsonObject;
        }
    }
}
