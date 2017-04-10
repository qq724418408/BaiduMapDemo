package com.forms.wjl.map.demo.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.IdRes;
import android.text.TextUtils;
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
import com.baidu.mapapi.overlayutil.BikingRouteOverlay;
import com.baidu.mapapi.overlayutil.DrivingRouteOverlay;
import com.baidu.mapapi.overlayutil.IndoorRouteOverlay;
import com.baidu.mapapi.overlayutil.MassTransitRouteOverlay;
import com.baidu.mapapi.overlayutil.TransitRouteOverlay;
import com.baidu.mapapi.overlayutil.WalkingRouteOverlay;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiNearbySearchOption;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.baidu.mapapi.search.route.BikingRouteLine;
import com.baidu.mapapi.search.route.BikingRoutePlanOption;
import com.baidu.mapapi.search.route.BikingRouteResult;
import com.baidu.mapapi.search.route.DrivingRouteLine;
import com.baidu.mapapi.search.route.DrivingRoutePlanOption;
import com.baidu.mapapi.search.route.DrivingRouteResult;
import com.baidu.mapapi.search.route.IndoorRouteLine;
import com.baidu.mapapi.search.route.IndoorRouteResult;
import com.baidu.mapapi.search.route.MassTransitRouteLine;
import com.baidu.mapapi.search.route.MassTransitRouteResult;
import com.baidu.mapapi.search.route.OnGetRoutePlanResultListener;
import com.baidu.mapapi.search.route.PlanNode;
import com.baidu.mapapi.search.route.RoutePlanSearch;
import com.baidu.mapapi.search.route.TransitRouteLine;
import com.baidu.mapapi.search.route.TransitRoutePlanOption;
import com.baidu.mapapi.search.route.TransitRouteResult;
import com.baidu.mapapi.search.route.WalkingRouteLine;
import com.baidu.mapapi.search.route.WalkingRoutePlanOption;
import com.baidu.mapapi.search.route.WalkingRouteResult;
import com.baidu.navisdk.adapter.BaiduNaviManager;
import com.forms.wjl.map.demo.R;
import com.forms.wjl.map.demo.base.BaseActivity;
import com.forms.wjl.map.demo.view.MyOrientationListener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.baidu.navisdk.adapter.PackageUtil.getSdcardDir;

public class NaviActivity extends BaseActivity {

