package com.example.uuuup.myapplication;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.AMapOptions;
import com.amap.api.maps2d.AMapUtils;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.LocationSource;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.UiSettings;
import com.amap.api.maps2d.model.BitmapDescriptor;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;
import com.example.uuuup.myapplication.base.BaseApplication;
import com.example.uuuup.myapplication.entity.Constant;
import com.example.uuuup.myapplication.utils.NavigationUtils;
import com.example.uuuup.myapplication.utils.PhoneCallUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static android.R.attr.description;

public class MainActivity extends BaseActivity implements LocationSource, AMapLocationListener, PoiSearch.OnPoiSearchListener, AMap.OnMarkerClickListener, AMap.OnMapClickListener, AMap.InfoWindowAdapter{

    private MapView mMapView;//地图容器
    private boolean isFirstLoc = true;//标识，用于判断是否只显示一次定位信息和用户重新定位

    public AMapLocationClient mLocationClient = null;//声明AMapLocationClient类对象
    public AMapLocationClientOption mLocationOption = null;//声明AMapLocationClientOption对象，实际是关于定位的参数

    private double lat = 39.9088691069;//经纬度 默认为天安门39.9088691069,116.3973823161
    private double lon = 116.3973823161;

    private AMap aMap;//地图类
    private float zoomlevel = 17f; //地图放大级别

