package com.covision.utils;

import static com.covision.moapp.BaseActivity.sharedPreferences;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.RemoteException;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.covision.moapp.BaseActivity;
import com.covision.moapp.DocConverterForSynap2016;
import com.covision.moapp.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by hwlee on 2017-11-15.
 */

public class MOUtils {

    //태그용
    final static String TAG = "MOUtils";
    final public static String APP_NAME = "그룹웨어";  //TODO: <string name="app_name">그룹웨어</string>과 맞출 것.

    //앱 저장소용
    final public static String SHAREDPREFERENCE_NAME = "MobileOffice";    //TODO: <string name="app_fcmchannel">MobileOffice</string>과 맞출 것.

    // 디버그 모드 구분용 (로그 및 webview 디버깅 옵션 처리)
    final public static boolean DEBUG_MODE = true;

    /*
        try {

        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "showLoading-CATCH: " + e.toString());
        }
    * */


    private MOUtils() {
    }


    //region { getDeviceID : 2022 버전}

    //DeviceID(IMEI> 조회
    public static String getDeviceID(Context context) {

        SharedPreferences sharePref = context.getSharedPreferences(MOUtils.SHAREDPREFERENCE_NAME, Context.MODE_MULTI_PROCESS);
        String sRet = "";

        try {

            //저장된 DeviceID가 있으면 그걸 리턴
            String savedDeviceID = sharePref.getString("device_id", "NA");
            if (!savedDeviceID.equals("NA")) {
                sRet = savedDeviceID;
            } else {
                sRet = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            }
        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "getDeviceID-CATCH: " + e.toString());
            sRet = "NA";
        }

        sharePref.edit().putString("device_id", sRet).commit();