    private static final String TAG = "NaviActivity";
    private MapView mapView;
    private BaiduMap baiduMap;
    private Button button;
    private Button btnPop; // 点击地图显示获取地址的泡泡
    private Button btnSearch; // 搜索按钮
    private AutoCompleteTextView etKeyword; // 关键字输入编辑框
    private LatLng point;
    private LatLng startLatLng; // 起点坐标
    private LatLng endLatLng; // 终点坐标
    private LocationClient locationClient;
    private LocationClientOption mOption;
    private MyLocationListener myLocationListener;
    private RoutePlanSearch routePlanSearch; // 线路检索实例
    private MyOrientationListener myOrientationListener; // 方向传感器
    private float mCurrentX = 0; // 定位箭头方向
    private boolean isFirstLocation = true; // 首次定位
    private BitmapDescriptor mBitmapDescriptor = null; // 自定义定位图标
    private ImageView ivLocal; // 定位按钮
    private boolean isRequest = false;
    private Toast toast;
    private String city;
    private ArrayAdapter<String> adapter;
    private List<String> names;
    private TextView tvSearchResult;
    private RadioGroup rgSearchType;
    private PoiSearch poiSearch; // 周边检索
    private DrivingRouteOverlay drivingRouteOverlay;
    private MassTransitRouteOverlay massTransitRouteOverlay;
    private IndoorRouteOverlay indoorRouteOverlay;
    private BikingRouteOverlay bikingRouteOverlay;
    private WalkingRouteOverlay walkingRouteOverlay;
    private String mSDCardPath = null;
    private static final String[] authBaseArr = {Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION};
    private static final int authBaseRequestCode = 1;
    private static final int authComRequestCode = 2;
    private String authInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_plan);
        initViews();
        initData();
        initListener();
        if (initDirs()) {
            initNavi();
        }
    }

    private boolean initDirs() {
        mSDCardPath = getSdcardDir();
        if (mSDCardPath == null) {
            return false;
        }
        File f = new File(mSDCardPath, TAG);
        if (!f.exists()) {
            try {
                f.mkdir();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    /**
     * 内部TTS播报状态回传handler
     */
    private Handler ttsHandler = new Handler() {
        public void handleMessage(Message msg) {
            int type = msg.what;
            switch (type) {
                case BaiduNaviManager.TTSPlayMsgType.PLAY_START_MSG: {
                    showToast("Handler : TTS play start");
                    break;
                }
                case BaiduNaviManager.TTSPlayMsgType.PLAY_END_MSG: {
                    showToast("Handler : TTS play end");
                    break;
                }
                default:
                    break;
            }
        }
    };

    /**
     * 内部TTS播报状态回调接口
     */
    private BaiduNaviManager.TTSPlayStateListener ttsPlayStateListener = new BaiduNaviManager.TTSPlayStateListener() {

        @Override
        public void playEnd() {
            showToast("TTSPlayStateListener : TTS play end");
        }

        @Override
        public void playStart() {
            showToast("TTSPlayStateListener : TTS play start");
        }
    };

    private boolean hasBasePhoneAuth() {
        PackageManager pm = this.getPackageManager();
        for (String auth : authBaseArr) {
            if (pm.checkPermission(auth, this.getPackageName()) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void initNavi() {
        // 申请权限
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            if (!hasBasePhoneAuth()) {
                this.requestPermissions(authBaseArr, authBaseRequestCode);
                return;
            }
        }
        // 在初始化导航前，需要调用APPID的设置接口，否则会没有声音
//        Bundle bundle = new Bundle();
//        // 必须设置APPID，否则会静音
//        bundle.putString(BNCommonSettingParam.TTS_APP_ID, "9463570");
//        BNaviSettingManager.setNaviSdkParam(bundle);
        BaiduNaviManager.getInstance().init(this, mSDCardPath, TAG, new BaiduNaviManager.NaviInitListener() {

            @Override
            public void onAuthResult(int status, String msg) {
                if (0 == status) {
                    authInfo = "key校验成功";
                } else {
                    authInfo = "key校验失败 --> " + msg;
                }
                NaviActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        showToast(authInfo);
                    }
                });
            }

            @Override
            public void initStart() {
                showToast("百度导航引擎初始化开始");
            }

            @Override
            public void initSuccess() {
                showToast("百度导航引擎初始化成功");
            }

            @Override
            public void initFailed() {
                showToast("百度导航引擎初始化失败");
            }
        }, null, null, null);
    }

    @Override
    protected void initViews() {
        poiSearch = PoiSearch.newInstance();
        routePlanSearch = RoutePlanSearch.newInstance();
        rgSearchType = (RadioGroup) findViewById(R.id.rgSearchType);
        tvSearchResult = (TextView) findViewById(R.id.tvSearchResult);
        etKeyword = (AutoCompleteTextView) findViewById(R.id.etKeyword);
        btnSearch = (Button) findViewById(R.id.btnSearch);
        ivLocal = (ImageView) findViewById(R.id.ivLocal);
        myOrientationListener = new MyOrientationListener(this);
        mapView = (MapView) findViewById(R.id.mv_map);
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
        mBitmapDescriptor = BitmapDescriptorFactory.fromResource(R.mipmap.ic_arrow);
    }

    /**
     * 初始化、注册事件监听
     */
    @Override
    protected void initListener() {
        poiSearch.setOnGetPoiSearchResultListener(poiListener);
        routePlanSearch.setOnGetRoutePlanResultListener(myRoutePlanResultListener);
        ivLocal.setOnClickListener(this);
        btnSearch.setOnClickListener(this);
        baiduMap.setOnMapClickListener(onMapClickListener); // 地图单击事件
        baiduMap.setOnMapLongClickListener(onMapLongClickListener); // 地图长按事件
        baiduMap.setOnMarkerClickListener(onMarkerClickListener); //
        myOrientationListener.setmOnOrientationListener(new MyOrientationListener.OnOrientationListener() {
            @Override
            public void onOrientationChanged(float x) {
                mCurrentX = x;
            }
        });
        rgSearchType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                RadioButton rb = (RadioButton) group.findViewById(checkedId);
                String string = rb.getText().toString();
                PlanNode startPlanNode = PlanNode.withLocation(startLatLng);
                PlanNode endPlanNode = PlanNode.withLocation(endLatLng);
                removeOverlayFromMap(); // 清除已有线路
                switch (string) {
                    case "驾车":
                        if (startLatLng != null && endLatLng != null) {
                            routePlanSearch.drivingSearch(new DrivingRoutePlanOption().from(startPlanNode).to(endPlanNode));
                        }
                        break;
                    case "步行":
                        if (startLatLng != null && endLatLng != null) {
                            routePlanSearch.walkingSearch(new WalkingRoutePlanOption().from(startPlanNode).to(endPlanNode));
                        }
                        break;
                    case "公交":
                        if (startLatLng != null && endLatLng != null) {
                            //routePlanSearch.masstransitSearch(new MassTransitRoutePlanOption().from(startPlanNode).to(endPlanNode));
                            routePlanSearch.transitSearch(new TransitRoutePlanOption().city(city).from(startPlanNode).to(endPlanNode));
                        }
                        break;
                    case "骑行":
                        if (startLatLng != null && endLatLng != null) {
                            routePlanSearch.bikingSearch(new BikingRoutePlanOption().from(startPlanNode).to(endPlanNode));
                        }
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
                    searchNearby(point, keyword, 10, 1000);
                } else {
                    showToast("请输入关键字");
                }
                break;
        }
    }

    /**
     *
     */
    protected void removeOverlayFromMap() {
        if (walkingRouteOverlay != null) {
            walkingRouteOverlay.removeFromMap();
        }
        if (transitRouteOverlay != null) {
            transitRouteOverlay.removeFromMap();
        }
        if (drivingRouteOverlay != null) {
            drivingRouteOverlay.removeFromMap();
        }
        if (indoorRouteOverlay != null) {
            indoorRouteOverlay.removeFromMap();
        }
        if (massTransitRouteOverlay != null) {
            massTransitRouteOverlay.removeFromMap();
        }
        if (bikingRouteOverlay != null) {
            bikingRouteOverlay.removeFromMap();
        }
    }

    private TransitRouteOverlay transitRouteOverlay;

    protected OnGetRoutePlanResultListener myRoutePlanResultListener = new OnGetRoutePlanResultListener() {


        @Override
        public void onGetWalkingRouteResult(WalkingRouteResult result) {
            Log.d(TAG, "onGetWalkingRouteResult: ");
            if (null != result && result.error == SearchResult.ERRORNO.NO_ERROR) {
                // 有结果
                List<WalkingRouteLine> routeLines = result.getRouteLines();
                if (routeLines != null && routeLines.size() > 0) {
                    walkingRouteOverlay = new WalkingRouteOverlay(baiduMap);
                    baiduMap.setOnMarkerClickListener(walkingRouteOverlay);
                    walkingRouteOverlay.setData(result.getRouteLines().get(0));
                    walkingRouteOverlay.addToMap();
                    walkingRouteOverlay.zoomToSpan();
                }
            }
        }

        @Override
        public void onGetTransitRouteResult(TransitRouteResult result) {
            Log.d(TAG, "onGetTransitRouteResult: ");
            if (null != result && result.error == SearchResult.ERRORNO.NO_ERROR) {
                // 有结果
                List<TransitRouteLine> routeLines = result.getRouteLines();
                if (routeLines != null && routeLines.size() > 0) {
                    transitRouteOverlay = new TransitRouteOverlay(baiduMap);
                    baiduMap.setOnMarkerClickListener(transitRouteOverlay);
                    transitRouteOverlay.setData(result.getRouteLines().get(0));
                    transitRouteOverlay.addToMap();
                    transitRouteOverlay.zoomToSpan();
                }
            }
        }

        @Override
        public void onGetMassTransitRouteResult(MassTransitRouteResult result) {
            Log.d(TAG, "onGetMassTransitRouteResult: ");
            if (null != result && result.error == SearchResult.ERRORNO.NO_ERROR) {
                // 有结果
                List<MassTransitRouteLine> routeLines = result.getRouteLines();
                if (routeLines != null && routeLines.size() > 0) {
                    massTransitRouteOverlay = new MassTransitRouteOverlay(baiduMap);
                    baiduMap.setOnMarkerClickListener(massTransitRouteOverlay);
                    massTransitRouteOverlay.setData(result.getRouteLines().get(0));
                    massTransitRouteOverlay.addToMap();
                    massTransitRouteOverlay.zoomToSpan();
                }
            }
        }

        @Override
        public void onGetDrivingRouteResult(DrivingRouteResult result) {
            Log.d(TAG, "onGetDrivingRouteResult: ");
            if (null != result && result.error == SearchResult.ERRORNO.NO_ERROR) {
                // 有结果
                List<DrivingRouteLine> routeLines = result.getRouteLines();
                if (routeLines != null && routeLines.size() > 0) {
                    drivingRouteOverlay = new DrivingRouteOverlay(baiduMap);
                    baiduMap.setOnMarkerClickListener(drivingRouteOverlay);
                    drivingRouteOverlay.setData(result.getRouteLines().get(0));
                    drivingRouteOverlay.addToMap();
                    drivingRouteOverlay.zoomToSpan();
                }
            }
        }

        @Override
        public void onGetIndoorRouteResult(IndoorRouteResult result) {
            Log.d(TAG, "onGetIndoorRouteResult: ");
            if (null != result && result.error == SearchResult.ERRORNO.NO_ERROR) {
                // 有结果
                List<IndoorRouteLine> routeLines = result.getRouteLines();
                if (routeLines != null && routeLines.size() > 0) {
                    indoorRouteOverlay = new IndoorRouteOverlay(baiduMap);
                    baiduMap.setOnMarkerClickListener(indoorRouteOverlay);
                    indoorRouteOverlay.setData(result.getRouteLines().get(0));
                    indoorRouteOverlay.addToMap();
                    indoorRouteOverlay.zoomToSpan();
                }
            }
        }

        @Override
        public void onGetBikingRouteResult(BikingRouteResult result) {
            Log.d(TAG, "onGetBikingRouteResult: ");
            if (null != result && result.error == SearchResult.ERRORNO.NO_ERROR) {
                // 有结果
                List<BikingRouteLine> routeLines = result.getRouteLines();
                if (routeLines != null && routeLines.size() > 0) {
                    bikingRouteOverlay = new BikingRouteOverlay(baiduMap);
                    baiduMap.setOnMarkerClickListener(bikingRouteOverlay);
                    bikingRouteOverlay.setData(result.getRouteLines().get(0));
                    bikingRouteOverlay.addToMap();
                    bikingRouteOverlay.zoomToSpan();
                }
            }
        }
    };

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
        public boolean onMarkerClick(final Marker marker) {
            LatLng latLng = marker.getPosition();
            Bundle bundle = marker.getExtraInfo();
            View popView = LayoutInflater.from(NaviActivity.this).inflate(R.layout.layout_pop_window_route_plan, null);
            TextView tvName = (TextView) popView.findViewById(R.id.tvName);
            TextView tvGotoHere = (TextView) popView.findViewById(R.id.tvGotoHere);
            TextView tvAddress = (TextView) popView.findViewById(R.id.tvAddress);
            TextView tvFromHere = (TextView) popView.findViewById(R.id.tvFromHere);
            TextView tvPhone = (TextView) popView.findViewById(R.id.tvPhone);
            tvGotoHere.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showToast("去这里"); // 终点
                    endLatLng = marker.getPosition();
                    //Marker endMarker = createMarker(endLatLng, R.mipmap.ic_end_marker, false);
                    //marker.setTitle("终点");
                }
            });
            tvFromHere.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showToast("从这出发"); // 起点
                    startLatLng = marker.getPosition();
                    //Marker startMarker = createMarker(startLatLng, R.mipmap.ic_end_marker, false);
                    //startMarker.setTitle("起点");
                }
            });
            if (bundle != null) {
                final PoiInfo poiInfo = bundle.getParcelable("poiInfo");
                if (poiInfo != null) {
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
                        adapter = new ArrayAdapter<>(NaviActivity.this, android.R.layout.simple_dropdown_item_1line, names);
                        etKeyword.setAdapter(adapter);
                    }
                    break;
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == authBaseRequestCode) {
            for (int ret : grantResults) {
                if (ret == 0) {
                    continue;
                } else {
                    showToast("缺少导航基本的权限");
                    return;
                }
            }
            initNavi();
        } else if (requestCode == authComRequestCode) {
            for (int ret : grantResults) {
                if (ret == 0) {
                    continue;
                }
            }
            //routeplanToNavi(mCoordinateType);
        }

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
        //关闭定位
        baiduMap.setMyLocationEnabled(false);
        locationClient.stop();
        //停止方向传感器
        myOrientationListener.stop();
        locationClient.unRegisterLocationListener(myLocationListener);
        routePlanSearch.destroy();
        poiSearch.destroy();
        if (BaiduNaviManager.isNaviInited()) {
            BaiduNaviManager.getInstance().uninit();
        }
    }

}
