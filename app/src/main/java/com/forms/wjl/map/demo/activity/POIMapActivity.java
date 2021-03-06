package com.forms.wjl.map.demo.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.IdRes;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
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
import com.baidu.mapapi.search.busline.BusLineResult;
import com.baidu.mapapi.search.busline.BusLineSearch;
import com.baidu.mapapi.search.busline.BusLineSearchOption;
import com.baidu.mapapi.search.busline.OnGetBusLineSearchResultListener;
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

import java.util.ArrayList;
import java.util.List;

public class POIMapActivity extends BaseActivity implements OnGetGeoCoderResultListener {

    private static final String TAG = "POIMapActivity";
    private MapView mapView;
    private BaiduMap baiduMap;
    private Button button;
    private Button btnPop; // 点击地图显示获取地址的泡泡
    private Button btnSearch; // 搜索按钮
    private AutoCompleteTextView etKeyword; // 关键字输入编辑框
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
    private BusLineSearch busLineSearch; // 公交线路检索
    private ArrayAdapter<String> adapter;
    private List<String> names;
    private TextView tvSearchResult;
    private RadioGroup rgSearchType;
    private int searchType = 0;

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
        rgSearchType = (RadioGroup) findViewById(R.id.rgSearchType);
        tvSearchResult = (TextView) findViewById(R.id.tvSearchResult);
        etKeyword = (AutoCompleteTextView) findViewById(R.id.etKeyword);
        btnSearch = (Button) findViewById(R.id.btnSearch);
        ivLocal = (ImageView) findViewById(R.id.ivLocal);
        myOrientationListener = new MyOrientationListener(this);
        mapView = (MapView) findViewById(R.id.mv_map);
        poiSearch = PoiSearch.newInstance();
        busLineSearch = BusLineSearch.newInstance();
        button = getPopButton();
        btnPop = getPopButton(); // 创建InfoWindow展示的view
        baiduMap = mapView.getMap();
        // 定义Maker坐标点
        locationClient = new LocationClient(this);
        locationClient.setLocOption(getDefaultLocationClientOption());
        myLocationListener = new MyLocationListener();
        locationClient.registerLocationListener(myLocationListener);
        locationClient.start();
    }

    @Override
    protected void initData() {
        names = new ArrayList<>();
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
        etKeyword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                Log.d(TAG, "beforeTextChanged: " + s.toString());
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.d(TAG, "onTextChanged: " + s.toString());
                if(s.length() > 0){
                    searchInCity(s.toString(),100);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                Log.d(TAG, "afterTextChanged: " + s.toString());
            }
        });
        rgSearchType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                RadioButton rb = (RadioButton) group.findViewById(checkedId);
                String string = rb.getText().toString();
                Log.d(TAG, "onCheckedChanged: " + string);
                switch (string) {
                    case "周边":
                        searchType = 0;
                        etKeyword.setHint("输入关键字检索周边");
                        break;
                    case "城市":
                        searchType = 1;
                        etKeyword.setHint("输入关键字检索当前城市");
                        break;
                    case "公交":
                        searchType = 2;
                        etKeyword.setHint("输入关键字检索公交");
                        break;
                }
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
                String keyword = etKeyword.getText().toString();
                if (!TextUtils.isEmpty(keyword)) {
                    switch (searchType) {
                        case 0: // 周边
                            searchNearby(searchPoint, keyword, 10, 1000);
                            break;
                        case 1: // 需要获取搜索范围的城市
                            searchInCity(keyword, 10);
                            break;
                        case 2: // 公交
                            searchInCity(keyword, 100);
                            break;
                    }
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
     * @param popView
     * @param latLng  地理坐标
     * @param offset  y 轴偏移量
     */
    private void createInfoWindow(View popView, LatLng latLng, int offset) {
        InfoWindow mInfoWindow = new InfoWindow(popView, latLng, offset);
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
            if (null != result && result.error == SearchResult.ERRORNO.NO_ERROR) {
                //获取POI检索结果
                List<PoiInfo> allPoi = result.getAllPoi();
                if (allPoi != null) {
                    if(searchType == 2) {
                        for(PoiInfo poiInfo : allPoi){
                            if (poiInfo.type == PoiInfo.POITYPE.BUS_LINE || poiInfo.type == PoiInfo.POITYPE.SUBWAY_LINE) {
                                //说明该条POI为公交信息，获取该条POI的UID
                                String uid = poiInfo.uid;
                                busLineSearch.searchBusLine(new BusLineSearchOption().city(city).uid(uid));
                                busLineSearch.setOnGetBusLineSearchResultListener(new OnGetBusLineSearchResultListener() {
                                    @Override
                                    public void onGetBusLineResult(BusLineResult busLineResult) {

                                    }
                                });
                            } else {
                                showToast("没有找到公交信息");
                            }
                        }
                    }
                    tvSearchResult.setText("本次共搜索到" + allPoi.size() + "条记录");
                    Message msg = handler.obtainMessage();
                    msg.obj = allPoi;
                    msg.what = 200;
                    handler.sendMessage(msg);
                } else {
                    showToast("没有搜到相关结果!");
                    tvSearchResult.setText("本次共搜索到0条记录");
                }
            } else {
                tvSearchResult.setText("本次共搜索到0条记录");
                showToast("没有搜到相关结果!!");
            }
        }

        public void onGetPoiDetailResult(PoiDetailResult result) {
            //获取Place详情页检索结果
            Log.d(TAG, "onGetPoiDetailResult: " + result.toString());
            if (result.error != SearchResult.ERRORNO.NO_ERROR) {
                //详情检索失败
            } else {
                //检索成功
            }
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
            LatLng latLng = marker.getPosition();
            Bundle bundle = marker.getExtraInfo();
            if (bundle != null) {
                PoiInfo poiInfo = bundle.getParcelable("poiInfo");
                if (poiInfo != null) {
                    View popView = LayoutInflater.from(POIMapActivity.this).inflate(R.layout.layout_pop_window_poi, null);
                    TextView tvName = (TextView) popView.findViewById(R.id.tvName);
                    TextView tvAddress = (TextView) popView.findViewById(R.id.tvAddress);
                    TextView tvPhone = (TextView) popView.findViewById(R.id.tvPhone);
                    tvName.setText(poiInfo.name);
                    tvAddress.setText(poiInfo.address);
                    tvPhone.setText(poiInfo.phoneNum);
                    popView.setBackgroundResource(R.mipmap.ic_pop_info_window);
                    createInfoWindow(popView, latLng, -64); // 创建InfoWindow
                }
            } else {
                btnPop.setText(marker.getTitle());
                createInfoWindow(btnPop, marker.getPosition(), -64);
            }
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
            baiduMap.hideInfoWindow();
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

                    break;
                case 200: // 显示附近POI检索结果
                    baiduMap.clear();
                    List<PoiInfo> allPoi = (List<PoiInfo>) msg.obj;
                    List<String> list = new ArrayList<>();
                    if (allPoi != null) {
                        for (PoiInfo poiInfo : allPoi) {
                            Bundle bundle = new Bundle();
                            Marker marker = createMarker(poiInfo.location, R.mipmap.ic_localtion, false);// 构建Marker图标
                            marker.setTitle(poiInfo.name);
                            list.add(poiInfo.name);
                            bundle.putParcelable("poiInfo", poiInfo);
                            marker.setExtraInfo(bundle);
                        }
                        updateMap(allPoi.get(0).location);
                        names.clear();
                        names.addAll(list);
                        adapter = new ArrayAdapter<>(POIMapActivity.this, android.R.layout.simple_dropdown_item_1line, names);
                        etKeyword.setAdapter(adapter);
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
