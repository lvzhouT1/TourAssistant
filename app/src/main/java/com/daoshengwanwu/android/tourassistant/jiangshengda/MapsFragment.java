package com.daoshengwanwu.android.tourassistant.jiangshengda;


import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.Point;
import android.util.Log;
import android.widget.Button;
import android.widget.FrameLayout;

import com.amap.api.location.AMapLocation;//定位信息类
import com.amap.api.location.AMapLocationClient;//定位服务类
import com.amap.api.location.AMapLocationClientOption;//定位参数设置（在定位服务时需要这些参数）
import com.amap.api.location.AMapLocationListener;//定位回调接口
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.Projection;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.LatLng;

import com.daoshengwanwu.android.tourassistant.R;
import com.daoshengwanwu.android.tourassistant.baihaoran.SharingService;

import java.util.HashMap;


public class MapsFragment extends Fragment implements AMapLocationListener, SharingService.SharingLocationListener{
    //------------------------------胜达-------------------------------
    private int i = 0;
    private MyView myView;
    private double x;
    private double y;
    private float dot_x = 0;
    private float dot_y = 0;
    private FrameLayout act_main;
    private Button btn;
    private AMap aMap;
    private MapView mapView;
    private LocationSource.OnLocationChangedListener mListener;
    private AMapLocationClient mlocationClient;
    private AMapLocationClientOption mLocationOption;
    //--------------------------------------------------------------------

