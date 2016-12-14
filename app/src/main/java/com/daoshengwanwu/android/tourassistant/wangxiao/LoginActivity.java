package com.daoshengwanwu.android.tourassistant.wangxiao;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.daoshengwanwu.android.tourassistant.R;
import com.daoshengwanwu.android.tourassistant.baihaoran.LauncherActivity;
import com.daoshengwanwu.android.tourassistant.wangxiao.util.Util1;
import com.daoshengwanwu.android.tourassistant.wangxiao.utils.HttpCallBackListener;
import com.daoshengwanwu.android.tourassistant.wangxiao.utils.HttpUtil;
import com.daoshengwanwu.android.tourassistant.wangxiao.utils.PrefParams;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.sina.weibo.sdk.auth.AuthInfo;
import com.sina.weibo.sdk.auth.Oauth2AccessToken;
import com.sina.weibo.sdk.auth.WeiboAuthListener;
import com.sina.weibo.sdk.auth.sso.SsoHandler;
import com.sina.weibo.sdk.exception.WeiboException;
import com.sina.weibo.sdk.net.RequestListener;
import com.sina.weibo.sdk.openapi.UsersAPI;
import com.sina.weibo.sdk.openapi.models.User;
import com.tencent.connect.UserInfo;
import com.tencent.connect.auth.QQAuth;
import com.tencent.mm.sdk.modelmsg.SendAuth;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.WXAPIFactory;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;
import com.tencent.tauth.UiError;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;


public class LoginActivity extends Activity implements OnClickListener{
    private AuthInfo mAuthInfo;
    private ImageView btnweibo;

    /** 封装了 "access_token"，"expires_in"，"refresh_token"，并提供了他们的管理功能 */
    private Oauth2AccessToken mAccessToken;

