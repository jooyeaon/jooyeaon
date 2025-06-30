package com.covision.incomingcall;

import android.app.Service;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.os.Build;
import android.telecom.Call;
import android.telecom.CallScreeningService;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.covision.GlideApp;
import com.covision.moapp.LoginActivity;
import com.covision.moapp.R;
import com.covision.sqlite.MODatabase;
import com.covision.sqlite.OrgPerson;
import com.covision.sqlite.OrgPersonDao;
import com.covision.utils.AES256CipherUtils;
import com.covision.utils.MOUtils;
import com.covision.utils.OneTimeURLGenerator;

import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NewIncomingCallService extends CallScreeningService {

    //태그
    protected final String TAG = getClass().getSimpleName();

    public WindowManager windowManager = null;

    public View windowLayout = null;

    private String info_DisplayName = "";
    private String info_JobPositionName = "";
    private String info_CompanyName = "";
    private String info_DeptName = "";
    private String info_JobTitleName = "";
    private String info_PhotoPath = "";

    private InputStream info_Photo;

    private String url_placeholderimg = "";

    private ImageView popup_close = null;

    private ImageView popup_photo = null;
    private TextView popup_name = null;
    private TextView popup_position = null;
    private TextView popup_info = null;
    private TextView popup_number = null;

    private float x = 0f;
    private float y = 0f;

    private SharedPreferences sharedPreferences = null;

    SharedPreferences sharedPreferences_config = null;

    @Override
    public void onScreenCall(@NonNull Call.Details callDetails) {
        //region 1. 사용여부 확인
        if (MOUtils.GetCompanyCode(this).equalsIgnoreCase("YBROAD")) {
            return;
        }

        // 기본앱 설정 확인
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager mRoleManager = getSystemService(RoleManager.class);

            // 기본프로그램으로 사용 할 수 있는지 확인
            boolean isRoleAvailable = mRoleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING);
            if (!isRoleAvailable) {
                MOUtils.WriteLog(TAG, "onReceive-알림창 사용안함");
                return;
            }
        }

        sharedPreferences = getApplicationContext().getSharedPreferences(MOUtils.SHAREDPREFERENCE_NAME, Context.MODE_MULTI_PROCESS);

        sharedPreferences_config = PreferenceManager.getDefaultSharedPreferences(this);
        if (!sharedPreferences_config.getBoolean("config_ul_incomingcall", false)) {
            MOUtils.WriteLog(TAG, "onReceive-알림창 사용안함");
            return;
        }
        //endregion

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            closeWindow(this);

            if (callDetails.getCallDirection() == Call.Details.DIRECTION_INCOMING) {
                String phoneNumber = callDetails.getHandle().getSchemeSpecificPart();

                phoneNumber = "0269653100";

                getOrgPerson(this, phoneNumber);

                respondToCall(callDetails, new CallResponse.Builder().build());
            }

        }
    }

    private void getOrgPerson(Context context, String incomingNumber) {

        String s = getBaseContext().getPackageName() + getBaseContext().getPackageName() + getBaseContext().getPackageName();
        s = s.substring(0, 32);

        String formatInfo = "";

        // region 1) 저장 된 DB에 있는 데이터 조회
        OrgPerson orgPersonDB = SearchUserInfoFromMODatabase(context, incomingNumber);

        try {

            if (orgPersonDB != null) {
                // 이미지 URL 호출
                if (!orgPersonDB.getUserCode().equals("")) {
                    getIncomingIsmageURL(orgPersonDB.getUserCode());
                }

            }
        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "orgPersonDB: " + e.getMessage());
        }
        // endregion

    }

    // region {후스콜 이미지 URL}
    public void getIncomingImageURL(String userID) {
		
        String sPost = "";

        try {
            OneTimeURLGenerator otu = new OneTimeURLGenerator();
            String enc = otu.encode(userID);

            sPost = "img= " + enc;

            // 로컬 URL 이므로 개발 서버로 변경
            String urlTest = "https://xxxx.xxxx.xxxx/xxxx.do";
			
            info_Photo = MOUtils.HttpPostRequestWhosCallImage(urlTest, sPost);

        }catch (Exception e) {
            e.printStackTrace();
        }
    }
    // endregion

    public void showWindow(Context context, String phone) {

        boolean bAlreadyExsistView = true;

        try {
            FrameLayout interceptorLayout = new FrameLayout(this) {

                @Override
                public boolean dispatchKeyEvent(KeyEvent event) {

                    // Only fire on the ACTION_DOWN event, or you'll get two events (one for _DOWN, one for _UP)
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {

                        // Check if the HOME button is pressed
                        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {

                            MOUtils.WriteLog(TAG, "KEYCODE_BACK");

                            // Kill service
                            closeWindow(context);

                            // As we've taken action, we'll return true to prevent other apps from consuming the event as well
                            return true;
                        }
                    }

                    // Otherwise don't intercept the event
                    return super.dispatchKeyEvent(event);
                }
            };

            if (windowLayout == null) {
                LayoutInflater inflate = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                windowLayout = inflate.inflate(R.layout.service_incomingcall, interceptorLayout);

                GlideApp.with(this)
                        .load(BitmapFactory.decodeStream(info_Photo))
                        .placeholder("기본이미지")
                        .circleCrop()
                        .into(popup_photo);

                popup_close.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            closeWindow(context);
                        } catch (Exception e) {
                            MOUtils.WriteLog(TAG, "showWindow-close-catch: " + e.toString());
                        }

                        MOUtils.WriteLog(TAG, "showWindow-close-onclick");
                    }
                });

                bAlreadyExsistView = false;
            }

            if (!bAlreadyExsistView) {

                int LAYOUT_FLAG;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
                } else {
                    LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE
                            | WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                            | WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
                }

                WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        LAYOUT_FLAG,
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                        PixelFormat.TRANSLUCENT
                );

                windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
                windowManager.addView(windowLayout, layoutParams);
            } else {
                windowLayout.invalidate();
            }

        } catch (Exception e) {
            MOUtils.WriteLog(TAG, "showWindow-catch: " + e.toString());

            Toast.makeText(NewIncomingCallService.this, getString(R.string.popup_msg_error), Toast.LENGTH_SHORT).show();
        }
    }
}
