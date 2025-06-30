package com.test.screen_test;

public class TabActivity extends BaseActivity {

    // true : 캡쳐방지 적용 , false : 캡쳐방지 제외
    boolean screen = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // region { 캡쳐방지 }
		
		// 캡쳐방지 리스너 선언

        int licenseResult = getSecureScreenLicense();
        if (licenseResult >= 0) {
            if (screen) {
                // 캡쳐 방지 적용
            } else {
                // 캡쳐 방지 해제
            }
        }
        // endregion
    }

}