        MOUtils.WriteLog(TAG, "getDeviceID> " + sharePref.getString("device_id", sRet));
        return sRet;
    }


    //endregion

    //region { getDeviceID_Backup : DeviceID(IMEI) 조회 }

    //DeviceID(IMEI> 조회
    public static String getDeviceID_Backup(Context context) {

        SharedPreferences sharePref = context.getSharedPreferences(MOUtils.SHAREDPREFERENCE_NAME, Context.MODE_MULTI_PROCESS);
        String sRet = "";

        try {

            //저장된 DeviceID가 있으면 그걸 리턴
            String savedDeviceID = sharePref.getString("device_id", "NA");
            if (!savedDeviceID.equals("NA")) {
                sRet = savedDeviceID;
            } else {
                TelephonyManager tm = null;
                String imei = "", usim = "", android = "";

                tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

                //Highest priority: IMEI
                if (Build.VERSION.SDK_INT > 25) {
                    imei = tm.getImei();
                    //Returns the IMEI (International Mobile Equipment Identity). Return null if IMEI is not available.
                } else {
                    imei = tm.getDeviceId();
                    //Returns the unique device ID, for example, the IMEI for GSM and the MEID or ESN for CDMA phones. Return null if device ID is not available.
                }
                sRet = imei;

                if (imei == null || imei.isEmpty()) {
                    //Middle priority: USIM ID
                    usim = tm.getSimSerialNumber();
                    sRet = usim;

                    if (usim == null || usim.isEmpty()) {
                        //Lowest priority: Android ID
                        android = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
                        sRet = android;
                    }
                }
            }
        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "getDeviceID-CATCH: " + e.toString());
            sRet = "NA";
        }

        sharePref.edit().putString("device_id", sRet).commit();

        MOUtils.WriteLog(TAG, "getDeviceID> " + sharePref.getString("device_id", sRet));

        return sRet;
    }


    //endregion


    //region { 네트워크 통신 }

    public static String HttpPostRequest(final String url, final String jsonParam) {

        MOUtils.WriteLog(TAG, "HttpPostRequest| url:" + url);
        MOUtils.WriteLog(TAG, "HttpPostRequest| jsonParam:" + jsonParam);

        String sRet = "";
        int code = 0;

        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        OkHttpClient client = DoHHttpClientUtils.getInstance();

        RequestBody requestBody = null;
        Request request = null;
        Response response = null;

        try {

            //네트워크 오류 방지
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitNetwork().build());

            //파라미터 처리

            HttpUrl.Builder httpBuider = HttpUrl.parse(url).newBuilder();

            if (!jsonParam.isEmpty()) {
                JSONObject jsonObject = new JSONObject(jsonParam);
                Iterator<String> keysItr = jsonObject.keys();
                while (keysItr.hasNext()) {
                    String key = keysItr.next();
                    String value = jsonObject.getString(key);

                    httpBuider.addQueryParameter(key, value);
                }
            }

            //post
            //requestBody = RequestBody.create(mediaType, data);
            //request = new Request.Builder().url(url).post(requestBody).build();

            requestBody = RequestBody.create(mediaType, "");
            request = new Request.Builder().url(httpBuider.build()).post(requestBody).build();

            response = client.newCall(request).execute();
            code = response.code();

            if (code == 200) {
                sRet = response.body().string();

                if (sRet.startsWith("\"")) {
                    sRet = sRet.substring(1);
                }
                if (sRet.endsWith("\"")) {
                    sRet = sRet.substring(0, sRet.length() - 1);
                }
            } else {
                sRet = "ERROR";
            }
        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "HttpPostRequest-CATCH: " + e.toString());
            sRet = "ERROR";
        } finally {
            MOUtils.WriteLog(TAG, "HttpPostRequest> " + url + " | " + jsonParam + " > " + sRet);
        }

        return sRet;
    }

    public static String HttpPostRequestEnc(final String url, final String data) {

        String sendData;
        String sRet = "";
        int code = 0;

        JSONObject jsn = null;

        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        OkHttpClient client = DoHHttpClientUtils.getInstance();
        //OkHttpClient client = new OkHttpClient();

        RequestBody requestBody = null;
        Request request = null;
        Response response = null;

        try {

            //네트워크 오류 방지
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitNetwork().build());

            /*
            StringBuffer buffer = new StringBuffer();
			if(sMode.toUpperCase().equals("CONVERT")) {
				//서버로 값 전송
				buffer.append(data);//buffer.append(stringTojson(data));
			} else {
				//서버로 값 전송
				String sTmpkey = AES256Cipher.AES_GenerateKey();
				buffer.append("data").append("=").append(AES256Cipher.AES_Encode(stringTojson(data).toString(), sTmpkey)).append("&");
				buffer.append("key").append("=").append(sTmpkey);
			}
            * */

            String sTmpkey = AES256CipherUtils.AES_GenerateKey();   // 보안 취약점 (고정된 암호화키 값 사용X)
            MOUtils.WriteLog(TAG, "HttpPostRequestEnc| sTmpkey:" + sTmpkey);
            sendData = "data=" + AES256CipherUtils.AES_Encode(stringTojson(data).toString(), sTmpkey);
            sendData += "&key=" + sTmpkey;

            MOUtils.WriteLog(TAG, "HttpPostRequestEnc| sendData:" + sendData);

            //post
            requestBody = RequestBody.create(mediaType, sendData);
            request = new Request.Builder().url(url).post(requestBody).build();

            response = client.newCall(request).execute();
            code = response.code();

            MOUtils.WriteLog(TAG, "HttpPostRequestEnc| sRet : " + sRet);

            if (code == 200) {
                sRet = response.body().string();

                MOUtils.WriteLog(TAG, "HttpPostRequestEnc| " + sRet);

                //복호화
                jsn = new JSONObject(sRet);
                sRet = AES256CipherUtils.AES_Decode(jsn.getString("Data"), jsn.getString("key"));

                MOUtils.WriteLog(TAG, "HttpPostRequestEnc| " + sRet);
            } else {
                sRet = "ERROR";
            }
        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "HttpPostRequestEnc-CATCH: " + e.toString());
            sRet = "ERROR";
        } finally {
            MOUtils.WriteLog(TAG, "HttpPostRequestEnc| " + url + " | " + data + " > " + sRet);
        }

        return sRet;
    }

    public static String HttpPostRequestEncJson(final String url, final JSONObject obj) {

        String sendData;
        String sRet = "";
        int code = 0;

        JSONObject jsn = null;

        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        OkHttpClient client = DoHHttpClientUtils.getInstance();

        RequestBody requestBody = null;
        Request request = null;
        Response response = null;

        try {

            //네트워크 오류 방지
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitNetwork().build());

            String sTmpkey = AES256CipherUtils.AES_GenerateKey();   // 보안 취약점 (고정된 암호화키 값 사용X)
            MOUtils.WriteLog(TAG, "HttpPostRequestEncJson| sTmpkey:" + sTmpkey);
            sendData = "data=" + AES256CipherUtils.AES_Encode(obj.toString(), sTmpkey);
            sendData += "&key=" + sTmpkey;

            MOUtils.WriteLog(TAG, "HttpPostRequestEncJson| sendData:" + sendData);

            //post
            requestBody = RequestBody.create(mediaType, sendData);
            request = new Request.Builder().url(url).post(requestBody).build();

            response = client.newCall(request).execute();
            code = response.code();

            if (code == 200) {
                sRet = response.body().string();

                MOUtils.WriteLog(TAG, "HttpPostRequestEncJson| " + sRet);

                //복호화
                jsn = new JSONObject(sRet);
                sRet = AES256CipherUtils.AES_Decode(jsn.getString("Data"), jsn.getString("key"));

                MOUtils.WriteLog(TAG, "HttpPostRequestEncJson| " + sRet);
            } else {
                sRet = "ERROR";
            }
        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "HttpPostRequestEncJson-CATCH: " + e.toString());
            sRet = "ERROR";
        } finally {
            MOUtils.WriteLog(TAG, "HttpPostRequestEncJson| " + url + " | " + obj.toString() + " > " + sRet);
        }

        return sRet;
    }

    // Widget용 HttpPostRequestEnc 추가
    public static String HttpPostRequestEncForWidget(final String url, final String data) {

        String sendData;
        String sRet = "";
        int code = 0;

        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded; charset=utf-8");
        OkHttpClient client = DoHHttpClientUtils.getInstance();

        RequestBody requestBody = null;
        Request request = null;
        Response response = null;
        JSONObject jsonObject = null;
        String WidgetInfo = null;
        String menuListStr = null;

        try {
            //네트워크 오류 방지
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitNetwork().build());

            if (!data.isEmpty()) {
                jsonObject = new JSONObject(data);

                if (jsonObject != null) {
                    WidgetInfo = URLEncoder.encode(jsonObject.getString("WidgetInfo"), "UTF-8");
                    menuListStr = URLEncoder.encode(jsonObject.getString("menuListStr"), "UTF-8");
                } else {
                    sRet = "ERROR - data is null";
                }
            }
            sendData = "WidgetInfo=" + WidgetInfo;
            sendData += "&menuListStr=" + menuListStr;

            MOUtils.WriteLog(TAG, "HttpPostRequestEncForWidget | sendData:" + sendData);

            //post
            requestBody = RequestBody.create(mediaType, sendData);

            request = new Request.Builder().url(url).post(requestBody).build();
            response = client.newCall(request).execute();
            code = response.code();

            if (code == 200) {
                if (response.body() != null) {
                    sRet = response.body().string();
                }
                MOUtils.WriteLog(TAG, "HttpPostRequestEncForWidget | " + sRet);
            } else {
                sRet = "ERROR";
            }
        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "HttpPostRequestEncForWidget -CATCH: " + e.toString());
            sRet = "ERROR";
        } finally {
            MOUtils.WriteLog(TAG, "HttpPostRequestEncForWidget | " + url + " | " + data + " > " + sRet);
        }

        return sRet;
    }


    public static String HttpPostRequestEncForWidgetM365(final String url, final String data) {

        String sendData;
        String sRet = "";
        int code = 0;

        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded; charset=utf-8");
        OkHttpClient client = DoHHttpClientUtils.getInstance();

        RequestBody requestBody = null;
        Request request = null;
        Response response = null;
        JSONObject jsonObject = null;
        String WidgetInfo = null;
        String menuListStr = null;

        try {
            //네트워크 오류 방지
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitNetwork().build());

            if (!data.isEmpty()) {
                jsonObject = new JSONObject(data);

                if (jsonObject != null) {
                    WidgetInfo = URLEncoder.encode(jsonObject.getString("WidgetInfo"), "UTF-8");
                    menuListStr = URLEncoder.encode(jsonObject.getString("menuListStr"), "UTF-8");
                } else {
                    sRet = "ERROR - data is null";
                }
            }
            sendData = "WidgetInfo=" + WidgetInfo;
            sendData += "&menuListStr=" + menuListStr;

            MOUtils.WriteLog(TAG, "HttpPostRequestEncForWidgetM365 | sendData:" + sendData);


            //post
            requestBody = RequestBody.create(mediaType, sendData);
            String azuressotoken = sharedPreferences.getString("azuressotoken", "");
            request = new Request.Builder().url(url).addHeader("azuressotoken", azuressotoken).addHeader("mobilelang", "ko").post(requestBody).build();
            response = client.newCall(request).execute();

            MOUtils.WriteLog(TAG, "HttpPostRequestEncForWidgetM365| azuressotoken : " + azuressotoken);
            MOUtils.WriteLog(TAG, "HttpPostRequestEncForWidgetM365| mobilelang : " + "ko");
            code = response.code();

            if (code == 200) {
                if (response.body() != null) {
                    sRet = response.body().string();
                }
                MOUtils.WriteLog(TAG, "HttpPostRequestEncForWidgetM365 | " + sRet);
            } else {
                sRet = "ERROR";
            }
        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "HttpPostRequestEncForWidgetM365 -CATCH: " + e.toString());
            sRet = "ERROR";
        } finally {
            MOUtils.WriteLog(TAG, "HttpPostRequestEncForWidgetM365 | " + url + " | " + data + " > " + sRet);
        }

        return sRet;
    }

    public static String HttpPostRequestEncForWidgetJson(final String url, final JSONObject obj) {

        String sendData;
        String sRet = "";
        int code = 0;

        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded; charset=utf-8");
        OkHttpClient client = DoHHttpClientUtils.getInstance();

        RequestBody requestBody = null;
        Request request = null;
        Response response = null;
        String WidgetInfo = null;
        String menuListStr = null;

        try {
            //네트워크 오류 방지
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitNetwork().build());


            if (obj != null) {
                WidgetInfo = URLEncoder.encode(obj.getString("WidgetInfo"), "UTF-8");
                menuListStr = URLEncoder.encode(obj.getString("menuListStr"), "UTF-8");
            } else {
                sRet = "ERROR - data is null";
            }

            sendData = "WidgetInfo=" + WidgetInfo;
            sendData += "&menuListStr=" + menuListStr;

            MOUtils.WriteLog(TAG, "HttpPostRequestEncForWidget | sendData:" + sendData);

            //post
            requestBody = RequestBody.create(mediaType, sendData);

            request = new Request.Builder().url(url).post(requestBody).build();
            response = client.newCall(request).execute();
            code = response.code();

            if (code == 200) {
                if (response.body() != null) {
                    sRet = response.body().string();
                }
                MOUtils.WriteLog(TAG, "HttpPostRequestEncForWidget | " + sRet);
            } else {
                sRet = "ERROR";
            }
        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "HttpPostRequestEncForWidget -CATCH: " + e.toString());
            sRet = "ERROR";
        } finally {
            MOUtils.WriteLog(TAG, "HttpPostRequestEncForWidget | " + url + " | " + obj.toString() + " > " + sRet);
        }

        return sRet;
    }

    public static String HttpPostRequestM365Graph(final String accessToken) {

        String url = "https://graph.microsoft.com/v1.0/me/";
        MOUtils.WriteLog(TAG, "HttpPostRequestSSO| url:" + url);
        MOUtils.WriteLog(TAG, "HttpPostRequestSSO| token:" + accessToken);

        String sRet = "";
        int code = 0;

        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        OkHttpClient client = DoHHttpClientUtils.getInstance();

        RequestBody requestBody = null;
        Request request = null;
        Response response = null;

        try {

            //네트워크 오류 방지
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitNetwork().build());

            //파라미터 처리

            HttpUrl.Builder httpBuider = HttpUrl.parse(url).newBuilder();

            requestBody = RequestBody.create(mediaType, "");
            request = new Request.Builder().url(httpBuider.build()).addHeader("Authorization", "Bearer " + accessToken).build();

            response = client.newCall(request).execute();
            code = response.code();

            if (code == 200) {
                sRet = response.body().string();

                JSONObject jsnRet = new JSONObject(sRet);
                sRet = jsnRet.get("userPrincipalName").toString();

            } else {
                sRet = "ERROR";
            }
        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "HttpPostRequestSSO-CATCH: " + e.toString());
            sRet = "ERROR";
        } finally {
            MOUtils.WriteLog(TAG, "HttpPostRequestSSO> " + url + " | " + accessToken + " > " + sRet);
        }

        return sRet;
    }


    public static String HttpGetRequest(final String url) {

        MOUtils.WriteLog(TAG, "HttpGetRequest| url:" + url);

        String sRet = "";
        int code = 0;

        OkHttpClient client = DoHHttpClientUtils.getInstance();

        Request request = null;
        Response response = null;

        try {

            //네트워크 오류 방지
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitNetwork().build());

            HttpUrl.Builder httpBuider = HttpUrl.parse(url).newBuilder();

            request = new Request.Builder().url(httpBuider.build()).build();

            response = client.newCall(request).execute();

            code = response.code();

            if (code == 200) {
                sRet = response.body().string();

                if (sRet.startsWith("\"")) {
                    sRet = sRet.substring(1);
                }
                if (sRet.endsWith("\"")) {
                    sRet = sRet.substring(0, sRet.length() - 1);
                }
            } else {
                sRet = "ERROR";
            }
        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "HttpGetRequest-CATCH: " + e.toString());
            sRet = "ERROR";
        } finally {
            MOUtils.WriteLog(TAG, "HttpGetRequest> " + url + " > " + sRet);
        }

        return sRet;
    }

    public static InputStream HttpPostRequestWhosCallImage(final String url, final String data) {

        InputStream sRet = null;
        int code = 0;

        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        OkHttpClient client = DoHHttpClientUtils.getInstance();

        RequestBody requestBody = null;
        Request request = null;
        Response response = null;

        try {

            //네트워크 오류 방지
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitNetwork().build());

            // get
//            HttpUrl.Builder httpBuider = HttpUrl.parse(url).newBuilder();
//            request = new Request.Builder().url(httpBuider.build()).build();
//            response = client.newCall(request).execute();
//            code = response.code();


            //post
            requestBody = RequestBody.create(mediaType, data);
            request = new Request.Builder().url(url).post(requestBody).build();

            response = client.newCall(request).execute();
            code = response.code();

            if (code == 200) {
                sRet = response.body().byteStream();
            }
        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "HttpGetRequestWhosCallImage-CATCH: " + e.toString());
        } finally {
            MOUtils.WriteLog(TAG, "HttpGetRequestWhosCallImage> " + url);
        }

        return sRet;
    }

    public static JSONObject stringTojson(String str) {
        JSONObject jsnRet = null;
        try {
            jsnRet = new JSONObject(str);
        } catch (JSONException e) {
            MOUtils.WriteLog(TAG, "stringTojson-CATCH: " + e.toString());
        }

        return jsnRet;
    }


    //endregion

    //region { 앱 데이터 초기화 }

    //TODO: 캐시/데이터초기화 불리할 것

    public static void ClearApp(Context context) {

        if (BaseActivity.sharedPreferences == null) {
            BaseActivity.sharedPreferences = context.getSharedPreferences(MOUtils.SHAREDPREFERENCE_NAME, Context.MODE_MULTI_PROCESS);
        }
        if (BaseActivity.sharedPreferences_config == null) {
            BaseActivity.sharedPreferences_config = PreferenceManager.getDefaultSharedPreferences(context);
        }

        BaseActivity.sharedPreferences.edit().clear().commit();
        BaseActivity.sharedPreferences_config.edit().clear().commit();
    }

    public static void ClearAppFilesDir(Context context) {

        if (BaseActivity.sharedPreferences == null) {
            BaseActivity.sharedPreferences = context.getSharedPreferences(MOUtils.SHAREDPREFERENCE_NAME, Context.MODE_MULTI_PROCESS);
        }

        BaseActivity.sharedPreferences.edit().clear().commit();
        BaseActivity.sharedPreferences_config.edit().clear().commit();

        String strfildDir = context.getFilesDir().toString();
        DeletePackageFolder(strfildDir.substring(0, strfildDir.length() - 6), true, context);
        strfildDir = context.getExternalFilesDir(null).toString();
        DeletePackageFolder(strfildDir.substring(0, strfildDir.length() - 6), true, context);

        DeletePackageFolder(MOUtils.GetFolder(), true, context);
    }

    private static void DeletePackageFolder(String parentPath, boolean isAll, Context context) {
        boolean bretDel = false;

        if (isAll) {
            if (!parentPath.toLowerCase(Locale.US).contains("/files")) {
                MOUtils.WriteLog(TAG, "DeletePackageFolder| " + parentPath);
                File file = new File(parentPath);

                if (file.exists()) {
                    String[] fnameList = file.list();
                    if (fnameList != null && fnameList.length > 0) {
                        int fCnt = fnameList.length;
                        String childPath = "";

                        for (int i = 0; i < fCnt; i++) {
                            childPath = parentPath + "/" + fnameList[i];
                            File f = new File(childPath);
                            if (!f.isDirectory()) {
                                if (!f.toString().contains("com.google.")                           //구글 관련 데이터 제외
                                        && !f.toString().toLowerCase(Locale.US).contains("cookies")) //쿠키 제외하고 삭제
                                {
                                    bretDel = f.delete();
                                    MOUtils.WriteLog(TAG, "DeletePackageFolder| 01/ del(" + bretDel + "): " + f.toString());
                                }
                            } else {
                                DeletePackageFolder(childPath, isAll, context);
                            }
                        }
                    }

                    File f = new File(parentPath);
                    bretDel = f.delete();
                    MOUtils.WriteLog(TAG, "DeletePackageFolder| 02/ del(" + bretDel + "): " + f.toString());
                }
            }
        } else {
            if (!parentPath.toLowerCase(Locale.US).contains("shared_prefs")
                    && !parentPath.toLowerCase(Locale.US).contains("/files")) {
                MOUtils.WriteLog(TAG, "DeletePackageFolder| " + parentPath);
                File file = new File(parentPath);

                if (file.exists()) {
                    String[] fnameList = file.list();
                    if (fnameList != null && fnameList.length > 0) {
                        int fCnt = fnameList.length;
                        String childPath = "";

                        for (int i = 0; i < fCnt; i++) {
                            childPath = parentPath + "/" + fnameList[i];
                            File f = new File(childPath);
                            if (!f.isDirectory()) {
                                if (!f.toString().contains("com.google.")                          //구글 관련 데이터 제외
                                        && !f.toString().toLowerCase(Locale.US).contains("cookies") //쿠키 제외하고 삭제
                                        && !f.toString().toLowerCase().contains(context.getString(R.string.app_pushhistory_txt))) {
                                    bretDel = f.delete();
                                    MOUtils.WriteLog(TAG, "DeletePackageFolder| 01/ del(" + bretDel + "): " + f.toString());
                                }
                            } else {
                                DeletePackageFolder(childPath, isAll, context);
                            }
                        }
                    }

                    File f = new File(parentPath);
                    bretDel = f.delete();
                    MOUtils.WriteLog(TAG, "DeletePackageFolder| 02/ del(" + bretDel + "): " + f.toString());
                }
            }
        }


    }

    //endregion

    //region { 암복호화 }


    //TripleDES
    //key => workplace2.0connworkplac
    //iv  => www.no1.


    public static String GetTriEncString(Context context, String k, String decoded) {
        String sRet = "";

        try {
            String iv = MOUtils.GetGWInfo(context, 1);

            TripleDesUtils ti = new TripleDesUtils(k, iv);

            sRet = ti.encryptText(decoded);
        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "GetTriEncString-CATCH: " + e.toString());
        }

        return sRet;
    }

    public static String GetTriDecString(Context context, String k, String encoded) {
        String sRet = "";

        try {
            String iv = MOUtils.GetGWInfo(context, 1);

            TripleDesUtils ti = new TripleDesUtils(k, iv);

            sRet = ti.decryptText(encoded);
        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "GetTriDecString-CATCH: " + e.toString());
        }

        return sRet;
    }

    //endregion


    //region { 사용자 정보 get/set }