    //------------------------------浩然-----------------------------------
    private SharingService.SharingBinder mSharingBinder;
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mSharingBinder = (SharingService.SharingBinder)service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mSharingBinder.stopUploadLocation();
            mSharingBinder.stopLocationService();
            mSharingBinder.unregisterLocationListener(MapsFragment.this);
            mSharingBinder.unregisterSharginLocationListener(MapsFragment.this);
            mSharingBinder = null;
        }
    };
    //----------------------------------------------------------------------


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.jiangshengda_fragment_maps, container, false);

        btn = (Button) v.findViewById(R.id.Fog_btn);
        act_main = (FrameLayout)v.findViewById(R.id.fragment_maps);
        mapView = (MapView) v.findViewById(R.id.map);
        if (aMap == null) {
            aMap = mapView.getMap();
            setUpMap();
        }

        //在activity执行onCreate时执行mMapView.onCreate(savedInstanceState)，实现地图生命周期管理
        mapView.onCreate(savedInstanceState);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Fog
                myView = new MyView(getActivity().getApplicationContext());
                act_main.addView(myView);
                i = 0;//初始化计数器
                aMap.getUiSettings().setAllGesturesEnabled(false);//禁止所有手势操作
            }
        });

        removeAMapLogo(); //删除高德logo
        //设置使用普通地图
        //aMap.setMapType(AMap.MAP_TYPE_NIGHT);//夜景地图模式
        //aMap.setMapType(AMap.MAP_TYPE_NORMAL);

        SharingService.actionStartService(getActivity());
        SharingService.actionBindService(getActivity(), mServiceConnection);

        return v;
    }

    /*
    设置一些amap的属性
     */
    private void setUpMap() {
        aMap.getUiSettings().setRotateGesturesEnabled(false);//禁止地图旋转手势
        aMap.getUiSettings().setTiltGesturesEnabled(false);//禁止倾斜手势
        //aMap.setLocationSource(this);// 设置定位监听
        aMap.getUiSettings().setMyLocationButtonEnabled(false);// 设置默认定位按钮是否显示
        aMap.getUiSettings().setScaleControlsEnabled(true);//显示比例尺控件
        aMap.moveCamera(CameraUpdateFactory.zoomTo(15));//设置比例尺，3-19
        aMap.setMyLocationEnabled(true);// 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
        // 设置定位的类型为定位模式 ，可以由定位、跟随或地图根据面向方向旋转几种
        //aMap.setMyLocationType(AMap.LOCATION_TYPE_MAP_FOLLOW);//跟随模式
       // aMap.setMyLocationType(AMap.LOCATION_TYPE_LOCATE); //定位模式
        //aMap.setMyLocationType(AMap.LOCATION_TYPE_MAP_ROTATE); // 设置定位的类型为根据地图面向方向旋转
    }

    private void removeAMapLogo() {
        //这两行代码可以隐藏高德地图logo
        UiSettings uiSettings =  aMap.getUiSettings();
        uiSettings.setLogoBottomMargin(-50);//隐藏logo
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        if (null != mlocationClient) {
            mlocationClient.onDestroy();
        }
    }



    @Override
    public void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView.onResume ()，实现地图生命周期管理
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView.onPause ()，实现地图生命周期管理
        mapView.onPause();
    }

    /*
    定位成功后回调函数
     */
    @Override
    public void onLocationChanged(final AMapLocation amapLocation) {
        if (mListener != null && amapLocation != null) {
            if (amapLocation != null && amapLocation.getErrorCode() == 0) {
                mListener.onLocationChanged(amapLocation);// 显示系统小蓝点
                x = amapLocation.getLatitude();//获取纬度
                y = amapLocation.getLongitude();//获取经度
                LatLng pos = new LatLng(x,y);
                Projection projection = aMap.getProjection();
                //将地图的点，转换为屏幕上的点 
                Point dot = projection.toScreenLocation(pos);
                dot_x = dot.x;
                dot_y = dot.y;
                if (i == 0){
                    myView.start_pot(dot_x,dot_y);
                } else{
                    myView.line(dot_x, dot_y);
                }
                i++;
                //amapLocation.getLocationType();//获取当前定位结果来源，如网络定位结果，详见定位类型表
                //amapLocation.getLatitude();//获取纬度
                //amapLocation.getLongitude();//获取经度
                //amapLocation.getAccuracy();//获取精度信息
                //amapLocation.getAddress();//地址，如果option中设置isNeedAddress为false，则没有此结果，网络定位结果中会有地址信息，GPS定位不返回地址信息。
                //amapLocation.getCountry();//国家信息
                //amapLocation.getProvince();//省信息
                //amapLocation.getCity();//城市信息
                //amapLocation.getDistrict();//城区信息
                //amapLocation.getStreet();//街道信息
                //amapLocation.getStreetNum();//街道门牌号信息
                //amapLocation.getCityCode();//城市编码
                //amapLocation.getAdCode();//地区编码
                //amapLocation.getAoiName();//获取当前定位点的AOI信息
                //amapLocation.getGpsStatus();//获取GPS的当前状态

            } else {
                String errText = "定位失败," + amapLocation.getErrorCode() + ": " + amapLocation.getErrorInfo();
                Log.e("AmapErr", errText);
            }
        }
    }

    /*
    激活定位
     */
    /*
    @Override
    public void activate(LocationSource.OnLocationChangedListener listener) {
        mListener = listener;
        if (mlocationClient == null) {
            mlocationClient = new AMapLocationClient(getActivity());
            mLocationOption = new AMapLocationClientOption();
            //设置定位监听
            mlocationClient.setLocationListener(this);
            //设置为高精度定位模式
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            //设置定位参数
            mlocationClient.setLocationOption(mLocationOption);
            // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
            // 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用stopLocation()方法来取消定位请求
            // 在定位结束后，在合适的生命周期调用onDestroy()方法
            // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
            mlocationClient.startLocation();
        }
    }
*/
    /*
    停止定位
     */
    /*
    @Override
    public void deactivate() {
        mListener = null;
        if (mlocationClient != null) {
            mlocationClient.stopLocation();
            mlocationClient.onDestroy();
        }
        mlocationClient = null;
    }
    */

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //在activity执行onSaveInstanceState时执行mMapView.onSaveInstanceState (outState)，实现地图生命周期管理
        mapView.onSaveInstanceState(outState);
    }


    public static MapsFragment newInstance() {
        return new MapsFragment();
    }

    @Override
    public void onLocationsDataArrived(HashMap<String, double[]> locationsData) {

    }
}