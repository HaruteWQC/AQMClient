package com.wangqc.aqm;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.support.design.widget.NavigationView;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;

import static java.lang.Thread.sleep;

public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener, AdapterView.OnItemClickListener, NavigationView.OnNavigationItemSelectedListener, View.OnClickListener {
    //搜索栏和列表栏控件
    private SearchView mSearchView; //搜索栏
    private ListView mListView; //搜索栏候选项列表
    private ArrayAdapter cityListAdapter;   //搜索栏候选项适配器
    private LinearLayout sitesListContainer;   //站点信息布局

    //主界面各城市信息显示控件
    private DrawerLayout mDrawerLayout;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private ScrollView mScrollView;
    private HorizontalScrollView mHorizontalScrollView;
    private TextView cityTextView;   //标题城市名
    private TextView aqiTextView;   //aqi数据显示TextView
    private TextView levelTextView;
    private TextView tipTextView;
    private TextView[] hsvCityItem = new TextView[6];   //主界面6个空气数值TextView

    //侧边导航栏
    private NavigationView navView;

    //各数据处理对象
    private String cityListPath;    //城市列表文件目录
    private String historyListPath;
    private String cityJsonPreUrl = "http://45.77.33.19:8080/AQMServer/CitiesJson/";
    private String[] cityList;
    private String[] historyList = new String[5];
    public String cityString; //标题栏城市名
    public JSONObject cityJson; //当前查询城市JSON数据

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        //配置ToolBar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        //配置侧边栏效果
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_main);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mDrawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerLayout.setDrawerListener(toggle);
        toggle.syncState();

        //配置侧边导航栏
        navView = (NavigationView) findViewById(R.id.nav_view);
        navView.setNavigationItemSelectedListener(this);

        //配置输入候选栏ListView
        mListView = (ListView) findViewById(R.id.listview);
        mListView.setTextFilterEnabled(true);
        mListView.setVisibility(View.INVISIBLE);
        mListView.setOnItemClickListener(this);

        //
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                queryCityAirInfo(cityString);
            }
        });
        //配置垂直滚动控件
        mScrollView = (ScrollView) findViewById(R.id.scroll_view);

        //配置水平滚动栏控件
        mHorizontalScrollView = (HorizontalScrollView)findViewById(R.id.horscroll_view);
        RelativeLayout hsvLayout = (RelativeLayout) findViewById(R.id.hsv_RLayout);
        for (int i = 0; i < 6; i++) {
            hsvCityItem[i] = (TextView) hsvLayout.getChildAt(i + 11);
        }

        //配置TextView
        cityTextView = (TextView) findViewById(R.id.text_city);
        aqiTextView = (TextView) findViewById(R.id.text_aqi);
        levelTextView = (TextView) findViewById(R.id.text_level);

        //配置主界面按钮
        Button dayDetailBtn = (Button) findViewById(R.id.btn_24Hdetail);
        dayDetailBtn.setOnClickListener(this);
        Button weekDetailBtn = (Button) findViewById(R.id.btn_7Ddetail);
        weekDetailBtn.setOnClickListener(this);
        Button rankBtn = (Button) findViewById(R.id.btn_rank);
        rankBtn.setOnClickListener(this);

        //配置提示文本控件
        tipTextView = (TextView) findViewById(R.id.text_tip);

        //站点条目容器布局
        sitesListContainer = (LinearLayout) findViewById(R.id.siteslist_container);

        //配置文件目录
        Context cont = this.getApplicationContext();
        cityListPath = cont.getFilesDir() + "/cityList";
        historyListPath = cont.getFilesDir() + "/history";

        //更新ListView的Adapter
        setCityListAdapter();

        //获取导航栏历史查询记录
        readHistoryToList(historyListPath);
        setNavigationItem();
        if (historyList[0] != null && !historyList[0].equals("")) {
            cityTextView.setText(historyList[0]);
            queryCityAirInfo(historyList[0]);
        }else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mSearchView.setIconified(false);
                        }
                    });
                }
            }).start();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_toolbar_main, menu);
        MenuItem menuItem = menu.findItem(R.id.search_view);
        mSearchView = (SearchView) MenuItemCompat.getActionView(menuItem);
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (!b) {
                    mSearchView.setIconified(true);
                    cityTextView.setVisibility(View.VISIBLE);
                }else {
                    cityTextView.setVisibility(View.INVISIBLE);
                }
            }
        });
        mSearchView.setSubmitButtonEnabled(true);
        mSearchView.setQueryHint("输入城市名");
        mSearchView.setIconifiedByDefault(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (TextUtils.isEmpty(newText)) {
            mListView.setVisibility(View.INVISIBLE);
            mListView.clearTextFilter();

        } else {
            if (mListView.getVisibility() == View.INVISIBLE) {
                mListView.setVisibility(View.VISIBLE);
            }
            if (cityListAdapter != null) {
                cityListAdapter.getFilter().filter(newText);
            }
        }
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(final String query) {
        String bestAdapter;
        if (cityListAdapter != null) {
            try {
                bestAdapter = cityListAdapter.getItem(0).toString();
                if (query != null && bestAdapter != null && bestAdapter.equals(query)) {
                    queryCityAirInfo(query);
                    mSearchView.clearFocus();
                    addToHistoryList(cityString);
                    setNavigationItem();
                } else {
                    Toast.makeText(MainActivity.this, "请输入有效城市名", Toast.LENGTH_SHORT).show();
                }
            } catch (IndexOutOfBoundsException e) {
                //adapter的getItem方法越界异常，即无与筛选符合的结果
                Toast.makeText(MainActivity.this, "请输入有效城市名", Toast.LENGTH_SHORT).show();
            }
        } else {
            setCityListAdapter();
            Toast.makeText(MainActivity.this, "无法获取服务器数据", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        String itemName = cityListAdapter.getItem(i).toString();
        if (itemName != null) {
            queryCityAirInfo(itemName);
            mSearchView.clearFocus();
            addToHistoryList(cityString);
            setNavigationItem();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_24Hdetail:
                JSONObject json1;
                if (cityJson == null) {
                    break;
                }
                try {
                    json1 = cityJson.getJSONObject("24H");
                } catch (Exception e) {
                    break;
                }
                if (cityString != null && json1 != null) {
                    Intent intent = new Intent(MainActivity.this, DayDetailActivity.class);
                    intent.putExtra("city_name", cityString);
                    intent.putExtra("city_data", json1.toString());
                    startActivity(intent);
                }
                break;
            case R.id.btn_7Ddetail:
                JSONObject json2;
                if (cityJson == null) {
                    break;
                }
                try {
                    json2 = cityJson.getJSONObject("week");
                } catch (Exception e) {
                    break;
                }
                if (cityString != null && json2 != null) {
                    Intent intent = new Intent(MainActivity.this, WeekDetailActivity.class);
                    intent.putExtra("city_name", cityString);
                    intent.putExtra("city_data", json2.toString());
                    startActivity(intent);
                }
                break;
            case R.id.btn_rank:
                Intent intent = new Intent(MainActivity.this, RankActivity.class);
                intent.putExtra("default_city", cityString);
                startActivity(intent);
            default:
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nav_item_setting:
                //Intent intent = new Intent(MainActivity.this, SettingActivity.class);
                //startActivityForResult(intent, 1);
                break;
            case R.id.nav_item_help:
                Intent intent = new Intent(MainActivity.this, IntroduceActivity.class);
                startActivity(intent);
                break;
            default:
                String title = item.getTitle().toString();
                if (title != null && !title.equals("")) {
                    item.setChecked(true);
                    mDrawerLayout.closeDrawers();
                    cityString = title;
                    mSwipeRefreshLayout.setRefreshing(true);
                    queryCityAirInfo(cityString);
                }
                break;
        }
        return false;
    }

    private void setCityListAdapter() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String sitesTableUrl = "http://45.77.33.19:8080/AQMServer/SitesTable/SitesTable.json";  //城市-站点对应数据文件URL
                File file = new File(cityListPath);
                if (file.exists()) {
                    try {
                        String lastStr = "";
                        BufferedReader reader = new BufferedReader(new FileReader(file));
                        String tempString;
                        while ((tempString = reader.readLine()) != null) {
                            lastStr = lastStr + tempString;
                        }
                        reader.close();

                        cityList = lastStr.split(",");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    JSONObject jsonObject = new JsonUtil().getJsonFromUrl(sitesTableUrl);
                    if (jsonObject == null) {
                        Looper.prepare();
                        Toast.makeText(MainActivity.this, "无法获取服务器数据", Toast.LENGTH_SHORT).show();
                        Looper.loop();
                        return;
                    }
                    Iterator it = jsonObject.keys();
                    ArrayList<String> keyList = new ArrayList<>();
                    String tmpStr = null;
                    while (it.hasNext()) {
                        tmpStr = it.next().toString();
                        if (!tmpStr.equals("")) {
                            keyList.add(tmpStr);
                        }
                    }
                    PrintStream ps = null;
                    try {
                        ps = new PrintStream(new FileOutputStream(file));
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    if (ps != null) {
                        for (String str : keyList) {
                            ps.append(str + ",");
                        }
                        ps.close();
                    }
                    cityList = keyList.toArray(new String[keyList.size()]);
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (cityList != null) {
                            cityListAdapter = new ArrayAdapter(MainActivity.this, android.R.layout.simple_list_item_1, cityList);
                            mListView.setAdapter(cityListAdapter);
                        }
                    }
                });
            }
        }).start();
    }

    private void queryCityAirInfo(final String name) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSwipeRefreshLayout.setRefreshing(true);
            }
        });
        new Thread(new Runnable() {
            @Override
            public void run() {
                cityString = name;
                String pyCityName = PinyinUtil.getPingYin(name);
                cityJson = new JsonUtil().getJsonFromUrl(cityJsonPreUrl + pyCityName + ".json");

                if (cityJson != null) {
                    try {
                        sleep(500);
                    } catch (InterruptedException e) {
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            refreshUI(cityJson, cityString);
                            mSwipeRefreshLayout.setRefreshing(false);
                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "无法获取服务器数据", Toast.LENGTH_SHORT).show();
                            mSwipeRefreshLayout.setRefreshing(false);
                        }
                    });
                }
            }
        }).start();
    }

    private void refreshUI(final JSONObject json, final String name) {
        String aqiTip1 = "空气质量令人满意, 基本无空气污染, 各类人群可正常活动。";
        String aqiTip2 = "空气质量可接受, 但某些污染物可能对极少数异常敏感人群健康有较弱影响, 建议极少数异常敏感人群应减少户外活动。";
        String aqiTip3 = "易感人群症状会有轻度加剧, 健康人群会出现刺激症状。建议儿童、老年人及心脏病、呼吸系统疾病患者应减少长时间、高强度的户外锻炼。";
        String aqiTip4 = "易感人群症状会进一步加剧, 可能对健康人群心脏、呼吸系统有影响, 建议疾病患者避免长时间、高强度的户外锻练,一般人群适量减少户外运动。";
        String aqiTip5 = "心脏病和肺病患者症状会显著加剧, 运动耐受力降低, 健康人群普遍出现症状, 建议儿童、老年人和心脏病、肺病患者应停留在室内, 停止户外运动, 一般人群减少户外运动。";
        String aqiTip6 = "健康人群运动耐受力降低, 有明显强烈症状, 提前出现某些疾病, 建议儿童、老年人和病人应当留在室内, 避免体力消耗, 一般人群应避免户外活动。";
        String[] aqiLevel = {"优", "良", "轻度污染", "中度污染", "重度污染", "严重污染"};

        cityTextView.setText(name);
        String[] itemArray = {"PM2.5", "PM10", "SO2", "NO2", "O3", "CO"};
        try {
            //获取最新数据
            JSONObject dayJson = json.getJSONObject("24H");
            String tmp;
            tmp = dayJson.getJSONArray("AQI").getString(23);
            if (tmp.equals("")) {
                tmp = dayJson.getJSONArray("AQI").getString(22);
            }
            //更新AQI和提示文本控件
            if (!tmp.equals("")) {
                aqiTextView.setText(tmp);
                int aqiNum = Integer.parseInt(tmp);
                if (aqiNum >= 0 && aqiNum <= 50) {
                    levelTextView.setTextColor(getResources().getColor(R.color.colorAQI1));
                    levelTextView.setText(aqiLevel[0]);
                    tipTextView.setText(aqiTip1);
                } else {
                    if (aqiNum <= 100) {
                        levelTextView.setTextColor(getResources().getColor(R.color.colorAQI2));
                        levelTextView.setText(aqiLevel[1]);
                        tipTextView.setText(aqiTip2);
                    } else {
                        if (aqiNum <= 150) {
                            levelTextView.setTextColor(getResources().getColor(R.color.colorAQI3));
                            levelTextView.setText(aqiLevel[2]);
                            tipTextView.setText(aqiTip3);
                        } else {
                            if (aqiNum <= 200) {
                                levelTextView.setTextColor(getResources().getColor(R.color.colorAQI4));
                                levelTextView.setText(aqiLevel[3]);
                                tipTextView.setText(aqiTip4);
                            } else {
                                if (aqiNum <= 300) {
                                    levelTextView.setTextColor(getResources().getColor(R.color.colorAQI5));
                                    levelTextView.setText(aqiLevel[4]);
                                    tipTextView.setText(aqiTip5);
                                } else {
                                    levelTextView.setTextColor(getResources().getColor(R.color.colorAQI6));
                                    levelTextView.setText(aqiLevel[5]);
                                    tipTextView.setText(aqiTip6);
                                }
                            }
                        }
                    }
                }
            } else {
                aqiTextView.setText("---");
            }

            //更新水平滚动栏文本控件
            for (int i = 0; i < 6; i++) {
                tmp = dayJson.getJSONArray(itemArray[i]).getString(23);
                if (tmp.equals("")) {
                    tmp = dayJson.getJSONArray(itemArray[i]).getString(22);
                }
                if (!tmp.equals("")) {
                    hsvCityItem[i].setText(tmp);
                } else {
                    hsvCityItem[i].setText("--");
                }
            }

            //更新城市列表
            sitesListContainer.removeAllViews();
            JSONObject sitesListJson = json.getJSONObject("Sites");
            JSONObject siteJson;
            Iterator it = sitesListJson.keys();
            String siteName;
            while (it.hasNext()) {
                siteName = it.next().toString();
                siteJson = sitesListJson.getJSONObject(siteName);
                ViewGroup viewGroup = (ViewGroup) View.inflate(MainActivity.this, R.layout.sitelist_item, null);
                ((TextView) viewGroup.getChildAt(0)).setText(siteName);
                for (int i = 1; i < 7; i++) {
                    tmp = siteJson.getString(itemArray[i - 1]);
                    if (!tmp.equals("")) {
                        ((TextView) viewGroup.getChildAt(i)).setText(tmp);
                    } else {
                        ((TextView) viewGroup.getChildAt(i)).setText("-");
                    }
                }
                View view = View.inflate(MainActivity.this, R.layout.boerder, null);
                sitesListContainer.addView(viewGroup);
                sitesListContainer.addView(view);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        mScrollView.fullScroll(ScrollView.FOCUS_UP);
        mHorizontalScrollView.fullScroll(ScrollView.FOCUS_LEFT);
    }

    private void readHistoryToList(String path) {
        File file = new File(path);
        if (file.exists()) {
            String laststr = "";
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String tempString;
                while ((tempString = reader.readLine()) != null) {
                    laststr = laststr + tempString;
                }
                reader.close();
            } catch (Exception e) {
                return;
            }
            String[] tmpList = laststr.split(",");
            for (int i = 0; i < 5; i++) {
                if (i < tmpList.length && !tmpList[i].equals("")) {
                    historyList[i] = tmpList[i];
                } else {
                    historyList[i] = "";
                }
            }
        } else {
            for (int i = 0; i < 5; i++) {
                historyList[i] = "";
            }
        }
    }

    private void addToHistoryList(String city) {
        int p = 0;
        while (p < 4 && historyList[p] != null && !historyList[p].equals(city)) {
            p++;
        }
        for (int i = p; i > 0; i--) {
            historyList[i] = historyList[i - 1];
        }
        historyList[0] = city;

        File file = new File(historyListPath);
        PrintStream ps = null;
        try {
            ps = new PrintStream(new FileOutputStream(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (ps != null) {
            ps.print(historyList[0]);
            for (int i = 1; i < 5; i++) {
                if (historyList[i] != null && !historyList[i].equals("")) {
                    ps.append("," + historyList[i]);
                } else {
                    break;
                }
            }
            ps.close();
        }
    }

    private void setNavigationItem() {
        int i = 0;
        while (i < 5 && !historyList[i].equals("")) {
            navView.getMenu().getItem(i).setTitle(historyList[i]);
            navView.getMenu().getItem(i).setCheckable(true);
            i++;
        }
        if (!historyList[0].equals("")){
            navView.getMenu().getItem(0).setChecked(true);
        }
    }
}