    /** 注意：SsoHandler 仅当 SDK 支持 SSO 时有效 */
    private SsoHandler mSsoHandler;
    private UsersAPI mUsersAPI;
    private Button bt;
    private String qqname, qqid,qqgender;
    private SharedPreferences s,s1;
    private String name1;
    private String pwd1;
    private String user_id;
    private String user_name;
    private String user_pwd;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wx_login);
        initweibologin();
        initViews();
        initViews2();
        initViews3();
        initEvents();
        initEvents2();
        wechatlogin();
        s1 = getSharedPreferences("ty_user",Context.MODE_PRIVATE);
        name1 = s1.getString("name", "");
        pwd1 = s1.getString("pwd","");
        name.setText(name1);
        pwd.setText(pwd1);
    }
    Thread login = new Thread(){
        @Override
        public void run() {
            super.run();
            String result = "";
            PrintWriter out = null;
            BufferedReader in = null;
            try {

                //登录
                    URL url = new URL("http://192.168.191.1/user/login");
                    HttpURLConnection con = (HttpURLConnection)url.openConnection();
                    con.setDoInput(true);
                    con.setDoOutput(true);
                    out = new PrintWriter(con.getOutputStream());
                    out.print(user_name+"\n"+user_pwd);
                    out.flush();
                    //定义BufferedReader输入流读取URL响应
                    in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    String line;
                    while ((line = in.readLine()) != null){
                        result += "\n" +line;
                    }
                      user_id = result;
            }  catch (Exception e) {
                System.out.println("发送POST请求出现异常！" + e);
                e.printStackTrace();
            }  finally {
                try{
                    if (out != null){
                        out.close();
                    }
                    if (in != null){
                        in.close();
                    }
                }catch (IOException ex){
                    ex.printStackTrace();
                }
            }
        }
    };


    private void initViews() {
        bt = (Button)findViewById(R.id.lg_bt2);

        btnweibo = (ImageView) findViewById(R.id.lg_weibo);

    }

    private void initEvents() {
        btnweibo.setOnClickListener(this);
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

    }

    public static void actionStartActivity(Context context) {
        context.startActivity(new Intent(context, LoginActivity.class));
    }

    /**
     * 进行微博授权初始化操作
     */
    private void initweibologin() {
        // 初始化授权类对象，将应用的信息保存
        mAuthInfo = new AuthInfo(this, Constants.APP_KEY,
                Constants.REDIRECT_URL, Constants.SCOPE);
        mSsoHandler = new SsoHandler(LoginActivity.this, mAuthInfo);

    }

    /**
     * 当 SSO 授权 Activity 退出时，该函数被调用。
     *
     * @see {@link Activity#onActivityResult}
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // SSO 授权回调
        if (mSsoHandler != null) {
            mSsoHandler.authorizeCallBack(requestCode, resultCode, data);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.lg_weibo:// SSO 授权, ALL IN ONE
                // 如果手机安装了微博客户端则使用客户端授权,没有则进行网页授权
                mSsoHandler.authorize(new AuthListener());
                break;
            case R.id.lg_bt:
                user_name = name.getText().toString();
                user_pwd = pwd.getText().toString();
                login.start();
            default:
                break;


        }
    }

    /**
     * 微博认证授权回调类。
     */
    class AuthListener implements WeiboAuthListener {

        @Override
        public void onCancel() {
            Toast.makeText(LoginActivity.this, "取消授权", Toast.LENGTH_LONG)
                    .show();
        }

        @Override
        public void onComplete(Bundle values) {
            // 从 Bundle 中解析 Token
            mAccessToken = Oauth2AccessToken.parseAccessToken(values);
            if (mAccessToken.isSessionValid()) {
                // 显示 Token
                getUserInfo();
                // 保存 Token 到 SharedPreferences
                AccessTokenKeeper.writeAccessToken(LoginActivity.this,
                        mAccessToken);
                Toast.makeText(LoginActivity.this, "授权成功", Toast.LENGTH_SHORT)
                        .show();

            } else {
                String code = values.getString("code");
                String message = "授权失败";
                if (!TextUtils.isEmpty(code)) {
                    message = message + "\nObtained the code: " + code;
                }
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG)
                        .show();
            }
        }

        @Override
        public void onWeiboException(WeiboException e) {
//	e		Toast.makeText(LoginActivity.this,
//					"Auth exception : " + e.getMessage(), Toast.LENGTH_LONG)
//					.show();
        }

    }

    /**
     * 获取用户个人信息
     */
    private void getUserInfo() {
        //获取用户信息接口
        mUsersAPI = new UsersAPI(LoginActivity.this, Constants.APP_KEY, mAccessToken);
        System.out.println("mUsersAPI  ----->   " + mUsersAPI.toString());

        //调用接口
        long uid = Long.parseLong(mAccessToken.getUid());
        System.out.println("--------------uid-------------->    " + uid);
        mUsersAPI.show(uid, mListener);//将uid传递到listener中，通过uid在listener回调中接收到该用户的json格式的个人信息
    }
    /**
     * 实现异步请求接口回调，并在回调中直接解析User信息
     */
    private RequestListener mListener = new RequestListener() {
        @Override
        public void onComplete(String response) {

            if (!TextUtils.isEmpty(response)) {
                //调用User#parse将JSON串解析成User对象
                User user = User.parse(response);
                String gender;
                if(user.gender.equals("f")) {
                    gender = "女";
                } else {
                    gender = "男";
                }
                String name = user.screen_name;
                String id = user.id;
                new Userty(id,name,gender);

                Toast.makeText(LoginActivity.this, "用户id： " + id + "\n用户昵称： " + name + "\n用户性别： " + gender, Toast.LENGTH_SHORT).show();
            }
        }
        @Override
        public void onWeiboException(WeiboException e) {
            e.printStackTrace();
            Toast.makeText(LoginActivity.this, "获取用户个人信息 出现异常", Toast.LENGTH_SHORT).show();
        }
    };


    /**
     * QQ第三方登录
     */
    private static final String TAG = LoginActivity.class.getName();
    public static String mAppid;
    private ImageView mNewLoginButton;
    public static QQAuth mQQAuth;
    private UserInfo mInfo;
    private Tencent mTencent;
    private final String APP_ID = "1105835094";// 测试时使用，真正发布的时候要换成自己的APP_ID


    @Override
    protected void onStart() {
        Log.d(TAG, "-->onStart");
        final Context context = LoginActivity.this;
        final Context ctxContext = context.getApplicationContext();
        mAppid = APP_ID;
        mQQAuth = QQAuth.createInstance(mAppid, ctxContext);
        mTencent = Tencent.createInstance(mAppid, LoginActivity.this);
        super.onStart();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "-->onResume");
        super.onResume();
        receiveBroadCast = new ReceiveBroadCast();
        IntentFilter filter = new IntentFilter();
        filter.addAction("authlogin");
        LoginActivity.this.registerReceiver(receiveBroadCast, filter);
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "-->onPause");
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "-->onStop");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "-->onDestroy");
        super.onDestroy();
        LoginActivity.this.unregisterReceiver(receiveBroadCast);
    }

    private void initViews2() {
        mNewLoginButton = (ImageView) findViewById(R.id.lg_qq);
        OnClickListener listener = new NewClickListener();
        mNewLoginButton.setOnClickListener(listener);
        updateLoginButton();
    }

    private void updateLoginButton() {
        if (mQQAuth != null && mQQAuth.isSessionValid()) {
        } else {
        }
    }

    private void updateUserInfo() {
        if (mQQAuth != null && mQQAuth.isSessionValid()) {
            IUiListener listener = new IUiListener() {

                @Override
                public void onError(UiError e) {

                }
                @Override
                public void onComplete(final Object response) {
                    Message msg = new Message();
                    msg.obj = response;
                    msg.what = 0;
                    mHandler.sendMessage(msg);
                    new Thread() {

                        @Override
                        public void run() {
                            JSONObject json = (JSONObject) response;
                            if (json.has("figureurl")) {
                                Bitmap bitmap = null;
                                try {
                                    bitmap = Util1.getbitmap(json
                                            .getString("figureurl_qq_2"));
                                } catch (JSONException e) {

                                }
                                Message msg = new Message();
                                msg.obj = bitmap;
                                msg.what = 1;
                                mHandler.sendMessage(msg);
                            }
                        }

                    }.start();
                }

                @Override
                public void onCancel() {
                }
            };
            mInfo = new UserInfo(this, mQQAuth.getQQToken());
            mInfo.getUserInfo(listener);

        } else {
        }
    }

    Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                JSONObject response = (JSONObject) msg.obj;
                try {
                    qqid = response.getString("openid");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (response.has("nickname")) {
                    try {
                        qqgender = response.getString("gender");
                        qqname = response.getString("nickname");
                    } catch (JSONException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            } else if (msg.what == 1) {
                Bitmap bitmap = (Bitmap) msg.obj;
            }
        }

    };

    private void onClickLogin() {
        if (!mQQAuth.isSessionValid()) {
            IUiListener listener = new BaseUiListener() {
                @Override
                protected void doComplete(JSONObject values) {
                    updateUserInfo();
                    updateLoginButton();
                    Toast.makeText(LoginActivity.this, "用户id： " + qqid + "\n用户昵称： " + qqname + "\n用户性别： " + qqgender, Toast.LENGTH_SHORT).show();
                    final String[] tv = new String[1];

                    //建立连接
                    AsyncHttpClient client = new AsyncHttpClient();
                    String Url_add = "http://10.7.84.97:8080/qq/add";
                    //获取参数
                    RequestParams params = new RequestParams();
                    params.add("qqid",qqid);
                    params.add("qqname",qqname);
                    params.add("qqgender",qqgender);
                    //服务器获取参数
                    client.get(getApplicationContext(), Url_add, params, new AsyncHttpResponseHandler() {
                        @Override
                        public void onSuccess(int i, Header[] headers, byte[] bytes) {
                            tv[0] = new String(bytes);
                            // System.out.print(response);
//                    try {
//                        JSONObject result=response.getJSONObject("result");
//                        JSONObject qq=result.getJSONObject("qq");
//                        String output= qq.getString("qq")+"\n"
//                                +qq.getString("nick_name")+"\n"
//                                +qq.getString("user_id");
//                        Tv.setText("123");
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
                            String result = tv[0].toString();
                            Toast.makeText(LoginActivity.this, result, Toast.LENGTH_LONG).show();
                            if (result.equals("注册成功")) {
                                Intent intent = new Intent(LoginActivity.this, LauncherActivity.class);
                                startActivity(intent);
                            }
                        }


                        @Override
                        public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {

                        }


                    });

                }
            };

            mQQAuth.login(this, "all", listener);
            // mTencent.loginWithOEM(this, "all",
            // listener,"10000144","10000144","xxxx");
            mTencent.login(this, "all", listener);
        } else {
            mQQAuth.logout(this);
            updateUserInfo();
            updateLoginButton();
        }

    }

    public static boolean ready(Context context) {
        if (mQQAuth == null) {
            return false;
        }
        boolean ready = mQQAuth.isSessionValid()
                && mQQAuth.getQQToken().getOpenId() != null;
        if (!ready)
            Toast.makeText(context, "login and get openId first, please!",
                    Toast.LENGTH_SHORT).show();
        return ready;
    }

    private class BaseUiListener implements IUiListener {

        @Override
        public void onComplete(Object response) {

            //	Util1.showResultDialog(LoginActivity.this, response.toString(),
            //			"登录成功");


            JSONObject response1 = (JSONObject) response;
            try {
                qqid = response1.getString("openid");
                qqgender = response1.getString("gender");
                qqname = response1.getString("nickname");



            } catch (JSONException e) {
                e.printStackTrace();
            }

            doComplete((JSONObject) response);
        }

        protected void doComplete(JSONObject values) {

        }

        @Override
        public void onError(UiError e) {
            Util1.toastMessage(LoginActivity.this, "onError: " + e.errorDetail);
            Util1.dismissDialog();
        }

        @Override
        public void onCancel() {
            Util1.toastMessage(LoginActivity.this, "onCancel: ");
            Util1.dismissDialog();
        }
    }


    class NewClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            Context context = v.getContext();
            Class<?> cls = null;
            switch (v.getId()) {
                case R.id.lg_qq:
                    onClickLogin();

                    return;
            }
            if (cls != null) {
                Intent intent = new Intent(context, cls);
                context.startActivity(intent);
            }
        }
    }

    /**
     * 记住密码
     */

    private CheckBox cb;
    private EditText name;
    private EditText pwd;
    private Button lgbt;

    private void initViews3() {
        cb = (CheckBox) findViewById(R.id.lg_cb);
        name = (EditText)findViewById(R.id.lg_user);
        pwd = (EditText)findViewById(R.id.lg_pwd);
        lgbt = (Button)findViewById(R.id.lg_bt);
    }

    private void initEvents2() {
        lgbt.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

                if(cb.isChecked()){
                    s = getSharedPreferences("ty_user",Context.MODE_PRIVATE);
                    SharedPreferences.Editor editer = s.edit();
                    editer.putString("name",name.getText().toString());
                    editer.putString("pwd",pwd.getText().toString());
                    editer.commit();
                    Toast.makeText(LoginActivity.this, "密码保存成功！",Toast.LENGTH_LONG).show();
                } else {
                    s = getSharedPreferences("ty_user",Context.MODE_PRIVATE);
                    SharedPreferences.Editor editer = s.edit();
                    editer.putString("name",name.getText().toString());
                    editer.putString("pwd","");
                    editer.commit();
                }
            }
        });
    }

    /**
     * 微信第三方登录
     */
    public static final String TAG1 = "WeChatLogin";

    private ImageView mLoginWeChat;
    private IWXAPI api;
    private ReceiveBroadCast receiveBroadCast;

    private void wechatlogin() {

        mLoginWeChat = (ImageView)findViewById(R.id.lg_wechat);
        mLoginWeChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                weChatAuth();
            }
        });
    }

    private void weChatAuth() {
        if (api == null) {
            api = WXAPIFactory.createWXAPI(this, App.WX_APPID, true);
        }
        SendAuth.Req req = new SendAuth.Req();
        req.scope = "snsapi_userinfo";
        req.state = "wx_login_duzun";
        api.sendReq(req);
    }

    public void getAccessToken() {
        SharedPreferences WxSp = this.getApplicationContext()
                .getSharedPreferences(PrefParams.spName, Context.MODE_PRIVATE);
        String code = WxSp.getString(PrefParams.CODE, "");
        final SharedPreferences.Editor WxSpEditor = WxSp.edit();
        Log.d(TAG1, "-----获取到的code----" + code);
        String url = "https://api.weixin.qq.com/sns/oauth2/access_token?appid="
                + App.WX_APPID
                + "&secret="
                + App.WX_APPSecret
                + "&code="
                + code
                + "&grant_type=authorization_code";
        Log.d(TAG1, "--------即将获取到的access_token的地址--------");
        HttpUtil.sendHttpRequest(url, new HttpCallBackListener() {
            @Override
            public void onFinish(String response) {

                //解析以及存储获取到的信息
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    Log.d(TAG1, "-----获取到的json数据1-----" + jsonObject.toString());
                    String access_token = jsonObject.getString("access_token");
                    Log.d(TAG1, "--------获取到的access_token的地址--------" + access_token);
                    String openid = jsonObject.getString("openid");
                    String refresh_token = jsonObject.getString("refresh_token");
                    if (!access_token.equals("")) {
                        WxSpEditor.putString(PrefParams.ACCESS_TOKEN, access_token);
                        WxSpEditor.apply();
                    }
                    if (!refresh_token.equals("")) {
                        WxSpEditor.putString(PrefParams.REFRESH_TOKEN, refresh_token);
                        WxSpEditor.apply();
                    }
                    if (!openid.equals("")) {
                        WxSpEditor.putString(PrefParams.WXOPENID, openid);
                        WxSpEditor.apply();
                        getPersonMessage(access_token, openid);

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(LoginActivity.this, "通过code获取数据没有成功", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getPersonMessage(String access_token, String openid) {
        String url = "https://api.weixin.qq.com/sns/userinfo?access_token="
                + access_token
                + "&openid="
                + openid;
        HttpUtil.sendHttpRequest(url, new HttpCallBackListener() {
            @Override
            public void onFinish(String response) {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    Log.d(TAG1, "------获取到的个人信息------" + jsonObject.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(LoginActivity.this, "通过openid获取数据没有成功", Toast.LENGTH_SHORT).show();
            }
        });
    }

    class ReceiveBroadCast extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            getAccessToken();
        }
    }

}
