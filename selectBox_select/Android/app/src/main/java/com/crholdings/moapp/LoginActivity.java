package com.crholdings.moapp;

public class LoginActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void showLayout(String status) {
		
        ImageView login_background = null;
        String url_background = "";
        try {
            login_background = (ImageView) findViewById(R.id.login_background);

			// 선택한 계열사 별로 배경 이미지 다르게 가져오기
			int url_position = "selectBox 선택칸";
			url_background = "https://고객사URL/imageURL" + "login_img_" + ("0" + url_position) + ".jpg";

            GlideApp.with(this)
                    .load(url_background)
                    .fitCenter()
                    .centerCrop()
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(login_background);


        } catch(Exception e) {
            WriteLog("showLayout| catch:" + e.toString());
        }
    }

}
