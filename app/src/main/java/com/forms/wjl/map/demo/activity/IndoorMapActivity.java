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
import android.widget.LinearLayout;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.MapBaseIndoorMapInfo;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.overlayutil.IndoorRouteOverlay;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiIndoorInfo;
import com.baidu.mapapi.search.poi.PoiIndoorOption;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.baidu.mapapi.search.route.BikingRouteResult;
import com.baidu.mapapi.search.route.DrivingRouteResult;
import com.baidu.mapapi.search.route.IndoorPlanNode;
import com.baidu.mapapi.search.route.IndoorRouteLine;
import com.baidu.mapapi.search.route.IndoorRoutePlanOption;
import com.baidu.mapapi.search.route.IndoorRouteResult;
import com.baidu.mapapi.search.route.MassTransitRouteResult;
import com.baidu.mapapi.search.route.OnGetRoutePlanResultListener;
import com.baidu.mapapi.search.route.RoutePlanSearch;
import com.baidu.mapapi.search.route.TransitRouteResult;
import com.baidu.mapapi.search.route.WalkingRouteResult;
import com.forms.wjl.map.demo.R;
import com.forms.wjl.map.demo.base.BaseActivity;
import com.forms.wjl.map.demo.view.MyOrientationListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IndoorMapActivity extends BaseActivity implements OnGetGeoCoderResultListener {

    private static final String TAG = "BaseMapActivity";
    private LinearLayout lltInput;
    private RoutePlanSearch mRoutePlanSearch;
    private PoiSearch mPoiSearch;
    private MapView mapView;
    private BaiduMap baiduMap;
    private EditText etStartNode;
    private EditText etEndNode;
    private Button btnIndoorMapRoute;
    private Button button;
    private Button btnPop; // 点击地图显示获取地址的泡泡
    private GeoCoder geocoder = null;
    private String address;
    private LatLng point;
    private LatLng startNodeLatLng;
    private LatLng endNodeLatLng;
    private LocationClient locationClient;
    private LocationClientOption mOption;
    private MyLocationListener myLocationListener;
    private MyOrientationListener myOrientationListener; // 方向传感器
    private float mCurrentX = 0; // 定位箭头方向
    private boolean isFirstLocation = true; // 首次定位
    private BitmapDescriptor mBitmapDescriptor = null; // 自定义定位图标
    private Toast toast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_indoor_map);
        initViews();
        initData();
        initListener();
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

    @Override
    protected void initViews() {
        mRoutePlanSearch = RoutePlanSearch.newInstance();
        mPoiSearch = PoiSearch.newInstance();
        myOrientationListener = new MyOrientationListener(this);
        mapView = (MapView) findViewById(R.id.mv_map);
        lltInput = (LinearLayout) findViewById(R.id.lltInput);
        btnIndoorMapRoute = (Button) findViewById(R.id.btnIndoorMapRoute);
        etStartNode = (EditText) findViewById(R.id.etStartNode);
        etEndNode = (EditText) findViewById(R.id.etEndNode);
        button = getPopButton();
        btnPop = getPopButton(); // 创建InfoWindow展示的view
        baiduMap = mapView.getMap();
        baiduMap.setIndoorEnable(true); // 打开室内图(默认是关闭的)
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
        btnIndoorMapRoute.setOnClickListener(this);
        geocoder.setOnGetGeoCodeResultListener(this);
        baiduMap.setOnMapClickListener(new BaiduMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) { // 点击地图的时候，返回经纬度
                setReverseGeoCodeListener(latLng); // 设置地理反编码监听，根据经纬度获取地理位置信息等
                Log.d(TAG, "onMapClick: latLng-->" + latLng.toString());
                startNodeLatLng = latLng;
                etStartNode.setText(latLng.toString());
            }

            @Override
            public boolean onMapPoiClick(MapPoi mapPoi) {
                Log.d(TAG, "onMapPoiClick: " + mapPoi.toString());
                return false;
            }
        });
        baiduMap.setOnMapLongClickListener(new BaiduMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                endNodeLatLng = latLng;
                etEndNode.setText(latLng.toString());
            }
        });
        myOrientationListener.setmOnOrientationListener(new MyOrientationListener.OnOrientationListener() {
            @Override
            public void onOrientationChanged(float x) {
                mCurrentX = x;
            }
        });
        // 通过设置室内图监听事件来监听进入和移出室内图：
        baiduMap.setOnBaseIndoorMapListener(new BaiduMap.OnBaseIndoorMapListener() {
            @Override
            public void onBaseIndoorMapMode(boolean b, MapBaseIndoorMapInfo mapBaseIndoorMapInfo) {
                if (b) {
                    // 进入室内图
                    Log.d(TAG, "onBaseIndoorMapMode: 已入室内图");
                    // 通过获取回调参数mapBaseIndoorMapInfo 来获取室内图信息，包含楼层信息，室内ID等
                    String id = mapBaseIndoorMapInfo.getID();
                    Log.d(TAG, "onBaseIndoorMapMode: ID-->" + id);
                    Log.d(TAG, "onBaseIndoorMapMode: Floors-->" + mapBaseIndoorMapInfo.getFloors());
                    String floor = mapBaseIndoorMapInfo.getCurFloor();
                    Log.d(TAG, "onBaseIndoorMapMode: CurFloor-->" + floor);
                    mPoiSearch.setOnGetPoiSearchResultListener(poiSearchResultListener);
                    PoiIndoorOption option = new PoiIndoorOption().poiIndoorBid(id).poiIndoorWd("DHC");
                    mPoiSearch.searchPoiIndoor(option);
                    // 切换楼层信息
                    MapBaseIndoorMapInfo.SwitchFloorError error = baiduMap.switchBaseIndoorMapFloor(floor, id);
                    switch (error) {
                        case SWITCH_OK:
                            // 切换成功
                            Log.d(TAG, "onBaseIndoorMapMode: 切换成功");
                            break;
                        case FLOOR_INFO_ERROR:
                            // 切换楼层, 室内ID信息错误
                            Log.d(TAG, "onBaseIndoorMapMode: 切换楼层, 室内ID信息错误");
                            break;
                        case FLOOR_OVERLFLOW:
                            // 切换楼层室内ID与当前聚焦室内ID不匹配
                            Log.d(TAG, "onBaseIndoorMapMode: 切换楼层室内ID与当前聚焦室内ID不匹配");
                            break;
                        case FOCUSED_ID_ERROR:
                            // 切换楼层室内ID与当前聚焦室内ID不匹配
                            Log.d(TAG, "onBaseIndoorMapMode: 切换楼层室内ID与当前聚焦室内ID不匹配");
                            break;
                        case SWITCH_ERROR:
                            // 切换楼层错误
                            Log.d(TAG, "onBaseIndoorMapMode: 切换楼层错误");
                            break;
                        default:
                            break;
                    }
                } else {
                    Log.d(TAG, "onBaseIndoorMapMode: 已出室内图");
                    // 移出室内图
                }
            }
        });
    }

    private OnGetPoiSearchResultListener poiSearchResultListener = new OnGetPoiSearchResultListener() {
        @Override
        public void onGetPoiResult(PoiResult poiResult) {

        }

        @Override
        public void onGetPoiDetailResult(PoiDetailResult poiDetailResult) {

        }

        @Override
        public void onGetPoiIndoorResult(PoiIndoorResult poiIndoorResult) {
            List<PoiIndoorInfo> indoorInfoList = poiIndoorResult.getmArrayPoiInfo();
            if (indoorInfoList != null) {
                for (PoiIndoorInfo info : indoorInfoList) {
                    Log.d(TAG, "onGetPoiIndoorResult: floor--> " + info.floor);
                    Log.d(TAG, "onGetPoiIndoorResult: price--> " + info.price);
                    Log.d(TAG, "onGetPoiIndoorResult: discount--> " + info.discount);
                }
            }
        }
    };

    private IndoorRouteOverlay indoorRouteOverlay;

    private OnGetRoutePlanResultListener routeListener = new OnGetRoutePlanResultListener() {
        @Override
        public void onGetWalkingRouteResult(WalkingRouteResult walkingRouteResult) {

        }

        @Override
        public void onGetTransitRouteResult(TransitRouteResult transitRouteResult) {

        }

        @Override
        public void onGetMassTransitRouteResult(MassTransitRouteResult massTransitRouteResult) {

        }

        @Override
        public void onGetDrivingRouteResult(DrivingRouteResult drivingRouteResult) {

        }

        @Override
        public void onGetIndoorRouteResult(IndoorRouteResult result) {
            //获取室内线路规划检索结果
            List<IndoorRouteLine> routeLines = result.getRouteLines();
            if (null != routeLines) {
                Log.d(TAG, "onGetIndoorRouteResult: result-->" + routeLines.size());
                for (IndoorRouteLine rl : routeLines) {
                    List<IndoorRouteLine.IndoorRouteStep> allStep = rl.getAllStep();
                    allStep.get(0).getEntrace().getLocation();
                    Log.d(TAG, "onGetIndoorRouteResult: getDistance" + rl.getDistance());
                    Log.d(TAG, "onGetIndoorRouteResult: getDuration" + rl.getDuration());
                }
                if (routeLines.size() > 0) {
                    indoorRouteOverlay = new IndoorRouteOverlay(baiduMap);
                    baiduMap.setOnMarkerClickListener(indoorRouteOverlay);
                    indoorRouteOverlay.setData(result.getRouteLines().get(0));
                    indoorRouteOverlay.addToMap();
                    indoorRouteOverlay.zoomToSpan();
                }
            } else {
                showToast("没有找到相关路线");
            }
        }

        @Override
        public void onGetBikingRouteResult(BikingRouteResult bikingRouteResult) {

        }
    };

    protected void showToast(String s) {
        if (toast == null) {
            toast = Toast.makeText(this, s, Toast.LENGTH_SHORT);
        } else {
            toast.setText(s);
        }
        toast.show();
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
    private void createMarker(LatLng latLng, int resId, boolean draggable) {
        BitmapDescriptor bitmap = BitmapDescriptorFactory.fromResource(resId); // 构建Marker图标
        // 构建MarkerOption，用于在地图上添加Marker
        OverlayOptions option = new MarkerOptions().draggable(draggable).position(latLng).icon(bitmap);
        baiduMap.addOverlay(option); // 在地图上添加Marker，并显示
    }

    /***
     *
     * @return DefaultLocationClientOption
     */
    public LocationClientOption getDefaultLocationClientOption() {
        if (mOption == null) {
            mOption = new LocationClientOption();
            mOption.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);//可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
            mOption.setCoorType("bd09ll");//可选，默认gcj02，设置返回的定位结果坐标系，如果配合百度地图使用，建议设置为bd09ll;
            mOption.setScanSpan(3000);//可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
            mOption.setIsNeedAddress(true);//可选，设置是否需要地址信息，默认不需要
            mOption.setIsNeedLocationDescribe(true);//可选，设置是否需要地址描述
            mOption.setNeedDeviceDirect(false);//可选，设置是否需要设备方向结果
            mOption.setLocationNotify(false);//可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
            mOption.setIgnoreKillProcess(true);//可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
            mOption.setIsNeedLocationDescribe(true);//可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
            mOption.setIsNeedLocationPoiList(true);//可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
            mOption.SetIgnoreCacheException(false);//可选，默认false，设置是否收集CRASH信息，默认收集
        }
        return mOption;
    }

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
                // 设置定位数据
                baiduMap.setMyLocationData(locData);
                MyLocationConfiguration configuration = new MyLocationConfiguration(MyLocationConfiguration.LocationMode.NORMAL, true, mBitmapDescriptor);
                baiduMap.setMyLocationConfigeration(configuration);
                if (isFirstLocation) {
                    String city = bdLocation.getCity();
                    Log.d(TAG, "onReceiveLocation: " + city);
                    point = new LatLng(bdLocation.getLatitude(), bdLocation.getLongitude()); // 获取定位经纬度
                    button.setText(city);
                    baiduMap.clear();
                    createInfoWindow(button, point, -32);
                    MapStatusUpdate update = MapStatusUpdateFactory.newLatLngZoom(point, baiduMap.getMaxZoomLevel() - 4);
                    baiduMap.animateMapStatus(update);
                    isFirstLocation = false;
                }
            }

        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnIndoorMapRoute:
                if (TextUtils.isEmpty(etStartNode.getText()) || TextUtils.isEmpty(etEndNode.getText())) {
                    lltInput.setVisibility(View.VISIBLE);
                } else {
                    // 开始导航
                    if (indoorRouteOverlay != null) {
                        indoorRouteOverlay.removeFromMap();
                    }
                    mRoutePlanSearch.setOnGetRoutePlanResultListener(routeListener);
                    IndoorPlanNode startNode = new IndoorPlanNode(startNodeLatLng, "F1");
                    IndoorPlanNode endNode = new IndoorPlanNode(endNodeLatLng, "F1");
                    IndoorRoutePlanOption option = new IndoorRoutePlanOption().from(startNode).to(endNode);
                    mRoutePlanSearch.walkingIndoorSearch(option);
                    etStartNode.setText("");
                    etEndNode.setText("");
                    lltInput.setVisibility(View.GONE);
                }
                break;
        }
    }

    @Override
    public void onGetGeoCodeResult(GeoCodeResult geoCodeResult) {

    }

    protected final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 100:
                    Map<String, Object> map = (Map<String, Object>) msg.obj;
                    address = (String) map.get("address");
                    LatLng latLng = (LatLng) map.get("latLng");
                    baiduMap.clear();
                    btnPop.setText(address); // 点击地图的时候弹出的泡泡显示地址信息
                    createMarker(latLng, R.mipmap.ic_localtion, false); // 构建Marker图标
                    createInfoWindow(btnPop, latLng, -64); // 创建InfoWindow
                    updateMap(latLng); // 更新地图显示
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
        Message msg = handler.obtainMessage();
        Map<String, Object> map = new HashMap();
        map.put("address", address);
        map.put("latLng", reverseGeoCodeResult.getLocation());
        msg.obj = map;
        msg.what = 100;
        handler.sendMessage(msg);
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
        //关闭定位
        baiduMap.setMyLocationEnabled(false);
        locationClient.stop();
        //停止方向传感器
        myOrientationListener.stop();
        locationClient.unRegisterLocationListener(myLocationListener);
        mRoutePlanSearch.destroy();
        mPoiSearch.destroy();
    }
}