//    public static String GetUserInfo(Context context) {
//        String sRet = "";
//
//        try {
//            String s = context.getPackageName() + context.getPackageName() + context.getPackageName();
//            s = s.substring(0, 32);
//
//            if(BaseActivity.sharedPreferences == null) {
//                BaseActivity.sharedPreferences = context.getSharedPreferences(MOUtils.SHAREDPREFERENCE_NAME, Context.MODE_MULTI_PROCESS);
//            }
//
//            sRet = BaseActivity.sharedPreferences.getString("user_info", "NA");
//
//            MOUtils.WriteLog(TAG, "GetUserInfo-sRet: " + sRet);
//            try {
//                if(!sRet.equals("NA")) {
//                    sRet = AES256CipherUtils.AES_Decode_forApp(sRet, s);
//                }
//
//            } catch (Exception e) {
//                MOUtils.WriteLog(TAG, "GetUserInfo-CATCH 1: " + e.toString());
//
//                if(!sRet.equals("NA")) {
//                    String abc = AES256CipherUtils.AES_Encode_forApp(stringTojson(sRet).toString(), s);
//                    BaseActivity.sharedPreferences.edit().putString("user_info", abc).commit();
//                }
//            }
//        } catch (Exception e) {
//            MOUtils.WriteLog(TAG, "GetUserInfo-CATCH: " + e.toString());
//        }
//
//        return sRet;
//    }

