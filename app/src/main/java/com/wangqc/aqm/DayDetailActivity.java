package com.wangqc.aqm;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Looper;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import lecho.lib.hellocharts.formatter.SimpleLineChartValueFormatter;
import lecho.lib.hellocharts.gesture.ContainerScrollType;
import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.ValueShape;
import lecho.lib.hellocharts.view.LineChartView;

public class DayDetailActivity extends AppCompatActivity {
    private TextView title;
    private String cityName;
    private JSONObject dayJson;
    private LineChartView[] mLineChartView;
    private TextView[] mTextView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout mLinearLayout;
    private String cityJsonPreUrl = "http://45.77.33.19:8080/AQMServer/CitiesJson/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daydetail);

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

        //设置ToolBar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_daydetail);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        title = (TextView) findViewById(R.id.title_day);

        swipeRefreshLayout = (SwipeRefreshLayout)findViewById(R.id.swipe_refresh_day);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String pyCityName = PinyinUtil.getPingYin(cityName);
                        JSONObject cityJson = new JsonUtil().getJsonFromUrl(cityJsonPreUrl + pyCityName + ".json");
                        if (cityJson != null){
                            try {
                                dayJson = cityJson.getJSONObject("24H");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            if (dayJson != null){
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        InitUI(cityName, dayJson);
                                        swipeRefreshLayout.setRefreshing(false);
                                    }
                                });
                            }
                        }else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(DayDetailActivity.this, "无法获取服务器数据", Toast.LENGTH_SHORT).show();
                                    swipeRefreshLayout.setRefreshing(false);
                                }
                            });
                        }

                    }
                }).start();
            }
        });

        mLinearLayout = (LinearLayout)findViewById(R.id.container_daydetail);
        mLineChartView = new LineChartView[7];
        mTextView = new TextView[7];
        for (int i = 0; i < 7; i++) {
            mLineChartView[i] = (LineChartView) mLinearLayout.getChildAt(i * 3);
            mTextView[i] = (TextView)mLinearLayout.getChildAt(i * 3 + 1);
        }

        Intent intent = getIntent();
        cityName = intent.getStringExtra("city_name");
        String jsonString = intent.getStringExtra("city_data");
        try {
            dayJson = new JSONObject(jsonString);
        } catch (JSONException e) {
            return;
        }

        InitUI(cityName, dayJson);
    }

    private void InitUI(String cityName, JSONObject json) {
        String[] itemArray = {"AQI", "PM2.5", "PM10", "SO2", "NO2", "O3", "CO"};
        if (cityName == null || cityName.equals("")) {
            return;
        }

        title.setText(cityName+ " 24小时空气数据");
        JSONArray jsonArray;
        List<PointValue> mPointValues;
        List<AxisValue> mAxisXValues;
        float pointValue;

        for (int i = 0; i < 7; i++) {
            try {
                jsonArray = json.getJSONArray(itemArray[i]);
            } catch (JSONException e) {
                e.printStackTrace();
                break;
            }
            mPointValues = new ArrayList<>();
            mAxisXValues = new ArrayList<>();
            for (int j = 0; j < 24; j++){
                try {
                    pointValue  = (float)jsonArray.getDouble(j);
                } catch (JSONException e) {
                    e.printStackTrace();
                    pointValue = 0;
                }
                mPointValues.add(new PointValue(j, pointValue));
                mAxisXValues.add(new AxisValue(j).setLabel(Integer.toString(j+1)));
            }
            mTextView[i].setText(itemArray[i]);
            SimpleLineChartValueFormatter formatter = null;
            if (i == 6) {
                formatter = new SimpleLineChartValueFormatter(2);
            }
                initLineCharts(i, mPointValues, mAxisXValues, formatter);
            }
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

    private void initLineCharts(int index, List<PointValue> pointValueList, List<AxisValue> axisValueList, SimpleLineChartValueFormatter formatter){
        Line line = new Line(pointValueList).setColor(Color.parseColor("#FFCD41"));  //折线的颜色（橙色）

        if (formatter != null) {
            line.setFormatter(formatter);//显示小数点
        }

        List<Line> lines = new ArrayList<>();
        line.setShape(ValueShape.CIRCLE);//折线图上每个数据点的形状
        line.setCubic(false);//曲线是否平滑，即是曲线还是折线
        line.setFilled(false);//是否填充曲线的面积
        line.setHasLabels(true);//曲线的数据坐标是否加上备注
        line.setHasLines(true);//是否用线显示
        line.setHasPoints(true);//是否显示圆点
        lines.add(line);
        LineChartData data = new LineChartData();
        data.setValueLabelTextSize(10);
        data.setValueLabelBackgroundEnabled(false);
        data.setLines(lines);

        //X坐标轴
        Axis axisX = new Axis(); //X轴
        axisX.setHasTiltedLabels(false);  //X坐标轴字体是斜的显示还是直的，true是斜的显示
        axisX.setTextColor(Color.WHITE);  //设置字体颜色
        axisX.setTextSize(12);//设置字体大小
        axisX.setValues(axisValueList);  //填充X轴的坐标名称
        data.setAxisXBottom(axisX); //x 轴在底部
        axisX.setHasLines(true); //x 轴分割线

        // Y轴是根据数据的大小自动设置Y轴上限
        Axis axisY = new Axis();  //Y轴
        axisY.setName(" ");//y轴标注
        axisY.setTextSize(10);//设置字体大小
        data.setAxisYLeft(axisY);  //Y轴设置在左边


        //设置行为属性，支持缩放、滑动以及平移
        mLineChartView[index].setInteractive(true);
        mLineChartView[index].setZoomType(ZoomType.HORIZONTAL);
        mLineChartView[index].setMaxZoom((float) 3);//最大方法比例
        mLineChartView[index].setContainerScrollEnabled(true, ContainerScrollType.HORIZONTAL);
        mLineChartView[index].setLineChartData(data);
        mLineChartView[index].setVisibility(View.VISIBLE);
    }
}
