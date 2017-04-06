package com.forms.wjl.map.demo.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
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
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.forms.wjl.map.demo.R;
import com.forms.wjl.map.demo.base.BaseActivity;
import com.forms.wjl.map.demo.view.MyOrientationListener;

import java.util.HashMap;
import java.util.Map;

public class BaseMapActivity extends BaseActivity implements OnGetGeoCoderResultListener {

    private static final String TAG = "BaseMapActivity";
    private MapView mapView;
    private ImageView ivLocal; // 定位按钮
    private TextView tvMapType; // 地图类型切换按钮
    private BaiduMap baiduMap;
    private Button button;
    private Button btnPop; // 点击地图显示获取地址的泡泡
    private GeoCoder geocoder = null;
    private String address;
    private LatLng point;
    private LocationClient locationClient;
    private LocationClientOption mOption;
    private MyLocationListener myLocationListener;
    private MyOrientationListener myOrientationListener; // 方向传感器
    private float mCurrentX = 0; // 定位箭头方向
    private boolean isFirstLocation = true; // 首次定位
    private BitmapDescriptor mBitmapDescriptor = null; // 自定义定位图标
    private boolean isRequest = false;
    private Toast toast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base_map);
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
        myOrientationListener = new MyOrientationListener(this);
        mapView = (MapView) findViewById(R.id.mv_map);
        tvMapType = (TextView) findViewById(R.id.tvMapType);
        ivLocal = (ImageView) findViewById(R.id.ivLocal);
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
        int childCount = mapView.getChildCount();
        Log.d(TAG, "initViews: childCount-->" + childCount);
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

    @Override
    protected void initData() {
        geocoder = GeoCoder.newInstance();
        mBitmapDescriptor = BitmapDescriptorFactory.fromResource(R.mipmap.ic_arrow);
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
                String city = bdLocation.getCity();
                Log.d(TAG, "onReceiveLocation: " + city);
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
                if (isFirstLocation || isRequest) {
                    point = new LatLng(bdLocation.getLatitude(), bdLocation.getLongitude()); // 获取定位经纬度
                    button.setText(city);
                    baiduMap.clear();
                    createInfoWindow(button, point, -32);
                    MapStatusUpdate update = MapStatusUpdateFactory.newLatLngZoom(point, baiduMap.getMaxZoomLevel() - 4);
                    baiduMap.animateMapStatus(update);
                    isRequest = false;
                }
                isFirstLocation = false;
            }

        }
    }

    /**
     * 初始化、注册事件监听
     */
    @Override
    protected void initListener() {
        geocoder.setOnGetGeoCodeResultListener(this);
        ivLocal.setOnClickListener(this);
        tvMapType.setOnClickListener(this);
        baiduMap.setOnMapClickListener(new BaiduMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) { // 点击地图的时候，返回经纬度
                setReverseGeoCodeListener(latLng); // 设置地理反编码监听，根据经纬度获取地理位置信息等
            }

            @Override
            public boolean onMapPoiClick(MapPoi mapPoi) {
                Log.d(TAG, "onMapPoiClick: " + mapPoi.toString());
                return false;
            }
        });
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
            case R.id.tvMapType: // 切换地图类型
                switch (tvMapType.getText().toString()){
                    // 空白地图, 基础地图瓦片将不会被渲染。在地图类型中设置为NONE，将不会使用流量下载基础地图瓦片图层。
                    // 使用场景：与瓦片图层一起使用，节省流量，提升自定义瓦片图下载速度。
                    case "交通": // 普通地图切换到交通图
                        //baiduMap.setMapType(BaiduMap.MAP_TYPE_NONE);
                        baiduMap.setTrafficEnabled(true); // 交通图
                        showToast("当前是交通图");
                        tvMapType.setText("卫星");
                        break;
                    case "卫星": // 交通地图切换到卫星图
                        baiduMap.setTrafficEnabled(false);
                        baiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
                        showToast("当前是卫星图");
                        tvMapType.setText("地图");
                        break;
                    case "地图": // 卫星地图切换到普通地图
                        baiduMap.setTrafficEnabled(false);
                        baiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
                        showToast("当前基础图");
                        tvMapType.setText("交通");
                        break;
                }
                break;
        }
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
    }
}