//    public static void SetUserInfo(Context context, String i) {
//        try {
//
//            String s = context.getPackageName() + context.getPackageName() + context.getPackageName();
//            s = s.substring(0, 32);
//
//            if(BaseActivity.sharedPreferences == null) {
//                BaseActivity.sharedPreferences = context.getSharedPreferences(MOUtils.SHAREDPREFERENCE_NAME, Context.MODE_MULTI_PROCESS);
//            }
//
//            if(!i.equals("NA")) {
//                BaseActivity.sharedPreferences.edit().putString("user_info", AES256CipherUtils.AES_Encode_forApp(stringTojson(i).toString(), s)).commit();
//            } else {
//                BaseActivity.sharedPreferences.edit().putString("user_info", i).commit();
//            }
//        } catch (Exception e) {
//            MOUtils.WriteLog(TAG, "SetUserInfo-CATCH: " + e.toString());
//        }
//    }


    public static String GetUserID(Context context) {
        String sRet = "";
        try {
            if (BaseActivity.sharedPreferences == null) {
                BaseActivity.sharedPreferences = context.getSharedPreferences(MOUtils.SHAREDPREFERENCE_NAME, Context.MODE_MULTI_PROCESS);
            }
            sRet = BaseActivity.sharedPreferences.getString("user_id", "NA");
        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "GetUserID-CATCH: " + e.toString());
        }

        MOUtils.WriteLog(TAG, "GetUserID-sRet: " + sRet);
        return sRet;
    }

    public static void SetUserID(Context context, String id) {
        try {
            if (BaseActivity.sharedPreferences == null) {
                BaseActivity.sharedPreferences = context.getSharedPreferences(MOUtils.SHAREDPREFERENCE_NAME, Context.MODE_MULTI_PROCESS);
            }
            BaseActivity.sharedPreferences.edit().putString("user_id", id).commit();
        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "SetUserID-CATCH: " + e.toString());
        }
    }

    public static String GetUserInfoNew(Context context) {
        String sRet = "";
        try {
            if (BaseActivity.sharedPreferences == null) {
                BaseActivity.sharedPreferences = context.getSharedPreferences(MOUtils.SHAREDPREFERENCE_NAME, Context.MODE_MULTI_PROCESS);
            }
            sRet = BaseActivity.sharedPreferences.getString("user_info", "NA");
        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "GetUserInfoNew-CATCH: " + e.toString());
        }

        MOUtils.WriteLog(TAG, "GetUserInfoNew-sRet: " + sRet);
        return sRet;
    }

    public static void SetUserInfoNew(Context context, String user_info) {
        try {
            if (BaseActivity.sharedPreferences == null) {
                BaseActivity.sharedPreferences = context.getSharedPreferences(MOUtils.SHAREDPREFERENCE_NAME, Context.MODE_MULTI_PROCESS);
            }

            if (user_info.startsWith("\"")) {
                user_info = user_info.substring(1);
            }
            if (user_info.endsWith("\"")) {
                user_info = user_info.substring(0, user_info.length() - 1);
            }

            BaseActivity.sharedPreferences.edit().putString("user_info", user_info).commit();
        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "SetUserInfoNew-CATCH: " + e.toString());
        }
    }

    public static void setServerVersion(Context context, String version) {
        try {
            if (BaseActivity.sharedPreferences == null) {
                BaseActivity.sharedPreferences = context.getSharedPreferences(MOUtils.SHAREDPREFERENCE_NAME, Context.MODE_MULTI_PROCESS);
            }
        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "setServerVersion-CATCH: " + e.toString());
        }
        BaseActivity.sharedPreferences.edit().putString("serverVersion", version).commit();

    }

    public static int getServerVersion(Context context) {
        int sRet = 0;
        try {
            if (BaseActivity.sharedPreferences == null) {
                BaseActivity.sharedPreferences = context.getSharedPreferences(MOUtils.SHAREDPREFERENCE_NAME, Context.MODE_MULTI_PROCESS);
            }

            String version = BaseActivity.sharedPreferences.getString("serverVersion", "0.0.0");
            version = version.replace(".", "");
            sRet = Integer.parseInt(version);

        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "getServerVersion-CATCH: " + e.toString());
        }

        return sRet;
    }

    public static String GetDeviceBrandName(Context context) {
        String sRet = "";
        try {
            if (BaseActivity.sharedPreferences == null) {
                BaseActivity.sharedPreferences = context.getSharedPreferences(MOUtils.SHAREDPREFERENCE_NAME, Context.MODE_MULTI_PROCESS);
            }
            sRet = BaseActivity.sharedPreferences.getString("brand_name", "NA");
        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "GetDeviceBrandName-CATCH: " + e.toString());
        }

        MOUtils.WriteLog(TAG, "GetDeviceBrandName-sRet: " + sRet);
        return sRet;
    }

    public static void SetDeviceBrandName(Context context, String brandName) {
        try {
            if (BaseActivity.sharedPreferences == null) {
                BaseActivity.sharedPreferences = context.getSharedPreferences(MOUtils.SHAREDPREFERENCE_NAME, Context.MODE_MULTI_PROCESS);
            }
            BaseActivity.sharedPreferences.edit().putString("brand_name", brandName).commit();
        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "SetDeviceBrandName-CATCH: " + e.toString());
        }
    }

    //endregion

    public static String GetUserLogin(Context context) {
        String sRet = "";
        try {
            if (BaseActivity.sharedPreferences == null) {
                BaseActivity.sharedPreferences = context.getSharedPreferences(MOUtils.SHAREDPREFERENCE_NAME, Context.MODE_MULTI_PROCESS);
            }
            sRet = BaseActivity.sharedPreferences.getString("user_login", "NA");
        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "GetUserLogin-CATCH: " + e.toString());
        }

        MOUtils.WriteLog(TAG, "GetUserLogin-sRet: " + sRet);
        return sRet;
    }

    public static void SetUserLogin(Context context, Date date) {
        try {
            if (BaseActivity.sharedPreferences == null) {
                BaseActivity.sharedPreferences = context.getSharedPreferences(MOUtils.SHAREDPREFERENCE_NAME, Context.MODE_MULTI_PROCESS);
            }

            if (date == null) {
                date = new Date();
            }

            BaseActivity.sharedPreferences.edit().putString("user_logintime", GetFormatedDateTime("yyyy-MM-dd HH:mm:ss", date)).commit();

        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "SetUserLogin-CATCH: " + e.toString());
        }
    }


    //region { GW자동인증 생성정보 get/set }

    public static String GetGWInfo(Context context, int index) {

        //0: GWEncAESKey/ 1: GWEncIV/ 2: GWEncPK/ 3: GWEncXK

        String sRet = "NA";

        try {
            String s = context.getPackageName() + context.getPackageName() + context.getPackageName();
            s = s.substring(0, 32);

            if (BaseActivity.sharedPreferences == null) {
                BaseActivity.sharedPreferences = context.getSharedPreferences(MOUtils.SHAREDPREFERENCE_NAME, Context.MODE_MULTI_PROCESS);
            }

            if (0 == index) {
                sRet = BaseActivity.sharedPreferences.getString("gw_enc_aeskey", "NA");
            } else if (1 == index) {
                sRet = BaseActivity.sharedPreferences.getString("gw_enc_iv", "NA");
            } else if (2 == index) {
                sRet = BaseActivity.sharedPreferences.getString("gw_enc_pk", "NA");
            } else if (3 == index) {
                sRet = BaseActivity.sharedPreferences.getString("gw_enc_xk", "NA");
            }

//            MOUtils.WriteLog(TAG, "GetGWInfo-sRet: " + sRet);

            try {
                if (!sRet.equals("NA")) {
                    sRet = AES256CipherUtils.AES_Decode_forApp(sRet, s);
                }
            } catch (Exception e) {
                MOUtils.WriteLog(TAG, "GetGWInfo-CATCH 1: " + e.toString());

                if (!sRet.equals("NA")) {
                    String abc = AES256CipherUtils.AES_Encode_forApp(sRet, s);
                    if (0 == index) {
                        BaseActivity.sharedPreferences.edit().putString("gw_enc_aeskey", abc).commit();
                    } else if (1 == index) {
                        BaseActivity.sharedPreferences.edit().putString("gw_enc_iv", abc).commit();
                    } else if (2 == index) {
                        BaseActivity.sharedPreferences.edit().putString("gw_enc_pk", abc).commit();
                    } else if (3 == index) {
                        BaseActivity.sharedPreferences.edit().putString("gw_enc_xk", abc).commit();
                    }
                }
            }
        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "GetGWInfo-CATCH: " + e.toString());
        }

        MOUtils.WriteLog(TAG, "GetGWInfo-sRet: " + sRet);

        return sRet;
    }

    public static void SetGWInfo(Context context, String i, int index) {

        //0: GWEncAESKey/ 1: GWEncIV/ 2: GWEncPK/ 3: GWEncXK
        //String sKey24 = "workplace2.0connworkplac";
        //String sIV = "www.no1.";

        try {
            String s = context.getPackageName() + context.getPackageName() + context.getPackageName();
            s = s.substring(0, 32);

            if (BaseActivity.sharedPreferences == null) {
                BaseActivity.sharedPreferences = context.getSharedPreferences(MOUtils.SHAREDPREFERENCE_NAME, Context.MODE_MULTI_PROCESS);
            }

            if (!i.equals("NA")) {
                if (0 == index) {
                    BaseActivity.sharedPreferences.edit().putString("gw_enc_aeskey", AES256CipherUtils.AES_Encode_forApp(i, s)).commit();
                } else if (1 == index) {
                    BaseActivity.sharedPreferences.edit().putString("gw_enc_iv", AES256CipherUtils.AES_Encode_forApp(i, s)).commit();
                } else if (2 == index) {
                    //16자 => 16자+앞8자

                    if (i.length() < 16) {
                        i += "Covision";
                        i = i.substring(0, 16);
                    }

                    i = i + (i.substring(0, 8));

                    //MOUtils.WriteLog(TAG, "SetGWInfo-gw_enc_pk: " + i);

                    BaseActivity.sharedPreferences.edit().putString("gw_enc_pk", AES256CipherUtils.AES_Encode_forApp(i, s)).commit();
                } else if (3 == index) {
                    //16자 => 16자+앞8자

                    if (i.length() < 16) {
                        i += "Covision";
                        i = i.substring(0, 16);
                    }

                    i = i + (i.substring(0, 8));

                    BaseActivity.sharedPreferences.edit().putString("gw_enc_xk", AES256CipherUtils.AES_Encode_forApp(i, s)).commit();
                }
            } else {
                if (0 == index) {
                    BaseActivity.sharedPreferences.edit().putString("gw_enc_aeskey", i).commit();
                } else if (1 == index) {
                    BaseActivity.sharedPreferences.edit().putString("gw_enc_iv", i).commit();
                } else if (2 == index) {
                    BaseActivity.sharedPreferences.edit().putString("gw_enc_pk", i).commit();
                } else if (3 == index) {
                    BaseActivity.sharedPreferences.edit().putString("gw_enc_xk", i).commit();
                }
            }
        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "SetGWInfo-CATCH: " + e.toString());
        }
    }

    //endregion

    //region { 언어설정 }

    public static String GetAppLanguageCode(Context context) {
        String sRet = "";
        boolean availableLang = false;
        try {
            if (BaseActivity.sharedPreferences == null) {
                BaseActivity.sharedPreferences = context.getSharedPreferences(MOUtils.SHAREDPREFERENCE_NAME, Context.MODE_MULTI_PROCESS);
            }

            sRet = BaseActivity.sharedPreferences_config.getString("config_lang_code", "NA");

            if (sRet.equals("NA")) {
                sRet = Locale.getDefault().getLanguage();
                // 기기 설정된 언어를 앱에서 지원하는지 확인
                String[] useLangCodeList = InitialLanguageCodeList(context);
                for (int i = 0; i < useLangCodeList.length; i++) {
                    if (useLangCodeList[i].equals(sRet)) {
                        availableLang = true;
                        break;
                    }
                }
                if (availableLang == false) {
                    sRet = useLangCodeList[0];
                    MOUtils.WriteLog(TAG, "GetAppLanguageCode-Unavailable language: " + sRet);
                }
                SetAppLanguageCode(context, sRet);
            }
        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "GetAppLanguageCode-CATCH: " + e.toString());
        }

        MOUtils.WriteLog(TAG, "GetAppLanguageCode-sRet: " + sRet);
        return sRet;
    }

    public static void SetAppLanguageCode(Context context, String code) {

        MOUtils.WriteLog(TAG, "SetAppLanguageCode-code: " + code);

        try {
            if (BaseActivity.sharedPreferences == null) {
                BaseActivity.sharedPreferences = context.getSharedPreferences(MOUtils.SHAREDPREFERENCE_NAME, Context.MODE_MULTI_PROCESS);
            }

            BaseActivity.sharedPreferences_config.edit().putString("config_lang_code", code).commit();
        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "SetAppLanguageCode-CATCH: " + e.toString());
        }
    }

    public static String GetAppLanguageName(Context context) {
        String sRet = "";
        try {
            String[] arrLangTitle = InitialLanguageTitleList(context);
            String[] arrLangCode = InitialLanguageCodeList(context);
            String code = MOUtils.GetAppLanguageCode(context);

            for (int i = 0; i < arrLangCode.length; i++) {
                if (arrLangCode[i].equals(code))
                    sRet = arrLangTitle[i];
            }
        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "GetAppLanguageName-CATCH: " + e.toString());
        }

        return sRet;
    }

    // 다국어 array 초기화
    public static String[] InitialLanguageTitleList(Context context) {
        String[] arrLangTitle = context.getResources().getStringArray(R.array.select_language);
        String[] useLangCodeList = null;    //temp
        String[] initialLangList = null;
        int index = -1;
        try {
            //다국어 array 초기화
            useLangCodeList = context.getResources().getStringArray(R.array.select_ues_language); // 사용 언어 코드
            int countLang = 0;
            for (int i = 0; i < useLangCodeList.length; i++) {
                if (useLangCodeList[i].equals("Y"))
                    countLang++;
            }

            initialLangList = new String[countLang];
            for (int i = 0; i < useLangCodeList.length; i++) {
                if (useLangCodeList[i].equals("Y")) {
                    initialLangList[++index] = arrLangTitle[i];
                }
            }

        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "InitialLanguageTitleList-CATCH: " + e.toString());
        }

        return initialLangList;
    }

    public static String[] InitialLanguageCodeList(Context context) {
        String[] arrLangCode = context.getResources().getStringArray(R.array.select_language_code);
        String[] useLangCodeList = null;    //temp
        String[] initialLangCodeList = null;
        int index = -1;
        try {
            useLangCodeList = context.getResources().getStringArray(R.array.select_ues_language);
            int countLang = 0;
            for (int i = 0; i < useLangCodeList.length; i++) {
                if (useLangCodeList[i].equals("Y"))
                    countLang++;
            }

            initialLangCodeList = new String[countLang];
            for (int i = 0; i < useLangCodeList.length; i++) {
                if (useLangCodeList[i].equals("Y")) {
                    initialLangCodeList[++index] = arrLangCode[i];
                }
            }

        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "InitialLanguageCodeList-CATCH: " + e.toString());
        }

        return initialLangCodeList;
    }

    public static void LocaleSet(Context context) {

        String code = MOUtils.GetAppLanguageCode(context);

        MOUtils.WriteLog(TAG, "LocaleSet| code:" + code + ", from: " + context.getClass().getSimpleName());

        Configuration confg = context.getResources().getConfiguration();

        try {
            switch (code) {
                case "ko":
                    Locale.setDefault(Locale.KOREAN);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        confg.setLocale(Locale.KOREAN);
                    } else {
                        confg.locale = Locale.KOREAN;
                    }

                    break;
                case "en":
                    Locale.setDefault(Locale.ENGLISH);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        confg.setLocale(Locale.ENGLISH);
                    } else {
                        confg.locale = Locale.ENGLISH;
                    }

                    break;

                case "ja":
                    Locale.setDefault(Locale.JAPANESE);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        confg.setLocale(Locale.JAPANESE);
                    } else {
                        confg.locale = Locale.JAPANESE;
                    }

                    break;

                case "zh":
                    Locale.setDefault(Locale.CHINESE);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        confg.setLocale(Locale.CHINESE);
                    } else {
                        confg.locale = Locale.CHINESE;
                    }

                    break;
            }
            context.getResources().updateConfiguration(confg, context.getResources().getDisplayMetrics());
        } catch (Exception ex) {
            MOUtils.WriteLog(TAG, "LocaleSet| catch:" + ex.toString());
        }
    }


    //endregion


    //region  { CreateFolder : 앱 전용 디렉터리 생성 }

    public static void CreateFolder(Context context) {
        try {
            String sPath = MOUtils.GetFolder();

            File fl = new File(sPath);
            if (!fl.isDirectory()) {
                fl.mkdirs();
            }

            //PUSH HISTORY 파일 생성

            String sHistoryPath = sPath + context.getString(R.string.app_pushhistory_txt);
            fl = new File(sHistoryPath);
            if (!fl.exists()) {
                fl.createNewFile();
            }

        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "CreateFolder-CATCH: " + e.toString());
        }
    }

    //endregion

    //region  { GetFolder : 앱 전용 디렉터리 조회 ('/'포함) }

    public static String GetFolder() {

        String sPath = "";
        try {
            sPath = Environment.getExternalStorageDirectory() + "/" + MOUtils.SHAREDPREFERENCE_NAME + "/";

            File fl = new File(sPath);
            if (!fl.isDirectory()) {
                fl.mkdirs();
            }

        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "GetFolder-CATCH: " + e.toString());
        }

        return sPath;
    }

    //endregion

    //region  { InstalledAppVersion: 설치된 앱 버전 조회 }

    public static String InstalledAppVersion(Context context) {
        String sRet = "";

        try {
            sRet = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "InstalledAppVersion-CATCH: " + e.toString());
        }

        return sRet;
    }

    //endregion

    //region { GoDocConverter : 문서변환 바로가기 }

    public static void GoDocConverter(Context context, String fileID, String fullUrl) {

        MOUtils.WriteLog(TAG, "GoDocConverter|fileID: " + fileID + ",fullUrl: " + fullUrl);

        try {

            //서버로그 추가
            try {

                String sPost = "{";
                sPost += "DvcId:\"" + MOUtils.getDeviceID(context) + "\",";
                sPost += "FullURL:\"" + fullUrl + "\"";
                sPost += "}";

                SharedPreferences sharedPreferences = context.getSharedPreferences(MOUtils.SHAREDPREFERENCE_NAME, Context.MODE_MULTI_PROCESS);

                String loggingurl = sharedPreferences.getString("push_server", "NA");
                if (loggingurl.equals("NA")) {
                    MOUtils.WriteLog(TAG, "GoDocConverter| push_server invalid!!");
                }
                if (loggingurl.endsWith("/")) {
                    loggingurl = loggingurl.substring(0, loggingurl.length() - 1);
                }
                loggingurl += context.getString(R.string.url_log_fileview);

                //페이지 이동 로그 서버호출
                String sRet = MOUtils.HttpPostRequestEnc(loggingurl, sPost);
                MOUtils.WriteLog(TAG, "GoDocConverter| response: " + sRet);

            } catch (Exception e) {
                MOUtils.WriteLog(TAG, "GoDocConverter| catch:" + e.toString());
            }

            Bundle bundle = new Bundle();
            bundle.putString("FileID", fileID);
            bundle.putString("FullURI", fullUrl);

            Intent intent = new Intent(context, DocConverterForSynap2016.class);
            intent.putExtras(bundle);
            context.startActivity(intent);

        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "GoDocConverter-CATCH: " + e.toString());
        }


    }

    public static void GoDocConverter(Context context, String fileID, String fullUrl, String waterMarkText) {

        MOUtils.WriteLog(TAG, "GoDocConverter|fileID: " + fileID + ",fullUrl: " + fullUrl + ",waterMark:" + waterMarkText);

        try {

            //서버로그 추가
            try {

                String sPost = "{";
                sPost += "DvcId:\"" + MOUtils.getDeviceID(context) + "\",";
                sPost += "FullURL:\"" + fullUrl + "\"";
                sPost += "}";

                SharedPreferences sharedPreferences = context.getSharedPreferences(MOUtils.SHAREDPREFERENCE_NAME, Context.MODE_MULTI_PROCESS);

                String loggingurl = sharedPreferences.getString("push_server", "NA");
                if (loggingurl.equals("NA")) {
                    MOUtils.WriteLog(TAG, "GoDocConverter| push_server invalid!!");
                }
                if (loggingurl.endsWith("/")) {
                    loggingurl = loggingurl.substring(0, loggingurl.length() - 1);
                }
                loggingurl += context.getString(R.string.url_log_fileview);

                //페이지 이동 로그 서버호출
                String sRet = MOUtils.HttpPostRequestEnc(loggingurl, sPost);
                MOUtils.WriteLog(TAG, "GoDocConverter| response: " + sRet);

            } catch (Exception e) {
                MOUtils.WriteLog(TAG, "GoDocConverter| catch:" + e.toString());
            }

            Bundle bundle = new Bundle();
            bundle.putString("FileID", fileID);
            bundle.putString("FullURI", fullUrl);
            bundle.putString("WatermarkText", waterMarkText);

            Intent intent = new Intent(context, DocConverterForSynap2016.class);
            intent.putExtras(bundle);
            context.startActivity(intent);

        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "GoDocConverter-CATCH: " + e.toString());
        }


    }

    //endregion

    //region { GoBrowser : 브라우져 열고 바로가기 }

    public static void GoBrowser(Context context, String url) {
        try {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(Uri.parse(url));
            context.startActivity(intent);

            return;
        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "GoBrowser-CATCH: " + e.toString());
        }
    }

    //endregion

    //region { NowGet : 현재시간 YYYY-MM-DD hh:mm:ss 포맷으로 조회 }

    public static String NowGet(String sSep) {//이준희(2011-07-05): Added a method to return the current time in {YYYY-MM-DD hh:mm:ss} format.
        String sRet = "";
        String sSepSub = "", sSpc = "", sYer = "";
        Calendar calendar;

        try {
            if (sSep.equals(null)) {
                sSep = "-";
            }
            sSepSub = sSep;
            if (sSep.equals("-")) {
                sSepSub = ":";
            }
            sSpc = " ";
            if (sSep.equals("")) {
                sSpc = "";
            }

            calendar = Calendar.getInstance();
            sYer = calendar.get(Calendar.YEAR) + "";
            String sMon = (calendar.get(Calendar.MONTH) + 1) + "";
            String sDay = calendar.get(Calendar.DAY_OF_MONTH) + "";
            String sHur = calendar.get(Calendar.HOUR_OF_DAY) + "";
            String sMin = calendar.get(Calendar.MINUTE) + "";
            String sSec = calendar.get(Calendar.SECOND) + "";
            sRet = sYer + sSep + DigitFill(sMon, 2) + sSep + DigitFill(sDay, 2) + sSpc + DigitFill(sHur, 2) + sSepSub + DigitFill(sMin, 2) + sSepSub + DigitFill(sSec, 2);
        } catch (Exception ex) {
            WriteLog(TAG, "NowGet| catch:" + ex.toString());
        }
        return sRet;
    }

    //endregion

    //region { encodeURIComponent/decodeURIComponent : 자바스크립트 encodeURIComponent/decodeURIComponent }

    public static String encodeURIComponent(String s) {
        String result = null;

        try {
            result = URLEncoder.encode(s, "UTF-8")
                    .replaceAll("\\+", "%20")
                    .replaceAll("\\%21", "!")
                    .replaceAll("\\%27", "'")
                    .replaceAll("\\%28", "(")
                    .replaceAll("\\%29", ")")
                    .replaceAll("\\%7E", "~");
        }

        // This exception should never occur.
        catch (UnsupportedEncodingException e) {
            result = s;
        }

        return result;
    }

    public static String decodeURIComponent(String s) {
        String result = null;

        try {
            s = s.replaceAll("\\%20", "+")
                    .replaceAll("\\!", "%21")
                    .replaceAll("\\'", "%27")
                    .replaceAll("\\(", "%28")
                    .replaceAll("\\)", "%29")
                    .replaceAll("\\~", "%7E");

            result = URLDecoder.decode(s, "UTF-8");
        }

        // This exception should never occur.
        catch (UnsupportedEncodingException e) {
            result = s;
        }

        return result;
    }

    //endregion

    //region { GetParamValueFromURL : URL에서 파라미터 읽어오기 }

    public static String GetParamValueFromURL(String url, String param) {
        String sRet = "";
        Uri uri = null;

        try {

            uri = Uri.parse(url);
            sRet = uri.getQueryParameter(param);

        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "GetParamValueFromURL-CATCH: " + e.toString());
        }

        return sRet;
    }

    //endregion

    //region { GetFileNameFromContentDisp : contentDisposition에서 파일명 읽어오기 }

    public static String GetFileNameFromContentDisp(String contentdisposition) {
        String sRet = "";

        try {
            String arr[] = contentdisposition.split("filename=");
            sRet = arr[1].replace("filename=", "").replace("\"", "").trim();
            sRet = URLDecoder.decode(sRet, "UTF-8");

        /*
        String contentSplit[] = content.split("filename=");
        filename = contentSplit[1].replace("filename=", "").replace("\"", "").trim();
        * */

        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "GetParamValueFromURL-CATCH: " + e.toString());
        }

        return sRet;
    }

    //endregion

    //region { GetExtensionSupport : 첨부파일 지원하는지 확인 }

    public static boolean GetExtensionSupport(Context context, Intent intent) {//첨부파일 형식이 지원되는지 확인하는 메서드를 추가함.

        boolean bRet = false;
        List<ResolveInfo> list;

        try {
            list = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            if (list.size() > 0) {
                bRet = true;
            }
        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "GetExtensionSupport-CATCH: " + e.toString());
        }
        return bRet;
    }

    //endregion

    //region { GetFileNameAndExtension : 파일명.확장자 조회 }

    public static String GetFileNameAndExtension(String filepath) {
        String sRet = "";

        try {
            Uri uri = Uri.parse(filepath);
            sRet = uri.getPathSegments().get(uri.getPathSegments().size() - 1);
        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "GetFileNameAndExtension-CATCH: " + e.toString());
        }
        return sRet;
    }

    //endregion

    //region { GetExtension : 파일 확장자 조회 }

    public static String GetExtension(String filepath) {
        String sRet = "";

        try {
            Uri uri = Uri.parse(filepath);
            sRet = uri.getPathSegments().get(uri.getPathSegments().size() - 1);
            sRet = sRet.substring(sRet.lastIndexOf(".") + 1);
        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "GetExtension-CATCH: " + e.toString());
        }
        return sRet;
    }

    //endregion

    //region { GetMimeType : 확장자별 MimeType 조회 }

    public static String GetMimeType(String ext) {
        String sRet = "";

        try {
            switch (ext.toLowerCase()) {

                //region  { 케이스별 마임타입 리턴 }

                case "accdb":
                    sRet = "application/msaccess";
                    break;
                case "ai":
                    sRet = "application/postscript";
                    break;
                case "aif":
                    sRet = "audio/x-aiff";
                    break;
                case "aifc":
                    sRet = "audio/x-aiff";
                    break;
                case "aiff":
                    sRet = "audio/x-aiff";
                    break;
                case "amr":
                    sRet = "audio/amr";
                    break;
                case "application":
                    sRet = "application/x-ms-application";
                    break;
                case "au":
                    sRet = "audio/basic";
                    break;
                case "avi":
                    sRet = "video/x-msvideo";
                    break;
                case "bin":
                    sRet = "application/octet-stream";
                    break;
                case "bmp":
                    sRet = "image/bmp";
                    break;
                case "cab":
                    sRet = "application/vnd.ms-cab-compressed";
                    break;
                case "ccad":
                    sRet = "application/clariscad";
                    break;
                case "cdf":
                    sRet = "application/x-cdf";
                    break;
                case "csh":
                    sRet = "application/x-csh";
                    break;
                case "css":
                    sRet = "text/css";
                    break;
                case "deploy":
                    sRet = "application/octet-stream";
                    break;
                case "doc":
                    sRet = "application/msword";
                    break;
                case "docm":
                    sRet = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                    break;
                case "docx":
                    sRet = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                    break;
                case "dvi":
                    sRet = "application/x-dvi";
                    break;
                case "dwg":
                    sRet = "application/acad";
                    break;
                case "dxf":
                    sRet = "application/dxf";
                    break;
                case "eps":
                    sRet = "application/postscript";
                    break;
                case "etx":
                    sRet = "text/x-setext";
                    break;
                case "gif":
                    sRet = "image/gif";
                    break;
                case "gzip":
                    sRet = "multipart/x-gzip";
                    break;
                case "htm":
                    sRet = "text/html";
                    break;
                case "html":
                    sRet = "text/html";
                    break;
                case "hwp":
                    sRet = "application/haansofthwp";//2013-03 추가(테스트완료)-태림
                    break;
                case "ief":
                    sRet = "image/ief";
                    break;
                case "jpe":
                    sRet = "image/jpeg";
                    break;
                case "jpeg":
                    sRet = "image/jpeg";
                    break;
                case "jpg":
                    sRet = "image/jpg";
                    break;
                case "js":
                    sRet = "application/x-javascript";
                    break;
                case "latex":
                    sRet = "application/x-latex";
                    break;
                case "log":
                    sRet = "text/html";
                    break;
                case "man":
                    sRet = "application/x-troff-man";
                    break;
                case "manifest":
                    sRet = "application/manifest";
                    break;
                case "mdb":
                    sRet = "application/msaccess";
                    break;
                case "me":
                    sRet = "application/x-troff-me";
                    break;
                case "mif":
                    sRet = "application/x-mif";
                    break;
                case "mov":
                    sRet = "video/quicktime";
                    break;
                case "movie":
                    sRet = "video/x-sgi-movie";
                    break;
                case "mp4":
                    sRet = "video/mp4";
                    break;
                case "mp3":
                    sRet = "audio/mp3";
                    break;
                case "wma":
                    sRet = "audio/x-ms-wma";
                    break;
                case "mpe":
                    sRet = "video/mpeg";
                    break;
                case "mpeg":
                    sRet = "video/mpeg";
                    break;
                case "mpg":
                    sRet = "video/mpeg";
                    break;
                case "ms":
                    sRet = "application/x-troff-ms";
                    break;
                case "pbm":
                    sRet = "image/x-portable-bitmap";
                    break;
                case "pdf":
                    sRet = "application/pdf";
                    break;
                case "pgm":
                    sRet = "image/x-portable-graymap";
                    break;
                case "png":
                    sRet = "image/png";
                    break;
                case "pnm":
                    sRet = "image/x-portable-anymap";
                    break;
                case "ppm":
                    sRet = "image/x-portable-pixmap";
                    break;
                case "ppt":
                    sRet = "application/vnd.ms-powerpoint";//application/x-mspowerpoint
                    break;
                case "pptm":
                    sRet = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
                    break;
                case "pptx":
                    sRet = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
                    break;
                case "ps":
                    sRet = "application/postscript";
                    break;
                case "pub":
                    sRet = "application/x-mspublisher";
                    break;
                case "qt":
                    sRet = "video/quicktime";
                    break;
                case "ras":
                    sRet = "image/x-cmu-raster";
                    break;
                case "rgb":
                    sRet = "image/x-rgb";
                    break;
                case "roff":
                    sRet = "application/x-troff";
                    break;
                case "rtf":
                    sRet = "application/rtf";
                    break;
                case "rtx":
                    sRet = "text/richtext";
                    break;
                case "snd":
                    sRet = "audio/basic";
                    break;
                case "src":
                    sRet = "application/x-wais-source";
                    break;
                case "svg":
                    sRet = "image/svg+xml";
                    break;
                case "t":
                    sRet = "application/x-troff";
                    break;
                case "tcl":
                    sRet = "application/x-tcl";
                    break;
                case "tex":
                    sRet = "application/x-tex";
                    break;
                case "texi":
                    sRet = "application/x-texinfo";
                    break;
                case "texinfo":
                    sRet = "application/x-texinfo";
                    break;
                case "tif":
                    sRet = "image/tiff";
                    break;
                case "tiff":
                    sRet = "image/tiff";
                    break;
                case "tr":
                    sRet = "application/x-troff";
                    break;
                case "tsv":
                    sRet = "text/tab-separated-values";
                    break;
                case "txt":
                    sRet = "text/plain";
                    break;
                case "wav":
                    sRet = "audio/x-wav";
                    break;
                case "xaml":
                    sRet = "application/xaml+xml";
                    break;
                case "xap":
                    sRet = "application/x-silverlight-app";
                    break;
                case "xbap":
                    sRet = "application/x-ms-xbap";
                    break;
                case "xbm":
                    sRet = "image/x-xbitmap";
                    break;
                case "xht":
                    sRet = "application/xhtml+xml";
                    break;
                case "xhtml":
                    sRet = "application/xhtml+xml";
                    break;
                case "xls":
                    sRet = "application/vnd.ms-excel";//application/x-msexcel
                    break;
                case "xlsm":
                    sRet = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    break;
                case "xlsx":
                    sRet = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    break;
                case "xml":
                    sRet = "text/xml";
                    break;
                case "xpm":
                    sRet = "image/x-xpixmap	";
                    break;
                case "xps":
                    sRet = "application/vnd.ms-xpsdocument";
                    break;
                case "xsl":
                    sRet = "text/xsl";
                    break;
                case "xwd":
                    sRet = "image/x-xwindowdump";
                    break;
                case "zip":
                    sRet = "multipart/x-zip";//application/zip
                    break;
                default:
                    sRet = "";
                    break;

                //endregion
            }
        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "GetMimeType-CATCH: " + e.toString());
        }
        return sRet;
    }

    //endregion

    //region { DigitFill : 0으로 채운 특정 자리수의 숫자 생성 }

    public static String DigitFill(Object obj, int iLen) {//Added a method to align the digits of the given number to two.
        String sRet = "";
        String sSrc = obj.toString();

        sRet = sSrc;
        sRet = "000000000000000000000000000000" + sSrc;
        sRet = sRet.substring(sRet.length() - iLen, sRet.length());

        return sRet;
    }

    //endregion

    //region { DisplayBadge : 뱃지 표시 }

    public static void DisplayBadge(Context context, int iBadgeCnt) {
        SharedPreferences sharePref;

        try {
            // 뱃지 카운트 설정
            sharePref = PreferenceManager.getDefaultSharedPreferences(context);
            sharePref.edit().putInt("BADGE_COUNT_UPDATE", iBadgeCnt).commit();

            // 앱 아이콘에 뱃지 카운트 update
            Intent intent_badge = new Intent("android.intent.action.BADGE_COUNT_UPDATE");
            intent_badge.putExtra("badge_count", iBadgeCnt);
            intent_badge.putExtra("badge_count_package_name", context.getString(R.string.app_packagename));
            intent_badge.putExtra("badge_count_class_name", context.getString(R.string.app_mainactivity));

            context.sendBroadcast(intent_badge);

        } catch (Exception ex) {
            WriteLog(TAG, "DisplayBadge| catch: " + ex.toString());
        }
    }

    //endregion

    //region { CompareVer : 2개 버전을 비교 }

    public static boolean CompareVer(String latest_version, String current_version) {//이준희(2011-07-25): 버전값 2개를 받아, 후자가 전자 이상인지 확인하는 메서드를 추가함.
        boolean bRet = false;
        String[] latest_versions, current_versions;

        try {
            if (latest_version.indexOf(".") == -1) {
                if (Integer.parseInt(latest_version) <= Integer.parseInt(current_version)) {
                    bRet = true;
                }
            } else {
                latest_versions = latest_version.split("\\.");
                current_versions = current_version.split("\\.");

                if (latest_versions.length == current_versions.length) {

                    String sFullVerBase = "", sFullVerCurr = "";
                    for (int ifor = 0; ifor < latest_versions.length; ifor++) {
                        if (latest_versions[ifor].length() > current_versions[ifor].length()) {
                            sFullVerBase += DigitFill(latest_versions[ifor], latest_versions[ifor].length());
                            sFullVerCurr += DigitFill(current_versions[ifor], latest_versions[ifor].length());
                        } else {
                            sFullVerBase += DigitFill(latest_versions[ifor], current_versions[ifor].length());
                            sFullVerCurr += DigitFill(current_versions[ifor], current_versions[ifor].length());
                        }
                    }

                    if (Integer.parseInt(sFullVerBase) <= Integer.parseInt(sFullVerCurr)) {
                        bRet = true;
                    }
                }
            }
        } catch (Exception e) {
            bRet = false;
            MOUtils.WriteLog(TAG, "CompareVer-CATCH: " + e.toString());
        }

        return bRet;
    }

    //endregion

    //region { FormattedFileSize : 숫자를 받아 파일용량을 표시(byte/KB/MB/GB) }

    public static String FormattedFileSize(int size) {

        String sRet = "";
        BigDecimal bdSize = new BigDecimal(size);

        try {
            if (size == 0) {
                sRet = "Empty";
            }
            if (size > 0 && size < 1024) {
                sRet = String.format(Locale.US, "%d bytes", size);
            }
            if (size >= 1024 && size < Math.pow(1024, 2)) {
                sRet = bdSize.divide(new BigDecimal(1024), 1, BigDecimal.ROUND_UP) + " KB";
            }
            if (size >= Math.pow(1024, 2) && size < Math.pow(1024, 3)) {
                sRet = bdSize.divide(new BigDecimal(Math.pow(1024, 2)), 1, BigDecimal.ROUND_UP) + " MB";
            }
            if (size >= Math.pow(1024, 3)) {
                sRet = bdSize.divide(new BigDecimal(Math.pow(1024, 3)), 1, BigDecimal.ROUND_UP) + " GB";
            }
        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "FormattedFileSize-CATCH: " + e.toString());
        }
        return sRet;
    }

    //endregion

    //region { GetFolderSize : 폴더의 용량 조회 (하위 폴더 포함) }

    public static int GetFolderSize(File parent) {
        int totalsize = 0;

        //앱 데이터 저장소일 경우 예외처리
        if (parent.toString().toLowerCase().contains("shared_prefs")
                || parent.toString().toLowerCase().contains("/files")) {
            return totalsize;
        }

        try {

            if (parent.exists()) {
                File[] arrFiles = parent.listFiles();

                for (File f : arrFiles) {
                    if (!f.isDirectory()) {
                        totalsize += f.length();
                    } else {
                        totalsize += GetFolderSize(f);
                    }
                }
            }
        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "GetFolderSize-CATCH: " + e.toString());
        }

        MOUtils.WriteLog(TAG, "GetFolderSize-totalsize (" + parent.toString() + "): " + totalsize);

        return totalsize;
    }

    //endregion

    //region { DeleteFolderRecursive : 폴더 삭제 (하위 폴더 포함) }

    public static boolean DeleteFolderRecursive(File parent, Context context) {

        boolean bRet = false;

        //앱 데이터 저장소일 경우 예외처리
        if (parent.toString().toLowerCase().contains("shared_prefs")
                || parent.toString().toLowerCase().contains("/files")
                || parent.toString().toLowerCase().contains("/databases")) {
            return true;
        }

        try {

            if (parent.exists()) {
                File[] arrFiles = parent.listFiles();
                boolean bDeleted = false;

                for (File f : arrFiles) {
                    if (!f.isDirectory()) {
                        if (!f.toString().contains(context.getString(R.string.app_pushhistory_txt))) {
                            bDeleted = f.delete();
                            MOUtils.WriteLog(TAG, "DeleteFolderRecursive(" + bDeleted + "): " + f.toString());
                        }
                    } else {
                        bRet = DeleteFolderRecursive(f, context);
                        if (bRet) {
                            f.delete();
                        }
                    }
                }

                bRet = true;
            }
        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "DeleteFolderRecursive-CATCH: " + e.toString());
        }

        MOUtils.WriteLog(TAG, "DeleteFolderRecursive >> " + bRet + ": " + parent.toString());
        return bRet;
    }

    public static boolean DeleteFolderRecursiveNoCookie(File parent, Context context) {

        boolean bRet = false;

        //앱 데이터 저장소일 경우 예외처리
        if (parent.toString().toLowerCase().contains("shared_prefs")
                || parent.toString().toLowerCase().contains("/files")
                || parent.toString().toLowerCase().contains("/databases")) {
            return true;
        }

        try {

            if (parent.exists()) {
                File[] arrFiles = parent.listFiles();
                boolean bDeleted = false;

                for (File f : arrFiles) {
                    if (!f.isDirectory()) {
                        if (!f.toString().contains(context.getString(R.string.app_pushhistory_txt))
                                && !f.toString().toLowerCase().contains("cookies")) {
                            bDeleted = f.delete();
                            MOUtils.WriteLog(TAG, "DeleteFolderRecursiveNoCookie(" + bDeleted + "): " + f.toString());
                        }
                    } else {
                        bRet = DeleteFolderRecursiveNoCookie(f, context);
                        if (bRet) {
                            f.delete();
                        }
                    }
                }

                bRet = true;
            }
        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "DeleteFolderRecursiveNoCookie-CATCH: " + e.toString());
        }

        MOUtils.WriteLog(TAG, "DeleteFolderRecursiveNoCookie >> " + bRet + ": " + parent.toString());
        return bRet;
    }

    //endregion

    //region { DeleteDatabaseFolder : Database 폴더 삭제 }

    public static boolean DeleteDatabaseFolder(File parent) {

        boolean bRet = false;

        try {

            if (parent.exists()) {
                File[] arrFiles = parent.listFiles();
                boolean bDeleted = false;

                for (File f : arrFiles) {
                    if (!f.isDirectory()) {
                        bDeleted = f.delete();
                        MOUtils.WriteLog(TAG, "DeleteDatabaseFolder(" + bDeleted + "): " + f.toString());
                    }
                }

                bRet = true;
            }
        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "DeleteDatabaseFolder-CATCH: " + e.toString());
        }

        MOUtils.WriteLog(TAG, "DeleteDatabaseFolder >> " + bRet + ": " + parent.toString());
        return bRet;
    }

    //endregion

    //region { GetFormatedDateTime : Formated Date 조회 }

    public static String GetFormatedDateTime(String format, Date date) {

        String sRet = "";
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
            sRet = simpleDateFormat.format(date).toString();
        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "GetFormatedDateTime-CATCH: " + e.toString());
        }

        return sRet;
    }

    //endregion

    //region { GetCompanyCode : 회사코드 조회(Android 임의로 사용) }

    public static String GetCompanyCode(Context context) {
        String sRet = "";

        try {
            sRet = context.getString(R.string.project_company).toUpperCase();
        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "GetCompanyCode-CATCH: " + e.toString());
        }
        return sRet;
    }

    //endregion

    //region { IsEmulator : 가상머신 여부 확인 }

    public static boolean IsEmulator(Context context) {

        boolean bRet = Build.FINGERPRINT.toLowerCase().startsWith("generic")
                || Build.FINGERPRINT.toLowerCase().startsWith("unknown")
                || Build.MODEL.toLowerCase().contains("google_sdk")
                || Build.MODEL.toLowerCase().contains("emulator")
                || Build.MODEL.toLowerCase().contains("android sdk built for x86")
                || Build.MANUFACTURER.toLowerCase().contains("genymotion")
                || (Build.BRAND.toLowerCase().startsWith("generic") && Build.DEVICE.toLowerCase().startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT.toLowerCase())
                || Build.getRadioVersion().isEmpty();

        //Toast.makeText(context, "IsEmulator: " + bRet, Toast.LENGTH_LONG).show();

        return bRet;
    }

    //endregion

    //region { AddContact : 연락처 저장 }

    public static void AddContact(Context context, String name, String job, String company,
                                  String mobile, String phone, String mail) {

        MOUtils.WriteLog(TAG, "AddContact-" + name + "/" + job + "/" + company
                + "/" + mobile + "/" + phone + "/" + mail);
        //name, job, group, domain, phone, mobile, mail
        //이재규|과장|경영지원팀|태광관광개발(주)|070-8189-6025|010-8893-1038|28074771@tgw.co.kr

        Intent intent = null;

        try {
            intent = new Intent(ContactsContract.Intents.Insert.ACTION);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            intent.setType(ContactsContract.RawContacts.CONTENT_TYPE);

            intent.putExtra(ContactsContract.Intents.Insert.NAME, name);

            intent.putExtra(ContactsContract.Intents.Insert.JOB_TITLE, job);

            intent.putExtra(ContactsContract.Intents.Insert.COMPANY, company);

            intent.putExtra(ContactsContract.Intents.Insert.PHONE, mobile);
            intent.putExtra(ContactsContract.Intents.Insert.PHONE_TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE);

            intent.putExtra(ContactsContract.Intents.Insert.SECONDARY_PHONE, phone);
            intent.putExtra(ContactsContract.Intents.Insert.SECONDARY_PHONE_TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_WORK);

            intent.putExtra(ContactsContract.Intents.Insert.EMAIL, mail);
            intent.putExtra(ContactsContract.Intents.Insert.EMAIL_TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK);

            context.startActivity(intent);
        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "AddContact-CATCH: " + e.toString());
        }
    }
    //endregion

    //region { AddContactList : 연락처 동기화 }

    public static void AddContactList(Context context, JSONArray jsonList) {
        try {
            for (int i = 0; i < jsonList.length(); i++) {       // "장욱진", "사원", "연구3팀", "코비젼", "01035019762","021234567","ujjang@covision.co.kr"
                MOUtils.WriteLog(TAG, "GetUserNumberInfoFromServer-(" + i + ")" + jsonList.get(i));

                JSONObject jsonObject = jsonList.getJSONObject(i);
                String name = jsonObject.getString("displayName");
                String job = jsonObject.getString("jobLevelName");
                String group = jsonObject.getString("groupName");
                String company = jsonObject.getString("companyName");
                String mobile = jsonObject.getString("mobile");
                String phone = jsonObject.getString("phoneNumber");
                String mail = jsonObject.getString("mailAddress");

                long rawContactId = CheckDuplicationContact(context, name, mail);  // rawContactId = 0 > 등록되지 않은 사용자(신규 등록)

                if (rawContactId == 0) { // 중복 데이터가 없으면, 기본 주소록에 저장
                    ArrayList<ContentProviderOperation> ops = new ArrayList<>();

                    ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                            .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                            .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                            .build());

                    // 이름
                    if (name != null && !name.trim().equals("")) {
                        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                                .withValue(ContactsContract.Data.MIMETYPE,
                                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                                .build());
                    }

                    // 회사/부서/직급
                    if ((job != null && !job.trim().equals("")) && (group != null && !group.trim().equals("")) && (company != null && !company.trim().equals(""))) {
                        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                                .withValue(ContactsContract.Data.MIMETYPE,
                                        ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Organization.TITLE, job)
                                .withValue(ContactsContract.CommonDataKinds.Organization.DEPARTMENT, group)
                                .withValue(ContactsContract.CommonDataKinds.Organization.COMPANY, company)
                                .withValue(ContactsContract.CommonDataKinds.Organization.TYPE, ContactsContract.CommonDataKinds.Organization.TYPE_WORK)
                                .build());
                    }

                    // 휴대폰번호
                    if (mobile != null && !mobile.trim().equals("")) {
                        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                                .withValue(ContactsContract.Data.MIMETYPE,
                                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, mobile.replace("-", ""))
                                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                                .build());
                    }

                    // 내선번호
                    if (phone != null && !phone.trim().equals("")) {
                        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                                .withValue(ContactsContract.Data.MIMETYPE,
                                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone.replace("-", ""))
                                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_WORK_MOBILE)
                                .build());
                    }

                    // 메일주소
                    if (mail != null && !mail.trim().equals("")) {
                        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                                .withValue(ContactsContract.Data.MIMETYPE,
                                        ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, mail)
                                .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK)
                                .build());
                    }

                    if (ops.size() > 0) {
                        try {
                            context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
                        } catch (RemoteException e) {
                            MOUtils.WriteLog(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
                        } catch (OperationApplicationException e) {
                            MOUtils.WriteLog(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
                        }
                    }
                } else {    // update contact info
                    updateContactWorkInfo(context.getContentResolver(), rawContactId, job, group, company);
                    updateContactMobileNumber(context.getContentResolver(), rawContactId, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE, mobile);
                    updateContactPhoneNumber(context.getContentResolver(), rawContactId, ContactsContract.CommonDataKinds.Phone.TYPE_WORK_MOBILE, phone);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static long getRawContactIdByMobileNumber(Context context, String name, String mail) {
        String queryColumnArr[] = {ContactsContract.CommonDataKinds.Phone._ID, ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Email.ADDRESS};
        int rawContactId = -1;

        MOUtils.WriteLog(TAG, "getRawContactIdByName return rawContactId : " + mail);

        String where = ContactsContract.CommonDataKinds.Email.ADDRESS + " IN ('" + mail + "') ";

        //Contact 검색 쿼리
        Cursor cursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, queryColumnArr, where, null, null);
        MOUtils.WriteLog(TAG, "Cursor.getCount() : " + cursor.getCount());

        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
//            if(name.equals(cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))))
            rawContactId = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID));
        } else {
            return rawContactId;
        }

        MOUtils.WriteLog(TAG, "getRawContactIdByName return rawContactId : " + rawContactId);

        return rawContactId;
    }

    private static long CheckDuplicationContact(Context context, String name, String mail) {
        long ret = 0;
        long rawContactId;
        rawContactId = getRawContactIdByMobileNumber(context, name, mail);

        if (rawContactId > -1) { // 신규 등록되는 사용자가 아님 > Update Contact Info
            MOUtils.WriteLog(TAG, "CheckDuplicationContact : 등록된 사용자, ID = " + rawContactId);  // 어떤 데이터가 중복되는지 체크?
            ret = rawContactId;
        } else {
            MOUtils.WriteLog(TAG, "CheckDuplicationContact : 신규 사용자");
        }
        return ret;

    }

    private static void updateContactMobileNumber(ContentResolver contentResolver,
                                                  long rawContactId, int phoneType, String newMobileNumber) {
        // Create content values object.
        ContentValues contentValues = new ContentValues();

        // Put new phone number value.
        contentValues.put(ContactsContract.CommonDataKinds.Phone.NUMBER, newMobileNumber);

        // Create query condition, query with the raw contact id.
        StringBuffer whereClauseBuf = new StringBuffer();

        // Specify the update contact id.
        whereClauseBuf.append(ContactsContract.Data.RAW_CONTACT_ID);
        whereClauseBuf.append("=");
        whereClauseBuf.append(rawContactId);

        // Specify the row data mimetype to phone mimetype( vnd.android.cursor.item/phone_v2 )
        whereClauseBuf.append(" and ");
        whereClauseBuf.append(ContactsContract.Data.MIMETYPE);
        whereClauseBuf.append(" = '");
        String mimetype = ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE;
        whereClauseBuf.append(mimetype);
        whereClauseBuf.append("'");

        // Specify phone type.
        whereClauseBuf.append(" and ");
        whereClauseBuf.append(ContactsContract.CommonDataKinds.Phone.TYPE);
        whereClauseBuf.append(" = ");
        whereClauseBuf.append(phoneType);

        // Update phone info through Data uri.Otherwise it may throw java.lang.UnsupportedOperationException.
        Uri dataUri = ContactsContract.Data.CONTENT_URI;

        // Get update data count.
        int updateCount = contentResolver.update(dataUri, contentValues, whereClauseBuf.toString(), null);
        MOUtils.WriteLog(TAG, "updateMobileNumber - updateCount : " + updateCount);
    }

    private static void updateContactPhoneNumber(ContentResolver contentResolver, long rawContactId, int phoneType, String newPhoneNumber) {
        // Create content values object.
        ContentValues contentValues = new ContentValues();

        // Put new phone number value.
        contentValues.put(ContactsContract.CommonDataKinds.Phone.NUMBER, newPhoneNumber);

        // Create query condition, query with the raw contact id.
        StringBuffer whereClauseBuf = new StringBuffer();

        // Specify the update contact id.
        whereClauseBuf.append(ContactsContract.Data.RAW_CONTACT_ID);
        whereClauseBuf.append("=");
        whereClauseBuf.append(rawContactId);

        // Specify the row data mimetype to phone mimetype( vnd.android.cursor.item/phone_v2 )
        whereClauseBuf.append(" and ");
        whereClauseBuf.append(ContactsContract.Data.MIMETYPE);
        whereClauseBuf.append(" = '");
        String mimetype = ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE;
        whereClauseBuf.append(mimetype);
        whereClauseBuf.append("'");

        // Specify phone type.
        whereClauseBuf.append(" and ");
        whereClauseBuf.append(ContactsContract.CommonDataKinds.Phone.TYPE);
        whereClauseBuf.append(" = ");
        whereClauseBuf.append(phoneType);

        // Update phone info through Data uri.Otherwise it may throw java.lang.UnsupportedOperationException.
        Uri dataUri = ContactsContract.Data.CONTENT_URI;

        // Get update data count.
        int updateCount = contentResolver.update(dataUri, contentValues, whereClauseBuf.toString(), null);
        MOUtils.WriteLog(TAG, "updatePhoneNumber - updateCount : " + updateCount);
    }

    private static void updateContactWorkInfo(ContentResolver contentResolver, long rawContactId, String newJobTitleName, String newGroupName, String newCompanyName) {
        // Create content values object.
        ContentValues contentValues = new ContentValues();

        // Put new phone number value.
        contentValues.put(ContactsContract.CommonDataKinds.Organization.TITLE, newJobTitleName);
        contentValues.put(ContactsContract.CommonDataKinds.Organization.DEPARTMENT, newGroupName);
        contentValues.put(ContactsContract.CommonDataKinds.Organization.COMPANY, newCompanyName);

        // Create query condition, query with the raw contact id.
        StringBuffer whereClauseBuf = new StringBuffer();

        // Specify the update contact id.
        whereClauseBuf.append(ContactsContract.Data.RAW_CONTACT_ID);
        whereClauseBuf.append("=");
        whereClauseBuf.append(rawContactId);

        // Specify the row data mimetype to phone mimetype( vnd.android.cursor.item/phone_v2 )
        whereClauseBuf.append(" and ");
        whereClauseBuf.append(ContactsContract.Data.MIMETYPE);
        whereClauseBuf.append(" = '");
        String mimetype = ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE;
        whereClauseBuf.append(mimetype);
        whereClauseBuf.append("'");

        // Specify organization type.
        whereClauseBuf.append(" and ");
        whereClauseBuf.append(ContactsContract.CommonDataKinds.Organization.TYPE);
        whereClauseBuf.append(" = ");
        whereClauseBuf.append(ContactsContract.CommonDataKinds.Organization.TYPE_WORK);

        // Update phone info through Data uri.Otherwise it may throw java.lang.UnsupportedOperationException.
        Uri dataUri = ContactsContract.Data.CONTENT_URI;

        // Get update data count.
        int updateCount = contentResolver.update(dataUri, contentValues, whereClauseBuf.toString(), null);
        MOUtils.WriteLog(TAG, "updateWorkInfo - updateCount : " + updateCount);
    }

    private static void DeleteContactPhoneByName(Context context, long rawContactId) {
        ContentResolver contentResolver = context.getContentResolver();
        MOUtils.WriteLog(TAG, "DeleteContactPhoneByName - delete : " + rawContactId);

        //******************************* delete data table related data ****************************************
        // Data table content process uri.
        Uri dataContentUri = ContactsContract.Data.CONTENT_URI;
//        Uri dataContentUri = ContactsContract.RawContacts.CONTENT_URI;

        // Create data table where clause.
        StringBuffer dataWhereClauseBuf = new StringBuffer();
        dataWhereClauseBuf.append(ContactsContract.Data.RAW_CONTACT_ID);
        dataWhereClauseBuf.append(" = ");
        dataWhereClauseBuf.append(rawContactId);

        // Delete all this contact related data in data table.
        contentResolver.delete(dataContentUri, dataWhereClauseBuf.toString(), null);


        //******************************** delete raw_contacts table related data ***************************************
        // raw_contacts table content process uri.
        Uri rawContactUri = ContactsContract.RawContacts.CONTENT_URI;

        // Create raw_contacts table where clause.
        StringBuffer rawContactWhereClause = new StringBuffer();
        rawContactWhereClause.append(ContactsContract.RawContacts._ID);
        rawContactWhereClause.append(" = ");
        rawContactWhereClause.append(rawContactId);

        // Delete raw_contacts table related data.
        contentResolver.delete(rawContactUri, rawContactWhereClause.toString(), null);


        //******************************** delete contacts table related data ***************************************
        // contacts table content process uri.
        Uri contactUri = ContactsContract.Contacts.CONTENT_URI;

        // Create contacts table where clause.
        StringBuffer contactWhereClause = new StringBuffer();
        contactWhereClause.append(ContactsContract.Contacts._ID);
        contactWhereClause.append(" = ");
        contactWhereClause.append(rawContactId);

        // Delete raw_contacts table related data.
        contentResolver.delete(contactUri, contactWhereClause.toString(), null);

    }

    //endregion

    //region { LoadImageToDrawableFromWeb : URL에서 이미지 Drawable로 가져오기 }

    public static Drawable LoadImageToDrawableFromWeb(String url) {

        //android.os.NetworkOnMainThreadException 방지
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());
        try {
            InputStream is = (InputStream) new URL(url).getContent();
            Drawable d = Drawable.createFromStream(is, "logo_company");

            return d;
        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "LoadImageFromWeb-CATCH: " + e.toString());
            return null;
        }
    }

    //endregion

    //region { GetTermOfDate : datestring 시간차 조회 (ms) }

    public static long GetTermOfDate(String date1, String date2) {

        long time_term = 0;
        try {
            SimpleDateFormat dtformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            Date dt1 = dtformat.parse(date1);
            Date dt2 = dtformat.parse(date2);

            time_term = dt1.getTime() - dt2.getTime();
            MOUtils.WriteLog(TAG, "GetTermOfDate-tern: " + time_term);

        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "GetTermOfDate-CATCH: " + e.toString());
        }

        return time_term;
    }

    //endregion

    //region { GetUTCTicks : FIDO용 Ticks 조회 }

    private static final long TICKS_AT_EPOCH = 621355968000000000L;
    private static final long TICKS_PER_MILLISECOND = 10000;

