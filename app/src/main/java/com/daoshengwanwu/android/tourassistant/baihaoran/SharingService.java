package com.daoshengwanwu.android.tourassistant.baihaoran;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationListener;
import com.daoshengwanwu.android.tourassistant.baihaoran.AppUtil.SharingServer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;


public class SharingService extends Service {
    private double mLatitude = 0.0;
    private double mLongitude = 0.0;
    private SharingBinder mUniqueBinder = null;
    private AMapLocationClient mLocationClient = null;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if (null == mUniqueBinder) {
            mUniqueBinder = new SharingBinder();
        }

        return mUniqueBinder;
    }

    @Override
    public void onDestroy() {
        if (null == mUniqueBinder) {
            super.onDestroy();
            return;
        }

        if (null != mLocationClient) {
            if (mUniqueBinder.isStartLocation()) {
                mLocationClient.stopLocation();
            }

            mLocationClient.onDestroy();
            mLocationClient = null;
        }


        if (mUniqueBinder.isUploadStart()) {
            mUniqueBinder.stopUploadLocation();
        }

        mUniqueBinder = null;
        super.onDestroy();
    }

    public static Intent newIntent(Context applicationContext) {
        return new Intent(applicationContext, SharingService.class);
    }

    public static void actionStartService(Context applicationContext) {
        applicationContext.startService(new Intent(applicationContext, SharingService.class));
    }

    public static boolean actionBindService(Context packageContext, ServiceConnection serviceConnection) {
        return packageContext.bindService(newIntent(packageContext), serviceConnection, BIND_AUTO_CREATE);
    }


    public interface SharingLocationListener {
        void onLocationsDataArrived(HashMap<String, double[]> locationsData);
    }


    public final class SharingBinder extends Binder {
        private boolean mIsUploadStarting = false; //标识当前是否正在开启上传服务
        private boolean mIsStartingLocation = false; //标识当前是否正在开启定位服务
        private boolean mIsUploadStart = false; //标识当前是否已经开启上传服务
        private boolean mIsStartLocation = false; //标识当前是否已经开启定位服务
        private Socket mClient = null; //连接服务器的套接字
        private boolean mNeedLoop = false; //标识上传位置线程以及读取指令两个线程是否需要继续循环。
        private BufferedReader mInfoReader = null; //接收服务器端发来的指令
        private BufferedWriter mInfoReporter = null; //向服务器发送指令
        //记录着每个成员的位置信息，key为user的user_id, value是一个double数组，其中double[0]位latitude， double[1]为longitude
        private HashMap<String, double[]> mGroupMemberLocations = new HashMap<>();
        //保存着所有注册到该服务的监听者
        private Set<AMapLocationListener> mLocationListeners = new HashSet<>();
        private Set<SharingLocationListener> mSharingLocationListeners = new HashSet<>();


        public boolean isStartingLocation() {
            return mIsStartingLocation;
        }

        public boolean isUploadStarting() {
            return mIsUploadStarting;
        }

        public boolean isStartLocation() {
            return mIsStartLocation;
        }

        public boolean isUploadStart() {
            return mIsUploadStart;
        }

        public void registerLocationListener(AMapLocationListener locationListener) {
            mLocationListeners.add(locationListener);
        }

        public void unregisterLocationListener(AMapLocationListener locationListener) {
            mLocationListeners.remove(locationListener);
        }

        public void registerSharingLocationListener(SharingLocationListener listener) {
            mSharingLocationListeners.add(listener);
        }

        public void unregisterSharginLocationListener(SharingLocationListener listener) {
            mSharingLocationListeners.remove(listener);
        }

        public void startLocationService() {
            mIsStartingLocation = true;
            //获取定位客户端以及开启定位服务
            if (null == mLocationClient) {
                mLocationClient = new AMapLocationClient(getApplicationContext()); //首先实例化一个定位客户端（由高德SDK提供）
                mLocationClient.setLocationListener(new AMapLocationListener() { //设置位置改变时的回调方法
                    @Override
                    public void onLocationChanged(AMapLocation aMapLocation) { //这么做的好处是对于位置改变的回调定制可以写在主调对象中
                        mLatitude = aMapLocation.getLatitude();
                        mLongitude = aMapLocation.getLongitude();
                        //调用每个注册的监听者的onLocationChanged方法
                        for (AMapLocationListener locationListener : mLocationListeners) {
                            locationListener.onLocationChanged(aMapLocation);
                        }
                    }
                });
                mLocationClient.startLocation();
                mIsStartLocation = true;
            }
        }

        public void stopLocationService() {
            if (null == mLocationClient || isUploadStarting() || isUploadStart()) {
                return;
            }

            mLocationClient.stopLocation();
            mLocationClient = null;
            mIsStartingLocation = false;
            mIsStartLocation = false;
        }

        public void stopUploadLocation() {
            if (mIsUploadStart && null != mClient) {
                mNeedLoop = false; //将循环标志置为false
            }
        }

        public void startUploadLocation() throws IOException { //开启上传位置服务
            if (!mIsUploadStarting && !mIsUploadStart && isStartLocation()) {
                mIsUploadStarting = true;
                //在新线程中实例化与ServerSocket服务器通讯的客户端Socket
                new Thread() {
                    @Override
                    public void run() {
                        super.run();
                        try {
                            instanceSocket(); //实例化Socket对象
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        /*
                            在这里应该有这样一个步骤：
                                获取当前登录的唯一标识用户的user_id
                            由于登录功能还没有实现，所以这里先假定一个user_id
                         */
                        final String user_id = AppUtil.User.USER_ID; //假定一个用户id

                        //向服务器发送user_id信息,该步骤为必须步骤，服务器必须接收到该唯一id才可将Socket与id绑定
                        try {
                            sendCommandToServer(SharingServer.COMMAND_SET_USERID, user_id);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        //开启一个新线程，用来监听服务端发来的消息
                        mNeedLoop = true; //首先要将循环标志置为true

                        //开启一个接收ServerSocket服务器指令的线程，该线程只有在接收到服务器结束指令时才会停止.
                        new Thread() {
                            @Override
                            public void run() {
                                super.run();
                                try {
                                    while (true) {
                                        String info = getInfoFromServer(); //从服务器读取一条指令
                                        switch (getInfoCommand(info)) {
                                            case SharingServer.RECEIVED_MEMBER_LOCATIONS:
                                                String content = getInfoContent(info);
                                                analysisLocationData(content);
                                                for (SharingLocationListener listener : mSharingLocationListeners) {
                                                    listener.onLocationsDataArrived(mGroupMemberLocations);
                                                }
                                                break;
                                            case SharingServer.REQUEST_STOP:
                                                mClient.close();
                                                mClient = null;
                                                mGroupMemberLocations.clear();
                                                mInfoReader = null;
                                                mInfoReporter = null;
                                                mIsUploadStart = false;
                                                mIsUploadStarting = false;
                                                return;
                                            default:
                                                break;
                                        }
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }.start();

                        //开启一个线程，每秒向服务器上传自己的位置，并且请求一次组内成员的位置
                        new Thread() {
                            @Override
                            public void run() {
                                super.run();
                                try {
                                    while (mNeedLoop) {
                                        //向服务器上传自己的位置
                                        sendCommandToServer(SharingServer.COMMAND_SET_LOCATION, getCurrentLocationString());
                                        //向服务器请求组内成员的位置
                                        sendRequestToServer(SharingServer.REQUEST_MEMBER_LOCATION);

                                        Thread.sleep(2000);
                                    }

                                    //当循环跳出时说明mNeedLoop已经置为false，此时说明外部已经调用stopSharingLocation()方法
                                    //所以向服务发送结束请求
                                    sendRequestToServer(SharingServer.REQUEST_STOP);
                                } catch (IOException | InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }.start();
                    }
                }.start();

                mIsUploadStart = true;
            }
        }

        private String getCurrentLocationString() {
            return "" + mLatitude + "," + mLongitude;
        }

        private void clearLocationListeners() {
            mLocationListeners.clear();
        }

        private void clearSharingLocationListeners() {
            mSharingLocationListeners.clear();
        }

        private void closeStream() throws IOException {
            mInfoReporter.close();
            mInfoReader.close();
            mInfoReporter = null;
            mInfoReader = null;
        }

        private void sendCommandToServer(String commandCode, String info) throws IOException { //向服务器发送一条信息
            info = commandCode + ":" + info + "\n";
            mInfoReporter.write(info);
            mInfoReporter.flush();
        }

        private void sendRequestToServer(String requestCode) throws IOException {
            mInfoReporter.write(requestCode + "\n");
            mInfoReporter.flush();
        }

        private String getInfoFromServer() throws IOException { //从服务器获取一条信息
            return mInfoReader.readLine();
        }

        private void instanceSocket() throws IOException { //实例化Socket对象
            mClient = new Socket(SharingServer.HOST, SharingServer.PORT);
            mInfoReader = new BufferedReader(new InputStreamReader(mClient.getInputStream()));
            mInfoReporter = new BufferedWriter(new OutputStreamWriter(mClient.getOutputStream()));
        }

        private String getInfoCommand(String info) {
            return info.split(":")[0];
        }

        private String getInfoContent(String info) {
            return info.split(":")[1];
        }

        /**
         * 功能：解析位置信息
         * @param locationData
         *      locationData: 记录着定位信息的字符串，格式："user_id->latitude,longitude#user_id->..."
         *
         * 执行结果：填充私有成员：mGroupMemberLocations
         */
        private void analysisLocationData(String locationData) {
            //首先把已经记录的信息清除掉（因为可能已经有其他用户与服务器断开连接了，如果不清除数据，可能会继续在地图上显示出已经离线的用户
            mGroupMemberLocations.clear();

            String[] membersLocation = locationData.split(SharingServer.SEPARATOR_LOCATION_DIFFER_MEMBER);
            for (String memberLoc : membersLocation) {
                String[] userId_Location = memberLoc.split(SharingServer.SEPARATOR_LOCATION_ID_LOC);
                String[] lat_lng_str = userId_Location[1].split(SharingServer.SEPARATOR_LOCATION_LAT_LON);

                String user_id = userId_Location[0];
                double latitude = Double.parseDouble(lat_lng_str[0]);
                double longitude = Double.parseDouble(lat_lng_str[1]);
                double[] lat_lng = {latitude, longitude};

                mGroupMemberLocations.put(user_id, lat_lng);
            }
        }
    }
}