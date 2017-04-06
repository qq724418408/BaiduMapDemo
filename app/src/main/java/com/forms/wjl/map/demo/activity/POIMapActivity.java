package com.forms.wjl.map.demo.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiCitySearchOption;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiNearbySearchOption;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.forms.wjl.map.demo.R;
import com.forms.wjl.map.demo.base.BaseActivity;
import com.forms.wjl.map.demo.view.MyOrientationListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class POIMapActivity extends BaseActivity implements OnGetGeoCoderResultListener {

    private static final String TAG = "POIMapActivity";
    private MapView mapView;
    private BaiduMap baiduMap;
    private Button button;
    private Button btnPop; // 点击地图显示获取地址的泡泡
    private Button btnSearch; // 搜索按钮
    private EditText etKeyword; // 关键字输入编辑框
    private GeoCoder geocoder = null;
    private String address;
    private LatLng point;
    private LocationClient locationClient;
    private LocationClientOption mOption;
    private MyLocationListener myLocationListener;
    private PoiSearch poiSearch;
    private MyOrientationListener myOrientationListener; // 方向传感器
    private float mCurrentX = 0; // 定位箭头方向
    private boolean isFirstLocation = true; // 首次定位
    private BitmapDescriptor mBitmapDescriptor = null; // 自定义定位图标
    private ImageView ivLocal; // 定位按钮
    private boolean isRequest = false;
    private Toast toast;
    private String city;
    private LatLng searchPoint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poi_map);
        initViews();
        initData();
        initListener();
    }

    @Override
    protected void initViews() {
        etKeyword = (EditText) findViewById(R.id.etKeyword);
        btnSearch = (Button) findViewById(R.id.btnSearch);
        ivLocal = (ImageView) findViewById(R.id.ivLocal);
        myOrientationListener = new MyOrientationListener(this);
        mapView = (MapView) findViewById(R.id.mv_map);
        poiSearch = PoiSearch.newInstance();
        button = getPopButton();
        btnPop = getPopButton(); // 创建InfoWindow展示的view
        baiduMap = mapView.getMap();
        // 定义Maker坐标点
        locationClient = new LocationClient(this);
        locationClient.setLocOption(getDefaultLocationClientOption());
        myLocationListener = new MyLocationListener();
        locationClient.registerLocationListener(myLocationListener);
        locationClient.start();
        mapView.removeViewAt(1); // 去掉百度地图logo
    }

    @Override
    protected void initData() {
        geocoder = GeoCoder.newInstance();
        mBitmapDescriptor = BitmapDescriptorFactory.fromResource(R.mipmap.ic_arrow);
    }

    /**
     * 初始化、注册事件监听
     */
    @Override
    protected void initListener() {
        ivLocal.setOnClickListener(this);
        btnSearch.setOnClickListener(this);
        geocoder.setOnGetGeoCodeResultListener(this);
        baiduMap.setOnMapClickListener(onMapClickListener); // 地图单击事件
        baiduMap.setOnMapLongClickListener(onMapLongClickListener); // 地图长按事件
        baiduMap.setOnMarkerClickListener(onMarkerClickListener); //
        poiSearch.setOnGetPoiSearchResultListener(poiListener);
        myOrientationListener.setmOnOrientationListener(new MyOrientationListener.OnOrientationListener() {
            @Override
            public void onOrientationChanged(float x) {
                mCurrentX = x;
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ivLocal: // 手动定位
                requestLocation(); // 请求定位
                break;
            case R.id.btnSearch: // 搜索按钮
                // 需要获取搜索范围的城市
                String keyword = etKeyword.getText().toString();
                if (!TextUtils.isEmpty(keyword)) {
                    //searchInCity(keyword, 10); // 搜索
                    searchNearby(searchPoint, keyword, 10, 1000);
                } else {
                    showToast("请输入关键字");
                }
                break;
        }
    }

    /**
     * 创建InfoWindow展示的view
     *
     * @return
     */
    protected Button getPopButton() {
        Button btn = new Button(getApplicationContext());
        btn.setBackgroundResource(R.mipmap.ic_pop_info_window);
        btn.setPadding(16, 0, 16, 8);
        btn.setGravity(Gravity.CENTER_VERTICAL);
        return btn;
    }

    /**
     * 更新地图显示
     *
     * @param latLng
     */
    private void updateMap(LatLng latLng) {
        MapStatusUpdate update = MapStatusUpdateFactory.newLatLng(latLng);
        baiduMap.animateMapStatus(update);
    }

    /**
     * 设置地理反编码监听，根据经纬度获取地理位置信息等
     *
     * @param latLng
     */
    private void setReverseGeoCodeListener(LatLng latLng) {
        ReverseGeoCodeOption reverseGeoCodeOption = new ReverseGeoCodeOption();
        reverseGeoCodeOption.location(latLng);
        geocoder.reverseGeoCode(reverseGeoCodeOption);
    }

    /**
     * 创建InfoWindow
     *
     * @param btn
     * @param latLng 地理坐标
     * @param offset y 轴偏移量
     */
    private void createInfoWindow(Button btn, LatLng latLng, int offset) {
        baiduMap.hideInfoWindow();
        InfoWindow mInfoWindow = new InfoWindow(btn, latLng, offset);
        baiduMap.showInfoWindow(mInfoWindow); // 显示InfoWindow
    }

    /**
     * 构建Marker图标
     *
     * @param latLng
     * @param resId
     * @param draggable
     */
    private Marker createMarker(LatLng latLng, int resId, boolean draggable) {
        BitmapDescriptor bitmap = BitmapDescriptorFactory.fromResource(resId); // 构建Marker图标
        // 构建MarkerOption，用于在地图上添加Marker
        OverlayOptions option = new MarkerOptions().draggable(draggable).position(latLng).icon(bitmap);
        Marker marker = (Marker) baiduMap.addOverlay(option);// 在地图上添加Marker，并显示
        return marker;
    }

    /**
     * LocationClientOption
     *
     * @return DefaultLocationClientOption
     */
    public LocationClientOption getDefaultLocationClientOption() {
        if (mOption == null) {
            mOption = new LocationClientOption();
            //可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
            mOption.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
            //可选，默认gcj02，设置返回的定位结果坐标系，如果配合百度地图使用，建议设置为bd09ll;
            mOption.setCoorType("bd09ll");
            //可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
            mOption.setScanSpan(3000);
            mOption.setIsNeedAddress(true);//可选，设置是否需要地址信息，默认不需要
            mOption.setIsNeedLocationDescribe(true);//可选，设置是否需要地址描述
            mOption.setNeedDeviceDirect(false);//可选，设置是否需要设备方向结果
            //可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
            mOption.setLocationNotify(false);
            //可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
            mOption.setIgnoreKillProcess(true);
            //可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
            mOption.setIsNeedLocationDescribe(true);
            //可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
            mOption.setIsNeedLocationPoiList(true);
            //可选，默认false，设置是否收集CRASH信息，默认收集
            mOption.SetIgnoreCacheException(false);
        }
        return mOption;
    }

    /**
     * 搜索监听
     */
    private OnGetPoiSearchResultListener poiListener = new OnGetPoiSearchResultListener() {

        public void onGetPoiResult(PoiResult result) {
            if(null != result &&  result.error == SearchResult.ERRORNO.NO_ERROR){
                //获取POI检索结果
                List<PoiInfo> allPoi = result.getAllPoi();
                if (allPoi != null) {
                    Message msg = handler.obtainMessage();
                    msg.obj = allPoi;
                    msg.what = 200;
                    handler.sendMessage(msg);
                } else {
                    showToast("没有搜到相关结果");
                }
            } else {
                showToast("没有搜到相关结果");
            }
        }

        public void onGetPoiDetailResult(PoiDetailResult result) {
            //获取Place详情页检索结果
            Log.d(TAG, "onGetPoiDetailResult: " + result.toString());
        }

        @Override
        public void onGetPoiIndoorResult(PoiIndoorResult poiIndoorResult) {
            Log.d(TAG, "onGetPoiIndoorResult: " + poiIndoorResult.toString());
        }
    };

    /**
     * 定位监听
     */
    private class MyLocationListener implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            if (bdLocation != null) {
                // 开启定位图层
                baiduMap.setMyLocationEnabled(true);
                // 构造定位数据
                MyLocationData locData = new MyLocationData.Builder()
                        .accuracy(bdLocation.getRadius())
                        // 此处设置开发者获取到的方向信息，顺时针0-360
                        .direction(mCurrentX).latitude(bdLocation.getLatitude())
                        .longitude(bdLocation.getLongitude()).build();
                Log.d(TAG, "onReceiveLocation: mCurrentX--> " + mCurrentX);
                // 设置定位数据
                baiduMap.setMyLocationData(locData);
                MyLocationConfiguration configuration = new MyLocationConfiguration(MyLocationConfiguration.LocationMode.NORMAL, true, mBitmapDescriptor);
                baiduMap.setMyLocationConfigeration(configuration);
                if (isFirstLocation || isRequest) {
                    point = new LatLng(bdLocation.getLatitude(), bdLocation.getLongitude()); // 获取定位经纬度
                    searchPoint = point;
                    city = bdLocation.getCity();
                    button.setText(city);
                    baiduMap.clear();
                    createInfoWindow(button, point, -32);
                    MapStatusUpdate update = MapStatusUpdateFactory.newLatLngZoom(point, baiduMap.getMaxZoomLevel() - 4); // 50米
                    baiduMap.animateMapStatus(update);
                    isRequest = false;
                }
                isFirstLocation = false;
            }
        }
    }

    /**
     * Marker 覆盖物点击事件监听接口：
     */
    private BaiduMap.OnMarkerClickListener onMarkerClickListener = new BaiduMap.OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(Marker marker) {
            Message msg = handler.obtainMessage();
            Map<String, Object> map = new HashMap();
            map.put("title", marker.getTitle());
            map.put("position", marker.getPosition());
            msg.obj = map;
            msg.what = 100;
            handler.sendMessage(msg);
            return false;
        }
    };

    /**
     * 地图定位图标点击事件监听接口
     */
    private BaiduMap.OnMyLocationClickListener myLocationClickListener = new BaiduMap.OnMyLocationClickListener() {
        @Override
        public boolean onMyLocationClick() {
            Log.d(TAG, "onMyLocationClick: 就是被点了一下，没感觉");
            return false;
        }
    };

    /**
     * 地图长按事件
     */
    private BaiduMap.OnMapLongClickListener onMapLongClickListener = new BaiduMap.OnMapLongClickListener() {
        @Override
        public void onMapLongClick(LatLng latLng) {
            Log.d(TAG, "onMapLongClick: 被长按了一下");
            searchPoint = latLng;
            baiduMap.clear();
            Marker marker = createMarker(latLng, R.mipmap.ic_localtion, false);
            marker.setTitle("点击搜索按钮，搜索周边");
            marker.setPosition(latLng);
        }
    };

    /**
     * 地图点击监听
     */
    private BaiduMap.OnMapClickListener onMapClickListener = new BaiduMap.OnMapClickListener() {
        @Override
        public void onMapClick(LatLng latLng) { // 点击地图的时候，返回经纬度
            Log.d(TAG, "onMapClick: 被单击了一下");
            // 地图定位图标点击事件监听接口
            baiduMap.setOnMyLocationClickListener(myLocationClickListener);
        }

        @Override
        public boolean onMapPoiClick(MapPoi mapPoi) {
            Log.d(TAG, "onMapPoiClick: " + mapPoi.toString());
            return false;
        }
    };

    /**
     * 发起检索请求
     *
     * @param keyword
     * @param pageNum
     */
    private void searchInCity(String keyword, int pageNum) {
        if (city == null) {
            city = "深圳市";
        }
        poiSearch.searchInCity(new PoiCitySearchOption().city(city).keyword(keyword).pageNum(pageNum));
    }

    /**
     * 发起检索请求
     *
     * @param latLng
     * @param keyword
     * @param pageNum
     * @param radius
     */
    private void searchNearby(LatLng latLng, String keyword, int pageNum, int radius) {
        poiSearch.searchNearby(new PoiNearbySearchOption().keyword(keyword).location(latLng).pageNum(pageNum).radius(radius));
    }

    /**
     * 请求定位
     */
    private void requestLocation() {
        isRequest = true;
        if (null != locationClient && locationClient.isStarted()) {
            showToast("正在定位..."); // 正在定位
            locationClient.requestLocation();
        } else {
            Log.d(TAG, "requestLocation: 定位未开始");
        }
    }

    protected void showToast(String s) {
        if (toast == null) {
            toast = Toast.makeText(this, s, Toast.LENGTH_SHORT);
        } else {
            toast.setText(s);
        }
        toast.show();
    }

    @Override
    public void onGetGeoCodeResult(GeoCodeResult geoCodeResult) {
        Log.d(TAG, "onGetGeoCodeResult: geoCodeResult.getLocation-->" + geoCodeResult.getLocation());
        Log.d(TAG, "onGetGeoCodeResult: geoCodeResult.getAddress-->" + geoCodeResult.getAddress());
    }

    protected final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 100:
                    Map<String, Object> map = (Map<String, Object>) msg.obj;
                    String title = (String) map.get("title");
                    LatLng latLng = (LatLng) map.get("position");
                    btnPop.setText(title); // 点击地图的时候弹出的泡泡显示地址信息
                    createInfoWindow(btnPop, latLng, -64); // 创建InfoWindow
                    break;
                case 200: // 显示附近POI检索结果
                    baiduMap.clear();
                    List<PoiInfo> allPoi = (List<PoiInfo>) msg.obj;
                    if (allPoi != null) {
                        for (PoiInfo poiInfo : allPoi) {
                            Marker marker = createMarker(poiInfo.location, R.mipmap.ic_localtion, false);// 构建Marker图标
                            marker.setTitle(poiInfo.name);
                        }
                        updateMap(allPoi.get(0).location);
                    }
                    break;
            }
        }
    };

    @Override
    public void onGetReverseGeoCodeResult(ReverseGeoCodeResult reverseGeoCodeResult) {
        if (reverseGeoCodeResult == null || reverseGeoCodeResult.error != SearchResult.ERRORNO.NO_ERROR) {
            Log.d(TAG, "onGetReverseGeoCodeResult: 没有结果");
            return;
        }
        address = reverseGeoCodeResult.getAddress();
        if (TextUtils.isEmpty(address)) {
            address = "未知地址";
        }
        Log.d(TAG, "onGetReverseGeoCodeResult: 当前地址：" + address);
        searchPoint = reverseGeoCodeResult.getLocation();
    }

    @Override
    protected void onStart() {
        super.onStart();
        //开启定位的允许
        baiduMap.setMyLocationEnabled(true);
        if (!locationClient.isStarted()) {
            locationClient.start();
            //开启方向传感器
            myOrientationListener.star();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        locationClient.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        geocoder.destroy();
        poiSearch.destroy();
        //关闭定位
        baiduMap.setMyLocationEnabled(false);
        locationClient.stop();
        //停止方向传感器
        myOrientationListener.stop();
        locationClient.unRegisterLocationListener(myLocationListener);
    }

}