//    public static String GetUTCTicks(Date date) {    //yyyy-MM-dd HH:mm:ss
//
//        /*
//        Calendar calendar = Calendar.getInstance();
//        calendar.setTime(date);
//
//        return ((calendar.getTimeInMillis() * TICKS_PER_MILLISECOND) + TICKS_AT_EPOCH) + "";
//        */
//
//        TimeZone timezone = TimeZone.getTimeZone("GMT+00:00");
//        Calendar calendar = Calendar.getInstance(timezone);
//
//        String sToken = calendar.get(Calendar.YEAR) + "-" + MOUtils.DigitFill(calendar.get(Calendar.MONTH) + 1, 2) + "-" + MOUtils.DigitFill(calendar.get(Calendar.DATE), 2)
//                + " "
//                + MOUtils.DigitFill(calendar.get(Calendar.HOUR_OF_DAY), 2) + ":" + MOUtils.DigitFill(calendar.get(Calendar.MINUTE), 2) + ":" + MOUtils.DigitFill(calendar.get(Calendar.SECOND), 2);
//
//        return sToken;
//    }

    public static String GetUTCNow() {    //yyyy-MM-dd HH:mm:ss

        TimeZone timezone = TimeZone.getTimeZone("GMT+00:00");
        Calendar calendar = Calendar.getInstance(timezone);

        String sToken = calendar.get(Calendar.YEAR) + "-" + MOUtils.DigitFill(calendar.get(Calendar.MONTH) + 1, 2) + "-" + MOUtils.DigitFill(calendar.get(Calendar.DATE), 2)
                + " "
                + MOUtils.DigitFill(calendar.get(Calendar.HOUR_OF_DAY), 2) + ":" + MOUtils.DigitFill(calendar.get(Calendar.MINUTE), 2) + ":" + MOUtils.DigitFill(calendar.get(Calendar.SECOND), 2);

        return sToken;
    }

    //endregion

    //region { GetAPKHash : 앱 hash 조회 }

    public static String GetAPKHash(Context context) {
        String sRet = "NA";

        try {

            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            String sApkPath = info.applicationInfo.sourceDir;

            FileInputStream file = new FileInputStream(sApkPath);

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.reset();

            byte[] bytes = new byte[2048];
            int iBytes;
            while ((iBytes = file.read(bytes)) != -1) {
                md.update(bytes, 0, iBytes);
            }

            byte[] digest = md.digest();

            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < digest.length; i++) {
                sb.append(Integer.toString((digest[i] & 0xff) + 0x100, 16).substring(1));
            }
            sRet = sb.toString();

            String pk = MOUtils.GetGWInfo(context, 2);

            sRet = MOUtils.GetTriEncString(context, pk, sRet);

            MOUtils.WriteLog(TAG, "GetAPKHash| sRet 1: " + sRet);

            sRet = MOUtils.GetTriEncString(context, pk, context.getString(R.string.app_version) + "|" + sRet);
        } catch (Exception e) {
            WriteLog(TAG, "GetAPKHash| catch: " + e.getMessage());
        }

        WriteLog(TAG, "GetAPKHash| sRet: " + sRet);

        return sRet;
    }

    //endregion

    //region { GetGWServer : 그룹웨어 URL 조회 }

    //BaseActivity 상속받지 않을 경우 사용
    public static String GetGWServer(Context context) {

        String sRet = "";

        try {

            if (BaseActivity.sharedPreferences == null) {
                BaseActivity.sharedPreferences = context.getSharedPreferences(MOUtils.SHAREDPREFERENCE_NAME, Context.MODE_MULTI_PROCESS);
            }

            sRet = BaseActivity.sharedPreferences.getString("gw_server", "NA");
        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "GetGWServer-CATCH: " + e.toString());
        }

        return sRet;
    }
    //endregion

    //region { IsInstalledApplication : 설치된 앱인지 확인 }

    public static boolean IsInstalledApplication(Context ctx, String packageName) {//패키지 설치 유무 확인
        MOUtils.WriteLog(TAG, "IsInstalledApplication| package:" + packageName);

        PackageManager pm = ctx.getPackageManager();
        try {
            pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            MOUtils.WriteLog(TAG, "IsInstalledApplication| catch:" + e.toString());
            return false;
        }
        return true;
    }

    //endregion

    //region { Rooted : 루팅/탈옥 체크 }
    public static boolean Rooted(Context context) {
        boolean bRet = false;

        String[] saRootFilePaths = new String[]{
                "/su/bin/su",
                "/su/xbin/su",
                "/su/bin/.user/.su",
                "/system/xbin/su",
                "/system/bin/su",
                "/system/bin/.user/.su",
                "/dev/com.noshufou.android.su",
                "/data/data/com.tegrak.lagfix",
                "/data/data/eu.chainfire.supersu",
                "/data/data/com.noshufou.android.su",
                "/data/data/com.jrummy.root.browserfree",
                "/system/app/Superuser.apk/",
                "/data/app/com.tegrak.lagfix.apk",
                "/data/app/eu.chainfire.supersu.apk",
                "/data/app/com.noshufou.android.su.apk",
                "/data/app/com.jrummy.root.browserfree.apk"
        };

        String[] saRootApps = new String[]{
                "com.tegrak.lagfix",
                "eu.chainfire.supersu",
                "com.noshufou.android.su",
                "com.jrummy.root.browserfree",
                "com.jrummy.busybox.installer",
                "me.blog.markan.UnRooting",
                "com.formyhm.hideroot"
        };

        try {
            Runtime.getRuntime().exec("su");
            bRet = true;
            if (bRet) {
                WriteLog(TAG, "Rooted| is rooted device(1) !! ");
                return bRet;
            }
        } catch (Exception e) {
        }

        try {

            //Path 검사
            int i = 0;
            while (i < saRootFilePaths.length) {
                if (new File(saRootFilePaths[i]).exists()) {
                    WriteLog(TAG, "Rooted| Path : " + saRootFilePaths[i]);
                    bRet = true;
                }
                i++;
            }
            if (bRet) {
                WriteLog(TAG, "Rooted| is rooted device(2) !! ");
                return bRet;
            }
        } catch (Exception e) {
        }

        try {
            //루팅 앱이 설치되어 있는지 확인
            int i = 0;
            while (i < saRootApps.length) {
                if (IsInstalledApplication(context, saRootApps[i])) {
                    bRet = true;
                }
                i++;
            }
            if (bRet) {
                WriteLog(TAG, "Rooted| is rooted device(3) !! ");
                return bRet;
            }

        } catch (Exception e) {
        }

        return bRet;
    }
    //endregion

    //region { GetDeviceBrand : 기기 브랜드 조회 (Samsung/LG/Android) }

    public enum DeviceBrand {Samsung, LG, Android}

    public static DeviceBrand GetDeviceBrand(Context context) {

        DeviceBrand retDeviceBrand = DeviceBrand.Android;

        SharedPreferences sharePref = context.getSharedPreferences(MOUtils.SHAREDPREFERENCE_NAME, Context.MODE_MULTI_PROCESS);

        try {
            //저장된 DeviceBrand가 있으면 그걸 리턴
            String savedDeviceBrand = sharePref.getString("device_brand", "NA");
            if (!savedDeviceBrand.equals("NA")) {
                switch (savedDeviceBrand.toUpperCase()) {
                    case "SAMSUNG":
                        retDeviceBrand = DeviceBrand.Samsung;
                        break;
                    case "LG":
                        retDeviceBrand = DeviceBrand.LG;
                        break;
                }
            } else {
                String brand = Build.BRAND;
                if (brand.toLowerCase().contains("samsung")) {
                    retDeviceBrand = DeviceBrand.Samsung;
                } else if (brand.toLowerCase().contains("lg")) {
                    retDeviceBrand = DeviceBrand.LG;
                }
            }
        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "GetDeviceBrand-CATCH: " + e.toString());
            retDeviceBrand = DeviceBrand.Android;
        }

        sharePref.edit().putString("device_brand", retDeviceBrand.toString()).commit();

        MOUtils.WriteLog(TAG, "GetDeviceBrand| sharePref> " + sharePref.getString("device_brand", "NA"));

        return retDeviceBrand;
    }


    //endregion

    //region { GetPolicy : 저장된 정책 조회 }

    public static boolean GetPolicy(String pol, Context context) {

        SharedPreferences sharePref = context.getSharedPreferences(MOUtils.SHAREDPREFERENCE_NAME, Context.MODE_MULTI_PROCESS);

        if (!pol.equals("LogOutAuto")) {
            MOUtils.WriteLog(TAG, "GetPolicy| pol: " + pol);
        }

        boolean bRet = false;
        JSONObject jsn = null;

        try {

            String sPolicy = sharePref.getString("policy", "NA");
            if (sPolicy.equals("NA")) {
                return bRet;
            }
            jsn = new JSONObject(sPolicy);

            bRet = jsn.getBoolean(pol);

            //루팅/탈옥&화면저장&기기관리자해제 정책 - MDM 미사용시 true(허용)
            if (pol.equalsIgnoreCase("DeviceAdmin")) {
                if ("N".equals(sharePref.getString("mdmuse", "N"))) {
                    bRet = true;
                }
            }
        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "GetPolicy| catch: " + e.toString());
        }

        if (!pol.equals("LogOutAuto")) {
            MOUtils.WriteLog(TAG, "GetPolicy| return: " + bRet);
        }

        return bRet;
    }

    public static int GetPolicyInt(String pol, Context context) {

        if (!pol.equals("LogOutAuto")) {
            MOUtils.WriteLog(TAG, "GetPolicyInt| pol: " + pol);
        }

        int nRet = 0;
        JSONObject jsn = null;

        SharedPreferences sharePref = context.getSharedPreferences(MOUtils.SHAREDPREFERENCE_NAME, Context.MODE_MULTI_PROCESS);

        try {

            String sPolicy = sharePref.getString("policy", "NA");
            if (sPolicy.equals("NA")) {
                return nRet;
            }
            jsn = new JSONObject(sPolicy);

            nRet = jsn.getInt(pol);

        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "GetPolicyInt| catch: " + e.toString());
        }

        if (!pol.equals("LogOutAuto")) {
            MOUtils.WriteLog(TAG, "GetPolicyInt| return: " + nRet);
        }

        return nRet;
    }

    //endregion

    //region { CallWebService : [위젯] 웹서비스 호출 }

    public static String CallWebService(Context context, String url, String namespace, String method, String mobileToken) {

        MOUtils.WriteLog(TAG, "CallWebService| url:" + url);
        MOUtils.WriteLog(TAG, "CallWebService| namespace:" + namespace);
        MOUtils.WriteLog(TAG, "CallWebService| method:" + method);
        MOUtils.WriteLog(TAG, "CallWebService| mobileToken:" + mobileToken);

        //네트워크 오류 방지
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitNetwork().build());

        CallRemote callRemote = new CallRemote(context, url, namespace, method, mobileToken);

        String sRet = "";
        AsyncTask<String, String, String> webServiceTask = callRemote.execute();

        try {
            sRet = webServiceTask.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return sRet;
    }

    public static class CallRemote extends AsyncTask<String, String, String> {

        // 위젯 카운트 조회를 위한 webservice 정보 설정

        private Context CONTEXT;
        private String URL;           //웹서비스 위치
        private String NAMESPACE;    //웹서비스 만들 때 기재
        private String SOAP_METHOD;  //호출되는 함수의 이름
        private String TOKEN;

        private String SOAP_ACTION;  //웹에서 확인하면 함수 설명이 나옴(namespace + soap_method)

        CallRemote(Context context, String url, String namespace, String method, String mobileToken) {
            this.CONTEXT = context;
            this.TOKEN = mobileToken;
            this.URL = url;
            this.NAMESPACE = namespace;
            this.SOAP_METHOD = method;

            this.SOAP_ACTION = this.NAMESPACE + this.SOAP_METHOD;
        }

        public String doInBackground(String... params) {

            String results = null;

            try {
                SoapObject request = new SoapObject(NAMESPACE, SOAP_METHOD);
                request.addProperty("mobileToken", TOKEN);//전달 파라미터(변수명 값 입력해야함)

                //웹서비스 호출 준비
                SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                envelope.setOutputSoapObject(request);
                envelope.dotNet = true;

                HttpTransportSE androidHttpTransport = new HttpTransportSE(URL);
                androidHttpTransport.call(SOAP_ACTION, envelope);   //웹서비스 호출(soap action 변수 사용)

                results = envelope.getResponse().toString();        //결과 출력(리턴값 가져옴)
            } catch (Exception e) {
                MOUtils.WriteLog(TAG, "exception: " + e.fillInStackTrace());

                results = "" + e.fillInStackTrace();
            }

            return results;
        }
    }

    //endregion

    //region { GetMobileToken : 인증토큰값 조회 }

    public static String GetMobileToken(Context context) {
        String sRet = "";

        if (BaseActivity.sharedPreferences == null) {
            BaseActivity.sharedPreferences = context.getSharedPreferences(MOUtils.SHAREDPREFERENCE_NAME, Context.MODE_MULTI_PROCESS);
        }

        try {

            TimeZone timezone = TimeZone.getTimeZone("GMT+09:00");
            Calendar calendar = Calendar.getInstance(timezone);

            String token = "|"
                    + "|"
                    + MOUtils.GetAppLanguageCode(context) + "|"
                    + BaseActivity.sharedPreferences.getString("device_id", "NA") + "|"
                    + calendar.get(Calendar.YEAR) + "-" + MOUtils.DigitFill(calendar.get(Calendar.MONTH) + 1, 2) + "-" + MOUtils.DigitFill(calendar.get(Calendar.DATE), 2) + "_" + MOUtils.DigitFill(calendar.get(Calendar.HOUR_OF_DAY), 2) + MOUtils.DigitFill(calendar.get(Calendar.MINUTE), 2) + MOUtils.DigitFill(calendar.get(Calendar.SECOND), 2) + "|"
                    + context.getPackageName() + "|"
                    + MOUtils.GetUserInfoNew(context) + "|";

            MOUtils.WriteLog(TAG, "GetMobileToken> " + token);

            String key = MOUtils.GetGWInfo(context, 0);
            String iv = MOUtils.GetGWInfo(context, 1);
            String pk = MOUtils.GetGWInfo(context, 2);

            // CP - AES256 / MP - TripleDes
            String encToken = "";
            switch (context.getString(R.string.app_platform)) {
                case "CP": //AES256
                    encToken = AES256CipherUtils.AES_Encode(token, key, iv);
                    encToken = encToken.replaceAll(System.getProperty("line.separator"), "");
                    break;
                case "MP": //TripleDes
                    TripleDesUtils ti = new TripleDesUtils(pk, iv);
                    encToken = ti.encryptText(token);
                    break;
            }

            sRet = encToken;

        } catch (Exception e) {
            WriteLog(TAG, "GetMobileToken| catch: " + e.getMessage());
        }

        WriteLog(TAG, "GetMobileToken| sRet: " + sRet);

        return sRet;
    }

    //endregion


    public static void WriteLog(String tag, String msg) {
        if (DEBUG_MODE) {
            Log.i(APP_NAME + "|" + tag, msg);
        }
    }
}
