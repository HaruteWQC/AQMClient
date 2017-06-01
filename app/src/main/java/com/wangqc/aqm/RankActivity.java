package com.wangqc.aqm;

import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class RankActivity extends AppCompatActivity {
    public static final int UPDATE_RANKLIST = 1; //根据数据刷新界面传递给Handler的消息
    private RecyclerView mRecyclerView;
    private JSONObject jsonObject;
    private List<RankItemUtil> rankItemList = new ArrayList<>();
    private RankItemAdapter adapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rank);

        //设置沉浸状态栏和导航栏
        if (Build.VERSION.SDK_INT >= 21) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_rank);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        new Thread(new Runnable() {
            @Override
            public void run() {
                String aqiUrlString = "http://45.77.33.19:8080/AQMServer/CitiesJson/AQI.json";
                jsonObject = new JsonUtil().getJsonFromUrl(aqiUrlString);
                if (jsonObject != null) {
                   runOnUiThread(new Runnable() {
                       @Override
                       public void run() {
                           updateRankListView();
                       }
                   });
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(RankActivity.this, "无法获取服务器数据", Toast.LENGTH_SHORT).show();
                        }
                    });

                }
            }
        }
        ).start();


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            default:
        }
        return true;
    }

    public void updateRankListView() {
        initRankList();
        //RankItemAdapter rankItemAdapter = new RankItemAdapter(rankItemList);

        if (rankItemList != null) {
            adapter = new RankItemAdapter(rankItemList);
            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            mRecyclerView.setLayoutManager(layoutManager);
            mRecyclerView.setAdapter(adapter);
        }

    }

    private void initRankList() {
        String cityName;
        String[] aqiLevel = {"优", "良", "轻度污染", "中度污染", "重度污染", "严重污染"};
        String level;
        String aqiStr;
        int aqiValue;

        Iterator it = jsonObject.keys();
        while (it.hasNext()) {
            cityName = it.next().toString();
            try {
                aqiStr = jsonObject.getString(cityName);
            } catch (JSONException e) {
                continue;
            }

            if (!aqiStr.equals("")) {
                aqiValue = Integer.parseInt(aqiStr);

                if (aqiValue >= 0 && aqiValue <= 50) {
                    level = aqiLevel[0];
                } else {
                    if (aqiValue <= 100) {
                        level = aqiLevel[1];
                    } else {
                        if (aqiValue <= 150) {
                            level = aqiLevel[2];
                        } else {
                            if (aqiValue <= 200) {
                                level = aqiLevel[3];
                            } else {
                                if (aqiValue <= 300) {
                                    level = aqiLevel[4];
                                } else {
                                    level = aqiLevel[5];
                                }
                            }
                        }
                    }
                }
                RankItemUtil rankItemUtil = new RankItemUtil(cityName, Integer.toString(aqiValue), level);
                rankItemList.add(rankItemUtil);
            }
        }

        Collections.sort(rankItemList, new SortByAQI());
    }

    class SortByAQI implements Comparator {
        public int compare(Object o1, Object o2) {
            RankItemUtil s1 = (RankItemUtil) o1;
            RankItemUtil s2 = (RankItemUtil) o2;
            int value1 = Integer.parseInt(s1.getAqi());
            int value2 = Integer.parseInt(s2.getAqi());
            if (value1 > value2) {
                return 1;
            } else {
                if (value1 < value2){
                    return -1;
                } else {
                    return 0;
                }
            }
        }
    }

}
