package com.test.screen_test;


public abstract class BaseActivity extends AppCompatActivity {

    // 캡쳐방지

    // region { 캡쳐방지 라이센스 체크 }
    public int getSecureScreenLicense() {
        int licenseResult = "라이센스 파일 init"

        String message = "";
        switch (licenseResult) {
            // 결과 값 체크 
        }

		// 결과 값이 정상이 아니면 alert
        if (licenseResult < 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(message)
                    .setCancelable(false)
                    .setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            CloseApp();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }

        return licenseResult;
    }
    // endregion
}