    private OnLocationChangedListener mListener = null;//声明mListener对象，定位监听器
    private android.widget.TextView tvLocation;
    private Marker oldMarker;
    private LatLng myLatLng;
    private AMap.InfoWindowAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button forceOffline = (Button) findViewById(R.id.force_offline);
        forceOffline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent("com.example.broadcastbestpractice.FORCE_OFFLINE");
                sendBroadcast(intent);
            }
        });

        this.tvLocation = (TextView) findViewById(R.id.tvLocation);
        //this.map = (MapView) findViewById(R.id.map);
        // /获取地图控件引用
        mMapView = (MapView) findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);//在activity执行onCreate时执行mMapView.onCreate(savedInstanceState)，实现地图生命周期管理

        //自定义的回到当前位置的事件
        tvLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startLocation();
            }
        });

        if (aMap == null) {
            aMap =  mMapView.getMap();
            aMap.setLocationSource(this);//设置了定位的监听,这里要实现LocationSource接口
            aMap.setMyLocationEnabled(true);//显示定位层并且可以触发定位,默认是flase

            UiSettings settings =  aMap.getUiSettings();//设置显示定位按钮 并且可以点击
            settings.setMyLocationButtonEnabled(false);// 是否显示定位按钮

            settings.setZoomControlsEnabled(true);//管理缩放控件
            settings.setLogoPosition(AMapOptions.LOGO_POSITION_BOTTOM_LEFT);//设置logo位置，左下，底部居中，右下
            settings.setScaleControlsEnabled(true);//设置显示地图的默认比例尺


            //添加指南针
            aMap.getCameraPosition(); //方法可以获取地图的旋转角度
            settings.setCompassEnabled(true);

            aMap.setOnMarkerClickListener(this);

            //每像素代表几米
            //float scale = aMap.getScalePerPixel();
        }
        //开始定位
        location();
    }

    private void location() {
        mLocationClient = new AMapLocationClient(getApplicationContext());//初始化定位
        mLocationClient.setLocationListener(this);//设置定位回调监听

        mLocationOption = new AMapLocationClientOption();//初始化定位参数
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);//设置定位模式为Hight_Accuracy高精度模式，Battery_Saving为低功耗模式，Device_Sensors是仅设备模式

        mLocationOption.setNeedAddress(true);//设置是否返回地址信息（默认返回地址信息）
        mLocationOption.setOnceLocation(false);//设置是否只定位一次,默认为false
        mLocationOption.setWifiActiveScan(true);//设置是否强制刷新WIFI，默认为强制刷新
        mLocationOption.setMockEnable(true);//设置是否允许模拟位置,默认为false，不允许模拟位置
        mLocationOption.setInterval(2000);//设置定位间隔,单位毫秒,默认为2000ms

        mLocationClient.setLocationOption(mLocationOption);//给定位客户端对象设置定位参数
        mLocationClient.startLocation();//启动定位
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mMapView.onDestroy();
        mLocationClient.stopLocation();//停止定位
        mLocationClient.onDestroy();//销毁定位客户端
    }

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView.onResume ()，实现地图生命周期管理
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView.onPause ()，实现地图生命周期管理
        mMapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //在activity执行onSaveInstanceState时执行mMapView.onSaveInstanceState (outState)，实现地图生命周期管理
        mMapView.onSaveInstanceState(outState);
    }

    /**
     * 定位成功后回调函数
     */
    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (aMapLocation != null) {
            if (aMapLocation.getErrorCode() == 0) {
                //可在其中解析amapLocation获取相应内容。
                aMapLocation.getLocationType();//获取当前定位结果来源，如网络定位结果，详见定位类型表
                lat = aMapLocation.getLatitude();//获取纬度
                lon = aMapLocation.getLongitude();//获取经度
                aMapLocation.getAccuracy();//获取精度信息
                aMapLocation.getAddress();//地址，如果option中设置isNeedAddress为false，则没有此结果，网络定位结果中会有地址信息，GPS定位不返回地址信息。
                aMapLocation.getCountry();//国家信息
                aMapLocation.getProvince();//省信息
                aMapLocation.getCity();//城市信息
                aMapLocation.getDistrict();//城区信息
                aMapLocation.getStreet();//街道信息
                aMapLocation.getStreetNum();//街道门牌号信息
                aMapLocation.getCityCode();//城市编码
                aMapLocation.getAdCode();//地区编码
                aMapLocation.getAoiName();//获取当前定位点的AOI信息
                myLatLng = new LatLng( lat, lon);

                //获取定位时间
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date date = new Date(aMapLocation.getTime());
                df.format(date);//定位时间

                poiSearch( lat, lon);//传入此时定位的经纬度，进行搜索公交站点

                // 如果不设置标志位，此时再拖动地图时，它会不断将地图移动到当前的位置
                if (isFirstLoc) {
                    aMap.moveCamera(CameraUpdateFactory.zoomTo(zoomlevel));//设置缩放级别
                    aMap.moveCamera(CameraUpdateFactory.changeLatLng(new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude())));//将地图移动到定位点
                    mListener.onLocationChanged(aMapLocation);//点击定位按钮 能够将地图的中心移动到定位点

                    StringBuffer buffer = new StringBuffer();//获取定位信息
                    buffer.append(aMapLocation.getCountry() + ""
                            + aMapLocation.getProvince() + ""
                            + aMapLocation.getCity() + ""
                            + aMapLocation.getProvince() + ""
                            + aMapLocation.getDistrict() + ""
                            + aMapLocation.getStreet() + ""
                            + aMapLocation.getStreetNum());
                    Toast.makeText(getApplicationContext(), buffer.toString(), Toast.LENGTH_LONG).show();
                    isFirstLoc = false;
                }

            }else {
                //定位失败时，可通过ErrCode（错误码）信息来确定失败的原因，errInfo是错误信息，详见错误码表。
                Log.e("地图错误","定位失败, 错误码:" + aMapLocation.getErrorCode() + ", 错误信息:"
                        + aMapLocation.getErrorInfo());
            }
        }
    }

    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        mListener = onLocationChangedListener;
    }

    @Override
    public void deactivate() {
        mListener = null;
    }

    private void startLocation(){
        if(mLocationClient != null){
            aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat,lon),zoomlevel));//17f代表地图放大的级别
        }
    }


    public void poiSearch( double lat, double lon){
        PoiSearch.Query query = new PoiSearch.Query( "公交站点", "" , "");//"150702"为公交站点的poi
        query.setPageSize(20);
        PoiSearch search = new PoiSearch(this, query);
        search.setBound(new PoiSearch.SearchBound(new LatLonPoint( 45.7784237183, 126.6177728296), 10000));//哈尔滨的经纬度是45.7784237183, 126.6177728296
        search.setOnPoiSearchListener(this);
        search.searchPOIAsyn();
        //query设置的范围“哈尔滨”需要跟setBound的范围一致, query的第三个参数不设置也可以, 跟设置成“哈尔滨”一致
    }
    @Override
    public void onPoiSearched(PoiResult poiResult, int i) {
        ArrayList<PoiItem> pois = poiResult.getPois();
        for (PoiItem poi : pois) {
            //获取经纬度对象
            LatLonPoint llp = poi.getLatLonPoint();
            double latOfPoi = llp.getLatitude();
            double lonOfPoi = llp.getLongitude();
            //获取标题
            String title = poi.getTitle();
            //获取内容
            String snippet = poi.getSnippet();
            LatLng latLng = new LatLng( latOfPoi, lonOfPoi);

            final Marker marker = aMap.addMarker(new MarkerOptions().anchor(0.5f, 0.5f).position(latLng).title(title).snippet(snippet).icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_normal)));
            aMap.setOnMarkerClickListener(this);

            System.out.println(lon+"~~~"+lat+"~~~"+title+"~~~"+snippet);
        }
        System.out.println("直接打印这个信息，代表没有搜索到信息");
    }

    @Override
    public void onPoiItemSearched(PoiItem poiItem, int i) {
    }

    //maker的点击事件
    //地图的点击事件
    @Override
    public void onMapClick(LatLng latLng) {
        //点击地图上没marker 的地方，隐藏inforwindow
        if (oldMarker != null) {
            oldMarker.hideInfoWindow();
            oldMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.marker_normal));
        }
    }

    //maker的点击事件
    @Override
    public boolean onMarkerClick(Marker marker) {
        if (!marker.getPosition().equals(myLatLng)){ //点击的marker不是自己位置的那个marker
            if (oldMarker != null) {
                oldMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.marker_normal));
            }
            oldMarker = marker;
            marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.marker_selected));
        }
        marker.showInfoWindow();
        return false; //返回 “false”，除定义的操作之外，默认操作也将会被执行
    }

    @Override
    public View getInfoWindow(Marker marker) {
        aMap.setInfoWindowAdapter(adapter);
        Context mContext = BaseApplication.getIntance().getBaseContext();
        LatLng latLng;
        LinearLayout call;
        LinearLayout navigation;
        TextView nameTV;
        String Name;
        TextView addrTV;
        String snippet;
        latLng = marker.getPosition();
        snippet = marker.getSnippet();
        Name = marker.getTitle();
        snippet = marker.getSnippet();

        View view = LayoutInflater.from(mContext).inflate(R.layout.view_infowindow, null);
        navigation = (LinearLayout) view.findViewById(R.id.navigation_LL);
        call = (LinearLayout) view.findViewById(R.id.call_LL);
        nameTV = (TextView) view.findViewById(R.id.agent_name);
        addrTV = (TextView) view.findViewById(R.id.agent_addr);

        nameTV.setText(Name);
        addrTV.setText(snippet);
        //navigation.setOnClickListener(this);
        //call.setOnClickListener(this);
        return view;
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }
}