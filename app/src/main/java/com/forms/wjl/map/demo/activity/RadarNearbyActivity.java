package com.forms.wjl.map.demo.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
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
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.radar.RadarNearbyInfo;
import com.baidu.mapapi.radar.RadarNearbyResult;
import com.baidu.mapapi.radar.RadarNearbySearchOption;
import com.baidu.mapapi.radar.RadarSearchError;
import com.baidu.mapapi.radar.RadarSearchListener;
import com.baidu.mapapi.radar.RadarSearchManager;
import com.baidu.mapapi.radar.RadarUploadInfo;
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
import java.util.List;
import java.util.Map;

public class RadarNearbyActivity extends BaseActivity implements OnGetGeoCoderResultListener, RadarSearchListener {

    private static final String TAG = "RadarNearbyActivity";
    private MapView mapView;
    private BaiduMap baiduMap;
    private RadarSearchManager radarSearchManager;
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
    private ImageView ivLocal; // 定位按钮
    private boolean isRequest = false;
    private Toast toast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_radar_nearby_map);
        initViews();
        initData();
        initListener();
    }

    @Override
    protected void initViews() {
        myOrientationListener = new MyOrientationListener(this);
        ivLocal = (ImageView) findViewById(R.id.ivLocal);
        mapView = (MapView) findViewById(R.id.mv_map);
        radarSearchManager = RadarSearchManager.getInstance();
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
        radarSearchManager.addNearbyInfoListener(this);
        geocoder.setOnGetGeoCodeResultListener(this);
        ivLocal.setOnClickListener(this);
        baiduMap.setOnMapClickListener(onMapClickListener); // 地图单击事件
        baiduMap.setOnMapLongClickListener(onMapLongClickListener); // 地图长按事件
        baiduMap.setOnMarkerClickListener(onMarkerClickListener); //
        myOrientationListener.setmOnOrientationListener(new MyOrientationListener.OnOrientationListener() {
            @Override
            public void onOrientationChanged(float x) {
                mCurrentX = x;
            }
        });
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
     * @param latLng 地理坐标
     * @param offset y 轴偏移量
     */
    private void createInfoWindow(View popView, LatLng latLng, int offset) {
        baiduMap.hideInfoWindow();
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
        Marker marker = (Marker) baiduMap.addOverlay(option); // 在地图上添加Marker，并显示
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

    /**
     * 位置信息上传
     *
     * @param pt
     */
    private void radarUploadInfo(LatLng pt) {
        // 周边雷达设置用户身份标识，id为空默认是设备标识
        radarSearchManager.setUserID(null);
        // 上传位置
        RadarUploadInfo info = new RadarUploadInfo();
        info.comments = "用户备注信息";
        info.pt = pt;
        radarSearchManager.uploadInfoRequest(info);
    }

    /**
     * 发起周边查询
     *
     * @param pt
     */
    private void radarNearbySearchOption(LatLng pt) {
        // 构造请求参数，其中centerPt是自己的位置坐标
        RadarNearbySearchOption option = new RadarNearbySearchOption().centerPt(pt).pageNum(0).radius(2000);
        // 发起查询请求
        radarSearchManager.nearbyInfoRequest(option);
    }

    @Override
    public void onGetNearbyInfoList(RadarNearbyResult radarNearbyResult, RadarSearchError radarSearchError) {
        if (radarSearchError == RadarSearchError.RADAR_NO_ERROR) {
            // 获取成功，处理数据
            Log.d(TAG, "onGetNearbyInfoList: 查询周边成功");
            Toast.makeText(this, "查询周边成功", Toast.LENGTH_SHORT).show();
            List<RadarNearbyInfo> infoList = radarNearbyResult.infoList;
            for (RadarNearbyInfo info : infoList) {
                Log.d(TAG, "onGetNearbyInfoList: pt--> " + info.pt.toString());
                Marker marker = createMarker(info.pt, R.mipmap.ic_localtion, false);
                marker.setTitle(info.comments);
            }
        } else {
            // 获取失败
            Toast.makeText(this, "查询周边失败", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "onGetNearbyInfoList: radarSearchError-->" + radarSearchError.toString());
        }
    }

    @Override
    public void onGetUploadState(RadarSearchError radarSearchError) {
        if (radarSearchError == RadarSearchError.RADAR_NO_ERROR) {
            // 上传成功
            Log.d(TAG, "onGetUploadState: 单次上传位置成功");
            Toast.makeText(this, "单次上传位置成功", Toast.LENGTH_SHORT).show();
        } else {
            // 上传失败
            Toast.makeText(this, "单次上传位置失败", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "onGetUploadState: 单次上传位置失败");
        }
    }

    @Override
    public void onGetClearInfoState(RadarSearchError radarSearchError) {
        if (radarSearchError == RadarSearchError.RADAR_NO_ERROR) {
            // 清除成功
            Log.d(TAG, "onGetClearInfoState: 清除位置成功");
            Toast.makeText(this, "清除位置成功", Toast.LENGTH_SHORT).show();
        } else {
            // 清除失败
            Toast.makeText(this, "清除位置失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 定位监听
     */
    private class MyLocationListener implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            if (bdLocation != null) {
                String city = bdLocation.getCity();
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
                    Log.d(TAG, "onReceiveLocation: " + city);
                    point = new LatLng(bdLocation.getLatitude(), bdLocation.getLongitude()); // 获取定位经纬度
                    button.setText(city);
                    baiduMap.clear();
                    createInfoWindow(button, point, -32);
                    MapStatusUpdate update = MapStatusUpdateFactory.newLatLngZoom(point, baiduMap.getMaxZoomLevel() - 4);
                    baiduMap.animateMapStatus(update);
                    isRequest = false;
                    radarUploadInfo(point); // 定位成功就上传位置信息
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
            Log.d(TAG, "onMarkerClick: 被点击了一下-->" + marker.getTitle());
            Log.d(TAG, "onMarkerClick: 被点击了一下-->" + marker.getPosition());
            btnPop.setText(marker.getTitle());
            createInfoWindow(btnPop,marker.getPosition(),-64);
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
            setReverseGeoCodeListener(latLng); // 设置地理反编码监听，根据经纬度获取地理位置信息等
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

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ivLocal: // 手动定位
                requestLocation(); // 请求定位
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
                    radarSearchManager.clearUserInfo(); // 清空用户信息
                    radarUploadInfo(latLng); // 雷达位置信息上传
                    radarNearbySearchOption(latLng); // 我的雷达周边检索
                    baiduMap.clear();
                    View popView = LayoutInflater.from(RadarNearbyActivity.this).inflate(R.layout.layout_pop_window, null);
                    TextView tvAddress = (TextView) popView.findViewById(R.id.tvAddress);
                    tvAddress.setText(address); // 点击地图的时候弹出的泡泡显示地址信息
                    Marker marker = createMarker(latLng, R.mipmap.ic_localtion, false);// 构建Marker图标
                    marker.setTitle(address);
                    createInfoWindow(popView, latLng, -64); // 创建InfoWindow
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
        // 移除监听
        radarSearchManager.removeNearbyInfoListener(this);
        // 清除用户信息
        radarSearchManager.clearUserInfo();
        // 释放资源
        radarSearchManager.destroy();
        radarSearchManager = null;
        //关闭定位
        baiduMap.setMyLocationEnabled(false);
        locationClient.stop();
        //停止方向传感器
        myOrientationListener.stop();
        locationClient.unRegisterLocationListener(myLocationListener);
    }

}
