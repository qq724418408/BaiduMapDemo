package com.forms.wjl.map.demo.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.forms.wjl.map.demo.R;
import com.forms.wjl.map.demo.base.BaseActivity;

public class MainActivity extends BaseActivity {

    private Button btnBaseMap;
    private Button btnIndoorMap;
    private Button btnPOIMap;
    private Button btnRadarNearbyMap;
    private Button btnRoutePlan;
    private Button btnNavi;
    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        initData();
        initListener();
    }

    @Override
    protected void initViews() {
        btnNavi = (Button) findViewById(R.id.btnNavi);
        btnBaseMap = (Button) findViewById(R.id.btnBaseMap);
        btnIndoorMap = (Button) findViewById(R.id.btnIndoorMap);
        btnRoutePlan = (Button) findViewById(R.id.btnRoutePlan);
        btnPOIMap = (Button) findViewById(R.id.btnPOIMap);
        btnRadarNearbyMap = (Button) findViewById(R.id.btnRadarNearbyMap);
    }

    @Override
    protected void initData() {

    }

    @Override
    protected void initListener() {
        btnBaseMap.setOnClickListener(this);
        btnNavi.setOnClickListener(this);
        btnIndoorMap.setOnClickListener(this);
        btnPOIMap.setOnClickListener(this);
        btnRadarNearbyMap.setOnClickListener(this);
        btnRoutePlan.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnBaseMap:
                intent = new Intent(this, BaseMapActivity.class);
                startActivity(intent);
                break;
             case R.id.btnIndoorMap:
                intent = new Intent(this, IndoorMapActivity.class);
                startActivity(intent);
                break;
            case R.id.btnPOIMap:
                intent = new Intent(this, POIMapActivity.class);
                startActivity(intent);
                break;
            case R.id.btnRadarNearbyMap:
                intent = new Intent(this, RadarNearbyActivity.class);
                startActivity(intent);
                break;
            case R.id.btnRoutePlan:
                intent = new Intent(this, RoutePlanActivity.class);
                startActivity(intent);
                break;
            case R.id.btnNavi:
                intent = new Intent(this, NaviActivity.class);
                startActivity(intent);
                break;
        }
    }
}
